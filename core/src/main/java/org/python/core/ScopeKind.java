// Copyright (c)2021 Jython Developers.
// Licensed to PSF under a contributor agreement.
package org.python.core;

/** Exposers are of type or module kind. */
enum ScopeKind {

    MODULE("$module"), //
    TYPE("$self");

    ScopeKind(String selfName) {
        this.selfName = selfName;
    }

    /** Name of a "self" parameter in instance methods. */
    String selfName;
}
