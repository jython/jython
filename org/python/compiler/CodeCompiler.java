// Copyright (c) Corporation for National Research Initiatives

package org.python.compiler;

import org.python.parser.*;
import org.python.parser.ast.*;
import org.python.parser.ast.Attribute;
import org.python.core.Py;
import org.python.core.PyObject;
import org.python.core.PyInteger;
import org.python.core.PyFloat;
import org.python.core.PyLong;
import org.python.core.PyComplex;
import org.python.core.PyException;
import org.python.core.CompilerFlags;
import java.io.IOException;
import java.util.Stack;
import java.util.Hashtable;
import java.util.Vector;

public class CodeCompiler extends Visitor
    implements ClassConstants //, PythonGrammarTreeConstants
{

    public static final Object Exit=new Integer(1);
    public static final Object NoExit=null;

    public static final int GET=0;
    public static final int SET=1;
    public static final int DEL=2;
    public static final int AUGGET=3;
    public static final int AUGSET=4;

    public static final Object DoFinally=new Integer(2);

    public Module module;
    public Code code;
    public ConstantPool pool;
    public CodeCompiler mrefs;

    int temporary;
    int augmode;
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

    public Stack continueLabels, breakLabels, finallyLabels;

    public CodeCompiler(Module module, boolean print_results) {
        this.module = module;
        mrefs = this;
        pool = module.classfile.pool;

        continueLabels = new Stack();
        breakLabels = new Stack();
        finallyLabels = new Stack();
        this.print_results = print_results;
    }

    public int PyNone;
    public void getNone() throws IOException {
        if (mrefs.PyNone == 0) {
            mrefs.PyNone = pool.Fieldref("org/python/core/Py", "None",
                                         $pyObj);
        }
        code.getstatic(mrefs.PyNone);
    }

    public void loadFrame() throws Exception {
        code.aload(1);
    }

    public int storeTop() throws Exception {
        int tmp = code.getLocal();
        code.astore(tmp);
        return tmp;
    }

    public int setline;
    public void setline(int line) throws Exception {
        //System.out.println("line: "+line+", "+code.stack);
        if (module.linenumbers) {
            code.setline(line);
            loadFrame();
            code.iconst(line);
            if (mrefs.setline == 0) {
                mrefs.setline = pool.Methodref("org/python/core/PyFrame",
                                               "setline", "(I)V");
            }
            code.invokevirtual(mrefs.setline);
        }
    }

    public void setline(SimpleNode node) throws Exception {
        setline(node.beginLine);
    }

    public void set(SimpleNode node) throws Exception {
        int tmp = storeTop();
        set(node, tmp);
        code.aconst_null();
        code.astore(tmp);
        code.freeLocal(tmp);
    }


    boolean inSet = false;
    public void set(SimpleNode node, int tmp) throws Exception {
        //System.out.println("tmp: "+tmp);
        if (inSet) {
            System.out.println("recurse set: "+tmp+", "+temporary+", "+
                               code.getLocal()+", "+code.getLocal());
        }
        temporary = tmp;
        visit(node);
    }


    private void saveAugTmps(SimpleNode node, int count) throws Exception {
        if (count >= 4) {
            augtmp4 = code.getLocal();
            code.astore(augtmp4);
        }
        if (count >= 3) {
            augtmp3 = code.getLocal();
            code.astore(augtmp3);
        }
        if (count >= 2) {
            augtmp2 = code.getLocal();
            code.astore(augtmp2);
        }
        augtmp1 = code.getLocal();
        code.astore(augtmp1);

        code.aload(augtmp1);
        if (count >= 2)
            code.aload(augtmp2);
        if (count >= 3)
            code.aload(augtmp3);
        if (count >= 4)
            code.aload(augtmp4);
    }


    private void restoreAugTmps(SimpleNode node, int count) throws Exception {
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
                      boolean classBody, ScopeInfo scope,CompilerFlags cflags)
        throws Exception
    {
        this.fast_locals = fast_locals;
        this.className = className;
        this.code = code;

        my_scope = scope;
        names = scope.names;

        tbl = scope.tbl;
        optimizeGlobals = fast_locals&&!scope.exec&&!scope.from_import_star;

        Object exit = visit(node);
        //System.out.println("exit: "+exit+", "+(exit==null));

        if (classBody) {
            loadFrame();
            code.invokevirtual("org/python/core/PyFrame", "getf_locals",
                               "()" + $pyObj);
            code.areturn();
        } else {
            if (exit == null) {
                //System.out.println("no exit");
                getNone();
                code.areturn();
            }
        }
    }

    public Object visitInteractive(Interactive node) throws Exception {
        return visit(node.body);
    }

    public Object visitModule(org.python.parser.ast.Module suite)
        throws Exception
    {
        if (mrefs.setglobal == 0) {
            mrefs.setglobal = code.pool.Methodref(
                "org/python/core/PyFrame", "setglobal",
                "(" +$str + $pyObj + ")V");
        }

        if (suite.body.length > 0 &&
            suite.body[0] instanceof Expr &&
            ((Expr)suite.body[0]).value instanceof Str)
        {
            loadFrame();
            code.ldc("__doc__");
            visit(((Expr) suite.body[0]).value);
            code.invokevirtual(mrefs.setglobal);
        }
        if (module.setFile) {
            loadFrame();
            code.ldc("__file__");
            module.filename.get(code);
            code.invokevirtual(mrefs.setglobal);
        }
        traverse(suite);
        return null;
    }

    public Object visitExpression(Expression node) throws Exception {
        return visitReturn(new Return(node.body, node), true);
    }

    public int EmptyObjects;
    public void makeArray(SimpleNode[] nodes) throws Exception {
        int n;

        if (nodes == null)
            n = 0;
        else
            n = nodes.length;

        if (n == 0) {
            if (mrefs.EmptyObjects == 0) {
                mrefs.EmptyObjects = code.pool.Fieldref(
                    "org/python/core/Py", "EmptyObjects", $pyObjArr);
            }
            code.getstatic(mrefs.EmptyObjects);
        } else {
            int tmp = code.getLocal();
            code.iconst(n);
            code.anewarray(code.pool.Class("org/python/core/PyObject"));
            code.astore(tmp);

            for(int i=0; i<n; i++) {
                code.aload(tmp);
                code.iconst(i);
                visit(nodes[i]);
                code.aastore();
            }
            code.aload(tmp);
            code.freeLocal(tmp);
        }
    }

    public void getDocString(stmtType[] suite) throws Exception {
        //System.out.println("doc: "+suite.getChild(0));
        if (suite.length > 0 && suite[0] instanceof Expr &&
            ((Expr) suite[0]).value instanceof Str)
        {
            visit(((Expr) suite[0]).value);
        } else {
            code.aconst_null();
        }
    }

    int getclosure;

    public boolean makeClosure(Vector freenames) throws Exception {
        if (freenames == null) return false;
        int n = freenames.size();
        if (n == 0) return false;

        if (mrefs.getclosure == 0) {
            mrefs.getclosure = code.pool.Methodref(
            "org/python/core/PyFrame", "getclosure", "(I)" + $pyObj);
        }

        int tmp = code.getLocal();
        code.iconst(n);
        code.anewarray(code.pool.Class("org/python/core/PyObject"));
        code.astore(tmp);

        for(int i=0; i<n; i++) {
            code.aload(tmp);
            code.iconst(i);
            code.aload(1); // get frame
            code.iconst(((SymInfo)tbl.get(freenames.elementAt(i))).env_index);
            code.invokevirtual(getclosure);
            code.aastore();
        }

        code.aload(tmp);
        code.freeLocal(tmp);

        return true;
    }



    int f_globals, PyFunction_init, PyFunction_closure_init;

    public Object visitFunctionDef(FunctionDef node) throws Exception {
        String name = getName(node.name);

        setline(node);

        code.new_(code.pool.Class("org/python/core/PyFunction"));
        code.dup();
        loadFrame();
        if (mrefs.f_globals == 0) {
            mrefs.f_globals = code.pool.Fieldref(
                "org/python/core/PyFrame", "f_globals", $pyObj);
        }
        code.getfield(mrefs.f_globals);

        ScopeInfo scope = module.getScopeInfo(node);

        makeArray(scope.ac.getDefaults());

        scope.setup_closure(my_scope);
        scope.dump();
        module.PyCode(new Suite(node.body, node), name, true,
                      className, false, false,
                      node.beginLine, scope).get(code);
        Vector freenames = scope.freevars;

        getDocString(node.body);

        if (!makeClosure(freenames)) {
            if (mrefs.PyFunction_init == 0) {
                mrefs.PyFunction_init = code.pool.Methodref(
                    "org/python/core/PyFunction", "<init>",
                    "(" + $pyObj + $pyObjArr + $pyCode + $pyObj + ")V");
            }
            code.invokespecial(mrefs.PyFunction_init);
        } else {
            if (mrefs.PyFunction_closure_init == 0) {
                mrefs.PyFunction_closure_init = code.pool.Methodref(
                    "org/python/core/PyFunction", "<init>",
                    "(" + $pyObj + $pyObjArr + $pyCode + $pyObj + $pyObjArr +
                    ")V");
            }
            code.invokespecial(mrefs.PyFunction_closure_init);

        }

        set(new Name(node.name, Name.Store, node));
        return null;
    }

    public int printResult;

    public Object visitExpr(Expr node) throws Exception {
        setline(node);
        visit(node.value);

        if (print_results) {
            if (mrefs.printResult == 0) {
                mrefs.printResult = code.pool.Methodref(
                    "org/python/core/Py",
                    "printResult", "(" + $pyObj + ")V");
            }
            code.invokestatic(mrefs.printResult);
        } else {
            code.pop();
        }
        return null;
    }

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

    public int print1, print2, print3, print4, print5, print6;

    public Object visitPrint(Print node) throws Exception {
        setline(node);
        int tmp = -1;
        int printcomma, printlnv, println;

        if (node.dest != null) {
            visit(node.dest);
            tmp = storeTop();
            if (mrefs.print4 == 0) {
                mrefs.print4 = pool.Methodref(
                    "org/python/core/Py", "printComma",
                    "(" + $pyObj + $pyObj + ")V");
            }
            printcomma = mrefs.print4;
            if (mrefs.print5 == 0) {
                mrefs.print5 = pool.Methodref(
                    "org/python/core/Py", "println",
                    "(" + $pyObj + $pyObj + ")V");
            }
            println = mrefs.print5;
            if (mrefs.print6 == 0) {
                mrefs.print6 = pool.Methodref(
                    "org/python/core/Py", "printlnv",
                    "(" + $pyObj + ")V");
            }
            printlnv = mrefs.print6;
        }
        else {
            if (mrefs.print1 == 0) {
                mrefs.print1 = pool.Methodref(
                    "org/python/core/Py", "printComma",
                    "(" + $pyObj + ")V");
            }
            printcomma = mrefs.print1;
            if (mrefs.print2 == 0) {
                mrefs.print2 = pool.Methodref(
                    "org/python/core/Py", "println",
                    "(" + $pyObj + ")V");
            }
            println = mrefs.print2;
            if (mrefs.print3 == 0) {
                mrefs.print3 = pool.Methodref(
                    "org/python/core/Py",
                    "println", "()V");
            }
            printlnv = mrefs.print3;
        }

        if (node.value == null || node.value.length == 0) {
            if (node.dest != null)
                code.aload(tmp);
            code.invokestatic(printlnv);
        } else {
            for (int i = 0; i < node.value.length; i++) {
                if (node.dest != null)
                    code.aload(tmp);
                visit(node.value[i]);
                if (node.nl && i == node.value.length - 1) {
                    code.invokestatic(println);
                } else {
                    code.invokestatic(printcomma);
                }
            }
        }
        if (node.dest != null)
            code.freeLocal(tmp);
        return null;
    }

    public Object visitDelete(Delete node) throws Exception {
        setline(node);
        traverse(node);
        return null;
    }

    public Object visitPass(Pass node) throws Exception {
        setline(node);
        return null;
    }

    public Object visitBreak(Break node) throws Exception {
        //setline(node); Not needed here...
        if (breakLabels.empty()) {
            throw new ParseException("'break' outside loop", node);
        }

        Object obj = breakLabels.peek();
        if (obj == DoFinally) {
            code.jsr((Label)finallyLabels.peek());
            Object tmp = obj;
            breakLabels.pop();
            obj = breakLabels.peek();
            breakLabels.push(tmp);
        }
        code.goto_((Label)obj);
        return null;
    }

    public Object visitContinue(Continue node) throws Exception {
        //setline(node); Not needed here...
        if (continueLabels.empty()) {
            throw new ParseException("'continue' not properly in loop", node);
        }

        Object obj = continueLabels.peek();
        if (obj == DoFinally) {
            code.jsr((Label)finallyLabels.peek());
            Object tmp = obj;
            continueLabels.pop();
            obj = continueLabels.peek();
            continueLabels.push(tmp);
        }
        code.goto_((Label)obj);
        return null;
    }

    public Object visitYield(Yield node) throws Exception {
        return null;
    }

    public Object visitReturn(Return node) throws Exception {
        return visitReturn(node, false);
    }

    public Object visitReturn(Return node, boolean inEval)
        throws Exception
    {
        setline(node);
        if (!inEval && !fast_locals) {
            throw new ParseException("'return' outside function", node);
        }
        if (node.value != null) {
            visit(node.value);
        } else {
            getNone();
        }
        int tmp = code.getLocal();
        code.astore(tmp);
        if (!finallyLabels.empty()) {
            code.jsr((Label)finallyLabels.peek());
        }
        code.aload(tmp);
        code.areturn();
        return Exit;
    }

    public int makeException0, makeException1, makeException2, makeException3;

    public Object visitRaise(Raise node) throws Exception {
        setline(node);
        traverse(node);
        if (node.type == null) {
            if (mrefs.makeException0 == 0) {
                mrefs.makeException0 = code.pool.Methodref(
                    "org/python/core/Py", "makeException",
                    "()" + $pyExc);
            }
            code.invokestatic(mrefs.makeException0);
        } else if (node.inst == null) {
            if (mrefs.makeException1 == 0) {
                mrefs.makeException1 = code.pool.Methodref(
                    "org/python/core/Py", "makeException",
                    "(" + $pyObj + ")" + $pyExc);
            }
            code.invokestatic(mrefs.makeException1);
        } else if (node.tback == null) {
            if (mrefs.makeException2 == 0) {
                mrefs.makeException2 = code.pool.Methodref(
                    "org/python/core/Py", "makeException",
                    "(" + $pyObj + $pyObj + ")" + $pyExc);
            }
            code.invokestatic(mrefs.makeException2);
        } else {
            if (mrefs.makeException3 == 0) {
                mrefs.makeException3 = code.pool.Methodref(
                    "org/python/core/Py", "makeException",
                    "(" + $pyObj + $pyObj + $pyObj + ")" + $pyExc);
            }
            code.invokestatic(mrefs.makeException3);
        }
        code.athrow();
        return Exit;
    }

    public int importOne, importOneAs;

    public Object visitImport(Import node) throws Exception {
        setline(node);
        for (int i = 0; i < node.names.length; i++) {
            String asname = null;
            if (node.names[i].asname != null) {
                String name = node.names[i].name;
                asname = node.names[i].asname;
                code.ldc(name);
                loadFrame();
                if (mrefs.importOneAs == 0) {
                     mrefs.importOneAs = code.pool.Methodref(
                        "org/python/core/imp", "importOneAs",
                        "(" + $str + $pyFrame + ")" + $pyObj);
                }
                code.invokestatic(mrefs.importOneAs);
            } else {
                String name = node.names[i].name;
                asname = name;
                if (asname.indexOf('.') > 0)
                    asname = asname.substring(0, asname.indexOf('.'));
                code.ldc(name);
                loadFrame();
                if (mrefs.importOne == 0) {
                    mrefs.importOne = code.pool.Methodref(
                        "org/python/core/imp", "importOne",
                        "(" + $str + $pyFrame + ")" + $pyObj);
                }
                code.invokestatic(mrefs.importOne);
            }
            set(new Name(asname, Name.Store, node));
        }
        return null;
    }


    public int importAll, importFrom;

    public Object visitImportFrom(ImportFrom node) throws Exception {
        Future.checkFromFuture(node); // future stmt support
        setline(node);
        code.ldc(node.module);
        if (node.names.length > 0) {
            String[] names = new String[node.names.length];
            String[] asnames = new String[node.names.length];
            for (int i = 0; i < node.names.length; i++) {
                names[i] = node.names[i].name;
                asnames[i] = node.names[i].asname;
                if (asnames[i] == null)
                    asnames[i] = names[i];
            }
            makeStrings(code, names, names.length);

            loadFrame();
            if (mrefs.importFrom == 0) {
                mrefs.importFrom = code.pool.Methodref(
                    "org/python/core/imp", "importFrom",
                    "(" + $str + $strArr + $pyFrame + ")" + $pyObjArr);
            }
            code.invokestatic(mrefs.importFrom);
            int tmp = storeTop();
            for (int i = 0; i < node.names.length; i++) {
                code.aload(tmp);
                code.iconst(i);
                code.aaload();
                set(new Name(asnames[i], Name.Store, node));
            }
            code.freeLocal(tmp);
        } else {
            loadFrame();
            if (mrefs.importAll == 0) {
                mrefs.importAll = code.pool.Methodref(
                    "org/python/core/imp", "importAll",
                    "(" + $str + $pyFrame + ")V");
            }
            code.invokestatic(mrefs.importAll);
        }
        return null;
    }

    public Object visitGlobal(Global node) throws Exception {
        return null;
    }

    public int exec;
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
        if (mrefs.exec == 0) {
            mrefs.exec = code.pool.Methodref(
                "org/python/core/Py", "exec",
                "(" + $pyObj + $pyObj + $pyObj + ")V");
        }
        code.invokestatic(mrefs.exec);
        return null;
    }

    public int assert1, assert2;
    public Object visitAssert(Assert node) throws Exception {
        setline(node);
        Label end_of_assert = code.getLabel();

        /* First do an if __debug__: */
        loadFrame();
        emitGetGlobal("__debug__");

        if (mrefs.nonzero == 0) {
            mrefs.nonzero = code.pool.Methodref("org/python/core/PyObject",
                                                "__nonzero__", "()Z");
        }
        code.invokevirtual(mrefs.nonzero);

        code.ifeq(end_of_assert);

        /* Now do the body of the assert */
        visit(node.test);
        if (node.msg != null) {
            visit(node.msg);
            if (mrefs.assert2 == 0) {
                mrefs.assert2 = code.pool.Methodref(
                    "org/python/core/Py", "assert_",
                    "(" + $pyObj + $pyObj + ")V");
            }
            code.invokestatic(mrefs.assert2);
        } else {
            if (mrefs.assert1 == 0) {
                mrefs.assert1 = code.pool.Methodref(
                    "org/python/core/Py", "assert_",
                    "(" + $pyObj + ")V");
            }
            code.invokestatic(mrefs.assert1);
        }

        /* And finally set the label for the end of it all */
        end_of_assert.setPosition();

        return null;
    }

    public int nonzero;
    public Object doTest(Label end_of_if, If node, int index)
        throws Exception
    {
        Label end_of_suite = code.getLabel();

        setline(node.test);
        visit(node.test);
        if (mrefs.nonzero == 0) {
            mrefs.nonzero = code.pool.Methodref("org/python/core/PyObject",
                                                "__nonzero__", "()Z");
        }
        code.invokevirtual(mrefs.nonzero);
        code.ifeq(end_of_suite);

        Object exit = suite(node.body);

        if (end_of_if != null && exit == null)
            code.goto_(end_of_if);

        end_of_suite.setPosition();

        if (node.orelse != null) {
            return suite(node.orelse) != null ? exit : null;
        } else {
            return null;
        }
    }

    public Object visitIf(If node) throws Exception {
        Label end_of_if = null;
        if (node.orelse != null)
            end_of_if = code.getLabel();

        Object exit = doTest(end_of_if, node, 0);
        if (end_of_if != null)
            end_of_if.setPosition();
        return exit;
    }

    public void beginLoop() {
        continueLabels.push(code.getLabel());
        breakLabels.push(code.getLabel());
    }

    public void finishLoop() {
        continueLabels.pop();
        breakLabels.pop();
    }


    public Object visitWhile(While node) throws Exception {
        beginLoop();
        Label continue_loop = (Label)continueLabels.peek();
        Label break_loop = (Label)breakLabels.peek();

        Label start_loop = code.getLabel();

        code.goto_(continue_loop);
        start_loop.setPosition();

        //Do suite
        suite(node.body);

        continue_loop.setPosition();
        setline(node);

        //Do test
        visit(node.test);
        if (mrefs.nonzero == 0) {
            mrefs.nonzero = code.pool.Methodref("org/python/core/PyObject",
                                                "__nonzero__", "()Z");
        }
        code.invokevirtual(mrefs.nonzero);
        code.ifne(start_loop);

        finishLoop();

        if (node.orelse != null) {
            //Do else
            suite(node.orelse);
        }
        break_loop.setPosition();

        // Probably need to detect "guaranteed exits"
        return null;
    }

    public int iter=0;
    public int iternext=0;

    public Object visitFor(For node) throws Exception {
        beginLoop();
        Label continue_loop = (Label)continueLabels.peek();
        Label break_loop = (Label)breakLabels.peek();
        Label start_loop = code.getLabel();
        Label next_loop = code.getLabel();

        int list_tmp = code.getLocal();
        int iter_tmp = code.getLocal();
        int expr_tmp = code.getLocal();

        setline(node);

        //parse the list
        visit(node.iter);
        code.astore(list_tmp);

        //set up the loop iterator

        code.aload(list_tmp);
        if (mrefs.iter == 0) {
            mrefs.iter = code.pool.Methodref(
                "org/python/core/PyObject",
                "__iter__", "()" + $pyObj);
        }
        code.invokevirtual(mrefs.iter);
        code.astore(iter_tmp);

        //do check at end of loop.  Saves one opcode ;-)
        code.goto_(next_loop);

        start_loop.setPosition();
        //set iter variable to current entry in list
        set(node.target, expr_tmp);

        //evaluate for body
        suite(node.body);

        continue_loop.setPosition();

        next_loop.setPosition();
        setline(node);
        //get the next element from the list
        code.aload(iter_tmp);
        if (mrefs.iternext == 0) {
            mrefs.iternext = code.pool.Methodref(
                "org/python/core/PyObject",
                "__iternext__", "()" + $pyObj);
        }
        code.invokevirtual(mrefs.iternext);
        code.astore(expr_tmp);
        code.aload(expr_tmp);
        //if no more elements then fall through
        code.ifnonnull(start_loop);

        finishLoop();

        if (node.orelse != null) {
            //Do else clause if provided
            suite(node.orelse);
        }

        break_loop.setPosition();

        code.freeLocal(list_tmp);
        code.freeLocal(iter_tmp);
        code.freeLocal(expr_tmp);

        // Probably need to detect "guaranteed exits"
        return null;
    }

    public int match_exception;

    public void exceptionTest(int exc, Label end_of_exceptions,
                              TryExcept node, int index)
        throws Exception
    {
        for (int i = 0; i < node.handlers.length; i++) {
            excepthandlerType handler = node.handlers[i];

            //setline(name);
            Label end_of_self = code.getLabel();

            if (handler.type != null) {
                code.aload(exc);
                //get specific exception
                visit(handler.type);
                if (mrefs.match_exception == 0) {
                    mrefs.match_exception = code.pool.Methodref(
                        "org/python/core/Py", "matchException",
                        "(" + $pyExc + $pyObj + ")Z");
                }
                code.invokestatic(mrefs.match_exception);
                code.ifeq(end_of_self);
            } else {
                if (i != node.handlers.length-1) {
                    throw new ParseException(
                        "bare except must be last except clause", handler.type);
                }
            }

            if (handler.name != null) {
                code.aload(exc);
                code.getfield(code.pool.Fieldref("org/python/core/PyException",
                                                "value",
                                                "Lorg/python/core/PyObject;"));
                set(handler.name);
            }

            //do exception body
            suite(handler.body);
            code.goto_(end_of_exceptions);
            end_of_self.setPosition();
        }
        code.aload(exc);
        code.athrow();
    }

    public int add_traceback;
    public Object visitTryFinally(TryFinally node) throws Exception
    {
        Label start = code.getLabel();
        Label end = code.getLabel();
        Label handlerStart = code.getLabel();
        Label finallyStart = code.getLabel();
        Label finallyEnd = code.getLabel();
        Label skipSuite = code.getLabel();

        Object ret;

        // Do protected suite
        continueLabels.push(DoFinally);
        breakLabels.push(DoFinally);
        finallyLabels.push(finallyStart);

        start.setPosition();
        ret = suite(node.body);
        end.setPosition();
        if (ret == null) {
            code.jsr(finallyStart);
            code.goto_(finallyEnd);
        }

        continueLabels.pop();
        breakLabels.pop();
        finallyLabels.pop();

        // Handle any exceptions that get thrown in suite
        handlerStart.setPosition();
        code.stack = 1;
        int excLocal = code.getLocal();
        code.astore(excLocal);

        code.aload(excLocal);
        loadFrame();

        if (mrefs.add_traceback == 0) {
            mrefs.add_traceback = code.pool.Methodref(
                "org/python/core/Py", "addTraceback",
                "(" + $throwable + $pyFrame + ")V");
        }
        code.invokestatic(mrefs.add_traceback);

        code.jsr(finallyStart);
        code.aload(excLocal);
        code.athrow();

        // Do finally suite
        finallyStart.setPosition();
        code.stack = 1;
        int retLocal = code.getLocal();
        code.astore(retLocal);

        // Trick the JVM verifier into thinking this code might not
        // be executed
        code.iconst(1);
        code.ifeq(skipSuite);

        // The actual finally suite is always executed (since 1 != 0)
        ret = suite(node.finalbody);

        // Fake jump to here to pretend this could always happen
        skipSuite.setPosition();

        code.ret(retLocal);
        finallyEnd.setPosition();

        code.freeLocal(retLocal);
        code.freeLocal(excLocal);
        code.addExceptionHandler(start, end, handlerStart,
                                 code.pool.Class("java/lang/Throwable"));

        // According to any JVM verifiers, this code block might not return
        return null;
    }

    public int set_exception;
    public Object visitTryExcept(TryExcept node) throws Exception {
        Label start = code.getLabel();
        Label end = code.getLabel();
        Label handler_start = code.getLabel();
        Label handler_end = code.getLabel();

        start.setPosition();
        //Do suite
        Object exit = suite(node.body);
        //System.out.println("exit: "+exit+", "+(exit != null));
        end.setPosition();
        if (exit == null)
            code.goto_(handler_end);

        handler_start.setPosition();
        //Stack has eactly one item at start of handler
        code.stack = 1;

        loadFrame();

        if (mrefs.set_exception == 0) {
            mrefs.set_exception = code.pool.Methodref(
                "org/python/core/Py", "setException",
                "(" + $throwable + $pyFrame + ")" + $pyExc);
        }
        code.invokestatic(mrefs.set_exception);

        int exc = storeTop();

        if (node.orelse == null) {
            //No else clause to worry about
            exceptionTest(exc, handler_end, node, 1);
            handler_end.setPosition();
        } else {
            //Have else clause
            Label else_end = code.getLabel();
            exceptionTest(exc, else_end, node, 1);
            handler_end.setPosition();

            //do else clause
            suite(node.orelse);
            else_end.setPosition();
        }

        code.freeLocal(exc);
        code.addExceptionHandler(start, end, handler_start,
                                 code.pool.Class("java/lang/Throwable"));
        return null;
    }

    public Object visitSuite(Suite node) throws Exception {
        return suite(node.body);
    }

    public Object suite(stmtType[] stmts) throws Exception {
        int n = stmts.length;
        for(int i = 0; i < n; i++) {
            Object exit = visit(stmts[i]);
            //System.out.println("exit: "+exit+", "+n+", "+(exit != null));
            if (exit != null)
                return Exit;
        }
        return null;
    }

    public Object visitBoolOp(BoolOp node) throws Exception {
        Label end = code.getLabel();
        visit(node.values[0]);
        for (int i = 1; i < node.values.length; i++) {
            code.dup();
            if (mrefs.nonzero == 0) {
                mrefs.nonzero = code.pool.Methodref("org/python/core/PyObject",
                                                    "__nonzero__", "()Z");
            }
            code.invokevirtual(mrefs.nonzero);
            switch (node.op) {
            case BoolOp.Or : 
                code.ifne(end);
                break;
            case BoolOp.And : 
                code.ifeq(end);
                break;
            }
            code.pop();
            visit(node.values[i]);
        }
        end.setPosition();
        return null;
    }


    public Object visitCompare(Compare node) throws Exception {
        int tmp1 = code.getLocal();
        int tmp2 = code.getLocal();
        int op;

        if (mrefs.nonzero == 0) {
            mrefs.nonzero = code.pool.Methodref("org/python/core/PyObject",
                                                "__nonzero__", "()Z");
        }

        Label end = code.getLabel();

        visit(node.left);

        int n = node.ops.length;
        for(int i = 0; i < n - 1; i++) {
            visit(node.comparators[i]);
            code.dup();
            code.astore(tmp1);
            code.invokevirtual(make_cmpop(node.ops[i]));
            code.dup();
            code.astore(tmp2);
            code.invokevirtual(mrefs.nonzero);
            code.ifeq(end);
            code.aload(tmp1);
        }

        visit(node.comparators[n-1]);
        code.invokevirtual(make_cmpop(node.ops[n-1]));

        if (n > 1) {
            code.astore(tmp2);
            end.setPosition();
            code.aload(tmp2);
        }
        code.freeLocal(tmp1);
        code.freeLocal(tmp2);
        return null;
    }

    int[] compare_ops = new int[11];
    public int make_cmpop(int op) throws Exception {
        if (compare_ops[op] == 0) {
            String name = null;
            switch (op) {
            case Compare.Eq:    name = "_eq"; break;
            case Compare.NotEq: name = "_ne"; break;
            case Compare.Lt:    name = "_lt"; break;
            case Compare.LtE:   name = "_le"; break;
            case Compare.Gt:    name = "_gt"; break;
            case Compare.GtE:   name = "_ge"; break;
            case Compare.Is:    name = "_is"; break;
            case Compare.IsNot: name = "_isnot"; break;
            case Compare.In:    name = "_in"; break;
            case Compare.NotIn: name = "_notin"; break;
            }
            compare_ops[op] = code.pool.Methodref(
                "org/python/core/PyObject", name,
                "(" + $pyObj + ")" + $pyObj);
        }
        return compare_ops[op];
    }

    static String[] bin_methods = new String[] {
        null,
        "_add",
        "_sub",
        "_mul",
        "_div",
        "_mod",
        "_pow",
        "_lshift",
        "_rshift",
        "_or",
        "_xor",
        "_and",
        "_floordiv",
    };

    int[] bin_ops = new int[13];
    public int make_binop(int op) throws Exception {
        if (bin_ops[op] == 0) {
            String name = bin_methods[op];
            if (op == BinOp.Div && module.getFutures().areDivisionOn()) {
                name = "_truediv";
            }
            bin_ops[op] = code.pool.Methodref(
                "org/python/core/PyObject", name,
                "(" + $pyObj + ")" + $pyObj);
        }
        return bin_ops[op];
    }

    public Object visitBinOp(BinOp node) throws Exception {
        visit(node.left);
        visit(node.right);
        code.invokevirtual(make_binop(node.op));
        return null;
    }

    
    static String[] unary_methods = new String[] {
        null,
        "__invert__",
        "__not__",
        "__pos__",
        "__neg__",
    };

    int[] unary_ops = new int[unary_methods.length];
    public int make_unaryop(int op) throws Exception {
        if (unary_ops[op] == 0) {
            String name = unary_methods[op];
            unary_ops[op] = code.pool.Methodref(
                "org/python/core/PyObject", name, "()" + $pyObj);
        }
        return unary_ops[op];
    }

    public Object visitUnaryOp(UnaryOp node) throws Exception {
        visit(node.operand);
        code.invokevirtual(make_unaryop(node.op));
        return null;
    }


    static String[] aug_methods = new String[] {
        null,
        "__iadd__",
        "__isub__",
        "__imul__",
        "__idiv__",
        "__imod__",
        "__ipow__",
        "__ilshift__",
        "__irshift__",
        "__ior__",
        "__ixor__",
        "__iand__",
        "__ifloordiv__",
    };

    int[] augbin_ops = new int[aug_methods.length];
    public int make_augbinop(int op) throws Exception {
        if (augbin_ops[op] == 0) {
            String name = aug_methods[op];
            if (op == BinOp.Div && module.getFutures().areDivisionOn()) {
                name = "__itruediv__";
            }
            augbin_ops[op] = code.pool.Methodref(
                "org/python/core/PyObject", name,
                "(" + $pyObj + ")" + $pyObj);
        }
        return augbin_ops[op];
    }

    public Object visitAugAssign(AugAssign node) throws Exception {
        visit(node.value);
        int tmp = storeTop();

        augmode = expr_contextType.Load;
        visit(node.target);

        code.aload(tmp);
        code.invokevirtual(make_augbinop(node.op));
        code.freeLocal(tmp);

        temporary = storeTop();
        augmode = expr_contextType.Store;
        visit(node.target);

        return null;
    }


    public static void makeStrings(Code c, String[] names, int n)
        throws IOException
    {
        c.iconst(n);
        c.anewarray(c.pool.Class("java/lang/String"));
        int strings = c.getLocal();
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

    public int invokea0, invokea1, invokea2;
    public int invoke2;
    public Object Invoke(Attribute node, SimpleNode[] values)
        throws Exception
    {
        String name = getName(node.attr);
        visit(node.value);
        code.ldc(name);

        //System.out.println("invoke: "+name+": "+values.length);

        switch (values.length) {
        case 0:
            if (mrefs.invokea0 == 0) {
                mrefs.invokea0 = code.pool.Methodref(
                    "org/python/core/PyObject", "invoke",
                    "(" + $str + ")" + $pyObj);
            }
            code.invokevirtual(mrefs.invokea0);
            break;
        case 1:
            if (mrefs.invokea1 == 0) {
                mrefs.invokea1 = code.pool.Methodref(
                    "org/python/core/PyObject", "invoke",
                    "(" + $str + $pyObj + ")" + $pyObj);
            }
            visit(values[0]);
            code.invokevirtual(mrefs.invokea1);
            break;
        case 2:
            if (mrefs.invokea2 == 0) {
                mrefs.invokea2 = code.pool.Methodref(
                    "org/python/core/PyObject", "invoke",
                    "(" + $str + $pyObj + $pyObj + ")" + $pyObj);
            }
            visit(values[0]);
            visit(values[1]);
            code.invokevirtual(mrefs.invokea2);
            break;
        default:
            makeArray(values);
            if (mrefs.invoke2 == 0) {
                mrefs.invoke2 = code.pool.Methodref(
                    "org/python/core/PyObject", "invoke",
                    "(" + $str + $pyObjArr + ")" + $pyObj);
            }
            code.invokevirtual(mrefs.invoke2);
            break;
        }

        return null;
    }


    public int callextra;
    public int call1, call2;
    public int calla0, calla1, calla2, calla3, calla4;
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

        // Detect a method invocation with no keywords
        if (node.keywords == null && node.starargs == null &&
            node.kwargs == null && node.func instanceof Attribute)
        {
            return Invoke((Attribute) node.func, values);
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

            if (mrefs.callextra == 0) {
                mrefs.callextra = code.pool.Methodref(
                    "org/python/core/PyObject", "_callextra",
                    "(" + $pyObjArr + $strArr + $pyObj + $pyObj + ")" +
                    $pyObj);
            }
            code.invokevirtual(mrefs.callextra);
        } else if (keys.length > 0) {
            makeArray(values);
            makeStrings(code, keys, keys.length);

            if (mrefs.call1 == 0) {
                mrefs.call1 = code.pool.Methodref(
                    "org/python/core/PyObject", "__call__",
                    "(" + $pyObjArr + $strArr + ")" + $pyObj);
            }
            code.invokevirtual(mrefs.call1);
        } else {
            switch (values.length) {
            case 0:
                if (mrefs.calla0 == 0) {
                    mrefs.calla0 = code.pool.Methodref(
                        "org/python/core/PyObject", "__call__",
                        "()" + $pyObj);
                }
                code.invokevirtual(mrefs.calla0);
                break;
            case 1:
                if (mrefs.calla1 == 0) {
                    mrefs.calla1 = code.pool.Methodref(
                        "org/python/core/PyObject", "__call__",
                        "(" + $pyObj + ")" + $pyObj);
                }
                visit(values[0]);
                code.invokevirtual(mrefs.calla1);
                break;
            case 2:
                if (mrefs.calla2 == 0) {
                    mrefs.calla2 = code.pool.Methodref(
                        "org/python/core/PyObject", "__call__",
                        "(" + $pyObj + $pyObj + ")" + $pyObj);
                }
                visit(values[0]);
                visit(values[1]);
                code.invokevirtual(mrefs.calla2);
                break;
            case 3:
                if (mrefs.calla3 == 0) {
                    mrefs.calla3 = code.pool.Methodref(
                        "org/python/core/PyObject", "__call__",
                        "(" + $pyObj + $pyObj + $pyObj + ")" + $pyObj);
                }
                visit(values[0]);
                visit(values[1]);
                visit(values[2]);
                code.invokevirtual(mrefs.calla3);
                break;
            case 4:
                if (mrefs.calla4 == 0) {
                    mrefs.calla4 = code.pool.Methodref(
                        "org/python/core/PyObject", "__call__",
                        "(" + $pyObj + $pyObj + $pyObj + $pyObj + ")" +
                            $pyObj);
                }
                visit(values[0]);
                visit(values[1]);
                visit(values[2]);
                visit(values[3]);
                code.invokevirtual(mrefs.calla4);
                break;
            default:
                makeArray(values);
                if (mrefs.call2 == 0) {
                    mrefs.call2 = code.pool.Methodref(
                        "org/python/core/PyObject", "__call__",
                        "(" + $pyObjArr + ")" + $pyObj);
                }
                code.invokevirtual(mrefs.call2);
                break;
            }
        }
        return null;
    }


    public int getslice, setslice, delslice;
    public Object Slice(Subscript node, Slice slice) throws Exception {
        int ctx = node.ctx;
        if (ctx == node.AugStore && augmode == node.Store) {
            restoreAugTmps(node, 4);
            ctx = node.Store;
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

            if (node.ctx == node.AugStore && augmode == node.Load) {
                saveAugTmps(node, 4);
                ctx = node.Load;
            }
        }

        switch (ctx) {
        case Subscript.Del:
            if (mrefs.delslice == 0) {
                mrefs.delslice = code.pool.Methodref(
                    "org/python/core/PyObject", "__delslice__",
                    "(" + $pyObj + $pyObj + $pyObj + ")V");
            }
            code.invokevirtual(mrefs.delslice);
            return null;
        case Subscript.Load:
            if (mrefs.getslice == 0) {
                mrefs.getslice = code.pool.Methodref(
                    "org/python/core/PyObject", "__getslice__",
                    "(" + $pyObj + $pyObj + $pyObj + ")" + $pyObj);
            }
            code.invokevirtual(mrefs.getslice);
            return null;
        case Subscript.Store:
            code.aload(temporary);
            if (mrefs.setslice == 0) {
                mrefs.setslice = code.pool.Methodref(
                    "org/python/core/PyObject", "__setslice__",
                    "(" + $pyObj + $pyObj + $pyObj + $pyObj + ")V");
            }
            code.invokevirtual(mrefs.setslice);
            return null;
        }
        return null;

    }

    public int getitem, delitem, setitem;
    public Object visitSubscript(Subscript node) throws Exception {
        if (node.slice instanceof Slice) {
            return Slice(node, (Slice) node.slice);
        }

        int ctx = node.ctx;
        if (node.ctx == node.AugStore && augmode == node.Store) {
            restoreAugTmps(node, 2);
            ctx = node.Store;
        } else {
            visit(node.value);
            visit(node.slice);

            if (node.ctx == node.AugStore && augmode == node.Load) {
                saveAugTmps(node, 2);
                ctx = node.Load;
            }
        }

        switch (ctx) {
        case Subscript.Del:
            if (mrefs.delitem == 0) {
                mrefs.delitem = code.pool.Methodref(
                    "org/python/core/PyObject", "__delitem__",
                    "(" + $pyObj + ")V");
            }
            code.invokevirtual(mrefs.delitem);
            return null;
        case Subscript.Load:
            if (mrefs.getitem == 0) {
                mrefs.getitem = code.pool.Methodref(
                    "org/python/core/PyObject", "__getitem__",
                    "(" + $pyObj + ")" + $pyObj);
            }
            code.invokevirtual(mrefs.getitem);
            return null;
        case Subscript.Store:
            code.aload(temporary);
            if (mrefs.setitem == 0) {
                mrefs.setitem = code.pool.Methodref(
                    "org/python/core/PyObject", "__setitem__",
                    "(" + $pyObj + $pyObj + ")V");
            }
            code.invokevirtual(mrefs.setitem);
            return null;
        }
        return null;
    }

    public Object visitIndex(Index node) throws Exception {
        traverse(node);
        return null;
    }

    public Object visitExtSlice(ExtSlice node) throws Exception {
        code.new_(code.pool.Class("org/python/core/PyTuple"));
        code.dup();
        makeArray(node.dims);
        if (mrefs.PyTuple_init == 0) {
            mrefs.PyTuple_init = code.pool.Methodref(
                "org/python/core/PyTuple", "<init>",
                "(" + $pyObjArr + ")V");
        }
        code.invokespecial(mrefs.PyTuple_init);
        return null;
    }

    public int getattr, delattr, setattr;
    public Object visitAttribute(Attribute node) throws Exception {

        int ctx = node.ctx;
        if (node.ctx == node.AugStore && augmode == node.Store) {
            restoreAugTmps(node, 2);
            ctx = node.Store;
        } else {
            visit(node.value);
            code.ldc(getName(node.attr));

            if (node.ctx == node.AugStore && augmode == node.Load) {
                saveAugTmps(node, 2);
                ctx = node.Load;
            }
        }

        switch (ctx) {
        case Attribute.Del:
            if (mrefs.delattr == 0) {
                mrefs.delattr = code.pool.Methodref(
                    "org/python/core/PyObject", "__delattr__",
                    "(" + $str + ")V");
            }
            code.invokevirtual(mrefs.delattr);
            return null;
        case Attribute.Load:
            if (mrefs.getattr == 0) {
                mrefs.getattr = code.pool.Methodref(
                    "org/python/core/PyObject", "__getattr__",
                    "(" + $str + ")" + $pyObj);
            }
            code.invokevirtual(mrefs.getattr);
            return null;
        case Attribute.Store:
            code.aload(temporary);
            if (mrefs.setattr == 0) {
                mrefs.setattr = code.pool.Methodref(
                    "org/python/core/PyObject", "__setattr__",
                    "(" + $str + $pyObj + ")V");
            }
            code.invokevirtual(mrefs.setattr);
            return null;
        }
        return null;
    }

    public int getitem2, unpackSequence;
    public Object seqSet(exprType[] nodes) throws Exception {
        if (mrefs.unpackSequence == 0) {
            mrefs.unpackSequence = code.pool.Methodref(
                "org/python/core/Py",
                "unpackSequence",
                "(" + $pyObj + "I)" + $pyObjArr);
        }

        code.aload(temporary);
        code.iconst(nodes.length);
        code.invokestatic(mrefs.unpackSequence);

        int tmp = code.getLocal();
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

    public int PyTuple_init, PyList_init, PyDictionary_init;
    public Object visitTuple(Tuple node) throws Exception {
        /* if (mode ==AUGSET)
            throw new ParseException(
                      "augmented assign to tuple not possible", node); */
        if (node.ctx == node.Store) return seqSet(node.elts);
        if (node.ctx == node.Del) return seqDel(node.elts);

        code.new_(code.pool.Class("org/python/core/PyTuple"));
        code.dup();
        makeArray(node.elts);
        if (mrefs.PyTuple_init == 0) {
            mrefs.PyTuple_init = code.pool.Methodref(
                "org/python/core/PyTuple", "<init>",
                "(" + $pyObjArr + ")V");
        }
        code.invokespecial(mrefs.PyTuple_init);
        return null;
    }

/*
    public Object fplist(SimpleNode node) throws Exception {
        if (mode == SET) return seqSet(node);
        throw new ParseException("in fplist node", node);
    }
*/

    public Object visitList(List node) throws Exception {
        /* if (mode ==AUGSET)
            throw new ParseException(
                      "augmented assign to list not possible", node); */

        if (node.ctx == node.Store) return seqSet(node.elts);
        if (node.ctx == node.Del) return seqDel(node.elts);

        code.new_(code.pool.Class("org/python/core/PyList"));
        code.dup();
        makeArray(node.elts);
        if (mrefs.PyList_init == 0) {
            mrefs.PyList_init = code.pool.Methodref(
                "org/python/core/PyList", "<init>",
                "(" + $pyObjArr + ")V");
        }
        code.invokespecial(mrefs.PyList_init);
        return null;
    }

    int list_comprehension_count = 0;

    public int PyList_init2;
    public Object visitListComp(ListComp node) throws Exception {
        code.new_(code.pool.Class("org/python/core/PyList"));
        code.dup();
        if (mrefs.PyList_init2 == 0) {
            mrefs.PyList_init2 = code.pool.Methodref(
                "org/python/core/PyList", "<init>", "()V");
        }
        code.invokespecial(mrefs.PyList_init2);

        code.dup();

        int tmp_list = storeTop();
        code.aload(tmp_list);
        code.ldc("append");

        if (mrefs.getattr == 0) {
            mrefs.getattr = code.pool.Methodref(
                "org/python/core/PyObject", "__getattr__",
                "(" + $str + ")" + $pyObj);
        }
        code.invokevirtual(mrefs.getattr);
        String tmp_append = "_[" + (++list_comprehension_count) + "]";

        set(new Name(tmp_append, Name.Store, node));

        stmtType n = new Expr(new Call(new Name(tmp_append, Name.Load, node), 
                                       new exprType[] { node.target },
                                       new keywordType[0], null, null, node),
                                            node);

        for (int i = node.generators.length - 1; i >= 0; i--) {
            listcompType lc = node.generators[i];
            for (int j = lc.ifs.length - 1; j >= 0; j--) {
                n = new If(lc.ifs[j], new stmtType[] { n }, null, lc.ifs[j]);
            }
            n = new For(lc.target, lc.iter, new stmtType[] { n }, null, lc);
        }
        visit(n);
        visit(new Delete(new exprType[] { new Name(tmp_append, Name.Del) }));

        return null;
    }

    public Object visitDict(Dict node) throws Exception {
        code.new_(code.pool.Class("org/python/core/PyDictionary"));
        code.dup();
        SimpleNode[] elts = new SimpleNode[node.keys.length * 2];
        for (int i = 0; i < node.keys.length; i++) {
            elts[i * 2] = node.keys[i];
            elts[i * 2 + 1] = node.values[i];
        }
        makeArray(elts);
        if (mrefs.PyDictionary_init == 0) {
            mrefs.PyDictionary_init = code.pool.Methodref(
                "org/python/core/PyDictionary", "<init>",
                "(" + $pyObjArr + ")V");
        }
        code.invokespecial(mrefs.PyDictionary_init);
        return null;
    }

    public Object visitRepr(Repr node) throws Exception {
        visit(node.value);
        code.invokevirtual("org/python/core/PyObject", "__repr__",
                           "()" + $pyStr);
        return null;
    }

    public int PyFunction_init1,PyFunction_closure_init1;
    public Object visitLambda(Lambda node) throws Exception {
        String name = "<lambda>";

        //Add a return node onto the outside of suite;
        modType retSuite = new Suite(new stmtType[] {
            new Return(node.body, node) }, node);

        setline(node);

        code.new_(code.pool.Class("org/python/core/PyFunction"));
        code.dup();
        loadFrame();
        if (mrefs.f_globals == 0) {
            mrefs.f_globals = code.pool.Fieldref("org/python/core/PyFrame",
                                                 "f_globals", $pyObj);
        }
        code.getfield(mrefs.f_globals);

        ScopeInfo scope = module.getScopeInfo(node);

        makeArray(scope.ac.getDefaults());

        scope.setup_closure(my_scope);
        scope.dump();
        module.PyCode(retSuite, name, true, className,
                      false, false, node.beginLine, scope).get(code);
        Vector freenames = scope.freevars;

        if (!makeClosure(freenames)) {
            if (mrefs.PyFunction_init1 == 0) {
                mrefs.PyFunction_init1 = code.pool.Methodref(
                "org/python/core/PyFunction", "<init>",
                "(" + $pyObj + $pyObjArr + $pyCode + ")V");
            }
            code.invokespecial(mrefs.PyFunction_init1);
        } else {
            if (mrefs.PyFunction_closure_init1 == 0) {
                mrefs.PyFunction_closure_init1 = code.pool.Methodref(
                "org/python/core/PyFunction", "<init>",
                "(" + $pyObj + $pyObjArr + $pyCode + $pyObjArr + ")V");
            }
            code.invokespecial(mrefs.PyFunction_closure_init1);
        }

        return null;
    }


    public int Ellipsis;
    public Object visitEllipsis(Ellipsis node) throws Exception {
        if (mrefs.Ellipsis == 0) {
            mrefs.Ellipsis = code.pool.Fieldref(
                "org/python/core/Py", "Ellipsis",
                "Lorg/python/core/PyObject;");
        }
        code.getstatic(mrefs.Ellipsis);
        return null;
    }

    public int PySlice_init;
    public Object visitSlice(Slice node) throws Exception {
        code.new_(code.pool.Class("org/python/core/PySlice"));
        code.dup();
        if (node.lower == null) getNone(); else visit(node.lower);
        if (node.upper == null) getNone(); else visit(node.upper);
        if (node.step == null) getNone(); else visit(node.step);
        if (mrefs.PySlice_init == 0) {
            mrefs.PySlice_init = code.pool.Methodref(
                "org/python/core/PySlice", "<init>",
                "(" + $pyObj + $pyObj + $pyObj + ")V");
        }
        code.invokespecial(mrefs.PySlice_init);
        return null;
    }

    public int makeClass,makeClass_closure;
    public Object visitClassDef(ClassDef node) throws Exception {
        setline(node);

        //Get class name
        String name = getName(node.name);
        //System.out.println("name: "+name);
        code.ldc(name);

        makeArray(node.bases);

        ScopeInfo scope = module.getScopeInfo(node);

        scope.setup_closure(my_scope);
        scope.dump();
        //Make code object out of suite
        module.PyCode(new Suite(node.body, node), name, false, name,
                      true, false, node.beginLine, scope).get(code);
        Vector freenames = scope.freevars;

        //Get doc string (if there)
        getDocString(node.body);

        //Make class out of name, bases, and code
        if (!makeClosure(freenames)) {
            if (mrefs.makeClass == 0) {
                mrefs.makeClass = code.pool.Methodref(
                "org/python/core/Py", "makeClass",
                "(" + $str + $pyObjArr + $pyCode + $pyObj + ")" + $pyObj);
            }
            code.invokestatic(mrefs.makeClass);
        } else {
            if (mrefs.makeClass_closure == 0) {
                mrefs.makeClass_closure = code.pool.Methodref(
                "org/python/core/Py", "makeClass",
                "(" + $str + $pyObjArr + $pyCode + $pyObj + $pyObjArr + ")" +
                    $pyObj);
            }
            code.invokestatic(mrefs.makeClass_closure);
        }

        //Assign this new class to the given name
        set(new Name(node.name, Name.Store, node));
        return null;
    }

    public Object visitNum(Num node) throws Exception {
        if (node.n instanceof PyInteger) {
            module.PyInteger(((PyInteger) node.n).getValue()).get(code);
        } else if (node.n instanceof PyLong) {
            module.PyLong(node.n.__str__().toString()).get(code);
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
            return "_"+className+name;
        }
        return name;
    }

    int getglobal, getlocal1, getlocal2;
    int setglobal, setlocal1, setlocal2;
    int delglobal, dellocal1, dellocal2;
    int getderef,setderef;

    void emitGetGlobal(String name) throws Exception {
        code.ldc(name);
        if (mrefs.getglobal == 0) {
            mrefs.getglobal = code.pool.Methodref(
            "org/python/core/PyFrame", "getglobal",
            "(" + $str + ")" + $pyObj);
        }
        code.invokevirtual(mrefs.getglobal);
    }

    public Object visitName(Name node) throws Exception {
        String name;
        if (fast_locals)
            name = node.id;
        else
            name = getName(node.id);

        SymInfo syminf = (SymInfo)tbl.get(name);

        int ctx = node.ctx;
        if (ctx == node.AugStore) {
            ctx = augmode;
        }        

        switch (ctx) {
        case Name.Load:
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
                        if (mrefs.getderef == 0) {
                            mrefs.getderef = code.pool.Methodref(
                            "org/python/core/PyFrame", "getderef",
                            "(I)" + $pyObj);
                        }
                        code.invokevirtual(mrefs.getderef);
                        return null;
                    }
                    if ((flags&ScopeInfo.BOUND) != 0) {
                        code.iconst(syminf.locals_index);
                        if (mrefs.getlocal2 == 0) {
                            mrefs.getlocal2 = code.pool.Methodref(
                            "org/python/core/PyFrame", "getlocal",
                            "(I)" + $pyObj);
                        }
                        code.invokevirtual(mrefs.getlocal2);
                        return null;
                    }
                }
                if ((flags&ScopeInfo.FREE) != 0 &&
                            (flags&ScopeInfo.BOUND) == 0) {
                    code.iconst(syminf.env_index);
                    if (mrefs.getderef == 0) {
                        mrefs.getderef = code.pool.Methodref(
                        "org/python/core/PyFrame", "getderef",
                        "(I)" + $pyObj);
                    }
                    code.invokevirtual(mrefs.getderef);
                    return null;
                }
            }
            code.ldc(name);
            if (mrefs.getlocal1 == 0) {
                mrefs.getlocal1 = code.pool.Methodref(
                    "org/python/core/PyFrame", "getname",
                    "(" + $str + ")" + $pyObj);
            }
            code.invokevirtual(mrefs.getlocal1);
            return null;

        case Name.Store:
            loadFrame();
            if (syminf != null && (syminf.flags&ScopeInfo.GLOBAL) != 0) {
                code.ldc(name);
                code.aload(temporary);
                if (mrefs.setglobal == 0) {
                    mrefs.setglobal = code.pool.Methodref(
                        "org/python/core/PyFrame", "setglobal",
                        "(" + $str + $pyObj + ")V");
                }
                code.invokevirtual(mrefs.setglobal);
            } else {
                if (!fast_locals) {
                    code.ldc(name);
                    code.aload(temporary);
                    if (mrefs.setlocal1 == 0) {
                        mrefs.setlocal1 = code.pool.Methodref(
                            "org/python/core/PyFrame", "setlocal",
                            "(" + $str + $pyObj + ")V");
                    }
                    code.invokevirtual(mrefs.setlocal1);
                } else {
                    if (syminf == null) {
                        System.err.println("internal compiler error: "+node);
                    }
                    if ((syminf.flags&ScopeInfo.CELL) != 0) {
                        code.iconst(syminf.env_index);
                        code.aload(temporary);
                        if (mrefs.setderef == 0) {
                            mrefs.setderef = code.pool.Methodref(
                            "org/python/core/PyFrame", "setderef",
                            "(I" + $pyObj + ")V");
                        }
                        code.invokevirtual(mrefs.setderef);
                    } else {
                        code.iconst(syminf.locals_index);
                        code.aload(temporary);
                        if (mrefs.setlocal2 == 0) {
                            mrefs.setlocal2 = code.pool.Methodref(
                            "org/python/core/PyFrame", "setlocal",
                            "(I" + $pyObj + ")V");
                        }
                        code.invokevirtual(mrefs.setlocal2);
                    }
                }
            }
            return null;
        case Name.Del: {
            loadFrame();
            if (syminf != null && (syminf.flags&ScopeInfo.GLOBAL) != 0) {
                code.ldc(name);
                if (mrefs.delglobal == 0) {
                    mrefs.delglobal = code.pool.Methodref(
                        "org/python/core/PyFrame", "delglobal",
                        "(" + $str + ")V");
                }
                code.invokevirtual(mrefs.delglobal);
            } else {
                if (!fast_locals) {
                    code.ldc(name);
                    if (mrefs.dellocal1 == 0) {
                        mrefs.dellocal1 = code.pool.Methodref(
                            "org/python/core/PyFrame", "dellocal",
                            "(" + $str + ")V");
                    }
                    code.invokevirtual(mrefs.dellocal1);
                } else {
                    if (syminf == null) {
                        System.err.println("internal compiler error: "+node);
                    }
                    if ((syminf.flags&ScopeInfo.CELL) != 0) {
                        module.error("can not delete variable '"+name+
                              "' referenced in nested scope",true,node);
                    }
                    code.iconst(syminf.locals_index);
                    if (mrefs.dellocal2 == 0) {
                        mrefs.dellocal2 = code.pool.Methodref(
                            "org/python/core/PyFrame", "dellocal",
                            "(I)V");
                    }
                    code.invokevirtual(mrefs.dellocal2);
                }
            }
            return null; }
        }
        return null;
    }

    public Object visitStr(Str node) throws Exception {
        String s = node.s;
        if (s.length() > 32767) {
            throw new ParseException(
                "string constant too large (more than 32767 characters)",
                node);
        }
        module.PyString(s).get(code);
        return null;
    }

    protected Object unhandled_node(SimpleNode node) throws Exception {
        throw new Exception("Unhandled node " + node);
    }
}
