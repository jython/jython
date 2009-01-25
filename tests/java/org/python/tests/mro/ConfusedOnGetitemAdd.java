package org.python.tests.mro;

/**
 * If {@link GetitemAdder#addPredefined} is called, this class is imported, then
 * {@link GetitemAdder#addPostdefined} is called, the call to postdefined should raise a TypeError
 * as this class produces a MRO conflict between {@link FirstPredefinedGetitem} and
 * {@link PostdefinedGetitem}.
 */
public interface ConfusedOnGetitemAdd extends FirstAndPost, PostAndFirst {}
