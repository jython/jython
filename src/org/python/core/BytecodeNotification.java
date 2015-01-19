package org.python.core;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.Collections;
import java.io.ByteArrayOutputStream;

/**
 * Notifies registered callbacks if new bytecode is loaded.
 */
public class BytecodeNotification {
    /**
     * Interface for callbacks. 
     * Notifies the name of the loaded class, raw bytes of the class, 
     * and the Java class object.
     */
    public interface Callback {
        public void notify(String name, byte[] bytes, Class c);
    }

    /**
     * The following list stores register callback objects. 
     * The list is shared among the PySystemState objects 
     * if there are multiple instances.
     */
    private static List<Callback> callbacks = new CopyOnWriteArrayList();

    static {
        // Maintain legacy behavior
        register(new Callback() {
            public void notify(String name, byte[] bytes, Class c) {
                if (Options.proxyDebugDirectory == null ||
                        (!name.startsWith("org.python.pycode.") &&
                                !name.startsWith("org.python.proxies."))) {
                    return;
                }
                ByteArrayOutputStream ostream = new ByteArrayOutputStream(bytes.length);
                ostream.write(bytes, 0, bytes.length);
                Py.saveClassFile(name, ostream);
            }
        });
    }

    /**
     * Registers the class as a callback
     *
     * @param n the callback object
     */
    public static void register(Callback n) { callbacks.add(n); }

    /**
     * Unregisters the callback object
     *
     * @param n the callback object
     * @return true if successfully removed and 
     *         false if the callback object was not registered
     */
    public static boolean unregister(Callback n) { return callbacks.remove(n); }
    
    /**
     * Clears all the registered callbacks
     */
    public static void clear() { callbacks.clear(); }

    /**
     * Notifies that the new bytecode to the registered callbacks
     *
     * @param name the name of the class of the new bytecode
     * @param data raw byte data of the class
     * @param class Java class object of the new bytecode
     */
    public static void notify(String name, byte[] data, Class klass) {
        for (Callback c:callbacks) {
            try {
                c.notify(name, data, klass);
            } catch (Exception e) {
                Py.writeWarning("BytecodeNotification", "Exception from callback:"+e); 
            }
        }
    }
}
