package vanguard.game.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import vanguard.content.Content;
import vanguard.content.ContentLoader;
import vanguard.content.Difficulty;
import vanguard.content.SimSpecs;
import vanguard.sim.Ally;
import vanguard.sim.Enemy;
import vanguard.sim.LevelScript;
import vanguard.sim.Sortie;

/**
 * Level 10 flown without input (the ship invulnerable, so the escort never fails): what the renderer
 * reads of M5 part D's simulation. A Wraith is a shimmer on high-air (no shadow) through its swoop,
 * the gap and its rise, and on the play plane from its decloak on, its body fading in over the flash;
 * the shuttles stand on their pads under the low-air layer, lift off, fly their stations with the
 * flyers and climb out; Lifeline Three's glow pulses from 116 s and the lance takes it at 118.
 */
class Level10LooksTest {
    private static final Content CONTENT = ContentLoader.fromClasspath();

    private static Sortie sortie() {
        String key = CONTENT.levelKey(10).orElseThrow();
        return new Sortie(
                1,
                SimSpecs.starterLoadout(CONTENT, Difficulty.MEDIUM),
                SimSpecs.level(CONTENT, key, Difficulty.MEDIUM),
                SimSpecs.rules(CONTENT, key, Difficulty.MEDIUM).withInvulnerableShip(),
                60);
    }

    @Test
    void aWraithIsAShimmerOnHighAirUntilItDecloaksThenItsBodyFadesIn() {
        Sortie sortie = sortie();
        Set<Enemy.AmbushPhase> seen = EnumSet.noneOf(Enemy.AmbushPhase.class);
        boolean faded = false;
        for (int t = 0; t < 60 * 70 && !sortie.complete(); t++) {
            sortie.step(0);
            for (int i = 0; i < sortie.enemyCount(); i++) {
                Enemy enemy = sortie.enemy(i);
                Enemy.AmbushPhase phase = enemy.ambushPhase();
                if (phase == Enemy.AmbushPhase.NONE) {
                    continue;
                }
                seen.add(phase);
                LevelRenderer.Depth depth = LevelRenderer.Depth.of(enemy);
                switch (phase) {
                    case SWOOP, GAP, RISE -> {
                        assertEquals(LevelRenderer.Depth.HIGH_AIR, depth, phase + " at step " + t);
                        assertEquals(0, enemy.decloak(1), 1e-9);
                    }
                    case DECLOAK -> {
                        assertEquals(LevelRenderer.Depth.AIR, depth, "on air from the flash's start");
                        double progress = enemy.decloak(0);
                        assertTrue(progress >= 0 && progress < 1, "the flash plays: " + progress);
                        faded |= EnemyLooks.bodyOpacity(progress) > 0.4
                                && EnemyLooks.bodyOpacity(progress) < 0.6
                                && EnemyLooks.shimmerOpacity(progress) == 0;
                    }
                    default -> {
                        assertEquals(LevelRenderer.Depth.AIR, depth, phase.name());
                        assertEquals(1, enemy.decloak(1), 1e-9);
                    }
                }
            }
        }
        assertTrue(
                seen.containsAll(EnumSet.of(
                        Enemy.AmbushPhase.SWOOP,
                        Enemy.AmbushPhase.RISE,
                        Enemy.AmbushPhase.DECLOAK,
                        Enemy.AmbushPhase.HOLD)),
                "the first Wraith's way by 70 s: " + seen);
        assertTrue(faded, "the body half in, the shimmer gone, mid-flash");
    }

    @Test
    void theShuttlesLiftOffFlyTheirStationsAndLifelineThreeIsLostToTheLance() {
        Sortie sortie = sortie();
        LevelScript.ScriptedLoss loss = sortie.script()
                .escort()
                .flatMap(LevelScript.Escort::air)
                .flatMap(LevelScript.Air::scriptedLoss)
                .orElseThrow();
        int scripted = sortie.scriptedAlly();
        assertEquals(2, scripted, "Lifeline Three");
        boolean padsLow = false;
        boolean liftedHigh = false;
        boolean glowed = false;
        boolean struck = false;
        for (int t = 0; t < 60 * 125 && !sortie.complete(); t++) {
            sortie.step(0);
            double seconds = sortie.levelSeconds();
            for (int k = 0; k < sortie.allyCount(); k++) {
                Ally ally = sortie.ally(k);
                switch (ally.state()) {
                    case PAD -> {
                        assertTrue(ShuttleLooks.low(ally, 1), "on its pad under the low-air layer");
                        padsLow = true;
                    }
                    case LIFTING -> {
                        boolean low = ally.lift(1) < ShuttleLooks.LOW_LIFT;
                        assertEquals(low, ShuttleLooks.low(ally, 1));
                        liftedHigh |= !low;
                    }
                    case FLYING -> {
                        assertFalse(ShuttleLooks.low(ally, 1), "with the flyers");
                        int bank = ShuttleLooks.bankFrame(ally.bankVelocity(), 60, 5);
                        assertTrue(bank >= 0 && bank < 5);
                    }
                    case WRECK -> assertTrue(ShuttleLooks.low(ally, 1), "the glide under the low-air layer");
                    default -> {}
                }
            }
            float glow = LossLooks.intensity(seconds - (loss.t() - loss.glow()), loss.glow());
            if (seconds > loss.t() - loss.glow() + 0.05 && seconds < loss.t()) {
                assertTrue(glow > 0, "the glow pulses over its unit at " + seconds);
                assertFalse(sortie.ally(scripted).lost(), "not lost before the hit");
                glowed = true;
            }
            if (seconds > loss.t() + 0.05 && seconds < loss.t() + 3) {
                assertTrue(sortie.ally(scripted).lost(), "the lance took it at " + seconds);
                struck = true;
            }
        }
        assertTrue(padsLow && liftedHigh && glowed && struck);
    }
}
