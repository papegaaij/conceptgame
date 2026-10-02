package vanguard.sim;

import java.util.Arrays;
import java.util.List;

/**
 * The level's radio cues (design/campaign, radio chatter): each starts once per attempt, at its
 * time or on its event, as a {@link SimEvents.Type#RADIO} event at the ship.
 */
final class Radio {
    private final List<LevelScript.RadioCue> cues;
    private final boolean[] fired;
    private final SimEvents events;
    private final Ship ship;

    Radio(List<LevelScript.RadioCue> cues, SimEvents events, Ship ship) {
        this.cues = cues;
        this.fired = new boolean[cues.size()];
        this.events = events;
        this.ship = ship;
    }

    /** Back to the level start: every cue can start again. */
    void reset() {
        Arrays.fill(fired, false);
    }

    /** Starts the time cues that are due at {@code levelTick}. */
    void byTime(int levelTick) {
        for (int i = 0; i < cues.size(); i++) {
            LevelScript.RadioCue cue = cues.get(i);
            if (!fired[i] && cue.trigger() == LevelScript.CueTrigger.TIME && SimStep.ticks(cue.t()) <= levelTick) {
                start(i);
            }
        }
    }

    /** Starts the cues of an event; {@code subject} is the enemy slug or secret name it is about. */
    void cue(LevelScript.CueTrigger trigger, String subject) {
        for (int i = 0; i < cues.size(); i++) {
            LevelScript.RadioCue cue = cues.get(i);
            if (!fired[i] && cue.trigger() == trigger && cue.subject().equals(subject)) {
                start(i);
            }
        }
    }

    private void start(int index) {
        fired[index] = true;
        events.add(SimEvents.Type.RADIO, ship.x(), ship.y(), index);
    }
}
