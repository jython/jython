//Copyright (c) Corporation for National Research Initiatives
package javatests;

/**
 * @author updikca1
 */
public class TestSupport {

    public static class AssertionError extends RuntimeException {
        
        public AssertionError() {
            super();
        }
        
        public AssertionError(String message) {
            super(message);
        }
        
        public AssertionError(String message, Throwable cause) {
            super(message, cause);
        }
        
        public AssertionError(Throwable cause) {
            super(cause);
        }
    }
    
    public static void assertThat(boolean test, String message) {
        
        if (test == false) {
            throw new AssertionError(message);
        }
    }
    
    public static void assertEquals(Object a, Object b, String message) {
        
        assertThat(a.equals(b), message + "[a.equals(b) failed]");
        assertThat(b.equals(a), message + "[b.equals(a) failed]");
    }
    
    public static void assertNotEquals(Object a, Object b, String message) {
        
        assertThat( !a.equals(b), message + "[not a.equals(b) failed]");
        assertThat( !b.equals(a), message + "[not b.equals(a) failed]");
    }
}

