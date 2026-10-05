package vanguard.sim;

import java.lang.management.ManagementFactory;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Measures what a piece of simulation work allocates on the calling thread, for the tests that hold
 * the simulation to its rule: no allocation per step.
 *
 * <p>A single measured run is not reliable. Besides the work's own allocations the thread's counter
 * also picks up one-off allocations the JVM makes on that thread: loading and initialising a class
 * the first time, and, whenever HotSpot queues a method for its optimising compiler (C2), the String
 * objects for every not yet resolved literal in that method's class (CompileBroker::compile_method
 * resolves the whole constant pool's strings on the requesting thread). When those happen depends
 * on how far the JIT has got, so on a slower machine (the 3-core macOS runners) they land in a later
 * run than on a fast one.
 *
 * <p>So the work runs several times, each run prepared outside the measurement, and the least
 * allocation counts. A one-off of the JVM shows in one run only; an allocation of the work itself
 * shows in every run, because the simulation is deterministic and every run takes the same path.
 * The tests therefore demand exactly zero bytes (see AllocationsTest).
 */
public final class Allocations {
    /** How often the work runs at most; the first run is the warm-up that loads the classes. */
    static final int RUNS = 10;

    private Allocations() {}

    /**
     * The fewest bytes {@code work} allocated in one of up to {@link #RUNS} runs, each on a fresh
     * subject from {@code prepare} (which is not measured). Stops at the first run that allocates
     * nothing.
     */
    public static <T> long least(Supplier<T> prepare, Consumer<T> work) {
        var threads = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        long least = Long.MAX_VALUE;
        for (int run = 0; run < RUNS && least > 0; run++) {
            T subject = prepare.get();
            long before = threads.getCurrentThreadAllocatedBytes();
            work.accept(subject);
            least = Math.min(least, threads.getCurrentThreadAllocatedBytes() - before);
        }
        return least;
    }
}
