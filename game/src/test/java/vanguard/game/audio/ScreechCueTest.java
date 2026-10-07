package vanguard.game.audio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import vanguard.content.Content;
import vanguard.content.ContentLoader;
import vanguard.content.Difficulty;
import vanguard.content.SimSpecs;
import vanguard.sim.Enemy;
import vanguard.sim.PlayField;
import vanguard.sim.SimStep;
import vanguard.sim.Sortie;

/** The Vrell screech as a large unit enters the screen (design/audio/sfx, Enemies). */
class ScreechCueTest {
    private static final int THROTTLE = SimStep.ticks(ScreechCue.THROTTLE_SECONDS);

    @Test
    void theLargeVrellUnitsScreechAndNoOtherKind() {
        for (String slug : List.of("mantis", "coilwyrm", "spore-bomber", "scuttler", "creeper")) {
            assertTrue(ScreechCue.screeches(slug), slug);
        }
        for (String slug : List.of(
                "skitter",
                "needler",
                "brood-pod",
                "coilwyrm-segment",
                "coilwyrm-tail",
                "coilwyrm-regrown",
                "polyp-mortar",
                "spine-turret")) {
            assertFalse(ScreechCue.screeches(slug), slug);
        }
    }

    @Test
    void aUnitScreechesOnceAndTheScreechesAlternate() {
        var cue = new ScreechCue(0);

        assertEquals(Sfx.ENEMY_SCREECH_C, cue.onScreen(7, true));
        for (int step = 0; step < 10; step++) {
            cue.step();
            assertNull(cue.onScreen(7, true), "still on the screen, or back on it");
        }
        assertEquals(Sfx.ENEMY_SCREECH_D, cue.onScreen(8, true));
        cue.step();
        assertEquals(Sfx.ENEMY_SCREECH_C, cue.onScreen(9, true));
    }

    @Test
    void oneScreechEveryThreeSecondsAtMost() {
        var cue = new ScreechCue(THROTTLE);

        assertEquals(Sfx.ENEMY_SCREECH_C, cue.onScreen(1, true));
        assertNull(cue.onScreen(2, true), "the wave's second unit, in the same step");
        for (int step = 1; step < THROTTLE; step++) {
            cue.step();
            assertNull(cue.onScreen(100 + step, true), "step " + step);
        }
        cue.step();
        assertEquals(Sfx.ENEMY_SCREECH_D, cue.onScreen(3, true), "3 s after the last");
        for (int step = 0; step < 2 * THROTTLE; step++) {
            cue.step();
            assertNull(cue.onScreen(2, true), "a unit that entered silenced stays silent");
        }
    }

    @Test
    void aRestartForgetsTheUnitsButUnitsAlreadyThereStaySilent() {
        var cue = new ScreechCue(THROTTLE);
        assertEquals(Sfx.ENEMY_SCREECH_C, cue.onScreen(1, true));

        cue.reset();
        assertNull(cue.onScreen(5, false), "on the screen at the restart: noted only");
        assertNull(cue.onScreen(5, true));
        assertEquals(Sfx.ENEMY_SCREECH_D, cue.onScreen(1, true), "the new attempt's unit 1, no throttle left");
    }

    /**
     * Level 06 flown without input (Mantises, Coilwyrms, Spore Bombers): a screech plays exactly in
     * the steps where a screeching unit comes onto the screen for the first time and the last
     * screech is at least 3 s old, it is that unit's, no unit screeches twice, and c and d alternate.
     */
    @Test
    void inFarsideTheLargeVrellUnitsScreechAsTheyEnter() {
        Content content = ContentLoader.fromClasspath();
        String key = content.levelKey(6).orElseThrow();
        Sortie sortie = new Sortie(
                1,
                SimSpecs.starterLoadout(content, Difficulty.MEDIUM),
                SimSpecs.level(content, key, Difficulty.MEDIUM),
                SimSpecs.rules(content, key, Difficulty.MEDIUM).withInvulnerableShip(),
                60);
        var cue = new ScreechCue(THROTTLE);
        Set<Integer> entered = new HashSet<>();
        Set<Integer> screeched = new HashSet<>();
        int screeches = 0;
        int last = -THROTTLE;
        Sfx lastSound = null;
        for (int t = 0; t < 60 * 240 && !sortie.complete(); t++) {
            sortie.step(0);
            Set<Integer> newcomers = new HashSet<>();
            for (int i = 0; i < sortie.enemyCount(); i++) {
                Enemy enemy = sortie.enemy(i);
                String slug = sortie.enemyKinds().get(enemy.kind()).slug();
                if (ScreechCue.screeches(slug)
                        && PlayField.overlaps(enemy.renderX(1), enemy.renderY(1), enemy.hitbox())
                        && entered.add(enemy.serial())) {
                    newcomers.add(enemy.serial());
                }
            }
            Sfx sound = cue.watch(sortie, t == 0);
            boolean due = t > 0 && !newcomers.isEmpty() && t - last >= THROTTLE;
            assertEquals(due, sound != null, "step " + t + ", newcomers " + newcomers + ", last screech " + last);
            if (sound != null) {
                assertTrue(sound != lastSound, "alternating at step " + t);
                int serial = serialAt(sortie, cue.x(), newcomers);
                assertTrue(screeched.add(serial), "unit " + serial + " screeched twice");
                lastSound = sound;
                last = t;
                screeches++;
            }
        }
        assertTrue(screeches >= 2, screeches + " screeches for " + entered.size() + " units");
    }

    /** The serial of the newcomer at {@code x}. */
    private static int serialAt(Sortie sortie, double x, Set<Integer> newcomers) {
        for (int i = 0; i < sortie.enemyCount(); i++) {
            Enemy enemy = sortie.enemy(i);
            if (enemy.renderX(1) == x && newcomers.contains(enemy.serial())) {
                return enemy.serial();
            }
        }
        throw new AssertionError("no unit entered at x " + x);
    }
}
