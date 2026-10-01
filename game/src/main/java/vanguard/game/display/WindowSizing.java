package vanguard.game.display;

import vanguard.game.render.PixelScreen;

/** The size of a new window: the largest integer multiple of the 960x540 screen that fits. */
public final class WindowSizing {
    private WindowSizing() {}

    /** The largest whole-number scale of the pixel screen that fits the work area, centred in it; at least 1x. */
    public static Bounds largestWindow(Bounds workArea) {
        int scale = Math.max(1, Math.min(workArea.width() / PixelScreen.WIDTH, workArea.height() / PixelScreen.HEIGHT));
        int width = PixelScreen.WIDTH * scale;
        int height = PixelScreen.HEIGHT * scale;
        return new Bounds(
                workArea.x() + (workArea.width() - width) / 2,
                workArea.y() + (workArea.height() - height) / 2,
                width,
                height);
    }
}
