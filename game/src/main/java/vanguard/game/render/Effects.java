package vanguard.game.render;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.utils.Array;

/**
 * Short additive animations at fixed play-field positions (explosions, impact sparks), started by
 * simulation events and advanced with the simulation steps, so they slow down with it. Pure
 * presentation: they are not part of the game state. All instances exist up front.
 */
public final class Effects {
    private static final int CAPACITY = 128;

    private static final class Effect {
        Array<AtlasRegion> frames;
        int ticksPerFrame;
        int age;
        float x;
        float y;
    }

    private final Effect[] effects = new Effect[CAPACITY];
    private int size;

    public Effects() {
        for (int i = 0; i < CAPACITY; i++) {
            effects[i] = new Effect();
        }
    }

    /** Starts an animation centred on the play-field position; dropped when all are in use. */
    public void start(Array<AtlasRegion> frames, int ticksPerFrame, double x, double y) {
        if (size < CAPACITY) {
            Effect effect = effects[size++];
            effect.frames = frames;
            effect.ticksPerFrame = ticksPerFrame;
            effect.age = 0;
            effect.x = (float) x;
            effect.y = (float) y;
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

    public void draw(SpriteBatch batch) {
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
        for (int i = 0; i < size; i++) {
            Effect effect = effects[i];
            AtlasRegion frame = effect.frames.get(effect.age / effect.ticksPerFrame);
            batch.draw(
                    frame,
                    Math.round(PixelScreen.PLAY_FIELD_X + effect.x - frame.getRegionWidth() / 2f),
                    Math.round(effect.y - frame.getRegionHeight() / 2f));
        }
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
    }
}
