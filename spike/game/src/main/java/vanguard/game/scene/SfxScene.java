package vanguard.game.scene;

import com.badlogic.gdx.Gdx;
import vanguard.game.audio.MusicStreamer;
import vanguard.game.audio.Sfx;
import vanguard.game.audio.SfxBank;
import vanguard.game.render.PixelScreen;

/**
 * Gate 5: once a second, starts all sound effects twice (at two pitches), 32 at the same moment,
 * over the streaming music. A flash on screen marks each burst.
 */
public final class SfxScene implements Scene {
    private static final float INTERVAL = 1f;
    private static final float[] PITCHES = {1f, 1.12f};

    private final SharedGraphics graphics;
    private final SfxBank sfx = new SfxBank(Gdx.audio, Gdx.files);
    private final MusicStreamer music = MusicStreamer.coalitionRising(Gdx.audio, Gdx.files);
    private float sinceBurst = INTERVAL;
    private int bursts;

    public SfxScene(SharedGraphics graphics) {
        this.graphics = graphics;
    }

    @Override
    public void render(float delta) {
        sinceBurst += delta;
        if (sinceBurst >= INTERVAL) {
            sinceBurst = 0;
            bursts++;
            for (float pitch : PITCHES) {
                for (Sfx effect : Sfx.values()) {
                    sfx.play(effect, 0.15f, pitch);
                }
            }
        }
        var batch = graphics.batch();
        graphics.screen().begin(batch);
        float flash = Math.max(0, 1 - sinceBurst * 4);
        batch.setColor(flash, flash * 0.6f, 0.1f, 1);
        batch.draw(graphics.art().pixel, PixelScreen.PLAY_FIELD_X, 0, 480, PixelScreen.HEIGHT);
        batch.setColor(1, 1, 1, 1);
        graphics.screen().end(batch);
    }

    @Override
    public String report() {
        return "sfx               %d bursts of %d, %d plays, %d without a free source%n".formatted(bursts,
                Sfx.values().length * PITCHES.length, sfx.plays(), sfx.dropouts());
    }

    @Override
    public void dispose() {
        music.close();
        sfx.dispose();
    }
}
