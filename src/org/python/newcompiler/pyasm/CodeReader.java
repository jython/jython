// (C) Copyright 2007 Tobias Ivarsson
package org.python.newcompiler.pyasm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * @author Tobias Ivarsson
 * 
 */
public class CodeReader implements PythonOpCodes {

    private interface VisitableInstruction {

        /**
         * Let a {@link BytecodeVisitor} visit the instruction held by this
         * {@link VisitableInstruction}.
         * 
         * @param codeVisitor The {@link BytecodeVisitor} that should visit the
         *            instruction.
         */
        public void accept(BytecodeVisitor codeVisitor);

    }

    private class LabelInstruction implements VisitableInstruction {

        private Label label;

        private LabelInstruction(Label label) {
            this.label = label;
        }

        public void accept(BytecodeVisitor codeVisitor) {
            codeVisitor.visitLabel(label);
        }

        public String toString() {
            return "LabelInstruction:" + label;
        }

    }

    private class LineNumberInstruction implements VisitableInstruction {
        private int lineNumber;

        private LineNumberInstruction(int lineNumber) {
            this.lineNumber = lineNumber;
        }

        public void accept(BytecodeVisitor codeVisitor) {
            codeVisitor.visitLineNumber(lineNumber);
        }
    }

    private class JumpInstruction implements VisitableInstruction {

        private static final int REGULAR_JUMP = 0;
        private static final int JUMP_IF_TRUE = 1;
        private static final int JUMP_IF_FALSE = 2;

        private Label label;
        private int kind;

        private JumpInstruction(int kind, Label label) {
            this.kind = kind;
            this.label = label;
        }

        private JumpInstruction(Label label) {
            this(REGULAR_JUMP, label);
        }

        private JumpInstruction(Label label, boolean inCase) {
            this(inCase ? JUMP_IF_TRUE : JUMP_IF_FALSE, label);
        }

        public void accept(BytecodeVisitor codeVisitor) {
            switch (kind) {
            case REGULAR_JUMP:
                codeVisitor.visitJump(label);
                break;
            case JUMP_IF_TRUE:
                codeVisitor.visitJumpIfTrue(label);
                break;
            case JUMP_IF_FALSE:
                codeVisitor.visitJumpIfFalse(label);
                break;
            default:
                break;
            }
        }

    }

    private class RawVisitor {

        private SortedMap instructions = new TreeMap();
        private Map labels = new HashMap();
        private List generatorLabels = new ArrayList();

        private Label newLabel(int address) {
            Integer addr = new Integer(address);
            if (labels.containsKey(addr)) {
                return ((LabelInstruction) labels.get(addr)).label;
            } else {
                Label label = new Label();
                labels.put(addr, new LabelInstruction(label));
                return label;
            }
        }

        private void addInstruction(VisitableInstruction instruction) {
            instructions.put(new Integer(codePosition), instruction);
        }

        Iterable compile() {
            List result = new ArrayList();
            if (!generatorLabels.isEmpty()) {
                final Label start = new Label();
                final Label[] resume = new Label[generatorLabels.size()];
                int i = 0;
                for (Iterator labels = generatorLabels.iterator(); labels.hasNext(); i++) {
                    resume[i] = (Label) labels.next();
                }
                result.add(new VisitableInstruction() {
                    public void accept(BytecodeVisitor codeVisitor) {
                        codeVisitor.visitResumeTable(start, resume);
                        codeVisitor.visitLabel(start);
                    }
                });
            }
            for (Iterator entries = instructions.entrySet().iterator(); entries.hasNext();) {
                Map.Entry entry = (Map.Entry) entries.next();
                if (lineNumberTable.containsKey(entry.getKey())) {
                    result.add(new LineNumberInstruction(
                            ((Integer) lineNumberTable.get(entry.getKey())).intValue()));
                }
                if (labels.containsKey(entry.getKey())) {
                    result.add(labels.remove(entry.getKey()));
                }
                result.add(entry.getValue());
            }
            if (!labels.isEmpty()) {
                System.err.println("UNALIGNING LABELS:");
                for (Iterator entries = labels.entrySet().iterator(); entries.hasNext();) {
                    Map.Entry entry = (Map.Entry) entries.next();
                    System.err.println("" + entry.getKey() + ": "
                            + entry.getValue());
                }
                throw new RuntimeException("Labels and code does not align!");
            }
            return result;
        }

        /* INSTRUCTIONS */

        void visitStopCode() {
            addInstruction(new VisitableInstruction() {
                public void accept(BytecodeVisitor codeVisitor) {
                    codeVisitor.visitStopCode();
                }
            });
        }

        // Simple stack operations
        void visitNOP() {
            addInstruction(new VisitableInstruction() {
                public void accept(BytecodeVisitor codeVisitor) {
                    codeVisitor.visitNOP();
                }
            });
        }

        void visitPop() {
            addInstruction(new VisitableInstruction() {
                public void accept(BytecodeVisitor codeVisitor) {
                    codeVisitor.visitPop();
                }
            });
        }

        void visitDup() {
            addInstruction(new VisitableInstruction() {
                public void accept(BytecodeVisitor codeVisitor) {
                    codeVisitor.visitDup(1);
                }
            });
        }

        void visitRotTwo() {
            addInstruction(new VisitableInstruction() {
                public void accept(BytecodeVisitor codeVisitor) {
                    codeVisitor.visitRot(2);
                }
            });
        }

        void visitRotThree() {
            addInstruction(new VisitableInstruction() {
                public void accept(BytecodeVisitor codeVisitor) {
                    codeVisitor.visitRot(3);
                }
            });
        }

        void visitRotFour() {
            addInstruction(new VisitableInstruction() {
                public void accept(BytecodeVisitor codeVisitor) {
                    codeVisitor.visitRot(4);
                }
            });
        }

        void visitDupTopX(final int x) {
            addInstruction(new VisitableInstruction() {
                public void accept(BytecodeVisitor codeVisitor) {
                    codeVisitor.visitDup(x);
                }
            });
        }

        // Operators

        void visitUnaryOperation(int op) {
            final int operator;
            switch (op) {
            case UNARY_INVERT:
                operator = Operator.INVERT;
                break;
            case UNARY_POSITIVE:
                operator = Operator.POSITIVE;
                break;
            case UNARY_NEGATIVE:
                operator = Operator.NEGATIVE;
                break;
            case UNARY_NOT:
                operator = Operator.NOT;
                break;
            case UNARY_CONVERT:
                operator = Operator.CONVERT;
                break;

            default:
                throw new RuntimeException(
                        "Unimplemented Unary Operation op code: '" + op + "'.");
            }

            addInstruction(new VisitableInstruction() {
                public void accept(BytecodeVisitor codeVisitor) {
                    codeVisitor.visitUnaryOperator(operator);
                }
            });
        }

        void visitBinaryOperator(int op) {
            final int operator;
            switch (op) {
            case BINARY_ADD:
                operator = Operator.ADD;
                break;
            case BINARY_SUBTRACT:
                operator = Operator.SUBTRACT;
                break;
            case BINARY_MULTIPLY:
                operator = Operator.MULTIPLY;
                break;
            case BINARY_DIVIDE:
                operator = Operator.DIVIDE;
                break;
            case BINARY_FLOOR_DIVIDE:
                operator = Operator.FLOOR_DIVIDE;
                break;
            case BINARY_TRUE_DIVIDE:
                operator = Operator.TRUE_DIVIDE;
                break;
            case BINARY_MODULO:
                operator = Operator.MODULO;
                break;
            case BINARY_POWER:
                operator = Operator.POWER;
                break;
            case BINARY_LSHIFT:
                operator = Operator.LSHIFT;
                break;
            case BINARY_RSHIFT:
                operator = Operator.RSHIFT;
                break;
            case BINARY_AND:
                operator = Operator.AND;
                break;
            case BINARY_OR:
                operator = Operator.OR;
                break;
            case BINARY_XOR:
                operator = Operator.XOR;
                break;
            case BINARY_SUBSCR:
                operator = Operator.SUBSCRIPT;
                break;

            default:
                throw new RuntimeException(
                        "Unimplemented Binary Operation op code: '" + op + "'.");
            }

            addInstruction(new VisitableInstruction() {
                public void accept(BytecodeVisitor codeVisitor) {
                    codeVisitor.visitBinaryOperator(operator);
                }
            });
        }

        void visitInplaceOperator(int op) {
            final int operator;
            switch (op) {
            case INPLACE_ADD:
                operator = Operator.ADD;
                break;
            case INPLACE_SUBTRACT:
                operator = Operator.SUBTRACT;
                break;
            case INPLACE_MULTIPLY:
                operator = Operator.MULTIPLY;
                break;
            case INPLACE_DIVIDE:
                operator = Operator.DIVIDE;
                break;
            case INPLACE_FLOOR_DIVIDE:
                operator = Operator.FLOOR_DIVIDE;
                break;
            case INPLACE_TRUE_DIVIDE:
                operator = Operator.TRUE_DIVIDE;
                break;
            case INPLACE_MODULO:
                operator = Operator.MODULO;
                break;
            case INPLACE_POWER:
                operator = Operator.POWER;
                break;
            case INPLACE_LSHIFT:
                operator = Operator.LSHIFT;
                break;
            case INPLACE_RSHIFT:
                operator = Operator.RSHIFT;
                break;
            case INPLACE_AND:
                operator = Operator.AND;
                break;
            case INPLACE_OR:
                operator = Operator.OR;
                break;
            case INPLACE_XOR:
                operator = Operator.XOR;
                break;

            default:
                throw new RuntimeException(
                        "Unimplemented Inplace Operation op code: '" + op
                                + "'.");
            }

            addInstruction(new VisitableInstruction() {
                public void accept(BytecodeVisitor codeVisitor) {
                    codeVisitor.visitInplaceOperator(operator);
                }
            });
        }

        void visitCompareOperator(final int opid) {
            addInstruction(new VisitableInstruction() {
                public void accept(BytecodeVisitor codeVisitor) {
                    codeVisitor.visitCompareOperator(opid);
                }
            });
        }

        // List operations

        void visitListAppend() {
            addInstruction(new VisitableInstruction() {
                public void accept(BytecodeVisitor codeVisitor) {
                    codeVisitor.visitListAppend();
                }
            });
        }

        void visitStoreSubscript() {
            addInstruction(new VisitableInstruction() {
                public void accept(BytecodeVisitor codeVisitor) {
                    codeVisitor.visitStoreSubscript();
                }
            });
        }

        void visitDeleteSubscript() {
            addInstruction(new VisitableInstruction() {
                public void accept(BytecodeVisitor codeVisitor) {
                    codeVisitor.visitDeleteSubscript();
                }
            });
        }

        void visitSlice(final int plus) {
            addInstruction(new VisitableInstruction() {
                public void accept(BytecodeVisitor codeVisitor) {
                    codeVisitor.visitLoadSlice(plus);
                }
            });
        }

        void visitStoreSlice(final int plus) {
            addInstruction(new VisitableInstruction() {
                public void accept(BytecodeVisitor codeVisitor) {
                    codeVisitor.visitStoreSlice(plus);
                }
            });
        }

        void visitDeleteSlice(final int plus) {
            addInstruction(new VisitableInstruction() {
                public void accept(BytecodeVisitor codeVisitor) {
                    codeVisitor.visitDeleteSlice(plus);
                }
            });
        }

        void visitBuildSlice(final int numargs) {
            addInstruction(new VisitableInstruction() {
                public void accept(BytecodeVisitor codeVisitor) {
                    codeVisitor.visitBuildSlice(numargs);
                }
            });
        }

        // Print methods

        void visitPrintExpression() {
            addInstruction(new VisitableInstruction() {
                public void accept(BytecodeVisitor codeVisitor) {
                    codeVisitor.visitPrintExpression();
                }
            });
        }

        void visitPrintItem() {
            addInstruction(new VisitableInstruction() {
                public void accept(BytecodeVisitor codeVisitor) {
                    codeVisitor.visitPrintItem();
                }
            });
        }

        void visitPrintNewline() {
            addInstruction(new VisitableInstruction() {
                public void accept(BytecodeVisitor codeVisitor) {
                    codeVisitor.visitPrintNewline();
                }
            });
        }

        void visitPrintItemTo() {
            addInstruction(new VisitableInstruction() {
                public void accept(BytecodeVisitor codeVisitor) {
                    codeVisitor.visitPrintItemTo();
                }
            });
        }

        void visitPrintNewlineTo() {
            addInstruction(new VisitableInstruction() {
                public void accept(BytecodeVisitor codeVisitor) {
                    codeVisitor.visitPrintNewlineTo();
                }
            });
        }

        // Return points

        void visitReturnValue() {
            addInstruction(new VisitableInstruction() {
                public void accept(BytecodeVisitor codeVisitor) {
                    codeVisitor.visitReturnValue();
                }
            });
        }

        void visitYieldValue() {
            final Label resume = new Label();
            generatorLabels.add(resume);
            final int index = generatorLabels.size();
            addInstruction(new VisitableInstruction() {
                public void accept(BytecodeVisitor codeVisitor) {
                    codeVisitor.visitYieldValue(index, resume);
                }
            });
        }

        void visitRaiseVarargs(final int count) {
            addInstruction(new VisitableInstruction() {
                public void accept(BytecodeVisitor codeVisitor) {
                    codeVisitor.visitRaiseVarargs(count);
                }
            });
        }

        // Misc.

        void visitWithCleanup() {
            addInstruction(new VisitableInstruction() {
                public void accept(BytecodeVisitor codeVisitor) {
                    codeVisitor.visitWithCleanup();
                }
            });
        }

        void visitExecStatement() {
            addInstruction(new VisitableInstruction() {
                public void accept(BytecodeVisitor codeVisitor) {
                    codeVisitor.visitExecStatement();
                }
            });
        }

        void visitUnpackSequence(final int count) {
            addInstruction(new VisitableInstruction() {
                public void accept(BytecodeVisitor codeVisitor) {
                    codeVisitor.visitUnpackSequence(count);
                }
            });
        }

        // Import operations

        void visitImportName(final int index) {
            addInstruction(new VisitableInstruction() {
                public void accept(BytecodeVisitor codeVisitor) {
                    codeVisitor.visitImportName(codeVisitor.getName(index));
                }
            });
        }

        void visitImportFrom(final int index) {
            addInstruction(new VisitableInstruction() {
                public void accept(BytecodeVisitor codeVisitor) {
                    codeVisitor.visitImportFrom(codeVisitor.getName(index));
                }
            });
        }

        void visitImportStar() {
            addInstruction(new VisitableInstruction() {
                public void accept(BytecodeVisitor codeVisitor) {
                    codeVisitor.visitImportStar();
                }
            });
        }

        // Loading and storeing

        void visitLoadLocals() {
            addInstruction(new VisitableInstruction() {
                public void accept(BytecodeVisitor codeVisitor) {
                    codeVisitor.visitLoadLocals();
                }
            });
        }

        void visitLoadConstant(final int index) {
            addInstruction(new VisitableInstruction() {
                public void accept(BytecodeVisitor codeVisitor) {
                    codeVisitor.visitLoadConstant(codeVisitor.getConstant(index));
                }
            });
        }

        void visitLoadName(final int index) {
            addInstruction(new VisitableInstruction() {
                public void accept(BytecodeVisitor codeVisitor) {
                    codeVisitor.visitLoadName(codeVisitor.getName(index));
                }
            });
        }

        void visitStoreName(final int index) {
            addInstruction(new VisitableInstruction() {
                public void accept(BytecodeVisitor codeVisitor) {
                    codeVisitor.visitStoreName(codeVisitor.getName(index));
                }
            });
        }

        void visitDeleteName(final int index) {
            addInstruction(new VisitableInstruction() {
                public void accept(BytecodeVisitor codeVisitor) {
                    codeVisitor.visitDeleteName(codeVisitor.getName(index));
                }
            });
        }

        void visitLoadFast(final int index) {
            addInstruction(new VisitableInstruction() {
                public void accept(BytecodeVisitor codeVisitor) {
                    codeVisitor.visitLoadFast(codeVisitor.getVariableName(index));
                }
            });
        }

        void visitStoreFast(final int index) {
            addInstruction(new VisitableInstruction() {
                public void accept(BytecodeVisitor codeVisitor) {
                    codeVisitor.visitStoreFast(codeVisitor.getVariableName(index));
                }
            });
        }

        void visitDeleteFast(final int index) {
            addInstruction(new VisitableInstruction() {
                public void accept(BytecodeVisitor codeVisitor) {
                    codeVisitor.visitDeleteFast(codeVisitor.getVariableName(index));
                }
            });
        }

        void visitLoadGlobal(final int index) {
            addInstruction(new VisitableInstruction() {
                public void accept(BytecodeVisitor codeVisitor) {
                    codeVisitor.visitLoadGlobal(codeVisitor.getName(index));
                }
            });
        }

        void visitStoreGlobal(final int index) {
            addInstruction(new VisitableInstruction() {
                public void accept(BytecodeVisitor codeVisitor) {
                    codeVisitor.visitStoreGlobal(codeVisitor.getName(index));
                }
            });
        }

        void visitDeleteGlobal(final int index) {
            addInstruction(new VisitableInstruction() {
                public void accept(BytecodeVisitor codeVisitor) {
                    codeVisitor.visitDeleteGlobal(codeVisitor.getName(index));
                }
            });
        }

        void visitLoadAttribute(final int index) {
            addInstruction(new VisitableInstruction() {
                public void accept(BytecodeVisitor codeVisitor) {
                    codeVisitor.visitLoadAttribute(codeVisitor.getName(index));
                }
            });
        }

        void visitStoreAttribute(final int index) {
            addInstruction(new VisitableInstruction() {
                public void accept(BytecodeVisitor codeVisitor) {
                    codeVisitor.visitStoreAttribute(codeVisitor.getName(index));
                }
            });
        }

        void visitDeleteAttribute(final int index) {
            addInstruction(new VisitableInstruction() {
                public void accept(BytecodeVisitor codeVisitor) {
                    codeVisitor.visitDeleteAttribute(codeVisitor.getName(index));
                }
            });
        }

        // Constructing builtin datastructures

        void visitBuildTuple(final int size) {
            addInstruction(new VisitableInstruction() {
                public void accept(BytecodeVisitor codeVisitor) {
                    codeVisitor.visitBuildTuple(size);
                }
            });
        }

        void visitBuildList(final int size) {
            addInstruction(new VisitableInstruction() {
                public void accept(BytecodeVisitor codeVisitor) {
                    codeVisitor.visitBuildList(size);
                }
            });
        }

        void visitBuildMap(final int size) {
            addInstruction(new VisitableInstruction() {
                public void accept(BytecodeVisitor codeVisitor) {
                    codeVisitor.visitBuildMap(size);
                }
            });
        }

        void visitBuildClass() {
            addInstruction(new VisitableInstruction() {
                public void accept(BytecodeVisitor codeVisitor) {
                    codeVisitor.visitBuildClass();
                }
            });
        }

        // Defining functions

        void visitMakeFunction(final int num_default) {
            addInstruction(new VisitableInstruction() {
                public void accept(BytecodeVisitor codeVisitor) {
                    codeVisitor.visitMakeFunction(num_default);
                }
            });
        }

        void visitMakeClosure(final int num_default) {
            addInstruction(new VisitableInstruction() {
                public void accept(BytecodeVisitor codeVisitor) {
                    codeVisitor.visitMakeClosure(num_default);
                }
            });
        }

        void visitLoadClosure(final int index) {
            addInstruction(new VisitableInstruction() {
                public void accept(BytecodeVisitor codeVisitor) {
                    codeVisitor.visitLoadClosure(codeVisitor.getOuterName(index));
                }
            });
        }

        void visitLoadDeref(final int index) {
            addInstruction(new VisitableInstruction() {
                public void accept(BytecodeVisitor codeVisitor) {
                    codeVisitor.visitLoadDeref(codeVisitor.getOuterName(index));
                }
            });
        }

        void visitStoreDeref(final int index) {
            addInstruction(new VisitableInstruction() {
                public void accept(BytecodeVisitor codeVisitor) {
                    codeVisitor.visitStoreDeref(codeVisitor.getOuterName(index));
                }
            });
        }

        // Function calls

        void visitCallFunction(int arg) {
            final int num_positional = arg & 255;
            final int num_keyword = (arg >> 8) & 255;
            addInstruction(new VisitableInstruction() {
                public void accept(BytecodeVisitor codeVisitor) {
                    codeVisitor.visitCallFunction(num_positional, num_keyword);
                }
            });
        }

        void visitCallFunctionVararg(int arg) {
            final int num_positional = arg & 255;
            final int num_keyword = (arg >> 8) & 255;
            addInstruction(new VisitableInstruction() {
                public void accept(BytecodeVisitor codeVisitor) {
                    codeVisitor.visitCallFunctionVararg(num_positional,
                            num_keyword);
                }
            });
        }

        void visitCallFuntionKeyword(int arg) {
            final int num_positional = arg & 255;
            final int num_keyword = (arg >> 8) & 255;
            addInstruction(new VisitableInstruction() {
                public void accept(BytecodeVisitor codeVisitor) {
                    codeVisitor.visitCallFunctionKeyword(num_positional,
                            num_keyword);
                }
            });
        }

        void visitCallFunctionVarargKeyword(int arg) {
            final int num_positional = arg & 255;
            final int num_keyword = (arg >> 8) & 255;
            addInstruction(new VisitableInstruction() {
                public void accept(BytecodeVisitor codeVisitor) {
                    codeVisitor.visitCallFunctionVarargKeyword(num_positional,
                            num_keyword);
                }
            });
        }

        // Iteration operations

        void visitGetIterator() {
            addInstruction(new VisitableInstruction() {
                public void accept(BytecodeVisitor codeVisitor) {
                    codeVisitor.visitGetIterator();
                }
            });
        }

        void visitBreakLoop() {
            addInstruction(new VisitableInstruction() {
                public void accept(BytecodeVisitor codeVisitor) {
                    codeVisitor.visitBreakLoop();
                }
            });
        }

        // Block defining instructions

        void visitPopBlock() {
            addInstruction(new VisitableInstruction() {
                public void accept(BytecodeVisitor codeVisitor) {
                    codeVisitor.visitPopBlock();
                }
            });
        }

        void visitEndFinally() {
            addInstruction(new VisitableInstruction() {
                public void accept(BytecodeVisitor codeVisitor) {
                    codeVisitor.visitEndFinally();
                }
            });
        }

        void visitForIteration(int arg) {
            final Label label = newLabel(getAbsoluteAddress(arg));
            addInstruction(new VisitableInstruction() {
                public void accept(BytecodeVisitor codeVisitor) {
                    codeVisitor.visitForIteration(label);
                }
            });
        }

        void visitContinueLoop(int arg) {
            final Label label = newLabel(arg);
            addInstruction(new VisitableInstruction() {
                public void accept(BytecodeVisitor codeVisitor) {
                    codeVisitor.visitContinueLoop(label);
                }
            });
        }

        void visitSetupLoop(int arg) {
            final Label label = newLabel(getAbsoluteAddress(arg));
            addInstruction(new VisitableInstruction() {
                public void accept(BytecodeVisitor codeVisitor) {
                    codeVisitor.visitSetupLoop(label);
                }
            });
        }

        void visitSetupExcept(int arg) {
            final Label label = newLabel(getAbsoluteAddress(arg));
            addInstruction(new VisitableInstruction() {
                public void accept(BytecodeVisitor codeVisitor) {
                    codeVisitor.visitSetupExcept(label);
                }
            });
        }

        void visitSetupFinally(int arg) {
            final Label label = newLabel(getAbsoluteAddress(arg));
            addInstruction(new VisitableInstruction() {
                public void accept(BytecodeVisitor codeVisitor) {
                    codeVisitor.visitSetupFinally(label);
                }
            });
        }

        // Jump instructions

        void visitJumpForward(int arg) {
            addInstruction(new JumpInstruction(
                    newLabel(getAbsoluteAddress(arg))));
        }

        void visitJumpIfTrue(int arg) {
            addInstruction(new JumpInstruction(
                    newLabel(getAbsoluteAddress(arg)), true));
        }

        void visitJumpIfFalse(int arg) {
            addInstruction(new JumpInstruction(
                    newLabel(getAbsoluteAddress(arg)), false));
        }

        void visitJumpAbsolute(int arg) {
            addInstruction(new JumpInstruction(newLabel(arg)));
        }

    }

    private char[] bytes;
    private int pos, codePosition;
    private Iterable instructions;
    private SortedMap lineNumberTable;

    /**
     * Create a new {@link CodeReader} from a byte array that represent the
     * Python byte code.
     * 
     * FIXME: This (only) constructor starts the interpretation of the byte code
     * right away. This meens that it is impossible to override the
     * {@link #read()}/{@link #hasData()} methods as described and achive
     * correct behaviour. This can be fixed in a number of ways:
     * 
     * 1. The interpretation can be started at the first invocation of
     * {@link #accept(BytecodeVisitor)}.
     * 
     * 2. The interpretatin method can be made <code>protected</code>, and a
     * protected empty constructor could be provided.
     * 
     * 3. The {@link #read()} and {@link #hasData()} methods can be moved to a
     * separate object (an {@link Iterator}?).
     * 
     * @param bytes An array of bytes containing Python byte code.
     * @param firstLineNumber The first line number in source of this code
     *            object.
     * @param lineNumberTable The line number increment table. (Documented in
     *            compile.c in the Python source)
     * @throws BytecodeError when errors are found in the bytecode.
     */
    public CodeReader(char[] bytes, int firstLineNumber, char[] lineNumberTable) throws BytecodeError {
        this.bytes = bytes;
        pos = 0;

        this.lineNumberTable = buildLineNumberTable(firstLineNumber,
                lineNumberTable);

        instructions = interpret();
    }

    /**
     * Transform a table of line numbers (lnotab) to a mapping from byte code
     * offsets to line numbers.
     * 
     * The resulting mapping will only contain entries for each (significant)
     * line number, not for each byte code offset.
     * 
     * @param lineNumber The initial line number for the entire code block.
     * @param lineNumberTable The line number table according to the lnotab
     *            specification in the Python compile.c
     * @return A {@link Map} from byte code offsets to line numbers.
     */
    public static SortedMap buildLineNumberTable(int lineNumber,
            char[] lineNumberTable) {
        SortedMap table = new TreeMap();
        int bytecodeOffset = 0;

        for (int i = 0; i < lineNumberTable.length;) {
            bytecodeOffset += lineNumberTable[i++];
            char lineInc = lineNumberTable[i++];
            lineNumber += lineInc;
            if (lineInc != 0) {
                table.put(new Integer(bytecodeOffset), new Integer(lineNumber));
            }
        }

        return table;
    }

    /**
     * Let a {@link BytecodeVisitor} visit the instructions in the code read by this
     * {@link CodeReader}.
     * 
     * @param visitor The {@link BytecodeVisitor} that should visit the
     *            instructions.
     */
    public final void accept(BytecodeVisitor visitor) {
        for (Iterator iter = instructions.iterator(); iter.hasNext();) {
            ((VisitableInstruction) iter.next()).accept(visitor);
        }
        visitor.visitEnd();
    }

    /**
     * The {@link #read()} and {@link #hasData()} methods can be overridden to
     * create a {@link CodeReader} that reads from annother kind of source.
     * 
     * These are the only methods needed for reading since the code is read only
     * once, regardless of the number of {@link BytecodeVisitor}s that visit the
     * code.
     * 
     * @return The next byte in the sequence.
     */
    protected char read() {
        return bytes[pos++];
    }

    /**
     * The {@link #read()} and {@link #hasData()} methods can be overridden to
     * create a {@link CodeReader} that reads from annother kind of source.
     * 
     * These are the only methods needed for reading since the code is read only
     * once, regardless of the number of {@link BytecodeVisitor}s that visit the
     * code.
     * 
     * @return <code>true</code> if there are more bytes in the code that can
     *         be read, <code>false</code> otherwise.
     */
    protected boolean hasData() {
        return pos < bytes.length;
    }

    private int readArg() {
        int a = read();
        int b = read();
        return a + (b << 8);
    }

    private int getAbsoluteAddress(int relative) {
        return pos + relative;
    }

    private Iterable interpret() throws BytecodeError {
        RawVisitor visitor = new RawVisitor();

        while (hasData()) {

            codePosition = pos;

            int op = read();
            int arg = 0;

            if (op >= __HAVE_ARGUMENT) {
                arg = readArg();

                if (op == EXTENDED_ARG) {
                    op = read();
                    arg = (arg << 16) + readArg();
                }
            }

            switch (op) {
            case STOP_CODE:
                visitor.visitStopCode();
                break;
            // This marks the end of the code block
            // return visitor.compile();
            case POP_TOP:
                visitor.visitPop();
                break;
            case ROT_TWO:
                visitor.visitRotTwo();
                break;
            case ROT_THREE:
                visitor.visitRotThree();
                break;
            case DUP_TOP:
                visitor.visitDup();
                break;
            case ROT_FOUR:
                visitor.visitRotFour();
                break;

            case NOP:
                visitor.visitNOP();
                break;
            case UNARY_POSITIVE:
            case UNARY_NEGATIVE:
            case UNARY_NOT:
            case UNARY_CONVERT:

            case UNARY_INVERT:
                visitor.visitUnaryOperation(op);
                break;

            case LIST_APPEND:
                visitor.visitListAppend();
                break;
            case BINARY_POWER:
            case BINARY_MULTIPLY:
            case BINARY_DIVIDE:
            case BINARY_MODULO:
            case BINARY_ADD:
            case BINARY_SUBTRACT:
            case BINARY_SUBSCR:
            case BINARY_FLOOR_DIVIDE:
            case BINARY_TRUE_DIVIDE:
                visitor.visitBinaryOperator(op);
                break;
            case INPLACE_FLOOR_DIVIDE:
            case INPLACE_TRUE_DIVIDE:
                visitor.visitInplaceOperator(op);
                break;
            case SLICE__0:
                visitor.visitSlice(0);
                break;
            case SLICE__1:
                visitor.visitSlice(1);
                break;
            case SLICE__2:
                visitor.visitSlice(2);
                break;
            case SLICE__3:
                visitor.visitSlice(3);
                break;

            case STORE_SLICE__0:
                visitor.visitStoreSlice(0);
                break;
            case STORE_SLICE__1:
                visitor.visitStoreSlice(1);
                break;
            case STORE_SLICE__2:
                visitor.visitStoreSlice(2);
                break;
            case STORE_SLICE__3:
                visitor.visitStoreSlice(3);
                break;

            case DELETE_SLICE__0:
                visitor.visitDeleteSlice(0);
                break;
            case DELETE_SLICE__1:
                visitor.visitDeleteSlice(1);
                break;
            case DELETE_SLICE__2:
                visitor.visitDeleteSlice(2);
                break;
            case DELETE_SLICE__3:
                visitor.visitDeleteSlice(3);
                break;

            case INPLACE_ADD:
            case INPLACE_SUBTRACT:
            case INPLACE_MULTIPLY:
            case INPLACE_DIVIDE:
            case INPLACE_MODULO:
                visitor.visitInplaceOperator(op);
                break;
            case STORE_SUBSCR:
                visitor.visitStoreSubscript();
                break;
            case DELETE_SUBSCR:
                visitor.visitDeleteSubscript();
                break;
            case BINARY_LSHIFT:
            case BINARY_RSHIFT:
            case BINARY_AND:
            case BINARY_XOR:
            case BINARY_OR:
                visitor.visitBinaryOperator(op);
                break;
            case INPLACE_POWER:
                visitor.visitInplaceOperator(op);
                break;
            case GET_ITER:
                visitor.visitGetIterator();
                break;

            case PRINT_EXPR:
                visitor.visitPrintExpression();
                break;
            case PRINT_ITEM:
                visitor.visitPrintItem();
                break;
            case PRINT_NEWLINE:
                visitor.visitPrintNewline();
                break;
            case PRINT_ITEM_TO:
                visitor.visitPrintItemTo();
                break;
            case PRINT_NEWLINE_TO:
                visitor.visitPrintNewlineTo();
                break;
            case INPLACE_LSHIFT:
            case INPLACE_RSHIFT:
            case INPLACE_AND:
            case INPLACE_XOR:
            case INPLACE_OR:
                visitor.visitInplaceOperator(op);
                break;
            case BREAK_LOOP:
                visitor.visitBreakLoop();
                break;
            case WITH_CLEANUP:
                visitor.visitWithCleanup();
                break;
            case LOAD_LOCALS:
                visitor.visitLoadLocals();
                break;
            case RETURN_VALUE:
                visitor.visitReturnValue();
                break;
            case IMPORT_STAR:
                visitor.visitImportStar();
                break;
            case EXEC_STMT:
                visitor.visitExecStatement();
                break;
            case YIELD_VALUE:
                visitor.visitYieldValue();
                break;
            case POP_BLOCK:
                visitor.visitPopBlock();
                break;
            case END_FINALLY:
                visitor.visitEndFinally();
                break;
            case BUILD_CLASS:
                visitor.visitBuildClass();
                break;

            // HAVE ARGUMENT:

            case STORE_NAME:
                visitor.visitStoreName(arg);
                break;
            case DELETE_NAME:
                visitor.visitDeleteName(arg);
                break;
            case UNPACK_SEQUENCE:
                visitor.visitUnpackSequence(arg);
                break;
            case FOR_ITER:
                visitor.visitForIteration(arg);
                break;

            case STORE_ATTR:
                visitor.visitStoreAttribute(arg);
                break;
            case DELETE_ATTR:
                visitor.visitDeleteAttribute(arg);
                break;
            case STORE_GLOBAL:
                visitor.visitStoreGlobal(arg);
                break;
            case DELETE_GLOBAL:
                visitor.visitDeleteGlobal(arg);
                break;
            case DUP_TOPX:
                visitor.visitDupTopX(arg);
                break;
            case LOAD_CONST:
                visitor.visitLoadConstant(arg);
                break;
            case LOAD_NAME:
                visitor.visitLoadName(arg);
                break;
            case BUILD_TUPLE:
                visitor.visitBuildTuple(arg);
                break;
            case BUILD_LIST:
                visitor.visitBuildList(arg);
                break;
            case BUILD_MAP:
                visitor.visitBuildMap(arg);
                break;
            case LOAD_ATTR:
                visitor.visitLoadAttribute(arg);
                break;
            case COMPARE_OP:
                visitor.visitCompareOperator(arg);
                break;
            case IMPORT_NAME:
                visitor.visitImportName(arg);
                break;
            case IMPORT_FROM:
                visitor.visitImportFrom(arg);
                break;

            case JUMP_FORWARD:
                visitor.visitJumpForward(arg);
                break;
            case JUMP_IF_FALSE:
                visitor.visitJumpIfFalse(arg);
                break;
            case JUMP_IF_TRUE:
                visitor.visitJumpIfTrue(arg);
                break;
            case JUMP_ABSOLUTE:
                visitor.visitJumpAbsolute(arg);
                break;

            case LOAD_GLOBAL:
                visitor.visitLoadGlobal(arg);
                break;

            case CONTINUE_LOOP:
                visitor.visitContinueLoop(arg);
                break;
            case SETUP_LOOP:
                visitor.visitSetupLoop(arg);
                break;
            case SETUP_EXCEPT:
                visitor.visitSetupExcept(arg);
                break;
            case SETUP_FINALLY:
                visitor.visitSetupFinally(arg);
                break;

            case LOAD_FAST:
                visitor.visitLoadFast(arg);
                break;
            case STORE_FAST:
                visitor.visitStoreFast(arg);
                break;
            case DELETE_FAST:
                visitor.visitDeleteFast(arg);
                break;

            case RAISE_VARARGS:
                visitor.visitRaiseVarargs(arg);
                break;
            case CALL_FUNCTION:
                visitor.visitCallFunction(arg);
                break;
            case MAKE_FUNCTION:
                visitor.visitMakeFunction(arg);
                break;
            case BUILD_SLICE:
                visitor.visitBuildSlice(arg);
                break;
            case MAKE_CLOSURE:
                visitor.visitMakeClosure(arg);
                break;
            case LOAD_CLOSURE:
                visitor.visitLoadClosure(arg);
                break;
            case LOAD_DEREF:
                visitor.visitLoadDeref(arg);
                break;
            case STORE_DEREF:
                visitor.visitStoreDeref(arg);
                break;

            case CALL_FUNCTION_VAR:
                visitor.visitCallFunctionVararg(arg);
                break;
            case CALL_FUNCTION_KW:
                visitor.visitCallFuntionKeyword(arg);
                break;
            case CALL_FUNCTION_VAR_KW:
                visitor.visitCallFunctionVarargKeyword(arg);
                break;
            case EXTENDED_ARG:
                // FIXME: annother exception type ?
                throw new BytecodeError(
                        "Arugment extension two times in a row!");
            default:
                // FIXME: annother exception type ?
                throw new BytecodeError("Illegal opcode '" + op + "'.");
            }
        }
        return visitor.compile();
    }

}
