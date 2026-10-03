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
        Optional<At> at) {
    public WaveSpec {
        carried = List.copyOf(carried);
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
        CIRCLE
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
