package org.python.antlr.adapter;

import org.python.antlr.ast.stmtType;

import java.util.ArrayList;
import java.util.List;

public class StmtListAdapter implements AstObjectAdapter {
    public Object adaptList(Object list) {
        List<stmtType> s = new ArrayList<stmtType>();
        if (list instanceof List) {
            for (Object o : (List)list) {
                s.add(AstAdapters.to_stmt(o));
            }
        }
        return s;
    }

    public Object adapt(Object o) {
        return AstAdapters.to_stmt(o);
    }
}
