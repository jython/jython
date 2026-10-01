# repl_compiler.py
#
# Compile interactive input for the Jython REPL (org.python.core.Repl).
#
# Jython 3 does not yet have its own compiler, so the REPL runs this
# in a CPython subprocess of the version whose byte code Jython executes.
# Messages in both directions are a 4-byte big-endian length followed by that
# many bytes. A request is Python source (UTF-8). A reply is one of:
#
#   b'C' + marshal.dumps(code)   the source is a complete statement
#   b'I'                         the source is incomplete (need more lines)
#   b'E' + message (UTF-8)       the source cannot be compiled
#
# The process exits when its standard input is closed.

import sys, struct, marshal, codeop, traceback

SUPPORTED_VERSION = (3, 11)

if sys.version_info[:2] != SUPPORTED_VERSION:
    print(f"The Jython REPL requires CPython "
          f"{'.'.join(map(str, SUPPORTED_VERSION))} to compile, not "
          f"{sys.version.split()[0]} ({sys.executable})", file=sys.stderr)
    sys.exit(2)


def read_exactly(f, n):
    "Read n bytes from f, or return None at end of file"
    b = f.read(n)
    if len(b) < n:
        return None
    return b


def reply(f, data):
    f.write(struct.pack(">I", len(data)))
    f.write(data)
    f.flush()


def compile_source(source):
    "Compile source as interactive input and encode the result"
    try:
        code = codeop.compile_command(source, "<stdin>", "single")
    except (SyntaxError, OverflowError, ValueError) as e:
        msg = "".join(traceback.format_exception_only(e))
        return b'E' + msg.encode("utf-8")
    if code is None:
        return b'I'
    return b'C' + marshal.dumps(code)


def main():
    fin, fout = sys.stdin.buffer, sys.stdout.buffer
    while True:
        header = read_exactly(fin, 4)
        if header is None:
            break
        (n,) = struct.unpack(">I", header)
        body = read_exactly(fin, n)
        if body is None:
            break
        reply(fout, compile_source(body.decode("utf-8")))


main()
