package vanguard.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static vanguard.sim.PartGSpecs.RELAY;
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
 * M5 part G's {@code defend} primary (design/campaign Level 13, Objective; design/allies, Nansen
 * Relay; user decisions G1, G2 and G6 = a): the relay follows the ground and stands at (240, 290) for
 * the halt; only relay-aimed hits hurt it; its loss fails the mission; its {@code ally-below} cues and
 * the {@code integrity} secondary follow its share; the synchronisation completes at t=218; the supply
 * drones' patches appear at their point.
 */
class DefendTest {
    private static final LevelScript.Secondary HALF = LevelScript.Secondary.integrity(0.5, 95, "INTEGRITY");

    private static LevelScript.RadioCue below(String share) {
        return new LevelScript.RadioCue(
                LevelScript.CueTrigger.ALLY_BELOW, 0, share, "nansen", "Below " + share, false, "grim");
    }

    private static LevelScript level(
            AllySpec relay,
            List<LevelScript.RadioCue> radio,
            LevelScript.Secondary secondary,
            List<LevelScript.PlacedPickup> placed) {
        return PartGSpecs.level(
                PartGSpecs.sections(),
                List.of(),
                List.of(),
                radio,
                Optional.of(PartGSpecs.defend(relay, Map.of())),
                secondary,
                placed);
    }

    private static LevelScript level() {
        return level(RELAY, List.of(), LevelScript.Secondary.NONE, List.of());
    }

    /** A relay-aimed bullet arriving at the relay's centre from the right this step (dealing {@code damage}). */
    private static void hitRelay(Sortie sortie, double damage) {
        Ally relay = sortie.ally(0);
        sortie.fireEnemyBullet(relay.x() + 2, relay.y(), -60, 0, damage, 0);
        sortie.step(Command.NONE);
    }

    @Test
    void theRelayScrollsInAtTheTopEdgeAndStandsAtItsPlaceForTheHalt() {
        Sortie sortie = PartGSpecs.sortie(level());
        assertTrue(sortie.defending());
        assertEquals(1, sortie.allyCount());
        double entered = Double.NaN;
        while (sortie.levelSeconds() < 40 - 1e-9) {
            sortie.step(Command.NONE);
            if (Double.isNaN(entered) && sortie.ally(0).y() - 48 < PlayField.HEIGHT) {
                entered = sortie.levelSeconds();
            }
        }
        assertEquals(37, entered, 0.3, "its footprint comes in at the top edge at about t=37");
        assertEquals(RELAY_X, sortie.ally(0).x());
        assertEquals(RELAY_Y, sortie.ally(0).y(), 1e-9, "it stands at its place when the ground halts");
        runTo(sortie, 219);
        assertEquals(RELAY_Y, sortie.ally(0).y(), 1e-9);
        runTo(sortie, 230);
        assertTrue(sortie.ally(0).y() < RELAY_Y - 1000, "after the halt it scrolls away with the ground");
    }

    @Test
    void onlyRelayAimedHitsHurtIt() {
        Sortie sortie = PartGSpecs.sortie(level());
        runTo(sortie, 50);
        Ally relay = sortie.ally(0);
        // A shot aimed at the ship crosses the relay: it passes over.
        sortie.fireEnemyBullet(relay.x() + 2, relay.y(), -60, 0, 6);
        sortie.step(Command.NONE);
        assertEquals(1, relay.hpShare());
        hitRelay(sortie, 6);
        assertEquals(594, relay.hpShare() * 600, 1e-9);
        assertEquals(1, count(sortie, SimEvents.Type.ALLY_HIT));
    }

    @Test
    void losingTheRelayFailsTheMissionButNotWithTheDebugOption() {
        List<LevelScript.RadioCue> radio = List.of(new LevelScript.RadioCue(
                LevelScript.CueTrigger.MISSION_FAILED, 0, "", "okafor", "Nansen is gone.", false, "grim"));
        Sortie sortie = PartGSpecs.sortie(level(PartGSpecs.relay(10), radio, LevelScript.Secondary.NONE, List.of()));
        runTo(sortie, 50);
        hitRelay(sortie, 12);
        assertTrue(sortie.primaryFailed());
        assertEquals(1, count(sortie, SimEvents.Type.ALLY_LOST));
        assertEquals(1, count(sortie, SimEvents.Type.PRIMARY_FAILED));
        assertEquals(1, count(sortie, SimEvents.Type.RADIO));
        assertFalse(sortie.ally(0).alive());

        Sortie debug =
                PartGSpecs.untouchable(level(PartGSpecs.relay(10), radio, LevelScript.Secondary.NONE, List.of()));
        runTo(debug, 50);
        hitRelay(debug, 12);
        assertFalse(debug.primaryFailed(), "--invulnerable sees the level to its end");
        runTo(debug, 235.1);
        assertTrue(debug.complete());
    }

    @Test
    void theAllyBelowCuesFireOncePerShareAndTheIntegritySecondaryFailsAtOnce() {
        List<LevelScript.RadioCue> radio = List.of(below("0.5"), below("0.25"));
        Sortie sortie = PartGSpecs.sortie(level(PartGSpecs.relay(100), radio, HALF, List.of()));
        runTo(sortie, 50);
        hitRelay(sortie, 40);
        assertEquals(0, count(sortie, SimEvents.Type.ALLY_BELOW));
        assertFalse(sortie.integrityFailed());
        hitRelay(sortie, 12);
        assertEquals(1, count(sortie, SimEvents.Type.ALLY_BELOW));
        assertEquals(1, count(sortie, SimEvents.Type.RADIO));
        assertEquals(1, count(sortie, SimEvents.Type.OBJECTIVE_FAILED), "below half: the secondary failed at once");
        assertTrue(sortie.integrityFailed());
        hitRelay(sortie, 4);
        assertEquals(0, count(sortie, SimEvents.Type.ALLY_BELOW), "each share once");
        hitRelay(sortie, 20);
        assertEquals(1, count(sortie, SimEvents.Type.ALLY_BELOW));
        assertEquals(25, sortie.events().value(indexOf(sortie, SimEvents.Type.ALLY_BELOW)));

        runTo(sortie, 219);
        assertFalse(sortie.secondaryMet(), "a failed integrity secondary is not met at the end");
    }

    @Test
    void theSynchronisationCompletesAtItsEndMeetingTheSecondaryAndTheRelayIsSafeAfterIt() {
        LevelScript.RadioCue met = new LevelScript.RadioCue(
                LevelScript.CueTrigger.SECONDARY_OBJECTIVE, 0, "", "nansen", "Holding above half.", false, "neutral");
        Sortie sortie = PartGSpecs.sortie(level(RELAY, List.of(met), HALF, List.of()));
        runTo(sortie, 39);
        assertEquals(178, sortie.defendSecondsLeft(), 1e-9, "the countdown waits for the start");
        assertFalse(sortie.defendStarted());
        runTo(sortie, 100);
        assertTrue(sortie.defendStarted());
        assertEquals(118, sortie.defendSecondsLeft(), 1e-9);
        int credits = sortie.credits();
        boolean completed = false;
        while (sortie.levelSeconds() < 218 - 1e-9) {
            sortie.step(Command.NONE);
            completed |= count(sortie, SimEvents.Type.DEFEND_COMPLETE) > 0;
        }
        assertTrue(completed);
        assertTrue(sortie.defendComplete());
        assertEquals(0, sortie.defendSecondsLeft());
        assertTrue(sortie.secondaryMet());
        assertEquals(
                credits + Math.round(95 * TestSpecs.SCORING.creditFactor()),
                sortie.credits(),
                "the secondary's credits");
        hitRelay(sortie, 50);
        assertEquals(1, sortie.ally(0).hpShare(), "nothing hurts it once the grid is synchronised");
    }

    @Test
    void theDefendPaysNoCreditsAndTheDebriefShowsNoEscort() {
        Sortie sortie = PartGSpecs.sortie(level());
        runTo(sortie, 235.1);
        assertTrue(sortie.complete());
        assertEquals(0, sortie.credits());
        assertEquals(LevelResult.Escort.NONE, sortie.result().escort());
    }

    @Test
    void aRetryStandsTheRelayUpAgainWithFullIntegrity() {
        List<LevelScript.RadioCue> radio = List.of(below("0.5"));
        Sortie sortie = PartGSpecs.sortie(level(PartGSpecs.relay(100), radio, HALF, List.of()));
        runTo(sortie, 50);
        hitRelay(sortie, 60);
        assertTrue(sortie.integrityFailed());
        sortie.retry(TestSpecs.FULL_ARMOUR);
        sortie.step(Command.NONE);
        assertEquals(1, sortie.ally(0).hpShare());
        assertFalse(sortie.integrityFailed());
        runTo(sortie, 50);
        hitRelay(sortie, 60);
        assertEquals(1, count(sortie, SimEvents.Type.ALLY_BELOW), "the cue starts again in the new attempt");
    }

    @Test
    void theSupplyDronesPatchAppearsAtItsPointAndDriftsAsAnyPickup() {
        LevelScript.PlacedPickup patch = new LevelScript.PlacedPickup(162, 240, 380, PickupType.ARMOUR_PATCH);
        Sortie sortie = PartGSpecs.untouchable(level(RELAY, List.of(), LevelScript.Secondary.NONE, List.of(patch)));
        assertTrue(patch.atPoint());
        runTo(sortie, 161.98);
        assertEquals(0, sortie.pickupCount());
        boolean placed = false;
        while (sortie.levelSeconds() < 162.01) {
            sortie.step(Command.NONE);
            placed |= count(sortie, SimEvents.Type.PICKUP_PLACED) > 0;
        }
        assertTrue(placed);
        assertEquals(1, sortie.pickupCount());
        assertEquals(240, sortie.pickup(0).x(), 1e-9);
        assertTrue(sortie.pickup(0).y() <= 380 && sortie.pickup(0).y() > 370, "released at (240, 380), drifting down");
    }

    @Test
    void theRulesOfADefendPrimary() {
        assertThrows(
                IllegalArgumentException.class,
                () -> PartGSpecs.level(
                        PartGSpecs.sections(), List.of(), List.of(), List.of(), Optional.empty(), HALF, List.of()),
                "an integrity secondary needs a defend primary");
        assertThrows(
                IllegalArgumentException.class,
                () -> PartGSpecs.level(
                        PartGSpecs.sections(),
                        List.of(),
                        List.of(),
                        List.of(below("0.5")),
                        Optional.empty(),
                        LevelScript.Secondary.NONE,
                        List.of()),
                "an ally-below cue needs a defend primary");
        assertThrows(IllegalArgumentException.class, () -> level(RELAY, List.of(below("half")), HALF, List.of()));
        assertThrows(
                IllegalArgumentException.class,
                () -> new LevelScript.PlacedPickup(1, 240, 600, PickupType.ARMOUR_PATCH));
        assertFalse(new LevelScript.PlacedPickup(1, 240, PickupType.ARMOUR_PATCH).atPoint());
    }

    @Test
    void theRelayAndItsCuesHashOnlyInALevelWithThem() {
        Sortie a = PartGSpecs.sortie(level());
        Sortie b = PartGSpecs.sortie(level());
        runTo(a, 50);
        runTo(b, 50);
        assertEquals(a.stateHash(), b.stateHash());
        hitRelay(a, 6);
        b.step(Command.NONE);
        assertNotEquals(a.stateHash(), b.stateHash(), "the relay's integrity is state");
    }

    @Test
    void defendingDoesNotAllocate() {
        List<LevelScript.RadioCue> radio = List.of(below("0.5"), below("0.25"));
        LevelScript level = level(PartGSpecs.relay(100), radio, HALF, List.of());
        long allocated = Allocations.least(() -> PartGSpecs.sortie(level), sortie -> {
            for (int i = 0; i < SimStep.ticks(46); i++) {
                if (i % 60 == 0 && sortie.ally(0).y() < PlayField.HEIGHT) {
                    sortie.fireEnemyBullet(
                            sortie.ally(0).x() + 2, sortie.ally(0).y(), -60, 0, 4, 0);
                }
                sortie.step(Command.NONE);
            }
        });
        assertEquals(0, allocated);
    }

    private static int indexOf(Sortie sortie, SimEvents.Type type) {
        List<Integer> found = new ArrayList<>();
        for (int i = 0; i < sortie.events().size(); i++) {
            if (sortie.events().type(i) == type) {
                found.add(i);
            }
        }
        return found.getFirst();
    }
}
