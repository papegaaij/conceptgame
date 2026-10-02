package vanguard.game.settings;

/** Where the settings persist: the settings file of the desktop launcher. */
@FunctionalInterface
public interface SettingsStore {
    /** Writes the settings; a failure must not stop the game. */
    void save(Settings settings);
}
