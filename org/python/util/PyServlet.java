
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
 *
 * Many people have been involved with this class:
 *
 * Chris Gokey
 * David Syer
 * Finn Bock
 *
 * If somebody is missing from this list, let us know. 
 *
 *
 * <pre>
 *
 * e.g. http://localhost:8080/test/hello.py
 *
 * class hello(HttpServlet):
 *     def doGet(self,req, res):
 *         res.setContentType("text/html");
 *         out = res.getOutputStream()
 *         print >>out, "<html>"
 *         print >>out, "<head><title>Hello World, How are we?</title></head>"
 *         print >>out, "<body>Hello World, how are we?"
 *         print >>out, "</body>"
 *         print >>out, "</html>"
 *         out.close()
 *         return
 *
 * in web.xml for the PyServlet context:
 *
 * <web-app>
 *     <servlet>
 *         <servlet-name>PyServlet</servlet-name>
 *         <servlet-class>org.python.util.PyServlet</servlet-class>
 *         <init-param>
 *             <param-name>python.home</param-name>
 *             <param-value>/usr/home/jython-2.0</param-value>
 *         </init-param>
 *     </servlet>
 *     <servlet-mapping>
 *         <servlet-name>PyServlet</servlet-name>
 *         <url-pattern>*.py</url-pattern>
 *     </servlet-mapping>
 * </web-app>
 *
 * </pre>
 */

public class PyServlet extends HttpServlet {
    private PythonInterpreter interp;
    private Hashtable cache = new Hashtable();
    private String rootPath;


    public void init() {
        rootPath = getServletContext().getRealPath("/");

        Properties props = new Properties();
        Enumeration e = getInitParameterNames();
        while (e.hasMoreElements()) {
            String name = (String) e.nextElement();
            props.setProperty(name, getInitParameter(name));
        }
        if (props.getProperty("python.home") == null && 
                                   System.getProperty("python.home") == null) {
            props.setProperty("python.home", rootPath + File.separator +
                                             "WEB-INF" + File.separator +
                                             "lib");
        }

        props.setProperty("python.packages.directories",
                          "java.ext.dirs,pyservlet.lib");
        props.setProperty("pyservlet.lib",
                          rootPath + File.separator +
                          "WEB-INF" + File.separator +
                          "lib");

        props.setProperty("python.packages.paths",
                          "java.class.path,sun.boot.class.path,"+
                          "pyservlet.classes");
        props.setProperty("pyservlet.classes",
                          rootPath + File.separator +
                          "WEB-INF" + File.separator +
                          "classes");
 
        PythonInterpreter.initialize(System.getProperties(), props, new String[0]);
        reset();

        PySystemState sys = Py.getSystemState();
        sys.add_package("javax.servlet");
        sys.add_package("javax.servlet.http");
        sys.add_package("javax.servlet.jsp");
        sys.add_package("javax.servlet.jsp.tagext");
    }

    public void service(ServletRequest req, ServletResponse res)
        throws ServletException, IOException
    {
        req.setAttribute("pyservlet", this);

        String spath = (String)req.getAttribute("javax.servlet.include.servlet_path");
        if (spath == null)
            spath = ((HttpServletRequest) req).getServletPath();
        String rpath = getServletContext().getRealPath(spath);

        interp.set("__file__", rpath);
        
        HttpServlet servlet = getServlet(rpath);
        if (servlet !=  null)
            servlet.service(req, res);
        else
            throw new ServletException("No python servlet found at:" + spath);
    }

    public void reset() {
        interp = new PythonInterpreter(null, new PySystemState());
        cache.clear();
        PySystemState sys = Py.getSystemState();
        sys.path.append(new PyString(rootPath));

        String modulesDir = rootPath + File.separator +
                            "WEB-INF" + File.separator +
                            "jython";
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
}

class CacheEntry {
    public long date;
    public HttpServlet servlet;

    CacheEntry(HttpServlet servlet, long date) {
        this.servlet=  servlet;
        this.date = date;
    }
}
