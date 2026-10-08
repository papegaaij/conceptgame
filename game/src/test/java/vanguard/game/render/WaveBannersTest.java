package vanguard.game.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import java.io.File;
import org.junit.jupiter.api.Test;
import vanguard.content.ContentLoader;
import vanguard.content.Difficulty;
import vanguard.content.SimSpecs;
import vanguard.game.ui.Fonts;
import vanguard.sim.LevelScript;
import vanguard.sim.PlayField;
import vanguard.sim.SimStep;
import vanguard.sim.WarningEdge;

class WaveBannersTest {
    private static final int LEFT = WarningEdge.LEFT.bit();
    private static final int RIGHT = WarningEdge.RIGHT.bit();
    private static final int REAR = WarningEdge.BOTTOM.bit();
    private static final int BANNER_TICKS = SimStep.ticks(WaveBanners.SECONDS);

    private final WaveBanners banners = new WaveBanners(null, null, false);

    @Test
    void theWarnedEdgeNamesTheBanner() {
        assertEquals("WARNING — HOSTILES FROM THE REAR", WaveBanners.text(REAR));
        assertEquals("WARNING — HOSTILES FROM THE LEFT", WaveBanners.text(LEFT));
        assertEquals("WARNING — HOSTILES FROM THE RIGHT", WaveBanners.text(RIGHT));
        assertEquals("WARNING — HOSTILES FROM THE LEFT AND RIGHT", WaveBanners.text(LEFT | RIGHT));
        assertEquals("WARNING — HOSTILES FROM THE REAR AND LEFT", WaveBanners.text(REAR | LEFT));
        assertEquals("WARNING — HOSTILES ON ALL SIDES", WaveBanners.text(REAR | LEFT | RIGHT));
    }

    @Test
    void everyBannerFitsItsBandInTheBodyFont() {
        BitmapFont.BitmapFontData body = new BitmapFont.BitmapFontData(
                new FileHandle(new File(System.getProperty("vanguard.assetsDir"), Fonts.BODY_FILE)), false);
        for (int edges = 1; edges < 8; edges++) {
            String text = WaveBanners.text(edges);
            int width = 0;
            for (int i = 0; i < text.length(); i++) {
                BitmapFont.Glyph glyph = body.getGlyph(text.charAt(i));
                assertNotNull(glyph, "the font has '" + text.charAt(i) + "'");
                width += glyph.xadvance;
            }
            assertTrue(
                    width <= WaveBanners.WIDTH - 2 * WaveBanners.TEXT_INSET,
                    "'" + text + "' is " + width + " px, more than its band");
            assertTrue(body.capHeight <= WaveBanners.HEIGHT - 8, "the capitals fit the band's height");
        }
    }

    @Test
    void theBandSitsInThePlayFieldAboveTheBossBanner() {
        assertTrue(WaveBanners.WIDTH <= PlayField.WIDTH);
        assertTrue(WaveBanners.CENTRE_Y - WaveBanners.HEIGHT / 2 > 300 + 108 / 2, "clear of the boss banner's band");
        assertTrue(WaveBanners.CENTRE_Y + WaveBanners.HEIGHT / 2 < PlayField.HEIGHT - 40, "clear of the boss bar");
    }

    /**
     * 2026-10-08 (the round 32 capture: the rear warning over Lifeline One): with an air escort the
     * band sits above Level 10's shuttle stations, their sway and their sprites included, and stays
     * clear of the boss bar; without one it keeps its place.
     */
    @Test
    void withAnAirEscortTheBandClearsTheShuttleStations() {
        LevelScript.Escort escort = SimSpecs.level(
                        ContentLoader.fromClasspath(),
                        "act-2-homefront/level-10-evacuation-corridor",
                        Difficulty.MEDIUM)
                .escort()
                .orElseThrow();
        double highest = 0;
        for (LevelScript.Station station : escort.air().orElseThrow().stations()) {
            highest = Math.max(
                    highest,
                    station.y()
                            + Math.abs(station.swayY())
                            + escort.ally().size().height() / 2);
        }
        int bottom = WaveBanners.centreY(true) - WaveBanners.HEIGHT / 2;
        assertTrue(bottom > highest + 8, "the band's bottom " + bottom + " over the stations' top " + highest);
        assertTrue(WaveBanners.centreY(true) + WaveBanners.HEIGHT / 2 < PlayField.HEIGHT - 40, "clear of the boss bar");
        assertEquals(WaveBanners.CENTRE_Y, WaveBanners.centreY(false));
    }

    @Test
    void aBannerOpensWithItsEdgeWarningAndShowsForTwoAndAHalfSeconds() {
        banners.step(REAR, REAR, 100);
        assertEquals(WaveBanners.text(REAR), banners.shown());

        banners.step(REAR, 0, 100 + BANNER_TICKS - 1);
        assertEquals(WaveBanners.text(REAR), banners.shown());
        banners.step(REAR, 0, 100 + BANNER_TICKS);
        assertEquals("", banners.shown());
    }

    @Test
    void oneBannerAtATimeTheNextWaitsForIt() {
        banners.step(LEFT, LEFT, 100);
        banners.step(LEFT | REAR, REAR, 130);
        assertEquals(WaveBanners.text(LEFT), banners.shown(), "the rear waits");

        banners.step(REAR, 0, 100 + BANNER_TICKS);

        assertEquals(WaveBanners.text(REAR), banners.shown());
    }

    @Test
    void aWaitingBannerIsDroppedOnceItsWarningIsOver() {
        banners.step(LEFT, LEFT, 100);
        banners.step(LEFT | RIGHT, RIGHT, 110);
        banners.step(LEFT, 0, 120);

        banners.step(0, 0, 100 + BANNER_TICKS);

        assertEquals("", banners.shown());
    }

    @Test
    void edgesStartingTogetherShareOneBanner() {
        banners.step(LEFT | RIGHT, LEFT | RIGHT, 100);

        assertEquals(WaveBanners.text(LEFT | RIGHT), banners.shown());
    }

    @Test
    void aRestartClearsIt() {
        banners.step(REAR, REAR, 100);

        banners.clear();

        assertEquals("", banners.shown());
    }

    @Test
    void itOpensAndFadesOut() {
        assertEquals(0, WaveBanners.grow(0));
        assertEquals(1, WaveBanners.grow(WaveBanners.GROW_SECONDS));
        assertEquals(1, WaveBanners.opacity(1));
        assertTrue(WaveBanners.opacity(WaveBanners.SECONDS - 0.1) < 1);
        assertEquals(0, WaveBanners.opacity(WaveBanners.SECONDS));
    }
}
