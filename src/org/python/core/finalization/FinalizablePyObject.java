package org.python.core.finalization;

/**
 * This interface allows {@code PyObject}s to have finalizers. Alternatively one can use
 * {@link org.python.core.finalization.FinalizableBuiltin}.
 * <p>
 * The difference is that {@link #__del__()} can be overridden by a new-style subclass's
 * {@code __del__}-method on Python-side, while {@link FinalizableBuiltin#__del_builtin__()} is
 * always called. If a Python-side finalizer exists, {@link FinalizableBuiltin#__del_builtin__()}
 * will be called after the Python-side finalizer has been processed.
 * <p>
 * One can even implement both interfaces. If both interfaces are implemented, the
 * {@link FinalizeTrigger} will call {@link #__del__()} first and then
 * {@link FinalizableBuiltin#__del_builtin__()}. If a new-style subclass has an own, Python-side
 * {@code __del__}-method, this overrides the Java-implemented {@link #__del__()}, but not
 * {@link FinalizableBuiltin#__del_builtin__()}, which will be called after the Python-side
 * finalizer.
 * <p>
 * If you are writing a custom built-in that shall directly extend {@link org.python.core.PyObject}
 * or some other not-yet-finalizable builtin and have a finalizer, follow the instructions below.
 * <p>
 * <ol>
 * <li>Let your subclass implement {@link FinalizablePyObject} (or {@link FinalizableBuiltin}).</li>
 * <li>In every constructor call<br>
 * {@code FinalizeTrigger.ensureFinalizer(this);}<br>
 * </li>
 * <li>Write your {@link #__del__()}-method however you intend it. (or
 * {@link FinalizableBuiltin#__del_builtin__()} if {@link FinalizableBuiltin} was used)</li>
 * <li>(optional)<br>
 * If your finalizer resurrects the object (Python allows this) and you wish the finalizer to run
 * again on next collection of the object:<br>
 * In the block where the resurrection occurs, let your {@link #__del__()}- or
 * {@link FinalizableBuiltin#__del_builtin__()}-method call<br>
 * {@code FinalizeTrigger.ensureFinalizer(this);}. If you implement {@code __del__} in Python and
 * need this functionality, you can simply call {@code someObject.__ensure_finalizer__()}<br>
 * Note that this is Jython-specific and should be surrounded by a {@code try/except}-block to
 * ensure compatibility with other Python implementations.</li>
 * </ol>
 * <p>
 * Note: Regarding to object resurrection, Jython currently behaves like CPython &ge; 3.4. That means
 * the finalizer {@link #__del__()} or {@link FinalizableBuiltin#__del_builtin__()} is called only
 * the first time an object gets gc'ed. If pre-3.4.-behavior is required for some reason (i.e. have
 * the finalizer called repeatedly on every collection after a resurrection), one can achieve this
 * manually via step 5).
 * <p>
 * The built-in function {@code __ensure_finalizer__} is also useful if a class acquires a finalizer
 * after instances have already been created. Usually only those instances that were created after
 * their class acquired the finalizer will actually be finalized (in contrast to CPython). However,
 * one can manually tell earlier created instances to become finalizable by calling
 * {@code __ensure_finalizer__()} on them. As mentioned above, it is recommended to surround this
 * with a {@code try/except}-block to ensure compatibility with other Python implementations.
 * <p>
 * Note that it is not possible to override {@code __ensure_finalizer__} on Python side. If one
 * overrides {@code __ensure_finalizer__} on Python side, Jython will ignore the
 * override-implementation and still call the original one.
 * <p>
 * It is possible to switch finalization on and off at any desired time for a certain object. This
 * can be helpful if it is only necessary to have {@link #__del__()} or
 * {@link FinalizableBuiltin#__del_builtin__()} called for certain configurations of an object.
 * <p>
 * To turn off the finalizer, call: <pre>{@literal
 * ((FinalizeTrigger) JyAttribute.getAttr(this, JyAttribute.FINALIZE_TRIGGER_ATTR)).clear();
 * }</pre> To turn it on again, call <pre>{@literal
 * ((FinalizeTrigger) JyAttribute.getAttr(this, JyAttribute.FINALIZE_TRIGGER_ATTR)).trigger(this);
 * }</pre>
 *
 * @see org.python.core.JyAttribute#FINALIZE_TRIGGER_ATTR
 * @see FinalizableBuiltin#__del_builtin__()
 */

public interface FinalizablePyObject {
    public void __del__();
}
