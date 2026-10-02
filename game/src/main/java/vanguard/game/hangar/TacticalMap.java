package vanguard.game.hangar;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Disposable;
import vanguard.game.render.PixelScreen;

/**
 * The hangar's backdrop (design/ui/hangar, chosen hangar-r07-b): a darkened tactical display
 * behind the glass panels with a faint cyan grid, a planet's limb across the lower part, orange
 * range rings round the operation's area and a dashed cyan approach route. PLACEHOLDER drawn in
 * code, the same for every level until the production art draws one map per setting.
 */
public final class TacticalMap implements Disposable {
    private static final int GRID = 32;
    private static final int RING_X = 470;
    private static final int RING_Y = 300;
    private static final int[] RINGS = {50, 100, 160, 230};
    private static final int LIMB_RADIUS = 900;

    private final Texture texture;

    public TacticalMap() {
        int width = PixelScreen.WIDTH;
        int height = PixelScreen.HEIGHT;
        var pixmap = new Pixmap(width, height, Pixmap.Format.RGBA8888);
        pixmap.setColor(new Color(0.02f, 0.03f, 0.10f, 1));
        pixmap.fill();
        pixmap.setColor(new Color(0.05f, 0.12f, 0.28f, 1));
        pixmap.fillCircle(width / 2, height + LIMB_RADIUS - 150, LIMB_RADIUS);
        pixmap.setColor(new Color(0.12f, 0.30f, 0.50f, 1));
        pixmap.drawCircle(width / 2, height + LIMB_RADIUS - 150, LIMB_RADIUS);
        pixmap.setColor(new Color(0.04f, 0.16f, 0.22f, 1));
        for (int x = 0; x < width; x += GRID) {
            pixmap.drawLine(x, 0, x, height);
        }
        for (int y = 0; y < height; y += GRID) {
            pixmap.drawLine(0, y, width, y);
        }
        pixmap.setColor(new Color(0.45f, 0.22f, 0.05f, 1));
        for (int radius : RINGS) {
            pixmap.drawCircle(RING_X, RING_Y, radius);
        }
        pixmap.setColor(new Color(0.10f, 0.45f, 0.55f, 1));
        int steps = 40;
        for (int i = 0; i < steps; i += 2) {
            pixmap.drawLine(
                    lerp(60, RING_X, i, steps),
                    lerp(20, RING_Y, i, steps),
                    lerp(60, RING_X, i + 1, steps),
                    lerp(20, RING_Y, i + 1, steps));
        }
        texture = new Texture(pixmap);
        pixmap.dispose();
    }

    private static int lerp(int from, int to, int step, int steps) {
        return from + (to - from) * step / steps;
    }

    public void draw(SpriteBatch batch) {
        batch.draw(texture, 0, 0);
    }

    @Override
    public void dispose() {
        texture.dispose();
    }
}
