// Copyright © Corporation for National Research Initiatives
package org.python.rmi;
import java.rmi.*;

public class UnicastRemoteObject extends java.rmi.server.RemoteServer {
    Remote remote;

    public UnicastRemoteObject() throws RemoteException {
        this.remote = remote;
        java.rmi.server.UnicastRemoteObject.exportObject(remote);
    }

    private void readObject(java.io.ObjectInputStream in)
        throws java.io.IOException, java.lang.ClassNotFoundException {
        java.rmi.server.UnicastRemoteObject.exportObject(remote);
    }
}
