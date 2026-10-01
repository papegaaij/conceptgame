package vanguard.game.screen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import org.junit.jupiter.api.Test;

class ScreenFlowTest {
    /** A screen that moves on to {@code next} once set, and records what happened to it. */
    private static final class FakeScreen implements GameScreen {
        GameScreen next = this;
        int draws;
        boolean disposed;

        @Override
        public GameScreen update(float seconds) {
            return next;
        }

        @Override
        public void draw(SpriteBatch batch) {
            draws++;
        }

        @Override
        public void dispose() {
            disposed = true;
        }
    }

    @Test
    void staysOnAScreenThatReturnsItself() {
        var title = new FakeScreen();
        var flow = new ScreenFlow(title);

        flow.update(1 / 60f);
        flow.draw(null);

        assertEquals(1, title.draws);
        assertFalse(title.disposed);
    }

    @Test
    void switchesToTheReturnedScreenAndDisposesTheOldOne() {
        var title = new FakeScreen();
        var menu = new FakeScreen();
        var flow = new ScreenFlow(title);
        title.next = menu;

        flow.update(1 / 60f);
        flow.draw(null);

        assertTrue(title.disposed);
        assertEquals(0, title.draws);
        assertEquals(1, menu.draws);
    }

    @Test
    void disposesTheCurrentScreen() {
        var title = new FakeScreen();
        new ScreenFlow(title).dispose();

        assertTrue(title.disposed);
    }
}
