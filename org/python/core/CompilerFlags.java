
package org.python.core;

public class CompilerFlags extends Object {

    public CompilerFlags() {}

    public CompilerFlags(int co_flags) {
        if ((co_flags & org.python.core.PyTableCode.CO_NESTED) != 0)
            nested_scopes = true;
        if ((co_flags & org.python.core.PyTableCode.CO_FUTUREDIVISION) != 0)
            division = true;
        if ((co_flags & org.python.core.PyTableCode.CO_GENERATOR) != 0)
            generator = true;
        if ((co_flags & org.python.core.PyTableCode.CO_GENERATOR_ALLOWED) != 0)
            generator_allowed = true;
    }

    public boolean nested_scopes = true;
    public boolean division;
    public boolean generator;
    public boolean generator_allowed;

    // 'interactive' is true when reading from the console.
    // When false en empty line does not terminate an indented statement.
    public boolean interactive;

    public String encoding;
}
