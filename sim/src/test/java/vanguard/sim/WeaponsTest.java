package vanguard.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static vanguard.sim.TestSpecs.INFINITE;
import static vanguard.sim.TestSpecs.PULSE;
import static vanguard.sim.TestSpecs.PULSE_L2;
import static vanguard.sim.TestSpecs.bolt;
import static vanguard.sim.TestSpecs.level;
import static vanguard.sim.TestSpecs.loadout;
import static vanguard.sim.TestSpecs.muzzle;
import static vanguard.sim.TestSpecs.wave;
import static vanguard.sim.WaveSpec.Edge.NONE;
import static vanguard.sim.WaveSpec.Entry.FRONT;
import static vanguard.sim.WaveSpec.Formation.LINE_ABREAST;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** The Act 1 arsenal's rules (design/player/weapons and the layer rules of design/enemies), with the data's numbers. */
class WeaponsTest {
    private static final int FIRE = Command.FIRE.bit();

    private static final WeaponSpec AUTOCANNON =
            bolt("autocannon-pod", 5, 1.6, 1000, new Hitbox(4, 8), INFINITE, 1, muzzle(16, -3, -2));
    private static final WeaponSpec VULCAN = bolt(
            "scatter-vulcan",
            6,
            1,
            700,
            new Hitbox(6, 6),
            490,
            1,
            muzzle(0, 21, -12),
            muzzle(0, 21, 0),
            muzzle(0, 21, 12));
    private static final WeaponSpec LANCE =
            bolt("lance-laser", 5, 5, 1200, new Hitbox(4, 28), INFINITE, 2, muzzle(0, 21, 0));
    private static final WeaponSpec SIDE_SPLITTER =
            bolt("side-splitter", 6, 2, 800, new Hitbox(10, 4), 360, 1, muzzle(6, -3, 90), muzzle(-6, -3, -90));
    private static final WeaponSpec MISSILE = new WeaponSpec(
            "micro-missile-pod",
            "micromissile",
            "micromissile",
            WeaponSpec.Delivery.HOMING,
            false,
            2,
            4,
            500,
            new Hitbox(4, 10),
            350,
            1.2,
            1,
            0,
            Math.toRadians(270),
            Math.toRadians(70),
            0,
            0,
            List.of(muzzle(16, -3, 30)));
    private static final WeaponSpec BOMB = new WeaponSpec(
            "bomb-rack",
            "bomb",
            "bomb",
            WeaponSpec.Delivery.DROPPED,
            true,
            1.2,
            10,
            0,
            new Hitbox(8, 12),
            0,
            INFINITE,
            1,
            20,
            0,
            Math.PI,
            0.5,
            0,
            List.of(muzzle(-16, -3, 0)));
    private static final WeaponSpec MORTAR = new WeaponSpec(
            "hammer-mortar",
            "mortar",
            "mortar",
            WeaponSpec.Delivery.LOBBED,
            true,
            1.25,
            20,
            0,
            new Hitbox(10, 10),
            200,
            INFINITE,
            1,
            28,
            0,
            Math.PI,
            0.6,
            48,
            List.of(muzzle(0, 21, 0)));

    private static Armament.Mount front(WeaponSpec weapon) {
        return new Armament.Mount(Armament.Slot.FRONT, weapon, weapon);
    }

    private static Sortie sortie(LevelScript level, Armament.Mount... mounts) {
        return new Sortie(1, loadout(mounts), level, TestSpecs.RULES, TestSpecs.FULL_ARMOUR);
    }

    private static LevelScript ground(LevelScript.GroundObjectSpec... objects) {
        return level(30, List.of(), List.of(objects), List.of());
    }

    private static LevelScript.GroundObjectSpec container(double x, double hp, boolean hardened) {
        return new LevelScript.GroundObjectSpec(0, x, new Hitbox(32, 24), hp, 5, Optional.empty(), 0, 0, "", hardened);
    }

    private static LevelScript.GroundObjectSpec beacon(double x) {
        return new LevelScript.GroundObjectSpec(
                0, x, new Hitbox(12, 12), 0, 0, Optional.empty(), 99, 80, "cache", false);
    }

    /** Steps with fire held; returns the events of {@code type} per mount. */
    private static int[] count(Sortie sortie, int steps, int commands, SimEvents.Type type) {
        int[] counts = new int[sortie.armament().size()];
        for (int i = 0; i < steps; i++) {
            sortie.step(commands);
            for (int e = 0; e < sortie.events().size(); e++) {
                if (sortie.events().type(e) == type) {
                    counts[sortie.events().value(e)]++;
                }
            }
        }
        return counts;
    }

    @Test
    void everyMountFiresOnItsOwnClock() {
        var sortie = sortie(
                level(10, List.of()),
                front(PULSE),
                new Armament.Mount(Armament.Slot.RIGHT_WING, AUTOCANNON, AUTOCANNON));

        int[] volleys = count(sortie, SimStep.PER_SECOND, FIRE, SimEvents.Type.SHOT_FIRED);

        assertEquals(10, volleys[0]);
        assertEquals(5, volleys[1]);
        assertEquals(0, count(sortie, SimStep.PER_SECOND, Command.NONE, SimEvents.Type.SHOT_FIRED)[0]);
    }

    @Test
    void aFanLeavesAtItsAngles() {
        var sortie = sortie(level(10, List.of()), front(VULCAN));
        sortie.step(FIRE);

        List<Long> headings = new ArrayList<>();
        for (int i = 0; i < sortie.shotCount(); i++) {
            headings.add(Math.round(Math.toDegrees(sortie.shot(i).heading())));
        }
        assertEquals(List.of(-12L, 0L, 12L), headings.stream().sorted().toList());
    }

    @Test
    void aBoltIsGoneAtTheEndOfItsRange() {
        // 120 px to the right at 600 px/s: 0.2 s, well inside the play field.
        var sortie = sortie(
                level(10, List.of()), front(bolt("test", 1, 1, 600, new Hitbox(4, 4), 120, 1, muzzle(0, 0, 90))));
        sortie.step(FIRE);
        assertEquals(1, sortie.shot(0).rangeLeft(), 0.1);

        int steps = 1;
        while (sortie.shotCount() > 0) {
            sortie.step(Command.NONE);
            steps++;
        }

        assertEquals(SimStep.ticks(0.2), steps);
    }

    @Test
    void aLancePassesThroughTwoTargetsAndHitsEachOnce() {
        var sortie = sortie(
                ground(
                        container(Ship.START_X, 100, false),
                        container(Ship.START_X, 100, false),
                        container(Ship.START_X, 100, false)),
                front(LANCE));
        // The containers come down to meet the bolt; one volley only.
        run(sortie, SimStep.ticks(1.5), Command.NONE);
        sortie.step(FIRE);

        int hits = count(sortie, SimStep.ticks(1), Command.NONE, SimEvents.Type.GROUND_HIT)[0];

        assertEquals(2, hits);
        int damaged = 0;
        for (int i = 0; i < sortie.groundObjectCount(); i++) {
            damaged += sortie.groundObject(i).damaged() ? 1 : 0;
        }
        assertEquals(2, damaged);
    }

    @Test
    void sideGunsFireLeftAndRightFromTheWingRoots() {
        var sortie = sortie(level(10, List.of()), new Armament.Mount(Armament.Slot.REAR, SIDE_SPLITTER, SIDE_SPLITTER));
        sortie.step(FIRE);

        assertEquals(2, sortie.shotCount());
        Shot right = sortie.shot(0).x() > Ship.START_X ? sortie.shot(0) : sortie.shot(1);
        Shot left = right == sortie.shot(0) ? sortie.shot(1) : sortie.shot(0);
        assertEquals(Ship.START_X + 6, right.renderX(0), 1e-9);
        assertEquals(Ship.START_X - 6, left.renderX(0), 1e-9);
        assertEquals(Math.PI / 2, right.heading(), 1e-9);
        assertEquals(-Math.PI / 2, left.heading(), 1e-9);

        // 360 px at 800 px/s: the 240 px to either edge are flown first.
        run(sortie, SimStep.ticks(0.45) + 1, Command.NONE);
        assertEquals(0, sortie.shotCount());
    }

    @Test
    void onlyHomingShotsReachTheHighAirLayer() {
        EnemySpec high = highAir(TestSpecs.NEEDLER);
        var bolts = sortie(level(10, List.of(wave(0, LINE_ABREAST, high, 1, FRONT, NONE))), front(PULSE));
        var missiles = sortie(
                level(10, List.of(wave(0, LINE_ABREAST, high, 1, FRONT, NONE))),
                new Armament.Mount(Armament.Slot.RIGHT_WING, MISSILE, MISSILE));

        assertEquals(0, count(bolts, SimStep.ticks(4), FIRE, SimEvents.Type.ENEMY_HIT)[0]);
        assertTrue(count(missiles, SimStep.ticks(4), FIRE, SimEvents.Type.ENEMY_HIT)[0] > 0);
    }

    @Test
    void aMissileLeavesOutwardThenTurnsOntoItsTarget() {
        var sortie = sortie(
                level(10, List.of(wave(0, LINE_ABREAST, TestSpecs.NEEDLER, 1, FRONT, NONE))),
                new Armament.Mount(Armament.Slot.RIGHT_WING, MISSILE, MISSILE));
        // The Needler hovers straight ahead of the ship.
        run(sortie, SimStep.ticks(2), Command.NONE);
        // Fired and steered in the same step: at most 270 degrees/s, 4.5 degrees per step.
        sortie.step(FIRE);
        Shot missile = sortie.shot(0);
        assertEquals(Math.toRadians(30 - 4.5), missile.heading(), 1e-9);
        assertTrue(missile.target() >= 0, "locked on");

        sortie.step(Command.NONE);
        assertEquals(Math.toRadians(30 - 9), missile.heading(), 1e-9);
    }

    @Test
    void aMissileWithNothingInItsConeFliesStraightAndIsGoneAfterItsLifetime() {
        var sortie = sortie(level(10, List.of()), new Armament.Mount(Armament.Slot.RIGHT_WING, MISSILE, MISSILE));
        sortie.step(FIRE);
        Shot missile = sortie.shot(0);

        run(sortie, SimStep.ticks(0.5), Command.NONE);
        assertEquals(Math.toRadians(30), missile.heading(), 1e-9);
        assertEquals(-1, missile.target());
        // 30 degrees right at 500 px/s reaches the right edge in about 0.9 s, inside its 1.2 s.
        run(sortie, SimStep.ticks(1.2), Command.NONE);
        assertEquals(0, sortie.shotCount());
    }

    @Test
    void bombsBurstOnTheGroundUnderThePodAndNeverHitFlyers() {
        var withContainer = sortie(
                ground(container(Ship.START_X - 16, 1000, true)),
                new Armament.Mount(Armament.Slot.LEFT_WING, BOMB, BOMB));
        int blasts = count(withContainer, SimStep.ticks(6), FIRE, SimEvents.Type.BLAST)[0];
        assertTrue(blasts >= 6);
        assertTrue(withContainer.groundObject(0).damaged(), "bombs are anti-ground: hardened targets too");

        var withNeedler = sortie(
                level(10, List.of(wave(0, LINE_ABREAST, TestSpecs.NEEDLER, 1, FRONT, NONE))),
                new Armament.Mount(Armament.Slot.LEFT_WING, BOMB, BOMB));
        assertEquals(0, count(withNeedler, SimStep.ticks(4), FIRE, SimEvents.Type.ENEMY_HIT)[0]);
        assertEquals(0, withNeedler.kills());
    }

    @Test
    void aShellLandsAheadOrOnAContainerNearItsLandingPointButNeverOnABeacon() {
        assertEquals(List.of(Ship.START_X), blastColumns(container(Ship.START_X + 60, 1000, false)));
        assertTrue(blastColumns(container(Ship.START_X + 30, 1000, false)).contains(Ship.START_X + 30));
        assertEquals(List.of(Ship.START_X), blastColumns(beacon(Ship.START_X + 30)));
    }

    /** The distinct x of the mortar's blasts over a ground object coming down the screen. */
    private static List<Double> blastColumns(LevelScript.GroundObjectSpec object) {
        var sortie = sortie(ground(object), front(MORTAR));
        List<Double> columns = new ArrayList<>();
        for (int i = 0; i < SimStep.ticks(5); i++) {
            sortie.step(FIRE);
            for (int e = 0; e < sortie.events().size(); e++) {
                if (sortie.events().type(e) == SimEvents.Type.BLAST
                        && !columns.contains(sortie.events().x(e))) {
                    columns.add(sortie.events().x(e));
                }
            }
        }
        return columns;
    }

    @Test
    void boltsGlanceOffHardenedGroundTargets() {
        var sortie = sortie(ground(container(Ship.START_X, 3, true)), front(PULSE));

        int glanced = count(sortie, SimStep.ticks(5), FIRE, SimEvents.Type.SHOT_GLANCED)[0];

        assertTrue(glanced > 0);
        assertFalse(sortie.groundObject(0).damaged());
    }

    @Test
    void antiGroundBoltsDoDoubleDamageOnTheGround() {
        assertEquals(2, hitsToDestroy(PULSE));
        WeaponSpec antiGround = new WeaponSpec(
                "test",
                "pulse",
                "pulse",
                WeaponSpec.Delivery.BOLT,
                true,
                10,
                2,
                900,
                new Hitbox(4, 12),
                INFINITE,
                INFINITE,
                1,
                0,
                0,
                Math.PI,
                0,
                0,
                List.of(muzzle(0, 21, 0)));
        assertEquals(1, hitsToDestroy(antiGround));
    }

    /** Hits of 2 damage on a 3 HP container until it is destroyed. */
    private static int hitsToDestroy(WeaponSpec weapon) {
        var sortie = sortie(ground(container(Ship.START_X, 3, false)), front(weapon));
        sortie.step(Command.NONE);
        int hits = 0;
        while (sortie.groundObjectCount() > 0) {
            sortie.step(FIRE);
            hits += sortie.events().count(SimEvents.Type.GROUND_HIT);
        }
        return hits;
    }

    @Test
    void anOverdriveFiresTheNextLevelsPatternUntilItRunsOut() {
        var sortie = sortie(level(10, List.of()), new Armament.Mount(Armament.Slot.FRONT, PULSE, PULSE_L2));
        sortie.overdrive(1);
        sortie.step(FIRE);
        assertEquals(2, sortie.shotCount());

        int ended = count(sortie, SimStep.PER_SECOND, Command.NONE, SimEvents.Type.OVERDRIVE_ENDED)[0];
        assertEquals(1, ended);
        assertEquals(0, sortie.overdriveSeconds());
        run(sortie, SimStep.PER_SECOND, Command.NONE);
        sortie.step(FIRE);
        assertEquals(1, sortie.shotCount());
    }

    @Test
    void steppingWithEveryKindOfWeaponDoesNotAllocate() {
        var threads = (com.sun.management.ThreadMXBean) java.lang.management.ManagementFactory.getThreadMXBean();
        var sortie = new Sortie(
                3,
                loadout(
                        front(MORTAR),
                        new Armament.Mount(Armament.Slot.REAR, SIDE_SPLITTER, SIDE_SPLITTER),
                        new Armament.Mount(Armament.Slot.LEFT_WING, BOMB, BOMB),
                        new Armament.Mount(Armament.Slot.RIGHT_WING, MISSILE, MISSILE)),
                level(
                        80,
                        List.of(
                                wave(1, WaveSpec.Formation.SNAKE, TestSpecs.SKITTER, 6, FRONT, WaveSpec.Edge.LEFT),
                                wave(3, WaveSpec.Formation.V_WING, TestSpecs.NEEDLER, 5, FRONT, NONE)),
                        List.of(container(200, 50, false), container(300, 50, true)),
                        List.of()),
                TestSpecs.RULES,
                TestSpecs.FULL_ARMOUR);
        // Past the first kill and the first container destroyed: those load classes once.
        run(sortie, 900, FIRE);
        long before = threads.getCurrentThreadAllocatedBytes();
        run(sortie, 1200, FIRE | Command.LEFT.bit());
        long allocated = threads.getCurrentThreadAllocatedBytes() - before;

        assertTrue(allocated < 1024, "1200 steps allocated " + allocated + " bytes");
    }

    private static EnemySpec highAir(EnemySpec spec) {
        return new EnemySpec(
                spec.slug(),
                spec.hp(),
                spec.hitbox(),
                Layer.HIGH_AIR,
                spec.contactDamage(),
                spec.destroyedByRamming(),
                spec.bounty(),
                spec.speed(),
                spec.snake(),
                spec.streamSpeed(),
                spec.hover(),
                spec.orbit(),
                Optional.empty(),
                spec.drop());
    }

    private static void run(Sortie sortie, int steps, int commands) {
        for (int i = 0; i < steps; i++) {
            sortie.step(commands);
        }
    }
}
