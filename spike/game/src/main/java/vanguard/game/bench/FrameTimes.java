package vanguard.game.bench;

import java.util.Arrays;

/** Frame durations in a preallocated buffer, summarised as percentiles at the end of a run. */
public final class FrameTimes {
    private final int[] nanos;
    private int count;

    public FrameTimes(int capacity) {
        nanos = new int[capacity];
    }

    public void add(long frameNanos) {
        if (count < nanos.length) {
            nanos[count++] = (int) Math.min(Integer.MAX_VALUE, frameNanos);
        }
    }

    public int count() {
        return count;
    }

    public Summary summarise() {
        if (count == 0) {
            return new Summary(0, 0, 0, 0, 0, 0, 0, 0);
        }
        int[] sorted = Arrays.copyOf(nanos, count);
        Arrays.sort(sorted);
        long total = 0;
        int over = 0;
        for (int value : sorted) {
            total += value;
            if (value > Summary.BUDGET_NANOS) {
                over++;
            }
        }
        return new Summary(count, total / 1e9, millis(percentile(sorted, 50)), millis(percentile(sorted, 95)),
                millis(percentile(sorted, 99)), millis(percentile(sorted, 99.9)), millis(sorted[count - 1]), over);
    }

    private static int percentile(int[] sorted, double percent) {
        int index = (int) Math.ceil(percent / 100 * sorted.length) - 1;
        return sorted[Math.max(0, index)];
    }

    private static double millis(int nanos) {
        return nanos / 1e6;
    }

    /** Frame time statistics in milliseconds. */
    public record Summary(int frames, double seconds, double p50, double p95, double p99, double p999,
                          double max, int framesOverBudget) {
        /** One frame at 60 Hz. */
        public static final long BUDGET_NANOS = 16_666_667;

        public double fps() {
            return seconds == 0 ? 0 : frames / seconds;
        }
    }
}
