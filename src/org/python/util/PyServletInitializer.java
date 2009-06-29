package org.python.util;

import java.util.Properties;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * Initializes the jython runtime inside a servlet engine. Should be used with {@link PyFilter} to
 * initialize the system before the filter starts. Add the following to web.xml to run the
 * initializer:
 *
 * <pre>
 *   &lt;listener&gt;
 *       &lt;listener-class&gt;org.python.util.PyServletInitializer&lt;/listener-class&gt;
 *       &lt;load-on-startup&gt;1&lt;/load-on-startup&gt;
 *   &lt;/listener&gt;
 *</pre>
 *
 * To use modules from Python's standard library in servlets and filters initialized by this
 * listener, either add the standard library to the lib directory in WEB-INF, or add python.home as
 * a context-param. The latter can be done by adding the following to web.xml:
 *
 * <pre>
 * &lt;context-param&gt;
 *     &lt;param-name&gt;python.home&lt;/param-name&gt;
 *     &lt;param-value&gt;/usr/local/jython-2.5&lt;/param-value&gt;
 * &lt;/context-param&gt;
 * </pre>
 */
public class PyServletInitializer implements ServletContextListener {
    public void contextInitialized(ServletContextEvent evt) {
        PyServlet.init(new Properties(), evt.getServletContext());
    }

    public void contextDestroyed(ServletContextEvent evt) {}
}
