package vanguard.game.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import vanguard.content.Content;
import vanguard.content.ContentLoader;
import vanguard.content.LevelData;

/**
 * M5 part G's looks (design/campaign Level 13; design/allies, Nansen Relay and CDF supply drone;
 * design/enemies/naval/skimmer): the relay's state frames by its integrity, its lights' sequence once
 * online and its smoke column's puffs; the supply drone's arc from the helipad to the drop and back,
 * alternating sides; the Skimmer's frames, wakes and sinks split as the game reads them; the arctic snow
 * by the atmosphere, falling in the whiteout and not in the clear; easy's 6 s whiteout; the water splash
 * by unit on the level's ice.
 */
class Level13LooksTest {
    private static final Path ASSETS = Path.of(System.getProperty("vanguard.assetsDir", "../assets"));
    private static final Content CONTENT = ContentLoader.fromClasspath();
    private static final String KEY = CONTENT.levelKey(13).orElseThrow();
    private static final LevelData LEVEL = CONTENT.level(KEY);

    @Test
    void theRelaysStateFollowsItsIntegrity() {
        double smoke = 0.5;
        assertEquals(RelayLooks.INTACT, RelayLooks.state(false, 1, smoke));
        assertEquals(RelayLooks.INTACT, RelayLooks.state(false, 0.5, smoke), "at half it still stands clean");
        assertEquals(RelayLooks.DAMAGED, RelayLooks.state(false, 0.49, smoke), "smoke below 50 %");
        assertEquals(RelayLooks.DAMAGED, RelayLooks.state(false, 0.25, smoke));
        assertEquals(RelayLooks.CRITICAL, RelayLooks.state(false, 0.24, smoke), "burning below 25 %");
        assertEquals(RelayLooks.DESTROYED, RelayLooks.state(true, 0, smoke), "dark at 0");
    }

    @Test
    void theRelaysLightsComeOnInSequenceThenLoop() {
        int frames = 10;
        assertEquals(-1, RelayLooks.lightsFrame(-0.1, frames), "not before the synchronisation completes");
        assertEquals(-1, RelayLooks.lightsFrame(Double.NaN, frames));
        assertEquals(0, RelayLooks.lightsFrame(0, frames));
        assertEquals(3, RelayLooks.lightsFrame(0.8, frames), "4 fps");
        assertEquals(7, RelayLooks.lightsFrame(1.99, frames), "the sequence over 2 s");
        assertEquals(8, RelayLooks.lightsFrame(2.0, frames), "then the steady loop");
        assertEquals(9, RelayLooks.lightsFrame(2.5, frames), "at 2 fps");
        assertEquals(8, RelayLooks.lightsFrame(3.0, frames));
        assertEquals(-1, RelayLooks.lightsFrame(1, 0), "no lights without their frames");
    }

    @Test
    void theRelaysSpritesAndPivotsAreWhatTheLooksRead() {
        assertFrames("nansen-relay", 4);
        assertFrames("nansen-relay-lights", 10);
        assertFrames("nansen-relay-fire", 4);
        assertFrames("nansen-relay-smoke", 8);
        JsonValue pivots =
                new JsonReader().parse(new FileHandle(new File(ASSETS.toFile(), "pivots/nansen-relay.json")));
        assertEquals(4, pivots.get("smoke").size, "smoke points per state");
        assertEquals(4, pivots.get("fire").size, "fire points per state");
        assertEquals(0, pivots.get("smoke").get(0).size, "the intact relay does not smoke");
        assertEquals(2, pivots.get("column").size);
        float[] helipad = pivots.get("helipad").asFloatArray();
        assertTrue(helipad[0] > 0 && helipad[0] < 96 && helipad[1] > 0 && helipad[1] < 96, "the helipad on the frame");
        assertEquals(
                "nansen-relay",
                LEVEL.objectives().defend().orElseThrow().ally(),
                "the defend's ally is the relay drawn");
    }

    @Test
    void aDestroyedRelaysColumnRisesThroughItsPuffsLife() {
        // A puff every 0.15 s rising 42 px/s: the column stands about 84 px tall (tools/art/relay.py).
        assertEquals(84, RelayLooks.RISE * RelayLooks.PUFF_LIFE, 1e-9);
        assertTrue(RelayLooks.PUFF_EVERY < RelayLooks.PUFF_LIFE / 8, "puffs overlap into a column");
    }

    @Test
    void aSupplyDroneFliesFromTheHelipadToItsDropAndBack() {
        float[] at = new float[2];
        float padX = 219;
        float padY = 269;
        double dropX = 240;
        double dropY = 380;
        assertFalse(DroneLooks.position(-4.01, 0, padX, padY, dropX, dropY, at), "before its arc");
        assertTrue(DroneLooks.position(-4, 0, padX, padY, dropX, dropY, at));
        assertEquals(padX, at[0], 1e-3, "it lifts off the helipad");
        assertEquals(padY, at[1], 1e-3);
        assertTrue(DroneLooks.position(0, 0, padX, padY, dropX, dropY, at));
        assertEquals(dropX, at[0], 1e-3, "it is at the drop's point as the patch is released");
        assertEquals(dropY, at[1], 1e-3);
        assertTrue(DroneLooks.position(4, 0, padX, padY, dropX, dropY, at));
        assertEquals(padX, at[0], 1e-3, "and back on the helipad");
        assertFalse(DroneLooks.position(4.01, 0, padX, padY, dropX, dropY, at), "then it has landed");
        // The first drone swings out to the right on its way up, the second to the left.
        DroneLooks.position(-2, 0, padX, padY, dropX, dropY, at);
        float first = at[0];
        DroneLooks.position(-2, 1, padX, padY, dropX, dropY, at);
        float second = at[0];
        assertTrue(first > (padX + dropX) / 2 && second < (padX + dropX) / 2, first + " / " + second);
        // Its arc rises over the relay: never below the helipad, its top above the drop.
        float top = Float.NEGATIVE_INFINITY;
        for (double t = -4; t <= 4; t += 0.05) {
            DroneLooks.position(t, 0, padX, padY, dropX, dropY, at);
            assertTrue(at[1] >= padY - 1e-3, "at " + t);
            top = Math.max(top, at[1]);
        }
        assertTrue(top >= dropY, "the patch is released at the arc's top");
    }

    @Test
    void theDronesStrobeBlinksATenthOfASecondEveryPointEight() {
        assertEquals(0, DroneLooks.lightsFrame(0, 2));
        assertEquals(0, DroneLooks.lightsFrame(0.09, 2));
        assertEquals(1, DroneLooks.lightsFrame(0.11, 2));
        assertEquals(1, DroneLooks.lightsFrame(0.79, 2));
        assertEquals(0, DroneLooks.lightsFrame(0.81, 2));
        assertEquals(-1, DroneLooks.lightsFrame(1, 0));
    }

    @Test
    void theLevelsDropsAreCarriedByTheDrone() {
        List<LevelData.Drop> drops = LEVEL.released();
        assertEquals(2, drops.size());
        for (LevelData.Drop drop : drops) {
            assertEquals(Optional.of("supply-drone"), drop.sprite());
            assertTrue(
                    drop.t() - DroneLooks.ARC_SECONDS
                            > LEVEL.objectives().defend().orElseThrow().from(),
                    "its arc starts in the halt, the relay standing");
        }
        assertFrames("supply-drone", 4);
        assertFrames("supply-drone-open", 4);
        assertFrames("supply-drone-lights", 2);
    }

    @Test
    void theSkimmersFramesWakesAndSinksSplitAsTheGameReadsThem() {
        int headings = 16;
        assertFrames("skimmer", headings * 2);
        assertFrames("skimmer-sub", headings);
        assertFrames("skimmer-wake", headings * NavalLooks.WAKE_FRAMES);
        assertFrames("skimmer-sink", NavalLooks.SINK_HEADINGS * 10);
        // The sink's heading: the nearest of 8 (every second of the 16), as tools/art/skimmer.py renders it.
        assertEquals(0, EnemyLooks.heading(0, NavalLooks.SINK_HEADINGS), "straight down");
        assertEquals(2, EnemyLooks.heading(Math.PI / 2, NavalLooks.SINK_HEADINGS), "a quarter turn clockwise");
        assertEquals(4, EnemyLooks.heading(Math.PI, NavalLooks.SINK_HEADINGS), "straight up");
    }

    @Test
    void theIceOnlySplashesUnderANavalUnit() {
        assertTrue(LEVEL.overWater(), "E1 = a: the whole field is water in the simulation");
        assertTrue(LEVEL.navalSplashOnly(), "G5 = a: the splash by unit");
        assertFalse(CONTENT.level(CONTENT.levelKey(11).orElseThrow()).navalSplashOnly(), "Level 11's sea splashes");
        assertFalse(CONTENT.level(CONTENT.levelKey(12).orElseThrow()).navalSplashOnly());
    }

    @Test
    void theArcticSnowFallsWithTheAtmosphere() {
        assertEquals(SnowLooks.ARCTIC, LEVEL.threatProfile().setting());
        SnowLooks snow = SnowLooks.of(LEVEL, false, null);
        assertEquals(0, snow.density(20), 1e-9, "no snow in the approach's light fog");
        assertEquals(0, snow.density(120), 1e-9);
        assertEquals(
                SnowLooks.DENSITY[LevelData.Atmosphere.MEDIUM.ordinal()],
                snow.density(150),
                1e-9,
                "light snow as the fog thickens");
        assertEquals(1, snow.density(182), 1e-9, "the whiteout's peak");
        assertEquals(0, snow.density(230), 1e-9, "grid online: clear");
        assertTrue(snow.density(176) > 0.3 && snow.density(176) < 1, "ramping into the peak");
        assertTrue(SnowLooks.MAX_OPACITY <= 0.4f, "the high-air cap");
        // Another setting does not snow.
        assertEquals(null, SnowLooks.of(CONTENT.level(CONTENT.levelKey(12).orElseThrow()), false, null));
    }

    @Test
    void easysWhiteoutLastsSixSecondsCentredOnTheAuthoredPeak() {
        LevelData.Peak authored =
                new LevelData.Peak(LevelData.Atmosphere.HEAVY, 176, 188, Optional.of(6.0), Optional.empty());
        LevelData.Peak easy = authored.onEasy();
        assertEquals(179, easy.from(), 1e-9);
        assertEquals(185, easy.to(), 1e-9);
        LevelData.Peak plain = new LevelData.Peak(LevelData.Atmosphere.HEAVY, 176, 188);
        assertEquals(plain, plain.onEasy(), "a peak without an easy length is the same on easy");
        assertThrows(
                () -> new LevelData.Peak(LevelData.Atmosphere.HEAVY, 176, 188, Optional.of(13.0), Optional.empty()));
        // The stretches the backdrop and the snow read on easy, with a peak that has its easy length.
        List<LevelData.Stretch> stretches = LevelData.atmosphereStretches(whiteout(authored), true);
        LevelData.Stretch peak = stretches.stream()
                .filter(stretch -> stretch.atmosphere() == LevelData.Atmosphere.HEAVY)
                .findFirst()
                .orElseThrow();
        assertEquals(179, peak.start(), 1e-9);
        assertEquals(185, peak.end(), 1e-9);
        List<LevelData.Stretch> medium = LevelData.atmosphereStretches(whiteout(authored), false);
        assertNotEquals(stretches, medium, "medium keeps the authored 176-188");
    }

    private static void assertThrows(Runnable runnable) {
        try {
            runnable.run();
        } catch (IllegalArgumentException expected) {
            return;
        }
        throw new AssertionError("an easy length longer than the peak is refused");
    }

    private static void assertFrames(String name, int count) {
        for (int i = 0; i < count; i++) {
            assertTrue(Files.isRegularFile(ASSETS.resolve("sprites/" + name + "_" + i + ".png")), name + "_" + i);
        }
        assertFalse(
                Files.isRegularFile(ASSETS.resolve("sprites/" + name + "_" + count + ".png")),
                name + ": " + count + " frames");
    }

    /** Level 13's sections with the whiteout's peak (the heavy one) replaced by {@code peak}. */
    private static List<LevelData.Section> whiteout(LevelData.Peak peak) {
        return LEVEL.sections().stream()
                .map(section -> section.peak()
                                .filter(p -> p.atmosphere() == LevelData.Atmosphere.HEAVY)
                                .isPresent()
                        ? new LevelData.Section(
                                section.name(),
                                section.end(),
                                section.speed(),
                                section.atmosphere(),
                                Optional.of(peak),
                                section.tiles(),
                                section.arena(),
                                section.ramp())
                        : section)
                .toList();
    }
}
