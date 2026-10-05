package vanguard.game.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.ToIntFunction;

/**
 * The credits roll of design/ui/credits, laid out for a column of the screen: the entries of
 * {@code ui/credits.txt} (written from CREDITS.md by {@code :pipeline:credits}), one {@code kind|text}
 * per line, word-wrapped to the column in their fonts and stacked down from y = 0. The kinds: the
 * logo, a gap, a section title (amber heading), a role (amber label), a name (white body), an item
 * (white body: an asset's title and author), its detail (cyan label: licence and source) and text
 * (label).
 */
public final class CreditsRoll {
    /** The kinds of entry, each with its font, colour and the space below a line. */
    public enum Kind {
        LOGO(Size.LABEL, Glass.WHITE, 24),
        GAP(Size.LABEL, Glass.WHITE, 24),
        TITLE(Size.HEADING, Glass.AMBER, 16),
        ROLE(Size.LABEL, Glass.AMBER, 8),
        NAME(Size.BODY, Glass.WHITE, 10),
        ITEM(Size.BODY, Glass.WHITE, 4),
        DETAIL(Size.LABEL, Glass.CYAN, 12),
        TEXT(Size.LABEL, Glass.BODY, 6);

        public final Size size;
        final Color colour;
        final int below;

        Kind(Size size, Color colour, int below) {
            this.size = size;
            this.colour = colour;
            this.below = below;
        }
    }

    /** The UI kit's fonts by their cell height. */
    public enum Size {
        LABEL(12),
        BODY(20),
        HEADING(30);

        public final int height;

        Size(int height) {
            this.height = height;
        }
    }

    /** One line of the roll: its text in its kind's font, the top of its capitals at {@code y}. */
    public record Line(Kind kind, String text, int y) {}

    /** The logo's width on the roll. */
    public static final int LOGO_WIDTH = 360;

    private final List<Line> lines;
    private final int height;
    private final int logoHeight;
    private final Color colour = new Color();
    private final Color shadow = new Color();

    private CreditsRoll(List<Line> lines, int height, int logoHeight) {
        this.lines = lines;
        this.height = height;
        this.logoHeight = logoHeight;
    }

    /**
     * Lays the roll file out for a column {@code width} px wide.
     *
     * @param advance the cell width of a font size (the fonts are monospaced)
     * @param logoHeight the logo's height at {@link #LOGO_WIDTH}
     */
    public static CreditsRoll layout(String file, int width, ToIntFunction<Size> advance, int logoHeight) {
        List<Line> lines = new ArrayList<>();
        int y = 0;
        for (String raw : file.split("\n")) {
            String entry = raw.strip();
            if (entry.isEmpty() || entry.startsWith("#")) {
                continue;
            }
            int bar = entry.indexOf('|');
            String name = bar < 0 ? entry : entry.substring(0, bar);
            String text = bar < 0 ? "" : entry.substring(bar + 1).strip();
            Kind kind = Kind.valueOf(name.strip().toUpperCase(Locale.ROOT));
            switch (kind) {
                case LOGO -> {
                    lines.add(new Line(kind, "", y));
                    y += logoHeight + kind.below;
                }
                case GAP -> y += kind.below;
                default -> {
                    for (String line : Words.wrap(text, width / advance.applyAsInt(kind.size))) {
                        lines.add(new Line(kind, line, y));
                        y += kind.size.height;
                    }
                    y += kind.below;
                }
            }
        }
        return new CreditsRoll(List.copyOf(lines), y, logoHeight);
    }

    public List<Line> lines() {
        return lines;
    }

    /** The height of the whole roll. */
    public int height() {
        return height;
    }

    /**
     * Draws the lines that fall between {@code top} and {@code bottom} with the roll's start at
     * {@code y}, centred on {@code centreX}, fading out over {@code fade} px at either edge.
     */
    public void draw(
            SpriteBatch batch, Glass glass, TitleScene scene, float centreX, float y, int top, int bottom, int fade) {
        for (Line line : lines) {
            float lineTop = y + line.y();
            int lineHeight = line.kind() == Kind.LOGO ? logoHeight : line.kind().size.height;
            if (lineTop + lineHeight < top || lineTop > bottom) {
                continue;
            }
            float alpha = Math.clamp(Math.min(lineTop - top, bottom - lineTop - lineHeight) / fade, 0f, 1f);
            if (line.kind() == Kind.LOGO) {
                batch.setColor(1, 1, 1, alpha);
                scene.logo(batch, Math.round(lineTop), LOGO_WIDTH);
                batch.setColor(Color.WHITE);
                continue;
            }
            BitmapFont font = font(glass.fonts, line.kind().size);
            float x = Math.round(centreX - Fonts.width(font, line.text()) / 2f);
            float textY = Math.round(lineTop);
            shadow.set(Glass.SHADOW).a = alpha;
            colour.set(line.kind().colour).a = alpha;
            glass.text(batch, font, line.text(), shadow, x + 1, textY + 1);
            glass.text(batch, font, line.text(), colour, x, textY);
        }
    }

    public static BitmapFont font(Fonts fonts, Size size) {
        return switch (size) {
            case LABEL -> fonts.label;
            case BODY -> fonts.body;
            case HEADING -> fonts.heading;
        };
    }
}
