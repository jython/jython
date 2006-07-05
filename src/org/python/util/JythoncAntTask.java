/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2002 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "The Jakarta Project", "Ant", and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */
//package org.apache.tools.ant.taskdefs.optional.python;
package org.python.util;

import org.apache.tools.ant.taskdefs.MatchingTask;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.Java;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.DirectoryScanner;
import org.python.core.PySystemState;

import java.io.File;

/**
 * Jythonc is a Python compiler into Java Bytecode. So you can
 * call your python code from Java, and call you java code from
 * Python, create bean, servlet...
 *
 * <p>
 *
 * The task is a directory based task, so attributes like <b>includes="*.py"</b> and
 * <b>excludes="broken.py"</b> can be used to control the files pulled in. By default,
 * all *.py files from the project folder down are included in the command.
 *
 * @author Cyrille Morvan - cmorvan@ingenosya.com - Ingenosya France
 * @version 1.0
 */
public class JythoncAntTask extends MatchingTask {

  protected static final String JYTHONC_PY = "Tools/jythonc/jythonc.py";
  protected static final String JYTHON_CLASS = "org.python.util.jython";

  /**
   * The classpath for the virtual machine.
   */
  protected Path classpath;

  /**
   * Put all compiled code into the named Java package.
   */
  protected String packageName;

  /**
   * Specifies a .jar file to create and put the results of
   * the freeze into. Set the deep option to true.
   */
  protected File jarFile;

  /**
   * the compiler.
   */
  protected File jythoncpy;

  /**
   * Compile all Python dependencies of the module. This is
   * used for creating applets.
   */
  protected boolean deep;

  /**
   * Include the core Jython libraries (about 130K).
   * Needed for applets since Netscape doesn't yet support
   * multiple archives.
   */
  protected boolean core;

  /**
   * Include all of the Jython libraries (everything in core + compiler and
   * parser).
   */
  protected boolean all;

  /**
   * Include Java dependencies from this list of packages. Default is
   * org.python.modules and org.apache.oro.text.regex.
   */
  protected String addpackages;

  /**
   *  Compile into jarfile, including the correct manifest for the bean.
   */
  protected File jarFileBean;

  /**
   * Don't include any of these modules in compilation. This is a
   * comma-separated list of modules.
   */
  protected String skipModule;

  /**
   * Compiler name.
   */
  protected String compiler;

  /**
   * Use a different compiler than `standard' javac. If this is set to "NONE"
   * then compile ends with the generation of the Java source file.
   * Alternatively, you can set the property python.jythonc.compiler in the
   * registry.
   */
  protected String compileropts;

  /**
   * Options passed directly to the Java compiler. Alternatively, you can set
   * the property python.jythonc.compileropts in the registry.
   */
  protected String falsenames;

  /**
   * Path to jython directory.
   */
  protected File jythonHome;

  /**
   * Destination, build directory.
   */
  protected File destDir;

  /**
   * Source directory.
   */
  protected File srcDir;

  /**
   * Specify the working directory where the generated Java source code is
   * placed. Default is "./jpywork"
   */
  protected File workdir;

  /**
   * aditionnals args.
   */
  protected String extraArgs;

  /**
   * constructor set up the search pattern
   */
  public JythoncAntTask() {
    setIncludes("**/*.py");
  }

  /**
   * Add a classpath.  Used to handle the nested classpath
   * element.
   * @return A Path object representing the classpath to be used.
   */
  public Path createClasspath() {
    if(classpath == null) {
      classpath = new Path(this.getProject());
    }
    return classpath.createPath();
  }

  /**
   * Sets the classpath field.
   * @param aClasspath A Path object representing the "classpath" attribute.
   */
  public void setClasspath(Path aClasspath) {
    classpath = aClasspath;
  }

  /**
   * Put all compiled code into the named Java package.
   * @param aString the packake name.
   */
  public void setPackage(String aString) {
    packageName = aString;
  }

  /**
   * Specifies a .jar file to create and put the results of
   * the freeze into. Set the deep option to true.
   */
  public void setJar(File aJarFile) {
    jarFile = aJarFile;
    deep = true;
  }

  /**
   * Include the core Jython libraries (about 130K).
   * Needed for applets since Netscape doesn't yet support
   * multiple archives. Set the deep option to true.
   */
  public void setCore(boolean aValue) {
    core = aValue;
    deep = true;
  }

  /**
   * Include all of the Jython libraries (everything in core + compiler and
   * parser). Set the deep option to true.
   */
  public void setAll(boolean aValue) {
    all = aValue;
    deep = true;
  }

  /**
   * Compile into jarfile, including the correct manifest for the bean.
   */
  public void setBean(File aJarFileBean) {
    jarFileBean  = aJarFileBean;
  }

  /**
   * Don't include any of these modules in compilation. This is a
   * comma-separated list of modules.
   */
  public void setSkip(String aValue) {
    skipModule = aValue;
  }

  /**
   * Compile all Python dependencies of the module. This is
   * used for creating applets.
   */
  public void setDeep(boolean aValue) {
    deep  = aValue;
  }


  /**
   * Include Java dependencies from this list of packages. Default is
   * org.python.modules and org.apache.oro.text.regex.
   */
  public void setAddpackages(String aValue) {
    addpackages = aValue;
  }

  /**
   * Specify the working directory where the generated Java source code is
   * placed. Default is "./jpywork"
   */
  public void setWorkdir(File aValue) {
    if( aValue.exists() ) {
      if( ! aValue.isDirectory() ) {
        throw new BuildException( "Workdir ("+ aValue + ") is not a directory"  );
      }
    } else {
      aValue.mkdirs();
    }
    workdir = aValue;
  }

  /**
   * Set the compiler.
   */
  public void setCompiler(String aCompiler) {
    compiler = aCompiler;
  }

  /**
   * Options passed directly to the Java compiler. Alternatively, you can set
   * the property python.jythonc.compileropts in the registry.
   */
  public void setCompileropts(String aValue) {
    compileropts = aValue;
  }

  /**
   * A comma-separated list of names that are always false. Can be used to
   * short-circuit if clauses.
   */
  public void setFalsenames(String aValue) {
    falsenames = aValue;
  }

  /**
   * Jython home directory.
   */
  public void setHome(File aFile) {
    jythonHome = aFile;
  }

  /**
   * Home for the source.
   */
  public void setSrcdir(File aFile) {
    srcDir = aFile;
  }

  /**
   * Home for the destination (build).
   */
  public void setDestdir(File aFile) {
    destDir = aFile;
  }

  /**
   * Change the default Python compiler.
   */
  public void setJythoncpy(File aValue) {
    jythoncpy = aValue;
  }

  /**
   * sets some additional args to send to jythonc.
   */
  public void setArgs(String aValue) {
    extraArgs = aValue;
  }

  /**
   * get the compiler option, null if none.
   */
  public String getCompilerOptions() {
    StringBuffer aStringBuffer = new StringBuffer();
    if( destDir != null ) {
      aStringBuffer.append("-d \"");
      aStringBuffer.append( destDir );
      aStringBuffer.append("\"");

      createClasspath().setLocation(destDir);
      destDir.mkdirs();
    }
    if( compileropts != null ) {
      aStringBuffer.append(compileropts);
    }
    if( aStringBuffer.length() == 0 ) {
      return null;
    } else {
      return aStringBuffer.toString();
    }
  }

  /**
   * Get the path to the jython home (or python home)
   */
  public File getPythonHome() {
    if(jythonHome == null ) {
      String aPythonHome = getProject().getProperty("python.home");
      if(aPythonHome == null ) {
        throw new BuildException("No python.home or home specified");
      }
      jythonHome = new File(aPythonHome);
    }
    return jythonHome;
  }

  /**
   * Get the path to the jython compiler file (in python).
   */
  public File getJythoncPY() {
    if(jythoncpy == null ) {
      return new File(getPythonHome(),JYTHONC_PY);
    }
    return jythoncpy;
  }


  /**
   * Exectute the compiler.
   */
  public void execute() {
    try {
      Java javaTask = null;

      javaTask = (Java)getProject().createTask("java");
      javaTask.setTaskName("jythonc");

      javaTask.setClassname( JYTHON_CLASS );

      javaTask.createJvmarg().setValue( "-Dpython.home=" + getPythonHome() );

      // classpath
      File aJythonJarFile = new File(getPythonHome(), PySystemState.JYTHON_JAR );
      createClasspath().setLocation(aJythonJarFile);

      javaTask.setClasspath(classpath);

      // jythonc file
      javaTask.createArg().setFile( getJythoncPY() );

      if( packageName != null ) {
        javaTask.createArg().setValue("--package");
        javaTask.createArg().setValue(packageName);
      }

      if( jarFile != null ) {
        javaTask.createArg().setValue( "--jar" );
        javaTask.createArg().setFile( jarFile );
      }

      if(deep) {
        javaTask.createArg().setValue( "--deep" );
      }

      if(core) {
        javaTask.createArg().setValue( "--core" );
      }

      if(all) {
        javaTask.createArg().setValue( "--all" );
      }

      if( jarFileBean != null ) {
        javaTask.createArg().setValue( "--bean" );
        javaTask.createArg().setFile( jarFileBean );
      }

      if( addpackages != null ) {
        javaTask.createArg().setValue( "--addpackages " );
        javaTask.createArg().setValue( addpackages );
      }

      if( workdir != null ) {
        javaTask.createArg().setValue( "--workdir " );
        javaTask.createArg().setFile( workdir );
      }

      if( skipModule != null ) {
        javaTask.createArg().setValue("--skip");
        javaTask.createArg().setValue(skipModule);
      }

      // --compiler
      if( compiler == null ) {
        // try to use the compiler specified by build.compiler. Right now we are
        // just going to allow Jikes
        String buildCompiler = getProject().getProperty("build.compiler");
        if (buildCompiler != null && buildCompiler.equals("jikes")) {
            javaTask.createArg().setValue("--compiler");
            javaTask.createArg().setValue("jikes");
        }
      } else {
        javaTask.createArg().setValue("--compiler");
        javaTask.createArg().setValue(compiler);
      }

      String aCompilerOpts = getCompilerOptions();
      if( aCompilerOpts != null ) {
        javaTask.createArg().setValue("--compileropts");
        javaTask.createArg().setValue(aCompilerOpts);
      }

      if( falsenames != null ) {
        javaTask.createArg().setValue("--falsenames");
        javaTask.createArg().setValue(falsenames);
      }

      if( extraArgs != null ) {
        javaTask.createArg().setLine(extraArgs);
      }

      //get dependencies list.
      if( srcDir == null ) {
        srcDir = project.resolveFile(".");
      }
      DirectoryScanner scanner = super.getDirectoryScanner(srcDir);
      String[] dependencies = scanner.getIncludedFiles();
      log("compiling " + dependencies.length + " file" +
                         ((dependencies.length == 1)?"":"s"));
      String baseDir = scanner.getBasedir().toString() + File.separator;
      //add to the command
      for (int i = 0; i < dependencies.length; i++) {
          String targetFile = dependencies[i];
          javaTask.createArg().setValue(baseDir + targetFile);
      }

      // change the location directory
      javaTask.setDir(srcDir);
      javaTask.setFork(true);
      if (javaTask.executeJava() != 0) {
          throw new BuildException("jythonc reported an error");
      }
    } catch (Exception e) {
        // Have to catch this because of the semantics of calling main()
        String msg = "Exception while calling " + JYTHON_CLASS + ". Details: " + e.toString();
        throw new BuildException(msg, e);
    }
  }
}

