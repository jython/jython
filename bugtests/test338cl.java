
import java.io.*;

public class test338cl extends ClassLoader {

    protected Class loadClass(String name, boolean resolve)
        throws ClassNotFoundException
    {
//System.out.println("MyLoadClass " + name);
        Class c = findLoadedClass(name);
        if (c != null)
            return c;

        try {
            FileInputStream fis = new FileInputStream(name.replace('.', '/') + ".class");
            int size = fis.available();
            byte[] buf = new byte[size];
            fis.read(buf);
            fis.close();

            c = defineClass(name, buf, 0, buf.length);
            if (resolve)
                resolveClass(c);
            return c;
        } catch (IOException exc) {
            return super.loadClass(name, resolve);
        }
    }

}
