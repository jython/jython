
package org.python.core;

public interface Loader {
   public Class loadClassFromBytes(String name, byte[] data);
   public void addParent(ClassLoader referent);
}
