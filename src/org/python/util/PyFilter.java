package org.python.util;

import java.io.File;
import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.python.core.PyException;
import org.python.core.PySystemState;
import org.python.util.PythonInterpreter;

/**
 * Enables you to write Jython modules that inherit from <code>javax.servlet.Filter</code>, and to
 * insert them in your servlet container's filter chain, like any Java <code>Filter</code>.
 *
 * <p>
 * Example:
 *
 * <p>
 * <b>/WEB-INF/filters/HeaderFilter.py:</b>
 *
 * <pre>
 * from javax.servlet import Filter
 *
 * # Module must contain a class with the same name as the module
 * # which in turn must implement javax.servlet.Filter
 * class HeaderFilter (Filter):
 *   def init(self, config):
 *       self.header = config.getInitParameter('header')
 *
 *   def doFilter(self, request, response, chain):
 *       response.setHeader(self.header, &quot;Yup&quot;)
 *       chain.doFilter(request, response)
 * </pre>
 *
 * <p>
 * <b>web.xml:</b>
 * </p>
 *
 * <pre>
 * &lt;!-- PyFilter depends on PyServlet --&gt;
 * &lt;servlet&gt;
 *  &lt;servlet-name&gt;PyServlet&lt;/servlet-name&gt;
 *  &lt;servlet-class&gt;org.python.util.PyServlet&lt;/servlet-class&gt;
 *  &lt;load-on-startup&gt;1&lt;/load-on-startup&gt;
 * &lt;/servlet&gt;
 *
 * &lt;!-- Declare a uniquely-named PyFilter --&gt;
 * &lt;filter&gt;
 *  &lt;filter-name&gt;HeaderFilter&lt;/filter-name&gt;
 *  &lt;filter-class&gt;org.jython.util.PyFilter&lt;/filter-class&gt;
 *
 *  &lt;!-- The special param __filter__ gives the context-relative path to the Jython source file --&gt;
 *  &lt;init-param&gt;
 *    &lt;param-name&gt;__filter__&lt;/param-name&gt;
 *    &lt;param-value&gt;/WEB-INF/pyfilter/HeaderFilter.py&lt;/param-value&gt;
 *  &lt;/init-param&gt;
 *
 *  &lt;!-- Other params are passed on the the Jython filter --&gt;
 *  &lt;init-param&gt;
 *    &lt;param-name&gt;header&lt;/param-name&gt;
 *    &lt;param-value&gt;X-LookMaNoJava&lt;/param-value&gt;
 *  &lt;/init-param&gt;
 * &lt;/filter&gt;
 * &lt;filter-mapping&gt;
 *  &lt;filter-name&gt;HeaderFilter&lt;/filter-name&gt;
 *  &lt;url-pattern&gt;/*&lt;/url-pattern&gt;
 * &lt;/filter-mapping&gt;
 * </pre>
 *
 * <p>
 * PyFilter depends on initialization code from PyServlet being run. If PyServlet is used to serve
 * pages, this code will be executed and PyFilter will work properly. However, if you aren't using
 * PyServlet, the initialization code can be invoked as a ServletContextListener instead of as an
 * HttpServlet. Use the following in web.xml instead of a servlet tag:
 *
 * <pre>
 *   &lt;listener&gt;
 *       &lt;listener-class&gt;org.python.util.PyServlet&lt;/listener-class&gt;
 *       &lt;load-on-startup&gt;1&lt;/load-on-startup&gt;
 *   &lt;/listener&gt;
 * </pre>
 *
 */
public class PyFilter implements Filter {
    public static final String FILTER_PATH_PARAM = "__filter__";

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        request.setAttribute("pyfilter", this);
        getFilter().doFilter(request, response, chain);
    }

    public void init(FilterConfig config) throws ServletException {
        if (config.getServletContext().getAttribute(PyServlet.INIT_ATTR) == null) {
            throw new ServletException("PyServlet has not been initialized, either as a servlet "
                    + "or as context listener.  This must happen before PyFilter is initialized.");
        }
        this.config = config;
        String filterPath = config.getInitParameter(FILTER_PATH_PARAM);
        if (filterPath == null) {
            throw new ServletException("Missing required param '" + FILTER_PATH_PARAM + "'");
        }
        source = new File(getRealPath(config.getServletContext(), filterPath));
        if (!source.exists()) {
            throw new ServletException(source.getAbsolutePath() + " does not exist.");
        }
        interp = new PythonInterpreter(null, new PySystemState());
    }

    private String getRealPath(ServletContext context, String appPath) {
        String realPath = context.getRealPath(appPath);
        // This madness seems to be necessary on Windows
        return realPath.replaceAll("\\\\", "/");
    }

    private Filter getFilter() throws ServletException, IOException {
        if (cached == null || source.lastModified() > loadedMtime) {
            return loadFilter();
        }
        return cached;
    }

    private Filter loadFilter() throws ServletException, IOException {
        loadedMtime = source.lastModified();
        cached = PyServlet.createInstance(interp, source, Filter.class);
        try {
            cached.init(config);
        } catch (PyException e) {
            throw new ServletException(e);
        }
        return cached;
    }

    public void destroy() {
        if (cached != null) {
            cached.destroy();
        }
        if (interp != null) {
            interp.cleanup();
        }
    }

    private PythonInterpreter interp;

    private FilterConfig config;

    private File source;

    private Filter cached;

    private long loadedMtime;
}
