package vanguard.game.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import vanguard.sim.Enemy;
import vanguard.sim.Lob;
import vanguard.sim.PlayField;
import vanguard.sim.Sled;
import vanguard.sim.Sortie;

/**
 * Level 05's hazards and objective marks (design/campaign, Level 05; art: tools/art/l05_hazards.py):
 * the mass-driver sleds on Level 04's rail (its {@code sled-run} lamp frames, idle or in their fast
 * blink, with lit lamps added over them while the lights chase up the rail and while a sled runs,
 * and the lit sled racing up the rail on its additive motion streak), the Polyp Mortars' lobs (a
 * lime marker ring as big as the impact circle where they land, pulsing faster as the blob nears,
 * and the acid blob arcing over to it, its shadow on the ground) and the outline of the units of a
 * destroy-targets group in the objective colour.
 */
final class LunaLooks {
    private static final float X0 = PixelScreen.PLAY_FIELD_X;
    /** The objective colour of the battery outline (design/ui/hud). */
    static final Color OBJECTIVE = Color.valueOf("FFE04A");

    private static final Color SHADOW = new Color(0, 0, 0, 0.35f);
    /** The blob's highest point over its straight path, px. */
    private static final double ARC = 70;
    /** The marker sprite's ring radius, px (the data's impact radius, half its 32 px circle). */
    private static final float MARKER_RADIUS = 16;
    /** The sled-run frames: 0–13 idle blinking, 14–21 the fast warning blink (Level 04's art). */
    private static final int IDLE_FRAMES = 14;

    private static final int WARN_FIRST = 14;
    private static final int WARN_FRAMES = 8;
    /** The sled-run piece's height; its lamps line up with the rail's pylons every 60 px. */
    private static final int RUN_HEIGHT = 480;

    private static final int PYLON = 60;
    /** The rail lamps' centres in the sled-run piece: 20 px above each pylon line, in two columns. */
    private static final float LAMP_Y = 20.5f;

    private static final float[] LAMP_X = {7.5f, 41.5f};
    /** The chase sweeps up the rail this many times in its telegraph; its lit band's half width, px. */
    private static final int SWEEPS = 3;

    private static final double BAND = 45;
    /** The streak's rows above the sled's centre (its bloom around the sled). */
    private static final int STREAK_HEAD = 20;

    private final Array<AtlasRegion> sledRun;
    private final Array<AtlasRegion> blob;
    private final AtlasRegion marker;
    private final AtlasRegion sled;
    private final AtlasRegion streak;
    private final AtlasRegion lamp;
    private final TextureRegion pixel;
    private final FlashShader flash;

    /**
     * @param sledRun Level 04's {@code sled-run} frames, or null when the level has no sleds
     */
    LunaLooks(Sprites sprites, Array<AtlasRegion> sledRun, FlashShader flash) {
        this.pixel = sprites.pixel;
        this.flash = flash;
        this.sledRun = sledRun;
        blob = sprites.frames("mortar-blob");
        marker = sprites.region("mortar-marker");
        sled = sprites.region("sled");
        streak = sprites.region("sled-streak");
        lamp = sprites.region("sled-lamp");
    }

    /**
     * The rail's lamps over the ground (aligned with its pylons as the ground scrolls): idle, or the
     * fast blink with lit lamps chasing up the rail before a sled; then every lamp lit and the sled
     * on its streak while it runs.
     */
    void drawSled(SpriteBatch batch, Sortie sortie, double scroll, float alpha) {
        if (sortie.sled().isEmpty() || sledRun == null) {
            return;
        }
        Sled run = sortie.sled().get();
        if (sortie.levelSeconds() > run.spec().untilSeconds() + 2) {
            return;
        }
        long tick = sortie.tick();
        int frame = run.lit() ? WARN_FIRST + (int) (tick / 6 % WARN_FRAMES) : (int) (tick / 6 % IDLE_FRAMES);
        AtlasRegion image = sledRun.get(frame);
        float x = Math.round(X0 + run.spec().x() - image.getRegionWidth() / 2.0);
        long offset = Math.floorMod(Math.round(scroll), PYLON);
        for (long y = -offset; y < PlayField.HEIGHT; y += RUN_HEIGHT) {
            batch.draw(image, x, y);
        }
        if (!run.lit()) {
            return;
        }
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
        double head = (run.lightsProgress(alpha) * SWEEPS % 1) * (PlayField.HEIGHT + 2 * BAND) - BAND;
        float rail = (float) (X0 + run.spec().x());
        for (double y = LAMP_Y - offset; y < PlayField.HEIGHT + PYLON; y += PYLON) {
            float level = run.running() ? 1 : (float) (0.25 + 0.75 * Math.exp(-Math.pow((y - head) / BAND, 2)));
            batch.setColor(level, level, level, 1);
            for (float column : LAMP_X) {
                batch.draw(
                        lamp,
                        Math.round(x + column - lamp.getRegionWidth() / 2f),
                        Math.round(y - lamp.getRegionHeight() / 2.0));
            }
        }
        batch.setColor(Color.WHITE);
        if (run.running()) {
            float y = (float) (run.runProgress(alpha) * (PlayField.HEIGHT + 240));
            batch.draw(
                    streak,
                    Math.round(rail - streak.getRegionWidth() / 2f),
                    Math.round(y + STREAK_HEAD - streak.getRegionHeight()));
            batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
            batch.draw(
                    sled, Math.round(rail - sled.getRegionWidth() / 2f), Math.round(y - sled.getRegionHeight() / 2f));
        }
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
    }

    /** The lobs' markers on the ground: a lime ring as big as the impact circle, pulsing faster as it lands. */
    void drawMarkers(SpriteBatch batch, Sortie sortie, float alpha) {
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
        for (int i = 0; i < sortie.lobCount(); i++) {
            Lob lob = sortie.lob(i);
            double progress = lob.progress(alpha);
            float scale = (float) (lob.impactRadius() / MARKER_RADIUS);
            float pulse = (float) (0.55 + 0.45 * Math.abs(Math.sin(Math.PI * progress * (2 + 4 * progress))));
            batch.setColor(pulse, pulse, pulse, 1);
            float w = marker.getRegionWidth();
            float h = marker.getRegionHeight();
            batch.draw(
                    marker,
                    (float) Math.round(X0 + lob.targetX() - w / 2),
                    (float) Math.round(lob.targetY() - h / 2),
                    w / 2,
                    h / 2,
                    w,
                    h,
                    scale,
                    scale,
                    0);
        }
        batch.setColor(Color.WHITE);
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
    }

    /** The blobs in flight over their markers, arcing up and down, their shadows on the straight path. */
    void drawBlobs(SpriteBatch batch, Sortie sortie, float alpha) {
        for (int i = 0; i < sortie.lobCount(); i++) {
            Lob lob = sortie.lob(i);
            double progress = lob.progress(alpha);
            double x = lob.renderX(alpha);
            double y = lob.renderY(alpha);
            double lift = Math.sin(Math.PI * progress) * ARC;
            batch.setColor(SHADOW);
            batch.draw(pixel, (float) Math.round(X0 + x - 5), (float) Math.round(y - 3), 10, 6);
            batch.setColor(Color.WHITE);
            float scale = (float) (1 + 0.6 * Math.sin(Math.PI * progress));
            AtlasRegion frame = blob.get((int) ((sortie.tick() / 6 + i) % blob.size));
            float w = frame.getRegionWidth();
            float h = frame.getRegionHeight();
            batch.draw(
                    frame,
                    (float) Math.round(X0 + x - w / 2),
                    (float) Math.round(y + lift - h / 2),
                    w / 2,
                    h / 2,
                    w,
                    h,
                    scale,
                    scale,
                    0);
        }
    }

    /** Whether the unit belongs to a group of the level's destroy-targets primary (a battery). */
    static boolean target(Sortie sortie, Enemy enemy) {
        return enemy.groupIndex() >= 0 && !sortie.script().targets().isEmpty();
    }

    /** A one-pixel outline of {@code frame} centred on (x, y) in the objective colour, drawn under it. */
    void drawOutline(SpriteBatch batch, TextureRegion frame, double x, double y) {
        float cx = Math.round(X0 + x);
        float cy = Math.round(y);
        for (int k = 0; k < 4; k++) {
            float dx = k == 0 ? -1 : k == 1 ? 1 : 0;
            float dy = k == 2 ? -1 : k == 3 ? 1 : 0;
            flash.draw(batch, frame, cx + dx, cy + dy, OBJECTIVE, 1);
        }
    }
}
