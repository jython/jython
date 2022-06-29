package org.python.core.util;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * <b>Not for application use</b>, this class may be used to count how many times a {@code String}
 * occurs a point in the runtime. We use this to investigate optimisations of the handling of
 * strings in the runtime, principally to avoid checks for byte-nature where that is already known.
 */
public class StringCounter {

    private static HashMap<String, Integer> counts = new HashMap<String, Integer>(1000);

    /**
     * Count this occurrence.
     *
     * @param s to count
     */
    public synchronized void count(String s) {
        Integer n = counts.getOrDefault(s, 0);
        counts.put(s, n + 1);
    }

    /**
     * Report the top {@code n} counts.
     *
     * @param n number of counts to retrieve
     * @return counts of the top {@code n} strings.
     */
    public synchronized Map<String, Integer> top(int n) {
        // We perform an insertion sort to a list of size up to n
        ArrayList<String> keys = new ArrayList<>();
        ArrayList<Integer> vals = new ArrayList<>();
        // For speed, we cache here the lowest value on the list
        int min = 0, len = 0;

        for (String key : counts.keySet()) {
            int val = counts.get(key);
            if (val > min) {
                // We will add (key, val) to the list.
                int p = len - 1;

                // Work back from the (low value) end until vals[i] > val.
                for (; p >= 0; --p) {
                    if (vals.get(p) > val) {
                        break;
                    }
                }

                // Insert after the entry that stopped us.
                p += 1;

                // p is 0 if we dropped out of the loop at the end.
                keys.add(p, key);
                vals.add(p, val);

                if (len == n) {
                    // The list was already n-long so discard from the end.
                    keys.remove(n);
                    vals.remove(n);
                    // Raise the minimum for entry.
                    min = vals.get(n - 1);
                } else {
                    // Count the increase.
                    len += 1;
                }
            }
        }

        // Zip the resulting arrays into a map
        Map<String, Integer> m = new LinkedHashMap<>();
        for (int i = 0; i < keys.size(); i++) {
            m.put(keys.get(i), vals.get(i));
        }
        return m;
    }

    /** Report the top n counts. */
    public void top(int n, PrintStream out) {
        for (Map.Entry<String, Integer> e : top(n).entrySet()) {
            String key = e.getKey();
            out.printf("%5d  \"%.60s%s\"\n", e.getValue(), key, key.length() > 60 ? "..." : "");
        }
        out.println();
    }
}
