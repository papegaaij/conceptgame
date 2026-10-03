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
     */
    public record Speaker(
            List<String> names,
            String ref,
            Optional<Shift> shift,
            Optional<Settings> fixed,
            Optional<String> layering,
            Optional<Map<String, Integer>> pins) {
        public Speaker {
            Check.notEmpty("names", names);
            Check.that(ref.matches("ref-[a-z0-9-]+"), "ref: a clip name ref-<speaker>, was '" + ref + "'");
            Check.that(layering.map("choir"::equals).orElse(true), "layering: only 'choir' is known");
        }
    }

    /** The slug of the voice that speaks as {@code name}, if any. */
    public Optional<String> voiceOf(String name) {
        return speakers.entrySet().stream()
                .filter(entry -> entry.getValue().names().contains(name))
                .map(Map.Entry::getKey)
                .findFirst();
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
