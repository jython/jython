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
		//System.out.println("setting boundries on '"+t+"' with start: '" + startToken + "' and '" +
        //        stopToken + "'");
        int start = 0;
        int stop = 0;
        int startChar = 0;
        int stopChar = 0;
        if (startToken!=null) {
            start = startToken.getTokenIndex();
            startChar = ((CommonToken)startToken).getStartIndex();
        }
        if (stopToken!=null) {
            stop = stopToken.getTokenIndex();
            stopChar = ((CommonToken)stopToken).getStopIndex() + 1;
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
        return create(((PythonTree) t).getToken());
    }

	public boolean isNil(Object tree) {
		return ((PythonTree)tree).isNil();
	}

	public void addChild(Object t, Object child) {
		if ( t!=null && child!=null ) {
			((PythonTree)t).addChild((PythonTree)child);
		}
	}

	public Object becomeRoot(Object newRoot, Object oldRoot) {
        //System.out.println("becomeroot new "+newRoot.toString()+" old "+oldRoot);
        PythonTree newRootTree = (PythonTree)newRoot;
		PythonTree oldRootTree = (PythonTree)oldRoot;
		if ( oldRoot==null ) {
			return newRoot;
		}
		// handle ^(nil real-node)
		if ( newRootTree.isNil() ) {
            int nc = newRootTree.getChildCount();
            if ( nc==1 ) newRootTree = (PythonTree)newRootTree.getChild(0);
            else if ( nc >1 ) {
				// TODO: make tree run time exceptions hierarchy
				throw new RuntimeException("more than one node as root (TODO: make exception hierarchy)");
			}
        }
		// add oldRoot to newRoot; addChild takes care of case where oldRoot
		// is a flat list (i.e., nil-rooted tree).  All children of oldRoot
		// are added to newRoot.
		newRootTree.addChild(oldRootTree);
		return newRootTree;
	}

	public Object rulePostProcessing(Object root) {
		//System.out.println("rulePostProcessing: "+((PythonTree)root).toStringTree());
		PythonTree r = (PythonTree)root;
		if ( r!=null && r.isNil() ) {
			if ( r.getChildCount()==0 ) {
				r = null;
			}
			else if ( r.getChildCount()==1 ) {
				r = (PythonTree)r.getChild(0);
				// whoever invokes rule will set parent and child index
				r.setParent(null);
				r.setChildIndex(-1);
			}
		}
		return r;
	}

	public Object create(int tokenType, Token fromToken) {
		fromToken = createToken(fromToken);
		//((ClassicToken)fromToken).setType(tokenType);
		fromToken.setType(tokenType);
		PythonTree t = (PythonTree)create(fromToken);
		return t;
	}

	public Object create(int tokenType, Token fromToken, String text) {
		fromToken = createToken(fromToken);
		fromToken.setType(tokenType);
		fromToken.setText(text);
		PythonTree t = (PythonTree)create(fromToken);
		return t;
	}

	public Object create(int tokenType, String text) {
		Token fromToken = createToken(tokenType, text);
		PythonTree t = (PythonTree)create(fromToken);
		return t;
	}

	public int getType(Object t) {
		((PythonTree)t).getType();
		return 0;
	}

	public String getText(Object t) {
		return ((PythonTree)t).getText();
	}

	public Object getChild(Object t, int i) {
		return ((PythonTree)t).getChild(i);
	}

	public void setChild(Object t, int i, Object child) {
		((PythonTree)t).setChild(i, (PythonTree)child);
	}

	public Object deleteChild(Object t, int i) {
		return ((PythonTree)t).deleteChild(i);
	}

	public int getChildCount(Object t) {
		return ((PythonTree)t).getChildCount();
	}

}
