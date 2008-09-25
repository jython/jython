// Copyright (c) Corporation for National Research Initiatives

package org.python.compiler;

import java.io.IOException;
import java.util.Hashtable;
import java.util.Stack;
import java.util.Vector;

import org.python.objectweb.asm.ClassWriter;
import org.python.objectweb.asm.Label;
import org.python.objectweb.asm.Opcodes;
import org.python.objectweb.asm.Type;
import org.python.objectweb.asm.commons.Method;
import org.python.core.CompilerFlags;
import org.python.core.PyComplex;
import org.python.core.PyFloat;
import org.python.core.PyInteger;
import org.python.core.PyLong;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.core.PyUnicode;
import org.python.antlr.ParseException;
import org.python.antlr.PythonTree;
import org.python.antlr.Visitor;
import org.python.antlr.ast.Assert;
import org.python.antlr.ast.Assign;
import org.python.antlr.ast.Attribute;
import org.python.antlr.ast.AugAssign;
import org.python.antlr.ast.BinOp;
import org.python.antlr.ast.BoolOp;
import org.python.antlr.ast.Break;
import org.python.antlr.ast.Call;
import org.python.antlr.ast.ClassDef;
import org.python.antlr.ast.Compare;
import org.python.antlr.ast.Continue;
import org.python.antlr.ast.Delete;
import org.python.antlr.ast.Dict;
import org.python.antlr.ast.Ellipsis;
import org.python.antlr.ast.Exec;
import org.python.antlr.ast.Expr;
import org.python.antlr.ast.Expression;
import org.python.antlr.ast.ExtSlice;
import org.python.antlr.ast.For;
import org.python.antlr.ast.FunctionDef;
import org.python.antlr.ast.GeneratorExp;
import org.python.antlr.ast.Global;
import org.python.antlr.ast.If;
import org.python.antlr.ast.IfExp;
import org.python.antlr.ast.Import;
import org.python.antlr.ast.ImportFrom;
import org.python.antlr.ast.Index;
import org.python.antlr.ast.Interactive;
import org.python.antlr.ast.Lambda;
import org.python.antlr.ast.List;
import org.python.antlr.ast.ListComp;
import org.python.antlr.ast.Name;
import org.python.antlr.ast.Num;
import org.python.antlr.ast.Pass;
import org.python.antlr.ast.Print;
import org.python.antlr.ast.Raise;
import org.python.antlr.ast.Repr;
import org.python.antlr.ast.Return;
import org.python.antlr.ast.Slice;
import org.python.antlr.ast.Str;
import org.python.antlr.ast.Subscript;
import org.python.antlr.ast.Suite;
import org.python.antlr.ast.TryExcept;
import org.python.antlr.ast.TryFinally;
import org.python.antlr.ast.Tuple;
import org.python.antlr.ast.UnaryOp;
import org.python.antlr.ast.While;
import org.python.antlr.ast.With;
import org.python.antlr.ast.Yield;
import org.python.antlr.ast.cmpopType;
import org.python.antlr.ast.comprehensionType;
import org.python.antlr.ast.excepthandlerType;
import org.python.antlr.ast.exprType;
import org.python.antlr.ast.expr_contextType;
import org.python.antlr.ast.keywordType;
import org.python.antlr.ast.modType;
import org.python.antlr.ast.operatorType;
import org.python.antlr.ast.stmtType;

public class CodeCompiler extends Visitor implements Opcodes, ClassConstants //, PythonGrammarTreeConstants
{

    public static final Object Exit=new Integer(1);
    public static final Object NoExit=null;

    public static final int GET=0;
    public static final int SET=1;
    public static final int DEL=2;
    public static final int AUGGET=3;
    public static final int AUGSET=4;

    public Module module;
    public ClassWriter cw;
    public Code code;
    public CodeCompiler mrefs;
    public CompilerFlags cflags;

    int temporary;
    expr_contextType augmode;
    int augtmp1;
    int augtmp2;
    int augtmp3;
    int augtmp4;

    public boolean fast_locals, print_results;

    public Hashtable tbl;
    public ScopeInfo my_scope;

    boolean optimizeGlobals = true;
    public Vector names;
    public String className;

    public Stack continueLabels, breakLabels;
    public Stack exceptionHandlers;
    public Vector yields = new Vector();

    /* break/continue finally's level.
     * This is the lowest level in the exceptionHandlers which should
     * be executed at break or continue.
     * It is saved/updated/restored when compiling loops.
     * A similar level for returns is not needed because a new CodeCompiler
     * is used for each PyCode, ie. each 'function'.
     * When returning through finally's all the exceptionHandlers are executed.
     */
    public int bcfLevel = 0;

    int yield_count = 0;
    int with_count = 0;

    public CodeCompiler(Module module, boolean print_results) {
        this.module = module;
        this.print_results = print_results;

        mrefs = this;
        cw = module.classfile.cw;

        continueLabels = new Stack();
        breakLabels = new Stack();
        exceptionHandlers = new Stack();
    }

    public void getNone() throws IOException {
        code.getstatic("org/python/core/Py", "None", $pyObj);
    }

    public void loadFrame() throws Exception {
        code.aload(1);
    }

    public void setLastI(int idx) throws Exception {
        loadFrame();
        code.iconst(idx);
        code.putfield("org/python/core/PyFrame", "f_lasti", "I");
    }
    
    private void loadf_back() throws Exception {
        code.getfield("org/python/core/PyFrame", "f_back", $pyFrame);
    }

    public int storeTop() throws Exception {
        int tmp = code.getLocal("org/python/core/PyObject");
        code.astore(tmp);
        return tmp;
    }

    public void setline(int line) throws Exception {
        if (module.linenumbers) {
            //FJW code.setline(line);
            loadFrame();
            code.iconst(line);
            code.invokevirtual("org/python/core/PyFrame", "setline", "(I)V");
        }
    }

    public void setline(PythonTree node) throws Exception {
        setline(node.getLine());
    }

    public void set(PythonTree node) throws Exception {
        int tmp = storeTop();
        set(node, tmp);
        code.aconst_null();
        code.astore(tmp);
        code.freeLocal(tmp);
    }


    public void set(PythonTree node, int tmp) throws Exception {
        temporary = tmp;
        visit(node);
    }


    private void saveAugTmps(PythonTree node, int count) throws Exception {
        if (count >= 4) {
            augtmp4 = code.getLocal("org/python/core/PyObject");
            code.astore(augtmp4);
        }
        if (count >= 3) {
            augtmp3 = code.getLocal("org/python/core/PyObject");
            code.astore(augtmp3);
        }
        if (count >= 2) {
            augtmp2 = code.getLocal("org/python/core/PyObject");
            code.astore(augtmp2);
        }
        augtmp1 = code.getLocal("org/python/core/PyObject");
        code.astore(augtmp1);

        code.aload(augtmp1);
        if (count >= 2)
            code.aload(augtmp2);
        if (count >= 3)
            code.aload(augtmp3);
        if (count >= 4)
            code.aload(augtmp4);
    }


    private void restoreAugTmps(PythonTree node, int count) throws Exception {
       code.aload(augtmp1);
       code.freeLocal(augtmp1);
       if (count == 1)
           return;
       code.aload(augtmp2);
       code.freeLocal(augtmp2);
       if (count == 2)
           return;
       code.aload(augtmp3);
       code.freeLocal(augtmp3);
       if (count == 3)
           return;
       code.aload(augtmp4);
       code.freeLocal(augtmp4);
   }


    public void parse(modType node, Code code,
                      boolean fast_locals, String className,
                      boolean classBody, ScopeInfo scope, CompilerFlags cflags)
        throws Exception
    {
        this.fast_locals = fast_locals;
        this.className = className;
        this.code = code;
        this.cflags = cflags;

        my_scope = scope;
        names = scope.names;

        tbl = scope.tbl;
        optimizeGlobals = fast_locals&&!scope.exec&&!scope.from_import_star;
        
        if (scope.max_with_count > 0) {
            // allocate for all the with-exits we will have in the frame;
            // this allows yield and with to happily co-exist
            loadFrame();
            code.iconst(scope.max_with_count);
            code.anewarray("org/python/core/PyObject");
            code.putfield("org/python/core/PyFrame", "f_exits", $pyObjArr);
        }

        Object exit = visit(node);

        if (classBody) {
            loadFrame();
            code.invokevirtual("org/python/core/PyFrame", "getf_locals", "()" + $pyObj);
            code.areturn();
        } else {
            if (exit == null) {
                setLastI(-1);

                getNone();
                code.areturn();
            }
        }
    }

    @Override
    public Object visitInteractive(Interactive node) throws Exception {
        traverse(node);
        return null;
    }

    @Override
    public Object visitModule(org.python.antlr.ast.Module suite)
        throws Exception
    {
        if (suite.body.length > 0 &&
            suite.body[0] instanceof Expr &&
            ((Expr)suite.body[0]).value instanceof Str)
        {
            loadFrame();
            code.ldc("__doc__");
            visit(((Expr) suite.body[0]).value);
            code.invokevirtual("org/python/core/PyFrame", "setglobal", "(" +$str + $pyObj + ")V");
        }
        if (module.setFile) {
            loadFrame();
            code.ldc("__file__");
            module.filename.get(code);
            code.invokevirtual("org/python/core/PyFrame", "setglobal", "(" +$str + $pyObj + ")V");
        }
        traverse(suite);
        return null;
    }

    @Override
    public Object visitExpression(Expression node) throws Exception {
        if (my_scope.generator && node.body != null) {
            module.error("'return' with argument inside generator",
                         true, node);
        }
        return visitReturn(new Return(node, node.body), true);
    }

    public void makeArray(PythonTree[] nodes) throws Exception {
        int n;

        if (nodes == null)
            n = 0;
        else
            n = nodes.length;

        if (n == 0) {
            code.getstatic("org/python/core/Py", "EmptyObjects", $pyObjArr);
        } else {
            int tmp = code.getLocal("[Lorg/python/core/PyObject;");
            code.iconst(n);
            code.anewarray("org/python/core/PyObject");
            code.astore(tmp);

            for(int i=0; i<n; i++) {
                visit(nodes[i]);
                code.aload(tmp);
                code.swap();
                code.iconst(i);
                code.swap();
                code.aastore();
            }
            code.aload(tmp);
            code.freeLocal(tmp);
        }
    }

    public void getDocString(stmtType[] suite) throws Exception {
        if (suite.length > 0 && suite[0] instanceof Expr &&
            ((Expr) suite[0]).value instanceof Str)
        {
            visit(((Expr) suite[0]).value);
        } else {
            code.aconst_null();
        }
    }

    public boolean makeClosure(ScopeInfo scope) throws Exception {
        if (scope == null || scope.freevars == null) return false;
        int n = scope.freevars.size();
        if (n == 0) return false;

        int tmp = code.getLocal("[org/python/core/PyObject");
        code.iconst(n);
        code.anewarray("org/python/core/PyObject");
        code.astore(tmp);
        Hashtable upTbl = scope.up.tbl;
        for(int i=0; i<n; i++) {
            code.aload(tmp);
            code.iconst(i);
            loadFrame();
            for(int j = 1; j < scope.distance; j++) {
                loadf_back();
            }
            SymInfo symInfo = (SymInfo)upTbl.get(scope.freevars.elementAt(i));
            code.iconst(symInfo.env_index);
            code.invokevirtual("org/python/core/PyFrame", "getclosure", "(I)" + $pyObj);
            code.aastore();
        }

        code.aload(tmp);
        code.freeLocal(tmp);

        return true;
    }

    @Override
    public Object visitFunctionDef(FunctionDef node) throws Exception {
        String name = getName(node.name);

        setline(node);

        code.new_("org/python/core/PyFunction");
        code.dup();
        loadFrame();
        code.getfield("org/python/core/PyFrame", "f_globals", $pyObj);

        ScopeInfo scope = module.getScopeInfo(node);

        makeArray(scope.ac.getDefaults());

        scope.setup_closure();
        scope.dump();
        module.PyCode(new Suite(node, node.body), name, true,
                      className, false, false,
                      node.getLine(), scope, cflags).get(code);

        getDocString(node.body);

        if (!makeClosure(scope)) {
            code.invokespecial("org/python/core/PyFunction", "<init>", "(" + $pyObj + $pyObjArr + $pyCode + $pyObj + ")V");
        } else {
            code.invokespecial( "org/python/core/PyFunction", "<init>", "(" + $pyObj + $pyObjArr + $pyCode + $pyObj + $pyObjArr + ")V");
        }

        set(new Name(node, node.name, expr_contextType.Store));
        doDecorators(node);
        return null;
    }

    private void doDecorators(FunctionDef func) throws Exception {
        if (func.decorators.length > 0) {
            exprType currentExpr = new Name(func, func.name, expr_contextType.Load);
            exprType[] decs = func.decorators;
            for (int i=decs.length - 1;i > -1;i--) {
                currentExpr = new Call(func, decs[i], new exprType[]{currentExpr}, new keywordType[0], null, null);
            }
            visit(currentExpr);
            set(new Name(func, func.name, expr_contextType.Store));
        }
    }

    @Override
    public Object visitExpr(Expr node) throws Exception {
        setline(node);
        visit(node.value);

        if (print_results) {
            code.invokestatic("org/python/core/Py", "printResult", "(" + $pyObj + ")V");
        } else {
            code.pop();
        }
        return null;
    }

    @Override
    public Object visitAssign(Assign node) throws Exception  {
        setline(node);
        visit(node.value);
        if (node.targets.length == 1) {
            set(node.targets[0]);
            return null;
        }
        int tmp = storeTop();
        for (int i=node.targets.length-1; i>=0; i--) {
            set(node.targets[i], tmp);
        }
        code.freeLocal(tmp);
        return null;
    }

    @Override
    public Object visitPrint(Print node) throws Exception {
        setline(node);
        int tmp = -1;

        if (node.dest != null) {
            visit(node.dest);
            tmp = storeTop();
        }
        if (node.values == null || node.values.length == 0) {
            if (node.dest != null) {
                code.aload(tmp);
                code.invokestatic("org/python/core/Py", "printlnv", "(" + $pyObj + ")V");
            } else {
                code.invokestatic("org/python/core/Py", "println", "()V");
            }
        } else {
            for (int i = 0; i < node.values.length; i++) {
                if (node.dest != null) {
                    code.aload(tmp);
                    visit(node.values[i]);
                    if (node.nl && i == node.values.length - 1) {
                        code.invokestatic("org/python/core/Py", "println", "(" + $pyObj + $pyObj + ")V");
                    } else {
                        code.invokestatic("org/python/core/Py", "printComma", "(" + $pyObj + $pyObj + ")V");
                    }
                } else {
                    visit(node.values[i]);
                    if (node.nl && i == node.values.length - 1) {
                        code.invokestatic("org/python/core/Py", "println", "(" + $pyObj + ")V");
                    } else {
                        code.invokestatic("org/python/core/Py", "printComma", "(" + $pyObj + ")V");
                    }

                }
            }
        }
        if (node.dest != null) {
            code.freeLocal(tmp);
        }
        return null;
    }

    @Override
    public Object visitDelete(Delete node) throws Exception {
        setline(node);
        traverse(node);
        return null;
    }

    @Override
    public Object visitPass(Pass node) throws Exception {
        setline(node);
        return null;
    }

    @Override
    public Object visitBreak(Break node) throws Exception {
        //setline(node); Not needed here...
        if (breakLabels.empty()) {
            throw new ParseException("'break' outside loop", node);
        }

        doFinallysDownTo(bcfLevel);

        code.goto_((Label)breakLabels.peek());
        return null;
    }

    @Override
    public Object visitContinue(Continue node) throws Exception {
        //setline(node); Not needed here...
        if (continueLabels.empty()) {
            throw new ParseException("'continue' not properly in loop", node);
        }

        doFinallysDownTo(bcfLevel);

        code.goto_((Label)continueLabels.peek());
        return Exit;
    }

    @Override
    public Object visitYield(Yield node) throws Exception {
        setline(node);
        if (!fast_locals) {
            throw new ParseException("'yield' outside function", node);
        }

        if (yield_count == 0) {
            loadFrame();
            code.invokevirtual("org/python/core/PyFrame", "getGeneratorInput", "()" + $obj);
            code.dup();
            code.instanceof_("org/python/core/PyException");
            Label done = new Label();
            code.ifeq(done);
            code.checkcast("java/lang/Throwable");
            code.athrow();
            code.label(done);
        }

        if (node.value != null) {
            visit(node.value);
        } else {
            getNone();
        }
        setLastI(++yield_count);
        saveLocals();
        code.areturn();

        Label restart = new Label();
        yields.addElement(restart);
        code.label(restart);
        restoreLocals();
        
        loadFrame();
        code.invokevirtual("org/python/core/PyFrame", "getGeneratorInput", "()" + $obj);
        code.dup();
        code.instanceof_("org/python/core/PyException");
        Label done2 = new Label();
        code.ifeq(done2);
        code.checkcast("java/lang/Throwable");
        code.athrow();
        code.label(done2);
        code.checkcast("org/python/core/PyObject");
        
        return null;
    }

    private boolean inFinallyBody() {
        for (int i = 0; i < exceptionHandlers.size(); ++i) {
            ExceptionHandler handler = 
                (ExceptionHandler)exceptionHandlers.elementAt(i);
            if (handler.isFinallyHandler()) {
                return true;
            }
        }
        return false;
    }
 
    private void restoreLocals() throws Exception {
        endExceptionHandlers();

        Vector v = code.getActiveLocals();

        loadFrame();
        code.getfield("org/python/core/PyFrame", "f_savedlocals", "[Ljava/lang/Object;");

        int locals = code.getLocal("[java/lang/Object");
        code.astore(locals);

        for (int i = 0; i < v.size(); i++) {
            String type = (String) v.elementAt(i);
            if (type == null)
                continue;
            code.aload(locals);
            code.iconst(i);
            code.aaload();
            code.checkcast(type);
            code.astore(i);
        }
        code.freeLocal(locals);

        restartExceptionHandlers();
    }

    /**
     *  Close all the open exception handler ranges.  This should be paired
     *  with restartExceptionHandlers to delimit internal code that
     *  shouldn't be handled by user handlers.  This allows us to set 
     *  variables without the verifier thinking we might jump out of our
     *  handling with an exception.
     */
    private void endExceptionHandlers()
    {
        Label end = new Label();
        code.label(end);
        for (int i = 0; i < exceptionHandlers.size(); ++i) {
            ExceptionHandler handler = 
                (ExceptionHandler)exceptionHandlers.elementAt(i);
            handler.exceptionEnds.addElement(end);
        }
    }

    private void restartExceptionHandlers()
    {
        Label start = new Label();
        code.label(start);
        for (int i = 0; i < exceptionHandlers.size(); ++i) {
            ExceptionHandler handler = 
                (ExceptionHandler)exceptionHandlers.elementAt(i);
            handler.exceptionStarts.addElement(start);
        }
    }

    private void saveLocals() throws Exception {
        Vector v = code.getActiveLocals();
        code.iconst(v.size());
        code.anewarray("java/lang/Object");
        int locals = code.getLocal("[java/lang/Object");
        code.astore(locals);

        for (int i = 0; i < v.size(); i++) {
            String type = (String) v.elementAt(i);
            if (type == null)
                continue;
            code.aload(locals);
            code.iconst(i);
            //code.checkcast(code.pool.Class("java/lang/Object"));
            if (i == 2222) {
                code.aconst_null();
            } else
                code.aload(i);
            code.aastore();
        }

        loadFrame();
        code.aload(locals);
        code.putfield("org/python/core/PyFrame", "f_savedlocals", "[Ljava/lang/Object;");
        code.freeLocal(locals);
    }


    @Override
    public Object visitReturn(Return node) throws Exception {
        return visitReturn(node, false);
    }

    public Object visitReturn(Return node, boolean inEval) throws Exception {
        setline(node);
        if (!inEval && !fast_locals) {
            throw new ParseException("'return' outside function", node);
        }
        int tmp = 0;
        if (node.value != null) {
            if (my_scope.generator)
                throw new ParseException("'return' with argument " +
                                         "inside generator", node);
            visit(node.value);
            tmp = code.getReturnLocal();
            code.astore(tmp);
        }
        doFinallysDownTo(0);

        setLastI(-1);

        if (node.value != null) {
            code.aload(tmp);
        } else {
            getNone();
        }
        code.areturn();
        return Exit;
    }

    @Override
    public Object visitRaise(Raise node) throws Exception {
        setline(node);
        traverse(node);
        if (node.type == null) {
            code.invokestatic("org/python/core/Py", "makeException", "()" + $pyExc);
        } else if (node.inst == null) {
            code.invokestatic("org/python/core/Py", "makeException", "(" + $pyObj + ")" + $pyExc);
        } else if (node.tback == null) {
            code.invokestatic("org/python/core/Py", "makeException", "(" + $pyObj + $pyObj + ")" + $pyExc);
        } else {
            code.invokestatic("org/python/core/Py", "makeException", "(" + $pyObj + $pyObj + $pyObj + ")" + $pyExc);
        }
        code.athrow();
        return Exit;
    }

    @Override
    public Object visitImport(Import node) throws Exception {
        setline(node);
        for (int i = 0; i < node.names.length; i++) {
            String asname = null;
            if (node.names[i].asname != null) {
                String name = node.names[i].name;
                asname = node.names[i].asname;
                code.ldc(name);
                loadFrame();
                code.invokestatic("org/python/core/imp", "importOneAs", "(" + $str + $pyFrame + ")" + $pyObj);
            } else {
                String name = node.names[i].name;
                asname = name;
                if (asname.indexOf('.') > 0)
                    asname = asname.substring(0, asname.indexOf('.'));
                code.ldc(name);
                loadFrame();
                code.invokestatic("org/python/core/imp", "importOne", "(" + $str + $pyFrame + ")" + $pyObj);
            }
            set(new Name(node.names[i], asname, expr_contextType.Store));
        }
        return null;
    }


    @Override
    public Object visitImportFrom(ImportFrom node) throws Exception {
        Future.checkFromFuture(node); // future stmt support
        setline(node);
        code.ldc(node.module);
        //Note: parser does not allow node.names.length == 0
        if (node.names.length == 1 && node.names[0].name.equals("*")) {
            if (my_scope.func_level > 0) {
                module.error("import * only allowed at module level", false, node);

                if (my_scope.contains_ns_free_vars) {
                    module.error("import * is not allowed in function '" +
                            my_scope.scope_name +
                            "' because it contains a nested function with free variables",
                            true, node);
                }
            }
            if (my_scope.func_level > 1) {
                module.error("import * is not allowed in function '" +
                        my_scope.scope_name +
                        "' because it is a nested function",
                        true, node);
            }
            
            loadFrame();
            code.invokestatic("org/python/core/imp", "importAll", "(" + $str + $pyFrame + ")V");
        } else {
            String[] fromNames = new String[node.names.length];
            String[] asnames = new String[node.names.length];
            for (int i = 0; i < node.names.length; i++) {
                fromNames[i] = node.names[i].name;
                asnames[i] = node.names[i].asname;
                if (asnames[i] == null)
                    asnames[i] = fromNames[i];
            }
            makeStrings(code, fromNames, fromNames.length);

            loadFrame();
            code.invokestatic("org/python/core/imp", "importFrom", "(" + $str + $strArr + $pyFrame + ")" + $pyObjArr);
            int tmp = storeTop();
            for (int i = 0; i < node.names.length; i++) {
                code.aload(tmp);
                code.iconst(i);
                code.aaload();
                set(new Name(node.names[i], asnames[i], expr_contextType.Store));
            }
            code.freeLocal(tmp);
        }
        return null;
    }

    @Override
    public Object visitGlobal(Global node) throws Exception {
        return null;
    }

    @Override
    public Object visitExec(Exec node) throws Exception {
        setline(node);
        visit(node.body);

        if (node.globals != null) {
            visit(node.globals);
        } else {
            code.aconst_null();
        }

        if (node.locals != null) {
            visit(node.locals);
        } else {
            code.aconst_null();
        }

        //do the real work here
        code.invokestatic("org/python/core/Py", "exec", "(" + $pyObj + $pyObj + $pyObj + ")V");
        return null;
    }

    @Override
    public Object visitAssert(Assert node) throws Exception {
        setline(node);
        Label end_of_assert = new Label();
        
        /* First do an if __debug__: */
        loadFrame();
        emitGetGlobal("__debug__");

        code.invokevirtual("org/python/core/PyObject", "__nonzero__", "()Z");

        code.ifeq(end_of_assert);

        /* Now do the body of the assert. If PyObject.__nonzero__ is true,
           then the assertion succeeded, the message portion should not be
           processed. Otherwise, the message will be processed. */
        visit(node.test);
        code.invokevirtual("org/python/core/PyObject", "__nonzero__", "()Z");

        /* If evaluation is false, then branch to end of method */
        code.ifne(end_of_assert);
        
        /* Push exception type onto stack(Py.AssertionError) */
        code.getstatic("org/python/core/Py", "AssertionError", "Lorg/python/core/PyObject;");

        /* Visit the message part of the assertion, or pass Py.None */
        if( node.msg != null ) {
             visit(node.msg);
        } else {
            getNone(); 
        }
        code.invokestatic("org/python/core/Py", "makeException", "(" + $pyObj + $pyObj + ")" + $pyExc);
        
        /* Raise assertion error. Only executes this logic if assertion
           failed */
        code.athrow();
 
        /* And finally set the label for the end of it all */
        code.label(end_of_assert);

        return null;
    }

    public Object doTest(Label end_of_if, If node, int index)
        throws Exception
    {
        Label end_of_suite = new Label();

        setline(node.test);
        visit(node.test);
        code.invokevirtual("org/python/core/PyObject", "__nonzero__", "()Z");

        code.ifeq(end_of_suite);

        Object exit = suite(node.body);

        if (end_of_if != null && exit == null)
            code.goto_(end_of_if);

        code.label(end_of_suite);

        if (node.orelse != null) {
            return suite(node.orelse) != null ? exit : null;
        } else {
            return null;
        }
    }

    @Override
    public Object visitIf(If node) throws Exception {
        Label end_of_if = null;
        if (node.orelse != null)
            end_of_if = new Label();

        Object exit = doTest(end_of_if, node, 0);
        if (end_of_if != null)
            code.label(end_of_if);
        return exit;
    }

    @Override
    public Object visitIfExp(IfExp node) throws Exception {
        setline(node.test);
        Label end = new Label();
        Label end_of_else = new Label();

        visit(node.test);
        code.invokevirtual("org/python/core/PyObject", "__nonzero__", "()Z");

        code.ifeq(end_of_else);
        visit(node.body);
        code.goto_(end);

        code.label(end_of_else);
        visit(node.orelse);

        code.label(end);

        return null;
    }

    public int beginLoop() {
        continueLabels.push(new Label());
        breakLabels.push(new Label());
        int savebcf = bcfLevel;
        bcfLevel = exceptionHandlers.size();
        return savebcf;
    }

    public void finishLoop(int savebcf) {
        continueLabels.pop();
        breakLabels.pop();
        bcfLevel = savebcf;
    }


    @Override
    public Object visitWhile(While node) throws Exception {
        int savebcf = beginLoop();
        Label continue_loop = (Label)continueLabels.peek();
        Label break_loop = (Label)breakLabels.peek();

        Label start_loop = new Label();

        code.goto_(continue_loop);
        code.label(start_loop);

        //Do suite
        suite(node.body);

        code.label(continue_loop);
        setline(node);

        //Do test
        visit(node.test);
        code.invokevirtual("org/python/core/PyObject", "__nonzero__", "()Z");
        code.ifne(start_loop);

        finishLoop(savebcf);

        if (node.orelse != null) {
            //Do else
            suite(node.orelse);
        }
        code.label(break_loop);

        // Probably need to detect "guaranteed exits"
        return null;
    }

    @Override
    public Object visitFor(For node) throws Exception {
        int savebcf = beginLoop();
        Label continue_loop = (Label)continueLabels.peek();
        Label break_loop = (Label)breakLabels.peek();
        Label start_loop = new Label();
        Label next_loop = new Label();

        int iter_tmp = code.getLocal("org/python/core/PyObject");
        int expr_tmp = code.getLocal("org/python/core/PyObject");

        setline(node);

        //parse the list
        visit(node.iter);

        //set up the loop iterator
        code.invokevirtual("org/python/core/PyObject", "__iter__", "()Lorg/python/core/PyObject;");
        code.astore(iter_tmp);

        //do check at end of loop.  Saves one opcode ;-)
        code.goto_(next_loop);

        code.label(start_loop);
        //set iter variable to current entry in list
        set(node.target, expr_tmp);

        //evaluate for body
        suite(node.body);

        code.label(continue_loop);

        code.label(next_loop);
        setline(node);
        //get the next element from the list
        code.aload(iter_tmp);
        code.invokevirtual("org/python/core/PyObject", "__iternext__", "()" + $pyObj);

        code.astore(expr_tmp);
        code.aload(expr_tmp);
        //if no more elements then fall through
        code.ifnonnull(start_loop);

        finishLoop(savebcf);

        if (node.orelse != null) {
            //Do else clause if provided
            suite(node.orelse);
        }

        code.label(break_loop);

        code.freeLocal(iter_tmp);
        code.freeLocal(expr_tmp);

        // Probably need to detect "guaranteed exits"
        return null;
    }

    public void exceptionTest(int exc, Label end_of_exceptions,
                              TryExcept node, int index)
        throws Exception
    {
        for (int i = 0; i < node.handlers.length; i++) {
            excepthandlerType handler = node.handlers[i];

            //setline(name);
            Label end_of_self = new Label();

            if (handler.type != null) {
                code.aload(exc);
                //get specific exception
                visit(handler.type);
                code.invokestatic("org/python/core/Py", "matchException", "(" + $pyExc + $pyObj + ")Z");
                code.ifeq(end_of_self);
            } else {
                if (i != node.handlers.length-1) {
                    throw new ParseException(
                        "bare except must be last except clause", handler.type);
                }
            }

            if (handler.name != null) {
                code.aload(exc);
                code.getfield("org/python/core/PyException", "value", "Lorg/python/core/PyObject;");
                set(handler.name);
            }

            //do exception body
            suite(handler.body);
            code.goto_(end_of_exceptions);
            code.label(end_of_self);
        }
        code.aload(exc);
        code.athrow();
    }

     
    @Override
    public Object visitTryFinally(TryFinally node) throws Exception
    {
        Label start = new Label();
        Label end = new Label();
        Label handlerStart = new Label();
        Label finallyEnd = new Label();

        Object ret;

        ExceptionHandler inFinally = new ExceptionHandler(node);

        // Do protected suite
        exceptionHandlers.push(inFinally);

        int excLocal = code.getLocal("java/lang/Throwable");
        code.aconst_null();
        code.astore(excLocal);

        code.label(start);
        inFinally.exceptionStarts.addElement(start);

        ret = suite(node.body);

        code.label(end);
        inFinally.exceptionEnds.addElement(end);
        inFinally.bodyDone = true;

        exceptionHandlers.pop();

        if (ret == NoExit) {
            inlineFinally(inFinally);
            code.goto_(finallyEnd);
        }

        // Handle any exceptions that get thrown in suite
        code.label(handlerStart);
        code.astore(excLocal);

        code.aload(excLocal);
        loadFrame();

        code.invokestatic("org/python/core/Py", "addTraceback", "(" + $throwable + $pyFrame + ")V");

        inlineFinally(inFinally);
        code.aload(excLocal);
        code.checkcast("java/lang/Throwable");
        code.athrow();

        code.label(finallyEnd);

        code.freeLocal(excLocal);

        inFinally.addExceptionHandlers(handlerStart);
        // According to any JVM verifiers, this code block might not return
        return null;
    }

    private void inlineFinally(ExceptionHandler handler) throws Exception {
        if (!handler.bodyDone) {
            // end the previous exception block so inlined finally code doesn't
            // get covered by our exception handler.
            Label end = new Label();
            code.label(end);
            handler.exceptionEnds.addElement(end);
            // also exiting the try: portion of this particular finally
         }
        if (handler.isFinallyHandler()) {
            suite(((TryFinally)handler.node).finalbody);
        }
    }
    
    private void reenterProtectedBody(ExceptionHandler handler) throws Exception {
        // restart exception coverage
        Label restart = new Label();
        code.label(restart);
        handler.exceptionStarts.addElement(restart);
    }
 
    /**
     *  Inline the finally handling code for levels down to the levelth parent
     *  (0 means all).  This takes care to avoid having more nested finallys
     *  catch exceptions throw by the parent finally code.  This also pops off
     *  all the handlers above level temporarily.
     */
    private void doFinallysDownTo(int level) throws Exception {
        Stack poppedHandlers = new Stack();
        while (exceptionHandlers.size() > level) {
            ExceptionHandler handler = 
                (ExceptionHandler)exceptionHandlers.pop();
            inlineFinally(handler);
            poppedHandlers.push(handler);
        }
        while (poppedHandlers.size() > 0) {
            ExceptionHandler handler = 
                (ExceptionHandler)poppedHandlers.pop();
            reenterProtectedBody(handler);
            exceptionHandlers.push(handler);
         }
     }
 
    @Override
    public Object visitTryExcept(TryExcept node) throws Exception {
        Label start = new Label();
        Label end = new Label();
        Label handler_start = new Label();
        Label handler_end = new Label();
        ExceptionHandler handler = new ExceptionHandler();

        code.label(start);
        handler.exceptionStarts.addElement(start);
        exceptionHandlers.push(handler);
        //Do suite
        Object exit = suite(node.body);
        exceptionHandlers.pop();
        code.label(end);
        handler.exceptionEnds.addElement(end);

        if (exit == null)
            code.goto_(handler_end);

        code.label(handler_start);

        loadFrame();

        code.invokestatic("org/python/core/Py", "setException", "(" + $throwable + $pyFrame + ")" + $pyExc);

        int exc = code.getFinallyLocal("java/lang/Throwable");
        code.astore(exc);

        if (node.orelse == null) {
            //No else clause to worry about
            exceptionTest(exc, handler_end, node, 1);
            code.label(handler_end);
        } else {
            //Have else clause
            Label else_end = new Label();
            exceptionTest(exc, else_end, node, 1);
            code.label(handler_end);

            //do else clause
            suite(node.orelse);
            code.label(else_end);
        }

        code.freeFinallyLocal(exc);
        handler.addExceptionHandlers(handler_start);
        return null;
    }

    @Override
    public Object visitSuite(Suite node) throws Exception {
        return suite(node.body);
    }

    public Object suite(stmtType[] stmts) throws Exception {
        int n = stmts.length;
        for(int i = 0; i < n; i++) {
            Object exit = visit(stmts[i]);
            if (exit != null)
                return Exit;
        }
        return null;
    }

    @Override
    public Object visitBoolOp(BoolOp node) throws Exception {
        Label end = new Label();
        visit(node.values[0]);
        for (int i = 1; i < node.values.length; i++) {
            code.dup();
            code.invokevirtual("org/python/core/PyObject", "__nonzero__", "()Z");
            switch (node.op) {
            case Or : 
                code.ifne(end);
                break;
            case And : 
                code.ifeq(end);
                break;
            }
            code.pop();
            visit(node.values[i]);
        }
        code.label(end);
        return null;
    }


    @Override
    public Object visitCompare(Compare node) throws Exception {
        int tmp1 = code.getLocal("org/python/core/PyObject");
        int tmp2 = code.getLocal("org/python/core/PyObject");
        Label end = new Label();

        visit(node.left);

        int n = node.ops.length;
        for(int i = 0; i < n - 1; i++) {
            visit(node.comparators[i]);
            code.dup();
            code.astore(tmp1);
            visitCmpop(node.ops[i]);
            code.dup();
            code.astore(tmp2);
            code.invokevirtual("org/python/core/PyObject", "__nonzero__", "()Z");
            code.ifeq(end);
            code.aload(tmp1);
        }

        visit(node.comparators[n-1]);
        visitCmpop(node.ops[n-1]);

        if (n > 1) {
            code.astore(tmp2);
            code.label(end);
            code.aload(tmp2);
        }
        code.freeLocal(tmp1);
        code.freeLocal(tmp2);
        return null;
    }

    public void visitCmpop(cmpopType op) throws Exception {
        String name = null;
        switch (op) {
        case Eq:    name = "_eq"; break;
        case NotEq: name = "_ne"; break;
        case Lt:    name = "_lt"; break;
        case LtE:   name = "_le"; break;
        case Gt:    name = "_gt"; break;
        case GtE:   name = "_ge"; break;
        case Is:    name = "_is"; break;
        case IsNot: name = "_isnot"; break;
        case In:    name = "_in"; break;
        case NotIn: name = "_notin"; break;
        }
        code.invokevirtual("org/python/core/PyObject", name, "(" + $pyObj + ")" + $pyObj);
    }

    @Override
    public Object visitBinOp(BinOp node) throws Exception {
        visit(node.left);
        visit(node.right);
        String name = null;
        switch (node.op) {
        case Add:    name = "_add"; break;
        case Sub: name = "_sub"; break;
        case Mult:    name = "_mul"; break;
        case Div:   name = "_div"; break;
        case Mod:    name = "_mod"; break;
        case Pow:   name = "_pow"; break;
        case LShift:    name = "_lshift"; break;
        case RShift: name = "_rshift"; break;
        case BitOr:    name = "_or"; break;
        case BitXor: name = "_xor"; break;
        case BitAnd: name = "_and"; break;
        case FloorDiv: name = "_floordiv"; break;
        }

        if (node.op == operatorType.Div && module.getFutures().areDivisionOn()) {
            name = "_truediv";
        }
        code.invokevirtual("org/python/core/PyObject", name, "(" + $pyObj + ")" + $pyObj);
        return null;
    }
    
    @Override
    public Object visitUnaryOp(UnaryOp node) throws Exception {
        visit(node.operand);
        String name = null;
        switch (node.op) {
        case Invert:    name = "__invert__"; break;
        case Not: name = "__not__"; break;
        case UAdd:    name = "__pos__"; break;
        case USub:   name = "__neg__"; break;
        }
        code.invokevirtual("org/python/core/PyObject", name, "()" + $pyObj);
        return null;
    }

    @Override
    public Object visitAugAssign(AugAssign node) throws Exception {
        setline(node);

        visit(node.value);
        int tmp = storeTop();

        augmode = expr_contextType.Load;
        visit(node.target);

        code.aload(tmp);
        String name = null;
        switch (node.op) {
        case Add:    name = "_iadd"; break;
        case Sub: name = "_isub"; break;
        case Mult:    name = "_imul"; break;
        case Div:   name = "_idiv"; break;
        case Mod:    name = "_imod"; break;
        case Pow:   name = "_ipow"; break;
        case LShift:    name = "_ilshift"; break;
        case RShift: name = "_irshift"; break;
        case BitOr:    name = "_ior"; break;
        case BitXor: name = "_ixor"; break;
        case BitAnd: name = "_iand"; break;
        case FloorDiv: name = "_ifloordiv"; break;
        }
        if (node.op == operatorType.Div && module.getFutures().areDivisionOn()) {
            name = "_itruediv";
        }
        code.invokevirtual("org/python/core/PyObject", name, "(" + $pyObj + ")" + $pyObj);
        code.freeLocal(tmp);

        temporary = storeTop();
        augmode = expr_contextType.Store;
        visit(node.target);
        code.freeLocal(temporary);

        return null;
    }


    public static void makeStrings(Code c, String[] names, int n)
        throws IOException
    {
        c.iconst(n);
        c.anewarray("java/lang/String");
        int strings = c.getLocal("[java/lang/String");
        c.astore(strings);
        for (int i=0; i<n; i++) {
            c.aload(strings);
            c.iconst(i);
            c.ldc(names[i]);
            c.aastore();
        }
        c.aload(strings);
        c.freeLocal(strings);
    }
    
    public Object invokeNoKeywords(Attribute node, PythonTree[] values)
        throws Exception
    {
        String name = getName(node.attr);
        visit(node.value);
        code.ldc(name);
        code.invokevirtual("org/python/core/PyObject", "__getattr__", "(" + $str + ")" + $pyObj);

        switch (values.length) {
        case 0:
            code.invokevirtual("org/python/core/PyObject", "__call__", "()" + $pyObj);
            break;
        case 1:
            visit(values[0]);
            code.invokevirtual("org/python/core/PyObject", "__call__", "(" + $pyObj + ")" + $pyObj);
            break;
        case 2:
            visit(values[0]);
            visit(values[1]);
            code.invokevirtual("org/python/core/PyObject", "__call__", "(" + $pyObj + $pyObj + ")" + $pyObj);
            break;
        case 3:
            visit(values[0]);
            visit(values[1]);
            visit(values[2]);
            code.invokevirtual("org/python/core/PyObject", "__call__", "(" + $pyObj + $pyObj + $pyObj + ")" + $pyObj);
            break;
        case 4:
            visit(values[0]);
            visit(values[1]);
            visit(values[2]);
            visit(values[3]);
            code.invokevirtual("org/python/core/PyObject", "__call__", "(" + $pyObj + $pyObj + $pyObj + $pyObj + ")" + $pyObj);
            break;
        default:
            makeArray(values);
            code.invokevirtual("org/python/core/PyObject", "__call__", "(" + $pyObjArr + ")" + $pyObj);
            break;
        }
        return null;
    }


    @Override
    public Object visitCall(Call node) throws Exception {
        String[] keys = new String[node.keywords.length];
        exprType[] values = new exprType[node.args.length + keys.length];
        for (int i = 0; i < node.args.length; i++) {
            values[i] = node.args[i];
        }
        for (int i = 0; i < node.keywords.length; i++) {
            keys[i] = node.keywords[i].arg;
            values[node.args.length + i] = node.keywords[i].value;
        }

        if ((node.keywords == null || node.keywords.length == 0)&& node.starargs == null &&
            node.kwargs == null && node.func instanceof Attribute)
        {
            return invokeNoKeywords((Attribute) node.func, values);
        }

        visit(node.func);

        if (node.starargs != null || node.kwargs != null) {
            makeArray(values);
            makeStrings(code, keys, keys.length);
            if (node.starargs == null)
                code.aconst_null();
            else
                visit(node.starargs);
            if (node.kwargs == null)
                code.aconst_null();
            else
                visit(node.kwargs);

            code.invokevirtual("org/python/core/PyObject", "_callextra", "(" + $pyObjArr + $strArr + $pyObj + $pyObj + ")" + $pyObj);
        } else if (keys.length > 0) {
            makeArray(values);
            makeStrings(code, keys, keys.length);
            code.invokevirtual("org/python/core/PyObject", "__call__", "(" + $pyObjArr + $strArr + ")" + $pyObj);
        } else {
            switch (values.length) {
            case 0:
                code.invokevirtual("org/python/core/PyObject", "__call__", "()" + $pyObj);
                break;
            case 1:
                visit(values[0]);
                code.invokevirtual("org/python/core/PyObject", "__call__", "(" + $pyObj + ")" + $pyObj);
                break;
            case 2:
                visit(values[0]);
                visit(values[1]);
                code.invokevirtual("org/python/core/PyObject", "__call__", "(" + $pyObj + $pyObj + ")" + $pyObj);
                break;
            case 3:
                visit(values[0]);
                visit(values[1]);
                visit(values[2]);
                code.invokevirtual("org/python/core/PyObject", "__call__", "(" + $pyObj + $pyObj + $pyObj + ")" + $pyObj);
                break;
            case 4:
                visit(values[0]);
                visit(values[1]);
                visit(values[2]);
                visit(values[3]);
                code.invokevirtual("org/python/core/PyObject", "__call__", "(" + $pyObj + $pyObj + $pyObj + $pyObj + ")" + $pyObj);
                break;
            default:
                makeArray(values);
                code.invokevirtual("org/python/core/PyObject", "__call__", "(" + $pyObjArr + ")" + $pyObj);
                break;
            }
        }
        return null;
    }


    public Object Slice(Subscript node, Slice slice) throws Exception {
        expr_contextType ctx = node.ctx;
        if (ctx == expr_contextType.AugStore && augmode == expr_contextType.Store) {
            restoreAugTmps(node, 4);
            ctx = expr_contextType.Store;
        } else {
            visit(node.value);
            if (slice.lower != null)
                visit(slice.lower);
            else
                code.aconst_null();
            if (slice.upper != null)
                visit(slice.upper);
            else
                code.aconst_null();
            if (slice.step != null)
                visit(slice.step);
            else
                code.aconst_null();

            if (node.ctx == expr_contextType.AugStore && augmode == expr_contextType.Load) {
                saveAugTmps(node, 4);
                ctx = expr_contextType.Load;
            }
        }

        switch (ctx) {
        case Del:
            code.invokevirtual("org/python/core/PyObject", "__delslice__", "(" + $pyObj + $pyObj + $pyObj + ")V");
            return null;
        case Load:
            code.invokevirtual("org/python/core/PyObject", "__getslice__", "(" + $pyObj + $pyObj + $pyObj + ")" + $pyObj);
            return null;
        case Param:
        case Store:
            code.aload(temporary);
            code.invokevirtual("org/python/core/PyObject", "__setslice__", "(" + $pyObj + $pyObj + $pyObj + $pyObj + ")V");
            return null;
        }
        return null;

    }

    @Override
    public Object visitSubscript(Subscript node) throws Exception {
        if (node.slice instanceof Slice) {
            return Slice(node, (Slice) node.slice);
        }

        expr_contextType ctx = node.ctx;
        if (node.ctx == expr_contextType.AugStore && augmode == expr_contextType.Store) {
            restoreAugTmps(node, 2);
            ctx = expr_contextType.Store;
        } else {
            visit(node.value);
            visit(node.slice);

            if (node.ctx == expr_contextType.AugStore && augmode == expr_contextType.Load) {
                saveAugTmps(node, 2);
                ctx = expr_contextType.Load;
            }
        }

        switch (ctx) {
        case Del:
            code.invokevirtual("org/python/core/PyObject", "__delitem__", "(" + $pyObj + ")V");
            return null;
        case Load:
            code.invokevirtual("org/python/core/PyObject", "__getitem__", "(" + $pyObj + ")" + $pyObj);
            return null;
        case Param:
        case Store:
            code.aload(temporary);
            code.invokevirtual("org/python/core/PyObject", "__setitem__", "(" + $pyObj + $pyObj + ")V");
            return null;
        }
        return null;
    }

    @Override
    public Object visitIndex(Index node) throws Exception {
        traverse(node);
        return null;
    }

    @Override
    public Object visitExtSlice(ExtSlice node) throws Exception {
        code.new_("org/python/core/PyTuple");
        code.dup();
        makeArray(node.dims);
        code.invokespecial("org/python/core/PyTuple", "<init>", "(" + $pyObjArr + ")V");
        return null;
    }

    @Override
    public Object visitAttribute(Attribute node) throws Exception {

        expr_contextType ctx = node.ctx;
        if (node.ctx == expr_contextType.AugStore && augmode == expr_contextType.Store) {
            restoreAugTmps(node, 2);
            ctx = expr_contextType.Store;
        } else {
            visit(node.value);
            code.ldc(getName(node.attr));

            if (node.ctx == expr_contextType.AugStore && augmode == expr_contextType.Load) {
                saveAugTmps(node, 2);
                ctx = expr_contextType.Load;
            }
        }

        switch (ctx) {
        case Del:
            code.invokevirtual("org/python/core/PyObject", "__delattr__", "(" + $str + ")V");
            return null;
        case Load:
            code.invokevirtual("org/python/core/PyObject", "__getattr__", "(" + $str + ")" + $pyObj);
            return null;
        case Param:
        case Store:
            code.aload(temporary);
            code.invokevirtual("org/python/core/PyObject", "__setattr__", "(" + $str + $pyObj + ")V");
            return null;
        }
        return null;
    }

    public Object seqSet(exprType[] nodes) throws Exception {
        code.aload(temporary);
        code.iconst(nodes.length);
        code.invokestatic("org/python/core/Py", "unpackSequence", "(" + $pyObj + "I)" + $pyObjArr);

        int tmp = code.getLocal("[org/python/core/PyObject");
        code.astore(tmp);

        for (int i = 0; i < nodes.length; i++) {
            code.aload(tmp);
            code.iconst(i);
            code.aaload();
            set(nodes[i]);
        }
        code.freeLocal(tmp);

        return null;
    }

    public Object seqDel(exprType[] nodes) throws Exception {
        for (int i = 0; i < nodes.length; i++) {
            visit(nodes[i]);
        }
        return null;
    }

    @Override
    public Object visitTuple(Tuple node) throws Exception {
        /* if (mode ==AUGSET)
            throw new ParseException(
                      "augmented assign to tuple not possible", node); */
        if (node.ctx == expr_contextType.Store) return seqSet(node.elts);
        if (node.ctx == expr_contextType.Del) return seqDel(node.elts);

        code.new_("org/python/core/PyTuple");

        code.dup();
        makeArray(node.elts);
        code.invokespecial("org/python/core/PyTuple", "<init>", "(" + $pyObjArr + ")V");
        return null;
    }

    @Override
    public Object visitList(List node) throws Exception {
        if (node.ctx == expr_contextType.Store) return seqSet(node.elts);
        if (node.ctx == expr_contextType.Del) return seqDel(node.elts);

        code.new_("org/python/core/PyList");
        code.dup();
        makeArray(node.elts);
        code.invokespecial("org/python/core/PyList", "<init>", "(" + $pyObjArr + ")V");
        return null;
    }

    @Override
    public Object visitListComp(ListComp node) throws Exception {
        code.new_("org/python/core/PyList");

        code.dup();
        code.invokespecial("org/python/core/PyList", "<init>", "()V");

        code.dup();

        code.ldc("append");

        code.invokevirtual("org/python/core/PyObject", "__getattr__", "(" + $str + ")" + $pyObj);

        String tmp_append ="_[" + node.getLine() + "_" + node.getCharPositionInLine() + "]";

        set(new Name(node, tmp_append, expr_contextType.Store));

        stmtType n = new Expr(node, new Call(node, new Name(node, tmp_append, expr_contextType.Load), 
                                       new exprType[] { node.elt },
                                       new keywordType[0], null, null));

        for (int i = node.generators.length - 1; i >= 0; i--) {
            comprehensionType lc = node.generators[i];
            for (int j = lc.ifs.length - 1; j >= 0; j--) {
                n = new If(lc.ifs[j], lc.ifs[j], new stmtType[] { n }, new stmtType[0]);
            }
            n = new For(lc, lc.target, lc.iter, new stmtType[] { n }, new stmtType[0]);
        }
        visit(n);
        visit(new Delete(n, new exprType[] { new Name(n, tmp_append, expr_contextType.Del) }));

        return null;
    }

    @Override
    public Object visitDict(Dict node) throws Exception {
        code.new_("org/python/core/PyDictionary");

        code.dup();
        PythonTree[] elts = new PythonTree[node.keys.length * 2];
        for (int i = 0; i < node.keys.length; i++) {
            elts[i * 2] = node.keys[i];
            elts[i * 2 + 1] = node.values[i];
        }
        makeArray(elts);
        code.invokespecial("org/python/core/PyDictionary", "<init>", "(" + $pyObjArr + ")V");
        return null;
    }

    @Override
    public Object visitRepr(Repr node) throws Exception {
        visit(node.value);
        code.invokevirtual("org/python/core/PyObject", "__repr__", "()" + $pyStr);
        return null;
    }

    @Override
    public Object visitLambda(Lambda node) throws Exception {
        String name = "<lambda>";

        //Add a return node onto the outside of suite;
        modType retSuite = new Suite(node, new stmtType[] {
            new Return(node, node.body) });

        setline(node);

        ScopeInfo scope = module.getScopeInfo(node);

        makeArray(scope.ac.getDefaults());

        code.new_("org/python/core/PyFunction");
        
        code.dup_x1();
        code.swap();

        loadFrame();
        code.getfield("org/python/core/PyFrame", "f_globals", $pyObj);
        
        code.swap();

        scope.setup_closure();
        scope.dump();
        module.PyCode(retSuite, name, true, className,
                      false, false, node.getLine(), scope, cflags).get(code);

        if (!makeClosure(scope)) {
            code.invokespecial("org/python/core/PyFunction", "<init>", "(" + $pyObj + $pyObjArr + $pyCode + ")V");
        } else {
            code.invokespecial("org/python/core/PyFunction", "<init>", "(" + $pyObj + $pyObjArr + $pyCode + $pyObjArr + ")V");
        }

        return null;
    }


    @Override
    public Object visitEllipsis(Ellipsis node) throws Exception {
        code.getstatic("org/python/core/Py", "Ellipsis", "Lorg/python/core/PyObject;");
        return null;
    }

    @Override
    public Object visitSlice(Slice node) throws Exception {
        code.new_("org/python/core/PySlice");

        code.dup();
        if (node.lower == null) getNone(); else visit(node.lower);
        if (node.upper == null) getNone(); else visit(node.upper);
        if (node.step == null) getNone(); else visit(node.step);
        code.invokespecial("org/python/core/PySlice", "<init>", "(" + $pyObj + $pyObj + $pyObj + ")V");
        return null;
    }

    @Override
    public Object visitClassDef(ClassDef node) throws Exception {
        setline(node);

        //Get class name
        String name = getName(node.name);
        code.ldc(name);

        makeArray(node.bases);

        ScopeInfo scope = module.getScopeInfo(node);

        scope.setup_closure();
        scope.dump();
        //Make code object out of suite
        module.PyCode(new Suite(node, node.body), name, false, name,
                      true, false, node.getLine(), scope, cflags).get(code);

        //Get doc string (if there)
        getDocString(node.body);

        //Make class out of name, bases, and code
        if (!makeClosure(scope)) {
            code.invokestatic("org/python/core/Py", "makeClass", "(" + $str + $pyObjArr + $pyCode + $pyObj + ")" + $pyObj);
        } else {
            code.invokestatic("org/python/core/Py", "makeClass", "(" + $str + $pyObjArr + $pyCode + $pyObj + $pyObjArr + ")" + $pyObj);
        }

        //Assign this new class to the given name
        set(new Name(node, node.name, expr_contextType.Store));
        return null;
    }

    @Override
    public Object visitNum(Num node) throws Exception {
        if (node.n instanceof PyInteger) {
            module.PyInteger(((PyInteger) node.n).getValue()).get(code);
        } else if (node.n instanceof PyLong) {
            module.PyLong(((PyObject)node.n).__str__().toString()).get(code);
        } else if (node.n instanceof PyFloat) {
            module.PyFloat(((PyFloat) node.n).getValue()).get(code);
        } else if (node.n instanceof PyComplex) {
            module.PyComplex(((PyComplex) node.n).imag).get(code);
        }
        return null;
    }

    private String getName(String name) {
        if (className != null && name.startsWith("__") &&
            !name.endsWith("__"))
        {
            //remove leading '_' from classname
            int i = 0;
            while (className.charAt(i) == '_')
                i++;
            return "_"+className.substring(i)+name;
        }
        return name;
    }

    void emitGetGlobal(String name) throws Exception {
        code.ldc(name);
        code.invokevirtual("org/python/core/PyFrame", "getglobal", "(" + $str + ")" + $pyObj);
    }

    @Override
    public Object visitName(Name node) throws Exception {
        String name;
        if (fast_locals)
            name = node.id;
        else
            name = getName(node.id);

        SymInfo syminf = (SymInfo)tbl.get(name);

        expr_contextType ctx = node.ctx;
        if (ctx == expr_contextType.AugStore) {
            ctx = augmode;
        }        

        switch (ctx) {
        case Load:
            loadFrame();
            if (syminf != null) {
                int flags = syminf.flags;
                if ((flags&ScopeInfo.GLOBAL) !=0 || optimizeGlobals&&
                        (flags&(ScopeInfo.BOUND|ScopeInfo.CELL|
                                                        ScopeInfo.FREE))==0) {
                    emitGetGlobal(name);
                    return null;
                }
                if (fast_locals) {
                    if ((flags&ScopeInfo.CELL) != 0) {
                        code.iconst(syminf.env_index);
                        code.invokevirtual("org/python/core/PyFrame", "getderef", "(I)" + $pyObj);

                        return null;
                    }
                    if ((flags&ScopeInfo.BOUND) != 0) {
                        code.iconst(syminf.locals_index);
                        code.invokevirtual("org/python/core/PyFrame", "getlocal", "(I)" + $pyObj);
                        return null;
                    }
                }
                if ((flags&ScopeInfo.FREE) != 0 &&
                            (flags&ScopeInfo.BOUND) == 0) {
                    code.iconst(syminf.env_index);
                    code.invokevirtual("org/python/core/PyFrame", "getderef", "(I)" + $pyObj);

                    return null;
                }
            }
            code.ldc(name);
            code.invokevirtual("org/python/core/PyFrame", "getname", "(" + $str + ")" + $pyObj);
            return null;

        case Param:
        case Store:
            loadFrame();
            if (syminf != null && (syminf.flags&ScopeInfo.GLOBAL) != 0) {
                code.ldc(name);
                code.aload(temporary);
                code.invokevirtual("org/python/core/PyFrame", "setglobal", "(" + $str + $pyObj + ")V");
            } else {
                if (!fast_locals) {
                    code.ldc(name);
                    code.aload(temporary);
                    code.invokevirtual("org/python/core/PyFrame", "setlocal", "(" + $str + $pyObj + ")V");
                } else {
                    if (syminf == null) {
                        System.err.println("internal compiler error: "+node);
                    }
                    if ((syminf.flags&ScopeInfo.CELL) != 0) {
                        code.iconst(syminf.env_index);
                        code.aload(temporary);
                        code.invokevirtual("org/python/core/PyFrame", "setderef", "(I" + $pyObj + ")V");
                    } else {
                        code.iconst(syminf.locals_index);
                        code.aload(temporary);
                        code.invokevirtual("org/python/core/PyFrame", "setlocal", "(I" + $pyObj + ")V");
                    }
                }
            }
            return null;
        case Del: {
            loadFrame();
            if (syminf != null && (syminf.flags&ScopeInfo.GLOBAL) != 0) {
                code.ldc(name);
                code.invokevirtual("org/python/core/PyFrame", "delglobal", "(" + $str + ")V");
            } else {
                if (!fast_locals) {
                    code.ldc(name);
                    code.invokevirtual("org/python/core/PyFrame", "dellocal", "(" + $str + ")V");
                } else {
                    if (syminf == null) {
                        System.err.println("internal compiler error: "+node);
                    }
                    if ((syminf.flags&ScopeInfo.CELL) != 0) {
                        module.error("can not delete variable '"+name+
                              "' referenced in nested scope",true,node);
                    }
                    code.iconst(syminf.locals_index);
                    code.invokevirtual("org/python/core/PyFrame", "dellocal", "(I)V");
                }
            }
            return null; }
        }
        return null;
    }

    @Override
    public Object visitStr(Str node) throws Exception {
        PyString s = (PyString)node.s;
        if (s instanceof PyUnicode) {
            module.PyUnicode(s.asString()).get(code);
        } else {
            module.PyString(s.asString()).get(code);
        }
        return null;
    }

    @Override
    public Object visitGeneratorExp(GeneratorExp node) throws Exception {
        String bound_exp = "_(x)";
        String tmp_append ="_(" + node.getLine() + "_" + node.getCharPositionInLine() + ")";

        setline(node);

        code.new_("org/python/core/PyFunction");
        code.dup();
        loadFrame();
        code.getfield("org/python/core/PyFrame", "f_globals", $pyObj);

        ScopeInfo scope = module.getScopeInfo(node);

        makeArray(new exprType[0]);

        scope.setup_closure();
        scope.dump();

        stmtType n = new Expr(node, new Yield(node, node.elt));

        exprType iter = null;
        for (int i = node.generators.length - 1; i >= 0; i--) {
            comprehensionType comp = node.generators[i];
            for (int j = comp.ifs.length - 1; j >= 0; j--) {
                n = new If(comp.ifs[j], comp.ifs[j], new stmtType[] { n }, new stmtType[0]);
            }
            if (i != 0) {
                n = new For(comp, comp.target, comp.iter, new stmtType[] { n }, new stmtType[0]);
            } else {
                n = new For(comp, comp.target, new Name(node, bound_exp, expr_contextType.Load), new stmtType[] { n }, new stmtType[0]);
                iter = comp.iter;
            }
        }

        module.PyCode(new Suite(node, new stmtType[]{n}), tmp_append, true,
                      className, false, false,
                      node.getLine(), scope, cflags).get(code);

        code.aconst_null();
        if (!makeClosure(scope)) {
            code.invokespecial("org/python/core/PyFunction", "<init>", "(" + $pyObj + $pyObjArr + $pyCode + $pyObj + ")V");
        } else {
            code.invokespecial( "org/python/core/PyFunction", "<init>", "(" + $pyObj + $pyObjArr + $pyCode + $pyObj + $pyObjArr + ")V");
        }

        set(new Name(node, tmp_append, expr_contextType.Store));

        visit(new Name(node, tmp_append, expr_contextType.Load));
        visit(iter);
        code.invokevirtual("org/python/core/PyObject", "__iter__", "()Lorg/python/core/PyObject;");
        code.invokevirtual("org/python/core/PyObject", "__call__", "(" + $pyObj + ")" + $pyObj);

        visit(new Delete(n, new exprType[] { new Name(n, tmp_append, expr_contextType.Del) }));

        return null;
    }

    @Override
    public Object visitWith(With node) throws Exception {
        if (!module.getFutures().withStatementSupported()) {
            throw new ParseException("'with' will become a reserved keyword in Python 2.6", node);
        }

        int my_with_count = with_count;
        with_count++;

        Label label_body_start = new Label();
        Label label_body_end = new Label();
        Label label_catch = new Label();
        Label label_finally = new Label();
        Label label_end = new Label();

        Method getattr = Method.getMethod("org.python.core.PyObject __getattr__ (String)");
        Method call = Method.getMethod("org.python.core.PyObject __call__ ()");
        Method call3 = Method.getMethod("org.python.core.PyObject __call__ (org.python.core.PyObject,org.python.core.PyObject,org.python.core.PyObject)");

        // mgr = (EXPR)
        visit(node.context_expr);
        int mgr_tmp = code.getLocal("org/python/core/PyObject");
        code.astore(mgr_tmp);

        // exit = mgr.__exit__  # Not calling it yet, so storing in the frame    
        loadFrame();
        code.getfield("org/python/core/PyFrame", "f_exits", $pyObjArr);
        code.iconst(my_with_count);
        code.aload(mgr_tmp);
        code.ldc("__exit__");
        code.invokevirtual(Type.getType(PyObject.class).getInternalName(), getattr.getName(), getattr.getDescriptor());
        code.aastore();

        // value = mgr.__enter__()
        code.aload(mgr_tmp);
        code.freeLocal(mgr_tmp);
        code.ldc("__enter__");
        code.invokevirtual(Type.getType(PyObject.class).getInternalName(), getattr.getName(), getattr.getDescriptor());
        code.invokevirtual(Type.getType(PyObject.class).getInternalName(), call.getName(), call.getDescriptor());
        int value_tmp = code.getLocal("org/python/core/PyObject");
        code.astore(value_tmp);

        // exc = True # not necessary, since we don't exec finally if exception
        // try-catch block here
        //code.trycatch(label_body_start, label_body_end, label_catch, "java/lang/Throwable");
        ExceptionHandler handler = new ExceptionHandler();
        handler.exceptionStarts.addElement(label_body_start);
        exceptionHandlers.push(handler);

        // VAR = value  # Only if "as VAR" is present
        code.label(label_body_start);
        if (node.optional_vars != null) {
            set(node.optional_vars, value_tmp);
        }
        code.freeLocal(value_tmp);
        
        // BLOCK
        suite(node.body);
        exceptionHandlers.pop();
        code.goto_(label_finally);
        code.label(label_body_end);
        handler.exceptionEnds.addElement(label_body_end);

        // CATCH
        code.label(label_catch);

        loadFrame();
        code.invokestatic("org/python/core/Py", "setException", "(" + $throwable + $pyFrame + ")" + $pyExc);
        code.pop();

        code.invokestatic("org/python/core/Py", "getThreadState", "()Lorg/python/core/ThreadState;");
        code.getfield("org/python/core/ThreadState", "exception", $pyExc);
        int ts_tmp = storeTop();

        // # The exceptional case is handled here
        // exc = False # implicit
        // if not exit(*sys.exc_info()):
        loadFrame();
        code.getfield("org/python/core/PyFrame", "f_exits", $pyObjArr);
        code.iconst(my_with_count);
        code.aaload();
        code.aload(ts_tmp);
        code.getfield("org/python/core/PyException", "type", $pyObj);
        code.aload(ts_tmp);
        code.getfield("org/python/core/PyException", "value", $pyObj);
        code.aload(ts_tmp);
        code.getfield("org/python/core/PyException", "traceback", "Lorg/python/core/PyTraceback;");
        code.checkcast("org/python/core/PyObject");
        code.invokevirtual(Type.getType(PyObject.class).getInternalName(), call3.getName(), call3.getDescriptor());
        code.invokevirtual("org/python/core/PyObject", "__nonzero__", "()Z");
        code.ifne(label_end);
        //    raise
        // # The exception is swallowed if exit() returns true
        code.invokestatic("org/python/core/Py", "makeException", "()Lorg/python/core/PyException;");
        code.checkcast("java/lang/Throwable");
        code.athrow();
        
        code.freeLocal(ts_tmp);
        
        handler.addExceptionHandlers(label_catch);

        // FINALLY
        // ordinarily with a finally, we need to duplicate the code. that's not the case here
        // # The normal and non-local-goto cases are handled here
        // if exc: # implicit
        //     exit(None, None, None)

        code.label(label_finally);

        loadFrame();
        code.getfield("org/python/core/PyFrame", "f_exits", $pyObjArr);
        code.iconst(my_with_count);
        code.aaload();
        getNone();
        code.dup();
        code.dup();
        code.invokevirtual(Type.getType(PyObject.class).getInternalName(), call3.getName(), call3.getDescriptor());
        code.pop();

        code.label(label_end);
        with_count--;

        return null;
    }
    
    @Override
    protected Object unhandled_node(PythonTree node) throws Exception {
        throw new Exception("Unhandled node " + node);
    }

    /**
     *  Data about a given exception range whether a try:finally: or a
     *  try:except:.  The finally needs to inline the finally block for
     *  each exit of the try: section, so we carry around that data for it.
     *  
     *  Both of these need to stop exception coverage of an area that is either
     *  the inlined fin ally of a parent try:finally: or the reentry block after
     *  a yield.  Thus we keep around a set of exception ranges that the
     *  catch block will eventually handle.
     */
    class ExceptionHandler {
        /**
         *  Each handler gets several exception ranges, this is because inlined
         *  finally exit code shouldn't be covered by the exception handler of
         *  that finally block.  Thus each time we inline the finally code, we
         *  stop one range and then enter a new one.
         *
         *  We also need to stop coverage for the recovery of the locals after
         *  a yield.
         */
        public Vector exceptionStarts = new Vector();
        public Vector exceptionEnds = new Vector();

        public boolean bodyDone = false;

        public PythonTree node = null;

        public ExceptionHandler() {
        }

        public ExceptionHandler(PythonTree n) {
            node = n;
        }

        public boolean isFinallyHandler() {
            return node != null;
        }

        public void addExceptionHandlers(Label handlerStart) throws Exception {
            for (int i = 0; i < exceptionStarts.size(); ++i) {
                Label start = (Label)exceptionStarts.elementAt(i);
                Label end = (Label)exceptionEnds.elementAt(i);
                //FIXME: not at all sure that getOffset() test is correct or necessary.
                if (start.getOffset() != end.getOffset()) {
                    code.trycatch(
                        (Label)exceptionStarts.elementAt(i),
                        (Label)exceptionEnds.elementAt(i),
                        handlerStart,
                        "java/lang/Throwable");
                }
            }
        }
    }
}
