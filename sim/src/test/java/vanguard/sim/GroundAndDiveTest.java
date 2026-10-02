package vanguard.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static vanguard.sim.TestSpecs.wave;
import static vanguard.sim.WaveSpec.Edge.NONE;
import static vanguard.sim.WaveSpec.Entry.FRONT;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Level 02's rules: the Stinger's dive and fan, the Spine Turret on the ground with its barrel and
 * blind arc, the drydocks as groups of the secondary objective, and Crane Four.
 */
class GroundAndDiveTest {
    private static final int FIRE = Command.FIRE.bit();

    static final EnemyGun FAN = new EnemyGun(
            Double.POSITIVE_INFINITY,
            0,
            1,
            170,
            4,
            false,
            3,
            Math.toRadians(30),
            Double.POSITIVE_INFINITY,
            Double.POSITIVE_INFINITY);
    static final EnemySpec STINGER = new EnemySpec(
            "stinger",
            6,
            new Hitbox(24, 30),
            Layer.AIR,
            10,
            true,
            15,
            100,
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.of(FAN),
            Optional.empty(),
            Optional.of(new EnemySpec.Dive(new Range(90, 160), 0.5, 420, 0.6)),
            false);
    static final EnemyGun BARREL = new EnemyGun(2, 1, 1, 160, 4, false, 1, 0, Math.toRadians(90), Math.toRadians(100));
    static final EnemySpec TURRET = new EnemySpec(
            "spine-turret",
            8,
            new Hitbox(30, 30),
            Layer.GROUND,
            10,
            true,
            12,
            0,
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.of(BARREL),
            Optional.empty(),
            Optional.empty(),
            true);

    private static LevelScript level(
            List<WaveSpec> waves,
            List<LevelScript.GroundUnit> units,
            LevelScript.Secondary secondary,
            List<LevelScript.CraneSpec> cranes) {
        return new LevelScript(
                2,
                1,
                0,
                List.of(new LevelScript.Section(40, 140)),
                waves,
                List.of(),
                units,
                0,
                List.of(),
                secondary,
                cranes);
    }

    private static Sortie sortie(LevelScript level) {
        return new Sortie(1, TestSpecs.LOADOUT, level, TestSpecs.RULES, TestSpecs.FULL_ARMOUR);
    }

    private static Sortie unhit(LevelScript level) {
        return new Sortie(1, TestSpecs.LOADOUT, level, TestSpecs.RULES.withInvulnerableShip(), TestSpecs.FULL_ARMOUR);
    }

    @Test
    void aStingerPausesThenDivesAndFiresOneFan() {
        var sortie = unhit(level(
                List.of(wave(0, WaveSpec.Formation.SINGLE, STINGER, 1, FRONT, NONE)),
                List.of(),
                new LevelScript.Secondary(0.8, 50),
                List.of()));
        int fired = 0;
        int bullets = 0;
        double slowest = Double.MAX_VALUE;
        double fastest = 0;
        for (int i = 0; i < SimStep.ticks(6); i++) {
            // The ship sidesteps once the Stinger has paused, as Varga teaches: a dive straight at
            // the ship would reach it before its fan, and no bullet spawns that close to the ship.
            sortie.step(sortie.levelSeconds() > 2.2 ? Command.RIGHT.bit() : Command.NONE);
            fired += sortie.events().count(SimEvents.Type.ENEMY_FIRED);
            bullets = Math.max(bullets, sortie.bulletCount());
            if (sortie.enemyCount() == 1) {
                Enemy stinger = sortie.enemy(0);
                double speed = Math.abs(stinger.renderY(1) - stinger.renderY(0)) * SimStep.PER_SECOND;
                slowest = Math.min(slowest, speed);
                fastest = Math.max(fastest, speed);
            }
        }

        assertEquals(1, fired, "one fan in its dive");
        assertEquals(3, bullets, "a 3-way fan");
        assertEquals(0, slowest, 1e-9, "it pauses");
        assertEquals(420, fastest, 1, "and dives at its dive speed");
        assertEquals(0, sortie.enemyCount(), "gone off the bottom edge");
    }

    @Test
    void aTurretScrollsWithTheGroundTurnsItsBarrelAndFallsSilentOncePassed() {
        var units = List.of(new LevelScript.GroundUnit(0, Ship.START_X + 120, TURRET, -1));
        var sortie = unhit(level(List.of(), units, new LevelScript.Secondary(0.8, 50), List.of()));
        sortie.step(Command.NONE);
        Enemy turret = sortie.enemy(0);
        double y = turret.renderY(1);

        List<Double> shotTimes = new ArrayList<>();
        double aim = 0;
        for (int i = 0; i < SimStep.ticks(6) && sortie.enemyCount() == 1; i++) {
            sortie.step(Command.NONE);
            if (sortie.events().count(SimEvents.Type.ENEMY_FIRED) > 0) {
                shotTimes.add(sortie.levelSeconds());
            }
            if (sortie.enemyCount() == 1 && sortie.enemy(0).renderY(1) > Ship.START_Y) {
                aim = turret.aim();
            }
        }

        assertTrue(turret.renderY(1) < y, "it scrolled down");
        assertTrue(aim > 0, "the barrel turned towards the ship on its left (positive: clockwise from down): " + aim);
        assertFalse(shotTimes.isEmpty());
        double passedAt = (PlayField.HEIGHT + 15 - Ship.START_Y) / 140.0;
        assertTrue(shotTimes.getLast() < passedAt, "silent once the ship is behind it: " + shotTimes);
    }

    @Test
    void aDockIsClearedWhenAllItsTurretsAreDestroyedAndPays() {
        var units = List.of(
                new LevelScript.GroundUnit(0, Ship.START_X, TURRET, 0),
                new LevelScript.GroundUnit(0.3, Ship.START_X, TURRET, 0));
        var sortie = unhit(level(List.of(), units, new LevelScript.Secondary(0, 25, List.of("Dock One")), List.of()));

        int cleared = 0;
        for (int i = 0; i < SimStep.ticks(5); i++) {
            sortie.step(FIRE);
            cleared += sortie.events().count(SimEvents.Type.GROUP_CLEARED);
        }

        assertEquals(1, cleared);
        assertEquals(Objectives.CLEARED, sortie.groupState(0));
        assertTrue(sortie.secondaryMet(), "the only dock: the objective is met");
        assertEquals(12 + 12 + 25, sortie.credits());
    }

    @Test
    void aDockIsLostWhenItsLastTurretLeavesTheScreenAlive() {
        var units =
                List.of(new LevelScript.GroundUnit(0, 40, TURRET, 0), new LevelScript.GroundUnit(0, 440, TURRET, 1));
        var sortie = unhit(
                level(List.of(), units, new LevelScript.Secondary(0, 25, List.of("Dock One", "Dock Two")), List.of()));

        int lost = 0;
        for (int i = 0; i < SimStep.ticks(6); i++) {
            sortie.step(Command.NONE);
            lost += sortie.events().count(SimEvents.Type.GROUP_LOST);
        }

        assertEquals(2, lost);
        assertEquals(Objectives.LOST, sortie.groupState(0));
        assertFalse(sortie.secondaryMet());
        assertEquals(0, sortie.credits());
    }

    private static LevelScript.CraneSpec crane(List<Double> swings) {
        return new LevelScript.CraneSpec(
                240, 550, 260, 16, Math.toRadians(-45), Math.toRadians(45), swings, 2.5, 1.5, 15, 3, 80, "crane cache");
    }

    @Test
    void theCraneIsFoldedAwayThenSwingsBetweenItsAnglesAndFoldsAgain() {
        var sortie = sortie(
                level(List.of(), List.of(), new LevelScript.Secondary(0.8, 50), List.of(crane(List.of(4.0, 9.0)))));
        Crane arm = sortie.crane(0);

        runTo(sortie, 2);
        assertFalse(arm.present());
        runTo(sortie, 3.5);
        assertTrue(arm.present() && arm.telegraph() && !arm.swinging(), "lowered while its lights blink");
        runTo(sortie, 5);
        assertTrue(arm.swinging());
        runTo(sortie, 7);
        assertEquals(45, Math.toDegrees(arm.renderAngle(1)), 1e-6);
        runTo(sortie, 13);
        assertFalse(arm.present(), "raised away after its last swing");
    }

    @Test
    void theArmStopsShotsAndHitsTheShipAtMostOncePerSecond() {
        // A crane whose arm hangs straight down over the ship's column after its swing to 0 degrees.
        var spec = new LevelScript.CraneSpec(
                240, 550, 500, 16, Math.toRadians(-45), 0, List.of(1.6), 0.5, 1.5, 15, 3, 80, "crane cache");
        var sortie = sortie(level(List.of(), List.of(), new LevelScript.Secondary(0.8, 50), List.of(spec)));

        int glanced = 0;
        int shieldHits = 0;
        for (int i = 0; i < SimStep.ticks(4); i++) {
            sortie.step(FIRE);
            glanced += sortie.events().count(SimEvents.Type.SHOT_GLANCED);
            shieldHits += sortie.events().count(SimEvents.Type.SHIELD_HIT);
        }

        assertTrue(glanced > 0, "the arm stops the bolts");
        assertTrue(shieldHits >= 1 && shieldHits <= 3, "about one hit per second while touching: " + shieldHits);
    }

    @Test
    void threeHitsOnTheClampDuringASwingDropTheCrate() {
        // A swing of a few degrees keeps the clamp over the ship's column while it swings.
        var spec = new LevelScript.CraneSpec(
                240,
                550,
                240,
                16,
                Math.toRadians(-1),
                Math.toRadians(1),
                List.of(1.6),
                2.5,
                1.5,
                15,
                3,
                80,
                "crane cache");
        var sortie = unhit(level(List.of(), List.of(), new LevelScript.Secondary(0.8, 50), List.of(spec)));

        int clampHits = 0;
        int found = 0;
        for (int i = 0; i < SimStep.ticks(5); i++) {
            sortie.step(FIRE);
            clampHits += sortie.events().count(SimEvents.Type.CLAMP_HIT);
            found += sortie.events().count(SimEvents.Type.SECRET_FOUND);
        }

        assertEquals(3, clampHits);
        assertEquals(1, found);
        assertFalse(sortie.crane(0).holding());
    }

    private static void runTo(Sortie sortie, double seconds) {
        while (sortie.levelSeconds() < seconds) {
            sortie.step(Command.NONE);
        }
    }
}
