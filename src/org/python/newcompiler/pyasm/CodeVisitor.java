// (C) Copyright 2007 Tobias Ivarsson
package org.python.newcompiler.pyasm;

/**
 * @author Tobias Ivarsson
 * 
 * This interface defines the capabilities of a class that writes code.
 * 
 * This is greatly inspired from ASM, but adapted for python.
 * 
 * This is intended to act as a layer in between the AST Visitor and the
 * bytecode generator.
 */
public interface CodeVisitor {

    /**
     * Visit a nested chunk of code.
     * 
     * FIXME: the set of parameters isn't worked out yet...
     * 
     * @param stuff Just some parameters...
     * @return A {@link CodeVisitor} to visit the instructions of the nested
     *         code object.
     */
    public CodeVisitor visitCode(Object[] stuff);

    /* Name handling methods */

    /**
     * Get the name of a something the scope, given its index.
     * 
     * Used for everything but vaiables, for variables
     * {@link #getVariableName(int)} is used.
     * 
     * @param index The index of the name.
     * @return The name of the item.
     */
    public String getName(int index);

    /**
     * Get the name of a variable in the scope, given its index.
     * 
     * @param index The index of the name.
     * @return The name of the variable.
     */
    public String getVariableName(int index);

    /**
     * Get the name of a variable in an enclosing scope, given its index.
     * 
     * @param index The index of the name.
     * @return The name of the variable.
     */
    public String getOuterName(int index);

    /**
     * Indicates a change of line number in the source of the code.
     * 
     * @param lineNumber The new source line number.
     */
    public void visitLineNumber(int lineNumber);

    /**
     * Visit a set of resume labels for generator code objects. The labels are
     * located after each yield statement in the byte code.
     * 
     * The start label is located at the beginning of the code block, generally
     * right after the resume table.
     * 
     * @param start The start point of the generator.
     * @param labels The points of resumption after yield.
     */
    public void visitResumeTable(Label start, Label[] labels);

    /* Visit methods for various kinds of instructions */

    /**
     * Used by the compiler to indicate end-of-code, but does not seem to be
     * used.
     */
    public void visitStopCode();

    // simple stack instructions
    /**
     * Do nothing.
     */
    public void visitNOP();

    /**
     * Remove the top value of the stack.
     */
    public void visitPop();

    /**
     * Duplicate the top X elements on the stack, and place the duplicates in
     * the same order.
     * 
     * @param num_elements The number of elements to duplicate, should be a
     *            number between 1 and 5 (inclusive).
     */
    public void visitDup(int num_elements);

    /**
     * Rotate the top X elements on the stack. Lift the Xth element from its
     * position and place it on top. (Rot 2 is swap)
     * 
     * @param depth The number of elements to rotate, should be a number between
     *            2 and 4 (inclusive).
     */
    public void visitRot(int depth);

    // jump instructions

    /**
     * Add a label at the curent position.
     * 
     * @param label The label to store at this position.
     */
    public void visitLabel(Label label);

    /**
     * Jump unconditionally to the given label.
     * 
     * @param destination The label to jump to.
     */
    public void visitJump(Label destination);

    /**
     * Jump to the given label if the value on the top of the stack is a
     * <code>true</code> value. The value is left on the stack.
     * 
     * @param destination The label to jump to.
     */
    public void visitJumpIfTrue(Label destination);

    /**
     * Jump to the given label if the value on the top of the stack is a
     * <code>false</code> value. The value is left on the stack.
     * 
     * @param destination The label to jump to.
     */
    public void visitJumpIfFalse(Label destination);

    // operators

    /**
     * Execute a unary operator on the element on the top of the stack.
     * 
     * <code>TOS = op TOS</code>
     * 
     * Unary operators are: INVERT, POSITIVE, NEGATIVE, NOT and CONVERT.
     * 
     * Constants representing these operators are found in the {@link Operator}
     * interface.
     * 
     * @param operator The operator to execute.
     */
    public void visitUnaryOperator(int operator);

    /**
     * Execute a binary operator with the Second element on the stack as the
     * first operand and the Top element on the stack as the second operand.
     * 
     * <code>TOS = TOS1 op TOS</code>
     * 
     * Binary operators are: ADD, SUBTRACT, MULTIPLY, DIVIDE, FLOOR_DIVIDE,
     * TRUE_DIVIDE, MODULO, POWER, LSHIFT, RSHIFT, bitwise AND, bitwise OR,
     * bitwise XOR and SUBSCRIPT.
     * 
     * Constants representing these operators are found in the {@link Operator}
     * interface.
     * 
     * @param operator The operator to execute.
     */
    public void visitBinaryOperator(int operator);

    /**
     * Execute an inplace binary operator with the Second element on the stack
     * as the first operand and the Top element on the stack as the second
     * operand.
     * 
     * <code>TOS = TOS1 op TOS</code>
     * 
     * Binary inplace operators are: ADD, SUBTRACT, MULTIPLY, DIVIDE,
     * FLOOR_DIVIDE, TRUE_DIVIDE, MODULO, POWER, LSHIFT, RSHIFT, bitwise AND,
     * bitwise OR and bitwise XOR.
     * 
     * Constants representing these operators are found in the {@link Operator}
     * interface.
     * 
     * @param operator The operator to execute.
     */
    public void visitInplaceOperator(int operator);

    /**
     * Execute a boolean operator on two topmost elements on the stack.
     * 
     * <code>TOS = TOS1 op TOS</code>
     * 
     * Boolean operators are: LESS_THAN, LESS_THAN_OR_EQUAL, EQUAL, NOT_EQUAL,
     * GREATER_THAN, GREATER_THAN_OR_EQUAL, IN, NOT_IN, IS, IS_NOT and
     * EXCEPTION_MATCH.
     * 
     * Constants representing these operators are found in the {@link Operator}
     * interface.
     * 
     * @param operator The operator to execute.
     */
    public void visitCompareOperator(int operator);

    // list operators

    /**
     * Append the topmost element on the stack to the list that is the second
     * element on the stack.
     * 
     * <code>TOS1.append(TOS)</code>, or if you like
     * <code>list.append(TOS1, TOS)</code>.
     * 
     * The list is kept on the stack.
     * 
     * This is used to implement list comprehension.
     */
    public void visitListAppend();

    /**
     * Load a slice from the top element on the stack. The kind of the slice
     * depends on the argument, like this:
     * 
     * 0. <code>TOS = TOS[:]</code>
     * 
     * 1. <code>TOS = TOS1[TOS:]</code>
     * 
     * 2. <code>TOS = TOS1[:TOS]</code>
     * 
     * 3. <code>TOS = TOS2[TOS1:TOS]</code>
     * 
     * @param plus The kind of slicing used, 0 to 3 (inclusive).
     */
    public void visitLoadSlice(int plus);

    /**
     * Store a value into a slice of the top element on the stack. The kind of
     * the slice depends on the argument, like this:
     * 
     * 0. <code>TOS[:] = TOS1</code>
     * 
     * 1. <code>TOS1[TOS:] = TOS2</code>
     * 
     * 2. <code>TOS1[:TOS] = TOS2</code>
     * 
     * 3. <code>TOS2[TOS1:TOS] = TOS3</code>
     * 
     * @param plus The kind of slicing used, 0 to 3 (inclusive).
     */
    public void visitStoreSlice(int plus);

    /**
     * Delete a slice from the top element on the stack. The kind of the slice
     * depends on the argument, like this:
     * 
     * 0. <code>del TOS[:]</code>
     * 
     * 1. <code>del TOS1[TOS:]</code>
     * 
     * 2. <code>del TOS1[:TOS]</code>
     * 
     * 3. <code>del TOS2[TOS1:TOS]</code>
     * 
     * @param plus The kind of slicing used, 0 to 3 (inclusive).
     */
    public void visitDeleteSlice(int plus);

    /**
     * Store a value into a specified position of the top element on the stack.
     * 
     * <code>TOS1[TOS] = TOS2</code>
     * 
     * Note that loading a subscript is considered a binary operator.
     * 
     * @see #visitBinaryOperator(int)
     */
    public void visitStoreSubscript();

    /**
     * Delete a value from a specified position of the top element on the stack.
     * 
     * <code>del TOS1[TOS]</code>
     * 
     * Note that loading a subscript is considered a binary operator.
     * 
     * @see #visitBinaryOperator(int)
     */
    public void visitDeleteSubscript();

    // exit points

    /**
     * Return the value on the top of the stack from the function this code
     * block implements.
     */
    public void visitReturnValue();

    /**
     * Yield the value on the top of the stack from the generator this code
     * block implements.
     * @param index The index of the yield point.
     * @param resume The label that marks the resume point after the yield.
     */
    public void visitYieldValue(int index, Label resume);

    /**
     * Raise an exception.
     * 
     * @param count The number of arguments to the raise statement, 0 to 3
     *            (inclusive).
     * 
     * TOS is the Exception TOS1 is the parameter TOS2 is the traceback
     */
    public void visitRaiseVarargs(int count);

    // import

    /**
     * Load all the names not starting with underscore from the module at the
     * top of the stack to the current namespace.
     * 
     * <code>from TOS import *</code>
     */
    public void visitImportStar();

    /**
     * Import a given module and store it on the top of the stack. To complete
     * the import the value should be stored in the namespace.
     * 
     * @param name The name of the module to import.
     */
    public void visitImportName(String name);

    /**
     * Import the given name from the module on top of the stack. Stores the
     * imported value to the top of the stack. To complete the import the value
     * should be stored in the namespace.
     * 
     * @param name The name to import.
     */
    public void visitImportFrom(String name);

    // misc.

    /**
     * Expects the __exit__ method of the Context on the stack, invokes the
     * __exit__ method with the appropriate arguments, depending on exception.
     */
    public void visitWithCleanup();

    /**
     * Push a reference to the locals to the top of the stack.
     */
    public void visitLoadLocals();

    /**
     * <code>exec TOS2 in TOS1, TOS</code>
     */
    public void visitExecStatement();

    /**
     * Unpack the element on the top of the stack into the given number of
     * elements. These elemtents are pushed to the stack from right to left.
     * 
     * @param count The number of elements to unpack.
     */
    public void visitUnpackSequence(int count);

    // iteration

    /**
     * Load the iterator of the object on the top of the stack onto the stack.
     * 
     * <code>TOS = iter(TOS)</code>
     */
    public void visitGetIterator();

    /**
     * Continue a loop due to a <code>continue</code> statement.
     * 
     * @param loopStart The start of the loop, right at the head of the loop.
     */
    public void visitContinueLoop(Label loopStart);

    /**
     * Terminate a loop due to a <code>break</code> statement.
     */
    public void visitBreakLoop();

    // blocks

    /**
     * Start a for loop, expects an iterator on the top of the stack. Invokes
     * the next-method of the iterator. If it yields a value that value is
     * pushed onto the stack (above the iterator, which is left below on the
     * stack), if the iterator is exhausted it is poped of the stack and
     * execution is continued after the for-block.
     * 
     * @param end Where the execution is continued when the iterator is
     *            exhausted.
     */
    public void visitForIteration(Label end);

    /**
     * Pop an item from the block stack. The stack of nested blocks.
     * 
     * NOTE: The block stack is a runtime stack in Python, in Jython it should
     * however probably be a compile time stack that is then replaced with
     * labels and the equevalent java blocks (try/catch/finally).
     */
    public void visitPopBlock();

    /**
     * Terminates a finally clause, re-raises any exceptions, returns function
     * values and resumes execution at the outer block.
     */
    public void visitEndFinally();

    /**
     * Start a new loop block.
     * 
     * @param end The end of the loop block.
     */
    public void visitSetupLoop(Label end);

    /**
     * Start a new try block, with the execption handling starting at the given
     * label.
     * 
     * @param startExcept The start of the execption handling code.
     */
    public void visitSetupExcept(Label startExcept);

    /**
     * Start a new try block, with the finally-code starting at the given label.
     * 
     * @param startFinally The start of the finally code.
     */
    public void visitSetupFinally(Label startFinally);

    // builtin types

    /**
     * Create a new tuple of given size. The elements for the tuple are consumed
     * from the stack and the newly produced tuple is pushed onto the stack.
     * 
     * @param size The number of elements in the tuple.
     */
    public void visitBuildTuple(int size);

    /**
     * Create a new list of given size. The elements for the list are consumed
     * from the stack and the newly produced list is pushed onto the stack.
     * 
     * @param size The number of elements in the list.
     */
    public void visitBuildList(int size);

    /**
     * Push an empty dictionary onto the stack.
     * 
     * @param zero Should be zero (0).
     */
    public void visitBuildMap(int zero);

    /**
     * Create a new slice from two or three arguments found on the stack.
     * 
     * The two first arguments are the start and end of the slice and the third
     * (optional) is the step of the slice.
     * 
     * @param numargs The number of arguments for the slice, should be 2 or 3.
     */
    public void visitBuildSlice(int numargs);

    /**
     * Create a new class from the elements on the stack.
     * 
     * TOS is the dictionary of the class.
     * 
     * TOS1 is the tuple of names of the base classes.
     * 
     * TOS2 is the class name.
     */
    public void visitBuildClass();

    /**
     * Create a new function, the code object for the function is found on the
     * top of the stack, and the default values for arguments are found below
     * that.
     * 
     * @param num_default The number of default values for parameters.
     */
    public void visitMakeFunction(int num_default);

    /**
     * Create a new function, the code object for the function is found on the
     * top of the stack, then comes the slots for the free variables of the code
     * object and the default values for arguments are found below that.
     * 
     * @param num_default The number of default values for parameters.
     */
    public void visitMakeClosure(int num_default);

    // function calls

    /**
     * Issue a normal function call, with a given number of positional arguments
     * and a given number of keyword arguments.
     * 
     * The keyword arguments are found in pairs at the top of the stack with the
     * value on top of the key.
     * 
     * Under the keyword arguments the positional arguments are found with the
     * rightmost parameter on top.
     * 
     * @param num_positional The number of positional arguments.
     * @param num_keyword The number of keyword arguments.
     */
    public void visitCallFunction(int num_positional, int num_keyword);

    /**
     * Issue a function call with a sequence passed on the top of the stack as
     * extra positional arguments. Below that the arguments and keyword
     * arguments are stored as described in {@link #visitCallFunction(int, int)}.
     * 
     * @param num_positional The number of positional arguments.
     * @param num_keyword The number of keyword arguments.
     */
    public void visitCallFunctionVararg(int num_positional, int num_keyword);

    /**
     * Issue a function call with a dictionary passed on the top of the stack as
     * extra keyword arguments. Below that the arguments and keyword arguments
     * are stored as described in {@link #visitCallFunction(int, int)}.
     * 
     * @param num_positional The number of positional arguments.
     * @param num_keyword The number of keyword arguments.
     */
    public void visitCallFunctionKeyword(int num_positional, int num_keyword);

    /**
     * Issue a function call with a dictionary passed on the top of the stack as
     * extra keyword arguments and a sequence passed as extra positional
     * arguments as the second element of the stack. Below that the arguments
     * and keyword arguments are stored as described in
     * {@link #visitCallFunction(int, int)}.
     * 
     * @param num_positional The number of positional arguments.
     * @param num_keyword The number of keyword arguments.
     */
    public void visitCallFunctionVarargKeyword(int num_positional,
            int num_keyword);

    // names / values

    /**
     * Load the constant with the given index onto the stack.
     * 
     * @param index The index of the constant to load.
     */
    public void visitLoadConstant(int index);

    /*
     * NOTE: These methods could be folded into three: visitLoad, visitStore and
     * visitDelete, then the compiler could figure out what to do from the name.
     */

    /**
     * Load the variable with the given name onto the stack.
     * 
     * @param variableName The name of the variable to load.
     */
    public void visitLoadName(String variableName);

    /**
     * Store the element on the top of the stack to the variable with the given
     * name.
     * 
     * @param variableName The name of the varaible to store.
     */
    public void visitStoreName(String variableName);

    /**
     * Delete the variable with the given name.
     * 
     * @param variableName The name of the variable to delete.
     */
    public void visitDeleteName(String variableName);

    /**
     * Load the global valiable with the given name onto the stack.
     * 
     * @param variableName The name of the global variable to load.
     */
    public void visitLoadGlobal(String variableName);

    /**
     * Store the element on the top of the stack to the global variable with the
     * given name.
     * 
     * @param variableName The name of the global variable to store.
     */
    public void visitStoreGlobal(String variableName);

    /**
     * Delete the global variable with the given name.
     * 
     * @param variableName The name of the global variable to delete.
     */
    public void visitDeleteGlobal(String variableName);

    /**
     * Load the variable in the current scope with the given name onto the
     * stack.
     * 
     * @param variableName The name of the variable in the current scope to
     *            laod.
     */
    public void visitLoadFast(String variableName);

    /**
     * Store the element on the top of the stack to the variable in the current
     * scope with the given name.
     * 
     * @param variableName The name of the variable in the current scope to
     *            store.
     */
    public void visitStoreFast(String variableName);

    /**
     * Delete the variable with the given name in the current scope.
     * 
     * @param variableName The name of the variable in the current scope to
     *            delete.
     */
    public void visitDeleteFast(String variableName);

    /**
     * Load a reference to the cell variable or free variable with the given
     * name onto the stack.
     * 
     * @param variableName The name of the cell variable or free variable to
     *            load a reference to.
     */
    public void visitLoadClosure(String variableName);

    /**
     * Load the value of a cell variable or free variable with the given name
     * onto the stack.
     * 
     * @param variableName The name of the cell variable or free variable to
     *            load.
     */
    public void visitLoadDeref(String variableName);

    /**
     * Store the element on the top of the stack into the cell variable or free
     * variable with the given name.
     * 
     * @param variableName The name of the cell variable or free variable to
     *            store.
     */
    public void visitStoreDeref(String variableName);

    /**
     * Load the attribute with the given name from the object on the top of the
     * stack.
     * 
     * @param attributeName The name of the attribute to load.
     */
    public void visitLoadAttribute(String attributeName);

    /**
     * Store the second element on the stack as the attribute of the given name
     * in the object on the top of the stack.
     * 
     * @param attributeName The name of the attribute to store.
     */
    public void visitStoreAttribute(String attributeName);

    /**
     * Delete the attribute with the given name from the object on the top of
     * the stack.
     * 
     * @param attributeName The name of the attribute to delete.
     */
    public void visitDeleteAttribute(String attributeName);

    // print

    /**
     * Print the element on the element on the top of the stack and a newline.
     * 
     * Used only in interactive mode.
     */
    public void visitPrintExpression();

    /**
     * Print the element on the top of the stack to <code>sys.stdout</code>.
     */
    public void visitPrintItem();

    /**
     * Print a newline to <code>sys.stdout</code>.
     */
    public void visitPrintNewline();

    /**
     * Print the element on the top of the stack to the second element on the
     * stack.
     */
    public void visitPrintItemTo();

    /**
     * Print a newline to the element on the top of the stack.
     */
    public void visitPrintNewlineTo();

    /**
     * Marks the end of the code object.
     */
    public void visitEnd();

}
