package vanguard.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * M5 part H, the maw's ambush launches (design/enemies/bosses/siege-spire, the maw's Wraiths; decision
 * H3 = a): as phase 2 begins the claw crown opens; every 8 s the maw launches 2 Wraiths (hard 3) that fly
 * a rear ambush from it, cloaked down their lanes past the ship, back up from the bottom edge, decloaking
 * and holding; the bottom edge is warned 3 s ahead of each re-entry on the real steps (the level clock
 * halts in the arena), the units are rear ones from their re-entry (Rook's Trail), they count among the
 * level's enemies, and at most 4 of them are alive at once.
 */
class MawLaunchTest {
    /** The test Spire at phase 2's start with {@code wraiths} a launch. */
    private static Sortie atMaw(int wraiths, Loadout loadout) {
        Sortie sortie = new Sortie(
                1,
                loadout,
                PartHSpecs.level(PartHSpecs.spire(wraiths, 10, 120)),
                TestSpecs.RULES.withInvulnerableShip(),
                TestSpecs.FULL_ARMOUR);
        PartHSpecs.until(sortie, SimEvents.Type.BOSS_SETTLED, 5, 0);
        PartHSpecs.killRoots(sortie);
        PartHSpecs.until(sortie, SimEvents.Type.BOSS_PHASE, 0.1, 0);
        return sortie;
    }

    private static List<Enemy> launched(Sortie sortie) {
        List<Enemy> units = new ArrayList<>();
        for (int i = 0; i < sortie.enemyCount(); i++) {
            if (sortie.enemy(i).launched()) {
                units.add(sortie.enemy(i));
            }
        }
        return units;
    }

    private static boolean bottomWarned(Sortie sortie) {
        return WarningEdge.BOTTOM.in(sortie.edgeWarnings());
    }

    @Test
    void theCrownOpensAndTheMawLaunchesTwoCloakedWraithsAtOnce() {
        Sortie sortie = atMaw(2, TestSpecs.LOADOUT);
        SetPiece boss = sortie.setPiece(0);
        assertEquals(List.of(PartHSpecs.MAW), PartHSpecs.values(sortie, SimEvents.Type.SPIRE_MAW_OPENED));
        int wraith = sortie.enemyKinds().indexOf(PartDSpecs.wraith());
        assertEquals(List.of(wraith, wraith), PartHSpecs.values(sortie, SimEvents.Type.BOSS_LAUNCHED));
        assertFalse(boss.partShielded(PartHSpecs.MAW), "the maw is open");
        List<Enemy> units = launched(sortie);
        assertEquals(2, units.size());
        assertEquals(2, sortie.launchedAlive());
        for (Enemy unit : units) {
            assertEquals(boss.partX(PartHSpecs.MAW), unit.x(), 1e-9, "out of the maw");
            assertEquals(boss.partY(PartHSpecs.MAW), unit.y(), 1e-9);
            assertTrue(unit.cloaked());
            assertEquals(Enemy.AmbushPhase.SWOOP, unit.ambushPhase());
            assertEquals(WaveSpec.Entry.FRONT, unit.entry(), "not a rear unit before its re-entry");
        }
    }

    @Test
    void theBottomEdgeIsWarnedThreeSecondsAheadOfTheReentry() {
        Sortie sortie = atMaw(2, TestSpecs.LOADOUT);
        Enemy unit = launched(sortie).getFirst();
        assertFalse(bottomWarned(sortie));
        int warned = -1;
        int step = 0;
        while (unit.ambushPhase() != Enemy.AmbushPhase.RISE) {
            sortie.step(0);
            step++;
            if (warned < 0 && bottomWarned(sortie)) {
                warned = step;
            }
            if (unit.ambushPhase() != Enemy.AmbushPhase.RISE) {
                assertEquals(warned >= 0, bottomWarned(sortie), "warned from its start until the re-entry");
            }
        }
        assertEquals(SimStep.ticks(WaveSchedule.EDGE_WARNING_SECONDS), step - warned, "3 s ahead");
        assertTrue(sortie.arenaHalted() || sortie.groundSpeed() == 0, "on the real steps in the halt");
        assertEquals(WaveSpec.Entry.REAR, unit.entry(), "a rear unit from its re-entry");
        PartHSpecs.steps(sortie, 0.1);
        assertFalse(bottomWarned(sortie), "the warning ends with the re-entry");
    }

    @Test
    void theyLoopBehindTheShipDecloakAndHoldThenLeaveUpASideLane() {
        Sortie sortie = atMaw(2, TestSpecs.LOADOUT);
        List<Enemy> units = launched(sortie);
        double[] lanes = {PlayField.WIDTH / 3.0, 2 * PlayField.WIDTH / 3.0};
        PartHSpecs.until(sortie, SimEvents.Type.DECLOAK, 6, 0);
        // Their hold heights differ a little: both have decloaked a moment later.
        PartHSpecs.steps(sortie, 0.3);
        for (int i = 0; i < units.size(); i++) {
            Enemy unit = units.get(i);
            assertEquals(lanes[i], unit.x(), 1e-6, "in its lane");
            assertTrue(unit.y() < sortie.ship().y(), "behind the ship");
            assertEquals(Layer.AIR, unit.layer());
        }
        List<Integer> serials = units.stream().map(Enemy::serial).toList();
        boolean gone = false;
        for (int i = 0; i < SimStep.ticks(12) && !gone; i++) {
            sortie.step(0);
            gone = launched(sortie).stream().noneMatch(unit -> serials.contains(unit.serial()));
        }
        assertTrue(gone, "out up the side lanes");
    }

    @Test
    void rookFliesTrailOnceTheyAreBehind() {
        Sortie sortie = atMaw(2, TestSpecs.LOADOUT.withWingman(WingmanTest.rook(WingmanSpec.Side.LEFT)));
        Wingman rook = sortie.wingman().orElseThrow();
        assertNotEquals(Wingman.Formation.TRAIL, rook.formation());
        PartHSpecs.until(sortie, SimEvents.Type.DECLOAK, 6, 0);
        PartHSpecs.steps(sortie, 1);
        assertEquals(Wingman.Formation.TRAIL, rook.formation(), "the bottom edge is an active rear wave");
    }

    @Test
    void everyEightSecondsWithAtMostFourAliveAndEachCountedAmongTheEnemies() {
        Sortie sortie = atMaw(3, TestSpecs.LOADOUT);
        int total = sortie.enemyTotal();
        assertEquals(3, launched(sortie).size(), "hard: 3 a launch");
        List<Integer> next = PartHSpecs.until(sortie, SimEvents.Type.BOSS_LAUNCHED, 8.1, 0);
        assertEquals(1, next.size(), "three still alive: only one more");
        assertEquals(4, sortie.launchedAlive());
        assertEquals(total + 1, sortie.enemyTotal(), "the launched units count among the level's enemies");
    }

    @Test
    void theLaunchesAreDeterministic() {
        Sortie a = atMaw(3, TestSpecs.LOADOUT);
        Sortie b = atMaw(3, TestSpecs.LOADOUT);
        for (int i = 0; i < SimStep.ticks(20); i++) {
            int commands = (i / 50) % 2 == 0 ? Command.LEFT.bit() | Command.FIRE.bit() : Command.RIGHT.bit();
            a.step(commands);
            b.step(commands);
        }
        assertEquals(a.stateHash(), b.stateHash());
    }
}
