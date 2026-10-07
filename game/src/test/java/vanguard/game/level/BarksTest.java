package vanguard.game.level;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import vanguard.content.Content;
import vanguard.content.ContentLoader;
import vanguard.content.Difficulty;
import vanguard.content.Expression;
import vanguard.content.SimSpecs;
import vanguard.content.WingmenData;
import vanguard.content.voice.VoiceLines;
import vanguard.game.level.Barks.Trigger;
import vanguard.sim.LevelScript;
import vanguard.sim.SimStep;
import vanguard.sim.WaveSpec;

/**
 * Rook's radio barks (design/player/wingmen, Radio barks): triggers, spacing, suppression, priority,
 * variants, and the eject bark that is never dropped.
 */
class BarksTest {
    private static final Content CONTENT = ContentLoader.fromClasspath();
    private static final WingmenData.Barks DATA = CONTENT.wingmen().barks();
    private static final double[] NONE = {};

    private final RadioQueue radio = new RadioQueue();
    private final List<Barks.Line> queued = new ArrayList<>();

    private Barks barks(int level, double[] timedRook, double[] rear, double[] sides) {
        return new Barks(DATA, level, timedRook, rear, sides, radio, line -> {
            queued.add(line);
            lastQueued = radio.add(
                    line.speaker(),
                    line.speaker(),
                    line.expression(),
                    line.text(),
                    false,
                    line.priority(),
                    Optional.empty(),
                    0);
            return lastQueued;
        });
    }

    private Barks barks(int level) {
        return barks(level, NONE, NONE, NONE);
    }

    private static List<String> lines(Trigger trigger) {
        return DATA.bark(trigger.id()).orElseThrow().lines();
    }

    /** Keeps the radio busy with a timed line, so a queued bark waits. */
    private void busy() {
        radio.add("Okafor", "neutral", "Five crawlers, four hundred civilians. Get them to the terminal.", false);
        radio.update(0.01f);
    }

    @Test
    void theDataHasTheEightTriggersInPriorityOrderWithTwentySevenLines() {
        assertEquals(
                List.of(Trigger.values()).stream().map(Trigger::id).toList(),
                DATA.triggers().stream().map(WingmenData.Bark::trigger).toList());
        assertEquals(
                27,
                DATA.triggers().stream().mapToInt(bark -> bark.lines().size()).sum());
        assertEquals("Rook", DATA.speaker());
        assertEquals(8, DATA.spacing());
    }

    @Test
    void theVariantsRotateWithTheLevelAndTheBarksOfTheAttempt() {
        Barks barks = barks(9);
        List<String> boss = lines(Trigger.BOSS_WARNING);

        assertTrue(barks.bossWarning(10));
        radio.update(0.01f);
        barks.opened("Rook", 10);
        assertTrue(barks.bossWarning(30));

        assertEquals(boss.get(9 % boss.size()), queued.get(0).text(), "level 9 opens on variant (9 + 0) mod 4");
        assertEquals(boss.get(10 % boss.size()), queued.get(1).text(), "the next one on (9 + 1) mod 4");
        assertEquals("grim", queued.get(0).expression());
        assertFalse(queued.get(0).shout());
    }

    @Test
    void aRetryReplaysTheSameVariants() {
        Barks barks = barks(8);
        barks.bossWarning(10);
        radio.update(0.01f);
        barks.opened("Rook", 10);
        barks.bossWarning(30);
        barks.reset(0);
        barks.bossWarning(10);

        assertEquals(queued.get(0).text(), queued.get(2).text());
    }

    @Test
    void hisFirstKillLineCountsAsAScriptedRookLineForTheSpacing() {
        // M5 part B: the kill that made a streak also started "Splash one": the bark queued with it goes,
        // and the next eight seconds stay free of barks.
        Barks barks = barks(8);
        busy();
        assertTrue(barks.bossWarning(20));
        assertTrue(radio.waiting(lastQueued));

        barks.scripted("Okafor", 20);
        assertTrue(radio.waiting(lastQueued), "only a Rook line counts");
        barks.scripted("Rook", 20);

        assertFalse(radio.waiting(lastQueued), "the waiting bark is withdrawn");
        assertFalse(barks.bossWarning(27.9));
        assertTrue(barks.bossWarning(28.1));
    }

    @Test
    void aBarkIsDroppedWithinEightSecondsOfTheStartOfARookLine() {
        Barks barks = barks(8);
        barks.opened("Rook", 20);

        assertFalse(barks.bossWarning(27.9), "7.9 s after a Rook line");
        assertTrue(barks.bossWarning(28), "8 s after");
    }

    @Test
    void otherSpeakersDoNotStartTheSpacing() {
        Barks barks = barks(8);
        barks.opened("Okafor", 20);

        assertTrue(barks.bossWarning(21));
    }

    @Test
    void aTimedRookLineWithinTheSpacingSuppressesTheBark() {
        Barks barks = barks(8, new double[] {92}, NONE, NONE);

        assertFalse(barks.fire(Trigger.REAR_WAVE, 90.5), "the scripted Six o'clock says it");
        assertFalse(barks.fire(Trigger.REAR_WAVE, 99.5), "the scripted line is still on the radio");
        assertTrue(barks.fire(Trigger.REAR_WAVE, 83.9));
        assertEquals(1, queued.size());
    }

    @Test
    void onlyOneBarkWaitsAndAHigherPriorityReplacesIt() {
        Barks barks = barks(8);
        busy();
        assertTrue(barks.fire(Trigger.KILL_STREAK, 5));
        RadioQueue.Message streak = lastQueued;
        assertTrue(radio.waiting(streak));

        assertFalse(barks.fire(Trigger.OVERDRIVE, 5.5), "a lower priority is dropped while one waits");
        assertFalse(barks.fire(Trigger.KILL_STREAK, 5.6), "so is the same priority");
        assertTrue(barks.fire(Trigger.BOSS_WARNING, 6), "a higher priority replaces the waiting one");
        assertFalse(radio.waiting(streak), "the replaced bark left the queue");
        assertEquals(Trigger.BOSS_WARNING, queued.getLast().trigger());
    }

    @Test
    void aBarkThatHasPlayedNoLongerBlocksALowerPriority() {
        Barks barks = barks(8);
        assertTrue(barks.fire(Trigger.BOSS_WARNING, 5));
        radio.update(0.01f);
        barks.opened("Rook", 5);
        for (int i = 0; i < 200; i++) {
            radio.update(0.1f);
        }

        assertTrue(barks.fire(Trigger.OVERDRIVE, 30));
    }

    @Test
    void aBarkWaitsForAGapAndNeverPushesATimedLine() {
        Barks barks = barks(8);
        barks.fire(Trigger.BOSS_WARNING, 5);

        assertEquals(RadioQueue.Change.NONE, radio.update(0.01f, 2), "no gap before the timed line due in 2 s");
        radio.add("Okafor", "neutral", "Timed line.", false);
        assertEquals(RadioQueue.Change.OPENED, radio.update(0.01f, Float.POSITIVE_INFINITY));
        assertEquals("Okafor", radio.current().orElseThrow().speaker(), "the timed line goes first");
    }

    @Test
    void rooksLowArmourBarkCannotFireAfterHeEjects() {
        Barks barks = barks(8);
        assertTrue(barks.rookEjected(10));
        List<String> eject = lines(Trigger.ROOK_EJECTS);
        assertEquals(eject.get(8 % eject.size()), queued.getLast().text());
        assertTrue(queued.getLast().shout(), "shouted");
        assertEquals("fierce", queued.getLast().expression());
        radio.update(0.01f);
        barks.opened("Rook", 10);

        assertFalse(barks.rookCritical(40));
        assertTrue(barks.bossWarning(40), "the others still fire");
    }

    @Test
    void theEjectBarkIgnoresTheSpacingAndTheScriptedRookLines() {
        Barks barks = barks(6, new double[] {136}, NONE, NONE);
        barks.opened("Rook", 133);

        assertTrue(barks.rookEjected(136.2), "0.2 s after a scripted Rook line, at the time of another");
        assertEquals(Trigger.ROOK_EJECTS, queued.getLast().trigger());
        assertEquals(RadioQueue.Priority.URGENT, queued.getLast().priority());
        assertEquals(
                RadioQueue.Priority.EVENT, new Barks.Line(Trigger.REAR_WAVE, "Rook", "fierce", true, "").priority());
    }

    @Test
    void theEjectBarkPlaysAtOnceAndTheInterruptedLineReplaysAfterIt() {
        Barks barks = barks(6);
        busy();
        barks.rookEjected(10);

        assertEquals(RadioQueue.Change.OPENED, radio.update(0.01f, 1), "even with a timed line due in 1 s");
        assertEquals("Rook", radio.current().orElseThrow().speaker());
        playOut();
        assertEquals("Okafor", radio.current().orElseThrow().speaker(), "the timed line plays again after it");
    }

    @Test
    void theEjectBarkNeverGoesStaleNorIsReplaced() {
        Barks barks = barks(6);
        radio.add(
                "Okafor",
                "Okafor",
                "neutral",
                "Hammer flight, inbound! Keep your heads down, Lancer, we come in low and fast over the ridge.",
                false,
                RadioQueue.Priority.URGENT);
        radio.update(0.01f);
        assertTrue(barks.rookEjected(10));
        RadioQueue.Message eject = lastQueued;

        assertFalse(barks.bossWarning(19), "nothing outranks it while it waits");
        for (int i = 0; i < 70; i++) {
            radio.update(0.1f);
        }
        assertTrue(radio.waiting(eject), "still waiting for the urgent line after 7 s: not stale");
        playOut();
        assertEquals(eject, radio.current().orElseThrow());
    }

    @Test
    void theEjectBarkCutsHisWaitingBark() {
        Barks barks = barks(6);
        busy();
        assertTrue(barks.fire(Trigger.KILL_STREAK, 5));
        RadioQueue.Message streak = lastQueued;
        assertTrue(barks.rookEjected(5.5));

        assertFalse(radio.waiting(streak));
    }

    @Test
    void theEjectBarkCutsHisPlayingBarkWhichDoesNotPlayAgain() {
        Barks barks = barks(6);
        assertTrue(barks.rookCritical(5));
        RadioQueue.Message critical = lastQueued;
        radio.update(0.01f);
        barks.opened("Rook", 5);
        assertEquals(critical, radio.current().orElseThrow());
        assertTrue(barks.rookEjected(6));
        RadioQueue.Message eject = lastQueued;

        assertEquals(RadioQueue.Change.OPENED, radio.update(0.01f));
        assertEquals(eject, radio.current().orElseThrow());
        for (int i = 0; i < 300; i++) {
            radio.update(0.1f);
        }
        assertTrue(radio.idle(), "the cut bark does not replay");
    }

    /** Updates the radio until the message on it closes and the next one opens (or nothing is left). */
    private void playOut() {
        RadioQueue.Message playing = radio.current().orElseThrow();
        for (int i = 0; i < 1000 && radio.current().map(m -> m == playing).orElse(true) && !radio.idle(); i++) {
            radio.update(0.05f);
        }
    }

    @Test
    void thePlayersArmourBarksWhenItFallsBelowThirtyPercentAgainOnlyAfterItWasBack() {
        Barks barks = barks(8);
        barks.step(1, 0.5, 0);
        barks.step(2, 0.29, 0);
        assertEquals(1, queued.size());
        radio.update(0.01f);
        barks.step(SimStep.ticks(20), 0.2, 0);
        assertEquals(1, queued.size(), "still low: no second bark");
        barks.step(SimStep.ticks(21), 0.3, 0);
        barks.step(SimStep.ticks(22), 0.25, 0);
        assertEquals(2, queued.size(), "back at 30 %, then below again");
        assertEquals(Trigger.PLAYER_ARMOUR, queued.getLast().trigger());
    }

    @Test
    void anAttemptThatStartsLowDoesNotBark() {
        Barks barks = barks(8);
        barks.step(1, 0.2, 0);

        assertTrue(queued.isEmpty());
    }

    @Test
    void tenKillsWithinFiveSecondsAreAStreakAndTheCountStartsOver() {
        Barks barks = barks(8);
        for (int i = 0; i < 9; i++) {
            assertFalse(barks.kill(10 + i * 0.5));
        }
        assertTrue(barks.kill(14.6), "the tenth within 5 s");
        for (int i = 0; i < 9; i++) {
            barks.kill(15 + i * 0.1);
        }
        assertEquals(1, queued.size(), "the count started over");
    }

    @Test
    void killsSpreadOverMoreThanFiveSecondsAreNoStreak() {
        Barks barks = barks(8);
        for (int i = 0; i < 20; i++) {
            barks.kill(i * 0.6);
        }

        assertTrue(queued.isEmpty());
    }

    @Test
    void aRearWaveBarksOneAndAHalfSecondsBeforeItEntersASidesWaveAsItEnters() {
        Barks barks = barks(8, NONE, new double[] {30}, new double[] {60});
        int ahead = SimStep.ticks(28.5);
        barks.step(ahead - 1, 1, 0);
        assertTrue(queued.isEmpty());
        barks.step(ahead, 1, 0);
        assertEquals(Trigger.REAR_WAVE, queued.getLast().trigger());
        assertTrue(queued.getLast().shout());
        radio.update(0.01f);

        barks.step(SimStep.ticks(60) - 1, 1, 0);
        assertEquals(1, queued.size());
        barks.step(SimStep.ticks(60), 1, 0);
        assertEquals(Trigger.SIDES_WAVE, queued.getLast().trigger());
    }

    @Test
    void aBossCheckpointRetryDoesNotBarkTheWavesBeforeIt() {
        Barks barks = barks(8, NONE, new double[] {30}, NONE);
        barks.reset(SimStep.ticks(100));
        barks.step(SimStep.ticks(100) + 1, 1, 0);

        assertTrue(queued.isEmpty());
    }

    @Test
    void anOverdrivePickupAppearingBarks() {
        Barks barks = barks(8);
        barks.step(1, 1, 0);
        barks.step(2, 1, 1);
        assertEquals(Trigger.OVERDRIVE, queued.getLast().trigger());
        barks.step(3, 1, 1);
        assertEquals(1, queued.size(), "the same pickup once");
    }

    /** The level script gives the timed Rook lines and the rear and sides waves (an Act 1 level with a rear wave). */
    @Test
    void ofReadsTheLevelScript() {
        LevelScript script = CONTENT.levels().keySet().stream()
                .sorted()
                .map(key -> SimSpecs.level(CONTENT, key, Difficulty.MEDIUM))
                .filter(level -> level.waves().stream().anyMatch(wave -> wave.entry() == WaveSpec.Entry.REAR))
                .findFirst()
                .orElseThrow();
        Barks barks = Barks.of(DATA, script, false, radio, line -> {
            queued.add(line);
            return radio.add(
                    line.speaker(),
                    line.speaker(),
                    line.expression(),
                    line.text(),
                    false,
                    RadioQueue.Priority.EVENT,
                    Optional.empty(),
                    0);
        });
        for (int tick = 0; tick < SimStep.ticks(400); tick++) {
            barks.step(tick, 1, 0);
            if (radio.update((float) SimStep.SECONDS) == RadioQueue.Change.OPENED) {
                barks.opened(radio.current().orElseThrow().speaker(), tick * SimStep.SECONDS);
            }
        }

        assertTrue(queued.stream().anyMatch(line -> line.trigger() == Trigger.REAR_WAVE), "a rear wave barks");
        assertTrue(queued.stream().allMatch(line -> line.speaker().equals("Rook")));
    }

    /** Every bark is a spoken line (Rook is cast), rendered by tools/art/voice.py with the others. */
    @Test
    void everyBarkIsAVoiceLine() {
        List<VoiceLines.VoiceLine> all = VoiceLines.all(CONTENT);
        for (WingmenData.Bark bark : DATA.triggers()) {
            for (String text : bark.lines()) {
                VoiceLines.VoiceLine line = all.stream()
                        .filter(voice ->
                                voice.speaker().equals("Rook") && voice.text().equals(text))
                        .filter(voice -> voice.expression() == bark.expression())
                        .findFirst()
                        .orElseThrow(() -> new AssertionError("no voice line for " + text));
                assertEquals(VoiceLines.BARKS + " " + bark.trigger(), line.source());
                assertEquals(bark.shouted(), line.shout(), text);
            }
        }
        assertTrue(DATA.triggers().stream().anyMatch(bark -> bark.expression() == Expression.FIERCE));
    }

    private RadioQueue.Message lastQueued;

    @Test
    void inAHoldTheSpacingToATimedRookLineIsMeasuredInRealSeconds() {
        // M5 part C: a timed Rook line 2 s of script time ahead; in a hold at a fifth of the speed it is
        // 10 real seconds away, beyond the 8 s spacing, so a bark may play now; outside a hold it may not.
        Barks barks = barks(9, new double[] {60}, NONE, NONE);
        barks.clock(58, 58, 1);
        assertFalse(barks.bossWarning(58), "2 s before his timed line");

        barks.clock(58, 70, 0.2);
        assertTrue(barks.bossWarning(70), "10 real seconds before it in the hold");
    }

    @Test
    void aTimedLineThatRequiresHimNoLongerCountsOnceHeEjected() {
        // M5 part C (requires: escort): after he ejects his scripted line does not play, so it holds no bark back.
        Barks barks = new Barks(DATA, 9, new double[] {60}, new boolean[] {true}, NONE, NONE, radio, line -> {
            queued.add(line);
            return radio.add(
                    line.speaker(),
                    line.speaker(),
                    line.expression(),
                    line.text(),
                    false,
                    line.priority(),
                    Optional.empty(),
                    0);
        });
        assertFalse(barks.bossWarning(57), "his line at 60 still counts while he flies");
        barks.rookEjected(57.5);
        radio.update(0.01f);
        barks.opened("Rook", 40);
        assertTrue(barks.bossWarning(59), "it does not once he is out");
    }

    @Test
    void ofTakesTheEscortFlagFromTheCue() {
        var line = new LevelScript.RadioCue(
                LevelScript.CueTrigger.TIME,
                60,
                "",
                "Rook",
                "Six bugs, two pilots.",
                false,
                "neutral",
                "Rook",
                false,
                0,
                Integer.MAX_VALUE,
                LevelScript.RadioCue.FITTED_ESCORT,
                0);
        var script = new LevelScript(
                9,
                2,
                0,
                List.of(new LevelScript.Section(150, 200)),
                List.of(),
                List.of(),
                List.of(),
                0,
                List.of(line),
                new LevelScript.Secondary(0.8, 50),
                List.of());
        Barks barks = Barks.of(DATA, script, 0, radio, bark -> {
            queued.add(bark);
            return radio.add(
                    bark.speaker(),
                    bark.speaker(),
                    bark.expression(),
                    bark.text(),
                    false,
                    bark.priority(),
                    Optional.empty(),
                    0);
        });
        assertFalse(barks.bossWarning(57), "his line at 60 is near");
        barks.rookEjected(57.5);
        radio.update(0.01f);
        barks.opened("Rook", 40);
        assertTrue(barks.bossWarning(59), "not once he is out");
    }
}
