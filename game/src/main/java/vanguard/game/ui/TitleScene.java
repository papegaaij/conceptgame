package vanguard.game.ui;

import com.badlogic.gdx.Files;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Disposable;
import vanguard.game.render.PixelScreen;

/**
 * The pre-rendered hero scene behind the out-of-game screens (design/ui/main-menu, menu A: the
 * Stormhawk climbing over Earth's limb towards the Vrell fleet) and logo D, rendered by
 * tools/art/ui_scenes.py.
 */
public final class TitleScene implements Disposable {
    private final Texture scene;
    private final Texture logo;

    public TitleScene(Files files) {
        scene = new Texture(files.internal("ui/title-scene.png"));
        logo = new Texture(files.internal("ui/title-logo.png"));
        logo.setFilter(TextureFilter.Linear, TextureFilter.Linear);
    }

    /** The scene over the whole screen, darkened to {@code brightness} (1 = as rendered). */
    public void draw(SpriteBatch batch, float brightness) {
        batch.setColor(brightness, brightness, brightness, 1);
        batch.draw(scene, 0, 0);
        batch.setColor(1, 1, 1, 1);
    }

    /** The logo's height at {@code width} px wide. */
    public float logoHeight(float width) {
        return logo.getHeight() * width / logo.getWidth();
    }

    /** The logo, {@code width} px wide, centred, its top {@code y} px below the top of the screen. */
    public void logo(SpriteBatch batch, float y, float width) {
        float height = logoHeight(width);
        batch.draw(logo, Math.round((PixelScreen.WIDTH - width) / 2), PixelScreen.HEIGHT - y - height, width, height);
    }

    @Override
    public void dispose() {
        scene.dispose();
        logo.dispose();
    }
}
