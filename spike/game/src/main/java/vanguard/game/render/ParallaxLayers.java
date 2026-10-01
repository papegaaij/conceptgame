package vanguard.game.render;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import vanguard.sim.Layer;
import vanguard.sim.World;

/** Draws the tiling background texture of a layer, scrolled by its parallax factor. */
public final class ParallaxLayers {
    /** Weather on the high-air layer may cover the play plane at no more than 40 % opacity. */
    private static final float HIGH_AIR_OPACITY = 0.4f;

    private final ProceduralArt art;

    public ParallaxLayers(ProceduralArt art) {
        this.art = art;
    }

    /** Draws {@code layer}; {@code groundScroll} is the ground layer's scrolled distance in pixels. */
    public void draw(SpriteBatch batch, Layer layer, double groundScroll) {
        Texture tile = art.tile(layer);
        if (tile == null) {
            return;
        }
        float size = tile.getHeight();
        float offset = (float) ((groundScroll * layer.scrollFactor()) % size) / size;
        float u2 = World.WIDTH / size;
        float v2 = World.HEIGHT / size;
        if (layer == Layer.HIGH_AIR) {
            batch.setColor(1, 1, 1, HIGH_AIR_OPACITY);
        }
        batch.draw(tile, PixelScreen.PLAY_FIELD_X, 0, World.WIDTH, World.HEIGHT, 0, offset, u2, offset + v2);
        batch.setColor(1, 1, 1, 1);
    }
}
