package vanguard.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import vanguard.sim.Armament;
import vanguard.sim.Command;
import vanguard.sim.Loadout;
import vanguard.sim.SetPiece;
import vanguard.sim.Sortie;

/**
 * The Brood Carrier's overhead pass against homing missiles (design/player/weapons/micro-missile-pod,
 * Behaviour): on high air a missile seeks the open sacs all round, climbs to the one it locks onto
 * at twice its turn rate and passes beneath the hull and the shut sacs, so an open sac beside the
 * ship (the first pair over a ship under the carrier) takes its hits, from the centre every sac,
 * from a flank the nearer sac of every pair; and what the autopilot's
 * missile fits do to the sacs in phase 1.
 */
class OverheadHomingTest {
    private static final int MAX_STEPS = 60 * 60 * 20;

    private final Content content = ContentLoader.fromClasspath();

    /** The plan's fit with Micro-missile Pods at {@code pods} on both wings and {@code front} up front. */
    private Loadout loadout(String front, int frontLevel, int pods) {
        return SimSpecs.loadout(
                content,
                content.systems().engines().getFirst().name(),
                List.of(
                        new SimSpecs.FittedWeapon(Armament.Slot.FRONT, front, frontLevel),
                        new SimSpecs.FittedWeapon(Armament.Slot.LEFT_WING, "micro-missile-pod", pods),
                        new SimSpecs.FittedWeapon(Armament.Slot.RIGHT_WING, "micro-missile-pod", pods)),
                content.shields().models().get(1).name(),
                content.armour().plating().get(1).name(),
                0,
                Difficulty.MEDIUM);
    }

    /**
     * The round-25 capture's case: the ship holds still under the hovering carrier, firing; every sac
     * that opens over it takes hits, the first pair beside the ship included.
     */
    @ParameterizedTest
    @CsvSource({"240, 60", "240, 110", "240, 170", "240, 250", "130, 124", "350, 124"})
    void everyOpenSacTakesHitsFromAShipUnderTheCarrier(double x, double y) {
        Loadout loadout = loadout("lance-laser", 4, 4);
        Sortie sortie = new Sortie(
                7,
                loadout,
                SimSpecs.level(content, Level07Test.LEVEL, Difficulty.MEDIUM),
                SimSpecs.rules(content, Level07Test.LEVEL, Difficulty.MEDIUM).withInvulnerableShip(),
                loadout.plating().maxArmour());
        SetPiece carrier = sortie.setPiece(0);
        int parts = -1;
        boolean[] opened = null;
        double[] hpAtOpen = null;
        double[] lost = null;
        int steps = 0;
        while (steps++ < MAX_STEPS && !(carrier.present() && carrier.phase() > 0)) {
            boolean overhead = carrier.present() && carrier.phase() == 0;
            sortie.step(overhead ? hold(sortie, x, y) : Autopilot.commands(sortie));
            if (!overhead || carrier.motion() != SetPiece.Motion.HOLD) {
                continue;
            }
            if (opened == null) {
                parts = carrier.partCount();
                opened = new boolean[parts];
                hpAtOpen = new double[parts];
                lost = new double[parts];
            }
            for (int p = 0; p < parts; p++) {
                if (carrier.partOpen(p) && !carrier.partShielded(p)) {
                    if (!opened[p]) {
                        opened[p] = true;
                        hpAtOpen[p] = carrier.partHp(p);
                    }
                    lost[p] = hpAtOpen[p] - Math.max(carrier.partHp(p), 0);
                }
            }
        }
        assertTrue(opened != null, "the carrier hovered");
        // per pair: from a flank both pods take the nearer sac of a pair
        java.util.Map<String, Double> pairs = new java.util.TreeMap<>();
        for (int p = 0; p < parts; p++) {
            if (opened[p]) {
                String name = carrier.spec().parts().get(p).name();
                pairs.merge(name.substring(0, name.lastIndexOf(' ')), lost[p], Double::sum);
            }
        }
        assertEquals(4, pairs.size(), "every pair opens while the carrier hovers");
        pairs.forEach((pair, damage) ->
                assertTrue(damage > 0, pair + " open over the ship at (" + x + ", " + y + ") and missed"));
        if (x == 240) {
            for (int p = 0; p < parts; p++) {
                if (opened[p]) {
                    assertTrue(lost[p] > 0, carrier.spec().parts().get(p).name() + " missed from the centre");
                }
            }
        }
    }

    /** Holds the ship at (x, y) with fire held. */
    private static int hold(Sortie sortie, double x, double y) {
        int commands = Command.FIRE.bit();
        if (sortie.ship().x() < x - 4) {
            commands |= Command.RIGHT.bit();
        } else if (sortie.ship().x() > x + 4) {
            commands |= Command.LEFT.bit();
        }
        if (sortie.ship().y() < y - 4) {
            commands |= Command.UP.bit();
        } else if (sortie.ship().y() > y + 4) {
            commands |= Command.DOWN.bit();
        }
        return commands;
    }

    /**
     * The autopilot's phase 1 with Micro-missile Pods at L1 and L4: the sac damage and kills it
     * reaches before the turn (printed for the balance notes in the Brood Carrier README).
     */
    @ParameterizedTest
    @CsvSource({"pulse-cannon, 3, 1", "pulse-cannon, 3, 4", "lance-laser, 4, 4"})
    void theAutopilotsMissilesBurstSacsInPhaseOne(String front, int frontLevel, int pods) {
        Sortie sortie = Level07Test.sortie(content, 2185, Difficulty.MEDIUM, loadout(front, frontLevel, pods));
        SetPiece carrier = sortie.setPiece(0);
        double share = 1;
        int kills = 0;
        int steps = 0;
        while (steps++ < MAX_STEPS && !sortie.complete()) {
            int attempt = sortie.attempt();
            Level05Test.step(sortie);
            if (sortie.attempt() != attempt) {
                share = 1;
            }
            if (carrier.present() && !carrier.destroyed() && carrier.phase() == 0) {
                share = carrier.barShare();
                kills = 0;
                for (int p = 0; p < carrier.partCount(); p++) {
                    kills += carrier.partWrecked(p) ? 1 : 0;
                }
            }
        }
        System.out.printf(
                "Level 07 medium, %s L%d + micro-missile pods L%d: phase-1 damage %.0f, sacs burst in phase 1 %d,"
                        + " bays %s, attempt %d%n",
                front,
                frontLevel,
                pods,
                (1 - share) * 3_840,
                kills,
                sortie.secondaryMet() ? "met" : sortie.secondaryFailed() ? "failed" : "open",
                sortie.attempt());
        assertTrue((1 - share) * 3_840 > 0, "missiles reach the sacs in phase 1");
    }
}
