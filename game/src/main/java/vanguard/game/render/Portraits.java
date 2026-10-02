package vanguard.game.render;

import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import java.util.Locale;
import vanguard.content.Expression;

/**
 * The speakers' portraits on the sprite pages (tools/art/portraits.py, design/story/characters):
 * {@code portraits/radio-<speaker>-<expression>} at 72x72 and {@code portraits/briefing-<speaker>-<expression>}
 * at 144x144, the speaker as a slug of the data's short name ({@code The Choir} is {@code the-choir}).
 * A radio portrait may be a loop (the Choir's glyph), played at {@link #FPS}.
 */
public final class Portraits {
    /** The frame rate of an animated portrait. */
    static final float FPS = 12;

    private Portraits() {}

    /** A speaker's radio portrait frames in an expression ({@code grim}). */
    public static Array<AtlasRegion> radio(Sprites sprites, String speaker, String expression) {
        return sprites.frames("portraits/radio-" + slug(speaker) + "-" + expression);
    }

    /** The frame of a radio portrait {@code seconds} after it opened. */
    public static TextureRegion frame(Array<AtlasRegion> frames, float seconds) {
        return frames.get((int) (seconds * FPS) % frames.size);
    }

    /** A briefing speaker's portrait in an expression. */
    public static TextureRegion briefing(Sprites sprites, String speaker, Expression expression) {
        return sprites.region("portraits/briefing-" + slug(speaker) + "-" + expression.slug());
    }

    /** The speaker's name in the portraits' file names: {@code The Choir} is {@code the-choir}. */
    public static String slug(String speaker) {
        return speaker.toLowerCase(Locale.ROOT).replace(' ', '-');
    }
}
