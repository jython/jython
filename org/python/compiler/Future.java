// (C) Copyright 2001 Samuele Pedroni

package org.python.compiler;

import org.python.parser.*;

public class Future extends Object implements PythonGrammarTreeConstants {

    private boolean nested_scopes;
    private boolean division;

    private static final String FUTURE = "__future__";

    private boolean check(SimpleNode cand) throws Exception {
        SimpleNode dotted_name = cand.getChild(0);
        if (dotted_name.getNumChildren() != 1 ||
                !((String)dotted_name.getChild(0).getInfo()).equals(FUTURE))
            return false;
        int n = cand.getNumChildren();
        if (n == 1) {
            throw new ParseException(
                    "future statement does not support import *",cand);
        }
        for (int i = 1; i < n; i++) {
            SimpleNode imp = cand.getChild(i);
            String feature;
            switch(imp.id) {
            default:
            case JJTNAME:
                feature = (String)imp.getInfo();
                break;
            case JJTIMPORT_AS_NAME:
                feature = (String)imp.getChild(0).getInfo();
                break;
            }
            // *known* features
            if (feature.equals("nested_scopes")) {
                nested_scopes = true;
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

    public void preprocessFutures(SimpleNode node,
                                  org.python.core.CompilerFlags cflags)
        throws Exception
    {
        if (cflags != null) {
            nested_scopes = cflags.nested_scopes;
            division = cflags.division;
        }
        if ( node.id != JJTFILE_INPUT && node.id != JJTSINGLE_INPUT)
            return;
        int n = node.getNumChildren();
        if (n == 0) return;

        int beg = 0;
        if (node.id == JJTFILE_INPUT &&
            node.getChild(0).id == JJTEXPR_STMT &&
            node.getChild(0).getChild(0).id == JJTSTRING) beg++;

        for (int i = beg; i < n; i++) {
            SimpleNode stmt = node.getChild(i);
            if (stmt.id != JJTIMPORTFROM) break;
            stmt.from_future_checked = true;
            if (!check(stmt)) break;
        }

        if (cflags != null) {
            cflags.nested_scopes = cflags.nested_scopes ||  nested_scopes;
            cflags.division      = cflags.division      ||  division;
        }
    }


    public static void checkFromFuture(SimpleNode node) throws Exception {
        if (node.from_future_checked) return;
        SimpleNode dotted_name = node.getChild(0);
        if (dotted_name.getNumChildren() == 1 &&
                ((String)dotted_name.getChild(0).getInfo()).equals(FUTURE)) {
            throw new ParseException("from __future__ imports must occur " +
                                     "at the beginning of the file",node);
        }
        node.from_future_checked = true;
    }

    public boolean areNestedScopesOn() {
        return nested_scopes;
    }

    public boolean areDivisionOn() {
        return division;
    }

}
