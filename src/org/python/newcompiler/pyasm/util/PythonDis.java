// (C) Copyright 2007 Tobias Ivarsson
package org.python.newcompiler.pyasm.util;

import java.io.PrintWriter;

import org.python.core.PyObject;
import org.python.newcompiler.DisassemblyDocument;
import org.python.newcompiler.pyasm.BytecodeVisitor;
import org.python.newcompiler.pyasm.CodeVisitor;
import org.python.newcompiler.pyasm.Label;
import org.python.newcompiler.pyasm.Operator;

/**
 * Python bytecode dissassembler, similar to the python dis-module
 * 
 * @author Tobias Ivarsson
 */
public class PythonDis extends CodeAdapter {

    private static class WriterDebugger implements DisassemblyDocument {

        private PrintWriter out;
        private boolean autoflush;
        private int indentation;

        private WriterDebugger(PrintWriter out, boolean autoflush,
                int indentation) {
            this.out = out;
            this.autoflush = autoflush;
            this.indentation = indentation;
        }

        private WriterDebugger(PrintWriter out, boolean autoflush) {
            this(out, autoflush, 0);
        }

        private void flush() {
            if (autoflush) {
                out.flush();
            }
        }

        private static String fill(String text, int width) {
            while (text.length() < width) {
                text += " ";
            }
            return text;
        }

        public DisassemblyDocument newPreTitle() {
            // REALLY too simple
            return newSubSection();
        }

        public DisassemblyDocument newSubSection() {
            // Too simple...
            return new WriterDebugger(out, autoflush, indentation + 1);
        }

        public void put(String string) {
            out.println(fill("", 6) + fill("", (indentation + 1) * 4) + string);
        }

        public void put(long offset, String string) {
            out.println(fill("" + offset, 6) + fill("", (indentation + 1) * 4)
                    + string);
        }

        public void putLabel(String string) {
            out.println(fill("", 6) + fill("", indentation * 4 + 2) + string);
        }

        public void putTitle(long offset, String string) {
            out.println(fill("" + offset, 6) + fill("", indentation * 4)
                    + string);
        }

        public void putTitle(String string) {
            out.println(fill("", 6) + fill("", indentation * 4) + string);
        }

    }

    private DisassemblyDocument debugger;
    private int offset = 0;

    /**
     * Create a Python disassembler that sends output to a specific document
     * generator.
     * 
     * @param debug The disassembly document generator to use
     */
    public PythonDis(DisassemblyDocument debug) {
        this(null, debug);
    }

    /**
     * Create a Python disassembler that uses a default document generator, and
     * forwards requests to annother {@link BytecodeVisitor}.
     * 
     * @param next the next {@link BytecodeVisitor} in the chain.
     */
    public PythonDis(CodeVisitor next) {
        this(next, new WriterDebugger(new PrintWriter(System.out), true));
    }

    /**
     * Create a Python disassembler that sends output to a specific document
     * generator, and forwards requests to annother {@link BytecodeVisitor}.
     * 
     * @param next the next {@link BytecodeVisitor} in the chain.
     * @param debug The disassembly document generator to use
     */
    public PythonDis(CodeVisitor next, DisassemblyDocument debug) {
        super(next);
        debugger = debug;
    }

    // BEGIN Legacy stuff
    /**
     * Create a Python disassembler that sends output to a specified output
     * writer via the standard document generator. Automatic flushing of the
     * output writer is enabled by default.
     * 
     * @param out The output writer.
     */
    public PythonDis(PrintWriter out) {
        this(out, true);
    }

    /**
     * Create a Python disassembler that sends output to a specified output
     * writer via the standard document generator. With the option to specify
     * wheter or not automatic flushing of the output writer sould be used.
     * 
     * @param out The output writer.
     * @param autoflush Should automatic flushing be used?
     */
    public PythonDis(PrintWriter out, boolean autoflush) {
        this(null, out, autoflush);
    }

    /**
     * Create a Python disassembler that sends output to a specified output
     * writer via the standard document generator. Automatic flushing of the
     * output writer is enabled by default. Also forwards requests to annother
     * {@link BytecodeVisitor}.
     * 
     * @param next the next {@link BytecodeVisitor} in the chain.
     * @param out The output writer.
     */
    public PythonDis(CodeVisitor next, PrintWriter out) {
        this(next, out, true);
    }

    /**
     * Create a Python disassembler that sends output to a specified output
     * writer via the standard document generator. With the option to specify
     * wheter or not automatic flushing of the output writer sould be used. Also
     * forwards requests to annother {@link BytecodeVisitor}.
     * 
     * @param next the next {@link BytecodeVisitor} in the chain.
     * @param out The output writer.
     * @param autoflush Should automatic flushing be used?
     */
    public PythonDis(CodeVisitor next, PrintWriter out, boolean autoflush) {
        this(next, new WriterDebugger(out, autoflush));
    }

    // END Legacy stuff

    private void instruction(String mnem, int len, Object param) {
        debugger.put(offset, mnem + " " + param);
        offset += len;
    }

    private void instruction(String mnem, int len) {
        instruction(mnem, len, "");
    }

    private void instruction(String mnem) {
        instruction(mnem, 1);
    }

    // CodeVisitor

    @Override
    public void visitCode(long argcount, long nlocals, long stacksize,
            long flags, PyObject[] constants, String[] names,
            String[] varnames, String[] freevars, String[] cellvars,
            String filename, String name, long firstlnno) {
        super.visitCode(argcount, nlocals, stacksize, flags, constants, names,
                varnames, freevars, cellvars, filename, name, firstlnno);
        String args = "";
        debugger.putTitle(firstlnno, "def " + name + "(" + args + "): # in "
                + filename);
    }

    @Override
    public void visitEnd() {
        super.visitEnd();
        if (debugger instanceof WriterDebugger) {
            WriterDebugger writer = (WriterDebugger) debugger;
            writer.flush();
        }
    }

    @Override
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

    @Override
    public void visitBreakLoop() {
        super.visitBreakLoop();

        instruction("BREAK LOOP");
    }

    @Override
    public void visitBuildClass() {
        super.visitBuildClass();

        instruction("BUILD CLASS");
    }

    @Override
    public void visitBuildList(int size) {
        super.visitBuildList(size);

        instruction("BUILD LIST", 3, new Integer(size));
    }

    @Override
    public void visitBuildMap(int zero) {
        super.visitBuildMap(zero);

        if (zero != 0) {
            throw new RuntimeException("BUILD MAP received non-zero argument "
                    + zero);
        }
        instruction("BUILD MAP", 3, new Integer(zero));
    }

    @Override
    public void visitBuildSlice(int numargs) {
        super.visitBuildSlice(numargs);

        instruction("BUILD SLICE", 3, new Integer(numargs));
    }

    @Override
    public void visitBuildTuple(int size) {
        super.visitBuildTuple(size);

        instruction("BUILD TUPLE", 3, new Integer(size));
    }

    @Override
    public void visitCallFunction(int num_positional, int num_keyword) {
        super.visitCallFunction(num_positional, num_keyword);

        instruction("CALL FUNCTION", 3, "" + num_positional
                + " positional and " + num_keyword + " keywords.");
    }

    @Override
    public void visitCallFunctionKeyword(int num_positional, int num_keyword) {
        super.visitCallFunctionKeyword(num_positional, num_keyword);

        instruction("CALL FUNCTION KEYWORD", 3, "" + num_positional
                + " positional and " + num_keyword + " keywords.");
    }

    @Override
    public void visitCallFunctionVararg(int num_positional, int num_keyword) {
        super.visitCallFunctionVararg(num_positional, num_keyword);

        instruction("CALL FUNCTION VARARG", 3, "" + num_positional
                + " positional and " + num_keyword + " keywords.");
    }

    @Override
    public void visitCallFunctionVarargKeyword(int num_positional,
            int num_keyword) {
        super.visitCallFunctionVarargKeyword(num_positional, num_keyword);

        instruction("CALL FUNCTION VARARG KEYWORD", 3, "" + num_positional
                + " positional and " + num_keyword + " keywords.");
    }

    @Override
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

    @Override
    public void visitContinueLoop(Label loopStart) {
        super.visitContinueLoop(loopStart);

        instruction("CONTINUE LOOP", 3, loopStart);
    }

    @Override
    public void visitDeleteAttribute(String attributeName) {
        super.visitDeleteAttribute(attributeName);

        instruction("DELETE ATTRIBUTE", 3, attributeName);
    }

    @Override
    public void visitDeleteFast(String variableName) {
        super.visitDeleteFast(variableName);

        instruction("DELETE FAST", 3, variableName);
    }

    @Override
    public void visitDeleteGlobal(String variableName) {
        super.visitDeleteGlobal(variableName);

        instruction("DELETE GLOBAL", 3, variableName);
    }

    @Override
    public void visitDeleteName(String variableName) {
        super.visitDeleteName(variableName);

        instruction("DELETE NAME", 3, variableName);
    }

    @Override
    public void visitDeleteSlice(int plus) {
        super.visitDeleteSlice(plus);

        instruction("DELETE SLICE +" + plus);
    }

    @Override
    public void visitDeleteSubscript() {
        super.visitDeleteSubscript();

        instruction("DELETE SUBSCRIPT");
    }

    @Override
    public void visitDup(int num_elements) {
        super.visitDup(num_elements);

        if (num_elements == 1) {
            instruction("DUP TOP");
        } else {
            instruction("DUP TOPX", 3, new Integer(num_elements));
        }
    }

    @Override
    public void visitEndFinally() {
        super.visitEndFinally();

        instruction("END FINALLY");
    }

    @Override
    public void visitExecStatement() {
        super.visitExecStatement();

        instruction("EXEC STMT");
    }

    @Override
    public void visitForIteration(Label end) {
        super.visitForIteration(end);

        instruction("FOR ITER", 3, end);
    }

    @Override
    public void visitGetIterator() {
        super.visitGetIterator();

        instruction("GET ITER");
    }

    @Override
    public void visitImportFrom(String name) {
        super.visitImportFrom(name);

        instruction("IMPORT FROM", 3, name);
    }

    @Override
    public void visitImportName(String name) {
        super.visitImportName(name);

        instruction("IMPORT NAME", 3, name);
    }

    @Override
    public void visitImportStar() {
        super.visitImportStar();

        instruction("IMPORT STAR");
    }

    @Override
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

    @Override
    public void visitJump(Label destination) {
        super.visitJump(destination);

        instruction("JUMP", 3, destination);
    }

    @Override
    public void visitJumpIfFalse(Label destination) {
        super.visitJumpIfFalse(destination);

        instruction("JUMP IF FALSE", 3, destination);
    }

    @Override
    public void visitJumpIfTrue(Label destination) {
        super.visitJumpIfTrue(destination);

        instruction("JUMP IF TRUE", 3, destination);
    }

    @Override
    public void visitLabel(Label label) {
        super.visitLabel(label);
        debugger.putLabel(label.toString());
    }

    @Override
    public void visitLineNumber(int lineNumber) {
        super.visitLineNumber(lineNumber);
    }

    @Override
    public void visitListAppend() {
        super.visitListAppend();

        instruction("LIST APPEND");
    }

    @Override
    public void visitLoadAttribute(String attributeName) {
        super.visitLoadAttribute(attributeName);

        instruction("LOAD ATTRIBUTE", 3, attributeName);
    }

    @Override
    public void visitLoadClosure(String variableName) {
        super.visitLoadClosure(variableName);

        instruction("LOAD CLOSURE", 3, variableName);
    }

    @Override
    public void visitLoadConstant(PyObject constant) {
        super.visitLoadConstant(constant);

        instruction("LOAD CONSTANT", 3, constant.__repr__());
    }

    @Override
    public void visitLoadDeref(String variableName) {
        super.visitLoadDeref(variableName);

        instruction("LOAD DEREF", 3, variableName);
    }

    @Override
    public void visitLoadFast(String variableName) {
        super.visitLoadFast(variableName);

        instruction("LOAD FAST", 3, variableName);
    }

    @Override
    public void visitLoadGlobal(String variableName) {
        super.visitLoadGlobal(variableName);

        instruction("LOAD GLOBAL", 3, variableName);
    }

    @Override
    public void visitLoadLocals() {
        super.visitLoadLocals();

        instruction("LOAD LOCALS");
    }

    @Override
    public void visitLoadName(String variableName) {
        super.visitLoadName(variableName);

        instruction("LOAD NAME", 3, variableName);
    }

    @Override
    public void visitLoadSlice(int plus) {
        super.visitLoadSlice(plus);

        instruction("LOAD SLICE +" + plus);
    }

    @Override
    public void visitMakeClosure(int num_default) {
        super.visitMakeClosure(num_default);

        instruction("MAKE CLOSURE", 3, "" + num_default + " default arguments");
    }

    @Override
    public void visitMakeFunction(int num_default) {
        super.visitMakeFunction(num_default);

        instruction("MAKE FUNCTION", 3, "" + num_default + " default arguments");
    }

    @Override
    public void visitNOP() {
        super.visitNOP();

        instruction("NOP");
    }

    @Override
    public void visitPop() {
        super.visitPop();

        instruction("POP TOP");
    }

    @Override
    public void visitPopBlock() {
        super.visitPopBlock();

        instruction("POP BLOCK");
    }

    @Override
    public void visitPrintExpression() {
        super.visitPrintExpression();

        instruction("PRINT EXPR");
    }

    @Override
    public void visitPrintItem() {
        super.visitPrintItem();

        instruction("PRINT ITEM");
    }

    @Override
    public void visitPrintItemTo() {
        super.visitPrintItemTo();

        instruction("PRINT ITEM TO");
    }

    @Override
    public void visitPrintNewline() {
        super.visitPrintNewline();

        instruction("PRINT NEWLINE");
    }

    @Override
    public void visitPrintNewlineTo() {
        super.visitPrintNewlineTo();

        instruction("PRINT NEWLINE TO");
    }

    @Override
    public void visitRaiseVarargs(int count) {
        super.visitRaiseVarargs(count);

        instruction("RAISE VARARGS", 3, new Integer(count));
    }

    @Override
    public void visitReturnValue() {
        super.visitReturnValue();

        instruction("RETURN VALUE");
    }

    @Override
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

    @Override
    public void visitSetupExcept(Label startExcept) {
        super.visitSetupExcept(startExcept);

        instruction("SETUP EXCEPT", 3, startExcept);
    }

    @Override
    public void visitSetupFinally(Label startFinally) {
        super.visitSetupFinally(startFinally);

        instruction("SETUP FINALLY", 3, startFinally);
    }

    @Override
    public void visitSetupLoop(Label end) {
        super.visitSetupLoop(end);

        instruction("SETUP LOOP", 3, end);
    }

    @Override
    public void visitStopCode() {
        super.visitStopCode();

        instruction("STOP CODE");
    }

    @Override
    public void visitStoreAttribute(String attributeName) {
        super.visitStoreAttribute(attributeName);

        instruction("STORE ATTR", 3, attributeName);
    }

    @Override
    public void visitStoreDeref(String variableName) {
        super.visitStoreDeref(variableName);

        instruction("STORE DEREF", 3, variableName);
    }

    @Override
    public void visitStoreFast(String variableName) {
        super.visitStoreFast(variableName);

        instruction("STORE FAST", 3, variableName);
    }

    @Override
    public void visitStoreGlobal(String variableName) {
        super.visitStoreGlobal(variableName);

        instruction("STORE GLOBAL", 3, variableName);
    }

    @Override
    public void visitStoreName(String variableName) {
        super.visitStoreName(variableName);

        instruction("STORE NAME", 3, variableName);
    }

    @Override
    public void visitStoreSlice(int plus) {
        super.visitStoreSlice(plus);

        instruction("STORE SLICE +" + plus);
    }

    @Override
    public void visitStoreSubscript() {
        super.visitStoreSubscript();

        instruction("STORE SUBSCRIPT");
    }

    @Override
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

    @Override
    public void visitUnpackSequence(int count) {
        super.visitUnpackSequence(count);

        instruction("UNPACK SEQUENCE", 3, new Integer(count));
    }

    @Override
    public void visitWithCleanup() {
        super.visitWithCleanup();

        instruction("WITH CLEANUP");
    }

    @Override
    public void visitYieldValue(int index, Label resume) {
        super.visitYieldValue(index, resume);

        instruction("YIELD VALUE");
    }

}
