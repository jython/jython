// (C) Copyright 2001 Samuele Pedroni

package org.python.compiler;

import org.python.antlr.*;
import org.python.antlr.ast.*;
import java.util.*;

public class ScopesCompiler extends Visitor implements ScopeConstants {

    private CompilationContext code_compiler;

    private Stack scopes;
    private ScopeInfo cur = null;
    private Hashtable nodeScopes;

    private int level      = 0;
    private int func_level = 0;

    public ScopesCompiler(CompilationContext code_compiler,
                          Hashtable nodeScopes) {
        this.code_compiler = code_compiler;
        this.nodeScopes = nodeScopes;
        scopes = new Stack();
    }

    public void beginScope(String name, int kind, PythonTree node,
                           ArgListCompiler ac) {
        if (cur != null) {
            scopes.push(cur);
        }
        if (kind == FUNCSCOPE) {
            func_level++;
        }
        cur = new ScopeInfo(name, node, level++, kind, func_level, ac);
        nodeScopes.put(node, cur);
    }

    public void endScope() throws Exception {
        if (cur.kind == FUNCSCOPE) {
            func_level--;
        }
        level--;
        ScopeInfo up = null;
        if (!scopes.empty()) {
            up = (ScopeInfo)scopes.pop();
        }
        //Go into the stack to find a non class containing scope to use making the closure
        //See PEP 227
        int dist = 1;
        ScopeInfo referenceable = up;
        for(int i = scopes.size() - 1; i >= 0 && referenceable.kind == CLASSSCOPE; i--, dist++){
            referenceable = ((ScopeInfo)scopes.get(i));
        }
        cur.cook(referenceable, dist, code_compiler);
        cur.dump(); // debug
        cur = up;
    }

    public void parse(PythonTree node) throws Exception {
        try {
            visit(node);
        } catch(Throwable t) {
            throw org.python.core.ParserFacade.fixParseError(null, t,
                    code_compiler.getFilename());
        }
    }

    @Override
    public Object visitInteractive(Interactive node) throws Exception {
        beginScope("<single-top>", TOPSCOPE, node, null);
        suite(node.body);
        endScope();
        return null;
    }

    @Override
    public Object visitModule(org.python.antlr.ast.Module node)
        throws Exception
    {
        beginScope("<file-top>", TOPSCOPE, node, null);
        suite(node.body);
        endScope();
        return null;
    }

    @Override
    public Object visitExpression(Expression node) throws Exception {
        beginScope("<eval-top>", TOPSCOPE, node, null);
        visit(new Return(node, node.body));
        endScope();
        return null;
    }

    private void def(String name) {
        cur.addBound(name);
    }

    @Override
    public Object visitFunctionDef(FunctionDef node) throws Exception {
        def(node.name);
        ArgListCompiler ac = new ArgListCompiler();
        ac.visitArgs(node.args);

        exprType[] defaults = ac.getDefaults();
        for (int i = 0; i < defaults.length; i++) {
            visit(defaults[i]);
        }

        beginScope(node.name, FUNCSCOPE, node, ac);
        int n = ac.names.size();
        for (int i = 0; i < n; i++) {
            cur.addParam((String)ac.names.elementAt(i));
        }
        for (int i = 0; i < ac.init_code.size(); i++) {
            visit((stmtType) ac.init_code.elementAt(i));
        }
        cur.markFromParam();
        suite(node.body);
        endScope();
        return null;
    }

    @Override
    public Object visitLambda(Lambda node) throws Exception {
        ArgListCompiler ac = new ArgListCompiler();
        ac.visitArgs(node.args);

        PythonTree[] defaults = ac.getDefaults();
        for (int i = 0; i < defaults.length; i++) {
            visit(defaults[i]);
        }

        beginScope("<lambda>", FUNCSCOPE, node, ac);
        for (Object o : ac.names) {
            cur.addParam((String)o);
        }
        for (Object o : ac.init_code)  {
            visit((stmtType)o);
        }
        cur.markFromParam();
        visit(node.body);
        endScope();
        return null;
    }

    public void suite(stmtType[] stmts) throws Exception {
        for (int i = 0; i < stmts.length; i++)
            visit(stmts[i]);
    }

    @Override
    public Object visitImport(Import node) throws Exception {
        for (int i = 0; i < node.names.length; i++) {
            if (node.names[i].asname != null) {
                cur.addBound(node.names[i].asname);
            } else {
                String name = node.names[i].name;
                if (name.indexOf('.') > 0) {
                    name = name.substring(0, name.indexOf('.'));
                }
                cur.addBound(name);
            }
        }
        return null;
    }

    @Override
    public Object visitImportFrom(ImportFrom node) throws Exception {
        Future.checkFromFuture(node); // future stmt support
        int n = node.names.length;
        if (n == 0) {
            cur.from_import_star = true;
            return null;
        }
        for (int i = 0; i < n; i++) {
            if (node.names[i].asname != null) {
                cur.addBound(node.names[i].asname);
            } else {
                cur.addBound(node.names[i].name);
            }
        }
        return null;
    }

    @Override
    public Object visitGlobal(Global node) throws Exception {
        int n = node.names.length;
        for (int i = 0; i < n; i++) {
            String name = node.names[i];
            int prev = cur.addGlobal(name);
            if (prev >= 0) {
                if ((prev&FROM_PARAM) != 0) {
                    code_compiler.error("name '"+name+"' is local and global",
                                        true,node);
                }
                if ((prev&GLOBAL) != 0) {
                    continue;
                }
                String what;
                if ((prev&BOUND) != 0) {
                    what = "assignment";
                } else {
                    what = "use";
                }
                code_compiler.error("name '"+name+"' declared global after "+
                                    what, false, node);
            }
        }
        return null;
    }

    @Override
    public Object visitExec(Exec node) throws Exception {
        cur.exec = true;
        if (node.globals == null && node.locals == null) {
            cur.unqual_exec = true;
        }
        traverse(node);
        return null;
    }

    @Override
    public Object visitClassDef(ClassDef node) throws Exception {
        def(node.name);
        int n = node.bases.length;
        for (int i = 0; i < n; i++) {
            visit(node.bases[i]);
        }
        beginScope(node.name, CLASSSCOPE, node, null);
        suite(node.body);
        endScope();
        return null;
    }

    @Override
    public Object visitName(Name node) throws Exception {
        String name = node.id;
        if (node.ctx != expr_contextType.Load) {
            if (name.equals("__debug__")) {
                code_compiler.error("can not assign to __debug__", true,node);
            }
            cur.addBound(name);
        } else {
            cur.addUsed(name);
        }
        return null;
    }

    @Override
    public Object visitListComp(ListComp node) throws Exception {
        String tmp ="_[" + node.getLine() + "_" + node.getCharPositionInLine() + "]";
        cur.addBound(tmp);
        traverse(node);
        return null;
    }

    @Override
    public Object visitYield(Yield node) throws Exception {
        cur.generator = true;
        cur.yield_count++;
        traverse(node);
        return null;
    }

    @Override
    public Object visitGeneratorExp(GeneratorExp node) throws Exception {
        String bound_exp = "_(x)";
        String tmp ="_(" + node.getLine() + "_" + node.getCharPositionInLine() + ")";
        def(tmp);
        ArgListCompiler ac = new ArgListCompiler();
        ac.visitArgs(new argumentsType(node, new exprType[]{new Name(node.token, bound_exp,
                        expr_contextType.Param)}, null, null, new exprType[0]));
        beginScope(tmp, FUNCSCOPE, node, ac);
        cur.addParam(bound_exp);
        cur.markFromParam();

        cur.generator = true;
        cur.yield_count++;
        traverse(node);

        endScope();
        return null;
    }

    @Override
    public Object visitWith(With node) throws Exception {                
        cur.max_with_count++;
        traverse(node);
        
        return null;
    }
    
}
