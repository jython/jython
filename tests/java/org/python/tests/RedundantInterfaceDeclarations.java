package org.python.tests;

/**
 * Part of a test for issue #1381. It checks that Jython finds the proper implementation of an
 * interface method when dealing with classes that have redundant declarations of implementing
 * interfaces. The test itself is in Lib/test_java_visibility.py#test_interface_methods_merged
 */
public class RedundantInterfaceDeclarations {

    public interface IntArg {

        String call(int arg);
    }

    public interface ClassArg {

        String call(Class<?> arg);
    }

    public interface StringArg extends ClassArg {

        String call(String name);
    }

    public static abstract class AbstractImplementation implements StringArg, IntArg {

        public String call(Class<?> arg) {
            return "Class";
        }
    }

    public static class Implementation extends AbstractImplementation implements StringArg {
        public String call(String name) {
            return "String";
        }

        public String call(int arg) {
            return "int";
        }
    }

    public static class ExtraString extends Implementation implements StringArg {}

    public static class ExtraClass extends Implementation implements ClassArg {}

    public static class ExtraStringAndClass extends Implementation implements StringArg, ClassArg {}

    public static class ExtraClassAndString extends Implementation implements ClassArg, StringArg {}
}
