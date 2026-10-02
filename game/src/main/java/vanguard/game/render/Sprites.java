package vanguard.game.render;

import com.badlogic.gdx.Files;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;

/**
 * The sprite and backdrop atlases packed by {@code :pipeline:packAtlases} from the
 * placeholders: sprites cut from the chosen concept art (see {@code PlaceholderSprites}), the
 * loot targets (tools/concept/ground_targets.py) and the levels' backdrop images
 * (tools/concept/backdrop_l01.py), plus a white pixel for the HUD's bars and frames.
 */
public final class Sprites implements Disposable {
    private final TextureAtlas sprites;
    private final TextureAtlas backdrop;
    private final Texture pixelTexture;

    /** Hard left .. hard right. */
    public final Array<AtlasRegion> ship;
    /** The wing beat. */
    public final Array<AtlasRegion> skitter;
    /** The claw snap. */
    public final Array<AtlasRegion> needler;
    /** The Vrell standard orb bullet (the Needler's thorn). */
    public final AtlasRegion orb;
    /** The pickups' 8-frame spin loops. */
    public final Array<AtlasRegion> salvageSmall;

    public final Array<AtlasRegion> crate;
    public final Array<AtlasRegion> shieldCell;
    public final Array<AtlasRegion> armourPatch;
    /** The 72x72 radio portraits. */
    public final AtlasRegion rook;

    public final AtlasRegion okafor;
    public final AtlasRegion varga;
    public final AtlasRegion choir;

    public final AtlasRegion pulseBolt;
    public final Array<AtlasRegion> pulseMuzzle;
    public final Array<AtlasRegion> pulseImpact;
    public final Array<AtlasRegion> explosionTiny;
    public final Array<AtlasRegion> explosionSmall;
    public final Array<AtlasRegion> explosionLarge;
    /** Intact, damaged. */
    public final Array<AtlasRegion> cargoContainer;
    /** The damaged container breaking apart, drawn solid. */
    public final Array<AtlasRegion> cargoContainerBreak;
    /** Dark, lit; the same two after the first hit. */
    public final Array<AtlasRegion> beacon;
    /** The loot targets' sparkle, drawn additively. */
    public final Array<AtlasRegion> glint;

    public final TextureRegion pixel;

    public Sprites(Files files) {
        sprites = new TextureAtlas(files.internal("atlas/sprites.atlas"));
        backdrop = new TextureAtlas(files.internal("atlas/backdrop.atlas"));
        ship = frames(sprites, "ship");
        skitter = frames(sprites, "skitter");
        needler = frames(sprites, "needler");
        orb = region(sprites, "orb");
        salvageSmall = frames(sprites, "pickup-salvage-small");
        crate = frames(sprites, "pickup-crate");
        shieldCell = frames(sprites, "pickup-shield-cell");
        armourPatch = frames(sprites, "pickup-armour-patch");
        rook = region(sprites, "portrait-rook");
        okafor = region(sprites, "portrait-okafor");
        varga = region(sprites, "portrait-varga");
        choir = region(sprites, "portrait-the-choir");
        pulseBolt = region(sprites, "pulse-bolt");
        pulseMuzzle = frames(sprites, "pulse-muzzle");
        pulseImpact = frames(sprites, "pulse-impact");
        explosionTiny = frames(sprites, "explosion-tiny");
        explosionSmall = frames(sprites, "explosion-small");
        explosionLarge = frames(sprites, "explosion-large");
        cargoContainer = frames(sprites, "cargo-container");
        cargoContainerBreak = frames(sprites, "cargo-container-break");
        beacon = frames(sprites, "beacon");
        glint = frames(sprites, "glint");
        Pixmap white = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        white.setColor(1, 1, 1, 1);
        white.fill();
        pixelTexture = new Texture(white);
        white.dispose();
        pixel = new TextureRegion(pixelTexture);
    }

    /** A backdrop image: its {@code count} frames or headings, or the single image. */
    public Array<AtlasRegion> backdrop(String name, int count) {
        Array<AtlasRegion> images = frames(backdrop, name);
        if (images.size != count) {
            throw new IllegalStateException(
                    "'" + name + "' has " + images.size + " images in the backdrop atlas, the level expects " + count);
        }
        return images;
    }

    private static Array<AtlasRegion> frames(TextureAtlas atlas, String name) {
        Array<AtlasRegion> frames = atlas.findRegions(name);
        if (frames.isEmpty()) {
            throw new IllegalStateException("no frames '" + name + "' in the atlas");
        }
        return frames;
    }

    private static AtlasRegion region(TextureAtlas atlas, String name) {
        AtlasRegion region = atlas.findRegion(name);
        if (region == null) {
            throw new IllegalStateException("no region '" + name + "' in the atlas");
        }
        return region;
    }

    @Override
    public void dispose() {
        sprites.dispose();
        backdrop.dispose();
        pixelTexture.dispose();
    }
}
