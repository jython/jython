import java.io.*;
import java.util.*;

public class test276j {
    Object	o;

    public void TellMeMoreS(Serializable o) {
	this.o = o;
    }

    public void TellMeMoreO(Object o) {
	this.o = o;
    }
    
    public String getClassName() {
        return o.getClass().getName();
    }
}
