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
import vanguard.sim.PlayField;
import vanguard.sim.Ship;
import vanguard.sim.ShipSpec;
import vanguard.sim.Shot;
import vanguard.sim.SimStep;
import vanguard.sim.Sortie;
import vanguard.sim.WarningEdge;

/**
 * Draws a level back to front, interpolating every position between the last two simulation
 * steps: the backdrop down to the ground layer, the ground objects, the low-air layer, the
 * enemies, the pickups, the ship, the glowing bolts and effects, the high-air layer, then the
 * enemy bullets above every layer (design/enemies, bullet readability rules), the edge warnings
 * and the credit numbers.
 */
public final class LevelRenderer {
    private static final float X0 = PixelScreen.PLAY_FIELD_X;
    /** The muzzle flash shows each of its three frames for two steps after a shot. */
    private static final int MUZZLE_FRAME_TICKS = 2;
    /** Pickups spin at about 10 fps and blink in their last 1.5 s (chosen pickups concept, round 09). */
    private static final int PICKUP_FRAME_TICKS = 6;

    private static final int BLINK_TICKS = SimStep.ticks(1.5);
    /** Edge warnings flash at 4 Hz. */
    private static final int WARNING_FLASH_TICKS = 8;

    private static final Color HIT_WHITE = Color.WHITE;
    private static final Color SHIELD_BLUE = Color.valueOf("00C0FF");
    private static final float SHIELD_SHIMMER = 0.6f;
    /** Placeholder ground objects, drawn in code until they have concept art. */
    private static final Color CONTAINER = Color.valueOf("3A4660");

    private static final Color CONTAINER_STRIPE = Color.valueOf("C86A1E");
    private static final Color CONTAINER_EDGE = Color.valueOf("1A2030");
    private static final Color BEACON_LIGHT = Color.valueOf("FF2020");
    private static final Color BEACON_DARK = Color.valueOf("401010");
    private static final Color WARNING = Color.valueOf("FF4030");

    private final Sprites sprites;
    private final EnemyLooks[] looks;
    private final Backdrop backdrop;
    private final FlashShader flash;
    private final BitmapFont font;

    /** @param levelKey the level's key, {@code <act>/level-NN-<slug>} */
    public LevelRenderer(
            Sprites sprites, EnemyLooks[] looks, FlashShader flash, BitmapFont font, LevelData level, String levelKey) {
        this.sprites = sprites;
        this.looks = looks;
        this.backdrop = new Backdrop(sprites, level, levelKey);
        this.flash = flash;
        this.font = font;
    }

    /**
     * @param alpha interpolation between the previous and the current step
     * @param shieldShimmer 0..1, how strongly the ship shows its last shield hit
     */
    public void draw(
            SpriteBatch batch,
            Sortie sortie,
            Effects effects,
            CreditNumbers credits,
            float alpha,
            float shieldShimmer) {
        double lag = SimStep.SECONDS * (1 - alpha);
        double scroll = sortie.groundScroll() - sortie.groundSpeed() * lag;
        double seconds = sortie.levelSeconds() - lag;
        backdrop.drawBehind(batch, scroll, seconds);
        drawGround(batch, sortie, alpha);
        backdrop.drawLowAir(batch, scroll, seconds);
        drawEnemies(batch, sortie, alpha);
        drawPickups(batch, sortie, alpha);
        if (sortie.flying()) {
            drawShip(batch, sortie.ship(), alpha, shieldShimmer);
        }
        drawBolts(batch, sortie, alpha);
        effects.draw(batch);
        backdrop.drawFront(batch, scroll, seconds);
        drawBullets(batch, sortie, alpha);
        drawWarnings(batch, sortie);
        credits.draw(batch, font);
    }

    private void drawGround(SpriteBatch batch, Sortie sortie, float alpha) {
        for (int i = 0; i < sortie.groundObjectCount(); i++) {
            GroundObject object = sortie.groundObject(i);
            float width = (float) object.spec().size().width();
            float height = (float) object.spec().size().height();
            float x = Math.round(X0 + object.renderX() - width / 2);
            float y = Math.round(object.renderY(alpha) - height / 2);
            if (object.spec().trigger()) {
                boolean lit = !object.spent() && sortie.tick() / 30 % 2 == 0;
                fill(batch, CONTAINER_EDGE, x - 2, y - 2, width + 4, height + 4);
                fill(batch, lit ? BEACON_LIGHT : BEACON_DARK, x, y, width, height);
            } else {
                fill(batch, CONTAINER_EDGE, x - 1, y - 1, width + 2, height + 2);
                fill(batch, CONTAINER, x, y, width, height);
                fill(batch, CONTAINER_STRIPE, x, y + height / 2 - 2, width, 4);
            }
        }
    }

    private void drawEnemies(SpriteBatch batch, Sortie sortie, float alpha) {
        for (int i = 0; i < sortie.enemyCount(); i++) {
            Enemy enemy = sortie.enemy(i);
            EnemyLooks look = looks[enemy.kind()];
            int frame = (int) ((sortie.tick() / look.frameTicks() + i) % look.frames().size);
            drawCentred(batch, look.frames().get(frame), enemy.renderX(alpha), enemy.renderY(alpha));
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
            flash.draw(batch, hull, x, y, HIT_WHITE, 1);
        } else if (shieldShimmer > 0) {
            flash.draw(batch, hull, x, y, SHIELD_BLUE, shieldShimmer * SHIELD_SHIMMER);
        } else {
            batch.draw(hull, x - hull.getRegionWidth() / 2f, y - hull.getRegionHeight() / 2f);
        }
    }

    private void drawBolts(SpriteBatch batch, Sortie sortie, float alpha) {
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
        for (int i = 0; i < sortie.shotCount(); i++) {
            Shot shot = sortie.shot(i);
            drawCentred(batch, sprites.pulseBolt, shot.renderX(alpha), shot.renderY(alpha));
        }
        Ship ship = sortie.ship();
        int frame = ship.ticksSinceShot() / MUZZLE_FRAME_TICKS;
        if (sortie.flying() && frame < sprites.pulseMuzzle.size) {
            drawCentred(
                    batch,
                    sprites.pulseMuzzle.get(frame),
                    ship.renderX(alpha),
                    ship.renderY(alpha) + ship.spec().muzzleOffsetY());
        }
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
    }

    private void drawBullets(SpriteBatch batch, Sortie sortie, float alpha) {
        for (int i = 0; i < sortie.bulletCount(); i++) {
            EnemyBullet bullet = sortie.bullet(i);
            drawCentred(batch, sprites.orb, bullet.renderX(alpha), bullet.renderY(alpha));
        }
    }

    /** A flashing arrow at each warned edge (design/ui/hud, in the play field). */
    private void drawWarnings(SpriteBatch batch, Sortie sortie) {
        int edges = sortie.edgeWarnings();
        if (edges == 0 || sortie.tick() / WARNING_FLASH_TICKS % 2 == 1) {
            return;
        }
        float middle = PlayField.HEIGHT / 2f;
        for (int row = 0; row < 8; row++) {
            float length = 8 - row;
            if (WarningEdge.LEFT.in(edges)) {
                fill(batch, WARNING, X0 + 4 + row, middle - length * 2, 1, length * 4);
            }
            if (WarningEdge.RIGHT.in(edges)) {
                fill(batch, WARNING, X0 + PlayField.WIDTH - 5 - row, middle - length * 2, 1, length * 4);
            }
            if (WarningEdge.BOTTOM.in(edges)) {
                fill(batch, WARNING, X0 + PlayField.WIDTH / 2f - length * 2, 4 + row, length * 4, 1);
            }
        }
    }

    private void fill(SpriteBatch batch, Color colour, float x, float y, float width, float height) {
        batch.setColor(colour);
        batch.draw(sprites.pixel, x, y, width, height);
        batch.setColor(Color.WHITE);
    }

    private static void drawCentred(SpriteBatch batch, TextureRegion region, double x, double y) {
        batch.draw(
                region,
                Math.round(X0 + x - region.getRegionWidth() / 2.0),
                Math.round(y - region.getRegionHeight() / 2.0));
    }
}
