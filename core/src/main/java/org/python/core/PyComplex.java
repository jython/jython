package org.python.core;

/**
 * This is a placeholder to satisfy references in implementations of
 * {@code __complex__} preserved from Jython 2.
 */
public class PyComplex {
    double real, imag;

    public PyComplex(double real, double imag) {
        this.real = real;
        this.imag = imag;
    }
}
