package vanguard.game.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import vanguard.sim.Ship;
import vanguard.sim.ShipSpec;
import vanguard.sim.Shot;
import vanguard.sim.SimStep;
import vanguard.sim.Skitter;
import vanguard.sim.Sortie;
import vanguard.sim.TestSortie;

/**
 * Draws a sortie back to front, interpolating every position between the last two simulation
 * steps: the backdrop below the play plane, the Skitters, the ship, the glowing bolts and effects,
 * then the high-air layer on top.
 */
public final class FlightRenderer {
    private static final float X0 = PixelScreen.PLAY_FIELD_X;
    /** The wing beat runs at 10 fps (Vrell organic motion: 8-12 fps). */
    private static final int WING_BEAT_TICKS = 6;
    /** The muzzle flash shows each of its three frames for two steps after a shot. */
    private static final int MUZZLE_FRAME_TICKS = 2;

    private static final Color HIT_WHITE = Color.WHITE;
    private static final Color SHIELD_BLUE = Color.valueOf("00C0FF");
    private static final float SHIELD_SHIMMER = 0.6f;

    private final Sprites sprites;
    private final Backdrop backdrop;
    private final FlashShader flash;

    public FlightRenderer(Sprites sprites, FlashShader flash) {
        this.sprites = sprites;
        this.backdrop = new Backdrop(sprites);
        this.flash = flash;
    }

    /**
     * @param alpha interpolation between the previous and the current step
     * @param shieldShimmer 0..1, how strongly the ship shows its last shield hit
     */
    public void draw(SpriteBatch batch, Sortie sortie, Effects effects, float alpha, float shieldShimmer) {
        double scroll = sortie.groundScroll() - TestSortie.SCROLL_SPEED * SimStep.SECONDS * (1 - alpha);
        backdrop.drawBehind(batch, scroll);
        drawSkitters(batch, sortie, alpha);
        if (sortie.flying()) {
            drawShip(batch, sortie.ship(), alpha, shieldShimmer);
        }
        drawBolts(batch, sortie, alpha);
        effects.draw(batch);
        backdrop.drawFront(batch, scroll);
    }

    private void drawSkitters(SpriteBatch batch, Sortie sortie, float alpha) {
        for (int i = 0; i < sortie.skitterCount(); i++) {
            Skitter skitter = sortie.skitter(i);
            int frame = (int) ((sortie.tick() / WING_BEAT_TICKS + i) % sprites.skitter.size);
            drawCentred(batch, sprites.skitter.get(frame), skitter.renderX(alpha), skitter.renderY(alpha));
        }
    }

    private void drawShip(SpriteBatch batch, Ship ship, float alpha, float shieldShimmer) {
        TextureRegion hull = sprites.ship.get(ship.bank() + ShipSpec.HARD_BANK);
        float x = Math.round(X0 + ship.renderX(alpha));
        float y = Math.round(ship.renderY(alpha));
        int mercy = ship.defences().mercyTicks();
        // The hull blinks white in steps of three frames while the mercy invulnerability lasts.
        if (mercy > 0 && (mercy + 2) / 3 % 2 == 1) {
            flash.draw(batch, hull, x, y, HIT_WHITE, 1);
        } else if (shieldShimmer > 0) {
            flash.draw(batch, hull, x, y, SHIELD_BLUE, shieldShimmer * SHIELD_SHIMMER);
        } else {
            batch.draw(hull, x - hull.getRegionWidth() / 2f, y - hull.getRegionHeight() / 2f);
        }
    }

    private void drawBolts(SpriteBatch batch, Sortie sortie, float alpha) {
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
        for (int i = 0; i < sortie.shotCount(); i++) {
            Shot shot = sortie.shot(i);
            drawCentred(batch, sprites.pulseBolt, shot.renderX(alpha), shot.renderY(alpha));
        }
        Ship ship = sortie.ship();
        int frame = ship.ticksSinceShot() / MUZZLE_FRAME_TICKS;
        if (sortie.flying() && frame < sprites.pulseMuzzle.size) {
            drawCentred(
                    batch,
                    sprites.pulseMuzzle.get(frame),
                    ship.renderX(alpha),
                    ship.renderY(alpha) + ship.spec().muzzleOffsetY());
        }
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
    }

    private static void drawCentred(SpriteBatch batch, TextureRegion region, double x, double y) {
        batch.draw(
                region,
                Math.round(X0 + x - region.getRegionWidth() / 2.0),
                Math.round(y - region.getRegionHeight() / 2.0));
    }
}
