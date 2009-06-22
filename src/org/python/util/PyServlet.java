package org.python.util;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;

import org.python.core.Py;
import org.python.core.PyException;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.core.PySystemState;

/**
 * This servlet is used to re-serve Jython servlets. It stores bytecode for Jython servlets and
 * re-uses it if the underlying .py file has not changed.
 * <p>
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
    @Override
    public void init() {
        rootPath = getServletContext().getRealPath("/");
        if (!rootPath.endsWith(File.separator)) {
            rootPath += File.separator;
        }
        Properties props = new Properties();
        Properties baseProps = PySystemState.getBaseProperties();
        // Context parameters
        ServletContext context = getServletContext();
        Enumeration<?> e = context.getInitParameterNames();
        while (e.hasMoreElements()) {
            String name = (String)e.nextElement();
            props.put(name, context.getInitParameter(name));
        }

        // Config parameters
        e = getInitParameterNames();
        while (e.hasMoreElements()) {
            String name = (String)e.nextElement();
            props.put(name, getInitParameter(name));
        }
        if (props.getProperty("python.home") == null
                && baseProps.getProperty("python.home") == null) {
            props.put("python.home", rootPath + "WEB-INF" + File.separator + "lib");
        }

        PySystemState.initialize(baseProps, props, new String[0]);
        reset();

        PySystemState.add_package("javax.servlet");
        PySystemState.add_package("javax.servlet.http");
        PySystemState.add_package("javax.servlet.jsp");
        PySystemState.add_package("javax.servlet.jsp.tagext");

        PySystemState.add_classdir(rootPath + "WEB-INF" + File.separator + "classes");

        PySystemState.add_extdir(rootPath + "WEB-INF" + File.separator + "lib", true);
    }

    @Override
    public void service(ServletRequest req, ServletResponse res)
        throws ServletException, IOException
    {
        req.setAttribute("pyservlet", this);

        String spath = (String) req.getAttribute("javax.servlet.include.servlet_path");
        if (spath == null) {
            spath = ((HttpServletRequest)req).getServletPath();
            if (spath == null || spath.length() == 0) {
                // Servlet 2.1 puts the path of an extension-matched servlet in PathInfo.
                spath = ((HttpServletRequest)req).getPathInfo();
            }
        }
        String rpath = getServletContext().getRealPath(spath);
        getServlet(rpath).service(req, res);
    }

    @Override
    public void destroy() {
        super.destroy();
        destroyCache();
    }

    /**
     * Clears the cache of loaded servlets and makes a new PythonInterpreter to service further
     * requests.
     */
    public void reset() {
        destroyCache();
        interp = new PythonInterpreter(null, new PySystemState());

        PySystemState sys = Py.getSystemState();
        sys.path.append(new PyString(rootPath));

        String modulesDir = rootPath + "WEB-INF" + File.separator + "jython";
        sys.path.append(new PyString(modulesDir));
    }

    private synchronized HttpServlet getServlet(String path)
        throws ServletException, IOException
    {
        CacheEntry entry = cache.get(path);
        if (entry == null || new File(path).lastModified() > entry.date) {
            return loadServlet(path);
        }
        return entry.servlet;
    }

    private HttpServlet loadServlet(String path)
        throws ServletException, IOException
    {
        File file = new File(path);

        // Extract servlet name from path (strip ".../" and ".py")
        int start = path.lastIndexOf(File.separator);
        if (start < 0) {
            start = 0;
        } else {
            start++;
        }
        int end = path.lastIndexOf('.');
        if (end < 0 || end <= start) {
            end = path.length();
        }
        String name = path.substring(start, end);

        HttpServlet servlet;
        try {
            interp.set("__file__", path);
            interp.execfile(path);
            PyObject cls = interp.get(name);
            if (cls == null) {
                throw new ServletException("No callable (class or function) named " + name + " in "
                        + path);
            }
            PyObject pyServlet = cls.__call__();
            Object o = pyServlet.__tojava__(HttpServlet.class);
            if (o == Py.NoConversion) {
                throw new ServletException("The value from " + name + " must extend HttpServlet");
            }
            servlet = (HttpServlet)o;
            servlet.init(getServletConfig());
        } catch (PyException e) {
            throw new ServletException(e);
        }
        cache.put(path, new CacheEntry(servlet, file.lastModified()));
        return servlet;
    }

    private void destroyCache() {
        for (CacheEntry entry : cache.values()) {
            entry.servlet.destroy();
        }
        cache.clear();
    }

    private static class CacheEntry {
        public long date;
        public HttpServlet servlet;

        CacheEntry(HttpServlet servlet, long date) {
            this.servlet=  servlet;
            this.date = date;
        }
    }

    private PythonInterpreter interp;
    private String rootPath;
    private Map<String, CacheEntry> cache = Generic.map();
}