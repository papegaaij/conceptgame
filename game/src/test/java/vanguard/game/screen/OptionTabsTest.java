package vanguard.game.screen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Files;
import com.badlogic.gdx.backends.lwjgl3.audio.mock.MockAudio;
import java.util.List;
import org.junit.jupiter.api.Test;
import vanguard.game.audio.Mixer;
import vanguard.game.audio.SfxBank;
import vanguard.game.audio.SoundTest;
import vanguard.game.input.Action;
import vanguard.game.input.ControlSettings;
import vanguard.game.settings.Scaling;
import vanguard.game.settings.Settings;

class OptionTabsTest {
    /** Settings, a display mode and a sound test without an audio device. */
    private static final class Target implements OptionsTarget {
        Settings settings = Settings.defaults();
        boolean fullScreen = true;
        final SoundTest soundTest;

        Target() {
            var audio = new MockAudio();
            var files = new Lwjgl3Files();
            var mixer = new Mixer(settings.audio());
            soundTest = new SoundTest(audio, files, mixer, new SfxBank(audio, files, mixer));
        }

        @Override
        public Settings settings() {
            return settings;
        }

        @Override
        public void change(Settings changed) {
            settings = changed;
        }

        @Override
        public boolean fullScreen() {
            return fullScreen;
        }

        @Override
        public void toggleFullScreen() {
            fullScreen = !fullScreen;
        }

        @Override
        public SoundTest soundTest() {
            return soundTest;
        }
    }

    private final Target target = new Target();
    private final List<OptionTabs.Tab> tabs = OptionTabs.all();

    private OptionRow row(String tab, String label) {
        return tabs.stream()
                .filter(t -> t.name().equals(tab))
                .flatMap(t -> t.rows().stream())
                .filter(r -> r.label().equals(label))
                .findFirst()
                .orElseThrow();
    }

    @Test
    void theFourTabsOfTheOptionsDocument() {
        assertEquals(
                List.of("VIDEO", "AUDIO", "CONTROLS", "GAMEPLAY"),
                tabs.stream().map(OptionTabs.Tab::name).toList());
    }

    @Test
    void theDisplayModeChipTogglesTheMode() {
        OptionRow mode = row("VIDEO", "DISPLAY MODE");

        assertTrue(mode.change(target, 1));
        assertFalse(target.fullScreen);
        assertFalse(mode.change(target, 1), "WINDOW is the last chip");
        assertTrue(mode.change(target, -1));
        assertTrue(target.fullScreen);
    }

    @Test
    void scalingAndScanlinesChangeTheVideoSettings() {
        row("VIDEO", "SCALING").change(target, 1);
        row("VIDEO", "CRT SCANLINES").change(target, 1);

        assertEquals(Scaling.SHARP_BILINEAR, target.settings.video().scaling());
        assertTrue(target.settings.video().scanlines());
    }

    @Test
    void volumesStepByFivePercentBetweenZeroAndAll() {
        OptionRow music = row("AUDIO", "MUSIC");
        assertFalse(music.change(target, 1), "it starts at 100 %");

        for (int i = 0; i < 3; i++) {
            music.change(target, -1);
        }

        assertEquals(0.85, target.settings.audio().music());
        assertEquals(1, target.settings.audio().master(), "the other volumes stay");
    }

    @Test
    void theSoundTestRowsPickFromTheirLists() {
        OptionRow music = row("AUDIO", "SOUND TEST: MUSIC");
        OptionRow effects = row("AUDIO", "SOUND TEST: EFFECTS");

        assertTrue(music.change(target, -1), "it wraps round to the last track");
        assertTrue(effects.change(target, 1));
        assertFalse(effects.change(target, 0));

        SoundTest test = target.soundTest;
        assertEquals(test.names(SoundTest.Kind.MUSIC).size() - 1, test.picked(SoundTest.Kind.MUSIC));
        assertEquals(1, test.picked(SoundTest.Kind.EFFECTS));
    }

    @Test
    void theControlsTabRemapsEveryInLevelActionAndResets() {
        List<Action> remapped = tabs.get(2).rows().stream()
                .filter(OptionRow.Remap.class::isInstance)
                .map(r -> ((OptionRow.Remap) r).action())
                .toList();
        assertEquals(Action.REMAPPABLE, remapped);

        row("CONTROLS", "AUTO-FIRE").change(target, 1);
        row("CONTROLS", "STICK DEAD ZONE").change(target, 1);
        assertTrue(target.settings.controls().autoFire());
        assertEquals(0.25, target.settings.controls().deadZone());

        ((OptionRow.Button) row("CONTROLS", "RESET TO DEFAULTS")).action().accept(target);

        assertEquals(ControlSettings.defaults(), target.settings.controls());
    }

    @Test
    void theGameplayTabSetsTextSpeedShakeAndFlashReduction() {
        row("GAMEPLAY", "TEXT SPEED").change(target, 1);
        row("GAMEPLAY", "SCREEN SHAKE").change(target, -1);
        row("GAMEPLAY", "FLASH REDUCTION").change(target, 1);

        assertEquals(40, target.settings.gameplay().textSpeed());
        assertEquals(0.9, target.settings.gameplay().screenShake());
        assertTrue(target.settings.gameplay().flashReduction());
    }

    @Test
    void theGameplayTabTurnsTheCreditNumbersOff() {
        assertTrue(target.settings.gameplay().creditNumbers());

        row("GAMEPLAY", "CREDIT NUMBERS").change(target, -1);

        assertFalse(target.settings.gameplay().creditNumbers());
    }
}
