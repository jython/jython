package org.python.antlr;

import org.antlr.runtime.CommonToken;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.Token;
import org.antlr.runtime.TokenStream;
import org.antlr.runtime.tree.CommonTreeAdaptor;

public class PythonTreeAdaptor extends CommonTreeAdaptor {

    public void setTokenBoundaries(Object t, Token startToken, Token stopToken) {
        if (t==null) {
            return;
        }
        int start = 0;
        int stop = 0;
        int startChar = 0;
        int stopChar = 0;
        if (startToken!=null) {
            start = startToken.getTokenIndex();
            //XXX: instanceof is necessary because of the use of ClassicToken
            //     for leading whitespace lexing.
            if (startToken instanceof CommonToken) {
                startChar = ((CommonToken)startToken).getStartIndex();
            } else if (startToken instanceof ImaginaryToken) {
                startChar = ((ImaginaryToken)startToken).getStartIndex();
            }
        }
        if (stopToken!=null) {
            stop = stopToken.getTokenIndex();
            //XXX: instanceof is necessary because of the use of ClassicToken
            //     for leading whitespace lexing.
            if (stopToken instanceof CommonToken) {
                stopChar = ((CommonToken)stopToken).getStopIndex() + 1;
            } else if (stopToken instanceof ImaginaryToken) {
                stopChar = ((ImaginaryToken)stopToken).getStopIndex() + 1;
            }
        }
        PythonTree pt = (PythonTree)t;
        pt.setTokenStartIndex(start);
        pt.setTokenStopIndex(stop);
        pt.setCharStartIndex(startChar);
        pt.setCharStopIndex(stopChar);
    }

    public Object create(Token token) {
        return new PythonTree(token);
    }

	public Object errorNode(TokenStream input, Token start, Token stop,
							RecognitionException e)
	{
		PythonErrorNode t = new PythonErrorNode(input, start, stop, e);
		//System.out.println("returning error node '"+t+"' @index="+input.index());
		return t;
	}

    public Object dupNode(Object t) {
        if (t == null) {
            return null;
        }
        return create(((PythonTree) t).token);
    }
}
