package javatests;

public class InheritanceC extends InheritanceB {

    public Inheritance replicateMe() {
        return new InheritanceC();
    }

    public Inheritance replicateParent() {
        return new InheritanceB();
    }

    public static Inheritance build() {
        return new InheritanceC();
    }

    public static Inheritance buildParent() {
        return new InheritanceB();
    }

    public String whoAmI() {
	    return "C";
    }

    public String everyOther() {
	    return "C";
    }

    public static String staticWhoAmI() {
	    return "C";
    }

}
