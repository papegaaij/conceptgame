package vanguard.game.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import java.util.ArrayList;
import java.util.List;
import vanguard.content.BackdropData;
import vanguard.content.BackdropLayer;
import vanguard.content.LevelData;
import vanguard.sim.PlayField;

/**
 * A level's parallax backdrop as its data file lays it out (design/art-direction, Parallax layer
 * model): per layer the sections' tile sets, which change at a seam entering at the top edge when
 * a section starts, the set pieces at their places along the scroll, and the atmosphere of the
 * section (low-air cloud banks, high-air wisps, haze over the deep and far layers), which ramps
 * across a section boundary. Every layer scrolls by its factor of the ground's distance and is
 * drawn at whole pixels; nothing is allocated per frame.
 */
public final class Backdrop {
    private static final int WIDTH = PlayField.WIDTH;
    private static final int HEIGHT = PlayField.HEIGHT;
    private static final float X0 = PixelScreen.PLAY_FIELD_X;
    /** Weather on high-air may cover the play plane at no more than 40 % opacity. */
    private static final float HIGH_AIR_OPACITY = 0.4f;

    private final LevelData level;
    private final double ramp;
    /** The atmosphere along the level, each with its look in {@link #looks}. */
    private final List<LevelData.Stretch> stretches;

    private final TextureRegion pixel;
    private final Color haze;
    private final double[] factors = new double[BackdropLayer.values().length];
    /** Per layer and section: the tile set, or null where the layer is empty. */
    private final Tiles[][] tiles;
    /** Per layer and section: where the section's tile set begins on the layer. */
    private final long[][] seams;

    private final Piece[][] pieces;
    private final Look[] looks;
    private final TextureRegion strip = new TextureRegion();
    private Look from;
    private Look to;
    private float weight;

    private record Tiles(TextureRegion image, double drift) {}

    private record Look(Tiles banks, Tiles wisps, float haze) {}

    /** A placed set piece with its images and its bottom edge on its layer. */
    private record Piece(
            BackdropData.PlacedPiece placed, BackdropData.Piece spec, Array<AtlasRegion> images, long bottom) {}

    /**
     * @param sprites the backdrop atlas, which holds the level's images as {@code level-NN/<id>}
     * @param levelKey the level's key, {@code <act>/level-NN-<slug>}
     */
    public Backdrop(Sprites sprites, LevelData level, String levelKey) {
        this.level = level;
        BackdropData data = level.backdrop();
        ramp = data.ramp();
        pixel = sprites.pixel;
        haze = Color.valueOf(data.hazeColour());
        String folder = folder(levelKey);
        int layerCount = BackdropLayer.values().length;
        int sectionCount = level.sections().size();
        tiles = new Tiles[layerCount][sectionCount];
        seams = new long[layerCount][sectionCount];
        pieces = new Piece[layerCount][];
        for (BackdropLayer layer : BackdropLayer.values()) {
            int l = layer.ordinal();
            factors[l] = data.factor(layer);
            for (int s = 0; s < sectionCount; s++) {
                seams[l][s] = s == 0 ? Long.MIN_VALUE : Math.round(level.seam(layer, s));
            }
            List<Piece> onLayer = new ArrayList<>();
            for (BackdropData.PlacedPiece placed : data.placed()) {
                BackdropData.Piece spec = data.pieces().get(placed.piece());
                if (spec.layer() == layer) {
                    Array<AtlasRegion> images = sprites.backdrop(folder + placed.piece(), spec.imageCount());
                    long bottom =
                            Math.round(level.pieceCentre(placed) - spec.size().height() / 2);
                    onLayer.add(new Piece(placed, spec, images, bottom));
                }
            }
            pieces[l] = onLayer.toArray(Piece[]::new);
        }
        for (int s = 0; s < sectionCount; s++) {
            for (String id : level.sections().get(s).tiles()) {
                BackdropData.TileSet tileSet = data.tileSets().get(id);
                tiles[tileSet.layer().ordinal()][s] = tiles(sprites, folder, id, tileSet);
            }
        }
        stretches = level.atmosphereStretches();
        looks = new Look[stretches.size()];
        for (int s = 0; s < looks.length; s++) {
            BackdropData.Look look =
                    data.atmosphere().of(stretches.get(s).atmosphere()).orElseThrow();
            looks[s] = new Look(
                    look.banks()
                            .map(id ->
                                    tiles(sprites, folder, id, data.tileSets().get(id)))
                            .orElse(null),
                    look.wisps()
                            .map(id ->
                                    tiles(sprites, folder, id, data.tileSets().get(id)))
                            .orElse(null),
                    (float) look.haze());
        }
    }

    /** The atlas folder of a level's backdrop: {@code level-NN/}. */
    static String folder(String levelKey) {
        String name = levelKey.substring(levelKey.lastIndexOf('/') + 1);
        return name.substring(0, "level-NN".length()) + "/";
    }

    private static Tiles tiles(Sprites sprites, String folder, String id, BackdropData.TileSet tileSet) {
        return new Tiles(
                sprites.backdrop(folder + id, 1).first(), tileSet.drift().orElse(0.0));
    }

    /**
     * The layers below the ground objects: deep, far, the haze and ground.
     *
     * @param groundScroll the ground layer's distance in px
     * @param seconds the time since the level start
     */
    public void drawBehind(SpriteBatch batch, double groundScroll, double seconds) {
        blend(seconds);
        drawLayer(batch, BackdropLayer.DEEP, groundScroll, seconds);
        drawLayer(batch, BackdropLayer.FAR, groundScroll, seconds);
        float veil = from.haze() + (to.haze() - from.haze()) * weight;
        if (veil > 0) {
            batch.setColor(haze.r, haze.g, haze.b, veil);
            batch.draw(pixel, X0, 0, WIDTH, HEIGHT);
            batch.setColor(Color.WHITE);
        }
        drawLayer(batch, BackdropLayer.GROUND, groundScroll, seconds);
    }

    /** The low-air layer above the ground objects and below the play plane: its tiles, the cloud banks, its pieces. */
    public void drawLowAir(SpriteBatch batch, double groundScroll, double seconds) {
        blend(seconds);
        long scroll = scroll(BackdropLayer.LOW_AIR, groundScroll);
        drawSectionTiles(batch, BackdropLayer.LOW_AIR, scroll, seconds);
        drawAtmosphere(batch, from.banks(), to.banks(), scroll, seconds);
        drawPieces(batch, BackdropLayer.LOW_AIR, scroll, seconds);
    }

    /** The high-air layer above the play plane, additive and at most 40 % opaque. */
    public void drawFront(SpriteBatch batch, double groundScroll, double seconds) {
        blend(seconds);
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
        long scroll = scroll(BackdropLayer.HIGH_AIR, groundScroll);
        batch.setColor(1, 1, 1, HIGH_AIR_OPACITY);
        drawSectionTiles(batch, BackdropLayer.HIGH_AIR, scroll, seconds);
        drawPieces(batch, BackdropLayer.HIGH_AIR, scroll, seconds);
        drawAtmosphere(batch, from.wisps(), to.wisps(), scroll, seconds);
        batch.setColor(Color.WHITE);
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
    }

    /**
     * Sets the looks to draw: the atmosphere stretch's (a section, or its peak), or the two across a
     * stretch boundary while the change ramps.
     */
    private void blend(double seconds) {
        int current = looks.length - 1;
        for (int s = 0; s < looks.length; s++) {
            if (seconds < stretches.get(s).end()) {
                current = s;
                break;
            }
        }
        from = to = looks[current];
        weight = 1;
        for (int s = 1; s < looks.length; s++) {
            double progress = (seconds - stretches.get(s).start()) / ramp + 0.5;
            if (progress > 0 && progress < 1) {
                from = looks[s - 1];
                to = looks[s];
                weight = (float) (progress * progress * (3 - 2 * progress));
            }
        }
    }

    private void drawLayer(SpriteBatch batch, BackdropLayer layer, double groundScroll, double seconds) {
        long scroll = scroll(layer, groundScroll);
        drawSectionTiles(batch, layer, scroll, seconds);
        drawPieces(batch, layer, scroll, seconds);
    }

    private long scroll(BackdropLayer layer, double groundScroll) {
        return Math.round(groundScroll * factors[layer.ordinal()]);
    }

    private void drawSectionTiles(SpriteBatch batch, BackdropLayer layer, long scroll, double seconds) {
        int l = layer.ordinal();
        for (int s = 0; s < tiles[l].length; s++) {
            Tiles section = tiles[l][s];
            if (section != null) {
                long end = s + 1 < tiles[l].length ? seams[l][s + 1] : Long.MAX_VALUE;
                drawTiles(
                        batch, section, Math.max(seams[l][s], scroll), Math.min(end, scroll + HEIGHT), scroll, seconds);
            }
        }
    }

    /** One look's tile set, or two crossfading while the atmosphere changes. */
    private void drawAtmosphere(SpriteBatch batch, Tiles old, Tiles current, long scroll, double seconds) {
        float alpha = batch.getColor().a;
        if (old != current && old != null) {
            batch.setColor(1, 1, 1, alpha * (1 - weight));
            drawTiles(batch, old, scroll, scroll + HEIGHT, scroll, seconds);
        }
        if (current != null) {
            batch.setColor(1, 1, 1, old == current ? alpha : alpha * weight);
            drawTiles(batch, current, scroll, scroll + HEIGHT, scroll, seconds);
        }
        batch.setColor(1, 1, 1, alpha);
    }

    /** The rows {@code [from, to)} of a layer filled with a tile set that repeats from position 0. */
    private void drawTiles(SpriteBatch batch, Tiles tileSet, long from, long to, long scroll, double seconds) {
        TextureRegion image = tileSet.image();
        int height = image.getRegionHeight();
        int shift = (int) Math.floorMod(Math.round(tileSet.drift() * seconds), (long) WIDTH);
        for (long row = from; row < to; ) {
            int inTile = (int) Math.floorMod(row, (long) height);
            int rows = (int) Math.min(height - inTile, to - row);
            float y = row - scroll;
            // The tile image's top row is its highest; a strip [inTile, inTile + rows) counts from its bottom.
            int top = height - inTile - rows;
            drawStrip(batch, image, 0, top, WIDTH - shift, rows, X0 + shift, y);
            if (shift > 0) {
                drawStrip(batch, image, WIDTH - shift, top, shift, rows, X0, y);
            }
            row += rows;
        }
    }

    private void drawStrip(
            SpriteBatch batch, TextureRegion image, int x, int y, int width, int height, float screenX, float screenY) {
        strip.setRegion(image, x, y, width, height);
        batch.draw(strip, screenX, screenY);
    }

    private void drawPieces(SpriteBatch batch, BackdropLayer layer, long scroll, double seconds) {
        for (Piece piece : pieces[layer.ordinal()]) {
            BackdropData.PlacedPiece placed = piece.placed();
            float width = (float) piece.spec().size().width();
            float height = (float) piece.spec().size().height();
            float y = piece.bottom() - scroll + Math.round(placed.offsetY(seconds));
            if (y >= HEIGHT || y + height <= 0) {
                continue;
            }
            float x = X0 + Math.round(placed.x() + placed.offsetX(seconds) - width / 2);
            TextureRegion image = piece.images().get(imageAt(piece, seconds));
            if (placed.mirrored()) {
                batch.draw(image, x + width, y, -width, height);
            } else {
                batch.draw(image, x, y);
            }
        }
    }

    /** The animation frame, the heading along the path, or the only image. */
    private static int imageAt(Piece piece, double seconds) {
        int count = piece.images().size;
        if (piece.spec().animated()) {
            return (int)
                    Math.floorMod((long) Math.floor(seconds * piece.spec().fps().orElseThrow()), (long) count);
        }
        if (piece.spec().headings().isPresent()) {
            return (int) Math.floorMod(Math.round(piece.placed().heading(seconds) * count / 360), (long) count);
        }
        return 0;
    }
}
