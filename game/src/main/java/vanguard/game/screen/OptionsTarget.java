package vanguard.game.screen;

import vanguard.game.settings.Settings;

/** What the Options rows change: the settings, and the display mode, which the display switcher keeps. */
interface OptionsTarget {
    Settings settings();

    /** Applies changed settings at once. */
    void change(Settings changed);

    boolean fullScreen();

    void toggleFullScreen();
}
