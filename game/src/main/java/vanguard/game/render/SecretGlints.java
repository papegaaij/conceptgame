package vanguard.game.render;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import vanguard.sim.Crane;
import vanguard.sim.GroundObject;
import vanguard.sim.PlayField;
import vanguard.sim.SimStep;
import vanguard.sim.Sortie;
import vanguard.sim.Tow;

/**
 * The Salvage scanner's glint (design/player/systems, Salvage scanner): every object that reveals a
 * secret shows a short sparkle at its centre while it is on the screen and not yet spent: a
 * trigger or a destructible that hides a secret (a data core's too, and in the dark as well), a
 * crane's clamp while it holds its crate and a lifeboat's tow cable until it is cut. The sparkle
 * plays {@value #FRAMES} frames in {@value #SPARKLE_SECONDS} s every {@value #PERIOD_SECONDS} s,
 * each object at its own phase, additive. It plays its own sprite, {@value #OWN} (concept round 28,
 * option a until the round closes: tools/art/secret_glint.py); without it, the loot targets' glint
 * frames forwards and back. Drawn from the simulation's state, so it allocates nothing. Geometry in
 * play-field pixels, y up.
 */
public final class SecretGlints {
    static final int FRAMES = 4;
    static final double SPARKLE_SECONDS = 0.3;
    static final double PERIOD_SECONDS = 1.5;
    /** The glint's own frames on the sprite pages. */
    static final String OWN = "glint-secret";

    private static final float X0 = PixelScreen.PLAY_FIELD_X;
    private static final int PERIOD_TICKS = SimStep.ticks(PERIOD_SECONDS);
    private static final int SPARKLE_TICKS = SimStep.ticks(SPARKLE_SECONDS);
    /** An object this far outside the play field is not on the screen, px. */
    private static final double MARGIN = 8;

    /** The sparkle's frames in play order. */
    private final TextureRegion[] frames = new TextureRegion[FRAMES];

    /** With the glint's own frames, or the loot targets' glint when the sprite pages lack them. */
    public SecretGlints(Sprites sprites) {
        this(sprites.has(OWN) ? sprites.frames(OWN) : sprites.glint);
    }

    /**
     * @param glint the glint's frames: its own four, or the loot targets' three (rising, full,
     *     falling), played forwards and back
     */
    public SecretGlints(Array<AtlasRegion> glint) {
        for (int f = 0; f < FRAMES; f++) {
            int index = f < glint.size ? f : 2 * (glint.size - 1) - f;
            frames[f] = glint.get(Math.clamp(index, 0, glint.size - 1));
        }
    }

    /** The frame a glint shows {@code ticks} steps into the level at {@code phase}; -1 between sparkles. */
    static int frame(long ticks, int phase) {
        long into = Math.floorMod(ticks + phase, (long) PERIOD_TICKS);
        return into < SPARKLE_TICKS ? (int) (into * FRAMES / SPARKLE_TICKS) : -1;
    }

    public void draw(SpriteBatch batch, Sortie sortie, float alpha) {
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
        long tick = sortie.tick();
        for (int i = 0; i < sortie.groundObjectCount(); i++) {
            GroundObject object = sortie.groundObject(i);
            if (object.spec().secretIndex() >= 0 && !object.spent()) {
                sparkle(
                        batch,
                        tick,
                        object.renderX(),
                        object.renderY(alpha),
                        (int) object.spec().x() * 11);
            }
        }
        for (int c = 0; c < sortie.craneCount(); c++) {
            Crane crane = sortie.crane(c);
            if (crane.present() && crane.holding() && crane.spec().clampHits() > 0) {
                double angle = crane.renderAngle(alpha);
                sparkle(
                        batch,
                        tick,
                        crane.spec().pivotX() + Math.sin(angle) * crane.spec().length(),
                        crane.spec().pivotY() - Math.cos(angle) * crane.spec().length(),
                        29 * c);
            }
        }
        for (int k = 0; k < sortie.towCount(); k++) {
            Tow tow = sortie.tow(k);
            if (tow.present() && tow.holding()) {
                sparkle(
                        batch,
                        tick,
                        tow.renderX(alpha) + tow.cableX() - tow.x(),
                        tow.renderY(alpha) + tow.cableY() - tow.y(),
                        37 * k + 13);
            }
        }
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
    }

    /** A sparkle at ({@code x}, {@code y}) when it is due at {@code phase} (steps; fixed per object) and on the screen. */
    private void sparkle(SpriteBatch batch, long tick, double x, double y, int phase) {
        if (x < -MARGIN || x > PlayField.WIDTH + MARGIN || y < -MARGIN || y > PlayField.HEIGHT + MARGIN) {
            return;
        }
        int frame = frame(tick, phase);
        if (frame >= 0) {
            TextureRegion region = frames[frame];
            batch.draw(
                    region,
                    Math.round(X0 + x - region.getRegionWidth() / 2.0),
                    Math.round(y - region.getRegionHeight() / 2.0));
        }
    }
}
