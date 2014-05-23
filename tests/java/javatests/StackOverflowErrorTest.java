package javatests;

public class StackOverflowErrorTest {

    public static void throwStackOverflowError() {
	throw new StackOverflowError();
    }

    public static void causeStackOverflowError() {
	causeStackOverflowError();
    }
}
