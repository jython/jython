package org.python.core.finalization;

/**
 * <p>
 * This interface allows {@code PyObject}s to have finalizers.
 * Alternatively one can use
 * {@link org.python.core.finalization.FinalizableBuiltin}.
 * </p>
 * <p>
 * The difference is that {@code __del__} can be overwritten by a
 * new-style subclass's {@code __del__}-method on Python-side, while
 * {@code __del__Builtin} is always called. If a Python-side
 * finalizer exists, {@code __del__Builtin} will be called after the
 * Python-side finalizer has been processed.
 * </p>
 * <p>
 * One can even implement both interfaces.
 * If both interfaces are implemented, the {@code FinalizeTrigger} will
 * call {@code __del__} first and then {@code __del__Builtin}. If a
 * new-style subclass has an own, Python-side {@code __del__}-method, this
 * overwrites the Java-implemented {@code __del__}, but not
 * {@code __del__Builtin}, which will be called after the Python-side
 * finalizer.
 * </p>
 * <p>
 * If you are writing a custom built-in that shall directly
 * extend {@link org.python.core.PyObject} and have a finalizer, you can simply
 * extend {@link org.python.core.finalization.PyFinalizableObject}
 * and overwrite its {@code __del__}-method.
 * Follow the instructions below, starting at 4).
 * </p>
 * <p>
 * If you want to extend some subclass of PyObject that does not yet implement
 * this interface, you have to take care of the following steps:
 * <ol>
 * <li>
 *     Let your subclass implement {@code FinalizablePyObject}
 *     (or {@link org.python.core.finalization.FinalizableBuiltin}).
 * </li>
 * <li>
 *     Let it have a member<br>
 *     {@code public FinalizeTrigger finalizeTrigger;}<br>
 *     Other scopes also work, but might fail with security managers.
 * </li>
 * <li>
 *    In every constructor initialize this member via<br>
 *    {@code finalizeTrigger = FinalizeTrigger.makeTrigger(this);}<br>
 *    or<br>
 *    {@code FinalizeTrigger.ensureFinalizer(this);}<br>
 *    The latter is a better abstraction, but slightly less performant,
 *    since it uses reflection.
 * </li>
 * <li>
 *    Write your {@code __del__}-method however you intend it.
 *    (or {@code __del__Builtin} if
 *    {@link org.python.core.finalization.FinalizableBuiltin} was used)
 * </li>
 * <li>
 *    (optional)<br>
 *    If your finalizer resurrects the object (Python allows this) and you wish the
 *    finalizer to run again on next collection of the object:</br>
 *    In the block where the resurrection occurs, let your {@code __del__}- or
 *    {@code __del__Builtin}-method call<br>
 *    {@code FinalizeTrigger.ensureFinalizer(this);}.
 * </li>
 * </ol>
 * </p>
 * <p>
 * Note: Regarding to object resurrection, Jython currently behaves like CPython >= 3.4.
 * That means the finalizer {@code __del__} or {@code __del__Builtin} is called only the
 * first time an object gets gc'ed. If pre 3.4. behavior is required for some reason (i.e.
 * have the finalizer called repeatedly on every collection after a resurrection), one can
 * achieve this manually via step 5).
 * </p>
 * <p>
 * It is possible to switch finalization on and off at any desired time for a certain object.
 * This can be helpful if it is only necessary to have {@code __del__} or
 * {@code __del__Builtin} called for certain configurations of an object.
 * </p>
 * <p>
 * To turn off the finalizer, call</br>
 * {@code finalizeTrigger.clear();}</br>
 * To turn it on again, call</br>
 * {@code finalizeTrigger.trigger(this);}
 * </p>
 */

public interface FinalizablePyObject extends HasFinalizeTrigger {
    public void __del__();
}
