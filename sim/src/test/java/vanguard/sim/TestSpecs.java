package vanguard.sim;

import java.util.List;
import java.util.Optional;

/**
 * Specs with the Level 01 numbers at medium for the rule tests here: the starter loadout, the
 * Skitter, the Needler and the rules. The game builds them from the design data
 * (vanguard.content.SimSpecs, whose tests check the same numbers), and the test levels are made
 * up per test.
 */
final class TestSpecs {
    static final double INFINITE = Double.POSITIVE_INFINITY;

    /** The Stormhawk's hull boxes from design/player/ship/data.yaml, as offsets around the centre. */
    static final Hull HULL = new Hull(List.of(
            new Hull.Part(0, 15, new Hitbox(4, 10)),
            new Hull.Part(0, -5.5, new Hitbox(12, 31)),
            new Hull.Part(0, -3, new Hitbox(36, 2)),
            new Hull.Part(0, -6, new Hitbox(28, 4)),
            new Hull.Part(-15, 0.5, new Hitbox(6, 5)),
            new Hull.Part(15, 0.5, new Hitbox(6, 5))));

    static final ShipSpec SHIP = new ShipSpec(270, 0.08, 0.06, 0.5, 48, 12, HULL, 0.25, 21, 3);
    /** The Pulse Cannon at L1 from the front muzzle, 21 px above the ship's centre, and at L2 (its overdrive). */
    static final WeaponSpec PULSE =
            bolt("pulse-cannon", 10, 2.0, 900, new Hitbox(4, 12), INFINITE, 1, muzzle(0, 21, 0));

    static final WeaponSpec PULSE_L2 =
            bolt("pulse-cannon", 10, 1.6, 900, new Hitbox(4, 12), INFINITE, 1, muzzle(-5, 21, 0), muzzle(5, 21, 0));
    static final ShieldModel SHIELD = new ShieldModel(20, 2, 2.0, 1.0);
    static final Plating PLATING = new Plating(60);
    static final Loadout LOADOUT = loadout(new Armament.Mount(Armament.Slot.FRONT, PULSE, PULSE_L2));
    static final double FULL_ARMOUR = 60;
    static final EnemySpec SKITTER = new EnemySpec(
            "skitter",
            1,
            new Hitbox(16, 16),
            Layer.AIR,
            6,
            true,
            5,
            190,
            Optional.of(new EnemySpec.Snake(0.25)),
            Optional.of(160.0),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            false);
    static final EnemyGun NEEDLER_GUN = EnemyGun.aimed(2.5, 0.8, 1, 150, 4, false);
    static final EnemySpec NEEDLER = needler(NEEDLER_GUN);
    static final ScoringRules SCORING = new ScoringRules(
            10,
            10,
            new ScoringRules.Chain(2, 10, 0.5, 5),
            1,
            1,
            new ScoringRules.Weights(0.4, 0.3, 0.15, 0.15, 40),
            List.of(
                    new ScoringRules.Bonus(ScoringRules.BonusKind.DESTRUCTION, "Destruction", 100, true, true),
                    new ScoringRules.Bonus(ScoringRules.BonusKind.UNTOUCHED, "Untouched", 5000, false, false),
                    new ScoringRules.Bonus(ScoringRules.BonusKind.EXPLORER, "Explorer", 3000, false, false)),
            List.of(
                    new ScoringRules.Grade("A+", 85, 0.3),
                    new ScoringRules.Grade("A", 70, 0.2),
                    new ScoringRules.Grade("B", 50, 0.1),
                    new ScoringRules.Grade("C", 30, 0),
                    new ScoringRules.Grade("D", 0, 0)));
    static final Rules RULES = new Rules(120, 0, new PickupRules(10, 50, 20, 0.25, 10, 6, 40, 36), SCORING);

    private TestSpecs() {}

    /** The starter hull, shield and plating with these weapons. */
    static Loadout loadout(Armament.Mount... mounts) {
        return new Loadout(SHIP, new Armament(List.of(mounts)), SHIELD, PLATING);
    }

    static WeaponSpec.Muzzle muzzle(double dx, double dy, double degrees) {
        return new WeaponSpec.Muzzle(dx, dy, Math.toRadians(degrees));
    }

    /** A weapon whose bolts fly straight. */
    static WeaponSpec bolt(
            String slug,
            double rate,
            double damage,
            double speed,
            Hitbox size,
            double range,
            int pierce,
            WeaponSpec.Muzzle... muzzles) {
        return new WeaponSpec(
                slug,
                "pulse",
                "pulse",
                WeaponSpec.Delivery.BOLT,
                false,
                rate,
                damage,
                speed,
                size,
                range,
                INFINITE,
                pierce,
                0,
                0,
                Math.PI,
                0,
                0,
                List.of(muzzles));
    }

    static EnemySpec needler(EnemyGun gun) {
        return new EnemySpec(
                "needler",
                4,
                new Hitbox(26, 24),
                Layer.AIR,
                10,
                true,
                12,
                120,
                Optional.empty(),
                Optional.empty(),
                Optional.of(new EnemySpec.Hover(new Range(2, 4), new Range(80, 220))),
                Optional.of(new EnemySpec.Orbit(90, 60)),
                Optional.of(gun),
                Optional.of(new EnemySpec.Drop(PickupType.SHIELD_CELL, 4)),
                Optional.empty(),
                false);
    }

    /** A wave without hold, warning, speed or interval, breaking off one by one, carrying nothing. */
    static WaveSpec wave(
            double t,
            WaveSpec.Formation formation,
            EnemySpec enemy,
            int count,
            WaveSpec.Entry entry,
            WaveSpec.Edge edge) {
        return new WaveSpec(
                t,
                formation,
                enemy,
                count,
                entry,
                edge,
                Optional.empty(),
                Optional.empty(),
                1,
                Optional.empty(),
                Optional.empty(),
                List.of());
    }

    /** A one-section level of {@code seconds} at 130 px/s without a launch, ground objects or radio. */
    static LevelScript level(double seconds, List<WaveSpec> waves) {
        return level(seconds, waves, List.of(), List.of());
    }

    static LevelScript level(
            double seconds,
            List<WaveSpec> waves,
            List<LevelScript.GroundObjectSpec> ground,
            List<LevelScript.RadioCue> radio) {
        return new LevelScript(
                1,
                1,
                0,
                List.of(new LevelScript.Section(seconds, 130)),
                waves,
                ground,
                List.of(),
                (int) ground.stream()
                        .filter(LevelScript.GroundObjectSpec::trigger)
                        .count(),
                radio,
                new LevelScript.Secondary(0.8, 50),
                List.of());
    }

    static Sortie sortie(LevelScript level) {
        return new Sortie(1, LOADOUT, level, RULES, FULL_ARMOUR);
    }

    /** A homing missile pod after the Micro-missile Pod, seeking far enough to reach a boss above the ship. */
    static final WeaponSpec HOMING = new WeaponSpec(
            "micro-missile-pod",
            "micromissile",
            "micromissile",
            WeaponSpec.Delivery.HOMING,
            false,
            4,
            4,
            500,
            new Hitbox(4, 10),
            600,
            2,
            1,
            0,
            Math.toRadians(360),
            Math.toRadians(90),
            0,
            0,
            List.of(muzzle(0, 10, 0)));

    // The test carrier (design/enemies/bosses/brood-carrier, M4 part G): two sac pairs, a core and a
    // fire-only turret; nose-down on high air in phase 1, broadside on air from phase 2.
    static final int SAC_A_LEFT = 0;
    static final int SAC_A_RIGHT = 1;
    static final int SAC_B_LEFT = 2;
    static final int SAC_B_RIGHT = 3;
    static final int CARRIER_CORE = 4;
    static final int CARRIER_TURRET = 5;
    static final double CARRIER_ARRIVES = 1;
    /** The broadside station: x from the left, y up from the bottom edge (150 px below the top). */
    static final double STATION_X = 170;

    static final double STATION_Y = PlayField.HEIGHT - 150;
    static final List<Integer> SACS = List.of(SAC_A_LEFT, SAC_A_RIGHT, SAC_B_LEFT, SAC_B_RIGHT);

    /**
     * The test carrier: phase 1 timed (25 s) on high air from its arrival, its pairs opening every
     * 2.5 s and releasing 4 Skitters or 2 Needlers in turn ({@code spawns}: else none); phase 2 after
     * a 2 s descent and a 2 s turn, the turret's 5-way fan every 2.4 s and one pair open for 2 s
     * between volleys (1 Skitter per sac), until the sacs are gone or 70 s; phase 3 the core's
     * spiral of {@code arms} arms and its 16-ring together, every surviving pair opening every 6 s
     * (2 Skitters per pair).
     */
    static LevelScript.SetPieceSpec carrier(boolean spawns, int arms) {
        Hitbox sac = new Hitbox(30, 30);
        var parts = List.of(
                new LevelScript.PartSpec("sac a left", -50, -60, sac, 20, false, 25, Optional.empty(), 0, 1.5),
                new LevelScript.PartSpec("sac a right", 50, -60, sac, 20, false, 25, Optional.empty(), 0, 1.5),
                new LevelScript.PartSpec("sac b left", -50, 40, sac, 20, false, 25, Optional.empty(), 0, 1.5),
                new LevelScript.PartSpec("sac b right", 50, 40, sac, 20, false, 25, Optional.empty(), 0, 1.5),
                new LevelScript.PartSpec("core", 0, 100, new Hitbox(50, 50), 30, true, 50, Optional.empty(), 0, 2),
                new LevelScript.PartSpec(
                        "mandibles", 0, -140, new Hitbox(24, 24), 1, false, 0, Optional.empty(), 0, 1));
        var noseDown = new BossSpec.Pose(
                "arrival",
                new Hitbox(100, 300),
                parts.stream()
                        .map(part -> new BossSpec.Offset(part.dx(), part.dy()))
                        .toList());
        var broadside = new BossSpec.Pose(
                "broadside",
                new Hitbox(300, 100),
                List.of(
                        new BossSpec.Offset(60, -50),
                        new BossSpec.Offset(60, 50),
                        new BossSpec.Offset(-40, -50),
                        new BossSpec.Offset(-40, 50),
                        new BossSpec.Offset(-100, 0),
                        new BossSpec.Offset(140, 0)));
        var fan = new BossSpec.Attack(
                "mandible-fan",
                BossSpec.Pattern.FAN,
                new EnemyGun(
                        2.4,
                        0,
                        1,
                        150,
                        4,
                        false,
                        5,
                        Math.toRadians(50),
                        Double.POSITIVE_INFINITY,
                        Double.POSITIVE_INFINITY),
                0,
                false,
                5,
                0,
                0,
                0,
                List.of(CARRIER_TURRET));
        var spiral = new BossSpec.Attack(
                "core-spiral",
                BossSpec.Pattern.SPIRAL,
                EnemyGun.aimed(0.375, 0, 1, 120, 4, false),
                0,
                false,
                1,
                arms,
                Math.toRadians(90),
                0,
                List.of(CARRIER_CORE));
        var ring = new BossSpec.Attack(
                "core-ring",
                BossSpec.Pattern.RING,
                EnemyGun.aimed(4, 0, 1, 110, 4, false),
                0,
                false,
                16,
                0,
                0,
                0,
                List.of(CARRIER_CORE));
        var pairs = List.of(List.of(SAC_A_LEFT, SAC_A_RIGHT), List.of(SAC_B_LEFT, SAC_B_RIGHT));
        List<BossSpec.Spawn> pass = spawns
                ? List.of(
                        new BossSpec.Spawn("pass-skitters", SKITTER, 4, 150, Math.toRadians(90), 0),
                        new BossSpec.Spawn("pass-needlers", NEEDLER, 2, 100, Math.toRadians(60), 1))
                : List.of();
        List<BossSpec.Spawn> window =
                spawns ? List.of(new BossSpec.Spawn("broadside-skitters", SKITTER, 2, 150, 0, 0)) : List.of();
        List<BossSpec.Spawn> timeout = spawns
                ? List.of(new BossSpec.Spawn("timeout-skitters", SKITTER, 2, 150, Math.toRadians(40), 0))
                : List.of();
        var phases = List.of(
                new BossSpec.Phase(
                        "Overhead",
                        List.of(),
                        0,
                        List.of(),
                        false,
                        Optional.empty(),
                        List.of(),
                        Double.NaN,
                        25,
                        0,
                        Optional.empty(),
                        Optional.of(new BossSpec.Windows(pairs, 2.5, 2, 1, false, pass))),
                new BossSpec.Phase(
                        "Broadside",
                        SACS,
                        0,
                        List.of(0),
                        false,
                        Optional.empty(),
                        List.of(),
                        Double.NaN,
                        70,
                        0,
                        Optional.of(new BossSpec.Move(STATION_X, STATION_Y, Layer.AIR, 2, 1, 2)),
                        Optional.of(new BossSpec.Windows(pairs, 2.4, 2, 0.2, false, window))),
                new BossSpec.Phase(
                        "Core",
                        List.of(CARRIER_CORE),
                        0,
                        List.of(1, 2),
                        false,
                        Optional.empty(),
                        List.of(CARRIER_CORE),
                        Double.NaN,
                        Double.POSITIVE_INFINITY,
                        BossSpec.PHASE_DELAY_SECONDS,
                        Optional.empty(),
                        Optional.of(new BossSpec.Windows(pairs, 6, 2, 0, true, timeout))));
        return new LevelScript.SetPieceSpec(
                "test-carrier",
                new Hitbox(120, 320),
                new Hitbox(100, 300),
                40,
                parts,
                List.of(),
                Optional.empty(),
                Optional.of(new BossSpec(
                        CARRIER_ARRIVES,
                        240,
                        270,
                        40,
                        0,
                        1,
                        Layer.HIGH_AIR,
                        false,
                        "TEST CARRIER",
                        150,
                        List.of(),
                        List.of(fan, spiral, ring),
                        phases,
                        true,
                        List.of(CARRIER_TURRET),
                        List.of(noseDown, broadside),
                        3)));
    }

    /** A level of a short approach, the carrier's 200 s arena and 10 s after it, without radio. */
    static LevelScript carrierLevel(LevelScript.SetPieceSpec carrier) {
        return new LevelScript(
                7,
                1,
                0,
                List.of(
                        new LevelScript.Section(2, 130),
                        new LevelScript.Section(200, 20, true),
                        new LevelScript.Section(210, 130)),
                List.of(),
                List.of(),
                List.of(),
                0,
                List.of(),
                new LevelScript.Secondary(0.8, 50),
                List.of(),
                List.of(),
                List.of(carrier));
    }
}
