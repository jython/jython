package javatests;

public class ProxyTests {

    public static class Person {

        // models conventions of names in certain parts of the world

        private String firstName;
        private String lastName;
    
        public Person(String firstName, String lastName) {
            this.firstName = firstName;
            this.lastName = lastName;
        }
    
        public String toString() {
            return lastName + ", " + firstName;
        }
    
        public String getFirstName() {
            return firstName;
        }

        public void setFirstName(String firstName) {
            this.firstName = firstName;
        }

        public String getLastName() {
            return lastName;
        }
    
        public void setLastName(String lastName) {
            this.lastName = lastName;
        }
    }

    public static class NullToString {
        public String toString() { return null; }
    }

}
