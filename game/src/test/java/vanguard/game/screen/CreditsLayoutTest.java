package vanguard.game.screen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import vanguard.game.render.PixelScreen;
import vanguard.game.ui.CreditsRoll;
import vanguard.game.ui.Fonts;

/**
 * The credits roll in assets/ui/credits.txt, laid out as the credits screen does, fits its column:
 * no line is wider than the column's text width in the UI kit's fonts (read without a GL context),
 * every character is in its font, and the column lies on the screen.
 */
class CreditsLayoutTest {
    private static final String ASSETS = System.getProperty("vanguard.assetsDir", "../assets");

    private static final Map<CreditsRoll.Size, BitmapFont.BitmapFontData> FONTS = new EnumMap<>(Map.of(
            CreditsRoll.Size.LABEL, font(Fonts.LABEL_FILE),
            CreditsRoll.Size.BODY, font(Fonts.BODY_FILE),
            CreditsRoll.Size.HEADING, font(Fonts.HEADING_FILE)));

    private static BitmapFont.BitmapFontData font(String file) {
        return new BitmapFont.BitmapFontData(new FileHandle(new File(ASSETS, file)), false);
    }

    private static CreditsRoll roll(String file) {
        return CreditsRoll.layout(
                file, CreditsScreen.TEXT_WIDTH, size -> FONTS.get(size).getGlyph('M').xadvance, 119);
    }

    private static String committedRoll() throws IOException {
        return Files.readString(Path.of(ASSETS, CreditsScreen.FILE), StandardCharsets.UTF_8);
    }

    @Test
    void theColumnLiesOnTheScreen() {
        assertTrue(CreditsScreen.COLUMN_X >= 0);
        assertTrue(CreditsScreen.COLUMN_X + CreditsScreen.COLUMN_WIDTH <= PixelScreen.WIDTH);
        assertEquals(PixelScreen.WIDTH, 2 * CreditsScreen.COLUMN_X + CreditsScreen.COLUMN_WIDTH, "centred");
        assertTrue(CreditsScreen.TOP + 2 * CreditsScreen.FADE < CreditsScreen.BOTTOM);
    }

    @Test
    void noLineOfTheRollIsWiderThanTheColumn() throws IOException {
        CreditsRoll roll = roll(committedRoll());
        assertTrue(
                roll.lines().size() > 50,
                "the roll has its lines: " + roll.lines().size());
        for (CreditsRoll.Line line : roll.lines()) {
            if (line.kind() == CreditsRoll.Kind.LOGO) {
                continue;
            }
            int width = width(FONTS.get(line.kind().size), line.text());
            assertTrue(
                    width <= CreditsScreen.TEXT_WIDTH,
                    "'" + line.text() + "' is " + width + " px, more than " + CreditsScreen.TEXT_WIDTH);
        }
    }

    @Test
    void theRollCarriesTheAttributions() throws IOException {
        List<CreditsRoll.Line> lines = roll(committedRoll()).lines();
        assertTrue(lines.stream()
                .anyMatch(line ->
                        line.kind() == CreditsRoll.Kind.DETAIL && line.text().startsWith("CC-BY")));
        assertTrue(lines.stream()
                .anyMatch(line ->
                        line.kind() == CreditsRoll.Kind.TITLE && line.text().equals("FONTS")));
    }

    @Test
    void longTextsWrapToTheColumnAndLinesStackDown() {
        CreditsRoll roll = roll("logo|\ngap|\ntitle|SOUND\ntext|" + "word ".repeat(60) + "\nitem|a\n");
        List<CreditsRoll.Line> lines = roll.lines();
        assertEquals(CreditsRoll.Kind.LOGO, lines.getFirst().kind());
        assertTrue(lines.stream()
                        .filter(line -> line.kind() == CreditsRoll.Kind.TEXT)
                        .count()
                > 1);
        for (int i = 1; i < lines.size(); i++) {
            assertTrue(lines.get(i).y() > lines.get(i - 1).y(), "lines stack down");
        }
        assertTrue(roll.height() > lines.getLast().y());
    }

    @Test
    void theHintsFitTheScreen() {
        BitmapFont.BitmapFontData label = FONTS.get(CreditsRoll.Size.LABEL);
        assertTrue(width(label, CreditsScreen.HINTS) + 20 <= PixelScreen.WIDTH);
        assertTrue(width(label, CreditsScreen.END_HINTS) + 20 <= PixelScreen.WIDTH);
    }

    /** The advance of the text's glyphs with their kerning, as libGDX lays out one line. */
    private static int width(BitmapFont.BitmapFontData font, String text) {
        int width = 0;
        for (int i = 0; i < text.length(); i++) {
            BitmapFont.Glyph glyph = font.getGlyph(text.charAt(i));
            assertNotNull(glyph, "the font has '" + text.charAt(i) + "' (" + text + ")");
            width += glyph.xadvance;
            if (i + 1 < text.length()) {
                width += glyph.getKerning(text.charAt(i + 1));
            }
        }
        return width;
    }
}
