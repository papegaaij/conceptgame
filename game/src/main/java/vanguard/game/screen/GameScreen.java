package vanguard.game.screen;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Disposable;

/** One state of the screen flow in design/ui; it is disposed when the flow closes it. */
public interface GameScreen extends Disposable {
    /** Advances the screen by one frame and says where the flow goes next. */
    Transition update(float seconds);

    /** Draws the screen into the 960x540 pixel screen; {@code batch} is drawing. */
    void draw(SpriteBatch batch);
}
