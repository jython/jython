

import java.util.*;
import java.awt.event.*;

public class test006j {
    ComponentListener listener = null;

    public void addComponentListener(ComponentListener l) throws TooManyListenersException {
	if (listener != null)
	    throw new TooManyListenersException();
	listener = l;
    }

    public void removeComponentListener(ComponentListener l) {
	listener = null;
    }


    public void fireComponentMoved(ComponentEvent evt) {
	if (listener != null)
	    listener.componentMoved(evt);
    }

    public void fireComponentHidden(ComponentEvent evt) {
	if (listener != null)
	    listener.componentHidden(evt);
    }

    public void fireComponentShown(ComponentEvent evt) {
	if (listener != null)
	    listener.componentShown(evt);
    }

}