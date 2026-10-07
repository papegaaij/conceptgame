package vanguard.game.level;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import vanguard.content.Content;
import vanguard.content.ContentLoader;
import vanguard.content.Difficulty;
import vanguard.content.Expression;
import vanguard.content.LevelData;
import vanguard.content.SimSpecs;
import vanguard.content.voice.VoiceLines;
import vanguard.game.audio.VorbisFile;
import vanguard.sim.LevelScript;
import vanguard.sim.LevelScript.CueTrigger;
import vanguard.sim.SimStep;

/**
 * The timed radio lines of Levels 01–09 play when the level scripts mean them to: the real queue,
 * stepped at the simulation's rate at the default text speed, with the event lines a player can
 * set off in between (an escaped Spore Bomber before the Leviathan's first pass, the lifeboat
 * secret, a convoy's first hit and loss, the Brood Carrier's phases, Rook's first kill, …), starts none of them more
 * than a second after its time. Event lines wait for a gap and may be dropped as stale; they never
 * push a timed line back. Every run is played with each fit a cue can require (a special, a homing
 * weapon, both, neither): a cue the fit does not allow is never queued, as in the game. Okafor's
 * low-armour line (design/player/armor), which the game queues as urgent, plays at once wherever it
 * falls and every timed line still plays after it. A line with Rook's side ({@code {side}}, Level
 * 08) is played with each side's text. M5 part C: a level with hold zones (Level 09) is flown through
 * them: the level clock (script time) slows to the hold's speed over its ramp while the queue runs
 * on real time, each hold lasting a typical 6 s and the medium window of about 10.6 s, the collapse
 * starting as the last cluster dies; a line is late by the real seconds after the level clock
 * reached its time, and a line that requires the escort plays only with him flying (both fits).
 */
class RadioTimelineTest {
    private static final Content CONTENT = ContentLoader.fromClasspath();
    private static final double MAX_LATE_SECONDS = 1;
    private static final Map<String, VoiceLines.VoiceLine> VOICES = VoiceLines.radioIndex(CONTENT);
    private static final java.nio.file.Path ASSETS =
            java.nio.file.Path.of(System.getProperty("vanguard.assetsDir", "../assets"));

    private static final String LEVEL_01 = "act-1-first-contact/level-01-break-at-dawn";
    private static final String LEVEL_02 = "act-1-first-contact/level-02-shipyard-burning";
    private static final String LEVEL_03 = "act-1-first-contact/level-03-spore-drift";
    private static final String LEVEL_04 = "act-1-first-contact/level-04-tranquility-run";
    private static final String LEVEL_05 = "act-1-first-contact/level-05-crater-nest";
    private static final String LEVEL_06 = "act-1-first-contact/level-06-farside";
    private static final String LEVEL_07 = "act-1-first-contact/level-07-brood-carrier";
    private static final String LEVEL_08 = "act-2-homefront/level-08-neon-skyline";
    private static final String LEVEL_09 = "act-2-homefront/level-09-arcology-fall";

    /** Rook's sides: a {@code {side}} line is played with each one's text. */
    private static final List<String> SIDES = VoiceLines.SIDES;

    /**
     * Levels whose lines are not rendered yet (none: Level 09's were rendered in M5 part C's voice
     * step, Level 08's in M5 part B's step B9, Level 07's in M4 part G): their voiced runs play the
     * lines as text and list the late ones, without asking for the files. An uncast speaker's line
     * (marked {@code uncast} in the speaker table: Level 09's Kilo Lead until round 31 casts him)
     * plays as text in any level.
     */
    private static final Set<String> UNVOICED = Set.of();

    private static final int ESCORT = LevelScript.RadioCue.FITTED_ESCORT;

    /** The fits a cue can require: neither, a special, a homing weapon, both; each with and without the escort flying. */
    private static final int[] FITS = {
        0,
        LevelScript.RadioCue.FITTED_SPECIAL,
        LevelScript.RadioCue.FITTED_HOMING,
        LevelScript.RadioCue.FITTED_SPECIAL | LevelScript.RadioCue.FITTED_HOMING,
        ESCORT,
        ESCORT | LevelScript.RadioCue.FITTED_SPECIAL,
        ESCORT | LevelScript.RadioCue.FITTED_HOMING,
        ESCORT | LevelScript.RadioCue.FITTED_SPECIAL | LevelScript.RadioCue.FITTED_HOMING
    };

    /**
     * How long each hold zone lasts in real seconds in a level with holds (Level 09): a typical hold
     * (the README's 6 s) and the whole medium window (≈ 10.6 s at 30 px/s); NaN for a level without.
     */
    private static final double[] HOLD_SECONDS = {6, 10.6};

    /**
     * Timed lines written to follow the line before them rather than to start at their time, by
     * level and t: Level 01's Varga answers the Choir's song ("That isn't noise…"), so she starts
     * once the Choir's line has closed.
     */
    private static final Map<String, Set<Double>> FOLLOWING = Map.of(LEVEL_01, Set.of(161.0));

    /** Okafor's low-armour line, queued urgent by the game at the first armour hit to 15 % or below. */
    private static final LevelData.RadioLine LOW_ARMOUR = CONTENT.armour().radio();

    private static final String LOW_ARMOUR_EXPRESSION =
            LOW_ARMOUR.expression().orElse(Expression.NEUTRAL).slug();

    /**
     * An event a player sets off at script time {@code t}, or (M5 part C) {@code into} real seconds
     * after hold zone {@code hold} started (a node killed in its hold).
     */
    private record Event(double t, CueTrigger trigger, String subject, int hold, double into) {
        Event(double t, CueTrigger trigger, String subject) {
            this(t, trigger, subject, -1, 0);
        }

        static Event inHold(int hold, double into, CueTrigger trigger, String subject) {
            return new Event(Double.NaN, trigger, subject, hold, into);
        }
    }

    /**
     * A cue and when its message opened, if it did (real seconds), and when it was due (real seconds:
     * when the level clock reached its time, for a timed cue).
     */
    private record Played(LevelScript.RadioCue cue, Optional<Double> opened, double due) {
        /** How late a timed line opened after the level clock reached its time, real seconds. */
        double late() {
            return opened.orElseThrow() - due;
        }
    }

    /** Per level, the runs to check: each a list of events on top of the timed lines and the level end. */
    private static final Map<String, List<List<Event>>> RUNS = Map.of(
            LEVEL_09,
            List.of(
                    List.of(),
                    // the first node dies early or late in the plaza's hold; the first pounce on the boulevard
                    List.of(
                            Event.inHold(0, 2, CueTrigger.FIRST_KILL, "hive-node"),
                            new Event(60, CueTrigger.FIRST_POUNCE, "")),
                    List.of(
                            Event.inHold(0, 5, CueTrigger.FIRST_KILL, "hive-node"),
                            new Event(75, CueTrigger.FIRST_POUNCE, ""),
                            new Event(88, CueTrigger.SECRET, "cocoon cache")),
                    // the pounce just after Varga's warning, the cocoon as it passes
                    List.of(
                            new Event(58.5, CueTrigger.FIRST_POUNCE, ""),
                            new Event(86, CueTrigger.SECRET, "cocoon cache"))),
            LEVEL_08,
            List.of(
                    List.of(),
                    // Rook's first kill as the autopilot flies it with him (the first Skitters at t=12),
                    // just after his side line, and late, before the t=43 line
                    List.of(new Event(17.3, CueTrigger.ESCORT_FIRST_KILL, "")),
                    List.of(new Event(12.5, CueTrigger.ESCORT_FIRST_KILL, "")),
                    List.of(new Event(36, CueTrigger.ESCORT_FIRST_KILL, "")),
                    // the billboard toppled as it enters, or late as it passes
                    List.of(new Event(101, CueTrigger.SECRET, "billboard cache")),
                    List.of(
                            new Event(17.3, CueTrigger.ESCORT_FIRST_KILL, ""),
                            new Event(105, CueTrigger.SECRET, "billboard cache"))),
            LEVEL_01,
            List.of(
                    List.of(),
                    List.of(new Event(9, CueTrigger.FIRST_KILL, "skitter")),
                    List.of(new Event(30, CueTrigger.FIRST_KILL, "skitter"))),
            LEVEL_02,
            List.of(
                    List.of(),
                    List.of(new Event(36, CueTrigger.GROUP_CLEARED, "Dock One")),
                    List.of(
                            new Event(40, CueTrigger.GROUP_LOST, "Dock One"),
                            new Event(40, CueTrigger.FIRST_GROUP_LOST, ""))),
            LEVEL_03,
            List.of(
                    List.of(),
                    // the run in the capture: a bomber of the t=42 line slips past just before the first pass
                    List.of(new Event(55, CueTrigger.ENEMY_ESCAPED, "spore-bomber")),
                    List.of(new Event(20, CueTrigger.ENEMY_ESCAPED, "spore-bomber")),
                    List.of(new Event(45, CueTrigger.SECRET, "lifeboat rack")),
                    List.of(
                            new Event(45, CueTrigger.SECRET, "lifeboat rack"),
                            new Event(52, CueTrigger.ENEMY_ESCAPED, "spore-bomber")),
                    List.of(new Event(150, CueTrigger.FIRST_KILL, "leviathan")),
                    // it leaves alive: up through the top edge 2.7 s after its rise at t=158
                    List.of(new Event(160.7, CueTrigger.ENEMY_ESCAPED, "leviathan"))),
            LEVEL_04,
            List.of(
                    List.of(),
                    // the first turret nest hits a crawler, a walker of the pincer claws one to death
                    List.of(new Event(41, CueTrigger.FIRST_ALLY_HIT, "")),
                    List.of(
                            new Event(41, CueTrigger.FIRST_ALLY_HIT, ""),
                            new Event(91, CueTrigger.FIRST_ALLY_LOST, "")),
                    // a crawler lost to the bridge turrets, just before Varga's walker line … and after it
                    List.of(new Event(72, CueTrigger.FIRST_ALLY_LOST, "")),
                    List.of(new Event(126, CueTrigger.SECRET, "prospector's cache")),
                    List.of(new Event(23, CueTrigger.FIRST_ALLY_HIT, ""))),
            LEVEL_05,
            List.of(
                    List.of(),
                    // the batteries cleared as the autopilot clears them, the stuck sled opened between two sleds
                    List.of(
                            new Event(57.5, CueTrigger.GROUP_CLEARED, "Battery A"),
                            new Event(90.5, CueTrigger.GROUP_CLEARED, "Battery B"),
                            new Event(110.5, CueTrigger.GROUP_CLEARED, "Battery C"),
                            new Event(137.5, CueTrigger.GROUP_CLEARED, "Battery D")),
                    List.of(new Event(33, CueTrigger.SECRET, "stuck sled")),
                    // the frigate's phases in a fast fight (the arena clock runs on), and its death
                    List.of(
                            new Event(137.5, CueTrigger.GROUP_CLEARED, "Battery D"),
                            new Event(170, CueTrigger.BOSS_PHASE, "Core"),
                            new Event(185, CueTrigger.BOSS_DESTROYED, "gorgon-frigate"))),
            LEVEL_06,
            List.of(
                    List.of(),
                    // the survey cache lit by the t=112 flare (or found by headlight on the way in)
                    List.of(new Event(112.5, CueTrigger.SECRET, "survey cache")),
                    List.of(new Event(111, CueTrigger.SECRET, "survey cache")),
                    // the data core collected as the terminal passes, before and between the last lines
                    List.of(new Event(182, CueTrigger.SECRET, "settlement log")),
                    List.of(new Event(178, CueTrigger.SECRET, "settlement log"))),
            LEVEL_07,
            List.of(
                    List.of(),
                    // the lifeboat's cable cut as the tow comes in, or late as it drifts out
                    List.of(new Event(25, CueTrigger.SECRET, "lifeboat tow")),
                    List.of(new Event(33, CueTrigger.SECRET, "lifeboat tow")),
                    // the carrier as the autopilot fights it at medium (the broadside 25 s after the
                    // bar, the core at about 69 s, the kill at about 90 s; the clock then jumps to the
                    // arena's end), and a fast fight
                    List.of(
                            new Event(125, CueTrigger.BOSS_PHASE, "Broadside"),
                            new Event(169, CueTrigger.BOSS_PHASE, "Core"),
                            new Event(190, CueTrigger.BOSS_DESTROYED, "brood-carrier")),
                    List.of(
                            new Event(33, CueTrigger.SECRET, "lifeboat tow"),
                            new Event(125, CueTrigger.BOSS_PHASE, "Broadside"),
                            new Event(150, CueTrigger.BOSS_PHASE, "Core"),
                            new Event(165, CueTrigger.BOSS_DESTROYED, "brood-carrier")),
                    // the broadside times out: 70 s after its 4 s move
                    List.of(
                            new Event(125, CueTrigger.BOSS_PHASE, "Broadside"),
                            new Event(199, CueTrigger.BOSS_TIMEOUT, "Core"),
                            new Event(199, CueTrigger.BOSS_PHASE, "Core"),
                            new Event(230, CueTrigger.BOSS_DESTROYED, "brood-carrier"))));

    /** The real seconds each hold lasts to fly a level with: {@link #HOLD_SECONDS}, or NaN for a level without holds. */
    private static double[] holdSeconds(LevelScript script) {
        return script.holds().isEmpty() ? new double[] {Double.NaN} : HOLD_SECONDS;
    }

    @Test
    void theTimedLinesOfLevels01To09StartAtMostASecondLate() {
        RUNS.forEach((level, runs) -> {
            for (Difficulty difficulty : Difficulty.values()) {
                LevelScript script = SimSpecs.level(CONTENT, level, difficulty);
                for (List<Event> events : runs) {
                    for (int fitted : FITS) {
                        for (String side : SIDES) {
                            for (double hold : holdSeconds(script)) {
                                String where = level + " on " + difficulty + " with " + events + " (fit " + fitted
                                        + ", " + side + ", holds " + hold + " s): ";
                                List<Played> played =
                                        play(script, events, fitted, false, Double.NaN, new double[1], side, hold);
                                for (Played line : played) {
                                    LevelScript.RadioCue cue = line.cue();
                                    if (cue.trigger() != CueTrigger.TIME || !cue.allowedWith(fitted)) {
                                        continue;
                                    }
                                    assertTrue(
                                            line.opened().isPresent(),
                                            where + "the timed line at t=" + cue.t() + " plays");
                                    if (FOLLOWING.getOrDefault(level, Set.of()).contains(cue.t())) {
                                        continue;
                                    }
                                    double late = line.late();
                                    assertTrue(
                                            late <= MAX_LATE_SECONDS,
                                            String.format(
                                                    "%s%s's line at t=%s starts %.1f s late: %s",
                                                    where, cue.speaker(), cue.t(), late, cue.line()));
                                }
                            }
                        }
                    }
                }
            }
        });
    }

    /**
     * The same runs with the voices (design/audio/voice): a line holds the radio for the longer of
     * its text and its voice. Every timed line still plays; the ones that start more than a second
     * late are printed for the user to decide on a retiming (the data is not retimed here).
     */
    @Test
    void theVoicedTimelinePlaysEveryTimedLineAndListsTheLateOnes() {
        Map<String, Double> worst = new java.util.TreeMap<>();
        RUNS.forEach((level, runs) -> {
            for (Difficulty difficulty : Difficulty.values()) {
                LevelScript script = SimSpecs.level(CONTENT, level, difficulty);
                for (List<Event> events : runs) {
                    for (int fitted : FITS) {
                        for (String side : SIDES) {
                            for (double hold : holdSeconds(script)) {
                                for (Played line :
                                        play(script, events, fitted, true, Double.NaN, new double[1], side, hold)) {
                                    LevelScript.RadioCue cue = line.cue();
                                    if (cue.trigger() != CueTrigger.TIME || !cue.allowedWith(fitted)) {
                                        continue;
                                    }
                                    assertTrue(
                                            line.opened().isPresent(),
                                            level + ": the timed line at t=" + cue.t() + " plays");
                                    assertTrue(
                                            VoiceLines.spoken(cue.line()).isEmpty()
                                                    || voiceSeconds(cue, side) > 0
                                                    || UNVOICED.contains(level)
                                                    || CONTENT.voices().uncast(cue.speaker()),
                                            level + ": the timed line at t=" + cue.t() + " has its voice");
                                    if (FOLLOWING.getOrDefault(level, Set.of()).contains(cue.t())) {
                                        continue;
                                    }
                                    double late = line.late();
                                    if (late > MAX_LATE_SECONDS) {
                                        worst.merge(
                                                String.format(
                                                        "%s t=%s %s (%s)", level, cue.t(), cue.speaker(), difficulty),
                                                late,
                                                Math::max);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        });
        worst.forEach((line, late) -> System.out.printf("voiced line late by %.1f s: %s%n", late, line));
    }

    /**
     * The low-armour line, every 10 s of each level with each fit and its voice: it opens in the step
     * it is queued (urgent: it interrupts), and every timed line still plays; the timed lines it
     * pushes back by more than a second are printed (an urgent line may delay them, by design).
     */
    @Test
    void theLowArmourLineInterruptsAtOnceAndEveryTimedLineStillPlays() {
        assertTrue(
                voiceSeconds(LOW_ARMOUR.speaker(), LOW_ARMOUR.line(), LOW_ARMOUR_EXPRESSION) > 0,
                "the low-armour line has its voice");
        Map<String, Double> worst = new java.util.TreeMap<>();
        RUNS.keySet().forEach(level -> {
            LevelScript script = SimSpecs.level(CONTENT, level, Difficulty.MEDIUM);
            for (double at = 5; at < script.seconds(); at += 10) {
                for (int fitted : FITS) {
                    double[] opened = {Double.NaN};
                    List<Played> played = play(script, List.of(), fitted, true, at, opened);
                    String where = level + " with the line at t=" + at + " (fit " + fitted + "): ";
                    assertTrue(
                            opened[0] - at <= 2 * SimStep.SECONDS,
                            where + "the line opens at once, not at " + opened[0]);
                    for (Played line : played) {
                        LevelScript.RadioCue cue = line.cue();
                        if (cue.trigger() != CueTrigger.TIME || !cue.allowedWith(fitted)) {
                            continue;
                        }
                        assertTrue(line.opened().isPresent(), where + "the timed line at t=" + cue.t() + " plays");
                        double late = line.late();
                        if (late > MAX_LATE_SECONDS
                                && !FOLLOWING.getOrDefault(level, Set.of()).contains(cue.t())) {
                            worst.merge(level + " t=" + cue.t() + " " + cue.speaker(), late, Math::max);
                        }
                    }
                }
            }
        });
        worst.forEach(
                (line, late) -> System.out.printf("pushed back by the low-armour line: %.1f s: %s%n", late, line));
    }

    /** The voice length of a radio line from its rendered file with Rook on {@code side}, 0 without one. */
    private static float voiceSeconds(LevelScript.RadioCue cue, String side) {
        return voiceSeconds(cue.speaker(), text(cue, side), cue.expression());
    }

    /** A cue's line as shown: the third convoy unit's number word, Rook's {@code side}. */
    private static String text(LevelScript.RadioCue cue, String side) {
        return VoiceLines.sideLine(VoiceLines.allyLine(cue.line(), 2), side);
    }

    /**
     * The voice length of {@code speaker}'s {@code text} in {@code expression} from its rendered file
     * (a stage direction's: the speaker's stage sound, the Choir's sung sting), 0 without one.
     */
    private static float voiceSeconds(String speaker, String text, String expression) {
        Optional<String> path = VoiceLines.radioVoice(VOICES, CONTENT.voices(), speaker, text, expression);
        if (path.isEmpty()) {
            return 0;
        }
        try (VorbisFile file = new VorbisFile(java.nio.file.Files.readAllBytes(ASSETS.resolve(path.get())))) {
            return (float) file.frameCount() / file.sampleRate();
        } catch (java.io.IOException e) {
            return 0;
        }
    }

    /**
     * The holds are flown where Level 09's nodes reach their depth: three, in order, at a fifth of the
     * scroll on medium (30 of 150 px/s), the last one running on through the collapse.
     */
    @Test
    void level09sHoldsAreFlownWhereItsNodesReachTheirDepth() {
        LevelScript script = SimSpecs.level(CONTENT, LEVEL_09, Difficulty.MEDIUM);
        List<HoldRun> holds = holdRuns(script, 6);
        assertTrue(holds.size() == 3, "three holds: " + holds);
        for (int h = 0; h < holds.size(); h++) {
            HoldRun hold = holds.get(h);
            assertTrue(Double.isFinite(hold.start()) && hold.start() < script.seconds(), "hold " + h + ": " + hold);
            assertTrue(Math.abs(hold.ratio() - 0.2) < 1e-9, "hold " + h + " at a fifth: " + hold.ratio());
            assertTrue(h == 0 || hold.start() > holds.get(h - 1).start(), "in order");
        }
        assertTrue(
                holds.getLast().collapse() && holds.getLast().seconds() > 6, "the last runs on through the collapse");
        // A line inside a hold is due in real time: the Choir's 152.5 (between two holds) still plays at most 1 s late.
        for (Played line : play(script, List.of(), ESCORT, false, Double.NaN, new double[1], SIDES.getFirst(), 10.6)) {
            if (line.cue().trigger() == CueTrigger.TIME) {
                assertTrue(line.opened().isPresent() && line.late() <= MAX_LATE_SECONDS, line.toString());
            }
            if (line.cue().trigger() == CueTrigger.HOLD_START || line.cue().trigger() == CueTrigger.COLLAPSE) {
                assertTrue(line.opened().isPresent(), "the " + line.cue().trigger() + " line plays");
            }
        }
    }

    @Test
    void theLeviathansLinesFindTheirGapsAndALateReactionIsDropped() {
        LevelScript script = SimSpecs.level(CONTENT, LEVEL_03, Difficulty.MEDIUM);

        assertTrue(opened(play(script, List.of(new Event(160.7, CueTrigger.ENEMY_ESCAPED, "leviathan"))), "leviathan"));
        assertTrue(opened(play(script, List.of(new Event(150, CueTrigger.FIRST_KILL, "leviathan"))), "leviathan"));
        // killed while Okafor's long line plays: Ring Control would only find a gap after it
        assertTrue(!opened(play(script, List.of(new Event(169, CueTrigger.FIRST_KILL, "leviathan"))), "leviathan"));
    }

    /**
     * Whether one of {@code events} has set off the event cue by {@code scriptTick} (the level
     * clock), an event in a hold by {@code realTick} with hold {@code k} started at {@code holdStarts[k]}.
     */
    private static boolean setOff(
            List<Event> events, LevelScript.RadioCue cue, int scriptTick, int realTick, int[] holdStarts) {
        return events.stream()
                .anyMatch(event -> event.trigger() == cue.trigger()
                        && event.subject().equals(cue.subject())
                        && (event.hold() < 0
                                ? SimStep.ticks(event.t()) <= scriptTick
                                : holdStarts[event.hold()] >= 0
                                        && realTick - holdStarts[event.hold()] >= SimStep.ticks(event.into())));
    }

    /**
     * M5 part C: a hold zone as the test flies it: from {@code start} (script time, when the first
     * unit of its groups reaches its depth below the top edge, scrolling at the section's speed) the
     * level clock eases (smoothstep on the speed, as the simulation) over {@code ramp} to {@code ratio}
     * of real time, for {@code seconds} real seconds (its groups' kill), and back; a hold over the
     * collapse's groups runs on through the collapse's warning, drop and blast until its dust has
     * settled (user decision, 2026-10-07), the collapse cue starting as its groups die.
     */
    private record HoldRun(double start, double ratio, double ramp, double seconds, boolean collapse) {}

    private static List<HoldRun> holdRuns(LevelScript script, double seconds) {
        List<HoldRun> runs = new ArrayList<>();
        if (Double.isNaN(seconds)) {
            return runs;
        }
        for (LevelScript.Hold hold : script.holds()) {
            double start = Double.POSITIVE_INFINITY;
            for (LevelScript.GroundUnit unit : script.groundUnits()) {
                if (unit.group() >= 0 && hold.waitsFor(unit.group())) {
                    double speed = sectionSpeed(script, unit.t());
                    start = Math.min(start, unit.t() + (unit.enemy().hitbox().height() / 2 + hold.depth()) / speed);
                }
            }
            boolean collapse =
                    script.collapse().map(c -> c.groups().equals(hold.groups())).orElse(false);
            double extra = collapse
                    ? script.collapse().get().warningSeconds()
                            + script.collapse().get().dropSeconds()
                            + script.collapse().get().settleSeconds()
                    : 0;
            runs.add(new HoldRun(
                    start, hold.speed() / sectionSpeed(script, start), hold.rampSeconds(), seconds + extra, collapse));
        }
        return runs;
    }

    private static double sectionSpeed(LevelScript script, double t) {
        for (LevelScript.Section section : script.sections()) {
            if (t < section.end()) {
                return section.speed();
            }
        }
        return script.sections().getLast().speed();
    }

    private static boolean opened(List<Played> played, String subject) {
        return played.stream()
                .anyMatch(line ->
                        line.cue().subject().equals(subject) && line.opened().isPresent());
    }

    /**
     * Steps the queue over the level: the timed cues at their ticks and {@code events} at theirs,
     * each queued by its priority, the level-end cues at the end; the radio runs on after the end
     * until it is idle.
     */
    private static List<Played> play(LevelScript script, List<Event> events) {
        return play(script, events, ESCORT, false);
    }

    /** As the game queues them with {@code fitted} on the ship: a cue it does not allow never starts. */
    private static List<Played> play(LevelScript script, List<Event> events, int fitted, boolean voiced) {
        return play(script, events, fitted, voiced, Double.NaN, new double[1], SIDES.getFirst(), 6);
    }

    /**
     * As above, with the low-armour line queued urgent at {@code lowArmourAt} (NaN for never);
     * {@code lowArmourOpened[0]} gets the time its message opened.
     */
    private static List<Played> play(
            LevelScript script,
            List<Event> events,
            int fitted,
            boolean voiced,
            double lowArmourAt,
            double[] lowArmourOpened) {
        return play(script, events, fitted, voiced, lowArmourAt, lowArmourOpened, SIDES.getFirst(), 6);
    }

    /**
     * As above with Rook on {@code side} and each hold zone lasting {@code holdSeconds} real seconds
     * (NaN or a level without holds: the level clock runs with real time). The loop steps real time;
     * the level clock follows the holds.
     */
    private static List<Played> play(
            LevelScript script,
            List<Event> events,
            int fitted,
            boolean voiced,
            double lowArmourAt,
            double[] lowArmourOpened,
            String side,
            double holdSeconds) {
        int lowArmourTick = Double.isNaN(lowArmourAt) ? -1 : SimStep.ticks(lowArmourAt);
        List<String> lowArmourLines = RadioQueue.wrap(LOW_ARMOUR.line());
        RadioSchedule schedule = new RadioSchedule(script, fitted & ~ESCORT);
        boolean escort = (fitted & ESCORT) != 0;
        RadioQueue radio = new RadioQueue();
        List<LevelScript.RadioCue> cues = script.radio();
        boolean[] fired = new boolean[cues.size()];
        double[] dueAt = new double[cues.size()];
        List<Integer> queued = new ArrayList<>();
        Double[] opened = new Double[cues.size()];
        int end = SimStep.ticks(script.seconds());
        List<HoldRun> holds = holdRuns(script, holdSeconds);
        int[] holdStarts = new int[holds.size()];
        java.util.Arrays.fill(holdStarts, -1);
        // The level clock in fixed point, as the simulation's (1/65 536 of a step).
        long scriptFixed = 0;
        int scriptTick = 0;
        int afterEnd = -1;
        for (int tick = 1; afterEnd < 0 || tick <= afterEnd; tick++) {
            double seconds = (double) tick / SimStep.PER_SECOND;
            double rate = 1;
            boolean collapsing = false;
            for (int h = 0; h < holds.size(); h++) {
                HoldRun hold = holds.get(h);
                if (holdStarts[h] < 0 && scriptTick >= SimStep.ticks(hold.start())) {
                    holdStarts[h] = tick;
                }
                if (holdStarts[h] >= 0) {
                    double in = (tick - holdStarts[h]) * SimStep.SECONDS;
                    double out = in - hold.seconds();
                    double s = out < 0 ? Math.min(1, in / hold.ramp()) : Math.max(0, 1 - out / hold.ramp());
                    rate = Math.min(rate, 1 + (hold.ratio() - 1) * s * s * (3 - 2 * s));
                    collapsing |= hold.collapse() && in >= holdSeconds;
                }
            }
            scriptFixed += Math.max(1, Math.round(rate * 65536));
            scriptTick = (int) (scriptFixed >> 16);
            if (scriptTick >= end && afterEnd < 0) {
                afterEnd = tick + SimStep.ticks(60);
            }
            double scriptSeconds = (double) scriptFixed / 65536 * SimStep.SECONDS;
            for (int i = 0; i < cues.size(); i++) {
                LevelScript.RadioCue cue = cues.get(i);
                boolean due =
                        switch (cue.trigger()) {
                            case TIME -> scriptTick <= end && SimStep.ticks(cue.t()) <= scriptTick;
                            case LEVEL_END -> scriptTick >= end;
                            case SECONDARY_OBJECTIVE -> false;
                            case HOLD_START -> holdStarts.length > 0 && holdStarts[0] >= 0;
                            case COLLAPSE -> collapsing;
                            default -> setOff(events, cue, scriptTick, tick, holdStarts);
                        };
                if (due && !fired[i] && cue.allowedWith(fitted)) {
                    fired[i] = true;
                    dueAt[i] = cue.trigger() == CueTrigger.TIME && holds.isEmpty() ? cue.t() : seconds;
                    queued.add(i);
                    radio.add(
                            cue.speaker(),
                            cue.portrait(),
                            cue.expression(),
                            text(cue, side),
                            cue.distorted(),
                            schedule.priority(i),
                            Optional.empty(),
                            voiced ? voiceSeconds(cue, side) : 0);
                }
            }
            if (tick == lowArmourTick) {
                radio.add(
                        LOW_ARMOUR.speaker(),
                        LOW_ARMOUR.speaker(),
                        LOW_ARMOUR_EXPRESSION,
                        LOW_ARMOUR.line(),
                        LOW_ARMOUR.distorted().orElse(false),
                        RadioQueue.Priority.URGENT,
                        Optional.empty(),
                        voiced ? voiceSeconds(LOW_ARMOUR.speaker(), LOW_ARMOUR.line(), LOW_ARMOUR_EXPRESSION) : 0);
            }
            float untilTimed =
                    scriptTick < end ? schedule.untilTimed(scriptSeconds, rate, escort) : Float.POSITIVE_INFINITY;
            if (radio.update((float) SimStep.SECONDS, untilTimed) == RadioQueue.Change.OPENED) {
                RadioQueue.Message message = radio.current().orElseThrow();
                if (Double.isNaN(lowArmourOpened[0])
                        && message.speaker().equals(LOW_ARMOUR.speaker())
                        && message.lines().equals(lowArmourLines)) {
                    lowArmourOpened[0] = seconds;
                }
                for (int i : queued) {
                    if (opened[i] == null
                            && cues.get(i).speaker().equals(message.speaker())
                            && RadioQueue.wrap(text(cues.get(i), side)).equals(message.lines())) {
                        opened[i] = seconds;
                        break;
                    }
                }
            }
        }
        List<Played> played = new ArrayList<>();
        for (int i = 0; i < cues.size(); i++) {
            played.add(new Played(cues.get(i), Optional.ofNullable(opened[i]), dueAt[i]));
        }
        return played;
    }
}
