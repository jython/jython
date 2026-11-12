// Copyright (c)2026 Jython Developers.
// Licensed to PSF under a contributor agreement.
package org.python.kernel;

import static org.python.internal._PyUtil.cantSetAttributeError;
import static org.python.internal._PyUtil.mandatoryAttributeError;
import static org.python.internal._PyUtil.readonlyAttributeError;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.python.core.Abstract;
import org.python.core.ArgumentError;
import org.python.core.Callables;
import org.python.core.MethodDescriptor;
import org.python.core.PyAttributeError;
import org.python.core.PyBaseException;
import org.python.core.PyBytes;
import org.python.core.PyDict;
import org.python.core.PyErr;
import org.python.core.PyExc;
import org.python.core.PyFloat;
import org.python.core.PyList;
import org.python.core.PyLong;
import org.python.core.PyObject;
import org.python.core.PySequence;
import org.python.core.PyTuple;
import org.python.core.PyType;
import org.python.core.PyUnicode;
import org.python.core.PyWrapperDescr;
import org.python.internal.EmptyException;
import org.python.internal._PyUtil;
import org.python.kernel.TypeFactory.Clash;
import org.python.subclass.SubclassFactory;
import org.python.subclass.SubclassSpec;
import org.python.support.InterpreterError;
import org.python.types.Exposed;
import org.python.types.Exposed.KeywordCollector;
import org.python.types.FastCall;
import org.python.types.Feature;
import org.python.types.TypeFlag;
import org.python.types.TypeSpec;
import org.python.types.WithDict;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A base shared by the concrete implementation classes of the Python
 * {@code type} object. The class is used widely in the run time system
 * where a {@link PyType} might have been expected, so that methods may
 * be accessed that are not exposed as Jython API. Where a
 * {@link PyType} is accepted from outside the run-time system it must
 * be cast to {@code BaseType} (see {@link BaseType#cast(PyType)}). This
 * will succeed for all genuine Jython type objects.
 * <p>
 * In the layered architecture of the Python type object, this class
 * takes responsibility for attributes and lookup along the MRO. It
 * contains the apparatus to make the type "Python-ready".
 */
public abstract sealed class BaseType extends KernelType implements WithDict
        permits SimpleType, ReplaceableType, AdoptiveType {

    /** Logger for type object activity in the kernel. */
    protected static final Logger logger = LoggerFactory.getLogger(BaseType.class);

    /** Subclass factory} to use in creating subclasses. */
    static final SubclassFactory SUBCLASS_FACTORY = new SubclassFactory("JY$%s$%d");

    /**
     * Name of the type. This is the name exposed as {@link #getName()
     * __name__}. In an immutable type, it is defined at the creation of the
     * type.
     */
    // TODO Approach to __name, __qualname__, __module__.
    /*
     * The legacy handling of class names is a mess in CPython, but improved
     * with the introduction of heap types. We can probably follow only the
     * code path for heap types, if we are careful to set __module__ =
     * 'builtins' for built-in types.
     */
    // Compare CPython _heaptypeobject.ht_name in cpython/object.h
    private String name;

    /**
     * Qualified name of the type. This is the name exposed as
     * {@code __qualname__}.
     */
    // Compare CPython _heaptypeobject.ht_qualname in cpython/object.h
    private String qualname;

    /**
     * The {@code __mro__} of this type, that is, the method resolution
     * order, as defined for Python and constructed by the {@code mro()}
     * method (which may be overridden), by analysis of the
     * {@code __bases__}.
     */
    protected BaseType[] mro;

    /**
     * The dictionary of the type is always an ordered {@code Map}. The
     * real, writable dictionary is private because the type controls
     * writing strictly. Even in the kernel it is only accessible through a
     * read-only view {@link #dict}.
     */
    private final LinkedHashMap<String, Object> _dict;

    /**
     * The real, writable dictionary is made accessible here through a
     * wrapper that renders it a read-only {@code dict}-like object.
     * Internally names are stored as {@code String} for speed and accessed
     * via {@link #lookup(String)}.
     */
    protected final Map<String, Object> dict;

    /**
     * The resolved {@code __new__} method of this type object, which should
     * be callable. See {@link #updateNewMethodCache()}.
     */
    // TODO consider thread safety of newMethodCache
    private FastCall newMethodCache;

    /**
     * Constructor used by (permitted) subclasses of {@code PyType}.
     *
     * @param name of the type (fully qualified)
     * @param javaClass implementing Python instances of the type
     * @param bases of the new type
     */
    protected BaseType(String name, Class<?> javaClass, BaseType[] bases) {
        super(javaClass, bases);
        assert name != null;
        this.name = name;
        this.qualname = name;
        this._dict = new LinkedHashMap<>();
        // FIXME: define mappingproxy type for this use
        this.dict = Collections.unmodifiableMap(_dict);
    }

    @Override
    @Exposed.Getter("__name__")
    public String getName() { return name; }

    /**
     * Set the name of the type, which may be changed by assignment in a
     * mutable type.
     *
     * @param name of the type
     */
    @Exposed.Setter("__name__")
    public void setName(String name) {
        checkTypeMutable(name, "__name__");
        this.name = name;
    }

    @Override
    @Exposed.Getter("__qualname__")
    public String getQualName() { return qualname; }

    /**
     * Set the qualified name of the type, which may be changed by
     * assignment in a mutable type.
     *
     * @param qualname of the type
     */
    @Exposed.Setter("__qualname__")
    public void setQualName(String qualname) {
        checkTypeMutable(qualname, "__qualname__");
        this.qualname = qualname;
    }

    @Override
    public BaseType[] getMRO() { return Arrays.copyOf(mro, mro.length, BaseType[].class); }

    /**
     * Compute and install the MRO from the bases. Used from type factory
     * and during certain kinds of change to the type hierarchy.
     */
    // Compare CPython mro_internal in typeobject.c
    final void computeInstallMRO() {
        // TODO propagate MRO change to Python subclasses
        try {
            mro = mro_invoke();
        } catch (Throwable e) {
            // FIXME handle errors creating new MRO with roll-back
            throw new InterpreterError(e, "Computing new MRO of '%s'", this.getName());
        }
    }

    // Compare CPython PyType_IsSubtype in typeobject.c
    // CPython documentation:
    // int PyType_IsSubtype(PyTypeObject *a, PyTypeObject *b)
    // Return true if a is a subtype of b.
    //
    // This function only checks for actual subtypes, which means that
    // __subclasscheck__() is not called on b. Call
    // PyObject_IsSubclass() to do the same check that issubclass()
    // would do.
    @Override
    public boolean isSubTypeOf(PyType b) {
        if (mro != null) {
            /*
             * Deal with multiple inheritance without recursion by walking the MRO
             * tuple
             */
            for (PyType base : mro) { if (base == b) { return true; } }
            return false;
        } else
            // a is not completely initialised yet; follow base
            return type_is_subtype_base_chain(b);
    }

    // Compare CPython PyType_GetFlags
    @Override
    public EnumSet<TypeFlag> getFeatures() {
        // No synchronisaton since final before published
        return features.clone();
    }

    // unsigned long PyType_GetFlags(PyTypeObject *type)
    // Maybe in addition to getFeatures() for CPython compatibility

    // int PyType_Ready(PyTypeObject *type)
    // Finalize a type object. This should be called on all type objects
    // to finish their initialization. This function is responsible for
    // adding inherited slots from a type’s base class. Return 0 on
    // success, or return -1 and sets an exception on error.

    // PyObject *PyType_GetName(PyTypeObject *type)
    // Return the type’s name. Equivalent to getting the type’s __name__
    // attribute.
    // New in version 3.11.

    // PyObject *PyType_GetQualName(PyTypeObject *type)
    // Return the type’s qualified name. Equivalent to getting the
    // type’s __qualname__ attribute.
    // New in version 3.11.

    // PyObject *PyType_GetModule(PyTypeObject *type)
    // Return the module object associated with the given type when the
    // type was created using PyType_FromModuleAndSpec().

    // void *PyType_GetModuleState(PyTypeObject *type)
    // Return the state of the module object associated with the given
    // type.

    // PyObject *PyType_GetModuleByDef(PyTypeObject *type, struct
    // PyModuleDef *def)
    //
    // Find the first superclass whose module was created from the given
    // PyModuleDef def, and return that module.

    /**
     * The dictionary of the {@code type} in a read-only view.
     *
     * @return dictionary of the {@code type} in a read-only view.
     */
    @Override
    // @Getter("__dict__")
    @SuppressWarnings({"unchecked", "rawtypes"})
    public final Map<Object, Object> getDict() {
        // TODO Ought to be a mappingproxy
        // For now just erase type: safe (I think) since unmodifiable.
        return (Map)dict;
    }

    /**
     * How durable is the result of the lookup. This supports the
     * possibility of a client behaviour adapting to a result that is valid
     * until a change is notified. See the callback option in
     * {@link lookup(String, Consumer)}.
     */
    public enum LookupStatus {
        /** The result is valid for just this enquiry. */
        ONCE,
        /** The result is valid until notified. */
        CURRENT,
        /** The result is final: valid forever. */
        FINAL;
    }

    /**
     * Extended information returned from a lookup on a type object when we
     * need it.
     *
     * @param obj the object found
     * @param where type in which it was found
     * @param status the extent to which this result is stable
     */
    public static record LookupResult(Object obj, BaseType where, LookupStatus status) {}

    /**
     * Lookup for the definition of the name along the MRO, returning an
     * extended result that includes where it was found and whether it might
     * change. The caller provides a callback method that will be called
     * every time the definition of this name changes.
     *
     * @param name to look up
     * @param callback to deliver updates (or {@code null})
     * @return extended result or {@code null} if not found
     */
    // FIXME Support the lookup callback properly
    // Deal with *some* uses not needing a callback when found on this
    public LookupResult lookup(String name, Consumer<LookupResult> callback) {
        /*
         * CPython wraps this in a cache keyed by (type, name) and sensitive to
         * the "version" of this type. (Version changes when any change occurs,
         * even in a super-class, that would alter the result of a look-up.) We
         * do not reproduce that at present.
         */

        // CPython checks here to see if this type is "ready".
        // Could we be "not ready" in some loop of types? Think not.

        LookupStatus status = LookupStatus.FINAL;

        /* Search along the MRO. */
        for (BaseType base : mro) {
            Object obj;
            /*
             * The result is not FINAL if *any* type on the way is mutable since
             * that type could later introduce a definition in front of the one we
             * now find.
             */
            if (status == LookupStatus.FINAL && base.isMutable()) {
                status = callback == null ? LookupStatus.ONCE : LookupStatus.CURRENT;
            }
            if ((obj = base.dict.get(name)) != null) {
                // We found a definition in type object base.
                return new LookupResult(obj, base, status);
            }
        }
        return null;
    }

    // Compare CPython _PyType_Lookup in typeobject.c
    // and find_name_in_mro in typeobject.c
    @Override
    public Object lookup(String name) {

        /*
         * CPython wraps this in a cache keyed by (type, name) and sensitive to
         * the "version" of this type. (Version changes when any change occurs,
         * even in a super-class, that would alter the result of a look-up.) We
         * do not reproduce that at present.
         */

        // CPython checks here to see in this type is "ready".
        // Could we be "not ready" in some loop of types? Think not.

        for (BaseType base : mro) {
            Object res;
            if ((res = base.dict.get(name)) != null)
                return res;
        }
        return null;
    }

    /**
     * {@inheritDoc}
     * <p>
     * For a {@link SimpleType}, this is a list with exactly one element:
     * the type itself. In a {@link ReplaceableType}, the single element is
     * a {@link SharedRepresentation} shared with those types that may
     * replace this type on an object. Only an {@link AdoptiveType} is able
     * to support multiple representations.
     */
    @Override
    public abstract List<Representation> representations();

    /**
     * Return a Python callable that is the result of looking up
     * {@code __new__} of this type and wrapping it (if necessary) so that
     * it may be called from Java with the usual arguments
     * {@code __new__(cls, name, bases, namespace, **kwargs)}.
     *
     * @return a new instance of this type
     */
    public FastCall newMethod() {
        // FIXME remove when we propagate changes to sub-types:
        if (newMethodCache == null) { updateNewMethodCache(); }
        return newMethodCache;
    }

    /**
     * Determine (or create if necessary) the Python type for the given
     * object. In the run-time system, we use this in place of
     * {@link PyType#of(Object)}, so that we get the more specific type of
     * result.
     *
     * @param o for which a type is required
     * @return the type
     */
    public static BaseType of(Object o) { return registry.get(o.getClass()).pythonType(o); }

    /**
     * Cast the argument to a Jython type object (or throw). It is possible
     * for a client application to supply a {@link PyType} that did not
     * originate in the Jython run-time system, whereas internally we take
     * advantage of private API that requires a {@code BaseType}.
     * Occasionally, we have to check that, usually on assignment to a local
     * variable or a member.
     *
     * @param t presented to our API somewhere
     * @return {@code t} if it is a {@code BaseType}
     * @throws PyBaseException {@code t} is not a {@code BaseType}
     */
    public static BaseType cast(Object t) throws PyBaseException, ClassCastException {
        if (t instanceof BaseType bt) {
            return bt;
        } else if (t instanceof PyType) {
            throw new ClassCastException(
                    String.format("non-Jython PyType encountered: %s", t.getClass()));
        } else {
            throw Abstract.requiredTypeError("a type", t);
        }
    }

    // Constructor from Python ----------------------------------------

    /**
     * Create a new instance of Python {@code type}, or of a subclass
     * {@code M} of {@code type}. The actual Python type of the returned
     * object may be a subclass of {@code M}, as required by the bases. In
     * order to create a working type, {@code __new__} must also find or
     * create the Java representation class of instances of the returned
     * Python type.
     * <p>
     * The surface Python syntax <pre>
     * class T(A, B): pass
     * </pre> produces a new instance of {@code type}, while<pre>
     * class T(A, B, metaclass=M): pass
     * </pre> produces a new instance of {@code M}, which must be a Python
     * sub-type of {@code type}. Both result in a call to
     * {@code type.__new__}, differing in the {@code cls} argument.
     * <p>
     * Note the following cases. Suppose {@code T} to be a superclass of
     * {@code M}, and a subclass of {@code type}, that has not overridden
     * {@code __new__}. We may arrive here:
     * <ol>
     * <li>directly as a call to {@code T.__new__(M, name, bases, ns)},</li>
     * <li>directly from a call {@code M(name, bases, ns)}, where {@code M}
     * does not override {@code __call__} or {@code __new__},
     * {@code M.__new__} resolves to this method.</li>
     * <li>indirectly, beginning with either
     * {@code M.__new__(M, name, bases, ns)} or {@code M(name, bases, ns)},
     * but where {@code M} overrides {@code __new__}, and we arrive by one
     * or more calls to {@code super().__new__(M, name, bases, ns)}.</li>
     * </ol>
     * The internal logic of {@code __new__} is subtle enough to do the
     * right thing in these various cases.
     *
     *
     * @param cls requested Python sub-class being created (a metaclass)
     * @param name to be reported by the type object
     * @param bases specified for the type object
     * @param namespace initially specified elements for the namespace of
     *     the class
     * @param kwargs keyword arguments given to {@code __new__}
     * @return a newly-created type.
     * @throws PyBaseException (TypeError) When the type specification
     *     implied by the arguments is not realisable for reasons given in
     *     the exception.
     * @throws Throwable from other errors
     */
    // Compare CPython type_new in typeobject.c
    @Exposed.PythonNewMethod
    public static Object __new__(PyType cls, String name, PyTuple bases, PyDict namespace,
            @KeywordCollector PyDict kwargs) throws PyBaseException, Throwable {
        // FIXME type.__new__ currently ignoring keyword arguments

        logger.atTrace().setMessage("defining class {}{}").addArgument(name).addArgument(bases)
                .log();

        // Make a list of the bases, checking they are acceptable
        List<BaseType> checkedBases =
                updateBases(bases, () -> PyErr.format(PyExc.TypeError, MRO_ENTRIES_NOT_SUPPORTED));

        /*
         * From cls and the bases, choose a common metaclass. This could be a
         * subclass of the requested metaclass cls.
         */
        BaseType metaclass = commonMetaclass(BaseType.cast(cls), checkedBases);

        if (metaclass != cls) {

            if (metaclass.newMethod() != typeType().newMethod()) {
                /*
                 * The metaclass redefined __new__, so delegate to metaclass.__new__.
                 * However, metaclass.__new__ should eventually call type.__new__ with
                 * cls==metaclass, skipping this clause. So although this looks like an
                 * early exit, we shall execute all the code after this, just in another
                 * frame.
                 */
                return metaclass.newMethod().call(metaclass, bases, namespace, kwargs);
            }
        }

        /*
         * We must find or create a class capable of representing instances of
         * the new type. Begin by finding the best Python base for that.
         */
        BaseType bestBase = best_base(checkedBases);
        logger.atTrace().setMessage("new class {} best base is {}").addArgument(name)
                .addArgument(bestBase).log();

        /*
         * When nothing is added, we should find we can use the same
         * representation as bestBase.
         */
        SubclassSpec instanceSpec = new SubclassSpec(name, bestBase.canonicalClass());
        instanceSpec.addConstructors(bestBase);

        // Creating the class gives it to us with access privileges.
        Lookup lu = SUBCLASS_FACTORY.findOrCreateSubclass(instanceSpec);
        logger.atTrace().setMessage("instances of {} are represented by {}").addArgument(name)
                .addArgument(lu).log();

        /*
         * We create the type object required using the TypeFactory, with the
         * new synthetic class as the representation for its instances.
         */
        TypeSpec spec = new TypeSpec(name, lu).bases(checkedBases).metaclass(metaclass)
                .namespace(namespace).add(Feature.REPLACEABLE, Feature.BASETYPE);
        try {
            BaseType type = factory.fromSpec(spec);
            return type;
        } catch (Clash e) {
            // TODO Interpret as a type error?
            throw new InterpreterError(e);
        }
    }

    // Special methods -----------------------------------------------

    /*
     * For technical reasons to do with bootstrapping the type system, the
     * methods and attributes of 'type' that are exposed to Python have to
     * be defined with at least package visibility.
     */

    /** @return {@code repr()} of this Python object. */
    Object __repr__() {
        return String.format("<class '%s'>", getName());
    }

    /**
     * Handle calls to a type object, which will normally be a request to
     * construct a Python object of the type this object describes. For
     * example the call {@code int()} is a request to create a Python
     * {@code int}, although we often think of it as a built-in function.
     * The exception is when {@code this} is {@code type} itself. There must
     * be one or three arguments. The call {@code type(obj)} enquires the
     * Python type of the object, which is even more like a built-in
     * function. The call {@code type(name, bases, dict)} constructs a new
     * type (instance of {@code type}).
     *
     * @param args argument list (length 1 in a type enquiry).
     * @param names of keyword arguments (empty or {@code null} in a type
     *     enquiry).
     * @return new object (or a type if an enquiry).
     * @throws PyBaseException ({@link PyExc#TypeError TypeError}) when
     *     cannot create instances
     * @throws Throwable from implementation slot functions
     */
    Object __call__(Object[] args, String[] names) throws PyBaseException, Throwable {
        try {
            return call(args, names);
        } catch (ArgumentError ae) {
            throw typeError(ae, args, names);
        }
    }

    /**
     * {@link SpecialMethod#op_getattribute} provides attribute read access
     * on this type object and its metatype. This is very like
     * {@code object.__getattribute__}, but the instance is replaced by a
     * type object, and that object's type is a meta-type (which is also a
     * {@code type}).
     * <p>
     * The behavioural difference is that in looking for attributes on a
     * type:
     * <ul>
     * <li>we use {@link #lookup(String)} to search along along the MRO,
     * and</li>
     * <li>if we find a descriptor, we use it.
     * ({@code object.__getattribute__} does not check for descriptors on
     * the instance.)</li>
     * </ul>
     * <p>
     * The following order of precedence applies when looking for the value
     * of an attribute:
     * <ol>
     * <li>a data descriptor from the dictionary of the meta-type</li>
     * <li>a descriptor or value in the dictionary of {@code type}</li>
     * <li>a non-data descriptor or value from dictionary of the meta
     * type</li>
     * </ol>
     *
     * @param name of the attribute
     * @return attribute value
     * @throws PyAttributeError if no such attribute
     * @throws Throwable on other errors, typically from the descriptor
     */
    // Compare CPython type_getattro in typeobject.c
    Object __getattribute__(String name) throws PyAttributeError, Throwable {

        PyType metatype = getType();
        MethodHandle descrGet = null;

        // Look up the name in the type of the type (null if not found).
        Object metaAttr = metatype.lookup(name);
        if (metaAttr != null) {
            // Found in the metatype, it might be a descriptor
            Representation metaAttrRep = Representation.get(metaAttr);
            descrGet = metaAttrRep.op_get();
            if (metaAttrRep.isDataDescr(metaAttr)) {
                // metaAttr is a data descriptor so call its __get__.
                try {
                    // Note the cast of 'this', to match op_get
                    return descrGet.invokeExact(metaAttr, (Object)this, metatype);
                } catch (EmptyException e) {
                    /*
                     * Only __set__ or __delete__ was defined. We do not catch
                     * PyAttributeError: it's definitive. Suppress trying __get__ again.
                     */
                    descrGet = null;
                }
            }
        }

        /*
         * At this stage: metaAttr is the value from the meta-type, or a
         * non-data descriptor, or null if the attribute was not found. It's
         * time to give the type's instance dictionary a chance.
         */
        Object attr = lookup(name);
        if (attr != null) {
            // Found in this type. Try it as a descriptor.
            try {
                /*
                 * Note the args are (null, this): we respect descriptors in this step,
                 * but have not forgotten we are dereferencing a type.
                 */
                return Representation.get(attr).op_get().invokeExact(attr, (Object)null,
                        (PyType)this);
            } catch (EmptyException e) {
                // Do not catch AttributeError: it's definitive.
                // Not a descriptor: the attribute itself.
                return attr;
            }
        }

        /*
         * The name wasn't in the type dictionary. metaAttr is now the result of
         * look-up on the meta-type: a value, a non-data descriptor, or null if
         * the attribute was not found.
         */
        if (descrGet != null) {
            // metaAttr may be a non-data descriptor: call __get__.
            try {
                return descrGet.invokeExact(metaAttr, (Object)this, metatype);
            } catch (EmptyException e) {}
        }

        if (metaAttr != null) {
            /*
             * The attribute obtained from the meta-type, and that turned out not to
             * be a descriptor, is the return value.
             */
            return metaAttr;
        }

        // All the look-ups and descriptors came to nothing :(
        throw typeNoAttributeError(name);
    }

    /**
     * {@link SpecialMethod#op_setattr} provides attribute write access on
     * this type object. The behaviour is very like the default
     * {@code object.__setattr__} except that it manages write access to the
     * type dictionary that is denied through {@link WithDict#getDict()}.
     *
     * @param name of the attribute
     * @param value to give the attribute
     * @throws PyAttributeError if no such attribute or it is read-only
     * @throws Throwable on other errors, typically from the descriptor
     */
    // Compare CPython type_setattro in typeobject.c
    void __setattr__(String name, Object value) throws PyAttributeError, Throwable {

        // Accommodate CPython idiom that set null means delete.
        if (value == null) {
            // Do this to help porting. Really this is an error.
            __delattr__(name);
            return;
        }

        // Trap immutable types
        if (hasFeature(TypeFlag.IMMUTABLE))
            throw cantSetAttributeError(this);

        // Look up the name in the meta-type (null if not found).
        Object metaAttr = getType().lookup(name);
        if (metaAttr != null) {
            // Found in the meta-type, it might be a descriptor.
            Representation metaAttrRep = Representation.get(metaAttr);
            if (metaAttrRep.isDataDescr(metaAttr)) {
                // Try descriptor __set__
                try {
                    metaAttrRep.op_set().invokeExact(metaAttr, (Object)this, value);
                    updateAfterSetAttr(name);
                    return;
                } catch (EmptyException e) {
                    // Do not catch AttributeError: it's definitive.
                    // Descriptor but no __set__: do not fall through.
                    throw readonlyAttributeError(this, name);
                }
            }
        }

        /*
         * There was no data descriptor, so we will place the value in the
         * dictionary of the type directly.
         */
        _dict.put(name, value);
        updateAfterSetAttr(name);
    }

    /**
     * {@link SpecialMethod#op_delattr} provides attribute deletion on this
     * type object. The behaviour is very like the default
     * {@code object.__delattr__} except that it manages write access to the
     * type dictionary that is denied through {@link WithDict#getDict()}.
     *
     * @param name of the attribute
     * @throws PyAttributeError if no such attribute or it is read-only
     * @throws Throwable on other errors, typically from the descriptor
     */
    // Compare CPython type_setattro in typeobject.c
    void __delattr__(String name) throws PyAttributeError, Throwable {

        // Trap immutable types
        if (hasFeature(TypeFlag.IMMUTABLE))
            throw cantSetAttributeError(this);

        // Look up the name in the meta-type (null if not found).
        Object metaAttr = getType().lookup(name);
        if (metaAttr != null) {
            // Found in the meta-type, it might be a descriptor.
            Representation metaAttrRep = Representation.get(metaAttr);
            if (metaAttrRep.isDataDescr(metaAttr)) {
                // Try descriptor __delete__
                try {
                    metaAttrRep.op_delete().invokeExact(metaAttr, (Object)this);
                    updateAfterSetAttr(name);
                    return;
                } catch (EmptyException e) {
                    // Do not catch AttributeError: it's definitive.
                    // Data descriptor but no __delete__.
                    throw mandatoryAttributeError(this, name);
                }
            }
        }

        /*
         * There was no data descriptor, so we will remove the name from the
         * dictionary of the type directly.
         */
        Object previous = _dict.remove(name);
        if (previous == null) {
            // A null return implies it didn't exist
            throw typeNoAttributeError(name);
        }
        updateAfterSetAttr(name);
        return;
    }

    /**
     * Calculate a new MRO for this type by the default algorithm. This
     * method is exposed as the method {@code mro} of type {@code objects}
     * and may be overridden in a Python subclass of {@code type} (a
     * "metatype") to customise the MRO in the types it creates.
     *
     * @return a new MRO for the type
     */
    // Compare CPython type_mro_impl in typeobject.c
    @Exposed.PythonMethod
    protected PyList mro() {
        // Note: this is only the default behaviour (type)
        PyType[] newMRO = MROCalculator.getMRO(this, this.bases);
        return new PyList(newMRO);
    }

    // FastCall implementation ---------------------------------------

    /*
     * These methods may be called instead of __call__ to take advantage of
     * potentially efficient handling of arguments in a type's __new__
     * method. There is a short-cut in Callables.call that detects the
     * FastCall interface. In a CallSite, the MethodHandle could be formed
     * with this in mind.
     */

    @Override
    public Object call(Object[] args, String[] names) throws ArgumentError, Throwable {

        assert (args != null);
        int nk = names == null ? 0 : names.length;
        int np = args.length - nk;

        /*
         * Special case: type(x) should return the Python type of x, but only if
         * this is exactly the type 'type'.
         */
        if (this == typeType()) {
            // Deal with two special cases
            if (np == 1 && nk == 0) {
                // Call is exactly type(x) so this is a type enquiry
                return PyType.of(args[0]);

            } else if (np != 3) {
                // Call ought to be type(x, bases, dict [, **kwds])
                // __new__ will check too but we prefer this message.
                throw PyErr.format(PyExc.TypeError, "type() takes 1 or 3 arguments");
            }
        }

        // Call __new__ of the type described by this type object
        FastCall _new = newMethod();
        Object obj = _PyUtil.callPrepend(_new, this, args, names);

        // Call obj.__init__ if it is defined and type(obj) == this
        MethodHandle init = maybeInitHandle(obj);
        if (init != null) {
            try {
                init.invoke(obj, args, names);
            } catch (EmptyException ee) {}
        }
        return obj;
    }

    @Override
    public Object call(Object x) throws Throwable {
        if (this == PyType.TYPE()) {
            // Call is exactly type(x) so this is a type enquiry
            return PyType.of(x);
        }
        // Call __new__ of the type described by this type object
        FastCall _new = newMethod();
        Object obj = _new.call(this, x);

        // Call obj.__init__ if it is defined and type(obj) == this
        MethodHandle init = maybeInitHandle(obj);
        if (init != null) {
            Object[] args = {x};
            String[] names = null;
            try {
                init.invoke(obj, args, names);
            } catch (EmptyException ee) {}
        }
        return obj;
    }

    @Override
    public Object call(Object a0, Object a1, Object a2) throws Throwable {
        // Note that this cannot be a type enquiry

        // Call __new__ of the type described by this type object
        FastCall _new = newMethod();
        Object obj = _new.call(this, a0, a1, a2);

        // Call obj.__init__ if it is defined and type(obj) == this
        MethodHandle init = maybeInitHandle(obj);
        if (init != null) {
            Object[] args = {a0, a1, a2};
            String[] names = null;
            try {
                init.invoke(obj, args, names);
            } catch (EmptyException ee) {}
        }

        return obj;
    }

    /**
     * Return a handle on the {@code __init__} method, if we should try to
     * call it, or {@code null} if we shouldn't. We should call
     * {@code obj.__init__} after {@code __new__} if it is defined and if
     * {@code obj} is an instance of this type (or of a sub-type). It is not
     * an error for {@code __new__} not to return an instance of this type
     * or for {@code obj.__init__} not to be defined.
     *
     * @param obj returned from {@code __new__}
     * @return handle on {@code __init__} or {@code null}
     */
    private MethodHandle maybeInitHandle(Object obj) {
        assert obj != null;
        Representation rep = Representation.get(obj);
        BaseType objType = rep.pythonType(obj);
        // Does obj define __init__?
        if (objType.hasFeature(KernelTypeFlag.HAS_INIT)) {
            // Did __new__ return an object of the required type?
            if (objType.isSubTypeOf(this)) { return rep.op_init(); }
        }
        return null;
    }

    @Override
    public PyBaseException typeError(ArgumentError ae, Object[] args, String[] names) {
        // Note ae is one off here since call prepends this to args.
        int n = args.length;
        if (names != null) { n -= names.length; }
        return PyErr.format(PyExc.TypeError, "%s() %s (%d given)", getName(), ae.dropPrefix(), n);
    }

    // Support for Type Initialisation -------------------------------

    /*
     * For technical reasons to do with bootstrapping the type system, the
     * methods and attributes of 'type' that are exposed to Python have to
     * be defined with at least package visibility.
     */

    /**
     * Inherit feature flags (including kernel feature flags) from the base.
     * This should be called early enough in initialising the type that it
     * can be modified by configuration with exposed methods.
     */
    void inheritFeatures() {
        if (base != null) {
            // Inherit the customary features
            for (TypeFlag f : TypeFlag.HERITABLE) { if (base.hasFeature(f)) { features.add(f); } }
            // Inherit the customary kernel features
            for (KernelTypeFlag f : KernelTypeFlag.HERITABLE) {
                if (base.hasFeature(f)) { kernelFeatures.add(f); }
            }
        }
    }

    /**
     * Add feature flags from the specification. This should be called early
     * enough in initialising the type that it can influence configuration
     * with exposed methods.
     *
     * @param spec specification of this type
     */
    void addFeatures(TypeSpec spec) {
        for (Feature f : spec.getFeatures()) { features.add(f.flag); }
    }

    /**
     * Add feature flags derived from observations of the type itself (and
     * the specification if necessary). This should be called at the end of
     * initialising the type so that it is influenced by the set of exposed
     * methods.
     *
     * @param spec specification of this type
     */
    // Compare CPython inherit_special in typeobject.c
    void deriveFeatures(TypeSpec spec) {
        /*
         * Set at most one of the fast sub-type test flags. (Use short circuit
         * evaluation to stop at the first flag set.) We cannot rely on
         * this==PyWhatever.TYPE when constructing a type for 'whatever', as the
         * TYPE won't be assigned yet.
         */
        //
        @SuppressWarnings("unused")
        boolean unused = setFeature(PyLong.class, TypeFlag.INT_SUBCLASS)
                || setFeature(PyTuple.class, TypeFlag.TUPLE_SUBCLASS)
                || setFeature(PyUnicode.class, TypeFlag.STR_SUBCLASS)
                || setFeature(PyDict.class, TypeFlag.DICT_SUBCLASS)
                || setFeature(PyBaseException.class, TypeFlag.EXCEPTION_SUBCLASS);

        /*
         * Certain built-ins have this feature, which relates to pattern
         * matching. It would be nice to accomplish this in the TypeSpec, but it
         * is not public API.
         */
        unused = setFeature(PyLong.class, KernelTypeFlag.MATCH_SELF)
                || setFeature(PyFloat.class, KernelTypeFlag.MATCH_SELF)
                || setFeature(PyBytes.class, KernelTypeFlag.MATCH_SELF)
                || setFeature(PyUnicode.class, KernelTypeFlag.MATCH_SELF)
                || setFeature(PyList.class, KernelTypeFlag.MATCH_SELF)
                || setFeature(PyDict.class, KernelTypeFlag.MATCH_SELF);
    }

    /**
     * Maybe set the given feature flag. The flag is set if and only if the
     * implementation class of this type matches that given, or the same
     * flag is set in the base. This way, there is a first qualifying type
     * {@code k}, then every type that descends from it inherits the flag.
     *
     * @param k identifying the first qualifying type
     * @param f flag marking a sub-type
     * @return {@code true} iff the flag was set in this call
     */
    private final boolean setFeature(Class<?> k, TypeFlag f) {
        if (base != null) {
            if (javaClass == k || base.hasFeature(f)) { features.add(f); return true; }
        }
        return false;
    }

    /**
     * Maybe set the given kernel feature flag. The flag is set if and only
     * if the implementation class of this type matches that given, or the
     * same flag is set in the base. This way, there is a first qualifying
     * type {@code k}, then every type that descends from it inherits the
     * flag.
     *
     * @param k identifying the first qualifying type
     * @param f flag marking a sub-type
     * @return {@code true} iff the flag was set in this call
     */
    private final boolean setFeature(Class<?> k, KernelTypeFlag f) {
        if (base != null) {
            if (javaClass == k || base.hasFeature(f)) { kernelFeatures.add(f); return true; }
        }
        return false;
    }

    /**
     * Load the dictionary of this type with attributes discovered by an
     * exposer. This is a package-visible hook with direct access to the
     * dictionary, for {@link TypeFactory} to use during type construction.
     *
     * @param exposer from which to populate the dictionary
     * @param spec supplying the type's lookup object
     */
    void populateDict(TypeExposer exposer, TypeSpec spec) {

        // Add contents of the exposer to the dictionary
        Lookup lookup = spec.getLookup();
        for (TypeExposer.Entry e : exposer.entries(lookup)) {
            _dict.put(e.name(), e.value());
            updateAfterSetAttr(e.name());
        }

        // Fill the cache for each special method (not just defined).
        for (SpecialMethod sm : SpecialMethod.values()) {
            LookupResult result = lookup(sm.methodName, null);
            updateSpecialMethodCache(sm, result);
        }
    }

    /**
     * Discover the Java constructors of representations of this type and
     * create method handles on them. This is what allows {@code __new__} in
     * a Python superclass of this type to create an instance, even though
     * it has no knowledge of the type at compile time.
     *
     * @param spec specification of this type
     */
    // FIXME Constructor lookup: Shared and Adoptive types.
    // This only looks right for a SimpleType extending Representation
    void fillConstructorLookup(TypeSpec spec) {
        // Collect constructors in this table
        Map<MethodType, ConstructorAndHandle> table = new HashMap<>();
        // We allow public construction only of the canonical base.
        // (This is right for use by __new__ ... and generally?)
        Class<?> baseClass = spec.getCanonicalClass();
        Lookup lookup = spec.getLookup();
        final int accept = Modifier.PUBLIC | Modifier.PROTECTED;
        for (Constructor<?> cons : baseClass.getDeclaredConstructors()) {
            int modifiers = cons.getModifiers();
            try {
                if ((modifiers & accept) != 0) {
                    MethodHandle mh = lookup.unreflectConstructor(cons);
                    // Neutralise the actual return type to Object
                    MethodType mt = mh.type();
                    mt = mt.changeReturnType(Object.class);
                    mh = mh.asType(mt);
                    table.put(mt, new ConstructorAndHandle(cons, mh));
                }
            } catch (IllegalAccessException e) {
                throw new InterpreterError(e, "Failed to get handle for constructor %s", cons);
            }
        }
        // Must be some constructors if we hope to sub-class
        if (table.isEmpty() && this.hasFeature(TypeFlag.BASETYPE)) {
            logger.atWarn().setMessage("%s (%s) is base type but defines no public constructor")
                    .addArgument(this).addArgument(javaClass());
        }
        // Freeze the table as the constructor lookup
        setConstructorLookup(table);
    }

    /**
     * Called from {@code type.__setattr__} and
     * {@code type.__delattr__(String)} after an attribute has been set or
     * deleted. This gives the type the opportunity to recompute caches and
     * perform any other actions needed.
     *
     * @param name of the attribute modified
     */
    private /* ? */ void updateAfterSetAttr(String name) {

        // FIXME Notify sub-classes and other watchers.
        // Think about synchronisation: must take a lock on lookup.

        /*
         * We look up the current definition of name for this type, which has
         * recently changed. Note that even when removing the name from the
         * dictionary of this type, the effect may be to uncover a definition
         * that already exists somewhere along the MRO, and so it becomes a
         * change.
         */
        LookupResult result = lookup(name, null);
        SpecialMethod sm = SpecialMethod.forMethodName(name);

        if (sm != null) {
            // Update affects a special method.
            updateSpecialMethodCache(sm, result);
            // Some special methods need:
            KernelTypeFlag feature = switch (sm) {
                case op_getitem -> KernelTypeFlag.HAS_GETITEM;
                case op_setitem -> KernelTypeFlag.HAS_SETITEM;
                case op_delitem -> KernelTypeFlag.HAS_DELITEM;
                case op_iter -> KernelTypeFlag.HAS_ITER;
                case op_next -> KernelTypeFlag.HAS_NEXT;
                case op_index -> KernelTypeFlag.HAS_INDEX;
                case op_get -> KernelTypeFlag.HAS_GET;
                case op_set -> KernelTypeFlag.HAS_SET;
                case op_delete -> KernelTypeFlag.HAS_DELETE;
                case op_init -> KernelTypeFlag.HAS_INIT;
                default -> null;
            };
            // If sm corresponds to a feature flag in that case
            if (feature != null) {
                if (result != null) {
                    // We are defining or changing sm
                    kernelFeatures.add(feature);
                } else {
                    // We are deleting sm
                    kernelFeatures.remove(feature);
                }
            }

            // Special handling of name == __getattribute__
            if (sm == SpecialMethod.op_getattribute) {
                // We are setting __getattribute__ ...
                feature = KernelTypeFlag.OBJECT_GETATTRIBUTE;
                if (result.obj instanceof PyWrapperDescr wd && wd.__objclass__() == objectType()) {
                    // ... to object.__getattribute__
                    kernelFeatures.add(feature);
                } else {
                    // ... to something else
                    kernelFeatures.remove(feature);
                }
            }

        } else if ("__new__".equals(name)) {
            // Update affects __new__.
            updateNewMethodCache();
        }
    }

    /**
     * Update the cache for each representation of this type, and certain
     * feature values, by looking up the definition along the MRO.
     *
     * @param sm the special method
     * @param result of looking up the name, may be ({@code null}
     */
    private void updateSpecialMethodCache(SpecialMethod sm, LookupResult result) {

        if (sm.cache == null) {
            // There is no cache for this special method. Ignore.
            return;

        } else if (result == null) {
            /*
             * The special method is not defined for this type. Install a handle
             * that throws EmptyException. (Unlike in CPython, null won't do.)
             */
            for (Representation rep : representations()) { sm.setEmpty(rep); }
        } else if (result.status == LookupStatus.ONCE) {
            /*
             * We can't cache the result. Use a generic slot wrapper so we look it
             * up on the type object every time.
             */
            for (Representation rep : representations()) { sm.setGeneric(rep); }
        } else if (result.obj instanceof MethodDescriptor descr) {
            /*
             * A method descriptor can give us a direct handle to the implementation
             * for a given self class.
             */
            updateSpecialMethodCache(sm, result.where, descr);
        } else {
            /*
             * It is a method defined in Python or some other object or descriptor.
             * Use a generic slot wrapper to look it up (and bind it) each time.
             */
            for (Representation rep : representations()) { sm.setGeneric(rep); }
        }
    }

    /**
     * Update the cache for each representation of this type, from a method
     * descriptor.
     *
     * @param sm the special method
     * @param where the descriptor was found along the MRO
     * @param descr the descriptor defining the special method
     */
    private void updateSpecialMethodCache(SpecialMethod sm, BaseType where,
            MethodDescriptor descr) {
        if (where == this) {
            /*
             * We found the definition locally. Method descriptors created for this
             * type explicitly support all its representations.
             */
            for (Representation rep : representations()) {
                int index = rep.getIndex();
                sm.setCache(rep, descr.getHandle(index));
            }
        } else {
            /*
             * The method descriptor is in a super class. Every representation class
             * of this type should be assignment-compatible with one of the
             * self-classes of the Python type where we found the descriptor.
             */
            List<Class<?>> classes = where.selfClasses();
            for (Representation rep : representations()) {
                Class<?> c = rep.javaClass();
                int index = where.getSubclassIndex(c);
                assert index < classes.size();
                sm.setCache(rep, descr.getHandle(index));
            }
        }
    }

    /**
     * Write the resolved value of __new__ into a cache.
     */
    private void updateNewMethodCache() {
        LookupResult result = lookup("__new__", null);
        if (result.where != this) {
            // Avoid making a new object so == equality is valid.
            newMethodCache = result.where.newMethod();
        } else {
            Object obj = result.obj;
            if (obj instanceof FastCall fast) {
                newMethodCache = fast;
            } else {
                newMethodCache = new FastCall.Slow(obj);
            }
        }
    }

    /**
     * Various consistency checks on a completed type object.
     *
     * @param spec that led to this type object
     */
    // TODO Provide for Python exceptions when type system not ready
    void checkConsistency(TypeSpec spec) {
        if (hasFeature(TypeFlag.SEQUENCE_PROTOCOL) && !hasFeature(KernelTypeFlag.HAS_GETITEM)) {
            throw PyErr.format(PyExc.TypeError, SEQUENCE_WITHOUT_GETITEM, spec.getName(),
                    Feature.SEQUENCE_PROTOCOL);
        }
    }

    // plumbing ------------------------------------------------------

    private static final String MRO_ENTRIES_NOT_SUPPORTED =
            "type() doesn't support MRO entry resolution; " + "use types.new_class()";
    private static final String METACLASS_CONFLICT =
            "metaclass conflict: " + "the metaclass of a derived class "
                    + "must be a (non-strict) subclass " + "of the metaclasses of all its bases";
    private static final String SEQUENCE_WITHOUT_GETITEM =
            "type '%s' declared to have %s did not define __getitem__";

    /**
     * Determine if this type is a Python sub-type of {@code b} by chaining
     * through the {@link #base} property. (This is a fall-back when
     * {@link #mro} is not valid.)
     *
     * @param b to test
     * @return {@code true} if {@code this} is a sub-type of {@code b}
     */
    // Compare CPython type_is_subtype_base_chain in typeobject.c
    private boolean type_is_subtype_base_chain(PyType b) {
        // Useful to know when we resort to this
        logger.atTrace().setMessage("ask {} is sub-type of {} from base chain")
                .addArgument(() -> getName()).addArgument(() -> b.getName()).log();
        PyType t = this;
        while (t != b) { t = t.getBase(); if (t == null) { return b == PyObject.TYPE; } }
        return true;
    }

    /**
     * Invoke a custom {@code mro()}, if one has been defined for this type,
     * or do the equivalent of {@link #mro()} if not.
     * <p>
     * The method is safe to use during type system initialisation where
     * {@link #getType()}{@code ==type}, that is, this is not a Python
     * metaclass.
     *
     * @return a valid MRO for the type
     * @throws Throwable on Python errors
     */
    // Compare CPython mro_invoke in typeobject.c
    // Unlike CPython, we return an array, avoiding PyTuple.
    private BaseType[] mro_invoke() throws Throwable {
        logger.atTrace().setMessage("calculating __mro__ of {}").addArgument(name).log();
        BaseType[] newMRO = null;
        SimpleType typeType = typeType();
        // FIXME getType() of concrete PyTypes should be concrete
        // This will affect the synthesis of sub-type representations
        BaseType metatype = BaseType.cast(getType());

        if (metatype != typeType) {
            LookupResult lur = metatype.lookup("mro", null);
            if (lur != null && lur.where != typeType) {
                /*
                 * This is a metatype that redefines mro(). We call its specific mro()
                 * as a Python method.
                 */
                logger.atDebug().setMessage("calling {}").addArgument(lur.obj).log();
                Object mro_result = Callables.call(lur.obj, this);
                // It returns some kind of iterable.
                List<Object> result =
                        PySequence.fastList(mro_result, () -> PyErr.format(PyExc.TypeError,
                                "mro() should return an iterable", PyType.of(mro_result)));
                newMRO = checkedMRO(result);
            }
        }

        if (newMRO == null) {
            /*
             * This type is exactly 'type' (or has somehow undefined mro()). We
             * shall do the equivalent of type.mro() but without building a PyList.
             */
            newMRO = MROCalculator.getMRO(this, this.bases);
        }

        return newMRO;
    }

    /**
     * Build an array of bases to represent the MRO of this type, from the
     * list returned by a custom {@code mro()}, after careful type and
     * consistency checks.
     *
     * @param mro result of custom {@code mro()} call as list
     * @return validated array of bases
     */
    // Compare CPython mro_check in typeobject.c
    // Unlike CPython, we build an array here, avoiding PyTuple.
    private BaseType[] checkedMRO(List<Object> mro) {

        int n = mro.size(), index = 0;

        if (n == 0) { throw PyErr.format(PyExc.TypeError, "type MRO must not be empty"); }

        BaseType[] new_mro = new BaseType[n];
        BaseType solid = solid_base();

        for (Object obj : mro) {
            if (obj instanceof BaseType base) {
                if (solid.isSubTypeOf(base.solid_base())) {
                    // Admissible base: all other paths throw.
                    new_mro[index++] = base;
                } else {
                    throw PyErr.format(PyExc.TypeError,
                            "mro() returned base with unsuitable layout ('%.500s')",
                            base.getName());
                }
            } else if (obj instanceof PyType) {
                // Extra help for non-BaseType PyType
                throw PyErr.format(PyExc.TypeError,
                        "mro() returned a type not created by the runtime ('%.500s')",
                        obj.getClass().getTypeName());
            } else {
                // obj isn't even pretending to be a Python type.
                throw PyErr.format(PyExc.TypeError, "mro() returned a non-class ('%.500s')",
                        PyType.of(obj).getName());
            }
        }

        return new_mro;
    }

    /**
     * Find the "solid base" of this type. The "solid base" of a type T is a
     * type S that is the most distant ancestor of T along the chain of
     * bases of T, including T itself, that has the same primary
     * representation as T.
     * <p>
     * Put another way, S is the least-derived Python class that has the
     * same (primary) representation class as T. CPython refers here to the
     * "layout" of the type, meaning the memory layout of instances, but for
     * us it is the Java class (or base of several classes) that matters.
     * <p>
     * Having the same representation means that methods defined in Java for
     * S can be applied to instances (in Python) of T, because the Java
     * class of every instance of T is a subclass in Java of the (primary)
     * class of S. This property plays a role in determining what makes an
     * acceptable set of bases for a Python type.
     *
     * @return the "solid base" of this type
     */
    // Compare CPython solid_base() in typeobject.c
    private BaseType solid_base() {
        // Walk the base chain and return just before the class change.
        BaseType type = this, base;
        Class<?> c = javaClass();
        while ((base = type.base) != null) {
            // type has a base (=> type is not object).
            if (c != (c = base.javaClass())) { break; }
            // The base has the same representation
            type = base;
        }
        return type;
    }

    /**
     * Calculate the "best base" amongst multiple base classes. A type that
     * has the given types as bases will have as its "best base" the first
     * base (in the list) with a representation in Java that is a sub-class
     * in Java of the representations of all the bases.
     * <p>
     * When defining a new type with these bases, the appropriate
     * representation Java class may be that of the "best base", or it may
     * be one that extends that representation, e.g to accommodate new
     * members ({@code __slots__} or an instance dictionary). It follows,
     * however, that the representation of the new type will be a Java
     * subclass of the representations of all the bases, and therefore their
     * operations apply to the new type.
     * <p>
     * If no such candidate is found amongst the bases, the representations
     * are said to conflict, and the type cannot exist in this
     * implementation. (CPython calls this a "layout conflict". We think the
     * logic here is no more restrictive than in CPython.)
     *
     * @param bases of a potential new type
     * @return the best amongst them as a base for representations
     */
    // Compare CPython best_base in typeobject.c
    private static BaseType best_base(List<BaseType> bases) {
        BaseType base = objectType();

        /*
         * In the search, winner is always the Java representation class of
         * base.
         */
        Class<?> winner = base.javaClass();

        /*
         * Scan the list and maintain in base the type whose Java representation
         * is layout compatible with all the bases so far.
         */
        for (BaseType b : bases) {
            assert b.isReady();

            /*
             * CPython checks here for b has feature BASETYPE. We did that already
             * in updateBases.
             */

            /*
             * CPython refers here to the solid base of b which is the most senior
             * ancestor of b having the same memory layout. For us, memory layout
             * means Java class.
             */
            Class<?> candidate = b.javaClass();

            if (candidate.isAssignableFrom(winner)) {
                // Don't change the winner: it is less derived
            } else if (winner.isAssignableFrom(candidate)) {
                // candidate is more derived so it is the new winner
                winner = candidate;
                base = b;
            } else {
                throw PyErr.format(PyExc.TypeError,
                        "bases (%s, %s) have conflicting representations", b, base);
            }
        }

        // base will be object type if bases was empty
        return base;
    }

    /**
     * Determine the proper metaclass to construct a new type, given that
     * the metaclass and bases originally specified, in an expression like
     * {@code class C(bases, metaclass=m):pass} or as the function called as
     * {@code m(name, bases, {})}. The proper metaclass is a Python
     * sub-class of the given metatype and the types of all the bases.
     * <p>
     * At the same time, we check for metaclass conflicts and bases that are
     * not types at all.
     *
     * @param metaclass specified originally
     * @param bases of the new type
     * @return proper metaclass to create new type (not {@code null})
     */
    // Compare CPython _PyType_CalculateMetaclass in typeobject.c
    // Private for now but used from builtin.__build_class__
    private static BaseType commonMetaclass(BaseType metaclass, List<BaseType> bases) {
        for (BaseType base : bases) {
            // FIXME: specialise getType to avoid the cast
            // This means reworking type attribute in SubclassFactory
            BaseType candidate = (BaseType)base.getType();
            if (metaclass.isSubTypeOf(candidate)) {
                // Don't change the winning metaclass
            } else if (candidate.isSubTypeOf(metaclass)) {
                // candidate is the new winner
                metaclass = candidate;
            } else {
                throw PyErr.format(PyExc.TypeError, METACLASS_CONFLICT);
            }
        }
        return metaclass;
    }

    /**
     * Make a list of the bases from the {@code args}, checking that each is
     * acceptable as a base (is a {@link BaseType} and allows itself to be a
     * base in Python. This method may be used in {@code type.__new__} and
     * also {@code builtins.__build_class__}.
     * <p>
     * In the latter context, objects are allowed that resolve to a tuple of
     * bases using the {@code __mro_entries__} protocol, and this method
     * makes that expansion. In the former context, {@code type.__new__}
     * forbids this by providing a non null {@code exceptionSupplier}.
     *
     * @param spec to accumulate the bases.
     * @param args purported bases.
     * @param exceptionSupplier if not {@code null}, {@code __mro_entries__}
     *     protocol is forbidden.
     * @throws PyBaseException (TypeError) when some object in {@code args}
     *     cannot be a base.
     * @throws Throwable from other errors in the implementation of the
     *     tuple etc..
     */
    // Compare CPython update_bases in bltinmodule.c
    // Compare CPython type_new_get_bases in typeobject.c
    private static List<BaseType> updateBases(PyTuple args,
            Supplier<PyBaseException> exceptionSupplier) throws PyBaseException, Throwable {

        // Almost always the same size as args.
        List<BaseType> bases = new ArrayList<>(args.size());

        for (Object arg : args) {

            if (arg instanceof BaseType base) {
                addChecked(bases, base);

            } else {
                // Non-type object: does it implement __mro_entries__?
                Object meth = null, entries = null;
                try {
                    meth = Abstract.lookupAttr(arg, "__mro_entries__");
                } catch (Throwable e) {
                    // Treat as not found.
                }

                if (meth != null) {
                    /*
                     * We are expected to call __mro_entries__, and replace arg with the
                     * entries it provides. But first, is that allowed here?
                     */
                    if (exceptionSupplier != null) {
                        // No: throw as specified by the caller.
                        throw exceptionSupplier.get();
                    } else {
                        /*
                         * __mro_entries__ expects the original args as its argument and returns
                         * a tuple of types
                         */
                        entries = Callables.call(meth, args);

                        // And should return a tuple of types.
                        if (entries instanceof PyTuple t) {
                            for (Object o : t) { addChecked(bases, o); }
                        } else {
                            throw PyErr.format(PyExc.TypeError,
                                    "__mro_entries__ must return a tuple");
                        }
                    }
                }
            }
        }
        return bases;
    }

    private static void addChecked(List<BaseType> bases, Object o) {
        if (o instanceof PyType base) {
            // Diagnose foreign PyType implementations here
            addChecked(bases, BaseType.cast(base));
        } else {
            throw PyErr.format(PyExc.TypeError, "bases must be types");
        }
    }

    private static void addChecked(List<BaseType> bases, BaseType base) {
        if (base.hasFeature(TypeFlag.BASETYPE)) {
            bases.add(base);
        } else {
            throw PyErr.format(PyExc.TypeError, "type '%.100s' is not an acceptable base type",
                    base);
        }
    }

    /**
     * Throw a {@link PyExc#TypeError TypeError} if setting the named
     * attribute is not permitted (due to the immutability of this type).
     *
     * @param value to set or null for delete
     * @param name of attribute being set (for error message)
     */
    // Compare CPython check_set_special_type_attr in typeobject.c
    private void checkTypeMutable(Object value, String name) {
        if (hasFeature(TypeFlag.IMMUTABLE)) {
            String action = value == null ? "delete" : "set";
            throw PyErr.format(PyExc.TypeError, "cannot %s '%s' attribute of immutable type '%s'",
                    action, name, getName());
        }
        if (value == null) {
            throw PyErr.format(PyExc.TypeError, "cannot delete '%s' attribute of type '%s'", name,
                    getName());
        }
    }

    /**
     * Create a {@link PyAttributeError} with a message along the lines
     * "type object 'T' has no attribute N", where T is the type name and N
     * is the attribute name.
     *
     * @param name of attribute
     * @return exception to throw
     */
    private PyAttributeError typeNoAttributeError(Object name) {
        String fmt = "type object '%.50s' has no attribute '%.50s'";
        return (PyAttributeError)PyErr.format(PyExc.AttributeError, fmt, getName(), name);
    }
}
