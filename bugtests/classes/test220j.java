
public class test220j {
    public static void checkFoo(test220i o) {
        try {
            o.foo();
        } catch (test220e t) {
            // Success.
        } catch (Throwable t) {
            System.out.println(t.getClass() + " " + t);
        }
    }


    public static void checkFoo2(test220i o, int i) {
        try {
            o.foo2(i);
        } catch (test220e t) {
            if (i != 0)
                System.out.println("failure to catch test220e");
        } catch (Throwable t) {
            if (i != 1)
                System.out.println("failure to catch Throwable");
        }
    }
}