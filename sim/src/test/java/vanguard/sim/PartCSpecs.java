package vanguard.sim;

import static vanguard.sim.TestSpecs.INFINITE;
import static vanguard.sim.TestSpecs.muzzle;

import java.util.List;
import java.util.Optional;

/**
 * The units of M5 part C (Level 09) for the simulation tests: the Hive Node (hardened, a periodic
 * spawner), the Ravager (a walker that pounces) and a plain hardened ground target, with the numbers
 * of their data files at medium.
 */
final class PartCSpecs {
    private PartCSpecs() {}

    /** A Hive Node releasing {@code count} Skitters every 4 s, shut while the ship is within {@code shutWithin} px. */
    static EnemySpec hiveNode(int count, double shutWithin) {
        return spec(
                "hive-node",
                64,
                new Hitbox(56, 56),
                0,
                true,
                Optional.empty(),
                true,
                Optional.of(new EnemySpec.Spawner(
                        TestSpecs.SKITTER, count, 4.0, 0.5, Math.toRadians(120), 160, shutWithin)),
                Optional.empty(),
                45);
    }

    /** A Ravager whose pounce spends {@code airSeconds} of its 0.75 s leap on air, {@code interval} s between pounces. */
    static EnemySpec ravager(double airSeconds, double interval) {
        return spec(
                "ravager",
                16,
                new Hitbox(40, 28),
                160,
                false,
                Optional.of(new EnemySpec.Walker(160, Math.toRadians(180), 46, 0, Optional.empty(), 0)),
                false,
                Optional.empty(),
                Optional.of(new EnemySpec.Pounce(200, 0.75, airSeconds, 1.43, interval)),
                18);
    }

    /** A hardened target fixed to the ground, wide enough for both Airstrike carpets. */
    static EnemySpec hardenedTarget(double hp) {
        return spec(
                "bunker",
                hp,
                new Hitbox(120, 60),
                0,
                true,
                Optional.empty(),
                true,
                Optional.empty(),
                Optional.empty(),
                10);
    }

    /** A hardened target fixed to the ground at the Hive Node's size (56 px) that releases nothing. */
    static EnemySpec nodeTarget(double hp) {
        return spec(
                "node",
                hp,
                new Hitbox(56, 56),
                0,
                true,
                Optional.empty(),
                true,
                Optional.empty(),
                Optional.empty(),
                45);
    }

    /**
     * Rook's Mortar at level 1 (the Hammer Mortar × 0.6): a 12-damage shell lobbed 200 px ahead of his
     * nose in 0.6 s, a 28 px blast, snapping to a ground target within 48 px.
     */
    static WeaponSpec mortar() {
        return new WeaponSpec(
                "hammer-mortar",
                "mortar",
                "mortar",
                WeaponSpec.Delivery.LOBBED,
                true,
                1.25,
                12,
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
                List.of(muzzle(0, 18, 0)));
    }

    private static EnemySpec spec(
            String slug,
            double hp,
            Hitbox box,
            double speed,
            boolean terrain,
            Optional<EnemySpec.Walker> walker,
            boolean hardened,
            Optional<EnemySpec.Spawner> spawner,
            Optional<EnemySpec.Pounce> pounce,
            int bounty) {
        return new EnemySpec(
                slug,
                hp,
                box,
                Layer.GROUND,
                15,
                false,
                bounty,
                speed,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                terrain,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                walker,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                hardened,
                spawner,
                pounce);
    }

    /** A Bomb Rack-like dropped bomb, anti-ground or not: 12 damage in a 30 px blast, 0.5 s to fall. */
    static WeaponSpec bomb(boolean antiGround) {
        return new WeaponSpec(
                "bomb",
                "bomb",
                "bomb",
                WeaponSpec.Delivery.DROPPED,
                antiGround,
                2,
                12,
                0,
                new Hitbox(8, 8),
                INFINITE,
                INFINITE,
                1,
                30,
                0,
                Math.PI,
                0.5,
                0,
                List.of(muzzle(0, 0, 0)));
    }

    /** The Pulse Cannon's bolt, anti-ground or not. */
    static WeaponSpec bolt(boolean antiGround) {
        return new WeaponSpec(
                "pulse-cannon",
                "pulse",
                "pulse",
                WeaponSpec.Delivery.BOLT,
                antiGround,
                10,
                2.0,
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
    }

    /** {@link TestSpecs#HOMING}, anti-ground or not. */
    static WeaponSpec homing(boolean antiGround) {
        WeaponSpec h = TestSpecs.HOMING;
        return new WeaponSpec(
                h.slug(),
                h.vfx(),
                h.sfx(),
                h.delivery(),
                antiGround,
                h.rate(),
                h.damage(),
                h.speed(),
                h.size(),
                h.range(),
                h.lifetimeSeconds(),
                h.pierce(),
                h.blast(),
                h.turnRate(),
                h.coneHalfAngle(),
                h.airSeconds(),
                h.snap(),
                h.muzzles());
    }

    /** A one-section level at {@code speed} px/s with these waves and ground units; nothing else. */
    static LevelScript level(double seconds, double speed, List<WaveSpec> waves, List<LevelScript.GroundUnit> units) {
        return new LevelScript(
                9,
                2,
                0,
                List.of(new LevelScript.Section(seconds, speed)),
                waves,
                List.of(),
                units,
                0,
                List.of(),
                new LevelScript.Secondary(0.8, 50),
                List.of());
    }

    /** A walker wave of {@code count} on {@code paths} (screen points: x, px below the top edge). */
    static WaveSpec walkers(
            double t, WaveSpec.Formation formation, EnemySpec enemy, int count, List<List<WaveSpec.At>> paths) {
        return new WaveSpec(
                t,
                formation,
                enemy,
                count,
                WaveSpec.Entry.FRONT,
                WaveSpec.Edge.NONE,
                Optional.empty(),
                Optional.empty(),
                1,
                Optional.empty(),
                Optional.empty(),
                List.of(),
                Optional.empty(),
                paths);
    }

    /** How many events of {@code type} the last step recorded. */
    static int count(Sortie sortie, SimEvents.Type type) {
        SimEvents events = sortie.events();
        int n = 0;
        for (int i = 0; i < events.size(); i++) {
            n += events.type(i) == type ? 1 : 0;
        }
        return n;
    }

    /** The first unit of {@code slug} on the field; null for none. */
    static Enemy find(Sortie sortie, String slug) {
        for (int i = 0; i < sortie.enemyCount(); i++) {
            if (sortie.enemy(i).spec().slug().equals(slug)) {
                return sortie.enemy(i);
            }
        }
        return null;
    }
}
