
public class test219j {
    public static void checkFoo(test219i o) {
        try {
            o.foo();
        } catch (test219e t) {
            // Success.
        } catch (Throwable t) {
            System.out.println(t.getClass() + " " + t);
        }
    }


    public static void checkFoo2(test219i o, int i) {
        try {
            o.foo2(i);
        } catch (test219e t) {
            if (i != 0)
                System.out.println("failure to catch test219e");
        } catch (Throwable t) {
            if (i != 1)
                System.out.println("failure to catch Throwable");
        }
    }
}