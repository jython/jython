/*
 * Copyright (c) 2008 Jython Developers
 * Licensed to PSF under a Contributor Agreement.
 */
package org.python.compiler;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface APIVersion {
    int value();
}
