package vanguard.game.render;

import com.badlogic.gdx.Files;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import java.util.HashMap;
import java.util.Map;
import vanguard.content.LevelData;
import vanguard.sim.Crane;
import vanguard.sim.Debris;
import vanguard.sim.Enemy;
import vanguard.sim.EnemyBullet;
import vanguard.sim.GroundObject;
import vanguard.sim.Layer;
import vanguard.sim.LevelScript;
import vanguard.sim.Mine;
import vanguard.sim.Pickup;
import vanguard.sim.PickupType;
import vanguard.sim.SetPiece;
import vanguard.sim.Ship;
import vanguard.sim.ShipSpec;
import vanguard.sim.Shot;
import vanguard.sim.SimStep;
import vanguard.sim.Sortie;
import vanguard.sim.WeaponSpec;

/**
 * Draws a level back to front, interpolating every position between the last two simulation
 * steps: the backdrop down to the ground layer, the ground objects with their debris and glints,
 * the low-air flyers, the low-air layer's banks (so a low flyer can sit inside them), the flyers
 * and a set piece on the play plane, a set piece breaking up at its death, the solid death pieces of air units (tatters, husks), the debris chunks, the cranes, the pickups, the solid rounds
 * (missiles, bombs, shells), the ship with its wing pods, the glowing shots, muzzle flashes and
 * effects, a set piece on high-air (above the ship, at the high-air scale), the high-air layer,
 * then the spore mines and the enemy bullets above every layer (design/enemies, bullet readability
 * rules), the edge warnings and the credit numbers.
 */
public final class LevelRenderer {
    private static final float X0 = PixelScreen.PLAY_FIELD_X;
    /** The muzzle flash shows each of its three frames for two steps after a shot. */
    private static final int MUZZLE_FRAME_TICKS = 2;
    /** Pickups spin at about 10 fps and blink in their last 1.5 s (chosen pickups concept, round 09). */
    private static final int PICKUP_FRAME_TICKS = 6;
    /** A diver's pause flare pulses at 10 fps; a crane's lights blink at 3 Hz. */
    private static final int FLARE_FRAME_TICKS = 6;

    private static final int BLINK_FRAME_TICKS = 10;
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

    /** A spore mine pulses at 10 fps; while it rises it grows from 70 % and brightens from half. */
    private static final int MINE_FRAME_TICKS = 6;

    private static final float MINE_RISING_SCALE = 0.7f;
    private static final float MINE_RISING_ALPHA = 0.5f;
    /**
     * A set piece on high-air is drawn this much larger than on the play plane (the first pass's
     * sprites are drawn at it); descending and rising it scales between the two.
     */
    private static final float HIGH_AIR_SCALE = 1.25f;
    /**
     * A set piece off the play plane is drawn at 75 % opacity (body, parts and glow alike); descending
     * and rising it eases between that and opaque by its altitude, so it is opaque when it reaches the
     * plane, where it switches below the ship and can collide.
     */
    private static final float HIGH_AIR_OPACITY = 0.75f;
    /** The vital part's glow pulses between 55 % and full every 1.2 s (the review loop's). */
    private static final double GLOW_PERIOD_SECONDS = 1.2;

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
    private final CraneLooks craneLooks;
    /** Per set piece of the level, its sprites. */
    private final SetPieceLooks[] setPieceLooks;
    /** The debris chunks' sprites by name, looked up once. */
    private final Map<String, AtlasRegion> debrisSprites = new HashMap<>();
    /**
     * The light a level's triggers are drawn as when its backdrop has one (Level 03's lifeboat
     * rack: a lit lamp over the wreck's lens, off once shot); otherwise the crane beacon.
     */
    private final AtlasRegion triggerLight;

    private final Array<AtlasRegion> mine;
    private final Backdrop backdrop;
    private final FlashShader flash;
    private final BitmapFont font;
    private float whiteFlash = 1;

    /**
     * @param files the assets, for the pivot files of the cranes and set pieces
     * @param script the level's script, whose set pieces it draws
     * @param levelKey the level's key, {@code <act>/level-NN-<slug>}
     */
    public LevelRenderer(
            Sprites sprites,
            EnemyLooks[] looks,
            WeaponLooks weapons,
            Files files,
            FlashShader flash,
            BitmapFont font,
            LevelData level,
            LevelScript script,
            String levelKey) {
        this.sprites = sprites;
        this.looks = looks;
        this.weapons = weapons;
        this.craneLooks = new CraneLooks(sprites, level.cranes().isPresent() ? pivots(files, "crane-four") : null);
        setPieceLooks = script.setPieces().stream()
                .map(spec -> new SetPieceLooks(sprites, spec, pivots(files, spec.slug())))
                .toArray(SetPieceLooks[]::new);
        String light = Backdrop.folder(levelKey) + "lifeboat-light";
        triggerLight = sprites.hasBackdrop(light) ? sprites.backdrop(light, 1).first() : null;
        mine = sprites.has("spore-mine") ? sprites.frames("spore-mine") : null;
        this.backdrop = new Backdrop(sprites, level, levelKey);
        this.flash = flash;
        this.font = font;
    }

    /** A unit's pivot file in assets/pivots/, or null when it has none. */
    private static JsonValue pivots(Files files, String name) {
        FileHandle file = files.internal("pivots/" + name + ".json");
        return file.exists() ? new JsonReader().parse(file) : null;
    }

    /**
     * @param debris animations started at ground positions (y plus the ground's scroll), see
     *     {@link Effects#draw}
     * @param pieces solid animations at play-field positions: the death pieces of air units
     * @param wrecks the set pieces whose death is playing, drawn breaking up
     * @param alpha interpolation between the previous and the current step
     * @param shieldShimmer 0..1, how strongly the ship shows its last shield hit
     * @param flashReduction tone the white hit and invulnerability flashes down (Gameplay tab)
     */
    public void draw(
            SpriteBatch batch,
            Sortie sortie,
            Effects effects,
            Effects debris,
            Effects pieces,
            SetPieceWrecks wrecks,
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
        drawEnemies(batch, sortie, alpha, Depth.GROUND);
        drawGlints(batch, sortie, alpha);
        drawEnemies(batch, sortie, alpha, Depth.LOW_AIR);
        backdrop.drawLowAir(batch, scroll, seconds);
        drawEnemies(batch, sortie, alpha, Depth.AIR);
        drawSetPieces(batch, sortie, alpha, seconds, false);
        drawWrecks(batch, sortie, wrecks, alpha, seconds);
        pieces.draw(batch, 0);
        drawDebris(batch, sortie, alpha);
        drawCranes(batch, sortie, alpha);
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
        drawSetPieces(batch, sortie, alpha, seconds, true);
        backdrop.drawFront(batch, scroll, seconds);
        drawMines(batch, sortie, alpha);
        drawBullets(batch, sortie, alpha);
        warnings.draw(batch, sortie.tick(), alpha);
        credits.draw(batch, font);
    }

    /**
     * The loot targets, intact or damaged, a hit flashing white; the beacon blinks until spent, a
     * trigger light blinks until it is shot and is off then.
     */
    private void drawGround(SpriteBatch batch, Sortie sortie, float alpha) {
        for (int i = 0; i < sortie.groundObjectCount(); i++) {
            GroundObject object = sortie.groundObject(i);
            if (object.spec().trigger() && triggerLight != null) {
                drawTriggerLight(batch, sortie, object, alpha);
                continue;
            }
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

    /** A trigger light: lit at the beacon's 1 Hz blink until shot, a white flash, then dark (the wreck's lens shows). */
    private void drawTriggerLight(SpriteBatch batch, Sortie sortie, GroundObject object, float alpha) {
        float x = Math.round(X0 + object.renderX());
        float y = Math.round(object.renderY(alpha));
        if (object.ticksSinceHit() < HIT_FLASH_TICKS) {
            flash.draw(batch, triggerLight, x, y, HIT_WHITE, whiteFlash);
        } else if (!object.spent() && sortie.tick() / BEACON_BLINK_TICKS % 2 == 0) {
            batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
            drawCentred(batch, triggerLight, object.renderX(), object.renderY(alpha));
            batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
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

    /** Where an enemy is drawn in the stack. */
    private enum Depth {
        /** The ground units, with the ground objects. */
        GROUND,
        /** The low flyers, below the low-air banks. */
        LOW_AIR,
        /** The flyers on the play plane. */
        AIR;

        static Depth of(Enemy enemy) {
            if (enemy.grounded()) {
                return GROUND;
            }
            return enemy.spec().layer() == Layer.LOW_AIR ? LOW_AIR : AIR;
        }
    }

    /** The enemies at one depth; a diver flares in its pause. */
    private void drawEnemies(SpriteBatch batch, Sortie sortie, float alpha, Depth depth) {
        for (int i = 0; i < sortie.enemyCount(); i++) {
            Enemy enemy = sortie.enemy(i);
            if (Depth.of(enemy) != depth) {
                continue;
            }
            EnemyLooks look = looks[enemy.kind()];
            AtlasRegion frame = look.frame(enemy.facing(), look.step(sortie.tick(), i));
            drawCentred(batch, frame, enemy.renderX(alpha), enemy.renderY(alpha));
            if (enemy.paused() && !look.flare().isEmpty()) {
                batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
                AtlasRegion flare = look.flare().get((int) (sortie.tick() / FLARE_FRAME_TICKS % look.flare().size));
                drawCentred(batch, flare, enemy.renderX(alpha), enemy.renderY(alpha));
                batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
            }
        }
    }

    /**
     * The set pieces on the play plane ({@code high} false) or above it: the first pass's unit as
     * drawn on its heading; the second pass's body in its sway with every part on it, intact or
     * wrecked, a hit part flashing white, and the vital part's glow pulsing until it is destroyed.
     * Off the play plane (arriving, descending, rising) it is drawn above the ship, scaled between
     * the high-air and the play-plane size and faded between 75 % and full opacity by its altitude;
     * it switches below the ship, opaque, when it reaches the plane, where it can collide.
     */
    private void drawSetPieces(SpriteBatch batch, Sortie sortie, float alpha, double seconds, boolean high) {
        for (int k = 0; k < sortie.setPieceCount(); k++) {
            SetPiece piece = sortie.setPiece(k);
            if (!piece.present() || piece.onPlane() == high) {
                continue;
            }
            SetPieceLooks look = setPieceLooks[k];
            double x = piece.renderX(alpha);
            double y = piece.renderY(alpha);
            int sway = SetPieceLooks.sway(seconds);
            float altitude = piece.onPlane() ? 0 : (float) piece.altitude(alpha);
            float opacity = highAirOpacity(altitude);
            batch.setColor(1, 1, 1, opacity);
            if (!piece.spec().passes().get(piece.pass()).descends()) {
                // The crossing pass: one sprite of the whole unit at its heading and the high-air
                // scale. Its parts have no wrecked look there; a part a homing weapon wrecks on
                // this pass stays drawn intact until the second pass.
                drawCentred(batch, look.cross.get(sway % look.cross.size), x, y);
                batch.setColor(Color.WHITE);
                continue;
            }
            float scale = highAirScale(altitude);
            drawBody(batch, piece, look, x, y, sway, scale, true);
            batch.setColor(Color.WHITE);
            if (look.glow != null && !piece.partWrecked(look.glowPart)) {
                double pulse = 0.55 + 0.45 * Math.sin(2 * Math.PI * seconds / GLOW_PERIOD_SECONDS);
                batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
                batch.setColor(1, 1, 1, (float) pulse * opacity);
                drawScaled(
                        batch,
                        look.glow,
                        x + piece.partOffsetX(look.glowPart) * scale,
                        y + piece.partOffsetY(look.glowPart) * scale,
                        scale);
                batch.setColor(Color.WHITE);
                batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
            }
        }
    }

    /** The second pass's body in sway frame {@code sway} with every part on it, intact or wrecked (a hit part flashing). */
    private void drawBody(
            SpriteBatch batch,
            SetPiece piece,
            SetPieceLooks look,
            double x,
            double y,
            int sway,
            float scale,
            boolean hitFlash) {
        drawScaled(batch, look.down.get(sway % look.down.size), x, y, scale);
        for (int p = 0; p < piece.partCount(); p++) {
            if (!look.drawn(p)) {
                continue;
            }
            double dx = look.pivoted(p) ? look.pivot(p, sway)[0] : piece.partOffsetX(p);
            double dy = look.pivoted(p) ? look.pivot(p, sway)[1] : piece.partOffsetY(p);
            AtlasRegion part = look.part(p, sway, piece.partWrecked(p));
            double px = x + dx * scale;
            double py = y + dy * scale;
            if (hitFlash && scale == 1 && piece.partTicksSinceHit(p) < HIT_FLASH_TICKS) {
                flash.draw(batch, part, Math.round(X0 + px), Math.round(py), HIT_WHITE, whiteFlash);
            } else {
                drawScaled(batch, part, px, py, scale);
            }
        }
    }

    /**
     * The set pieces whose death is playing (their {@link SetPieceDeath break-up}): until the swap
     * the body where it died, as it was drawn there (without the glow of its destroyed vital part);
     * then, facing down, the chunks drifting apart from their offsets, sinking (drawn smaller),
     * darkening and fading out. A death on the crossing pass keeps its sprite until the swap and
     * has no chunks. Drawn on the play plane's layer, under the effects, whatever the altitude.
     */
    private void drawWrecks(SpriteBatch batch, Sortie sortie, SetPieceWrecks wrecks, float alpha, double seconds) {
        for (int k = 0; k < sortie.setPieceCount(); k++) {
            SetPieceDeath death = setPieceLooks[k].death;
            if (death == null || !wrecks.active(k)) {
                continue;
            }
            SetPieceLooks look = setPieceLooks[k];
            float age = wrecks.age(k) + alpha;
            float x = wrecks.x(k);
            float y = wrecks.y(k);
            float scale = wrecks.scale(k);
            if (age < death.swap) {
                batch.setColor(1, 1, 1, wrecks.opacity(k));
                int sway = SetPieceLooks.sway(seconds);
                if (wrecks.crossing(k)) {
                    drawCentred(batch, look.cross.get(sway % look.cross.size), x, y);
                } else {
                    drawBody(batch, sortie.setPiece(k), look, x, y, sway, scale, false);
                }
            } else if (!wrecks.crossing(k) && age < death.end) {
                float t = death.progress(age);
                float s = scale * (1 - death.sink * t);
                float e = 1 - (1 - t) * (1 - t);
                float shade = 1 - death.darken * t;
                float fade = t < death.fadeFrom ? 1 : 1 - (t - death.fadeFrom) / (1 - death.fadeFrom);
                batch.setColor(shade, shade, shade, wrecks.opacity(k) * fade);
                for (SetPieceDeath.Chunk chunk : death.chunks) {
                    int f = Math.min(chunk.frames().size - 1, (int) ((age - death.swap) / death.frameSteps));
                    int[] offset = chunk.offsets()[f];
                    drawScaled(
                            batch,
                            chunk.frames().get(f),
                            x + s * (offset[0] + chunk.driftX() * e),
                            y + s * (offset[1] + chunk.driftY() * e),
                            s);
                }
            }
            batch.setColor(Color.WHITE);
        }
    }

    /** Set piece {@code k}'s break-up at its death, or null for none. */
    public SetPieceDeath death(int k) {
        return setPieceLooks[k].death;
    }

    /** A set piece's scale at {@code altitude} (0 on the play plane, 1 on high-air), for it and its effects. */
    public static float highAirScale(double altitude) {
        return (float) (1 + (HIGH_AIR_SCALE - 1) * altitude);
    }

    /** A set piece's opacity at {@code altitude} (0 on the play plane, 1 on high-air), for it and its effects. */
    public static float highAirOpacity(double altitude) {
        return (float) (1 - (1 - HIGH_AIR_OPACITY) * altitude);
    }

    /**
     * The debris chunks on the play plane, unrotated; a hit on a small, breakable chunk flashes it
     * white. A large chunk only shows the glancing spark: it takes no damage, and with the guns on
     * it a white flash per hit would strobe.
     */
    private void drawDebris(SpriteBatch batch, Sortie sortie, float alpha) {
        for (int i = 0; i < sortie.debrisCount(); i++) {
            Debris chunk = sortie.debris(i);
            AtlasRegion sprite = debrisSprites.get(chunk.sprite());
            if (sprite == null) {
                sprite = sprites.region(chunk.sprite());
                debrisSprites.put(chunk.sprite(), sprite);
            }
            if (!chunk.large() && chunk.ticksSinceHit() < HIT_FLASH_TICKS) {
                flash.draw(
                        batch,
                        sprite,
                        Math.round(X0 + chunk.renderX(alpha)),
                        Math.round(chunk.renderY(alpha)),
                        HIT_WHITE,
                        whiteFlash);
            } else {
                drawCentred(batch, sprite, chunk.renderX(alpha), chunk.renderY(alpha));
            }
        }
    }

    /**
     * The spore mines, additive and above the haze like bullets, each pulsing at its own phase:
     * while one rises to the play plane it grows from 70 % and brightens from half, armed it is full.
     */
    private void drawMines(SpriteBatch batch, Sortie sortie, float alpha) {
        if (sortie.mineCount() == 0) {
            return;
        }
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
        for (int i = 0; i < sortie.mineCount(); i++) {
            Mine spore = sortie.mine(i);
            AtlasRegion frame = mine.get((int) ((sortie.tick() / MINE_FRAME_TICKS + i) % mine.size));
            double x = spore.renderX(alpha);
            double y = spore.renderY(alpha);
            if (spore.armed()) {
                drawCentred(batch, frame, x, y);
            } else {
                float rising = (float) spore.rising();
                batch.setColor(1, 1, 1, MINE_RISING_ALPHA + (1 - MINE_RISING_ALPHA) * rising);
                drawScaled(batch, frame, x, y, MINE_RISING_SCALE + (1 - MINE_RISING_SCALE) * rising);
                batch.setColor(Color.WHITE);
            }
        }
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
    }

    /**
     * The cranes' arms at the nearest drawn angle, hanging from their pivots, with the canister on
     * the hook while it holds; the jib's lights blink in the telegraph and the clamp lights while the
     * arm swings.
     */
    private void drawCranes(SpriteBatch batch, Sortie sortie, float alpha) {
        for (int c = 0; c < sortie.craneCount(); c++) {
            Crane crane = sortie.crane(c);
            if (!crane.present()) {
                continue;
            }
            LevelScript.CraneSpec spec = crane.spec();
            double angle = crane.renderAngle(alpha);
            int k = craneLooks.index(angle);
            AtlasRegion arm = craneLooks.arm(k);
            int[] pivot = craneLooks.pivot(k);
            batch.draw(
                    arm,
                    Math.round(X0 + spec.pivotX() - pivot[0]),
                    Math.round(spec.pivotY() + pivot[1] - arm.getRegionHeight()));
            double drawn = craneLooks.angle(k);
            double dx = Math.sin(drawn);
            double dy = -Math.cos(drawn);
            double tipX = spec.pivotX() + dx * spec.length();
            double tipY = spec.pivotY() + dy * spec.length();
            if (crane.holding()) {
                drawCentred(batch, craneLooks.canister, tipX, tipY - craneLooks.canister.getRegionHeight() / 2.0);
            }
            batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
            if (crane.telegraph() && sortie.tick() / BLINK_FRAME_TICKS % 2 == 0) {
                for (int light : craneLooks.lights()) {
                    drawCentred(batch, craneLooks.light, spec.pivotX() + dx * light, spec.pivotY() + dy * light);
                }
            }
            if (crane.swinging() && crane.holding()) {
                drawCentred(batch, craneLooks.clampLight, tipX, tipY - 4);
            }
            batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
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
            case MEDIUM_SALVAGE -> sprites.frames("pickup-salvage-medium");
            case OVERDRIVE -> sprites.frames("pickup-overdrive");
            case LARGE_SALVAGE -> sprites.frames("pickup-salvage-large");
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

    /** The muzzle flashes of the mounts that just fired, at each muzzle of the pattern they fire. */
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
