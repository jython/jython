package test279p; 

public class Foo extends Bar { 
    public String test() {
        return hi();
    }
} 

class Bar { 
    public String hi() { 
        return "hi"; 
    } 

    public String private_hi() { 
        return "private_hi"; 
    } 

} 
