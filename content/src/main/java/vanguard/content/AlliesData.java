package vanguard.content;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.Map;
import java.util.TreeMap;

/**
 * design/allies/data.yaml, planned (part D): the allies' specs, one entry per ally slug. Read and
 * checked; the simulation does not fly allies yet.
 *
 * @param allies the allies by slug
 */
public record AlliesData(Map<String, Ally> allies) {
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public AlliesData {
        allies = Map.copyOf(new TreeMap<>(allies));
    }

    /**
     * An ally unit.
     *
     * @param size the sprite, px
     * @param hp at medium; the level sets the difficulty variants
     * @param follows what it moves along ({@code road}: the level's road at the scroll speed)
     * @param smokeBelow the share of its HP under which it smokes
     */
    public record Ally(
            String name,
            String layer,
            Size size,
            Size hitbox,
            double hp,
            DamagedBy damagedBy,
            String follows,
            Headings headings,
            double smokeBelow) {
        public Ally {
            Layers.of(layer);
            Check.positive("hp", hp);
            Check.share("smoke_below", smokeBelow);
        }
    }

    /**
     * What hurts an ally.
     *
     * @param objectiveAimed only enemy shots aimed at it by the target-the-objective hook
     * @param claws damage per second while a walker's hitbox overlaps it
     */
    public record DamagedBy(boolean objectiveAimed, double claws) {
        public DamagedBy {
            Check.notNegative("claws", claws);
        }
    }

    /** The rendered headings: {@code count} of them, {@code step} ° apart. */
    public record Headings(int count, double step) {
        public Headings {
            Check.positive("count", count);
            Check.positive("step", step);
        }
    }
}
