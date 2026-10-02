package vanguard.game.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import java.io.File;
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
import vanguard.game.ui.Fonts;

/**
 * The left panel's regions do not overlap, and what they show fits them, measured with the
 * metrics of the UI kit's fonts the HUD draws with (read without a GL context): the 10x20 body font
 * for the one-line readouts and the speakers' names, the 8x12 label font for the radio pages and
 * the control prompts.
 */
class MissionLayoutTest {
    private static final BitmapFont.BitmapFontData SMALL = font(Fonts.LABEL_FILE);
    private static final BitmapFont.BitmapFontData BODY = font(Fonts.BODY_FILE);

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

    private static BitmapFont.BitmapFontData font(String file) {
        return new BitmapFont.BitmapFontData(
                new FileHandle(new File(System.getProperty("vanguard.assetsDir"), file)), false);
    }

    /** From the top of the capitals down to the lowest descender, below the top of a well. */
    private static float drop(BitmapFont.BitmapFontData font) {
        return MissionLayout.TEXT_DROP + font.capHeight - font.descent;
    }

    @Test
    void linesOfTheFontsFitTheWells() {
        assertTrue(drop(BODY) <= MissionLayout.WELL, "a body line fits its well: " + drop(BODY));
        assertTrue(SMALL.lineHeight <= MissionLayout.LINE, "label lines fit their spacing");
        float page = (RadioQueue.PAGE_LINES - 1) * MissionLayout.LINE + drop(SMALL);
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
                assertFits(SMALL, text.action(), MissionLayout.PROMPT_ACTION_WIDTH - MissionLayout.PAD);
                assertFits(SMALL, text.keys(), MissionLayout.TEXT_WIDTH - MissionLayout.PROMPT_ACTION_WIDTH);
            }
        }
    }

    @Test
    void everyRadioLineFitsTheSubtitleAndEverySpeakerBesideThePortrait() {
        List<LevelData.RadioLine> lines = new ArrayList<>();
        for (LevelData level : CONTENT.levels().values()) {
            level.radio()
                    .forEach(cue -> lines.add(
                            new LevelData.RadioLine(cue.speaker(), cue.line(), cue.distorted(), cue.expression())));
            level.secrets().forEach(secret -> lines.add(secret.radio()));
        }
        for (LevelData.RadioLine line : lines) {
            for (String typed : RadioQueue.wrap(line.line())) {
                assertFits(SMALL, typed, MissionLayout.TEXT_WIDTH);
            }
            for (String word : line.speaker().toUpperCase(Locale.ROOT).split(" ", 2)) {
                assertFits(BODY, word, MissionLayout.NAME_WIDTH);
            }
        }
    }

    private static void assertFits(BitmapFont.BitmapFontData font, String text, int width) {
        int measured = width(font, text);
        assertTrue(measured <= width, "'" + text + "' is " + measured + " px, more than " + width);
    }

    /** The advance of the text's glyphs with their kerning, as libGDX lays out one line. */
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
