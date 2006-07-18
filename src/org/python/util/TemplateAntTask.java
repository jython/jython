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
 * Template is an Ant task for generating new-style object definitions based
 * on template files.  These template files come in two flavors; *.expose and
 * *.derived, both are supported by this task.
 *
 * @author Matt Small - msmall@activegrid.com
 * @version 1.0
 */
public class TemplateAntTask extends MatchingTask {

    /**
     * Specifies the mapping file, for template short name to Python class
     * name.
     */
    protected File mappingFile;

    /**
     * Specifies the mapping file, for template short name to Python class
     * name.
     */
    public void setMappingFile(String aMappingFile) {
        String amp=getProject().replaceProperties(aMappingFile);
        mappingFile=new File(amp);
    }

    /**
     * Specifies the Python interpreter.
     */
    protected String python;

    /**
     * Specifies the Python interpreter.
     */
    public void setPython(String aPE) {
        python=aPE;
    }

    /**
     * Specifies the destination directory; this should usually be the source
     * tree, as the templates modify source files in-place.
     */
    protected File destdir;

    /**
     * Specifies the destination directory; this should usually be the source
     * tree, as the templates modify source files in-place.
     */
    public void setDestdir(String in) {
        destdir=new File(getProject().replaceProperties(in));
    }

    /**
     * Source paths.
     */
    protected Path src;

    public Path createSrc() {
        if (null==src) {
            src=new Path(getProject());
        }
        return src.createPath();
    }

    /**
     * Source paths.
     */
    public void setSrcdir(Path srcDir) {
        if (null==src) {
            src=srcDir;
        } else {
            src.append(srcDir);
        }
    }

    /**
     * Path to find scripts (gexpose.py &amp; gderived.py).
     */
    protected File scriptDir;

    /**
     * Path to find scripts (gexpose.py &amp; gderived.py).
     */
    public void setScriptdir(String in) {
        scriptDir=new File(getProject().replaceProperties(in));
    }

    /**
     * Verbose flag.
     */
    protected boolean verbose=false;

    /**
     * Verbose flag.
     */
    public void setVerbose(String in) {
        verbose=(new Boolean(getProject().replaceProperties(in))).booleanValue();
    }

    public void execute() {
        if (null==mappingFile) {
            throw new BuildException("no mappingFile specified");
        } else if (!mappingFile.canRead()) {
            throw new BuildException("Can't read mappingFile: "+mappingFile);
        }
        if (null==python) {
            // throw new BuildException("no python executable specified");
            python="python";
        }
        if (null==destdir) {
            throw new BuildException("no destdir specified");
        } else if (!destdir.exists()) {
            throw new BuildException("destdir '"+destdir+"' doesn't exist");
        }

        Map mapping;
        try {
            mapping=fileToMap(mappingFile);
        } catch (IOException e) {
            throw new BuildException(e.toString(), e);
        }

        if (null==scriptDir) {
            throw new BuildException("no scriptdir specified");
        } else if (!scriptDir.exists()) {
            throw new BuildException("scriptdir '"+scriptDir+"' doesn't exist");
        }
        File gexposeScript=new File(scriptDir.getAbsolutePath()+File.separator+
                "gexpose.py");
        File gderiveScript=new File(scriptDir.getAbsolutePath()+File.separator+
                "gderived.py");
        if (!gexposeScript.exists()) {
            throw new BuildException("no gexpose.py script found at: "+
                    gexposeScript);
        }
        if (!gderiveScript.exists()) {
            throw new BuildException("no gderive.py script found at: "+
                    gderiveScript);
        }


        String[] list=src.list();
        for (int i=0;i<list.length;i++) {
            File srcDir=getProject().resolveFile(list[i]);
            if (!srcDir.exists()) {
                throw new BuildException("srcDir '"+list[i]+"' does not exist");
            }

            DirectoryScanner ds=getDirectoryScanner(srcDir);
            String[] files=ds.getIncludedFiles();

            for (int j=0;j<files.length;j++) {
                // TODO - use FileNameMapper, SourceFileScanner to only update
                // changed files
                //

                if (null==mapping.get(files[j])) {
                    if (verbose) {
                        log("ignoring file ("+files[j]+") with no mapping");
                    }
                    continue;
                }

                String fromFile=srcDir.getAbsolutePath()+File.separator+
                    files[j];
                String mapFile=(String)mapping.get(files[j]);
                String mapFileName=mapFile.replace(".", File.separator)+".java";
                String toFile=destdir.getAbsolutePath()+File.separator+
                    mapFileName;

                if (! (new File(fromFile)).exists() ) {
                    throw new BuildException("source file '"+fromFile+
                            "' does not exist");
                }
                if (! (new File(toFile)).exists() ) {
                    throw new BuildException("destination file '"+toFile+
                            "' (from class named "+mapFile+") does not exist");
                }

                String[] cmd;
                if (fromFile.endsWith(".expose")) {
                    cmd=new String[] {python, gexposeScript.getAbsolutePath(),
                        fromFile, toFile};
                } else if (fromFile.endsWith(".derived")) {
                    cmd=new String[] {python, gderiveScript.getAbsolutePath(),
                        fromFile, toFile};
                } else {
                    throw new BuildException("source file: '"+fromFile+
                            "' has unknown extension; expected .derived or .expose");
                }

                Execute e=new Execute();
                e.setWorkingDirectory(scriptDir);
                e.setCommandline(cmd);
                if (verbose) {
                    String out="";
                    for (int k=0;k<e.getCommandline().length;k++) {
                        out+=(e.getCommandline()[k]+" ");
                    }
                    log("executing: "+out);
                }

                try {
                    e.execute();
                } catch (IOException e2) {
                    throw new BuildException(e2.toString(), e2);
                }
            }
        }
    }

    protected Map fileToMap (File mapFile) throws IOException, BuildException {
        HashMap ret=new HashMap();
        BufferedReader in=new BufferedReader(new InputStreamReader(
                    new FileInputStream(mapFile)));
        String line;
        int idx;
        String key;
        String val;

        while (null!=(line=in.readLine())) {
            if (line.trim().startsWith("#")) {
                continue;
            } else if (0==line.trim().compareTo("")) {
                continue;
            }

            idx=line.indexOf(":");
            if (-1==idx) {
                throw new BuildException("invalid mapping syntax; no ':' on line: '"+line+"'");
            }
            key=line.substring(0, idx);
            val=line.substring(idx+1, line.length());

            ret.put(key, val);
        }
        
        return ret;
    }
}
