package vanguard.game.render;

import java.util.Arrays;

/**
 * The set pieces whose death is playing: per set piece of the level the steps since its death,
 * where it died and how it was drawn there (scale and opacity off the play plane, the crossing
 * pass or facing down), for its {@link SetPieceDeath break-up}. Advanced with the simulation
 * steps like the effects; pure presentation, not part of the game state.
 */
public final class SetPieceWrecks {
    private final int[] age;
    private final float[] x;
    private final float[] y;
    private final float[] scale;
    private final float[] opacity;
    private final boolean[] crossing;

    public SetPieceWrecks(int setPieces) {
        age = new int[setPieces];
        x = new float[setPieces];
        y = new float[setPieces];
        scale = new float[setPieces];
        opacity = new float[setPieces];
        crossing = new boolean[setPieces];
        clear();
    }

    /** Set piece {@code k} died at the play-field position, drawn at {@code scale} and {@code opacity}. */
    public void start(int k, double atX, double atY, float atScale, float atOpacity, boolean onCrossing) {
        age[k] = 0;
        x[k] = (float) atX;
        y[k] = (float) atY;
        scale[k] = atScale;
        opacity[k] = atOpacity;
        crossing[k] = onCrossing;
    }

    public void step() {
        for (int k = 0; k < age.length; k++) {
            if (age[k] >= 0) {
                age[k]++;
            }
        }
    }

    public void clear() {
        Arrays.fill(age, -1);
    }

    /** Whether set piece {@code k} has died (in this attempt); its break-up draws nothing once over. */
    boolean active(int k) {
        return age[k] >= 0;
    }

    int age(int k) {
        return age[k];
    }

    float x(int k) {
        return x[k];
    }

    float y(int k) {
        return y[k];
    }

    float scale(int k) {
        return scale[k];
    }

    float opacity(int k) {
        return opacity[k];
    }

    boolean crossing(int k) {
        return crossing[k];
    }
}
