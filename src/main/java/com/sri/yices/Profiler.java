package com.sri.yices;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;


//FIXME: decide on the units. millisecs or nanoseconds.

public final class Profiler {

    /**
     * Compile-time on off switch for profiling.
     */
    public static final boolean enabled = false;

    static private final boolean distributions = false;

    static private long cost = 0;

    static private Set<Long> threads = new HashSet<Long>();

    static private Map<String,Long> lineItems = new HashMap<String,Long>();


    // for each instrumented API routine: the value maps a call time in TENTHS of a second to the number of calls that have that time.
    static private Map<String, Map<Integer,Integer>> distribution = new HashMap<String, Map<Integer,Integer>>();

    private static void addThread(){
        long tid = Thread.currentThread().getId();
        threads.add(tid);
    }

    private static void addLineItem(String caller, long cost){
        Long current = lineItems.get(caller);
        lineItems.put(caller, current == null ? cost : current + cost);
    }

    private static void addDistribution(String caller, int callTime){
        // callTime is in millseconds; lets try tenths for a readable histogram
        callTime /= 100;
        Map<Integer,Integer> map = distribution.get(caller);
        if (map == null){
            map = new HashMap<Integer,Integer>();
            distribution.put(caller, map);
        }
        int count = map.getOrDefault(callTime, 0);
        map.put(callTime, count + 1);
    }


    public static int getThreadCount(){
        return threads.size();
    }

    /**
     * Increments the cost by (stop - start)
     */
    public static void delta(String caller, long start, long stop){
        delta(caller, start, stop, false);
    }

    /**
     * Increments the cost by (stop - start)
     * If distribution is true, adds the delta to the distribution.
     */
    public static void delta(String caller, long start, long stop, boolean distribution){
        long dcost = stop >= start ? stop - start : start - stop;
        addThread();
        addLineItem(caller, dcost);
        cost += dcost;
        if (distributions && distribution) {
            addDistribution(caller, (int)(cost/1000000));
        }
    }



    /**
     * Resets the cost accumulation counter to zero.
     */
    public static void reset(){
        cost = 0;
    }


    /**
     * Returns the accumulated time spent in the Yices solver in nanoseconds.
     */
    public static long get(){
        long retval = cost;
        cost = 0;
        return retval;
    }

    public static String report(){
        StringBuilder sb = new StringBuilder();
        toString(sb);
        return sb.toString();
    }



    private static void lineItems2StringBuilder(StringBuilder sb){
        LinkedHashMap<String, Long> sortedLineItems = new LinkedHashMap<String, Long>();
        lineItems.entrySet()
            .stream()
            .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
            .forEachOrdered(x -> sortedLineItems.put(x.getKey(), x.getValue()));
        for (String caller : sortedLineItems.keySet()){
            sb.append(caller).append(":\t\t").append(lineItems.get(caller)/1000000).append(" milliseconds\n");
        }
    }



    /* Data is formatted for this:
import matplotlib.pyplot as plt
# frequencies
counts = <data goes here>
rng = list(range(0, 100))
plt.bar(rng, counts)
plt.xlabel('call time (tenths of a second)')
plt.ylabel('No. of calls')
plt.title('<caller> on ...')
plt.show()
     */
    private static void distribution2StringBuilder(String caller, StringBuilder sb){
        Map<Integer,Integer> map = distribution.get(caller);
        sb.append("[");
        for (int i = 0; i < 100; i++) {
            sb.append(map.getOrDefault(i, 0));
            if (i < 99) {
                sb.append(", ");
            }
        }
        sb.append("]\n");
    }

    private static void distribution2StringBuilder(StringBuilder sb){
        for (String caller : distribution.keySet()){
            sb.append("\n").append(caller).append(" Distribution").append("\n\n");
            distribution2StringBuilder(caller, sb);
        }
    }



    public static void toString(StringBuilder sb){
        if (enabled) {
            sb.append("\n--- PROFILING SUMMARY ---\n\n");
            sb.append("Calling thread count: ").append(getThreadCount()).append("\n");
            lineItems2StringBuilder(sb);
            if (distributions) {
                distribution2StringBuilder(sb);
                sb.append("\n |distribution| = ").append(distribution.size()).append("\n");
            }
        }
    }


}
