package vanguard.game.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** The act boss's warning banner and death flash timing (design/ui/hud; design/enemies/bosses). */
class BossOverlaysTest {
    @Test
    void theBannerGrowsInShowsFiveSecondsAndGrowsOut() {
        assertFalse(BossBanner.visible(-0.01));
        assertEquals(0.5f, BossBanner.grow(0.1), 1e-6);
        assertEquals(1f, BossBanner.grow(2.5), 1e-6);
        assertEquals(0.5f, BossBanner.grow(4.9), 1e-4);
        assertFalse(BossBanner.visible(5));
    }

    @Test
    void theWarningBlinks() {
        assertTrue(BossBanner.lit(0));
        assertFalse(BossBanner.lit(0.5));
        assertTrue(BossBanner.lit(0.85));
    }

    @Test
    void theFlashHoldsThenFadesAndIsNothingBeforeItStarts() {
        assertEquals(0, ScreenFlash.opacity(-0.1));
        assertEquals(ScreenFlash.OPACITY, ScreenFlash.opacity(0.1));
        assertTrue(ScreenFlash.opacity(0.4) < ScreenFlash.OPACITY);
        assertEquals(0, ScreenFlash.opacity(ScreenFlash.HOLD_SECONDS + ScreenFlash.FADE_SECONDS));
    }
}
