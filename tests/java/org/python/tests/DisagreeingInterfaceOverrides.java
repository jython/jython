package org.python.tests;

import java.util.ArrayList;
import java.util.List;

/**
 * Part of a test for issue #1381. It checks that Jython finds the proper overridden method when
 * dealing with several interfaces. The test itself is in
 * Lib/test_java_visibility.py#test_interface_methods_merged
 */
public class DisagreeingInterfaceOverrides {

    public interface StringArg {

        String call(String arg);
    }

    public interface IntArg {

        String call(int arg);
    }

    public interface ListArg {

        String call(List<Object> arg);
    }

    public interface ArrayListArg {

        String call(List<Object> arg);
    }

    public static class Implementation implements StringArg, IntArg, ListArg, ArrayListArg {

        public String call(String arg) {
            return "String";
        }

        public String call(int arg) {
            return "int";
        }

        public String call(List<Object> arg) {
            return "List";
        }

        public String call(ArrayList<Object> arg) {
            return "ArrayList";
        }
    }
}
