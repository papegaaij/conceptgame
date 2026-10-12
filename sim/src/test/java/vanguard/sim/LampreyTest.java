package vanguard.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * M5 part F, the Lamprey (design/enemies/air/lamprey; user decisions F3, F4 and F9 = a): the chase, the
 * latch into a free slot at its spot, the drain on the shield only, the shake-off by commanded
 * reversals, the stun and the flight, the hit rule while latched, the chaser swarm and the rear
 * stream. A fixture Lamprey with the stat block's medium numbers (the data comes in step 4).
 */
class LampreyTest {
    /** The latched pose's spots from assets/pivots/lamprey.json (image y down), here y up. */
    static final List<EnemySpec.Spot> SPOTS =
            List.of(new EnemySpec.Spot(-22, -13), new EnemySpec.Spot(21, -13), new EnemySpec.Spot(0, -35));

    static final EnemySpec.Latch LATCH = new EnemySpec.Latch(1, SPOTS, 2, 4, 1.2, 1, 120, 8);

    static final EnemySpec LAMPREY = lamprey(LATCH);

    static EnemySpec lamprey(EnemySpec.Latch latch) {
        return new EnemySpec(
                        "lamprey",
                        3,
                        new Hitbox(20, 28),
                        Layer.AIR,
                        6,
                        true,
                        12,
                        220,
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        false)
                .withChase(new EnemySpec.Chase(220, Math.toRadians(180), 4), Optional.of(latch));
    }

    private static Sortie single(Loadout loadout, double seconds, WaveSpec... waves) {
        return new Sortie(1, loadout, TestSpecs.level(seconds, List.of(waves)), TestSpecs.RULES, TestSpecs.FULL_ARMOUR);
    }

    private static WaveSpec lampreyAt(double t) {
        return TestSpecs.wave(t, WaveSpec.Formation.SINGLE, LAMPREY, 1, WaveSpec.Entry.FRONT, WaveSpec.Edge.NONE);
    }

    private static Enemy first(Sortie sortie) {
        return sortie.enemyCount() > 0 ? sortie.enemy(0) : null;
    }

    /** Steps without commands until the first unit latched; returns the steps taken. */
    private static int untilLatched(Sortie sortie) {
        for (int i = 1; i <= 600; i++) {
            sortie.step(Command.NONE);
            if (sortie.latched() > 0) {
                return i;
            }
        }
        throw new AssertionError("no latch");
    }

    @Test
    void itChasesWithinItsTurnRateThenFliesStraightOn() {
        // A Lamprey down the left third; nothing touches the ship (the debug rule), so it chases its 4 s.
        Sortie sortie = new Sortie(
                1,
                TestSpecs.LOADOUT,
                TestSpecs.level(
                        20,
                        List.of(TestSpecs.wave(
                                0, WaveSpec.Formation.SINGLE, LAMPREY, 1, WaveSpec.Entry.FRONT, WaveSpec.Edge.LEFT))),
                TestSpecs.RULES.withInvulnerableShip(),
                TestSpecs.FULL_ARMOUR);
        sortie.step(Command.NONE);
        Enemy lamprey = first(sortie);
        assertEquals(Enemy.LatchPhase.ENTER, lamprey.latchPhase());
        double last = Double.NaN;
        int chasing = 0;
        double most = Math.toRadians(180) * SimStep.SECONDS + 1e-9;
        while (lamprey.latchPhase() != Enemy.LatchPhase.ON) {
            sortie.step(Command.NONE);
            if (lamprey.latchPhase() == Enemy.LatchPhase.CHASE) {
                if (!Double.isNaN(last)) {
                    assertTrue(Math.abs(Math.IEEEremainder(lamprey.facing() - last, 2 * Math.PI)) <= most);
                }
                last = lamprey.facing();
                chasing++;
            }
            assertEquals(0, sortie.latched(), "nothing touches the ship");
        }
        assertEquals(SimStep.ticks(4), chasing, "it chases for its 4 s");
        double x = lamprey.x();
        double y = lamprey.y();
        sortie.step(Command.NONE);
        double vx = lamprey.x() - x;
        double vy = lamprey.y() - y;
        assertEquals(220 * SimStep.SECONDS, Math.hypot(vx, vy), 1e-6, "straight on at its speed");
    }

    @Test
    void itLatchesAtItsSpotMovesWithTheShipAndDrainsTheShieldOnly() {
        Sortie sortie = single(TestSpecs.LOADOUT, 30, lampreyAt(0));
        untilLatched(sortie);
        Enemy lamprey = first(sortie);
        assertEquals(EnemySpec.Latch.LEFT, lamprey.spot(), "from above, left of the centre line (a tie)");
        assertEquals(1, sortie.events().count(SimEvents.Type.LAMPREY_LATCHED));
        assertTrue(sortie.draining());
        Defences defences = sortie.ship().defences();
        double shield = defences.shield();
        for (int i = 0; i < 30; i++) {
            sortie.step(Command.of(Command.RIGHT));
            assertEquals(sortie.ship().x() - 22, lamprey.x(), 1e-9, "it moves with the ship");
            assertEquals(sortie.ship().y() - 13, lamprey.y(), 1e-9);
        }
        assertEquals(shield - 8 * 0.5, defences.shield(), 1e-6, "8 shield points a second, regeneration paused");
        for (int i = 0; i < 600; i++) {
            sortie.step(Command.NONE);
        }
        assertEquals(0, defences.shield(), "the drain stops at the break and the shield stays down");
        assertTrue(defences.broken());
        assertEquals(TestSpecs.FULL_ARMOUR, defences.armour(), "never the armour");
        assertEquals(1, sortie.latched());
    }

    @Test
    void withEverySlotTakenATouchIsAContact() {
        Sortie sortie = single(TestSpecs.LOADOUT, 30, lampreyAt(0), lampreyAt(1.5));
        untilLatched(sortie);
        Defences defences = sortie.ship().defences();
        boolean destroyed = false;
        for (int i = 0; i < 300 && !destroyed; i++) {
            sortie.step(Command.NONE);
            destroyed = sortie.events().count(SimEvents.Type.ENEMY_DESTROYED) > 0;
        }
        assertTrue(destroyed, "the second one rammed and died");
        assertTrue(defences.armour() < TestSpecs.FULL_ARMOUR, "a contact: half to the armour");
        assertEquals(1, sortie.latched());
    }

    @Test
    void fourQuickReversalsThrowItOffAndItNeverLatchesAgain() {
        Sortie sortie = single(TestSpecs.LOADOUT, 30, lampreyAt(0));
        untilLatched(sortie);
        Enemy lamprey = first(sortie);
        int shaken = 0;
        // Left and right, 0.1 s each, with neutral steps between that do not count.
        for (int i = 0; i < 60 && shaken == 0; i++) {
            int phase = i / 6;
            int commands = i % 6 == 5 ? Command.NONE : Command.of(phase % 2 == 0 ? Command.LEFT : Command.RIGHT);
            sortie.step(commands);
            shaken += sortie.events().count(SimEvents.Type.LAMPREY_SHAKEN);
            if (shaken > 0) {
                assertEquals(1, sortie.events().value(index(sortie, SimEvents.Type.LAMPREY_SHAKEN)));
                assertTrue(i < 30, "four reversals within 1.2 s: " + i);
            }
        }
        assertEquals(1, shaken);
        assertEquals(Enemy.LatchPhase.STUNNED, lamprey.latchPhase());
        assertFalse(sortie.draining());
        // Then it flees; flying into it does not latch it again.
        int latches = 0;
        boolean fled = false;
        for (int i = 0; i < 300 && sortie.enemyCount() > 0; i++) {
            sortie.step(Command.NONE);
            latches += sortie.events().count(SimEvents.Type.LAMPREY_LATCHED);
            fled |= sortie.enemyCount() > 0 && first(sortie).latchPhase() == Enemy.LatchPhase.FLEE;
        }
        assertTrue(fled);
        assertEquals(0, latches);
        assertEquals(0, sortie.enemyCount(), "it left by the nearer side edge");
    }

    @Test
    void slowReversalsDoNotShakeItOff() {
        Sortie sortie = single(TestSpecs.LOADOUT, 30, lampreyAt(0));
        untilLatched(sortie);
        for (int i = 0; i < 240; i++) {
            // A reversal every 0.5 s: never four within 1.2 s.
            sortie.step(Command.of((i / 30) % 2 == 0 ? Command.LEFT : Command.RIGHT));
            assertEquals(0, sortie.events().count(SimEvents.Type.LAMPREY_SHAKEN));
        }
        assertEquals(1, sortie.latched());
    }

    @Test
    void whileLatchedOnlyTheRearMountAndFlankingWeaponsHitItAtTwiceTheDamage() {
        // Bolts leaving a muzzle right on the left spot: by geometry every one would hit it.
        WeaponSpec atSpot = TestSpecs.bolt(
                "test-gun", 10, 2.0, 900, new Hitbox(4, 12), TestSpecs.INFINITE, 1, TestSpecs.muzzle(-22, -13, 0));
        assertEquals(1, survivors(new Armament.Mount(Armament.Slot.FRONT, atSpot, atSpot)), "front shots pass");
        assertEquals(0, survivors(new Armament.Mount(Armament.Slot.REAR, atSpot, atSpot)), "the rear mount hits");
        WeaponSpec side = atSpot.flanking(true);
        assertEquals(0, survivors(new Armament.Mount(Armament.Slot.LEFT_WING, side, side)), "a side weapon hits");
    }

    /** Latches the Lamprey, then fires {@code mount} for half a second; returns the units left. */
    private static int survivors(Armament.Mount mount) {
        Sortie sortie = single(TestSpecs.loadout(mount), 30, lampreyAt(0));
        untilLatched(sortie);
        Enemy lamprey = first(sortie);
        double hp = lamprey.hp();
        sortie.step(Command.of(Command.FIRE));
        if (sortie.enemyCount() == 1) {
            assertEquals(hp, lamprey.hp(), "no hit");
            for (int i = 0; i < 30; i++) {
                sortie.step(Command.of(Command.FIRE));
            }
        } else {
            // One bolt of 2 at x2 took its 3 HP.
            assertEquals(1, sortie.events().count(SimEvents.Type.ENEMY_DESTROYED));
        }
        return sortie.enemyCount();
    }

    @Test
    void theFirstLatchStartsItsRadioLineOnce() {
        LevelScript.RadioCue line = new LevelScript.RadioCue(
                LevelScript.CueTrigger.FIRST_LATCH, 0, "", "rook", "Shake it off!", false, "neutral");
        LevelScript level = TestSpecs.level(30, List.of(lampreyAt(0)), List.of(), List.of(line));
        Sortie sortie = new Sortie(1, TestSpecs.LOADOUT, level, TestSpecs.RULES, TestSpecs.FULL_ARMOUR);
        int started = 0;
        for (int i = 0; i < 300; i++) {
            sortie.step(Command.NONE);
            started += sortie.events().count(SimEvents.Type.RADIO);
        }
        assertEquals(1, started);
    }

    @Test
    void aSwarmOfChasersStartsInACloudWithSeededDelays() {
        WaveSpec swarm =
                TestSpecs.wave(5, WaveSpec.Formation.SWARM, LAMPREY, 6, WaveSpec.Entry.SIDES, WaveSpec.Edge.LEFT);
        List<Spawn> out = new ArrayList<>();
        Formations.plan(swarm, 0, new SplitMix64(7), out);
        assertEquals(6, out.size());
        boolean different = false;
        for (Spawn spawn : out) {
            assertEquals(SimStep.ticks(5), spawn.tick());
            assertTrue(spawn.swarm().isEmpty(), "no flock");
            assertTrue(spawn.chaseDelay() >= 0 && spawn.chaseDelay() <= Formations.CHASER_DELAY_MOST);
            assertTrue(spawn.path().x(0, 0) < 0, "outside the left edge");
            different |= spawn.chaseDelay() != out.getFirst().chaseDelay();
        }
        assertTrue(different);
    }

    @Test
    void onlyChasersStreamInFromTheRear() {
        WaveSpec rear = new WaveSpec(
                5,
                WaveSpec.Formation.STREAM,
                LAMPREY,
                3,
                WaveSpec.Entry.REAR,
                WaveSpec.Edge.ALTERNATING,
                Optional.empty(),
                Optional.empty(),
                1,
                Optional.empty(),
                Optional.of(0.5),
                List.of());
        List<Spawn> out = new ArrayList<>();
        Formations.plan(rear, 0, new SplitMix64(7), out);
        assertEquals(3, out.size());
        assertTrue(out.getFirst().path().y(0, 0) < 0, "from below the bottom edge");
        WaveSpec skitters = new WaveSpec(
                5,
                WaveSpec.Formation.STREAM,
                TestSpecs.SKITTER,
                3,
                WaveSpec.Entry.REAR,
                WaveSpec.Edge.ALTERNATING,
                Optional.empty(),
                Optional.empty(),
                1,
                Optional.empty(),
                Optional.of(0.5),
                List.of());
        assertThrows(IllegalArgumentException.class, () -> Formations.plan(skitters, 0, new SplitMix64(7), out));
    }

    @Test
    void aClockwiseCircleOrbitsTheOtherWay() {
        WaveSpec circle = new WaveSpec(
                5,
                WaveSpec.Formation.CIRCLE,
                TestSpecs.NEEDLER,
                4,
                WaveSpec.Entry.FRONT,
                WaveSpec.Edge.NONE,
                Optional.of(8.0),
                Optional.empty(),
                1,
                Optional.empty(),
                Optional.empty(),
                List.of());
        List<Spawn> plain = new ArrayList<>();
        Formations.plan(circle, 0, new SplitMix64(7), plain);
        List<Spawn> turned = new ArrayList<>();
        Formations.plan(circle.clockwise(true), 0, new SplitMix64(7), turned);
        double rate = plain.getFirst().orbit().orElseThrow().radiansPerSecond();
        assertTrue(rate > 0, "counter-clockwise on the screen, y up");
        assertEquals(-rate, turned.getFirst().orbit().orElseThrow().radiansPerSecond());
    }

    @Test
    void theHullSpotGoesByTheTouch() {
        assertEquals(EnemySpec.Latch.TAIL, EnemySpec.Latch.spotFor(5, -30));
        assertEquals(EnemySpec.Latch.LEFT, EnemySpec.Latch.spotFor(-30, -5));
        assertEquals(EnemySpec.Latch.RIGHT, EnemySpec.Latch.spotFor(30, 20));
        assertEquals(EnemySpec.Latch.LEFT, EnemySpec.Latch.spotFor(0, 20));
    }

    @Test
    void aLatchAndItsShakeOffDoNotAllocate() {
        int left = Command.LEFT.bit();
        int right = Command.RIGHT.bit();
        long allocated = Allocations.least(
                () -> single(TestSpecs.LOADOUT, 30, lampreyAt(0), lampreyAt(4), lampreyAt(8)), sortie -> {
                    for (int i = 0; i < 900; i++) {
                        sortie.step((i / 6) % 2 == 0 ? left : right);
                    }
                });
        assertEquals(0, allocated);
    }

    private static int index(Sortie sortie, SimEvents.Type type) {
        SimEvents events = sortie.events();
        for (int i = 0; i < events.size(); i++) {
            if (events.type(i) == type) {
                return i;
            }
        }
        assertNotNull(null, "no " + type);
        return -1;
    }
}
