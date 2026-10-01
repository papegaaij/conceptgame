package vanguard.game;

import java.util.Locale;

/** The {@code --bench} mode: runs for a fixed time, then reports the frames so the game can exit. */
final class BenchRun {
    private final double seconds;
    private double elapsed;
    private int frames;
    private float longestFrame;

    BenchRun(double seconds) {
        this.seconds = seconds;
    }

    /** Counts a frame of {@code frameSeconds}; returns whether the run is over. */
    boolean frame(float frameSeconds) {
        elapsed += frameSeconds;
        frames++;
        longestFrame = Math.max(longestFrame, frameSeconds);
        return elapsed >= seconds;
    }

    String report() {
        return String.format(
                Locale.ROOT,
                "%d frames in %.1f s (%.0f fps), longest frame %.1f ms",
                frames,
                elapsed,
                frames / elapsed,
                longestFrame * 1000);
    }
}
