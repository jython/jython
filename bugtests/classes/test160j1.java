
public class test160j1 {
    public static test160j1 clField;

    public void go() {
        clField = this;
        test160j2 b = new test160j2();
    }

    public String func() {
        return "success!";
    }

    public static void main( String[] arg ) {
        test160j1 a = new test160j1();
        a.go();
    }
}