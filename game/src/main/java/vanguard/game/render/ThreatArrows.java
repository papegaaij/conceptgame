package vanguard.game.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;
import vanguard.sim.Enemy;
import vanguard.sim.Layer;
import vanguard.sim.PlayField;
import vanguard.sim.Sortie;

/**
 * The sensor suite's threat arrows (design/player/systems, sensor suite L2+; design/ui/hud, in the
 * play field; the chosen round-09 sheet edge-warnings-r09-a, panel D): with a sensor suite at L2 or
 * better, a small amber arrowhead at the play field's edge points at each enemy that is off the
 * screen and coming in (a unit about to enter, the body of a Coilwyrm still below the screen as it
 * comes up from the rear), at the enemy's place along that edge. An
 * arrow grows and brightens as its enemy closes in, from 240 px out. Enemies moving away (leaving
 * the screen, a Coilwyrm diving out on its loop) and ground units (they scroll in, in sight) get
 * none; enemies close together along an edge share one arrow, and at most eight show. An arrow also
 * shows at an edge whose edge warning is up: the warning says which edge, the arrow where along it.
 * The simulation only knows a unit once it is spawned, 40 px or more outside its edge, so a wave's
 * units show their arrows for about 0.3 s before they enter. Planned anew every frame from the
 * simulation's positions, so it has no state and allocates nothing.
 * Geometry in play-field pixels, y up.
 */
public final class ThreatArrows implements Disposable {
    static final int LEFT = 0;
    static final int RIGHT = 1;
    static final int TOP = 2;
    static final int BOTTOM = 3;

    /** How far beyond the edge the sensors see a threat, px. */
    static final double RANGE = 240;
    /** The most arrows at once. */
    static final int MAX = 8;
    /** Threats closer than this along the same edge share an arrow, px. */
    static final float MERGE = 28;
    /** An arrow's middle sits this far inside its edge, and at least twice this far from a corner. */
    static final float INSET = 14;
    /** An arrowhead's length, far and near, px (the concept's 7–12 px at 1.5×). */
    static final float SMALL = 10;

    static final float LARGE = 18;
    /** Its opacity far and near (the concept's 150..255). */
    static final float FAINT = 150 / 255f;

    private static final Color AMBER = EdgeWarnings.WARN;
    private static final Color OUTLINE = new Color(20 / 255f, 12 / 255f, 0, 200 / 255f);
    private static final int W = PlayField.WIDTH;
    private static final int H = PlayField.HEIGHT;
    /** The arrowhead texture's size; drawn scaled. */
    private static final int TEXTURE = 32;

    /** The arrowhead, made at the first draw (the planning needs no graphics). */
    private Texture texture;

    private TextureRegion head;
    private final int[] edges = new int[MAX];
    private final float[] alongs = new float[MAX];
    private final float[] nears = new float[MAX];
    private int count;

    /** Plans the arrows for the enemies at the tick being shown ({@code alpha} of a step after the last). */
    void plan(Sortie sortie, float alpha) {
        count = 0;
        for (int i = 0; i < sortie.enemyCount(); i++) {
            Enemy enemy = sortie.enemy(i);
            if (enemy.layer() == Layer.GROUND) {
                continue;
            }
            track(
                    enemy.renderX(0),
                    enemy.renderY(0),
                    enemy.renderX(1),
                    enemy.renderY(1),
                    enemy.renderX(alpha),
                    enemy.renderY(alpha));
        }
    }

    /**
     * Adds an arrow for a threat at ({@code x}, {@code y}) as shown, which was at ({@code x0},
     * {@code y0}) a step ago and is at ({@code x1}, {@code y1}) now, unless it is on the screen, out
     * of range or moving away.
     */
    void track(double x0, double y0, double x1, double y1, double x, double y) {
        double now = outside(x1, y1);
        if (now <= 0 || now > RANGE || now > outside(x0, y0)) {
            return;
        }
        int edge = edge(x, y);
        float along = along(edge, x, y);
        float near = (float) Math.clamp(1 - outside(x, y) / RANGE, 0, 1);
        for (int k = 0; k < count; k++) {
            if (edges[k] == edge && Math.abs(alongs[k] - along) < MERGE) {
                if (near > nears[k]) {
                    nears[k] = near;
                    alongs[k] = along;
                }
                return;
            }
        }
        if (count < MAX) {
            edges[count] = edge;
            alongs[count] = along;
            nears[count] = near;
            count++;
        }
    }

    /** How far a point is outside the play field, px; 0 on it. */
    static double outside(double x, double y) {
        return Math.max(0, Math.max(Math.max(-x, x - W), Math.max(-y, y - H)));
    }

    /** The edge a point off the field is farthest beyond. */
    static int edge(double x, double y) {
        double dx = Math.max(-x, x - W);
        double dy = Math.max(-y, y - H);
        if (dx >= dy) {
            return x < 0 ? LEFT : RIGHT;
        }
        return y < 0 ? BOTTOM : TOP;
    }

    /** The arrow's place along its edge: the threat's, kept off the corners. */
    private static float along(int edge, double x, double y) {
        boolean side = edge == LEFT || edge == RIGHT;
        double length = side ? H : W;
        return (float) Math.clamp(side ? y : x, 2 * INSET, length - 2 * INSET);
    }

    int count() {
        return count;
    }

    int edgeOf(int arrow) {
        return edges[arrow];
    }

    /** An arrow's middle, x in play-field pixels. */
    float x(int arrow) {
        return switch (edges[arrow]) {
            case LEFT -> INSET;
            case RIGHT -> W - INSET;
            default -> alongs[arrow];
        };
    }

    /** An arrow's middle, y in play-field pixels (up). */
    float y(int arrow) {
        return switch (edges[arrow]) {
            case BOTTOM -> INSET;
            case TOP -> H - INSET;
            default -> alongs[arrow];
        };
    }

    /** Which way an arrow points, degrees counter-clockwise from the right: out of its edge, at its threat. */
    float rotation(int arrow) {
        return switch (edges[arrow]) {
            case LEFT -> 180;
            case TOP -> 90;
            case BOTTOM -> 270;
            default -> 0;
        };
    }

    /** An arrowhead's length, growing as its threat closes in. */
    float size(int arrow) {
        return SMALL + (LARGE - SMALL) * nears[arrow];
    }

    /** An arrow's opacity, brightening as its threat closes in. */
    float opacity(int arrow) {
        return FAINT + (1 - FAINT) * nears[arrow];
    }

    /** Plans and draws the arrows for the enemies at the tick being shown. */
    public void draw(SpriteBatch batch, Sortie sortie, float alpha) {
        plan(sortie, alpha);
        if (count > 0 && texture == null) {
            texture = arrowhead();
            head = new TextureRegion(texture);
        }
        for (int i = 0; i < count; i++) {
            float size = size(i);
            float cx = PixelScreen.PLAY_FIELD_X + x(i);
            float cy = y(i);
            float rotation = rotation(i);
            batch.setColor(OUTLINE);
            head(batch, cx, cy, size + 3, rotation);
            batch.setColor(AMBER.r, AMBER.g, AMBER.b, opacity(i));
            head(batch, cx, cy, size, rotation);
        }
        batch.setColor(Color.WHITE);
    }

    private void head(SpriteBatch batch, float cx, float cy, float size, float rotation) {
        float half = size / 2;
        batch.draw(head, cx - half, cy - half, half, half, size, size, 1, 1, rotation);
    }

    /** A white arrowhead pointing right, filling its square texture's height. */
    private static Texture arrowhead() {
        Pixmap pixmap = new Pixmap(TEXTURE, TEXTURE, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fillTriangle(TEXTURE / 4, 0, TEXTURE - 1, TEXTURE / 2, TEXTURE / 4, TEXTURE - 1);
        Texture arrow = new Texture(pixmap);
        pixmap.dispose();
        return arrow;
    }

    @Override
    public void dispose() {
        if (texture != null) {
            texture.dispose();
        }
    }
}
