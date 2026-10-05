package vanguard.sim;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A boss's script at one difficulty (design/enemies/bosses): it arrives at {@code arriveSeconds} on
 * the level clock on {@code layer}, descends from above the top edge at {@code descentSpeed} px/s
 * until its centre is at {@code hoverY}, then holds there with a sideways sine until it is killed.
 * Its parts (heads, core, sacs) are those of its {@link LevelScript.SetPieceSpec}; the
 * {@link Chain}s hang parts on articulated necks that bend toward the player. Its {@link Phase}s
 * run in order, each ending on how many of a set of parts are still alive or on its timer; a phase
 * may start with a {@link Move} (a descent to another layer and a turn into another {@link Pose}),
 * open its parts in {@link Windows} that release {@link Spawn}s, and fire its attacks.
 *
 * <p>Part G (the Brood Carrier): a boss that {@code engagesOnArrival} starts its first phase when
 * it arrives (its entrance is part of the fight) instead of when it settles; its
 * {@code armoured} parts fire but take no damage, are not in the bar and pay nothing; its
 * {@code poses} give each part its offset per pose (the first is the arrival pose).
 *
 * @param x its centre's x at rest, px from the left
 * @param hoverY its centre's height at rest, px from the bottom edge (y up)
 * @param sineAmplitude px each side of {@code x} it sways once settled
 * @param sinePeriod s per sway
 * @param layer the layer it arrives on ({@code high-air}: out of reach of all but homing shots,
 *     drawn at the high-air scale, no contact)
 * @param midBoss a mid-boss has the short bar
 * @param barName the name on its bar
 * @param parSeconds a kill within this time from the bar appearing pays the Boss rush bonus
 * @param engagesOnArrival its first phase starts when it arrives, not when it settles
 * @param armoured the fire-only parts: never damaged, out of the bar and the bounty
 * @param poses the part layouts, the arrival pose first; empty: the parts' own offsets only
 * @param deathSeconds how long its chained death runs (the parts bursting tail to head), for the
 *     game; 0 for the mid-boss's quick chain
 */
public record BossSpec(
        double arriveSeconds,
        double x,
        double hoverY,
        double descentSpeed,
        double sineAmplitude,
        double sinePeriod,
        Layer layer,
        boolean midBoss,
        String barName,
        double parSeconds,
        List<Chain> chains,
        List<Attack> attacks,
        List<Phase> phases,
        boolean engagesOnArrival,
        List<Integer> armoured,
        List<Pose> poses,
        double deathSeconds) {
    /** The beat a later phase holds its fire by default in the data (the frigate's crown opening), s. */
    public static final double PHASE_DELAY_SECONDS = 1;
    /** How much larger a unit is drawn on {@code high-air} than on the play plane; a boss's part offsets grow with it. */
    public static final double HIGH_AIR_SCALE = 1.25;

    public BossSpec {
        chains = List.copyOf(chains);
        attacks = List.copyOf(attacks);
        phases = List.copyOf(phases);
        armoured = List.copyOf(armoured);
        poses = List.copyOf(poses);
        if (phases.isEmpty() || !(descentSpeed > 0) || !(sinePeriod > 0) || !(parSeconds > 0) || deathSeconds < 0) {
            throw new IllegalArgumentException(barName + ": a boss has phases, a descent, a sway period and a par");
        }
        for (Phase phase : phases) {
            if (phase.move().isPresent() && phase.move().get().pose() >= Math.max(1, poses.size())) {
                throw new IllegalArgumentException(barName + ": phase " + phase.name() + " turns into no pose");
            }
        }
    }

    /** A boss of Level 05's kind: it engages when it settles, all parts damageable, one pose. */
    public BossSpec(
            double arriveSeconds,
            double x,
            double hoverY,
            double descentSpeed,
            double sineAmplitude,
            double sinePeriod,
            Layer layer,
            boolean midBoss,
            String barName,
            double parSeconds,
            List<Chain> chains,
            List<Attack> attacks,
            List<Phase> phases) {
        this(
                arriveSeconds,
                x,
                hoverY,
                descentSpeed,
                sineAmplitude,
                sinePeriod,
                layer,
                midBoss,
                barName,
                parSeconds,
                chains,
                attacks,
                phases,
                false,
                List.of(),
                List.of(),
                0);
    }

    /** The distinct units its windows release, for the level's enemy kinds. */
    public List<EnemySpec> spawnKinds() {
        List<EnemySpec> kinds = new ArrayList<>();
        for (Phase phase : phases) {
            for (Spawn spawn : phase.windows().map(Windows::spawns).orElse(List.of())) {
                if (!kinds.contains(spawn.enemy())) {
                    kinds.add(spawn.enemy());
                }
            }
        }
        return List.copyOf(kinds);
    }

    /**
     * A neck: {@code segments} armoured hit boxes of {@code box} from the anchor ({@code fromDx},
     * {@code fromDy}) on the body (its own frame, as a part's offset) to {@code part}, which sits at
     * the chain's end; at rest the chain runs straight to that part's offset. The anchor turns
     * toward the player by up to {@code bendRadians} and each piece after it follows the one before
     * {@code lagSeconds} late (a first-order lag), so the head swings through last.
     */
    public record Chain(
            String name,
            double fromDx,
            double fromDy,
            int part,
            int segments,
            Hitbox box,
            double lagSeconds,
            double bendRadians) {
        public Chain {
            if (segments < 1 || !(lagSeconds > 0) || bendRadians < 0) {
                throw new IllegalArgumentException(name + ": a chain has segments, a lag and a bend");
            }
        }
    }

    /** The attack patterns a boss fires (design/enemies, attack vocabulary). */
    public enum Pattern {
        /** Bursts of aimed shots: {@link Attack#gun()}'s burst, {@link Attack#burstGapSeconds()} apart. */
        AIMED,
        /** A ring of {@link Attack#count()} bullets, once at the start of its turn. */
        RING,
        /** {@link Attack#arms()} arms turning at {@link Attack#turnRadiansPerSecond()}, a bullet per arm every interval. */
        SPIRAL,
        /** An n-way fan centred on the ship: {@link Attack#gun()}'s fan and spread, once per interval. */
        FAN
    }

    /**
     * A boss attack, with the difficulty's levers and changes applied.
     *
     * @param gun its interval, burst, bullet speed and damage (a fan's bullets and spread)
     * @param rotate the parts that fire it take turns: one volley every interval among the living ones
     * @param durationSeconds how long a spiral runs in an alternation (a ring's turn is its interval)
     * @param parts the parts that fire it; empty: the living parts the phase ends on
     */
    public record Attack(
            String name,
            Pattern pattern,
            EnemyGun gun,
            double burstGapSeconds,
            boolean rotate,
            int count,
            int arms,
            double turnRadiansPerSecond,
            double durationSeconds,
            List<Integer> parts) {
        public Attack {
            parts = List.copyOf(parts);
        }

        /** How long its turn lasts in an alternation. */
        public double turnSeconds() {
            return pattern == Pattern.SPIRAL ? durationSeconds : gun.intervalSeconds();
        }
    }

    /**
     * A phase: it ends once at most {@code left} of {@code untilParts} are alive, or {@code seconds}
     * after it engaged (a timeout; infinite for none). It starts with its {@code move}, if any (the
     * boss takes no damage and holds its fire until the move is done); then, {@code delaySeconds}
     * later, it fires {@code attacks} together (each a volley one interval after that), or in turn
     * when {@code alternate}, and opens its {@code windows}; sends its {@code stream}; the parts in
     * {@code exposes} take damage only from this phase on; the chains bend {@code bendRadians} in it
     * (NaN: their own bend).
     */
    public record Phase(
            String name,
            List<Integer> untilParts,
            int left,
            List<Integer> attacks,
            boolean alternate,
            Optional<Stream> stream,
            List<Integer> exposes,
            double bendRadians,
            double seconds,
            double delaySeconds,
            Optional<Move> move,
            Optional<Windows> windows) {
        public Phase {
            untilParts = List.copyOf(untilParts);
            attacks = List.copyOf(attacks);
            exposes = List.copyOf(exposes);
            if (!(seconds > 0) || !(delaySeconds >= 0)) {
                throw new IllegalArgumentException(name + ": a phase has a positive timeout and a delay");
            }
        }

        /** A phase of Level 05's kind: no timeout, delay, move or windows. */
        public Phase(
                String name,
                List<Integer> untilParts,
                int left,
                List<Integer> attacks,
                boolean alternate,
                Optional<Stream> stream,
                List<Integer> exposes,
                double bendRadians) {
            this(
                    name,
                    untilParts,
                    left,
                    attacks,
                    alternate,
                    stream,
                    exposes,
                    bendRadians,
                    Double.POSITIVE_INFINITY,
                    0,
                    Optional.empty(),
                    Optional.empty());
        }

        /** Whether it ends on its timer as well as (or instead of) on its parts. */
        public boolean timed() {
            return Double.isFinite(seconds);
        }
    }

    /**
     * A stream of {@code count} units of {@code enemy} from the side edges, {@code intervalSeconds}
     * apart (alternating sides, or all from one): the first when the boss settles, then every
     * {@code everySeconds} while its phase lasts.
     */
    public record Stream(EnemySpec enemy, int count, double everySeconds, double intervalSeconds, WaveSpec.Edge edge) {}

    /**
     * A part layout: each part's offset from the centre in this pose (px, x right, y up, on the play
     * plane; on {@code high-air} the offsets grow with the {@link #HIGH_AIR_SCALE}), and the armoured
     * body's hit box. A pose is drawn from its own pre-rendered sprites: nothing is rotated.
     */
    public record Pose(String name, Hitbox body, List<Offset> offsets) {
        public Pose {
            offsets = List.copyOf(offsets);
        }
    }

    /** A part's offset in a {@link Pose}, px right and up from the centre. */
    public record Offset(double dx, double dy) {}

    /**
     * A phase's opening move, invulnerable: over {@code descendSeconds} the centre glides to
     * ({@code x}, {@code y}) (px from the left, px up from the bottom edge) and the boss sinks or
     * rises to {@code layer}; then over {@code turnSeconds} it turns in place into pose
     * {@code pose} (an index into {@link BossSpec#poses()}), the renderer stepping through the
     * turn's pre-rendered frames. It then holds there (swaying around the new x).
     */
    public record Move(double x, double y, Layer layer, double descendSeconds, int pose, double turnSeconds) {
        public Move {
            if (descendSeconds < 0 || turnSeconds < 0 || pose < 0) {
                throw new IllegalArgumentException("a move has no negative times and a pose");
            }
        }
    }

    /**
     * A phase's windows (the Brood Carrier's sacs): every {@code everySeconds} the next group in
     * order with a living part on the play field opens for {@code openSeconds} (every such group at
     * once when {@code all}), the first {@code offsetSeconds} after the phase's delay. A part in a
     * group takes damage only while open. Each opening releases its {@link Spawn} (the list in turn,
     * one entry per opening) from every group it opened: the entry's count from a group whose parts
     * all live, a share of it rounded up from one with parts destroyed, none from a group without
     * living parts.
     *
     * @param groups part indexes, in the order the windows cycle through them
     */
    public record Windows(
            List<List<Integer>> groups,
            double everySeconds,
            double openSeconds,
            double offsetSeconds,
            boolean all,
            List<Spawn> spawns) {
        public Windows {
            groups = groups.stream().map(List::copyOf).toList();
            spawns = List.copyOf(spawns);
            if (groups.isEmpty()
                    || groups.stream().anyMatch(List::isEmpty)
                    || !(everySeconds > 0)
                    || !(openSeconds > 0)
                    || offsetSeconds < 0) {
                throw new IllegalArgumentException("windows have groups of parts, an interval and an open time");
            }
        }

        /** Whether part {@code p} is in one of its groups. */
        public boolean holds(int p) {
            for (List<Integer> group : groups) {
                if (group.contains(p)) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * What an opened window group releases: {@code count} units of {@code enemy} (the difficulty's
     * count) from its open parts in turn, spread evenly over {@code arcRadians} centred on the
     * direction to the ship, flying straight out at {@code speed} px/s. A unit that glides
     * ({@code glideSeconds} above 0) then holds for its hover time, its gun firing, and leaves
     * down the screen; otherwise it flies on until it leaves the play field.
     */
    public record Spawn(String name, EnemySpec enemy, int count, double speed, double arcRadians, double glideSeconds) {
        public Spawn {
            if (count < 0 || !(speed > 0) || arcRadians < 0 || glideSeconds < 0) {
                throw new IllegalArgumentException(name + ": a spawn has a count, a speed, an arc and a glide");
            }
        }
    }
}
