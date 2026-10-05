package vanguard.game.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import vanguard.sim.PlayField;

/**
 * The boss bar at the top of the play field (design/ui/hud): the sum of the boss's remaining part HP
 * as a red fill, 400 px for an act boss and 240 px for a mid-boss, the boss's name above it. With
 * the production plate {@code hud/boss-bar-plate} (a nine-patch, {@code assets/sprites/hud/boss-bar-plate.9.png})
 * the plate is stretched round the bar and the fill drawn in its content box (the nine-patch's
 * padding, or the middle between its splits without one), at the plate's own height; without it
 * the bar is drawn plainly: a dark frame, a dark red trough and the fill, 6 px tall.
 */
final class BossBar {
    /** The plate's region in the sprite pages. */
    static final String PLATE = "hud/boss-bar-plate";
    /** The bar's width for an act boss and a mid-boss, the plain bar's height, the bar's top below the top edge. */
    static final float WIDTH = 400;

    static final float MID_WIDTH = 240;
    static final float PLAIN_HEIGHT = 6;
    static final float TOP = 22;

    private static final float X0 = PixelScreen.PLAY_FIELD_X;
    private static final Color FRAME = Color.valueOf("0A0C1A");
    private static final Color EMPTY = Color.valueOf("3A1414");
    private static final Color FILL = Color.valueOf("E03C28");

    /** Where the plate and the fill go, in play-field pixels (y up); the plate's box is empty without one. */
    record Layout(
            float plateX,
            float plateY,
            float plateWidth,
            float plateHeight,
            float fillX,
            float fillY,
            float fillWidth,
            float fillHeight) {}

    private final TextureRegion pixel;
    /** The production plate, or null until it exists. */
    private final NinePatch plate;

    BossBar(Sprites sprites) {
        this.pixel = sprites.pixel;
        this.plate = plate(sprites);
    }

    /** The plate if the sprite pages hold it as a nine-patch (a plain PNG is not used). */
    private static NinePatch plate(Sprites sprites) {
        if (!sprites.has(PLATE) || sprites.region(PLATE).findValue("split") == null) {
            return null;
        }
        return sprites.patch(PLATE);
    }

    /**
     * The layout of a bar {@code width} wide with its fill's top {@value #TOP} px below the play
     * field's top, round a plate with these paddings and total height (all 0 for the plain bar).
     */
    static Layout layout(float width, float padLeft, float padRight, float padTop, float padBottom, float plateHeight) {
        float left = (PlayField.WIDTH - width) / 2;
        float top = PlayField.HEIGHT - TOP;
        if (plateHeight <= 0) {
            return new Layout(left, top, 0, 0, left, top - PLAIN_HEIGHT, width, PLAIN_HEIGHT);
        }
        float fillHeight = plateHeight - padTop - padBottom;
        float bottom = top - fillHeight;
        return new Layout(
                left - padLeft,
                bottom - padBottom,
                width + padLeft + padRight,
                plateHeight,
                left,
                bottom,
                width,
                fillHeight);
    }

    /** Draws the bar of a boss with {@code share} (0..1) of its HP left and its name. */
    void draw(SpriteBatch batch, BitmapFont font, String name, boolean midBoss, double share) {
        float width = midBoss ? MID_WIDTH : WIDTH;
        Layout at = plate == null
                ? layout(width, 0, 0, 0, 0, 0)
                : layout(
                        width,
                        plate.getPadLeft(),
                        plate.getPadRight(),
                        plate.getPadTop(),
                        plate.getPadBottom(),
                        plate.getTotalHeight());
        float nameTop;
        if (plate == null) {
            batch.setColor(FRAME);
            batch.draw(pixel, X0 + at.fillX() - 2, at.fillY() - 2, at.fillWidth() + 4, at.fillHeight() + 4);
            batch.setColor(EMPTY);
            batch.draw(pixel, X0 + at.fillX(), at.fillY(), at.fillWidth(), at.fillHeight());
            nameTop = at.fillY() + at.fillHeight() + 12;
        } else {
            batch.setColor(Color.WHITE);
            plate.draw(batch, X0 + at.plateX(), at.plateY(), at.plateWidth(), at.plateHeight());
            // The name's capitals sit 3 px above the plate.
            nameTop = at.plateY() + at.plateHeight() + 3 + font.getCapHeight();
        }
        batch.setColor(FILL);
        batch.draw(
                pixel,
                X0 + at.fillX(),
                at.fillY(),
                (float) Math.floor(at.fillWidth() * Math.clamp(share, 0, 1)),
                at.fillHeight());
        batch.setColor(Color.WHITE);
        font.setColor(Color.WHITE);
        font.draw(batch, name, X0 + at.fillX(), Math.round(nameTop));
    }
}
