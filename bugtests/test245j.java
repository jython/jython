import java.util.*; 
import org.python.util.PythonInterpreter;
import org.python.core.*;
import java.lang.reflect.*; 

public abstract class test245j {
	
    public static test245j createPythonScriptClass(
                 String var[],String script,String name) {
        StringTokenizer st=new StringTokenizer(script,"\n\r");

        StringBuffer scr=new StringBuffer(
        "import test245j\n");

        scr.append("class PySS");
        scr.append(name);
        scr.append("(test245j):\n  def get(self,variables):\n");

        for (int i=0;i<var.length;i++) {
            scr.append("    ");
            scr.append(var[i]);
            scr.append("=variables[");
            scr.append(i);
            scr.append("]\n");
        }

        while (st.hasMoreTokens()) {
            scr.append("    ");
            scr.append(st.nextToken());
            scr.append("\n");
        }
        scr.append("    return ");
        scr.append(name);
        scr.append("\nrr=PySS");
        scr.append(name);
        scr.append("()\n\n");

        String scriptString=scr.toString();
	
//System.out.println("------------------------\n"+scriptString+"------------------------\n");
        PythonInterpreter interp = new PythonInterpreter();
        interp.exec("import sys");
        interp.exec(scriptString);

		
        Object pso = interp.get("rr",Object.class);
//        System.out.println("pso class: "+pso.getClass().getName());
//        System.out.println("pso superclass:"+(pso.getClass()).getSuperclass().getName());
//        Method met[]=pso.getClass().getMethods();
//        for(int i=0;i<met.length;i++) 
//            System.out.println("Met: "+met[i]);

        test245j psc = null;

        psc =(test245j) pso;  //<-------------------------------------------------------------------------EXCEPTION
//        System.out.println("     psc class: "+psc.getClass().getName());
//        System.out.println("     psc superclass:"+(psc.getClass()).getSuperclass().getName());
        return psc;
    }
	
    public double get(double variable[]) {
        return 0.0;
    }; 

    public static void main(String args[]) throws Exception {
        String names[]={"a","b"};
        double variables[]={1.0,2.0};
        String script="c = a + b";
        test245j psc=test245j.createPythonScriptClass(names,script,"c");
        double c=psc.get(variables);
        if (c != 3.0)
            throw new Exception("Wrong result");
        //System.out.println("C = "+c);
    }
}
