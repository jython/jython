package pygraph;

import java.awt.*;
import java.awt.event.*;

public class PythonGraph {
    TextField expression;
    Graph graph;

    public PythonGraph() {
        Frame frame = new Frame("Python Graph");

        graph = new Graph();
        frame.add(graph, "Center");
        graph.setExpression("sin(x)");

        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent event) {
                System.exit(0);
            }
        });

        frame.pack();
        frame.setSize(300, 300);
        frame.setVisible(true);
    }

    public static void main(String[] args) {
        PythonGraph pg = new PythonGraph();
    }
}