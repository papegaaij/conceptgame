package vanguard.game.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import java.io.File;
import java.util.List;
import org.junit.jupiter.api.Test;
import vanguard.content.Content;
import vanguard.content.ContentLoader;
import vanguard.content.Difficulty;
import vanguard.content.LevelData;
import vanguard.content.SimSpecs;
import vanguard.game.ui.Fonts;
import vanguard.sim.LevelScript;
import vanguard.sim.PlayField;
import vanguard.sim.Sortie;

/**
 * M5 part F's HUD (design/ui/hud, Level 12): {@code SHIELD DRAIN} fits the shield gauge's label
 * plate; the {@code SONDES n / 3} tracker fits its one-line well with {@code DONE} and {@code FAILED},
 * flashes green once per sonde collected, and counts the level's three sondes; the shake-off's event
 * prompt shows in the play field below the ship in a box sized to its line, on every latch of at least
 * 0.3 s for at most 6 s, until the attempt's first shake-off.
 */
class PartFHudTest {
    private static final BitmapFont.BitmapFontData SMALL = font(Fonts.LABEL_FILE);
    private static final BitmapFont.BitmapFontData BODY = font(Fonts.BODY_FILE);
    private static final Content CONTENT = ContentLoader.fromClasspath();
    private static final String KEY = CONTENT.levelKey(12).orElseThrow();
    /** The label plate (assets/sprites/hud/plate.png) is 120 px wide, its text indented 4 px. */
    private static final int PLATE_WIDTH = 120;

    @Test
    void shieldDrainFitsTheLabelPlate() {
        assertEquals("SHIELD DRAIN", ShipPanel.SHIELD_DRAIN);
        int width = width(SMALL, ShipPanel.SHIELD_DRAIN);
        assertTrue(width <= PLATE_WIDTH - 2 * HudKit.PLATE_INDENT, "on the plate: " + width);
        assertEquals(HudKit.AMBER, ShipPanel.DRAIN_COLOUR, "in the warning colour");
    }

    @Test
    void theSondesTrackerFitsItsWellAndCountsTheLevelsSondes() {
        LevelScript.Secondary secondary =
                SimSpecs.level(CONTENT, KEY, Difficulty.MEDIUM).secondary();
        assertTrue(secondary.sondes(), "collect: sondes");
        assertEquals("SONDES", secondary.label());
        for (String count : List.of("0 / 3", "3 / 3", "DONE", "FAILED")) {
            int width = width(BODY, secondary.label()) + 8 + width(BODY, count);
            assertTrue(width <= MissionLayout.TEXT_WIDTH, count + ": " + width);
        }
        Sortie sortie = new Sortie(
                1,
                SimSpecs.starterLoadout(CONTENT, Difficulty.MEDIUM),
                SimSpecs.level(CONTENT, KEY, Difficulty.MEDIUM),
                SimSpecs.rules(CONTENT, KEY, Difficulty.MEDIUM),
                60);
        assertTrue(sortie.secondaryByEscapes(), "the escapes tracker's line: label and count");
        assertEquals(3, sortie.escapesTotal(), "SONDES 0 / 3");
        assertEquals(0, sortie.escapesDestroyed());
    }

    @Test
    void aCollectedSondeFlashesTheTrackerGreenOnce() {
        assertFalse(MissionPanel.collectFlash(-1));
        assertTrue(MissionPanel.collectFlash(0), "at once");
        assertFalse(MissionPanel.collectFlash(8), "blinking");
        assertTrue(MissionPanel.collectFlash(16));
        assertFalse(MissionPanel.collectFlash(MissionPanel.COLLECT_FLASH_FRAMES), "over");
    }

    @Test
    void theShakeOffPromptsBoxFitsItsOneLineInThePlayField() {
        LevelData.Prompt prompt = CONTENT.level(KEY).prompts().orElseThrow().stream()
                .filter(LevelData.Prompt::onEvent)
                .findFirst()
                .orElseThrow();
        assertEquals("SHAKE OFF", prompt.action());
        assertEquals("LEFT-RIGHT", prompt.keys());
        assertTrue(Double.isNaN(prompt.t()), "never due by the clock");
        assertEquals(6, prompt.seconds(), 1e-9, "at most 6 s each time");
        int text = width(BODY, prompt.action()) + EventPrompt.GAP + width(BODY, prompt.keys());
        int box = EventPrompt.width(text);
        assertTrue(box <= PlayField.WIDTH - 2 * 40, "well inside the play field: " + box);
        // One line: the box is the body font's capitals with 4 px above and below, not a three-line well.
        assertEquals(BODY.capHeight + 8, EventPrompt.HEIGHT, 1, "sized to its line");
        assertTrue(EventPrompt.HEIGHT < 2 * BODY.lineHeight, "one line");
    }

    @Test
    void theShakeOffPromptSitsBelowTheShipInsideThePlayField() {
        assertEquals(96 - EventPrompt.BELOW_SHIP, EventPrompt.centreY(96), "below the ship at its start");
        assertEquals(300 - EventPrompt.BELOW_SHIP, EventPrompt.centreY(300), "and follows it up");
        assertEquals(EventPrompt.MIN_Y, EventPrompt.centreY(40), "never below the bottom edge");
        assertTrue(EventPrompt.MIN_Y - EventPrompt.HEIGHT / 2 >= 0);
        // Under the hull and a Lamprey at its tail spot (35 px below the ship's centre).
        assertTrue(EventPrompt.BELOW_SHIP - EventPrompt.HEIGHT / 2 > 35 + 16);
    }

    @Test
    void theShakeOffPromptShowsOnEveryLatchUntilTheFirstShakeOff() {
        assertFalse(EventPrompt.shows(Double.NaN, 6, false), "nothing latched");
        assertFalse(EventPrompt.shows(0.2, 6, false), "a latch shorter than 0.3 s raises none");
        assertTrue(EventPrompt.shows(0.3, 6, false));
        assertTrue(EventPrompt.shows(6.2, 6, false));
        assertFalse(EventPrompt.shows(6.3, 6, false), "at most 6 s each time");
        assertFalse(EventPrompt.shows(1, 6, true), "never after the attempt's first shake-off");
        EventPrompt prompt = new EventPrompt(null, null, "SHAKE OFF", "LEFT-RIGHT", 6);
        assertFalse(prompt.showing(10));
        prompt.latched(10);
        prompt.step(1);
        assertFalse(prompt.showing(10.2));
        assertTrue(prompt.showing(10.5), "the first latch");
        prompt.latched(11);
        assertTrue(prompt.showing(11), "a second Lamprey on the same latch");
        assertFalse(prompt.showing(16.4), "its 6 s are over");
        prompt.step(0);
        prompt.latched(20);
        assertTrue(prompt.showing(20.5), "the next latch shows it again");
        prompt.step(0);
        assertFalse(prompt.showing(20.6), "the latch ended (a Lamprey shot off)");
        prompt.latched(30);
        prompt.shaken();
        prompt.step(0);
        prompt.latched(40);
        assertFalse(prompt.showing(41), "shaken off once: never again this attempt");
        prompt.reset();
        prompt.latched(50);
        assertTrue(prompt.showing(51), "a new attempt");
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
