/*
 * Copyright 2000 Finn Bock
 *
 * This program contains material copyrighted by:
 * Copyright (c) 1997-2000 by Secret Labs AB.  All rights reserved.
 *
 * This version of the SRE library can be redistributed under CNRI's
 * Python 1.6 license.  For any other use, please contact Secret Labs
 * AB (info@pythonware.com).
 *
 * Portions of this engine have been developed in cooperation with
 * CNRI.  Hewlett-Packard provided funding for 1.6 integration and
 * other compatibility work.
 */

// Last updated to _sre.c: 2.52

package org.python.modules.sre;

import java.util.*;

public class SRE_STATE {
    /* illegal opcode */
    public static final int SRE_ERROR_ILLEGAL = -1;

    /* illegal state */
    public static final int SRE_ERROR_STATE   = -2;

    /* runaway recursion */
    public static final int SRE_ERROR_RECURSION_LIMIT = -3;

    public static final int SRE_OP_FAILURE                 = 0;
    public static final int SRE_OP_SUCCESS                 = 1;
    public static final int SRE_OP_ANY                     = 2;
    public static final int SRE_OP_ANY_ALL                 = 3;
    public static final int SRE_OP_ASSERT                  = 4;
    public static final int SRE_OP_ASSERT_NOT              = 5;
    public static final int SRE_OP_AT                      = 6;
    public static final int SRE_OP_BRANCH                  = 7;
    public static final int SRE_OP_CALL                    = 8;
    public static final int SRE_OP_CATEGORY                = 9;
    public static final int SRE_OP_CHARSET                 = 10;
    public static final int SRE_OP_BIGCHARSET              = 11;
    public static final int SRE_OP_GROUPREF                = 12;
    public static final int SRE_OP_GROUPREF_IGNORE         = 13;
    public static final int SRE_OP_IN                      = 14;
    public static final int SRE_OP_IN_IGNORE               = 15;
    public static final int SRE_OP_INFO                    = 16;
    public static final int SRE_OP_JUMP                    = 17;
    public static final int SRE_OP_LITERAL                 = 18;
    public static final int SRE_OP_LITERAL_IGNORE          = 19;
    public static final int SRE_OP_MARK                    = 20;
    public static final int SRE_OP_MAX_UNTIL               = 21;
    public static final int SRE_OP_MIN_UNTIL               = 22;
    public static final int SRE_OP_NOT_LITERAL             = 23;
    public static final int SRE_OP_NOT_LITERAL_IGNORE      = 24;
    public static final int SRE_OP_NEGATE                  = 25;
    public static final int SRE_OP_RANGE                   = 26;
    public static final int SRE_OP_REPEAT                  = 27;
    public static final int SRE_OP_REPEAT_ONE              = 28;
    public static final int SRE_OP_SUBPATTERN              = 29;

    public static final int SRE_AT_BEGINNING               = 0;
    public static final int SRE_AT_BEGINNING_LINE          = 1;
    public static final int SRE_AT_BEGINNING_STRING        = 2;
    public static final int SRE_AT_BOUNDARY                = 3;
    public static final int SRE_AT_NON_BOUNDARY            = 4;
    public static final int SRE_AT_END                     = 5;
    public static final int SRE_AT_END_LINE                = 6;
    public static final int SRE_AT_END_STRING              = 7;
    public static final int SRE_AT_LOC_BOUNDARY            = 8;
    public static final int SRE_AT_LOC_NON_BOUNDARY        = 9;
    public static final int SRE_AT_UNI_BOUNDARY            = 10;
    public static final int SRE_AT_UNI_NON_BOUNDARY        = 11;

    public static final int SRE_CATEGORY_DIGIT             = 0;
    public static final int SRE_CATEGORY_NOT_DIGIT         = 1;
    public static final int SRE_CATEGORY_SPACE             = 2;
    public static final int SRE_CATEGORY_NOT_SPACE         = 3;
    public static final int SRE_CATEGORY_WORD              = 4;
    public static final int SRE_CATEGORY_NOT_WORD          = 5;
    public static final int SRE_CATEGORY_LINEBREAK         = 6;
    public static final int SRE_CATEGORY_NOT_LINEBREAK     = 7;
    public static final int SRE_CATEGORY_LOC_WORD          = 8;
    public static final int SRE_CATEGORY_LOC_NOT_WORD      = 9;
    public static final int SRE_CATEGORY_UNI_DIGIT         = 10;
    public static final int SRE_CATEGORY_UNI_NOT_DIGIT     = 11;
    public static final int SRE_CATEGORY_UNI_SPACE         = 12;
    public static final int SRE_CATEGORY_UNI_NOT_SPACE     = 13;
    public static final int SRE_CATEGORY_UNI_WORD          = 14;
    public static final int SRE_CATEGORY_UNI_NOT_WORD      = 15;
    public static final int SRE_CATEGORY_UNI_LINEBREAK     = 16;
    public static final int SRE_CATEGORY_UNI_NOT_LINEBREAK = 17;

    public static final int SRE_FLAG_TEMPLATE              = 1;
    public static final int SRE_FLAG_IGNORECASE            = 2;
    public static final int SRE_FLAG_LOCALE                = 4;
    public static final int SRE_FLAG_MULTILINE             = 8;
    public static final int SRE_FLAG_DOTALL                = 16;
    public static final int SRE_FLAG_UNICODE               = 32;
    public static final int SRE_FLAG_VERBOSE               = 64;

    public static final int SRE_INFO_PREFIX                = 1;
    public static final int SRE_INFO_LITERAL               = 2;
    public static final int SRE_INFO_CHARSET               = 4;


    public static final int USE_RECURSION_LIMIT = 2000;


    /* string pointers */
    int ptr; /* current position (also end of current slice) */
    int beginning; /* start of original string */
    int start; /* start of current slice */
    int end; /* end of original string */

    /* attributes for the match object */
    char[] str;
    int pos;
    int endpos;

    /* character size */
    int charsize;

    /* registers */
    int lastindex;
    int lastmark;

    /* FIXME: <fl> should be dynamically allocated! */
    int[] mark = new int[200];

    /* dynamically allocated stuff */
    int[] mark_stack;
    int mark_stack_size;
    int mark_stack_base;

    SRE_REPEAT repeat; /* current repeat context */

    /* debugging */
    int maxlevel;

    /* duplicated from the PatternObject */
    int flags;




    public SRE_STATE(String str, int start, int end, int flags) {
        this.str = str.toCharArray();
        int size = str.length();

        this.charsize = 1;

        /* adjust boundaries */
        if (start < 0)
            start = 0;
        else if (start > size)
            start = size;

        if (end < 0)
            end = 0;
        else if (end > size)
            end = size;

        this.start = start;
        this.end = end;

        this.pos = start;
        this.endpos = end;

        state_reset();

        this.flags = flags;
    }



    private void mark_fini() {
        mark_stack = null;
        mark_stack_size = mark_stack_base = 0;
    }


    private void mark_save(int lo, int hi) {
        if (hi <= lo)
            return;

        int size = (hi - lo) + 1;

        int newsize = mark_stack_size;
        int minsize = mark_stack_base + size;

        int[] stack;

        if (newsize < minsize) {
            /* create new stack */
            if (newsize == 0) {
                newsize = 512;
                if (newsize < minsize)
                    newsize = minsize;
                //TRACE(0, ptr, "allocate stack " + newsize);
                stack = new int[newsize];
            } else {
                /* grow the stack */
                while (newsize < minsize)
                    newsize += newsize;
                //TRACE(0, ptr, "grow stack to " + newsize);
                stack = new int[newsize];
                System.arraycopy(mark_stack, 0, stack, 0, mark_stack.length);
            }
            mark_stack = stack;
            mark_stack_size = newsize;
        }

        //TRACE(0, ptr, "copy " + lo + ":" + hi + " to " + mark_stack_base + " (" + size + ")");

        System.arraycopy(mark, lo, mark_stack, mark_stack_base, size);

        mark_stack_base += size;
    }


    private void mark_restore(int lo, int hi) {
        if (hi <= lo)
            return;

        int size = (hi - lo) + 1;

        mark_stack_base -= size;

        //TRACE(0, ptr, "copy " + lo + ":" + hi +  " from " + mark_stack_base);

        System.arraycopy(mark_stack, mark_stack_base, mark, lo, size);
    }



    final boolean SRE_AT(int ptr, char at) {
        /* check if pointer is at given position.  return 1 if so, 0
           otherwise */

        boolean thiS, that;

        switch (at) {
        case SRE_AT_BEGINNING:
        case SRE_AT_BEGINNING_STRING:
            return ptr == beginning;

        case SRE_AT_BEGINNING_LINE:
            return (ptr == beginning || SRE_IS_LINEBREAK(str[ptr-1]));

        case SRE_AT_END:
            return (ptr+1 == end && SRE_IS_LINEBREAK(str[ptr])) || ptr == end;

        case SRE_AT_END_LINE:
            return ptr == end || SRE_IS_LINEBREAK(str[ptr]);

        case SRE_AT_END_STRING:
            return ptr == end;

        case SRE_AT_BOUNDARY:
            /* word boundary */
            if (beginning == end)
                return false;
            that = (ptr > beginning) ? SRE_IS_WORD(str[ptr-1]) : false;
            thiS = (ptr < end) ? SRE_IS_WORD(str[ptr]) : false;
            return thiS != that;

        case SRE_AT_NON_BOUNDARY:
            /* word non-boundary */
            if (beginning == end)
                return false;
            that = (ptr > beginning) ? SRE_IS_WORD(str[ptr-1]) : false;
            thiS = (ptr < end) ? SRE_IS_WORD(str[ptr]) : false;
            return thiS == that;

        case SRE_AT_LOC_BOUNDARY:
        case SRE_AT_UNI_BOUNDARY:
            if (beginning == end)
                return false;
            that = (ptr > beginning) ? SRE_LOC_IS_WORD(str[ptr-1]) : false;
            thiS = (ptr < end) ? SRE_LOC_IS_WORD(str[ptr]) : false;
            return thiS != that;

        case SRE_AT_LOC_NON_BOUNDARY:
        case SRE_AT_UNI_NON_BOUNDARY:
            /* word non-boundary */
            if (beginning == end)
                return false;
            that = (ptr > beginning) ? SRE_LOC_IS_WORD(str[ptr-1]) : false;
            thiS = (ptr < end) ? SRE_LOC_IS_WORD(str[ptr]) : false;
            return thiS == that;
        }

        return false;
    }




    final boolean SRE_CHARSET(char[] set, int setidx, char ch) {
        /* check if character is a member of the given set. return 1 if
           so, 0 otherwise */

        boolean ok = true;

        for (;;) {
            switch (set[setidx++]) {

            case SRE_OP_LITERAL:
                /* <LITERAL> <code> */
                if (ch == set[setidx])
                    return ok;
                setidx++;
                break;

            case SRE_OP_RANGE:
                /* <RANGE> <lower> <upper> */
                if (set[setidx] <= ch && ch <= set[setidx+1])
                    return ok;
                setidx += 2;
                break;

            case SRE_OP_CHARSET:
                /* <CHARSET> <bitmap> (16 bits per code word) */
                if (ch < 256 &&
                            (set[setidx + (ch >> 4)] & (1 << (ch & 15))) != 0)
                    return ok;
                setidx += 16;
                break;

            case SRE_OP_BIGCHARSET:
                /* <BIGCHARSET> <blockcount> <256 blockindices> <blocks> */
                int count = set[setidx++];
                int block = set[ch >> 8];
                setidx += 128;
                int idx = block*16 + ((ch & 255)>>4);
                if ((set[setidx + idx] & (1 << (ch & 15))) != 0)
                    return ok;
                setidx += count*16;
                break;

            case SRE_OP_CATEGORY:
                /* <CATEGORY> <code> */
                if (sre_category(set[setidx], ch))
                    return ok;
                setidx++;
                break;

            case SRE_OP_NEGATE:
                ok = !ok;
                break;

            case SRE_OP_FAILURE:
                return !ok;

            default:
                /* internal error -- there's not much we can do about it
                   here, so let's just pretend it didn't match... */
                return false;
            }
        }
    }




    private int SRE_COUNT(char[] pattern, int pidx, int maxcount, int level) {
        char chr;
        int ptr = this.ptr;
        int end = this.end;
        int i;

        /* adjust end */
        if (maxcount < end - ptr && maxcount != 65535)
            end = ptr + maxcount;

        switch (pattern[pidx]) {

        case SRE_OP_ANY:
            /* repeated dot wildcard. */
            //TRACE(pidx, ptr, "COUNT ANY");
            while (ptr < end && !SRE_IS_LINEBREAK(str[ptr]))
                ptr++;
            break;

        case SRE_OP_ANY_ALL:
            /* repeated dot wildcare.  skip to the end of the target
               string, and backtrack from there */
            //TRACE(pidx, ptr, "COUNT ANY_ALL");
            ptr = end;
            break;

        case SRE_OP_LITERAL:
            /* repeated literal */
            chr = pattern[pidx+1];
            //TRACE(pidx, ptr, "COUNT LITERAL " + (int) chr);
            while (ptr < end && str[ptr] == chr)
                ptr++;
            break;

        case SRE_OP_LITERAL_IGNORE:
            /* repeated literal */
            chr = pattern[pidx+1];
            //TRACE(pidx, ptr, "COUNT LITERAL_IGNORE " + (int) chr);
            while (ptr < end && lower(str[ptr]) == chr)
                ptr++;
            break;

        case SRE_OP_NOT_LITERAL:
            /* repeated non-literal */
            chr = pattern[pidx+1];
            //TRACE(pidx, ptr, "COUNT NOT_LITERAL " + (int) chr);
            while (ptr < end && str[ptr] != chr)
                ptr++;
            break;

        case SRE_OP_NOT_LITERAL_IGNORE:
            /* repeated non-literal */
            chr = pattern[pidx+1];
            //TRACE(pidx, ptr, "COUNT NOT_LITERAL_IGNORE " + (int) chr);
            while (ptr < end && lower(str[ptr]) != chr)
                ptr++;
            break;

        case SRE_OP_IN:
            /* repeated set */
            //TRACE(pidx, ptr, "COUNT IN");
            while (ptr < end && SRE_CHARSET(pattern, pidx + 2, str[ptr]))
                ptr++;
            break;

        default:
            /* repeated single character pattern */
            //TRACE(pidx, ptr, "COUNT SUBPATTERN");
            while (this.ptr < end) {
                i = SRE_MATCH(pattern, pidx, level);
                if (i < 0)
                    return i;
                if (i == 0)
                    break;
            }
            return this.ptr - ptr;
        }

        return ptr - this.ptr;
    }



    final int SRE_MATCH(char[] pattern, int pidx, int level) {
        /* check if string matches the given pattern.  returns <0 for
           error, 0 for failure, and 1 for success */

        int end = this.end;
        int ptr = this.ptr;
        int i, count;
        char chr;

        int lastmark;

        //TRACE(pidx, ptr, "ENTER " + level);

        if (level > USE_RECURSION_LIMIT)
           return SRE_ERROR_RECURSION_LIMIT;

        if (pattern[pidx] == SRE_OP_INFO) {
            /* optimization info block */
            /* args: <1=skip> <2=flags> <3=min> ... */
            if (pattern[pidx+3] != 0 && (end - ptr) < pattern[pidx+3]) {
                return 0;
            }
            pidx += pattern[pidx+1] + 1;
        }


        for (;;) {

            switch (pattern[pidx++]) {

            case SRE_OP_FAILURE:
                /* immediate failure */
                //TRACE(pidx, ptr, "FAILURE");
                return 0;

            case SRE_OP_SUCCESS:
                /* end of pattern */
                //TRACE(pidx, ptr, "SUCCESS");
                this.ptr = ptr;
                return 1;

            case SRE_OP_AT:
                /* match at given position */
                /* <AT> <code> */
                //TRACE(pidx, ptr, "AT " + (int) pattern[pidx]);
                if (!SRE_AT(ptr, pattern[pidx]))
                    return 0;
                pidx++;
                break;

            case SRE_OP_CATEGORY:
                /* match at given category */
                /* <CATEGORY> <code> */
                //TRACE(pidx, ptr, "CATEGORY " + (int)pattern[pidx]);

                if (ptr >= end || !sre_category(pattern[pidx], str[ptr]))
                    return 0;

                pidx++;
                ptr++;
                break;

            case SRE_OP_LITERAL:
                /* match literal character */
                /* <LITERAL> <code> */
                //TRACE(pidx, ptr, "LITERAL " + (int) pattern[pidx]);

                if (ptr >= end || str[ptr] != pattern[pidx])
                    return 0;
                pidx++;
                ptr++;
                break;

            case SRE_OP_NOT_LITERAL:
                /* match anything that is not literal character */
                /* args: <code> */
                //TRACE(pidx, ptr, "NOT_LITERAL " + (int) pattern[pidx]);
                if (ptr >= end || str[ptr] == pattern[pidx])
                    return 0;
                pidx++;
                ptr++;
                break;

            case SRE_OP_ANY:
                /* match anything */
                //TRACE(pidx, ptr, "ANY");
                if (ptr >= end || SRE_IS_LINEBREAK(str[ptr]))
                    return 0;
                ptr++;
                break;

            case SRE_OP_ANY_ALL:
                /* match anything */
                /* <ANY_ALL> */
                //TRACE(pidx, ptr, "ANY_ALL");
                if (ptr >= end)
                    return 0;
                ptr++;
                break;

            case SRE_OP_IN:
                /* match set member (or non_member) */
                /* <IN> <skip> <set> */
                //TRACE(pidx, ptr, "IN");
                if (ptr >= end || !SRE_CHARSET(pattern, pidx + 1, str[ptr]))
                    return 0;
                pidx += (int)pattern[pidx];
                ptr++;
                break;

            case SRE_OP_GROUPREF:
                /* match backreference */
                i = pattern[pidx];
                //TRACE(pidx, ptr, "GROUPREF " + i);
                int p = mark[i+i];
                int e = mark[i+i+1];
                if (p == -1 || e == -1 || e < p)
                    return 0;
                while (p < e) {
                    if (ptr >= end || str[ptr] != str[p])
                        return 0;
                    p++;
                    ptr++;
                }
                pidx++;
                break;

            case SRE_OP_GROUPREF_IGNORE:
                /* match backreference */
                i = pattern[pidx];
                //TRACE(pidx, ptr, "GROUPREF_IGNORE " + i);
                p = mark[i+i];
                e = mark[i+i+1];
                if (p == -1 || e == -1 || e < p)
                    return 0;
                while (p < e) {
                    if (ptr >= end || lower(str[ptr]) != lower(str[p]))
                        return 0;
                    p++;
                    ptr++;
                }
                pidx++;
                break;

            case SRE_OP_LITERAL_IGNORE:
                //TRACE(pidx, ptr, "LITERAL_IGNORE " + (int) pattern[pidx]);
                if (ptr >= end || lower(str[ptr]) != lower(pattern[pidx]))
                    return 0;
                pidx++;
                ptr++;
                break;

            case SRE_OP_NOT_LITERAL_IGNORE:
                //TRACE(pidx, ptr, "NOT_LITERAL_IGNORE " + (int) pattern[pidx]);
                if (ptr >= end || lower(str[ptr]) == lower(pattern[pidx]))
                    return 0;
                pidx++;
                ptr++;
                break;

            case SRE_OP_IN_IGNORE:
                //TRACE(pidx, ptr, "IN_IGNORE");
                if (ptr >= end ||
                        !SRE_CHARSET(pattern, pidx + 1, lower(str[ptr])))
                    return 0;
                pidx += (int)pattern[pidx];
                ptr++;
                break;

            case SRE_OP_MARK:
                /* set mark */
                /* <MARK> <gid> */
                //TRACE(pidx, ptr, "MARK " + (int) pattern[pidx]);
                i = pattern[pidx];
                if ((i & 1) != 0)
                    lastindex = i / 2 + 1;
                if (i > this.lastmark)
                    this.lastmark = i;
                mark[i] = ptr;
                pidx++;
                break;

            case SRE_OP_JUMP:
            case SRE_OP_INFO:
                /* jump forward */
                /* <JUMP> <offset> */
                //TRACE(pidx, ptr, "JUMP " + (int) pattern[pidx]);
                pidx += (int)pattern[pidx];
                break;

            case SRE_OP_ASSERT:
                /* assert subpattern */
                /* args: <skip> <back> <pattern> */
                //TRACE(pidx, ptr, "ASSERT " + (int) pattern[pidx+1]);

                this.ptr = ptr - pattern[pidx + 1];
                if (this.ptr < this.beginning)
                    return 0;
                i = SRE_MATCH(pattern, pidx + 2, level + 1);
                if (i <= 0)
                    return i;
                pidx += pattern[pidx];
                break;

            case SRE_OP_ASSERT_NOT:
                /* assert not subpattern */
                /* args: <skip> <pattern> */
                //TRACE(pidx, ptr, "ASSERT_NOT " + (int) pattern[pidx]);
                this.ptr = ptr - pattern[pidx + 1];
                if (this.ptr >= this.beginning) {
                    i = SRE_MATCH(pattern, pidx + 2, level + 1);
                    if (i < 0)
                        return i;
                    if (i != 0)
                        return 0;
                }
                pidx += pattern[pidx];
                break;

            case SRE_OP_BRANCH:
                /* try an alternate branch */
                /* <BRANCH> <0=skip> code <JUMP> ... <NULL> */
                //TRACE(pidx, ptr, "BRANCH");
                lastmark = this.lastmark;
                for (; pattern[pidx] != 0; pidx += pattern[pidx]) {
                    if (pattern[pidx+1] == SRE_OP_LITERAL &&
                        (ptr >= end || str[ptr] != pattern[pidx+2]))
                        continue;
                    if (pattern[pidx+1] == SRE_OP_IN && (ptr >= end ||
                                !SRE_CHARSET(pattern, pidx + 3, str[ptr])))
                        continue;
                    this.ptr = ptr;
                    i = SRE_MATCH(pattern, pidx + 1, level + 1);
                    if (i != 0)
                        return i;
                    while (this.lastmark > lastmark)
                        mark[this.lastmark--] = -1;
                }

                return 0;

            case SRE_OP_REPEAT_ONE:
                /* match repeated sequence (maximizing regexp) */

                /* this operator only works if the repeated item is
                   exactly one character wide, and we're not already
                   collecting backtracking points.  for other cases,
                   use the MAX_REPEAT operator */

                /* <REPEAT_ONE> <skip> <1=min> <2=max> item <SUCCESS> tail */

                int mincount = pattern[pidx+1];

                //TRACE(pidx, ptr, "REPEAT_ONE " + mincount + " " + (int)pattern[pidx+2]);
                if (ptr + mincount > end)
                    return 0; /* cannot match */

                this.ptr = ptr;

                count = SRE_COUNT(pattern, pidx + 3, pattern[pidx+2],
                                  level + 1);
                if (count < 0)
                    return count;

                ptr += count;

                /* when we arrive here, count contains the number of
                   matches, and ptr points to the tail of the target
                   string.  check if the rest of the pattern matches,
                   and backtrack if not. */

                if (count < mincount)
                    return 0;

                if (pattern[pidx + pattern[pidx]] == SRE_OP_SUCCESS) {
                    /* tail is empty.  we're finished */
                    this.ptr = ptr;
                    return 1;

                } else if (pattern[pidx + pattern[pidx]] == SRE_OP_LITERAL) {
                    /* tail starts with a literal. skip positions where
                       the rest of the pattern cannot possibly match */
                    chr = pattern[pidx + pattern[pidx]+1];
                    for (;;) {
                        while (count >= mincount &&
                               (ptr >= end || str[ptr] != chr)) {
                            ptr--;
                            count--;
                        }
                        if (count < mincount)
                            break;
                        this.ptr = ptr;
                        i = SRE_MATCH(pattern, pidx + pattern[pidx],
                                     level + 1);
                        if (i != 0)
                            return 1;
                        ptr--;
                        count--;
                    }

                } else {
                    /* general case */
                    lastmark = this.lastmark;
                    while (count >= mincount) {
                        this.ptr = ptr;
                        i = SRE_MATCH(pattern, pidx + pattern[pidx],
                                      level + 1);
                        if (i != 0)
                            return i;
                        ptr--;
                        count--;
                        while (this.lastmark > lastmark)
                            mark[this.lastmark--] = -1;
                    }
                }
                return 0;


            case SRE_OP_REPEAT:
                /* create repeat context.  all the hard work is done
                   by the UNTIL operator (MAX_UNTIL, MIN_UNTIL) */
                /* <REPEAT> <skip> <1=min> <2=max> item <UNTIL> tail */

                //TRACE(pidx, ptr, "REPEAT " + (int)pattern[pidx+1] + " " + (int)pattern[pidx+2]);

                SRE_REPEAT rep = new SRE_REPEAT(repeat);
                rep.count = -1;
                rep.pidx = pidx;
                repeat = rep;

                this.ptr = ptr;
                i = SRE_MATCH(pattern, pidx + pattern[pidx], level + 1);

                repeat = rep.prev;
                return i;



            case SRE_OP_MAX_UNTIL:
                /* maximizing repeat */
                /* <REPEAT> <skip> <1=min> <2=max> item <MAX_UNTIL> tail */

                /* FIXME: we probably need to deal with zero-width
                   matches in here... */

                SRE_REPEAT rp = this.repeat;
                if (rp == null)
                    return SRE_ERROR_STATE;

                this.ptr = ptr;

                count = rp.count + 1;

                //TRACE(pidx, ptr, "MAX_UNTIL " + count);

                if (count < pattern[rp.pidx + 1]) {
                    /* not enough matches */

                    rp.count = count;
                    i = SRE_MATCH(pattern, rp.pidx + 3, level + 1);
                    if (i != 0)
                        return i;
                    rp.count = count - 1;
                    this.ptr = ptr;
                    return 0;
                }

                if (count < pattern[rp.pidx+2] ||
                                            pattern[rp.pidx+2] == 65535) {
                    /* we may have enough matches, but if we can
                       match another item, do so */
                    rp.count = count;
                    lastmark = this.lastmark;
                    mark_save(0, lastmark);
                    /* RECURSIVE */
                    i = SRE_MATCH(pattern, rp.pidx + 3, level + 1);
                    if (i != 0)
                        return i;
                    mark_restore(0, lastmark);
                    this.lastmark = lastmark;
                    rp.count = count - 1;
                    this.ptr = ptr;
                }

                /* cannot match more repeated items here.  make sure the
                   tail matches */
                this.repeat = rp.prev;
                /* RECURSIVE */
                i = SRE_MATCH(pattern, pidx, level + 1);
                if (i != 0)
                    return i;
                this.repeat = rp;
                this.ptr = ptr;
                return 0;

            case SRE_OP_MIN_UNTIL:
                /* minimizing repeat */
                /* <REPEAT> <skip> <1=min> <2=max> item <MIN_UNTIL> tail */

                rp = this.repeat;
                if (rp == null)
                    return SRE_ERROR_STATE;

                count = rp.count + 1;

                //TRACE(pidx, ptr, "MIN_UNTIL " + count + " " + rp.pidx);

                this.ptr = ptr;

                if (count < pattern[rp.pidx + 1]) {
                    /* not enough matches */
                    rp.count = count;
                    /* RECURSIVE */
                    i = SRE_MATCH(pattern, rp.pidx + 3, level + 1);
                    if (i != 0)
                        return i;
                    rp.count = count-1;
                    this.ptr = ptr;
                    return 0;
                }

                /* see if the tail matches */
                this.repeat = rp.prev;
                i = SRE_MATCH(pattern, pidx, level + 1);
                if (i != 0)
                    return i;

                this.ptr = ptr;
                this.repeat = rp;

                if (count >= pattern[rp.pidx+2] &&
                                                pattern[rp.pidx+2] != 65535)
                    return 0;

                rp.count = count;
                /* RECURSIVE */
                i = SRE_MATCH(pattern, rp.pidx + 3, level + 1);
                if (i != 0)
                    return i;
                rp.count = count - 1;
                this.ptr = ptr;
                return 0;


            default:
                //TRACE(pidx, ptr, "UNKNOWN " + (int) pattern[pidx-1]);
                return SRE_ERROR_ILLEGAL;
            }
        }

        //return SRE_ERROR_ILLEGAL;
    }




    int SRE_SEARCH(char[] pattern, int pidx) {
        int ptr = this.start;
        int end = this.end;
        int status = 0;
        int prefix_len = 0;
        int prefix_skip = 0;
        int prefix = 0;
        int charset = 0;
        int overlap = 0;
        int flags = 0;

        if (pattern[pidx] == SRE_OP_INFO) {
            /* optimization info block */
            /* <INFO> <1=skip> <2=flags> <3=min> <4=max> <5=prefix info>  */

            flags = pattern[pidx+2];

            if (pattern[pidx+3] > 0) {
                /* adjust end point (but make sure we leave at least one
                   character in there, so literal search will work) */
                end -= pattern[pidx+3]-1;
                if (end <= ptr)
                    end = ptr; // FBO
            }

            if ((flags & SRE_INFO_PREFIX) != 0) {
                /* pattern starts with a known prefix */
                /* <length> <skip> <prefix data> <overlap data> */
                prefix_len = pattern[pidx+5];
                prefix_skip = pattern[pidx+6];
                prefix = pidx + 7;
                overlap = prefix + prefix_len - 1;
            } else if ((flags & SRE_INFO_CHARSET) != 0) {
                /* pattern starts with a character from a known set */
                /* <charset> */
                charset = pidx + 5;
            }

            pidx += 1 + pattern[pidx+1];
        }


        if (prefix_len > 1) {
            /* pattern starts with a known prefix.  use the overlap
               table to skip forward as fast as we possibly can */
            int i = 0;
            end = this.end;
            while (ptr < end) {
                for (;;) {
                    if (str[ptr] != pattern[prefix+i]) {
                        if (i == 0)
                            break;
                        else
                            i = pattern[overlap+i];
                    } else {
                        if (++i == prefix_len) {
                            /* found a potential match */
                            //TRACE(pidx, ptr, "SEARCH SCAN " + prefix_skip + " " + prefix_len);
                            this.start = ptr + 1 - prefix_len;
                            this.ptr = ptr + 1 - prefix_len + prefix_skip;
                            if ((flags & SRE_INFO_LITERAL) != 0)
                                return 1; /* we got all of it */
                            status = SRE_MATCH(pattern,
                                               pidx + 2*prefix_skip, 1);
                            if (status != 0)
                                return status;
                            /* close but no cigar -- try again */
                            i = pattern[overlap + i];
                        }
                        break;
                    }

                }
                ptr++;
            }
            return 0;
        }

        if (pattern[pidx] == SRE_OP_LITERAL) {
            /* pattern starts with a literal */
            char chr = pattern[pidx + 1];
            end = this.end;
            for (;;) {
                while (ptr < end && str[ptr] != chr)
                    ptr++;
                if (ptr == end)
                    return 0;
                //TRACE(pidx, ptr, "SEARCH LITERAL");
                this.start = ptr;
                this.ptr = ++ptr;
                if ((flags & SRE_INFO_LITERAL) != 0)
                    return 1;
                status = SRE_MATCH(pattern, pidx + 2, 1);
                if (status != 0)
                    break;
            }

        } else if (charset != 0) {
            /* pattern starts with a character from a known set */
            end = this.end;
            for (;;) {
                while (ptr < end && !SRE_CHARSET(pattern, charset, str[ptr]))
                    ptr++;
                if (ptr == end)
                    return 0;
                //TRACE(pidx, ptr, "SEARCH CHARSET");
                this.start = ptr;
                this.ptr = ptr;
                status = SRE_MATCH(pattern, pidx, 1);
                if (status != 0)
                    break;
                ptr++;
            }

        } else {
            /* general case */
            while (ptr <= end) {
                //TRACE(pidx, ptr, "SEARCH");
                this.start = this.ptr = ptr++;
                status = SRE_MATCH(pattern, pidx, 1);
                if (status != 0)
                    break;
            }
        }

        return status;
    }


    final boolean sre_category(char category, char ch) {
        switch (category) {

        case SRE_CATEGORY_DIGIT:
            return SRE_IS_DIGIT(ch);
        case SRE_CATEGORY_NOT_DIGIT:
            return ! SRE_IS_DIGIT(ch);

        case SRE_CATEGORY_SPACE:
            return SRE_IS_SPACE(ch);
        case SRE_CATEGORY_NOT_SPACE:
            return ! SRE_IS_SPACE(ch);

        case SRE_CATEGORY_WORD:
            return SRE_IS_WORD(ch);
        case SRE_CATEGORY_NOT_WORD:
            return ! SRE_IS_WORD(ch);

        case SRE_CATEGORY_LINEBREAK:
            return SRE_IS_LINEBREAK(ch);
        case SRE_CATEGORY_NOT_LINEBREAK:
            return ! SRE_IS_LINEBREAK(ch);

        case SRE_CATEGORY_LOC_WORD:
            return SRE_LOC_IS_WORD(ch);
        case SRE_CATEGORY_LOC_NOT_WORD:
            return ! SRE_LOC_IS_WORD(ch);


        case SRE_CATEGORY_UNI_DIGIT:
            return Character.isDigit(ch);
        case SRE_CATEGORY_UNI_NOT_DIGIT:
            return !Character.isDigit(ch);

        case SRE_CATEGORY_UNI_SPACE:
            return Character.isWhitespace(ch);
        case SRE_CATEGORY_UNI_NOT_SPACE:
            return !Character.isWhitespace(ch);

        case SRE_CATEGORY_UNI_WORD:
            return Character.isLetterOrDigit(ch) || ch == '_';
        case SRE_CATEGORY_UNI_NOT_WORD:
            return ! (Character.isLetterOrDigit(ch) || ch == '_');

        case SRE_CATEGORY_UNI_LINEBREAK:
           return SRE_UNI_IS_LINEBREAK(ch);
        case SRE_CATEGORY_UNI_NOT_LINEBREAK:
           return ! SRE_UNI_IS_LINEBREAK(ch);

        }
        return false;
    }


    /* default character predicates (run sre_chars.py to regenerate tables) */

    static final int SRE_DIGIT_MASK = 1;
    static final int SRE_SPACE_MASK = 2;
    static final int SRE_LINEBREAK_MASK = 4;
    static final int SRE_ALNUM_MASK = 8;
    static final int SRE_WORD_MASK  = 16;

    static byte[] sre_char_info = new byte[] {
        0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 6, 2,
        2, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 25, 25, 25, 25, 25, 25, 25, 25,
        25, 25, 0, 0, 0, 0, 0, 0, 0, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24,
        24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 0, 0,
        0, 0, 16, 0, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24,
        24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 0, 0, 0, 0, 0 };

    static byte[] sre_char_lower = new byte[] {
        0, 1, 2, 3, 4, 5, 6, 7, 8, 9,
        10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26,
        27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43,
        44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60,
        61, 62, 63, 64, 97, 98, 99, 100, 101, 102, 103, 104, 105, 106, 107,
        108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118, 119, 120, 121,
        122, 91, 92, 93, 94, 95, 96, 97, 98, 99, 100, 101, 102, 103, 104, 105,
        106, 107, 108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118, 119,
        120, 121, 122, 123, 124, 125, 126, 127 };

    final boolean SRE_IS_DIGIT(char ch) {
        return ((ch) < 128 ?
                (sre_char_info[(ch)] & SRE_DIGIT_MASK) != 0 : false);
    }

    final boolean SRE_IS_SPACE(char ch) {
        return ((ch) < 128 ?
                (sre_char_info[(ch)] & SRE_SPACE_MASK) != 0 : false);
    }

    final boolean SRE_IS_WORD(char ch) {
        return ((ch) < 128 ?
                (sre_char_info[(ch)] & SRE_WORD_MASK) != 0 : false);
    }

    final boolean SRE_IS_LINEBREAK(char ch) {
        return ch == '\n';
    }

    final boolean SRE_LOC_IS_WORD(char ch) {
        return Character.isLetterOrDigit(ch) || ch == '_';
    }

    final boolean SRE_UNI_IS_LINEBREAK(char ch) {
        switch (ch) {
        case 0x000A: /* LINE FEED */
        case 0x000D: /* CARRIAGE RETURN */
        case 0x001C: /* FILE SEPARATOR */
        case 0x001D: /* GROUP SEPARATOR */
        case 0x001E: /* RECORD SEPARATOR */
        case 0x0085: /* NEXT LINE */
        case 0x2028: /* LINE SEPARATOR */
        case 0x2029: /* PARAGRAPH SEPARATOR */
            return true;
        default:
            return false;
        }
    }


    final char lower(char ch) {
        if ((flags & SRE_FLAG_LOCALE) != 0)
             return ((ch) < 256 ? Character.toLowerCase(ch) : ch);
        if ((flags & SRE_FLAG_UNICODE) != 0)
             return Character.toLowerCase(ch);
        return ((ch) < 128 ? (char)sre_char_lower[ch] : ch);
    }


    public static int getlower(int ch, int flags) {
        if ((flags & SRE_FLAG_LOCALE) != 0)
             return ((ch) < 256 ? Character.toLowerCase((char) ch) : ch);
        if ((flags & SRE_FLAG_UNICODE) != 0)
             return Character.toLowerCase((char)ch);
        return ((ch) < 128 ? (char)sre_char_lower[ch] : ch);
    }





    String getslice(int index, String string, boolean empty) {
        int i, j;

        index = (index - 1) * 2;

        if (string == null || mark[index] == -1 || mark[index+1] == -1) {
            if (empty) {
                /* want empty string */
                i = j = 0;
            } else {
                return null;
            }
        } else {
            i = mark[index];
            j = mark[index+1];
        }

        return string.substring(i, j);
    }




    void state_reset() {
        lastmark = 0;

        /* FIXME: dynamic! */
        for (int i = 0; i < mark.length; i++)
            mark[i] = -1;

        lastindex = -1;
        repeat = null;

        mark_fini();
    }


    private void TRACE(int pidx, int ptr, String string) {
        //System.out.println("      |" + pidx + "|" + ptr + ": " + string);
    }
}
