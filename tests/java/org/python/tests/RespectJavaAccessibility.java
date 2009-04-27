package org.python.tests;

public class RespectJavaAccessibility {

    public static class Banana {

        public String amethod() {
            return "Banana.amethod()";
        }

        public String amethod(int x, int y) {
            return "Banana.amethod(x,y)";
        }

        protected String protMethod() {
            return "Banana.protMethod()";
        }

        protected String protMethod(int x, int y) {
            return "Banana.protMethod(x,y)";
        }

        private String privBanana() {
            return "Banana.privBanana()";
        }
    }

    public static class Pear extends Banana {

        public String amethod(int x, int y) {
            return "Pear.amethod(x,y)";
        }

        protected String protMethod(int x, int y) {
            return "Pear.protMethod(x,y)";
        }

        private String privPear() {
            return "Pear.privPear()";
        }
    }
}
