// (C) Copyright 2007 Tobias Ivarsson
package org.python.newcompiler.pyasm.util;

import java.io.PrintWriter;

import org.python.core.PyObject;
import org.python.newcompiler.pyasm.CodeVisitor;
import org.python.newcompiler.pyasm.Label;
import org.python.newcompiler.pyasm.Operator;

public class PythonDis extends CodeAdapter {

    private PrintWriter out;
    private boolean autoflush;

    public PythonDis(PrintWriter out) {
        this(out, true);
    }

    public PythonDis(PrintWriter out, boolean autoflush) {
        this(null, out, autoflush);
    }

    public PythonDis(CodeVisitor next) {
        this(next, new PrintWriter(System.out));
    }

    public PythonDis(CodeVisitor next, PrintWriter out) {
        this(next, out, true);
    }

    public PythonDis(CodeVisitor next, PrintWriter out, boolean autoflush) {
        super(next);
        this.out = out;
        this.autoflush = autoflush;
    }

    private void print(Object what) {
        out.print(what);
    }

    private void println() {
        out.println();
    }

    private void println(Object what) {
        print(what);
        println();
    }

    private int offset = 0;

    private void len(int len) {
        offset += len;
    }

    private void printOffset() {
        print("\t" + offset + "\t");
    }

    private void instruction(String mnem, int len, Object param) {
        printOffset();
        print(mnem + "\t");
        len(len);
        println(param);
    }

    private void instruction(String mnem, int len) {
        printOffset();
        print(mnem + "\t");
        len(len);
        println();
    }

    private void instruction(String mnem) {
        instruction(mnem, 1);
    }

    // CodeVisitor

    public void visitCode(long argcount, long nlocals, long stacksize,
            long flags, PyObject[] constants, String[] names,
            String[] varnames, String[] freevars, String[] cellvars,
            String filename, String name, long firstlnno) {
        super.visitCode(argcount, nlocals, stacksize, flags, constants, names,
                varnames, freevars, cellvars, filename, name, firstlnno);

        println("Visiting code for " + name + " in " + filename);
    }

    public void visitEnd() {
        super.visitEnd();

        if (autoflush) {
            out.flush();
        }
    }

    public void visitBinaryOperator(int operator) {
        super.visitBinaryOperator(operator);

        switch (operator) {
        case Operator.ADD:
            instruction("BINARY ADD");
            break;
        case Operator.SUBTRACT:
            instruction("BINARY SUBTRACT");
            break;
        case Operator.MULTIPLY:
            instruction("BINARY MULTIPLY");
            break;
        case Operator.DIVIDE:
            instruction("BINARY DIVIDE");
            break;
        case Operator.FLOOR_DIVIDE:
            instruction("BINARY FLOOR DIVIDE");
            break;
        case Operator.TRUE_DIVIDE:
            instruction("BINARY TRUE DIVIDE");
            break;
        case Operator.MODULO:
            instruction("BINARY MODULO");
            break;
        case Operator.POWER:
            instruction("BINARY POWER");
            break;
        case Operator.LSHIFT:
            instruction("BINARY LSHIFT");
            break;
        case Operator.RSHIFT:
            instruction("BINARY RSHIFT");
            break;
        case Operator.AND:
            instruction("BINARY AND");
            break;
        case Operator.OR:
            instruction("BINARY OR");
            break;
        case Operator.XOR:
            instruction("BINARY XOR");
            break;
        case Operator.SUBSCRIPT:
            instruction("BINARY SUBSCRIPT");
            break;

        default:
            throw new RuntimeException("unknown binary operator " + operator);
        }
    }

    public void visitBreakLoop() {
        super.visitBreakLoop();

        instruction("BREAK LOOP");
    }

    public void visitBuildClass() {
        super.visitBuildClass();

        instruction("BUILD CLASS");
    }

    public void visitBuildList(int size) {
        super.visitBuildList(size);

        instruction("BUILD LIST", 3, new Integer(size));
    }

    public void visitBuildMap(int zero) {
        super.visitBuildMap(zero);

        if (zero != 0) {
            throw new RuntimeException("BUILD MAP received non-zero argument "
                    + zero);
        }
        instruction("BUILD MAP", 3, new Integer(zero));
    }

    public void visitBuildSlice(int numargs) {
        super.visitBuildSlice(numargs);

        instruction("BUILD SLICE", 3, new Integer(numargs));
    }

    public void visitBuildTuple(int size) {
        super.visitBuildTuple(size);

        instruction("BUILD TUPLE", 3, new Integer(size));
    }

    public void visitCallFunction(int num_positional, int num_keyword) {
        super.visitCallFunction(num_positional, num_keyword);

        instruction("CALL FUNCTION", 3, "" + num_positional
                + " positional and " + num_keyword + " keywords.");
    }

    public void visitCallFunctionKeyword(int num_positional, int num_keyword) {
        super.visitCallFunctionKeyword(num_positional, num_keyword);

        instruction("CALL FUNCTION KEYWORD", 3, "" + num_positional
                + " positional and " + num_keyword + " keywords.");
    }

    public void visitCallFunctionVararg(int num_positional, int num_keyword) {
        super.visitCallFunctionVararg(num_positional, num_keyword);

        instruction("CALL FUNCTION VARARG", 3, "" + num_positional
                + " positional and " + num_keyword + " keywords.");
    }

    public void visitCallFunctionVarargKeyword(int num_positional,
            int num_keyword) {
        super.visitCallFunctionVarargKeyword(num_positional, num_keyword);

        instruction("CALL FUNCTION VARARG KEYWORD", 3, "" + num_positional
                + " positional and " + num_keyword + " keywords.");
    }

    public void visitCompareOperator(int operator) {
        super.visitCompareOperator(operator);

        String op;
        switch (operator) {
        case Operator.LESS_THAN:
            op = "<";
            break;
        case Operator.LESS_THAN_OR_EQUAL:
            op = "<=";
            break;
        case Operator.EQUAL:
            op = "==";
            break;
        case Operator.NOT_EQUAL:
            op = "!=";
            break;
        case Operator.GREATER_THAN:
            op = ">";
            break;
        case Operator.GREATER_THAN_OR_EQUAL:
            op = ">=";
            break;
        case Operator.IN:
            op = "in";
            break;
        case Operator.NOT_IN:
            op = "not in";
            break;
        case Operator.IS:
            op = "is";
            break;
        case Operator.IS_NOT:
            op = "is not";
            break;
        case Operator.EXCEPTION_MATCH:
            op = "exception match";
            break;
        default:
            throw new RuntimeException("Unknown compare operator " + operator);
        }
        instruction("COMPARE OPERATOR", 3, op);
    }

    public void visitContinueLoop(Label loopStart) {
        super.visitContinueLoop(loopStart);

        instruction("CONTINUE LOOP", 3, loopStart);
    }

    public void visitDeleteAttribute(String attributeName) {
        super.visitDeleteAttribute(attributeName);

        instruction("DELETE ATTRIBUTE", 3, attributeName);
    }

    public void visitDeleteFast(String variableName) {
        super.visitDeleteFast(variableName);

        instruction("DELETE FAST", 3, variableName);
    }

    public void visitDeleteGlobal(String variableName) {
        super.visitDeleteGlobal(variableName);

        instruction("DELETE GLOBAL", 3, variableName);
    }

    public void visitDeleteName(String variableName) {
        super.visitDeleteName(variableName);

        instruction("DELETE NAME", 3, variableName);
    }

    public void visitDeleteSlice(int plus) {
        super.visitDeleteSlice(plus);

        instruction("DELETE SLICE +" + plus);
    }

    public void visitDeleteSubscript() {
        super.visitDeleteSubscript();

        instruction("DELETE SUBSCRIPT");
    }

    public void visitDup(int num_elements) {
        super.visitDup(num_elements);

        if (num_elements == 1) {
            instruction("DUP TOP");
        } else {
            instruction("DUP TOPX", 3, new Integer(num_elements));
        }
    }

    public void visitEndFinally() {
        super.visitEndFinally();

        instruction("END FINALLY");
    }

    public void visitExecStatement() {
        super.visitExecStatement();

        instruction("EXEC STMT");
    }

    public void visitForIteration(Label end) {
        super.visitForIteration(end);

        instruction("FOR ITER", 3, end);
    }

    public void visitGetIterator() {
        super.visitGetIterator();

        instruction("GET ITER");
    }

    public void visitImportFrom(String name) {
        super.visitImportFrom(name);

        instruction("IMPORT FROM", 3, name);
    }

    public void visitImportName(String name) {
        super.visitImportName(name);

        instruction("IMPORT NAME", 3, name);
    }

    public void visitImportStar() {
        super.visitImportStar();

        instruction("IMPORT STAR");
    }

    public void visitInplaceOperator(int operator) {
        super.visitInplaceOperator(operator);

        switch (operator) {
        case Operator.ADD:
            instruction("INPLACE ADD");
            break;
        case Operator.SUBTRACT:
            instruction("INPLACE SUBTRACT");
            break;
        case Operator.MULTIPLY:
            instruction("INPLACE MULTIPLY");
            break;
        case Operator.DIVIDE:
            instruction("INPLACE DIVIDE");
            break;
        case Operator.FLOOR_DIVIDE:
            instruction("INPLACE FLOOR DIVIDE");
            break;
        case Operator.TRUE_DIVIDE:
            instruction("INPLACE TRUE DIVIDE");
            break;
        case Operator.MODULO:
            instruction("INPLACE MODULO");
            break;
        case Operator.POWER:
            instruction("INPLACE POWER");
            break;
        case Operator.LSHIFT:
            instruction("INPLACE LSHIFT");
            break;
        case Operator.RSHIFT:
            instruction("INPLACE RSHIFT");
            break;
        case Operator.AND:
            instruction("INPLACE AND");
            break;
        case Operator.OR:
            instruction("INPLACE OR");
            break;
        case Operator.XOR:
            instruction("INPLACE XOR");
            break;

        default:
            throw new RuntimeException("unknown inplace operator " + operator);
        }
    }

    public void visitJump(Label destination) {
        super.visitJump(destination);

        instruction("JUMP", 3, destination);
    }

    public void visitJumpIfFalse(Label destination) {
        super.visitJumpIfFalse(destination);

        instruction("JUMP IF FALSE", 3, destination);
    }

    public void visitJumpIfTrue(Label destination) {
        super.visitJumpIfTrue(destination);

        instruction("JUMP IF TRUE", 3, destination);
    }

    public void visitLabel(Label label) {
        super.visitLabel(label);

        print(label);
        println(":");
    }

    public void visitLineNumber(int lineNumber) {
        super.visitLineNumber(lineNumber);
    }

    public void visitListAppend() {
        super.visitListAppend();

        instruction("LIST APPEND");
    }

    public void visitLoadAttribute(String attributeName) {
        super.visitLoadAttribute(attributeName);

        instruction("LOAD ATTRIBUTE", 3, attributeName);
    }

    public void visitLoadClosure(String variableName) {
        super.visitLoadClosure(variableName);

        instruction("LOAD CLOSURE", 3, variableName);
    }

    public void visitLoadConstant(PyObject constant) {
        super.visitLoadConstant(constant);

        instruction("LOAD CONSTANT", 3, constant.__repr__());
    }

    public void visitLoadDeref(String variableName) {
        super.visitLoadDeref(variableName);

        instruction("LOAD DEREF", 3, variableName);
    }

    public void visitLoadFast(String variableName) {
        super.visitLoadFast(variableName);

        instruction("LOAD FAST", 3, variableName);
    }

    public void visitLoadGlobal(String variableName) {
        super.visitLoadGlobal(variableName);

        instruction("LOAD GLOBAL", 3, variableName);
    }

    public void visitLoadLocals() {
        super.visitLoadLocals();

        instruction("LOAD LOCALS");
    }

    public void visitLoadName(String variableName) {
        super.visitLoadName(variableName);

        instruction("LOAD NAME", 3, variableName);
    }

    public void visitLoadSlice(int plus) {
        super.visitLoadSlice(plus);

        instruction("LOAD SLICE +" + plus);
    }

    public void visitMakeClosure(int num_default) {
        super.visitMakeClosure(num_default);

        instruction("MAKE CLOSURE", 3, "" + num_default + " default arguments");
    }

    public void visitMakeFunction(int num_default) {
        super.visitMakeFunction(num_default);

        instruction("MAKE FUNCTION", 3, "" + num_default + " default arguments");
    }

    public void visitNOP() {
        super.visitNOP();

        instruction("NOP");
    }

    public void visitPop() {
        super.visitPop();

        instruction("POP TOP");
    }

    public void visitPopBlock() {
        super.visitPopBlock();

        instruction("POP BLOCK");
    }

    public void visitPrintExpression() {
        super.visitPrintExpression();

        instruction("PRINT EXPR");
    }

    public void visitPrintItem() {
        super.visitPrintItem();

        instruction("PRINT ITEM");
    }

    public void visitPrintItemTo() {
        super.visitPrintItemTo();

        instruction("PRINT ITEM TO");
    }

    public void visitPrintNewline() {
        super.visitPrintNewline();

        instruction("PRINT NEWLINE");
    }

    public void visitPrintNewlineTo() {
        super.visitPrintNewlineTo();

        instruction("PRINT NEWLINE TO");
    }

    public void visitRaiseVarargs(int count) {
        super.visitRaiseVarargs(count);

        instruction("RAISE VARARGS", 3, new Integer(count));
    }

    public void visitReturnValue() {
        super.visitReturnValue();

        instruction("RETURN VALUE");
    }

    public void visitRot(int depth) {
        super.visitRot(depth);

        switch (depth) {
        case 2:
            instruction("ROT TWO");
            break;
        case 3:
            instruction("ROT THREE");
            break;
        case 4:
            instruction("ROT FOUR");
            break;
        default:
            throw new RuntimeException("Cannot rot " + depth + " positions.");
        }
    }

    public void visitSetupExcept(Label startExcept) {
        super.visitSetupExcept(startExcept);

        instruction("SETUP EXCEPT", 3, startExcept);
    }

    public void visitSetupFinally(Label startFinally) {
        super.visitSetupFinally(startFinally);

        instruction("SETUP FINALLY", 3, startFinally);
    }

    public void visitSetupLoop(Label end) {
        super.visitSetupLoop(end);

        instruction("SETUP LOOP", 3, end);
    }

    public void visitStopCode() {
        super.visitStopCode();

        instruction("STOP CODE");
    }

    public void visitStoreAttribute(String attributeName) {
        super.visitStoreAttribute(attributeName);

        instruction("STORE ATTR", 3, attributeName);
    }

    public void visitStoreDeref(String variableName) {
        super.visitStoreDeref(variableName);

        instruction("STORE DEREF", 3, variableName);
    }

    public void visitStoreFast(String variableName) {
        super.visitStoreFast(variableName);

        instruction("STORE FAST", 3, variableName);
    }

    public void visitStoreGlobal(String variableName) {
        super.visitStoreGlobal(variableName);

        instruction("STORE GLOBAL", 3, variableName);
    }

    public void visitStoreName(String variableName) {
        super.visitStoreName(variableName);

        instruction("STORE NAME", 3, variableName);
    }

    public void visitStoreSlice(int plus) {
        super.visitStoreSlice(plus);

        instruction("STORE SLICE +" + plus);
    }

    public void visitStoreSubscript() {
        super.visitStoreSubscript();

        instruction("STORE SUBSCRIPT");
    }

    public void visitUnaryOperator(int operator) {
        super.visitUnaryOperator(operator);

        switch (operator) {
        case Operator.INVERT:
            instruction("UNARY INVERT");
            break;
        case Operator.POSITIVE:
            instruction("UNARY POSITIVE");
            break;
        case Operator.NEGATIVE:
            instruction("UNARY NEGATIVE");
            break;
        case Operator.NOT:
            instruction("UNARY NOT");
            break;
        case Operator.CONVERT:
            instruction("UNARY CONVERT");
            break;
        default:
            throw new RuntimeException("unknown unary operator " + operator);
        }
    }

    public void visitUnpackSequence(int count) {
        super.visitUnpackSequence(count);

        instruction("UNPACK SEQUENCE", 3, new Integer(count));
    }

    public void visitWithCleanup() {
        super.visitWithCleanup();

        instruction("WITH CLEANUP");
    }

    public void visitYieldValue(int index, Label resume) {
        super.visitYieldValue(index, resume);

        instruction("YIELD VALUE");
    }

}
