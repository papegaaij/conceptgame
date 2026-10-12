package vanguard.game.render;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import vanguard.content.Content;
import vanguard.content.ContentLoader;
import vanguard.content.Difficulty;
import vanguard.content.LevelData;
import vanguard.content.SimSpecs;
import vanguard.sim.Enemy;
import vanguard.sim.EnemySpec;
import vanguard.sim.LevelScript;
import vanguard.sim.SimEvents;
import vanguard.sim.SimStep;
import vanguard.sim.Sortie;

/**
 * M5 part F's looks (design/campaign Level 12, Weather; design/enemies/air/lamprey): the lightning's
 * sky flash and reveal as the data times them, never more than three flashes a second; the rain by
 * the data's keys, none in the eye, and bright and dense enough to see within the high-air cap; the
 * gust arrow big and clear of the edge warnings; the reveal showing the bodies under the banks; the eye's wall turning clockwise at 3 deg/s over the eye's span
 * with its 5 s fades; the banks hiding the low-air bodies; the Lamprey's latched pose at its hull spot
 * with the gulp's shield pulse, and its stun's tumble.
 */
class Level12LooksTest {
    private static final Path ASSETS = Path.of(System.getProperty("vanguard.assetsDir", "../assets"));
    private static final Content CONTENT = ContentLoader.fromClasspath();
    private static final String KEY = CONTENT.levelKey(12).orElseThrow();
    private static final LevelData LEVEL = CONTENT.level(KEY);

    @Test
    void aFlashIsATenthOfASecondsSkyFlashAndAHalfSecondsReveal() {
        double[] flashes = {10, 20};
        float[] state = new float[3];
        WeatherLooks.flashAt(flashes, 0.5, 9.99, state);
        assertArrayEquals(new float[] {0, 0, -1}, state, "before the flash");
        WeatherLooks.flashAt(flashes, 0.5, 10, state);
        assertEquals(1, state[0], 1e-6, "the sky flash at full");
        assertEquals(1, state[1], 1e-6, "the banks fully lit");
        assertEquals(0, state[2]);
        WeatherLooks.flashAt(flashes, 0.5, 10.05, state);
        assertEquals(0.5, state[0], 1e-4);
        WeatherLooks.flashAt(flashes, 0.5, 10.1, state);
        assertEquals(0, state[0], 1e-6, "the sky flash is over after 0.1 s");
        assertEquals(1, state[1], 1e-6, "the reveal holds at full");
        WeatherLooks.flashAt(flashes, 0.5, 10.35, state);
        assertEquals(0.5, state[1], 1e-4, "then fades out over its last 0.3 s");
        WeatherLooks.flashAt(flashes, 0.5, 10.5, state);
        assertArrayEquals(new float[] {0, 0, -1}, state, "the reveal is over after 0.5 s");
        WeatherLooks.flashAt(flashes, 0.5, 20.2, state);
        assertEquals(1, state[2], "the second flash");
    }

    @Test
    void neverMoreThanThreeFlashesASecond() {
        double[] kept = WeatherLooks.accepted(List.of(1.0, 1.1, 1.2, 1.34, 1.5, 1.7, 5.0));
        assertArrayEquals(new double[] {1.0, 1.34, 1.7, 5.0}, kept, 1e-9);
        // Level 12's schedule passes as authored (at least 4 s apart), every one of its flashes shown.
        List<Double> at =
                LEVEL.weather().orElseThrow().lightning().orElseThrow().at();
        double[] level = WeatherLooks.accepted(at);
        assertEquals(at.size(), level.length);
        for (int i = 1; i < level.length; i++) {
            assertTrue(level[i] - level[i - 1] >= WeatherLooks.MIN_FLASH_GAP, "flash " + i);
        }
        assertEquals(
                0.5, LEVEL.weather().orElseThrow().lightning().orElseThrow().reveal(), 1e-9);
        assertTrue(WeatherLooks.SKY_SECONDS <= 0.1, "a sky flash of at most 0.1 s");
    }

    @Test
    void theBanksAreLitAndSeenThroughOnlyInAReveal() {
        assertEquals(0.45f, WeatherLooks.LIT_STRENGTH, 1e-6, "toward (190, 205, 235) at 0.45");
        assertEquals(190 / 255f, WeatherLooks.LIT.r, 1e-6);
        assertEquals(0.6f, WeatherLooks.SEE_THROUGH, 1e-6);
        // The bodies under the banks show at full see-through for the reveal's first 0.2 s.
        assertEquals(1, WeatherLooks.revealAt(0, 0.5), 1e-6);
        assertEquals(1, WeatherLooks.revealAt(0.19, 0.5), 1e-6);
        assertEquals(0.5, WeatherLooks.revealAt(0.35, 0.5), 1e-4);
        assertEquals(0, WeatherLooks.revealAt(0.5, 0.5), 1e-6);
        assertTrue(LEVEL.weather().orElseThrow().occludes(), "F1 = a: Level 12's banks hide the low-air bodies");
        LevelScript script = SimSpecs.level(CONTENT, KEY, Difficulty.MEDIUM);
        LevelScript.GroundObjectSpec canister = script.groundObjects().stream()
                .filter(LevelScript.GroundObjectSpec::lowAir)
                .findFirst()
                .orElseThrow();
        assertEquals("supply-canister", canister.look());
        assertTrue(
                Files.isRegularFile(ASSETS.resolve("sprites/supply-canister-glow_0.png")), "its glint over the bank");
        assertTrue(Files.isRegularFile(ASSETS.resolve("sprites/weather-glow_0.png")), "a hidden body's glow");
    }

    @Test
    void theRainFollowsTheKeysAndStopsInTheEye() throws IOException {
        LevelData.Weather weather = LEVEL.weather().orElseThrow();
        assertEquals(0, weather.rainAt(5), 1e-9, "the squall's first seconds are dry");
        assertTrue(weather.rainAt(60) > 0.3 && weather.rainAt(60) <= 0.4, "steady rain");
        assertEquals(1, weather.rainAt(140), 1e-9, "dense in the wall");
        for (double t = 150; t <= 175; t += 0.5) {
            assertEquals(0, weather.rainAt(t), 1e-9, "no rain in the eye at " + t);
        }
        float[] gates = new float[1000];
        for (int i = 0; i < gates.length; i++) {
            gates[i] = (i + 0.5f) / gates.length;
        }
        assertEquals(400, WeatherLooks.drops(gates, 0.4));
        assertEquals(0, WeatherLooks.drops(gates, 0));
        assertEquals(1000, WeatherLooks.drops(gates, 1));
        // The round 34 capture found it very faint (about 1 % streak pixels): more, longer streaks,
        // their brightest pixel adding the high-air cap of 40 % of white, not 40 % of their dim image.
        assertTrue(WeatherLooks.RAIN_DROPS >= 2 * 220, "at least twice the streaks");
        double steady = WeatherLooks.RAIN_DROPS * 0.4 * WeatherLooks.stretch(0.4);
        double wall = WeatherLooks.RAIN_DROPS * WeatherLooks.stretch(1);
        assertTrue(wall >= 3 * steady, "the wall clearly denser than the steady rain: " + wall / steady);
        assertEquals(1, WeatherLooks.stretch(0), 1e-6);
        assertEquals(1.5, WeatherLooks.stretch(1), 1e-6, "the wall's streaks half as long again");
        float rainPeak = peak("weather-rain", 4);
        assertEquals(WeatherLooks.RAIN_PEAK, rainPeak, 1e-6, "the streak images' brightest pixel");
        assertTrue(WeatherLooks.RAIN_OPACITY * rainPeak <= 0.4f + 1e-6, "high-air at most 40 %");
        assertTrue(WeatherLooks.RAIN_OPACITY > 0.4f, "brighter than step 6's 40 % of the image");
        float streakPeak = peak("weather-gust-streak", 2);
        assertEquals(WeatherLooks.STREAK_PEAK, streakPeak, 1e-6);
        assertTrue(WeatherLooks.STREAK_OPACITY * streakPeak <= 0.4f + 1e-6, "the wind streaks too");
    }

    @Test
    void theGustArrowIsBigAndClearOfTheEdgeWarningsAndTheWaveBanner() throws IOException {
        float h = 40 * WeatherLooks.ARROW_SCALE;
        float w = 64 * WeatherLooks.ARROW_SCALE;
        assertTrue(WeatherLooks.ARROW_SCALE >= 2, "twice its image's size");
        assertTrue(WeatherLooks.ARROW_PASSES >= 2, "brighter: drawn twice, additive");
        float bottom = WeatherLooks.ARROW_DOWN + h / 2;
        // A side warning's bar runs 120 to 420 px below the top edge, its glow 21 px further out.
        assertTrue(bottom <= 120 - 20, "above the side warnings' bar and glow: " + bottom);
        assertTrue(bottom < PlayFieldBanner.TOP, "above the wave banner's band");
        assertTrue(WeatherLooks.ARROW_DOWN - h / 2 >= 0, "inside the top edge");
        assertTrue(WeatherLooks.arrowCentre(1) - w / 2 >= 0, "inside the left edge");
        assertTrue(WeatherLooks.arrowCentre(-1) + w / 2 <= vanguard.sim.PlayField.WIDTH, "inside the right edge");
        assertEquals(7, WeatherLooks.STREAK_ROWS.length, "more wind streaks");
        assertEquals(2, WeatherLooks.STREAK_THICKNESS, 1e-6, "twice as thick");
        for (int frame = 0; frame < 6; frame++) {
            BufferedImage image = image("weather-gust-arrow_" + frame);
            assertEquals(64, image.getWidth());
            assertEquals(40, image.getHeight());
        }
    }

    /** The wave banner's band, px below the top edge (its middle 164 px down, 30 px tall). */
    private static final class PlayFieldBanner {
        static final float TOP = vanguard.sim.PlayField.HEIGHT - WaveBanners.CENTRE_Y - WaveBanners.HEIGHT / 2f;
    }

    /** The brightest channel of {@code frames} frames of a sprite as drawn additive, 0..1. */
    private static float peak(String sprite, int frames) throws IOException {
        int peak = 0;
        for (int frame = 0; frame < frames; frame++) {
            BufferedImage image = image(sprite + "_" + frame);
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    // Drawn additive: each channel weighted by the pixel's alpha.
                    int argb = image.getRGB(x, y);
                    int channel = Math.max((argb >> 16) & 0xff, Math.max((argb >> 8) & 0xff, argb & 0xff));
                    peak = Math.max(peak, channel * ((argb >>> 24) & 0xff) / 255);
                }
            }
        }
        return peak / 255f;
    }

    private static BufferedImage image(String name) throws IOException {
        return ImageIO.read(ASSETS.resolve("sprites/" + name + ".png").toFile());
    }

    @Test
    void theEyesWallTurnsClockwiseAtThreeDegreesASecondThroughTheEye() {
        double[] eye = WeatherLooks.eye(LEVEL);
        assertEquals(150, eye[0], 1e-9);
        assertEquals(175, eye[1], 1e-9);
        assertEquals(0, WeatherLooks.wallDegrees(150, 150), 1e-6);
        assertEquals(-30, WeatherLooks.wallDegrees(160, 150), 1e-4, "libGDX turns counter-clockwise: -30 is clockwise");
        double ramp = LEVEL.backdrop().ramp();
        assertEquals(5, ramp, 1e-9);
        assertEquals(0, WeatherLooks.window(147, eye[0], eye[1], ramp), 1e-9, "not before the fade");
        assertEquals(0.5, WeatherLooks.window(150, eye[0], eye[1], ramp), 1e-9, "half in at the eye's start");
        assertEquals(1, WeatherLooks.window(162, eye[0], eye[1], ramp), 1e-9);
        assertEquals(0.5, WeatherLooks.window(175, eye[0], eye[1], ramp), 1e-9);
        assertEquals(0, WeatherLooks.window(178, eye[0], eye[1], ramp), 1e-9);
        assertEquals(0, WeatherLooks.window(100, Double.NaN, Double.NaN, ramp), 1e-9, "a level without an eye");
        for (String look : List.of("eye-wall", "eye-shaft", "breakout-light")) {
            assertTrue(Files.isRegularFile(ASSETS.resolve("backdrop/level-12/" + look + ".png")), look);
        }
    }

    @Test
    void theLampreysLatchedFramesGoBySpotAndGulpWithTheShieldsPulse() throws Exception {
        JsonValue pivots = new JsonReader().parse(new FileHandle(new File(ASSETS.toFile(), "pivots/lamprey.json")));
        assertArrayEquals(LampreyLooks.GULP, pivots.get("gulp_pulse").asFloatArray(), 1e-6f, "latch look a");
        assertEquals(EnemySpec.Latch.RIGHT * 4 + 2, LampreyLooks.latchedIndex(EnemySpec.Latch.RIGHT, 12, 0));
        // 10 fps: a frame every 6 steps, each unit at its own phase.
        assertEquals(0, LampreyLooks.gulpFrame(5, 0));
        assertEquals(1, LampreyLooks.gulpFrame(6, 0));
        assertEquals(3, LampreyLooks.gulpFrame(0, 3));
        assertEquals(0, LampreyLooks.gulpFrame(24, 0), "a 0.4 s gulp");
        for (int frame = 0; frame < 12; frame++) {
            assertTrue(
                    Files.isRegularFile(ASSETS.resolve("sprites/lamprey-latched_" + frame + ".png")),
                    "latched " + frame);
        }
        assertEquals(0, LampreyLooks.stunnedIndex(0, 8));
        assertEquals(4, LampreyLooks.stunnedIndex(0.5, 8));
        assertEquals(7, LampreyLooks.stunnedIndex(1, 8), "the last frame at the stun's end");
        for (int frame = 0; frame < 8; frame++) {
            assertTrue(
                    Files.isRegularFile(ASSETS.resolve("sprites/lamprey-stunned_" + frame + ".png")),
                    "stunned " + frame);
        }
    }

    @Test
    void aLatchedLampreySitsAtItsSpotOnTheHull() {
        Sortie sortie = new Sortie(
                1,
                SimSpecs.starterLoadout(CONTENT, Difficulty.MEDIUM),
                SimSpecs.level(CONTENT, KEY, Difficulty.MEDIUM),
                SimSpecs.rules(CONTENT, KEY, Difficulty.MEDIUM),
                60);
        Enemy latched = null;
        for (int t = 0; t < SimStep.ticks(60) && latched == null; t++) {
            sortie.step(0);
            if (sortie.events().count(SimEvents.Type.LAMPREY_LATCHED) > 0) {
                for (int i = 0; i < sortie.enemyCount(); i++) {
                    if (sortie.enemy(i).latched()) {
                        latched = sortie.enemy(i);
                    }
                }
            }
        }
        assertTrue(latched != null, "the introduction's Lamprey latched on");
        assertTrue(sortie.draining(), "the HUD's SHIELD DRAIN");
        EnemySpec.Spot spot = latched.spec().latch().orElseThrow().spots().get(latched.spot());
        assertEquals(sortie.ship().x() + spot.dx(), latched.renderX(1), 1e-6);
        assertEquals(sortie.ship().y() + spot.dy(), latched.renderY(1), 1e-6);
        assertEquals(Enemy.LatchPhase.LATCHED, latched.latchPhase());
    }
}
