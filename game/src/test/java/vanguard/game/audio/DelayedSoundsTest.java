package vanguard.game.audio;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class DelayedSoundsTest {
    @Test
    void aSoundWaitsItsStepsThenPlaysOnce() {
        var sounds = new DelayedSounds(null);
        sounds.play(Sfx.EXPLOSION_SMALL_A, 1, 1, 0, 2);
        sounds.play(Sfx.EXPLOSION_SMALL_B, 1, 1, 0, 0);

        sounds.step();
        assertEquals(1, sounds.waiting(), "the undelayed one played at the next step");
        sounds.step();
        assertEquals(1, sounds.waiting());
        sounds.step();
        assertEquals(0, sounds.waiting());
    }

    @Test
    void itDropsWhatIsPastItsCapacityAndForgetsOnARestart() {
        var sounds = new DelayedSounds(null);
        for (int i = 0; i < DelayedSounds.CAPACITY + 5; i++) {
            sounds.play(Sfx.EXPLOSION_SMALL_A, 1, 1, 0, 10);
        }
        assertEquals(DelayedSounds.CAPACITY, sounds.waiting());

        sounds.clear();
        assertEquals(0, sounds.waiting());
    }
}
