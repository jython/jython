// Copyright (c)2026 Jython Developers.
// Licensed to PSF under a contributor agreement.
/**
 * The Jython core classes and API as a module.
 *
 * The intention for the core module (jython-core JAR) is that it
 * contain all that a developer would depend on to embed Jython or write
 * an extension module, mixing their own Java and Python code. It will
 * export several packages as API (and keep some packages to itself).
 */
module org.python.core {

    // For now, before any restructuring, we just export all the modules
    exports org.python.core;
    exports org.python.stringlib;
    exports org.python.modules;
    exports org.python.support;
    exports org.python.types;

    // We require SLF4 logging API and ASM
    requires transitive org.slf4j;
    requires org.objectweb.asm;
    requires org.objectweb.asm.tree;
}
