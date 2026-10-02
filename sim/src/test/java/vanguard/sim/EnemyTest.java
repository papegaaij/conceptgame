package vanguard.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class EnemyTest {
    /** Down 120 px at 120 px/s: it stops after one second, holds 10 s, then leaves down. */
    private static Spawn hovering(EnemySpec spec, Optional<Spawn.Orbit> orbit, Spawn.Exit exit) {
        return new Spawn(
                0, 0, spec, FlightPath.through(240, 400, 240, 280), 120, 10, orbit, exit, false, Optional.empty());
    }

    private static List<Integer> shotSteps(Enemy enemy, int steps) {
        List<Integer> shots = new ArrayList<>();
        for (int i = 1; i <= steps; i++) {
            enemy.move(240, 96, 0);
            if (enemy.trigger()) {
                shots.add(i);
            }
        }
        return shots;
    }

    @Test
    void theFirstShotComesTheDelayAfterItStopsThenEveryInterval() {
        var enemy = new Enemy();
        enemy.spawn(hovering(TestSpecs.NEEDLER, Optional.empty(), Spawn.Exit.DOWN), 0);

        List<Integer> shots = shotSteps(enemy, 60 + 360);

        int stop = 60;
        assertEquals(List.of(stop + 48, stop + 48 + 150, stop + 48 + 300), shots);
    }

    @Test
    void aBurstFiresItsShotsInQuickSuccession() {
        var enemy = new Enemy();
        enemy.spawn(
                hovering(
                        TestSpecs.needler(EnemyGun.aimed(2.5, 0.8, 2, 150, 4, false)),
                        Optional.empty(),
                        Spawn.Exit.DOWN),
                0);

        List<Integer> shots = shotSteps(enemy, 60 + 60);

        assertEquals(List.of(108, 108 + SimStep.ticks(EnemyGun.BURST_GAP_SECONDS)), shots);
    }

    @Test
    void itStopsFiringAndLeavesAfterTheHold() {
        var enemy = new Enemy();
        enemy.spawn(hovering(TestSpecs.NEEDLER, Optional.empty(), Spawn.Exit.DOWN), 0);
        shotSteps(enemy, 60 + 600);

        double y = enemy.y();
        assertFalse(enemy.trigger());
        enemy.move(240, 96, 0);
        assertEquals(y - 2, enemy.y(), 1e-9, "down at 120 px/s");
        int steps = 0;
        while (enemy.move(240, 96, 0)) {
            steps++;
        }
        assertTrue(steps < 3 * SimStep.PER_SECOND, "gone off the bottom edge");
    }

    @Test
    void aCircleUnitOrbitsItsCentreThenBreaksTowardTheShip() {
        var enemy = new Enemy();
        var orbit = new Spawn.Orbit(240, 360, 80, -StrictMath.PI / 2, StrictMath.toRadians(60));
        enemy.spawn(hovering(TestSpecs.NEEDLER, Optional.of(orbit), Spawn.Exit.TOWARD_SHIP), 0);

        for (int i = 0; i < 60 + 300; i++) {
            enemy.move(240, 96, 0);
            if (i > 60) {
                assertEquals(80, Math.hypot(enemy.x() - 240, enemy.y() - 360), 1e-5);
            }
        }
        for (int i = 0; i < 400; i++) {
            enemy.move(100, 96, 0);
        }
        double x = enemy.x();
        double y = enemy.y();
        enemy.move(100, 96, 0);

        assertTrue(enemy.x() <= x && enemy.y() < y, "breaks off toward where the ship was");
    }

    @Test
    void itFacesItsDirectionOfFlightTurningAtMostOneHeadingStepPerStep() {
        var enemy = new Enemy();
        var orbit = new Spawn.Orbit(240, 360, 80, -StrictMath.PI / 2, StrictMath.toRadians(60));
        enemy.spawn(hovering(TestSpecs.SKITTER, Optional.of(orbit), new Spawn.Exit(false, 0, 1)), 0);
        assertEquals(0, enemy.facing(), 1e-12, "flying down the screen");

        double previous = enemy.facing();
        for (int i = 0; i < 60 + 600 + 60; i++) {
            enemy.move(240, 96, 0);
            double turn = Math.abs(Math.IEEEremainder(enemy.facing() - previous, 2 * StrictMath.PI));
            assertTrue(turn <= StrictMath.PI / 8 + 1e-12, "step " + i + " turned " + turn);
            previous = enemy.facing();
        }

        assertEquals(StrictMath.PI, Math.abs(enemy.facing()), 1e-9, "leaving up the screen");
    }

    @Test
    void anUnarmedEnemyNeverFires() {
        var enemy = new Enemy();
        enemy.spawn(hovering(TestSpecs.SKITTER, Optional.empty(), Spawn.Exit.DOWN), 0);

        assertEquals(List.of(), shotSteps(enemy, 600));
    }
}
