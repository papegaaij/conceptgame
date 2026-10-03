package vanguard.game.render;

import com.badlogic.gdx.Files;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import java.util.Comparator;

/**
 * The sprite and backdrop atlases packed by {@code :pipeline:packAtlases} from {@code assets/}:
 * the final sprites rendered by tools/art/ (ship, Level 01's enemies, weapon effects, bullets,
 * pickups, explosions, loot targets), the placeholders still cut from the chosen concept art (see
 * {@code PlaceholderSprites}), the HUD's metal parts (tools/art/hud.py), the glass UI kit
 * (tools/art/ui_kit.py), the hangar's equipment icons (tools/art/icons.py) and intel portraits
 * (tools/art/intel.py) and the speakers' portraits (tools/art/portraits.py), looked up by name,
 * and the levels' backdrop images (tools/art/backdrop_l01.py), plus a white pixel for drawn lines.
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
    /** The Vrell standard orb bullet (the Needler's thorn): its core pulse. */
    public final Array<AtlasRegion> orb;
    /** The pickups' 8-frame spin loops. */
    public final Array<AtlasRegion> salvageSmall;

    public final Array<AtlasRegion> crate;
    public final Array<AtlasRegion> shieldCell;
    public final Array<AtlasRegion> armourPatch;
    public final AtlasRegion pulseBolt;
    public final Array<AtlasRegion> pulseMuzzle;
    public final Array<AtlasRegion> pulseImpact;
    public final Array<AtlasRegion> explosionTiny;
    public final Array<AtlasRegion> explosionSmall;
    public final Array<AtlasRegion> explosionLarge;
    /** Dark, lit; the same two after the first hit. */
    public final Array<AtlasRegion> beacon;
    /** The loot targets' sparkle, drawn additively. */
    public final Array<AtlasRegion> glint;

    /** The 240x540 side-panel plates of the HUD. */
    public final AtlasRegion hudPanelLeft;

    public final AtlasRegion hudPanelRight;
    /** The HUD's 120x22 label plate. */
    public final AtlasRegion hudPlate;
    /** The HUD's recessed LCD well, also its bars' trough. */
    public final NinePatch hudWell;
    /** The radio portrait's well with its idle screen. */
    public final NinePatch hudPortrait;
    /** A grey phosphor bar cell, tinted for every bar, segment and pip. */
    public final NinePatch hudFill;
    /** The white glow behind a readout, tinted with its colour. */
    public final NinePatch hudGlow;

    public final TextureRegion pixel;

    public Sprites(Files files) {
        sprites = new TextureAtlas(files.internal("atlas/sprites.atlas"));
        backdrop = new TextureAtlas(files.internal("atlas/backdrop.atlas"));
        ship = frames(sprites, "ship");
        skitter = frames(sprites, "skitter");
        needler = frames(sprites, "needler");
        orb = frames(sprites, "orb");
        salvageSmall = frames(sprites, "pickup-salvage-small");
        crate = frames(sprites, "pickup-crate");
        shieldCell = frames(sprites, "pickup-shield-cell");
        armourPatch = frames(sprites, "pickup-armour-patch");
        pulseBolt = region(sprites, "pulse-bolt");
        pulseMuzzle = frames(sprites, "pulse-muzzle");
        pulseImpact = frames(sprites, "pulse-impact");
        explosionTiny = frames(sprites, "explosion-tiny");
        explosionSmall = frames(sprites, "explosion-small");
        explosionLarge = frames(sprites, "explosion-large");
        beacon = frames(sprites, "beacon");
        glint = frames(sprites, "glint");
        hudPanelLeft = region(sprites, "hud/panel-left");
        hudPanelRight = region(sprites, "hud/panel-right");
        hudPlate = region(sprites, "hud/plate");
        hudWell = patch(sprites, "hud/well");
        hudPortrait = patch(sprites, "hud/portrait");
        hudFill = patch(sprites, "hud/fill");
        hudGlow = patch(sprites, "hud/glow");
        Pixmap white = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        white.setColor(1, 1, 1, 1);
        white.fill();
        pixelTexture = new Texture(white);
        white.dispose();
        pixel = new TextureRegion(pixelTexture);
    }

    /** A region of the sprite pages by its name, such as {@code ui/knob}; it must exist. */
    public AtlasRegion region(String name) {
        return region(sprites, name);
    }

    /**
     * The frames of a region of the sprite pages by its name in their index order ({@code _0},
     * {@code _1}, ...), or the single region; it must exist.
     */
    public Array<AtlasRegion> frames(String name) {
        return frames(sprites, name);
    }

    /** Whether the backdrop pages hold an image of that name ({@code level-NN/<id>}). */
    public boolean hasBackdrop(String name) {
        return backdrop.findRegion(name) != null;
    }

    /** Whether the sprite pages hold a region of that name. */
    public boolean has(String name) {
        return sprites.findRegion(name) != null;
    }

    /** A nine-patch of the sprite pages by its name, such as {@code ui/frame}; it must exist. */
    public NinePatch patch(String name) {
        return patch(sprites, name);
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
        // The atlas lists a sprite's frames page by page; animations and sway sets go by the index.
        frames.sort(Comparator.comparingInt(region -> region.index));
        return frames;
    }

    private static AtlasRegion region(TextureAtlas atlas, String name) {
        AtlasRegion region = atlas.findRegion(name);
        if (region == null) {
            throw new IllegalStateException("no region '" + name + "' in the atlas");
        }
        return region;
    }

    /** A nine-patch: a region packed from a {@code .9.png} with its splits. */
    private static NinePatch patch(TextureAtlas atlas, String name) {
        NinePatch patch = atlas.createPatch(name);
        if (patch == null) {
            throw new IllegalStateException("no nine-patch '" + name + "' in the atlas");
        }
        return patch;
    }

    @Override
    public void dispose() {
        sprites.dispose();
        backdrop.dispose();
        pixelTexture.dispose();
    }
}
