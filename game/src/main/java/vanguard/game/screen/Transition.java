package vanguard.game.screen;

/** What a screen asks the {@link ScreenFlow} to do after a frame. */
public sealed interface Transition {
    /** Stay on this screen. */
    Transition STAY = new Stay();
    /** Close this screen and return to the one below it. */
    Transition BACK = new Back();
    /** Quit the game. */
    Transition QUIT = new Quit();

    record Stay() implements Transition {}

    /** Open {@code screen} over this one, which waits below it, neither updated nor drawn by the flow. */
    record Open(GameScreen screen) implements Transition {}

    record Back() implements Transition {}

    /** Close every screen and show {@code screen}. */
    record Replace(GameScreen screen) implements Transition {}

    record Quit() implements Transition {}

    static Transition open(GameScreen screen) {
        return new Open(screen);
    }

    static Transition replace(GameScreen screen) {
        return new Replace(screen);
    }
}
