package vanguard.game.bench;

import java.lang.management.ManagementFactory;

/** Bytes allocated by the calling thread (HotSpot's per-thread allocation counter). */
public final class AllocationMeter {
    private final com.sun.management.ThreadMXBean threads =
            (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
    private long start;

    public void start() {
        start = threads.getCurrentThreadAllocatedBytes();
    }

    public long bytesSinceStart() {
        return threads.getCurrentThreadAllocatedBytes() - start;
    }
}
