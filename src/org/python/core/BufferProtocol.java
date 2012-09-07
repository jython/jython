package org.python.core;

/**
 * Interface marking an object as capable of exposing its internal state as a {@link PyBuffer}.
 */
public interface BufferProtocol {

    /**
     * Method by which the consumer requests the buffer from the exporter. The consumer provides
     * information on its intended method of navigation and the features the buffer object is asked
     * (or assumed) to provide. Each consumer requesting a buffer in this way, when it has finished
     * using it, should make a corresponding call to {@link PyBuffer#release()} on the buffer it
     * obtained, since some objects alter their behaviour while buffers are exported.
     * 
     * @param flags specifying features demanded and the navigational capabilities of the consumer
     * @return exported buffer
     * @throws PyException (BufferError) when expectations do not correspond with the buffer
     */
    PyBuffer getBuffer(int flags) throws PyException;
}
