package vanguard.game.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import vanguard.content.AlliesData;
import vanguard.content.Content;
import vanguard.content.ContentLoader;
import vanguard.sim.SimStep;

/**
 * M5 part D's looks by their numbers (tools/art/shuttle.py, tools/art/wraith.py, tools/art/lance_l10.py):
 * the shuttle's bank, liftoff and glide frames, the wreck's sink, the Wraith's decloak, and the
 * scripted loss's glow and lance; and the production files and pivots they index.
 */
class ShuttleLossLooksTest {
    private static final Path ASSETS = Path.of(System.getProperty("vanguard.assetsDir", "../assets"));
    private static final Content CONTENT = ContentLoader.fromClasspath();

    @Test
    void theBankFrameFollowsTheSidewaysSpeedFullAtItsFullBank() {
        // tools/art/shuttle.py: clamp(2 + round(2 v / 60), 0, 4).
        assertEquals(2, ShuttleLooks.bankFrame(0, 60, 5), "level");
        assertEquals(2, ShuttleLooks.bankFrame(14, 60, 5), "a slow drift stays level");
        assertEquals(3, ShuttleLooks.bankFrame(15, 60, 5));
        assertEquals(4, ShuttleLooks.bankFrame(60, 60, 5), "full bank right");
        assertEquals(0, ShuttleLooks.bankFrame(-60, 60, 5), "full bank left");
        assertEquals(1, ShuttleLooks.bankFrame(-30, 60, 5));
        assertEquals(4, ShuttleLooks.bankFrame(500, 60, 5), "clamped");
        assertEquals(0, ShuttleLooks.bankFrame(10, 60, 1), "a single frame");
    }

    @Test
    void theLiftoffStepsFollowTheLiftAndTheLowOnesAreUnderTheLowAirLayer() {
        assertEquals(0, ShuttleLooks.liftStep(0));
        assertEquals(0, ShuttleLooks.liftStep(0.24));
        assertEquals(1, ShuttleLooks.liftStep(0.25));
        assertEquals(2, ShuttleLooks.liftStep(0.5));
        assertEquals(3, ShuttleLooks.liftStep(0.99));
        assertEquals(3, ShuttleLooks.liftStep(1));
        assertEquals(0.70, ShuttleLooks.LIFT_SCALES[0], 1e-9, "the pad: the ground's 1 / 1.43");
        assertEquals(0.85, ShuttleLooks.LIFT_SCALES[2], 1e-9);
        assertTrue(ShuttleLooks.LOW_LIFT == 0.5, "the 0.85 step and above with the flyers");
    }

    @Test
    void theWreckGlidesIntoFarOverItsWholeGlideSinkingUpToFarsSpeedDarkening() {
        double glide = 3;
        int total = SimStep.ticks(glide);
        // 2026-10-08: the six frames down to far's scale (0.47; far 0.45), half a second each.
        assertEquals(0, ShuttleLooks.wreckStep(ShuttleLooks.glide(0, glide), 8));
        assertEquals(0, ShuttleLooks.wreckStep(ShuttleLooks.glide(29, glide), 8), "30 steps a frame");
        assertEquals(1, ShuttleLooks.wreckStep(ShuttleLooks.glide(30, glide), 8));
        assertEquals(5, ShuttleLooks.wreckStep(ShuttleLooks.glide(total - 1, glide), 8), "never past far's scale");
        assertEquals(3, ShuttleLooks.wreckStep(0.8, 4), "fewer frames: all of them");
        // Dark from the loss on, darker at the end; whole until its last 12 %.
        assertEquals(0.75f, ShuttleLooks.wreckShade(0), 1e-6);
        assertEquals(0.5f, ShuttleLooks.wreckShade(1), 1e-6);
        assertTrue(ShuttleLooks.wreckShade(0.5) < ShuttleLooks.wreckShade(0.25));
        assertEquals(1, ShuttleLooks.wreckFade(0), 1e-6);
        assertEquals(1, ShuttleLooks.wreckFade(0.88), 1e-6, "seen at full strength for 2.6 of the 3 s");
        assertEquals(0.5f, ShuttleLooks.wreckFade(0.94), 1e-6);
        assertEquals(0, ShuttleLooks.wreckFade(1), 1e-6);
        assertTrue(ShuttleLooks.WRECK_SMOKE_TICKS < ShuttleLooks.SMOKE_TICKS, "a denser plume than a damaged unit's");
        // It slides out of its column, away from the middle: clear of the shuttle flying below it.
        assertEquals(0, ShuttleLooks.drift(0, 312), 1e-9);
        assertEquals(64, ShuttleLooks.drift(1, 312), 1e-9, "Lifeline Three: to the right");
        assertEquals(-64, ShuttleLooks.drift(1, 168), 1e-9);
        assertEquals(-64, ShuttleLooks.drift(1, 240), 1e-9, "Lifeline One: to the left");
        assertTrue(ShuttleLooks.drift(0.62, 312) > 48, "past Lifeline Five's half width when level with it");
        assertEquals(1, ShuttleLooks.glide(total + 30, glide), 1e-9, "over after the glide");
        // tools/art/lance_l10.py's review: 142.5 px at 190 px/s over 3 s, far's 95 px/s at the end.
        assertEquals(142.5, ShuttleLooks.sink(1, 190, glide), 1e-9);
        assertEquals(0, ShuttleLooks.sink(0, 190, glide), 1e-9);
        double speedAtEnd = (ShuttleLooks.sink(1, 190, glide) - ShuttleLooks.sink(0.99, 190, glide)) / (0.01 * glide);
        assertEquals(95, speedAtEnd, 1);
        assertEquals(1, ShuttleLooks.wreckShadow(0), 1e-6);
        assertEquals(0.4f, ShuttleLooks.wreckShadow(3), 1e-6);
        assertEquals(0, ShuttleLooks.wreckShadow(5), 1e-6, "gone by step 5");
        assertEquals(0, ShuttleLooks.wreckShadow(7), 1e-6);
    }

    @Test
    void theShuttlesFilesAndPivotsMatchItsDataAndTheLooks() throws IOException {
        AlliesData.Ally shuttle = CONTENT.allies().allies().get("evacuation-shuttle");
        int banks = shuttle.banks().orElseThrow().frames();
        assertEquals(banks, count("evacuation-shuttle"));
        assertEquals(banks, count("evacuation-shuttle-damaged"));
        assertEquals(ShuttleLooks.LIFT_SCALES.length, count("evacuation-shuttle-lift"));
        assertEquals(8, count("evacuation-shuttle-wreck"));
        JsonValue pivots = new JsonReader().parse(Files.readString(ASSETS.resolve("pivots/evacuation-shuttle.json")));
        double[] scales = pivots.get("lift-scales").asDoubleArray();
        assertEquals(ShuttleLooks.LIFT_SCALES.length, scales.length);
        for (int i = 0; i < scales.length; i++) {
            assertEquals(ShuttleLooks.LIFT_SCALES[i], scales[i], 1e-9);
        }
        for (JsonValue engine = pivots.get("points").child; engine != null; engine = engine.next) {
            assertEquals(banks, engine.size, engine.name);
        }
        for (JsonValue engine = pivots.get("lift").child; engine != null; engine = engine.next) {
            assertEquals(ShuttleLooks.LIFT_SCALES.length, engine.size, engine.name);
        }
        assertEquals(banks, pivots.get("smoke").size);
        assertEquals(8, pivots.get("wreck-smoke").size);
        assertEquals(2, pivots.get("evacuation-shuttle-flame").get("attach").asIntArray().length);
    }

    @Test
    void theWraithDecloaksOverItsFlashTheShimmerOutInTheFirstHalf() throws IOException {
        assertEquals(0, EnemyLooks.bodyOpacity(0), 1e-6);
        assertEquals(0.5f, EnemyLooks.bodyOpacity(0.5), 1e-6);
        assertEquals(1, EnemyLooks.bodyOpacity(1), 1e-6);
        assertEquals(1, EnemyLooks.shimmerOpacity(0), 1e-6);
        assertEquals(0.5f, EnemyLooks.shimmerOpacity(0.25), 1e-6);
        assertEquals(0, EnemyLooks.shimmerOpacity(0.5), 1e-6);
        assertEquals(0, EnemyLooks.shimmerOpacity(1), 1e-6);
        // Six flash frames of four steps over the 0.4 s (24 steps).
        assertEquals(EnemyLooks.DECLOAK_TICKS, SimStep.ticks(0.4));
        assertEquals(0, EnemyLooks.decloakFrame(0, 6));
        assertEquals(1, EnemyLooks.decloakFrame(4.0 / 24, 6));
        assertEquals(5, EnemyLooks.decloakFrame(23.5 / 24, 6));
        assertEquals(-1, EnemyLooks.decloakFrame(1, 6), "played out");
        assertEquals(-1, EnemyLooks.decloakFrame(0.5, 0), "no flash frames");
        assertEquals(64, count("wraith"));
        assertEquals(64, count("wraith-cloak"), "the shimmer in the body's order");
        assertEquals(6, count("wraith-decloak"));
        assertEquals(48, count("mote-swarm"), "16 headings x 3 flicker frames");
    }

    @Test
    void theGlowPulsesFiveTimesQuickeningToItsPeakAtTheHitThenFades() {
        double glow = 2;
        assertEquals(0, LossLooks.intensity(-0.01, glow), 1e-6);
        assertEquals(0.25, LossLooks.intensity(0, glow), 1e-6, "a quarter at its start, at a peak");
        assertEquals(1, LossLooks.intensity(glow, glow), 1e-6, "the last peak exactly at the hit");
        assertEquals(1, LossLooks.intensity(glow + 1e-9, glow), 1e-6);
        assertEquals(0.25, LossLooks.intensity(glow + 0.4, glow), 1e-6, "ease out: (1 - u)^2");
        assertEquals(0, LossLooks.intensity(glow + LossLooks.FADE_SECONDS, glow), 1e-6);
        assertEquals(0, LossLooks.intensity(glow + 5, glow), 1e-6);
        int peaks = 0;
        double last = 0;
        double dt = 0.001;
        for (double tau = dt; tau < glow + 0.1; tau += dt) {
            float before = LossLooks.intensity(tau - dt, glow);
            float at = LossLooks.intensity(tau, glow);
            float after = LossLooks.intensity(tau + dt, glow);
            if (at > before && at >= after) {
                peaks++;
                last = tau;
            }
        }
        assertEquals(5, peaks, "five pulses after the start's");
        assertEquals(glow, last, 0.002, "the last at the hit");
        assertEquals(LossLooks.GLOW_SCALE_B, LossLooks.glowScale(false, 0, glow), 1e-6);
        assertEquals(1, LossLooks.glowScale(false, glow, glow), 1e-6);
        assertEquals(1, LossLooks.glowScale(true, 0, glow), 1e-6, "a's glow is not scaled");
    }

    @Test
    void theLanceAndItsFlashPlayFromTheHit() {
        assertEquals(-1, LossLooks.lanceFrame(-1, 6, 4));
        assertEquals(0, LossLooks.lanceFrame(0, 6, 4));
        assertEquals(5, LossLooks.lanceFrame(23.9, 6, 4), "a: six frames of four steps, 0.4 s");
        assertEquals(-1, LossLooks.lanceFrame(24, 6, 4));
        assertEquals(11, LossLooks.lanceFrame(35, 12, 3), "b: twelve frames of three steps, 0.6 s");
        assertEquals(-1, LossLooks.lanceFrame(36, 12, 3));
        // a's spear: its tip 72 px below its 88 px frame's top, the glow 72 px above the unit.
        assertEquals(LossLooks.GLOW_DY_A, LossLooks.LANCE_TIP_A);
    }

    @Test
    void theGameDrawsTheChosenIrisColumn() throws IOException {
        // Round 32's pick b (2026-10-08): no lance-flash, so LossLooks draws the column centred on the hit.
        assertEquals(6, count("cloud-glow"), "the iris vortex loop");
        assertEquals(12, count("lance"), "the column, its collapse and the shock rings");
        assertEquals(0, count("lance-flash"), "a's ring flash is gone");
    }

    private static int count(String name) throws IOException {
        try (Stream<Path> files = Files.list(ASSETS.resolve("sprites"))) {
            return (int) files.filter(file -> file.getFileName().toString().matches(name + "_\\d+\\.png"))
                    .count();
        }
    }
}
