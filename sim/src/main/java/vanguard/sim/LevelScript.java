package vanguard.sim;

import java.util.List;
import java.util.Optional;

/**
 * A level's script at one difficulty (design/campaign/&lt;act&gt;/&lt;level&gt;/data.yaml), built
 * by {@code vanguard.content.SimSpecs}: the scroll, the waves, the ground objects, the radio
 * cues and the objectives. The level ends when the scroll reaches the end of the last section
 * (the {@code reach-end} primary objective).
 *
 * @param number the global level number (01–50)
 * @param act the act the level belongs to
 * @param launchSeconds the non-playable launch at the start
 * @param sections back to back from t = 0
 * @param waves in time order
 * @param groundObjects every placed ground object, in time order
 * @param groundUnits every enemy fixed to the ground, in time order
 * @param secrets the number of secrets in the level
 * @param radio the radio chatter cues
 * @param secondary the secondary objective
 * @param cranes the crane hazards
 */
public record LevelScript(
        int number,
        int act,
        double launchSeconds,
        List<Section> sections,
        List<WaveSpec> waves,
        List<GroundObjectSpec> groundObjects,
        List<GroundUnit> groundUnits,
        int secrets,
        List<RadioCue> radio,
        Secondary secondary,
        List<CraneSpec> cranes) {
    public LevelScript {
        sections = List.copyOf(sections);
        waves = List.copyOf(waves);
        groundObjects = List.copyOf(groundObjects);
        groundUnits = List.copyOf(groundUnits);
        radio = List.copyOf(radio);
        cranes = List.copyOf(cranes);
        if (sections.isEmpty()) {
            throw new IllegalArgumentException("a level needs at least one section");
        }
    }

    /** The level's length in seconds. */
    public double seconds() {
        return sections.getLast().end();
    }

    /** A stretch of the scroll ending at {@code end} seconds, scrolling at {@code speed} px/s. */
    public record Section(double end, double speed) {}

    /**
     * The secondary objective: destroy at least {@code killRatio} of all enemies for
     * {@code credits} (before the credit factor), or, with {@code groups}, clear every ground
     * unit of a group (Level 02's docks) before the last of them leaves the screen, for
     * {@code credits} per group; the objective is met when every group is cleared.
     *
     * @param groups the groups' names (the ground units' {@link GroundUnit#group()} indexes them); empty for a kill ratio
     */
    public record Secondary(double killRatio, int credits, List<String> groups) {
        public Secondary {
            groups = List.copyOf(groups);
        }

        public Secondary(double killRatio, int credits) {
            this(killRatio, credits, List.of());
        }

        /** Whether the objective is about groups rather than the kill ratio. */
        public boolean byGroups() {
            return !groups.isEmpty();
        }
    }

    /**
     * An enemy fixed to the ground layer (a turret), entering at the top edge at {@code t} at
     * {@code x}, in the secondary objective's group {@code group} (-1 for none).
     */
    public record GroundUnit(double t, double x, EnemySpec enemy, int group) {}

    /**
     * A crane hazard (design/campaign, Level 02: Crane Four): an arm hanging from a pivot above the
     * play field that swings between two angles, blinking its lights for the telegraph before each
     * swing. It is lowered from along the gantry during the telegraph before its first swing and
     * raised back after its last one. The arm deals contact damage, at most once per
     * {@link #HIT_INTERVAL_SECONDS}, and blocks every shot; a clamp at its tip counts the player's
     * hits while the arm swings and releases a secret's hidden crate after enough of them.
     *
     * @param pivotX the pivot in play-field px
     * @param pivotY the pivot, above the top edge
     * @param length the arm, px
     * @param width the arm's thickness, px
     * @param fromRadians the angle the first swing starts at, from straight down, positive to the right
     * @param toRadians the angle the first swing ends at; swings alternate between the two
     * @param swings the times the swings start, s from the level start
     * @param swingSeconds how long a swing takes
     * @param telegraphSeconds how long the lights blink before a swing
     * @param damage contact damage (shield first)
     * @param clampHits the player's hits on the clamp that release the crate; 0 for no clamp
     * @param crateCredits the crate's credits
     * @param secret the secret's name, for its radio cue
     */
    public record CraneSpec(
            double pivotX,
            double pivotY,
            double length,
            double width,
            double fromRadians,
            double toRadians,
            List<Double> swings,
            double swingSeconds,
            double telegraphSeconds,
            double damage,
            int clampHits,
            int crateCredits,
            String secret) {
        /** The arm hits the ship at most once in this time. */
        public static final double HIT_INTERVAL_SECONDS = 1;

        public CraneSpec {
            swings = List.copyOf(swings);
            if (swings.isEmpty()) {
                throw new IllegalArgumentException("a crane swings at least once");
            }
        }
    }

    /**
     * An object on the ground layer, entering at the top edge at {@code t} and scrolling with the
     * ground: a destructible ({@code hp}, {@code bounty}, {@code drop}) or a trigger that releases a
     * secret's hidden crate after {@code hits} hits.
     *
     * @param x its position in the play field
     * @param size its hit box
     * @param crateCredits the hidden crate's credits (triggers only)
     * @param secret the secret's name (triggers only), for its radio cue
     * @param hardened only {@code anti-ground} weapons damage it; other shots glance off
     */
    public record GroundObjectSpec(
            double t,
            double x,
            Hitbox size,
            double hp,
            int bounty,
            Optional<PickupType> drop,
            int hits,
            int crateCredits,
            String secret,
            boolean hardened) {
        /** Whether it is a trigger rather than a destructible. */
        public boolean trigger() {
            return hits > 0;
        }
    }

    /**
     * A radio chatter line and what triggers it.
     *
     * @param t seconds from the level start, for {@link CueTrigger#TIME}
     * @param subject the enemy slug of a first kill, the secret's name or the group's name; empty otherwise
     * @param expression the speaker's portrait expression ({@code neutral}, {@code grim}, {@code fierce});
     *     presentation only, nothing in the simulation reads it
     * @param portrait the portrait's speaker when it is not the speaker's own (a generic one);
     *     presentation only
     */
    public record RadioCue(
            CueTrigger trigger,
            double t,
            String subject,
            String speaker,
            String line,
            boolean distorted,
            String expression,
            String portrait) {
        public RadioCue(
                CueTrigger trigger,
                double t,
                String subject,
                String speaker,
                String line,
                boolean distorted,
                String expression) {
            this(trigger, t, subject, speaker, line, distorted, expression, speaker);
        }
    }

    public enum CueTrigger {
        TIME,
        FIRST_KILL,
        SECRET,
        SECONDARY_OBJECTIVE,
        LEVEL_END,
        /** A group of the secondary objective was cleared; the subject is its name. */
        GROUP_CLEARED,
        /** A group was lost; the subject is its name. */
        GROUP_LOST,
        /** The first group of the attempt was lost. */
        FIRST_GROUP_LOST
    }
}
