

package org.python.modules;

/**
PyIOFiles encapsulates and optimise access to the different file
representation. Used by cPickle and marshall.
 */

public interface PyIOFile {

    public abstract void write(String str);
    // Usefull optimization since most data written are chars.

    public abstract void write(char str);

    public abstract void flush();

    public abstract String read(int len);
    // Usefull optimization since all readlines removes the
    // trainling newline.

    public abstract String readlineNoNl();
}