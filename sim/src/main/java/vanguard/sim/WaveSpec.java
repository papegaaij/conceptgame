package vanguard.sim;

import java.util.List;
import java.util.Optional;

/**
 * One group of a wave in the level script, with the difficulty's changes applied (a mixed wave
 * becomes one spec per group at the same time).
 *
 * @param t seconds from the level start when its first unit enters
 * @param formation how the units are placed and fly (design/enemies, formation vocabulary)
 * @param count units in the group
 * @param entry the edge it enters from
 * @param edge which side of that edge
 * @param holdSeconds how long a pincer holds at the edge or a circle orbits
 * @param warningSeconds how long before {@code t} the edge warning starts, if longer than the minimum
 * @param breakGroup how many units of a circle break toward the player together
 * @param speed px/s instead of the enemy's own speed
 * @param intervalSeconds the gap between two units of a stream
 * @param carried pickups that a unit of this group drops when destroyed
 * @param at a whirl cluster's release point
 * @param paths a walker wave's ground paths, one per unit (a pincer mirrors the first for the
 *     units on the right, a convoy repeats it), in play-field points at {@code t}; a segment
 *     chain's flight paths, one per unit (its head flies it); M5 part D: a swarm's route (its
 *     leader point flies it) and a snake's (every unit flies it)
 * @param loopBack a segment chain's loop-back after its path; M5 part D: a swarm's leader point's
 *     (with its count)
 * @param tag M5 part C: the wave's tag (Level 09's {@code bridge}), which a secondary {@code escapes}
 *     objective may be scoped to; empty for none
 * @param field M5 part E: a {@code field} wave's area (design/enemies/naval/driftjelly)
 */
public record WaveSpec(
        double t,
        Formation formation,
        EnemySpec enemy,
        int count,
        Entry entry,
        Edge edge,
        Optional<Double> holdSeconds,
        Optional<Double> warningSeconds,
        int breakGroup,
        Optional<Double> speed,
        Optional<Double> intervalSeconds,
        List<Carried> carried,
        Optional<At> at,
        List<List<At>> paths,
        Optional<LoopBack> loopBack,
        String tag,
        Optional<Field> field) {
    public WaveSpec {
        carried = List.copyOf(carried);
        paths = paths.stream().map(List::copyOf).toList();
        if (field.isPresent() != (formation == Formation.FIELD)) {
            throw new IllegalArgumentException("a field wave has its area, other waves none");
        }
    }

    /** A wave without M5 part E's field. */
    public WaveSpec(
            double t,
            Formation formation,
            EnemySpec enemy,
            int count,
            Entry entry,
            Edge edge,
            Optional<Double> holdSeconds,
            Optional<Double> warningSeconds,
            int breakGroup,
            Optional<Double> speed,
            Optional<Double> intervalSeconds,
            List<Carried> carried,
            Optional<At> at,
            List<List<At>> paths,
            Optional<LoopBack> loopBack,
            String tag) {
        this(
                t,
                formation,
                enemy,
                count,
                entry,
                edge,
                holdSeconds,
                warningSeconds,
                breakGroup,
                speed,
                intervalSeconds,
                carried,
                at,
                paths,
                loopBack,
                tag,
                Optional.empty());
    }

    /**
     * M5 part E, a {@code field} wave's area (design/enemies/naval/driftjelly; the stated default of
     * 2026-10-08): {@code width} × {@code height} px centred on {@code x} px from the left edge, its
     * leading (bottom) edge entering at the top edge at the wave's {@code t}; its units scattered in
     * it by the level's seeded scatter at least {@code spacing} px apart, scrolling with the ground
     * and drifting at the enemy's speed along the current, {@code currentRadians} from straight down
     * (positive to the right).
     */
    public record Field(double x, double width, double height, double spacing, double currentRadians) {
        public Field {
            if (!(width > 0) || !(height > 0) || !(spacing > 0)) {
                throw new IllegalArgumentException("a field has an area and a spacing");
            }
        }

        /** The units a field holds at its spacing: whole cells of the spacing across and down. */
        public int capacity() {
            return (int) Math.floor(width / spacing) * (int) Math.floor(height / spacing);
        }
    }

    /** A wave without a tag. */
    public WaveSpec(
            double t,
            Formation formation,
            EnemySpec enemy,
            int count,
            Entry entry,
            Edge edge,
            Optional<Double> holdSeconds,
            Optional<Double> warningSeconds,
            int breakGroup,
            Optional<Double> speed,
            Optional<Double> intervalSeconds,
            List<Carried> carried,
            Optional<At> at,
            List<List<At>> paths,
            Optional<LoopBack> loopBack) {
        this(
                t,
                formation,
                enemy,
                count,
                entry,
                edge,
                holdSeconds,
                warningSeconds,
                breakGroup,
                speed,
                intervalSeconds,
                carried,
                at,
                paths,
                loopBack,
                "");
    }

    public WaveSpec(
            double t,
            Formation formation,
            EnemySpec enemy,
            int count,
            Entry entry,
            Edge edge,
            Optional<Double> holdSeconds,
            Optional<Double> warningSeconds,
            int breakGroup,
            Optional<Double> speed,
            Optional<Double> intervalSeconds,
            List<Carried> carried,
            Optional<At> at,
            List<List<At>> paths) {
        this(
                t,
                formation,
                enemy,
                count,
                entry,
                edge,
                holdSeconds,
                warningSeconds,
                breakGroup,
                speed,
                intervalSeconds,
                carried,
                at,
                paths,
                Optional.empty());
    }

    /**
     * A segment chain's loop-back (design/enemies/air/coilwyrm): {@code afterSeconds} after its
     * head reached its path's end (off the screen) it re-enters on {@code path}, shifted sideways
     * so it starts at the head's x; without points straight up from below the bottom edge. M5 part
     * D: a swarm's leader point loops back {@code count} times (each loop-back the next re-entry
     * {@code afterSeconds} after the previous path's end); a chain loops back once.
     */
    public record LoopBack(double afterSeconds, List<At> path, int count) {
        public LoopBack {
            path = List.copyOf(path);
            if (count < 1) {
                throw new IllegalArgumentException("a loop-back comes at least once: " + count);
            }
        }

        /** A single loop-back. */
        public LoopBack(double afterSeconds, List<At> path) {
            this(afterSeconds, path, 1);
        }
    }

    public WaveSpec(
            double t,
            Formation formation,
            EnemySpec enemy,
            int count,
            Entry entry,
            Edge edge,
            Optional<Double> holdSeconds,
            Optional<Double> warningSeconds,
            int breakGroup,
            Optional<Double> speed,
            Optional<Double> intervalSeconds,
            List<Carried> carried,
            Optional<At> at) {
        this(
                t,
                formation,
                enemy,
                count,
                entry,
                edge,
                holdSeconds,
                warningSeconds,
                breakGroup,
                speed,
                intervalSeconds,
                carried,
                at,
                List.of());
    }

    public WaveSpec(
            double t,
            Formation formation,
            EnemySpec enemy,
            int count,
            Entry entry,
            Edge edge,
            Optional<Double> holdSeconds,
            Optional<Double> warningSeconds,
            int breakGroup,
            Optional<Double> speed,
            Optional<Double> intervalSeconds,
            List<Carried> carried) {
        this(
                t,
                formation,
                enemy,
                count,
                entry,
                edge,
                holdSeconds,
                warningSeconds,
                breakGroup,
                speed,
                intervalSeconds,
                carried,
                Optional.empty());
    }

    /** A point in the play field: {@code x} from the left edge, {@code depth} px below the top edge. */
    public record At(double x, double depth) {}

    /** The formations of design/enemies that the simulation flies so far. */
    public enum Formation {
        /** One unit on its own. */
        SINGLE,
        /** One behind the other on the same path. */
        COLUMN,
        /** A column that comes down and turns across the screen at its strafe height. */
        CONVOY,
        /** Tiny spinners released from one point, spiralling out. */
        WHIRL_CLUSTER,
        SNAKE,
        V_WING,
        LINE_ABREAST,
        STREAM,
        PINCER,
        CIRCLE,
        /** A spawner (the carrier) with the units that circle it as escorts (design/enemies/air/brood-pod). */
        CARRIER_ESCORTS,
        /**
         * M5 part C: walkers entering together, each on its own ground path, {@link
         * Formations#PACK_INTERVAL_SECONDS} apart (design/enemies/ground/ravager).
         */
        PACK,
        /**
         * M5 part D: a flock round a leader point that flies the wave's route, with its loop-backs
         * (design/enemies/air/mote-swarm).
         */
        SWARM,
        /**
         * M5 part D: 1–4 units, each in its own lane across the bottom edge, x = (i + 1) × 480 ÷ (n + 1)
         * (design/enemies/air/wraith): an {@link EnemySpec.Ambush} unit enters at the top and comes
         * back up from below.
         */
        REAR_AMBUSH,
        /**
         * M5 part E: units scattered over an area that enters with the sea and drifts on the wave's
         * current (design/enemies/naval/driftjelly), a {@link Field}.
         */
        FIELD
    }

    /** The play-field edge a wave enters from. */
    public enum Entry {
        FRONT,
        SIDES,
        REAR
    }

    /** Which side of the entry edge: {@code NONE} at the front means centred, at the sides both edges. */
    public enum Edge {
        NONE,
        LEFT,
        RIGHT,
        ALTERNATING
    }

    /**
     * A pickup carried by one unit of the group.
     *
     * @param unit the unit's index in the group, or {@link #LAST}
     */
    public record Carried(PickupType pickup, int unit) {
        /** The group's last unit, whatever its size on the difficulty. */
        public static final int LAST = -1;

        /** Carried by the group's first or last unit. */
        public Carried(PickupType pickup, boolean lastUnit) {
            this(pickup, lastUnit ? LAST : 0);
        }

        /** Whether the last unit carries it. */
        public boolean lastUnit() {
            return unit == LAST;
        }
    }
}
