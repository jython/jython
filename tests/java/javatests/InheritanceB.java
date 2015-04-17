package javatests;

public class InheritanceB extends InheritanceA {

    public Inheritance replicateMe() {
        return new InheritanceB();
    }

    public Inheritance replicateParent() {
	    throw new UnsupportedOperationException("parent is abstract");
    }

    public static Inheritance build() {
        return new InheritanceB();
    }

    public static Inheritance buildParent() {
        throw new UnsupportedOperationException("parent is abstract");
    }

    public String whoAmI() {
	    return "B";
    }

    public String notInAbstract() {
	    return "B";
    }

    public static String staticWhoAmI() {
	    return "B";
    }

}
