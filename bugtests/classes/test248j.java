
public class test248j {
   static {
      //System.out.println("hello1");
      String s = null;
      s.length();
      //It seems like jdk1.4 doesn't like a naked throws in static.
      //throw new NullPointerException("init failure");
   }
}
