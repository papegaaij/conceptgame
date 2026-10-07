package vanguard.game.audio;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import vanguard.sim.Enemy;
import vanguard.sim.EnemySpec;
import vanguard.sim.PlayField;
import vanguard.sim.Sortie;

/**
 * The Vrell screech (design/audio/sfx, Enemies; user decision 2026-10-06): a large Vrell unit
 * screeches once as it enters the screen (its first step with its hit box over the play field):
 * the Mantis, the Coilwyrm's head, the Spore Bomber and the Scuttler, the two chosen screeches
 * (round 08 c and d) in turn. At most one screech every {@value #THROTTLE_SECONDS} s, so a wave
 * entering together does not stack them: a unit that enters while the last screech is still fresh
 * stays silent (and does not screech later). Each unit is told by its serial, unique in an attempt,
 * so a unit that leaves the screen and comes back does not screech again. A fixed ring of serials,
 * no allocation per step; it forgets them when an attempt restarts.
 */
final class ScreechCue {
    /** The least time between two screeches. */
    static final double THROTTLE_SECONDS = 3;

    /** The enemy kinds that screech, by slug: the large Vrell units of Act 1 (not the Coilwyrm's body or regrown head) and the Creeper (M5 part B). */
    static final Set<String> SCREECHERS = Set.of("mantis", "coilwyrm", "spore-bomber", "scuttler", "creeper");

    /** How many units it remembers: far more than the large units ever on the screen at once. */
    static final int REMEMBERED = 64;

    private final int throttleSteps;
    private final int[] seen = new int[REMEMBERED];
    private int seenCount;
    private int next;
    private int quiet;
    private boolean nextIsD;
    /** Which of the sortie's enemy kinds screech, from the first {@link #watch}; null before. */
    private boolean[] kinds;
    /** Where the last screech's unit is, play-field x. */
    private double x;

    /** @param throttleSteps the least simulation steps between two screeches */
    ScreechCue(int throttleSteps) {
        this.throttleSteps = throttleSteps;
    }

    /** Whether an enemy kind screeches as it enters. */
    static boolean screeches(String slug) {
        return SCREECHERS.contains(slug);
    }

    /**
     * Once a simulation step: the screech of a unit that entered the screen in it, or null (at most
     * one a step); {@link #x()} is that unit's position.
     *
     * @param resync the first step after a restart or a boss checkpoint's restore: every unit is
     *     counted afresh and the ones already on the screen do not screech
     */
    Sfx watch(Sortie sortie, boolean resync) {
        if (kinds == null) {
            List<EnemySpec> specs = sortie.enemyKinds();
            kinds = new boolean[specs.size()];
            for (int k = 0; k < kinds.length; k++) {
                kinds[k] = screeches(specs.get(k).slug());
            }
        }
        if (resync) {
            reset();
        }
        step();
        Sfx screech = null;
        for (int i = 0; i < sortie.enemyCount(); i++) {
            Enemy enemy = sortie.enemy(i);
            int kind = enemy.kind();
            if (kind < kinds.length
                    && kinds[kind]
                    && PlayField.overlaps(enemy.renderX(1), enemy.renderY(1), enemy.hitbox())) {
                Sfx sound = onScreen(enemy.serial(), !resync);
                if (sound != null) {
                    screech = sound;
                    x = enemy.renderX(1);
                }
            }
        }
        return screech;
    }

    /** The play-field x of the last screech's unit. */
    double x() {
        return x;
    }

    /** One simulation step passed: the throttle runs down. */
    void step() {
        if (quiet > 0) {
            quiet--;
        }
    }

    /**
     * A screeching unit is on the screen: its screech if it has just entered and the throttle allows
     * one, else null. Either way it counts as seen.
     *
     * @param play false to only note it (units already on the screen after a restart)
     */
    Sfx onScreen(int serial, boolean play) {
        for (int i = 0; i < seenCount; i++) {
            if (seen[i] == serial) {
                return null;
            }
        }
        seen[next] = serial;
        next = (next + 1) % REMEMBERED;
        seenCount = Math.min(seenCount + 1, REMEMBERED);
        if (!play || quiet > 0) {
            return null;
        }
        quiet = throttleSteps;
        Sfx screech = nextIsD ? Sfx.ENEMY_SCREECH_D : Sfx.ENEMY_SCREECH_C;
        nextIsD = !nextIsD;
        return screech;
    }

    /** Forgets every unit and the throttle (a new attempt numbers its units afresh); the alternation goes on. */
    void reset() {
        Arrays.fill(seen, 0);
        seenCount = 0;
        next = 0;
        quiet = 0;
    }
}
