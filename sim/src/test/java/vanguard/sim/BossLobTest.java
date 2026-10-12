package vanguard.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * M5 part H, the boss {@code lob} (design/enemies, Boss attacks and root bursts; decision H5 = a): the
 * Siege Spire's mortar roots lob the Polyp Mortar's acid from their part at the ship's position at the
 * throw, every 4 s each and staggered between the two (a turn every 2 s), a destroyed root's turn passing
 * silently; the marker shows 1.0 s, then the ring of 10 (hard 12) bursts there. The pods' orbs are
 * staggered the same way (every 1.6 s each).
 */
class BossLobTest {
    /** The settled test Spire with its pods destroyed (only the mortar roots fire). */
    private static Sortie mortarsOnly(int ring) {
        Sortie sortie = PartHSpecs.sortie(
                PartHSpecs.level(PartHSpecs.spire(2, ring, 120)), TestSpecs.RULES.withInvulnerableShip());
        PartHSpecs.until(sortie, SimEvents.Type.BOSS_SETTLED, 5, 0);
        for (int p = PartHSpecs.POD_1; p <= PartHSpecs.POD_4; p++) {
            sortie.destroyPart(0, p);
        }
        return sortie;
    }

    /** The x of every lob thrown in the next {@code seconds}, with the step it was thrown in. */
    private static List<double[]> lobs(Sortie sortie, double seconds) {
        List<double[]> lobs = new ArrayList<>();
        for (int i = 0; i < SimStep.ticks(seconds); i++) {
            sortie.step(0);
            for (int e = 0; e < sortie.events().size(); e++) {
                if (sortie.events().type(e) == SimEvents.Type.MORTAR_LOBBED) {
                    lobs.add(new double[] {sortie.events().x(e), sortie.realSeconds()});
                }
            }
        }
        return lobs;
    }

    @Test
    void theMortarRootsLobInTurnEveryFourSecondsEach() {
        Sortie sortie = mortarsOnly(10);
        SetPiece boss = sortie.setPiece(0);
        List<double[]> lobs = lobs(sortie, 8.5);
        assertEquals(4, lobs.size(), "a lob every 2 s: " + lobs.size());
        double left = boss.partX(PartHSpecs.MORTAR_LEFT);
        double right = boss.partX(PartHSpecs.MORTAR_RIGHT);
        assertEquals(left, lobs.get(0)[0], 1e-9, "the left root first");
        assertEquals(right, lobs.get(1)[0], 1e-9, "then the right one");
        assertEquals(left, lobs.get(2)[0], 1e-9);
        assertEquals(right, lobs.get(3)[0], 1e-9);
        for (int i = 1; i < lobs.size(); i++) {
            assertEquals(2, lobs.get(i)[1] - lobs.get(i - 1)[1], 1e-9, "staggered 2 s apart");
        }
    }

    @Test
    void aDestroyedRootsTurnPassesSilently() {
        Sortie sortie = mortarsOnly(10);
        sortie.destroyPart(0, PartHSpecs.MORTAR_LEFT);
        List<double[]> lobs = lobs(sortie, 8.5);
        assertEquals(2, lobs.size(), "only the right root, still every 4 s");
        assertEquals(4, lobs.get(1)[1] - lobs.get(0)[1], 1e-9);
        assertEquals(sortie.setPiece(0).partX(PartHSpecs.MORTAR_RIGHT), lobs.get(0)[0], 1e-9);
    }

    @Test
    void theMarkerLiesOnTheShipAtTheThrowAndTheRingBurstsThereASecondLater() {
        for (int ring : new int[] {10, 12}) {
            Sortie sortie = mortarsOnly(ring);
            PartHSpecs.until(sortie, SimEvents.Type.MORTAR_LOBBED, 3, Command.LEFT.bit());
            assertEquals(1, sortie.lobCount());
            Lob lob = sortie.lob(0);
            assertEquals(sortie.ship().x(), lob.targetX(), 1e-9, "the marker on the ship at the throw");
            assertEquals(sortie.ship().y(), lob.targetY(), 1e-9);
            double x = lob.targetX();
            double y = lob.targetY();
            int steps = 0;
            List<Integer> impact = List.of();
            while (impact.isEmpty()) {
                // The ship flies on: the marker stays.
                sortie.step(Command.RIGHT.bit());
                steps++;
                impact = PartHSpecs.values(sortie, SimEvents.Type.MORTAR_IMPACT);
            }
            // The throw's step is the marker's first: it shows 1.0 s, the impact in its last step.
            assertEquals(SimStep.ticks(1.0) - 1, steps, "the marker shows 1.0 s");
            int ringBullets = 0;
            for (int i = 0; i < sortie.bulletCount(); i++) {
                EnemyBullet bullet = sortie.bullet(i);
                double d = Math.hypot(bullet.x() - x, bullet.y() - y);
                if (Math.abs(d - (20 + 12) - 110 * SimStep.SECONDS) < 1e-6 || Math.abs(d - (20 + 12)) < 1e-6) {
                    ringBullets++;
                }
            }
            assertEquals(ring, ringBullets, "the ring at the marker (hard 12)");
        }
    }

    @Test
    void aShipOnTheMarkerTakesTheDirectHit() {
        Sortie sortie = PartHSpecs.sortie(PartHSpecs.level(PartHSpecs.spire()), TestSpecs.RULES);
        PartHSpecs.until(sortie, SimEvents.Type.BOSS_SETTLED, 5, 0);
        for (int p = PartHSpecs.POD_1; p <= PartHSpecs.POD_4; p++) {
            sortie.destroyPart(0, p);
        }
        PartHSpecs.until(sortie, SimEvents.Type.MORTAR_LOBBED, 3, 0);
        Defences defences = sortie.ship().defences();
        double before = defences.shield() + defences.armour();
        assertEquals(List.of(1), PartHSpecs.until(sortie, SimEvents.Type.MORTAR_IMPACT, 1.1, 0), "a direct hit");
        assertEquals(before - 10, defences.shield() + defences.armour(), 1e-9);
    }

    @Test
    void thePodsFireTheirOrbsStaggeredEveryOnePointSixSecondsEach() {
        Sortie sortie = PartHSpecs.sortie(PartHSpecs.level(PartHSpecs.spire()), TestSpecs.RULES.withInvulnerableShip());
        PartHSpecs.until(sortie, SimEvents.Type.BOSS_SETTLED, 5, 0);
        sortie.destroyPart(0, PartHSpecs.MORTAR_LEFT);
        sortie.destroyPart(0, PartHSpecs.MORTAR_RIGHT);
        sortie.destroyPart(0, PartHSpecs.POD_2);
        List<Double> times = new ArrayList<>();
        for (int i = 0; i < SimStep.ticks(3.3); i++) {
            sortie.step(0);
            if (!PartHSpecs.values(sortie, SimEvents.Type.ENEMY_FIRED).isEmpty()) {
                times.add(sortie.realSeconds());
            }
        }
        // Turns every 0.4 s; pod 2's turn (every fourth) passes silently.
        assertEquals(6, times.size(), "three pods of four, two rounds: " + times);
        assertEquals(0.8, times.get(1) - times.get(0), 1e-9, "pod 2's turn skipped");
        assertEquals(0.4, times.get(2) - times.get(1), 1e-9);
    }

    @Test
    void aLobFiresAMortarAndOnlyASweepHasASweep() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new BossSpec.Attack(
                        "lob",
                        BossSpec.Pattern.LOB,
                        PartHSpecs.ORB,
                        0.15,
                        false,
                        1,
                        0,
                        0,
                        0,
                        List.of(0),
                        false,
                        Optional.empty()));
        assertThrows(
                IllegalArgumentException.class,
                () -> new BossSpec.Attack(
                        "aimed",
                        BossSpec.Pattern.AIMED,
                        PartHSpecs.ORB,
                        0.15,
                        false,
                        1,
                        0,
                        0,
                        0,
                        List.of(0),
                        false,
                        Optional.of(PartHSpecs.sweep(120))));
        assertTrue(PartHSpecs.spire().boss().orElseThrow().anchored());
    }
}
