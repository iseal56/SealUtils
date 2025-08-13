package dev.iseal.sealUtils.utils;

import dev.iseal.sealUtils.SealUtils;
import dev.iseal.sealUtils.Interfaces.SealLogger;
import dev.iseal.sealUtils.systems.analytics.AnalyticsManager;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Utility for measuring and aggregating performance timings.
 */
public class PerfManager {

    private static final PerfManager instance = new PerfManager();

    /**
     * Get the singleton instance.
     */
    public static PerfManager getInstance() {
        return instance;
    }

    // Unique key generator for timers
    private final AtomicLong keyGen = new AtomicLong();

    // Per-thread map of key -> start time
    private final ThreadLocal<Map<Object, Long>> threadTimers = ThreadLocal.withInitial(HashMap::new);

    // Utilizer -> List of durations (ns)
    private final ConcurrentHashMap<String, List<Long>> durations = new ConcurrentHashMap<>();

    /**
     * Start a timer for the current thread. Returns a unique key for this timer.
     */
    public static Object start() {
        Object key = getInstance().keyGen.incrementAndGet();
        getInstance().threadTimers.get().put(key, System.nanoTime());
        return key;
    }

    /**
     * Stop timing for the given utilizer and key.
     * @param utilizer The name of the code section being timed.
     * @param key The key returned by start().
     */
    public static void stop(String utilizer, Object key) {
        PerfManager instance = getInstance();
        Map<Object, Long> timers = instance.threadTimers.get();
        Long start = timers.remove(key);
        if (start == null) {
            throw new IllegalStateException("PerfManager.stop() called with invalid or already used key");
        }
        long duration = System.nanoTime() - start;
        instance.durations.computeIfAbsent(utilizer, k -> Collections.synchronizedList(new ArrayList<>())).add(duration);
    }

    /**
     * Print and clear all recorded average durations using SealLogger.
     */
    public static void printAndClear() {
        PerfManager instance = getInstance();
        SealLogger logger = SealUtils.getLogger();
        if (instance.durations.isEmpty()) {
            logger.info("No timings recorded.");
            return;
        }
        logger.info("Performance timings (average):");
        instance.durations.forEach((key, list) -> {
            double avg = list.stream().mapToLong(Long::longValue).average().orElse(0.0) / 1_000_000.0;
            logger.info(String.format("  %-20s : %8.3f ms", key, avg));
        });
        instance.durations.clear();
    }

    /**
     * Get a snapshot of all recorded average durations (in milliseconds).
     */
    public static Map<String, Double> getDurations() {
        PerfManager instance = getInstance();
        Map<String, Double> averages = new HashMap<>();
        instance.durations.forEach((key, list) -> {
            double avg = list.stream().mapToLong(Long::longValue).average().orElse(0.0) / 1_000_000.0;
            averages.put(key, avg);
        });
        return averages;
    }

    /**
     * Clear all recorded durations.
     */
    public static void clear() {
        getInstance().durations.clear();
    }
}