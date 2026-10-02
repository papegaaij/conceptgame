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
 * @param secrets the number of secrets in the level
 * @param radio the radio chatter cues
 * @param secondary the secondary objective
 */
public record LevelScript(
        int number,
        int act,
        double launchSeconds,
        List<Section> sections,
        List<WaveSpec> waves,
        List<GroundObjectSpec> groundObjects,
        int secrets,
        List<RadioCue> radio,
        Secondary secondary) {
    public LevelScript {
        sections = List.copyOf(sections);
        waves = List.copyOf(waves);
        groundObjects = List.copyOf(groundObjects);
        radio = List.copyOf(radio);
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

    /** Destroy at least {@code killRatio} of all enemies for {@code credits} (before the credit factor). */
    public record Secondary(double killRatio, int credits) {}

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
     * @param subject the enemy slug of a first kill or the secret's name; empty otherwise
     * @param expression the speaker's portrait expression ({@code neutral}, {@code grim}, {@code fierce});
     *     presentation only, nothing in the simulation reads it
     */
    public record RadioCue(
            CueTrigger trigger,
            double t,
            String subject,
            String speaker,
            String line,
            boolean distorted,
            String expression) {}

    public enum CueTrigger {
        TIME,
        FIRST_KILL,
        SECRET,
        SECONDARY_OBJECTIVE,
        LEVEL_END
    }
}
