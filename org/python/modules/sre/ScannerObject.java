package org.python.modules.sre;


public class ScannerObject {
    PatternObject pattern;
    String string;
    SRE_STATE state;

    public MatchObject match() {
        state.state_reset();
        state.ptr = state.start;

        int status = state.SRE_MATCH(pattern.code, 0, 1);
        MatchObject match = pattern._pattern_new_match(state, string, status);

        if (status == 0 || state.ptr == state.start)
            state.start = state.ptr + 1;
        else
            state.start = state.ptr;

        return match;
    }


    public MatchObject search() {
        state.state_reset();
        state.ptr = state.start;
    
        int status = state.SRE_SEARCH(pattern.code, 0);
        MatchObject match = pattern._pattern_new_match(state, string, status);

        if (status == 0 || state.ptr == state.start)
            state.start = state.ptr + 1;
        else
            state.start = state.ptr;

        return match;
    }
}




