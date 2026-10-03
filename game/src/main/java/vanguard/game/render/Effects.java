package vanguard.game.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.utils.Array;

/**
 * Short animations at fixed play-field positions, started by simulation events and advanced with
 * the simulation steps, so they slow down with it: glowing ones drawn additively (explosions,
 * impact sparks, a Vrell's spore cloud) or solid ones (debris of a destroyed ground target, a flyer's
 * membrane tatters); an animation can be drawn scaled and faded, as a set piece off the play plane.
 * Pure presentation: they are not part of the game state. All instances exist up front.
 */
public final class Effects {
    private static final int CAPACITY = 128;

    private static final class Effect {
        Array<AtlasRegion> frames;
        int ticksPerFrame;
        int age;
        float x;
        double y;
        float scale;
        float opacity;
    }

    private final Effect[] effects = new Effect[CAPACITY];
    private final boolean additive;
    private int size;

    private Effects(boolean additive) {
        this.additive = additive;
        for (int i = 0; i < CAPACITY; i++) {
            effects[i] = new Effect();
        }
    }

    /** Animations that add light: explosions, impact sparks. */
    public static Effects glowing() {
        return new Effects(true);
    }

    /** Animations drawn over what is below them: debris, tatters. */
    public static Effects solid() {
        return new Effects(false);
    }

    /** Starts an animation centred on the play-field position; dropped when all are in use. */
    public void start(Array<AtlasRegion> frames, int ticksPerFrame, double x, double y) {
        start(frames, ticksPerFrame, x, y, 0);
    }

    /** Starts an animation that shows after {@code delayTicks} steps (a chain of bursts). */
    public void start(Array<AtlasRegion> frames, int ticksPerFrame, double x, double y, int delayTicks) {
        start(frames, ticksPerFrame, x, y, delayTicks, 1, 1);
    }

    /** Starts a delayed animation drawn at {@code scale} and {@code opacity} (0..1). */
    public void start(
            Array<AtlasRegion> frames,
            int ticksPerFrame,
            double x,
            double y,
            int delayTicks,
            float scale,
            float opacity) {
        if (size < CAPACITY) {
            Effect effect = effects[size++];
            effect.frames = frames;
            effect.ticksPerFrame = ticksPerFrame;
            effect.age = -delayTicks;
            effect.x = (float) x;
            effect.y = y;
            effect.scale = scale;
            effect.opacity = opacity;
        }
    }

    /** Advances every animation by one simulation step and drops the finished ones. */
    public void step() {
        for (int i = size - 1; i >= 0; i--) {
            Effect effect = effects[i];
            if (++effect.age >= effect.frames.size * effect.ticksPerFrame) {
                effects[i] = effects[--size];
                effects[size] = effect;
            }
        }
    }

    public void clear() {
        size = 0;
    }

    /**
     * @param offsetY added to every animation's y: 0 for play-field positions, minus the ground's
     *     scroll for animations started at ground positions (y plus the scroll at their start)
     */
    public void draw(SpriteBatch batch, double offsetY) {
        if (additive) {
            batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
        }
        for (int i = 0; i < size; i++) {
            Effect effect = effects[i];
            if (effect.age < 0) {
                continue;
            }
            AtlasRegion frame = effect.frames.get(effect.age / effect.ticksPerFrame);
            float width = frame.getRegionWidth();
            float height = frame.getRegionHeight();
            float left = Math.round(PixelScreen.PLAY_FIELD_X + effect.x - width / 2);
            float bottom = Math.round(effect.y + offsetY - height / 2);
            if (effect.scale == 1 && effect.opacity == 1) {
                batch.draw(frame, left, bottom);
            } else {
                batch.setColor(1, 1, 1, effect.opacity);
                batch.draw(frame, left, bottom, width / 2, height / 2, width, height, effect.scale, effect.scale, 0);
                batch.setColor(Color.WHITE);
            }
        }
        if (additive) {
            batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        }
    }
}
