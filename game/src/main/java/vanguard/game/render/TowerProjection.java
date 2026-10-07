package vanguard.game.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.NumberUtils;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import vanguard.content.BackdropData;
import vanguard.sim.PlayField;

/**
 * A level's perspective towers (design/art-direction, Perspective towers are scenery): each frame
 * every tower on screen is projected round the play field's projection centre with parallax B's
 * camera model, a roof {@code h} camera units high drawn at k = 6 ÷ (6 − h). Its visible walls (the
 * sides that face the centre, so the camera sees them) run from the footprint's edges up to the
 * roof's, textured with the wall texture (its columns along the wall, repeating, its rows from the
 * foot up) and shaded per side (those facing right and down at the tower's {@code shade}); then the
 * roof, pre-rendered at its drawn size, at its projected place on whole pixels. Towers are drawn
 * lowest first. Rows up a wall follow the true perspective (a band's edge at height z is drawn at
 * 6 ÷ (6 − z)); a wall is cut into cells of at most {@value #CELL} px each way and bands that widen by
 * at most a tenth, so the affine texturing of a cell's two triangles stays within about a third of
 * a pixel of the exact mapping.
 *
 * <p>M5 part C (Level 09's collapse, round 31's look c): one tower can be {@link #bend bent}: it leans
 * sideways (each band's edge at height z moved by the lean's offset, H sin(lean) (z ÷ H)², and the
 * shudder), sinks (its height a share of the full one: the roof drawn smaller at its lower k, the
 * walls up to it with the texture's rows kept from the foot), its top storeys crushed dark and its
 * roof greyed with dust; its walls are still the sides the standing tower shows the camera.
 *
 * <p>Cost: per tower on screen one roof sprite and its one or two visible walls, each a quad per
 * cell (a 100 px wall leaning 60 px: 7 cells along it and 4 bands up it, 28 quads); nothing is
 * allocated per frame.
 */
final class TowerProjection {
    /** A wall cell's longest side, px: along the wall and up it. */
    static final float CELL = 16;
    /** At most this many bands up a wall, however far it leans. */
    static final int MAX_BANDS = 8;
    /** How much longer a band's top edge may be than its foot (see {@link #bands}). */
    private static final double MAX_WIDENING = 0.1;

    private static final float CX = (float) BackdropData.Tower.CENTRE_X;
    private static final float CY = (float) BackdropData.Tower.CENTRE_Y;
    private static final float X0 = PixelScreen.PLAY_FIELD_X;
    private static final int WIDTH = PlayField.WIDTH;
    private static final int HEIGHT = PlayField.HEIGHT;

    /** The sides of a tower (bits of {@link #visibleSides}): its walls facing down, up, left and right. */
    static final int SOUTH = 1;

    static final int NORTH = 2;
    static final int WEST = 4;
    static final int EAST = 8;
    /**
     * Pixels per camera unit of height for a bent tower's lean (the scale of round 31's look c, where
     * the arcology's 1.5 units are 400 px): the lean's sideways offset is in ground pixels.
     */
    static final double UNIT = 267;
    /** A bent tower's walls: this many bands up to its crushed top, however short it is. */
    private static final int BENT_BANDS = MAX_BANDS;
    /** The crushed storeys' darkest shade (the dust-grey look of the concept's broken floors). */
    private static final float CRUSHED = 0.3f;

    /**
     * A placed tower.
     *
     * @param x the footprint's centre, px from the play field's left edge
     * @param centre where the footprint's centre lies on the ground layer (see {@link
     *     vanguard.content.LevelData#pieceCentre})
     * @param bottom the footprint's bottom edge on the ground layer, whole px
     * @param scale the roof's scale k
     * @param height the roof's height in camera units
     * @param shade the brightness of the walls facing right and down
     * @param roof the roof, at the footprint's size times k
     */
    record Tower(
            double x,
            double centre,
            long bottom,
            int footWidth,
            int footHeight,
            double scale,
            double height,
            float shade,
            TextureRegion roof,
            TextureRegion wall) {}

    private final Tower[] towers;
    private final TextureRegion pixel;
    private final float[] quad = new float[20];
    /** M5 part C: the tower not drawn (Level 09's arcology once it falls), its index in {@link #towers}; -1 for none. */
    private int hidden = -1;
    /** M5 part C: the bent tower ({@link #bend}), its index in {@link #towers}; -1 for none. */
    private int bent = -1;

    private double bendLean;
    private double bendShakeX;
    private double bendShakeY;
    private double bendShare = 1;
    private double bendCrush;
    private float bendDust;
    /** A bent tower's band edges: their heights (camera units), scales and sideways offsets (px). */
    private final double[] edgeZ = new double[BENT_BANDS + 2];

    private final double[] edgeK = new double[BENT_BANDS + 2];
    private final double[] edgeX = new double[BENT_BANDS + 2];
    private final double[] edgeY = new double[BENT_BANDS + 2];

    /**
     * @param towers the level's placed towers, in any order: drawn lowest first, then in this order
     * @param pixel a white pixel (a bent tower's roof dust)
     */
    TowerProjection(List<Tower> towers, TextureRegion pixel) {
        this.towers = towers.toArray(Tower[]::new);
        this.pixel = pixel;
        Arrays.sort(this.towers, Comparator.comparingDouble(Tower::height));
    }

    /**
     * The walls of a tower the camera sees, as bits: a wall faces the projection centre where the
     * roof is drawn past its foot's edge on the other side, away from the centre. A tower above the
     * centre shows its south wall, below it its north wall, left of it its east wall and right of it
     * its west wall (one straddling the centre's line shows neither of that pair).
     */
    static int visibleSides(float fx0, float fy0, float fx1, float fy1, float rx0, float ry0, float rx1, float ry1) {
        int sides = 0;
        if (ry0 > fy0) {
            sides |= SOUTH;
        }
        if (ry1 < fy1) {
            sides |= NORTH;
        }
        if (rx0 > fx0) {
            sides |= WEST;
        }
        if (rx1 < fx1) {
            sides |= EAST;
        }
        return sides;
    }

    /**
     * The lean's sideways offset of a point {@code fraction} of a tower's full height up, px: the
     * tower bends from its foot, {@code height} px tall, leaning {@code lean} radians (+ to the right).
     */
    static double leanOffset(double fraction, double height, double lean) {
        return height * Math.sin(lean) * fraction * fraction;
    }

    /**
     * Where a roof's left edge is drawn: its footprint's centre {@code footX} px from the play
     * field's left edge projected at scale {@code k}, less half the roof's width, on a whole pixel.
     */
    static int roofLeft(double footX, int roofWidth, double k) {
        return (int) Math.round(BackdropData.Tower.project(footX, CX, k) - roofWidth / 2.0);
    }

    /** Where a roof's bottom edge is drawn: as {@link #roofLeft}, up from the play field's bottom edge. */
    static int roofBottom(double footY, int roofHeight, double k) {
        return (int) Math.round(BackdropData.Tower.project(footY, CY, k) - roofHeight / 2.0);
    }

    /**
     * How far up a wall a point {@code fraction} of the tower's {@code height} above the ground is
     * drawn: the share of the way from the foot to the roof's edge, (s(z) − 1) ÷ (k − 1) with s(z) =
     * 6 ÷ (6 − z), so window rows grow a little towards the roof as in true perspective.
     */
    static double rise(double fraction, double height) {
        double k = BackdropData.Tower.scaleAt(height);
        return (BackdropData.Tower.scaleAt(fraction * height) - 1) / (k - 1);
    }

    /**
     * The bands up a wall that leans {@code depth} px from its foot to the roof's edge, of a tower
     * whose roof is drawn at scale {@code k}: at most {@value #CELL} px each, and enough that a band's
     * top edge is at most a tenth longer than its foot (a cell's two triangles then place its texture
     * columns within a third of a pixel of the exact mapping; at k = 1.33 that is four bands).
     */
    static int bands(float depth, double k) {
        int byDepth = (int) Math.ceil(depth / CELL);
        int byWidening = (int) Math.ceil((k - 1) / MAX_WIDENING - 1e-9);
        return Math.max(1, Math.min(MAX_BANDS, Math.max(byDepth, byWidening)));
    }

    /**
     * M5 part C: the tower whose footprint's centre lies nearest the ground position {@code
     * groundCentre} (the ground layer's distance, as {@link Tower#centre}), within {@code reach} px of
     * it; -1 for none. The collapse finds the tower that falls by its band.
     */
    int nearest(double groundCentre, double reach) {
        int best = -1;
        double distance = reach;
        for (int i = 0; i < towers.length; i++) {
            double d = Math.abs(towers[i].centre() - groundCentre);
            if (d <= distance) {
                distance = d;
                best = i;
            }
        }
        return best;
    }

    /** M5 part C: tower {@code index} ({@link #nearest}) is not drawn; -1 draws them all. */
    void hide(int index) {
        hidden = index;
    }

    /**
     * M5 part C: tower {@code index} ({@link #nearest}) is drawn bent; -1 draws them all standing.
     *
     * @param lean radians to the right, the roof's offset H sin(lean) before the projection
     * @param shakeX the shudder at the roof, px (scaled by the height up the tower)
     * @param shakeY the shudder at the roof, px, up
     * @param share the height left, a share of the full height (the drop)
     * @param crush the crushed storeys at the top, a share of the full height
     * @param dust how grey the roof is with dust, 0 to 1
     */
    void bend(int index, double lean, double shakeX, double shakeY, double share, double crush, float dust) {
        bent = index;
        bendLean = lean;
        bendShakeX = shakeX;
        bendShakeY = shakeY;
        bendShare = Math.clamp(share, 0, 1);
        bendCrush = Math.max(0, crush);
        bendDust = dust;
    }

    /** M5 part C: tower {@code index} ({@link #nearest}). */
    Tower tower(int index) {
        return towers[index];
    }

    /**
     * Draws the towers on screen.
     *
     * @param groundScroll the ground layer's distance in px
     */
    void draw(SpriteBatch batch, double groundScroll) {
        long scroll = Math.round(groundScroll);
        Color tint = batch.getColor();
        float lit = batch.getPackedColor();
        for (int t = 0; t < towers.length; t++) {
            Tower tower = towers[t];
            if (t == hidden) {
                continue;
            }
            int footX = (int) Math.round(tower.x() - tower.footWidth() / 2.0);
            int footY = (int) (tower.bottom() - scroll);
            int roofWidth = tower.roof().getRegionWidth();
            int roofHeight = tower.roof().getRegionHeight();
            int roofX = roofLeft(tower.x(), roofWidth, tower.scale());
            int roofY = roofBottom(tower.centre() - groundScroll, roofHeight, tower.scale());
            int left = Math.min(footX, roofX);
            int right = Math.max(footX + tower.footWidth(), roofX + roofWidth);
            int low = Math.min(footY, roofY);
            int high = Math.max(footY + tower.footHeight(), roofY + roofHeight);
            if (right <= 0 || left >= WIDTH || high <= 0 || low >= HEIGHT) {
                continue;
            }
            float shaded =
                    Color.toFloatBits(tint.r * tower.shade(), tint.g * tower.shade(), tint.b * tower.shade(), tint.a);
            float fx0 = X0 + footX;
            float fx1 = fx0 + tower.footWidth();
            float fy0 = footY;
            float fy1 = fy0 + tower.footHeight();
            float rx0 = X0 + roofX;
            float rx1 = rx0 + roofWidth;
            float ry0 = roofY;
            float ry1 = ry0 + roofHeight;
            int sides = visibleSides(fx0, fy0, fx1, fy1, rx0, ry0, rx1, ry1);
            if (t == bent) {
                drawBent(batch, tower, sides, groundScroll, lit, shaded, tint);
                continue;
            }
            // The sides facing the centre, seen from outside, left to right.
            if ((sides & SOUTH) != 0) {
                wall(batch, tower, shaded, fx0, fy0, fx1, fy0, rx0, ry0, rx1, ry0);
            }
            if ((sides & NORTH) != 0) {
                wall(batch, tower, lit, fx1, fy1, fx0, fy1, rx1, ry1, rx0, ry1);
            }
            if ((sides & WEST) != 0) {
                wall(batch, tower, lit, fx0, fy1, fx0, fy0, rx0, ry1, rx0, ry0);
            }
            if ((sides & EAST) != 0) {
                wall(batch, tower, shaded, fx1, fy0, fx1, fy1, rx1, ry0, rx1, ry1);
            }
            batch.draw(tower.roof(), rx0, ry0);
        }
    }

    /**
     * The bent tower ({@link #bend}): its walls on {@code sides} (the standing tower's) from the foot up
     * to its sunken top, each band's edge at the projection of its height moved by the lean and the
     * shudder, the crushed storeys darkening toward the top; then its roof at the top's scale, greyed
     * with dust.
     */
    private void drawBent(
            SpriteBatch batch, Tower tower, int sides, double groundScroll, float lit, float shaded, Color tint) {
        double full = tower.height();
        double top = full * bendShare;
        if (top <= 1e-6) {
            return;
        }
        double heightPx = full * UNIT;
        double crush = Math.min(top, bendCrush * full);
        int bands = BENT_BANDS;
        int edges = bands + 1 + (crush > 0 ? 1 : 0);
        for (int e = 0; e < edges; e++) {
            double z = e <= bands ? (top - crush) * e / bands : top;
            double fraction = z / full;
            edgeZ[e] = z;
            edgeK[e] = BackdropData.Tower.scaleAt(z);
            edgeX[e] = leanOffset(fraction, heightPx, bendLean) + bendShakeX * fraction;
            edgeY[e] = bendShakeY * fraction;
        }
        float x0 = (float) (tower.x() - tower.footWidth() / 2.0);
        float x1 = x0 + tower.footWidth();
        float y0 = (float) (tower.centre() - groundScroll - tower.footHeight() / 2.0);
        float y1 = y0 + tower.footHeight();
        float dark = Color.toFloatBits(
                tint.r * tower.shade() * CRUSHED,
                tint.g * tower.shade() * CRUSHED,
                tint.b * tower.shade() * CRUSHED,
                tint.a);
        float darkLit = Color.toFloatBits(tint.r * CRUSHED, tint.g * CRUSHED, tint.b * CRUSHED, tint.a);
        boolean crushed = crush > 0;
        if ((sides & SOUTH) != 0) {
            bentWall(batch, tower, edges, crushed, shaded, dark, x0, y0, x1, y0);
        }
        if ((sides & NORTH) != 0) {
            bentWall(batch, tower, edges, crushed, lit, darkLit, x1, y1, x0, y1);
        }
        if ((sides & WEST) != 0) {
            bentWall(batch, tower, edges, crushed, lit, darkLit, x0, y1, x0, y0);
        }
        if ((sides & EAST) != 0) {
            bentWall(batch, tower, edges, crushed, shaded, dark, x1, y0, x1, y1);
        }
        int last = edges - 1;
        double k = edgeK[last];
        double scale = k / tower.scale();
        float width = (float) (tower.roof().getRegionWidth() * scale);
        float height = (float) (tower.roof().getRegionHeight() * scale);
        float rx = X0 + (float) BackdropData.Tower.project(tower.x() + edgeX[last], CX, k) - width / 2;
        float ry = (float) BackdropData.Tower.project(tower.centre() - groundScroll + edgeY[last], CY, k) - height / 2;
        batch.draw(tower.roof(), rx, ry, width, height);
        if (bendDust > 0) {
            // The batch's colour is the tint itself: keep its packed value to restore.
            batch.setColor(118 / 255f, 112 / 255f, 110 / 255f, bendDust * tint.a);
            batch.draw(pixel, rx, ry, width, height);
            batch.setPackedColor(lit);
        }
    }

    /**
     * One wall of the bent tower: the foot from (ax, ay) to (bx, by), play-field px (y up), left to
     * right as seen from outside; up through the band edges ({@link #edgeZ}), each the foot's edge moved
     * by its offset and projected at its scale; the texture's rows by height from the foot; a last,
     * crushed band darkening to {@code dark} at the top, raggedly (per cell column).
     */
    private void bentWall(
            SpriteBatch batch,
            Tower tower,
            int edges,
            boolean crushed,
            float colour,
            float dark,
            float ax,
            float ay,
            float bx,
            float by) {
        float length = Math.abs(bx - ax) + Math.abs(by - ay);
        TextureRegion wall = tower.wall();
        float texture = wall.getRegionWidth();
        float u = wall.getU();
        float du = wall.getU2() - u;
        float vFoot = wall.getV2();
        float dv = wall.getV() - vFoot;
        double full = tower.height();
        int column = 0;
        for (float start = 0; start < length; start += texture) {
            float end = Math.min(length, start + texture);
            int cells = (int) Math.ceil((end - start) / CELL);
            float cell = (end - start) / cells;
            for (int c = 0; c < cells; c++, column++) {
                float a0 = start + c * cell;
                float a1 = c == cells - 1 ? end : a0 + cell;
                float u0 = u + du * (a0 - start) / texture;
                float u1 = u + du * (a1 - start) / texture;
                float t0 = a0 / length;
                float t1 = a1 / length;
                // The foot's points at t0 and t1 (play-field px, before the projection).
                double p0x = ax + (bx - ax) * t0;
                double p0y = ay + (by - ay) * t0;
                double p1x = ax + (bx - ax) * t1;
                double p1y = ay + (by - ay) * t1;
                // The crushed top's shade at this column: ragged, 55 % to 100 % of the darkening.
                float ragged = 0.55f + 0.45f * (float) hash(column);
                float crushedTop = lerpColour(colour, dark, ragged);
                for (int e = 1; e < edges; e++) {
                    boolean crushBand = crushed && e == edges - 1;
                    float c0 = colour;
                    float c1 = crushBand ? crushedTop : colour;
                    float v0 = vFoot + dv * (float) (edgeZ[e - 1] / full);
                    float v1 = vFoot + dv * (float) (edgeZ[e] / full);
                    vertex(0, edgeXAt(p0x, e - 1), edgeYAt(p0y, e - 1), c0, u0, v0);
                    vertex(1, edgeXAt(p0x, e), edgeYAt(p0y, e), c1, u0, v1);
                    vertex(2, edgeXAt(p1x, e), edgeYAt(p1y, e), c1, u1, v1);
                    vertex(3, edgeXAt(p1x, e - 1), edgeYAt(p1y, e - 1), c0, u1, v0);
                    batch.draw(wall.getTexture(), quad, 0, quad.length);
                }
            }
        }
    }

    /** A foot point's x at band edge {@code e} of the bent tower, drawn on the screen. */
    private float edgeXAt(double x, int e) {
        return X0 + (float) BackdropData.Tower.project(x + edgeX[e], CX, edgeK[e]);
    }

    /** A foot point's y at band edge {@code e} of the bent tower, drawn on the screen. */
    private float edgeYAt(double y, int e) {
        return (float) BackdropData.Tower.project(y + edgeY[e], CY, edgeK[e]);
    }

    /** A value in [0, 1) from {@code i}, the same every frame. */
    private static double hash(int i) {
        long h = (i + 1) * 0x9E3779B97F4A7C15L;
        h ^= h >>> 31;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 29;
        return (h >>> 11) * 0x1.0p-53;
    }

    /** The packed colour {@code share} of the way from {@code from} to {@code to}. */
    private static float lerpColour(float from, float to, float share) {
        int a = NumberUtils.floatToIntColor(from);
        int b = NumberUtils.floatToIntColor(to);
        int r = 0;
        for (int shift = 0; shift < 32; shift += 8) {
            int ca = (a >>> shift) & 0xFF;
            int cb = (b >>> shift) & 0xFF;
            r |= (Math.round(ca + (cb - ca) * share) & 0xFF) << shift;
        }
        return NumberUtils.intToFloatColor(r);
    }

    /**
     * One wall: the foot from (ax, ay) to (bx, by) and the roof's edge from (cx, cy) to (dx, dy),
     * left to right as seen from outside; cut into cells along the texture's repeats and up the
     * bands.
     */
    private void wall(
            SpriteBatch batch,
            Tower tower,
            float colour,
            float ax,
            float ay,
            float bx,
            float by,
            float cx,
            float cy,
            float dx,
            float dy) {
        float length = Math.abs(bx - ax) + Math.abs(by - ay);
        float depth = Math.max(Math.abs(cx - ax) + Math.abs(cy - ay), Math.abs(dx - bx) + Math.abs(dy - by));
        TextureRegion wall = tower.wall();
        int bands = bands(depth, tower.scale());
        float texture = wall.getRegionWidth();
        float u = wall.getU();
        float du = wall.getU2() - u;
        float vFoot = wall.getV2();
        float dv = wall.getV() - vFoot;
        // The texture repeats every `texture` px along the foot; each repeat is cut into cells.
        for (float start = 0; start < length; start += texture) {
            float end = Math.min(length, start + texture);
            int cells = (int) Math.ceil((end - start) / CELL);
            float cell = (end - start) / cells;
            for (int c = 0; c < cells; c++) {
                float a0 = start + c * cell;
                float a1 = c == cells - 1 ? end : a0 + cell;
                float u0 = u + du * (a0 - start) / texture;
                float u1 = u + du * (a1 - start) / texture;
                float t0 = a0 / length;
                float t1 = a1 / length;
                // The foot's points at t0 and t1, and the way from each up to the roof's edge.
                float f0x = ax + (bx - ax) * t0;
                float f0y = ay + (by - ay) * t0;
                float f1x = ax + (bx - ax) * t1;
                float f1y = ay + (by - ay) * t1;
                float up0x = cx + (dx - cx) * t0 - f0x;
                float up0y = cy + (dy - cy) * t0 - f0y;
                float up1x = cx + (dx - cx) * t1 - f1x;
                float up1y = cy + (dy - cy) * t1 - f1y;
                float g0 = 0;
                for (int b = 1; b <= bands; b++) {
                    float g1 = b == bands ? 1 : (float) rise((double) b / bands, tower.height());
                    float v0 = vFoot + dv * (b - 1) / bands;
                    float v1 = vFoot + dv * b / bands;
                    vertex(0, f0x + up0x * g0, f0y + up0y * g0, colour, u0, v0);
                    vertex(1, f0x + up0x * g1, f0y + up0y * g1, colour, u0, v1);
                    vertex(2, f1x + up1x * g1, f1y + up1y * g1, colour, u1, v1);
                    vertex(3, f1x + up1x * g0, f1y + up1y * g0, colour, u1, v0);
                    batch.draw(wall.getTexture(), quad, 0, quad.length);
                    g0 = g1;
                }
            }
        }
    }

    private void vertex(int i, float x, float y, float colour, float u, float v) {
        int o = i * 5;
        quad[o] = x;
        quad[o + 1] = y;
        quad[o + 2] = colour;
        quad[o + 3] = u;
        quad[o + 4] = v;
    }
}
