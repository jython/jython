// (C) Copyright 2001 Samuele Pedroni

package org.python.compiler;

import org.python.parser.*;
import org.python.parser.ast.*;
import org.python.parser.ast.Module;

public class Future extends Object implements PythonGrammarTreeConstants {

    private boolean division;

    private static final String FUTURE = "__future__";

    private boolean check(ImportFrom cand) throws Exception {
        if (!cand.module.equals(FUTURE))
            return false;
        int n = cand.names.length;
        if (n == 0) {
            throw new ParseException(
                    "future statement does not support import *",cand);
        }
        for (int i = 0; i < n; i++) {
            String feature = cand.names[i].name;
            // *known* features
            if (feature.equals("nested_scopes")) {
                continue;
            }
            if (feature.equals("division")) {
                division = true;
                continue;
            }
            throw new ParseException("future feature "+feature+
                                     " is not defined",cand);
        }
        return true;
    }

    public void preprocessFutures(modType node,
                                  org.python.core.CompilerFlags cflags)
        throws Exception
    {
        if (cflags != null) {
            division = cflags.division;
        }
        int beg = 0;
        stmtType[] suite = null;
        if (node instanceof Module) {
            suite = ((Module) node).body;
            if (suite.length > 0 && suite[0] instanceof Expr &&
                            ((Expr) suite[0]).value instanceof Str) {
                beg++;
            }
        } else if (node instanceof Interactive) {
            suite = new stmtType[] { ((Interactive) node).body };
        } else {
            return;
        }

        for (int i = beg; i < suite.length; i++) {
            stmtType stmt = suite[i];
            if (!(stmt instanceof ImportFrom))
                break;
            stmt.from_future_checked = true;
            if (!check((ImportFrom) stmt))
                break;
        }

        if (cflags != null) {
            cflags.division      = cflags.division      ||  division;
        }
    }


    public static void checkFromFuture(ImportFrom node) throws Exception {
        if (node.from_future_checked)
            return;
        if (node.module.equals(FUTURE)) {
            throw new ParseException("from __future__ imports must occur " +
                                     "at the beginning of the file",node);
        }
        node.from_future_checked = true;
    }

    public boolean areDivisionOn() {
        return division;
    }

}
