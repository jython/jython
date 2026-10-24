// Copyright (c)2026 Jython Developers.
// Licensed to PSF under a contributor agreement.
package org.python.types;

import java.util.Map;

/**
 * An instance of a class implementing {@code WithDict} possesses a
 * dictionary, generally exposed as a {@code __dict__} attribute. The
 * dictionary is not necessarily a Python {@code dict} or directly
 * writable. (A {@code type} object, for example, implements
 * {@link #getDict()} to return only an unmodifiable view of its
 * dictionary.) See also {@link WithDictAssignment}.
 */
public interface WithDict extends WithClass {
    /**
     * The instance dictionary. This is not necessarily a Python
     * {@code dict}, and may not be directly writable. Some implementing
     * types override the signature to specify the return is a fully-fledged
     * Python {@code dict}.
     *
     * @return instance dictionary
     */
    // @Exposed.Get(name="__dict__")
    Map<Object, Object> getDict();
}
