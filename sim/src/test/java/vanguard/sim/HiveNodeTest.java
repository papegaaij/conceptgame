package vanguard.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static vanguard.sim.PartCSpecs.count;
import static vanguard.sim.PartCSpecs.find;
import static vanguard.sim.PartCSpecs.hiveNode;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * The Hive Node (design/enemies/ground/hive-node, M5 part C): a hardened ground unit whose iris opens
 * over 0.5 s every 4 s from its centre crossing the top edge and releases its Skitters toward the
 * ship like a brood, counted among the level's enemies as released; shut while the ship is close.
 */
class HiveNodeTest {
    /** The node scrolls in at 30 px/s above the ship's column: its centre on the field from about 0.93 s, 96 px above the ship at 15 s. */
    private static Sortie sortie(EnemySpec node) {
        LevelScript level =
                PartCSpecs.level(20, 30, List.of(), List.of(new LevelScript.GroundUnit(0, Ship.START_X, node, -1)));
        return new Sortie(1, TestSpecs.LOADOUT, level, TestSpecs.RULES.withInvulnerableShip(), TestSpecs.FULL_ARMOUR);
    }

    /** The steps of each event of {@code type} over {@code steps} steps, with the iris's openness per step. */
    private record Run(List<Integer> telegraphs, List<Integer> releases, double[] iris, int[] total) {}

    private static Run fly(Sortie sortie, int steps) {
        List<Integer> telegraphs = new ArrayList<>();
        List<Integer> releases = new ArrayList<>();
        double[] iris = new double[steps];
        int[] total = new int[steps];
        for (int tick = 0; tick < steps; tick++) {
            sortie.step(Command.NONE);
            if (count(sortie, SimEvents.Type.SPAWN_TELEGRAPH) > 0) {
                telegraphs.add(tick);
            }
            if (count(sortie, SimEvents.Type.SPAWN_RELEASED) > 0) {
                releases.add(tick);
            }
            Enemy node = find(sortie, "hive-node");
            iris[tick] = node == null ? 0 : node.iris();
            total[tick] = sortie.enemyTotal();
        }
        return new Run(telegraphs, releases, iris, total);
    }

    @Test
    void itsIrisOpensOverTheTelegraphAndReleasesEveryFourSecondsFromTheTopEdge() {
        Sortie sortie = sortie(hiveNode(2, 96));
        Run run = fly(sortie, SimStep.ticks(10));

        // Its centre (28 px above the field at the start) crosses the top edge after 28 / 30 s.
        int crossing = SimStep.ticks(28 / 30.0);
        int every = SimStep.ticks(4);
        int telegraph = SimStep.ticks(0.5);
        assertEquals(2, run.releases().size(), "two releases in 10 s");
        assertEquals(crossing + every, run.releases().get(0), 2);
        assertEquals(run.releases().get(0) + every, run.releases().get(1));
        assertEquals(List.of(run.releases().get(0) - telegraph, run.releases().get(1) - telegraph), run.telegraphs());

        int release = run.releases().get(0);
        assertEquals(0, run.iris()[release - telegraph - 1], "shut before its telegraph");
        assertEquals(0.5, run.iris()[release - telegraph / 2], 1e-9, "half open halfway through");
        assertEquals(1, run.iris()[release], 1e-9, "open at the release");
        assertEquals(0, run.iris()[release + telegraph], 1e-9, "shut again as long after");
    }

    @Test
    void itsSkittersFlyOutTowardTheShipAndCountAmongTheEnemiesAsReleased() {
        Sortie sortie = sortie(hiveNode(3, 96));
        int before = sortie.enemyTotal();
        while (count(sortie, SimEvents.Type.SPAWN_RELEASED) == 0) {
            assertEquals(before, sortie.enemyTotal());
            sortie.step(Command.NONE);
        }
        assertEquals(before + 3, sortie.enemyTotal(), "counted as they are released");
        Enemy node = find(sortie, "hive-node");
        assertNotNull(node);
        double nodeX = node.x();
        double nodeY = node.y();
        double toShipX = sortie.ship().x() - nodeX;
        double toShipY = sortie.ship().y() - nodeY;
        sortie.step(Command.NONE);
        sortie.step(Command.NONE);

        int skitters = 0;
        for (int i = 0; i < sortie.enemyCount(); i++) {
            Enemy unit = sortie.enemy(i);
            if (unit.spec().slug().equals("skitter")) {
                skitters++;
                double dx = unit.x() - nodeX;
                double dy = unit.y() - nodeY;
                // Within the 120° arc centred on the ship: at most 60° off.
                double cos = (dx * toShipX + dy * toShipY) / Math.hypot(dx, dy) / Math.hypot(toShipX, toShipY);
                assertTrue(cos >= Math.cos(Math.toRadians(60)) - 1e-9, "toward the ship: " + cos);
            }
        }
        assertEquals(3, skitters);
        assertEquals(before + 3, sortie.enemyTotal());
    }

    @Test
    void anOpeningDueWhileTheShipIsCloseIsSkipped() {
        // A shut distance that covers the whole field: it never opens.
        Sortie sortie = sortie(hiveNode(2, 2000));
        int before = sortie.enemyTotal();
        Run run = fly(sortie, SimStep.ticks(9));

        assertEquals(List.of(), run.telegraphs());
        assertEquals(List.of(), run.releases());
        for (double iris : run.iris()) {
            assertEquals(0, iris);
        }
        assertEquals(before, sortie.enemyTotal());
    }

    @Test
    void theStandardShotsGlanceOffIt() {
        Sortie sortie = sortie(hiveNode(2, 96));
        int glanced = 0;
        for (int tick = 0; tick < SimStep.ticks(4); tick++) {
            sortie.step(Command.FIRE.bit());
            glanced += count(sortie, SimEvents.Type.SHOT_GLANCED);
            assertEquals(0, count(sortie, SimEvents.Type.ENEMY_HIT));
        }
        assertTrue(glanced > 10, "the Pulse Cannon's bolts spark off: " + glanced);
        assertEquals(64, find(sortie, "hive-node").hp());
    }

    @Test
    void itsCycleStepsDeterministicallyWithoutAllocating() {
        List<Sortie> flown = new ArrayList<>();
        long allocated = Allocations.least(
                () -> {
                    Sortie sortie = sortie(hiveNode(3, 96));
                    flown.add(sortie);
                    return sortie;
                },
                sortie -> {
                    for (int i = 0; i < SimStep.ticks(12); i++) {
                        sortie.step(SortieTest.Pilot.commands(i));
                    }
                });

        assertEquals(0, allocated, "12 s allocated " + allocated + " bytes");
        assertEquals(flown.getFirst().stateHash(), flown.getLast().stateHash());
    }
}
