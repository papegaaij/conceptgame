package vanguard.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** The special slot and the Airstrike (design/player/specials, Airstrike (L04)). */
class AirstrikeTest {
    private static final int SPECIAL = Command.SPECIAL.bit();
    /** The numbers of design/player/specials/data.yaml. */
    static final AirstrikeSpec AIRSTRIKE =
            new AirstrikeSpec(0.6, 64, 600, new Hitbox(56, 64), 36, 0.25, 32, 100, 300, 20, 60, 150);

    private static SpecialSpec airstrike(int charges) {
        return new SpecialSpec("Airstrike", charges, 4, 0.1, AIRSTRIKE);
    }

    /** A target fixed to the ground on {@code layer}, wide enough for both bombers' carpets. */
    private static EnemySpec target(Layer layer, double hp, int bounty) {
        return new EnemySpec(
                "target-" + layer,
                hp,
                new Hitbox(120, 60),
                layer,
                0,
                false,
                bounty,
                0,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                true);
    }

    private static LevelScript level(List<LevelScript.GroundUnit> units, List<LevelScript.GroundObjectSpec> ground) {
        return new LevelScript(
                4,
                1,
                0,
                List.of(new LevelScript.Section(40, 130)),
                List.of(),
                ground,
                units,
                0,
                List.of(),
                new LevelScript.Secondary(0.8, 50),
                List.of());
    }

    private static Sortie sortie(LevelScript level, int charges) {
        return new Sortie(
                1,
                TestSpecs.LOADOUT.withSpecial(airstrike(charges)),
                level,
                TestSpecs.RULES.withInvulnerableShip(),
                TestSpecs.FULL_ARMOUR);
    }

    /** One target per layer, 100 px apart, all on the field from 2.4 s and the lowest until 4.3 s. */
    private static LevelScript layers() {
        List<LevelScript.GroundUnit> units = new ArrayList<>();
        Layer[] layers = {Layer.GROUND, Layer.LOW_AIR, Layer.AIR, Layer.HIGH_AIR};
        for (int k = 0; k < layers.length; k++) {
            units.add(new LevelScript.GroundUnit(k * 100 / 130.0, Ship.START_X, target(layers[k], 1000, 10), -1));
        }
        return level(units, List.of());
    }

    private static void runTo(Sortie sortie, double seconds, int commands) {
        while (sortie.levelSeconds() < seconds) {
            sortie.step(commands);
        }
    }

    /** Presses the special for one step, then flies on until {@code seconds}. */
    private static void callAt(Sortie sortie, double seconds) {
        runTo(sortie, seconds, Command.NONE);
        sortie.step(SPECIAL);
    }

    private static double damage(Sortie sortie, String slug) {
        for (int i = 0; i < sortie.enemyCount(); i++) {
            if (sortie.enemy(i).spec().slug().equals(slug)) {
                return 1000 - sortie.enemy(i).hp();
            }
        }
        throw new AssertionError("no " + slug);
    }

    @Test
    void theSpecialCommandIsTheLastBitSoOlderRecordingsKeepTheirs() {
        assertEquals(6, Command.SPECIAL.ordinal());
        assertEquals(Command.values().length - 1, Command.SPECIAL.ordinal());
        assertEquals(1 << 6, SPECIAL);
    }

    @Test
    void aStrikeHitsGroundAndLowAirUpToTheirCapAirLessAndNeverHighAir() {
        var sortie = sortie(layers(), 2);
        callAt(sortie, 2.4);

        assertEquals(1, sortie.special().charges());
        assertEquals(1, sortie.special().used());
        runTo(sortie, 4.3, Command.NONE);

        assertEquals(300, damage(sortie, "target-GROUND"));
        assertEquals(300, damage(sortie, "target-LOW_AIR"));
        assertEquals(60, damage(sortie, "target-AIR"));
        assertEquals(0, damage(sortie, "target-HIGH_AIR"));
    }

    @Test
    void theCapsHoldPerStrikeAndANewStrikeDealsThemAgain() {
        var sortie = sortie(
                level(
                        List.of(
                                new LevelScript.GroundUnit(3, Ship.START_X, target(Layer.GROUND, 1000, 10), -1),
                                new LevelScript.GroundUnit(2.6, Ship.START_X, target(Layer.AIR, 1000, 10), -1)),
                        List.of()),
                2);
        callAt(sortie, 2.4);
        runTo(sortie, 4.1, Command.NONE);
        assertFalse(sortie.special().busy());
        sortie.step(SPECIAL);
        runTo(sortie, 6, Command.NONE);

        assertEquals(600, damage(sortie, "target-GROUND"));
        assertEquals(120, damage(sortie, "target-AIR"));
        assertEquals(0, sortie.special().charges());
    }

    @Test
    void theBombersEnterAfterTheDelayAndEachDropsFifteenBombsThatLandWhereTheGroundCarriedThem() {
        var sortie = sortie(level(List.of(), List.of()), 1);
        runTo(sortie, 1, Command.NONE);
        sortie.step(SPECIAL);
        assertEquals(1, sortie.events().count(SimEvents.Type.SPECIAL_CALLED));
        List<Double> xs = new ArrayList<>();
        List<Double> ys = new ArrayList<>();
        int steps = 0;
        int inbound = -1;
        while (steps < SimStep.ticks(3)) {
            sortie.step(Command.NONE);
            steps++;
            SimEvents events = sortie.events();
            for (int i = 0; i < events.size(); i++) {
                if (events.type(i) == SimEvents.Type.AIRSTRIKE_INBOUND) {
                    inbound = steps;
                } else if (events.type(i) == SimEvents.Type.AIRSTRIKE_BLAST) {
                    xs.add(events.x(i));
                    ys.add(events.y(i));
                }
            }
        }

        assertEquals(SimStep.ticks(0.6), inbound);
        assertEquals(30, ys.size());
        // Released every 36 px from 18 px up, then carried 0.25 s down by the 130 px/s scroll.
        double carried = SimStep.ticks(0.25) * 130.0 / SimStep.PER_SECOND;
        for (int i = 0; i < ys.size(); i++) {
            double release = ys.get(i) + carried;
            assertEquals(0, Math.IEEEremainder(release - 18, 36), 1e-6, "blast at " + ys.get(i));
            assertTrue(xs.get(i) == Ship.START_X - 64 || xs.get(i) == Ship.START_X + 64, "x " + xs.get(i));
        }
    }

    @Test
    void theBombersStayOnTheFieldNearAnEdge() {
        var sortie = sortie(level(List.of(), List.of()), 1);
        runTo(sortie, 3, Command.LEFT.bit());
        sortie.step(SPECIAL);
        runTo(sortie, 4, Command.NONE);

        assertTrue(sortie.special().bombersIn());
        assertEquals(28, sortie.special().bomberX(0));
    }

    @Test
    void aPressWithoutAChargeOrWhileTheStrikeFliesPastItsBufferIsDenied() {
        var sortie = sortie(level(List.of(), List.of()), 1);
        callAt(sortie, 1);
        runTo(sortie, 1.5, Command.NONE);
        sortie.step(SPECIAL);
        assertEquals(1, sortie.events().count(SimEvents.Type.SPECIAL_DENIED));

        var busy = sortie(level(List.of(), List.of()), 2);
        callAt(busy, 1);
        runTo(busy, 1.5, Command.NONE);
        busy.step(SPECIAL);
        int denied = 0;
        for (int i = 0; i < SimStep.ticks(0.1) + 1; i++) {
            busy.step(SPECIAL);
            denied += busy.events().count(SimEvents.Type.SPECIAL_DENIED);
        }
        assertEquals(1, denied);
        assertEquals(1, busy.special().charges());
    }

    @Test
    void aPressJustBeforeTheBombersLeaveWaitsForThem() {
        var sortie = sortie(level(List.of(), List.of()), 2);
        callAt(sortie, 1);
        while (true) {
            sortie.step(Command.NONE);
            // Two steps before the bombers' box has left the top edge.
            double bottom = sortie.special().bomberRenderY(1) - 32 + 2 * 10;
            if (sortie.special().bombersIn() && bottom >= PlayField.HEIGHT) {
                break;
            }
        }
        sortie.step(SPECIAL);
        int called = 0;
        for (int i = 0; i < SimStep.ticks(0.1); i++) {
            sortie.step(SPECIAL);
            called += sortie.events().count(SimEvents.Type.SPECIAL_CALLED);
        }

        assertEquals(1, called);
        assertEquals(0, sortie.special().charges());
    }

    @Test
    void killsPayTheirBountyAndKeepTheChain() {
        var sortie = sortie(
                level(
                        List.of(
                                new LevelScript.GroundUnit(0, Ship.START_X, target(Layer.GROUND, 50, 12), -1),
                                new LevelScript.GroundUnit(0.4, Ship.START_X, target(Layer.LOW_AIR, 50, 12), -1)),
                        List.of()),
                1);
        callAt(sortie, 2);
        runTo(sortie, 4, Command.NONE);

        assertEquals(0, sortie.enemyCount());
        assertEquals(2, sortie.kills());
        assertEquals(2, sortie.chain());
        assertTrue(sortie.credits() >= 24, "credits " + sortie.credits());
    }

    @Test
    void hardenedGroundObjectsTakeTheBlasts() {
        var bunker = new LevelScript.GroundObjectSpec(
                0, Ship.START_X, new Hitbox(120, 40), 250, 30, Optional.empty(), 0, 0, "", true);
        var sortie = sortie(level(List.of(), List.of(bunker)), 1);
        callAt(sortie, 2);
        runTo(sortie, 4, Command.NONE);

        assertEquals(0, sortie.groundObjectCount());
        assertTrue(sortie.credits() >= 30, "credits " + sortie.credits());
    }

    @Test
    void aRetryRestoresTheLevelStartCharges() {
        var sortie = sortie(level(List.of(), List.of()), 2);
        callAt(sortie, 1);
        assertEquals(1, sortie.special().charges());
        sortie.retry(TestSpecs.FULL_ARMOUR);
        sortie.step(Command.NONE);

        assertEquals(2, sortie.special().charges());
        assertEquals(0, sortie.special().used());
        assertFalse(sortie.special().busy());
    }

    @Test
    void withoutASpecialThePressIsDeniedAndChangesNothing() {
        var plain = TestSpecs.sortie(TestSpecs.level(10, List.of()));
        var pressed = TestSpecs.sortie(TestSpecs.level(10, List.of()));
        int denied = 0;
        for (int i = 0; i < SimStep.ticks(3); i++) {
            plain.step(Command.NONE);
            pressed.step(i % 30 == 0 ? SPECIAL : Command.NONE);
            denied += pressed.events().count(SimEvents.Type.SPECIAL_DENIED);
        }

        assertEquals(plain.stateHash(), pressed.stateHash());
        assertEquals(6, denied);
    }

    @Test
    void theSameCommandsGiveTheSameHashAndAStrikeChangesIt() {
        var first = sortie(layers(), 4);
        var second = sortie(layers(), 4);
        var none = sortie(layers(), 4);
        for (int i = 0; i < SimStep.ticks(8); i++) {
            int commands = SortieTest.Pilot.commands(i) | (i % 120 == 0 ? SPECIAL : 0);
            first.step(commands);
            second.step(commands);
            none.step(SortieTest.Pilot.commands(i));
        }

        assertEquals(first.stateHash(), second.stateHash());
        assertNotEquals(first.stateHash(), none.stateHash());
    }

    @Test
    void strikesDoNotAllocate() {
        List<Sortie> flown = new ArrayList<>();
        long allocated = Allocations.least(
                () -> {
                    Sortie sortie = sortie(layers(), 4);
                    flown.add(sortie);
                    return sortie;
                },
                sortie -> {
                    for (int i = 0; i < SimStep.ticks(10); i++) {
                        sortie.step(SortieTest.Pilot.commands(i) | (i % 120 == 0 ? SPECIAL : 0));
                    }
                });

        assertEquals(0, flown.getLast().special().charges());
        assertEquals(0, allocated, "10 s allocated " + allocated + " bytes");
    }
}
