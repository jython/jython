public class test217t {
        public static void main(String args[]) {
                test217p.test217i c = new test217c();
                String s = c.add(1, 2);
                if (!"The sum of 1 and 2 is 3".equals(s))
                    System.out.println("Wrong output:" + s);
        }
}
