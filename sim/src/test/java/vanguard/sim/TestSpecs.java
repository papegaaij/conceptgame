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
}
