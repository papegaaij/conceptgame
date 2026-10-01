package vanguard.game.screen;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Disposable;

/** The screen state machine: shows one {@link GameScreen} at a time and follows its transitions. */
public final class ScreenFlow implements Disposable {
    private GameScreen current;

    public ScreenFlow(GameScreen first) {
        current = first;
    }

    /** Updates the current screen and switches to the screen it returns, disposing the old one. */
    public void update(float seconds) {
        GameScreen next = current.update(seconds);
        if (next != current) {
            current.dispose();
            current = next;
        }
    }

    public void draw(SpriteBatch batch) {
        current.draw(batch);
    }

    @Override
    public void dispose() {
        current.dispose();
    }
}
