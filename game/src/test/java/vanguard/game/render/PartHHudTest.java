package vanguard.game.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import java.io.File;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import vanguard.content.Content;
import vanguard.content.ContentLoader;
import vanguard.content.Difficulty;
import vanguard.content.SimSpecs;
import vanguard.content.campaign.Intel;
import vanguard.game.ui.Fonts;
import vanguard.sim.LevelScript;
import vanguard.sim.Sortie;

/**
 * M5 part H's HUD (design/ui/hud, Level 14): the one-line tracker {@code ROOTS n / 8} (easy {@code n / 4}),
 * the root-burst growths destroyed of all, then {@code DONE} or {@code FAILED}, its label from the data
 * and every state fitting the well; the boss bar's name; the hangar intel's strip marks the root field.
 */
class PartHHudTest {
    private static final BitmapFont.BitmapFontData BODY = font(Fonts.BODY_FILE);
    private static final Content CONTENT = ContentLoader.fromClasspath();
    private static final String KEY = CONTENT.levelKey(14).orElseThrow();

    @Test
    void theRootsTrackerCountsTheGrowthsOfTheDifficulty() {
        for (Difficulty difficulty : Difficulty.values()) {
            LevelScript script = SimSpecs.level(CONTENT, KEY, difficulty);
            LevelScript.Secondary secondary = script.secondary();
            assertTrue(secondary.byGroups(), "a groups secondary");
            assertEquals("ROOTS", secondary.label().toUpperCase(Locale.ROOT), "the label from the data");
            Sortie sortie = new Sortie(
                    1,
                    SimSpecs.starterLoadout(CONTENT, difficulty),
                    script,
                    SimSpecs.rules(CONTENT, KEY, difficulty),
                    60);
            int growths = difficulty == Difficulty.EASY ? 4 : 8;
            assertEquals(growths, sortie.groupUnits(0), difficulty + ": the growths of all four bursts");
            assertEquals(0, sortie.groupUnitsDestroyed(0));
            assertEquals(
                    "0 / " + growths,
                    MissionPanel.unitsCount(
                            sortie.groupUnitsDestroyed(0),
                            sortie.groupUnits(0),
                            sortie.secondaryMet(),
                            sortie.secondaryFailed()));
        }
        assertEquals("3 / 8", MissionPanel.unitsCount(3, 8, false, false));
        assertEquals("DONE", MissionPanel.unitsCount(8, 8, true, false));
        assertEquals("FAILED", MissionPanel.unitsCount(5, 8, false, true));
    }

    @Test
    void theRootsLineFitsItsWell() {
        for (String status : List.of("8 / 8", "DONE", "FAILED")) {
            int width = width(BODY, "ROOTS") + 8 + width(BODY, status);
            assertTrue(width <= MissionLayout.TEXT_WIDTH, status + ": " + width);
        }
    }

    @Test
    void theBossBarNamesTheSpire() {
        LevelScript.SetPieceSpec boss = SimSpecs.level(CONTENT, KEY, Difficulty.MEDIUM).setPieces().stream()
                .filter(LevelScript.SetPieceSpec::isBoss)
                .findFirst()
                .orElseThrow();
        assertEquals("SIEGE SPIRE", boss.boss().orElseThrow().barName());
        assertTrue(!boss.boss().orElseThrow().midBoss(), "the act boss's long bar");
    }

    @Test
    void theIntelStripMarksTheRootField() {
        List<Intel.Mark> marks = Intel.of(CONTENT, KEY, 3).marks();
        assertTrue(
                marks.stream().anyMatch(mark -> mark.from() <= 63 && mark.to() >= 92),
                "the root field's bursts (t=63-92) are marked: " + marks);
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
