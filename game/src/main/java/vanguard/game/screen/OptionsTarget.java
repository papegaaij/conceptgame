package vanguard.game.screen;

import vanguard.game.audio.SoundTest;
import vanguard.game.settings.Settings;

/**
 * What the Options rows change: the settings, the display mode, which the display switcher keeps,
 * and the sound test.
 */
interface OptionsTarget {
    Settings settings();

    /** Applies changed settings at once. */
    void change(Settings changed);

    boolean fullScreen();

    void toggleFullScreen();

    SoundTest soundTest();
}
