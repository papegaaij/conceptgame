package vanguard.game.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
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
    private final float[] quad = new float[20];

    /** @param towers the level's placed towers, in any order: drawn lowest first, then in this order */
    TowerProjection(List<Tower> towers) {
        this.towers = towers.toArray(Tower[]::new);
        Arrays.sort(this.towers, Comparator.comparingDouble(Tower::height));
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
     * Draws the towers on screen.
     *
     * @param groundScroll the ground layer's distance in px
     */
    void draw(SpriteBatch batch, double groundScroll) {
        long scroll = Math.round(groundScroll);
        Color tint = batch.getColor();
        float lit = batch.getPackedColor();
        for (Tower tower : towers) {
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
            // A side faces the camera when the roof leans out past its foot: seen from outside, left to right.
            if (ry0 > fy0) { // south, facing down the screen
                wall(batch, tower, shaded, fx0, fy0, fx1, fy0, rx0, ry0, rx1, ry0);
            }
            if (ry1 < fy1) { // north, facing up
                wall(batch, tower, lit, fx1, fy1, fx0, fy1, rx1, ry1, rx0, ry1);
            }
            if (rx0 < fx0) { // west, facing left
                wall(batch, tower, lit, fx0, fy1, fx0, fy0, rx0, ry1, rx0, ry0);
            }
            if (rx1 > fx1) { // east, facing right
                wall(batch, tower, shaded, fx1, fy0, fx1, fy1, rx1, ry0, rx1, ry1);
            }
            batch.draw(tower.roof(), rx0, ry0);
        }
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
