// (C) Copyright 2001 Samuele Pedroni

package org.python.compiler;

import org.python.antlr.ParseException;
import org.python.antlr.ast.ImportFrom;
import org.python.antlr.ast.Expr;
import org.python.antlr.ast.Interactive;
import org.python.antlr.ast.Module;
import org.python.antlr.ast.Str;
import org.python.antlr.ast.alias;
import org.python.antlr.base.mod;
import org.python.antlr.base.stmt;
import org.python.core.CodeFlag;
import org.python.core.FutureFeature;
import org.python.core.Pragma;
import org.python.core.PragmaReceiver;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public class Future {
    Set<FutureFeature> featureSet = EnumSet.noneOf(FutureFeature.class);
    private final PragmaReceiver features = new PragmaReceiver() {

        public void add(Pragma pragma) {
            if (pragma instanceof FutureFeature) {
                FutureFeature feature = (FutureFeature) pragma;
                featureSet.add(feature);
            }
        }

    };

    private boolean check(ImportFrom cand) throws Exception {
        if (!cand.getInternalModule().equals(FutureFeature.MODULE_NAME))
            return false;
        if (cand.getInternalNames().isEmpty()) {
            throw new ParseException(
                    "future statement does not support import *", cand);
        }
        try {
            for (alias feature : cand.getInternalNames()) {
                // *known* features
                FutureFeature.addFeature(feature.getInternalName(), features);
            }
        } catch (ParseException pe) {
            throw new ParseException(pe.getMessage(), cand);
        }
        return true;
    }

    public void preprocessFutures(mod node, org.python.core.CompilerFlags cflags)
            throws Exception {
        if (cflags != null) {
            if (cflags.isFlagSet(CodeFlag.CO_FUTURE_DIVISION))
                FutureFeature.division.addTo(features);
            if (cflags.isFlagSet(CodeFlag.CO_FUTURE_WITH_STATEMENT))
                FutureFeature.with_statement.addTo(features);
            if (cflags.isFlagSet(CodeFlag.CO_FUTURE_ABSOLUTE_IMPORT))
                FutureFeature.absolute_import.addTo(features);
        }
        int beg = 0;
        List<stmt> suite = null;
        if (node instanceof Module) {
            suite = ((Module) node).getInternalBody();
            if (suite.size() > 0 && suite.get(0) instanceof Expr
                    && ((Expr) suite.get(0)).getInternalValue() instanceof Str) {
                beg++;
            }
        } else if (node instanceof Interactive) {
            suite = ((Interactive) node).getInternalBody();
        } else {
            return;
        }

        for (int i = beg; i < suite.size(); i++) {
            stmt s = suite.get(i);
            if (!(s instanceof ImportFrom)) break;
            s.from_future_checked = true;
            if (!check((ImportFrom) s)) break;
        }

        if (cflags != null) {
            for (FutureFeature feature : featureSet) {
                feature.setFlag(cflags);
            }
        }
    }

    public static void checkFromFuture(ImportFrom node) throws Exception {
        if (node.from_future_checked) return;
        if (node.getInternalModule().equals(FutureFeature.MODULE_NAME)) {
            throw new ParseException("from __future__ imports must occur "
                    + "at the beginning of the file", node);
        }
        node.from_future_checked = true;
    }

    public boolean areDivisionOn() {
        return featureSet.contains(FutureFeature.division);
    }

    public boolean withStatementSupported() {
        return featureSet.contains(FutureFeature.with_statement);
    }

    public boolean isAbsoluteImportOn() {
        return featureSet.contains(FutureFeature.absolute_import);
    }
}
