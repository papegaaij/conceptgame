package vanguard.sim;

import java.util.Arrays;
import java.util.List;

/**
 * The level's radio cues (design/campaign, radio chatter): each starts once per attempt, at its
 * time or on its event, as a {@link SimEvents.Type#RADIO} event at the ship. A cue starts only
 * with what it requires fitted and nothing it requires not (a special, a homing weapon), and a
 * level-end cue only with the convoy units home in its range.
 */
final class Radio {
    private final List<LevelScript.RadioCue> cues;
    private final boolean[] fired;
    private final SimEvents events;
    private final Ship ship;
    /** What is fitted, as {@link LevelScript.RadioCue#FITTED_SPECIAL} and {@link LevelScript.RadioCue#FITTED_HOMING} bits. */
    private final int fitted;

    Radio(List<LevelScript.RadioCue> cues, SimEvents events, Ship ship, int fitted) {
        this.cues = cues;
        this.fired = new boolean[cues.size()];
        this.events = events;
        this.ship = ship;
        this.fitted = fitted;
    }

    /** What {@code armament} and {@code special} fit, as the radio cues' requirement bits. */
    static int fitted(Armament armament, SpecialSlot special) {
        int fitted = special.fitted() ? LevelScript.RadioCue.FITTED_SPECIAL : 0;
        for (int m = 0; m < armament.size(); m++) {
            if (armament.mount(m).weapon().delivery() == WeaponSpec.Delivery.HOMING) {
                fitted |= LevelScript.RadioCue.FITTED_HOMING;
            }
        }
        return fitted;
    }

    /** Copies which cues have started into {@code into} (a boss checkpoint). */
    void saveFired(boolean[] into) {
        System.arraycopy(fired, 0, into, 0, fired.length);
    }

    /** Back to the cues started at a boss checkpoint. */
    void restoreFired(boolean[] from) {
        System.arraycopy(from, 0, fired, 0, fired.length);
    }

    /** The number of cues. */
    int size() {
        return fired.length;
    }

    /** Back to the level start: every cue can start again. */
    void reset() {
        Arrays.fill(fired, false);
    }

    /** Starts the time cues that are due at {@code levelTick}. */
    void byTime(int levelTick) {
        for (int i = 0; i < cues.size(); i++) {
            LevelScript.RadioCue cue = cues.get(i);
            if (!fired[i]
                    && cue.trigger() == LevelScript.CueTrigger.TIME
                    && SimStep.ticks(cue.t()) <= levelTick
                    && allowed(cue)) {
                start(i);
            }
        }
    }

    /** Starts the cues of an event; {@code subject} is the enemy slug or secret name it is about. */
    void cue(LevelScript.CueTrigger trigger, String subject) {
        for (int i = 0; i < cues.size(); i++) {
            LevelScript.RadioCue cue = cues.get(i);
            if (!fired[i] && cue.trigger() == trigger && cue.subject().equals(subject) && allowed(cue)) {
                start(i);
            }
        }
    }

    /** Starts the level-end cues for {@code home} convoy units at the end (0 without a convoy). */
    void end(int home) {
        for (int i = 0; i < cues.size(); i++) {
            LevelScript.RadioCue cue = cues.get(i);
            if (!fired[i]
                    && cue.trigger() == LevelScript.CueTrigger.LEVEL_END
                    && home >= cue.alliesMin()
                    && home <= cue.alliesMax()
                    && allowed(cue)) {
                start(i);
            }
        }
    }

    private boolean allowed(LevelScript.RadioCue cue) {
        return cue.allowedWith(fitted);
    }

    private void start(int index) {
        fired[index] = true;
        events.add(SimEvents.Type.RADIO, ship.x(), ship.y(), index);
    }
}
