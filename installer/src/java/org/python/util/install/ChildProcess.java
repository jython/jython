package org.python.util.install;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Easy start of a child process.
 * <p>
 * Features are:
 * <ul>
 * <li>wait for the child process to finish.
 * <li>kill the child process after a specified timeout.
 * <li>get the output of the child process (System.out and System.err) redirected to the calling
 * process, unless in silent mode.
 * </ul>
 */
public class ChildProcess {

    /**
     * Inner class for reading stdout of the child process and printing it onto the caller's stdout.
     */
    private class StdoutMonitor extends Thread {

        private StdoutMonitor() {}

        public void run() {
            String line = null;
            BufferedReader stdout = new BufferedReader(new InputStreamReader(_process.getInputStream()));
            try {
                // blocks until input found or process dead
                while ((line = stdout.readLine()) != null) {
                    if (!isSilent()) {
                        System.out.println(line);
                    }
                }
            } catch (IOException ioe) {
                if (!isSilent()) {
                    ioe.printStackTrace();
                }
            } finally {
                if (stdout != null)
                    try {
                        stdout.close();
                    } catch (IOException e) {}
            }
        }
    }

    /**
     * Inner class for reading stderr of the child process and printing it onto the caller's stderr.
     */
    private class StderrMonitor extends Thread {

        private StderrMonitor() {}

        public void run() {
            String line = null;
            BufferedReader stderr = new BufferedReader(new InputStreamReader(_process.getErrorStream()));
            try {
                // blocks until input found or process dead
                while ((line = stderr.readLine()) != null) {
                    if (!isSilent()) {
                        System.err.println(line);
                    }
                }
            } catch (IOException ioe) {
                if (!isSilent()) {
                    ioe.printStackTrace();
                }
            } finally {
                if (stderr != null)
                    try {
                        stderr.close();
                    } catch (IOException e) {}
            }
        }
    }

    /**
     * Constant indicating no timeout at all.
     */
    public static final long INFINITE_TIMEOUT = -1;

    /**
     * Constant indicating the exit value if the child process was destroyed due to a timeout.
     */
    public static final int DESTROYED_AFTER_TIMEOUT = -9898;

    /**
     * Constant indicating that the exit value was not yet set
     */
    private static final int NOT_SET_EXITVALUE = -9;

    /**
     * The command as an array of strings
     */
    private String _command[] = null;

    /**
     * The timeout (in milliseconds)
     */
    private long _timeout = INFINITE_TIMEOUT;

    /**
     * The interval for checking if the child process is still alive
     */
    private long _pollAliveInterval = 1000;

    /**
     * the effective child process
     */
    Process _process;

    /**
     * the exit value
     */
    private int _exitValue = NOT_SET_EXITVALUE;

    /**
     * The start time of the child process
     */
    private long _startTime;

    /**
     * debug option (default is false)
     */
    private boolean _debug = false;

    /**
     * silent flag
     */
    private boolean _silent = false;

    /**
     * Default constructor
     */
    public ChildProcess() {}

    /**
     * Constructor taking a command array as an argument
     * 
     * @param command
     *            The command to be executed, every token as array element.
     */
    public ChildProcess(String command[]) {
        setCommand(command);
    }

    /**
     * Constructor taking a command array and the timeout as an argument
     * 
     * @param command
     *            The command to be executed, every token as array element.
     * @param timeout
     *            in milliseconds. Special value: <code>INFINITE_TIMEOUT</code> indicates no timeout
     *            at all.
     */
    public ChildProcess(String command[], long timeout) {
        setCommand(command);
        setTimeout(timeout);
    }

    /**
     * Set the command array. This will override (but not overwrite) a previously set command
     */
    public void setCommand(String command[]) {
        _command = command;
    }

    /**
     * Returns the command array
     */
    public String[] getCommand() {
        return _command;
    }

    /**
     * Set the timeout (how long should the calling process wait for the child).
     * 
     * @param timeout
     *            in milliseconds. Special value: <code>INFINITE_TIMEOUT</code> indicates no timeout
     *            at all. This is the default.
     */
    public void setTimeout(long timeout) {
        _timeout = timeout;
    }

    /**
     * Returns the timeout in milliseconds.
     */
    public long getTimeout() {
        return _timeout;
    }

    /**
     * Set the debug flag.
     * <p>
     * Setting this to true will print the submitted command and an information if the child process
     * is destroyed after the timeout.
     */
    public void setDebug(boolean debug) {
        _debug = debug;
    }

    /**
     * Returns the debug flag
     */
    public boolean isDebug() {
        return _debug;
    }

    /**
     * Set the silent flag.
     * <p>
     * Setting this to true will suppress output of the called command.
     */
    public void setSilent(boolean silent) {
        _silent = silent;
    }

    /**
     * Returns the silent flag.
     */
    public boolean isSilent() {
        return _silent;
    }

    /**
     * Set the interval (in milliseconds) after which the subprocess is checked if it is still
     * alive. Defaults to 1000 ms.
     */
    public void setPollAliveInterval(long pollAliveInterval) {
        _pollAliveInterval = pollAliveInterval;
    }

    /**
     * Returns the interval (in milliseconds) after which the subprocess is checked if it is still
     * alive.
     */
    public long getPollAliveInterval() {
        return _pollAliveInterval;
    }

    /**
     * returns true if the timeout has expired
     */
    private boolean isTimeout() {
        boolean isTimeout = false;
        long currentTime = System.currentTimeMillis();
        long diff = 0;
        long timeout = getTimeout();
        if (timeout != INFINITE_TIMEOUT) {
            diff = currentTime - _startTime;
            if (diff > timeout) {
                isTimeout = true;
            }
        }
        return isTimeout;
    }

    /**
     * Start the child process
     */
    public int run() {
        try {
            // determine start time
            _startTime = System.currentTimeMillis();
            // start the process
            _process = Runtime.getRuntime().exec(getCommand());
            debugCommand();
            // handle stdout and stderr
            StdoutMonitor stdoutMonitor = new StdoutMonitor();
            stdoutMonitor.start();
            StderrMonitor stderrMonitor = new StderrMonitor();
            stderrMonitor.start();
            // run the subprocess as long as wanted
            while (!isTimeout() && isAlive()) {
                try {
                    Thread.sleep(getPollAliveInterval());
                } catch (InterruptedException ie) {
                    if (!isSilent()) {
                        ie.printStackTrace();
                    }
                }
            }
            // end properly
            if (isAlive()) { // sets the exit value in case process is dead
                destroy();
            } else {
                if (isDebug()) {
                    System.out.println("[ChildProcess] ended itself");
                }
            }
        } catch (IOException ioe) {
            if (!isSilent()) {
                ioe.printStackTrace();
            }
        }
        return getExitValue();
    }

    /**
     * The exit value
     */
    public int getExitValue() {
        return _exitValue;
    }

    private void setExitValue(int exitValue) {
        _exitValue = exitValue;
    }

    /**
     * Tests if the process is still alive
     */
    private boolean isAlive() {
        try {
            setExitValue(_process.exitValue());
            return false;
        } catch (IllegalThreadStateException itse) {
            return true;
        }
    }

    /**
     * Destroy the child process
     */
    private void destroy() {
        _process.destroy();
        setExitValue(DESTROYED_AFTER_TIMEOUT);
        if (isDebug()) {
            System.out.println("[ChildProcess] destroying because of timeout !");
        }
    }

    /**
     * Lists the submitted command (if so indicated)
     */
    private void debugCommand() {
        if (isDebug()) {
            String[] command = getCommand();
            if (command != null) {
                System.out.print("[ChildProcess] command '");
                for (int i = 0; i < command.length; i++) {
                    String commandPart = command[i];
                    if (i == 0) {
                        System.out.print(commandPart);
                    } else {
                        System.out.print(" " + commandPart);
                    }
                }
                System.out.println("' is now running...");
            }
        }
    }
}