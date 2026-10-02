package vanguard.game.audio;

import java.util.ArrayList;
import java.util.List;
import vanguard.game.settings.AudioSettings;

/**
 * The volume of each {@link Bus} from the Audio tab, times the master volume. What plays for a
 * while (music streams, looped effects) listens to changes, so a slider applies at once. While a
 * solo music stream plays (the sound test's), every other music stream is silent.
 */
public final class Mixer {
    /** Something playing that must follow a volume change. */
    @FunctionalInterface
    interface Listener {
        void gainsChanged();
    }

    private final List<Listener> listeners = new ArrayList<>();
    private AudioSettings settings;
    private boolean solo;

    public Mixer(AudioSettings settings) {
        this.settings = settings;
    }

    /** The gain of a bus, 0..1. */
    public float gain(Bus bus) {
        double volume =
                switch (bus) {
                    case MUSIC -> settings.music();
                    case EFFECTS -> settings.effects();
                    case RADIO -> settings.radio();
                };
        return (float) (settings.master() * volume);
    }

    /** The music gain of a stream: 0 while another stream plays solo. */
    float musicGain(boolean soloStream) {
        return solo && !soloStream ? 0 : gain(Bus.MUSIC);
    }

    public void set(AudioSettings changed) {
        settings = changed;
        notifyListeners();
    }

    /** Starts or ends a solo stream's turn. */
    void solo(boolean on) {
        solo = on;
        notifyListeners();
    }

    private void notifyListeners() {
        List.copyOf(listeners).forEach(Listener::gainsChanged);
    }

    void listen(Listener listener) {
        listeners.add(listener);
    }

    void forget(Listener listener) {
        listeners.remove(listener);
    }
}
