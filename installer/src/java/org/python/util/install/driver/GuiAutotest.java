package org.python.util.install.driver;

import java.awt.AWTException;
import java.awt.Robot;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.python.util.install.InstallerCommandLine;

public class GuiAutotest extends Autotest {

    private static final int _DEFAULT_DELAY = 500; // ms

    private Robot _robot;
    private List _keyActions;
    private boolean _waiting = false;

    protected GuiAutotest(InstallerCommandLine commandLine) throws IOException, DriverException {
        super(commandLine);
        _keyActions = new ArrayList();
        // pass in the target directory, verbositiy
        String[] args = new String[] { "-d", getTargetDir().getAbsolutePath() };
        setCommandLineArgs(args);
        addAdditionalArguments();
    }

    protected String getNameSuffix() {
        return "guiTest";
    }

    /**
     * add a normal key action (press and release)
     * 
     * @param keyCode
     */
    protected void addKeyAction(int keyCode) {
        KeyAction keyAction = new KeyAction(keyCode);
        addKeyAction(keyAction);
    }

    /**
     * add a normal key action (press and release), with a specific delay
     * 
     * @param keyCode
     * @param delay
     */
    protected void addKeyAction(int keyCode, int delay) {
        KeyAction keyAction = new KeyAction(keyCode, delay);
        addKeyAction(keyAction);
    }

    /**
     * add a key action (press and release) which waits before executing
     * 
     * @param keyCode
     */
    protected void addWaitingKeyAction(int keyCode) {
        KeyAction keyAction = new KeyAction(keyCode, true);
        addKeyAction(keyAction);
    }

    protected void setWaiting(boolean waiting) {
        _waiting = waiting;
    }

    /**
     * execute a single gui auto test
     * 
     * @throws DriverException
     */
    protected void execute() throws DriverException {
//        try {
//            _robot = new Robot();
//
//            System.out.println("waiting 2 seconds for the first gui ... please do not change focus");
//            _robot.delay(2000); // initial gui load
//
//            Iterator actionsIterator = _keyActions.iterator();
//            while (actionsIterator.hasNext()) {
//                KeyAction keyAction = (KeyAction) actionsIterator.next();
//                setWaiting(keyAction.isWait());
//                if (isWaiting()) {
//                    System.out.println("waiting for the installation to finish ...");
//                }
//                while (isWaiting()) {
//                    try {
//                        Thread.sleep(_DEFAULT_DELAY);
//                    } catch (InterruptedException e) {
//                        throw new DriverException(e);
//                    }
//                }
//                executeKeyAction(keyAction);
//            }
//        } catch (AWTException ae) {
//            throw new DriverException(ae);
//        }

    }

    /**
     * General KeyAction
     */
    protected static class KeyAction {
        private int _keyCode;
        private int _delay;
        private boolean _wait;

        /**
         * @param keyCode
         */
        protected KeyAction(int keyCode) {
            this(keyCode, _DEFAULT_DELAY);
        }

        /**
         * @param keyCode
         * @param delay in ms
         */
        protected KeyAction(int keyCode, int delay) {
            super();
            setKeyCode(keyCode);
            setDelay(delay);
        }

        /**
         * @param keyCode
         * @param wait true if we should wait before executing this key action
         */
        protected KeyAction(int keyCode, boolean wait) {
            this(keyCode, _DEFAULT_DELAY);
            setWait(wait);
        }

        protected void setKeyCode(int keyCode) {
            _keyCode = keyCode;
        }

        protected void setDelay(int delay) {
            _delay = delay;
        }

        protected int getDelay() {
            return _delay;
        }

        protected int getKeyCode() {
            return _keyCode;
        }

        protected void setWait(boolean wait) {
            _wait = wait;
        }

        protected boolean isWait() {
            return _wait;
        }
    }

    //
    // interface InstallationListener
    //

    public void progressFinished() {
        setWaiting(false);
    }

    //
    // private stuff
    //

    private boolean isWaiting() {
        return _waiting;
    }

    private void addKeyAction(KeyAction keyAction) {
        _keyActions.add(keyAction);
    }

    private void executeKeyAction(KeyAction keyAction) {
        _robot.delay(keyAction.getDelay());
        _robot.keyPress(keyAction.getKeyCode());
        _robot.delay(20); // delay was handled before press
        _robot.keyRelease(keyAction.getKeyCode());
    }

}
