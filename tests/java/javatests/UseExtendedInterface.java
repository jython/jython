// Used to test for http://bugs.jython.org/issue1795

package javatests;

public class UseExtendedInterface {
    public int countWords(ExtendedInterface obj) {
	    return obj.returnSomething().split("\\s+").length;
    }
}
