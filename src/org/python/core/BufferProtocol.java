package org.python.core;

/**
 * Interface marking an object as capable of exposing its internal state as a {@link PyBuffer}.
 */
public interface BufferProtocol {

    /**
     * Method by which the consumer requests the buffer from the exporter. The consumer
     * provides information on its intended method of navigation and the optional
     * features the buffer object must provide.
     * 
     * @param flags specification of options and the navigational capabilities of the consumer
     * @return exported buffer
     */
    PyBuffer getBuffer(int flags);
}
