package vanguard.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * M5 part E, step E2b: the Reef Spitter (design/enemies/naval/reef-spitter; the stated defaults of
 * 2026-10-08): a gun turret on a raft that, besides scrolling with the sea, drifts at its stat
 * block's drift (10 px/s) along its nest's current; it aims its 3-way fan at the ship as a turret
 * does. A unit that does not drift hashes and moves as before.
 */
class ReefSpitterTest {
    private static final EnemyGun FAN = new EnemyGun(
            2.4, 1.0, 1, 150, 4, false, 3, Math.toRadians(30), Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);

    /** The Reef Spitter at medium (its data's numbers), drifting {@code drift} px/s. */
    static EnemySpec spitter(double drift) {
        return new EnemySpec(
                "reef-spitter",
                7,
                new Hitbox(26, 26),
                Layer.GROUND,
                10,
                true,
                14,
                10,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.of(FAN),
                Optional.empty(),
                Optional.empty(),
                true,
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
                Optional.empty(),
                Optional.empty(),
                drift);
    }

    private static LevelScript level(LevelScript.GroundUnit... units) {
        return new LevelScript(
                        11,
                        2,
                        0,
                        List.of(new LevelScript.Section(30, 130)),
                        List.of(),
                        List.of(),
                        List.of(units),
                        0,
                        List.of(),
                        new LevelScript.Secondary(0.8, 50),
                        List.of())
                .withWater(true);
    }

    private static Sortie sortie(LevelScript level) {
        return new Sortie(1, TestSpecs.LOADOUT, level, TestSpecs.RULES.withInvulnerableShip(), TestSpecs.FULL_ARMOUR);
    }

    private static void steps(Sortie sortie, int steps) {
        for (int i = 0; i < steps; i++) {
            sortie.step(0);
        }
    }

    @Test
    void aRaftDriftsAlongItsNestsCurrentOnTopOfTheScroll() {
        // 90°: straight to the right, across the scroll.
        Sortie sortie = sortie(level(new LevelScript.GroundUnit(0, 200, spitter(10), -1, Math.toRadians(90))));
        sortie.step(0);
        Enemy raft = sortie.enemy(0);
        double x = raft.x();
        double y = raft.y();
        steps(sortie, SimStep.ticks(2));
        assertEquals(20, raft.x() - x, 1e-6, "10 px/s to the right");
        assertEquals(-260, raft.y() - y, 1e-6, "and down with the sea only");
        assertTrue(raft.grounded());
        assertEquals(Layer.GROUND, raft.layer());
    }

    @Test
    void withoutACurrentItDriftsStraightDownWithTheSea() {
        Sortie sortie = sortie(level(new LevelScript.GroundUnit(0, 200, spitter(10), -1)));
        sortie.step(0);
        Enemy raft = sortie.enemy(0);
        double x = raft.x();
        double y = raft.y();
        steps(sortie, SimStep.ticks(1));
        assertEquals(0, raft.x() - x, 1e-9);
        assertEquals(-140, raft.y() - y, 1e-6, "130 px/s of scroll and its 10 px/s drift");
    }

    @Test
    void aTurretThatDoesNotDriftStaysOnItsGroundPoint() {
        Sortie sortie = sortie(level(new LevelScript.GroundUnit(0, 200, spitter(0), -1, Math.toRadians(90))));
        sortie.step(0);
        Enemy turret = sortie.enemy(0);
        double x = turret.x();
        double y = turret.y();
        steps(sortie, SimStep.ticks(1));
        assertEquals(x, turret.x(), 1e-9, "a nest's current moves only what drifts");
        assertEquals(-130, turret.y() - y, 1e-6);
    }

    @Test
    void aRaftThatDriftsOffASideEdgeIsGone() {
        Sortie sortie = sortie(level(new LevelScript.GroundUnit(0, 470, spitter(10), -1, Math.toRadians(90))));
        sortie.step(0);
        assertEquals(1, sortie.enemyCount());
        steps(sortie, SimStep.ticks(1));
        assertEquals(1, sortie.enemyCount(), "still on the play field");
        steps(sortie, SimStep.ticks(2));
        assertEquals(0, sortie.enemyCount(), "past the right edge after about 2.4 s");
    }

    @Test
    void itFiresItsThreeWayFanAtTheShipEvery2Point4Seconds() {
        // To the left of the ship, so its second volley is not too close to fire.
        Sortie sortie = sortie(level(new LevelScript.GroundUnit(0, 60, spitter(10), -1)));
        int volleys = 0;
        int last = -1;
        for (int tick = 1; tick <= SimStep.ticks(4.2); tick++) {
            sortie.step(0);
            if (sortie.events().count(SimEvents.Type.ENEMY_FIRED) == 0) {
                continue;
            }
            volleys++;
            if (last > 0) {
                assertEquals(SimStep.ticks(2.4), tick - last, "every 2.4 s");
            } else {
                assertEquals(3, sortie.bulletCount(), "a three-way fan");
                for (int i = 0; i < 3; i++) {
                    EnemyBullet bullet = sortie.bullet(i);
                    assertEquals(150, Math.hypot(bullet.vx(), bullet.vy()), 1e-3);
                    assertTrue(bullet.vy() < 0 && bullet.vx() > 0, "down and across at the ship");
                }
            }
            last = tick;
        }
        assertEquals(2, volleys);
    }

    @Test
    void driftingChangesTheHashOnlyForAUnitThatDrifts() {
        Sortie still = sortie(level(new LevelScript.GroundUnit(0, 200, spitter(0), -1)));
        Sortie stillAgain = sortie(level(new LevelScript.GroundUnit(0, 200, spitter(0), -1, Math.toRadians(45))));
        Sortie drifting = sortie(level(new LevelScript.GroundUnit(0, 200, spitter(10), -1, Math.toRadians(45))));
        steps(still, 60);
        steps(stillAgain, 60);
        steps(drifting, 60);
        assertEquals(
                still.stateHash(), stillAgain.stateHash(), "a current on a unit that does not drift changes nothing");
        assertNotEquals(still.stateHash(), drifting.stateHash());
        assertThrows(IllegalArgumentException.class, () -> spitter(-1));
    }
}
