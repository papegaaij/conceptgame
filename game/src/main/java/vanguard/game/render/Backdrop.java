package vanguard.game.render;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import vanguard.sim.PlayField;

/**
 * The scrolling parallax layers of the Earth orbit scene behind and in front of the play plane
 * (design/art-direction, Parallax layer model), with the scroll factors of the chosen scene
 * (parallax r03 A). The M1 placeholders are the scene's 0.4x layer breakdown, drawn enlarged, and
 * every second tile is mirrored so the untiled images scroll without a seam.
 */
public final class Backdrop {
    private static final float WIDTH = PlayField.WIDTH;
    private static final float HEIGHT = PlayField.HEIGHT;
    /** Weather on high-air may cover the play plane at no more than 40 % opacity. */
    private static final float HIGH_AIR_OPACITY = 0.4f;

    private final Layer[] behind;
    private final Layer front;

    private record Layer(TextureRegion image, double scrollFactor, boolean additive) {}

    public Backdrop(Sprites sprites) {
        behind = new Layer[] {
            new Layer(sprites.deep, 0.12, false),
            new Layer(sprites.far, 0.45, false),
            new Layer(sprites.ground, 1.0, false),
            new Layer(sprites.lowAir, 1.4, false)
        };
        front = new Layer(sprites.highAir, 2.4, true);
    }

    /** The layers below the play plane; {@code groundScroll} is the ground layer's distance in px. */
    public void drawBehind(SpriteBatch batch, double groundScroll) {
        for (Layer layer : behind) {
            draw(batch, layer, groundScroll);
        }
    }

    /** The high-air layer above the play plane. */
    public void drawFront(SpriteBatch batch, double groundScroll) {
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
        batch.setColor(1, 1, 1, HIGH_AIR_OPACITY);
        draw(batch, front, groundScroll);
        batch.setColor(1, 1, 1, 1);
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
    }

    private static void draw(SpriteBatch batch, Layer layer, double groundScroll) {
        double distance = groundScroll * layer.scrollFactor();
        long tile = (long) Math.floor(distance / HEIGHT);
        float offset = (float) (distance - tile * HEIGHT);
        drawTile(batch, layer.image(), tile, -offset);
        drawTile(batch, layer.image(), tile + 1, HEIGHT - offset);
    }

    private static void drawTile(SpriteBatch batch, TextureRegion image, long tile, float y) {
        float x = PixelScreen.PLAY_FIELD_X;
        if (tile % 2 == 0) {
            batch.draw(image, x, y, WIDTH, HEIGHT);
        } else {
            batch.draw(image, x, y + HEIGHT, WIDTH, -HEIGHT);
        }
    }
}
