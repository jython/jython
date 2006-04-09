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

package org.python.modules.sre;

/* stack elements */

public class SRE_REPEAT {
    int count;
    int pidx;

    SRE_REPEAT prev;

    SRE_REPEAT(SRE_REPEAT prev) {
        this.prev = prev;
    }
}
