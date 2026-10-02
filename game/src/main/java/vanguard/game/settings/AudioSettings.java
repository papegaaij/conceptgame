package vanguard.game.settings;

/**
 * The Audio tab (design/ui/options): volumes 0..1 on top of the game's own mix. The master volume
 * scales the three others.
 *
 * @param radio the radio blips: squelch and typing
 */
public record AudioSettings(double master, double music, double effects, double radio) {
    public static AudioSettings defaults() {
        return new AudioSettings(1, 1, 1, 1);
    }

    public AudioSettings withMaster(double volume) {
        return new AudioSettings(volume, music, effects, radio);
    }

    public AudioSettings withMusic(double volume) {
        return new AudioSettings(master, volume, effects, radio);
    }

    public AudioSettings withEffects(double volume) {
        return new AudioSettings(master, music, volume, radio);
    }

    public AudioSettings withRadio(double volume) {
        return new AudioSettings(master, music, effects, volume);
    }
}
