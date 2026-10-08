package vanguard.sim;

import java.util.List;
import java.util.Optional;

/**
 * M5 part D's units with their data's numbers at medium (design/enemies/air/wraith and mote-swarm),
 * and their waves, for the simulation's tests; vanguard.content's PartDUnitsDataTest checks the
 * same numbers from the data.
 */
final class PartDSpecs {
    private PartDSpecs() {}

    /**
     * The Wraith's burst: two in its hold, 0.9 s after it stops and 1.2 s apart, 220 px/s, `medium` = 6,
     * straight up the screen as a fixed 40° fan (2026-10-08).
     */
    static EnemyGun burst(int shots) {
        return new EnemyGun(
                1.2,
                0.9,
                shots,
                220,
                6,
                false,
                1,
                Math.toRadians(40),
                Double.POSITIVE_INFINITY,
                Double.POSITIVE_INFINITY,
                Optional.empty(),
                Optional.empty(),
                0.12,
                true);
    }

    /** The Wraith at medium: hold 2.5 s, 5-shot bursts. */
    static EnemySpec wraith() {
        return wraith(2.5, 5);
    }

    /** The Wraith with a hold of {@code hold} s and {@code shots}-shot bursts (hard: 3.0 s, 7). */
    static EnemySpec wraith(double hold, int shots) {
        return new EnemySpec(
                "wraith",
                16,
                new Hitbox(50, 40),
                Layer.HIGH_AIR,
                15,
                false,
                30,
                200,
                Optional.empty(),
                Optional.of(120.0),
                Optional.of(new EnemySpec.Hover(new Range(hold, hold), new Range(470, 520))),
                Optional.empty(),
                Optional.of(burst(shots)),
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
                Optional.empty(),
                Optional.empty(),
                false,
                Optional.empty(),
                Optional.empty(),
                Optional.of(new EnemySpec.Cloak(Layer.AIR, 0.4)),
                Optional.of(new EnemySpec.Ambush(1.5, 40, 120)),
                Optional.empty());
    }

    /** The flock of the Mote Swarm's data. */
    static final EnemySpec.FlockSpec FLOCK =
            new EnemySpec.FlockSpec(18, 48, 0.5, 0.3, 0.6, 200, 260, Math.toRadians(360), 24);

    /** One mote: 1 HP, `tiny` contact 6 and destroyed by it, 2 credits, its leader point at 200 px/s. */
    static final EnemySpec MOTE = new EnemySpec(
            "mote-swarm",
            1,
            new Hitbox(10, 10),
            Layer.AIR,
            6,
            true,
            2,
            200,
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
            Optional.empty(),
            Optional.empty(),
            false,
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.of(FLOCK));

    /** A rear ambush of {@code count} {@code wraith}s at {@code t}. */
    static WaveSpec ambush(double t, EnemySpec wraith, int count) {
        return new WaveSpec(
                t,
                WaveSpec.Formation.REAR_AMBUSH,
                wraith,
                count,
                WaveSpec.Entry.REAR,
                WaveSpec.Edge.NONE,
                Optional.empty(),
                Optional.empty(),
                1,
                Optional.empty(),
                Optional.empty(),
                List.of(),
                Optional.empty(),
                List.of());
    }

    /**
     * The route of a Level 10 swarm (x, px below the top edge): in at the top left, a swirl across in
     * front of the ship and out at the bottom right.
     */
    static final List<WaveSpec.At> ROUTE = List.of(
            new WaveSpec.At(80, -60),
            new WaveSpec.At(120, 120),
            new WaveSpec.At(300, 200),
            new WaveSpec.At(360, 330),
            new WaveSpec.At(240, 420),
            new WaveSpec.At(520, 600));

    /** A swarm of {@code count} motes at {@code t} on {@link #ROUTE}, looping back {@code loops} times (0: none). */
    static WaveSpec swarm(double t, int count, int loops) {
        return new WaveSpec(
                t,
                WaveSpec.Formation.SWARM,
                MOTE,
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
                List.of(ROUTE),
                loops == 0 ? Optional.empty() : Optional.of(new WaveSpec.LoopBack(1.5, List.of(), loops)));
    }

    /** A level of {@code seconds} at 190 px/s with {@code waves}. */
    static LevelScript level(double seconds, List<WaveSpec> waves) {
        return PartCSpecs.level(seconds, 190, waves, List.of());
    }

    /** A sortie of {@code level} with the starter loadout, the ship untouchable. */
    static Sortie sortie(LevelScript level) {
        return new Sortie(1, TestSpecs.LOADOUT, level, TestSpecs.RULES.withInvulnerableShip(), TestSpecs.FULL_ARMOUR);
    }

    /** The units of {@code slug} on the field. */
    static List<Enemy> all(Sortie sortie, String slug) {
        List<Enemy> found = new java.util.ArrayList<>();
        for (int i = 0; i < sortie.enemyCount(); i++) {
            if (sortie.enemy(i).spec().slug().equals(slug)) {
                found.add(sortie.enemy(i));
            }
        }
        return found;
    }
}
