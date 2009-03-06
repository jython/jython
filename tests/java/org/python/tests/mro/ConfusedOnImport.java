package org.python.tests.mro;

/**
 * {@link GetitemAdder#addPredefined} is expected to be called before this class is imported. If
 * that's happened, there's a MRO conflict between {@link FirstPredefinedGetitem} and
 * {@link SecondPredefinedGetitem}, so importing this should cause a TypeError.
 */
public class ConfusedOnImport implements FirstAndSecond, SecondAndFirst {}
