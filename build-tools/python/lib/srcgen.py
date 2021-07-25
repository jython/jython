# Classes that emit indented, wrapped (Java) source code

import io
import sys

class IndentedEmitter:
    """Class to write wrapped, indented (program) text onto a stream.

    Text is supplied via the emit() and emit_line() methods, and added to
    an internal buffer. emit_line() writes the current buffer (if it is not
    empty), always beginning a new, indented line. emit() first checks for
    sufficient buffer space, writing existing content to the output stream
    only as necessary to respect the stated width. The supplied text is
    treated as atomic, however long: neither method inserts line-breaks.
    close() must be called to ensure the last buffered text reaches the
    output stream. (Consider using contextlib.closing.)
    """

    class IndentationContextManager:
        """Context in which the indentation is increased by one."""

        def __init__(self, emitter):
            self.emitter = emitter

        def __enter__(self):
            self.emitter.indent += 1
            return self

        def __exit__(self, exc_type, exc_val, exc_tb):
            self.emitter.indent -= 1

    def indentation(self):
        """Return a context manager to increase the indentation by one."""
        return IndentedEmitter.IndentationContextManager(self)

    def __init__(self, stream=None, width=None, indent=None):
        self.stream = stream or sys.stdout
        self.width = width if width is not None else 70
        self.indent = indent if indent is not None else 1
        # Output buffer when lines are pieced together
        self.buf = io.StringIO()

    def flush(self):
        """Emit residual line (if any) to the output stream."""
        residue = self.buf.getvalue().rstrip()
        if residue:
            print(residue, file=self.stream)
        self.buf.seek(0)
        self.buf.truncate()

    close = flush  # synonym for the benefit of "with closing(...)"

    def emit(self, text="", suffix=""):
        """Write the text+suffix to self.buf.

        Start a new line if necessary.
        """
        n = len(text)
        if suffix:
            n += len(suffix)
        if self.buf.tell() + n > self.width:
            # Must start a new line first
            self.emit_line()
        self.buf.write(text)
        if suffix:
            self.buf.write(suffix)
        return self

    def emit_line(self, text=""):
        """Begin a new line with indent and optional text."""
        if self.buf.tell() > 0:
            # Flush existing buffer to output
            print(self.buf.getvalue().rstrip(), file=self.stream)
        self.buf.seek(0)
        self.buf.truncate()
        for _ in range(self.indent):
            self.buf.write("    ")
        self.buf.write(text)
        return self

    def emit_lines(self, lines):
        """Begin a new line and emit with indented multi-line text."""
        for line in lines:
            self.emit_line(line)
        return self


class JavaConstantEmitter(IndentedEmitter):
    """A class capable of emitting Java constants from Python values.

    This class extends the basic IndentedEmitter for wrapped, indented
    program text with methods that translate Python values to equivalent
    Java constants (or constructor expressions).
    """

    MAX_INT = (1 << 31) - 1
    MIN_INT = -MAX_INT - 1

    def java_int(self, value, suffix=""):
        """Emit the value as a Java int constant."""
        if self.MIN_INT <= value <= self.MAX_INT:
            return self.emit(repr(value) + suffix)
        else:
            raise ValueError("Value out of range for Java int")

    def java_string(self, value, suffix=""):
        """Emit the value as a Java String constant."""
        text = repr(str(value))
        if text.startswith("'"):
            q = '"'
            text = q + text[1:-1].replace(q, '\\"') + q
        return self.emit(text, suffix)

    def java_byte(self, value, suffix=""):
        """Emit the value as a Java int constant wrapped to signed byte."""
        bstr = format(value if value < 128 else value - 256, "d")
        return self.emit(bstr, suffix)

    def java_double(self, value, suffix=""):
        """Emit the value as a Java double constant."""
        return self.emit(repr(value), suffix)

    def java_arglist(self, handler, a, suffix=""):
        """Emit comma-separated Java values using the given handler.

        The handler is a function f(obj, suffix="") that emits the
        individual argument. It must be capable of converting all types
        that may be supplied in a.
        """
        n = len(a)
        if n == 0:
            self.emit(suffix)
        else:
            with self.indentation():
                for i in range(n - 1):
                    handler(a[i], ", ")
                handler(a[-1], suffix)
        return self

    def java_array(self, handler, a, suffix=""):
        """Emit a Java array of elements emitted by the given handler.

        The handler is a function f(obj, suffix="") that emits the
        individual element. Since Java arrays are homogeneous, it
        will often be a single bound method emitting a compatible value
        e.g. self.java_byte.
        """
        n = len(a)
        if n == 0:
            self.emit("{}", suffix)
        else:
            self.emit("{ ")
            with self.indentation():
                for i in range(n - 1):
                    handler(a[i], ", ")
                handler(a[-1], " }" + suffix)
        return self

