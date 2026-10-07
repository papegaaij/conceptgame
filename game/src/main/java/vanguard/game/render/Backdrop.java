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
 * a section starts, the set pieces at their places along the scroll (a ground piece with a tower
 * block drawn in perspective by {@link TowerProjection}), and the atmosphere of the section (low-air
 * cloud banks, high-air wisps, haze over the deep and far layers), which ramps across a section
 * boundary. Every layer scrolls by its factor of the ground's distance and is drawn at whole pixels
 * (a tower's walls lean between them); nothing is allocated per frame. The pieces' places along the
 * scroll and the atmosphere go by the level clock, their paths, animation frames and the tile sets'
 * drift by the real clock (M5 part C: moving scenery keeps its speed in a hold zone, see {@link
 * BackdropClock}).
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
    private final TowerProjection towers;
    private final Look[] looks;
    /** M5 part C: the collapse's dust peak's look (Level 09's {@code heavy}); null without a collapse. */
    private final Look dust;
    /** How strongly the dust peak shows now, 0 to 1 ({@link #dustPeak}). */
    private float dustWeight;
    /** M5 part C: drawn between the ground pieces and the towers; null for nothing ({@link #underTowers}). */
    private UnderTowers underTowers;

    private final TextureRegion strip = new TextureRegion();
    private Look from;
    private Look to;
    private float weight;

    private record Tiles(TextureRegion image, double drift) {}

    private record Look(Tiles banks, Tiles wisps, float haze) {}

    /**
     * A placed set piece with its images and its bottom edge on its layer; {@code pathStart} is its
     * path's first waypoint's script time (NaN without a path).
     */
    private record Piece(
            BackdropData.PlacedPiece placed,
            BackdropData.Piece spec,
            Array<AtlasRegion> images,
            long bottom,
            double pathStart) {}

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
        String folder = folder(levelKey, level);
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
            for (BackdropData.PlacedPiece placed : data.placements()) {
                BackdropData.Piece spec = data.pieces().get(placed.piece());
                if (spec.layer() == layer && !spec.isTower()) {
                    Array<AtlasRegion> images = sprites.backdrop(folder + placed.piece(), spec.imageCount());
                    long bottom =
                            Math.round(level.pieceCentre(placed) - spec.size().height() / 2);
                    double pathStart =
                            placed.path().map(points -> points.getFirst().t()).orElse(Double.NaN);
                    onLayer.add(new Piece(placed, spec, images, bottom, pathStart));
                }
            }
            pieces[l] = onLayer.toArray(Piece[]::new);
        }
        towers = new TowerProjection(towers(sprites, folder, level), pixel);
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
        dust = level.collapse()
                .flatMap(collapse -> data.atmosphere().of(collapse.dust().atmosphere()))
                .map(look -> new Look(
                        look.banks()
                                .map(id -> tiles(
                                        sprites, folder, id, data.tileSets().get(id)))
                                .orElse(null),
                        look.wisps()
                                .map(id -> tiles(
                                        sprites, folder, id, data.tileSets().get(id)))
                                .orElse(null),
                        (float) look.haze()))
                .orElse(null);
    }

    /**
     * M5 part C: the collapse's event-triggered dust peak (Level 09) shows at {@code weight}, 0 (the
     * section's own atmosphere) to 1 (the peak's: its banks, wisps and haze), blended like a change
     * across a section boundary.
     */
    public void dustPeak(float weight) {
        dustWeight = dust == null ? 0 : Math.clamp(weight, 0, 1);
    }

    /**
     * M5 part C: the tower whose footprint's centre lies nearest {@code groundCentre} on the ground
     * layer, within {@code reach} px; -1 for none.
     */
    int towerNear(double groundCentre, double reach) {
        return towers.nearest(groundCentre, reach);
    }

    /** M5 part C: tower {@code index} ({@link #towerNear}) is not drawn (the fallen arcology); -1 draws them all. */
    void hideTower(int index) {
        towers.hide(index);
    }

    /** M5 part C: tower {@code index} ({@link #towerNear}) is drawn bent (see {@link TowerProjection#bend}); -1 for none. */
    void bendTower(int index, double lean, double shakeX, double shakeY, double share, double crush, float dust) {
        towers.bend(index, lean, shakeX, shakeY, share, crush, dust);
    }

    /** What is drawn over the ground layer's set pieces and under its towers (Level 09's collapse). */
    interface UnderTowers {
        void draw(SpriteBatch batch);
    }

    /** M5 part C: {@code layer} is drawn between the ground pieces and the towers; null for nothing. */
    void underTowers(UnderTowers layer) {
        underTowers = layer;
    }

    /** M5 part C: tower {@code index} ({@link #towerNear}). */
    TowerProjection.Tower tower(int index) {
        return towers.tower(index);
    }

    /**
     * The atlas folder a level's backdrop images come from: another level's when the data names one
     * (e.g. {@code images: level-04}, for a level without art of its own yet), else its own.
     */
    public static String folder(String levelKey, LevelData level) {
        return level.backdrop().images().map(folder -> folder + "/").orElseGet(() -> folder(levelKey));
    }

    /** The atlas folder of a level's backdrop: {@code level-NN/}. */
    static String folder(String levelKey) {
        String name = levelKey.substring(levelKey.lastIndexOf('/') + 1);
        return name.substring(0, "level-NN".length()) + "/";
    }

    /** The level's placed towers with their roofs and wall textures. */
    private static List<TowerProjection.Tower> towers(Sprites sprites, String folder, LevelData level) {
        BackdropData data = level.backdrop();
        List<TowerProjection.Tower> towers = new ArrayList<>();
        for (BackdropData.PlacedPiece placed : data.placements()) {
            BackdropData.Piece spec = data.pieces().get(placed.piece());
            if (spec.tower().isEmpty()) {
                continue;
            }
            BackdropData.Tower tower = spec.tower().get();
            AtlasRegion roof = sprites.backdrop(folder + placed.piece(), 1).first();
            if (roof.getRegionWidth() != spec.imageWidth() || roof.getRegionHeight() != spec.imageHeight()) {
                throw new IllegalStateException("'" + placed.piece() + "': a tower's roof is drawn at its scale, "
                        + spec.imageWidth() + "x" + spec.imageHeight() + ", the image is " + roof.getRegionWidth()
                        + "x" + roof.getRegionHeight());
            }
            int footWidth = (int) Math.round(spec.size().width());
            int footHeight = (int) Math.round(spec.size().height());
            double centre = level.pieceCentre(placed);
            towers.add(new TowerProjection.Tower(
                    placed.x(),
                    centre,
                    Math.round(centre - footHeight / 2.0),
                    footWidth,
                    footHeight,
                    tower.scale(),
                    tower.height(),
                    (float) tower.shadeOrDefault(),
                    roof,
                    sprites.backdrop(folder + tower.wall(), 1).first()));
        }
        return towers;
    }

    private static Tiles tiles(Sprites sprites, String folder, String id, BackdropData.TileSet tileSet) {
        return new Tiles(
                sprites.backdrop(folder + id, 1).first(), tileSet.drift().orElse(0.0));
    }

    /**
     * The layers below the ground: deep (if the level has one), far and the haze; then
     * {@link #drawGroundTiles}.
     *
     * @param groundScroll the ground layer's distance in px
     * @param clock the level clock and the real clock at the render time
     */
    public void drawBehind(SpriteBatch batch, double groundScroll, BackdropClock clock) {
        blend(clock.script());
        if (level.backdrop().hasDeep()) {
            // A surface without a deep layer (Luna) has ground tiles that cover the screen.
            drawLayer(batch, BackdropLayer.DEEP, groundScroll, clock);
        }
        drawLayer(batch, BackdropLayer.FAR, groundScroll, clock);
        float veil = from.haze() + (to.haze() - from.haze()) * weight;
        if (veil > 0) {
            batch.setColor(haze.r, haze.g, haze.b, veil);
            batch.draw(pixel, X0, 0, WIDTH, HEIGHT);
            batch.setColor(Color.WHITE);
        }
    }

    /** The ground layer's tiles, over the haze (they catch the flyers' shadows, see {@link Shadows}). */
    public void drawGroundTiles(SpriteBatch batch, double groundScroll, BackdropClock clock) {
        drawSectionTiles(batch, BackdropLayer.GROUND, scroll(BackdropLayer.GROUND, groundScroll), clock);
    }

    /**
     * The ground layer's set pieces, over its tiles and the level's road (design/campaign, Level 04:
     * the convoy apron, the bridge and the gate lie over the road), then its perspective towers,
     * lowest first, each its walls and then its roof (design/art-direction, Perspective towers are
     * scenery), with Level 09's collapse's shadow and base dust between them ({@link #underTowers}):
     * all under the ground objects and units drawn next, and marking the shadow stencil with the rest
     * of the ground (see {@link Shadows}).
     */
    public void drawGroundPieces(SpriteBatch batch, double groundScroll, BackdropClock clock) {
        drawPieces(batch, BackdropLayer.GROUND, scroll(BackdropLayer.GROUND, groundScroll), clock, false);
        if (underTowers != null) {
            underTowers.draw(batch);
        }
        towers.draw(batch, groundScroll);
    }

    /**
     * The ground layer's overhead pieces, over the convoy and the ground objects and under the
     * ground units (design/campaign, Level 04: the crawlers pass under the bridge's arches and drive
     * into the gate; the Spine Turrets stand on the arches).
     */
    public void drawOverhead(SpriteBatch batch, double groundScroll, BackdropClock clock) {
        drawPieces(batch, BackdropLayer.GROUND, scroll(BackdropLayer.GROUND, groundScroll), clock, true);
    }

    /** The low-air layer above the ground objects and below the play plane: its tiles, the cloud banks, its pieces. */
    public void drawLowAir(SpriteBatch batch, double groundScroll, BackdropClock clock) {
        blend(clock.script());
        long scroll = scroll(BackdropLayer.LOW_AIR, groundScroll);
        drawSectionTiles(batch, BackdropLayer.LOW_AIR, scroll, clock);
        drawAtmosphere(batch, from.banks(), to.banks(), scroll, clock.real());
        drawPieces(batch, BackdropLayer.LOW_AIR, scroll, clock, false);
    }

    /** The high-air layer above the play plane, additive and at most 40 % opaque. */
    public void drawFront(SpriteBatch batch, double groundScroll, BackdropClock clock) {
        blend(clock.script());
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
        long scroll = scroll(BackdropLayer.HIGH_AIR, groundScroll);
        batch.setColor(1, 1, 1, HIGH_AIR_OPACITY);
        drawSectionTiles(batch, BackdropLayer.HIGH_AIR, scroll, clock);
        drawPieces(batch, BackdropLayer.HIGH_AIR, scroll, clock, false);
        drawAtmosphere(batch, from.wisps(), to.wisps(), scroll, clock.real());
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
        if (dustWeight > 0) {
            // The collapse's dust over whatever the section shows (it falls in a section's middle).
            from = weight < 0.5f ? from : to;
            to = dust;
            weight = dustWeight;
        }
    }

    private void drawLayer(SpriteBatch batch, BackdropLayer layer, double groundScroll, BackdropClock clock) {
        long scroll = scroll(layer, groundScroll);
        drawSectionTiles(batch, layer, scroll, clock);
        drawPieces(batch, layer, scroll, clock, false);
    }

    private long scroll(BackdropLayer layer, double groundScroll) {
        return Math.round(groundScroll * factors[layer.ordinal()]);
    }

    private void drawSectionTiles(SpriteBatch batch, BackdropLayer layer, long scroll, BackdropClock clock) {
        int l = layer.ordinal();
        for (int s = 0; s < tiles[l].length; s++) {
            Tiles section = tiles[l][s];
            if (section != null) {
                long end = s + 1 < tiles[l].length ? seams[l][s + 1] : Long.MAX_VALUE;
                drawTiles(
                        batch,
                        section,
                        Math.max(seams[l][s], scroll),
                        Math.min(end, scroll + HEIGHT),
                        scroll,
                        clock.real());
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

    /** The rows {@code [from, to)} of a layer filled with a tile set that repeats from position 0, drifting by real {@code seconds}. */
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

    private void drawPieces(
            SpriteBatch batch, BackdropLayer layer, long scroll, BackdropClock clock, boolean overhead) {
        for (Piece piece : pieces[layer.ordinal()]) {
            BackdropData.PlacedPiece placed = piece.placed();
            if (placed.isOverhead() != overhead) {
                continue;
            }
            float width = (float) piece.spec().size().width();
            float height = (float) piece.spec().size().height();
            double along = Double.isNaN(piece.pathStart()) ? 0 : clock.pathTime(piece.pathStart());
            float y = piece.bottom() - scroll + Math.round(placed.offsetY(along));
            if (y >= HEIGHT || y + height <= 0) {
                continue;
            }
            float x = X0 + Math.round(placed.x() + placed.offsetX(along) - width / 2);
            TextureRegion image = piece.images().get(imageAt(piece, clock.real(), along));
            if (placed.mirrored()) {
                batch.draw(image, x + width, y, -width, height);
            } else {
                batch.draw(image, x, y);
            }
        }
    }

    /** The animation frame at real {@code seconds}, the heading along the path at {@code along}, or the only image. */
    private static int imageAt(Piece piece, double seconds, double along) {
        int count = piece.images().size;
        if (piece.spec().animated()) {
            return (int)
                    Math.floorMod((long) Math.floor(seconds * piece.spec().fps().orElseThrow()), (long) count);
        }
        if (piece.spec().headings().isPresent()) {
            return (int) Math.floorMod(Math.round(piece.placed().heading(along) * count / 360), (long) count);
        }
        return 0;
    }
}
