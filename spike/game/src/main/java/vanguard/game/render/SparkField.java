package vanguard.game.render;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;

/**
 * Cosmetic hit sparks, outside the simulation. Stored as parallel float arrays of fixed capacity,
 * so spawning and updating never allocate; when full, new sparks are skipped.
 */
public final class SparkField {
    private static final float LIFETIME = 0.45f;

    private final float[] x;
    private final float[] y;
    private final float[] vx;
    private final float[] vy;
    private final float[] age;
    private int count;

    public SparkField(int capacity) {
        x = new float[capacity];
        y = new float[capacity];
        vx = new float[capacity];
        vy = new float[capacity];
        age = new float[capacity];
    }

    /** Spawns {@code amount} sparks flying apart from a play-field position. */
    public void burst(float atX, float atY, int amount, float speed) {
        for (int i = 0; i < amount && count < x.length; i++, count++) {
            float angle = MathUtils.random(MathUtils.PI2);
            float velocity = speed * MathUtils.random(0.3f, 1f);
            x[count] = atX;
            y[count] = atY;
            vx[count] = MathUtils.cos(angle) * velocity;
            vy[count] = MathUtils.sin(angle) * velocity;
            age[count] = 0;
        }
    }

    public void update(float delta) {
        for (int i = count - 1; i >= 0; i--) {
            age[i] += delta;
            if (age[i] >= LIFETIME) {
                removeAt(i);
            } else {
                x[i] += vx[i] * delta;
                y[i] += vy[i] * delta;
            }
        }
    }

    private void removeAt(int i) {
        int last = --count;
        x[i] = x[last];
        y[i] = y[last];
        vx[i] = vx[last];
        vy[i] = vy[last];
        age[i] = age[last];
    }

    /** Draws all sparks; call with additive blending and the play-field offset applied. */
    public void draw(SpriteBatch batch, TextureRegion region, float offsetX) {
        float half = region.getRegionWidth() / 2f;
        for (int i = 0; i < count; i++) {
            float life = 1 - age[i] / LIFETIME;
            batch.setColor(1f, 0.75f * life + 0.25f, 0.3f * life, life);
            batch.draw(region, offsetX + x[i] - half, y[i] - half);
        }
        batch.setColor(1, 1, 1, 1);
    }

    public int count() {
        return count;
    }
}
