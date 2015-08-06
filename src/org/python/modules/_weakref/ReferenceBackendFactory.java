package org.python.modules._weakref;

import org.python.core.PyObject;

/**
 * Reserved for use by JyNI.
 */
public interface ReferenceBackendFactory {

    public ReferenceBackend makeBackend(GlobalRef caller, PyObject referent);
    public void notifyClear(ReferenceBackend ref, GlobalRef caller);
    public void updateBackend(ReferenceBackend ref, GlobalRef caller);
}
