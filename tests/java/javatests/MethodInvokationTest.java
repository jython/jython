package javatests;

public class MethodInvokationTest{
     public static String foo1(int i) {
       return "foo1 with int arg: " + i;
     }

     public static String foo1(char c) {
       return "foo1 with char arg: " + c;
     }

     public static String foo1(boolean b) {
       return "foo1 with boolean arg: " + b;
     }

     public static String foo1(byte bt) {
       return "foo1 with byte arg: " + bt;
     }
}
