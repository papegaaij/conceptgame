package vanguard.game.audio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Files;
import com.badlogic.gdx.backends.lwjgl3.audio.mock.MockAudio;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import vanguard.game.settings.AudioSettings;

/** The act boss's music cues in a level (design/audio/music, Loops and transitions), without a device. */
class BossMusicTest {
    private final MockAudio audio = new MockAudio();
    private final Lwjgl3Files files = new Lwjgl3Files();
    private final Mixer mixer = new Mixer(AudioSettings.defaults());
    private final SfxBank bank = new SfxBank(audio, files, mixer);

    private LevelMusic music(Optional<LevelMusic.Boss> boss) {
        return new LevelMusic(
                audio,
                mixer,
                files.internal(Tracks.basePath("coalition-rising")),
                files.internal(Tracks.path(5)),
                bank,
                Sfx.AMBIENCE_ORBIT,
                1,
                0,
                section -> true,
                Double.POSITIVE_INFINITY,
                Optional.empty(),
                boss);
    }

    private LevelMusic withBoss() {
        return music(Optional.of(new LevelMusic.Boss(
                Optional.of(files.internal(Tracks.path(Tracks.BOSS_WARNING))),
                Optional.of(files.internal(Tracks.path(Tracks.BOSS_VRELL))),
                Tracks.BOSS_WARNING_BARS_SECONDS)));
    }

    @Test
    void aLevelWithoutBossMusicLeavesTheArrivalToTheSting() {
        LevelMusic music = music(Optional.empty());

        assertFalse(music.bossArrived(), "Level 05's frigate plays its sting instead");
        assertEquals(LevelMusic.BossState.WAITING, music.bossState());
        music.dispose();
    }

    @Test
    void theBossMusicRunsFromTheArrivalToTheKillAndTheThemeStaysOut() {
        LevelMusic music = withBoss();
        music.update(4, 99, 0.1f, false);

        assertTrue(music.bossArrived());
        assertEquals(LevelMusic.BossState.FIGHT, music.bossState());
        music.update(4, 101, 0.1f, false);
        music.bossDown();
        assertEquals(LevelMusic.BossState.DOWN, music.bossState());
        music.update(4, 140, 2, false);
        assertEquals(LevelMusic.BossState.DOWN, music.bossState(), "only the ambience after the kill");
        music.dispose();
    }

    @Test
    void aRetryFromBossHoldsTheThemeUntilTheBossArrivesAndARestartForgetsIt() {
        LevelMusic music = withBoss();
        music.bossArrived();

        music.restart();
        music.bossRetry();
        assertEquals(LevelMusic.BossState.HELD, music.bossState());
        assertTrue(music.bossArrived());
        assertEquals(LevelMusic.BossState.FIGHT, music.bossState());

        music.restart();
        assertEquals(LevelMusic.BossState.WAITING, music.bossState());
        music.dispose();
    }

    @Test
    void bossMusicHasAWarningOrATrack() {
        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class, () -> new LevelMusic.Boss(Optional.empty(), Optional.empty(), 0));
    }
}
