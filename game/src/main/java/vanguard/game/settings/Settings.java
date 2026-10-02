package vanguard.game.settings;

import vanguard.game.input.ControlSettings;

/**
 * Everything the Options screen sets (design/ui/options) except the display mode: kept in the
 * settings file, apart from the save slots.
 */
public record Settings(VideoSettings video, AudioSettings audio, ControlSettings controls, GameplaySettings gameplay) {
    public static Settings defaults() {
        return new Settings(
                VideoSettings.defaults(),
                AudioSettings.defaults(),
                ControlSettings.defaults(),
                GameplaySettings.defaults());
    }

    public Settings withVideo(VideoSettings changed) {
        return new Settings(changed, audio, controls, gameplay);
    }

    public Settings withAudio(AudioSettings changed) {
        return new Settings(video, changed, controls, gameplay);
    }

    public Settings withControls(ControlSettings changed) {
        return new Settings(video, audio, changed, gameplay);
    }

    public Settings withGameplay(GameplaySettings changed) {
        return new Settings(video, audio, controls, changed);
    }
}
