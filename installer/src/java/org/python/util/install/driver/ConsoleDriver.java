package org.python.util.install.driver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.Iterator;

/**
 * A class driving another class, while the other class is performing console I/O.
 * 
 * <pre>
 *   (2)  [Driver]   receives question  [Tunnel]   sends question   [Console]  (1)
 *   (3)  [Driver]   sends answer       [Tunnel]   receives answer  [Console]  (4)
 * </pre>
 */
public class ConsoleDriver extends Thread {

    private Tunnel _tunnel;
    private Collection _answers;

    public ConsoleDriver(Tunnel tunnel, Collection answers) {
        _tunnel = tunnel;
        _answers = answers;
    }

    /**
     * Send answers in the correct sequence, as soon as the question is asked.
     */
    public void run() {
        Iterator answersIterator = _answers.iterator();
        while (answersIterator.hasNext()) {
            String answer = (String) answersIterator.next();
            try {
                readLine();
                sendAnswer(answer);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void sendAnswer(String answer) throws IOException, InterruptedException {
        Thread.sleep(100); // wait to be sure the question is really issued on the other end of the tunnel
        System.out.println(" -> driving: '" + answer + "'");
        answer += Tunnel.NEW_LINE;
        _tunnel.getAnswerSenderStream().write(answer.getBytes());
        _tunnel.getAnswerSenderStream().flush();
    }

    private void readLine() throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(_tunnel.getQuestionReceiverStream()));
        reader.readLine();
    }

}
