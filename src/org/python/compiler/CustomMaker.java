package org.python.compiler;

import org.python.core.BytecodeLoader;
import org.python.core.Py;
import org.python.core.PyObject;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Arrays;


public class CustomMaker extends JavaMaker {

    public CustomMaker(Class<?> superclass,
                     Class<?>[] interfaces,
                     String pythonClass,
                     String pythonModule,
                     String myClass,
                     PyObject methods) {
        super(superclass, interfaces, pythonClass, pythonModule, myClass, methods);
    }

    // Override to save bytes
    public void saveBytes(ByteArrayOutputStream bytes) {
    }

    // By default makeClass will have the same behavior as MakeProxies calling JavaMaker,
    // other than the debug behavior of saving the classfile (as controlled by
    // Options.ProxyDebugDirectory; users of CustomMaker simply need to save it themselves).
    //
    // Override this method to get custom classes built from any desired source.
    public Class<?> makeClass() {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            build(bytes); // Side effect of writing to bytes
            saveBytes(bytes);
            List<Class<?>> secondary = new LinkedList(Arrays.asList(interfaces));
            List<Class<?>> referents = null;
            if (secondary != null) {
                if (superclass != null) {
                    secondary.add(0, superclass);
                }
                referents = secondary;
            } else if (superclass != null) {
                referents = new ArrayList<Class<?>>(1);
                referents.add(superclass);
            }
            return BytecodeLoader.makeClass(myClass, referents, bytes.toByteArray());
        } catch (Exception exc) {
            throw Py.JavaError(exc);
        }
    }
}
