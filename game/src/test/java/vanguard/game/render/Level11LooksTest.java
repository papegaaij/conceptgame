package vanguard.game.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import vanguard.sim.Enemy;
import vanguard.sim.Layer;
import vanguard.sim.SetPiece;
import vanguard.sim.SimEvents;
import vanguard.sim.SlamArena;
import vanguard.sim.Sortie;

/**
 * M5 part E: what the game's water and Kraken looks read of the simulation, on the arena fixture
 * ({@link ArenaFixture}: Level 11's data comes in step E3a) flown without input, and the rules they
 * draw by: the {@code sub} pass's colour sums, the arm's segments and headings, the head's surfacing
 * and up frames, the lane telegraph's churn and the slam's splash, the jelly's swap and quickening
 * pulse, the ships' wakes and sinking, the foreshadowing.
 */
class Level11LooksTest {
    @Test
    void theSubPassTintsTowardTheWaterAsTheArtScriptsStandIn() {
        // tools/art/driftjelly.py, sub_pass: rgb x (1 - 0.45) x 0.85 + WATER_TINT x 0.45, alpha x 0.85.
        float[] white = WaterLooks.tinted(1, 1, 1, 1);
        assertEquals((255 * 0.55 * 0.85 + 6 * 0.45) / 255, white[0], 1e-4);
        assertEquals((255 * 0.55 * 0.85 + 40 * 0.45) / 255, white[1], 1e-4);
        assertEquals((255 * 0.55 * 0.85 + 78 * 0.45) / 255, white[2], 1e-4);
        assertEquals(0.85, white[3], 1e-4);
        float[] black = WaterLooks.tinted(0, 0, 0, 0.5f);
        assertTrue(black[2] > black[1] && black[1] > black[0], "a dark part turns toward the deep blue");
        for (int row = 0; row < 540; row += 7) {
            assertTrue(Math.abs(WaterLooks.sway(row, row * 0.01)) <= WaterLooks.SWAY_PX + 1e-9, "a gentle sway");
        }
    }

    @Test
    void theArmsSegmentsTaperAndTurnAsTheArtScriptsIndexThem() {
        assertEquals(0, KrakenLooks.headingIndex(0, -1), "straight down");
        assertEquals(8, KrakenLooks.headingIndex(-1, 0), "a quarter clockwise on the screen: left");
        assertEquals(16, KrakenLooks.headingIndex(0, 1), "up");
        assertEquals(24, KrakenLooks.headingIndex(1, 0), "right");
        assertEquals(42, KrakenLooks.taperedSize(0), 1e-6);
        assertEquals(14, KrakenLooks.taperedSize(1), 1e-6);
        assertEquals(0, KrakenLooks.taperIndex(KrakenLooks.taperedSize(0)));
        assertEquals(KrakenLooks.TAPERS.length - 1, KrakenLooks.taperIndex(KrakenLooks.taperedSize(1)));
        assertEquals(3, KrakenLooks.taperIndex(28), "the nearest taper (27 px)");
    }

    @Test
    void theHeadSurfacesThroughItsTwelveStepsAndOpensItsEyesBeforeTheBeakGlows() {
        int last = -1;
        for (int i = 0; i <= 100; i++) {
            int frame = KrakenLooks.surfaceFrame(i / 100.0);
            assertTrue(frame >= last && frame <= 11, "forward as it rises");
            last = frame;
        }
        assertEquals(0, KrakenLooks.surfaceFrame(0));
        assertEquals(11, KrakenLooks.surfaceFrame(1));
        assertEquals(KrakenLooks.EYES_HALF, KrakenLooks.upFrame(0, false, 0), "the eyes half open as it comes up");
        assertEquals(KrakenLooks.EYES_OPEN, KrakenLooks.upFrame(KrakenLooks.HALF_OPEN_TICKS, false, 0));
        assertEquals(KrakenLooks.GLOW_HALF, KrakenLooks.upFrame(200, true, 0), "the tell's first half");
        assertEquals(KrakenLooks.GLOW_FULL, KrakenLooks.upFrame(200, true, KrakenLooks.GLOW_HALF_TICKS));
    }

    @Test
    void theChurnFadesInAndOutlastsTheImpactAndTheSplashRunsDownTheArm() {
        assertEquals(0, KrakenLooks.churnStrength(10, Long.MIN_VALUE, Long.MIN_VALUE), "no telegraph");
        assertTrue(KrakenLooks.churnStrength(100, 100, Long.MIN_VALUE) < 0.1, "fading in");
        assertEquals(1, KrakenLooks.churnStrength(100 + KrakenLooks.CHURN_FADE_TICKS, 100, Long.MIN_VALUE), 1e-6);
        assertEquals(1, KrakenLooks.churnStrength(170, 100, 160), 1e-6, "just after the impact");
        assertEquals(0, KrakenLooks.churnStrength(160 + KrakenLooks.CHURN_TAIL_TICKS, 100, 160), "gone");
        assertEquals(0, KrakenLooks.splashFrame(0, 0), "the base splashes at the impact");
        assertEquals(-1, KrakenLooks.splashFrame(0, 1), "the next segment a frame later");
        assertEquals(0, KrakenLooks.splashFrame(KrakenLooks.SPLASH_FRAME_TICKS, 1));
        assertEquals(2, KrakenLooks.splashFrame(4 * KrakenLooks.SPLASH_FRAME_TICKS, 2));
    }

    @Test
    void theForeshadowingSlidesUnderTheConvoyAroundFiftyFiveSeconds() {
        assertEquals(-1, KrakenLooks.foreshadowShare(50), 1e-9);
        assertEquals(-1, KrakenLooks.foreshadowShare(60), 1e-9);
        assertTrue(KrakenLooks.foreshadowShare(55) > 0.3 && KrakenLooks.foreshadowShare(55) < 0.6);
        assertEquals(0, KrakenLooks.foreshadowOpacity(0), 1e-6);
        assertEquals(0, KrakenLooks.foreshadowOpacity(1), 1e-6, "faded into the deep");
        assertTrue(KrakenLooks.foreshadowOpacity(0.4) > 0.8);
    }

    @Test
    void theKrakensHitFlashDoesNotRestartUnderSustainedFireAndKeepsTheEyesReadable() {
        // Round 33's capture: a hit every step kept the head white for seconds; now at most one flash
        // of two steps each quarter second, at half strength.
        HitFlash flash = new HitFlash();
        int on = 0;
        for (long tick = 0; tick < 180; tick++) {
            if (flash.on(0, tick, 0)) {
                on++;
            }
        }
        assertEquals(180 / HitFlash.COOLDOWN_TICKS * HitFlash.TICKS, on, "a blink every quarter second");
        flash.reset();
        assertTrue(flash.on(1, 100, 0) && flash.on(1, 101, 1), "a single hit flashes for two steps");
        assertFalse(flash.on(1, 102, 2));
        assertFalse(flash.on(1, 110, 0), "within the cooldown a hit does not restart it");
        assertTrue(flash.on(1, 115, 0));
        // Round 33 (user): only the part hit blinks, and subtly: a third of the way to a pale flesh tint.
        assertTrue(KrakenLooks.PART_FLASH <= 0.35f, "a subtle tint, the eyes and beak kept");
        assertTrue(KrakenLooks.FLASH_TINT.g < 1 && KrakenLooks.FLASH_TINT.b < KrakenLooks.FLASH_TINT.g, "not white");
        assertFalse(flash.on(2, 116, Integer.MAX_VALUE), "another part's hit does not flash it");
        // The plain head is opaque while it is down (darker instead of fainter), so the gripping arms'
        // under-water stretches never show through the mantle.
        assertTrue(KrakenLooks.headShade(SlamArena.SurfaceState.DOWN) < 1);
        assertEquals(1, KrakenLooks.headShade(SlamArena.SurfaceState.UP), 1e-6);
    }

    /**
     * Round 33 (user): a slamming arm is no straight line. Its lane leg bends in a travelling S that
     * whips through the rise and settles awash to a slow curl, never collinear, always inside its
     * 120 px lane, and leaving the deck's edge on the lane's centre line; the right arm mirrors it.
     */
    @Test
    void aSlammingArmCurvesAndWhipsInsteadOfLyingStraight() {
        float[] xs = new float[KrakenLooks.LANE_POINTS];
        float[] ys = new float[KrakenLooks.LANE_POINTS];
        double laneX = 180;
        double laneTop = 354;
        double before = Double.NaN;
        for (int k = 0; k <= 80; k++) {
            double w = k * 2.5 / 80;
            KrakenLooks.lanePoints(laneX, laneTop, w, 1, xs, ys, 0);
            assertEquals(laneX, xs[0], 1e-4, "it leaves the deck's edge on the lane's centre");
            double most = 0;
            for (int i = 0; i < xs.length; i++) {
                most = Math.max(most, Math.abs(xs[i] - laneX));
                assertTrue(Math.abs(xs[i] - laneX) < 60 - 21, "inside its lane, root segment and all");
            }
            assertTrue(most > (w <= 1 ? 12 : 3), "not collinear at w = " + w + ": " + most);
            double tip = xs[xs.length - 2];
            assertTrue(Double.isNaN(before) || Math.abs(tip - before) < 12, "the bend moves smoothly");
            before = tip;
        }
        assertEquals(
                -KrakenLooks.whip(0.6, 0.4, 1), KrakenLooks.whip(0.6, 0.4, -1), 1e-9, "the right arm mirrors the left");
        double rising = Math.abs(KrakenLooks.whip(0.8, 0.5, 1) - KrakenLooks.whip(0.8, 0.6, 1));
        double settled = Math.abs(KrakenLooks.whip(0.8, 1.9, 1) - KrakenLooks.whip(0.8, 2.0, 1));
        assertTrue(rising > 4 * settled, "it whips as it rises and settles awash");
        assertEquals(0, KrakenLooks.whipTime(SlamArena.ArmState.TELEGRAPH, 0.7), 1e-9, "it rises with the first bend");
        assertEquals(1.5, KrakenLooks.whipTime(SlamArena.ArmState.AWASH, 0.5), 1e-9);
    }

    @Test
    void aTorpedoReadsAsALitBackOverTheChop() {
        // tools/art/water_fx.py's torpedo body (mean colour) over the sea's mean colour in the capture.
        float[] body = {85 / 255f, 92 / 255f, 127 / 255f};
        float[] sea = {50 / 255f, 69 / 255f, 83 / 255f};
        float[] tinted = WaterLooks.tinted(body[0], body[1], body[2], 1);
        double luma = 0;
        double lit = 0;
        for (int c = 0; c < 3; c++) {
            luma += Math.abs(sea[c] + (tinted[c] - sea[c]) * tinted[3] - sea[c]);
            lit += Math.min(1, sea[c] + LevelRenderer.TORPEDO_BACK * body[c]) - sea[c];
        }
        assertTrue(luma / 3 < 0.06, "through the sub pass alone the torpedo is the sea's colour");
        assertTrue(lit / 3 > 0.25, "its lit back over the chop is a pale streak");
    }

    @Test
    void aJellySwapsThroughItsStepsAndItsPulseQuickensWithoutJumping() {
        assertEquals(0, NavalLooks.swapFrame(0, false, 6));
        assertEquals(5, NavalLooks.swapFrame(0.99, false, 6), "surfacing: forward");
        assertEquals(5, NavalLooks.swapFrame(0, true, 6), "diving: backward");
        assertEquals(0, NavalLooks.swapFrame(0.99, true, 6));
        assertEquals(NavalLooks.JELLY_FPS, NavalLooks.jellyFps(400), 1e-9, "far off: its resting pulse");
        assertEquals(NavalLooks.JELLY_QUICK_FPS, NavalLooks.jellyFps(50), 1e-9, "within its ring's reach");
        assertTrue(NavalLooks.jellyFps(140) > NavalLooks.jellyFps(170));

        PulseClock clock = new PulseClock();
        int frame = clock.frame(7, 0, NavalLooks.JELLY_FPS, 4);
        int contractions = 0;
        for (long tick = 1; tick <= 600; tick++) {
            double fps = tick < 300 ? NavalLooks.JELLY_FPS : NavalLooks.JELLY_QUICK_FPS;
            int next = clock.frame(7, tick, fps, 4);
            assertTrue(next == frame || next == (frame + 1) % 4, "one frame at a time, also as it quickens");
            contractions += clock.contracted() ? 1 : 0;
            assertEquals(next, clock.frame(7, tick, fps, 4), "the same step, the same frame");
            assertFalse(clock.contracted(), "a contraction counts once");
            frame = next;
        }
        // 5 s at one pulse a second, then 5 s at three: about 20 contractions.
        assertTrue(contractions >= 19 && contractions <= 21, "contractions: " + contractions);
    }

    @Test
    void theWakesFadeAsTheScrollHaltsAndASunkHullPlaysItsStepsOnce() {
        assertEquals(1, ConvoyLooks.wakeStrength(140, 140), 1e-6);
        assertEquals(0, ConvoyLooks.wakeStrength(0, 140), 1e-6, "halted in the arena");
        assertEquals(0.5, ConvoyLooks.wakeStrength(70, 140), 1e-6, "easing in");
        assertEquals(0, ConvoyLooks.sinkStep(0, 10));
        assertEquals(9, ConvoyLooks.sinkStep(10 * ConvoyLooks.SINK_FRAME_TICKS - 1, 10));
        assertEquals(-1, ConvoyLooks.sinkStep(10 * ConvoyLooks.SINK_FRAME_TICKS, 10), "then gone");
    }

    /**
     * The fixture flown without input: the Kraken scrolls in drawn but not present, then fights; each
     * telegraphed lane belongs to an arm in its telegraph, an arm lies along its lane from its rise to
     * its sink, the head comes up and its frames follow the arena's surfacing; the released jellies swap
     * between the surface and {@code sub}, where the looks draw their bodies through the pass and the
     * screen tells their deaths by the layer they were on.
     */
    @Test
    void theArenaFightIsWhatTheLooksRead() {
        Sortie sortie = ArenaFixture.sortie();
        boolean approached = false;
        boolean telegraphs = false;
        boolean surfaced = false;
        boolean submerged = false;
        Set<SlamArena.ArmState> states = EnumSet.noneOf(SlamArena.ArmState.class);
        AirborneWalkers walkers = new AirborneWalkers();
        for (int t = 0; t < 60 * 40 && !sortie.complete(); t++) {
            walkers.record(sortie);
            sortie.step(0);
            SetPiece piece = sortie.setPiece(0);
            SlamArena arena = piece.arena().orElseThrow();
            if (piece.approaching()) {
                approached = true;
                assertFalse(piece.present(), "scrolling in: drawn, not present");
                assertTrue(KrakenLooks.shown(piece, false));
            }
            for (int a = 0; a < arena.armCount(); a++) {
                SlamArena.ArmState state = arena.armState(a);
                states.add(state);
                if (state == SlamArena.ArmState.TELEGRAPH) {
                    int lane = arena.armFirstLane(a);
                    assertTrue((arena.telegraphed() & (1 << lane)) != 0, "its lane is telegraphed");
                    assertEquals(0, arena.armLane(a), "still at rest under the water");
                }
                if (state == SlamArena.ArmState.RISE || state == SlamArena.ArmState.AWASH) {
                    assertTrue(arena.armLane(a) > 0, "along its lane");
                    assertEquals(Layer.GROUND, piece.partLayer(arena.armPart(a)), "out of the water");
                }
            }
            SimEvents events = sortie.events();
            for (int i = 0; i < events.size(); i++) {
                telegraphs |= events.type(i) == SimEvents.Type.TELEGRAPH;
            }
            if (arena.surfaceState() == SlamArena.SurfaceState.UP) {
                surfaced = true;
                assertEquals(11, KrakenLooks.surfaceFrame(arena.surfaceShare()));
            }
            for (int i = 0; i < sortie.enemyCount(); i++) {
                Enemy enemy = sortie.enemy(i);
                assertEquals(LevelRenderer.Depth.GROUND, LevelRenderer.Depth.of(enemy), "on the sea, either way");
                if (enemy.submerged()) {
                    submerged = true;
                    assertEquals(Layer.SUB, enemy.layer());
                }
            }
        }
        assertTrue(approached, "the platform and its Kraken scroll in first");
        assertTrue(telegraphs, "slams telegraphed");
        assertTrue(states.containsAll(EnumSet.allOf(SlamArena.ArmState.class)), "an arm's whole cycle: " + states);
        assertTrue(surfaced, "the head came up in 40 s");
        assertTrue(submerged, "the released jellies swap under the water");
    }

    @Test
    void aDeathUnderTheWaterIsToldByTheLayerItWasOn() {
        AirborneWalkers walkers = new AirborneWalkers();
        walkers.add(2, 100, 200, Layer.SUB);
        assertEquals(Layer.SUB, walkers.layerOf(2, Layer.GROUND, 104, 198), "a submerged jelly bursts under the water");
        assertEquals(Layer.GROUND, walkers.layerOf(2, Layer.GROUND, 160, 200), "a surfaced one on it");
        assertTrue(walkers.underNear(110, 210, 24), "a torpedo striking it bursts under the water");
        assertFalse(walkers.underNear(160, 260, 24));
    }
}
