package vanguard.game.render;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import vanguard.sim.Player;

/** Placeholder HUD side panels: frames plus shield and armour gauges. */
public final class HudPanels {
    private static final int PANEL_WIDTH = PixelScreen.PLAY_FIELD_X;
    private static final int RIGHT_PANEL_X = PixelScreen.WIDTH - PANEL_WIDTH;
    private static final int GAUGE_HEIGHT = 300;

    private final TextureRegion pixel;

    public HudPanels(ProceduralArt art) {
        this.pixel = art.pixel;
    }

    public void draw(SpriteBatch batch, Player player) {
        panel(batch, 0);
        panel(batch, RIGHT_PANEL_X);
        gauge(batch, 40, (float) (player.shield() / 100), 0.2f, 0.7f, 1f);
        gauge(batch, 80, player.armour() / 100f, 1f, 0.6f, 0.1f);
        batch.setColor(1, 1, 1, 1);
    }

    private void panel(SpriteBatch batch, int x) {
        batch.setColor(0.07f, 0.08f, 0.11f, 1);
        batch.draw(pixel, x, 0, PANEL_WIDTH, PixelScreen.HEIGHT);
        batch.setColor(0.75f, 0.5f, 0.1f, 1);
        batch.draw(pixel, x + 8, 8, PANEL_WIDTH - 16, 2);
        batch.draw(pixel, x + 8, PixelScreen.HEIGHT - 10, PANEL_WIDTH - 16, 2);
    }

    private void gauge(SpriteBatch batch, int x, float fill, float r, float g, float b) {
        batch.setColor(0.15f, 0.16f, 0.2f, 1);
        batch.draw(pixel, x, 120, 24, GAUGE_HEIGHT);
        batch.setColor(r, g, b, 1);
        batch.draw(pixel, x, 120, 24, GAUGE_HEIGHT * Math.max(0, fill));
    }
}
