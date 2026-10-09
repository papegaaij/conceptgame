package vanguard.game.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Array;
import java.util.Arrays;
import vanguard.sim.Enemy;
import vanguard.sim.Layer;
import vanguard.sim.Ship;
import vanguard.sim.ShipSpec;
import vanguard.sim.Shot;
import vanguard.sim.SimEvents;
import vanguard.sim.SimStep;
import vanguard.sim.Sortie;
import vanguard.sim.WeaponSpec;

/**
 * Draws the hangar's test fire (design/ui/hangar, Test fire) into its box with the level's own
 * pieces: the ship with its wing pod, the weapon's shots, muzzle flashes and impacts
 * ({@link WeaponLooks}) and the explosions ({@link Effects}), over a holographic range: a ground grid
 * moving with the scroll and the dummy targets as amber target boxes that flash white when hit. The
 * play field is drawn turned a quarter clockwise at half size, so the ship faces right along the
 * wide box: ahead is right, behind is left, the ship's left is up. A transform maps play-field
 * points to the box, a scissor keeps everything inside it. M5 part E: a torpedo's range is water (its
 * loop's level is over water): a band of sea under the grid, the dummies submerged under it as faint
 * cyan boxes, the torpedoes with their bubbles running under the surface (dimmed as the {@code sub}
 * pass would), dropping in with a splash and bursting under the water. Drawing allocates nothing.
 */
public final class TestFireView {
    /** Play-field px to box px. */
    public static final float SCALE = 0.5f;
    /** Steps an impact or explosion frame shows (the level's). */
    private static final int FRAME_TICKS = 2;
    /** A hit dummy shows white this many steps. */
    private static final int FLASH_TICKS = 3;
    /** The grid's lines lie this far apart on the ground, px. */
    private static final int GRID = 40;
    /** M5 part E: the water effects' frames (tools/art/water_fx.py's 20 fps). */
    private static final int WATER_FRAME_TICKS = 3;
    /** Bolts with a range fade out over its last quarter (the level's rule). */
    private static final double FADE_SHARE = 0.25;
    /** A missile's smoke trail leaves a puff every this many steps (the level's). */
    private static final int TRAIL_TICKS = 4;
    /** The dummies' pool slots that are tracked for their hit flash. */
    private static final int MAX_DUMMIES = 8;

    private static final Color GRID_LINE = new Color(0, 1, 1, 0.16f);
    private static final Color DUMMY_FILL = new Color(1, 0.75f, 0, 0.22f);
    private static final Color DUMMY_EDGE = new Color(1, 0.8f, 0.1f, 1);
    private static final Color DUMMY_FLASH = Color.WHITE;
    /** M5 part E: the range's water, and a submerged dummy and a torpedo seen through it. */
    private static final Color WATER = new Color(0.02f, 0.16f, 0.3f, 0.75f);

    private static final Color SUB_FILL = new Color(0.2f, 0.75f, 1, 0.14f);
    private static final Color SUB_EDGE = new Color(0.35f, 0.85f, 1, 0.6f);
    private static final Color UNDER = new Color(0.62f, 0.72f, 0.82f, 0.85f);

    private final Sprites sprites;
    private final PodPivots pods;
    private final Array<AtlasRegion> glance;
    private final Effects effects = Effects.glowing();
    /** A missile's smoke trail (the Hornet's), as the level leaves it. */
    private final Effects trails = Effects.solid();
    /** M5 part E: the torpedo's bubbles, splashes and water bursts (alpha-blended, as the level's water effects). */
    private final Effects water = Effects.solid();

    private final Matrix4 saved = new Matrix4();
    private final Matrix4 transform = new Matrix4();
    private final int[] flash = new int[MAX_DUMMIES];
    private WeaponLooks weapons;
    /** The craft's banking frames: the Stormhawk's, or Rook's for his guns. */
    private Array<AtlasRegion> hull;

    public TestFireView(Sprites sprites, PodPivots pods) {
        this.sprites = sprites;
        hull = sprites.ship;
        this.pods = pods;
        glance = sprites.frames("ballistic-impact");
    }

    /** A new loop of a new weapon: its looks, nothing of the last one left. */
    public void show(Sortie sortie, int level) {
        show(sortie, level, false);
    }

    /**
     * As {@link #show(Sortie, int)}; with {@code rook} the craft is Rook's (one of his guns in the
     * escort's test fire, M5 part A), while his frames exist.
     */
    public void show(Sortie sortie, int level, boolean rook) {
        weapons = new WeaponLooks(sortie.armament(), new int[] {level}, sprites, pods);
        hull = rook && sprites.has("rook") ? sprites.frames("rook") : sprites.ship;
        restart();
    }

    /** The loop started over: its effects are gone. */
    public void restart() {
        effects.clear();
        trails.clear();
        water.clear();
        Arrays.fill(flash, 0);
    }

    /** After each simulation step: the effects advance, the step's hits and explosions start theirs. */
    public void stepped(Sortie sortie) {
        effects.step();
        trails.step();
        water.step();
        if (sortie.tick() % TRAIL_TICKS == 0) {
            for (int i = 0; i < sortie.shotCount(); i++) {
                Shot shot = sortie.shot(i);
                Array<AtlasRegion> trail = weapons.trail(shot.mount());
                if (trail != null) {
                    (weapons.trailUnder(shot.mount()) ? water : trails)
                            .start(trail, WATER_FRAME_TICKS, shot.renderX(1), shot.renderY(1));
                }
            }
        }
        for (int i = 0; i < flash.length; i++) {
            if (flash[i] > 0) {
                flash[i]--;
            }
        }
        SimEvents events = sortie.events();
        for (int i = 0; i < events.size(); i++) {
            double x = events.x(i);
            double y = events.y(i);
            switch (events.type(i)) {
                case SHOT_FIRED -> {
                    Array<AtlasRegion> splash = weapons.dropSplash(events.value(i));
                    if (splash != null && sortie.script().water()) {
                        water.start(splash, WATER_FRAME_TICKS, x, y);
                    }
                }
                case ENEMY_HIT -> {
                    Array<AtlasRegion> under = weapons.waterImpact(events.value(i), true);
                    if (under != null) {
                        water.start(under, WATER_FRAME_TICKS, x, y);
                    } else {
                        effects.start(weapons.impact(events.value(i)), FRAME_TICKS, x, y);
                    }
                    int hit = nearest(sortie, x, y);
                    if (hit >= 0 && hit < flash.length) {
                        flash[hit] = FLASH_TICKS;
                    }
                }
                case SHOT_GLANCED -> effects.start(glance, FRAME_TICKS, x, y);
                case BLAST -> effects.start(weapons.blast(events.value(i), sprites.explosionSmall), FRAME_TICKS, x, y);
                case ENEMY_DESTROYED -> effects.start(sprites.explosionSmall, FRAME_TICKS, x, y);
                default -> {}
            }
        }
    }

    /** The dummy nearest to (x, y); -1 for none. */
    private static int nearest(Sortie sortie, double x, double y) {
        int nearest = -1;
        double best = Double.MAX_VALUE;
        for (int j = 0; j < sortie.enemyCount(); j++) {
            Enemy enemy = sortie.enemy(j);
            double dx = enemy.renderX(1) - x;
            double dy = enemy.renderY(1) - y;
            double d = dx * dx + dy * dy;
            if (d < best) {
                best = d;
                nearest = j;
            }
        }
        return nearest;
    }

    /**
     * Draws the sortie into the box whose top-left is ({@code boxX}, {@code boxY}) on the 960x540
     * screen (y down), with the play-field point ({@code centreX}, {@code centreY}) at its centre.
     */
    public void draw(
            SpriteBatch batch,
            Sortie sortie,
            float alpha,
            int boxX,
            int boxY,
            int boxWidth,
            int boxHeight,
            double centreX,
            double centreY) {
        batch.flush();
        saved.set(batch.getTransformMatrix());
        transform
                .set(saved)
                .translate(boxX + boxWidth / 2f, PixelScreen.HEIGHT - boxY - boxHeight / 2f, 0)
                .rotate(0, 0, 1, -90)
                .scale(SCALE, SCALE, 1)
                .translate((float) -(PixelScreen.PLAY_FIELD_X + centreX), (float) -centreY, 0);
        Gdx.gl.glEnable(GL20.GL_SCISSOR_TEST);
        Gdx.gl.glScissor(boxX + 1, PixelScreen.HEIGHT - boxY - boxHeight + 1, boxWidth - 2, boxHeight - 2);
        batch.setTransformMatrix(transform);
        boolean sea = sortie.script().water();
        if (sea) {
            drawWater(batch, boxWidth / SCALE, boxHeight / SCALE, centreX, centreY);
        }
        drawGrid(batch, sortie, alpha, boxWidth / SCALE, boxHeight / SCALE, centreX, centreY);
        drawDummies(batch, sortie, alpha);
        if (sea) {
            // Under the surface, dimmed toward the water as the level's sub pass draws them.
            batch.setColor(UNDER);
            drawUnder(batch, sortie, alpha);
            batch.setColor(Color.WHITE);
        }
        water.draw(batch, 0);
        trails.draw(batch, 0);
        drawShots(batch, sortie, alpha, false);
        drawShip(batch, sortie.ship(), alpha);
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
        drawShots(batch, sortie, alpha, true);
        drawMuzzles(batch, sortie, alpha, true);
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        drawMuzzles(batch, sortie, alpha, false);
        effects.draw(batch, 0);
        batch.setTransformMatrix(saved);
        Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST);
    }

    /** M5 part E: the range's water under the whole box (the loop's level is over water). */
    private void drawWater(SpriteBatch batch, float along, float across, double centreX, double centreY) {
        batch.setColor(WATER);
        batch.draw(
                sprites.pixel,
                (float) (PixelScreen.PLAY_FIELD_X + centreX - across / 2),
                (float) (centreY - along / 2),
                across,
                along);
        batch.setColor(Color.WHITE);
    }

    /** M5 part E: the torpedoes running under the water (their own heading set). */
    private void drawUnder(SpriteBatch batch, Sortie sortie, float alpha) {
        for (int i = 0; i < sortie.shotCount(); i++) {
            Shot shot = sortie.shot(i);
            if (shot.weapon().delivery() == WeaponSpec.Delivery.TORPEDO) {
                drawScaled(batch, weapons.sprite(shot), shot.renderX(alpha), shot.renderY(alpha), 1);
            }
        }
    }

    /** Lines across the field on the ground, moving with the scroll: the range drifts past the ship. */
    private void drawGrid(
            SpriteBatch batch, Sortie sortie, float alpha, float along, float across, double centreX, double centreY) {
        double scroll = sortie.groundScroll() - sortie.groundSpeed() * SimStep.SECONDS * (1 - alpha);
        float left = (float) (PixelScreen.PLAY_FIELD_X + centreX - across / 2);
        double first = centreY - along / 2;
        double offset = ((-scroll - first) % GRID + GRID) % GRID;
        batch.setColor(GRID_LINE);
        for (double y = first + offset; y < centreY + along / 2; y += GRID) {
            batch.draw(sprites.pixel, left, (float) y, across, 2);
        }
        batch.setColor(Color.WHITE);
    }

    /** The dummies: amber target boxes with a centre pip, white while hit. */
    private void drawDummies(SpriteBatch batch, Sortie sortie, float alpha) {
        TextureRegion pixel = sprites.pixel;
        for (int j = 0; j < sortie.enemyCount(); j++) {
            Enemy enemy = sortie.enemy(j);
            float w = (float) enemy.hitbox().width();
            float h = (float) enemy.hitbox().height();
            float x = (float) (PixelScreen.PLAY_FIELD_X + enemy.renderX(alpha) - w / 2);
            float y = (float) (enemy.renderY(alpha) - h / 2);
            boolean hit = j < flash.length && flash[j] > 0;
            // M5 part E: a submerged dummy (the torpedo's range) is a faint cyan box under the water.
            boolean under = enemy.layer() == Layer.SUB;
            batch.setColor(hit ? DUMMY_FLASH : under ? SUB_FILL : DUMMY_FILL);
            batch.draw(pixel, x, y, w, h);
            batch.setColor(hit ? DUMMY_FLASH : under ? SUB_EDGE : DUMMY_EDGE);
            batch.draw(pixel, x, y, w, 2);
            batch.draw(pixel, x, y + h - 2, w, 2);
            batch.draw(pixel, x, y, 2, h);
            batch.draw(pixel, x + w - 2, y, 2, h);
            batch.draw(pixel, x + w / 2 - 2, y + h / 2 - 2, 4, 4);
        }
        batch.setColor(Color.WHITE);
    }

    /** As the level draws the ship: its banking frame with the fitted wing pod. */
    private void drawShip(SpriteBatch batch, Ship ship, float alpha) {
        int bank = ship.bank() + ShipSpec.HARD_BANK;
        TextureRegion hull = this.hull.get(Math.min(bank, this.hull.size - 1));
        float x = Math.round(PixelScreen.PLAY_FIELD_X + ship.renderX(alpha));
        float y = Math.round(ship.renderY(alpha));
        float left = x - hull.getRegionWidth() / 2f;
        float top = y + hull.getRegionHeight() / 2f;
        batch.draw(hull, left, y - hull.getRegionHeight() / 2f);
        for (int m = 0; m < weapons.size(); m++) {
            WeaponLooks.Look look = weapons.look(m);
            if (look.pod != null) {
                AtlasRegion pod = look.pod.get(bank);
                int[] offset = look.podOffsets[bank];
                batch.draw(pod, left + offset[0], top - offset[1] - pod.getRegionHeight());
            }
        }
    }

    /** As the level draws them: the glowing shots, or the solid rounds (bombs shrink falling, shells swell on their arc). */
    private void drawShots(SpriteBatch batch, Sortie sortie, float alpha, boolean glowing) {
        for (int i = 0; i < sortie.shotCount(); i++) {
            Shot shot = sortie.shot(i);
            if (weapons.look(shot.mount()).glowingShot != glowing
                    || (shot.weapon().delivery() == WeaponSpec.Delivery.TORPEDO
                            && sortie.script().water())) {
                continue;
            }
            AtlasRegion sprite = weapons.sprite(shot);
            double x = shot.renderX(alpha);
            double y = shot.renderY(alpha);
            switch (shot.weapon().delivery()) {
                case DROPPED -> drawScaled(batch, sprite, x, y, 1 - 0.4f * (float) shot.airProgress(alpha));
                case LOBBED ->
                    drawScaled(batch, sprite, x, y, 1 + 0.4f * (float) Math.sin(Math.PI * shot.airProgress(alpha)));
                case MINE -> drawScaled(batch, sprite, x, y, 1);
                case BOLT, HOMING, TURRET, TORPEDO -> {
                    // Bolts with a range fade out over its last part, as in the level.
                    double left = shot.rangeLeft();
                    batch.setColor(1, 1, 1, left < FADE_SHARE ? (float) (left / FADE_SHARE) : 1);
                    drawScaled(batch, sprite, x, y, 1);
                    batch.setColor(Color.WHITE);
                }
            }
        }
    }

    /** The muzzle flash of the weapon that just fired, at each muzzle of its pattern. */
    private void drawMuzzles(SpriteBatch batch, Sortie sortie, float alpha, boolean glowing) {
        Ship ship = sortie.ship();
        for (int m = 0; m < weapons.size(); m++) {
            WeaponLooks.Look look = weapons.look(m);
            int frame = sortie.ticksSinceShot(m) / 2;
            if (look.glowingMuzzle != glowing || frame >= look.muzzle.size) {
                continue;
            }
            WeaponSpec weapon = sortie.armament().mount(m).weapon();
            for (int i = 0; i < weapon.muzzles().size(); i++) {
                WeaponSpec.Muzzle muzzle = weapon.muzzles().get(i);
                drawScaled(
                        batch,
                        look.muzzle.get(frame),
                        ship.renderX(alpha) + muzzle.dx(),
                        ship.renderY(alpha) + muzzle.dy(),
                        1);
            }
        }
    }

    private static void drawScaled(SpriteBatch batch, TextureRegion region, double x, double y, float scale) {
        float width = region.getRegionWidth();
        float height = region.getRegionHeight();
        batch.draw(
                region,
                Math.round(PixelScreen.PLAY_FIELD_X + x - width / 2),
                Math.round(y - height / 2),
                width / 2,
                height / 2,
                width,
                height,
                scale,
                scale,
                0);
    }
}
