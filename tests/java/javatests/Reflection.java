package javatests;

import java.util.Arrays;
import java.util.List;

import org.python.core.Py;
import org.python.core.PyComplex;
import org.python.core.PyObject;

public class Reflection {

    public static class StringVarargs {

        public String test(String... args) {
            return "String...:" + Arrays.toString(args);
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
