package org.python.util.install.driver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.python.util.install.driver.Tunnel;

/**
 * A simple class performing console I/O, easy to test.
 */
public class DrivableConsole {

    private static final String _PROMPT = ">>>";
    private Tunnel _tunnel;

    public DrivableConsole(Tunnel tunnel) {
        _tunnel = tunnel;
    }

    /**
     * The console logic.
     */
    public void handleConsoleIO() throws Exception {
        String answer;
        answer = question("first question");
        if ("1".equals(answer)) {
            System.out.println("answer1 is " + answer);
            answer = question("second question");
            if ("2".equals(answer)) {
                System.out.println("answer2 is " + answer);
                answer = question("third question");
                if ("3".equals(answer)) {
                    System.out.println("answer3 is " + answer);
                } else {
                    throw new Exception("wrong answer3: " + answer);
                }
            } else {
                throw new Exception("wrong answer2: " + answer);
            }
        } else {
            throw new Exception("wrong answer1: " + answer);
        }
    }

    /**
     * Write a question (to normal <code>System.out</code>)
     */
    private String question(String question) throws IOException {
        question = question + " " + _PROMPT + " ";
        String answer = "";
        // output to normal System.out
        System.out.print(question); // intended print, not println (!)
        answer = readLine();
        return answer;
    }

    /**
     * Send a signal through the tunnel, and then wait for the answer from the other side.
     * 
     * <pre>
     *     (2)  [Driver]   receives question  [Tunnel]   sends question   [Console]  (1)
     *     (3)  [Driver]   sends answer       [Tunnel]   receives answer  [Console]  (4)
     * </pre>
     */
    private String readLine() throws IOException {
        InputStream inputStream;
        String line = "";
        if (_tunnel == null) {
            inputStream = System.in;
        } else {
            inputStream = _tunnel.getAnswerReceiverStream();
            _tunnel.getQuestionSenderStream().write(Tunnel.NEW_LINE.getBytes());
            _tunnel.getQuestionSenderStream().flush();
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        line = reader.readLine();
        return line;
    }

}
