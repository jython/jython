
package org.python.util;

import java.io.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;
import org.python.core.*;


/**
 * This servlet is used to re-serve JPython servlets.  It stores
 * bytecode for JPython servlets and re-uses it if the underlying .py
 * file has not changed.
 * <p>
 * Many people have been involved with this class:
 * <ul>
 * <li>Chris Gokey
 * <li>David Syer
 * <li>Finn Bock
 * </ul>
 * If somebody is missing from this list, let us know.
 * <p>
 *
 * e.g. http://localhost:8080/test/hello.py
 * <pre>
 *
 * from javax.servlet.http import HttpServlet
 * class hello(HttpServlet):
 *     def doGet(self, req, res):
 *         res.setContentType("text/html");
 *         out = res.getOutputStream()
 *         print >>out, "<html>"
 *         print >>out, "<head><title>Hello World, How are we?</title></head>"
 *         print >>out, "<body>Hello World, how are we?"
 *         print >>out, "</body>"
 *         print >>out, "</html>"
 *         out.close()
 *         return
 * </pre>
 *
 * in web.xml for the PyServlet context:
 * <pre>
 * &lt;web-app>
 *     &lt;servlet>
 *         &lt;servlet-name>PyServlet&lt;/servlet-name>
 *         &lt;servlet-class>org.python.util.PyServlet&lt;/servlet-class>
 *         &lt;init-param>
 *             &lt;param-name>python.home&lt;/param-name>
 *             &lt;param-value>/usr/home/jython-2.1&lt;/param-value>
 *         &lt;/init-param>
 *     &lt;/servlet>
 *     &lt;servlet-mapping>
 *         &lt;servlet-name>PyServlet&lt;/servlet-name>
 *         &lt;url-pattern>*.py&lt;/url-pattern>
 *     &lt;/servlet-mapping>
 * &lt;/web-app>
 *
 * </pre>
 */

public class PyServlet extends HttpServlet {
    private PythonInterpreter interp;
    private Hashtable cache = new Hashtable();
    private String rootPath;


    public void init() {
        rootPath = getServletContext().getRealPath("/");
        if (!rootPath.endsWith(File.separator))
            rootPath += File.separator;

        Properties props = new Properties();
        Enumeration e = getInitParameterNames();
        while (e.hasMoreElements()) {
            String name = (String) e.nextElement();
            props.put(name, getInitParameter(name));
        }
        if (props.getProperty("python.home") == null &&
                                System.getProperty("python.home") == null) {
            props.put("python.home", rootPath + "WEB-INF" +
                                             File.separator + "lib");
        }

        PythonInterpreter.initialize(System.getProperties(), props,
                                     new String[0]);
        reset();

        PySystemState sys = Py.getSystemState();
        sys.add_package("javax.servlet");
        sys.add_package("javax.servlet.http");
        sys.add_package("javax.servlet.jsp");
        sys.add_package("javax.servlet.jsp.tagext");

        sys.add_classdir(rootPath + "WEB-INF" +
                          File.separator + "classes");

        sys.add_extdir(rootPath + "WEB-INF" +
                          File.separator + "lib");
    }

    /**
     * Implementation of the HttpServlet main method.
     * @param req the request parameter.
     * @param res the response parameter.
     * @exception ServletException
     * @exception IOException
     */
    public void service(ServletRequest req, ServletResponse res)
        throws ServletException, IOException
    {
        req.setAttribute("pyservlet", this);

        String spath = (String)req.getAttribute(
                                    "javax.servlet.include.servlet_path");
        if (spath == null) {
            spath = ((HttpServletRequest) req).getServletPath();
            if (spath == null || spath.length() == 0) {
                // Servlet 2.1 puts the path of an extension-matched
                // servlet in PathInfo.
                spath = ((HttpServletRequest) req).getPathInfo();
            }
        }
        String rpath = getServletContext().getRealPath(spath);

        interp.set("__file__", rpath);

        HttpServlet servlet = getServlet(rpath);
        if (servlet !=  null)
            servlet.service(req, res);
        else
            throw new ServletException("No python servlet found at:" + spath);
    }

    public void reset() {
        destroyCache();
        interp = new PythonInterpreter(null, new PySystemState());
        cache.clear();
        PySystemState sys = Py.getSystemState();
        sys.path.append(new PyString(rootPath));

        String modulesDir = rootPath + "WEB-INF" +
                            File.separator + "jython";
        sys.path.append(new PyString(modulesDir));
    }

    private synchronized HttpServlet getServlet(String path)
        throws ServletException, IOException
    {
        CacheEntry entry = (CacheEntry) cache.get(path);
        if (entry == null)
            return loadServlet(path);
        File file = new File(path);
        if (file.lastModified() > entry.date)
            return loadServlet(path);
        return entry.servlet;
    }

    private HttpServlet loadServlet(String path)
        throws ServletException, IOException
    {
        HttpServlet servlet = null;
        File file = new File(path);

        // Extract servlet name from path (strip ".../" and ".py")
        int start = path.lastIndexOf(File.separator);
        if (start < 0)
            start = 0;
        else
            start++;
        int end = path.lastIndexOf(".py");
        if ((end < 0) || (end <= start))
            end = path.length();
        String name = path.substring(start, end);

        try {
            interp.execfile(path);
            PyObject cls = interp.get(name);
            if (cls == null)
                throw new ServletException("No callable (class or function) "+
                                       "named " + name + " in " + path);

            PyObject pyServlet = cls.__call__();
            Object o = pyServlet.__tojava__(HttpServlet.class);
            if (o == Py.NoConversion)
                throw new ServletException("The value from " + name +
                                       "must extend HttpServlet");
            servlet = (HttpServlet)o;
            servlet.init(getServletConfig());

        } catch (PyException e) {
            throw new ServletException("Could not create "+
                                       "Jython servlet" + e.toString());
        }
        CacheEntry entry = new CacheEntry(servlet, file.lastModified());
        cache.put(path, entry);
        return servlet;
    }

    public void destroy() {
        destroyCache();
    }

    private void destroyCache() {
        for (Enumeration e = cache.elements(); e.hasMoreElements(); ) {
            CacheEntry entry = (CacheEntry) e.nextElement();
            entry.servlet.destroy();
        }
    }

}

class CacheEntry {
    public long date;
    public HttpServlet servlet;

    CacheEntry(HttpServlet servlet, long date) {
        this.servlet=  servlet;
        this.date = date;
    }
}
