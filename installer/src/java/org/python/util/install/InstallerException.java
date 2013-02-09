package org.python.util.install;

public class InstallerException extends RuntimeException {

    public InstallerException() {
        super();
    }

    public InstallerException(String message) {
        super(message);
    }

    public InstallerException(String message, Throwable cause) {
        super(message, cause);
    }

    public InstallerException(Throwable cause) {
        super(cause);
    }

}