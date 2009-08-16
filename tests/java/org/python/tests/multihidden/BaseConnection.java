package org.python.tests.multihidden;

/**
 * Derived from the Oracle JDBC connection classes for use in test_extending_multiple_hidden_classes
 * in test_java_visibility.
 */
public class BaseConnection {
    public static Connection newConnection() {
        return new Connection();
    }

    public String close() {
        return "base close";
    }
}

class Connection extends ConnectionWrapper implements SpecialConnection {

    public String close(int foo) {
        return "special close";
    }
}

class ConnectionWrapper extends BaseConnection {

// This method, plus the fact that Connection implements an interface with a different
// close, causes BaseConnection.close to be hidden in Connection because
// ConnectionWrapper is not public
    @Override
    public String close() {
        return "wrapper close";
    }
}
