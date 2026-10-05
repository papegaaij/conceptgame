package vanguard.game.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Align;

/**
 * The small floating numbers for credits picked up (design/ui/hud, in the play field): each
 * rises and fades for a second, advanced with the simulation steps. Pure presentation. The Gameplay
 * tab's {@code CREDIT NUMBERS} option hides them (design/ui/options); they are still followed while
 * hidden, so turning the option back on mid-level shows only the numbers still rising.
 */
public final class CreditNumbers {
    private static final int CAPACITY = 32;
    private static final int LIFE_TICKS = 60;
    private static final float RISE = 24;
    private static final Color COLOUR = Color.valueOf("FFE04A");

    private final String[] texts = new String[CAPACITY];
    private final float[] xs = new float[CAPACITY];
    private final float[] ys = new float[CAPACITY];
    private final int[] ages = new int[CAPACITY];
    private int size;
    private boolean visible = true;

    /** Whether the numbers are drawn (the Gameplay tab's {@code gameplay.credit-numbers}). */
    public void visible(boolean shown) {
        visible = shown;
    }

    /** How many numbers a draw shows now: none while they are hidden. */
    int drawn() {
        return visible ? size : 0;
    }

    /** Shows {@code credits} at a play-field position; dropped when all slots are in use. */
    public void show(int credits, double x, double y) {
        if (size < CAPACITY) {
            texts[size] = "+" + credits;
            xs[size] = (float) x;
            ys[size] = (float) y;
            ages[size] = 0;
            size++;
        }
    }

    public void step() {
        for (int i = size - 1; i >= 0; i--) {
            if (++ages[i] >= LIFE_TICKS) {
                size--;
                texts[i] = texts[size];
                xs[i] = xs[size];
                ys[i] = ys[size];
                ages[i] = ages[size];
            }
        }
    }

    public void clear() {
        size = 0;
    }

    void draw(SpriteBatch batch, BitmapFont font) {
        int shown = drawn();
        for (int i = 0; i < shown; i++) {
            float life = (float) ages[i] / LIFE_TICKS;
            font.setColor(COLOUR.r, COLOUR.g, COLOUR.b, 1 - life);
            font.draw(
                    batch,
                    texts[i],
                    PixelScreen.PLAY_FIELD_X + xs[i] - 20,
                    ys[i] + 10 + RISE * life,
                    40,
                    Align.center,
                    false);
        }
        font.setColor(Color.WHITE);
    }
}
