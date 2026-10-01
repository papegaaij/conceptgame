package vanguard.game.display;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class WindowSizingTest {
    @ParameterizedTest(name = "{0}x{1} work area -> {2}x{3}")
    @CsvSource({
        "1920, 1032,  960,  540", // 1080p with a task bar: 2x does not fit
        "1920, 1080, 1920, 1080", // 1080p without panels: exactly 2x
        "2560, 1400, 1920, 1080", // 1440p
        "3840, 2112, 2880, 1620", // 4K with a top bar: 4x does not fit
        "3840, 2160, 3840, 2160", // 4K without panels
        "1280,  720,  960,  540", // 720p
        " 800,  600,  960,  540", // smaller than 1x: still 1x
    })
    void picksTheLargestIntegerMultipleThatFits(int areaWidth, int areaHeight, int width, int height) {
        Bounds window = WindowSizing.largestWindow(new Bounds(0, 0, areaWidth, areaHeight));

        assertEquals(width, window.width());
        assertEquals(height, window.height());
    }

    @ParameterizedTest
    @CsvSource({"0, 0, 2560, 1400, 320, 160", "3840, 32, 3840, 2128, 4320, 286"})
    void centresTheWindowInTheWorkArea(int x, int y, int areaWidth, int areaHeight, int windowX, int windowY) {
        Bounds window = WindowSizing.largestWindow(new Bounds(x, y, areaWidth, areaHeight));

        assertEquals(windowX, window.x());
        assertEquals(windowY, window.y());
    }
}
