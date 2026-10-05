package vanguard.game.screen;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import vanguard.game.GameServices;
import vanguard.game.audio.Sfx;
import vanguard.game.input.Action;
import vanguard.game.render.PixelScreen;
import vanguard.game.ui.CreditsRoll;
import vanguard.game.ui.Fonts;
import vanguard.game.ui.Glass;

/**
 * The credits (design/ui/credits, chosen credits-r08-a): the roll of {@code ui/credits.txt}, the
 * game's own credits around the attributions that CREDITS.md's licences ask for, scrolls up a glass
 * column over the dimmed title scene at a reading pace and stops with its last line in the middle.
 * Holding confirm or down (Enter, the D-pad, A) runs it faster, holding up runs it back; back (Esc /
 * B) returns to the screen below at any time, as confirm does once the roll has stopped.
 */
public final class CreditsScreen implements GameScreen {
    /** The roll file, written from CREDITS.md by {@code :pipeline:credits}. */
    public static final String FILE = "ui/credits.txt";

    public static final int COLUMN_X = 200;
    public static final int COLUMN_WIDTH = 560;
    /** The text's margin inside the column on either side. */
    public static final int PADDING = 20;
    /** The width the roll's lines are wrapped to. */
    public static final int TEXT_WIDTH = COLUMN_WIDTH - 2 * PADDING;

    /** The roll shows between these heights, fading over {@link #FADE} px at either edge. */
    static final int TOP = 14;

    static final int BOTTOM = PixelScreen.HEIGHT - 40;
    static final int FADE = 40;
    /** The reading pace, px per second, and the factor holding a direction or confirm applies. */
    static final float SPEED = 30;

    static final float FAST = 8;

    static final String HINTS = "HOLD ENTER/DOWN FASTER    HOLD UP REWIND    ESC BACK";
    static final String END_HINTS = "ENTER / ESC BACK";

    private final GameServices services;
    private final CreditsRoll roll;
    /** Where the roll's top is: it starts just below the column and ends with its last line in the middle. */
    private float offset = BOTTOM;

    public CreditsScreen(GameServices services) {
        this.services = services;
        Fonts fonts = services.glass.fonts;
        roll = CreditsRoll.layout(
                services.files.internal(FILE).readString("UTF-8"),
                TEXT_WIDTH,
                size -> Fonts.advance(CreditsRoll.font(fonts, size)),
                Math.round(services.titleScene.logoHeight(CreditsRoll.LOGO_WIDTH)));
    }

    /** The offset at which the roll stops: its last line in the middle of the column. */
    private float end() {
        return (TOP + BOTTOM) / 2f - roll.height();
    }

    @Override
    public Transition update(float seconds) {
        boolean ended = offset <= end();
        if (services.menu.back() || (ended && services.menu.confirm())) {
            services.play(Sfx.MENU_BACK);
            return Transition.BACK;
        }
        float speed = SPEED;
        if (services.input.held(Action.MENU_UP)) {
            speed = -SPEED * FAST;
        } else if (services.input.held(Action.MENU_CONFIRM) || services.input.held(Action.MENU_DOWN)) {
            speed = SPEED * FAST;
        }
        offset = Math.clamp(offset - speed * seconds, end(), BOTTOM);
        return Transition.STAY;
    }

    @Override
    public void draw(SpriteBatch batch) {
        Glass glass = services.glass;
        services.titleScene.draw(batch, 0.5f);
        // The column runs past the top and bottom of the screen: only its sides' trim shows.
        glass.panel(batch, COLUMN_X, -24, COLUMN_WIDTH, PixelScreen.HEIGHT + 48);
        roll.draw(
                batch, glass, services.titleScene, COLUMN_X + COLUMN_WIDTH / 2f, Math.round(offset), TOP, BOTTOM, FADE);
        glass.hints(batch, offset <= end() ? END_HINTS : HINTS);
    }

    @Override
    public void dispose() {}
}
