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
     * @param flak M5 part E, presentation (the escort frigate): its flak bursts over the convoy
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
            Optional<Double> glide,
            Optional<Flak> flak) {
        /** What a ground ally follows. */
        public static final String ROAD = "road";
        /** M5 part D: what an air ally follows, its station in the level's band. */
        public static final String LANES = "lanes";
        /**
         * M5 part E: what a naval convoy's ally follows (a level's {@code convoy} block): its
         * screen-space station at the scroll speed, its arena lane at the halt.
         */
        public static final String STATIONS = "stations";

        public Ally {
            Layers.of(layer);
            Check.positive("hp", hp);
            Check.share("smoke_below", smokeBelow);
            firstLevel.ifPresent(level -> Check.positive("first_level", level));
            Check.that(
                    follows.equals(ROAD) || follows.equals(LANES) || follows.equals(STATIONS),
                    "follows: 'road', 'lanes' or 'stations', was '" + follows + "'");
            boolean air = follows.equals(LANES);
            boolean naval = follows.equals(STATIONS);
            Check.that(
                    air == layer.equals("air"),
                    "an ally on the air layer follows lanes, one on the ground follows the road or stations");
            Check.that(naval || damagedBy.slams().isEmpty(), "only a naval convoy's ally (stations) is hurt by slams");
            Check.that(
                    !naval
                            || (!damagedBy.objectiveAimed()
                                    && damagedBy.claws() == 0
                                    && !damagedBy.hitByBullets()
                                    && !damagedBy.hitByContact()),
                    "a naval convoy's ally (stations) is hurt only by slams");
            Check.that(naval || flak.isEmpty(), "only a naval convoy's ally (stations) fires flak");
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

        /** M5 part E: whether it sails in a naval convoy (its screen-space station), not on a road. */
        public boolean naval() {
            return follows.equals(STATIONS);
        }
    }

    /** M5 part E, presentation (the escort frigate): a flak burst every {@code every} s, hitting nothing. */
    public record Flak(double every) {
        public Flak {
            Check.positive("every", every);
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
     * @param slams M5 part E: the hits one boss slam in its lane deals (its {@code hp} then counts
     *     slams: the cargo ship's {@code hp: 2}, {@code slams: 1}); an ally whose {@code damaged_by}
     *     names nothing (the frigate) cannot be damaged and is never lost
     */
    public record DamagedBy(
            boolean objectiveAimed,
            double claws,
            Optional<Boolean> bullets,
            Optional<Boolean> contact,
            Optional<Double> slams) {
        public DamagedBy {
            Check.notNegative("claws", claws);
            slams.ifPresent(n -> Check.positive("slams", n));
        }

        /** M5 part E: the hits a slam in its lane deals; 0 when slams do not hurt it. */
        public double hitsPerSlam() {
            return slams.orElse(0.0);
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
