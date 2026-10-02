package vanguard.game.screen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import org.junit.jupiter.api.Test;

class ScreenFlowTest {
    /** A screen that answers {@code next} once, then stays, and records what happened to it. */
    private static final class FakeScreen implements GameScreen {
        Transition next = Transition.STAY;
        int updates;
        int draws;
        boolean disposed;

        @Override
        public Transition update(float seconds) {
            updates++;
            Transition answer = next;
            next = Transition.STAY;
            return answer;
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

    private int quits;

    private ScreenFlow flow(GameScreen first) {
        return new ScreenFlow(first, () -> quits++);
    }

    @Test
    void staysOnAScreenThatSaysStay() {
        var title = new FakeScreen();
        var flow = flow(title);

        flow.update(1 / 60f);
        flow.draw(null);

        assertEquals(1, title.draws);
        assertFalse(title.disposed);
    }

    @Test
    void anOpenedScreenLiesOverTheOldOneUntilItGoesBack() {
        var menu = new FakeScreen();
        var options = new FakeScreen();
        var flow = flow(menu);
        menu.next = Transition.open(options);

        flow.update(1 / 60f);
        flow.update(1 / 60f);
        flow.draw(null);
        assertSame(options, flow.top());
        assertEquals(1, menu.updates, "the screen below waits");
        assertEquals(0, menu.draws);
        assertFalse(menu.disposed);

        options.next = Transition.BACK;
        flow.update(1 / 60f);
        flow.draw(null);

        assertTrue(options.disposed);
        assertSame(menu, flow.top());
        assertEquals(1, menu.draws);
        assertFalse(menu.disposed, "it keeps its state, its music and its resources");
    }

    @Test
    void aReplacementClosesEveryScreen() {
        var menu = new FakeScreen();
        var difficulty = new FakeScreen();
        var level = new FakeScreen();
        var flow = flow(menu);
        menu.next = Transition.open(difficulty);
        flow.update(1 / 60f);
        difficulty.next = Transition.replace(level);

        flow.update(1 / 60f);

        assertTrue(menu.disposed);
        assertTrue(difficulty.disposed);
        assertSame(level, flow.top());
        assertEquals(1, flow.depth());
    }

    @Test
    void theLevelPauseOptionsChainReturnsStepByStep() {
        var level = new FakeScreen();
        var pause = new FakeScreen();
        var options = new FakeScreen();
        var flow = flow(level);
        level.next = Transition.open(pause);
        flow.update(1 / 60f);
        pause.next = Transition.open(options);
        flow.update(1 / 60f);
        assertEquals(3, flow.depth());

        options.next = Transition.BACK;
        flow.update(1 / 60f);
        assertSame(pause, flow.top());
        pause.next = Transition.BACK;
        flow.update(1 / 60f);

        assertSame(level, flow.top());
        assertFalse(level.disposed);
    }

    @Test
    void theLastScreenCannotGoBack() {
        var title = new FakeScreen();
        var flow = flow(title);
        title.next = Transition.BACK;

        assertThrows(IllegalStateException.class, () -> flow.update(1 / 60f));
    }

    @Test
    void quitEndsTheGame() {
        var menu = new FakeScreen();
        var flow = flow(menu);
        menu.next = Transition.QUIT;

        flow.update(1 / 60f);

        assertEquals(1, quits);
    }

    @Test
    void disposesEveryOpenScreen() {
        var level = new FakeScreen();
        var pause = new FakeScreen();
        var flow = flow(level);
        level.next = Transition.open(pause);
        flow.update(1 / 60f);

        flow.dispose();

        assertTrue(level.disposed);
        assertTrue(pause.disposed);
    }
}
