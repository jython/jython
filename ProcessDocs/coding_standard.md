# Coding Standard

When contributing code or patches to Jython,
please try to follow these guidelines.

This guidance is quite old (Java 6 or earlier)
so will not answer all questions.
It applies to Jython 2.
It has been rescued from the Jython Wiki.

Parts of the Jython code base do not conform very well to this guide.
(They may be even older than the rules.)
Avoid the temptation to reformat them when working on a contribution,
as it makes it difficult for a reviewer to see
the code change being contributed.


## Python Code

In general, follow PEP 8.

When importing Java code, always use fully qualified class names,
not package names i.e. `from java.lang import String`
instead of `from java import lang`.

## Java Code

In general, have in mind that code is read more times than it is written,
and that contributors not familiar with your thinking
will seek to maintain or extend it.
Code that cannot be followed by others is likely to be replaced
when it is found (or suspected) to be the source of a bug or limitation,
and then the value of your excellent work is lost.

1.  Javadoc on any publicly exposed method or field.
2.  4 spaces for indentation, no tabs.
3.  No nested ternary statements (no ternary statments inside other ternaries).
4.  A luxurious 100 characters per line.
5.  No copy and pasted, repeated code:
    if you're doing the same thing twice, make a method.
6.  Braces on all loops and `if-else` statements
7.  A space between an if and its parenthesis i.e. `if (` instead of `if(`.
8.  Spaces between annotation element-value pairs,
    i.e. `@ExposedType(name = "unicode", base = PyBaseString.class)`
    instead of `@ExposedType(name="unicode",base=PyBaseString.class)`.
9.  Methods longer than 10 lines should have whitespace and comments breaking them up into coherent operations.
10. Descriptive names for fields and methods. 
11. No @author tags in code. 
12. Any field on an object that isn't modified after construction should be final.
13. Fields at the top of the class. 
14. Don't declare fields with their default values ie `private Object blah;`
    instead of private `Object blah = null;`
    and `int i;` instead of `int i = 0;`
15. Comments begin with a space unless they're commented out code: 
    Poor:
    ```
    //TODO: Not implemented yet 
    // bar.bar()
    ```
    Better:
    ```
    // TODO: Not implemented yet 
    //bar.bar()
    ```

Beyond these rules, follow the Sun Java standards.

> [!NOTE]
> We should provide a set of formatting definitions that can be imported into
> the Eclipse IDE to get it to follow the standards.
> The Java formatter in VSCode will read the rules Eclipse exports.
> In 2025 this was incomplete and provided no way to edit the rules,
> buthings may have moved on,
> see https://code.visualstudio.com/docs/java/java-linting.


### Example (adapted from Sun document)

```java
package org.jython.blah;
import org.jython.blah.BlahBlah;
/**
 * Class description goes here.
 */
public class Blah extends SomeClass {
    /* A class implementation comment can go here. */
    /** classVar1 documentation comment */
    public static int classVar1;
    /**
     * classVar2 documentation comment that happens to be
     * more than one line long
     */
    private static Object classVar2;
    /** instanceVar1 documentation comment */
    public Object instanceVar1;
    /** instanceVar2 documentation comment */
    protected int instanceVar2;
    /** instanceVar3 documentation comment */
    private Object[] instanceVar3;

    /**
     * ...constructor Blah documentation comment...
     */
    public Blah() {
        // ...implementation goes here...
    }

    /**
     * ...method doSomething documentation comment...
     */
    public void doSomething() {
        // ...implementation goes here...
    }

    /**
     * ...method doSomethingElse documentation comment...
     * @param someParam description
     */
    public void doSomethingElse(Object someParam) {
        // ...implementation goes here...
    }
}
```

