package vanguard.game.render;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import vanguard.sim.PlayField;

class BossBarTest {
    @Test
    void thePlainBarIsCentredBelowTheTopEdge() {
        BossBar.Layout plain = BossBar.layout(BossBar.WIDTH, 0, 0, 0, 0, 0);

        assertEquals(40, plain.fillX());
        assertEquals(PlayField.HEIGHT - BossBar.TOP - BossBar.PLAIN_HEIGHT, plain.fillY());
        assertEquals(BossBar.PLAIN_HEIGHT, plain.fillHeight());
        assertEquals(0, plain.plateWidth());
    }

    @Test
    void thePlateWrapsTheFillWhichFillsItsContentBox() {
        // A 20 px plate with 6 px padding at the sides, 5 at the top and 7 at the bottom.
        BossBar.Layout mid = BossBar.layout(BossBar.MID_WIDTH, 6, 6, 5, 7, 20);

        assertEquals(120, mid.fillX());
        assertEquals(BossBar.MID_WIDTH, mid.fillWidth());
        assertEquals(8, mid.fillHeight());
        assertEquals(PlayField.HEIGHT - BossBar.TOP, mid.fillY() + mid.fillHeight(), "the fill's top stays");
        assertEquals(114, mid.plateX());
        assertEquals(BossBar.MID_WIDTH + 12, mid.plateWidth());
        assertEquals(mid.fillY() - 7, mid.plateY());
        assertEquals(20, mid.plateHeight());
    }
}
