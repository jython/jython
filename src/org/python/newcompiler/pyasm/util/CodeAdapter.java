// (C) Copyright 2007 Tobias Ivarsson
package org.python.newcompiler.pyasm.util;

import org.python.core.PyObject;
import org.python.newcompiler.pyasm.BytecodeVisitor;
import org.python.newcompiler.pyasm.CodeVisitor;
import org.python.newcompiler.pyasm.ConstantStore;
import org.python.newcompiler.pyasm.Label;

public class CodeAdapter implements BytecodeVisitor {

    private CodeVisitor next;
    private ConstantStore store;

    private static ConstantStore getStore(CodeVisitor visitor) {
        if (visitor != null & visitor instanceof ConstantStore) {
            return (ConstantStore) visitor;
        }
        return null;
    }

    public CodeAdapter() {
        this(getStore(null));
    }

    public CodeAdapter(ConstantStore store) {
        this(store, null);
    }

    public CodeAdapter(CodeVisitor next) {
        this(getStore(next), next);
    }

    public CodeAdapter(ConstantStore store, CodeVisitor next) {
        this.next = next;
        this.store = store;
    }

    // ConstantStore

    public void visitCode(long argcount, long nlocals, long stacksize,
            long flags, PyObject[] constants, String[] names,
            String[] varnames, String[] freevars, String[] cellvars,
            String filename, String name, long firstlnno) {
        if (store == null) {
            store = new SimpleConstantStore(constants, names, varnames,
                    freevars, cellvars);
        }
        if (next != null) {
            next.visitCode(argcount, nlocals, stacksize, flags, constants,
                    names, varnames, freevars, cellvars, filename, name,
                    firstlnno);
        }
    }

    public String getName(int index) {
        return store.getName(index);
    }

    public String getOuterName(int index) {
        return store.getOuterName(index);
    }

    public String getVariableName(int index) {
        return store.getVariableName(index);
    }

    public PyObject getConstant(int index) {
        return store.getConstant(index);
    }

    // CodeVisitor

    public void visitBinaryOperator(int operator) {
        if (next != null) {
            next.visitBinaryOperator(operator);
        }
    }

    public void visitBreakLoop() {
        if (next != null) {
            next.visitBreakLoop();
        }
    }

    public void visitBuildClass() {
        if (next != null) {
            next.visitBuildClass();
        }
    }

    public void visitBuildList(int size) {
        if (next != null) {
            next.visitBuildList(size);
        }
    }

    public void visitBuildMap(int zero) {
        if (next != null) {
            next.visitBuildMap(zero);
        }
    }

    public void visitBuildSlice(int numargs) {
        if (next != null) {
            next.visitBuildSlice(numargs);
        }
    }

    public void visitBuildTuple(int size) {
        if (next != null) {
            next.visitBuildTuple(size);
        }
    }

    public void visitCallFunction(int num_positional, int num_keyword) {
        if (next != null) {
            next.visitCallFunction(num_positional, num_keyword);
        }
    }

    public void visitCallFunctionKeyword(int num_positional, int num_keyword) {
        if (next != null) {
            next.visitCallFunctionKeyword(num_positional, num_keyword);
        }
    }

    public void visitCallFunctionVararg(int num_positional, int num_keyword) {
        if (next != null) {
            next.visitCallFunctionVararg(num_positional, num_keyword);
        }
    }

    public void visitCallFunctionVarargKeyword(int num_positional,
            int num_keyword) {
        if (next != null) {
            next.visitCallFunctionVarargKeyword(num_positional, num_keyword);
        }
    }

    public void visitCompareOperator(int operator) {
        if (next != null) {
            next.visitCompareOperator(operator);
        }
    }

    public void visitContinueLoop(Label loopStart) {
        if (next != null) {
            next.visitContinueLoop(loopStart);
        }
    }

    public void visitDeleteAttribute(String attributeName) {
        if (next != null) {
            next.visitDeleteAttribute(attributeName);
        }
    }

    public void visitDeleteFast(String variableName) {
        if (next != null) {
            next.visitDeleteFast(variableName);
        }
    }

    public void visitDeleteGlobal(String variableName) {
        if (next != null) {
            next.visitDeleteGlobal(variableName);
        }
    }

    public void visitDeleteName(String variableName) {
        if (next != null) {
            next.visitDeleteName(variableName);
        }
    }

    public void visitDeleteSlice(int plus) {
        if (next != null) {
            next.visitDeleteSlice(plus);
        }
    }

    public void visitDeleteSubscript() {
        if (next != null) {
            next.visitDeleteSubscript();
        }
    }

    public void visitDup(int num_elements) {
        if (next != null) {
            next.visitDup(num_elements);
        }
    }

    public void visitEnd() {
        if (next != null) {
            next.visitEnd();
        }
    }

    public void visitEndFinally() {
        if (next != null) {
            next.visitEndFinally();
        }
    }

    public void visitExecStatement() {
        if (next != null) {
            next.visitExecStatement();
        }
    }

    public void visitForIteration(Label end) {
        if (next != null) {
            next.visitForIteration(end);
        }
    }

    public void visitGetIterator() {
        if (next != null) {
            next.visitGetIterator();
        }
    }

    public void visitImportFrom(String name) {
        if (next != null) {
            next.visitImportFrom(name);
        }
    }

    public void visitImportName(String name) {
        if (next != null) {
            next.visitImportName(name);
        }
    }

    public void visitImportStar() {
        if (next != null) {
            next.visitImportStar();
        }
    }

    public void visitInplaceOperator(int operator) {
        if (next != null) {
            next.visitInplaceOperator(operator);
        }
    }

    public void visitJump(Label destination) {
        if (next != null) {
            next.visitJump(destination);
        }
    }

    public void visitJumpIfFalse(Label destination) {
        if (next != null) {
            next.visitJumpIfFalse(destination);
        }
    }

    public void visitJumpIfTrue(Label destination) {
        if (next != null) {
            next.visitJumpIfTrue(destination);
        }
    }

    public void visitLabel(Label label) {
        if (next != null) {
            next.visitLabel(label);
        }
    }

    public void visitLineNumber(int lineNumber) {
        if (next != null) {
            next.visitLineNumber(lineNumber);
        }
    }

    public void visitListAppend() {
        if (next != null) {
            next.visitListAppend();
        }
    }

    public void visitLoadAttribute(String attributeName) {
        if (next != null) {
            next.visitLoadAttribute(attributeName);
        }
    }

    public void visitLoadClosure(String variableName) {
        if (next != null) {
            next.visitLoadClosure(variableName);
        }
    }

    public void visitLoadConstant(PyObject constant) {
        if (next != null) {
            next.visitLoadConstant(constant);
        }
    }

    public void visitLoadDeref(String variableName) {
        if (next != null) {
            next.visitLoadDeref(variableName);
        }
    }

    public void visitLoadFast(String variableName) {
        if (next != null) {
            next.visitLoadFast(variableName);
        }
    }

    public void visitLoadGlobal(String variableName) {
        if (next != null) {
            next.visitLoadGlobal(variableName);
        }
    }

    public void visitLoadLocals() {
        if (next != null) {
            next.visitLoadLocals();
        }
    }

    public void visitLoadName(String variableName) {
        if (next != null) {
            next.visitLoadName(variableName);
        }
    }

    public void visitLoadSlice(int plus) {
        if (next != null) {
            next.visitLoadSlice(plus);
        }
    }

    public void visitMakeClosure(int num_default) {
        if (next != null) {
            next.visitMakeClosure(num_default);
        }
    }

    public void visitMakeFunction(int num_default) {
        if (next != null) {
            next.visitMakeFunction(num_default);
        }
    }

    public void visitNOP() {
        if (next != null) {
            next.visitNOP();
        }
    }

    public void visitPop() {
        if (next != null) {
            next.visitPop();
        }
    }

    public void visitPopBlock() {
        if (next != null) {
            next.visitPopBlock();
        }
    }

    public void visitPrintExpression() {
        if (next != null) {
            next.visitPrintExpression();
        }
    }

    public void visitPrintItem() {
        if (next != null) {
            next.visitPrintItem();
        }
    }

    public void visitPrintItemTo() {
        if (next != null) {
            next.visitPrintItemTo();
        }
    }

    public void visitPrintNewline() {
        if (next != null) {
            next.visitPrintNewline();
        }
    }

    public void visitPrintNewlineTo() {
        if (next != null) {
            next.visitPrintNewlineTo();
        }
    }

    public void visitRaiseVarargs(int count) {
        if (next != null) {
            next.visitRaiseVarargs(count);
        }
    }

    public void visitResumeTable(Label start, Label[] labels) {
        if (next != null) {
            next.visitResumeTable(start, labels);
        }
    }

    public void visitReturnValue() {
        if (next != null) {
            next.visitReturnValue();
        }
    }

    public void visitRot(int depth) {
        if (next != null) {
            next.visitRot(depth);
        }
    }

    public void visitSetupExcept(Label startExcept) {
        if (next != null) {
            next.visitSetupExcept(startExcept);
        }
    }

    public void visitSetupFinally(Label startFinally) {
        if (next != null) {
            next.visitSetupFinally(startFinally);
        }
    }

    public void visitSetupLoop(Label end) {
        if (next != null) {
            next.visitSetupLoop(end);
        }
    }

    public void visitStopCode() {
        if (next != null) {
            next.visitStopCode();
        }
    }

    public void visitStoreAttribute(String attributeName) {
        if (next != null) {
            next.visitStoreAttribute(attributeName);
        }
    }

    public void visitStoreDeref(String variableName) {
        if (next != null) {
            next.visitStoreDeref(variableName);
        }
    }

    public void visitStoreFast(String variableName) {
        if (next != null) {
            next.visitStoreFast(variableName);
        }
    }

    public void visitStoreGlobal(String variableName) {
        if (next != null) {
            next.visitStoreGlobal(variableName);
        }
    }

    public void visitStoreName(String variableName) {
        if (next != null) {
            next.visitStoreName(variableName);
        }
    }

    public void visitStoreSlice(int plus) {
        if (next != null) {
            next.visitStoreSlice(plus);
        }
    }

    public void visitStoreSubscript() {
        if (next != null) {
            next.visitStoreSubscript();
        }
    }

    public void visitUnaryOperator(int operator) {
        if (next != null) {
            next.visitUnaryOperator(operator);
        }
    }

    public void visitUnpackSequence(int count) {
        if (next != null) {
            next.visitUnpackSequence(count);
        }
    }

    public void visitWithCleanup() {
        if (next != null) {
            next.visitWithCleanup();
        }
    }

    public void visitYieldValue(int index, Label resume) {
        if (next != null) {
            next.visitYieldValue(index, resume);
        }
    }

}
