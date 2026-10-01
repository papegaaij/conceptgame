package vanguard.game.scene;

import com.badlogic.gdx.Gdx;
import vanguard.game.render.HaloPlatform;
import vanguard.game.render.ParallaxLayers;
import vanguard.game.render.PixelScreen;
import vanguard.sim.Layer;

/** The Halo Platform angle set alone, turning at its base speed over a static deck. */
public final class HaloScene implements Scene {
    private final SharedGraphics graphics;
    private final HaloPlatform halo;
    private final ParallaxLayers background;
    private float time;

    public HaloScene(SharedGraphics graphics) {
        this.graphics = graphics;
        this.halo = new HaloPlatform(Gdx.files, graphics.videoMemory());
        this.background = new ParallaxLayers(graphics.art());
        Gdx.app.log("halo", halo.describe());
    }

    @Override
    public void render(float delta) {
        time += delta;
        var batch = graphics.batch();
        graphics.screen().begin(batch);
        background.draw(batch, Layer.GROUND, 0);
        halo.draw(batch, time * HaloPlatform.BASE_SPEED, PixelScreen.WIDTH / 2f, PixelScreen.HEIGHT / 2f);
        graphics.screen().end(batch);
    }

    @Override
    public String report() {
        return "halo platform     %s%n".formatted(halo.describe());
    }

    @Override
    public void dispose() {
        halo.dispose();
    }
}
