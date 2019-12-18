package org.python.util;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;

import org.python.Version;
import org.python.core.PrePy;
import org.python.core.Py;
import org.python.core.PyException;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.core.PySystemState;

/**
 * This servlet is used to re-serve Jython servlets. It stores bytecode for Jython servlets and
 * re-uses it if the underlying .py file has not changed.
 * <p>
 * e.g. {@code http://localhost:8080/test/hello.py}
 * <pre>{@literal
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
 * }</pre>
 *
 * in web.xml for the PyServlet context:
 * <pre>{@literal
 * <web-app>
 *     <servlet>
 *         <servlet-name>PyServlet</servlet-name>
 *         <servlet-class>org.python.util.PyServlet</servlet-class>
 *         <init-param>
 *             <param-name>python.home</param-name>
 *             <param-value>/usr/home/jython-2.5</param-value>
 *         </init-param>
 *     </servlet>
 *     <servlet-mapping>
 *         <servlet-name>PyServlet</servlet-name>
 *         <url-pattern>*.py</url-pattern>
 *     </servlet-mapping>
 * </web-app>
 * }</pre>
 */
public class PyServlet extends HttpServlet {

    protected static final Logger logger = Logger.getLogger("org.python.servlet");

    public static final String SKIP_INIT_NAME = "skip_jython_initialization";

    protected static final String INIT_ATTR = "__jython_initialized__";

    @Override
    public void init() {
        logger.log(Level.INFO, "Jython {0} servlet {1}",
                new Object[] {Version.PY_VERSION, getServletName()});

        // Config parameters
        Properties props = new Properties();
        Enumeration<?> e = getInitParameterNames();
        while (e.hasMoreElements()) {
            String name = (String)e.nextElement();
            props.put(name, getInitParameter(name));
        }

        boolean initialize = getServletConfig().getInitParameter(SKIP_INIT_NAME) != null;

        if (getServletContext().getAttribute(INIT_ATTR) != null) {
            if (initialize) {
                logger.log(Level.WARNING, //
                        "Jython has already been initialized in this context."
                                + " Not initializing for ''{0}''."
                                + " Add {1} as an init param to this servlet''s configuration"
                                + " to indicate this is expected.",
                        new Object[] {getServletName(), SKIP_INIT_NAME});
            }
        } else if (initialize) {
            init(props, getServletContext());
        }
        reset();
    }

    /**
     * PyServlet's initialization can be performed as a ServletContextListener or as a regular
     * servlet, and this is the shared init code. If both initializations are used in a single
     * context, the system state initialization code only runs once.
     */
    protected static void init(Properties props, ServletContext context) {

        String rootPath = getRootPath(context);
        context.setAttribute(INIT_ATTR, true);
        Properties baseProps = PrePy.getSystemProperties();

        // Context parameters
        Enumeration<?> e = context.getInitParameterNames();
        while (e.hasMoreElements()) {
            String name = (String)e.nextElement();
            props.put(name, context.getInitParameter(name));
        }
        if (props.getProperty("python.home") == null
                && baseProps.getProperty("python.home") == null) {
            props.put("python.home", rootPath + "WEB-INF" + File.separator + "lib");
        }

        PySystemState.initialize(baseProps, props, new String[0]);
        PySystemState.add_package("javax.servlet");
        PySystemState.add_package("javax.servlet.http");
        PySystemState.add_package("javax.servlet.jsp");
        PySystemState.add_package("javax.servlet.jsp.tagext");
        PySystemState.add_classdir(rootPath + "WEB-INF" + File.separator + "classes");
        PySystemState.add_extdir(rootPath + "WEB-INF" + File.separator + "lib", true);
    }

    protected static PythonInterpreter createInterpreter(ServletContext servletContext) {
        String rootPath = getRootPath(servletContext);
        PySystemState sys = new PySystemState();
        PythonInterpreter interp = new PythonInterpreter(Py.newStringMap(), sys);
        sys.path.append(new PyString(rootPath));

        String modulesDir = rootPath + "WEB-INF" + File.separator + "jython";
        sys.path.append(new PyString(modulesDir));
        return interp;
    }

    protected static String getRootPath(ServletContext context) {
        String rootPath = context.getRealPath("/");
        if (!rootPath.endsWith(File.separator)) {
            rootPath += File.separator;
        }
        return rootPath;
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
        interp = createInterpreter(getServletContext());
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

        HttpServlet servlet = createInstance(interp, file, HttpServlet.class);
        try {
            servlet.init(getServletConfig());
        } catch (PyException e) {
            throw new ServletException(e);
        }
        cache.put(path, new CacheEntry(servlet, file.lastModified()));
        return servlet;
    }

    protected static <T> T createInstance(PythonInterpreter interp, File file, Class<T> type)
            throws ServletException {
        Matcher m = FIND_NAME.matcher(file.getName());
        if (!m.find()) {
            throw new ServletException("I can't guess the name of the class from "
                    + file.getAbsolutePath());
        }
        String name = m.group(1);
        try {
            interp.set("__file__", file.getAbsolutePath());
            interp.execfile(file.getAbsolutePath());
            PyObject cls = interp.get(name);
            if (cls == null) {
                throw new ServletException("No callable (class or function) named " + name + " in "
                        + file.getAbsolutePath());
            }
            PyObject pyServlet = cls.__call__();
            Object o = pyServlet.__tojava__(type);
            if (o == Py.NoConversion) {
                throw new ServletException("The value from " + name + " must extend "
                        + type.getSimpleName());
            }
            @SuppressWarnings("unchecked")
            T asT = (T)o;
            return asT;
        } catch (PyException e) {
            throw new ServletException(e);
        }
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
            this.servlet = servlet;
            this.date = date;
        }
    }

    private static final Pattern FIND_NAME = Pattern.compile("([^/]+)\\.py$");

    private PythonInterpreter interp;
    private Map<String, CacheEntry> cache = Generic.map();
}