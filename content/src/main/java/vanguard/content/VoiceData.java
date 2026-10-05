package vanguard.content;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * design/audio/voice/data.yaml: the speaker table of the spoken radio lines and briefings. The
 * renderer and the game compute a line's key from it (see {@code vanguard.content.voice.VoiceLines}).
 *
 * @param expressions the Chatterbox settings per expression ({@code neutral}, {@code grim},
 *     {@code fierce}) and for shouted lines ({@code shout})
 * @param speakers the voices by slug
 */
public record VoiceData(Map<String, Settings> expressions, Map<String, Speaker> speakers) {
    /** The row a shouted line uses whatever its expression. */
    public static final String SHOUT = "shout";

    public VoiceData {
        expressions = Map.copyOf(new TreeMap<>(expressions));
        speakers = Map.copyOf(new TreeMap<>(speakers));
        for (Expression expression : Expression.values()) {
            Check.that(expressions.containsKey(expression.slug()), "expressions: missing '" + expression.slug() + "'");
        }
        Check.that(expressions.containsKey(SHOUT), "expressions: missing '" + SHOUT + "'");
        for (String slug : speakers.keySet()) {
            Check.that(slug.matches("[a-z0-9]+(-[a-z0-9]+)*"), "speakers: '" + slug + "' is not a kebab-case slug");
        }
    }

    /** Chatterbox's emotion controls. */
    public record Settings(double exaggeration, double cfgWeight, double temperature) {
        public Settings {
            Check.notNegative("exaggeration", exaggeration);
            Check.notNegative("cfg_weight", cfgWeight);
            Check.positive("temperature", temperature);
        }

        Settings plus(Shift shift) {
            return new Settings(
                    exaggeration + shift.exaggeration().orElse(0.0),
                    cfgWeight + shift.cfgWeight().orElse(0.0),
                    temperature + shift.temperature().orElse(0.0));
        }
    }

    /** A speaker's offsets to the expression's row (Rook livelier, Okafor drier). */
    public record Shift(Optional<Double> exaggeration, Optional<Double> cfgWeight, Optional<Double> temperature) {}

    /**
     * A voice.
     *
     * @param names the speaker's names as the level data writes them ({@code Dock One}, …)
     * @param ref the reference clip in design/audio/voice/refs, without {@code .wav}
     * @param fixed settings for every line instead of the expression's (the Choir)
     * @param layering {@code choir}: the Choir's layering in post
     * @param pins a line's take seed by the line's key, when the automatic pick sounds wrong
     * @param uncast a speaker whose voice is not cast yet (Level 05's Driver Control): no reference
     *     clip, its lines have no voice file and play as text with the radio blips
     * @param filter {@code pa}: its radio lines go through the public-address filter instead of the
     *     radio filter (Level 06's perimeter beacon, an automated message from loudspeakers)
     */
    public record Speaker(
            List<String> names,
            Optional<String> ref,
            Optional<Shift> shift,
            Optional<Settings> fixed,
            Optional<String> layering,
            Optional<Map<String, Integer>> pins,
            Optional<Boolean> uncast,
            Optional<String> filter) {
        public Speaker {
            Check.notEmpty("names", names);
            Check.that(
                    ref.isPresent() != uncast.orElse(false),
                    "ref: a cast speaker has its reference clip, an uncast one none");
            ref.ifPresent(clip ->
                    Check.that(clip.matches("ref-[a-z0-9-]+"), "ref: a clip name ref-<speaker>, was '" + clip + "'"));
            Check.that(layering.map("choir"::equals).orElse(true), "layering: only 'choir' is known");
            Check.that(filter.map("pa"::equals).orElse(true), "filter: only 'pa' is known");
        }
    }

    /** The slug of the voice that speaks as {@code name}, if any; none for an uncast speaker. */
    public Optional<String> voiceOf(String name) {
        return speakers.entrySet().stream()
                .filter(entry -> entry.getValue().names().contains(name))
                .filter(entry -> entry.getValue().ref().isPresent())
                .map(Map.Entry::getKey)
                .findFirst();
    }

    /** Whether {@code name} is a speaker marked {@code uncast}: its lines have no voice yet. */
    public boolean uncast(String name) {
        return speakers.values().stream()
                .anyMatch(speaker ->
                        speaker.names().contains(name) && speaker.uncast().orElse(false));
    }

    /** The settings a line of {@code voice} is spoken with. */
    public Settings settings(String voice, Expression expression, boolean shout) {
        Speaker speaker = speakers.get(voice);
        if (speaker.fixed().isPresent()) {
            return speaker.fixed().get();
        }
        Settings row = expressions.get(shout ? SHOUT : expression.slug());
        return speaker.shift().map(row::plus).orElse(row);
    }
}
