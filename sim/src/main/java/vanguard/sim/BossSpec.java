package vanguard.sim;

import java.util.List;
import java.util.Optional;

/**
 * A boss's script at one difficulty (design/enemies/bosses): it arrives at {@code arriveSeconds} on
 * the level clock, descends from above the top edge at {@code descentSpeed} px/s (taking no damage)
 * until its centre is at {@code hoverY}, then hovers there with a sideways sine until it is killed.
 * Its parts (heads, core) are those of its {@link LevelScript.SetPieceSpec}; the {@link Chain}s
 * hang parts on articulated necks that bend toward the player. Its {@link Phase}s run in order,
 * each ending on how many of a set of parts are still alive.
 *
 * @param x its centre's x at rest, px from the left
 * @param hoverY its centre's height at rest, px from the bottom edge (y up)
 * @param sineAmplitude px each side of {@code x} it sways once settled
 * @param sinePeriod s per sway
 * @param midBoss a mid-boss has the short bar
 * @param barName the name on its bar
 * @param parSeconds a kill within this time from the bar appearing pays the Boss rush bonus
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
        List<Phase> phases) {
    public BossSpec {
        chains = List.copyOf(chains);
        attacks = List.copyOf(attacks);
        phases = List.copyOf(phases);
        if (phases.isEmpty() || !(descentSpeed > 0) || !(sinePeriod > 0) || !(parSeconds > 0)) {
            throw new IllegalArgumentException(barName + ": a boss has phases, a descent, a sway period and a par");
        }
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
        SPIRAL
    }

    /**
     * A boss attack, with the difficulty's levers and changes applied.
     *
     * @param gun its interval, burst, bullet speed and damage
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
     * A phase: it ends once at most {@code left} of {@code untilParts} are alive. It fires
     * {@code attacks} together, or in turn when {@code alternate}; sends its {@code stream}; the
     * parts in {@code exposes} take damage only from this phase on; the chains bend
     * {@code bendRadians} in it (NaN: their own bend).
     */
    public record Phase(
            String name,
            List<Integer> untilParts,
            int left,
            List<Integer> attacks,
            boolean alternate,
            Optional<Stream> stream,
            List<Integer> exposes,
            double bendRadians) {
        public Phase {
            untilParts = List.copyOf(untilParts);
            attacks = List.copyOf(attacks);
            exposes = List.copyOf(exposes);
        }
    }

    /**
     * A stream of {@code count} units of {@code enemy} from the side edges, {@code intervalSeconds}
     * apart (alternating sides, or all from one): the first when the boss settles, then every
     * {@code everySeconds} while its phase lasts.
     */
    public record Stream(EnemySpec enemy, int count, double everySeconds, double intervalSeconds, WaveSpec.Edge edge) {}
}
