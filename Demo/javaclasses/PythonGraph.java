import java.awt.*;
import java.awt.event.*;
import org.python.core.*;

public class PythonGraph {
    TextField expression;
    Graph graph;

    public PythonGraph() throws PyException {
        Frame frame = new Frame("Python Graph");

        graph = new Graph();
        frame.add(graph, "Center");

        ((PyProxy)graph).getProxy().invoke("setExpression", new PyString("sin(x)"));

        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent event) {
                System.exit(0);
            }
        });

        frame.pack();
        frame.setVisible(true);
        frame.setSize(300, 300);
    }

    public static void main(String[] args) throws PyException {
        PythonGraph pg = new PythonGraph();
    }
}