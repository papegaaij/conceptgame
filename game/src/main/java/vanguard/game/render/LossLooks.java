package vanguard.game.render;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.utils.Array;
import java.util.Optional;
import vanguard.sim.Ally;
import vanguard.sim.LevelScript;
import vanguard.sim.Sortie;

/**
 * M5 part D: the look of a scripted loss (design/campaign Level 10, the lance that takes Lifeline
 * Three; tools/art/lance_l10.py, concept round 32's pick b, the iris column), all additive. From the glow's start
 * ({@code t − glow}, the {@code LOSS_GLOW} event) the light in the cloud deck pulses over its unit,
 * following its sway, quickening to the hit ({@link #intensity}); at the hit ({@code t}, {@code
 * SCRIPTED_LOSS}) the lance strikes where the unit is, and the glow fades there over {@value
 * #FADE_SECONDS} s. Both are read from the level clock and the unit's state, so a restart or a
 * retry needs nothing reset.
 *
 * <p>The variant is told by its frames: variant a ("Thorn spear") has a {@code lance-flash} set, its
 * glow {@value #GLOW_DY_A} px above the unit and its spear's tip ({@code (12, 72)} of 24×88) on the
 * hit, {@value #LANCE_TICKS_A} steps a frame; variant b ("Iris column") has none, its glow centred on
 * the unit and growing from {@value #GLOW_SCALE_B} to full over the glow, its column centred on the
 * hit, {@value #LANCE_TICKS_B} steps a frame. The game ships b (round 32, 2026-10-08); a stays
 * supported for the script's {@code --variant a}. Without the frames it draws nothing.
 */
final class LossLooks {
    private static final float X0 = PixelScreen.PLAY_FIELD_X;
    /** The glow's churn loop plays at 10 fps. */
    static final double GLOW_FPS = 10;
    /** The glow fades out over this long after the hit (ease out). */
    static final double FADE_SECONDS = 0.8;
    /** The glow's envelope rises to full over this long. */
    static final double RISE_SECONDS = 1.6;

    static final int GLOW_DY_A = 72;
    /** Variant a's spear: its tip this far below the frame's top, on the hit. */
    static final int LANCE_TIP_A = 72;

    static final int LANCE_TICKS_A = 4;
    static final int LANCE_TICKS_B = 3;
    static final int FLASH_TICKS = 3;
    /** Variant b's glow starts at this scale and grows to 1 over the glow. */
    static final float GLOW_SCALE_B = 0.55f;

    private final Optional<LevelScript.ScriptedLoss> loss;
    private final Array<AtlasRegion> glow;
    private final Array<AtlasRegion> lance;
    private final Array<AtlasRegion> flash;
    /** Variant a: a spear from the glow above; b: a column centred on the hit. */
    private final boolean spear;

    LossLooks(Sprites sprites, LevelScript script) {
        loss = script.escort().flatMap(LevelScript.Escort::air).flatMap(LevelScript.Air::scriptedLoss);
        glow = sprites.has("cloud-glow") ? sprites.frames("cloud-glow") : new Array<>();
        lance = sprites.has("lance") ? sprites.frames("lance") : new Array<>();
        flash = sprites.has("lance-flash") ? sprites.frames("lance-flash") : new Array<>();
        spear = !flash.isEmpty();
    }

    /** Whether it is variant a's thorn spear (a {@code lance-flash} set), else b's iris column. */
    boolean spear() {
        return spear;
    }

    /**
     * The glow's strength (the batch alpha) {@code tau} s after it starts, for a glow of {@code
     * glowSeconds} (tools/art/lance_l10.py, {@code intensity}): env · (0.6 + 0.4 cos 2πφ), env = 0.25
     * + 0.75 smoothstep(0, 1.6, τ), φ = 1.25 τ + 0.625 τ² (five pulses quickening, the last peak at
     * the hit); after the hit 1 fading to 0 over {@value #FADE_SECONDS} s, ease out; 0 before.
     */
    static float intensity(double tau, double glowSeconds) {
        if (tau < 0) {
            return 0;
        }
        if (tau <= glowSeconds) {
            double env = 0.25 + 0.75 * smoothstep(0, RISE_SECONDS, tau);
            double phi = 1.25 * tau + 0.625 * tau * tau;
            return (float) (env * (0.6 + 0.4 * Math.cos(2 * Math.PI * phi)));
        }
        double u = Math.clamp((tau - glowSeconds) / FADE_SECONDS, 0, 1);
        return (float) ((1 - u) * (1 - u));
    }

    /** Variant b's glow scale {@code tau} s into the glow: 0.55 growing to 1 (smoothstep); a's is 1. */
    static float glowScale(boolean spear, double tau, double glowSeconds) {
        return spear ? 1 : (float) (GLOW_SCALE_B + (1 - GLOW_SCALE_B) * smoothstep(0, glowSeconds, tau));
    }

    static double smoothstep(double from, double to, double x) {
        double t = Math.clamp((x - from) / (to - from), 0, 1);
        return t * t * (3 - 2 * t);
    }

    /** The lance's frame {@code ticks} steps after the hit; -1 once it has played. */
    static int lanceFrame(double ticks, int frames, int ticksPerFrame) {
        int frame = (int) Math.floor(ticks / ticksPerFrame);
        return ticks < 0 || frame >= frames ? -1 : frame;
    }

    /**
     * The glow, under the air layer: over the unit until the hit (a 72 px above it, b on it), then
     * fading where it was hit.
     *
     * @param seconds the level clock at the render time
     */
    void drawGlow(SpriteBatch batch, Sortie sortie, float alpha, double seconds) {
        if (loss.isEmpty() || glow.isEmpty() || sortie.scriptedAlly() < 0) {
            return;
        }
        LevelScript.ScriptedLoss spec = loss.get();
        double tau = seconds - (spec.t() - spec.glow());
        float strength = intensity(tau, spec.glow());
        if (strength <= 0) {
            return;
        }
        Ally unit = sortie.ally(sortie.scriptedAlly());
        AtlasRegion frame = glow.get((int) Math.floorMod((long) Math.floor(seconds * GLOW_FPS), (long) glow.size));
        float scale = glowScale(spear, Math.min(tau, spec.glow()), spec.glow());
        double x = unit.renderX(alpha);
        double y = unit.renderY(alpha) + (spear ? GLOW_DY_A : 0);
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
        batch.setColor(1, 1, 1, strength);
        float width = frame.getRegionWidth();
        float height = frame.getRegionHeight();
        batch.draw(
                frame,
                Math.round(X0 + x - width / 2),
                Math.round(y - height / 2),
                width / 2,
                height / 2,
                width,
                height,
                scale,
                scale,
                0);
        batch.setColor(1, 1, 1, 1);
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
    }

    /** The lance and its flash, above the air layer, where the unit was hit (it does not follow the wreck). */
    void drawLance(SpriteBatch batch, Sortie sortie, float alpha) {
        if (loss.isEmpty() || lance.isEmpty() || sortie.scriptedAlly() < 0) {
            return;
        }
        Ally unit = sortie.ally(sortie.scriptedAlly());
        if (!unit.lost()) {
            return;
        }
        double ticks = unit.ticksSinceLost() + alpha;
        // The unit stays where it was lost (its glide is drawn apart): the hit point.
        double x = unit.x();
        double y = unit.y();
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
        int frame = lanceFrame(ticks, lance.size, spear ? LANCE_TICKS_A : LANCE_TICKS_B);
        if (frame >= 0) {
            AtlasRegion region = lance.get(frame);
            float left = Math.round(X0 + x - region.getRegionWidth() / 2.0);
            // a: the spear's tip on the hit, its top on the glow; b: the column centred on it.
            float bottom = spear
                    ? Math.round(y + LANCE_TIP_A - region.getRegionHeight())
                    : Math.round(y - region.getRegionHeight() / 2.0);
            batch.draw(region, left, bottom);
        }
        int flashFrame = lanceFrame(ticks, flash.size, FLASH_TICKS);
        if (flashFrame >= 0) {
            AtlasRegion region = flash.get(flashFrame);
            batch.draw(
                    region,
                    Math.round(X0 + x - region.getRegionWidth() / 2.0),
                    Math.round(y - region.getRegionHeight() / 2.0));
        }
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
    }
}
