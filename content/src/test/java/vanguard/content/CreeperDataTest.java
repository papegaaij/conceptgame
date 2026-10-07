package vanguard.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import vanguard.content.voice.VoiceLines;
import vanguard.sim.EnemyGun;
import vanguard.sim.EnemySpec;
import vanguard.sim.LevelScript;

/**
 * M5 part B's data mechanics: the Creeper (design/enemies/ground/creeper) as a walker with a fan
 * aimed at the player, its convoy stagger and its authored hard interval; the radio event {@code
 * escort-first-kill} and {@code {side}} in a radio line (design/player/wingmen, Scripted lines about
 * him).
 */
class CreeperDataTest {
    private static final Content CONTENT = ContentLoader.fromClasspath();
    private static final String CREEPER = "enemies/ground/creeper/data.yaml";
    private static final String STINGER = "enemies/air/stinger/data.yaml";
    private static final String LEVEL_01 = "campaign/act-1-first-contact/level-01-break-at-dawn/data.yaml";
    private static final String LEVEL_01_KEY = "act-1-first-contact/level-01-break-at-dawn";

    private static EnemySpec creeper(Difficulty difficulty) {
        return SimSpecs.enemy(CONTENT, "creeper", difficulty, Optional.empty());
    }

    @Test
    void itsFanIsAimedAtThePlayerAndStaggeredHalfASecond() {
        EnemySpec.Walker walker = creeper(Difficulty.MEDIUM).walker().orElseThrow();

        assertTrue(walker.fanAtShip(), "aim: target");
        assertEquals(0.5, walker.staggerSeconds());
        assertEquals(35, walker.speed());
        EnemySpec.Walker scuttler = SimSpecs.enemy(CONTENT, "scuttler", Difficulty.MEDIUM, Optional.empty())
                .walker()
                .orElseThrow();
        assertFalse(scuttler.fanAtShip(), "the Scuttler's fan goes along its facing");
        assertFalse(scuttler.staggered());
    }

    @Test
    void onHardItFiresSevenWaysAtTheAuthoredIntervalWithoutTheFireRateLever() {
        DifficultyData levers = CONTENT.difficulty();
        EnemyGun medium = creeper(Difficulty.MEDIUM).gun().orElseThrow();
        EnemyGun easy = creeper(Difficulty.EASY).gun().orElseThrow();
        EnemyGun hard = creeper(Difficulty.HARD).gun().orElseThrow();

        assertEquals(5, medium.fan());
        assertEquals(3.0 / levers.enemyFireRate().of(Difficulty.MEDIUM), medium.intervalSeconds(), 1e-9);
        assertEquals(3.0 / levers.enemyFireRate().of(Difficulty.EASY), easy.intervalSeconds(), 1e-9);
        assertEquals(7, hard.fan());
        assertEquals(2.6, hard.intervalSeconds(), 1e-9, "authored: the lever does not apply on top");
        assertTrue(2.6 != 3.0 / levers.enemyFireRate().of(Difficulty.HARD), "the lever alone would differ");
    }

    @Test
    void onlyAWalkersFanIsStaggered() {
        List<DataFile> files = replace(STINGER, "    spread: ", "    stagger: 0.5\n    spread: ");

        var e = assertThrows(ContentException.class, () -> ContentLoader.load(files));

        assertEquals(1, e.problems().size(), e.getMessage());
        assertTrue(e.problems().getFirst().contains("stagger"), e.problems().getFirst());
    }

    @Test
    void aStaggerIsPositive() {
        List<DataFile> files = replace(CREEPER, "    stagger: 0.5", "    stagger: -0.5");

        assertThrows(ContentException.class, () -> ContentLoader.load(files));
    }

    @Test
    void anEscortFirstKillCueNamesNoEnemyAndStartsOnHisKill() {
        String cue = "  - {event: escort-first-kill, speaker: Rook, line: \"Splash one.\"}\n";
        Content content = ContentLoader.load(
                replace(LEVEL_01, "  - {t: 18, speaker: Okafor", cue + "  - {t: 18, speaker: Okafor"));

        LevelScript.RadioCue loaded = SimSpecs.level(content, LEVEL_01_KEY, Difficulty.MEDIUM).radio().stream()
                .filter(radio -> radio.trigger() == LevelScript.CueTrigger.ESCORT_FIRST_KILL)
                .findFirst()
                .orElseThrow();
        assertEquals("", loaded.subject());
        assertEquals("Splash one.", loaded.line());

        String named = "  - {event: escort-first-kill, enemy: skitter, speaker: Rook, line: \"Splash one.\"}\n";
        assertThrows(
                ContentException.class,
                () -> ContentLoader.load(
                        replace(LEVEL_01, "  - {t: 18, speaker: Okafor", named + "  - {t: 18, speaker: Okafor")));
    }

    @Test
    void aSideLineIsVoicedOncePerSide() {
        Content content = ContentLoader.load(replace(
                LEVEL_01,
                "Lancer, Rook. Aegis Two's got the north arm, you've got the south. Try not to have all the fun.",
                "Lancer, I'm on your {side}."));

        List<String> texts = VoiceLines.radio(content.voices(), content.level(LEVEL_01_KEY), "level-01").stream()
                .map(VoiceLines.VoiceLine::text)
                .filter(text -> text.startsWith("Lancer, I'm on your"))
                .toList();

        assertEquals(List.of("Lancer, I'm on your left.", "Lancer, I'm on your right."), texts);
        assertEquals("Lancer, I'm on your right.", VoiceLines.sideLine("Lancer, I'm on your {side}.", "right"));
        // The game finds each side's take by the text it shows.
        var index = VoiceLines.radioIndex(content);
        for (String text : texts) {
            assertTrue(index.containsKey(VoiceLines.indexKey("Rook", text, "neutral")), text);
        }
    }

    private static List<DataFile> replace(String path, String from, String to) {
        assertTrue(DesignTree.dataFiles().stream().anyMatch(f -> f.path().equals(path)), path);
        return DesignTree.dataFiles().stream()
                .map(f -> {
                    if (!f.path().equals(path)) {
                        return f;
                    }
                    assertTrue(f.text().contains(from), path + " has " + from);
                    return new DataFile(path, f.text().replace(from, to));
                })
                .toList();
    }
}
