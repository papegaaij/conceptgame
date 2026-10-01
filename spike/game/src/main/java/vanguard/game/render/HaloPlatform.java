package vanguard.game.render;

import com.badlogic.gdx.Files;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;

/**
 * The Halo Platform ring as a pre-rendered angle set: 768 frames, one per 0.47 degrees, picked
 * by angle instead of rotating the sprite at draw time (design/art-direction "Smooth slow
 * rotation"). Loading reports how much video memory the atlas pages took.
 */
public final class HaloPlatform implements Disposable {
    /** Ring speed at the start of the fight (design/enemies, Halo Platform). */
    public static final float BASE_SPEED = 0.35f;

    private final TextureAtlas atlas;
    private final Array<AtlasRegion> frames;
    private final int videoMemoryKib;

    public HaloPlatform(Files files, VideoMemory videoMemory) {
        int freeBefore = videoMemory.freeKib();
        atlas = new TextureAtlas(files.internal("atlas/halo.atlas"));
        frames = atlas.findRegions("halo");
        videoMemoryKib = freeBefore < 0 ? -1 : freeBefore - videoMemory.freeKib();
    }

    /** Draws the frame for {@code angle} (radians, counter-clockwise) centred on a point. */
    public void draw(SpriteBatch batch, float angle, float centreX, float centreY) {
        float turns = angle / (2 * (float) Math.PI);
        int index = (int) Math.floor((turns - Math.floor(turns)) * frames.size) % frames.size;
        AtlasRegion frame = frames.get(index);
        batch.draw(frame, centreX - frame.originalWidth / 2f, centreY - frame.originalHeight / 2f);
    }

    /** Frame count, atlas pages and the video memory they took. */
    public String describe() {
        var page = atlas.getTextures().first();
        return "%d frames on %d pages of %dx%d, video memory %s".formatted(frames.size, atlas.getTextures().size,
                page.getWidth(), page.getHeight(),
                videoMemoryKib < 0 ? "n/a" : "%.1f MiB".formatted(videoMemoryKib / 1024.0));
    }

    @Override
    public void dispose() {
        atlas.dispose();
    }
}
