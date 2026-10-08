package vanguard.content;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * design/allies/data.yaml: the allies' specs, one entry per ally slug; the simulation flies them as
 * an escort objective's convoy (Level 04's crawlers on the road, M5 part D's shuttles in the air).
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
     * @param follows what it moves along ({@code road}: the level's road at the scroll speed; M5
     *     part D, {@code lanes}: its station in the level's band with the lane sway)
     * @param smokeBelow the share of its HP under which it smokes
     * @param firstLevel the level it is introduced in, optional: an ally no level's data escorts yet
     *     (its art lands before its level) goes in that level's sprite atlas
     * @param banks M5 part D, presentation: its rendered banking frames
     * @param glide M5 part D, presentation: s of a lost unit's glide into {@code far}
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
            double smokeBelow,
            Optional<Integer> firstLevel,
            Optional<Banks> banks,
            Optional<Double> glide) {
        /** What a ground ally follows. */
        public static final String ROAD = "road";
        /** M5 part D: what an air ally follows, its station in the level's band. */
        public static final String LANES = "lanes";

        public Ally {
            Layers.of(layer);
            Check.positive("hp", hp);
            Check.share("smoke_below", smokeBelow);
            firstLevel.ifPresent(level -> Check.positive("first_level", level));
            Check.that(
                    follows.equals(ROAD) || follows.equals(LANES), "follows: 'road' or 'lanes', was '" + follows + "'");
            boolean air = follows.equals(LANES);
            Check.that(
                    air == layer.equals("air"),
                    "an ally on the air layer follows lanes, one on the ground follows the road");
            Check.that(
                    !air || !damagedBy.objectiveAimed() && damagedBy.claws() == 0,
                    "an air ally is hurt by bullets and contact, not by objective-aimed shots or claws");
            Check.that(
                    air || !damagedBy.hitByBullets() && !damagedBy.hitByContact(),
                    "bullets and contact hurt an air ally only (a ground ally lies below the player's plane)");
            glide.ifPresent(seconds -> Check.positive("glide", seconds));
        }

        /** M5 part D: whether it flies in the air (its station in the band), not on the road. */
        public boolean air() {
            return follows.equals(LANES);
        }
    }

    /**
     * M5 part D, presentation: its rendered banking frames, {@code frames} of them (odd: level in
     * the middle), the full bank at {@code full} px/s of sideways speed.
     */
    public record Banks(int frames, double full) {
        public Banks {
            Check.positive("frames", frames);
            Check.that(frames % 2 == 1, "frames: an odd number (level in the middle), was " + frames);
            Check.positive("full", full);
        }
    }

    /**
     * What hurts an ally.
     *
     * @param objectiveAimed only enemy shots aimed at it by the target-the-objective hook
     * @param claws damage per second while a walker's hitbox overlaps it
     * @param bullets M5 part D (user decision D2 = a): every enemy bullet touching its hit box hurts
     *     it by its class and is spent
     * @param contact M5 part D: an {@code air} enemy's body hurts it by the enemy's tier once per
     *     contact, a {@code tiny} or {@code small} one destroyed by the impact
     */
    public record DamagedBy(
            boolean objectiveAimed, double claws, Optional<Boolean> bullets, Optional<Boolean> contact) {
        public DamagedBy {
            Check.notNegative("claws", claws);
        }

        /** Whether every enemy bullet hurts it. */
        public boolean hitByBullets() {
            return bullets.orElse(false);
        }

        /** Whether enemy bodies on its plane hurt it. */
        public boolean hitByContact() {
            return contact.orElse(false);
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
