package vanguard.game.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import vanguard.sim.Enemy;
import vanguard.sim.LevelScript;
import vanguard.sim.PlayField;
import vanguard.sim.SimStep;
import vanguard.sim.SmartBombSpec;
import vanguard.sim.Sortie;
import vanguard.sim.SpecialSlot;

/**
 * Level 06's looks (design/campaign Level 06, M4 part F): the darkness (a light map that multiplies
 * only the ground layer and the ground units: the static dome and rail-lamp pools, the ship's
 * headlight cone, the scripted flares and the player's shots), the turrets' and mortars' glow frames
 * after it, the flare shells, the Mantis's frames by side and state, its arc telegraph and beam, and
 * the Smart Bomb's flash and ring. The headlight cone, the flare's pool and shell and the glows are
 * the production art of tools/art/l06_darkness.py where it exists, shapes drawn in code otherwise;
 * the Mantis's telegraph wedge, beam, tip spark and eye ring are tools/art/mantis_beam.py's (strokes
 * until they exist), from its eye; the lamp pools and the Smart Bomb are still drawn in code.
 */
public final class FarsideLooks implements Disposable {
    private static final float X0 = PixelScreen.PLAY_FIELD_X;
    private static final int RADIAL_SIZE = 64;
    /** A shot lights the ground in this radius as it passes. */
    private static final float SHOT_LIGHT = 22;
    /** The headlight cone starts this far above the ship's centre (its nose). */
    private static final float NOSE = 18;
    /** A flare's pool brightens over its first and dims over its last second. */
    private static final double FLARE_FADE_SECONDS = 1;

    private static final Color LAMP = new Color(1f, 0.82f, 0.55f, 1);
    private static final Color HEADLIGHT = new Color(0.72f, 0.84f, 1f, 1);
    private static final Color FLARE = new Color(1f, 0.78f, 0.5f, 1);
    private static final Color SHOT = new Color(0.8f, 0.85f, 1f, 0.6f);
    private static final Color CRIMSON = Color.valueOf("D0203A");
    private static final Color TURRET_GLOW = Color.valueOf("9A5CFF");
    private static final Color MORTAR_GLOW = Color.valueOf("A8FF3A");
    private static final Color RING = Color.valueOf("CFE8FF");
    /** The Smart Bomb's ring is drawn as this many short strokes. */
    private static final int RING_STROKES = 72;

    private static final int ARC_DOTS = 18;
    /** The flare shell's flicker: a frame every 4 steps (15 fps). */
    private static final int FLARE_SHELL_TICKS = 4;
    /**
     * The Mantis's production frames (tools/art/mantis.py) per side, side 0 holding the left edge:
     * hover 0-3, telegraph 4-7 (the arms opening over the telegraph), sweep 8-9, exit 10-13.
     */
    static final int MANTIS_PER_SIDE = 14;

    private static final int MANTIS_TELEGRAPH = 4;
    private static final int MANTIS_SWEEP = 8;
    private static final int MANTIS_EXIT = 10;
    /** Hover and exit wing beats and the sweep's flicker: 10 fps, the Vrell's organic rate. */
    private static final int MANTIS_FRAME_TICKS = 6;
    /** The beam's knots run out a frame every 3 steps (20 fps, 4 px a frame); the tip spark too. */
    private static final int BEAM_FRAME_TICKS = 3;

    private final LevelScript.Darkness darkness;
    private final FrameBuffer lightMap;
    private final Texture radialTexture;
    private final TextureRegion radial;
    private final Texture coneTexture;
    private final TextureRegion pixel;
    private final Matrix4 lightProjection = new Matrix4();
    private final Matrix4 saved = new Matrix4();
    /** The frame buffer and viewport the light pass interrupts (the pixel screen's): restored after it. */
    private final java.nio.IntBuffer boundFramebuffer = com.badlogic.gdx.utils.BufferUtils.newIntBuffer(16);

    private final java.nio.IntBuffer boundViewport = com.badlogic.gdx.utils.BufferUtils.newIntBuffer(16);
    /** Per static light, the ground scroll at which it enters at the top edge. */
    private final double[] lightScroll;
    /** The production light shapes and flare (tools/art/l06_darkness.py); null or empty until they exist. */
    private final TextureRegion flarePool;

    private final TextureRegion headlightCone;
    private final Array<AtlasRegion> flareShell;
    /** Per Mantis in its telegraph, the step its telegraph started (for the arms' opening frames). */
    private final Map<Enemy, Long> telegraphStart = new IdentityHashMap<>();
    /**
     * The Mantis's beam (tools/art/mantis_beam.py, round 23 variant B): the strip tiled along the
     * beam from the eye, the tip spark, the eye ring; empty or null until they exist.
     */
    private final Array<AtlasRegion> beamStrip;

    private final Array<AtlasRegion> beamTip;
    private final TextureRegion beamEye;
    /** The telegraph wedges by their arc in whole degrees (the index), for the arcs the level's sweeps use. */
    private final TextureRegion[] telegraphs = new TextureRegion[181];

    public FarsideLooks(Sprites sprites, LevelScript script) {
        pixel = sprites.pixel;
        flarePool = sprites.has("flare-pool") ? sprites.frames("flare-pool").first() : null;
        headlightCone =
                sprites.has("headlight-cone") ? sprites.frames("headlight-cone").first() : null;
        flareShell = sprites.has("flare-shell") ? sprites.frames("flare-shell") : new Array<>();
        beamStrip = sprites.has("mantis-beam") ? sprites.frames("mantis-beam") : new Array<>();
        beamTip = sprites.has("mantis-beam-tip") ? sprites.frames("mantis-beam-tip") : new Array<>();
        beamEye = sprites.has("mantis-beam-eye")
                ? sprites.frames("mantis-beam-eye").first()
                : null;
        for (var wave : script.waves()) {
            wave.enemy().sweep().ifPresent(sweep -> {
                int degrees = arcDegrees(sweep.arcRadians());
                String name = "mantis-telegraph-" + degrees;
                if (telegraphs[degrees] == null && sprites.has(name)) {
                    telegraphs[degrees] = sprites.frames(name).first();
                }
            });
        }
        darkness = script.darkness().orElse(null);
        radialTexture = radialTexture();
        radial = new TextureRegion(radialTexture);
        if (darkness == null) {
            lightMap = null;
            coneTexture = null;
            lightScroll = new double[0];
            return;
        }
        lightMap = new FrameBuffer(Pixmap.Format.RGBA8888, PlayField.WIDTH, PlayField.HEIGHT, false);
        lightProjection.setToOrtho2D(0, 0, PlayField.WIDTH, PlayField.HEIGHT);
        coneTexture = coneTexture(darkness.headlightLength(), darkness.headlightAngle());
        List<LevelScript.Darkness.Light> lights = darkness.lights();
        lightScroll = new double[lights.size()];
        for (int i = 0; i < lights.size(); i++) {
            lightScroll[i] = scrollAt(script.sections(), lights.get(i).t());
        }
    }

    /** The ground scroll at {@code t} seconds: the sections' speeds over time. */
    private static double scrollAt(List<LevelScript.Section> sections, double t) {
        double scroll = 0;
        double start = 0;
        for (LevelScript.Section section : sections) {
            double end = Math.min(t, section.end());
            if (end > start) {
                scroll += (end - start) * section.speed();
            }
            start = section.end();
        }
        return scroll;
    }

    /** A white disc fading from its centre to its rim, for every light pool and glow. */
    private static Texture radialTexture() {
        Pixmap pixmap = new Pixmap(RADIAL_SIZE, RADIAL_SIZE, Pixmap.Format.RGBA8888);
        double half = RADIAL_SIZE / 2.0;
        for (int y = 0; y < RADIAL_SIZE; y++) {
            for (int x = 0; x < RADIAL_SIZE; x++) {
                double d = Math.hypot(x + 0.5 - half, y + 0.5 - half) / half;
                float a = (float) Math.max(0, 1 - d);
                pixmap.drawPixel(x, y, Color.rgba8888(1, 1, 1, a * a * (3 - 2 * a)));
            }
        }
        Texture texture = new Texture(pixmap);
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        pixmap.dispose();
        return texture;
    }

    /** The headlight: a cone {@code length} px long and {@code angle} wide, pointing up, fading to its far end and its sides. */
    private static Texture coneTexture(double length, double angle) {
        int height = (int) Math.ceil(length);
        int width = (int) Math.ceil(2 * length * Math.tan(angle / 2)) + 2;
        Pixmap pixmap = new Pixmap(width, height, Pixmap.Format.RGBA8888);
        double half = width / 2.0;
        for (int y = 0; y < height; y++) {
            // Pixmap rows run down: row 0 is the far end.
            double along = (height - y - 0.5) / length;
            for (int x = 0; x < width; x++) {
                double off = Math.abs(Math.atan2(x + 0.5 - half, (height - y - 0.5)));
                double side = 1 - off / (angle / 2);
                float a = (float) Math.max(0, Math.min(1, side * 3) * (1 - along * along));
                pixmap.drawPixel(x, y, Color.rgba8888(1, 1, 1, a));
            }
        }
        Texture texture = new Texture(pixmap);
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        pixmap.dispose();
        return texture;
    }

    /** Whether the level is dark. */
    public boolean dark() {
        return darkness != null;
    }

    /**
     * Darkens what is drawn so far on the play field (the ground layer and the ground units) by the
     * light map: the ambient light, the static pools, the headlight, the flares and the shots.
     */
    public void darken(SpriteBatch batch, Sortie sortie, float alpha, double scroll, double seconds) {
        if (darkness == null) {
            return;
        }
        batch.end();
        saved.set(batch.getProjectionMatrix());
        // FrameBuffer.end() binds the window, not the frame buffer that was bound before (no nesting).
        Gdx.gl.glGetIntegerv(GL20.GL_FRAMEBUFFER_BINDING, boundFramebuffer);
        Gdx.gl.glGetIntegerv(GL20.GL_VIEWPORT, boundViewport);
        lightMap.begin();
        float ambient = (float) darkness.ambient();
        Gdx.gl.glClearColor(ambient, ambient, ambient * 1.1f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        batch.setProjectionMatrix(lightProjection);
        batch.begin();
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
        List<LevelScript.Darkness.Light> lights = darkness.lights();
        batch.setColor(LAMP);
        for (int i = 0; i < lights.size(); i++) {
            LevelScript.Darkness.Light light = lights.get(i);
            double y = PlayField.HEIGHT + light.radius() - (scroll - lightScroll[i]);
            if (y + light.radius() > 0 && y - light.radius() < PlayField.HEIGHT) {
                pool(batch, light.x(), y, light.radius());
            }
        }
        if (darkness.headlightOn(seconds) && sortie.flying()) {
            batch.setColor(HEADLIGHT);
            float x = (float) sortie.ship().renderX(alpha);
            float y = (float) sortie.ship().renderY(alpha) + NOSE;
            if (headlightCone != null) {
                // The production cone, scaled to the data's length and angle like the drawn one.
                batch.draw(
                        headlightCone,
                        x - coneTexture.getWidth() / 2f,
                        y,
                        coneTexture.getWidth(),
                        coneTexture.getHeight());
            } else {
                batch.draw(coneTexture, x - coneTexture.getWidth() / 2f, y);
            }
        }
        for (int i = 0; i < darkness.flares().size(); i++) {
            if (!darkness.flareBurning(i, seconds)) {
                continue;
            }
            float strength = flareStrength(i, seconds);
            batch.setColor(FLARE.r, FLARE.g, FLARE.b, strength);
            double radius = darkness.flareRadius();
            float size = (float) (2 * radius);
            batch.draw(
                    flarePool != null ? flarePool : radial,
                    (float) (darkness.flares().get(i).x() - radius),
                    (float) (darkness.flareY(i, seconds) - radius),
                    size,
                    size);
        }
        batch.setColor(SHOT);
        for (int i = 0; i < sortie.shotCount(); i++) {
            pool(batch, sortie.shot(i).renderX(alpha), sortie.shot(i).renderY(alpha), SHOT_LIGHT);
        }
        batch.setColor(Color.WHITE);
        batch.end();
        lightMap.end();
        Gdx.gl.glBindFramebuffer(GL20.GL_FRAMEBUFFER, boundFramebuffer.get(0));
        Gdx.gl.glViewport(boundViewport.get(0), boundViewport.get(1), boundViewport.get(2), boundViewport.get(3));
        batch.setProjectionMatrix(saved);
        batch.begin();
        batch.setBlendFunction(GL20.GL_DST_COLOR, GL20.GL_ZERO);
        batch.draw(
                lightMap.getColorBufferTexture(),
                X0,
                0,
                PlayField.WIDTH,
                PlayField.HEIGHT,
                0,
                0,
                PlayField.WIDTH,
                PlayField.HEIGHT,
                false,
                true);
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
    }

    /** A flare's brightness: rising over its first second, dimming over its last. */
    private float flareStrength(int i, double seconds) {
        double into = seconds - darkness.flares().get(i).t();
        double left = darkness.flareSeconds() - into;
        return (float) Math.clamp(Math.min(into, left) / FLARE_FADE_SECONDS, 0, 1);
    }

    private void pool(SpriteBatch batch, double x, double y, double radius) {
        float size = (float) (2 * radius);
        batch.draw(radial, (float) (x - radius), (float) (y - radius), size, size);
    }

    /**
     * After the light pass: the turrets' and mortars' glow at full brightness (their {@code -glow}
     * frames, the heading and animation frame of the unit's own frame; a violet or lime glow for a
     * unit without them) and the flare shells.
     */
    public void drawGlows(SpriteBatch batch, Sortie sortie, EnemyLooks[] looks, float alpha, double seconds) {
        if (darkness == null) {
            return;
        }
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
        for (int i = 0; i < sortie.enemyCount(); i++) {
            Enemy enemy = sortie.enemy(i);
            if (!enemy.grounded()) {
                continue;
            }
            String slug = enemy.spec().slug();
            EnemyLooks look = looks[enemy.kind()];
            if (!look.glow().isEmpty()) {
                TextureRegion glow = look.glowFrame(enemy.facing(), look.step(sortie.tick(), i, enemy.burstSeconds()));
                batch.draw(
                        glow,
                        Math.round(X0 + enemy.renderX(alpha) - glow.getRegionWidth() / 2.0),
                        Math.round(enemy.renderY(alpha) - glow.getRegionHeight() / 2.0));
                continue;
            }
            Color tint = slug.equals("spine-turret") ? TURRET_GLOW : slug.equals("polyp-mortar") ? MORTAR_GLOW : null;
            if (tint != null) {
                batch.setColor(tint.r, tint.g, tint.b, 0.9f);
                float size = (float) enemy.hitbox().width() * 0.8f;
                batch.draw(
                        radial,
                        (float) (X0 + enemy.renderX(alpha) - size / 2),
                        (float) (enemy.renderY(alpha) - size / 2),
                        size,
                        size);
            }
        }
        for (int i = 0; i < darkness.flares().size(); i++) {
            if (darkness.flareBurning(i, seconds)) {
                float strength = flareStrength(i, seconds);
                double x = darkness.flares().get(i).x();
                double y = darkness.flareY(i, seconds);
                if (!flareShell.isEmpty()) {
                    AtlasRegion shell = flareShell.get((int) (sortie.tick() / FLARE_SHELL_TICKS % flareShell.size));
                    batch.setColor(1, 1, 1, strength);
                    batch.draw(
                            shell,
                            Math.round(X0 + x - shell.getRegionWidth() / 2.0),
                            Math.round(y - shell.getRegionHeight() / 2.0));
                    continue;
                }
                batch.setColor(FLARE.r, FLARE.g, FLARE.b, 0.5f * strength);
                batch.draw(radial, (float) (X0 + x - 24), (float) (y - 24), 48, 48);
                batch.setColor(1, 1, 0.9f, strength);
                batch.draw(radial, (float) (X0 + x - 6), (float) (y - 6), 12, 12);
            }
        }
        batch.setColor(Color.WHITE);
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
    }

    /**
     * The Mantis's frame (design/enemies/air/mantis): its own render for the edge it holds (nothing
     * lit is mirrored), by its state: the hover's wing beat, the telegraph's arms opening over the
     * telegraph, the sweep's flicker, and leaning out as it flies back out through its edge; null
     * for a unit without the production set (placeholder shapes).
     */
    public AtlasRegion mantisFrame(EnemyLooks look, Enemy enemy, long tick) {
        if (look.frames().size != 2 * MANTIS_PER_SIDE || enemy.spec().sweep().isEmpty()) {
            return null;
        }
        boolean left = enemy.renderX(1) < PlayField.WIDTH / 2.0;
        int beat = (int) (tick / MANTIS_FRAME_TICKS);
        int frame;
        if (enemy.telegraphing()) {
            long start = telegraphStart.computeIfAbsent(enemy, e -> tick);
            int ticks = SimStep.ticks(enemy.spec().sweep().get().telegraphSeconds());
            frame = MANTIS_TELEGRAPH + (int) Math.min(MANTIS_SWEEP - MANTIS_TELEGRAPH - 1, (tick - start) * 4 / ticks);
        } else {
            telegraphStart.remove(enemy);
            // Flying out through its own edge: its facing points outward (left edge: to the left).
            double outward = Math.sin(enemy.facing()) * (left ? 1 : -1);
            if (enemy.sweeping()) {
                frame = MANTIS_SWEEP + beat % 2;
            } else if (outward > 0.5) {
                frame = MANTIS_EXIT + beat % 4;
            } else {
                frame = beat % 4;
            }
        }
        return look.frames().get((left ? 0 : MANTIS_PER_SIDE) + frame);
    }

    /** A sweep's arc in whole degrees, 0-180: the telegraph wedge's index. */
    private static int arcDegrees(double arcRadians) {
        return Math.clamp(Math.round(Math.toDegrees(arcRadians)), 0, 180);
    }

    /**
     * The Mantis's sweeps, all from its eye (the sweep's origin): in the telegraph the pulsing
     * crimson wedge over the whole sweep, while it sweeps the charged beam with its knots running
     * out and the spark at its tip, the eye ring through both (additive); strokes and dots where the
     * production art is missing.
     */
    public void drawSweeps(SpriteBatch batch, Sortie sortie, float alpha) {
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
        for (int i = 0; i < sortie.enemyCount(); i++) {
            Enemy enemy = sortie.enemy(i);
            if (enemy.spec().sweep().isEmpty() || !(enemy.telegraphing() || enemy.sweeping())) {
                continue;
            }
            var sweep = enemy.spec().sweep().get();
            double length = sweep.length();
            double x = enemy.renderX(alpha) + enemy.beamOffsetX();
            double y = enemy.renderY(alpha) + enemy.beamOffsetY();
            int frame = (int) (sortie.tick() / BEAM_FRAME_TICKS);
            if (enemy.telegraphing()) {
                float pulse = 0.45f + 0.35f * (float) Math.abs(Math.sin(sortie.tick() * 0.4));
                TextureRegion wedge = telegraphs[arcDegrees(sweep.arcRadians())];
                if (wedge != null) {
                    batch.setColor(1, 1, 1, Math.min(1, 0.6f + 0.5f * pulse));
                    float h = wedge.getRegionHeight();
                    batch.draw(
                            wedge,
                            (float) (X0 + x),
                            (float) (y - h / 2),
                            0,
                            h / 2,
                            wedge.getRegionWidth(),
                            h,
                            1,
                            1,
                            degrees(enemy.sweepCentre()));
                } else {
                    batch.setColor(CRIMSON.r, CRIMSON.g, CRIMSON.b, pulse);
                    stroke(batch, x, y, enemy.sweepFrom(), length, 2);
                    stroke(batch, x, y, enemy.sweepTo(), length, 2);
                    for (int k = 0; k <= ARC_DOTS; k++) {
                        double heading = enemy.sweepFrom() + (enemy.sweepTo() - enemy.sweepFrom()) * k / ARC_DOTS;
                        double ax = x - Math.sin(heading) * length;
                        double ay = y - Math.cos(heading) * length;
                        batch.draw(pixel, (float) (X0 + ax - 1.5), (float) (ay - 1.5), 3, 3);
                    }
                }
            } else {
                double heading = enemy.beam(alpha);
                if (!beamStrip.isEmpty()) {
                    batch.setColor(Color.WHITE);
                    drawBeam(batch, beamStrip.get(frame % beamStrip.size), x, y, heading, length);
                    if (!beamTip.isEmpty()) {
                        centred(
                                batch,
                                beamTip.get(frame % beamTip.size),
                                x - Math.sin(heading) * length,
                                y - Math.cos(heading) * length);
                    }
                } else {
                    double width = sweep.width();
                    batch.setColor(CRIMSON.r, CRIMSON.g, CRIMSON.b, 0.9f);
                    stroke(batch, x, y, heading, length, (float) width);
                    batch.setColor(1, 0.9f, 0.9f, 0.9f);
                    stroke(batch, x, y, heading, length, (float) Math.max(1, width / 3));
                }
            }
            if (beamEye != null) {
                batch.setColor(Color.WHITE);
                centred(batch, beamEye, x, y);
            }
        }
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        batch.setColor(Color.WHITE);
    }

    /**
     * The beam strip tiled from (x, y) along {@code heading}, {@code length} long, each tile rotated
     * about the beam's start; the last tile cut to what is left.
     */
    private static void drawBeam(
            SpriteBatch batch, TextureRegion strip, double x, double y, double heading, double length) {
        float degrees = degrees(heading);
        int w = strip.getRegionWidth();
        int h = strip.getRegionHeight();
        float left = (float) (X0 + x);
        float bottom = (float) (y - h / 2.0);
        for (int along = 0; along < length; along += w) {
            int piece = (int) Math.min(w, Math.ceil(length - along));
            batch.draw(
                    strip.getTexture(),
                    left + along,
                    bottom,
                    -along,
                    h / 2f,
                    piece,
                    h,
                    1,
                    1,
                    degrees,
                    strip.getRegionX(),
                    strip.getRegionY(),
                    piece,
                    h,
                    false,
                    false);
        }
    }

    /** A region centred on (x, y) of the play field. */
    private static void centred(SpriteBatch batch, TextureRegion region, double x, double y) {
        batch.draw(
                region, (float) (X0 + x - region.getRegionWidth() / 2.0), (float) (y - region.getRegionHeight() / 2.0));
    }

    /** A heading (radians clockwise from straight down) as a batch rotation: 0° points right, counter-clockwise. */
    private static float degrees(double heading) {
        return (float) Math.toDegrees(Math.atan2(-Math.cos(heading), -Math.sin(heading)));
    }

    /** A stroke from (x, y) along {@code heading} (radians clockwise from straight down), {@code length} long. */
    private void stroke(SpriteBatch batch, double x, double y, double heading, double length, float width) {
        // The pixel stretched to the stroke, rotated about its start.
        batch.draw(
                pixel,
                (float) (X0 + x),
                (float) (y - width / 2),
                0,
                width / 2,
                (float) length,
                width,
                1,
                1,
                degrees(heading));
    }

    /**
     * The Smart Bomb: the white flash over the play field (its hold at its opacity, then its fade;
     * toned down by the flash reduction) and the ring expanding from where it went off.
     */
    public void drawSmartBomb(SpriteBatch batch, Sortie sortie, float alpha, float flashStrength) {
        SpecialSlot special = sortie.special();
        SmartBombSpec bomb = special.smartBomb();
        if (bomb == null) {
            return;
        }
        double seconds = special.bombSeconds(alpha);
        if (seconds < 0) {
            return;
        }
        double flash = seconds <= bomb.flashSeconds()
                ? bomb.flashOpacity()
                : bomb.flashOpacity() * Math.max(0, 1 - (seconds - bomb.flashSeconds()) / bomb.fadeSeconds());
        if (flash > 0) {
            batch.setColor(1, 1, 1, (float) flash * flashStrength);
            batch.draw(pixel, X0, 0, PlayField.WIDTH, PlayField.HEIGHT);
        }
        double radius = special.ringRadius(alpha);
        if (radius > 0) {
            batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
            batch.setColor(RING.r, RING.g, RING.b, 0.85f);
            double stroke = 2 * Math.PI * radius / RING_STROKES + 1;
            for (int k = 0; k < RING_STROKES; k++) {
                double angle = 2 * Math.PI * k / RING_STROKES;
                double x = special.bombX() + Math.cos(angle) * radius;
                double y = special.bombY() + Math.sin(angle) * radius;
                if (x < -8 || x > PlayField.WIDTH + 8 || y < -8 || y > PlayField.HEIGHT + 8) {
                    continue;
                }
                float degrees = (float) Math.toDegrees(angle) + 90;
                batch.draw(
                        pixel,
                        (float) (X0 + x - stroke / 2),
                        (float) (y - 3),
                        (float) stroke / 2,
                        3,
                        (float) stroke,
                        6,
                        1,
                        1,
                        degrees);
            }
            batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        }
        batch.setColor(Color.WHITE);
    }

    @Override
    public void dispose() {
        radialTexture.dispose();
        if (lightMap != null) {
            lightMap.dispose();
            coneTexture.dispose();
        }
    }
}
