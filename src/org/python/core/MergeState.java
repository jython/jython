// Copyright 2002 Finn Bock

package org.python.core;

/**
 * The MergeState class is a java implementation of the sort created
 * Tim Peters and added to CPython2.3.
 *
 * The algorithm is described in details in the file
 * python/dist/src/Objects/listsort.txt in the CPython development CVS.
 */
class MergeState {
    /**
     * The maximum number of entries in a MergeState's pending-runs stack.
     * This is enough to sort arrays of size up to about
     *     32 * phi ** MAX_MERGE_PENDING
     * where phi ~= 1.618.  85 is ridiculously large enough, good for an
     * array with 2**64 elements.
     */
    static final int MAX_MERGE_PENDING = 85;

    /**
     * If a run wins MIN_GALLOP times in a row, we switch to galloping mode,
     * and stay there until both runs win less often than MIN_GALLOP
     * consecutive times.  See listsort.txt for more info.
     */
    static final int MIN_GALLOP = 8;

    /**
     * Initial temp array size
     */
    static final int MERGESTATE_TEMP_SIZE = 256;

    private KVPair[] a = new KVPair[MERGESTATE_TEMP_SIZE];

    private int[] base = new int[MAX_MERGE_PENDING];
    private int[] len = new int[MAX_MERGE_PENDING];

    private PyObject compare;
    private PyObject key;
    private boolean reverse;
    private int size;
    private int n;
    private PyList gOriginalList;
    private KVPair[] kvdata;
    
    MergeState(PyList list, PyObject compare, PyObject key, boolean reverse) {              
        if(compare != Py.None) {
            this.compare = compare;
        }            
        if(key != Py.None) {
            this.key = key;
        }                          
        this.reverse = reverse;
        this.n = 0;
        this.size = list.size();
        // not exactly desirable, but works for the moment
        this.kvdata = new KVPair[size];
               
        //resetting the list to find if any update is done after sorting
        this.gOriginalList = list;
        this.gOriginalList.gListAllocatedStatus = -1;
    }
   
    private class KVPair {
        public PyObject key;
        public PyObject value;

        public KVPair(PyObject key, PyObject value) {
            this.key = key;
            this.value = value;
        }
    }
    
    public void sort() {       
        PyObject origData[] = gOriginalList.getArray();
        PyObject[] data = new PyObject[size];
        //list data is copied to new array and is temporarily made empty, so that 
        //mutations performed by comparison or key functions can't affect 
        //the slice of memory we're sorting.
        System.arraycopy(origData, 0, data, 0, size);
        //list.clear();
        
        //If keyfunction is given, object of type KVPair with key resulting from 
        //key(data[pos]) and value from data[pos] is created. Otherwise the key
        //of KVPair object will take the value of data[pos] and the corresponding
        //value will be null. Thus, we will do sorting on the keys of KVPair object
        //array effectively without disturbing the incoming list object.
        if (this.key != null) {
            for (int i = 0; i < size; i++) {
                this.kvdata[i] = new KVPair(key.__call__(data[i]), data[i]);
            }
        } else {
            for (int i = 0; i < size; i++) {
                this.kvdata[i] = new KVPair(data[i], null);
            }
        }
        //make data null, we dont need this reference afterwards
        data = null;
        
        //Reverse sort stability achieved by initially reversing the list,
        //applying a stable forward sort, then reversing the final result.      
        if (reverse && size > 1) {
            reverse_slice(0, size);
        }
        
        int nremaining = this.size;
        if (nremaining < 2) {
            return;
        }

        int lo = 0;
        int hi = nremaining;
        int minrun = merge_compute_minrun(nremaining);

        boolean[] descending = new boolean[1];
        do {
            /* Identify next run. */
            int localN = count_run(lo, hi, descending);
            if (descending[0])
                reverse_slice(lo, lo + localN);
            /* If short, extend to min(minrun, nremaining). */
            if (localN < minrun) {
                int force = nremaining < minrun ?  nremaining : minrun;
                binarysort(lo, lo + force, lo + localN);
                localN = force;
            }
            /* Push run onto pending-runs stack, and maybe merge. */
            //ms.assert_(ms.n < ms.MAX_MERGE_PENDING);
            this.base[this.n] = lo;
            this.len[this.n] = localN;
            ++this.n;
            merge_collapse();
            /* Advance to find next run */
            lo += localN;
            nremaining -= localN;
        } while (nremaining != 0);
        //assert_(lo == hi);

        merge_force_collapse();
        //assert_(ms.n == 1);
        //assert_(ms.base[0] == 0);
        //assert_(ms.len[0] == size);
        
        //The user mucked up with the list during the sort,
        //and so, the value error is thrown
        if (gOriginalList.gListAllocatedStatus >= 0) {
            throw Py.ValueError("list modified during sort");
        }
        
        if (reverse && size > 1) {
            reverse_slice(0, size);
        }
        
        //Now copy the sorted values from KVPairs if key function is given,
        //otherwise the keys from KVPairs.    
        if (this.key != null) {
            for (int i = 0; i < size; i++) {
                origData[i] = this.kvdata[i].value;
            }   
        } else {
            for (int i = 0; i < size; i++) {
                origData[i] = this.kvdata[i].key;
            }            
        }
    }

    public void getmem(int need) {
        if (need <= this.a.length)
            return;
        this.a = new KVPair[need];
    }

    int count_run(int lo, int hi, boolean[] descending) {
        //assert_(lo < hi);
        descending[0] = false;
        ++lo;
        if (lo == hi)
            return 1;
        int localN = 2;
        if (iflt(this.kvdata[lo].key, this.kvdata[lo-1].key)) {
            descending[0] = true;
            for (lo = lo + 1; lo < hi; ++lo, ++localN) {
                if (! iflt(this.kvdata[lo].key, this.kvdata[lo-1].key))
                    break;
            }
        } else {
            for (lo = lo + 1; lo < hi; ++lo, ++localN) {
                if (iflt(this.kvdata[lo].key, this.kvdata[lo-1].key))
                    break;
            }
        }
        return localN;
    }


    void merge_lo(int pa, int na, int pb, int nb) {
        //debug("merge_lo pa:" + pa + " na:" + na + " pb:" + pb + " nb:" + nb);
        //dump_data("padata", pa, na);
        //dump_data("pbdata", pb, nb);

        //assert_(na > 0 && nb > 0 && pa + na == pb);
        getmem(na);
        System.arraycopy(this.kvdata, pa, this.a, 0, na);
        int dest = pa;
        pa = 0;

        this.kvdata[dest++] = this.kvdata[pb++];
        --nb;
        if (nb == 0) {
            // Succeed; (falls through to Fail)
            if (na != 0)
                System.arraycopy(this.a, pa, this.kvdata, dest, na);
            return;
        }
        if (na == 1) {
            // CopyB;
            System.arraycopy(this.kvdata, pb, this.kvdata, dest, nb);
            this.kvdata[dest + nb] = this.a[pa];
            return;
        }

        try {
            for (;;) {
                int acount = 0; /* # of time A won in a row */
                int bcount = 0; /* # of time B won in a row */

                /* Do the straightforward thing until (if ever) one run
                 * appears to win consistently.
                 */
                for (;;) {
                    boolean k = iflt(this.kvdata[pb].key, this.a[pa].key);
                    if (k) {
                        this.kvdata[dest++] = this.kvdata[pb++];
                        ++bcount;
                        acount = 0;
                        --nb;
                        if (nb == 0)
                            return;
                        if (bcount >= MIN_GALLOP)
                            break;
                    } else {
                        this.kvdata[dest++] = this.a[pa++];
                        ++acount;
                        bcount = 0;
                        --na;
                        if (na == 1) {
                            // CopyB;
                            System.arraycopy(this.kvdata, pb, this.kvdata, dest, nb);
                            this.kvdata[dest + nb] = this.a[pa];
                            na = 0;
                            return;
                        }
                        if (acount >= MIN_GALLOP)
                            break;
                    }
                }

                /* One run is winning so consistently that galloping may
                 * be a huge win. So try that, and continue galloping until
                 * (if ever) neither run appears to be winning consistently
                 * anymore.
                 */
                do {
                    int k = gallop_right(this.kvdata[pb].key, this.a, pa, na, 0);
                    acount = k;
                    if (k != 0) {
                        System.arraycopy(this.a, pa, this.kvdata, dest, k);
                        dest += k;
                        pa += k;
                        na -= k;
                        if (na == 1) {
                            // CopyB
                            System.arraycopy(this.kvdata, pb, this.kvdata, dest, nb);
                            this.kvdata[dest + nb] = this.a[pa];
                            na = 0;
                            return;
                        }
                        /* na==0 is impossible now if the comparison
                         * function is consistent, but we can't assume
                         * that it is.
                         */
                        if (na == 0)
                            return;
                    }

                    this.kvdata[dest++] = this.kvdata[pb++];
                    --nb;
                    if (nb == 0)
                        return;

                    k = gallop_left(this.a[pa].key, this.kvdata, pb, nb, 0);
                    bcount = k;
                    if (k != 0) {
                        System.arraycopy(this.kvdata, pb, this.kvdata, dest, k);
                        dest += k;
                        pb += k;
                        nb -= k;
                        if (nb == 0)
                            return;
                    }
                    this.kvdata[dest++] = this.a[pa++];
                    --na;
                    if (na == 1) {
                        // CopyB;
                        System.arraycopy(this.kvdata, pb, this.kvdata, dest, nb);
                        this.kvdata[dest + nb] = this.a[pa];
                        na = 0;
                        return;
                    }
                } while (acount >= MIN_GALLOP || bcount >= MIN_GALLOP);
            }
        } finally {
            if (na != 0)
                System.arraycopy(this.a, pa, this.kvdata, dest, na);

            //dump_data("result", origpa, cnt);
        }
    }



    void merge_hi(int pa, int na, int pb, int nb) {
        //debug("merge_hi pa:" + pa + " na:" + na + " pb:" + pb + " nb:" + nb);
        //dump_data("padata", pa, na);
        //dump_data("pbdata", pb, nb);

        //assert_(na > 0 && nb > 0 && pa + na == pb);
        getmem(nb);
        int dest = pb + nb - 1;
        int basea = pa;
        System.arraycopy(this.kvdata, pb, this.a, 0, nb);

        pb = nb - 1;
        pa += na - 1;

        this.kvdata[dest--] = this.kvdata[pa--];
        --na;
        if (na == 0) {
            // Succeed; (falls through to Fail)
            if (nb != 0)
                System.arraycopy(this.a, 0, this.kvdata, dest-(nb-1), nb);
            return;
        }
        if (nb == 1) {
            // CopyA;
            dest -= na;
            pa -= na;
            System.arraycopy(this.kvdata, pa+1, this.kvdata, dest+1, na);
            this.kvdata[dest] = this.a[pb];
            nb = 0;
            return;
        }

        try {
            for (;;) {
                int acount = 0; /* # of time A won in a row */
                int bcount = 0; /* # of time B won in a row */

                /* Do the straightforward thing until (if ever) one run
                 * appears to win consistently.
                 */
                for (;;) {
                    boolean k = iflt(this.a[pb].key, this.kvdata[pa].key);
                    if (k) {
                        this.kvdata[dest--] = this.kvdata[pa--];
                        ++acount;
                        bcount = 0;
                        --na;
                        if (na == 0)
                            return;
                        if (acount >= MIN_GALLOP)
                            break;
                    } else {
                        this.kvdata[dest--] = this.a[pb--];
                        ++bcount;
                        acount = 0;
                        --nb;
                        if (nb == 1) {
                            // CopyA
                            dest -= na;
                            pa -= na;
                            System.arraycopy(this.kvdata, pa+1, this.kvdata, dest+1, na);
                            this.kvdata[dest] = this.a[pb];
                            nb = 0;
                            return;
                        }
                        if (bcount >= MIN_GALLOP)
                            break;
                    }
                }

                /* One run is winning so consistently that galloping may
                 * be a huge win. So try that, and continue galloping until
                 * (if ever) neither run appears to be winning consistently
                 * anymore.
                 */
                do {
                    int k = gallop_right(this.a[pb].key, this.kvdata, basea, na, na-1);
                    acount = k = na - k;
                    if (k != 0) {
                        dest -= k;
                        pa -= k;
                        System.arraycopy(this.kvdata, pa+1, this.kvdata, dest+1, k);
                        na -= k;
                        if (na == 0)
                            return;
                    }

                    this.kvdata[dest--] = this.a[pb--];
                    --nb;
                    if (nb == 1) {
                        // CopyA
                        dest -= na;
                        pa -= na;
                        System.arraycopy(this.kvdata, pa+1, this.kvdata, dest+1, na);
                        this.kvdata[dest] = this.a[pb];
                        nb = 0;
                        return;
                    }

                    k = gallop_left(this.kvdata[pa].key, this.a, 0, nb, nb-1);
                    bcount = k = nb - k;
                    if (k != 0) {
                        dest -= k;
                        pb -= k;
                        System.arraycopy(this.a, pb+1, this.kvdata, dest+1, k);
                        nb -= k;
                        if (nb == 1) {
                            // CopyA
                            dest -= na;
                            pa -= na;
                            System.arraycopy(this.kvdata, pa+1, this.kvdata, dest+1, na);
                            this.kvdata[dest] = this.a[pb];
                            nb = 0;
                            return;
                        }
                        /* nb==0 is impossible now if the comparison
                         * function is consistent, but we can't assume
                         * that it is.
                         */
                        if (nb == 0)
                            return;
                    }
                    this.kvdata[dest--] = this.kvdata[pa--];
                    --na;
                    if (na == 0)
                        return;
                } while (acount >= MIN_GALLOP || bcount >= MIN_GALLOP);
            }
        } finally {
            if (nb != 0)
                System.arraycopy(this.a, 0, this.kvdata, dest-(nb-1), nb);

            //dump_data("result", origpa, cnt);
        }
    }        



   /*
    Locate the proper position of key in a sorted vector; if the vector contains
    an element equal to key, return the position immediately to the left of
    the leftmost equal element.  [gallop_right() does the same except returns
    the position to the right of the rightmost equal element (if any).]
    
    "a" is a sorted vector with n elements, starting at a[0].  n must be > 0.
    
    "hint" is an index at which to begin the search, 0 <= hint < n.  The closer
    hint is to the final result, the faster this runs.
    
    The return value is the int k in 0..n such that
    
        a[k-1] < key <= a[k]
    
    pretending that *(a-1) is minus infinity and a[n] is plus infinity.  IOW,
    key belongs at index k; or, IOW, the first k elements of a should precede
    key, and the last n-k should follow key.

    Returns -1 on error.  See listsort.txt for info on the method.
    */

    private int gallop_left(PyObject key, KVPair[] localData, int localA, int localN,
                            int hint)
    {
        //assert_(n > 0 && hint >= 0 && hint < n);
        localA += hint;
        int ofs = 1;
        int lastofs = 0;
    
        if (iflt(localData[localA].key, key)) {
            /* a[hint] < key -- gallop right, until
             * a[hint + lastofs] < key <= a[hint + ofs]
             */
            int maxofs = localN - hint; // data[a + n - 1] is highest
            while (ofs < maxofs) {
                if (iflt(localData[localA + ofs].key, key)) {
                    lastofs = ofs;
                    ofs = (ofs << 1) + 1;
                    if (ofs <= 0) // int overflow
                        ofs = maxofs;
                } else {
                    // key < data[a + hint + ofs]
                    break;
                }
            }
            if (ofs > maxofs)
                ofs = maxofs;
            // Translate back to offsets relative to a.
            lastofs += hint;
            ofs += hint;
        } else {
            /* key <= a[hint] -- gallop left, until
             * a[hint - ofs] < key <= a[hint - lastofs]
             */
            int maxofs = hint + 1; // data[a] is lowest
            while (ofs < maxofs) {
                if (iflt(localData[localA - ofs].key, key))
                    break;
                // key <= data[a + hint - ofs]
                lastofs = ofs;
                ofs = (ofs << 1) + 1;
                if (ofs <= 0) // int overflow
                    ofs = maxofs;
            }
            if (ofs > maxofs)
                ofs = maxofs;
            // Translate back to offsets relative to a.
            int k = lastofs;
            lastofs = hint - ofs;
            ofs = hint - k;
        }
        localA -= hint;
        //assert_(-1 <= lastofs && lastofs < ofs && ofs <= n);
        /* Now a[lastofs] < key <= a[ofs], so key belongs somewhere to the
         * right of lastofs but no farther right than ofs.  Do a binary
         * search, with invariant a[lastofs-1] < key <= a[ofs].
         */
        ++lastofs;
        while (lastofs < ofs) {
            int m = lastofs + ((ofs - lastofs) >> 1);
            if (iflt(localData[localA + m].key, key))
                lastofs = m+1;  // data[a + m] < key
            else
                ofs = m;        // key <= data[a + m]
        }
        //assert_(lastofs == ofs); // so data[a + ofs -1] < key <= data[a+ofs]
        return ofs;
    }


    /*
    * Exactly like gallop_left(), except that if key already exists in a[0:n],
    * finds the position immediately to the right of the rightmost equal value.
    *  
    * The return value is the int k in 0..n such that
    *     a[k-1] <= key < a[k]
    * or -1 if error.
    *  
    * The code duplication is massive, but this is enough different given that
    * we're sticking to "<" comparisons that it's much harder to follow if
    * written as one routine with yet another "left or right?" flag.
    */

    private int gallop_right(PyObject key, KVPair[] aData, int localA, int localN,
                             int hint)
    {
        //assert_(n > 0 && hint >= 0 && hint < n);
        localA += hint;
        int lastofs = 0;
        int ofs = 1;

        if (iflt(key, aData[localA].key)) {
            /* key < a[hint] -- gallop left, until
             * a[hint - ofs] <= key < a[hint - lastofs]
             */
            int maxofs = hint + 1;    /* data[a] is lowest */
            while (ofs < maxofs) {
                if (iflt(key, aData[localA - ofs].key)) {
                    lastofs = ofs;
                    ofs = (ofs << 1) + 1;
                    if (ofs <= 0)  // int overflow
                        ofs = maxofs;
                } else {
                    /* a[hint - ofs] <= key */
                    break;
                }
            }
            if (ofs > maxofs)
                ofs = maxofs;
            /* Translate back to positive offsets relative to &a[0]. */
            int k = lastofs;
            lastofs = hint - ofs;
            ofs = hint - k;
        } else {
            /* a[hint] <= key -- gallop right, until
             * a[hint + lastofs] <= key < a[hint + ofs]
             */
            int maxofs = localN - hint; /* data[a + n - 1] is highest */
            while (ofs < maxofs) {
                if (iflt(key, aData[localA + ofs].key))
                    break;
                /* a[hint + ofs] <= key */
                lastofs = ofs;
                ofs = (ofs << 1) + 1;
                if (ofs <= 0) /* int overflow */
                    ofs = maxofs;
            }
            if (ofs > maxofs)
                ofs = maxofs;
            /* Translate back to offsets relative to &a[0]. */
            lastofs += hint;
            ofs += hint;
        }
        localA -= hint;

        //assert_(-1 <= lastofs && lastofs < ofs && ofs <= n);
        
        /* Now a[lastofs] <= key < a[ofs], so key belongs somewhere to the
         * right of lastofs but no farther right than ofs.  Do a binary
         * search, with invariant a[lastofs-1] <= key < a[ofs].
         */
        ++lastofs;
        while (lastofs < ofs) {
            int m = lastofs + ((ofs - lastofs) >> 1);
            if (iflt(key, aData[localA + m].key))
                ofs = m;        // key < data[a + m]
            else
                lastofs = m+1;  // data[a + m] <= key
        }
        //assert_(lastofs == ofs); // so data[a + ofs -1] <= key < data[a+ofs]
        return ofs;
    }


    void merge_at(int i) {
        //assert_(n >= 2);
        //assert_(i >= 0);
        //assert_(i == n - 2 || i == n - 3);

        int pa = this.base[i];
        int pb = this.base[i+1];
        int na = this.len[i];
        int nb = this.len[i+1];

        //assert_(na > 0 && nb > 0);
        //assert_(pa + na == pb);

        // Record the length of the combined runs; if i is the 3rd-last
        // run now, also slide over the last run (which isn't involved
        // in this merge).  The current run i+1 goes away in any case.
        if (i == this.n - 3) {
            this.len[i+1] = this.len[i+2];
            this.base[i+1] = this.base[i+2];
        }
        this.len[i] = na + nb;
        --this.n;

        // Where does b start in a?  Elements in a before that can be
        // ignored (already in place).
        int k = gallop_right(this.kvdata[pb].key, this.kvdata, pa, na, 0);
        pa += k;
        na -= k;
        if (na == 0)
            return;
        
        // Where does a end in b?  Elements in b after that can be
        // ignored (already in place).
        nb = gallop_left(this.kvdata[pa + na - 1].key, this.kvdata, pb, nb, nb-1);
        if (nb == 0)
            return;

        // Merge what remains of the runs, using a temp array with
        // min(na, nb) elements.
        if (na <= nb)
            merge_lo(pa, na, pb, nb);
        else
            merge_hi(pa, na, pb, nb);
    }


    /* Examine the stack of runs waiting to be merged, merging adjacent runs
     * until the stack invariants are re-established:
     *
     * 1. len[-3] > len[-2] + len[-1]
     * 2. len[-2] > len[-1]
     *
     * See listsort.txt for more info.
     */
    void merge_collapse() {
        while (this.n > 1) {
            int localN = this.n - 2;
            if (localN > 0 && this.len[localN-1] <= this.len[localN] + this.len[localN+1]) {
                if (this.len[localN-1] < this.len[localN+1])
                    --localN;
                merge_at(localN);
            } else if (this.len[localN] <= this.len[localN+1]) {
                merge_at(localN);
            } else {
                break;
            }
        }
    }


    /* Regardless of invariants, merge all runs on the stack until only one
     * remains.  This is used at the end of the mergesort.
     *
     * Returns 0 on success, -1 on error.
     */
    void merge_force_collapse() {
        while (this.n > 1) {
            int localN = this.n - 2;
            if (localN > 0 && this.len[localN-1] < this.len[localN+1])
                --localN;
            merge_at(localN);
        }
    }


    /* Compute a good value for the minimum run length; natural runs shorter
     * than this are boosted artificially via binary insertion.
     *
     * If n < 64, return n (it's too small to bother with fancy stuff).
     * Else if n is an exact power of 2, return 32.
     * Else return an int k, 32 <= k <= 64, such that n/k is close to, but
     * strictly less than, an exact power of 2.
     *
     * See listsort.txt for more info.
     */
    int merge_compute_minrun(int localN) {
        int r = 0;  // becomes 1 if any 1 bits are shifted off

        //assert_(n >= 0);
        while (localN >= 64) {
             r |= localN & 1;
             localN >>= 1;
        }
        return localN + r;
    }


    void assert_(boolean expr) {
        if (!expr)
            throw new RuntimeException("assert");
    }


   private boolean iflt(PyObject x, PyObject y) {
        //assert_(x != null);
        //assert_(y != null);

        if (this.compare == null) {
            /* NOTE: we rely on the fact here that the sorting algorithm
               only ever checks whether k<0, i.e., whether x<y.  So we
               invoke the rich comparison function with _lt ('<'), and
               return -1 when it returns true and 0 when it returns
               false. */
            return x._lt(y).__nonzero__();
        }

        PyObject ret = this.compare.__call__(x, y);

        if (ret instanceof PyInteger) {
            int v = ((PyInteger)ret).getValue();
            return v < 0;
        }
        throw Py.TypeError("comparision function must return int");
    }            


    void reverse_slice(int lo, int hi) {
        --hi;
        while (lo < hi) {
            KVPair t = this.kvdata[lo];
            this.kvdata[lo] = this.kvdata[hi];
            this.kvdata[hi] = t;
            ++lo;
            --hi;
        }
    }



    /*
     * binarysort is the best method for sorting small arrays: it does
     * few compares, but can do data movement quadratic in the number of
     * elements.
     * [lo, hi) is a contiguous slice of a list, and is sorted via
     * binary insertion.  This sort is stable.
     * On entry, must have lo <= start <= hi, and that [lo, start) is already
     * sorted (pass start == lo if you don't know!).
     * If islt() complains return -1, else 0.
     * Even in case of error, the output slice will be some permutation of
     * the input (nothing is lost or duplicated).
     */
    void binarysort(int lo, int hi, int start) {
        //debug("binarysort: lo:" + lo + " hi:" + hi + " start:" + start);
        int p;

        //assert_(lo <= start && start <= hi);
        /* assert [lo, start) is sorted */
        if (lo == start)
                ++start;
        for (; start < hi; ++start) {
            /* set l to where *start belongs */
            int l = lo;
            int r = start;
            KVPair pivot = this.kvdata[r];
            // Invariants:
            // pivot >= all in [lo, l).
            // pivot  < all in [r, start).
            // The second is vacuously true at the start.
            //assert_(l < r);
            do {
                p = l + ((r - l) >> 1);
                if (iflt(pivot.key, this.kvdata[p].key))
                    r = p;
                else
                    l = p+1;
            } while (l < r);
            //assert_(l == r);
            // The invariants still hold, so pivot >= all in [lo, l) and
            // pivot < all in [l, start), so pivot belongs at l.  Note
            // that if there are elements equal to pivot, l points to the
            // first slot after them -- that's why this sort is stable.
            // Slide over to make room.
            for (p = start; p > l; --p)
                this.kvdata[p] = this.kvdata[p - 1];
            this.kvdata[l] = pivot;
        }
        //dump_data("binsort", lo, hi - lo);
    }

/*  //debugging methods.
    private void dump_data(String txt, int lo, int n) {
        System.out.print(txt + ":");
        for (int i = 0; i < n; i++)
            System.out.print(data[lo + i] + " ");
        System.out.println();
    }
    private void debug(String str) {
        //System.out.println(str);
    }
*/
}
