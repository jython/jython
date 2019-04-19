// Copyright (c)2019 Jython Developers.
// Licensed to the Python Software Foundation under a Contributor Agreement.

package org.python.tests.mro;

/**
 * A class providing interface and abstract class relationships that approximate the structure of
 * org.eclipse.emf.ecore.util.DelegatingFeatureMap, in order to exercise b.j.o issue 2445. The
 * complex inheritance confused PyJavaType handling of the MRO. This class is imported by
 * {@code test_java_integration.JavaMROTest.test_mro_eclipse} as a test.
 * <p>
 * An invocation at the prompt (for debugging use), and output before the fix, is: <pre>
 * PS &gt; dist\bin\jython -S -c"from org.python.tests.mro import EclipseChallengeMRO"
 * Traceback (most recent call last):
 *   File "&lt;string&gt;", line 1, in &lt;module&gt;
 * TypeError: Supertypes that share a modified attribute have an MRO
 * conflict[attribute=sort, supertypes=[&lt;type 'java.util.List'&gt;,
 * &lt;type 'java.util.AbstractList'&gt;], type=EclipseChallengeMRO$Target]
 * </pre>
 */
public class EclipseChallengeMRO {

    interface Entry {} // Was FeatureMap.Entry
    interface Thing {} // Was EStructuralFeature.Setting
    interface EList<E> extends java.util.List<E> {}
    interface IEList<E> extends EList<E> {}
    interface FList extends EList<Entry> {} // Was FeatureMap
    interface FIEListThing extends FList, IEList<Entry>, Thing {}  // Was 2 FeatureMap.Internal
    abstract static class AbstractEList<E> extends java.util.AbstractList<E> implements EList<E> {}
    abstract static class DEList<E> extends AbstractEList<E> {}
    interface NList<E> extends EList<E> {}
    abstract static class DNListImpl<E> extends DEList<E> implements NList<E> {}
    abstract static class DNIEListImpl<E> extends DNListImpl<E> implements IEList<E> {}
    abstract static class DNIEListThing<E> extends DNIEListImpl<E> implements Thing {}
    public abstract static class Target extends DNIEListThing<Entry> implements FIEListThing {}
}
