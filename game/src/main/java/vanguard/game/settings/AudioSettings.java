package vanguard.game.settings;

/**
 * The Audio tab (design/ui/options): volumes 0..1 on top of the game's own mix. The master volume
 * scales the four others.
 *
 * @param radio the radio blips: squelch and typing
 * @param voice the spoken radio lines and briefing pages (design/audio/voice)
 */
public record AudioSettings(double master, double music, double effects, double radio, double voice) {
    public static AudioSettings defaults() {
        return new AudioSettings(1, 1, 1, 1, 1);
    }

    public AudioSettings withMaster(double volume) {
        return new AudioSettings(volume, music, effects, radio, voice);
    }

    public AudioSettings withMusic(double volume) {
        return new AudioSettings(master, volume, effects, radio, voice);
    }

    public AudioSettings withEffects(double volume) {
        return new AudioSettings(master, music, volume, radio, voice);
    }

    public AudioSettings withRadio(double volume) {
        return new AudioSettings(master, music, effects, volume, voice);
    }

    public AudioSettings withVoice(double volume) {
        return new AudioSettings(master, music, effects, radio, volume);
    }
}
