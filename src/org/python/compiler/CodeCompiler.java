// Copyright (c) Corporation for National Research Initiatives

package org.python.compiler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.ListIterator;
import java.util.Map;
import java.util.Stack;
import java.util.Vector;

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
import org.python.antlr.ast.ExceptHandler;
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
import org.python.antlr.ast.alias;
import org.python.antlr.ast.cmpopType;
import org.python.antlr.ast.comprehension;
import org.python.antlr.ast.expr_contextType;
import org.python.antlr.ast.keyword;
import org.python.antlr.ast.operatorType;
import org.python.antlr.base.expr;
import org.python.antlr.base.mod;
import org.python.antlr.base.stmt;
import org.python.core.CompilerFlags;
import org.python.core.ContextGuard;
import org.python.core.ContextManager;
import org.python.core.imp;
import org.python.core.Py;
import org.python.core.PyCode;
import org.python.core.PyComplex;
import org.python.core.PyDictionary;
import org.python.core.PyException;
import org.python.core.PyFloat;
import org.python.core.PyFrame;
import org.python.core.PyFunction;
import org.python.core.PyInteger;
import org.python.core.PyList;
import org.python.core.PyLong;
import org.python.core.PyObject;
import org.python.core.PySlice;
import org.python.core.PyString;
import org.python.core.PyTuple;
import org.python.core.PyUnicode;
import org.python.core.ThreadState;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;
import static org.python.util.CodegenUtils.*;

public class CodeCompiler extends Visitor implements Opcodes, ClassConstants {

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

    public Map<String, SymInfo> tbl;
    public ScopeInfo my_scope;

    boolean optimizeGlobals = true;
    public Vector<String> names;
    public String className;

    public Stack<Label> continueLabels, breakLabels;
    public Stack<ExceptionHandler> exceptionHandlers;
    public Vector<Label> yields = new Vector<Label>();

    /*
     * break/continue finally's level.  This is the lowest level in the
     * exceptionHandlers which should be executed at break or continue.  It is
     * saved/updated/restored when compiling loops.  A similar level for
     * returns is not needed because a new CodeCompiler is used for each
     * PyCode, in other words: each 'function'.  When returning through
     * finally's all the exceptionHandlers are executed.
     */
    public int bcfLevel = 0;

    int yield_count = 0;
    
    private Stack<String> stack = new Stack<String>();

    public CodeCompiler(Module module, boolean print_results) {
        this.module = module;
        this.print_results = print_results;

        mrefs = this;
        cw = module.classfile.cw;

        continueLabels = new Stack<Label>();
        breakLabels = new Stack<Label>();
        exceptionHandlers = new Stack<ExceptionHandler>();
    }

    public void getNone() throws IOException {
        code.getstatic(p(Py.class), "None", ci(PyObject.class));
    }

    public void loadFrame() throws Exception {
        code.aload(1);
    }

    public void loadThreadState() throws Exception {
        code.aload(2);
    }

    public void setLastI(int idx) throws Exception {
        loadFrame();
        code.iconst(idx);
        code.putfield(p(PyFrame.class), "f_lasti", "I");
    }
    
    private void loadf_back() throws Exception {
        code.getfield(p(PyFrame.class), "f_back", ci(PyFrame.class));
    }

    public int storeTop() throws Exception {
        int tmp = code.getLocal(p(PyObject.class));
        code.astore(tmp);
        return tmp;
    }

    public void setline(int line) throws Exception {
        if (module.linenumbers) {
            code.setline(line);
            loadFrame();
            code.iconst(line);
            code.invokevirtual(p(PyFrame.class), "setline", sig(Void.TYPE, Integer.TYPE));
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
            augtmp4 = code.getLocal(ci(PyObject.class));
            code.astore(augtmp4);
        }
        if (count >= 3) {
            augtmp3 = code.getLocal(ci(PyObject.class));
            code.astore(augtmp3);
        }
        if (count >= 2) {
            augtmp2 = code.getLocal(ci(PyObject.class));
            code.astore(augtmp2);
        }
        augtmp1 = code.getLocal(ci(PyObject.class));
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


    public void parse(mod node, Code code,
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
            code.anewarray(p(PyObject.class));
            code.putfield(p(PyFrame.class), "f_exits", ci(PyObject[].class));
        }

        Object exit = visit(node);

        if (classBody) {
            loadFrame();
            code.invokevirtual(p(PyFrame.class), "getf_locals", sig(PyObject.class));
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
        if (suite.getInternalBody().size() > 0 &&
            suite.getInternalBody().get(0) instanceof Expr &&
            ((Expr) suite.getInternalBody().get(0)).getInternalValue() instanceof Str)
        {
            loadFrame();
            code.ldc("__doc__");
            visit(((Expr) suite.getInternalBody().get(0)).getInternalValue());
            code.invokevirtual(p(PyFrame.class), "setglobal", sig(Void.TYPE, String.class,
                        PyObject.class));
        }
        traverse(suite);
        return null;
    }

    @Override
    public Object visitExpression(Expression node) throws Exception {
        if (my_scope.generator && node.getInternalBody() != null) {
            module.error("'return' with argument inside generator",
                         true, node);
        }
        return visitReturn(new Return(node,node.getInternalBody()), true);
    }

    public int makeArray(java.util.List<? extends PythonTree> nodes) throws Exception {
        // XXX: This should produce an array on the stack (if possible) instead of a local
        // the caller is responsible for freeing.
        int n;

        if (nodes == null)
            n = 0;
        else
            n = nodes.size();

        int array = code.getLocal(ci(PyObject[].class));
        if (n == 0) {
            code.getstatic(p(Py.class), "EmptyObjects", ci(PyObject[].class));
            code.astore(array);
        } else {
            code.iconst(n);
            code.anewarray(p(PyObject.class));
            code.astore(array);

            for(int i=0; i<n; i++) {
                visit(nodes.get(i));
                code.aload(array);
                code.swap();
                code.iconst(i);
                code.swap();
                code.aastore();
            }
        }
        return array;
    }

    // nulls out an array of references
    public void freeArray(int array) {
        code.aload(array);
        code.aconst_null();
        code.invokestatic(p(Arrays.class), "fill", sig(Void.TYPE, Object[].class, Object.class));
        code.freeLocal(array);
    }

    public void getDocString(java.util.List<stmt> suite) throws Exception {
        if (suite.size() > 0 && suite.get(0) instanceof Expr &&
            ((Expr) suite.get(0)).getInternalValue() instanceof Str)
        {
            visit(((Expr) suite.get(0)).getInternalValue());
        } else {
            code.aconst_null();
        }
    }

    public boolean makeClosure(ScopeInfo scope) throws Exception {
        if (scope == null || scope.freevars == null) return false;
        int n = scope.freevars.size();
        if (n == 0) return false;

        int tmp = code.getLocal(ci(PyObject[].class));
        code.iconst(n);
        code.anewarray(p(PyObject.class));
        code.astore(tmp);
        Map<String, SymInfo> upTbl = scope.up.tbl;
        for(int i=0; i<n; i++) {
            code.aload(tmp);
            code.iconst(i);
            loadFrame();
            for(int j = 1; j < scope.distance; j++) {
                loadf_back();
            }
            SymInfo symInfo = upTbl.get(scope.freevars.elementAt(i));
            code.iconst(symInfo.env_index);
            code.invokevirtual(p(PyFrame.class), "getclosure", sig(PyObject.class, Integer.TYPE));
            code.aastore();
        }

        code.aload(tmp);
        code.freeLocal(tmp);

        return true;
    }

    @Override
    public Object visitFunctionDef(FunctionDef node) throws Exception {
        String name = getName(node.getInternalName());

        setline(node);

        ScopeInfo scope = module.getScopeInfo(node);

        // NOTE: this is attached to the constructed PyFunction, so it cannot be nulled out
        // with freeArray, unlike other usages of makeArray here
        int defaults = makeArray(scope.ac.getDefaults());

        code.new_(p(PyFunction.class));
        code.dup();
        loadFrame();
        code.getfield(p(PyFrame.class), "f_globals", ci(PyObject.class));
        code.aload(defaults);
        code.freeLocal(defaults);

        scope.setup_closure();
        scope.dump();
        module.codeConstant(new Suite(node,node.getInternalBody()), name, true,
                      className, false, false,
                      node.getLine(), scope, cflags).get(code);

        getDocString(node.getInternalBody());

        if (!makeClosure(scope)) {
            code.invokespecial(p(PyFunction.class), "<init>", sig(Void.TYPE, PyObject.class,
                        PyObject[].class, PyCode.class, PyObject.class));
        } else {
            code.invokespecial(p(PyFunction.class), "<init>", sig(Void.TYPE, PyObject.class,
                        PyObject[].class, PyCode.class, PyObject.class, PyObject[].class));
        }
        
        applyDecorators(node.getInternalDecorator_list());

        set(new Name(node,node.getInternalName(), expr_contextType.Store));
        return null;
    }

    private void applyDecorators(java.util.List<expr> decorators) throws Exception {
        if (decorators != null && !decorators.isEmpty()) {
            int res = storeTop();
            for (expr decorator : decorators) {
                visit(decorator); stackProduce();
            }
            for (int i = decorators.size(); i > 0; i--) {
                stackConsume();
                loadThreadState();
                code.aload(res);
                code.invokevirtual(p(PyObject.class), "__call__", sig(PyObject.class,
                            ThreadState.class, PyObject.class));
                code.astore(res);
            }
            code.aload(res);
            code.freeLocal(res);
        }
    }

    @Override
    public Object visitExpr(Expr node) throws Exception {
        setline(node);
        visit(node.getInternalValue());

        if (print_results) {
            code.invokestatic(p(Py.class), "printResult", sig(Void.TYPE, PyObject.class));
        } else {
            code.pop();
        }
        return null;
    }

    @Override
    public Object visitAssign(Assign node) throws Exception  {
        setline(node);
        visit(node.getInternalValue());
        if (node.getInternalTargets().size() == 1) {
            set(node.getInternalTargets().get(0));
        } else {
            int tmp = storeTop();
            for (expr target : node.getInternalTargets()) {
                set(target, tmp);
            }
            code.freeLocal(tmp);
        }
        return null;
    }

    @Override
    public Object visitPrint(Print node) throws Exception {
        setline(node);
        int tmp = -1;

        if (node.getInternalDest() != null) {
            visit(node.getInternalDest());
            tmp = storeTop();
        }
        if (node.getInternalValues() == null || node.getInternalValues().size() == 0) {
            if (node.getInternalDest() != null) {
                code.aload(tmp);
                code.invokestatic(p(Py.class), "printlnv", sig(Void.TYPE, PyObject.class));
            } else {
                code.invokestatic(p(Py.class), "println", sig(Void.TYPE));
            }
        } else {
            for (int i = 0; i < node.getInternalValues().size(); i++) {
                if (node.getInternalDest() != null) {
                    code.aload(tmp);
                    visit(node.getInternalValues().get(i));
                    if (node.getInternalNl() && i == node.getInternalValues().size() - 1) {
                        code.invokestatic(p(Py.class), "println", sig(Void.TYPE, PyObject.class,
                                    PyObject.class));
                    } else {
                        code.invokestatic(p(Py.class), "printComma",  sig(Void.TYPE, PyObject.class,
                                    PyObject.class));
                    }
                } else {
                    visit(node.getInternalValues().get(i));
                    if (node.getInternalNl() && i == node.getInternalValues().size() - 1) {
                        code.invokestatic(p(Py.class), "println", sig(Void.TYPE, PyObject.class));
                    } else {
                        code.invokestatic(p(Py.class), "printComma", sig(Void.TYPE,
                                    PyObject.class));
                    }

                }
            }
        }
        if (node.getInternalDest() != null) {
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

        code.goto_(breakLabels.peek());
        return null;
    }

    @Override
    public Object visitContinue(Continue node) throws Exception {
        //setline(node); Not needed here...
        if (continueLabels.empty()) {
            throw new ParseException("'continue' not properly in loop", node);
        }

        doFinallysDownTo(bcfLevel);

        code.goto_(continueLabels.peek());
        return Exit;
    }

    @Override
    public Object visitYield(Yield node) throws Exception {
        setline(node);
        if (!fast_locals) {
            throw new ParseException("'yield' outside function", node);
        }

        int stackState = saveStack();
        
        if (node.getInternalValue() != null) {
            visit(node.getInternalValue());
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
        restoreStack(stackState);
        
        loadFrame();
        code.invokevirtual(p(PyFrame.class), "getGeneratorInput", sig(Object.class));
        code.dup();
        code.instanceof_(p(PyException.class));
        Label done2 = new Label();
        code.ifeq(done2);
        code.checkcast(p(Throwable.class));
        code.athrow();
        code.label(done2);
        code.checkcast(p(PyObject.class));
        
        return null;
    }
    
    private void stackProduce() {
        stackProduce(p(PyObject.class));
    }
    
    private void stackProduce(String signature) {
        stack.push(signature);
    }

    private void stackConsume() {
        stackConsume(1);
    }
    
    private void stackConsume(int numItems) {
        for (int i = 0; i < numItems; i++) {
            stack.pop();
        }
    }

    private int saveStack() throws Exception {
        if (stack.size() > 0) {
            int array = code.getLocal(ci(Object[].class));
            code.iconst(stack.size());
            code.anewarray(p(Object.class));
            code.astore(array);
            ListIterator<String> content = stack.listIterator(stack.size());
            for (int i = 0; content.hasPrevious(); i++) {
                String signature = content.previous();
                if (p(ThreadState.class).equals(signature)) {
                    // Stack: ... threadstate
                    code.pop();
                    // Stack: ...
                } else {
                    code.aload(array);
                    // Stack: |- ... value array
                    code.swap();
                    code.iconst(i++);
                    code.swap();
                    // Stack: |- ... array index value
                    code.aastore();
                    // Stack: |- ...
                }
            }
            return array;
        } else {
            return -1;
        }
    }

    private void restoreStack(int array) throws Exception {
        if (stack.size() > 0) {
            int i = stack.size() -1;
            for (String signature : stack) {
                if (p(ThreadState.class).equals(signature)) {
                    loadThreadState();
                } else {
                    code.aload(array);
                    // Stack: |- ... array
                    code.iconst(i--);
                    code.aaload();
                    // Stack: |- ... value
                    code.checkcast(signature);
                }
            }
            code.freeLocal(array);
        }
    }

    private void restoreLocals() throws Exception {
        endExceptionHandlers();
        
        Vector<String> v = code.getActiveLocals();

        loadFrame();
        code.getfield(p(PyFrame.class), "f_savedlocals", ci(Object[].class));

        int locals = code.getLocal(ci(Object[].class));
        code.astore(locals);

        for (int i = 0; i < v.size(); i++) {
            String type = v.elementAt(i);
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
            ExceptionHandler handler = exceptionHandlers.elementAt(i);
            handler.exceptionEnds.addElement(end);
        }
    }

    private void restartExceptionHandlers()
    {
        Label start = new Label();
        code.label(start);
        for (int i = 0; i < exceptionHandlers.size(); ++i) {
            ExceptionHandler handler = exceptionHandlers.elementAt(i);
            handler.exceptionStarts.addElement(start);
        }
    }

    private void saveLocals() throws Exception {
        Vector<String> v = code.getActiveLocals();
        code.iconst(v.size());
        code.anewarray(p(Object.class));
        int locals = code.getLocal(ci(Object[].class));
        code.astore(locals);

        for (int i = 0; i < v.size(); i++) {
            String type = v.elementAt(i);
            if (type == null)
                continue;
            code.aload(locals);
            code.iconst(i);
            //code.checkcast(code.pool.Class(p(Object.class)));
            if (i == 2222) {
                code.aconst_null();
            } else
                code.aload(i);
            code.aastore();
        }

        loadFrame();
        code.aload(locals);
        code.putfield(p(PyFrame.class), "f_savedlocals", ci(Object[].class));
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
        if (node.getInternalValue() != null) {
            if (my_scope.generator)
                throw new ParseException("'return' with argument " +
                                         "inside generator", node);
            visit(node.getInternalValue());
            tmp = code.getReturnLocal();
            code.astore(tmp);
        }
        doFinallysDownTo(0);

        setLastI(-1);

        if (node.getInternalValue() != null) {
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
        if (node.getInternalType() != null) { visit(node.getInternalType()); stackProduce(); }
        if (node.getInternalInst() != null) { visit(node.getInternalInst()); stackProduce(); }
        if (node.getInternalTback() != null) { visit(node.getInternalTback()); stackProduce(); }
        if (node.getInternalType() == null) {
            code.invokestatic(p(Py.class), "makeException", sig(PyException.class));
        } else if (node.getInternalInst() == null) {
            stackConsume();
            code.invokestatic(p(Py.class), "makeException", sig(PyException.class, PyObject.class));
        } else if (node.getInternalTback() == null) {
            stackConsume(2);
            code.invokestatic(p(Py.class), "makeException", sig(PyException.class, PyObject.class,
                        PyObject.class));
        } else {
            stackConsume(3);
            code.invokestatic(p(Py.class), "makeException", sig(PyException.class, PyObject.class,
                        PyObject.class, PyObject.class));
        }
        code.athrow();
        return Exit;
    }

    @Override
    public Object visitImport(Import node) throws Exception {
        setline(node);
        for (alias a : node.getInternalNames()) {
            String asname = null;
            if (a.getInternalAsname() != null) {
                String name = a.getInternalName();
                asname = a.getInternalAsname();
                code.ldc(name);
                loadFrame();
                code.invokestatic(p(imp.class), "importOneAs", sig(PyObject.class, String.class,
                            PyFrame.class));
            } else {
                String name = a.getInternalName();
                asname = name;
                if (asname.indexOf('.') > 0)
                    asname = asname.substring(0, asname.indexOf('.'));
                code.ldc(name);
                loadFrame();
                code.invokestatic(p(imp.class), "importOne", sig(PyObject.class, String.class,
                            PyFrame.class));
            }
            set(new Name(a, asname, expr_contextType.Store));
        }
        return null;
    }


    @Override
    public Object visitImportFrom(ImportFrom node) throws Exception {
        Future.checkFromFuture(node); // future stmt support
        setline(node);
        code.ldc(node.getInternalModule());
        java.util.List<alias> aliases = node.getInternalNames();
        if (aliases == null || aliases.size() == 0) {
            throw new ParseException("Internel parser error", node);
        } else if (aliases.size() == 1 && aliases.get(0).getInternalName().equals("*")) {
            if (node.getInternalLevel() > 0) {
                throw new ParseException("'import *' not allowed with 'from .'", node);
            }
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
            code.invokestatic(p(imp.class), "importAll", sig(Void.TYPE, String.class,
                        PyFrame.class));
        } else {
            java.util.List<String> fromNames = new ArrayList<String>();//[names.size()];
            java.util.List<String> asnames = new ArrayList<String>();//[names.size()];
            for (int i = 0; i < aliases.size(); i++) {
                fromNames.add(aliases.get(i).getInternalName());
                asnames.add(aliases.get(i).getInternalAsname());
                if (asnames.get(i) == null)
                    asnames.set(i, fromNames.get(i));
            }
            int strArray = makeStrings(code, fromNames);
            code.aload(strArray);
            code.freeLocal(strArray);

            loadFrame();

            if (node.getInternalLevel() == 0) {
                if (module.getFutures().isAbsoluteImportOn()) {
                    code.iconst_0();
                } else {
                    code.iconst_m1();
                }
            } else {
                code.iconst(node.getInternalLevel());
            }
            code.invokestatic(p(imp.class), "importFrom", sig(PyObject[].class, String.class,
                        String[].class, PyFrame.class, Integer.TYPE));
            int tmp = storeTop();
            for (int i = 0; i < aliases.size(); i++) {
                code.aload(tmp);
                code.iconst(i);
                code.aaload();
                set(new Name(aliases.get(i), asnames.get(i), expr_contextType.Store));
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
        visit(node.getInternalBody());
        stackProduce();

        if (node.getInternalGlobals() != null) {
            visit(node.getInternalGlobals());
        } else {
            code.aconst_null();
        }
        stackProduce();

        if (node.getInternalLocals() != null) {
            visit(node.getInternalLocals());
        } else {
            code.aconst_null();
        }
        stackProduce();

        //do the real work here
        stackConsume(3);
        code.invokestatic(p(Py.class), "exec", sig(Void.TYPE, PyObject.class, PyObject.class,
                    PyObject.class));
        return null;
    }

    @Override
    public Object visitAssert(Assert node) throws Exception {
        setline(node);
        Label end_of_assert = new Label();
        
        /* First do an if __debug__: */
        loadFrame();
        emitGetGlobal("__debug__");

        code.invokevirtual(p(PyObject.class), "__nonzero__", sig(Boolean.TYPE));

        code.ifeq(end_of_assert);

        /* Now do the body of the assert. If PyObject.__nonzero__ is true,
           then the assertion succeeded, the message portion should not be
           processed. Otherwise, the message will be processed. */
        visit(node.getInternalTest());
        code.invokevirtual(p(PyObject.class), "__nonzero__", sig(Boolean.TYPE));

        /* If evaluation is false, then branch to end of method */
        code.ifne(end_of_assert);

        /* Visit the message part of the assertion, or pass Py.None */
        if( node.getInternalMsg() != null ) {
             visit(node.getInternalMsg());
        } else {
            getNone(); 
        }
        
        /* Push exception type onto stack(AssertionError) */
        loadFrame();
        emitGetGlobal("AssertionError");
        
        code.swap(); // The type is the first argument, but the message could be a yield
        
        code.invokestatic(p(Py.class), "makeException", sig(PyException.class, PyObject.class,
                    PyObject.class));
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

        setline(node.getInternalTest());
        visit(node.getInternalTest());
        code.invokevirtual(p(PyObject.class), "__nonzero__", sig(Boolean.TYPE));

        code.ifeq(end_of_suite);

        Object exit = suite(node.getInternalBody());

        if (end_of_if != null && exit == null)
            code.goto_(end_of_if);

        code.label(end_of_suite);

        if (node.getInternalOrelse() != null) {
            return suite(node.getInternalOrelse()) != null ? exit : null;
        } else {
            return null;
        }
    }

    @Override
    public Object visitIf(If node) throws Exception {
        Label end_of_if = null;
        if (node.getInternalOrelse() != null)
            end_of_if = new Label();

        Object exit = doTest(end_of_if, node, 0);
        if (end_of_if != null)
            code.label(end_of_if);
        return exit;
    }

    @Override
    public Object visitIfExp(IfExp node) throws Exception {
        setline(node.getInternalTest());
        Label end = new Label();
        Label end_of_else = new Label();

        visit(node.getInternalTest());
        code.invokevirtual(p(PyObject.class), "__nonzero__", sig(Boolean.TYPE));

        code.ifeq(end_of_else);
        visit(node.getInternalBody());
        code.goto_(end);

        code.label(end_of_else);
        visit(node.getInternalOrelse());

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
        Label continue_loop = continueLabels.peek();
        Label break_loop = breakLabels.peek();

        Label start_loop = new Label();

        code.goto_(continue_loop);
        code.label(start_loop);

        //Do suite
        suite(node.getInternalBody());

        code.label(continue_loop);
        setline(node);

        //Do test
        visit(node.getInternalTest());
        code.invokevirtual(p(PyObject.class), "__nonzero__", sig(Boolean.TYPE));
        code.ifne(start_loop);

        finishLoop(savebcf);

        if (node.getInternalOrelse() != null) {
            //Do else
            suite(node.getInternalOrelse());
        }
        code.label(break_loop);

        // Probably need to detect "guaranteed exits"
        return null;
    }

    @Override
    public Object visitFor(For node) throws Exception {
        int savebcf = beginLoop();
        Label continue_loop = continueLabels.peek();
        Label break_loop = breakLabels.peek();
        Label start_loop = new Label();
        Label next_loop = new Label();

        setline(node);

        //parse the list
        visit(node.getInternalIter());

        int iter_tmp = code.getLocal(p(PyObject.class));
        int expr_tmp = code.getLocal(p(PyObject.class));

        //set up the loop iterator
        code.invokevirtual(p(PyObject.class), "__iter__", sig(PyObject.class));
        code.astore(iter_tmp);

        //do check at end of loop.  Saves one opcode ;-)
        code.goto_(next_loop);

        code.label(start_loop);
        //set iter variable to current entry in list
        set(node.getInternalTarget(), expr_tmp);

        //evaluate for body
        suite(node.getInternalBody());

        code.label(continue_loop);

        code.label(next_loop);
        setline(node);
        //get the next element from the list
        code.aload(iter_tmp);
        code.invokevirtual(p(PyObject.class), "__iternext__", sig(PyObject.class));

        code.astore(expr_tmp);
        code.aload(expr_tmp);
        //if no more elements then fall through
        code.ifnonnull(start_loop);

        finishLoop(savebcf);

        if (node.getInternalOrelse() != null) {
            //Do else clause if provided
            suite(node.getInternalOrelse());
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
        for (int i = 0; i < node.getInternalHandlers().size(); i++) {
            ExceptHandler handler = (ExceptHandler)node.getInternalHandlers().get(i);

            //setline(name);
            Label end_of_self = new Label();

            if (handler.getInternalType() != null) {
                code.aload(exc);
                //get specific exception
                visit(handler.getInternalType());
                code.invokevirtual(p(PyException.class), "match", sig(Boolean.TYPE,
                            PyObject.class));
                code.ifeq(end_of_self);
            } else {
                if (i != node.getInternalHandlers().size()-1) {
                    throw new ParseException(
                        "default 'except:' must be last", handler);
                }
            }

            if (handler.getInternalName() != null) {
                code.aload(exc);
                code.getfield(p(PyException.class), "value", ci(PyObject.class));
                set(handler.getInternalName());
            }

            //do exception body
            suite(handler.getInternalBody());
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

        int excLocal = code.getLocal(p(Throwable.class));
        code.aconst_null();
        code.astore(excLocal);

        code.label(start);
        inFinally.exceptionStarts.addElement(start);

        ret = suite(node.getInternalBody());

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

        code.invokestatic(p(Py.class), "addTraceback", sig(Void.TYPE, Throwable.class,
                    PyFrame.class));

        inlineFinally(inFinally);
        code.aload(excLocal);
        code.checkcast(p(Throwable.class));
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
            handler.finalBody(this);
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
        Stack<ExceptionHandler> poppedHandlers = new Stack<ExceptionHandler>();
        while (exceptionHandlers.size() > level) {
            ExceptionHandler handler = exceptionHandlers.pop();
            inlineFinally(handler);
            poppedHandlers.push(handler);
        }
        while (poppedHandlers.size() > 0) {
            ExceptionHandler handler = poppedHandlers.pop();
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
        Object exit = suite(node.getInternalBody());
        exceptionHandlers.pop();
        code.label(end);
        handler.exceptionEnds.addElement(end);

        if (exit == null)
            code.goto_(handler_end);

        code.label(handler_start);

        loadFrame();

        code.invokestatic(p(Py.class), "setException", sig(PyException.class, Throwable.class,
                    PyFrame.class));

        int exc = code.getFinallyLocal(p(Throwable.class));
        code.astore(exc);

        if (node.getInternalOrelse() == null) {
            //No else clause to worry about
            exceptionTest(exc, handler_end, node, 1);
            code.label(handler_end);
        } else {
            //Have else clause
            Label else_end = new Label();
            exceptionTest(exc, else_end, node, 1);
            code.label(handler_end);

            //do else clause
            suite(node.getInternalOrelse());
            code.label(else_end);
        }

        code.freeFinallyLocal(exc);
        handler.addExceptionHandlers(handler_start);
        return null;
    }

    @Override
    public Object visitSuite(Suite node) throws Exception {
        return suite(node.getInternalBody());
    }

    public Object suite(java.util.List<stmt> stmts) throws Exception {
        for(stmt s: stmts) {
            Object exit = visit(s);
            if (exit != null)
                return Exit;
        }
        return null;
    }

    @Override
    public Object visitBoolOp(BoolOp node) throws Exception {
        Label end = new Label();
        visit(node.getInternalValues().get(0));
        for (int i = 1; i < node.getInternalValues().size(); i++) {
            code.dup();
            code.invokevirtual(p(PyObject.class), "__nonzero__", sig(Boolean.TYPE));
            switch (node.getInternalOp()) {
            case Or : 
                code.ifne(end);
                break;
            case And : 
                code.ifeq(end);
                break;
            }
            code.pop();
            visit(node.getInternalValues().get(i));
        }
        code.label(end);
        return null;
    }


    @Override
    public Object visitCompare(Compare node) throws Exception {
        int last = code.getLocal(p(PyObject.class));
        int result = code.getLocal(p(PyObject.class));
        Label end = new Label();

        visit(node.getInternalLeft());
        code.astore(last);

        int n = node.getInternalOps().size();
        for(int i = 0; i < n - 1; i++) {
            visit(node.getInternalComparators().get(i));
            code.aload(last);
            code.swap();
            code.dup();
            code.astore(last);
            visitCmpop(node.getInternalOps().get(i));
            code.dup();
            code.astore(result);
            code.invokevirtual(p(PyObject.class), "__nonzero__", sig(Boolean.TYPE));
            code.ifeq(end);
        }

        visit(node.getInternalComparators().get(n-1));
        code.aload(last);
        code.swap();
        visitCmpop(node.getInternalOps().get(n-1));

        if (n > 1) {
            code.astore(result);
            code.label(end);
            code.aload(result);
        }

        code.aconst_null();
        code.astore(last);
        code.freeLocal(last);
        code.freeLocal(result);
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
        code.invokevirtual(p(PyObject.class), name, sig(PyObject.class, PyObject.class));
    }

    @Override
    public Object visitBinOp(BinOp node) throws Exception {
        visit(node.getInternalLeft());
        stackProduce();
        visit(node.getInternalRight());
        stackConsume();
        String name = null;
        switch (node.getInternalOp()) {
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

        if (node.getInternalOp() == operatorType.Div && module.getFutures().areDivisionOn()) {
            name = "_truediv";
        }
        code.invokevirtual(p(PyObject.class), name, sig(PyObject.class, PyObject.class));
        return null;
    }
    
    @Override
    public Object visitUnaryOp(UnaryOp node) throws Exception {
        visit(node.getInternalOperand());
        String name = null;
        switch (node.getInternalOp()) {
        case Invert:    name = "__invert__"; break;
        case Not: name = "__not__"; break;
        case UAdd:    name = "__pos__"; break;
        case USub:   name = "__neg__"; break;
        }
        code.invokevirtual(p(PyObject.class), name, sig(PyObject.class));
        return null;
    }

    @Override
    public Object visitAugAssign(AugAssign node) throws Exception {
        setline(node);

        augmode = expr_contextType.Load;
        visit(node.getInternalTarget());
        int target = storeTop();

        visit(node.getInternalValue());

        code.aload(target);
        code.swap();
        String name = null;
        switch (node.getInternalOp()) {
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
        if (node.getInternalOp() == operatorType.Div && module.getFutures().areDivisionOn()) {
            name = "_itruediv";
        }
        code.invokevirtual(p(PyObject.class), name, sig(PyObject.class, PyObject.class));
        code.freeLocal(target);

        temporary = storeTop();
        augmode = expr_contextType.Store;
        visit(node.getInternalTarget());
        code.freeLocal(temporary);

        return null;
    }


    public static int makeStrings(Code c, Collection<String> names)
        throws IOException
    {
        if (names != null) {
            c.iconst(names.size());
        } else {
            c.iconst_0();
        }
        c.anewarray(p(String.class));
        int strings = c.getLocal(ci(String[].class));
        c.astore(strings);
        if (names != null) {
            int i = 0;
            for (String name : names) {
                c.aload(strings);
                c.iconst(i);
                c.ldc(name);
                c.aastore();
                i++;
            }
        }
        return strings;
    }
    
    public Object invokeNoKeywords(Attribute node, java.util.List<expr> values)
        throws Exception
    {
        String name = getName(node.getInternalAttr());
        visit(node.getInternalValue()); stackProduce();
        code.ldc(name);
        code.invokevirtual(p(PyObject.class), "__getattr__", sig(PyObject.class, String.class));
        loadThreadState(); stackProduce(p(ThreadState.class));

        switch (values.size()) {
        case 0:
            stackConsume(2); // target + ts
            code.invokevirtual(p(PyObject.class), "__call__", sig(PyObject.class, ThreadState.class));
            break;
        case 1:
            visit(values.get(0));
            stackConsume(2); // target + ts
            code.invokevirtual(p(PyObject.class), "__call__", sig(PyObject.class, ThreadState.class,
                        PyObject.class));
            break;
        case 2:
            visit(values.get(0)); stackProduce();
            visit(values.get(1));
            stackConsume(3); // target + ts + arguments
            code.invokevirtual(p(PyObject.class), "__call__", sig(PyObject.class, ThreadState.class,
                        PyObject.class, PyObject.class));
            break;
        case 3:
            visit(values.get(0)); stackProduce();
            visit(values.get(1)); stackProduce();
            visit(values.get(2));
            stackConsume(4); // target + ts + arguments
            code.invokevirtual(p(PyObject.class), "__call__", sig(PyObject.class, ThreadState.class,
                        PyObject.class, PyObject.class, PyObject.class));
            break;
        case 4:
            visit(values.get(0)); stackProduce();
            visit(values.get(1)); stackProduce();
            visit(values.get(2)); stackProduce();
            visit(values.get(3));
            stackConsume(5); // target + ts + arguments
            code.invokevirtual(p(PyObject.class), "__call__", sig(PyObject.class, ThreadState.class,
                        PyObject.class, PyObject.class, PyObject.class, PyObject.class));
            break;
        default:
            int argArray = makeArray(values);
            code.aload(argArray);
            code.freeLocal(argArray);
            stackConsume(2); // target + ts
            code.invokevirtual(p(PyObject.class), "__call__", sig(PyObject.class, ThreadState.class,
                        PyObject[].class));
            break;
        }
        return null;
    }


    @Override
    public Object visitCall(Call node) throws Exception {
        java.util.List<String> keys = new ArrayList<String>();//[node.keywords.size()];
        java.util.List<expr> values = new ArrayList<expr>();//[node.args.size() + keys.size()];
        for (int i = 0; i < node.getInternalArgs().size(); i++) {
            values.add(node.getInternalArgs().get(i));
        }
        for (int i = 0; i < node.getInternalKeywords().size(); i++) {
            keys.add(node.getInternalKeywords().get(i).getInternalArg());
            values.add(node.getInternalKeywords().get(i).getInternalValue());
        }

        if ((node.getInternalKeywords() == null || node.getInternalKeywords().size() == 0)&& node.getInternalStarargs() == null &&
            node.getInternalKwargs() == null && node.getInternalFunc() instanceof Attribute)
        {
            return invokeNoKeywords((Attribute) node.getInternalFunc(), values);
        }

        visit(node.getInternalFunc()); stackProduce();

        if (node.getInternalStarargs() != null || node.getInternalKwargs() != null) {
            int argArray = makeArray(values);
            int strArray = makeStrings(code, keys);
            if (node.getInternalStarargs() == null)
                code.aconst_null();
            else
                visit(node.getInternalStarargs());
            stackProduce();
            if (node.getInternalKwargs() == null)
                code.aconst_null();
            else
                visit(node.getInternalKwargs());
            stackProduce();
            
            code.aload(argArray);
            code.aload(strArray);
            code.freeLocal(argArray);
            code.freeLocal(strArray);
            code.dup2_x2();
            code.pop2();
            
            stackConsume(3); // target + starargs + kwargs

            code.invokevirtual(p(PyObject.class), "_callextra", sig(PyObject.class,
                        PyObject[].class, String[].class, PyObject.class, PyObject.class));
        } else if (keys.size() > 0) {
            loadThreadState(); stackProduce(p(ThreadState.class));
            int argArray = makeArray(values);
            int strArray = makeStrings(code, keys);
            code.aload(argArray);
            code.aload(strArray);
            code.freeLocal(argArray);
            code.freeLocal(strArray);
            stackConsume(2); // target + ts
            code.invokevirtual(p(PyObject.class), "__call__", sig(PyObject.class, ThreadState.class,
                        PyObject[].class, String[].class));
        } else {
            loadThreadState(); stackProduce(p(ThreadState.class));
            switch (values.size()) {
            case 0:
                stackConsume(2); // target + ts
                code.invokevirtual(p(PyObject.class), "__call__", sig(PyObject.class,
                            ThreadState.class));
                break;
            case 1:
                visit(values.get(0));
                stackConsume(2); // target + ts
                code.invokevirtual(p(PyObject.class), "__call__", sig(PyObject.class,
                            ThreadState.class, PyObject.class));
                break;
            case 2:
                visit(values.get(0)); stackProduce();
                visit(values.get(1));
                stackConsume(3); // target + ts + arguments
                code.invokevirtual(p(PyObject.class), "__call__", sig(PyObject.class,
                            ThreadState.class, PyObject.class, PyObject.class));
                break;
            case 3:
                visit(values.get(0)); stackProduce();
                visit(values.get(1)); stackProduce();
                visit(values.get(2));
                stackConsume(4); // target + ts + arguments
                code.invokevirtual(p(PyObject.class), "__call__", sig(PyObject.class,
                            ThreadState.class, PyObject.class, PyObject.class, PyObject.class));
                break;
            case 4:
                visit(values.get(0)); stackProduce();
                visit(values.get(1)); stackProduce();
                visit(values.get(2)); stackProduce();
                visit(values.get(3));
                stackConsume(5); // target + ts + arguments
                code.invokevirtual(p(PyObject.class), "__call__", sig(PyObject.class,
                            ThreadState.class, PyObject.class, PyObject.class, PyObject.class,
                            PyObject.class));
                break;
            default:
                int argArray = makeArray(values);
                code.aload(argArray);
                code.freeLocal(argArray);
                stackConsume(2); // target + ts
                code.invokevirtual(p(PyObject.class), "__call__", sig(PyObject.class,
                            ThreadState.class, PyObject[].class));
                break;
            }
        }
        return null;
    }


    public Object Slice(Subscript node, Slice slice) throws Exception {
        expr_contextType ctx = node.getInternalCtx();
        if (ctx == expr_contextType.AugStore && augmode == expr_contextType.Store) {
            restoreAugTmps(node, 4);
            ctx = expr_contextType.Store;
        } else {
            visit(node.getInternalValue());
            stackProduce();
            if (slice.getInternalLower() != null)
                visit(slice.getInternalLower());
            else
                code.aconst_null();
            stackProduce();
            if (slice.getInternalUpper() != null)
                visit(slice.getInternalUpper());
            else
                code.aconst_null();
            stackProduce();
            if (slice.getInternalStep() != null)
                visit(slice.getInternalStep());
            else
                code.aconst_null();
            stackProduce();

            if (node.getInternalCtx() == expr_contextType.AugStore && augmode == expr_contextType.Load) {
                saveAugTmps(node, 4);
                ctx = expr_contextType.Load;
            }
            stackConsume(4);
        }

        switch (ctx) {
        case Del:
            code.invokevirtual(p(PyObject.class), "__delslice__", sig(Void.TYPE, PyObject.class,
                        PyObject.class, PyObject.class));
            return null;
        case Load:
            code.invokevirtual(p(PyObject.class), "__getslice__", sig(PyObject.class,
                        PyObject.class, PyObject.class, PyObject.class));
            return null;
        case Param:
        case Store:
            code.aload(temporary);
            code.invokevirtual(p(PyObject.class), "__setslice__", sig(Void.TYPE, PyObject.class,
                        PyObject.class, PyObject.class, PyObject.class));
            return null;
        }
        return null;

    }

    @Override
    public Object visitSubscript(Subscript node) throws Exception {
        if (node.getInternalSlice() instanceof Slice) {
            return Slice(node, (Slice) node.getInternalSlice());
        }

        int value = temporary;
        expr_contextType ctx = node.getInternalCtx();
        if (node.getInternalCtx() == expr_contextType.AugStore && augmode == expr_contextType.Store) {
            restoreAugTmps(node, 2);
            ctx = expr_contextType.Store;
        } else {
            visit(node.getInternalValue()); stackProduce();
            visit(node.getInternalSlice());
            stackConsume();

            if (node.getInternalCtx() == expr_contextType.AugStore && augmode == expr_contextType.Load) {
                saveAugTmps(node, 2);
                ctx = expr_contextType.Load;
            }
        }

        switch (ctx) {
        case Del:
            code.invokevirtual(p(PyObject.class), "__delitem__", sig(Void.TYPE, PyObject.class));
            return null;
        case Load:
            code.invokevirtual(p(PyObject.class), "__getitem__", sig(PyObject.class, PyObject.class));
            return null;
        case Param:
        case Store:
            code.aload(value);
            code.invokevirtual(p(PyObject.class), "__setitem__", sig(Void.TYPE, PyObject.class,
                        PyObject.class));
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
        int dims = makeArray(node.getInternalDims());
        code.new_(p(PyTuple.class));
        code.dup();
        code.aload(dims);
        code.invokespecial(p(PyTuple.class), "<init>", sig(Void.TYPE, PyObject[].class));
        freeArray(dims);
        return null;
    }

    @Override
    public Object visitAttribute(Attribute node) throws Exception {

        expr_contextType ctx = node.getInternalCtx();
        if (node.getInternalCtx() == expr_contextType.AugStore && augmode == expr_contextType.Store) {
            restoreAugTmps(node, 2);
            ctx = expr_contextType.Store;
        } else {
            visit(node.getInternalValue());
            code.ldc(getName(node.getInternalAttr()));

            if (node.getInternalCtx() == expr_contextType.AugStore && augmode == expr_contextType.Load) {
                saveAugTmps(node, 2);
                ctx = expr_contextType.Load;
            }
        }

        switch (ctx) {
        case Del:
            code.invokevirtual(p(PyObject.class), "__delattr__", sig(Void.TYPE, String.class));
            return null;
        case Load:
            code.invokevirtual(p(PyObject.class), "__getattr__", sig(PyObject.class, String.class));
            return null;
        case Param:
        case Store:
            code.aload(temporary);
            code.invokevirtual(p(PyObject.class), "__setattr__", sig(Void.TYPE, String.class,
                        PyObject.class));
            return null;
        }
        return null;
    }

    public Object seqSet(java.util.List<expr> nodes) throws Exception {
        code.aload(temporary);
        code.iconst(nodes.size());
        code.invokestatic(p(Py.class), "unpackSequence", sig(PyObject[].class, PyObject.class,
                    Integer.TYPE));

        int tmp = code.getLocal("[org/python/core/PyObject");
        code.astore(tmp);

        for (int i = 0; i < nodes.size(); i++) {
            code.aload(tmp);
            code.iconst(i);
            code.aaload();
            set(nodes.get(i));
        }
        code.freeLocal(tmp);

        return null;
    }

    public Object seqDel(java.util.List<expr> nodes) throws Exception {
        for (expr e: nodes) {
            visit(e);
        }
        return null;
    }

    @Override
    public Object visitTuple(Tuple node) throws Exception {
        /* if (mode ==AUGSET)
            throw new ParseException(
                      "augmented assign to tuple not possible", node); */
        if (node.getInternalCtx() == expr_contextType.Store) return seqSet(node.getInternalElts());
        if (node.getInternalCtx() == expr_contextType.Del) return seqDel(node.getInternalElts());
        
        int content = makeArray(node.getInternalElts());

        code.new_(p(PyTuple.class));

        code.dup();
        code.aload(content);
        code.invokespecial(p(PyTuple.class), "<init>", sig(Void.TYPE, PyObject[].class));
        freeArray(content);
        return null;
    }

    @Override
    public Object visitList(List node) throws Exception {
        if (node.getInternalCtx() == expr_contextType.Store) return seqSet(node.getInternalElts());
        if (node.getInternalCtx() == expr_contextType.Del) return seqDel(node.getInternalElts());
        
        int content = makeArray(node.getInternalElts());

        code.new_(p(PyList.class));
        code.dup();
        code.aload(content);
        code.invokespecial(p(PyList.class), "<init>", sig(Void.TYPE, PyObject[].class));
        freeArray(content);
        return null;
    }

    @Override
    public Object visitListComp(ListComp node) throws Exception {
        code.new_(p(PyList.class));

        code.dup();
        code.invokespecial(p(PyList.class), "<init>", sig(Void.TYPE));

        code.dup();

        code.ldc("append");

        code.invokevirtual(p(PyObject.class), "__getattr__", sig(PyObject.class, String.class));
        String tmp_append ="_[" + node.getLine() + "_" + node.getCharPositionInLine() + "]";

        set(new Name(node, tmp_append, expr_contextType.Store));

        java.util.List<expr> args = new ArrayList<expr>();
        args.add(node.getInternalElt());
        stmt n = new Expr(node, new Call(node, new Name(node, tmp_append, expr_contextType.Load), 
                                       args,
                                       new ArrayList<keyword>(), null, null));

        for (int i = node.getInternalGenerators().size() - 1; i >= 0; i--) {
            comprehension lc = node.getInternalGenerators().get(i);
            for (int j = lc.getInternalIfs().size() - 1; j >= 0; j--) {
                java.util.List<stmt> body = new ArrayList<stmt>();
                body.add(n);
                n = new If(lc.getInternalIfs().get(j), lc.getInternalIfs().get(j), body, new ArrayList<stmt>());
            }
            java.util.List<stmt> body = new ArrayList<stmt>();
            body.add(n);
            n = new For(lc,lc.getInternalTarget(), lc.getInternalIter(), body, new ArrayList<stmt>());
        }
        visit(n);
        java.util.List<expr> targets = new ArrayList<expr>();
        targets.add(new Name(n, tmp_append, expr_contextType.Del));
        visit(new Delete(n, targets));

        return null;
    }

    @Override
    public Object visitDict(Dict node) throws Exception {
        java.util.List<PythonTree> elts = new ArrayList<PythonTree>();
        for (int i = 0; i < node.getInternalKeys().size(); i++) {
            elts.add(node.getInternalKeys().get(i));
            elts.add(node.getInternalValues().get(i));
        }
        int content = makeArray(elts);
        
        code.new_(p(PyDictionary.class));
        code.dup();
        code.aload(content);
        code.invokespecial(p(PyDictionary.class), "<init>", sig(Void.TYPE, PyObject[].class));
        freeArray(content);
        return null;
    }

    @Override
    public Object visitRepr(Repr node) throws Exception {
        visit(node.getInternalValue());
        code.invokevirtual(p(PyObject.class), "__repr__", sig(PyString.class));
        return null;
    }

    @Override
    public Object visitLambda(Lambda node) throws Exception {
        String name = "<lambda>";

        //Add a return node onto the outside of suite;
        java.util.List<stmt> bod = new ArrayList<stmt>();
        bod.add(new Return(node,node.getInternalBody()));
        mod retSuite = new Suite(node, bod);

        setline(node);

        ScopeInfo scope = module.getScopeInfo(node);

        int defaultsArray = makeArray(scope.ac.getDefaults());

        code.new_(p(PyFunction.class));
        
        code.dup();
        code.aload(defaultsArray);
        code.freeLocal(defaultsArray);

        loadFrame();
        code.getfield(p(PyFrame.class), "f_globals", ci(PyObject.class));
        
        code.swap();

        scope.setup_closure();
        scope.dump();
        module.codeConstant(retSuite, name, true, className,
                      false, false, node.getLine(), scope, cflags).get(code);

        if (!makeClosure(scope)) {
            code.invokespecial(p(PyFunction.class), "<init>", sig(Void.TYPE, PyObject.class,
                        PyObject[].class, PyCode.class));
        } else {
            code.invokespecial(p(PyFunction.class), "<init>", sig(Void.TYPE, PyObject.class,
                        PyObject[].class, PyCode.class, PyObject[].class));
        }
        return null;
    }


    @Override
    public Object visitEllipsis(Ellipsis node) throws Exception {
        code.getstatic(p(Py.class), "Ellipsis", ci(PyObject.class));
        return null;
    }

    @Override
    public Object visitSlice(Slice node) throws Exception {
        if (node.getInternalLower() == null) getNone(); else visit(node.getInternalLower()); stackProduce();
        if (node.getInternalUpper() == null) getNone(); else visit(node.getInternalUpper()); stackProduce();
        if (node.getInternalStep() == null) getNone(); else visit(node.getInternalStep());
        int step = storeTop();
        stackConsume(2);
        
        code.new_(p(PySlice.class));
        code.dup();
        code.dup2_x2();
        code.pop2();
        
        code.aload(step);
        code.freeLocal(step);
        
        code.invokespecial(p(PySlice.class), "<init>", sig(Void.TYPE, PyObject.class,
                    PyObject.class, PyObject.class));
        return null;
    }

    @Override
    public Object visitClassDef(ClassDef node) throws Exception {
        setline(node);

        int baseArray = makeArray(node.getInternalBases());

        //Get class name
        String name = getName(node.getInternalName());
        code.ldc(name);
        
        code.aload(baseArray);

        ScopeInfo scope = module.getScopeInfo(node);

        scope.setup_closure();
        scope.dump();
        //Make code object out of suite
        module.codeConstant(new Suite(node,node.getInternalBody()), name, false, name,
                      true, false, node.getLine(), scope, cflags).get(code);

        //Get doc string (if there)
        getDocString(node.getInternalBody());

        //Make class out of name, bases, and code
        if (!makeClosure(scope)) {
            code.invokestatic(p(Py.class), "makeClass", sig(PyObject.class, String.class,
                        PyObject[].class, PyCode.class, PyObject.class));
        } else {
            code.invokestatic(p(Py.class), "makeClass", sig(PyObject.class, String.class,
                        PyObject[].class, PyCode.class, PyObject.class, PyObject[].class));
        }
        
        applyDecorators(node.getInternalDecorator_list());

        //Assign this new class to the given name
        set(new Name(node,node.getInternalName(), expr_contextType.Store));
        //doDecorators(node,node.getInternalDecorator_list(), node.getInternalName());
        freeArray(baseArray);
        return null;
    }

    @Override
    public Object visitNum(Num node) throws Exception {
        if (node.getInternalN() instanceof PyInteger) {
            module.integerConstant(((PyInteger) node.getInternalN()).getValue()).get(code);
        } else if (node.getInternalN() instanceof PyLong) {
            module.longConstant(((PyObject)node.getInternalN()).__str__().toString()).get(code);
        } else if (node.getInternalN() instanceof PyFloat) {
            module.floatConstant(((PyFloat) node.getInternalN()).getValue()).get(code);
        } else if (node.getInternalN() instanceof PyComplex) {
            module.complexConstant(((PyComplex) node.getInternalN()).imag).get(code);
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
        code.invokevirtual(p(PyFrame.class), "getglobal", sig(PyObject.class, String.class));
    }

    @Override
    public Object visitName(Name node) throws Exception {
        String name;
        if (fast_locals)
            name = node.getInternalId();
        else
            name = getName(node.getInternalId());

        SymInfo syminf = tbl.get(name);

        expr_contextType ctx = node.getInternalCtx();
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
                        code.invokevirtual(p(PyFrame.class), "getderef", sig(PyObject.class,
                                    Integer.TYPE));
                        return null;
                    }
                    if ((flags&ScopeInfo.BOUND) != 0) {
                        code.iconst(syminf.locals_index);
                        code.invokevirtual(p(PyFrame.class), "getlocal", sig(PyObject.class,
                                    Integer.TYPE));
                        return null;
                    }
                }
                if ((flags&ScopeInfo.FREE) != 0 &&
                            (flags&ScopeInfo.BOUND) == 0) {
                    code.iconst(syminf.env_index);
                    code.invokevirtual(p(PyFrame.class), "getderef", sig(PyObject.class,
                                Integer.TYPE));
                    return null;
                }
            }
            code.ldc(name);
            code.invokevirtual(p(PyFrame.class), "getname", sig(PyObject.class, String.class));
            return null;

        case Param:
        case Store:
            loadFrame();
            if (syminf != null && (syminf.flags&ScopeInfo.GLOBAL) != 0) {
                code.ldc(name);
                code.aload(temporary);
                code.invokevirtual(p(PyFrame.class), "setglobal", sig(Void.TYPE, String.class,
                            PyObject.class));
            } else {
                if (!fast_locals) {
                    code.ldc(name);
                    code.aload(temporary);
                    code.invokevirtual(p(PyFrame.class), "setlocal", sig(Void.TYPE, String.class,
                                PyObject.class));
                } else {
                    if (syminf == null) {
                        throw new ParseException("internal compiler error", node);
                    }
                    if ((syminf.flags&ScopeInfo.CELL) != 0) {
                        code.iconst(syminf.env_index);
                        code.aload(temporary);
                        code.invokevirtual(p(PyFrame.class), "setderef", sig(Void.TYPE,
                                    Integer.TYPE, PyObject.class));
                    } else {
                        code.iconst(syminf.locals_index);
                        code.aload(temporary);
                        code.invokevirtual(p(PyFrame.class), "setlocal", sig(Void.TYPE,
                                    Integer.TYPE, PyObject.class));
                    }
                }
            }
            return null;
        case Del: {
            loadFrame();
            if (syminf != null && (syminf.flags&ScopeInfo.GLOBAL) != 0) {
                code.ldc(name);
                code.invokevirtual(p(PyFrame.class), "delglobal", sig(Void.TYPE, String.class));
            } else {
                if (!fast_locals) {
                    code.ldc(name);
                    code.invokevirtual(p(PyFrame.class), "dellocal", sig(Void.TYPE, String.class));
                } else {
                    if (syminf == null) {
                        throw new ParseException("internal compiler error", node);
                    }
                    if ((syminf.flags&ScopeInfo.CELL) != 0) {
                        module.error("can not delete variable '"+name+
                              "' referenced in nested scope",true,node);
                    }
                    code.iconst(syminf.locals_index);
                    code.invokevirtual(p(PyFrame.class), "dellocal", sig(Void.TYPE, Integer.TYPE));
                }
            }
            return null; }
        }
        return null;
    }

    @Override
    public Object visitStr(Str node) throws Exception {
        PyString s = (PyString)node.getInternalS();
        if (s instanceof PyUnicode) {
            module.unicodeConstant(s.asString()).get(code);
        } else {
            module.stringConstant(s.asString()).get(code);
        }
        return null;
    }

    @Override
    public Object visitGeneratorExp(GeneratorExp node) throws Exception {
        String bound_exp = "_(x)";

        setline(node);

        code.new_(p(PyFunction.class));
        code.dup();
        loadFrame();
        code.getfield(p(PyFrame.class), "f_globals", ci(PyObject.class));

        ScopeInfo scope = module.getScopeInfo(node);

        int emptyArray = makeArray(new ArrayList<expr>());
        code.aload(emptyArray);
        scope.setup_closure();
        scope.dump();

        stmt n = new Expr(node, new Yield(node,node.getInternalElt()));

        expr iter = null;
        for (int i = node.getInternalGenerators().size() - 1; i >= 0; i--) {
            comprehension comp = node.getInternalGenerators().get(i);
            for (int j = comp.getInternalIfs().size() - 1; j >= 0; j--) {
                java.util.List<stmt> bod = new ArrayList<stmt>();
                bod.add(n);
                n = new If(comp.getInternalIfs().get(j), comp.getInternalIfs().get(j), bod, new ArrayList<stmt>());
            }
            java.util.List<stmt> bod = new ArrayList<stmt>();
            bod.add(n);
            if (i != 0) {
                n = new For(comp,comp.getInternalTarget(), comp.getInternalIter(), bod, new ArrayList<stmt>());
            } else {
                n = new For(comp,comp.getInternalTarget(), new Name(node, bound_exp, expr_contextType.Load), bod, new ArrayList<stmt>());
                iter = comp.getInternalIter();
            }
        }

        java.util.List<stmt> bod = new ArrayList<stmt>();
        bod.add(n);
        module.codeConstant(new Suite(node, bod), "<genexpr>", true,
                      className, false, false,
                      node.getLine(), scope, cflags).get(code);

        code.aconst_null();
        if (!makeClosure(scope)) {
            code.invokespecial(p(PyFunction.class), "<init>", sig(Void.TYPE, PyObject.class,
                        PyObject[].class, PyCode.class, PyObject.class));
        } else {
            code.invokespecial(p(PyFunction.class), "<init>", sig(Void.TYPE, PyObject.class,
                        PyObject[].class, PyCode.class, PyObject.class, PyObject[].class));
        }
        int genExp = storeTop();

        visit(iter);
        code.aload(genExp);
        code.freeLocal(genExp);
        code.swap();
        code.invokevirtual(p(PyObject.class), "__iter__", sig(PyObject.class));
        loadThreadState();
        code.swap();
        code.invokevirtual(p(PyObject.class), "__call__", sig(PyObject.class, ThreadState.class, PyObject.class));
        freeArray(emptyArray);

        return null;
    }

    @Override
    public Object visitWith(With node) throws Exception {
        if (!module.getFutures().withStatementSupported()) {
            throw new ParseException("'with' will become a reserved keyword in Python 2.6", node);
        }

        final Label label_body_start = new Label();
        final Label label_body_end = new Label();
        final Label label_catch = new Label();
        final Label label_end = new Label();

        final Method contextGuard_getManager = Method.getMethod(
                "org.python.core.ContextManager getManager (org.python.core.PyObject)");
        final Method __enter__ = Method.getMethod(
                "org.python.core.PyObject __enter__ (org.python.core.ThreadState)");
        final Method __exit__ = Method.getMethod(
                "boolean __exit__ (org.python.core.ThreadState,org.python.core.PyException)");

        // mgr = (EXPR)
        visit(node.getInternalContext_expr());

        // wrap the manager with the ContextGuard (or get it directly if it
        // supports the ContextManager interface)
        code.invokestatic(Type.getType(ContextGuard.class).getInternalName(),
                contextGuard_getManager.getName(), contextGuard_getManager.getDescriptor());
        code.dup();
        

        final int mgr_tmp = code.getLocal(Type.getType(ContextManager.class).getInternalName());
        code.astore(mgr_tmp);

        // value = mgr.__enter__()
        loadThreadState();
        code.invokeinterface(Type.getType(ContextManager.class).getInternalName(),
                __enter__.getName(), __enter__.getDescriptor());
        int value_tmp = code.getLocal(p(PyObject.class));
        code.astore(value_tmp);

        // exc = True # not necessary, since we don't exec finally if exception

        // FINALLY (preparation)
        // ordinarily with a finally, we need to duplicate the code. that's not the case
        // here
        // # The normal and non-local-goto cases are handled here
        // if exc: # implicit
        //     exit(None, None, None)
        ExceptionHandler normalExit = new ExceptionHandler() {
            @Override
            public boolean isFinallyHandler() { return true; }

            @Override
            public void finalBody(CodeCompiler compiler) throws Exception {
                compiler.code.aload(mgr_tmp);
                loadThreadState();
                compiler.code.aconst_null();
                compiler.code.invokeinterface(Type.getType(ContextManager.class).getInternalName(), __exit__.getName(), __exit__.getDescriptor());
                compiler.code.pop();
            }
        };
        exceptionHandlers.push(normalExit);

        // try-catch block here
        ExceptionHandler handler = new ExceptionHandler();
        exceptionHandlers.push(handler);
        handler.exceptionStarts.addElement(label_body_start);

        // VAR = value  # Only if "as VAR" is present
        code.label(label_body_start);
        if (node.getInternalOptional_vars() != null) {
            set(node.getInternalOptional_vars(), value_tmp);
        }
        code.freeLocal(value_tmp);
        
        // BLOCK + FINALLY if non-local-goto
        Object blockResult = suite(node.getInternalBody());
        normalExit.bodyDone = true;
        exceptionHandlers.pop();
        exceptionHandlers.pop();
        code.label(label_body_end);
        handler.exceptionEnds.addElement(label_body_end);

        // FINALLY if *not* non-local-goto
        if (blockResult == NoExit) {
            // BLOCK would have generated FINALLY for us if it exited (due to a break,
            // continue or return)
            inlineFinally(normalExit);
            code.goto_(label_end);
        }

        // CATCH
        code.label(label_catch);

        loadFrame();
        code.invokestatic(p(Py.class), "setException", sig(PyException.class, Throwable.class,
                    PyFrame.class));
        code.aload(mgr_tmp);
        code.swap();
        loadThreadState();
        code.swap();
        code.invokeinterface(Type.getType(ContextManager.class).getInternalName(),
                __exit__.getName(), __exit__.getDescriptor());

        // # The exceptional case is handled here
        // exc = False # implicit
        // if not exit(*sys.exc_info()):
        code.ifne(label_end);
        //    raise
        // # The exception is swallowed if exit() returns true
        code.invokestatic(p(Py.class), "makeException", sig(PyException.class));
        code.checkcast(p(Throwable.class));
        code.athrow();

        code.label(label_end);
        code.freeLocal(mgr_tmp);

        handler.addExceptionHandlers(label_catch);
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
        public Vector<Label> exceptionStarts = new Vector<Label>();
        public Vector<Label> exceptionEnds = new Vector<Label>();

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
                Label start = exceptionStarts.elementAt(i);
                Label end = exceptionEnds.elementAt(i);
                //FIXME: not at all sure that getOffset() test is correct or necessary.
                if (start.getOffset() != end.getOffset()) {
                    code.trycatch(
                        exceptionStarts.elementAt(i),
                        exceptionEnds.elementAt(i),
                        handlerStart,
                        p(Throwable.class));
                }
            }
        }

        public void finalBody(CodeCompiler compiler) throws Exception {
            if (node instanceof TryFinally) {
                suite(((TryFinally)node).getInternalFinalbody());
            }
        }
    }
}
