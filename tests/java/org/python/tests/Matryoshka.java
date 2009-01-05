package org.python.tests;

public class Matryoshka {

    public static Outermost makeOutermost() {
        return new Outermost();
    }

    public static class Outermost {

        public static class Middle {

            public static class Innermost {}
        }
    }
}
