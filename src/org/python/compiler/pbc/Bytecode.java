package org.python.compiler.pbc;

// Copyright (c) 2009 Jython Developers
//
// ported from peak.util.assembler (BytecodeAssembler):
// Copyright (C) 1996-2004 by Phillip J. Eby and Tyler C. Sarna.
// All rights reserved.  This software may be used under the same terms
// as Zope or Python. (http://cvs.eby-sarna.com/*checkout*/PEAK/README.txt)
//
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.python.core.Py;
import org.python.core.Opcode;
import org.python.core.PyBaseCode;
import org.python.core.PyBytecode;
import org.python.core.PyObject;

// must be thread confined, as might be expected
class Bytecode {

    private int co_argcount = 0;
    private int co_stacksize = 0;
    private int co_flags = PyBaseCode.CO_OPTIMIZED | PyBaseCode.CO_NEWLOCALS; // typical usage
//    co_filename = '<generated code>'
//    co_name = '<lambda>'
//    co_firstlineno = 0
//    co_freevars = ()
//    co_cellvars = ()
//    _last_lineofs = 0
//    _ss = 0
//    _tmp_level = 0
//

    public Bytecode(int co_flags) {
        this.co_flags = co_flags;
        co_const.put(Py.None, 0);
    }
//    def __init__(self):
//        self.co_code = array('B')
//        self.co_names = []
//        self.co_varnames = []
//        self.blocks = []
    private final List<Integer> stack_history = new ArrayList<Integer>();
    private final List<Integer> co_code = new ArrayList<Integer>();
    private final List<Integer> co_lnotab = new ArrayList<Integer>();
    private final Map<PyObject, Integer> co_const = new HashMap<PyObject, Integer>();

    private void emit(int opcode) {
        assert (opcode >= 0 && opcode <= 0xFF); // at this point we should verify we are only emitting unsigned bytes
        co_code.add(opcode);
    }

    private void emit(int opcode, int oparg) {
        if (oparg > 0xFFFF) {
            emit(Opcode.EXTENDED_ARG);
            emit((oparg >> 16) & 0xFF);
            emit((oparg >> 24) & 0xFF);
        }
        emit(opcode);
        emit(oparg & 0xFF);
        emit((oparg >> 8) & 0xFF);
    }

    private void LOAD_CONST(PyObject constant) {
        int arg;
        if (co_const.containsKey(constant)) {
            arg = co_const.get(constant);
        } else {
            arg = co_const.size() + 1;
            co_const.put(constant, arg);
        }
        stackchange(0, 1);
        emit(Opcode.LOAD_CONST, arg);
    }

    private void RETURN_VALUE() {
        stackchange(1, 0);
        emit(Opcode.RETURN_VALUE);
        stack_unknown();
    }

    public void code() {
//        return new PyBytecode();
    }


//

//
//    def locals_written(self):
//        vn = self.co_varnames
//        hl = dict.fromkeys([STORE_FAST, DELETE_FAST])
//        return dict.fromkeys([vn[arg] for ofs, op, arg in self if op in hl])
//
//
//
//
    public void set_lineno(int lno) {
//        if not self.co_firstlineno:
//            self.co_firstlineno = self._last_line = lno
//            return
//
//        append = self.co_lnotab.append
//        incr_line = lno - self._last_line
//        incr_addr = len(self.co_code) - self._last_lineofs
//        if not incr_line:
//            return
//
//        assert incr_addr>=0 and incr_line>=0
//
//        while incr_addr>255:
//            append(255)
//            append(0)
//            incr_addr -= 255
//
//        while incr_line>255:
//            append(incr_addr)
//            append(255)
//            incr_line -= 255
//            incr_addr = 0
//
//        if incr_addr or incr_line:
//            append(incr_addr)
//            append(incr_line)
//
//        self._last_line = lno
//        self._last_lineofs = len(self.co_code)
    }
    //

    public void YIELD_VALUE() {
        stackchange(1, 1);
        co_flags |= PyBaseCode.CO_GENERATOR;
        emit(Opcode.YIELD_VALUE);
    }

    // defaults? def CALL_FUNCTION(self, argc=0, kwargc=0, op=CALL_FUNCTION, extra=0):
    public void CALL_FUNCTION(int argc, int kwargc, int op, int extra) {
        stackchange(1 + argc + 2 * kwargc + extra, 1);
        emit(op);
        emit(argc);
        emit(kwargc);
    }
//
//    def CALL_FUNCTION_VAR(self, argc=0, kwargc=0):
//        self.CALL_FUNCTION(argc,kwargc,CALL_FUNCTION_VAR, 1)    # 1 for *args
//
//    def CALL_FUNCTION_KW(self, argc=0, kwargc=0):
//        self.CALL_FUNCTION(argc,kwargc,CALL_FUNCTION_KW, 1)     # 1 for **kw
//
//    def CALL_FUNCTION_VAR_KW(self, argc=0, kwargc=0):
//        self.CALL_FUNCTION(argc,kwargc,CALL_FUNCTION_VAR_KW, 2) # 2 *args,**kw
//

    public void BUILD_TUPLE(int count) {
        stackchange(count, 1);
        emit(Opcode.BUILD_TUPLE, count);
    }

    public void BUILD_LIST(int count) {
        stackchange(count, 1);
        emit(Opcode.BUILD_LIST, count);
    }

    public void UNPACK_SEQUENCE(int count) {
        stackchange(1, count);
        emit(Opcode.UNPACK_SEQUENCE, count);
    }

    public void BUILD_SLICE(int count) {
        if (count != 2 && count != 3) {
            throw Py.AssertionError("Invalid number of arguments for BUILD_SLICE");
        }
        stackchange(count, 1);
        emit(Opcode.BUILD_SLICE, count);
    }

    public void DUP_TOPX(int count) {
        stackchange(count, count * 2);
        emit(Opcode.DUP_TOPX, count);
    }

    public void RAISE_VARARGS(int argc) {
        if (0 <= argc && argc <= 3) {
            throw Py.AssertionError("Invalid number of arguments for RAISE_VARARGS");
        }
        stackchange(argc, 0);
        emit(Opcode.RAISE_VARARGS, argc);
    }

    public void MAKE_FUNCTION(int ndefaults) {
        stackchange(1 + ndefaults, 1);
        emit(Opcode.MAKE_FUNCTION, ndefaults);
    }

    public void MAKE_CLOSURE(int ndefaults, int freevars) {
        freevars = 1;
        stackchange(1 + freevars + ndefaults, 1);
        emit(Opcode.MAKE_CLOSURE, ndefaults);
    }

    public int here() {
        return co_code.size();
    }
    private int _ss = -1;

    public void set_stack_size(int size) {
        if (size < 0) {
            throw Py.AssertionError("Stack underflow");
        }
        if (size > co_stacksize) {
            co_stacksize = size;
        }
        int bytes = co_code.size() - stack_history.size() + 1;
        if (bytes > 0) {
            for (int i = 0; i < bytes; i++) {
                stack_history.add(_ss);
            }
        }
        _ss = size;
    }

    public int get_stack_size() {
        return _ss;
    }

    public void stackchange(int inputs, int outputs) {
        if (_ss == -1) {
            throw Py.AssertionError("Unknown stack size at this location");
        }
        set_stack_size(get_stack_size() - inputs);   // check underflow
        set_stack_size(get_stack_size() + outputs); // update maximum height

    }

    public void stack_unknown() {
        _ss = -1;
    }

    public void branch_stack(int location, int expected) {
        if (location >= stack_history.size()) {
            if (location > co_code.size()) {
                throw Py.AssertionError(String.format(
                        "Forward-looking stack prediction! %d, %d", location, co_code.size()));
            }
            int actual = get_stack_size();
            if (actual == -1) {
                actual = expected;
                set_stack_size(actual);
                stack_history.set(location, actual);
            } else {
                actual = stack_history.get(location);
                if (actual == -1) {
                    actual = expected;
                    stack_history.set(location, actual);
                }
                if (actual != expected) {
                    throw Py.AssertionError(String.format(
                            "Stack level mismatch: actual=%d expected=%d", actual, expected));
                }
            }
        }
    }

//    def jump(self, op, arg=None):
//        def jump_target(offset):
//            target = offset
//            if op not in hasjabs:
//                target = target - (posn+3)
//                assert target>=0, "Relative jumps can't go backwards"
//                if target>0xFFFF:
//                    target = offset - (posn+6)
//            return target
//
//        def backpatch(offset):
//            target = jump_target(offset)
//            if target>0xFFFF:
//                raise AssertionError("Forward jump span must be <64K bytes")
//            self.patch_arg(posn, 0, target)
//            self.branch_stack(offset, old_level)
//
//        if op==FOR_ITER:
//            old_level = self.stack_size = self.stack_size - 1
//            self.stack_size += 2
//        else:
//            old_level = self.stack_size
//        posn = self.here()
//
//        if arg is not None:
//            self.emit_arg(op, jump_target(arg))
//            self.branch_stack(arg, old_level)
//            lbl = None
//        else:
//            self.emit_arg(op, 0)
//            def lbl(code=None):
//                backpatch(self.here())
//        if op in (JUMP_FORWARD, JUMP_ABSOLUTE, CONTINUE_LOOP):
//            self.stack_unknown()
//        return lbl
//
//    def COMPARE_OP(self, op):
//        self.stackchange((2,1))
//        self.emit_arg(COMPARE_OP, compares[op])
//
//
//    def setup_block(self, op):
//        jmp = self.jump(op)
//        self.blocks.append((op,self.stack_size,jmp))
//        return jmp
//
//    def SETUP_EXCEPT(self):
//        ss = self.stack_size
//        self.stack_size = ss+3  # simulate the level at "except:" time
//        self.setup_block(SETUP_EXCEPT)
//        self.stack_size = ss    # restore the current level
//
//    def SETUP_FINALLY(self):
//        ss = self.stack_size
//        self.stack_size = ss+3  # allow for exceptions
//        self.stack_size = ss+1  # simulate the level after the None is pushed
//        self.setup_block(SETUP_FINALLY)
//        self.stack_size = ss    # restore original level
//
//    def SETUP_LOOP(self):
//        self.setup_block(SETUP_LOOP)
//
//    def POP_BLOCK(self):
//        if not self.blocks:
//            raise AssertionError("Not currently in a block")
//
//        why, level, fwd = self.blocks.pop()
//        self.emit(POP_BLOCK)
//
//        if why!=SETUP_LOOP:
//            if why==SETUP_FINALLY:
//                self.LOAD_CONST(None)
//                fwd()
//            else:
//                self.stack_size = level-3   # stack level resets here
//                else_ = self.JUMP_FORWARD()
//                fwd()
//                return else_
//        else:
//            return fwd
//
//
//    def assert_loop(self):
//        for why,level,fwd in self.blocks:
//            if why==SETUP_LOOP:
//                return
//        raise AssertionError("Not inside a loop")
//
//    def BREAK_LOOP(self):
//        self.assert_loop(); self.emit(BREAK_LOOP)
//        self.stack_unknown()
//
//    def CONTINUE_LOOP(self, label):
//        self.assert_loop()
//        if self.blocks[-1][0]==SETUP_LOOP:
//            op = JUMP_ABSOLUTE  # more efficient if not in a nested block
//        else:
//            op = CONTINUE_LOOP
//        return self.jump(op, label)
//
//    def __call__(self, *args):
//        last = None
//        for ob in args:
//            if callable(ob):
//                last = ob(self)
//            else:
//                try:
//                    f = generate_types[type(ob)]
//                except KeyError:
//                    raise TypeError("Can't generate", ob)
//                else:
//                    last = f(self, ob)
//        return last
//
//    def return_(self, ob=None):
//        return self(ob, Code.RETURN_VALUE)
//
//    decorate(classmethod)
//    def from_function(cls, function, copy_lineno=False):
//        code = cls.from_code(function.func_code, copy_lineno)
//        return code
//
//
//    decorate(classmethod)
//    def from_code(cls, code, copy_lineno=False):
//        import inspect
//        self = cls.from_spec(code.co_name, *inspect.getargs(code))
//        if copy_lineno:
//            self.set_lineno(code.co_firstlineno)
//            self.co_filename = code.co_filename
//        self.co_freevars = code.co_freevars     # XXX untested!
//        return self
//
//    decorate(classmethod)
//    def from_spec(cls, name='<lambda>', args=(), var=None, kw=None):
//        self = cls()
//        self.co_name = name
//        self.co_argcount = len(args)
//        self.co_varnames.extend(args)
//        if var:
//            self.co_varnames.append(var)
//            self.co_flags |= CO_VARARGS
//        if kw:
//            self.co_varnames.append(kw)
//            self.co_flags |= CO_VARKEYWORDS
//
//        def tuple_arg(args):
//            self.UNPACK_SEQUENCE(len(args))
//            for arg in args:
//                if not isinstance(arg, basestring):
//                    tuple_arg(arg)
//                else:
//                    self.STORE_FAST(arg)
//
//        for narg, arg in enumerate(args):
//            if not isinstance(arg, basestring):
//                dummy_name = '.'+str(narg)
//                self.co_varnames[narg] = dummy_name
//                self.LOAD_FAST(dummy_name)
//                tuple_arg(arg)
//
//        return self
//
//
//    def patch_arg(self, offset, oldarg, newarg):
//        code = self.co_code
//        if (oldarg>0xFFFF) != (newarg>0xFFFF):
//            raise AssertionError("Can't change argument size", oldarg, newarg)
//        code[offset+1] = newarg & 255
//        code[offset+2] = (newarg>>8) & 255
//        if newarg>0xFFFF:
//            newarg >>=16
//            code[offset-2] = newarg & 255
//            code[offset-1] = (newarg>>8) & 255
//
//    def nested(self, name='<lambda>', args=(), var=None, kw=None, cls=None):
//        if cls is None:
//            cls = Code
//        code = cls.from_spec(name, args, var, kw)
//        code.co_filename=self.co_filename
//        return code
//
//    def __iter__(self):
//        i = 0
//        extended_arg = 0
//        code = self.co_code
//        n = len(code)
//        while i < n:
//            op = code[i]
//            if op >= HAVE_ARGUMENT:
//                oparg = code[i+1] + code[i+2]*256 + extended_arg
//                extended_arg = 0
//                if op == EXTENDED_ARG:
//                    extended_arg = oparg*65536
//                    i+=3
//                    continue
//                yield i, op, oparg
//                i += 3
//            else:
//                yield i, op, None
//                i += 1
//
//
//
//
//    def makefree(self, names):
//        nowfree = dict.fromkeys(self.co_freevars)
//        newfree = [n for n in names if n not in nowfree]
//        if newfree:
//            self.co_freevars += tuple(newfree)
//            self._locals_to_cells()
//
//    def makecells(self, names):
//        nowcells = dict.fromkeys(self.co_cellvars+self.co_freevars)
//        newcells = [n for n in names if n not in nowcells]
//        if newcells:
//            if not (self.co_flags & CO_OPTIMIZED):
//                raise AssertionError("Can't use cellvars in unoptimized scope")
//            cc = len(self.co_cellvars)
//            nc = len(newcells)
//            self.co_cellvars += tuple(newcells)
//            if self.co_freevars:
//                self._patch(
//                    deref_to_deref,
//                    dict([(n+cc,n+cc+nc)for n in range(len(self.co_freevars))])
//                )
//            self._locals_to_cells()
//
//    def _locals_to_cells(self):
//        freemap = dict(
//            [(n,p) for p,n in enumerate(self.co_cellvars+self.co_freevars)]
//        )
//        argmap = dict(
//            [(p,freemap[n]) for p,n in enumerate(self.co_varnames)
//                if n in freemap]
//        )
//        if argmap:
//            for ofs, op, arg in self:
//                if op==DELETE_FAST and arg in argmap:
//                    raise AssertionError(
//                        "Can't delete local %r used in nested scope"
//                        % self.co_varnames[arg]
//                    )
//            self._patch(fast_to_deref, argmap)
//
//
//    def _patch(self, opmap, argmap={}):
//        code = self.co_code
//        for ofs, op, arg in self:
//            if op in opmap:
//                if arg in argmap:
//                    self.patch_arg(ofs, arg, argmap[arg])
//                elif arg is not None:
//                    continue
//                code[ofs] = opmap[op]
//
//    def code(self, parent=None):
//        if self.blocks:
//            raise AssertionError("%d unclosed block(s)" % len(self.blocks))
//
//        flags = self.co_flags & ~CO_NOFREE
//        if parent is not None:
//            locals_written = self.locals_written()
//            self.makefree([
//                n for n in self.co_varnames[
//                    self.co_argcount
//                    + ((self.co_flags & CO_VARARGS)==CO_VARARGS)
//                    + ((self.co_flags & CO_VARKEYWORDS)==CO_VARKEYWORDS)
//                    :
//                ] if n not in locals_written
//            ])
//
//        if not self.co_freevars and not self.co_cellvars:
//            flags |= CO_NOFREE
//        elif parent is not None and self.co_freevars:
//            parent.makecells(self.co_freevars)
//
//        return code(
//            self.co_argcount, len(self.co_varnames),
//            self.co_stacksize, flags, self.co_code.tostring(),
//            tuple(self.co_consts), tuple(self.co_names),
//            tuple(self.co_varnames),
//            self.co_filename, self.co_name, self.co_firstlineno,
//            self.co_lnotab.tostring(), self.co_freevars, self.co_cellvars
//        )
}
