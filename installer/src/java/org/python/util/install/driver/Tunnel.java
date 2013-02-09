package org.python.util.install.driver;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

/**
 * A communication tunnel between a console driver and a console.
 * 
 * <pre>
 *   (2)  [Driver]   receives question  [Tunnel]   sends question   [Console]  (1)
 *   (3)  [Driver]   sends answer       [Tunnel]   receives answer  [Console]  (4)
 * </pre>
 */
public class Tunnel {

    public static final String NEW_LINE = "\n";

    private PipedOutputStream _questionSenderStream;
    private PipedInputStream _questionReceiverStream;
    private PipedOutputStream _answerSenderStream;
    private PipedInputStream _answerReceiverStream;

    public Tunnel() throws IOException {
        _questionSenderStream = new PipedOutputStream();
        _questionReceiverStream = new PipedInputStream();
        _questionSenderStream.connect(_questionReceiverStream);

        _answerSenderStream = new PipedOutputStream();
        _answerReceiverStream = new PipedInputStream();
        _answerSenderStream.connect(_answerReceiverStream);
    }

    public PipedOutputStream getQuestionSenderStream() {
        return _questionSenderStream;
    }

    public PipedInputStream getQuestionReceiverStream() {
        return _questionReceiverStream;
    }

    public PipedOutputStream getAnswerSenderStream() {
        return _answerSenderStream;
    }

    public PipedInputStream getAnswerReceiverStream() {
        return _answerReceiverStream;
    }

    public void close() throws IOException {
        _questionReceiverStream.close();
        _questionSenderStream.close();
        _answerReceiverStream.close();
        _answerSenderStream.close();

        _questionReceiverStream = null;
        _questionSenderStream = null;
        _answerReceiverStream = null;
        _answerSenderStream = null;
    }
}
