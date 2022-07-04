package org.python.core.util;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

/**
 * Not for application use, a cache of recently given results from some costly function. This is
 * only public so we can reach it from the run-time. The user sets a nominal size for the cache. The
 * cache will grow to this size and a little more, but from time to time discard the "little more"
 * on the basis of a score that depends on recency of use.
 */
public class LimitedCache<K, V> {

    /**
     * Threshold at which we re-normalise times using {@link #scaleClock()}. Big enough it hardly
     * ever happens. Small enough to prevent overflow (much less than max integer).
     */
    private final int CLOCK_THRESHOLD = 10_000;

    /** Maximum size of cache as requested. */
    private final int limit;
    /** Working upper limit (&gt; limit) at which to evict cached items. */
    private final int upperLimit;
    /** Increases with every operation (until renormalised by {@link #scaleClock()}). */
    private int clock = 0;

    /** Object that holds one cached value and its statistics. */
    private static class Holder<T> {

        final T value;
        int used;

        Holder(T value, int time) {
            this.value = value;
            this.used = time;
        }

        int score() {
            return used;
        }

        @Override
        public String toString() {
            return String.format("(%6d %.60s)", used, value);
        }
    }

    private final HashMap<K, Holder<? extends V>> map;

    /**
     * Construct a cache that will hold (at least) the specified number of entries. (It will
     * sometimes contain a few more so we don't have to scan the cache with every addition.)
     *
     * @param capacity the number of entries required
     */
    public LimitedCache(int capacity) {
        this.limit = capacity;
        this.upperLimit = capacity + capacity / 3 + 1;
        this.map = new HashMap<>(this.upperLimit);
    }

    /**
     * Get a value corresponding to the key, if it was previously cached.
     *
     * @param key against which cached
     * @return the cached value or {@code null} if not present
     */
    public synchronized V get(K key) {
        Holder<? extends V> h = map.get(key);
        if (h != null) {
            h.used = clock++;
            if (clock >= CLOCK_THRESHOLD) {
                scaleClock();
            }
            return h.value;
        } else {
            return null;
        }
    }

    /**
     * Add a value corresponding to a given key.
     *
     * @param key against which to cache the value
     * @param value to store
     */
    public synchronized void add(K key, V value) {
        map.computeIfAbsent(key, k -> new Holder<V>(value, clock++));
        if (map.size() >= upperLimit) {
            evictLowest();
        }
    }

    /**
     * Reduce the working size of the cache to at most the originally-specified capacity by removing
     * low-scoring entries. We only do this when adding to the cache, and only then if the
     * {@link #upperLimit} has been reached.
     */
    private void evictLowest() {
        // Work out how many to discard: this can be zero and the call still useful.
        final int N = Math.max(0, map.size() - limit);
        // We collect the N worst (lowest) scores in ascending order.
        int[] worst = new int[N];
        Arrays.fill(worst, Integer.MAX_VALUE);
        int disqualifyingScore = Integer.MIN_VALUE;

        if (N > 0) {
            // Sort scores into worst[], keeping lowest N.
            for (Entry<K, Holder<? extends V>> e : map.entrySet()) {
                Holder<? extends V> h = e.getValue();
                int s = h.score();
                if (s < worst[N - 1]) { // Is this going in the array at all?
                    /*
                     * It is. The process is the same as for shifting an array to the right by one,
                     * except we start only once we see a score bigger than s. At that point the
                     * "element in hand" is the score s to insert there.
                     */
                    int i = 0;
                    while (i < N && s > worst[i]) {
                        i++;
                    }
                    // Now start shifting the array. First in is s.
                    while (i < N) {
                        int t = worst[i];
                        worst[i++] = s;
                        s = t;
                    }
                }
            }
            disqualifyingScore = worst[N - 1];
        }

        // Remove everything scoring worst[N - 1] or lower
        Iterator<Entry<K, Holder<? extends V>>> iter = map.entrySet().iterator();
        while (iter.hasNext()) {
            Entry<K, Holder<? extends V>> e = iter.next();
            Holder<? extends V> h = e.getValue();
            if (h.score() <= disqualifyingScore) {
                iter.remove();
            }
        }
    }

    /**
     * Scale down all the last used times and the current clock. We do this because otherwise the
     * clock increases indefinitely and theoretically could overflow, at which point the algorithm
     * breaks. We don't do this very often: only when {@link #clock} reached
     * {@link #CLOCK_THRESHOLD}.
     */
    private void scaleClock() {
        for (Holder<? extends V> h : map.values()) {
            h.used >>>= 1;
        }
        // And set the time back on the clock too
        clock >>>= 1;
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append("LimitedCache [clock=").append(clock);
        b.append(", size=").append(map.size());
        b.append(" (").append(limit);
        b.append(", ").append(upperLimit);
        b.append(")]\n");
        for (Holder<? extends V> h : map.values()) {
            b.append(h.toString()).append("\n");;
        }
        return b.toString();
    }
}
