
import org.python.core.*;

public class test142j extends PyObject {
  double[] data;
  int len;
  public test142j(double[] darray) { 
     data=darray;
     len=data.length;
  }   
  public int __len__() {
    return len; 
  }  

  public PyString __repr__() {
    StringBuffer buf = new StringBuffer();
    for(int i=0; i<len; i++) {
      buf.append(data[i]);
      buf.append(", ");
    }
    return new PyString(buf.toString());  
  }

  public static test142j new$(double[] array) {
    return new test142j(array);
  }
}