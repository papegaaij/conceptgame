package vanguard.game.ui;

import com.badlogic.gdx.Files;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.utils.Disposable;

/**
 * The UI kit's three bitmap fonts (design/ui, Shared UI rules): 8x12 labels, 10x20 body text and
 * 20x30 headings, monospaced, white and tinted when drawn. Rendered by {@code tools/art/fonts.py}
 * (production art) from the chosen UI kit's font specimen.
 */
public final class Fonts implements Disposable {
    public static final String LABEL_FILE = "fonts/label-8x12.fnt";
    public static final String BODY_FILE = "fonts/body-10x20.fnt";
    public static final String HEADING_FILE = "fonts/heading-20x30.fnt";

    public final BitmapFont label;
    public final BitmapFont body;
    public final BitmapFont heading;

    public Fonts(Files files) {
        label = load(files, LABEL_FILE);
        body = load(files, BODY_FILE);
        heading = load(files, HEADING_FILE);
    }

    private static BitmapFont load(Files files, String path) {
        BitmapFont font = new BitmapFont(files.internal(path));
        font.getRegion().getTexture().setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
        font.setUseIntegerPositions(true);
        return font;
    }

    /** The width of a text in a font at its scale: all of its glyphs advance by the cell width. */
    public static int width(BitmapFont font, String text) {
        return Math.round(text.length() * advance(font) * font.getScaleX());
    }

    /** The cell width of a font. */
    public static int advance(BitmapFont font) {
        return font.getData().getGlyph('M').xadvance;
    }

    @Override
    public void dispose() {
        label.dispose();
        body.dispose();
        heading.dispose();
    }
}
