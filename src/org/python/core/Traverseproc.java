package org.python.core;

import org.python.expose.ExposedType;
import org.python.modules.gc;

/**
 * <p>
 * This interface defines a
 * <a href="https://docs.python.org/2.7/c-api/gcsupport.html" target="_blank">
 * CPython-equivalent traverse-mechanism
 * </a> allowing to detect reference cycles. While this is crucial for cyclic
 * gc support in CPython, it only serves debugging purposes in Jython. As a
 * side-effect it allows a more complete implementation of the gc module.
 * </p>
 * <p>
 * Note that implementing this interface is only OPTIONAL.<b> Gc will work fine
 * in Jython without it. </b>Still we took care to have all core classes implement
 * it and recommend third party extension providers to do so as well with
 * custom PyObject-implementations.
 * </p>
 * <p>
 * Of course this interface shall only be implemented by {@code PyObject}s that
 * potentially own direct references to other {@code PyObject}s. Note that indirect
 * references via non-PyObjects should also be treated as "direct" (c.f.
 * tracefunc in {@link org.python.core.PyFrame}).
 * {@code PyObject}s that don't own references to other {@code PyObject}s under any
 * condition and neither inherit such references from a superclass are strictly
 * recommended to be annotated {@link org.python.core.Untraversable}.
 * </p>
 * <p>
 * Jython's traverse mechanism serves debugging purposes to ease finding memory
 * leaks and compare gc behavior with CPython. While it is of course not required
 * that gc behaviors of Jython and CPython equal, there might arise subtle bugs
 * from these different behaviors. Jython's traverse mechanism is intended to
 * allow finding such bugs by comparing gc behavior to CPython and isolating
 * the Python code that is not robust enough to work invariant under different
 * gc behaviors. See also {@link org.python.modules.gc} for more details on this.
 * </p>
 * <p>
 * Further this mechanism is crucial for some aspects of gc-support of the
 * <a href="http://www.jyni.org" target="_blank">JyNI</a>
 * project. JyNI does not strictly depend on it to emulate CPython's gc
 * for extensions, but would have to perform inefficient reflection-based
 * traversal in some edge-cases (which might also conflict with security managers).
 * </p>
 * <p>
 * Note that the slots-array and - if existent - the user-dict of {@code fooDerived}
 * classes is traversed by {@link org.python.core.TraverseprocDerived}.
 * The gc-module takes care of exploiting both traverse methods in its static traverse
 * method. So for manual traversion one should always use
 * {@link org.python.modules.gc#traverse(PyObject, Visitproc, Object)} rather
 * than directly calling methods in this interface.
 * </p>
 * <p>
 * Also note that {@link org.python.core.PyObject#objtype} is not subject to
 * {@code Traverseproc}s by default. In CPython only objects with heap-types
 * traverse their {@code ob_type}-field. In Jython, {@code fooDerived}-classes
 * are the equivalents of heapTypes. For such classes
 * {@link org.python.core.PyObject#objtype} is actually
 * traversed (along with the user dict).
 * </p>
 * <p>
 * <b>Note for implementing:</b><br>
 * Every non-static, strong-referenced {@code PyObject} should be passed to the
 * {@link org.python.core.Visitproc}. If {@code Object}s or {@code interface}-types are
 * referenced where it is not known, whether it is a {@code PyObject} or
 * references other {@code PyObjects}, one should check for {@code PyObject}
 * via {@code instanceof}. If a non-{@code PyObject}
 * implements {@code Traverseproc}, one can traverse
 * it by delegating to its {@code Traverseproc} methods.<br><br>
 * <b>Warning:</b><br>
 * If one lets non-{@code PyObject}s implement {@code Traverseproc}, extreme
 * care must be taken, whether the traverse call shall be passed on to other
 * non-{@code PyObject} {@code Traverseproc}-implementers, as this can cause
 * infinite traverse cycles.<br>
 * Examples for non-{@code PyObject}s that implement {@code Traverseproc} are
 * {@link org.python.core.PyException} and {@link com.ziclix.python.sql.Fetch}.
 * A safer, but potentially slower way to deal with
 * non-{@code PyObject}-{@code Traverseproc}s or any other non-{@code PyObject}
 * that might contain references to other {@code PyObject}s is
 * {@link org.python.modules.gc#traverseByReflection(Object, Visitproc, Object)}.
 * This is for instance used in {@link org.python.core.PyArray}.
 * </p>
 * <p>
 * <br>
 * <b>Examples</b><br><br>
 * In the following we provide some examples with code-snippets to demonstrate
 * and streamline the writing of {@code Traverseproc}-implementations.<br>
 * Since this peace of API was introduced to enhance a large existing
 * code-base, we recommend to put the {@code Traverseproc}-implementation always
 * to the end of a class and separate it from the original code by two blank
 * lines and a comment "Traverseproc implementation".<br><br>
 * Let's start with classes that don't hold references to {@code PyObject}s.
 * If the class extends some other class that implements {@code Traverseproc}, nothing
 * special needs to be done. For instance, we have this situation in
 * {@link org.python.core.PySet}. It extends {@link org.python.core.BaseSet},
 * which in turn implements {@code Traverseproc}:<br><br>
 * <pre>
 * {@literal @}ExposedType(name = "set", base = PyObject.class, doc = BuiltinDocs.set_doc)
 * public class PySet extends BaseSet {
 *   ...
 * }
 * </pre>
 * If the class neither contains {@code PyObject}-references, nor extends some
 * {@code Traverseproc}-implementing class, it is recommended to be annotated
 * {@link org.python.core.Untraversable}. {@link org.python.core.PyInteger} is
 * an example for this:<br><br>
 * <pre>
 * {@literal @}Untraversable
 * {@literal @}ExposedType(name = "int", doc = BuiltinDocs.int_doc)
 * public class PyInteger extends PyObject {
 *   ...
 * }
 * </pre>
 * If there are simply some {@code PyObject}(-subclass), non-static fields in the class,
 * let it implement {@link org.python.core.Traverseproc}.
 * Write {@link org.python.core.Traverseproc#traverse(Visitproc, Object)} by
 * just visiting the fields one by one. Check each to be non-{@code null} previously
 * unless the field cannot be {@code null} for some good reason. If
 * {@link org.python.core.Visitproc#visit(PyObject, Object)} returns non-zero,
 * return the result immediately (i.e. abort the traverse process).
 * The following example is taken from
 * {@link org.python.core.PyMethod}:<br><br>
 * <pre>
 *  /{@literal *} Traverseproc implementation {@literal *}/
 *  {@literal @}Override
 *  public int traverse(Visitproc visit, Object arg) {
 *      int retVal;
 *      if (im_class != null) {
 *          retVal = visit.visit(im_class, arg);
 *          if (retVal != 0) {
 *              return retVal;
 *          }
 *      }
 *      if (__func__ != null) {
 *          retVal = visit.visit(__func__, arg);
 *          if (retVal != 0) {
 *              return retVal;
 *          }
 *      }
 *      return __self__ == null ? 0 : visit.visit(__self__, arg);
 *  }
 * </pre>
 * Implement {@link org.python.core.Traverseproc#refersDirectlyTo(PyObject)}
 * by checking the argument to be non-{@code null} and identity-comparing it to
 * every field:<br><br>
 * <pre>
 *  {@literal @}Override
 *  public boolean refersDirectlyTo(PyObject ob) {
 *      return ob != null {@literal &&} (ob == im_class || ob == __func__ || ob == __self__);
 *  }
 * </pre>
 * If there is a Java-set or other iterable that it is not a {@code PyObject},
 * but contains {@code PyObject}s, visit every element. Don't forget to check
 * for non-{@code null} if necessary and return immediately, if
 * {@link org.python.core.Visitproc#visit(PyObject, Object)} returns non-zero.
 * The following example is taken from {@link org.python.core.BaseSet}:<br><br>
 * <pre>
 *  /{@literal *} Traverseproc implementation {@literal *}/
 *  {@literal @}Override
 *  public int traverse(Visitproc visit, Object arg) {
 *      int retValue;
 *      for (PyObject ob: _set) {
 *          if (ob != null) {
 *              retValue = visit.visit(ob, arg);
 *              if (retValue != 0) {
 *                  return retValue;
 *              }
 *          }
 *      }
 *      return 0;
 *  }
 * </pre>
 * In this case, {@link org.python.core.Traverseproc#refersDirectlyTo(PyObject)}
 * can be implemented (potentially) efficiently by using the backing set's
 * {@code contains}-method:<br><br>
 * <pre>
 *  {@literal @}Override
 *  public boolean refersDirectlyTo(PyObject ob) {
 *      return ob != null {@literal &&} _set.contains(ob);
 *  }
 * </pre>
 * If a class extends a {@code Traverseproc}-implementing class and adds
 * {@code PyObject}-references to it, the parent-{@code traverse}-method
 * should be called initially via {@code super} (example is taken from
 * {@link org.python.core.PyJavaType}):<br><br>
 * <pre>
 *  /{@literal *} Traverseproc implementation {@literal *}/
 *  {@literal @}Override
 *  public int traverse(Visitproc visit, Object arg) {
 *      int retVal = super.traverse(visit, arg);
 *      if (retVal != 0) {
 *          return retVal;
 *      }
 *      if (conflicted != null) {
 *          for (PyObject ob: conflicted) {
 *              if (ob != null) {
 *                  retVal = visit.visit(ob, arg);
 *                  if (retVal != 0) {
 *                      return retVal;
 *                  }
 *              }
 *          }
 *      }
 *      return 0;
 *  }
 * </pre>
 * In contrast to that, {@link org.python.core.Traverseproc#refersDirectlyTo(PyObject)}
 * should call its parent-method as late as possible, because that method might throw an
 * {@code UnsupportedOperationException}. By calling it in the end, we have the chance
 * to fail- or succeed fast before a potential exception occurs:<br><br>
 * <pre>
 *  {@literal @}Override
 *  public boolean refersDirectlyTo(PyObject ob) throws UnsupportedOperationException {
 *      if (ob == null) {
 *          return false;
 *      }
 *      if (conflicted != null) {
 *          for (PyObject obj: conflicted) {
 *              if (obj == ob) {
 *                  return true;
 *              }
 *          }
 *      }
 *      return super.refersDirectlyTo(ob);
 *  }
 * </pre>
 * While reflection-based traversal should be avoided if possible, it can be used to
 * traverse fields that might contain references to {@code PyObject}s, but cannot be
 * inferred at compile-time.
 * {@link org.python.modules.gc#canLinkToPyObject(Class, boolean)} can help to safe
 * some performance by failing fast if type-info already rules out the possibility
 * of the field holding {@code PyObject}-references.
 * This technique is for instance used to traverse the content of
 * {@link org.python.core.PyArray}:<br><br>
 * <pre>
 *  /{@literal *} Traverseproc implementation {@literal *}/
 *  {@literal @}Override
 *  public int traverse(Visitproc visit, Object arg) {
 *      if (data == null || !gc.canLinkToPyObject(data.getClass(), true)) {
 *          return 0;
 *      }
 *      return gc.traverseByReflection(data, visit, arg);
 *  }
 * </pre>
 * {@link org.python.modules.gc#canLinkToPyObject(Class, boolean)} also
 * offers a way to let {@link org.python.core.Traverseproc#refersDirectlyTo(PyObject)}
 * fail fast by type-information:<br><br>
 * <pre>
 *  {@literal @}Override
 *  public boolean refersDirectlyTo(PyObject ob)
 *          throws UnsupportedOperationException {
 *      if (data == null || !gc.canLinkToPyObject(data.getClass(), true)) {
 *          return false;
 *      }
 *      throw new UnsupportedOperationException();
 *  }
 * </pre>
 * <p>
 * <br>
 * <b>List of {@code PyObject}-subclasses</b><br><br>
 * We conclude with a list of {@code PyObject} subclasses in Jython, excluding
 * derived classes.<br>
 * {@code PyObject}-subclasses in Jython checked for need of {@code Traverseproc}:<br>
 * <br>
 * <br>
 * org.python.core:<br>
 *   __builtin__:<br>
 *     BuiltinFunctions              - no refs, untraversable<br>
 *     ImportFunction                - no refs, untraversable<br>
 *     SortedFunction                - no refs, untraversable<br>
 *     AllFunction                   - no refs, untraversable<br>
 *     AnyFunction                   - no refs, untraversable<br>
 *     FormatFunction                - no refs, untraversable<br>
 *     PrintFunction                 - no refs, untraversable<br>
 *     MaxFunction                   - no refs, untraversable<br>
 *     MinFunction                   - no refs, untraversable<br>
 *     RoundFunction                 - no refs, untraversable<br>
 *     CompileFunction               - no refs, untraversable<br>
 *     OpenFunction                  - no refs, untraversable<br>
 *     NextFunction                  - no refs, untraversable<br>
 *     BinFunction                   - no refs, untraversable<br>
 *   AstList                         - Traverseproc<br>
 *   BaseBytes                       - no refs, untraversable<br>
 *   BaseDictionaryView              - Traverseproc<br>
 *   BaseSet                         - Traverseproc<br>
 *   ClasspathPyImporter             - no refs, untraversable<br>
 *   ContextGuard:                   - no PyObject<br>
 *     ContextCode                   - Traverseproc<br>
 *     GeneratorContextManager       - Traverseproc<br>
 *   exceptions                      - no refs, untraversable<br>
 *     BoundStaticJavaMethod         - no refs, untraversable<br>
 *   JavaImporter                    - no refs, untraversable<br>
 *   JavaProxyList:<br>
 *     ListMethod                    - no refs, untraversable (extends PyBuiltinMethodNarrow)<br>
 *     ListMulProxyClass             - no refs, untraversable<br>
 *   JavaProxyMap:<br>
 *     MapMethod                     - no refs, untraversable (extends PyBuiltinMethodNarrow)<br>
 *     MapClassMethod                - no refs, untraversable (extends PyBuiltinClassMethodNarrow)<br>
 *   JavaProxySet:<br>
 *     SetMethod                     - no refs, untraversable (extends PyBuiltinMethodNarrow)<br>
 *     SetMethodVarargs              - no refs, untraversable (extends SetMethod)<br>
 *     CopyMethod                    - no refs, untraversable<br>
 *     IsSubsetMethod                - no refs, untraversable<br>
 *     IsSupersetMethod              - no refs, untraversable<br>
 *   Py:<br>
 *     JavaCode                      - Traverseproc<br>
 *     JavaFunc                      - no refs, untraversable<br>
 *   Py2kBuffer                      - no refs, untraversable<br>
 *   PyArray                         - Traverseproc, traverses via reflection<br>
 *   PyBaseCode                      - no refs, abstract class<br>
 *   PyBaseException                 - Traverseproc<br>
 *   PyBaseString                    - no refs, abstract class<br>
 *   PyBeanEvent                     - no refs, untraversable<br>
 *   PyBeanEventProperty             - no refs, untraversable<br>
 *   PyBeanProperty                  - no refs, untraversable<br>
 *   PyBoolean                       - no refs, untraversable<br>
 *   PyBuffer                        - no PyObject<br>
 *   PyBuiltinCallable               - no refs, untraversable<br>
 *   PyBuiltinClassMethodNarrow      - no refs, abstract class<br>
 *   PyBuiltinFunction               - no refs, untraversable<br>
 *   PyBuiltinFunctionNarrow         - no refs, untraversable<br>
 *   PyBuiltinFunctionSet            - no refs, untraversable<br>
 *   PyBuiltinMethod                 - Traverseproc<br>
 *   PyBuiltinMethodNarrow           - no refs, abstract class<br>
 *   PyBuiltinMethodSet              - Traverseproc<br>
 *   PyByteArray                     - no refs, untraversable<br>
 *   PyBytecode                      - Traverseproc<br>
 *     PyStackWhy                    - no refs, untraversable<br>
 *     PyStackException              - Traverseproc<br>
 *     PyTryBlock                    - no refs, untraversable<br>
 *   PyCallIter                      - Traverseproc (with call to super)<br>
 *   PyCell                          - Traverseproc<br>
 *   PyClass                         - Traverseproc<br>
 *   PyClassMethod                   - Traverseproc<br>
 *   PyClassMethodDescr              - no refs, untraversable<br>
 *   PyCode                          - no refs, abstract class<br>
 *   PyComplex                       - no refs, untraversable<br>
 *   PyCompoundCallable              - Traverseproc<br>
 *   PyDataDescr                     - no refs, untraversable<br>
 *   PyDescriptor                    - Traverseproc<br>
 *   PyDictionary                    - Traverseproc<br>
 *     ValuesIter                    - no refs, extends PyIterator<br>
 *     ItemsIter                     - no refs, extends PyIterator<br>
 *     PyMapKeyValSet                - no PyObject<br>
 *     PyMapEntrySet                 - no PyObject<br>
 *   PyDictProxy                     - Traverseproc<br>
 *   PyEllipsis                      - no refs, untraversable<br>
 *   PyEnumerate                     - Traverseproc<br>
 *   PyFastSequenceIter              - Traverseproc<br>
 *   PyFile                          - Traverseproc<br>
 *   PyFileReader                    - no refs, untraversable<br>
 *   PyFileWriter                    - no refs, untraversable<br>
 *   PyFloat                         - no refs, untraversable<br>
 *   PyFrame                         - Traverseproc<br>
 *   PyFunction                      - Traverseproc<br>
 *   PyGenerator                     - Traverseproc (with call to super)<br>
 *   PyIndentationError              - no PyObject<br>
 *   PyInstance                      - Traverseproc<br>
 *   PyInteger                       - no refs, untraversable<br>
 *   PyIterator                      - Traverseproc<br>
 *   PyJavaPackage                   - Traverseproc<br>
 *   PyJavaType                      - Traverseproc (with call to super)<br>
 *     EnumerationIter               - no refs, extends PyIterator<br>
 *     ComparableMethod              - no refs, abstract class<br>
 *   PyList                          - Traverseproc<br>
 *   PyLong                          - no refs, untraversable<br>
 *   PyMemoryView                    - Traverseproc<br>
 *   PyMethod                        - Traverseproc<br>
 *   PyMethodDescr                   - Traverseproc<br>
 *   PyModule                        - Traverseproc<br>
 *   PyNewWrapper                    - Traverseproc<br>
 *   PyNone                          - no refs, untraversable<br>
 *   PyNotImplemented                - no refs, untraversable<br>
 *   PyObject                        - no refs (objtype is special case)<br>
 *     PyIdentityTuple               - Traverseproc<br>
 *   PyOverridableNew                - no refs, abstract class<br>
 *   PyProperty                      - Traverseproc<br>
 *   PyReflectedConstructor          - no refs, untraversable<br>
 *   PyReflectedField                - no refs, untraversable<br>
 *   PyReflectedFunction             - Traverseproc<br>
 *   PyReversedIterator              - Traverseproc (with call to super)<br>
 *   PySequence                      - no refs, abstract class (default Traverseproc implementation)<br>
 *   PySequenceIter                  - Traverseproc (with call to super)<br>
 *   PySequenceList                  - no refs, abstract class<br>
 *   PySingleton                     - no refs, untraversable<br>
 *   PySlice                         - Traverseproc<br>
 *   PySlot                          - no refs, untraversable<br>
 *   PyStaticMethod                  - Traverseproc<br>
 *   PyString                        - no refs, untraversable (assuming baseBuffer is not a PyObject)<br>
 *   PyStringMap                     - Traverseproc<br>
 *     StringMapIter                 - no refs, extends PyIterator, abstract class<br>
 *     ItemsIter                     - no refs, extends StringMapIter<br>
 *     KeysIter                      - no refs, extends StringMapIter<br>
 *     ValuesIter                    - no refs, extends StringMapIter<br>
 *   PySuper                         - Traverseproc<br>
 *   PySyntaxError                   - no PyObject<br>
 *   PySystemState                   - Traverseproc<br>
 *     PySystemStateFunctions        - no refs, untraversable<br>
 *     PyAttributeDeleted            - no refs, untraversable<br>
 *     FloatInfo                     - Traverseproc<br>
 *     LongInfo                      - Traverseproc<br>
 *   PyTableCode                     - no refs, untraversable<br>
 *   PyTraceback                     - Traverseproc<br>
 *   PyTuple                         - Traverseproc<br>
 *   PyType                          - Traverseproc<br>
 *   PyUnicode                       - no refs, untraversable<br>
 *   PyXRange                        - no refs, untraversable<br>
 *   PyXRangeIter                    - no refs, extends PyIterator<br>
 *   SyspathArchive                  - no refs, untraversable<br>
 * <br>
 * org.python.core.stringlib:<br>
 *   FieldNameIterator               - no refs, traverses via reflection<br>
 *   MarkupIterator                  - no refs, untraversable<br>
 * <br>
 * org.python.core.util:<br>
 *   importer                        - no refs, abstract class<br>
 * <br>
 * org.python.jsr223:<br>
 *   PyScriptEngineScope             - no refs, untraversable<br>
 *     ScopeIterator                 - Traverseproc<br>
 * <br>
 * org.python.modules:<br>
 *   _codecs:<br>
 *     EncodingMap                   - no refs, untraversable<br>
 *   _hashlib:<br>
 *     Hash                          - no refs, untraversable<br>
 *   _marshal:<br>
 *     Marshaller                    - Traverseproc<br>
 *     Unmarshaller                  - Traverseproc<br>
 *   cStringIO:<br>
 *     StringIO                      - no refs, extends PyIterator<br>
 *   operator:<br>
 *     OperatorFunctions             - no refs, untraversable<br>
 *     operator                      - no refs, untraversable<br>
 *     PyAttrGetter                  - Traverseproc<br>
 *     PyItemGetter                  - Traverseproc<br>
 *     PyMethodCaller                - Traverseproc<br>
 *   PyStruct                        - no refs, untraversable<br>
 *   synchronize:<br>
 *     SynchronizedCallable          - Traverseproc<br>
 * <br>
 * org.python.modules._collections:<br>
 *   PyDefaultDict                   - Traverseproc (with call to super)<br>
 *   PyDeque                         - Traverseproc (assuming, Nodes can't build cycles)<br>
 *     PyDequeIter                   - Traverseproc (with call to super)<br>
 * <br>
 * org.python.modules._csv:<br>
 *   PyDialect                       - no refs, untraversable<br>
 *   PyReader                        - Traverseproc (with call to super)<br>
 *   PyWriter                        - Traverseproc<br>
 * <br>
 * org.python.modules._functools:<br>
 *   PyPartial                       - Traverseproc<br>
 * <br>
 * org.python.modules._io:<br>
 *   PyFileIO                        - no refs, untraversable (there is a final PyString
 *   "mode" that is guaranteed to be a PyString and no subclass; as such it needs not be
 *   traversed since it cannot have refs itself)<br>
 *   PyIOBase                        - Traverseproc<br>
 *   PyRawIOBase                     - no refs, extends PyIOBase<br>
 * <br>
 * org.python.modules._json:<br>
 *   Encoder                         - Traverseproc<br>
 *   Scanner                         - Traverseproc<br>
 *   _json:
 *     ScanstringFunction            - no refs, untraversable<br>
 *     EncodeBasestringAsciiFunction - no refs, untraversable<br>
 * <br>
 * org.python.modules._jythonlib:<br>
 *   dict_builder                    - Traverseproc<br>
 * <br>
 * org.python.modules._threading:<br>
 *   Condition                       - Traverseproc<br>
 *   Lock                            - no refs, untraversable<br>
 * <br>
 * org.python.modules._weakref:<br>
 *   AbstractReference               - Traverseproc<br>
 *   ReferenceType                   - no refs, extends AbstractReference<br>
 *   ProxyType                       - no refs, extends AbstractReference<br>
 *   CallableProxyType               - no refs, extends ProxyType<br>
 * <br>
 * org.python.modules.bz2:<br>
 *   PyBZ2Compressor                 - no refs, untraversable<br>
 *   PyBZ2Decompressor               - Traverseproc<br>
 *   PyBZ2File                       - no refs, untraversable<br>
 *     BZ2FileIterator               - no refs, extends PyIterator<br>
 * <br>
 * org.python.modules.itertools:<br>
 *   chain                           - Traverseproc (with call to super)<br>
 *   combinations                    - Traverseproc (with call to super)<br>
 *   combinationsWithReplacement     - Traverseproc (with call to super)<br>
 *   compress                        - Traverseproc (with call to super)<br>
 *   count                           - Traverseproc (with call to super)<br>
 *   cycle                           - Traverseproc (with call to super)<br>
 *   dropwhile                       - Traverseproc (with call to super)<br>
 *   groupby                         - Traverseproc (with call to super)<br>
 *   ifilter                         - Traverseproc (with call to super)<br>
 *   ifiIterfalse                    - Traverseproc (with call to super)<br>
 *   imap                            - Traverseproc (with call to super)<br>
 *   islice                          - Traverseproc (with call to super)<br>
 *   itertools:<br>
 *     ItertoolsIterator             - no refs, extends PyIterator, abstract class<br>
 *     FilterIterator                - Traverseproc, extends ItertoolsIterator<br>
 *     WhileIterator                 - Traverseproc, extends ItertoolsIterator
 *   izip                            - Traverseproc (with call to super)<br>
 *   izipLongest                     - Traverseproc (with call to super)<br>
 *   permutations                    - Traverseproc (with call to super)<br>
 *   product                         - Traverseproc (with call to super)<br>
 *   PyTeeIterator                   - Traverseproc (with call to super)<br>
 *   repeat                          - Traverseproc (with call to super)<br>
 *   starmap                         - Traverseproc (with call to super)<br>
 *   takewhile                       - Traverseproc (with call to super)<br>
 * <br>
 * org.python.modules.jffi:<br>
 *   ArrayCData                      - Traverseproc (with call to super; maybe check referenceMemory field whether it extends PyObject)<br>
 *     ArrayIter                     - no refs, extends PyIterator<br>
 *   BasePointer                     - no refs, abstract class<br>
 *   ByReference                     - no refs, untraversable (maybe check memory field whether it extends PyObject)<br>
 *   CData                           - Traverseproc (maybe check referenceMemory field whether it extends PyObject)<br>
 *   CType                           - no refs, abstract class<br>
 *     Builtin                       - no refs, untraversable<br>
 *   DynamicLibrary                  - no refs, untraversable<br>
 *   StructLayout:<br>
 *     Field                         - Traverseproc<br>
 * <br>
 * org.python.modules.posix:<br>
 *   PosixModule:<br>
 *     FstatFunction                 - no refs, untraversable<br>
 *     LstatFunction                 - no refs, untraversable<br>
 *     StatFunction                  - no refs, untraversable<br>
 *     WindowsStatFunction           - no refs, untraversable<br>
 *   PyStatResult                    - Traverseproc (with call to super)<br>
 * <br>
 * org.python.modules.random:<br>
 *   PyRandom                        - no refs, untraversable<br>
 * <br>
 * org.python.modules.sre:<br>
 *   MatchObject                     - Traverseproc<br>
 *   PatternObject                   - Traverseproc<br>
 *   ScannerObject                   - Traverseproc<br>
 * <br>
 * org.python.modules.thread:<br>
 *   PyLocal                         - Traverseproc<br>
 *   PyLock                          - no refs, untraversable<br>
 * <br>
 * org.python.modules.time:<br>
 *   PyTimeTuple                     - Traverseproc (with call to super)<br>
 *   Time:<br>
 *     TimeFunctions                 - no refs, untraversable<br>
 * <br>
 * org.python.modules.zipimport:<br>
 *   zipimporter                     - Traverseproc<br>
 * <br>
 * org.python.util:<br>
 *   InteractiveInterpreter          - no PyObject<br>
 * <br>
 * com.ziclix.python.sql:<br>
 *   DBApiType                       - no refs, untraversable<br>
 *   PyConnection                    - Traverseproc<br>
 *     ConnectionFunc                - no refs, extends PyBuiltinMethodSet<br>
 *   PyCursor                        - Traverseproc<br>
 *     CursorFunc                    - no refs, extends PyBuiltinMethodSet<br>
 *   PyExtendedCursor                - no refs, extends PyCursor<br>
 *     ExtendedCursorFunc            - no refs, extends PyBuiltinMethodSet<br>
 *   PyStatement                     - Traverseproc (because Object sql could be a PyObject or Traverseproc)<br>
 *   zxJDBC                          - no refs, untraversable<br>
 *     zxJDBCFunc                    - no refs, untraversable<br>
 * <br>
 * com.ziclix.python.sql.connect:<br>
 *   Connect                         - no refs, untraversable<br>
 *   Connectx                        - no refs, untraversable<br>
 *   Lookup                          - no refs, untraversable<br>
 * <br>
 * com.ziclix.python.sql.util:<br>
 *   BCP                             - Traverseproc<br>
 *     BCPFunc                       - no refs, extends PyBuiltinMethodSet<br>
 * <br>
 * org.python.antlr:<br>
 *   AnalyzingParser:<br>
 *     AnalyzerTreeAdaptor           - no PyObject<br>
 *   AST                             - no refs, untraversable<br>
 *   PythonErrorNode                 - no refs, extends PythonTree<br>
 *   PythonTree                      - Traverseproc<br>
 * <br>
 * org.python.antlr.ast:<br>
 *   alias                           - no refs, extends PythonTree<br>
 *   arguments                       - Traverseproc (with call to super)<br>
 *   comprehension                   - Traverseproc (with call to super)<br>
 *   keyword                         - Traverseproc (with call to super)<br>
 * <br>
 * org.python.antlr.base:<br>
 *   boolop                          - no refs, extends PythonTree<br>
 *   cmpop                           - no refs, extends PythonTree<br>
 *   excepthandler                   - no refs, extends PythonTree<br>
 *   expr_context                    - no refs, extends PythonTree<br>
 *   expr                            - no refs, extends PythonTree<br>
 *   mod                             - no refs, extends PythonTree<br>
 *   operator                        - no refs, extends PythonTree<br>
 *   slice                           - no refs, extends PythonTree<br>
 *   stmt                            - no refs, extends PythonTree<br>
 *   unaryop                         - no refs, extends PythonTree<br>
 * <br>
 * org.python.antlr.op:<br>
 *   Add                             - no refs, extends PythonTree<br>
 *   And                             - no refs, extends PythonTree<br>
 *   AugLoad                         - no refs, extends PythonTree<br>
 *   AugStore                        - no refs, extends PythonTree<br>
 *   BitAnd                          - no refs, extends PythonTree<br>
 *   BitOr                           - no refs, extends PythonTree<br>
 *   BitXor                          - no refs, extends PythonTree<br>
 *   Del                             - no refs, extends PythonTree<br>
 *   Div                             - no refs, extends PythonTree<br>
 *   Eq                              - no refs, extends PythonTree<br>
 *   FloorDiv                        - no refs, extends PythonTree<br>
 *   Gt                              - no refs, extends PythonTree<br>
 *   GtE                             - no refs, extends PythonTree<br>
 *   In                              - no refs, extends PythonTree<br>
 *   Invert                          - no refs, extends PythonTree<br>
 *   Is                              - no refs, extends PythonTree<br>
 *   IsNot                           - no refs, extends PythonTree<br>
 *   Load                            - no refs, extends PythonTree<br>
 *   LShift                          - no refs, extends PythonTree<br>
 *   Lt                              - no refs, extends PythonTree<br>
 *   LtE                             - no refs, extends PythonTree<br>
 *   Mod                             - no refs, extends PythonTree<br>
 *   Mult                            - no refs, extends PythonTree<br>
 *   Not                             - no refs, extends PythonTree<br>
 *   NotEq                           - no refs, extends PythonTree<br>
 *   NotIn                           - no refs, extends PythonTree<br>
 *   Or                              - no refs, extends PythonTree<br>
 *   Param                           - no refs, extends PythonTree<br>
 *   Pow                             - no refs, extends PythonTree<br>
 *   RShift                          - no refs, extends PythonTree<br>
 *   Store                           - no refs, extends PythonTree<br>
 *   Sub                             - no refs, extends PythonTree<br>
 *   UAdd                            - no refs, extends PythonTree<br>
 *   USub                            - no refs, extends PythonTree<br>
 * </p>
 * @see org.python.core.Untraversable
 * @see org.python.core.Visitproc
 * @see org.python.modules.gc#traverse(PyObject, Visitproc, Object)
 * @see org.python.modules.gc#traverseByReflection(Object, Visitproc, Object)
 * @see org.python.modules.gc#canLinkToPyObject(Class, boolean)
 */
public interface Traverseproc {

    /**
     * Traverses all directly contained {@code PyObject}s.
     * Like in CPython, {@code arg} must be passed
     * unmodified to {@code visit} as its second parameter.
     * If {@link Visitproc#visit(PyObject, Object)} returns
     * nonzero, this return value
     * must be returned immediately by traverse.
     *
     * {@link Visitproc#visit(PyObject, Object)} must not be
     * called with a {@code null} PyObject-argument.
     */
    public int traverse(Visitproc visit, Object arg);

    /**
     * Optional operation.
     * Should only be implemented if it is more efficient
     * than calling {@link #traverse(Visitproc, Object)} with
     * a visitproc that just watches out for {@code ob}.
     * Must return {@code false} if {@code ob} is {@code null}.
     */
    public boolean refersDirectlyTo(PyObject ob) throws UnsupportedOperationException;
}
