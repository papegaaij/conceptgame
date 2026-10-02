package vanguard.game.render;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.utils.Array;

/**
 * Short animations at fixed play-field positions, started by simulation events and advanced with
 * the simulation steps, so they slow down with it: glowing ones drawn additively (explosions,
 * impact sparks) or solid ones (debris of a destroyed ground target). Pure presentation: they are
 * not part of the game state. All instances exist up front.
 */
public final class Effects {
    private static final int CAPACITY = 128;

    private static final class Effect {
        Array<AtlasRegion> frames;
        int ticksPerFrame;
        int age;
        float x;
        double y;
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

    /** Animations drawn over what is below them: debris. */
    public static Effects solid() {
        return new Effects(false);
    }

    /** Starts an animation centred on the play-field position; dropped when all are in use. */
    public void start(Array<AtlasRegion> frames, int ticksPerFrame, double x, double y) {
        if (size < CAPACITY) {
            Effect effect = effects[size++];
            effect.frames = frames;
            effect.ticksPerFrame = ticksPerFrame;
            effect.age = 0;
            effect.x = (float) x;
            effect.y = y;
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
            AtlasRegion frame = effect.frames.get(effect.age / effect.ticksPerFrame);
            batch.draw(
                    frame,
                    Math.round(PixelScreen.PLAY_FIELD_X + effect.x - frame.getRegionWidth() / 2f),
                    Math.round(effect.y + offsetY - frame.getRegionHeight() / 2.0));
        }
        if (additive) {
            batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        }
    }
}
