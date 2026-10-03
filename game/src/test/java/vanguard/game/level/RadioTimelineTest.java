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
import vanguard.content.SimSpecs;
import vanguard.content.voice.VoiceLines;
import vanguard.game.audio.VorbisFile;
import vanguard.sim.LevelScript;
import vanguard.sim.LevelScript.CueTrigger;
import vanguard.sim.SimStep;

/**
 * The timed radio lines of Levels 01–04 play when the level scripts mean them to: the real queue,
 * stepped at the simulation's rate at the default text speed, with the event lines a player can
 * set off in between (an escaped Spore Bomber before the Leviathan's first pass, the lifeboat
 * secret, a convoy's first hit and loss, …), starts none of them more than a second after its time. Event lines wait for a gap
 * and may be dropped as stale; they never push a timed line back.
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

    /**
     * Timed lines written to follow the line before them rather than to start at their time, by
     * level and t: Level 01's Varga answers the Choir's song ("That isn't noise…"), so she starts
     * once the Choir's line has closed.
     */
    private static final Map<String, Set<Double>> FOLLOWING = Map.of(LEVEL_01, Set.of(161.0));

    /** An event a player sets off at {@code t}. */
    private record Event(double t, CueTrigger trigger, String subject) {}

    /** A cue and when its message opened, if it did. */
    private record Played(LevelScript.RadioCue cue, Optional<Double> opened) {}

    /** Per level, the runs to check: each a list of events on top of the timed lines and the level end. */
    private static final Map<String, List<List<Event>>> RUNS = Map.of(
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
                    List.of(new Event(23, CueTrigger.FIRST_ALLY_HIT, ""))));

    @Test
    void theTimedLinesOfLevels01To04StartAtMostASecondLate() {
        RUNS.forEach((level, runs) -> {
            for (Difficulty difficulty : Difficulty.values()) {
                LevelScript script = SimSpecs.level(CONTENT, level, difficulty);
                for (List<Event> events : runs) {
                    String where = level + " on " + difficulty + " with " + events + ": ";
                    List<Played> played = play(script, events);
                    for (Played line : played) {
                        LevelScript.RadioCue cue = line.cue();
                        if (cue.trigger() != CueTrigger.TIME) {
                            continue;
                        }
                        assertTrue(line.opened().isPresent(), where + "the timed line at t=" + cue.t() + " plays");
                        if (FOLLOWING.getOrDefault(level, Set.of()).contains(cue.t())) {
                            continue;
                        }
                        double late = line.opened().get() - cue.t();
                        assertTrue(
                                late <= MAX_LATE_SECONDS,
                                String.format(
                                        "%s%s's line at t=%s starts %.1f s late: %s",
                                        where, cue.speaker(), cue.t(), late, cue.line()));
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
                    for (Played line : play(script, events, true)) {
                        LevelScript.RadioCue cue = line.cue();
                        if (cue.trigger() != CueTrigger.TIME) {
                            continue;
                        }
                        assertTrue(line.opened().isPresent(), level + ": the timed line at t=" + cue.t() + " plays");
                        assertTrue(
                                VoiceLines.spoken(cue.line()).isEmpty() || voiceSeconds(cue) > 0,
                                level + ": the timed line at t=" + cue.t() + " has its voice");
                        if (FOLLOWING.getOrDefault(level, Set.of()).contains(cue.t())) {
                            continue;
                        }
                        double late = line.opened().get() - cue.t();
                        if (late > MAX_LATE_SECONDS) {
                            worst.merge(
                                    String.format("%s t=%s %s (%s)", level, cue.t(), cue.speaker(), difficulty),
                                    late,
                                    Math::max);
                        }
                    }
                }
            }
        });
        worst.forEach((line, late) -> System.out.printf("voiced line late by %.1f s: %s%n", late, line));
    }

    /** The voice length of a radio line from its rendered file, 0 without one. */
    private static float voiceSeconds(LevelScript.RadioCue cue) {
        VoiceLines.VoiceLine line =
                VOICES.get(VoiceLines.indexKey(cue.speaker(), VoiceLines.allyLine(cue.line(), 2), cue.expression()));
        if (line == null) {
            return 0;
        }
        try (VorbisFile file = new VorbisFile(java.nio.file.Files.readAllBytes(ASSETS.resolve(line.path())))) {
            return (float) file.frameCount() / file.sampleRate();
        } catch (java.io.IOException e) {
            return 0;
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

    /** Whether one of {@code events} has set off the event cue by {@code tick}. */
    private static boolean setOff(List<Event> events, LevelScript.RadioCue cue, int tick) {
        return events.stream()
                .anyMatch(event -> event.trigger() == cue.trigger()
                        && event.subject().equals(cue.subject())
                        && SimStep.ticks(event.t()) <= tick);
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
        return play(script, events, false);
    }

    private static List<Played> play(LevelScript script, List<Event> events, boolean voiced) {
        RadioSchedule schedule = new RadioSchedule(script);
        RadioQueue radio = new RadioQueue();
        List<LevelScript.RadioCue> cues = script.radio();
        boolean[] fired = new boolean[cues.size()];
        List<Integer> queued = new ArrayList<>();
        Double[] opened = new Double[cues.size()];
        int end = SimStep.ticks(script.seconds());
        for (int tick = 1; tick <= end + SimStep.ticks(60); tick++) {
            double seconds = (double) tick / SimStep.PER_SECOND;
            for (int i = 0; i < cues.size(); i++) {
                LevelScript.RadioCue cue = cues.get(i);
                boolean due =
                        switch (cue.trigger()) {
                            case TIME -> tick <= end && SimStep.ticks(cue.t()) <= tick;
                            case LEVEL_END -> tick == end;
                            case SECONDARY_OBJECTIVE -> false;
                            default -> setOff(events, cue, tick);
                        };
                if (due && !fired[i]) {
                    fired[i] = true;
                    queued.add(i);
                    radio.add(
                            cue.speaker(),
                            cue.portrait(),
                            cue.expression(),
                            cue.line(),
                            cue.distorted(),
                            schedule.priority(i),
                            Optional.empty(),
                            voiced ? voiceSeconds(cue) : 0);
                }
            }
            float untilTimed = tick < end ? schedule.untilTimed(seconds) : Float.POSITIVE_INFINITY;
            if (radio.update((float) SimStep.SECONDS, untilTimed) == RadioQueue.Change.OPENED) {
                RadioQueue.Message message = radio.current().orElseThrow();
                for (int i : queued) {
                    if (opened[i] == null
                            && cues.get(i).speaker().equals(message.speaker())
                            && RadioQueue.wrap(cues.get(i).line()).equals(message.lines())) {
                        opened[i] = seconds;
                        break;
                    }
                }
            }
        }
        List<Played> played = new ArrayList<>();
        for (int i = 0; i < cues.size(); i++) {
            played.add(new Played(cues.get(i), Optional.ofNullable(opened[i])));
        }
        return played;
    }
}
