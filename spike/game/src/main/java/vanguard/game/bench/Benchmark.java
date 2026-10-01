package vanguard.game.bench;

import java.lang.management.ManagementFactory;

/**
 * Measures a timed run: frame times after a warm-up, render-thread allocation and GC activity.
 * Call {@link #frame(long)} once per rendered frame, on the render thread.
 */
public final class Benchmark {
    private static final double WARM_UP_SECONDS = 2;
    private static final int SLOW_FRAMES_LOGGED = 16;
    /** Frames longer than this are listed with their time (vsync jitter stays below it). */
    private static final long SLOW_FRAME_NANOS = 25_000_000;

    private final double seconds;
    private final FrameTimes frameTimes;
    private final AllocationMeter allocation = new AllocationMeter();
    private final GcMeter gc = new GcMeter();
    private long startNanos;
    private long measureFromNanos;
    private long lastFrameNanos;
    private final long[] slowFrameEnds = new long[SLOW_FRAMES_LOGGED];
    private final long[] slowFrameNanos = new long[SLOW_FRAMES_LOGGED];
    private int slowFrames;

    public Benchmark(double seconds) {
        this.seconds = seconds;
        this.frameTimes = new FrameTimes((int) Math.min(8_000_000, seconds * 20_000));
    }

    public void frame(long nowNanos) {
        if (startNanos == 0) {
            startNanos = nowNanos;
            measureFromNanos = nowNanos + (long) (WARM_UP_SECONDS * 1e9);
        } else if (nowNanos >= measureFromNanos) {
            if (lastFrameNanos < measureFromNanos) {
                allocation.start();
                gc.start();
            } else {
                long frameNanos = nowNanos - lastFrameNanos;
                frameTimes.add(frameNanos);
                if (frameNanos > SLOW_FRAME_NANOS && slowFrames < SLOW_FRAMES_LOGGED) {
                    slowFrameEnds[slowFrames] = nowNanos;
                    slowFrameNanos[slowFrames++] = frameNanos;
                }
            }
        }
        lastFrameNanos = nowNanos;
    }

    public boolean finished(long nowNanos) {
        return startNanos != 0 && nowNanos - measureFromNanos >= seconds * 1e9;
    }

    /** The report lines for the measured part of the run. */
    public String report() {
        FrameTimes.Summary s = frameTimes.summarise();
        long bytes = allocation.bytesSinceStart();
        return """
                frames            %d in %.1f s (%.0f fps)
                frame time ms     p50 %.3f  p95 %.3f  p99 %.3f  p99.9 %.3f  max %.3f
                frames > 16.7 ms  %d
                render thread     %d bytes allocated (%.1f bytes/frame, %.1f KB/s)
                gc                %s
                frames > 25 ms    %s
                """.formatted(s.frames(), s.seconds(), s.fps(), s.p50(), s.p95(), s.p99(), s.p999(), s.max(),
                s.framesOverBudget(), bytes, s.frames() == 0 ? 0.0 : (double) bytes / s.frames(),
                s.seconds() == 0 ? 0.0 : bytes / 1024.0 / s.seconds(), gc.summary(), slowFrameList());
    }

    /** The first slow frames as "ms at s", seconds since the JVM started (as in -Xlog:gc). */
    private String slowFrameList() {
        long uptimeNanos = ManagementFactory.getRuntimeMXBean().getUptime() * 1_000_000;
        long jvmStartNanos = System.nanoTime() - uptimeNanos;
        var list = new StringBuilder();
        for (int i = 0; i < slowFrames; i++) {
            list.append("%.1f ms at %.2f s; ".formatted(slowFrameNanos[i] / 1e6, (slowFrameEnds[i] - jvmStartNanos) / 1e9));
        }
        return slowFrames == 0 ? "none" : list.toString();
    }
}
