package vanguard.game.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.Files.FileType;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import vanguard.content.Content;
import vanguard.content.ContentLoader;
import vanguard.content.LevelData;
import vanguard.game.input.Bindings;
import vanguard.game.level.ControlPrompts;
import vanguard.game.level.PromptTexts;
import vanguard.game.level.RadioQueue;

/**
 * The left panel's regions do not overlap, and what they show fits them, measured with the
 * metrics of the font the HUD draws with (libGDX's built-in font, read without a GL context).
 */
class MissionLayoutTest {
    /** The font file {@code new BitmapFont()} loads. */
    private static final BitmapFont.BitmapFontData FONT = new BitmapFont.BitmapFontData(
            new FileHandle("com/badlogic/gdx/utils/lsans-15.fnt", FileType.Classpath) {}, false);

    private static final Content CONTENT = ContentLoader.fromClasspath();

    @Test
    void theRegionsStackDownThePanelWithAGapAndNoOverlap() {
        List<MissionLayout.Region> regions = MissionLayout.REGIONS;
        assertEquals(MissionLayout.MARGIN, regions.getFirst().top());
        for (int i = 1; i < regions.size(); i++) {
            assertEquals(
                    regions.get(i - 1).bottom() + MissionLayout.GAP,
                    regions.get(i).top(),
                    "region " + i);
        }
        assertTrue(regions.getLast().bottom() <= PixelScreen.HEIGHT - MissionLayout.MARGIN);
    }

    @Test
    void linesOfTheFontFitTheWells() {
        assertEquals(MissionLayout.LINE, FONT.lineHeight);
        // From the top of the capitals down to the lowest descender.
        float drop = MissionLayout.TEXT_DROP + FONT.capHeight - FONT.descent;
        assertTrue(drop <= MissionLayout.WELL, "a line fits its well: " + drop);
        float page = (RadioQueue.PAGE_LINES - 1) * MissionLayout.LINE + drop;
        assertTrue(page <= MissionLayout.PAGE_WELL, "a full radio page fits its well: " + page);
        assertTrue(MissionLayout.PROMPT_LINES <= RadioQueue.PAGE_LINES, "the prompts share the page well's size");
    }

    @Test
    void everyControlPromptFitsItsLine() {
        var texts = new PromptTexts(Bindings.defaults());
        for (LevelData level : CONTENT.levels().values()) {
            List<ControlPrompts.Prompt> prompts = level.controlPrompts().stream()
                    .map(ControlPrompts.Prompt::of)
                    .toList();
            assertTrue(prompts.size() <= MissionLayout.PROMPT_LINES, "at most one line per prompt");
            for (PromptTexts.Text text : texts.of(prompts)) {
                assertFits(text.action(), MissionLayout.PROMPT_ACTION_WIDTH - MissionLayout.PAD);
                assertFits(text.keys(), MissionLayout.TEXT_WIDTH - MissionLayout.PROMPT_ACTION_WIDTH);
            }
        }
    }

    @Test
    void everyRadioLineFitsTheSubtitleAndEverySpeakerBesideThePortrait() {
        List<LevelData.RadioLine> lines = new ArrayList<>();
        for (LevelData level : CONTENT.levels().values()) {
            level.radio()
                    .forEach(cue -> lines.add(new LevelData.RadioLine(cue.speaker(), cue.line(), cue.distorted())));
            level.secrets().forEach(secret -> lines.add(secret.radio()));
        }
        for (LevelData.RadioLine line : lines) {
            for (String typed : RadioQueue.wrap(line.line())) {
                assertFits(typed, MissionLayout.TEXT_WIDTH);
            }
            for (String word : line.speaker().toUpperCase(Locale.ROOT).split(" ", 2)) {
                assertFits(word, MissionLayout.NAME_WIDTH);
            }
        }
    }

    private static void assertFits(String text, int width) {
        int measured = width(text);
        assertTrue(measured <= width, "'" + text + "' is " + measured + " px, more than " + width);
    }

    /** The advance of the text's glyphs with their kerning, as libGDX lays out one line. */
    private static int width(String text) {
        int width = 0;
        for (int i = 0; i < text.length(); i++) {
            BitmapFont.Glyph glyph = FONT.getGlyph(text.charAt(i));
            assertNotNull(glyph, "the font has '" + text.charAt(i) + "'");
            width += glyph.xadvance;
            if (i + 1 < text.length()) {
                width += glyph.getKerning(text.charAt(i + 1));
            }
        }
        return width;
    }
}
