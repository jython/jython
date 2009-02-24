package org.python.tests.inbred;

/**
 * Zeus ate Metis to prevent her from giving birth, but Athena was born inside Zeus' head anyway. By
 * the mythology Metis should extend from Oceanus and Tethys, but we need Metis to extend Zeus to
 * get the kind of inheritance that caused http://bugs.jython.org/issue1234
 */
public interface Zeus {

    public interface Athena extends Metis {}
}
