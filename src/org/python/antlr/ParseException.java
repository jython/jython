package org.python.antlr;

import org.python.antlr.PythonTree;
import org.antlr.runtime.*;

public class ParseException extends RuntimeException {
	public transient IntStream input;
	public int index;
	public Token token;
	public Object node;
	public int c;
	public int line;
	public int charPositionInLine;
	public boolean approximateLineInfo;

    private int offset = -2;

    public ParseException() {
        super();
    }

    public ParseException(String message) {
        super(message);
    }

    public ParseException(String message, PythonTree node) {
        super(message);
    }

    public ParseException(RecognitionException r) {
        super(getErrorMessage(r));
        input = r.input;
        index = r.index;
        token = r.token;
        node = r.node;
        c = r.c;
        line = r.line;
        charPositionInLine = r.charPositionInLine;
        approximateLineInfo = r.approximateLineInfo;
    }

    /**
     * getErrorMessage is a modified version of org.antlr.runtime.BaseRecognizer's
     * method of the same name from * antlr-3.1 beta1.  When we upgrade we should
     * make sure to remain consistent.
     */
    private static String getErrorMessage(RecognitionException e) {
        String msg = e.getMessage();
        String tokenNames[] = PythonParser.tokenNames;
        if ( e instanceof UnwantedTokenException ) {
            UnwantedTokenException ute = (UnwantedTokenException)e;
            String tokenName="<unknown>";
            if ( ute.expecting== Token.EOF ) {
                tokenName = "EOF";
            }
            else {
                tokenName = tokenNames[ute.expecting];
            }
            msg = "extraneous input "+getTokenErrorDisplay(ute.getUnexpectedToken())+
                " expecting "+tokenName;
        }
        else if ( e instanceof MissingTokenException ) {
            MissingTokenException mte = (MissingTokenException)e;
            String tokenName="<unknown>";
            if ( mte.expecting== Token.EOF ) {
                tokenName = "EOF";
            }
            else {
                tokenName = tokenNames[mte.expecting];
            }
            msg = "missing "+tokenName+" at "+getTokenErrorDisplay(e.token);
        }
        if ( e instanceof MismatchedTokenException ) {
            MismatchedTokenException mte = (MismatchedTokenException)e;
            String tokenName="<unknown>";
            if ( mte.expecting== Token.EOF ) {
                tokenName = "EOF";
            }
            else {
                tokenName = tokenNames[mte.expecting];
            }
            msg = "mismatched input "+getTokenErrorDisplay(e.token)+
                " expecting "+tokenName;
        }
        else if ( e instanceof MismatchedTreeNodeException ) {
            MismatchedTreeNodeException mtne = (MismatchedTreeNodeException)e;
            String tokenName="<unknown>";
            if ( mtne.expecting==Token.EOF ) {
                tokenName = "EOF";
            }
            else {
                tokenName = tokenNames[mtne.expecting];
            }
            msg = "mismatched tree node: "+mtne.node+
                " expecting "+tokenName;
        }
        else if ( e instanceof NoViableAltException ) {
            NoViableAltException nvae = (NoViableAltException)e;
            // for development, can add "decision=<<"+nvae.grammarDecisionDescription+">>"
            // and "(decision="+nvae.decisionNumber+") and
            // "state "+nvae.stateNumber
            msg = "no viable alternative at input "+getTokenErrorDisplay(e.token);
        }
        else if ( e instanceof EarlyExitException ) {
            EarlyExitException eee = (EarlyExitException)e;
            // for development, can add "(decision="+eee.decisionNumber+")"
            msg = "required (...)+ loop did not match anything at input "+
                getTokenErrorDisplay(e.token);
        }
        else if ( e instanceof MismatchedSetException ) {
            MismatchedSetException mse = (MismatchedSetException)e;
            msg = "mismatched input "+getTokenErrorDisplay(e.token)+
                " expecting set "+mse.expecting;
        }
        else if ( e instanceof MismatchedNotSetException ) {
            MismatchedNotSetException mse = (MismatchedNotSetException)e;
            msg = "mismatched input "+getTokenErrorDisplay(e.token)+
                " expecting set "+mse.expecting;
        }
        else if ( e instanceof FailedPredicateException ) {
            FailedPredicateException fpe = (FailedPredicateException)e;
            msg = "rule "+fpe.ruleName+" failed predicate: {"+
                fpe.predicateText+"}?";
        }
        return msg;
    }

    private static String getTokenErrorDisplay(Token t) {
        if (t == null) {
            return "";
        } else {
            return t.getText();
        }
    }

    public int getOffset() {
        //!= -2 is cached result
        if (offset != -2) {
            return offset;
        }
        if (input != null) {
            if (input instanceof CharStream) {
                System.out.println("cs");
                offset = c;
            } else {
                System.out.println("token");
                offset = ((CommonToken)token).getStartIndex();
            }
        } else {
            System.out.println("wth?");
            offset = -1;
        }
        return offset;
    }
}
