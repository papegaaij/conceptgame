package vanguard.game.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HullBossLooksTest {
    /** The slowest gun's shots are this many steps apart (5 shots/s at 60 steps/s). */
    private static final int SLOWEST_GUN_TICKS = 12;

    @Test
    void aHitTintsAPartBrieflyAndFadesOut() {
        assertEquals(HullBossLooks.HIT_TINT, HullBossLooks.hitTint(0));
        assertEquals(HullBossLooks.HIT_TINT, HullBossLooks.hitTint(HullBossLooks.HIT_HOLD_TICKS - 1));
        int gone = HullBossLooks.HIT_HOLD_TICKS + HullBossLooks.HIT_FADE_TICKS;
        assertTrue(HullBossLooks.hitTint(gone - 1) > 0);
        assertEquals(0, HullBossLooks.hitTint(gone));
        assertEquals(0, HullBossLooks.hitTint(Integer.MAX_VALUE), "never hit");
        assertTrue(HullBossLooks.HIT_TINT < 1, "a tint, not a solid white silhouette");
    }

    @Test
    void underSteadyFireTheTintHoldsInsteadOfStrobing() {
        // Hit every SLOWEST_GUN_TICKS steps: the tint never drops to nothing between two hits.
        for (int t = 0; t < SLOWEST_GUN_TICKS; t++) {
            assertTrue(HullBossLooks.hitTint(t) > 0, "step " + t);
        }
        for (int t = 1; t < SLOWEST_GUN_TICKS; t++) {
            assertTrue(HullBossLooks.hitTint(t) <= HullBossLooks.hitTint(t - 1), "fades without flicker");
        }
    }
}
