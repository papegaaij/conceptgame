package vanguard.game.audio;

import java.util.Arrays;
import vanguard.sim.Enemy;
import vanguard.sim.Flock;
import vanguard.sim.PlayField;
import vanguard.sim.Sortie;

/**
 * M5 part D: a Mote Swarm's rush of wings as it enters the screen (design/audio/sfx, Level 10: a
 * one-shot at its entry; its loop-backs play it on their {@code LOOP_BACK} events). A swarm enters in
 * the first step one of its members is over the play field; then every member it has is noted, so
 * the swarm is heard once however its members trickle in, die or leave and come back. Members are
 * told by their serials, unique in an attempt; a fixed ring, no allocation per step; it forgets them
 * when an attempt restarts.
 */
final class SwarmCue {
    /** How many members it remembers: a few swarms of at most 24 on the screen at once. */
    static final int REMEMBERED = 256;

    private final int[] seen = new int[REMEMBERED];
    private int seenCount;
    private int next;
    /** Where the last swarm entered, play-field x. */
    private double x;

    /**
     * Once a simulation step: whether a swarm entered the screen in it ({@link #x()} where).
     *
     * @param resync the first step after a restart or a boss checkpoint's restore: the swarms already
     *     on the screen are noted, not heard
     */
    boolean watch(Sortie sortie, boolean resync) {
        if (resync) {
            reset();
        }
        boolean entered = false;
        for (int f = 0; f < sortie.flockCount(); f++) {
            Flock flock = sortie.flock(f);
            Enemy shown = null;
            for (int i = 0; i < flock.size() && shown == null; i++) {
                Enemy member = flock.member(i);
                if (member != null && PlayField.overlaps(member.renderX(1), member.renderY(1), member.hitbox())) {
                    shown = member;
                }
            }
            if (shown == null || seen(shown.serial())) {
                continue;
            }
            for (int i = 0; i < flock.size(); i++) {
                Enemy member = flock.member(i);
                if (member != null) {
                    note(member.serial());
                }
            }
            if (!resync) {
                entered = true;
                x = shown.renderX(1);
            }
        }
        return entered;
    }

    /** The play-field x of the last swarm that entered. */
    double x() {
        return x;
    }

    /** Whether a member with this serial was noted. */
    boolean seen(int serial) {
        for (int i = 0; i < seenCount; i++) {
            if (seen[i] == serial) {
                return true;
            }
        }
        return false;
    }

    /** Notes a member's serial (the oldest forgotten once the ring is full). */
    void note(int serial) {
        if (seen(serial)) {
            return;
        }
        seen[next] = serial;
        next = (next + 1) % REMEMBERED;
        seenCount = Math.min(seenCount + 1, REMEMBERED);
    }

    /** Forgets every member (a new attempt numbers its units afresh). */
    void reset() {
        Arrays.fill(seen, 0);
        seenCount = 0;
        next = 0;
    }
}
