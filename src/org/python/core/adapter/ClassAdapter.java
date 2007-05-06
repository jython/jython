package org.python.core.adapter;

public abstract class ClassAdapter implements PyObjectAdapter {

	public ClassAdapter(Class adaptedClass) {
		this.adaptedClass = adaptedClass;
	}

	public Class getAdaptedClass() {
		return adaptedClass;
	}

	public boolean canAdapt(Object o) {
		return adaptedClass.getClass().equals(adaptedClass);
	}

	private Class adaptedClass;

}
