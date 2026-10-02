package vanguard.game.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Align;
import vanguard.game.ui.Fonts;

/**
 * The HUD's drawing pieces in the chosen HUD A look: the bevelled metal side panels with their
 * corner rivets, label plates, recessed LCD wells, bar troughs with phosphor fills and the glow
 * behind a readout, all rendered by tools/art/hud.py (the wells, fills and glow as nine-patches
 * stretched to their region), and the UI kit's bitmap fonts: 8x12 for the plates' labels, the radio
 * subtitles and the control prompts (a radio line of 22 characters must fit the 196 px of a well),
 * 10x20 for the readouts.
 */
final class HudKit {
    static final int PANEL_WIDTH = PixelScreen.PLAY_FIELD_X;
    static final int TOP = PixelScreen.HEIGHT;
    static final int INSET = 16;
    static final int INNER_WIDTH = PANEL_WIDTH - 2 * INSET;

    /** The LCD glass of a well. */
    static final Color LCD = Color.valueOf("08091F");

    static final Color LABEL = Color.valueOf("E0E6FF");
    static final Color READOUT = Color.valueOf("40FF80");
    static final Color AMBER = Color.valueOf("FFE04A");
    static final Color ALERT = Color.valueOf("FF5040");

    /** A well's frame around its glass, and a trough's around its bar. */
    private static final int FRAME = MissionLayout.FRAME;
    /** A bar segment and the gap after it. */
    private static final int SEGMENT_PITCH = 13;
    /** The glow behind a readout is this opaque. */
    private static final float GLOW_ALPHA = 28 / 255f;
    /** A plate's text sits this far right of the plate's left edge. */
    private static final int PLATE_INDENT = 4;

    /** The 8x12 label font. */
    final BitmapFont small;
    /** The 10x20 body font. */
    final BitmapFont body;

    private final TextureRegion leftPanel;
    private final TextureRegion rightPanel;
    private final TextureRegion plate;
    private final NinePatch well;
    private final NinePatch portrait;
    private final NinePatch fill;
    private final NinePatch glow;

    HudKit(Sprites sprites, Fonts fonts) {
        this.small = fonts.label;
        this.body = fonts.body;
        this.leftPanel = sprites.hudPanelLeft;
        this.rightPanel = sprites.hudPanelRight;
        this.plate = sprites.hudPlate;
        this.well = sprites.hudWell;
        this.portrait = sprites.hudPortrait;
        this.fill = sprites.hudFill;
        this.glow = sprites.hudGlow;
    }

    void leftPanel(SpriteBatch batch) {
        batch.draw(leftPanel, 0, 0);
    }

    void rightPanel(SpriteBatch batch) {
        batch.draw(rightPanel, PixelScreen.WIDTH - PANEL_WIDTH, 0);
    }

    /** A label plate with its text's left at {@code x} and the plate's top 4 px above {@code y}. */
    void label(SpriteBatch batch, String text, int x, int y) {
        batch.draw(plate, x - PLATE_INDENT, y + 4 - plate.getRegionHeight());
        text(batch, small, text, LABEL, x, y - 4);
    }

    /** An LCD well whose glass is {@code width} x {@code height} with its bottom at {@code y}. */
    void lcd(SpriteBatch batch, int x, int y, int width, int height) {
        well.draw(batch, x - FRAME, y - FRAME, width + 2 * FRAME, height + 2 * FRAME);
    }

    /** The radio portrait's well, its screen {@code size} px square with its bottom at {@code y}. */
    void portraitWell(SpriteBatch batch, int x, int y, int size) {
        portrait.draw(batch, x - FRAME, y - FRAME, size + 2 * FRAME, size + 2 * FRAME);
    }

    /** The phosphor glow of a readout in {@code colour} over a well's glass. */
    void glow(SpriteBatch batch, Color colour, float x, float y, float width, float height) {
        batch.setColor(colour.r, colour.g, colour.b, GLOW_ALPHA);
        glow.draw(batch, x, y, width, height);
        batch.setColor(Color.WHITE);
    }

    /** Text with its capitals' top at {@code y}. */
    void text(SpriteBatch batch, BitmapFont font, String text, Color colour, float x, float y) {
        font.setColor(colour);
        font.draw(batch, text, x, y);
        font.setColor(Color.WHITE);
    }

    /** Text left-aligned in {@code width}, cut off at its end if it is longer. */
    void text(SpriteBatch batch, BitmapFont font, String text, Color colour, float x, float y, float width) {
        fitted(batch, font, text, colour, x, y, width, Align.left);
    }

    /** Text right-aligned in {@code width}, cut off at its end if it is longer. */
    void textRight(SpriteBatch batch, BitmapFont font, String text, Color colour, float x, float y, float width) {
        fitted(batch, font, text, colour, x, y, width, Align.right);
    }

    private static void fitted(
            SpriteBatch batch, BitmapFont font, String text, Color colour, float x, float y, float width, int align) {
        font.setColor(colour);
        font.draw(batch, text, x, y, 0, text.length(), width, align, false, "");
        font.setColor(Color.WHITE);
    }

    /** A bar in its trough with its bottom at {@code y}, lit to {@code share} (0..1), unlit in {@code empty}. */
    void bar(SpriteBatch batch, Color full, Color empty, int x, int y, int width, int height, double share) {
        lcd(batch, x, y, width, height);
        fill(batch, empty, x, y, width, height);
        fill(batch, full, x, y, (float) Math.floor(width * Math.clamp(share, 0, 1)), height);
    }

    /**
     * A gauge of separate phosphor segments in its trough with its bottom at {@code y}: as many
     * lit as {@code share} (0..1) begins, the rest unlit in {@code empty}.
     */
    void segments(SpriteBatch batch, Color full, Color empty, int x, int y, int width, int height, double share) {
        lcd(batch, x, y, width, height);
        int count = width / SEGMENT_PITCH;
        int lit = (int) Math.ceil(count * Math.clamp(share, 0, 1));
        for (int i = 0; i < count; i++) {
            fill(batch, i < lit ? full : empty, x + 1 + i * SEGMENT_PITCH, y + 1, SEGMENT_PITCH - 1, height - 2);
        }
    }

    /** A phosphor cell (a pip, a bar's fill, a flash) in {@code colour}; nothing narrower than its two ends. */
    void fill(SpriteBatch batch, Color colour, float x, float y, float width, float height) {
        if (width < fill.getLeftWidth() + fill.getRightWidth()) {
            return;
        }
        batch.setColor(colour);
        fill.draw(batch, x, y, width, height);
        batch.setColor(Color.WHITE);
    }
}
