package vanguard.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static vanguard.sim.PartGSpecs.RELAY_X;
import static vanguard.sim.PartGSpecs.RELAY_Y;
import static vanguard.sim.PartGSpecs.count;
import static vanguard.sim.PartGSpecs.runTo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * M5 part G's target-the-objective hook modes (design/campaign Level 13, What hurts the relay; user
 * decision G1 = a): {@code alternate} (every second attack at the relay while it is closer than the
 * ship, a burst one attack), {@code in-arc} (a walker's fan centred on the relay within its armour's
 * arc, the spit on the ship), {@code always} (a diver dives at the relay's centre, its crossing deals
 * 10 once and it flies on, its fan at the relay; a mortar's lob, marker and direct hit on the relay, the
 * ring at the ship only), and {@code none} (easy's Stingers).
 */
class HookModesTest {
    private static final LevelScript.HookMode ALTERNATE = LevelScript.HookMode.ALTERNATE;
    private static final LevelScript.HookMode ALWAYS = LevelScript.HookMode.ALWAYS;

    /** A unit with {@code gun} that hovers 30 s {@code depth} px below the top edge, hooked as a "skimmer". */
    private static EnemySpec hoverer(EnemyGun gun, double depth) {
        return new EnemySpec(
                "skimmer",
                50,
                new Hitbox(22, 32),
                Layer.AIR,
                10,
                false,
                14,
                200,
                Optional.empty(),
                Optional.empty(),
                Optional.of(new EnemySpec.Hover(new Range(30, 30), new Range(depth, depth))),
                Optional.empty(),
                Optional.of(gun),
                Optional.empty(),
                Optional.empty(),
                false);
    }

    private static WaveSpec single(double t, EnemySpec enemy, WaveSpec.Edge edge) {
        return TestSpecs.wave(t, WaveSpec.Formation.SINGLE, enemy, 1, WaveSpec.Entry.FRONT, edge);
    }

    /** The bullets of the last step fired from within 8 px of (x, y), whether each was aimed at the relay. */
    private static List<Boolean> fresh(Sortie sortie, double x, double y) {
        List<Boolean> aimed = new ArrayList<>();
        for (int i = 0; i < sortie.bulletCount(); i++) {
            EnemyBullet bullet = sortie.bullet(i);
            if (Math.hypot(bullet.x() - x, bullet.y() - y) < 8) {
                aimed.add(bullet.objectiveAimed());
            }
        }
        return aimed;
    }

    /**
     * Runs to {@code until}, pressing {@code commands} while the ship is below {@code shipBelow}; per
     * shot of enemy 0 whether it went at the relay and whether the relay was closer to it than the ship.
     */
    private static List<boolean[]> shots(Sortie sortie, double until, int commands, double shipBelow) {
        List<boolean[]> shots = new ArrayList<>();
        while (sortie.levelSeconds() < until) {
            sortie.step(sortie.ship().y() < shipBelow ? commands : Command.NONE);
            if (count(sortie, SimEvents.Type.ENEMY_FIRED) > 0 && sortie.enemyCount() > 0) {
                Enemy enemy = sortie.enemy(0);
                double relay = Math.hypot(RELAY_X - enemy.x(), RELAY_Y - enemy.y());
                double ship =
                        Math.hypot(sortie.ship().x() - enemy.x(), sortie.ship().y() - enemy.y());
                for (boolean aimed : fresh(sortie, enemy.x(), enemy.y())) {
                    shots.add(new boolean[] {aimed, relay < ship});
                }
            }
        }
        return shots;
    }

    @Test
    void alternateSendsEverySecondAttackAtTheRelayWhileItIsCloserThanTheShip() {
        EnemySpec unit = hoverer(EnemyGun.aimed(1, 0.2, 1, 150, 4, false), 200);
        Sortie sortie = PartGSpecs.untouchable(PartGSpecs.level(
                Map.of("skimmer", ALTERNATE), List.of(single(50, unit, WaveSpec.Edge.LEFT)), List.of()));
        runTo(sortie, 50);
        List<boolean[]> shots = shots(sortie, 56, Command.NONE, 0);
        assertTrue(shots.size() >= 4, "attacks: " + shots.size());
        for (int i = 0; i < 4; i++) {
            assertTrue(shots.get(i)[1], "the relay is closer");
            assertEquals(i % 2 == 1, shots.get(i)[0], "attack " + (i + 1));
        }
        assertTrue(sortie.ally(0).hpShare() < 1, "the relay-aimed shots hit it");
    }

    @Test
    void alternateCountsABurstAsOneAttack() {
        EnemySpec unit = hoverer(EnemyGun.aimed(1.2, 0.2, 2, 150, 4, false), 200);
        Sortie sortie = PartGSpecs.untouchable(PartGSpecs.level(
                Map.of("skimmer", ALTERNATE), List.of(single(50, unit, WaveSpec.Edge.LEFT)), List.of()));
        runTo(sortie, 50);
        List<boolean[]> shots = shots(sortie, 56, Command.NONE, 0);
        assertTrue(shots.size() >= 8, "shots: " + shots.size());
        boolean[] expected = {false, false, true, true, false, false, true, true};
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], shots.get(i)[0], "shot " + (i + 1));
        }
    }

    @Test
    void alternateSendsEveryAttackAtTheShipWhileTheShipIsCloser() {
        EnemySpec unit = hoverer(EnemyGun.aimed(1, 0.2, 1, 150, 4, false), 100);
        Sortie sortie = PartGSpecs.untouchable(PartGSpecs.level(
                Map.of("skimmer", ALTERNATE), List.of(single(50, unit, WaveSpec.Edge.LEFT)), List.of()));
        runTo(sortie, 48);
        List<boolean[]> shots = shots(sortie, 57, Command.UP.bit(), 330);
        int checked = 0;
        for (boolean[] shot : shots) {
            if (!shot[1]) {
                assertFalse(shot[0], "the ship is closer: at the ship");
                checked++;
            }
        }
        assertTrue(checked >= 3, "attacks while the ship was closer: " + checked);
    }

    @Test
    void inArcCentresTheFanOnTheRelayWhileItLiesWithinTheArmoursArc() {
        // Walking right along the relay's height, the relay ahead of it.
        List<WaveSpec.At> toward =
                List.of(new WaveSpec.At(-30, 250), new WaveSpec.At(140, 250), new WaveSpec.At(140, 500));
        WaveSpec wave = TestSpecs.wave(
                50, WaveSpec.Formation.SINGLE, PartGSpecs.SCUTTLER, 1, WaveSpec.Entry.SIDES, WaveSpec.Edge.LEFT);
        Sortie sortie = PartGSpecs.untouchable(PartGSpecs.level(
                Map.of("scuttler", LevelScript.HookMode.IN_ARC),
                List.of(new WaveSpec(
                        wave.t(),
                        wave.formation(),
                        wave.enemy(),
                        1,
                        wave.entry(),
                        wave.edge(),
                        Optional.empty(),
                        Optional.empty(),
                        1,
                        Optional.empty(),
                        Optional.empty(),
                        List.of(),
                        Optional.empty(),
                        List.of(toward))),
                List.of()));
        runTo(sortie, 50);
        List<boolean[]> shots = shots(sortie, 54, Command.NONE, 0);
        assertEquals(5, shots.size(), "one 5-way fan");
        for (boolean[] shot : shots) {
            assertTrue(shot[0], "the fan goes at the relay");
        }
    }

    @Test
    void inArcLeavesTheFanAlongItsFacingAndTheSpitOnTheShipWithTheRelayBehindIt() {
        // Walking left away from the relay, the ship behind it to the right: the fan ahead, the spit at the ship.
        List<WaveSpec.At> away = List.of(new WaveSpec.At(170, 250), new WaveSpec.At(-60, 250));
        WaveSpec base = TestSpecs.wave(
                50, WaveSpec.Formation.SINGLE, PartGSpecs.SCUTTLER, 1, WaveSpec.Entry.FRONT, WaveSpec.Edge.NONE);
        Sortie sortie = PartGSpecs.untouchable(PartGSpecs.level(
                Map.of("scuttler", LevelScript.HookMode.IN_ARC),
                List.of(new WaveSpec(
                        base.t(),
                        base.formation(),
                        base.enemy(),
                        1,
                        base.entry(),
                        base.edge(),
                        Optional.empty(),
                        Optional.empty(),
                        1,
                        Optional.empty(),
                        Optional.empty(),
                        List.of(),
                        Optional.empty(),
                        List.of(away))),
                List.of()));
        runTo(sortie, 50);
        List<boolean[]> shots = shots(sortie, 54, Command.NONE, 0);
        assertTrue(shots.size() >= 6, "a fan and a spit: " + shots.size());
        for (boolean[] shot : shots) {
            assertFalse(shot[0], "nothing at the relay behind it");
        }
    }

    @Test
    void alwaysDivesAtTheRelaysCentreHitsItOnceAndFliesOn() {
        Sortie sortie = PartGSpecs.untouchable(PartGSpecs.level(
                Map.of("stinger", ALWAYS),
                List.of(single(50, GroundAndDiveTest.STINGER, WaveSpec.Edge.NONE)),
                List.of()));
        // The ship out of the fan's way (and its no-fire distance).
        while (sortie.levelSeconds() < 50) {
            sortie.step(sortie.ship().x() < 420 ? Command.RIGHT.bit() : Command.NONE);
        }
        boolean dove = false;
        int dives = 0;
        while (sortie.enemyCount() > 0 || !dove) {
            sortie.step(Command.NONE);
            if (sortie.enemyCount() > 0 && sortie.enemy(0).divedAtObjective()) {
                dove = true;
                assertEquals(RELAY_X, sortie.enemy(0).x(), 1e-6, "straight at the relay's centre");
            }
            dives += count(sortie, SimEvents.Type.ALLY_HIT);
            assertTrue(sortie.levelSeconds() < 60, "it leaves");
        }
        // Its fan, fired back up at the relay below it, is still on its way.
        for (int i = 0; i < SimStep.ticks(1); i++) {
            sortie.step(Command.NONE);
            dives += count(sortie, SimEvents.Type.ALLY_HIT);
        }
        assertEquals(0, sortie.kills(), "it is not destroyed on the relay");
        double lost = (1 - sortie.ally(0).hpShare()) * 600;
        assertTrue(lost >= 10 && lost <= 22, "the dive's 10 once, then its fan from below: " + lost);
        assertTrue(dives >= 2 && dives <= 4, "the dive and the fan's hits: " + dives);
    }

    @Test
    void easysUnhookedStingersDiveAtTheShipAndLeaveTheRelayAlone() {
        Sortie sortie = PartGSpecs.untouchable(PartGSpecs.level(
                Map.of(), List.of(single(50, GroundAndDiveTest.STINGER, WaveSpec.Edge.NONE)), List.of()));
        runTo(sortie, 50);
        while (sortie.levelSeconds() < 56) {
            sortie.step(Command.NONE);
            if (sortie.enemyCount() > 0) {
                assertFalse(sortie.enemy(0).divedAtObjective());
            }
        }
        assertEquals(1, sortie.ally(0).hpShare(), "it passes over the relay");
    }

    @Test
    void alwaysLobsAtTheRelayTheDirectHitOnItTheRingAtTheShipOnly() {
        LevelScript.GroundUnit floe = LevelScript.GroundUnit.fromSide(
                50, CraterNestTest.MORTAR, -1, new LevelScript.SideEntry(true, 420, 10));
        Sortie sortie =
                PartGSpecs.untouchable(PartGSpecs.level(Map.of("polyp-mortar", ALWAYS), List.of(), List.of(floe)));
        runTo(sortie, 50);
        boolean lobbed = false;
        boolean landed = false;
        while (sortie.levelSeconds() < 56 && !landed) {
            sortie.step(Command.NONE);
            if (sortie.lobCount() > 0) {
                Lob lob = sortie.lob(0);
                lobbed = true;
                assertTrue(lob.objectiveAimed());
                assertEquals(RELAY_X, lob.targetX(), 1e-9, "the marker lies on the relay");
                assertEquals(RELAY_Y, lob.targetY(), 1e-9);
            }
            if (count(sortie, SimEvents.Type.MORTAR_IMPACT) > 0) {
                landed = true;
                assertEquals(590, sortie.ally(0).hpShare() * 600, 1e-9, "the direct hit on the relay");
                for (int i = 0; i < sortie.bulletCount(); i++) {
                    assertFalse(sortie.bullet(i).objectiveAimed(), "the ring goes at nobody");
                }
            }
        }
        assertTrue(lobbed && landed);
    }

    @Test
    void hookedAttacksDoNotAllocate() {
        EnemySpec unit = hoverer(EnemyGun.aimed(1, 0.2, 2, 150, 4, false), 200);
        LevelScript.GroundUnit floe = LevelScript.GroundUnit.fromSide(
                45, CraterNestTest.MORTAR, -1, new LevelScript.SideEntry(true, 420, 10));
        LevelScript level = PartGSpecs.level(
                Map.of("skimmer", ALTERNATE, "polyp-mortar", ALWAYS, "stinger", ALWAYS),
                List.of(
                        single(42, unit, WaveSpec.Edge.LEFT),
                        single(43, GroundAndDiveTest.STINGER, WaveSpec.Edge.NONE)),
                List.of(floe));
        long allocated = Allocations.least(() -> PartGSpecs.untouchable(level), sortie -> {
            for (int i = 0; i < SimStep.ticks(50); i++) {
                sortie.step(Command.NONE);
            }
        });
        assertEquals(0, allocated);
    }
}
