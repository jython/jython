""" Emulate module 'readline' from CPython.
We are using the JavaReadline JNI wrapper for GNU readline.

2004-10-27, mark.asbach@rwth-aachen.de

"""

from __future__ import with_statement
import os.path
import sys

# XXX move to _jline_readline.py, just like our _gnu_readline.py (which is orphaned)
# then simply try successive imports to see what is installed

# XXX what's oru character encoding issues here, if any?

try:    
    reader = sys._jy_interpreter.reader
    #from JLine import Completor
except AttributeError:
    raise ImportError("Cannot access JLineConsole")

def parse_and_bind(string):
    # XXX this should probably reinitialize the reader, if possible
    # with any desired settings; this will require some more work to implement

    # most importantly, need to support
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
    raise Exception("not implemented")

def redisplay():
    reader.drawLine() # XXX not certain

def set_startup_hook(function=None):
    pass

def set_pre_input_hook(function=None):
    pass


_completion_function = None

def set_completer(function=None):
    # XXX single method interface, http://jline.sourceforge.net/apidocs/jline/Completor.html
    # just need to figure out what's necessary to adapt to Python's convention,
    # but it should be fine :)

    """The completer method is called as completerclass.completer(text, state), for state in 0, 1, 2, ..., 
    until it returns a non-string value. It should return the next possible completion starting with text."""

    _completion_function = function

    def complete_handler(buffer, cursor, candidates):
        for state in xrange(100):
            completion = function(buffer[:cursor], state)
            if completion:
                candidates.add(completion)
            else:
                break
        return 0

    reader.addCompletor(complete_handler)
    

def get_completer():
    return _completion_function

def get_begidx():
    pass

def get_endidx():
    pass

def set_completer_delims(string):
    pass

def get_completer_delims():
    pass

def add_history(line):
    reader.addToHistory(line)


