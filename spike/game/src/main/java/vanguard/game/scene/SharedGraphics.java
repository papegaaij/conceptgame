package vanguard.game.scene;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import vanguard.game.render.PixelScreen;
import vanguard.game.render.ProceduralArt;
import vanguard.game.render.VideoMemory;

/** Graphics resources every scene uses; owned and disposed by the game. */
public record SharedGraphics(SpriteBatch batch, PixelScreen screen, ProceduralArt art, VideoMemory videoMemory) {
}
