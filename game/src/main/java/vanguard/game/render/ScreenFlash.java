package vanguard.game.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import vanguard.sim.PlayField;
import vanguard.sim.SimStep;

/**
 * An act boss's death flash (design/enemies/bosses: the death sequence's screen flash): the play
 * field white at the end of its chained death, held a moment, then fading, as the Smart Bomb's flash
 * ({@code FarsideLooks.drawSmartBomb}); the Gameplay tab's flash reduction tones it down like every
 * white flash. Advanced with the simulation steps; pure presentation.
 */
public final class ScreenFlash {
    /** Its opacity while held, how long it holds and how long it fades, s. */
    static final float OPACITY = 0.9f;

    static final double HOLD_SECONDS = 0.12;
    static final double FADE_SECONDS = 0.6;
    private static final float X0 = PixelScreen.PLAY_FIELD_X;

    /** Steps until it starts (negative once running), or {@link Integer#MIN_VALUE} while none is due. */
    private int ticks = Integer.MIN_VALUE;

    /** A flash {@code delayTicks} steps from now. */
    public void start(int delayTicks) {
        ticks = delayTicks;
    }

    public void step() {
        if (ticks != Integer.MIN_VALUE) {
            ticks--;
            if (opacity(-ticks * SimStep.SECONDS) <= 0 && ticks < 0) {
                ticks = Integer.MIN_VALUE;
            }
        }
    }

    /** No flash, as when the level restarts. */
    public void clear() {
        ticks = Integer.MIN_VALUE;
    }

    /** Its opacity {@code seconds} after it started: held, then fading to nothing; 0 before. */
    static float opacity(double seconds) {
        if (seconds < 0) {
            return 0;
        }
        if (seconds <= HOLD_SECONDS) {
            return OPACITY;
        }
        return (float) (OPACITY * Math.max(0, 1 - (seconds - HOLD_SECONDS) / FADE_SECONDS));
    }

    /** Draws it over the play field at {@code strength} (1, or the reduced white flash). */
    public void draw(SpriteBatch batch, TextureRegion pixel, float alpha, float strength) {
        if (ticks == Integer.MIN_VALUE) {
            return;
        }
        float opacity = opacity((-ticks + alpha) * SimStep.SECONDS) * strength;
        if (opacity <= 0) {
            return;
        }
        batch.setColor(1, 1, 1, opacity);
        batch.draw(pixel, X0, 0, PlayField.WIDTH, PlayField.HEIGHT);
        batch.setColor(Color.WHITE);
    }
}
