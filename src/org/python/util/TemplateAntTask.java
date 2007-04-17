/*
 * The Apache Software License, Version 1.1
 * 
 * Copyright (c) 2002 The Apache Software Foundation. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any, must
 * include the following acknowlegement: "This product includes software
 * developed by the Apache Software Foundation (http://www.apache.org/)."
 * Alternately, this acknowlegement may appear in the software itself, if and
 * wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "The Jakarta Project", "Ant", and "Apache Software Foundation"
 * must not be used to endorse or promote products derived from this software
 * without prior written permission. For written permission, please contact
 * apache@apache.org.
 * 
 * 5. Products derived from this software may not be called "Apache" nor may
 * "Apache" appear in their names without prior written permission of the Apache
 * Group.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE APACHE
 * SOFTWARE FOUNDATION OR ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 * OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many individuals on
 * behalf of the Apache Software Foundation. For more information on the Apache
 * Software Foundation, please see <http://www.apache.org/>.
 */
package org.python.util;

import org.apache.tools.ant.taskdefs.MatchingTask;
import org.apache.tools.ant.taskdefs.Execute;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.Java;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.DirectoryScanner;
import java.io.File;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.FileInputStream;
import java.io.Reader;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;

/**
 * Template is an Ant task for generating new-style object definitions based on
 * template files. These template files come in two flavors; *.expose and
 * *.derived, both are supported by this task.
 * 
 * @author Matt Small - msmall@activegrid.com
 * @version 1.0
 */
public class TemplateAntTask extends MatchingTask {

    /**
     * Specifies the Python interpreter.
     */
    protected String python;

    /**
     * Specifies the Python interpreter.
     */
    public void setPython(String aPE) {
        python = aPE;
    }

    /**
     * Source paths.
     */
    private File srcDir;

    /**
     * Source paths.
     */
    public void setSrcdir(String in) {
        srcDir = new File(getProject().replaceProperties(in));
    }

    /**
     * Verbose flag.
     */
    protected boolean verbose = false;

    /**
     * Verbose flag.
     */
    public void setVerbose(String in) {
        verbose = (new Boolean(getProject().replaceProperties(in))).booleanValue();
    }

    /**
     * Lazy flag.
     */
    protected boolean lazy = false;

    /**
     * Lazy flag.
     */
    public void setLazy(String in) {
        lazy = (new Boolean(getProject().replaceProperties(in))).booleanValue();
    }

    public void execute() {
        if(null == srcDir) {
            throw new BuildException("no srcdir specified");
        } else if(!srcDir.exists()) {
            throw new BuildException("srcdir '" + srcDir + "' doesn't exist");
        }
        File gexposeScript = new File(srcDir.getAbsolutePath() + File.separator
                + "gexpose.py");
        File gderiveScript = new File(srcDir.getAbsolutePath() + File.separator
                + "gderived.py");
        if(!gexposeScript.exists()) {
            throw new BuildException("no gexpose.py script found at: "
                    + gexposeScript);
        }
        if(!gderiveScript.exists()) {
            throw new BuildException("no gderive.py script found at: "
                    + gderiveScript);
        }
        runPythonScript(gexposeScript.getAbsolutePath());
        runPythonScript(gderiveScript.getAbsolutePath());
    }

    private void runPythonScript(String script) throws BuildException {
        if(null == python) {
            python = "python";
        }
        Execute e = new Execute();
        e.setWorkingDirectory(srcDir);
        String[] command;
        if(lazy) {
            command = new String[] {python, script, "--lazy"};
        } else {
            command = new String[] {python, script};
        }
        e.setCommandline(command);
        if(verbose) {
            String out = "";
            for(int k = 0; k < e.getCommandline().length; k++) {
                out += (e.getCommandline()[k] + " ");
            }
            log("executing: " + out);
        }
        try {
            e.execute();
        } catch(IOException e2) {
            throw new BuildException(e2.toString(), e2);
        }
    }
}
