package vanguard.game.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import vanguard.sim.Hitbox;
import vanguard.sim.Sortie;
import vanguard.sim.Tow;

/**
 * Level 07's lifeboat tow (design/campaign/act-1-first-contact/level-07-brood-carrier, Secrets): a
 * friendly CDF lifeboat drifting down the screen on the air layer with a cargo pod on an amber-lit
 * cable. The cable shows the hits it has taken while it holds the pod, then the cut stub hanging from
 * the boat; the loose pod falls away from the boat on its own, tumbling ({@link #TUMBLE}), until it
 * leaves the screen, and the hidden crate (a pickup) falls out of it on the way down ({@link Tow}),
 * after which the pod shows its empty hold. A hit on the cable sparks (the level screen's glance on
 * its hit event). The tow is drawn under the air units and the shots, so the shots pass over the boat.
 *
 * <p>Sprites (concept round 25 variant b, {@code tools/art/lifeboat.py}), each centred on its place
 * (the cable on its hit box): {@code lifeboat} (strobes lit, dark) with the additive
 * {@code lifeboat-glow} over the lit frame; {@code lifeboat-cable} (intact, 1 hit, 2 hits, ..., cut)
 * with the additive {@code lifeboat-cable-glow} (the light strip: intact, hit) pulsing while it holds
 * and the shared {@code glint} on its coupler every {@link #GLINT_PERIOD} s; {@code lifeboat-pod},
 * its headings clockwise from hanging, then the same headings empty (the tumble is pre-rendered, the
 * key light fixed). Without them a placeholder in flat shapes of the tow's boxes: a pale hull with a
 * CDF blue band, an olive pod and the amber cable.
 */
final class TowLooks {
    private static final float X0 = PixelScreen.PLAY_FIELD_X;
    private static final Color HULL = Color.valueOf("B8BEC4");
    private static final Color BAND = Color.valueOf("2A5CB0");
    private static final Color POD = Color.valueOf("6E6A3A");
    private static final Color CABLE = Color.valueOf("FFB020");
    private static final float CABLE_WIDTH = 3;
    /** The cut cable's stub: this share of the cable, hanging from the boat. */
    private static final float STUB = 0.3f;
    /** How fast a loose pod tumbles, degrees per second (clockwise on the screen). */
    static final float TUMBLE = -150;
    /** The strobes flash once per this many seconds from the tow's entry, lit for {@link #STROBE_ON} s. */
    private static final double STROBE_PERIOD = 1;

    private static final double STROBE_ON = 0.16;
    /** The light strip's pulse, Hz, between {@link #STRIP_LOW} and full. */
    private static final double STRIP_HZ = 1.2;

    private static final float STRIP_LOW = 0.1f;
    /** The coupler's glint: once per this many seconds from the tow's entry, a frame each {@link #GLINT_STEP} s. */
    private static final double GLINT_PERIOD = 2;

    private static final double GLINT_STEP = 0.08;
    /** Where the glint sits from the cable's centre (the coupler's upper corner), px, y up. */
    private static final int GLINT_DX = 3;

    private static final int GLINT_DY = 7;

    private final TextureRegion pixel;
    private final Array<AtlasRegion> boat;
    private final AtlasRegion boatGlow;
    private final Array<AtlasRegion> pod;
    private final Array<AtlasRegion> cable;
    private final Array<AtlasRegion> cableGlow;
    private final Array<AtlasRegion> glint;

    TowLooks(Sprites sprites) {
        pixel = sprites.pixel;
        boat = sprites.has("lifeboat") ? sprites.frames("lifeboat") : null;
        boatGlow = sprites.has("lifeboat-glow") ? sprites.region("lifeboat-glow") : null;
        pod = sprites.has("lifeboat-pod") ? sprites.frames("lifeboat-pod") : null;
        cable = sprites.has("lifeboat-cable") ? sprites.frames("lifeboat-cable") : null;
        cableGlow = sprites.has("lifeboat-cable-glow") ? sprites.frames("lifeboat-cable-glow") : null;
        glint = sprites.glint;
    }

    /** The tows on the screen at the render time {@code seconds}: cable, pod, then the boat over the cable's end. */
    void draw(SpriteBatch batch, Sortie sortie, float alpha, double seconds) {
        for (int i = 0; i < sortie.towCount(); i++) {
            Tow tow = sortie.tow(i);
            if (!tow.present()) {
                continue;
            }
            double x = tow.renderX(alpha);
            double y = tow.renderY(alpha);
            var spec = tow.spec();
            double age = Math.max(0, seconds - spec.t());
            drawCable(batch, tow, x, y, age);
            drawPod(batch, tow, alpha);
            drawBoat(batch, tow, x, y, age);
        }
        batch.setColor(Color.WHITE);
    }

    private void drawCable(SpriteBatch batch, Tow tow, double x, double y, double age) {
        var spec = tow.spec();
        double cx = x + spec.podDx() / 2;
        double cy = y + spec.podDy() / 2;
        if (cable == null) {
            drawPlaceholderCable(batch, x, y, spec.podDx(), spec.podDy(), tow.holding());
            return;
        }
        int taken = spec.hits() - tow.hitsLeft();
        int frame = tow.holding() ? Math.min(taken, cable.size - 2) : cable.size - 1;
        batch.setColor(Color.WHITE);
        drawCentred(batch, cable.get(Math.max(0, frame)), cx, cy);
        if (!tow.holding()) {
            return;
        }
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
        if (cableGlow != null) {
            float pulse = (float) (0.5 + 0.5 * Math.sin(2 * Math.PI * STRIP_HZ * age));
            float level = STRIP_LOW + (1 - STRIP_LOW) * pulse;
            batch.setColor(level, level, level, 1);
            drawCentred(batch, cableGlow.get(Math.min(taken > 0 ? 1 : 0, cableGlow.size - 1)), cx, cy);
            batch.setColor(Color.WHITE);
        }
        int glintFrame = (int) (age % GLINT_PERIOD / GLINT_STEP);
        if (glint.size > 0 && glintFrame < glint.size) {
            drawCentred(batch, glint.get(glintFrame), cx + GLINT_DX, cy + GLINT_DY);
        }
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
    }

    /**
     * The pod: hanging (frame 0), or loose, the heading its tumble has reached; the second half of the
     * frames (the same headings, the hold empty) once the crate has fallen out.
     */
    private void drawPod(SpriteBatch batch, Tow tow, float alpha) {
        double px = tow.podRenderX(alpha);
        double py = tow.podRenderY(alpha);
        double tumble = -TUMBLE * tow.looseSeconds(alpha);
        if (pod == null) {
            drawBox(batch, tow.spec().pod(), px, py, (float) -tumble);
            return;
        }
        int headings = Math.max(1, pod.size / 2);
        int frame = tow.holding() ? 0 : (int) Math.floor(tumble / (360.0 / headings)) % headings;
        if (!tow.holding() && !tow.crateDue() && pod.size >= 2 * headings) {
            frame += headings;
        }
        batch.setColor(Color.WHITE);
        drawCentred(batch, pod.get(frame), px, py);
    }

    private void drawBoat(SpriteBatch batch, Tow tow, double x, double y, double age) {
        Hitbox box = tow.spec().boat();
        if (boat == null) {
            batch.setColor(HULL);
            batch.draw(
                    pixel,
                    Math.round(X0 + x - box.width() / 2),
                    Math.round(y - box.height() / 2),
                    (float) box.width(),
                    (float) box.height());
            batch.setColor(BAND);
            batch.draw(pixel, Math.round(X0 + x - box.width() / 2), Math.round(y - 2), (float) box.width(), 4);
            return;
        }
        boolean lit = age % STROBE_PERIOD < STROBE_ON;
        batch.setColor(Color.WHITE);
        drawCentred(batch, boat.get(lit || boat.size < 2 ? 0 : 1), x, y);
        if (lit && boatGlow != null) {
            batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
            drawCentred(batch, boatGlow, x, y);
            batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        }
    }

    private static void drawCentred(SpriteBatch batch, AtlasRegion region, double x, double y) {
        batch.draw(
                region,
                Math.round(X0 + x - region.getRegionWidth() / 2.0),
                Math.round(y - region.getRegionHeight() / 2.0));
    }

    private void drawPlaceholderCable(SpriteBatch batch, double x, double y, double dx, double dy, boolean holding) {
        float length = (float) Math.hypot(dx, dy) * (holding ? 1 : STUB);
        float degrees = (float) Math.toDegrees(Math.atan2(dy, dx));
        batch.setColor(CABLE);
        batch.draw(
                pixel,
                (float) (X0 + x),
                (float) (y - CABLE_WIDTH / 2),
                0,
                CABLE_WIDTH / 2,
                length,
                CABLE_WIDTH,
                1,
                1,
                degrees);
    }

    /** The placeholder pod: a flat olive box centred on (x, y), turned by {@code degrees} (counter-clockwise). */
    private void drawBox(SpriteBatch batch, Hitbox box, double x, double y, float degrees) {
        float width = (float) box.width();
        float height = (float) box.height();
        batch.setColor(POD);
        batch.draw(
                pixel,
                (float) (X0 + x - width / 2.0),
                (float) (y - height / 2.0),
                width / 2,
                height / 2,
                width,
                height,
                1,
                1,
                degrees);
    }
}
