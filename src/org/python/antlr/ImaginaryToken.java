package org.python.antlr;

import org.antlr.runtime.ClassicToken;

public class ImaginaryToken extends ClassicToken {

	protected int start;
	protected int stop;
    
    public ImaginaryToken(int ttype, String text) {
        super(ttype, text);
    }

	public int getStartIndex() {
		return start;
	}

	public void setStartIndex(int start) {
		this.start = start;
	}

	public int getStopIndex() {
		return stop;
	}

	public void setStopIndex(int stop) {
		this.stop = stop;
	}
}
