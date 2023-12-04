package javatests;

import java.util.Arrays;
import java.util.List;

import org.python.core.Py;
import org.python.core.PyComplex;
import org.python.core.PyObject;

/**
 * Test objects used in Python {@code test.test_joverload} to test how our processing of arguments
 * from Python find their way to the formal arguments of the Java signature of a method or
 * constructor, particularly when methods in Java are overloaded and offer variable arity. When the
 * class is run as a program, it outputs (some of) the same strings expected in the Python tests.
 */
public class Reflection {

    public static class BooleanVarargs {

        public String test(boolean... args) {
            return "booleans...:" + Arrays.toString(args);
        }

        public String testOneFixedArg(boolean arg1, boolean... args) {
            return "boolean arg1:" + arg1 + " booleans...:" + Arrays.toString(args);
        }

        public String testTwoFixedArg(boolean arg1, boolean arg2, boolean... args) {
            return "boolean arg1:" + arg1 + " boolean arg2:" + arg2 + " booleans...:"
                    + Arrays.toString(args);
        }

        /** Compare {@code test_joverload.VarargsDispatchTests.test_booleans}. */
        void mainTest() {
            System.out.println("== Compare VarargsDispatchTests.test_booleans");

            System.out.println(test(true, false));
            System.out.println(test(true));
            System.out.println(test());

            System.out.println(testOneFixedArg(true));
            System.out.println(testOneFixedArg(true, false));
            System.out.println(testOneFixedArg(true, false, true));

            System.out.println(testTwoFixedArg(true, false));
            System.out.println(testTwoFixedArg(true, false, true));
            System.out.println(testTwoFixedArg(true, false, true, true));
        }
    }

    public static class StringVarargs {

        public String test(String... args) {
            return "String...:" + Arrays.toString(args);
        }

        public String testOneFixedArg(String arg1, String... args) {
            return "String arg1:" + arg1 + " String...:" + Arrays.toString(args);
        }

        public String testTwoFixedArg(String arg1, String arg2, String... args) {
            return "String arg1:" + arg1 + " String arg2:" + arg2 + " String...:"
                    + Arrays.toString(args);
        }

        /** Compare {@code test_joverload.StringVarargs.test_strings}. */
        void mainTest() {
            System.out.println("== Compare StringVarargs.test_strings");

            System.out.println(test("abc", "xyz"));
            System.out.println(test("abc"));
            System.out.println(test());

            System.out.println(test(new String[] {"abc", "xyz"}));
            System.out.println(test(new String[] {"abc"}));
            System.out.println(test(new String[0]));

            System.out.println(testOneFixedArg("abc"));
            System.out.println(testOneFixedArg("abc", "xyz"));
            System.out.println(testOneFixedArg("abc", "xyz", "123"));

            System.out.println(testTwoFixedArg("fix1", "fix2"));
            System.out.println(testTwoFixedArg("fix1", "fix2", "var1"));
            System.out.println(testTwoFixedArg("fix1", "fix2", "var1", "var2"));
        }
    }

    public static class ListVarargs {

        public String test(List... args) {
            return "List...:" + Arrays.toString(args);
        }

        /** Compare {@code VarargsDispatchTests.test_lists}. */
        void mainTest() {
            System.out.println("== Compare VarargsDispatchTests.test_lists");
            List a1 = Arrays.asList(1, 2, 3);
            List a2 = Arrays.asList(4, 5, 6);
            System.out.println(test(a1, a2));
            System.out.println(test(a1));
            System.out.println(test());
            System.out.println(test(new List[] {a1, a2}));
            System.out.println(test(new List[] {a1}));
            System.out.println(test(new List[] {}));
        }
    }

    public static class Overloaded {

        private final String constructorVersion;

        public Overloaded() {
            constructorVersion = "";
        }

        public Overloaded(int a) {
            constructorVersion = "int";
        }

        public Overloaded(int a, int b) {
            constructorVersion = "int, int";
        }

        public Overloaded(int a, int b, Object c) {
            constructorVersion = "int, int, Object";
        }

        public Overloaded(int a, int... others) {
            constructorVersion = "int, int...";
        }

        public Overloaded(String s) {
            constructorVersion = "String";
        }

        public Overloaded(String s, Object... objs) {
            constructorVersion = "String, Object...";
        }

        public Overloaded(String s, Throwable t) {
            constructorVersion = "String, Throwable";
        }

        public Overloaded(String s, Throwable t, Object... objs) {
            constructorVersion = "String, Throwable, Object...";
        }

        public String getConstructorVersion() {
            return constructorVersion;
        }

        public String foo(int a, int b) {
            return "int, int";
        }

        public String foo(int a, int... b) {
            return "int, int...";
        }

        public String foo(int a, int b, Object c) {
            return "int, int, Object";
        }

        public String foo(int... a) {
            return "int...";
        }

        public String bar(int a) {
            return "int";
        }

        public String bar(long a) {
            return "long";
        }

        public String bar(boolean a) {
            return "boolean";
        }

        public String bar(float a) {
            return "float";
        }

        public String bar(Number a) {
            return "Number";
        }

        public PyObject __call__(float x) {
            return dump(x);
        }

        public PyObject __call__(double x) {
            return dump(x);
        }

        public PyObject __call__(PyComplex x) {
            return dump(x);
        }

        private PyObject dump(Object o) {
            return Py.newString(o.getClass() + "=" + o);
        }

        /** Compare ComplexOverloadingTests.test_constructor_overloading */
        static void constructorTest() {
            System.out.println("== Compare ComplexOverloadingTests.test_constructor_overloading");
            System.out.println((new Overloaded(1)).constructorVersion);
            System.out.println((new Overloaded(1, 1)).constructorVersion);
            System.out.println((new Overloaded(1, 1, 1)).constructorVersion);
            System.out.println((new Overloaded(1, 1, 1, 1)).constructorVersion);

            System.out.println((new Overloaded(1, new int[] {2, 3, 4})).constructorVersion);

            Exception b = new Exception("Oops");
            System.out.println((new Overloaded("a")).constructorVersion);
            System.out.println((new Overloaded("a", 2)).constructorVersion);
            System.out.println((new Overloaded("a", b)).constructorVersion);
            System.out.println((new Overloaded("a", b, 3)).constructorVersion);
        }

        /** Compare {@code ComplexOverloadingTests.test_method_overloading}. */
        void mainTest() {
            System.out.println("== Compare ComplexOverloadingTests.test_method_overloading");
            System.out.println(foo());
            System.out.println(foo(1, 2));
            System.out.println(foo(1, 2, 3));
            System.out.println(foo(1, new int[] {2, 3, 4}));
            // Not possible in Java: both foo(int,int...) and foo(int...) match
            // System.out.println(foo(1));
            // System.out.println(foo(1, 2, 3, 4));
        }
    }

    /**
     * This program calls the test methods from Java for comparison with {@code test.test_joverload}
     * in the Jython-specific regression tests. You may need to run {@code ant compile-test} to
     * compile it, depending on the way your IDE is set up.
     *
     * @param args ignored
     */
    public static void main(String[] args) {
        new ListVarargs().mainTest();
        Overloaded.constructorTest();
        new Overloaded().mainTest();
        new BooleanVarargs().mainTest();
        new StringVarargs().mainTest();
    }
}
