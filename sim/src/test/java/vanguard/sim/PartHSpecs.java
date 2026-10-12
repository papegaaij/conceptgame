package vanguard.sim;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * M5 part H's test Siege Spire and levels (design/enemies/bosses/siege-spire, design/campaign Level 14;
 * decisions H1–H5 = a): an anchored arena boss without lanes, its centre 320 px above the bottom edge at
 * the halt, four turret pods firing staggered aimed orbs, two mortar roots lobbing in turn, an armoured
 * trunk, a maw exposed in phase 2 launching ambush Wraiths and fanning, and a core exposed in phase 3
 * that lifts to {@code air} and sweeps between rings. The numbers are the spec's where it has them; the
 * HP are small so that the tests reach every phase quickly.
 */
final class PartHSpecs {
    private PartHSpecs() {}

    static final int POD_1 = 0;
    static final int POD_2 = 1;
    static final int POD_3 = 2;
    static final int POD_4 = 3;
    static final int MORTAR_LEFT = 4;
    static final int MORTAR_RIGHT = 5;
    static final int TRUNK = 6;
    static final int MAW = 7;
    static final int CORE = 8;
    static final List<Integer> ROOTS = List.of(POD_1, POD_2, POD_3, POD_4, MORTAR_LEFT, MORTAR_RIGHT);

    static final int ORBS = 0;
    static final int ACID = 1;
    static final int SPIT = 2;
    static final int SWEEP = 3;
    static final int RING = 4;

    /** The boss arrives (the arena starts) here; the approach scrolls at {@link #SPEED} before it. */
    static final double ARRIVE = 3;

    static final double SPEED = 140;
    /** The anchored centre's height above the bottom edge at the halt. */
    static final double HOVER_Y = 320;
    /** The free spire's station after the lift. */
    static final double FREE_X = PlayField.WIDTH / 2.0;

    static final double FREE_Y = 420;
    static final double LIFT_SECONDS = 3;
    /** The sway of the free spire: ±100 px, 50 px/s at most (2π × 100 ÷ 50 s a sway). */
    static final double SWAY = 100;

    static final double SWAY_PERIOD = 2 * Math.PI * SWAY / 50;

    static final EnemyGun ORB = EnemyGun.aimed(1.6, 0, 1, 150, 6, false);
    /** The acid lob: 1.0 s marker, a 20 px impact circle, a ring of {@code ring} bullets at 110 px/s. */
    static EnemyGun acid(int ring) {
        return new EnemyGun(
                4,
                0,
                1,
                110,
                10,
                false,
                1,
                0,
                Double.POSITIVE_INFINITY,
                Double.POSITIVE_INFINITY,
                Optional.empty(),
                Optional.of(new EnemyGun.MortarSpec(1.0, 20, ring, 4)));
    }

    static final EnemyGun FAN = new EnemyGun(
            2.5, 0, 1, 140, 6, false, 9, Math.toRadians(80), Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
    /** The sweep's turn: 2.5 s, the ring's the other 2.5 s of every 5 s. */
    static final EnemyGun SWEEP_TURN = EnemyGun.aimed(2.5, 0, 1, 1, 8, false);

    static final EnemyGun RING_GUN = EnemyGun.aimed(2.5, 0, 1, 120, 6, false);

    /** The sweep: a 120° arc (hard 180°) over 1.0 s after a 0.6 s telegraph, 600 px long, 14 px wide, `laser` = 8. */
    static EnemySpec.Sweep sweep(double arcDegrees) {
        return new EnemySpec.Sweep(Math.toRadians(arcDegrees), 1.0, 0.6, 600, 14, 5, 0, 8, 0, 20);
    }

    static LevelScript.PartSpec part(String name, double dx, double dy, double hp, int bounty, boolean vital) {
        return new LevelScript.PartSpec(
                name, dx, dy, new Hitbox(48, 48), hp, vital, bounty, Optional.empty(), 0, vital ? 2 : 1);
    }

    /** The test Spire at medium: 2 Wraiths a launch, a ring of 10 for the lob, a 120° sweep. */
    static LevelScript.SetPieceSpec spire() {
        return spire(2, 10, 120);
    }

    /** The test Spire with {@code wraiths} a launch (hard 3), lobs ringing {@code ring} (hard 12) and a sweep of {@code arc}°. */
    static LevelScript.SetPieceSpec spire(int wraiths, int ring, double arc) {
        List<LevelScript.PartSpec> parts = List.of(
                part("pod 1", -170, -150, 20, 25, false),
                part("pod 2", -90, -230, 20, 25, false),
                part("pod 3", 90, -230, 20, 25, false),
                part("pod 4", 170, -150, 20, 25, false),
                part("mortar left", -190, -40, 20, 25, false),
                part("mortar right", 190, -40, 20, 25, false),
                new LevelScript.PartSpec("trunk", 0, 90, new Hitbox(120, 120), 1, false, 0, Optional.empty(), 0, 1),
                part("maw", 0, 20, 40, 50, false),
                part("core", 0, 150, 60, 100, true));
        var attacks = List.of(
                new BossSpec.Attack(
                        "orbs",
                        BossSpec.Pattern.AIMED,
                        ORB,
                        0.15,
                        false,
                        1,
                        0,
                        0,
                        0,
                        List.of(POD_1, POD_2, POD_3, POD_4),
                        true,
                        Optional.empty()),
                new BossSpec.Attack(
                        "acid",
                        BossSpec.Pattern.LOB,
                        acid(ring),
                        0.15,
                        false,
                        1,
                        0,
                        0,
                        0,
                        List.of(MORTAR_LEFT, MORTAR_RIGHT),
                        true,
                        Optional.empty()),
                new BossSpec.Attack("spit", BossSpec.Pattern.FAN, FAN, 0.15, false, 9, 0, 0, 0, List.of(MAW)),
                new BossSpec.Attack(
                        "sweep",
                        BossSpec.Pattern.SWEEP,
                        SWEEP_TURN,
                        0.15,
                        false,
                        1,
                        0,
                        0,
                        0,
                        List.of(CORE),
                        false,
                        Optional.of(sweep(arc))),
                new BossSpec.Attack("ring", BossSpec.Pattern.RING, RING_GUN, 0.15, false, 14, 0, 0, 0, List.of(CORE)));
        var roots = new BossSpec.Phase(
                "Roots",
                ROOTS,
                0,
                List.of(ORBS, ACID),
                false,
                Optional.empty(),
                List.of(),
                Double.NaN,
                Double.POSITIVE_INFINITY,
                0,
                Optional.empty(),
                Optional.empty(),
                BossSpec.PhaseArena.NONE);
        var maw = new BossSpec.Phase(
                "Maw",
                List.of(MAW),
                0,
                List.of(SPIT),
                false,
                Optional.empty(),
                List.of(MAW),
                Double.NaN,
                Double.POSITIVE_INFINITY,
                0,
                Optional.empty(),
                Optional.of(new BossSpec.Windows(
                        List.of(List.of(MAW)),
                        8,
                        8,
                        0,
                        false,
                        List.of(new BossSpec.Spawn("wraiths", PartDSpecs.wraith(), wraiths, 200, 0, 0, 4)))),
                BossSpec.PhaseArena.NONE);
        var free = new BossSpec.Phase(
                "Free",
                List.of(CORE),
                0,
                List.of(SWEEP, RING),
                true,
                Optional.empty(),
                List.of(CORE),
                Double.NaN,
                Double.POSITIVE_INFINITY,
                BossSpec.PHASE_DELAY_SECONDS,
                Optional.of(new BossSpec.Move(FREE_X, FREE_Y, Layer.AIR, LIFT_SECONDS, 1, 0)),
                Optional.empty(),
                new BossSpec.PhaseArena(
                        0,
                        List.of(),
                        Double.NaN,
                        BossSpec.Slamming.NONE,
                        0,
                        Optional.empty(),
                        Optional.empty(),
                        Optional.of(PickupType.ARMOUR_PATCH)));
        List<BossSpec.Offset> rooted = new ArrayList<>();
        List<BossSpec.Offset> freed = new ArrayList<>();
        for (LevelScript.PartSpec part : parts) {
            rooted.add(new BossSpec.Offset(part.dx(), part.dy()));
            freed.add(
                    part.name().equals("core") ? new BossSpec.Offset(0, 0) : new BossSpec.Offset(part.dx(), part.dy()));
        }
        var arena = new BossSpec.Arena(
                BossSpec.Lanes.NONE,
                null,
                List.of(
                        Layer.GROUND,
                        Layer.GROUND,
                        Layer.GROUND,
                        Layer.GROUND,
                        Layer.GROUND,
                        Layer.GROUND,
                        Layer.GROUND,
                        Layer.GROUND,
                        Layer.GROUND),
                List.of());
        return new LevelScript.SetPieceSpec(
                "test-spire",
                new Hitbox(440, 440),
                new Hitbox(440, 440),
                25,
                parts,
                List.of(),
                Optional.empty(),
                Optional.of(new BossSpec(
                        ARRIVE,
                        PlayField.WIDTH / 2.0,
                        HOVER_Y,
                        0,
                        SWAY,
                        SWAY_PERIOD,
                        Layer.GROUND,
                        false,
                        "TEST SPIRE",
                        135,
                        List.of(),
                        attacks,
                        List.of(roots, maw, free),
                        false,
                        List.of(TRUNK),
                        List.of(
                                new BossSpec.Pose("rooted", new Hitbox(440, 440), rooted),
                                new BossSpec.Pose("free", new Hitbox(100, 140), freed)),
                        3,
                        Optional.of(arena))));
    }

    /** The arena level: {@link #ARRIVE} s at {@link #SPEED}, the arena of 200 s at speed 0, 10 s after it. */
    static LevelScript level(LevelScript.SetPieceSpec spire) {
        return new LevelScript(
                14,
                2,
                0,
                List.of(
                        new LevelScript.Section(ARRIVE, SPEED),
                        new LevelScript.Section(ARRIVE + 200, 0, true),
                        new LevelScript.Section(ARRIVE + 210, SPEED)),
                List.of(),
                List.of(),
                List.of(),
                0,
                List.of(),
                LevelScript.Secondary.NONE,
                List.of(),
                List.of(),
                List.of(spire));
    }

    static Sortie sortie(LevelScript level, Rules rules) {
        return new Sortie(1, TestSpecs.LOADOUT, level, rules, TestSpecs.FULL_ARMOUR);
    }

    /** Steps until {@code type} happens (returns its values in that step), at most {@code seconds}. */
    static List<Integer> until(Sortie sortie, SimEvents.Type type, double seconds, int commands) {
        for (int i = 0; i < SimStep.ticks(seconds); i++) {
            sortie.step(commands);
            List<Integer> values = values(sortie, type);
            if (!values.isEmpty()) {
                return values;
            }
        }
        throw new AssertionError(type + " within " + seconds + " s");
    }

    static List<Integer> values(Sortie sortie, SimEvents.Type type) {
        List<Integer> values = new ArrayList<>();
        SimEvents events = sortie.events();
        for (int i = 0; i < events.size(); i++) {
            if (events.type(i) == type) {
                values.add(events.value(i));
            }
        }
        return values;
    }

    static void steps(Sortie sortie, double seconds) {
        for (int i = 0; i < SimStep.ticks(seconds); i++) {
            sortie.step(0);
        }
    }

    /** Destroys the six roots: phase 2 (the maw) begins at the next step. */
    static void killRoots(Sortie sortie) {
        // Indexed: a for-each would allocate an iterator in the allocation measurements.
        for (int p = POD_1; p <= MORTAR_RIGHT; p++) {
            sortie.destroyPart(0, p);
        }
    }

    /** From the level start to phase 3's lift: the roots and then the maw destroyed. */
    static Sortie atLift(Rules rules) {
        Sortie sortie = sortie(level(spire()), rules);
        until(sortie, SimEvents.Type.BOSS_SETTLED, 5, 0);
        killRoots(sortie);
        until(sortie, SimEvents.Type.BOSS_PHASE, 0.1, 0);
        sortie.destroyPart(0, MAW);
        until(sortie, SimEvents.Type.BOSS_PHASE, 0.1, 0);
        return sortie;
    }
}
