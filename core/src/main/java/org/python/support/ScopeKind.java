// Copyright (c)2026 Jython Developers.
// Licensed to PSF under a contributor agreement.
package org.python.support;

/**
 * The scope within which a method was found affects argument
 * processing, how it is processed for exposure, and how its signature
 * is presented in documentation.
 */
public enum ScopeKind {
    /** The method is for a module. */
    MODULE("$module"),
    /** The method is for a type. */
    TYPE("$self");

    /** @param selfName appearance of self in a documentation string. */
    ScopeKind(String selfName) {
        this.selfName = selfName;
    }

    /** Name of a "self" parameter in instance methods. */
    public String selfName;
}
