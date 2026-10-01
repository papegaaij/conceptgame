package vanguard.game.scene;

import com.badlogic.gdx.utils.Disposable;

/** One of the spike's test scenes. */
public sealed interface Scene extends Disposable permits PlayScene, HaloScene, SfxScene {
    /** Advances and draws one frame. */
    void render(float delta);

    /** Scene-specific benchmark lines. */
    String report();
}
