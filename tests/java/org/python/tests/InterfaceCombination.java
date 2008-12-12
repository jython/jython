package org.python.tests;

public class InterfaceCombination {

    public static final String NO_ARG_RESULT = "no_arg_result";

    public static final String ONE_ARG_RESULT = "one_arg_result";

    public static final String TWO_ARG_RESULT = "two_arg_result";

    public interface IFace {
        String getValue();
    }

    public interface IIFace {
        String getValue(String name);
    }

    interface Hidden {
        void internalMethod();
    }

    public static class Base {
        public String getValue(String one, String two) {
            return TWO_ARG_RESULT;
        }
    }

    private static class Implementation extends Base implements IFace, IIFace, Hidden {

        public String getValue(String one, String two, String three) {
            return three;
        }

        public String getValue() {
            return NO_ARG_RESULT;
        }

        public String getValue(String name) {
            return ONE_ARG_RESULT;
        }

        public void internalMethod() {}
    }

    public static Object newImplementation() {
        return new Implementation();
    }
}
