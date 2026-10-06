package vanguard.content.voice;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import vanguard.content.ActData;
import vanguard.content.BriefingPage;
import vanguard.content.Content;
import vanguard.content.Expression;
import vanguard.content.LevelData;
import vanguard.content.SpecialsData;
import vanguard.content.VoiceData;

/**
 * The spoken lines (design/audio/voice, Offline pipeline): every radio cue of every level (with its
 * easy and hard variants and one line per convoy unit for {@code {ally}}), the secrets' lines, the
 * specials' calls, the low-armour line (source {@code armour}) and every page of the levels' and acts' briefings and of the acts' outros (spoken
 * dry, as briefings), each with the key its rendered file is named by. The renderer (tools/art/voice.py, through {@code :pipeline:voiceLines}) and
 * the game share this list, so the game finds a line's file by the same key.
 */
public final class VoiceLines {
    /** The placeholder in a convoy line that names the unit it is about ("Crawler {ally} is hit!"). */
    public static final String ALLY = "{ally}";

    /** The placeholder in a line that names the ground-target group it is about ("{group} is behind you"). */
    public static final String GROUP = "{group}";

    /** Bumped when the rendering changes in a way that must redo every line. */
    private static final String VERSION = "voice-1";

    private static final String[] NUMBERS = {
        "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine", "Ten"
    };

    private VoiceLines() {}

    /** How a line is post-processed. */
    public enum Filter {
        /** Radio filter b. */
        RADIO,
        /** Radio filter b with more static and dropouts: a damaged channel. */
        DISTORTED,
        /** No radio filter: a briefing page, spoken face to face. */
        DRY,
        /**
         * The public-address filter instead of the radio filter: horn loudspeakers with echoes, for a
         * speaker whose table says {@code filter: pa} (Level 06's perimeter beacon).
         */
        PA;

        public String slug() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    /**
     * A spoken line.
     *
     * @param voice the speaker table's voice slug ({@code dock})
     * @param speaker the speaker's name in the data ({@code Dock One})
     * @param text the line as the subtitle shows it
     * @param spoken the text the voice says: markup and stage directions removed
     * @param settings the Chatterbox settings it is spoken with
     * @param key the hash its file is named by
     * @param source where it comes from, for the renderer's log
     */
    public record VoiceLine(
            String voice,
            String speaker,
            String text,
            String spoken,
            Expression expression,
            boolean shout,
            Filter filter,
            VoiceData.Settings settings,
            String key,
            String source) {
        /** The file under assets/. */
        public String path() {
            return VoiceLines.path(voice, key);
        }
    }

    /** The asset path of a voice file: {@code voice/<voice>/<key>.ogg}. */
    public static String path(String voice, String key) {
        return "voice/" + voice + "/" + key + ".ogg";
    }

    /** A cue's line with {@link #ALLY} as the convoy unit's number word ({@code unit} from 0). */
    public static String allyLine(String line, int unit) {
        if (unit < 0 || !line.contains(ALLY)) {
            return line;
        }
        return line.replace(ALLY, unit < NUMBERS.length ? NUMBERS[unit] : Integer.toString(unit + 1));
    }

    /** A cue's line with {@link #GROUP} as the group's name; unchanged without a group. */
    public static String groupLine(String line, String group) {
        return group.isEmpty() ? line : line.replace(GROUP, group);
    }

    /**
     * The text as the voice says it: square-bracketed stage directions left out, the {@code *}
     * of italics removed and a dash read as a comma; empty when nothing is left to say.
     */
    public static String spoken(String text) {
        String spoken = text.replaceAll("\\[[^]]*]", " ")
                .replace("*", "")
                .replaceAll("\\s*[—–]\\s*", ", ")
                .replaceAll("\\s+", " ")
                .strip();
        return spoken.matches("[\\p{Punct}\\s]*") ? "" : spoken;
    }

    /**
     * Whether {@code text} is only a stage direction in square brackets ({@code [the Choir sings]}):
     * nothing in it is spoken.
     */
    public static boolean stageDirection(String text) {
        return text.contains("[") && spoken(text).isEmpty();
    }

    /**
     * The sound a radio line that is only a stage direction plays in place of a voice: the speaker's
     * {@code stage} sound in the speaker table, {@code voice/<voice>/<file>} (the Choir's sung sting
     * for {@code [the Choir sings]}); empty for a spoken line or a speaker without one.
     */
    public static Optional<String> stageSound(VoiceData voices, String speaker, String text) {
        if (!stageDirection(text)) {
            return Optional.empty();
        }
        return voices.voiceOf(speaker)
                .flatMap(voice -> voices.speakers().get(voice).stage().map(file -> "voice/" + voice + "/" + file));
    }

    /** Every speaker's stage sound under assets/: files no rendered line names, kept by the renderer. */
    public static Set<String> stageSounds(VoiceData voices) {
        Set<String> sounds = new TreeSet<>();
        voices.speakers().forEach((voice, speaker) -> speaker.stage()
                .ifPresent(file -> sounds.add("voice/" + voice + "/" + file)));
        return Set.copyOf(sounds);
    }

    /**
     * The voice a radio line as shown plays, its asset path: the rendered file of the line in
     * {@code index} ({@link #radioIndex}), or for a stage direction the speaker's {@link #stageSound};
     * empty when it has neither (the line shows as text).
     */
    public static Optional<String> radioVoice(
            Map<String, VoiceLine> index, VoiceData voices, String speaker, String text, String expression) {
        VoiceLine line = index.get(indexKey(speaker, text, expression));
        return line != null ? Optional.of(line.path()) : stageSound(voices, speaker, text);
    }

    /** The line {@code speaker} says, if the speaker has a voice and the text anything to say. */
    public static Optional<VoiceLine> line(
            VoiceData voices,
            String speaker,
            String text,
            Expression expression,
            boolean shout,
            Filter filter,
            String source) {
        Optional<String> voice = voices.voiceOf(speaker);
        String spoken = spoken(text);
        if (voice.isEmpty() || spoken.isEmpty()) {
            return Optional.empty();
        }
        VoiceData.Speaker table = voices.speakers().get(voice.get());
        Filter post = filter != Filter.DRY && table.filter().isPresent() ? Filter.PA : filter;
        VoiceData.Settings settings = voices.settings(voice.get(), expression, shout);
        String key = key(String.join(
                "\n",
                VERSION,
                voice.get(),
                table.ref().orElseThrow(),
                format(settings.exaggeration()),
                format(settings.cfgWeight()),
                format(settings.temperature()),
                table.layering().orElse("-"),
                spoken,
                expression.slug(),
                Boolean.toString(shout),
                post.slug()));
        return Optional.of(
                new VoiceLine(voice.get(), speaker, text, spoken, expression, shout, post, settings, key, source));
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.3f", value);
    }

    /** The first 12 hex digits of the SHA-256 of {@code text}. */
    static String key(String text) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash).substring(0, 12);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Every spoken line of the game, each key once, in a stable order. */
    public static List<VoiceLine> all(Content content) {
        Map<String, VoiceLine> lines = new LinkedHashMap<>();
        for (VoiceLine line : every(content)) {
            lines.putIfAbsent(line.key(), line);
        }
        return List.copyOf(lines.values());
    }

    /** Every spoken line, a line shared by several speakers (the four docks) once per speaker. */
    private static List<VoiceLine> every(Content content) {
        Map<String, VoiceLine> lines = new LinkedHashMap<>();
        VoiceData voices = content.voices();
        SpecialsData.Radio call = content.specials().airstrike().radio();
        add(lines, line(voices, call.speaker(), call.line(), Expression.NEUTRAL, false, Filter.RADIO, "specials"));
        LevelData.RadioLine lowArmour = content.armour().radio();
        add(
                lines,
                line(
                        voices,
                        lowArmour.speaker(),
                        lowArmour.line(),
                        lowArmour.expression().orElse(Expression.NEUTRAL),
                        false,
                        lowArmour.distorted().orElse(false) ? Filter.DISTORTED : Filter.RADIO,
                        "armour"));
        for (Map.Entry<String, ActData> act : new TreeMap<>(content.acts()).entrySet()) {
            briefing(lines, voices, act.getValue().briefing(), act.getKey() + " briefing");
            act.getValue().outro().ifPresent(outro -> briefing(lines, voices, outro.pages(), act.getKey() + " outro"));
        }
        for (Map.Entry<String, LevelData> level : new TreeMap<>(content.levels()).entrySet()) {
            LevelData data = level.getValue();
            String where = level.getKey();
            briefing(lines, voices, data.briefing().pages(), where + " briefing");
            for (VoiceLine line : radio(voices, data, where)) {
                add(lines, Optional.of(line));
            }
        }
        return List.copyOf(lines.values());
    }

    /** The radio lines of one level (its cues with their variants and convoy units, and its secrets). */
    public static List<VoiceLine> radio(VoiceData voices, LevelData level, String where) {
        Map<String, VoiceLine> lines = new LinkedHashMap<>();
        int units = level.objectives().escort().map(escort -> escort.y().size()).orElse(0);
        for (LevelData.RadioCue cue : level.radio()) {
            Expression expression = cue.expression().orElse(Expression.NEUTRAL);
            boolean shout = cue.shout().orElse(false);
            Filter filter = cue.distorted().orElse(false) ? Filter.DISTORTED : Filter.RADIO;
            List<String> texts = new ArrayList<>();
            texts.add(cue.line());
            cue.easy().ifPresent(change -> texts.add(change.line()));
            cue.hard().ifPresent(change -> texts.add(change.line()));
            String source = where + " radio "
                    + cue.t()
                            .map(t -> "t=" + t)
                            .orElseGet(() -> cue.event().orElseThrow().toString());
            for (String text : texts) {
                if (text.contains(GROUP)) {
                    for (String group : level.objectives().groups()) {
                        add(
                                lines,
                                line(voices, cue.speaker(), groupLine(text, group), expression, shout, filter, source));
                    }
                } else if (text.contains(ALLY)) {
                    for (int unit = 0; unit < Math.max(1, units); unit++) {
                        add(
                                lines,
                                line(voices, cue.speaker(), allyLine(text, unit), expression, shout, filter, source));
                    }
                } else {
                    add(lines, line(voices, cue.speaker(), text, expression, shout, filter, source));
                }
            }
        }
        for (LevelData.Secret secret : level.secrets()) {
            LevelData.RadioLine line = secret.radio();
            Filter filter = line.distorted().orElse(false) ? Filter.DISTORTED : Filter.RADIO;
            add(
                    lines,
                    line(
                            voices,
                            line.speaker(),
                            line.line(),
                            line.expression().orElse(Expression.NEUTRAL),
                            false,
                            filter,
                            where + " secret " + secret.name()));
        }
        return List.copyOf(lines.values());
    }

    private static void briefing(
            Map<String, VoiceLine> lines, VoiceData voices, List<BriefingPage> pages, String source) {
        for (int i = 0; i < pages.size(); i++) {
            BriefingPage page = pages.get(i);
            add(lines, briefingPage(voices, page, source + " page " + (i + 1)));
        }
    }

    /** A briefing page's line, spoken without the radio filter. */
    public static Optional<VoiceLine> briefingPage(VoiceData voices, BriefingPage page, String source) {
        return line(voices, page.speaker(), page.line(), page.portrait(), false, Filter.DRY, source);
    }

    /** Adds a line once per speaker and key. */
    private static void add(Map<String, VoiceLine> lines, Optional<VoiceLine> line) {
        line.ifPresent(found -> lines.putIfAbsent(found.speaker() + "\n" + found.key(), found));
    }

    /**
     * The radio lines by speaker, text as shown and expression, for the game to find a cue's voice:
     * the shout flag and the filter come with the line.
     */
    public static Map<String, VoiceLine> radioIndex(Content content) {
        Map<String, VoiceLine> index = new TreeMap<>();
        for (VoiceLine line : every(content)) {
            if (line.filter() != Filter.DRY) {
                index.putIfAbsent(
                        indexKey(line.speaker(), line.text(), line.expression().slug()), line);
            }
        }
        return Map.copyOf(index);
    }

    /** The {@link #radioIndex} key of a line. */
    public static String indexKey(String speaker, String text, String expression) {
        return speaker + "\n" + text + "\n" + expression;
    }
}
