
import java.net.*;
import java.lang.reflect.*;

public class test340j {
    public static void main(String[] args) {
        try {
            String jar = "./test340.jar";
            URLClassLoader theLoader = new URLClassLoader(new URL[] {
                    new URL("file:" + jar)});
            Object theLoadedClass = Class.forName("test340c", true, theLoader).
                                    newInstance();
            String[] array = new String[] {};
            Method main = theLoadedClass.getClass().
            getMethod("main", new Class[] { array.getClass() });
            main.invoke(theLoadedClass, new Object[] {new String[] {}});
        }
        catch (Throwable t) {
             System.exit(42);
        }
        System.exit(43);
    }
}

