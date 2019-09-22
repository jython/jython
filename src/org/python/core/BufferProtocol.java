package org.python.core;

/**
 * Interface marking an object as capable of exposing its internal state as a {@link PyBuffer}.
 * <p>
 * A few objects implement {@code BufferProtocol} (e.g. by inheritance) but cannot actually provide
 * their value as a {@link PyBuffer}. These should throw {@code ClassCastException}, permitting the
 * idiom: <pre>
 *     try (PyBuffer buf = ((BufferProtocol) obj).getBuffer(PyBUF.SIMPLE)) {
 *         ... // Do something with buf
 *     } catch (ClassCastException e) {
 *         ... // expected bytes object or buffer not obj.getType()
 *     }
 * </pre> The {@code catch} is executed identically whether the cause is the explicit cast of
 * {@code obj} or {@code getBuffer}, and the try-with-resources releases the buffer if one was
 * obtained.
 */
public interface BufferProtocol {

    /**
     * Method by which the consumer requests the buffer from the exporter. The consumer provides
     * information on its ability to understand buffer navigation. Each consumer requesting a buffer
     * in this way, when it has finished using it, should make a corresponding call to
     * {@link PyBuffer#release()} on the buffer it obtained, or {@link PyBuffer#close()} using
     * try-with-resources, since some objects alter their behaviour while buffers are exported.
     *
     * @param flags specifying features demanded and the navigational capabilities of the consumer
     * @return exported buffer
     * @throws PyException {@code BufferError} when expectations do not correspond with the buffer
     * @throws ClassCastException when the object only formally implements {@code BufferProtocol}
     */
    PyBuffer getBuffer(int flags) throws PyException, ClassCastException;
}
