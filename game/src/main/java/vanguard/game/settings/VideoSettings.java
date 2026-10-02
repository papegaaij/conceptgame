package vanguard.game.settings;

/**
 * The Video tab (design/ui/options) apart from the display mode, which the display switcher keeps.
 *
 * @param scanlines darken every other line of the scaled image, as on a CRT
 */
public record VideoSettings(Scaling scaling, boolean scanlines) {
    /** Integer scaling with letterboxing, no scanlines. */
    public static VideoSettings defaults() {
        return new VideoSettings(Scaling.INTEGER, false);
    }

    public VideoSettings withScaling(Scaling changed) {
        return new VideoSettings(changed, scanlines);
    }

    public VideoSettings withScanlines(boolean on) {
        return new VideoSettings(scaling, on);
    }
}
