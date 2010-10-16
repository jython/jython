from __future__ import with_statement
import os.path
import sys

try:    
    reader = sys._jy_interpreter.reader
except AttributeError:
    raise ImportError("Cannot access JLineConsole")

def parse_and_bind(string):
    # TODO this should probably reinitialize the reader, if possible
    # with any desired settings; this will require some more work to implement

    # most importantly, need to support at least
    # readline.parse_and_bind("tab: complete")
    # but it's possible we can readily support other aspects of a readline file
    pass

def get_line_buffer():
    return str(reader.cursorBuffer.buffer) # or use toString to get unicode?

def insert_text(string):
    reader.putString(string)
    
def read_init_file(filename=None):
    pass

def read_history_file(filename="~/.history"):
    expanded = os.path.expanduser(filename)
    new_history = reader.getHistory().getClass()()
    with open(expanded) as f:
        for line in f:
            new_history.addToHistory(line)
    reader.history = new_history

def write_history_file(filename="~/.history"):
    expanded = os.path.expanduser(filename)
    with open(expanded, 'w') as f:
        for line in reader.history.historyList:
            f.write(line)

def clear_history():
    reader.history.clear()

def get_history_length():
    return reader.history.maxSize

def set_history_length(length):
    reader.history.maxSize = length

def get_current_history_length():
    return len(reader.history.historyList)

def get_history_item(index):
    return reader.history.historyList[index]

def remove_history_item(pos):
    # TODO possible?
    raise Exception("not implemented")

def redisplay():
    reader.redrawLine()

def set_startup_hook(function=None):
    # TODO add
    pass

def set_pre_input_hook(function=None):
    # TODO add
    pass


_completion_function = None

def set_completer(function=None):
    """set_completer([function]) -> None
    Set or remove the completer function.
    The function is called as function(text, state),
    for state in 0, 1, 2, ..., until it returns a non-string.
    It should return the next possible completion starting with 'text'."""

    _completion_function = function

    def complete_handler(buffer, cursor, candidates):
        for state in xrange(100): # TODO arbitrary, what's the number used by gnu readline?
            completion = None
            try:
                completion = function(buffer[:cursor], state)
            except:
                pass
            if completion:
                candidates.add(completion)
            else:
                break
        return 0

    reader.addCompletor(complete_handler)
    

def get_completer():
    return _completion_function

def get_begidx():
    # TODO add
    pass

def get_endidx():
    # TODO add
    pass

def set_completer_delims(string):
    pass

def get_completer_delims():
    pass

def add_history(line):
    reader.addToHistory(line)


