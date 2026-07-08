# How to Expose Python Types from Java

## Introduction

Starting in Jython 2.5,
Python types are exposed from Java code by adding several different
annotations to fields and methods on the Java class
to define the fields and methods that will be visible
on the resulting Python type.
A bytecode processor then adds code to the compiled class
to build the type's dict and descriptors.
An Ant task, `org.python.expose.generate.ExposeTask`,
handles finding the classes to expose, running the processor on them,
and writing them back out.
This document describes how to use this type exposing system.

## Adding `@ExposedType` and generating bytecode

The first step in exposing a class is to use the `org.python.expose.ExposedType`
annotation on the actual class to indicate that it should be exposed.
This annotation goes on the class itself and has three optional fields.
The first, name, defines what the type will be called in Python.
If it isn't specified,
the type's name is just assumed to be the same as the Java class name.
The second field, base, indicates the base type this type extends.
It must be another Java class that's likewise
been annotated with `@ExposedType` and sets up the type hierarchy in Python.
If unspecified,
it's assumed that this type extends from the base newstyle Python type, object.
Unlike classes defined in Python code,
types defined in Java can only use single inheritance.
The third, `isBaseType`,
defines if this type can be subclassed from Python code.
When unspecified this value defaults to true.
Any type defined in CPython
that lacks a Py_TPFLAGS_BASETYPE bit in its tp_flags
should have `isBaseType` set to `false` in Jython.

Once the class has `@ExposedType` on it,
it can be fed into the type bytecode processor
to fill in the additional bytecode necessary
to actually make the class work as a type.
The resulting type won't have any fields or methods defined on it,
but they can be added later.
To make the Ant task aware of it,
the newly exposed class needs to be added to `CoreExposed.includes` file
in the base of the jython checkout.
CoreExposed just contains a single line for each file to be exposed
with the full class name as a path in it,
ie `org/python/core/PyObject.class` for `PyObject`.
After adding the class to CoreExposed,
executing ant will compile the code and process the new bytecode.

## Exposing methods with `@ExposedMethod`

To actually add methods to the type,
they need to be exposed with `org.python.expose.ExposedMethod`.
`@ExposedMethod` can be applied to non-static methods on the class
that are public, protected or package protected.
When the bytecode processor encounters an `@ExposedMethod`,
it generates a new inner class extending `PyMethodDescriptor`
that simply calls the annotated method when bound.
Because it's being called from a method descriptor,
and Python method descriptors are available on types
regardless of their location in the inheritance hierarchy,
it's generally a good idea to make exposed methods final.
This ensures that subclasses won't override the behavior of the method,
so the descriptor always calls the same code
when accessed directly off of the type.
Also, exposed methods should avoid to call other virtual methods,
directly or indirectly,
unless the interactions with subclasses overriding the invoked methods
are clearly documented and understood.

The generated descriptor maps the generic `PyObject.__call__` method
into the actual exposed method.
Like `__call__`, it will pass up to four arguments directly to the method.
If more than four arguments are required,
the method must take the generic `__call__` arguments, `PyObject[], String[]`.
For methods with four or fewer arguments,
the descriptor will also coerce its `PyObject` arguments into `String`s or
Java primitives if the method takes that type.
If the `PyObject` can't be coerced to that type,
a `TypeException` will be raised.
If the argument isn't a String or primitive,
it must be a regular `PyObject` and the method must do its own type checking.

The return type from the method can be coerced in a similar fashion.
Returns of String and char will be wrapped in a `PyString`;
int, byte and short in `PyInteger`;
long in `PyLong`;
float and double in `PyFloat`;
and boolean in `PyBoolean`.
If the method returns void, `PyNone` will be returned in its place.
If it doesn't return any of these types, it must return `PyObject`.

As with `@ExposeType`,
there are several fields on `@ExposedMethod` that control
how it appears in Python.
The names field operates similarly to the name field on `@ExposedType`,
except that it accepts multiple values
in case the method needs to appear under multiple names in the type.
For instance, the method `int_toString` on `PyInteger` is exposed with
`names = {"__repr__", "__str__"}` as it works as
both `__repr__` and `__str__` for int.
If names isn't specified,
the Java method's name is used to determine what the method will be exposed as.
If the name starts with the type name followed by an underscore,
the method will be exposed as the portion of its name following the underscore.
This allows `final`, package protected methods to be specified for types
that on `PyObject` as well without requiring setting the name on all of them.
For example, `PyInteger`'s exposed method `int___add__` doesn't set names,
so it appears in its type dict as `__add__`.
If the method name doesn't start with the type name and an underscore,
it's just exposed as the full method name.

The next field, defaults, allows the method to specify that
some of its arguments are optional,
and if they aren't supplied, what values fill in.
If the Java method takes three arguments,
and two defaults are given in the annotation,
it can be called from Python with anywhere from one to three arguments.
If only one argument is given,
the first default is used for the second argument,
and the second default for the third argument.
If two arguments are given,
the second default is again used for the third argument.
If three arguments are given, the defaults are ignored.
If only a single default is given, it will only be used for the final argument.
As with argument coercion,
defaults can only be used with methods taking up to four arguments.
The defaults can take three types of value:

`null`
    the String null produces a Java null

`Py.None`
    the String `Py.None` produces the Python `None` value

primitive
    if the method takes a Java primitive
   (`boolean`, `byte`, `short`, `char`, `int`, `long`, `float` or `double`)
   in the default's position, the string will be parsed using
   the primtive type's Java wrapper (e.g. `java.lang.Boolean` for `boolean`).

The final field on `@ExposedMethod` is type.
This field takes one of three `MethodType` enums.
The aptly named default value, `DEFAULT`,
indicates that the method descriptor should simply call the exposed method
and return using the normal coercion directly.
`BINARY` indicates that the method is a binary operation
like `__add__` or `__sub__`.
For these types, the descriptor checks if the method returned null,
and if so, it raises a `NotImplemented` exception.
This is generally used in the case where the binary method
doesn't handle the passed in type.
The `CMP` type is only for `__cmp__` methods.
If used, it checks if the method returns -2, and if so,
raises a `TypeException`.
This is the same sort of type check that `BINARY` is doing with `null`.


## Exposing fields with `@ExposedGet`, `@ExposedSet` and `@ExposedDelete`

A trio of annotations are used to expose a field on a type,
each handling a different aspect of accessing the field.
`@ExposedGet` takes care of read access.
It can be applied to a method that takes no arguments
and returns a value, or to a field.
If on a method, that method will be called every time read access is made
on that field on instances of the type.
If on a field, the descriptor will just directly access that field
on the instance and return it.
`@ExposedSet` can also be applied to a field or a method.
If used on a method, the method must take a single argument
of the same type as the `@ExposedGet` with the same name.
`@ExposedDelete` is only allowed on methods that return `void`
and take no arguments.
If specified, when del `typeinstance.fieldname` is invoked in Python,
that delete method will be called.
Neither `@ExposedDelete` nor `@ExposedSet` can be used
if an `@ExposedGet` of the same doesn't exist on the type.
The names of the exposed field can be specified as `name` in the annotation,
or if that isn't specified the name is taken directly
from the name of the field or method.


## Making the type instantiable with `@ExposedNew`

The final step in making a Java class usable as a Python type
is to make the type instantiable by adding a `__new__` method to it.
This is done with the `@ExposedNew` annotation.
If there is no `@ExposedNew` in the class,
the type won't be instantiable from Python.
It can still be created from Java by calling its constructors directly.
See `org.python.core.PyNone` for an example of this.
However, in most cases, a type should be creatable from Python,
so it needs an `@ExposedNew`.
There are two ways it can be used,
a simple way for types that allow their subtypes to completely override
the `__new__` process,
and a more complicated version for types that need
to have their `__new__` invoked by every subclass.

In the simple form, `@ExposedNew` is applied to an instance method
that takes the standard Jython call arguments,
`PyObject[] args, String[] keywords`.
In this form, the basic new functionality is handled by
`org.python.core.PyOverridableNew`
and the method annotated with `@ExposedNew` is called as `__init__`
as part of that process for instantiating the defined type directly.
This allows subtypes to completely redefine new
and create objects however they like.

In the more complex form, `@ExposedNew` must be applied to a static method
that takes the arguments `PyNewWrapper new_, boolean init, PyType subtype,
PyObject[] args, String[] keywords`.
In this case,
the method has full responsibility for creating and initting the object
and will be invoked for every subtype of this exposed type.
Essentially it's for object instantation
that must be called for every instance of that object.
See `PyInteger.int_new` for an example of this type of `@ExposedNew`.

With either form, there can be only one `@ExposedNew` per class.


## Post-processing

Exposing Python types involves creating classes
that the class loader can find,
and then (as above) calling `PyType.fromClass()` on some class.
The classes get created by the `ExposedTypeProcessor` class
in `org.python.expose.generate`, 
but as of this writing (r5593),
these classes are not included in the Jython jar.
It is intended that eventually `ExposedTypeProcessor`
and related classes will be shipped separately,
as a support jar.
Your `build.xml` should also have a clause that runs the `ExposeTask`
on the relevant classes, like this:
```xml
<taskdef name="expose" classname="org.python.expose.generate.ExposeTask">
  <classpath>
    <path refid="classpath" />
    <pathelement path="${compile.dir}" />
  </classpath>
</taskdef>
```
```xml
<expose srcdir="${compile.dir}"
        destdir="${exposed.dir}"
        includesfile="${base.dir}/JythonExposed.includes"/>
```
(Taken from jython's `build.xml`.)
You probably want to make your non-preprocessed classes
(all the ones in `JythonExposed.includes`)
get compiled someplace else,
and only have the compiled ones show up in this "exposed" directory,
so that you don't have two versions of each class on your classpath,
and be careful if you're using an IDE.

See also [Generated Derived Classes](generated_derived_classes.md).
