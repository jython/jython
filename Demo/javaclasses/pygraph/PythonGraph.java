package pygraph;

import java.awt.*;
import java.awt.event.*;

import javax.swing.JTextField;

public class PythonGraph implements ActionListener {
    JTextField expression;
    Graph graph;

    public PythonGraph() {
        Frame frame = new Frame("Python Graph");
        String expr = "sin(x)";

        graph = new Graph(expr);
        frame.add(graph, "Center");

        expression = new JTextField(expr);
        frame.add(expression,"South");
        expression.addActionListener(this);

        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent event) {
                System.exit(0);
            }
        });

        frame.pack();
        frame.setSize(300, 300);
        frame.setVisible(true);
    }

    public void actionPerformed(ActionEvent evt) {
        graph.setExpression(expression.getText());
    }

    public static void main(String[] args) {
        PythonGraph pg = new PythonGraph();
    }
}

