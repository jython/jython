package test364p;
public class X {
    static Class myClass;
    // register a class to construct
    public static void useClass(Class cls) {
        myClass=cls;
    }
    Object o;
    public X() throws Exception {
        o=myClass.newInstance();
    }
} 

