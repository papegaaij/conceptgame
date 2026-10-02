package vanguard.game.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import vanguard.content.LevelData;
import vanguard.sim.Enemy;
import vanguard.sim.EnemyBullet;
import vanguard.sim.GroundObject;
import vanguard.sim.Pickup;
import vanguard.sim.PickupType;
import vanguard.sim.Ship;
import vanguard.sim.ShipSpec;
import vanguard.sim.Shot;
import vanguard.sim.SimStep;
import vanguard.sim.Sortie;
import vanguard.sim.WeaponSpec;

/**
 * Draws a level back to front, interpolating every position between the last two simulation
 * steps: the backdrop down to the ground layer, the ground objects with their debris and glints,
 * the low-air layer, the enemies, the pickups, the solid rounds (missiles, bombs, shells), the ship
 * with its wing pods, the glowing shots, muzzle flashes and effects, the high-air layer, then the
 * enemy bullets above every layer (design/enemies, bullet readability rules), the edge warnings
 * and the credit numbers.
 */
public final class LevelRenderer {
    private static final float X0 = PixelScreen.PLAY_FIELD_X;
    /** The muzzle flash shows each of its three frames for two steps after a shot. */
    private static final int MUZZLE_FRAME_TICKS = 2;
    /** Pickups spin at about 10 fps and blink in their last 1.5 s (chosen pickups concept, round 09). */
    private static final int PICKUP_FRAME_TICKS = 6;
    /** Enemy bullets pulse their core at 15 fps, each at its own phase. */
    private static final int BULLET_FRAME_TICKS = 4;

    private static final int BLINK_TICKS = SimStep.ticks(1.5);
    /**
     * Loot targets (design/art-direction, readability rule 7): a white hit flash for two steps, a
     * glint about every 2 s (each target at its own phase), and the secret's beacon blinks at 1 Hz.
     */
    private static final int HIT_FLASH_TICKS = 2;

    private static final int GLINT_PERIOD_TICKS = SimStep.ticks(2);
    private static final int GLINT_FRAME_TICKS = 3;
    private static final int BEACON_BLINK_TICKS = 30;

    private static final Color HIT_WHITE = Color.WHITE;
    /** The white flashes' strength with the Gameplay tab's flash reduction on. */
    private static final float REDUCED_FLASH = 0.35f;

    private static final Color SHIELD_BLUE = Color.valueOf("00C0FF");
    private static final float SHIELD_SHIMMER = 0.6f;
    /** A bomb is drawn shrinking to this scale as it falls (design/player/weapons/bomb-rack). */
    private static final float BOMB_LANDING_SCALE = 0.6f;
    /** A shell grows by this much at the top of its arc (design/player/weapons/hammer-mortar: 1.0 -> 1.4 -> 1.0). */
    private static final float SHELL_ARC_SCALE = 0.4f;
    /** Bolts with a range fade out over its last part (design/player/weapons/scatter-vulcan). */
    private static final double FADE_SHARE = 0.25;

    private final Sprites sprites;
    private final EnemyLooks[] looks;
    private final WeaponLooks weapons;
    private final AtlasRegion reticle;
    private final Backdrop backdrop;
    private final FlashShader flash;
    private final BitmapFont font;
    private float whiteFlash = 1;

    /** @param levelKey the level's key, {@code <act>/level-NN-<slug>} */
    public LevelRenderer(
            Sprites sprites,
            EnemyLooks[] looks,
            WeaponLooks weapons,
            FlashShader flash,
            BitmapFont font,
            LevelData level,
            String levelKey) {
        this.sprites = sprites;
        this.looks = looks;
        this.weapons = weapons;
        this.reticle = sprites.region("mortar-reticle");
        this.backdrop = new Backdrop(sprites, level, levelKey);
        this.flash = flash;
        this.font = font;
    }

    /**
     * @param debris animations started at ground positions (y plus the ground's scroll), see
     *     {@link Effects#draw}
     * @param alpha interpolation between the previous and the current step
     * @param shieldShimmer 0..1, how strongly the ship shows its last shield hit
     * @param flashReduction tone the white hit and invulnerability flashes down (Gameplay tab)
     */
    public void draw(
            SpriteBatch batch,
            Sortie sortie,
            Effects effects,
            Effects debris,
            CreditNumbers credits,
            EdgeWarnings warnings,
            float alpha,
            float shieldShimmer,
            boolean flashReduction) {
        whiteFlash = flashReduction ? REDUCED_FLASH : 1;
        double lag = SimStep.SECONDS * (1 - alpha);
        double scroll = sortie.groundScroll() - sortie.groundSpeed() * lag;
        double seconds = sortie.levelSeconds() - lag;
        backdrop.drawBehind(batch, scroll, seconds);
        drawGround(batch, sortie, alpha);
        debris.draw(batch, -scroll);
        drawGlints(batch, sortie, alpha);
        backdrop.drawLowAir(batch, scroll, seconds);
        drawEnemies(batch, sortie, alpha);
        drawPickups(batch, sortie, alpha);
        drawShots(batch, sortie, alpha, false);
        if (sortie.flying()) {
            drawShip(batch, sortie.ship(), alpha, shieldShimmer);
        }
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
        drawShots(batch, sortie, alpha, true);
        if (sortie.flying()) {
            drawMuzzles(batch, sortie, alpha, true);
        }
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        if (sortie.flying()) {
            drawMuzzles(batch, sortie, alpha, false);
        }
        effects.draw(batch, 0);
        backdrop.drawFront(batch, scroll, seconds);
        drawBullets(batch, sortie, alpha);
        warnings.draw(batch, sortie.tick(), alpha);
        credits.draw(batch, font);
    }

    /** The loot targets, intact or damaged, a hit flashing white; the beacon blinks until spent. */
    private void drawGround(SpriteBatch batch, Sortie sortie, float alpha) {
        for (int i = 0; i < sortie.groundObjectCount(); i++) {
            GroundObject object = sortie.groundObject(i);
            int damaged = object.damaged() ? 1 : 0;
            TextureRegion frame;
            if (object.spec().trigger()) {
                int lit = !object.spent() && sortie.tick() / BEACON_BLINK_TICKS % 2 == 0 ? 1 : 0;
                frame = sprites.beacon.get(2 * damaged + lit);
            } else {
                frame = sprites.cargoContainer.get(damaged);
            }
            if (object.ticksSinceHit() < HIT_FLASH_TICKS) {
                flash.draw(
                        batch,
                        frame,
                        Math.round(X0 + object.renderX()),
                        Math.round(object.renderY(alpha)),
                        HIT_WHITE,
                        whiteFlash);
            } else {
                drawCentred(batch, frame, object.renderX(), object.renderY(alpha));
            }
        }
    }

    /** Each loot target sparkles at its top-left quarter (the key light's side) every ~2 s. */
    private void drawGlints(SpriteBatch batch, Sortie sortie, float alpha) {
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
        for (int i = 0; i < sortie.groundObjectCount(); i++) {
            GroundObject object = sortie.groundObject(i);
            int phase = (int) object.renderX() * 7;
            int frame = (int) ((sortie.tick() + phase) % GLINT_PERIOD_TICKS / GLINT_FRAME_TICKS);
            if (!object.spent() && frame < sprites.glint.size) {
                drawCentred(
                        batch,
                        sprites.glint.get(frame),
                        object.renderX() - object.spec().size().width() / 4,
                        object.renderY(alpha) + object.spec().size().height() / 4);
            }
        }
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
    }

    private void drawEnemies(SpriteBatch batch, Sortie sortie, float alpha) {
        for (int i = 0; i < sortie.enemyCount(); i++) {
            Enemy enemy = sortie.enemy(i);
            EnemyLooks look = looks[enemy.kind()];
            AtlasRegion frame = look.frame(enemy.facing(), sortie.tick() / look.frameTicks() + i);
            drawCentred(batch, frame, enemy.renderX(alpha), enemy.renderY(alpha));
        }
    }

    private void drawPickups(SpriteBatch batch, Sortie sortie, float alpha) {
        for (int i = 0; i < sortie.pickupCount(); i++) {
            Pickup pickup = sortie.pickup(i);
            if (pickup.ticksLeft() < BLINK_TICKS && pickup.ticksLeft() / 6 % 2 == 0) {
                continue;
            }
            Array<AtlasRegion> frames = pickupFrames(pickup.type());
            int frame = (int) ((sortie.tick() / PICKUP_FRAME_TICKS + i) % frames.size);
            drawCentred(batch, frames.get(frame), pickup.renderX(), pickup.renderY(alpha));
        }
    }

    private Array<AtlasRegion> pickupFrames(PickupType type) {
        return switch (type) {
            case SMALL_SALVAGE -> sprites.salvageSmall;
            case HIDDEN_CRATE -> sprites.crate;
            case SHIELD_CELL -> sprites.shieldCell;
            case ARMOUR_PATCH -> sprites.armourPatch;
        };
    }

    private void drawShip(SpriteBatch batch, Ship ship, float alpha, float shieldShimmer) {
        TextureRegion hull = sprites.ship.get(ship.bank() + ShipSpec.HARD_BANK);
        float x = Math.round(X0 + ship.renderX(alpha));
        float y = Math.round(ship.renderY(alpha));
        int mercy = ship.defences().mercyTicks();
        // The hull blinks white in steps of three frames while the mercy invulnerability lasts.
        if (mercy > 0 && (mercy + 2) / 3 % 2 == 1) {
            flash.draw(batch, hull, x, y, HIT_WHITE, whiteFlash);
        } else if (shieldShimmer > 0) {
            flash.draw(batch, hull, x, y, SHIELD_BLUE, shieldShimmer * SHIELD_SHIMMER);
        } else {
            batch.draw(hull, x - hull.getRegionWidth() / 2f, y - hull.getRegionHeight() / 2f);
        }
        drawPods(
                batch,
                ship.bank() + ShipSpec.HARD_BANK,
                x - hull.getRegionWidth() / 2f,
                y + hull.getRegionHeight() / 2f);
    }

    /** The fitted wing pods over the hull, whose top-left is at (left, top). */
    private void drawPods(SpriteBatch batch, int bank, float left, float top) {
        for (int m = 0; m < weapons.size(); m++) {
            WeaponLooks.Look look = weapons.look(m);
            if (look.pod != null) {
                AtlasRegion pod = look.pod.get(bank);
                int[] offset = look.podOffsets[bank];
                batch.draw(pod, left + offset[0], top - offset[1] - pod.getRegionHeight());
            }
        }
    }

    /** The glowing shots (tracers, bolts, lances) or the solid rounds (missiles, bombs, shells). */
    private void drawShots(SpriteBatch batch, Sortie sortie, float alpha, boolean glowing) {
        for (int i = 0; i < sortie.shotCount(); i++) {
            Shot shot = sortie.shot(i);
            if (weapons.look(shot.mount()).glowingShot != glowing) {
                continue;
            }
            AtlasRegion sprite = weapons.sprite(shot);
            double x = shot.renderX(alpha);
            double y = shot.renderY(alpha);
            switch (shot.weapon().delivery()) {
                case DROPPED ->
                    drawScaled(batch, sprite, x, y, 1 - (1 - BOMB_LANDING_SCALE) * (float) shot.airProgress(alpha));
                case LOBBED ->
                    drawScaled(
                            batch,
                            sprite,
                            x,
                            y,
                            1 + SHELL_ARC_SCALE * (float) Math.sin(Math.PI * shot.airProgress(alpha)));
                case BOLT, HOMING -> {
                    double left = shot.rangeLeft();
                    if (left < FADE_SHARE) {
                        batch.setColor(1, 1, 1, (float) (left / FADE_SHARE));
                        drawCentred(batch, sprite, x, y);
                        batch.setColor(Color.WHITE);
                    } else {
                        drawCentred(batch, sprite, x, y);
                    }
                }
            }
        }
    }

    /**
     * The muzzle flashes of the mounts that just fired, at each muzzle of the pattern they fire, and
     * the mortar's faint landing reticle where its next shells land (design/player/weapons).
     */
    private void drawMuzzles(SpriteBatch batch, Sortie sortie, float alpha, boolean glowing) {
        Ship ship = sortie.ship();
        double shipX = ship.renderX(alpha);
        double shipY = ship.renderY(alpha);
        boolean overdrive = sortie.overdriveSeconds() > 0;
        for (int m = 0; m < weapons.size(); m++) {
            WeaponLooks.Look look = weapons.look(m);
            WeaponSpec weapon = overdrive
                    ? sortie.armament().mount(m).overdrive()
                    : sortie.armament().mount(m).weapon();
            if (glowing && weapon.delivery() == WeaponSpec.Delivery.LOBBED) {
                for (int i = 0; i < weapon.muzzles().size(); i++) {
                    WeaponSpec.Muzzle muzzle = weapon.muzzles().get(i);
                    drawCentred(batch, reticle, shipX + muzzle.dx(), shipY + muzzle.dy() + weapon.range());
                }
            }
            int frame = sortie.ticksSinceShot(m) / MUZZLE_FRAME_TICKS;
            if (look.glowingMuzzle != glowing || frame >= look.muzzle.size) {
                continue;
            }
            for (int i = 0; i < weapon.muzzles().size(); i++) {
                WeaponSpec.Muzzle muzzle = weapon.muzzles().get(i);
                drawCentred(batch, look.muzzle.get(frame), shipX + muzzle.dx(), shipY + muzzle.dy());
            }
        }
    }

    private void drawBullets(SpriteBatch batch, Sortie sortie, float alpha) {
        for (int i = 0; i < sortie.bulletCount(); i++) {
            EnemyBullet bullet = sortie.bullet(i);
            int frame = (int) ((sortie.tick() / BULLET_FRAME_TICKS + i) % sprites.orb.size);
            drawCentred(batch, sprites.orb.get(frame), bullet.renderX(alpha), bullet.renderY(alpha));
        }
    }

    private static void drawScaled(SpriteBatch batch, TextureRegion region, double x, double y, float scale) {
        float width = region.getRegionWidth();
        float height = region.getRegionHeight();
        batch.draw(
                region,
                Math.round(X0 + x - width / 2),
                Math.round(y - height / 2),
                width / 2,
                height / 2,
                width,
                height,
                scale,
                scale,
                0);
    }

    private static void drawCentred(SpriteBatch batch, TextureRegion region, double x, double y) {
        batch.draw(
                region,
                Math.round(X0 + x - region.getRegionWidth() / 2.0),
                Math.round(y - region.getRegionHeight() / 2.0));
    }
}
