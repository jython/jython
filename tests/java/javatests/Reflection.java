package javatests;

import java.util.Arrays;
import java.util.List;

import org.python.core.Py;
import org.python.core.PyComplex;
import org.python.core.PyObject;

public class Reflection {

    public static class BooleanVarargs {

        public String test(boolean... args) {
            return "booleans...:" + Arrays.toString(args);
        }
        
        public String testOneFixedArg(boolean arg1, boolean... args) {
            return "boolean arg1:" + arg1 + " booleans...:" + Arrays.toString(args);
        }
        
        public String testTwoFixedArg(boolean arg1, boolean arg2, boolean... args) {
            return "boolean arg1:" + arg1 + " boolean arg2:" + arg2 + " booleans...:" + Arrays.toString(args);
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
            return "String arg1:" + arg1 + " String arg2:" + arg2 + " String...:" + Arrays.toString(args);
        }
    }

    public static class ListVarargs {

        public String test(List... args) {
            return "List...:" + Arrays.toString(args);
        }
    }

    public static class Overloaded {

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
    }
}
