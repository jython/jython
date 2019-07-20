package org.python.util.install;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private class OutputMonitor extends Thread {

        private final List<String> output = new ArrayList<>();

        private final InputStream inputStream;

        public OutputMonitor(InputStream inputStream) {
            this.inputStream = inputStream;
        }

        public void run() {
            String line;
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            try {
                // blocks until input found or process dead
                while ((line = reader.readLine()) != null) {
                    output.add(line);
                    if (!isSilent()) {
                        System.out.println(line);
                    }
                }
            } catch (IOException ioe) {
                if (!isSilent()) {
                    ioe.printStackTrace();
                }
            } finally {
                if (reader != null)
                    try {
                        reader.close();
                    } catch (IOException e) {}
            }
        }

        public List<String> getOutput() {
            return output;
        }
    }

    private List<String> stdout;
    private List<String> stderr;

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
     * Environment name-value pairs to add or remove (if null) at the start of {@link #run()}.
     */
    private Map<String, String> _environmentChanges = new HashMap<>();

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
     * path for current working directory of the child process
     */
    private Path _cwd;

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

    public void setCWD(Path cwd) { _cwd = cwd; }

    /**
     * Set or delete an environment variable for the subsequently run command.
     *
     * @param key name of variable
     * @param value new value or {@code null} to delete it.
     */
    public void putEnvironment(String key, String value) {
        _environmentChanges.put(key, value);
    }

    public Path getCWD() { return _cwd; }

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
            // Create and configure the process specification
            ProcessBuilder pb = new ProcessBuilder();
            pb.command(getCommand());
            if (getCWD() != null) {
                pb.directory(getCWD().toFile());
            }
            // Adjust the environment variables from the default System.getenv()
            for (Map.Entry<String, String> change : _environmentChanges.entrySet()) {
                if (change.getValue() == null) {
                    pb.environment().remove(change.getKey());
                } else {
                    pb.environment().put(change.getKey(), change.getValue());
                }
            }
            // Run the process with redirected input and error streams.
            debugCommand(pb);
            _process = pb.start();
            // handle stdout and stderr
            OutputMonitor stdoutMonitor = new OutputMonitor(_process.getInputStream());
            stdoutMonitor.start();
            OutputMonitor stderrMonitor = new OutputMonitor(_process.getErrorStream());
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
            stdout = stdoutMonitor.getOutput();
            stderr = stderrMonitor.getOutput();
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

    public List<String> getStdout() {
        return stdout;
    }

    public List<String> getStderr() {
        return stderr;
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
    private void debugCommand(ProcessBuilder pb) {
        if (isDebug()) {
            System.out.print("[ChildProcess] command = ");
            System.out.println(pb.command());
            System.out.print("[ChildProcess] environment = ");
            System.out.println(pb.environment());
            System.out.print("[ChildProcess] working directory = ");
            System.out.println(pb.directory());
        }
    }
}

