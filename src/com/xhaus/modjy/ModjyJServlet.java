/*###
#
# Copyright Alan Kennedy.
#
# You may contact the copyright holder at this uri:
#
# http://www.xhaus.com/contact/modjy
#
# The licence under which this code is released is the Apache License v2.0.
#
# The terms and conditions of this license are listed in a file contained
# in the distribution that also contained this file, under the name
# LICENSE.txt.
#
# You may also read a copy of the license at the following web address.
#
# http://modjy.xhaus.com/LICENSE.txt
#
###*/

package com.xhaus.modjy;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.Enumeration;
import java.util.Properties;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.python.core.Py;
import org.python.core.PyException;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.core.PySystemState;
import org.python.core.PyType;
import org.python.util.PythonInterpreter;

public class ModjyJServlet extends HttpServlet {

    protected final static String MODJY_PYTHON_CLASSNAME = "modjy_servlet";

    protected final static String LIB_PYTHON = "/WEB-INF/lib-python";

    protected final static String PTH_FILE_EXTENSION = ".pth";

    protected PythonInterpreter interp;

    protected HttpServlet modjyServlet;

    /**
     * Read configuration 
     * 1. Both context and servlet parameters are included in the set, so that
     * the definition of some parameters (e.g python.*) can be shared between multiple WSGI
     * servlets. 
     * 2. servlet params take precedence over context parameters
     */
    protected Properties readConfiguration() {
        Properties props = new Properties();
        // Context parameters
        ServletContext context = getServletContext();
        Enumeration<?> e = context.getInitParameterNames();
        while (e.hasMoreElements()) {
            String name = (String)e.nextElement();
            props.put(name, context.getInitParameter(name));
        }
        // Servlet parameters override context parameters
        e = getInitParameterNames();
        while (e.hasMoreElements()) {
            String name = (String)e.nextElement();
            props.put(name, getInitParameter(name));
        }
        return props;
    }

    /**
     * Initialise the modjy servlet. 
     * 1. Read the configuration 
     * 2. Initialise the jython runtime 
     * 3. Setup, in relation to the J2EE servlet environment 
     * 4. Create the jython-implemented servlet
     * 5. Initialise the jython-implemented servlet
     */
    @Override
    public void init() throws ServletException {
        try {
            Properties props = readConfiguration();
            PythonInterpreter.initialize(System.getProperties(), props, new String[0]);
            PySystemState systemState = new PySystemState();
            interp = new PythonInterpreter(null, systemState);
            setupEnvironment(interp, props, systemState);
            try {
                interp.exec("from modjy.modjy import " + MODJY_PYTHON_CLASSNAME);
            } catch (PyException ix) {
                throw new ServletException("Unable to import '" + MODJY_PYTHON_CLASSNAME
                        + "': maybe you need to set the 'python.home' parameter?", ix);
            }
            PyObject pyServlet = ((PyType)interp.get(MODJY_PYTHON_CLASSNAME)).__call__();
            Object temp = pyServlet.__tojava__(HttpServlet.class);
            if (temp == Py.NoConversion)
                throw new ServletException("Corrupted modjy file: cannot find definition of '"
                        + MODJY_PYTHON_CLASSNAME + "' class");
            modjyServlet = (HttpServlet)temp;
            modjyServlet.init(this);
        } catch (PyException pyx) {
            throw new ServletException("Exception creating modjy servlet: " + pyx.toString(), pyx);
        }
    }

    /**
     * Actually service the incoming request. Simply delegate to the jython servlet.
     *
     * @param req
     *            - The incoming HttpServletRequest
     * @param resp
     *            - The outgoing HttpServletResponse
     */
    @Override
    public void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException,
            IOException {
        modjyServlet.service(req, resp);
    }

    /**
     * Close down the modjy servlet.
     *
     */
    @Override
    public void destroy( ) {
        interp.cleanup();
    }

    /**
     * Setup the modjy environment, i.e. 1. Find the location of the modjy.jar file and add it to
     * sys.path 2. Process the WEB-INF/lib-python directory, if it exists
     *
     * @param interp
     *            - The PythinInterpreter used to service requests
     * @param props
     *            - The properties from which config options are found
     * @param systemState
     *            - The PySystemState corresponding to the interpreter servicing requests
     */
    protected void setupEnvironment(PythonInterpreter interp,
                                    Properties props,
                                    PySystemState systemState) {
        processPythonLib(interp, systemState);
    }

    /**
     * Do all processing in relation to the lib-python subdirectory of WEB-INF
     *
     * @param interp
     *            - The PythonInterpreter used to service requests
     * @param systemState
     *            - The PySystemState whose path should be updated
     */
    protected void processPythonLib(PythonInterpreter interp, PySystemState systemState) {
        // Add the lib-python directory to sys.path
        String pythonLibPath = getServletContext().getRealPath(LIB_PYTHON);
        if (pythonLibPath == null)
            return;
        File pythonLib = new File(pythonLibPath);
        if (!pythonLib.exists())
            return;
        systemState.path.append(new PyString(pythonLibPath));
        // Now check for .pth files in lib-python and process each one
        String[] libPythonContents = pythonLib.list();
        for (String libPythonContent : libPythonContents)
            if (libPythonContent.endsWith(PTH_FILE_EXTENSION))
                processPthFile(interp, systemState, pythonLibPath, libPythonContent);
    }

    /**
     * Process an individual file .pth file in the lib-python directory
     *
     * @param interp
     *            - The PythonInterpreter which will execute imports
     * @param systemState
     *            - The PySystemState whose path should be updated
     * @param pythonLibPath
     *            - The actual path to the lib-python directory
     * @param pthFilename
     *            - The PySystemState whose path should be updated
     */
    protected void processPthFile(PythonInterpreter interp,
                                  PySystemState systemState,
                                  String pythonLibPath,
                                  String pthFilename) {
        try {
            LineNumberReader lineReader = new LineNumberReader(new FileReader(new File(pythonLibPath,
                                                                                       pthFilename)));
            String line;
            while ((line = lineReader.readLine()) != null) {
                line = line.trim();
                if (line.length() == 0)
                    continue;
                if (line.startsWith("#"))
                    continue;
                if (line.startsWith("import"))
                {
                    interp.exec(line);
                    continue;
                }
                File archiveFile = new File(pythonLibPath, line);
                String archiveRealpath = archiveFile.getAbsolutePath();
                systemState.path.append(new PyString(archiveRealpath));
            }
        } catch (IOException iox) {
            System.err.println("IOException: " + iox.toString());
        }
    }
}
