
package org.python.compiler;

import org.python.parser.SimpleNode;

public interface CompilationContext {

    public Future getFutures();
    public void error(String msg,boolean err,SimpleNode node) throws Exception;

    public String getFilename();

}
