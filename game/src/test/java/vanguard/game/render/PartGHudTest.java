package vanguard.game.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import java.io.File;
import java.util.List;
import org.junit.jupiter.api.Test;
import vanguard.content.Content;
import vanguard.content.ContentLoader;
import vanguard.content.Difficulty;
import vanguard.content.SimSpecs;
import vanguard.game.ui.Fonts;
import vanguard.sim.LevelScript;
import vanguard.sim.Sortie;

/**
 * M5 part G's HUD (design/ui/hud, Level 13): the {@code RELAY} tracker's first line holds the label,
 * an integrity bar wide enough to read with its tick at the secondary's 50 % and the synchronisation's
 * countdown ({@code 2:58} … {@code 0:00}); the bar green, white on a hit, amber below half, a red flash
 * then dark when the relay falls; the second line {@code INTEGRITY} with {@code ≥ 50 %} (a > with a bar
 * under it: the font has no ≥), {@code DONE} or {@code FAILED}, all fitting the well; the hangar intel's
 * wave strip marks the three defend phases and the whiteout.
 */
class PartGHudTest {
    private static final BitmapFont.BitmapFontData BODY = font(Fonts.BODY_FILE);
    private static final Content CONTENT = ContentLoader.fromClasspath();
    private static final String KEY = CONTENT.levelKey(13).orElseThrow();

    @Test
    void theRelayLineHoldsItsLabelABarAndTheCountdown() {
        int label = width(BODY, MissionPanel.RELAY);
        int countdown = width(BODY, MissionPanel.COUNTDOWN_WIDEST);
        int bar = MissionLayout.TEXT_WIDTH - label - countdown - 2 * MissionPanel.RELAY_GAP;
        assertTrue(bar >= 80, "the integrity bar is wide enough to read: " + bar);
        // Every countdown the level shows is no wider than the width kept for it.
        for (String time : List.of("2:58", "1:11", "0:00")) {
            assertTrue(width(BODY, time) <= countdown, time);
        }
    }

    @Test
    void theCountdownRunsFromTheSynchronisationsLengthToZero() {
        LevelScript script = SimSpecs.level(CONTENT, KEY, Difficulty.MEDIUM);
        LevelScript.Defend defend = script.defend().orElseThrow();
        assertEquals(178, defend.duration(), 1e-9, "t=40 to the sync's end at t=218");
        assertEquals("2:58", MissionPanel.countdown(defend.duration()));
        assertEquals("1:00", MissionPanel.countdown(60));
        assertEquals("0:01", MissionPanel.countdown(0.2), "rounded up: 0:00 only at the end");
        assertEquals("0:00", MissionPanel.countdown(0));
        assertEquals("0:00", MissionPanel.countdown(-3));
        Sortie sortie = new Sortie(
                1,
                SimSpecs.starterLoadout(CONTENT, Difficulty.MEDIUM),
                script,
                SimSpecs.rules(CONTENT, KEY, Difficulty.MEDIUM),
                60);
        assertTrue(sortie.defending(), "the tracker the level draws is the relay's");
        assertEquals(defend.duration(), sortie.defendSecondsLeft(), 1e-9, "the full length before the halt");
    }

    @Test
    void theBarIsGreenWhiteOnAHitAmberBelowHalfAndRedThenDarkWhenTheRelayFalls() {
        double half = 0.5;
        assertEquals(
                MissionPanel.relayBar(false, false, 1, half, -1), MissionPanel.relayBar(false, false, 0.5, half, -1));
        assertEquals(Color.WHITE, MissionPanel.relayBar(false, true, 0.8, half, -1), "a hit flashes it white");
        assertEquals(HudKit.AMBER, MissionPanel.relayBar(false, false, 0.49, half, -1), "amber below half");
        Color green = MissionPanel.relayBar(false, false, 0.9, half, -1);
        Color lost = MissionPanel.relayBar(true, false, 0, half, 0);
        Color dark = MissionPanel.relayBar(true, false, 0, half, 200);
        assertTrue(!green.equals(HudKit.AMBER) && !green.equals(Color.WHITE), "green while it holds");
        assertTrue(!lost.equals(dark), "a red flash, then dark");
        assertEquals(dark, MissionPanel.relayBar(true, false, 0, half, 8), "the flash blinks");
    }

    @Test
    void theIntegrityLineFitsItsWell() {
        var secondary = SimSpecs.level(CONTENT, KEY, Difficulty.MEDIUM).secondary();
        assertEquals(0.5, secondary.integrity(), 1e-9);
        assertEquals("INTEGRITY", secondary.label().toUpperCase(java.util.Locale.ROOT));
        assertEquals("> 50 %", MissionPanel.integrityStatus(false, false, 0.5), "drawn as ≥ 50 %");
        assertEquals("DONE", MissionPanel.integrityStatus(true, false, 0.5));
        assertEquals("FAILED", MissionPanel.integrityStatus(false, true, 0.5));
        for (String status : List.of("> 50 %", "DONE", "FAILED")) {
            int width = width(BODY, MissionPanel.INTEGRITY) + 8 + width(BODY, status);
            assertTrue(width <= MissionLayout.TEXT_WIDTH, status + ": " + width);
        }
        assertTrue(MissionPanel.integrityStatus(false, false, 0.5).startsWith(MissionPanel.AT_LEAST));
    }

    @Test
    void theIntelStripMarksTheThreeDefendPhasesAndTheWhiteout() {
        var marks = vanguard.content.campaign.Intel.of(CONTENT, KEY, 3).marks();
        assertEquals(
                List.of(
                        new vanguard.content.campaign.Intel.Mark(40, 100, false),
                        new vanguard.content.campaign.Intel.Mark(100, 160, false),
                        new vanguard.content.campaign.Intel.Mark(160, 220, false),
                        new vanguard.content.campaign.Intel.Mark(176, 188, true)),
                marks,
                "the halt's three sections and the whiteout's peak");
        assertTrue(
                vanguard.content.campaign.Intel.of(CONTENT, CONTENT.levelKey(12).orElseThrow(), 3)
                        .marks()
                        .isEmpty(),
                "a level without a defend has none");
    }

    private static BitmapFont.BitmapFontData font(String file) {
        return new BitmapFont.BitmapFontData(
                new FileHandle(new File(System.getProperty("vanguard.assetsDir"), file)), false);
    }

    private static int width(BitmapFont.BitmapFontData font, String text) {
        int width = 0;
        for (int i = 0; i < text.length(); i++) {
            BitmapFont.Glyph glyph = font.getGlyph(text.charAt(i));
            assertNotNull(glyph, "the font has '" + text.charAt(i) + "'");
            width += glyph.xadvance;
            if (i + 1 < text.length()) {
                width += glyph.getKerning(text.charAt(i + 1));
            }
        }
        return width;
    }
}
