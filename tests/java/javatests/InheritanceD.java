package javatests;

public class InheritanceD extends InheritanceC {

    public Inheritance replicateMe() {
        return new InheritanceD();
    }

    public Inheritance replicateParent() {
        return new InheritanceC();
    }

    public static Inheritance build() {
        return new InheritanceD();
    }

    public static Inheritance buildParent() {
        return new InheritanceC();
    }

    public String whoAmI() {
        return "D";
    }

    public static String staticWhoAmI() {
        return "D";
    }

}
