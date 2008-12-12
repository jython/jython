/*###
#
# Copyright 2004-2008 Alan Kennedy. 
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
# http://www.xhaus.com/modjy/LICENSE.txt
#
###*/

package com.xhaus.modjy;

import java.io.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;
import org.python.core.*;
import org.python.util.*;

public class ModjyJServlet extends HttpServlet
{

	protected final static String MODJY_PYTHON_CLASSNAME = "modjy_servlet";
	protected final static String LIB_PYTHON = "/WEB-INF/lib-python";
	protected final static String PTH_FILE_EXTENSION = ".pth";

	protected PythonInterpreter interp;
	protected HttpServlet modjyServlet;

	/**
	* Read configuration
	* 1. Both context and servlet parameters are included in the set,
	*    so that the definition of some parameters (e.g python.*) can be shared 
	*    between multiple WSGI servlets.
	* 2. servlet params take precedence over context parameters
	*/

	protected Properties readConfiguration ( )
	{
		Properties props = new Properties();
        // Context parameters
		ServletContext context = getServletContext();
		Enumeration e = context.getInitParameterNames();
		while (e.hasMoreElements())
       	{
			String name = (String) e.nextElement();
			props.put(name, context.getInitParameter(name));
		}
        // Servlet parameters override context parameters
		e = getInitParameterNames();
		while (e.hasMoreElements())
		{
			String name = (String) e.nextElement();
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

	public void init ( )
		throws ServletException
	{
		try
	   	{
	   		Properties props = readConfiguration();
			PythonInterpreter.initialize(System.getProperties(), props, new String[0]);
			interp = new PythonInterpreter(null, new PySystemState());
			String modjyJarLocation = setupEnvironment(props, Py.getSystemState());
			try
				{ interp.exec("from modjy import "+MODJY_PYTHON_CLASSNAME); }
			catch (PyException ix)
				{ throw new ServletException("Unable to import '"+MODJY_PYTHON_CLASSNAME+"' from "+modjyJarLocation+
					": do you maybe need to set the 'modjy_jar.location' parameter?");}
			PyObject pyServlet = ((PyClass)interp.get(MODJY_PYTHON_CLASSNAME)).__call__();
			Object temp = pyServlet.__tojava__(HttpServlet.class);
			if (temp == Py.NoConversion)
				throw new ServletException("Corrupted modjy file: cannot find definition of '"+MODJY_PYTHON_CLASSNAME+"' class");
			modjyServlet = (HttpServlet) temp;
			modjyServlet.init(this);
		}
		catch (PyException pyx)
		{
			throw new ServletException("Exception creating modjy servlet: " + pyx.toString(), pyx);
		}
	}

	/**
	* Actually service the incoming request.
	* Simply delegate to the jython servlet.
	*
	* @param request - The incoming HttpServletRequest
	* @param response - The outgoing HttpServletResponse
	*/

	public void service ( HttpServletRequest req, HttpServletResponse resp )
		throws ServletException, IOException
	{
		modjyServlet.service(req, resp);
	}

	/**
	* Setup the modjy environment, i.e.
	* 1. Find the location of the modjy.jar file and add it to sys.path
	* 2. Process the WEB-INF/lib-python directory, if it exists
	*
	* @param props The properties from which config options are found
	* @param systemState The PySystemState corresponding to the interpreter servicing requests
	* @returns A String giving the path to the modjy.jar file (which is used only for error reporting)
	*/

	protected String setupEnvironment(Properties props, PySystemState systemState)
	{
		String modjyJarLocation = locateModjyJar(props);
		systemState.path.append(new PyString(modjyJarLocation));
		processPythonLib(systemState);
		return modjyJarLocation;
	}

	/**
	* Find out the location of "modjy.jar", so that it can
	* be added to the sys.path and thus imported
	*
	* @param The properties from which config options are found
	* @returns A String giving the path to the modjy.jar file
	*/

	protected String locateModjyJar ( Properties props )
	{
		// Give priority to modjy_jar.location
		if (props.get("modjy_jar.location") != null)
			return (String)props.get("modjy_jar.location");
		// Then try to find it in WEB-INF/lib
		String location = getServletContext().getRealPath("/WEB-INF/lib/modjy.jar");
		if (location != null)
		{
			File f = new File(location);
			if (f.exists())
				return location;
		}
		// Try finding the archive that this class was loaded from
		try
			{ return this.getClass().getProtectionDomain().getCodeSource().getLocation().getFile(); }
		catch (Exception x)
			{ return null;}
	}

	/**
	* Do all processing in relation to the lib-python subdirectory of WEB-INF
	*
	* @param systemState - The PySystemState whose path should be updated
	*/

	protected void processPythonLib(PySystemState systemState)
	{
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
		for (int ix = 0 ; ix < libPythonContents.length ; ix++)
			if (libPythonContents[ix].endsWith(PTH_FILE_EXTENSION))
				processPthFile(systemState, pythonLibPath, libPythonContents[ix]);
	}

	/**
	* Process an individual file .pth file in the lib-python directory
	*
	* @param systemState - The PySystemState whose path should be updated
	* @param pythonLibPath - The actual path to the lib-python directory
	* @param pthFilename - The PySystemState whose path should be updated
	*/

	protected void processPthFile(PySystemState systemState, String pythonLibPath, String pthFilename)
	{
		try
		{
			LineNumberReader lineReader = new LineNumberReader(new FileReader(new File(pythonLibPath, pthFilename)));
			String line;
			while ((line = lineReader.readLine()) != null)
			{
				line = line.trim();
				if (line.startsWith("#"))
					continue;
				File archiveFile = new File(pythonLibPath, line);
				String archiveRealpath = archiveFile.getAbsolutePath();
				systemState.path.append(new PyString(archiveRealpath));
			}
		}
		catch (IOException iox)
		{
			System.err.println("IOException: " + iox.toString());
		}
	}
}
