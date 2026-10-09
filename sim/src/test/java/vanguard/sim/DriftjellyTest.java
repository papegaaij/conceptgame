package vanguard.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * M5 part E, step E2b: the Driftjelly (design/enemies/naval/driftjelly; user decision E3 = b and the
 * stated defaults of 2026-10-08). A {@code field} wave scatters its units over its area at its spacing
 * by the level's seed, entering with the sea and drifting on the current; each unit surfaces and
 * submerges on its own seeded timer on the real steps, its current layer flipping at the swap's middle;
 * a field starts about half submerged; the proximity ring fires within its reach, surfaced or
 * submerged, at most once per cooldown, and not while the ship is closer than bullets may spawn.
 */
class DriftjellyTest {
    private static final EnemySpec.Submerge SUBMERGE = new EnemySpec.Submerge(6, 10, 0.6, 0.5);
    private static final EnemySpec.ProximityRing PULSE = new EnemySpec.ProximityRing(8, 90, 4, 96, 2.5);

    /** A Driftjelly at medium (its data's numbers) with {@code submerge} and {@code ring}. */
    static EnemySpec jelly(Optional<EnemySpec.Submerge> submerge, Optional<EnemySpec.ProximityRing> ring) {
        return new EnemySpec(
                "driftjelly",
                4,
                new Hitbox(28, 28),
                Layer.GROUND,
                10,
                true,
                10,
                15,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                false,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                false,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                submerge,
                ring,
                0);
    }

    static EnemySpec jelly() {
        return jelly(Optional.of(SUBMERGE), Optional.of(PULSE));
    }

    /** A field wave of {@code count} units of {@code enemy} at {@code t}. */
    static WaveSpec field(double t, EnemySpec enemy, int count, WaveSpec.Field area) {
        return new WaveSpec(
                t,
                WaveSpec.Formation.FIELD,
                enemy,
                count,
                WaveSpec.Entry.FRONT,
                WaveSpec.Edge.NONE,
                Optional.empty(),
                Optional.empty(),
                1,
                Optional.empty(),
                Optional.empty(),
                List.of(),
                Optional.empty(),
                List.of(),
                Optional.empty(),
                "",
                Optional.of(area));
    }

    /** One section of {@code seconds} scrolling at {@code speed} px/s with the waves, over water. */
    static LevelScript level(double seconds, double speed, WaveSpec... waves) {
        return new LevelScript(
                        11,
                        2,
                        0,
                        List.of(new LevelScript.Section(seconds, speed)),
                        List.of(waves),
                        List.of(),
                        List.of(),
                        0,
                        List.of(),
                        new LevelScript.Secondary(0.8, 50),
                        List.of())
                .withWater(true);
    }

    private static Sortie sortie(long seed, LevelScript level) {
        return new Sortie(
                seed, TestSpecs.LOADOUT, level, TestSpecs.RULES.withInvulnerableShip(), TestSpecs.FULL_ARMOUR);
    }

    private static List<double[]> positions(Sortie sortie) {
        List<double[]> out = new ArrayList<>();
        for (int i = 0; i < sortie.enemyCount(); i++) {
            out.add(new double[] {sortie.enemy(i).x(), sortie.enemy(i).y()});
        }
        return out;
    }

    @Test
    void aFieldScattersItsUnitsAtLeastTheSpacingApartAboveTheTopEdgeByTheLevelsSeed() {
        WaveSpec.Field area = new WaveSpec.Field(240, 300, 200, 42, 0);
        Sortie sortie = sortie(7, level(30, 130, field(0, jelly(), 12, area)));
        sortie.step(0);

        assertEquals(12, sortie.enemyCount());
        List<double[]> at = positions(sortie);
        for (int a = 0; a < at.size(); a++) {
            double[] p = at.get(a);
            assertTrue(p[0] >= 90 && p[0] <= 390, "inside the area across: " + p[0]);
            assertTrue(p[1] > PlayField.HEIGHT, "the field enters with the sea, above the top edge: " + p[1]);
            assertTrue(p[1] < PlayField.HEIGHT + 200, "inside the area's depth: " + p[1]);
            assertTrue(sortie.enemy(a).grounded(), "on the sea, scrolling with it");
            for (int b = a + 1; b < at.size(); b++) {
                double d = Math.hypot(p[0] - at.get(b)[0], p[1] - at.get(b)[1]);
                assertTrue(d >= 42 - 1e-9, "at least the spacing apart: " + d);
            }
        }
        Sortie again = sortie(7, level(30, 130, field(0, jelly(), 12, area)));
        again.step(0);
        assertEquals(at.size(), positions(again).size());
        for (int i = 0; i < at.size(); i++) {
            assertEquals(at.get(i)[0], positions(again).get(i)[0], "the same seed scatters the same field");
        }
        Sortie other = sortie(8, level(30, 130, field(0, jelly(), 12, area)));
        other.step(0);
        assertNotEquals(at.getFirst()[0], positions(other).getFirst()[0], "another seed another scatter");
    }

    @Test
    void aFieldThatCannotHoldItsUnitsAtItsSpacingIsRejected() {
        WaveSpec.Field area = new WaveSpec.Field(240, 120, 80, 42, 0);
        assertEquals(2, area.capacity());
        assertThrows(IllegalArgumentException.class, () -> sortie(1, level(30, 130, field(0, jelly(), 3, area))));
    }

    @Test
    void itsUnitsScrollWithTheSeaAndDriftAlongTheCurrent() {
        // 30° to the right of straight down, the drift 15 px/s on top of the 130 px/s scroll.
        WaveSpec.Field area = new WaveSpec.Field(240, 200, 100, 42, Math.toRadians(30));
        Sortie sortie = sortie(3, level(30, 130, field(0, jelly(Optional.empty(), Optional.empty()), 4, area)));
        sortie.step(0);
        List<double[]> start = positions(sortie);
        for (int i = 0; i < SimStep.ticks(1); i++) {
            sortie.step(0);
        }
        List<double[]> later = positions(sortie);
        for (int i = 0; i < start.size(); i++) {
            assertEquals(15 * Math.sin(Math.toRadians(30)), later.get(i)[0] - start.get(i)[0], 1e-6, "across");
            assertEquals(-130 - 15 * Math.cos(Math.toRadians(30)), later.get(i)[1] - start.get(i)[1], 1e-6, "down");
        }
    }

    @Test
    void aFieldStartsWithItsShareSubmergedAndASubmergedUnitIsOnSub() {
        WaveSpec.Field area = new WaveSpec.Field(240, 400, 200, 42, 0);
        Sortie sortie = sortie(5, level(30, 130, field(0, jelly(), 8, area)));
        sortie.step(0);
        int under = 0;
        for (int i = 0; i < sortie.enemyCount(); i++) {
            Enemy unit = sortie.enemy(i);
            assertEquals(unit.submerged() ? Layer.SUB : Layer.GROUND, unit.layer());
            under += unit.submerged() ? 1 : 0;
        }
        assertEquals(4, under, "about half: 0.5 × 8");

        Sortie none = sortie(5, level(30, 130, field(0, jelly(Optional.empty(), Optional.of(PULSE)), 8, area)));
        none.step(0);
        for (int i = 0; i < none.enemyCount(); i++) {
            assertEquals(Layer.GROUND, none.enemy(i).layer(), "without a submerge it stays on the surface");
        }
    }

    @Test
    void eachUnitSwapsOnItsOwnSeededTimerItsLayerFlippingAtTheSwapsMiddle() {
        // A still sea (scroll 0): the units drift down at 15 px/s and stay on the screen for the test.
        WaveSpec.Field area = new WaveSpec.Field(240, 400, 100, 60, 0);
        Sortie sortie = sortie(11, level(60, 0, field(0, jelly(Optional.of(SUBMERGE), Optional.empty()), 6, area)));
        sortie.step(0);
        int units = sortie.enemyCount();
        int[] lastStart = new int[units];
        java.util.Arrays.fill(lastStart, -1);
        boolean[] wasSwapping = new boolean[units];
        Layer[] layer = new Layer[units];
        List<Integer> waits = new ArrayList<>();
        List<Integer> firsts = new ArrayList<>();
        for (int k = 0; k < units; k++) {
            layer[k] = sortie.enemy(k).layer();
        }
        int flips = 0;
        for (int tick = 1; tick <= SimStep.ticks(30); tick++) {
            sortie.step(0);
            assertEquals(units, sortie.enemyCount(), "the units stay on the screen");
            for (int k = 0; k < units; k++) {
                Enemy unit = sortie.enemy(k);
                double swap = unit.swap(1);
                boolean swapping = swap >= 0;
                if (swapping && !wasSwapping[k]) {
                    if (lastStart[k] < 0) {
                        firsts.add(tick);
                    } else {
                        waits.add(tick - lastStart[k]);
                    }
                    lastStart[k] = tick;
                }
                wasSwapping[k] = swapping;
                if (unit.layer() != layer[k]) {
                    flips++;
                    assertTrue(swapping, "the layer flips in a swap");
                    assertEquals(0.5, swap, 0.02, "at its middle");
                    layer[k] = unit.layer();
                }
            }
        }
        assertTrue(firsts.size() == units, "every unit swaps within 30 s: " + firsts);
        assertTrue(firsts.stream().allMatch(t -> t <= SimStep.ticks(10) + 1), "the first wait at most 10 s: " + firsts);
        assertTrue(firsts.stream().distinct().count() > 1, "not in step: " + firsts);
        assertFalse(waits.isEmpty());
        for (int wait : waits) {
            assertTrue(
                    wait >= SimStep.ticks(6 + 0.6) - 1 && wait <= SimStep.ticks(10 + 0.6) + 1,
                    "6–10 s between swaps, plus the swap: " + wait);
        }
        assertTrue(flips >= waits.size(), "every swap flips the layer");
    }

    /** The ring events of a still sea's run until {@code seconds}: the tick, the distance to the ship and the unit's layer. */
    private static List<double[]> rings(Sortie sortie, double seconds) {
        List<double[]> out = new ArrayList<>();
        sortie.step(0);
        for (int tick = 2; tick <= SimStep.ticks(seconds) && sortie.enemyCount() > 0; tick++) {
            sortie.step(0);
            if (sortie.events().count(SimEvents.Type.ENEMY_FIRED) > 0) {
                Enemy unit = sortie.enemy(0);
                double d = Math.hypot(
                        unit.x() - sortie.ship().x(), unit.y() - sortie.ship().y());
                out.add(new double[] {tick, d, unit.layer().ordinal(), sortie.bulletCount()});
            }
        }
        return out;
    }

    @Test
    void theRingFiresWithinItsReachOnEitherLayerAndNeverCloserThanBulletsMaySpawn() {
        for (boolean submerged : new boolean[] {false, true}) {
            // Never swapping: the unit keeps its starting layer.
            var never = new EnemySpec.Submerge(1000, 1000, 0.6, submerged ? 1 : 0);
            WaveSpec.Field area = new WaveSpec.Field(Ship.START_X, 60, 60, 42, 0);
            Sortie sortie = sortie(2, level(60, 0, field(0, jelly(Optional.of(never), Optional.of(PULSE)), 1, area)));
            List<double[]> rings = rings(sortie, 60);

            assertFalse(rings.isEmpty(), "it fires as it drifts past the ship");
            double[] first = rings.getFirst();
            assertTrue(first[1] <= 96 && first[1] > 72, "within 96 px, not under 72: " + first[1]);
            assertEquals((submerged ? Layer.SUB : Layer.GROUND).ordinal(), (int) first[2], "E3 = b: on either layer");
            assertEquals(8, (int) first[3], "an 8-bullet ring");
            for (double[] ring : rings) {
                assertTrue(ring[1] <= 96 && ring[1] >= 72, "never out of reach or too close: " + ring[1]);
            }
        }
    }

    @Test
    void theRingsBulletsSpreadEvenlyRoundTheUnitAtTheirSpeed() {
        var never = new EnemySpec.Submerge(1000, 1000, 0.6, 0);
        WaveSpec.Field area = new WaveSpec.Field(Ship.START_X, 60, 60, 42, 0);
        Sortie sortie = sortie(2, level(60, 0, field(0, jelly(Optional.of(never), Optional.of(PULSE)), 1, area)));
        for (int tick = 0; tick < SimStep.ticks(60) && sortie.bulletCount() == 0; tick++) {
            sortie.step(0);
        }
        assertEquals(8, sortie.bulletCount());
        for (int i = 0; i < sortie.bulletCount(); i++) {
            EnemyBullet bullet = sortie.bullet(i);
            assertEquals(90, Math.hypot(bullet.vx(), bullet.vy()), 1e-3);
        }
    }

    @Test
    void theRingWaitsItsCooldownFromTheLastRing() {
        // A long reach (200 px) the slow unit crosses in about 8.5 s: a ring every 2.5 s in it.
        var never = new EnemySpec.Submerge(1000, 1000, 0.6, 0);
        var wide = new EnemySpec.ProximityRing(8, 90, 4, 200, 2.5);
        WaveSpec.Field area = new WaveSpec.Field(Ship.START_X, 60, 60, 42, 0);
        Sortie sortie = sortie(2, level(60, 0, field(0, jelly(Optional.of(never), Optional.of(wide)), 1, area)));
        List<double[]> rings = rings(sortie, 60);

        assertTrue(rings.size() >= 3, "several rings while it crosses the reach: " + rings.size());
        for (int i = 1; i < rings.size(); i++) {
            double gap = rings.get(i)[0] - rings.get(i - 1)[0];
            assertTrue(gap >= SimStep.ticks(2.5), "2.5 s apart at the least: " + gap);
        }
        assertEquals(SimStep.ticks(2.5), rings.get(1)[0] - rings.get(0)[0], "the next ring as the cooldown ends");
    }

    @Test
    void aFieldOfSwappingPulsingUnitsDoesNotAllocate() {
        WaveSpec.Field area = new WaveSpec.Field(Ship.START_X, 400, 120, 42, Math.toRadians(5));
        long allocated = Allocations.least(
                () -> sortie(4, level(60, 20, field(0, jelly(), 12, area), field(20, jelly(), 10, area))), sortie -> {
                    for (int i = 0; i < SimStep.ticks(40); i++) {
                        sortie.step(Command.FIRE.bit());
                    }
                });
        assertEquals(0, allocated);
    }

    @Test
    void aFieldWaveHasItsAreaAndOnlyAFieldWaveHasOne() {
        WaveSpec.Field area = new WaveSpec.Field(240, 300, 200, 42, 0);
        assertThrows(
                IllegalArgumentException.class,
                () -> new WaveSpec(
                        0,
                        WaveSpec.Formation.SINGLE,
                        jelly(),
                        1,
                        WaveSpec.Entry.FRONT,
                        WaveSpec.Edge.NONE,
                        Optional.empty(),
                        Optional.empty(),
                        1,
                        Optional.empty(),
                        Optional.empty(),
                        List.of(),
                        Optional.empty(),
                        List.of(),
                        Optional.empty(),
                        "",
                        Optional.of(area)));
        assertThrows(
                IllegalArgumentException.class,
                () -> TestSpecs.wave(
                        0, WaveSpec.Formation.FIELD, jelly(), 1, WaveSpec.Entry.FRONT, WaveSpec.Edge.NONE));
        assertThrows(
                IllegalArgumentException.class, () -> new EnemySpec.Submerge(6, 10, 0.6, 1.5), "a share of 0 to 1");
    }
}
