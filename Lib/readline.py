from __future__ import with_statement
import os.path
import sys

try:    
    reader = sys._jy_interpreter.reader
except AttributeError:
    raise ImportError("Cannot access JLineConsole")

history_list = None

def _setup_history():
    # This is obviously not desirable, but avoids O(n) workarounds to
    # modify the history (ipython uses the function
    # remove_history_item to mutate the history relatively frequently)
    global history_list
    
    history = reader.history
    try:
        history_list_field = history.class.getDeclaredField("history")
        history_list_field.setAccessible(True)
        history_list = history_list_field.get(history)
    except:
        pass

_setup_history()

def parse_and_bind(string):
    # TODO this should probably reinitialize the reader, if possible
    # with any desired settings; this will require some more work to implement

    # most importantly, need to support at least
    # readline.parse_and_bind("tab: complete")
    # but it's possible we can readily support other aspects of a readline file

    # XXX first time through, print a warning message about the required setup
    # with jline properties (or potentially test...)
    pass

def get_line_buffer():
    return str(reader.cursorBuffer.buffer)

def insert_text(string):
    reader.putString(string)
    
def read_init_file(filename=None):
    print "Not implemented: read_init_file", filename

def read_history_file(filename="~/.history"):
    expanded = os.path.expanduser(filename)
    new_history = reader.getHistory().getClass()()
    with open(expanded) as f:
        for line in f:
            new_history.addToHistory(line.rstrip())
    reader.history = new_history
    _setup_history()

def write_history_file(filename="~/.history"):
    expanded = os.path.expanduser(filename)
    with open(expanded, 'w') as f:
        for line in reader.history.historyList:
            f.write(line)

def clear_history():
    reader.history.clear()

def add_history(line):
    reader.addToHistory(line)

def get_history_length():
    return reader.history.maxSize

def set_history_length(length):
    reader.history.maxSize = length

def get_current_history_length():
    return len(reader.history.historyList)

def get_history_item(index):
    return reader.history.historyList[index]

def remove_history_item(pos):
    if history_list:
        history_list.remove(pos)
    else:
        print "Cannot remove history item at position:", pos

def redisplay():
    reader.redrawLine()

def set_startup_hook(function=None):
    sys._jy_interpreter.startupHook = function
    
def set_pre_input_hook(function=None):
    print "Not implemented: set_pre_input_hook", function

_completer_function = None

def set_completer(function=None):
    """set_completer([function]) -> None
    Set or remove the completer function.
    The function is called as function(text, state),
    for state in 0, 1, 2, ..., until it returns a non-string.
    It should return the next possible completion starting with 'text'."""

    global _completer_function
    _completer_function = function

    def complete_handler(buffer, cursor, candidates):
        start = _get_delimited(buffer, cursor)[0]
        delimited = buffer[start:cursor]
        for state in xrange(100): # TODO arbitrary, what's the number used by gnu readline?
            completion = None
            try:
                completion = function(delimited, state)
            except:
                pass
            if completion:
                candidates.add(completion)
            else:
                break
        return start

    reader.addCompletor(complete_handler)
    

def get_completer():
    return _completer_function

def _get_delimited(buffer, cursor):
    start = cursor
    for i in xrange(cursor-1, -1, -1):
        if buffer[i] in _completer_delims:
            break
        start = i
    return start, cursor

def get_begidx():
    return _get_delimited(str(reader.cursorBuffer.buffer), reader.cursorBuffer.cursor)[0]

def get_endidx():
    return _get_delimited(str(reader.cursorBuffer.buffer), reader.cursorBuffer.cursor)[1]

def set_completer_delims(string):
    global _completer_delims, _completer_delims_set
    _completer_delims = string
    _completer_delims_set = set(string)

def get_completer_delims():
    return _completer_delims

set_completer_delims(' \t\n`~!@#$%^&*()-=+[{]}\\|;:\'",<>/?')
