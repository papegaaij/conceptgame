package vanguard.game.bench;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.List;

/** Garbage collection counts and accumulated time since {@link #start()}. */
public final class GcMeter {
    private final List<GarbageCollectorMXBean> collectors = ManagementFactory.getGarbageCollectorMXBeans();
    private long startCount;
    private long startMillis;

    public void start() {
        startCount = totalCount();
        startMillis = totalMillis();
    }

    public String summary() {
        String names = String.join(" + ", collectors.stream().map(GarbageCollectorMXBean::getName).toList());
        return "%d collections, %d ms total (%s)".formatted(totalCount() - startCount, totalMillis() - startMillis, names);
    }

    private long totalCount() {
        return collectors.stream().mapToLong(GarbageCollectorMXBean::getCollectionCount).sum();
    }

    private long totalMillis() {
        return collectors.stream().mapToLong(GarbageCollectorMXBean::getCollectionTime).sum();
    }
}
