// Copyright © Corporation for National Research Initiatives

package org.python.compiler;

import org.python.parser.*;
import org.python.core.Py;
import org.python.core.PyObject;
import org.python.core.PyException;
import org.python.core.CompilerFlags;
import java.io.IOException;
import java.util.Stack;
import java.util.Hashtable;
import java.util.Vector;

public class CodeCompiler extends Visitor implements CompilationContext
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

    int mode;
    int temporary;

    public boolean fast_locals, print_results;
    
    public Future futures;
    public Hashtable tbl;
    public ScopeInfo my_scope;
    
    public Future getFutures() { return futures; }
    public String getFilename() { return module.sfilename; }

    
    boolean optimizeGlobals = true;
    public Vector names;
    public String className;

    public Stack continueLabels, breakLabels, finallyLabels;
    public Stack listComprehensionExprs, listComprehensionAppends;

    public CodeCompiler(Module module, boolean print_results) {
        this.module = module;
        mrefs = this;
        pool = module.classfile.pool;

        continueLabels = new Stack();
        breakLabels = new Stack();
        finallyLabels = new Stack();
        listComprehensionExprs = new Stack();
        listComprehensionAppends = new Stack();
        this.print_results = print_results;
    }

    public int PyNone;
    public void getNone() throws IOException {
        if (mrefs.PyNone == 0) {
            mrefs.PyNone = pool.Fieldref("org/python/core/Py", "None",
                                         "Lorg/python/core/PyObject;");
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
        code.freeLocal(tmp);
    }


    public void set(SimpleNode node, int tmp) throws Exception {
        //System.out.println("tmp: "+tmp);
        if (mode == SET) {
            //System.out.println("recurse set: "+tmp+", "+temporary+", "+code.getLocal()+", "+code.getLocal());
            int old_tmp = temporary;
            temporary = tmp;
            node.visit(this);
            temporary = old_tmp;
        } else {
            temporary = tmp;
            mode = SET;
            node.visit(this);
            mode = GET;
        }
    }


    private void saveAugTmps(SimpleNode node, int count) throws Exception {
        if (count >= 4) {
            node.aug_tmp4 = code.getLocal();
            code.astore(node.aug_tmp4);
        }
        if (count >= 3) {
            node.aug_tmp3 = code.getLocal();
            code.astore(node.aug_tmp3);
        }
        if (count >= 2) {
            node.aug_tmp2 = code.getLocal();
            code.astore(node.aug_tmp2);
        }
        node.aug_tmp1 = code.getLocal();
        code.astore(node.aug_tmp1);

        code.aload(node.aug_tmp1);
        if (count >= 2)
            code.aload(node.aug_tmp2);
        if (count >= 3)
            code.aload(node.aug_tmp3);
        if (count >= 4)
            code.aload(node.aug_tmp4);
    }


    private void restoreAugTmps(SimpleNode node, int count) throws Exception {
       code.aload(node.aug_tmp1);
       code.freeLocal(node.aug_tmp1);
       if (count == 1)
           return;
       code.aload(node.aug_tmp2);
       code.freeLocal(node.aug_tmp2);
       if (count == 2)
           return;
       code.aload(node.aug_tmp3);
       code.freeLocal(node.aug_tmp3);
       if (count == 3)
           return;
       code.aload(node.aug_tmp4);
       code.freeLocal(node.aug_tmp4);
       if (count == 4)
           return;
   }


    public void error(String msg,boolean err,SimpleNode node) throws Exception {
        if (!err) {
            try {
                Py.warning(Py.SyntaxWarning,msg,(module.sfilename!=null)?module.sfilename:"?",
                           node.beginLine,null,Py.None);
                return;
            } catch(PyException e) {
                if (!Py.matchException(e,Py.SyntaxWarning)) throw e;
            }
        }
        throw new ParseException(msg,node);
    }

    public void parse(SimpleNode node, Code code,
                      boolean fast_locals, String className,
                      boolean classBody, ScopeInfo scope,CompilerFlags cflags)
        throws Exception
    {
        this.fast_locals = fast_locals;
        this.className = className;
        this.code = code;

        if (scope == null) {
            futures = new Future();
            futures.preprocessFutures(node,cflags);
            new ScopesCompiler(this).parse(node);
            scope = node.scope;
        }
        
        my_scope = scope;
        names = scope.names;

        tbl = scope.tbl;
        optimizeGlobals = fast_locals&&!scope.exec&&!scope.from_import_star;

        mode = GET;
        Object exit = node.visit(this);
        //System.out.println("exit: "+exit+", "+(exit==null));

        if (classBody) {
            loadFrame();
            code.invokevirtual("org/python/core/PyFrame", "getf_locals",
            "()Lorg/python/core/PyObject;");
            code.areturn();
        } else {
            if (exit == null) {
                //System.out.println("no exit");
                getNone();
                code.areturn();
            }
        }
    }

    public Object single_input(SimpleNode node) throws Exception {
        return suite(node);
        /*
          if (node.getNumChildren() == 1 &&
          node.getChild(0).id == PythonGrammarTreeConstants.JJTEXPR_STMT &&
          node.getChild(0).getNumChildren() == 1) {
          //System.out.println("returning: "+node.getChild(0).getChild(0));
          return return_stmt(node.getChild(0));
          } else {
          //System.out.println("suite");
          return suite(node);
          }
        */
    }

    public Object file_input(SimpleNode suite) throws Exception {
        if (mrefs.setglobal == 0) {
            mrefs.setglobal = code.pool.Methodref(
                "org/python/core/PyFrame", "setglobal",
                "(Ljava/lang/String;Lorg/python/core/PyObject;)V");
        }

        if (suite.getNumChildren() > 0 &&
            suite.getChild(0).id == PythonGrammarTreeConstants.JJTEXPR_STMT &&
            suite.getChild(0).getChild(0).id == PythonGrammarTreeConstants.JJTSTRING) 
        {
            loadFrame();
            code.ldc("__doc__");
            suite.getChild(0).getChild(0).visit(this);

            code.invokevirtual(mrefs.setglobal);
        }
        if (module.setFile) {
            loadFrame();
            code.ldc("__file__");
            module.filename.get(code);
            code.invokevirtual(mrefs.setglobal);
        }
        return suite(suite);
    }

    public Object eval_input(SimpleNode node) throws Exception {
        return return_stmt(node, true);
    }

    public int EmptyObjects;
    public void makeArray(SimpleNode[] nodes) throws Exception {
        int n;

        if (nodes == null)
            n = 0;
        else
            n = nodes.length;

        if (n > 0 && nodes[n-1].id == PythonGrammarTreeConstants.JJTCOMMA) {
            n -= 1;
        }

        if (n == 0) {
            if (mrefs.EmptyObjects == 0) {
                mrefs.EmptyObjects = code.pool.Fieldref(
                    "org/python/core/Py", "EmptyObjects",
                    "[Lorg/python/core/PyObject;");
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
                nodes[i].visit(this);
                code.aastore();
            }
            code.aload(tmp);
            code.freeLocal(tmp);
        }
    }

    public void getDocString(SimpleNode suite) throws Exception {
        //System.out.println("doc: "+suite.getChild(0));
        if (suite.getNumChildren() > 0 &&
            suite.getChild(0).id == PythonGrammarTreeConstants.JJTEXPR_STMT &&
            suite.getChild(0).getChild(0).id == PythonGrammarTreeConstants.JJTSTRING)
        {
            suite.getChild(0).getChild(0).visit(this);
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
            "org/python/core/PyFrame", "getclosure",
            "(I)Lorg/python/core/PyObject;");
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

    public Object funcdef(SimpleNode node) throws Exception {
        String name = getName(node.getChild(0));
        SimpleNode suite;
        if (node.getNumChildren() == 3) {
            suite = node.getChild(2);
            //Parse arguments
        } else {
            suite = node.getChild(1);
        }

        setline(node);

        code.new_(code.pool.Class("org/python/core/PyFunction"));
        code.dup();
        loadFrame();
        if (mrefs.f_globals == 0) {
            mrefs.f_globals = code.pool.Fieldref("org/python/core/PyFrame",
                                                 "f_globals",
                                                 "Lorg/python/core/PyObject;");
        }
        code.getfield(mrefs.f_globals);

        makeArray(node.scope.ac.getDefaults());
        
        node.scope.setup_closure(my_scope);
        node.scope.dump();
        module.PyCode(suite, name, true, className, false, false,
                      node.beginLine, node.scope).get(code);
        Vector freenames = node.scope.freevars;
        node.scope = null; // release scope info
        
        getDocString(suite);

        if (!makeClosure(freenames)) {
            if (mrefs.PyFunction_init == 0) {
                mrefs.PyFunction_init = code.pool.Methodref(
                "org/python/core/PyFunction", "<init>",
                "(Lorg/python/core/PyObject;[Lorg/python/core/PyObject;Lorg/python/core/PyCode;Lorg/python/core/PyObject;)V");
            }
            code.invokespecial(mrefs.PyFunction_init);
        } else {
            if (mrefs.PyFunction_closure_init == 0) {
                mrefs.PyFunction_closure_init = code.pool.Methodref(
                "org/python/core/PyFunction", "<init>",
                "(Lorg/python/core/PyObject;[Lorg/python/core/PyObject;Lorg/python/core/PyCode;Lorg/python/core/PyObject;[Lorg/python/core/PyObject;)V");
            }
            code.invokespecial(mrefs.PyFunction_closure_init);

        }

        set(node.getChild(0));
        return null;
    }

    public int printResult;

    public Object expr_stmt(SimpleNode node) throws Exception {
        setline(node);
        int n = node.getNumChildren();
        if (n == 1 && 
              node.getChild(0).id >= PythonGrammarTreeConstants.JJTAUG_PLUS &&
              node.getChild(0).id <= PythonGrammarTreeConstants.JJTAUG_POWER) {
           node.getChild(0).visit(this);
           return null;
        }

        node.getChild(n-1).visit(this);
        if (n == 1) {
            if (print_results) {
                if (mrefs.printResult == 0) {
                    mrefs.printResult = code.pool.Methodref(
                        "org/python/core/Py",
                        "printResult", "(Lorg/python/core/PyObject;)V");
                }
                code.invokestatic(mrefs.printResult);
            } else {
                code.pop();
            }
            return null;
        }
        if (n == 2) {
            set(node.getChild(0));
            return null;
        }
        int tmp = storeTop();
        for (int i=n-2; i>=0; i--) {
            set(node.getChild(i), tmp);
        }
        code.freeLocal(tmp);
        return null;
    }

    public int print1, print2, print3, print4, print5, print6;

    public Object print_ext(SimpleNode node) throws Exception {
        // There better be exactly one child
        node.getChild(0).visit(this);
        return null;
    }

    public Object print_stmt(SimpleNode node) throws Exception {
        setline(node);
        int n = node.getNumChildren();
        // Extended print statement assumes there's at least one child
        // node, otherwise a syntax error should have been raised.
        int tmp = -1;
        int printcomma, printlnv, println;
        int i = 0, nochildren = 0;
        boolean printext = false;

        if (node.getNumChildren() > 0 &&
            node.getChild(0).id == PythonGrammarTreeConstants.JJTPRINT_EXT)
        {
            printext = true;
            node.getChild(0).visit(this);
            tmp = storeTop();
            i = 1;
            nochildren = 1;
            if (mrefs.print4 == 0) {
                mrefs.print4 = pool.Methodref(
                    "org/python/core/Py", "printComma",
                    "(Lorg/python/core/PyObject;Lorg/python/core/PyObject;)V");
            }
            printcomma = mrefs.print4;
            if (mrefs.print5 == 0) {
                mrefs.print5 = pool.Methodref(
                    "org/python/core/Py", "println",
                    "(Lorg/python/core/PyObject;Lorg/python/core/PyObject;)V");
            }
            println = mrefs.print5;
            if (mrefs.print6 == 0) {
                mrefs.print6 = pool.Methodref(
                    "org/python/core/Py", "printlnv",
                    "(Lorg/python/core/PyObject;)V");
            }
            printlnv = mrefs.print6;
        }
        else {
            if (mrefs.print1 == 0) {
                mrefs.print1 = pool.Methodref(
                    "org/python/core/Py", "printComma",
                    "(Lorg/python/core/PyObject;)V");
            }
            printcomma = mrefs.print1;
            if (mrefs.print2 == 0) {
                mrefs.print2 = pool.Methodref(
                    "org/python/core/Py", "println",
                    "(Lorg/python/core/PyObject;)V");
            }
            println = mrefs.print2;
            if (mrefs.print3 == 0) {
                mrefs.print3 = pool.Methodref(
                    "org/python/core/Py",
                    "println", "()V");
            }
            printlnv = mrefs.print3;
        }
        for (; i < n-1; i++) {
            if (printext)
                code.aload(tmp);
            node.getChild(i).visit(this);
            code.invokestatic(printcomma);
        }
        if (node.getNumChildren() == nochildren) {
            if (printext)
                code.aload(tmp);
            code.invokestatic(printlnv);
        }
        else {
            if (node.getChild(n-1).id != PythonGrammarTreeConstants.JJTCOMMA) {
                if (printext)
                    code.aload(tmp);
                node.getChild(n-1).visit(this);
                code.invokestatic(println);
            }
        }
        if (printext)
            code.freeLocal(tmp);
        return null;
    }

    public Object del_stmt(SimpleNode node) throws Exception {
        setline(node);
        mode = DEL;
        node.getChild(0).visit(this);
        mode = GET;
        return null;
    }

    public Object pass_stmt(SimpleNode node) throws Exception {
        setline(node);
        return null;
    }

    public Object break_stmt(SimpleNode node) throws Exception {
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

    public Object continue_stmt(SimpleNode node) throws Exception {
        //setline(node); Not needed here...
        if (continueLabels.empty()) {
            throw new ParseException("'continue' outside loop", node);
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

    public Object return_stmt(SimpleNode node) throws Exception {
        return return_stmt(node, false);
    }
    
    public Object return_stmt(SimpleNode node, boolean inEval)
        throws Exception
    {
        setline(node);
        if (!inEval && !fast_locals) {
            throw new ParseException("'return' outside function", node);
        }
        if (node.getNumChildren() == 1) {
            node.getChild(0).visit(this);
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

    public Object raise_stmt(SimpleNode node) throws Exception {
        setline(node);
        int n = node.getNumChildren();
        for (int i=0; i<n; i++)
            node.getChild(i).visit(this);
        switch(n) {
        case 0:
            if (mrefs.makeException0 == 0) {
                mrefs.makeException0 = code.pool.Methodref(
                    "org/python/core/Py", "makeException",
                    "()Lorg/python/core/PyException;");
            }
            code.invokestatic(mrefs.makeException0);
            break;
        case 1:
            if (mrefs.makeException1 == 0) {
                mrefs.makeException1 = code.pool.Methodref(
                    "org/python/core/Py", "makeException",
                    "(Lorg/python/core/PyObject;)Lorg/python/core/PyException;");
            }
            code.invokestatic(mrefs.makeException1);
            break;
        case 2:
            if (mrefs.makeException2 == 0) {
                mrefs.makeException2 = code.pool.Methodref(
                    "org/python/core/Py", "makeException",
                    "(Lorg/python/core/PyObject;Lorg/python/core/PyObject;)Lorg/python/core/PyException;");
            }
            code.invokestatic(mrefs.makeException2);
            break;
        case 3:
            if (mrefs.makeException3 == 0) {
                mrefs.makeException3 = code.pool.Methodref(
                    "org/python/core/Py", "makeException",
                    "(Lorg/python/core/PyObject;Lorg/python/core/PyObject;Lorg/python/core/PyObject;)Lorg/python/core/PyException;");
            }
            code.invokestatic(mrefs.makeException3);
            break;
        }
        code.athrow();
        return Exit;
    }

    public int importOne, importOneAs;

    public Object Import(SimpleNode node) throws Exception {
        setline(node);
        int n = node.getNumChildren();
        for (int i=0; i<n; i++) {
            SimpleNode cnode = node.getChild(i);
            SimpleNode asnameNode;
            if (cnode.id == PythonGrammarTreeConstants.JJTDOTTED_AS_NAME) {
                String name = (String)cnode.getChild(0).visit(this);
                asnameNode = cnode.getChild(1);
                code.ldc(name);
                loadFrame();
                if (mrefs.importOneAs == 0) {
                     mrefs.importOneAs = code.pool.Methodref(
                        "org/python/core/imp", "importOneAs",
                        "(Ljava/lang/String;Lorg/python/core/PyFrame;)Lorg/python/core/PyObject;");
                }
                code.invokestatic(mrefs.importOneAs);
            } else {
                String name = (String)cnode.visit(this);
                asnameNode = cnode.getChild(0);
                code.ldc(name);
                loadFrame();
                if (mrefs.importOne == 0) {
                    mrefs.importOne = code.pool.Methodref(
                        "org/python/core/imp", "importOne",
                         "(Ljava/lang/String;Lorg/python/core/PyFrame;)Lorg/python/core/PyObject;");
                }
                code.invokestatic(mrefs.importOne);
            }
            
            set(asnameNode);

        }
        return null;
    }


    public int importAll, importFrom;

    public Object ImportFrom(SimpleNode node) throws Exception {
        Future.checkFromFuture(node); // future stmt support
        setline(node);
        String name = (String)node.getChild(0).visit(this);
        code.ldc(name);
        int n = node.getNumChildren();
        if (n > 1) {
            String[] names = new String[n-1];
            SimpleNode[] asnameNodes = new SimpleNode[n-1];
            for (int i=0; i<n-1; i++) {
                SimpleNode cnode = node.getChild(i+1);
                if (cnode.id == PythonGrammarTreeConstants.JJTIMPORT_AS_NAME) {
                    names[i] = (String)cnode.getChild(0).getInfo();
                    asnameNodes[i] = cnode.getChild(1); 
                } else {
                    names[i] = (String)cnode.getInfo();
                    asnameNodes[i] = cnode;
                }
            }
            makeStrings(code, names, names.length);

            loadFrame();
            if (mrefs.importFrom == 0) {
                mrefs.importFrom = code.pool.Methodref(
                    "org/python/core/imp", "importFrom",
                    "(Ljava/lang/String;[Ljava/lang/String;Lorg/python/core/PyFrame;)[Lorg/python/core/PyObject;");
            }
            code.invokestatic(mrefs.importFrom);
            int tmp = storeTop();
            for (int i=0; i<n-1; i++) {
                code.aload(tmp);
                code.iconst(i);
                code.aaload();
                set(asnameNodes[i]);
            }
            code.freeLocal(tmp);
        } else {
            loadFrame();
            if (mrefs.importAll == 0) {
                mrefs.importAll = code.pool.Methodref(
                    "org/python/core/imp", "importAll",
                    "(Ljava/lang/String;Lorg/python/core/PyFrame;)V");
            }
            code.invokestatic(mrefs.importAll);
        }
        return null;
    }

    public Object dotted_name(SimpleNode node) throws Exception {
        int n = node.getNumChildren();
        String name = (String)node.getChild(0).getInfo();
        for (int i=1; i<n; i++) {
            name = name+"."+(String)node.getChild(i).getInfo();
        }
        return name;
    }

    public Object global_stmt(SimpleNode node) throws Exception {
        return null;
    }

    public int exec;
    public Object exec_stmt(SimpleNode node) throws Exception {
        setline(node);
        //do expression
        node.getChild(0).visit(this);

        //get globals if provided
        if (node.getNumChildren() > 1) {
            node.getChild(1).visit(this);
        } else {
            code.aconst_null();
        }

        //get locals if provided
        if (node.getNumChildren() > 2) {
            node.getChild(2).visit(this);
        } else {
            code.aconst_null();
        }

        //do the real work here
        if (mrefs.exec == 0) {
            mrefs.exec = code.pool.Methodref(
                "org/python/core/Py", "exec",
                "(Lorg/python/core/PyObject;Lorg/python/core/PyObject;Lorg/python/core/PyObject;)V");
        }
        code.invokestatic(mrefs.exec);
        return null;
    }

    public int assert1, assert2;
    public Object assert_stmt(SimpleNode node) throws Exception {
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
        node.getChild(0).visit(this);
        if (node.getNumChildren() == 2) {
            node.getChild(1).visit(this);
            if (mrefs.assert2 == 0) {
                mrefs.assert2 = code.pool.Methodref(
                    "org/python/core/Py", "assert",
                    "(Lorg/python/core/PyObject;Lorg/python/core/PyObject;)V");
            }
            code.invokestatic(mrefs.assert2);
        } else {
            if (mrefs.assert1 == 0) {
                mrefs.assert1 = code.pool.Methodref(
                    "org/python/core/Py", "assert",
                    "(Lorg/python/core/PyObject;)V");
            }
            code.invokestatic(mrefs.assert1);
        }
        
        /* And finally set the label for the end of it all */
        end_of_assert.setPosition();
        
        return null;
    }

    public int nonzero;
    public Object doTest(Label end_of_if, SimpleNode node, int index)
        throws Exception
    {
        SimpleNode test = node.getChild(index);
        SimpleNode suite = node.getChild(index+1);
        Label end_of_suite = code.getLabel();

        setline(test);
        test.visit(this);
        if (mrefs.nonzero == 0) {
            mrefs.nonzero = code.pool.Methodref("org/python/core/PyObject",
                                                "__nonzero__", "()Z");
        }
        code.invokevirtual(mrefs.nonzero);
        code.ifeq(end_of_suite);

        Object exit = suite.visit(this);

        if (end_of_if != null && exit == null)
            code.goto_(end_of_if);

        end_of_suite.setPosition();

        int remaining = node.getNumChildren()-index-2;
        if (remaining > 1) {
            return doTest(end_of_if, node, index+2) != null ? exit : null;
        } else {
            if (remaining == 1) {
                return node.getChild(index+2).visit(this) != null
                    ? exit : null;
            } else {
                return null;
            }
        }
    }

    public Object if_stmt(SimpleNode node) throws Exception {
        int n = node.getNumChildren();
        Label end_of_if = null;
        if (n > 2)
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


    public Object while_stmt(SimpleNode node) throws Exception {
        beginLoop();
        Label continue_loop = (Label)continueLabels.peek();
        Label break_loop = (Label)breakLabels.peek();

        Label start_loop = code.getLabel();

        code.goto_(continue_loop);
        start_loop.setPosition();

        //Do suite
        node.getChild(1).visit(this);

        continue_loop.setPosition();
        setline(node);

        //Do test
        node.getChild(0).visit(this);
        if (mrefs.nonzero == 0) {
            mrefs.nonzero = code.pool.Methodref("org/python/core/PyObject",
                                                "__nonzero__", "()Z");
        }
        code.invokevirtual(mrefs.nonzero);
        code.ifne(start_loop);

        finishLoop();

        if (node.getNumChildren() == 3) {
            //Do else
            node.getChild(2).visit(this);
        }
        break_loop.setPosition();

        // Probably need to detect "guaranteed exits"
        return null;
    }

    public int safe_getitem=0;

    public Object for_stmt(SimpleNode node) throws Exception {
        beginLoop();
        Label continue_loop = (Label)continueLabels.peek();
        Label break_loop = (Label)breakLabels.peek();
        Label start_loop = code.getLabel();
        Label next_loop = code.getLabel();

        int list_tmp = code.getLocal();
        int index_tmp = code.getLocal();
        int expr_tmp = code.getLocal();

        setline(node);

        //parse the list
        node.getChild(1).visit(this);
        code.astore(list_tmp);

        //set up the loop counter
        code.iconst(0);
        code.istore(index_tmp);

        //do check at end of loop.  Saves one opcode ;-)
        code.goto_(next_loop);

        start_loop.setPosition();
        //set index variable to current entry in list
        set(node.getChild(0), expr_tmp);

        //evaluate for body
        node.getChild(2).visit(this);

        continue_loop.setPosition();
        //increment counter
        code.iinc(index_tmp, 1);

        next_loop.setPosition();
        setline(node);
        //get the next element from the list
        code.aload(list_tmp);
        code.iload(index_tmp);
        if (mrefs.safe_getitem == 0) {
            mrefs.safe_getitem = code.pool.Methodref(
                "org/python/core/PyObject",
                "__finditem__", "(I)Lorg/python/core/PyObject;");
        }
        code.invokevirtual(mrefs.safe_getitem);
        code.astore(expr_tmp);
        code.aload(expr_tmp);
        //if no more elements then fall through
        code.ifnonnull(start_loop);

        finishLoop();

        if (node.getNumChildren() > 3) {
            //Do else clause if provided
            node.getChild(3).visit(this);
        }

        break_loop.setPosition();

        code.freeLocal(list_tmp);
        code.freeLocal(index_tmp);
        code.freeLocal(expr_tmp);

        // Probably need to detect "guaranteed exits"
        return null;
    }

    public int match_exception;

    public void exceptionTest(int exc, Label end_of_exceptions,
                              SimpleNode node, int index)
        throws Exception
    {
        SimpleNode name = node.getChild(index);
        SimpleNode suite = node.getChild(index+1);

        setline(name);
        Label end_of_self = code.getLabel();

        if (name.getNumChildren() > 0) {
            code.aload(exc);
            //get specific exception
            name.getChild(0).visit(this);
            if (mrefs.match_exception == 0) {
                mrefs.match_exception = code.pool.Methodref(
                    "org/python/core/Py", "matchException",
                    "(Lorg/python/core/PyException;Lorg/python/core/PyObject;)Z");
            }
            code.invokestatic(mrefs.match_exception);
            code.ifeq(end_of_self);
        } else {
            if (node.getNumChildren() > index+3) {
                throw new ParseException(
                    "bare except must be last except clause", name);
            }
        }

        if (name.getNumChildren() > 1) {
            code.aload(exc);
            code.getfield(code.pool.Fieldref("org/python/core/PyException",
                                             "value",
                                             "Lorg/python/core/PyObject;"));
            set(name.getChild(1));
        }

        //do exception body
        suite.visit(this);
        code.goto_(end_of_exceptions);
        end_of_self.setPosition();

        if (node.getNumChildren() > index+3) {
            exceptionTest(exc, end_of_exceptions, node, index+2);
        } else {
            code.aload(exc);
            code.athrow();
        }
    }

    public int add_traceback;
    public Object tryFinally(SimpleNode trySuite, SimpleNode finallySuite)
        throws Exception
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
        ret = trySuite.visit(this);
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
                "(Ljava/lang/Throwable;Lorg/python/core/PyFrame;)V");
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
        
        // Trick the JVM verifier into thinking this code might not be executed
        code.iconst(1);
        code.ifeq(skipSuite);
        
        // The actual finally suite is always executed (since 1 != 0)
        ret = finallySuite.visit(this);
        
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
    public Object try_stmt(SimpleNode node) throws Exception {
        int n = node.getNumChildren();
        if (n == 2) {
            return tryFinally(node.getChild(0), node.getChild(1));
        }

        Label start = code.getLabel();
        Label end = code.getLabel();
        Label handler_start = code.getLabel();
        Label handler_end = code.getLabel();

        start.setPosition();
        //Do suite
        node.getChild(0).visit(this);
        end.setPosition();
        code.goto_(handler_end);

        handler_start.setPosition();
        //Stack has eactly one item at start of handler
        code.stack = 1;

        loadFrame();

        if (mrefs.set_exception == 0) {
            mrefs.set_exception = code.pool.Methodref(
                "org/python/core/Py", "setException",
                "(Ljava/lang/Throwable;Lorg/python/core/PyFrame;)Lorg/python/core/PyException;");
        }
        code.invokestatic(mrefs.set_exception);

        int exc = storeTop();

        if (n % 2 != 0) {
            //No else clause to worry about
            exceptionTest(exc, handler_end, node, 1);
            handler_end.setPosition();
        } else {
            //Have else clause
            Label else_end = code.getLabel();
            exceptionTest(exc, else_end, node, 1);
            handler_end.setPosition();

            //do else clause
            node.getChild(n-1).visit(this);
            else_end.setPosition();
        }

        code.freeLocal(exc);
        code.addExceptionHandler(start, end, handler_start,
                                 code.pool.Class("java/lang/Throwable"));
        return null;
    }

    public Object except_clause(SimpleNode node) throws Exception {
        throw new ParseException("Unhandled Node: "+node, node);
    }

    public Object suite(SimpleNode node) throws Exception {
        int n = node.getNumChildren();
        for(int i=0; i<n; i++) {
            Object exit = node.getChild(i).visit(this);
            //System.out.println("exit: "+exit+", "+n+", "+(exit != null));
            if (exit != null)
                return Exit;
        }
        return null;
    }

    public Object or_boolean(SimpleNode node) throws Exception {
        Label end = code.getLabel();
        node.getChild(0).visit(this);
        code.dup();
        if (mrefs.nonzero == 0) {
            mrefs.nonzero = code.pool.Methodref("org/python/core/PyObject",
                                                "__nonzero__", "()Z");
        }
        code.invokevirtual(mrefs.nonzero);
        code.ifne(end);
        code.pop();
        node.getChild(1).visit(this);
        end.setPosition();
        return null;
    }

    public Object and_boolean(SimpleNode node) throws Exception {
        Label end = code.getLabel();
        node.getChild(0).visit(this);
        code.dup();
        if (mrefs.nonzero == 0) {
            mrefs.nonzero = code.pool.Methodref("org/python/core/PyObject",
                                                "__nonzero__", "()Z");
        }
        code.invokevirtual(mrefs.nonzero);
        code.ifeq(end);
        code.pop();
        node.getChild(1).visit(this);
        end.setPosition();
        return null;
    }

    public Object not_1op(SimpleNode node) throws Exception {
        return unaryop(node, "__not__");
    }

    public Object comparision(SimpleNode node) throws Exception {
        int n = node.getNumChildren();
        int tmp1 = code.getLocal();
        int tmp2 = code.getLocal();

        if (mrefs.nonzero == 0) {
            mrefs.nonzero = code.pool.Methodref("org/python/core/PyObject",
                                                "__nonzero__", "()Z");
        }

        Label end = code.getLabel();

        node.getChild(0).visit(this);

        for(int i=1; i<n-2; i+=2) {
            node.getChild(i+1).visit(this);
            code.dup();
            code.astore(tmp1);
            code.invokevirtual(((Integer)node.getChild(i).visit(this)).intValue());
            code.dup();
            code.astore(tmp2);
            code.invokevirtual(mrefs.nonzero);
            code.ifeq(end);
            code.aload(tmp1);
        }

        node.getChild(n-1).visit(this);
        //System.out.println("node: "+node.getChild(n-2));
        code.invokevirtual(((Integer)node.getChild(n-2).visit(this)).intValue());

        if (n > 3) {
            code.astore(tmp2);
            end.setPosition();
            code.aload(tmp2);
        }
        code.freeLocal(tmp1);
        code.freeLocal(tmp2);
        return null;
    }

    public int make_binop(String name) throws Exception {
        return code.pool.Methodref(
            "org/python/core/PyObject", name,
            "(Lorg/python/core/PyObject;)Lorg/python/core/PyObject;");
    }

    public Integer less;
    public Object less_cmp(SimpleNode node) throws Exception {
        if (mrefs.less == null) less = new Integer(make_binop("_lt"));
        return mrefs.less;
    }

    public Integer greater;
    public Object greater_cmp(SimpleNode node) throws Exception {
        if (mrefs.greater == null) greater = new Integer(make_binop("_gt"));
        return mrefs.greater;
    }

    public Integer equal;
    public Object equal_cmp(SimpleNode node) throws Exception {
        if (mrefs.equal == null) equal = new Integer(make_binop("_eq"));
        return mrefs.equal;
    }

    public Integer less_equal;
    public Object less_equal_cmp(SimpleNode node) throws Exception {
        if (mrefs.less_equal == null) less_equal = new Integer(make_binop("_le"));
        return mrefs.less_equal;
    }

    public Integer greater_equal;
    public Object greater_equal_cmp(SimpleNode node) throws Exception {
        if (mrefs.greater_equal == null) greater_equal = new Integer(make_binop("_ge"));
        return mrefs.greater_equal;
    }

    public Integer notequal;
    public Object notequal_cmp(SimpleNode node) throws Exception {
        if (mrefs.notequal == null) notequal = new Integer(make_binop("_ne"));
        return mrefs.notequal;
    }

    public Integer in;
    public Object in_cmp(SimpleNode node) throws Exception {
        if (mrefs.in == null) in = new Integer(make_binop("_in"));
        return mrefs.in;
    }

    public Integer not_in;
    public Object not_in_cmp(SimpleNode node) throws Exception {
        if (mrefs.not_in == null) not_in = new Integer(make_binop("_notin"));
        return mrefs.not_in;
    }

    public Integer is;
    public Object is_cmp(SimpleNode node) throws Exception {
        if (mrefs.is == null) is = new Integer(make_binop("_is"));
        return mrefs.is;
    }

    public Integer is_not;
    public Object is_not_cmp(SimpleNode node) throws Exception {
        if (mrefs.is_not == null) is_not = new Integer(make_binop("_isnot"));
        return mrefs.is_not;
    }

    //Must handle caching of methods better here...
    public Object binaryop(SimpleNode node, String name) throws Exception {
        node.getChild(0).visit(this);
        node.getChild(1).visit(this);
        code.invokevirtual("org/python/core/PyObject", name,
                           "(Lorg/python/core/PyObject;)Lorg/python/core/PyObject;");
        return null;
    }

    public Object unaryop(SimpleNode node, String name) throws Exception {
        node.getChild(0).visit(this);
        code.invokevirtual("org/python/core/PyObject", name,
                           "()Lorg/python/core/PyObject;");
        return null;
    }

    public Object or_2op(SimpleNode node) throws Exception {
        return binaryop(node, "_or");
    }

    public Object xor_2op(SimpleNode node) throws Exception {
        return binaryop(node, "_xor");
    }

    public Object and_2op(SimpleNode node) throws Exception {
        return binaryop(node, "_and");
    }

    public Object lshift_2op(SimpleNode node) throws Exception {
        return binaryop(node, "_lshift");
    }

    public Object rshift_2op(SimpleNode node) throws Exception {
        return binaryop(node, "_rshift");
    }

    public Object add_2op(SimpleNode node) throws Exception {
        return binaryop(node, "_add");
    }

    public Object strjoin(SimpleNode node) throws Exception {
        return binaryop(node, "__add__");
    }

    public Object sub_2op(SimpleNode node) throws Exception {
        return binaryop(node, "_sub");
    }

    public Object mul_2op(SimpleNode node) throws Exception {
        return binaryop(node, "_mul");
    }

    public Object div_2op(SimpleNode node) throws Exception {
        return binaryop(node, "_div");
    }

    public Object mod_2op(SimpleNode node) throws Exception {
        return binaryop(node, "_mod");
    }

    public Object pos_1op(SimpleNode node) throws Exception {
        return unaryop(node, "__pos__");
    }

    public Object neg_1op(SimpleNode node) throws Exception {
        return unaryop(node, "__neg__");
    }

    public Object invert_1op(SimpleNode node) throws Exception {
        return unaryop(node, "__invert__");
    }

    public Object pow_2op(SimpleNode node) throws Exception {
        return binaryop(node, "_pow");
    }


    public Object aug_binaryop(SimpleNode node, String name) throws Exception {
        //node.dump("aug_binaryop:");
        node.getChild(1).visit(this);
        int tmp = storeTop();

        mode = AUGGET;
        node.getChild(0).visit(this);

        code.aload(tmp);

        code.invokevirtual("org/python/core/PyObject", name,
                 "(Lorg/python/core/PyObject;)Lorg/python/core/PyObject;");
        code.freeLocal(tmp);

        temporary = storeTop();
        mode = AUGSET;
        node.getChild(0).visit(this);
        mode = GET;

        return null;
    }

    public Object aug_plus(SimpleNode node) throws Exception {
        return aug_binaryop(node, "__iadd__");
    }

    public Object aug_minus(SimpleNode node) throws Exception {
        return aug_binaryop(node, "__isub__");
    }

    public Object aug_multiply(SimpleNode node) throws Exception {
        return aug_binaryop(node, "__imul__");
    }

    public Object aug_divide(SimpleNode node) throws Exception {
        return aug_binaryop(node, "__idiv__");
    }

    public Object aug_modulo(SimpleNode node) throws Exception {
        return aug_binaryop(node, "__imod__");
    }

    public Object aug_and(SimpleNode node) throws Exception {
        return aug_binaryop(node, "__iand__");
    }

    public Object aug_or(SimpleNode node) throws Exception {
        return aug_binaryop(node, "__ior__");
    }

    public Object aug_xor(SimpleNode node) throws Exception {
        return aug_binaryop(node, "__ixor__");
    }

    public Object aug_lshift(SimpleNode node) throws Exception {
        return aug_binaryop(node, "__ilshift__");
    }

    public Object aug_rshift(SimpleNode node) throws Exception {
        return aug_binaryop(node, "__irshift__");
    }

    public Object aug_power(SimpleNode node) throws Exception {
        return aug_binaryop(node, "__ipow__");
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
    public Object Invoke(SimpleNode inst, SimpleNode nname,
                         SimpleNode[] values)
        throws Exception
    {
        String name = getName(nname);
        inst.visit(this);
        code.ldc(name);
        
        //System.out.println("invoke: "+name+": "+values.length);
        
        switch (values.length) {
        case 0:
            if (mrefs.invokea0 == 0) {
                mrefs.invokea0 = code.pool.Methodref(
                    "org/python/core/PyObject", "invoke",
                    "(Ljava/lang/String;)Lorg/python/core/PyObject;");
            }
            code.invokevirtual(mrefs.invokea0);
            break;
        case 1:
            if (mrefs.invokea1 == 0) {
                mrefs.invokea1 = code.pool.Methodref(
                    "org/python/core/PyObject", "invoke",
                    "(Ljava/lang/String;Lorg/python/core/PyObject;)Lorg/python/core/PyObject;");
            }
            values[0].visit(this);
            code.invokevirtual(mrefs.invokea1);
            break;
        case 2:
            if (mrefs.invokea2 == 0) {
                mrefs.invokea2 = code.pool.Methodref(
                    "org/python/core/PyObject", "invoke",
                    "(Ljava/lang/String;Lorg/python/core/PyObject;Lorg/python/core/PyObject;)Lorg/python/core/PyObject;");
            }
            values[0].visit(this);
            values[1].visit(this);
            code.invokevirtual(mrefs.invokea2);
            break;                          
        default:
            makeArray(values);
            if (mrefs.invoke2 == 0) {
                mrefs.invoke2 = code.pool.Methodref(
                    "org/python/core/PyObject", "invoke",
                    "(Ljava/lang/String;[Lorg/python/core/PyObject;)Lorg/python/core/PyObject;");
            }
            code.invokevirtual(mrefs.invoke2);
            break;
        }

        return null;
        
    }


    public int callextra;
    public int call1, call2;
    public int calla0, calla1, calla2, calla3, calla4;
    public Object Call_Op(SimpleNode node) throws Exception {
        //do name
        SimpleNode callee = node.getChild(0);

        //get arguments and keywords
        SimpleNode args=null;
        if (node.getNumChildren() > 1)
            args = node.getChild(1);
        SimpleNode[] values;
        String[] keys=null;
        int nKeywords=0;
        SimpleNode starargs = null;
        SimpleNode kwargs = null;

        if (args != null) {
            int n = args.getNumChildren();

            SimpleNode lastarg = args.getChild(n-1);
            if (lastarg.id == PythonGrammarTreeConstants.JJTEXTRAKEYWORDVALUELIST) {
                n--;
                kwargs = lastarg;
            }
            if (n > 0) {
                lastarg = args.getChild(n-1);
                if (lastarg.id == PythonGrammarTreeConstants.JJTEXTRAARGVALUELIST) {
                    n--;
                    starargs = lastarg;
                }
            }

            values = new SimpleNode[n];
            keys = new String[n];

            for (int i=0; i<n; i++) {
                SimpleNode arg = args.getChild(i);
                if (arg.id != PythonGrammarTreeConstants.JJTKEYWORD) {
                    values[i] = arg;
                    if (nKeywords > 0)
                        throw new ParseException(
                            "non-keyword argument following keyword", node);
                } else {
                    values[i] = arg.getChild(1);
                    keys[nKeywords++] = (String)arg.getChild(0).getInfo();
                }
            }
        } else {
            values = new SimpleNode[0];
        }
                
        // Detect a method invocation with no keywords
        if (nKeywords == 0 && starargs == null && kwargs == null &&
            callee.id == PythonGrammarTreeConstants.JJTDOT_OP)
        {
            return Invoke(callee.getChild(0), callee.getChild(1), values);
        }
                
        callee.visit(this);

        if (starargs != null || kwargs != null) {
            makeArray(values);
            makeStrings(code, keys, nKeywords);
            if (starargs == null)
                code.aconst_null();
            else
                starargs.getChild(0).visit(this);
            if (kwargs == null)
                code.aconst_null();
            else
                kwargs.getChild(0).visit(this);

            if (mrefs.callextra == 0) {
                mrefs.callextra = code.pool.Methodref(
                    "org/python/core/PyObject", "_callextra",
                    "([Lorg/python/core/PyObject;[Ljava/lang/String;" +
                     "Lorg/python/core/PyObject;" +
                     "Lorg/python/core/PyObject;)Lorg/python/core/PyObject;");
            }
            code.invokevirtual(mrefs.callextra);
        } else if (nKeywords > 0) {
            makeArray(values);
            makeStrings(code, keys, nKeywords);

            if (mrefs.call1 == 0) {
                mrefs.call1 = code.pool.Methodref(
                    "org/python/core/PyObject", "__call__",
                    "([Lorg/python/core/PyObject;[Ljava/lang/String;)Lorg/python/core/PyObject;");
            }
            code.invokevirtual(mrefs.call1);
        } else {
            switch (values.length) {
            case 0:
                if (mrefs.calla0 == 0) {
                    mrefs.calla0 = code.pool.Methodref(
                        "org/python/core/PyObject", "__call__",
                        "()Lorg/python/core/PyObject;");
                }
                code.invokevirtual(mrefs.calla0);
                break;
            case 1:
                if (mrefs.calla1 == 0) {
                    mrefs.calla1 = code.pool.Methodref(
                        "org/python/core/PyObject", "__call__",
                        "(Lorg/python/core/PyObject;)Lorg/python/core/PyObject;");
                }
                values[0].visit(this);
                code.invokevirtual(mrefs.calla1);
                break;
            case 2:
                if (mrefs.calla2 == 0) {
                    mrefs.calla2 = code.pool.Methodref(
                        "org/python/core/PyObject", "__call__",
                        "(Lorg/python/core/PyObject;Lorg/python/core/PyObject;)Lorg/python/core/PyObject;");
                }
                values[0].visit(this);
                values[1].visit(this);
                code.invokevirtual(mrefs.calla2);
                break;                      
            case 3:
                if (mrefs.calla3 == 0) {
                    mrefs.calla3 = code.pool.Methodref(
                        "org/python/core/PyObject", "__call__",
                        "(Lorg/python/core/PyObject;Lorg/python/core/PyObject;Lorg/python/core/PyObject;)Lorg/python/core/PyObject;");
                }
                values[0].visit(this);
                values[1].visit(this);
                values[2].visit(this);
                code.invokevirtual(mrefs.calla3);
                break;
            case 4:
                if (mrefs.calla4 == 0) {
                    mrefs.calla4 = code.pool.Methodref(
                        "org/python/core/PyObject", "__call__",
                        "(Lorg/python/core/PyObject;Lorg/python/core/PyObject;Lorg/python/core/PyObject;Lorg/python/core/PyObject;)Lorg/python/core/PyObject;");
                }
                values[0].visit(this);
                values[1].visit(this);
                values[2].visit(this);
                values[3].visit(this);
                code.invokevirtual(mrefs.calla4);
                break;
            default:
                makeArray(values);
                if (mrefs.call2 == 0) {
                    mrefs.call2 = code.pool.Methodref(
                        "org/python/core/PyObject", "__call__",
                        "([Lorg/python/core/PyObject;)Lorg/python/core/PyObject;");
                }
                code.invokevirtual(mrefs.call2);
                break;
            }
        }
        return null;
    }


    public int getslice, setslice, delslice;
    public Object Slice_Op(SimpleNode seq, SimpleNode node) throws Exception {
        if (mode == AUGSET) {
            restoreAugTmps(node, 4);
            mode = SET;
        } else {
            int old_mode = mode;
            mode = GET;
            seq.visit(this);

            SimpleNode[] slice = new SimpleNode[3];
            int n = node.getNumChildren();
            int i=0;
            for (int j=0; j<n; j++) {
                SimpleNode child = node.getChild(j);
                if (child.id == PythonGrammarTreeConstants.JJTCOLON)
                    i++;
                else
                    slice[i] = child;
            }
            for (i=0; i<3; i++) {
                if (slice[i] == null) {
                    code.aconst_null();
                } else {
                    slice[i].visit(this);
                }
            }
            mode = old_mode;

            if (mode == AUGGET) {
                saveAugTmps(node, 4);
                mode = GET;
            }
        }

        switch(mode) {
        case DEL:
            if (mrefs.delslice == 0) {
                mrefs.delslice = code.pool.Methodref(
                    "org/python/core/PyObject", "__delslice__",
                    "(Lorg/python/core/PyObject;Lorg/python/core/PyObject;Lorg/python/core/PyObject;)V");
            }
            code.invokevirtual(mrefs.delslice);
            return null;
        case GET:
            if (mrefs.getslice == 0) {
                mrefs.getslice = code.pool.Methodref(
                    "org/python/core/PyObject", "__getslice__",
                    "(Lorg/python/core/PyObject;Lorg/python/core/PyObject;Lorg/python/core/PyObject;)Lorg/python/core/PyObject;");
            }
            code.invokevirtual(mrefs.getslice);
            return null;
        case SET:
            code.aload(temporary);
            if (mrefs.setslice == 0) {
                mrefs.setslice = code.pool.Methodref(
                    "org/python/core/PyObject", "__setslice__",
                    "(Lorg/python/core/PyObject;Lorg/python/core/PyObject;Lorg/python/core/PyObject;Lorg/python/core/PyObject;)V");
            }
            code.invokevirtual(mrefs.setslice);
            return null;
        }
        return null;
        
    }

    public int getitem, delitem, setitem;
    public Object Index_Op(SimpleNode node) throws Exception {
        SimpleNode seq = node.getChild(0);
        SimpleNode index = node.getChild(1);
        if (index.id == PythonGrammarTreeConstants.JJTSLICE)
            return Slice_Op(seq, index);

        if (mode == AUGSET) {
            restoreAugTmps(node, 2);
            mode = SET;
        } else {
            int old_mode = mode;
            mode = GET;             
            seq.visit(this);
            index.visit(this);
            mode = old_mode;

            if (mode == AUGGET) {
                saveAugTmps(node, 2);
                mode = GET;
            }
        }

        switch(mode) {
        case DEL:
            if (mrefs.delitem == 0) {
                mrefs.delitem = code.pool.Methodref(
                    "org/python/core/PyObject", "__delitem__",
                    "(Lorg/python/core/PyObject;)V");
            }
            code.invokevirtual(mrefs.delitem);
            return null;
        case GET:
            if (mrefs.getitem == 0) {
                mrefs.getitem = code.pool.Methodref(
                    "org/python/core/PyObject", "__getitem__",
                    "(Lorg/python/core/PyObject;)Lorg/python/core/PyObject;");
            }
            code.invokevirtual(mrefs.getitem);
            return null;
        case SET:
            code.aload(temporary);
            if (mrefs.setitem == 0) {
                mrefs.setitem = code.pool.Methodref(
                    "org/python/core/PyObject", "__setitem__",
                    "(Lorg/python/core/PyObject;Lorg/python/core/PyObject;)V");
            }
            code.invokevirtual(mrefs.setitem);
            return null;
        }
        return null;
    }

    public int getattr, delattr, setattr;
    public Object Dot_Op(SimpleNode node) throws Exception {

        if (mode == AUGSET) {
            restoreAugTmps(node, 2);
            mode = SET;
        } else {
            String name = getName(node.getChild(1));
            int old_mode = mode;
            mode = GET;
            node.getChild(0).visit(this);
            mode = old_mode;
            code.ldc(name);

            if (mode == AUGGET) {
                saveAugTmps(node, 2);
                mode = GET;
            }
        }
        
        switch(mode) {
        case DEL:
            if (mrefs.delattr == 0) {
                mrefs.delattr = code.pool.Methodref(
                    "org/python/core/PyObject", "__delattr__",
                    "(Ljava/lang/String;)V");
            }
            code.invokevirtual(mrefs.delattr);
            return null;
        case GET:
            if (mrefs.getattr == 0) {
                mrefs.getattr = code.pool.Methodref(
                    "org/python/core/PyObject", "__getattr__",
                    "(Ljava/lang/String;)Lorg/python/core/PyObject;");
            }
            code.invokevirtual(mrefs.getattr);
            return null;
        case SET:
            code.aload(temporary);
            if (mrefs.setattr == 0) {
                mrefs.setattr = code.pool.Methodref(
                    "org/python/core/PyObject", "__setattr__",
                    "(Ljava/lang/String;Lorg/python/core/PyObject;)V");
            }
            code.invokevirtual(mrefs.setattr);
            return null;
        }
        return null;
    }

    public int getitem2, unpackSequence;
    public Object seqSet(SimpleNode node) throws Exception {
        int n = node.getNumChildren();
        if (n > 0 &&
            node.getChild(n-1).id == PythonGrammarTreeConstants.JJTCOMMA)
        {
            n -= 1;
        }
                    
        if (mrefs.unpackSequence == 0) {
            mrefs.unpackSequence = code.pool.Methodref(
                "org/python/core/Py",
                "unpackSequence",
                "(Lorg/python/core/PyObject;I)[Lorg/python/core/PyObject;");
        }
        /*if (mrefs.checkSequence == 0) {
          mrefs.checkSequence = code.pool.Methodref("org/python/core/Py",
          "checkSequence", "(Lorg/python/core/PyObject;I)V");
          }*/

        code.aload(temporary);
        code.iconst(n);
        code.invokestatic(mrefs.unpackSequence);
                
        int tmp = code.getLocal();
        code.astore(tmp);

        for (int i=0; i<n; i++) {
            code.aload(tmp);
            code.iconst(i);
            code.aaload();
            set(node.getChild(i));
        }
        code.freeLocal(tmp);

        /*if (mrefs.getitem2 == 0) {
          mrefs.getitem2 = code.pool.Methodref("org/python/core/PyObject", "__getitem__",
          "(I)Lorg/python/core/PyObject;");
          }
          for(int i=0; i<n; i++) {
          code.aload(temporary);
          code.iconst(i);
          code.invokevirtual(mrefs.getitem2);
          set(node.getChild(i));
          }*/
        return null;
    }

    public Object seqDel(SimpleNode node) throws Exception {
        int n = node.getNumChildren();
        if (n > 0 &&
            node.getChild(n-1).id == PythonGrammarTreeConstants.JJTCOMMA)
        {
            n -= 1;
        }

        for (int i=0; i<n; i++) {
            node.getChild(i).visit(this);
        }
        return null;
    }

    public int PyTuple_init, PyList_init, PyDictionary_init;
    public Object tuple(SimpleNode node) throws Exception {
        /* if (mode ==AUGSET)
            throw new ParseException(
                      "augmented assign to tuple not possible", node); */
        if (mode == SET) return seqSet(node);
        if (mode == DEL) return seqDel(node);

        code.new_(code.pool.Class("org/python/core/PyTuple"));
        code.dup();
        makeArray(node.children);
        if (mrefs.PyTuple_init == 0) {
            mrefs.PyTuple_init = code.pool.Methodref(
                "org/python/core/PyTuple", "<init>",
                "([Lorg/python/core/PyObject;)V");
        }
        code.invokespecial(mrefs.PyTuple_init);
        return null;
    }

    public Object fplist(SimpleNode node) throws Exception {
        if (mode == SET) return seqSet(node);
        throw new ParseException("in fplist node", node);
    }

    public Object list(SimpleNode node) throws Exception {
        /* if (mode ==AUGSET)
            throw new ParseException(
                      "augmented assign to list not possible", node); */

        if (node.getNumChildren() > 1 && node.getChild(1).id ==
                      PythonGrammarTreeConstants.JJTFOR_STMT)
             return list_comprehension(node);

        if (mode == SET) return seqSet(node);
        if (mode == DEL) return seqDel(node);

        code.new_(code.pool.Class("org/python/core/PyList"));
        code.dup();
        makeArray(node.children);
        if (mrefs.PyList_init == 0) {
            mrefs.PyList_init = code.pool.Methodref(
                "org/python/core/PyList", "<init>",
                "([Lorg/python/core/PyObject;)V");
        }
        code.invokespecial(mrefs.PyList_init);
        return null;
    }

    public int PyList_init2;
    public Object list_comprehension(SimpleNode node) throws Exception {
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
                "(Ljava/lang/String;)Lorg/python/core/PyObject;");
        }
        code.invokevirtual(mrefs.getattr);
        int tmp_append = storeTop();

        listComprehensionExprs.push(node.getChild(0));
        listComprehensionAppends.push(new Integer(tmp_append));

        node.getChild(1).visit(this);

        listComprehensionAppends.pop();
        listComprehensionExprs.pop();

        return null;
    }


    public Object list_iter(SimpleNode node) throws Exception {
        if (node.getNumChildren() == 0) {
            int tmp_append = 
                 ((Integer) listComprehensionAppends.peek()).intValue();
            SimpleNode exprNode = (SimpleNode) listComprehensionExprs.peek();

            code.aload(tmp_append);
            exprNode.visit(this);

            if (mrefs.calla1 == 0) {
                mrefs.calla1 = code.pool.Methodref(
                    "org/python/core/PyObject", "__call__",
                    "(Lorg/python/core/PyObject;)Lorg/python/core/PyObject;");
            }
            code.invokevirtual(mrefs.calla1);

            code.pop();

            return null;
        }

        return node.getChild(0).visit(this);
    }



    public Object dictionary(SimpleNode node) throws Exception {
        code.new_(code.pool.Class("org/python/core/PyDictionary"));
        code.dup();
        makeArray(node.children);
        if (mrefs.PyDictionary_init == 0) {
            mrefs.PyDictionary_init = code.pool.Methodref(
                "org/python/core/PyDictionary", "<init>",
                "([Lorg/python/core/PyObject;)V");
        }
        code.invokespecial(mrefs.PyDictionary_init);
        return null;
    }

    public Object str_1op(SimpleNode node) throws Exception {
        node.getChild(0).visit(this);
        code.invokevirtual("org/python/core/PyObject", "__repr__",
                           "()Lorg/python/core/PyString;");
        return null;
    }

    public int PyFunction_init1,PyFunction_closure_init1;
    public Object lambdef(SimpleNode node) throws Exception {
        String name = "<lambda>";
        SimpleNode suite;
        if (node.getNumChildren() == 2) {
            suite = node.getChild(1);
        } else {
            suite = node.getChild(0);
        }

        //Add a return node onto the outside of suite;
        SimpleNode retSuite =
            new SimpleNode(PythonGrammarTreeConstants.JJTRETURN_STMT);
        retSuite.jjtAddChild(suite, 0);

        setline(node);

        code.new_(code.pool.Class("org/python/core/PyFunction"));
        code.dup();
        loadFrame();
        if (mrefs.f_globals == 0) {
            mrefs.f_globals = code.pool.Fieldref("org/python/core/PyFrame",
                                                 "f_globals",
                                                 "Lorg/python/core/PyObject;");
        }
        code.getfield(mrefs.f_globals);

        makeArray(node.scope.ac.getDefaults());
        
        node.scope.setup_closure(my_scope); 
        node.scope.dump();
        module.PyCode(retSuite, name, true, className,
                      false, false, node.beginLine, node.scope).get(code);
        Vector freenames = node.scope.freevars;
        node.scope = null; // release scope info

        if (!makeClosure(freenames)) {
            if (mrefs.PyFunction_init1 == 0) {
                mrefs.PyFunction_init1 = code.pool.Methodref(
                "org/python/core/PyFunction", "<init>",
                "(Lorg/python/core/PyObject;[Lorg/python/core/PyObject;Lorg/python/core/PyCode;)V");
            }
            code.invokespecial(mrefs.PyFunction_init1);
        } else {
            if (mrefs.PyFunction_closure_init1 == 0) {
                mrefs.PyFunction_closure_init1 = code.pool.Methodref(
                "org/python/core/PyFunction", "<init>",
                "(Lorg/python/core/PyObject;[Lorg/python/core/PyObject;Lorg/python/core/PyCode;[Lorg/python/core/PyObject;)V");
            }
            code.invokespecial(mrefs.PyFunction_closure_init1);
        }
        
        return null;
    }


    public int Ellipsis;
    public Object Ellipses(SimpleNode node) throws Exception {
        if (mrefs.Ellipsis == 0) {
            mrefs.Ellipsis = code.pool.Fieldref(
                "org/python/core/Py", "Ellipsis",
                "Lorg/python/core/PyObject;");
        }
        code.getstatic(mrefs.Ellipsis);
        return null;
    }

    public int PySlice_init;
    public Object Slice(SimpleNode node) throws Exception {
        SimpleNode[] slice = new SimpleNode[3];
        int n = node.getNumChildren();
        int i=0;
        for (int j=0; j<n; j++) {
            SimpleNode child = node.getChild(j);
            if (child.id == PythonGrammarTreeConstants.JJTCOLON)
                i++;
            else
                slice[i] = child;
        }

        code.new_(code.pool.Class("org/python/core/PySlice"));
        code.dup();
        for (i=0; i<3; i++) {
            if (slice[i] == null) {
                getNone();
            } else {
                slice[i].visit(this);
            }
        }
        if (mrefs.PySlice_init == 0) {
            mrefs.PySlice_init = code.pool.Methodref(
                "org/python/core/PySlice", "<init>",
                "(Lorg/python/core/PyObject;Lorg/python/core/PyObject;Lorg/python/core/PyObject;)V");
        }
        code.invokespecial(mrefs.PySlice_init);
        return null;
    }

    public int makeClass,makeClass_closure;
    public Object classdef(SimpleNode node) throws Exception {
        setline(node);

        //Get class name
        String name = getName(node.getChild(0));
        //System.out.println("name: "+name);
        code.ldc(name);

        //Get class bases
        int n = node.getNumChildren();
        SimpleNode[] bases = new SimpleNode[n-2];
        for (int i=0; i<n-2; i++)
            bases[i] = node.getChild(i+1);
        makeArray(bases);

        node.scope.setup_closure(my_scope);
        node.scope.dump();
        //Make code object out of suite
        module.PyCode(node.getChild(n-1), name, false, name, true, false,
                      node.beginLine, node.scope).get(code);
        Vector freenames = node.scope.freevars;
        node.scope = null; // release scope info

        //Get doc string (if there)
        getDocString(node.getChild(n-1));

        //Make class out of name, bases, and code
        if (!makeClosure(freenames)) {
            if (mrefs.makeClass == 0) {
                mrefs.makeClass = code.pool.Methodref(
                "org/python/core/Py", "makeClass",
                "(Ljava/lang/String;[Lorg/python/core/PyObject;Lorg/python/core/PyCode;Lorg/python/core/PyObject;)Lorg/python/core/PyObject;");
            }
            code.invokestatic(mrefs.makeClass);
        } else {
            if (mrefs.makeClass_closure == 0) {
                mrefs.makeClass_closure = code.pool.Methodref(
                "org/python/core/Py", "makeClass",
                "(Ljava/lang/String;[Lorg/python/core/PyObject;Lorg/python/core/PyCode;Lorg/python/core/PyObject;[Lorg/python/core/PyObject;)Lorg/python/core/PyObject;");
            }
            code.invokestatic(mrefs.makeClass_closure);            
        }
        
        //Assign this new class to the given name
        set(node.getChild(0));
        return null;
    }

    public Object Int(SimpleNode node) throws Exception {
        Object n = node.getInfo();
        if (n instanceof Integer) {
            int i = ((Integer)n).intValue();
            module.PyInteger(i).get(code);
        } else {
            module.PyLong((String)n).get(code);
        }
        return null;
    }

    public Object Float(SimpleNode node) throws Exception {
        module.PyFloat(((Double)node.getInfo()).doubleValue()).get(code);
        return null;
    }

    public Object Complex(SimpleNode node) throws Exception {
        module.PyComplex(((Double)node.getInfo()).doubleValue()).get(code);
        return null;
    }

    private String getName(SimpleNode node) {
        String name = (String)node.getInfo();
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
            "(Ljava/lang/String;)Lorg/python/core/PyObject;");
        }
        code.invokevirtual(mrefs.getglobal);
    }
    
    public Object Name(SimpleNode node) throws Exception {
        String name;
        if (fast_locals)
            name = (String)node.getInfo();
        else
            name = getName(node);

        if (mode == AUGGET) 
            mode = GET;
        else if (mode == AUGSET) 
            mode = SET;

        SymInfo syminf = (SymInfo)tbl.get(name);
        
        switch (mode) {
        case GET:
            loadFrame();
            if (syminf != null) {
                int flags = syminf.flags;
                if (!my_scope.nested_scopes) flags &= ~ScopeInfo.FREE;
                if ((flags&ScopeInfo.GLOBAL) !=0 || 
                     optimizeGlobals&&(flags&(ScopeInfo.BOUND|ScopeInfo.CELL|ScopeInfo.FREE))==0) {
                    emitGetGlobal(name);
                    return null;
                }
                if (fast_locals) {
                    if ((flags&ScopeInfo.CELL) != 0) {
                        code.iconst(syminf.env_index);
                        if (mrefs.getderef == 0) {
                            mrefs.getderef = code.pool.Methodref(
                            "org/python/core/PyFrame", "getderef",
                            "(I)Lorg/python/core/PyObject;");
                        }
                        code.invokevirtual(mrefs.getderef);
                        return null;
                    }
                    if ((flags&ScopeInfo.BOUND) != 0) {
                        code.iconst(syminf.locals_index);
                        if (mrefs.getlocal2 == 0) {
                            mrefs.getlocal2 = code.pool.Methodref(
                            "org/python/core/PyFrame", "getlocal",
                            "(I)Lorg/python/core/PyObject;");
                        }
                        code.invokevirtual(mrefs.getlocal2);
                        return null;
                    }
                }
                if ((flags&ScopeInfo.FREE) != 0 && (flags&ScopeInfo.BOUND) == 0) {
                    code.iconst(syminf.env_index);
                    if (mrefs.getderef == 0) {
                        mrefs.getderef = code.pool.Methodref(
                        "org/python/core/PyFrame", "getderef",
                        "(I)Lorg/python/core/PyObject;");
                    }
                    code.invokevirtual(mrefs.getderef);
                    return null;
                }
            }
            code.ldc(name);
            if (mrefs.getlocal1 == 0) {
                mrefs.getlocal1 = code.pool.Methodref(
                    "org/python/core/PyFrame", "getname",
                    "(Ljava/lang/String;)Lorg/python/core/PyObject;");
            }
            code.invokevirtual(mrefs.getlocal1);
            return null;

        case SET:
            loadFrame();
            if (syminf != null && (syminf.flags&ScopeInfo.GLOBAL) != 0) {
                code.ldc(name);
                code.aload(temporary);
                if (mrefs.setglobal == 0) {
                    mrefs.setglobal = code.pool.Methodref(
                        "org/python/core/PyFrame", "setglobal",
                        "(Ljava/lang/String;Lorg/python/core/PyObject;)V");
                }
                code.invokevirtual(mrefs.setglobal);
            } else {
                if (!fast_locals) {
                    code.ldc(name);
                    code.aload(temporary);
                    if (mrefs.setlocal1 == 0) {
                        mrefs.setlocal1 = code.pool.Methodref(
                            "org/python/core/PyFrame", "setlocal",
                            "(Ljava/lang/String;Lorg/python/core/PyObject;)V");
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
                            "(ILorg/python/core/PyObject;)V");
                        }
                        code.invokevirtual(mrefs.setderef);
                    } else {
                        code.iconst(syminf.locals_index);
                        code.aload(temporary);
                        if (mrefs.setlocal2 == 0) {
                            mrefs.setlocal2 = code.pool.Methodref(
                            "org/python/core/PyFrame", "setlocal",
                            "(ILorg/python/core/PyObject;)V");
                        }
                        code.invokevirtual(mrefs.setlocal2);
                    }
                }
            }
            return null;
        case DEL: {
            loadFrame();
            if (syminf != null && (syminf.flags&ScopeInfo.GLOBAL) != 0) {
                code.ldc(name);
                if (mrefs.delglobal == 0) {
                    mrefs.delglobal = code.pool.Methodref(
                        "org/python/core/PyFrame", "delglobal",
                        "(Ljava/lang/String;)V");
                }
                code.invokevirtual(mrefs.delglobal);
            } else {
                if (!fast_locals) {
                    code.ldc(name);
                    if (mrefs.dellocal1 == 0) {
                        mrefs.dellocal1 = code.pool.Methodref(
                            "org/python/core/PyFrame", "dellocal",
                            "(Ljava/lang/String;)V");
                    }
                    code.invokevirtual(mrefs.dellocal1);
                } else {
                    if (syminf == null) {
                        System.err.println("internal compiler error: "+node);
                    }
                    if ((syminf.flags&ScopeInfo.CELL) != 0) {
                        error("can not delete variable '"+name+"' referenced in nested scope",true,node);
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

    public Object String(SimpleNode node) throws Exception {
        String s = (String)node.getInfo();
        if (s.length() > 32767) {
            throw new ParseException(
                "string constant too large (more than 32767 characters)",
                node);
        }
        module.PyString(s).get(code);
        return null;
    }

}
