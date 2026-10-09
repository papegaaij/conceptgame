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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import vanguard.content.LevelData;
import vanguard.sim.AirstrikeBomb;
import vanguard.sim.BossSpec;
import vanguard.sim.Chain;
import vanguard.sim.Crane;
import vanguard.sim.Debris;
import vanguard.sim.Enemy;
import vanguard.sim.EnemyBullet;
import vanguard.sim.EnemySpec;
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
import vanguard.sim.SpecialSlot;
import vanguard.sim.WeaponSpec;
import vanguard.sim.Wingman;

/**
 * Draws a level back to front, interpolating every position between the last two simulation
 * steps: the backdrop down to the ground layer, the ground objects and the convoy, the overhead
 * ground pieces (a bridge's arches, a gate's roof: the convoy passes under them), the debris and
 * the ground units (a turret stands on an arch) with their glints, the flyers' drop shadows on the
 * ground layer (its tiles, road and pieces mark where they may fall, see {@link Shadows}),
 * the low-air flyers, the low-air layer's banks (so a low flyer can sit inside them), a hull
 * boss's shadow while it flies above the play plane, a lifeboat tow (friendly, under the flyers), the flyers
 * and a set piece on the play plane, a set piece breaking up at its death, the solid death pieces of air units (tatters, husks), the debris chunks, the cranes, the pickups, the solid rounds
 * (missiles, bombs, shells), Rook's craft beside it (or his drifting eject pod, {@link WingmanLooks}), the ship with its engine flames, damage smoke and sparks, wing pods and
 * shield ring ({@link ShipLooks}), the glowing shots, muzzle flashes and
 * effects, a set piece on high-air (above the ship, at the high-air scale), the units a boss off the
 * play plane has launched (they leave its sacs downward, so they show over its hull), the high-air layer,
 * then the spore mines and the enemy bullets (small or large orbs, {@link BulletLooks}) above every layer (design/enemies, bullet readability
 * rules), an act boss's death flash, the edge warnings and the credit numbers.
 *
 * <p>M5 part E, a level over water: between the deep layer and the ground's tiles the {@code sub}
 * layer through its {@link WaterLooks} pass (the Kraken's parts under the water or its foreshadowing,
 * the sunken triggers, the naval units' plain bodies, the torpedoes and the under-water effects);
 * over the ground's pieces the arena boss ({@link KrakenLooks}: its lanes' churn, its grip, head and
 * arms); the naval units' surface layers ({@link NavalLooks}) with the ground units; the surface's
 * water effects (bursts, splashes, ripple trains) with the debris; the lanes' marks and splashes over
 * the convoy; the frigate's flak puffs on low-air.
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
    /** M5 part E: a foam collar's flicker loop and a wake's stream show a frame this many steps. */
    private static final int COLLAR_FRAME_TICKS = 8;
    /** M5 part E: a freed sunken pod rises a frame every 6 steps; its first four frames are under the water. */
    private static final int RISE_FRAME_TICKS = 6;

    private static final int RISE_UNDER_FRAMES = 4;
    /** The stuck sled's ore canister (Level 05): beacon dark, beacon lit, clamp shot. */
    private static final String ORE_CANISTER = "ore-canister";
    /** The survey cache's markers twinkle at 10 fps while lit. */
    private static final int GLINT_TWINKLE_TICKS = 6;
    /** A thrown rock's tumble frames per shape, and the steps each shows. */
    private static final int ROCK_TUMBLE = 4;

    private static final int ROCK_TUMBLE_TICKS = 6;
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
    /** M5 part D: a cloaked unit without its shimmer set is drawn as its body at this opacity, additive. */
    private static final float PLACEHOLDER_CLOAK = 0.35f;
    /** The vital part's glow pulses between 55 % and full every 1.2 s (the review loop's). */
    private static final double GLOW_PERIOD_SECONDS = 1.2;

    private static final Color HIT_WHITE = Color.WHITE;
    /** M5 part E: the strength a torpedo's lit back is added over the chop with ({@link #drawTorpedoBacks}). */
    static final float TORPEDO_BACK = 0.9f;
    /** The white flashes' strength with the Gameplay tab's flash reduction on. */
    private static final float REDUCED_FLASH = 0.35f;

    private static final Color SHIELD_BLUE = Color.valueOf("00C0FF");
    private static final float SHIELD_SHIMMER = 0.6f;
    /** A bomb is drawn shrinking to this scale as it falls (design/player/weapons/bomb-rack). */
    private static final float BOMB_LANDING_SCALE = 0.6f;
    /** The bomber sprite's hull centre is this far above the sprite's centre, px. */
    private static final int BOMBER_HULL_DY = 4;
    /** The bomber's shadow on the ground, offset from the bomber (the concept's light from the upper left). */
    private static final int BOMBER_SHADOW_DX = 34;

    private static final int BOMBER_SHADOW_DY = -48;
    /** The bomber's engine flicker: each of its frames shows this many steps. */
    private static final int BOMBER_FRAME_TICKS = 2;
    /**
     * A shell grows by this much at the top of its arc (design/player/weapons/hammer-mortar: 1.0 -> 1.4
     * -> 1.0); a shorter lob (Rook's aimed Mortar) by its share of the full range.
     */
    private static final float SHELL_ARC_SCALE = 0.4f;
    /** Bolts with a range fade out over its last part (design/player/weapons/scatter-vulcan). */
    private static final double FADE_SHARE = 0.25;

    private final Sprites sprites;
    /** The Airstrike's bomb: the Bomb Rack's (design/player/specials, Audio / VFX). */
    private final AtlasRegion airstrikeBomb;
    /** The Airstrike's CDF bomber, facing up, with its engine flicker, and its shadow. */
    private final Array<AtlasRegion> airstrikeBomber;

    private final AtlasRegion airstrikeShadow;

    private final EnemyLooks[] looks;
    private final WeaponLooks weapons;
    private final CraneLooks craneLooks;
    /** Per set piece of the level, its sprites. */
    /** Per set piece its sprites; null for a boss, which {@link #bossLooks} draws. */
    private final SetPieceLooks[] setPieceLooks;

    private final BossLooks bossLooks;
    /** The units the level's boss launches from its windows (the Brood Carrier's sacs); empty without. */
    private final List<EnemySpec> launches;
    /**
     * The destructible ground objects' frames (intact, damaged, wrecked) and the frames of the
     * triggers with a look of their own (Level 06's survey cache: closed, hit, opened; its terminal:
     * intact, released) by their look, looked up once.
     */
    private final Map<String, Array<AtlasRegion>> groundLooks = new HashMap<>();
    /** The looks of the triggers drawn with frames of their own instead of the beacon or trigger light. */
    private final Set<String> triggerLooks = new HashSet<>();
    /** M5 part E: a ground object's foam collar ({@code <look>-collar}, the floating containers), by its look. */
    private final Map<String, Array<AtlasRegion>> groundCollars = new HashMap<>();
    /** M5 part E: a sunken trigger's rise once freed ({@code <look>-rise}), by its look. */
    private final Map<String, Array<AtlasRegion>> groundRises = new HashMap<>();
    /**
     * A trigger look's {@code -glow} frames (the terminal's LEDs; the billboard's neon flicker, a loop
     * by {@link GroundGlow}), drawn after the light pass until it is spent.
     */
    private final Map<String, Array<AtlasRegion>> groundGlows = new HashMap<>();
    /**
     * A trigger look's {@code -glint} frames (the survey cache's markers), drawn after the light
     * pass only while the headlight or a flare lights it, until it is spent.
     */
    private final Map<String, Array<AtlasRegion>> groundGlints = new HashMap<>();
    /** The debris chunks' sprites by name, looked up once. */
    private final Map<String, AtlasRegion> debrisSprites = new HashMap<>();
    /**
     * The light a level's triggers are drawn as when its backdrop has one (Level 03's lifeboat
     * rack: a lit lamp over the wreck's lens, off once shot); otherwise the crane beacon.
     */
    private final AtlasRegion triggerLight;

    private final Array<AtlasRegion> mine;
    /** Level 05's sleds, lobs and battery outlines. */
    private final LunaLooks luna;
    /** The thrown rocks' looks: three shapes of {@value #ROCK_TUMBLE} tumble frames each; null without them. */
    private final Array<AtlasRegion> rocks;

    private final Backdrop backdrop;
    /** M5 part C: the backdrop's level and real clocks at the render time. */
    private final BackdropClock clock = new BackdropClock();
    /** M5 part C: Level 09's collapse (the lean, the drop, its shadow, dust, dust peak and heap); null without one. */
    private final CollapseLooks collapse;
    /** The level's road and convoy, if it has them. */
    private final ConvoyLooks convoy;
    /** M5 part D: an air escort's units (Level 10's shuttles), if the level has one. */
    private final ShuttleLooks shuttles;
    /** M5 part D: a scripted loss's glow and lance (Level 10), if the level has one. */
    private final LossLooks loss;

    private final FlashShader flash;
    /** Level 06's darkness, glows, sweeps and the Smart Bomb's flash and ring. */
    private final FarsideLooks farside;

    /** Level 07's lifeboat tow. */
    private final TowLooks tows;
    /** The ship's engine flames, damage smoke and sparks and its shield ring. */
    private final ShipLooks shipLooks;
    /** Rook's craft, its flames, smoke, muzzle flash and eject pod (M5 part A). */
    private final WingmanLooks wingmanLooks;
    /** The flyers' drop shadows on the ground layer. */
    private final Shadows shadows;
    /** The enemy bullets' sprites by their class. */
    private final BulletLooks bullets;

    private final BitmapFont font;
    private float whiteFlash = 1;
    /** The Targeting computer's HP bars and weak-point brackets; null without one fitted. */
    private TargetingOverlay targeting;
    /** The Salvage scanner's glint on the secrets' objects; null without one fitted. */
    private SecretGlints secretGlints;
    /** M5 part E: the {@code sub} pass of a level over water; null over land. */
    private final WaterLooks water;
    /** M5 part E: the arena boss (the Harbour Kraken); null without one. */
    private final KrakenLooks kraken;
    /**
     * M5 part E: water effects on the surface (bursts of kills and blasts on the water, splashes,
     * ripple trains), alpha-blended, scrolling with the sea, under the ground units.
     */
    private final Effects surfaceWater = Effects.solid();
    /** M5 part E: effects under the water, drawn in the {@code sub} pass: under-water bursts. */
    private final Effects underWater = Effects.solid();
    /** M5 part E: the torpedoes' bubble trails, over the chop and under the convoy (bubbles rise to the surface). */
    private final Effects bubbles = Effects.solid();
    /** M5 part E: solid effects on low-air (the frigate's flak puffs over the convoy). */
    private final Effects lowAir = Effects.solid();
    /** M5 part E: each Driftjelly's quickening pulse. */
    private final PulseClock pulses = new PulseClock();
    /** M5 part E: the last enemy volleys (x, y, step), for a gun's recoil frame. */
    private final double[] firedX = new double[FIRED];

    private final double[] firedY = new double[FIRED];
    private final long[] firedTick = new long[FIRED];
    private int fired;
    private static final int FIRED = 16;
    private static final float[] NO_OFFSET = new float[2];
    /** A volley belongs to the raft within this distance of where it left, px. */
    private static final double FIRED_REACH = 24;

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
        airstrikeBomb = sprites.region("bomb-rack-shot");
        airstrikeBomber = sprites.frames("airstrike-bomber");
        airstrikeShadow = sprites.region("airstrike-bomber-shadow");
        this.looks = looks;
        this.weapons = weapons;
        this.craneLooks = new CraneLooks(sprites, level.cranes().isPresent() ? pivots(files, "crane-four") : null);
        setPieceLooks = script.setPieces().stream()
                .map(spec -> spec.isBoss() ? null : new SetPieceLooks(sprites, spec, pivots(files, spec.slug())))
                .toArray(SetPieceLooks[]::new);
        LevelScript.SetPieceSpec boss = script.setPieces().stream()
                .filter(LevelScript.SetPieceSpec::isBoss)
                .findFirst()
                .orElse(null);
        JsonValue bossPivots = boss == null ? null : pivots(files, boss.slug());
        bossLooks = new BossLooks(sprites, flash, boss, bossPivots);
        kraken = KrakenLooks.of(sprites, flash, boss, bossPivots);
        launches =
                boss == null ? List.of() : boss.boss().map(BossSpec::spawnKinds).orElse(List.of());
        String light = Backdrop.folder(levelKey) + "lifeboat-light";
        triggerLight = sprites.hasBackdrop(light) ? sprites.backdrop(light, 1).first() : null;
        mine = sprites.has("spore-mine") ? sprites.frames("spore-mine") : null;
        for (LevelScript.GroundObjectSpec spec : script.groundObjects()) {
            String look = spec.look();
            if (sprites.has(look + "-collar")) {
                groundCollars.computeIfAbsent(look, name -> sprites.frames(name + "-collar"));
            }
            if (spec.submerged() && sprites.has(look + "-rise")) {
                groundRises.computeIfAbsent(look, name -> sprites.frames(name + "-rise"));
            }
            if (!spec.trigger()) {
                groundLooks.computeIfAbsent(look, sprites::frames);
            } else if (!look.equals(LevelScript.GroundObjectSpec.CARGO_CONTAINER) && sprites.has(look)) {
                triggerLooks.add(look);
                groundLooks.computeIfAbsent(look, sprites::frames);
                if (sprites.has(look + "-glow")) {
                    groundGlows.computeIfAbsent(look, name -> sprites.frames(name + "-glow"));
                }
                if (sprites.has(look + "-glint")) {
                    groundGlints.computeIfAbsent(look, name -> sprites.frames(name + "-glint"));
                }
            }
        }
        String sledRun = Backdrop.folder(levelKey, level) + "sled-run";
        luna = new LunaLooks(
                sprites,
                script.sled().isPresent() && sprites.hasBackdrop(sledRun) ? sprites.backdrop(sledRun, 30) : null,
                flash);
        rocks = sprites.has("rock") ? sprites.frames("rock") : null;
        this.backdrop = new Backdrop(sprites, level, levelKey);
        this.collapse = level.collapse()
                .map(spec -> new CollapseLooks(sprites, spec, backdrop, Backdrop.folder(levelKey, level)))
                .orElse(null);
        this.convoy = new ConvoyLooks(sprites, flash, level, script, levelKey, name -> pivots(files, name));
        this.shuttles = new ShuttleLooks(
                sprites,
                flash,
                script,
                script.escort()
                        .map(escort -> pivots(files, escort.ally().slug()))
                        .orElse(null));
        this.loss = new LossLooks(sprites, script);
        this.flash = flash;
        this.farside = new FarsideLooks(sprites, script);
        this.tows = new TowLooks(sprites);
        this.shipLooks = new ShipLooks(sprites, files);
        this.wingmanLooks = new WingmanLooks(sprites, files, flash);
        this.shadows = new Shadows();
        this.bullets = new BulletLooks(sprites);
        this.font = font;
        this.water = script.water() ? new WaterLooks() : null;
        java.util.Arrays.fill(firedTick, Long.MIN_VALUE);
    }

    /**
     * The fitted utility modules' marks: the Targeting computer's (over the units, under the
     * bullets) and the Salvage scanner's glint (over the ground objects, cranes and tows, in the
     * dark as well); null for a module that is not fitted.
     */
    public void modules(TargetingOverlay targetingOverlay, SecretGlints glints) {
        targeting = targetingOverlay;
        secretGlints = glints;
    }

    /** Frees the light map and the generated textures. */
    public void dispose() {
        farside.dispose();
        shadows.dispose();
        if (water != null) {
            water.dispose();
        }
        if (bossLooks.hull != null) {
            bossLooks.hull.dispose();
        }
    }

    /** The level restarts: the boss's parts forget their opening animations. */
    public void restart() {
        clock.reset();
        shipLooks.reset();
        shuttles.reset();
        wingmanLooks.reset();
        if (bossLooks.hull != null) {
            bossLooks.hull.reset();
        }
        if (kraken != null) {
            kraken.reset();
        }
        pulses.reset();
        convoy.reset();
        surfaceWater.clear();
        underWater.clear();
        bubbles.clear();
        lowAir.clear();
        java.util.Arrays.fill(firedTick, Long.MIN_VALUE);
    }

    /** M5 part E: the renderer's own effects advance one simulation step (with the screen's). */
    public void stepEffects() {
        surfaceWater.step();
        underWater.step();
        bubbles.step();
        lowAir.step();
    }

    /** M5 part E: whether the level is over water: its kills and blasts burst on the water ({@link #surfaceWater()}). */
    public boolean overWater() {
        return water != null;
    }

    /** M5 part E: the surface's water effects (bursts, splashes, ripple trains), started on the ground. */
    public Effects surfaceWater() {
        return surfaceWater;
    }

    /** M5 part E: the effects under the water (under-water bursts), through the {@code sub} pass. */
    public Effects underWater() {
        return underWater;
    }

    /** M5 part E: the torpedoes' bubble trails, on the surface over the chop (not through the {@code sub} pass). */
    public Effects bubbles() {
        return bubbles;
    }

    /** M5 part E: solid effects on low-air (the frigate's flak puffs). */
    public Effects lowAir() {
        return lowAir;
    }

    /** M5 part E: the escort frigate (convoy unit {@code k}) fired a flak burst at step {@code tick}: its bow gun flashes. */
    public void flak(int k, long tick) {
        convoy.flak(k, tick);
    }

    /** M5 part E: an enemy fired a volley from (x, y) at step {@code tick} (a gun's recoil frame). */
    public void enemyFired(double x, double y, long tick) {
        firedX[fired] = x;
        firedY[fired] = y;
        firedTick[fired] = tick;
        fired = (fired + 1) % FIRED;
    }

    /** Whether a volley left within reach of (x, y) in the last {@code ticks} steps before {@code tick}. */
    private boolean firedNear(double x, double y, long tick, int ticks) {
        for (int i = 0; i < FIRED; i++) {
            if (firedTick[i] != Long.MIN_VALUE
                    && tick - firedTick[i] < ticks
                    && Math.abs(firedX[i] - x) < FIRED_REACH
                    && Math.abs(firedY[i] - y) < FIRED_REACH) {
                return true;
            }
        }
        return false;
    }

    /**
     * M5 part E: where a slam arm's segments lie, px from the boss's centre ({@link KrakenLooks#armPoints}),
     * for its severed arm's pops; empty without the Kraken's looks.
     */
    public static double[] armPoints(SetPiece piece, int part) {
        return piece.arena().isPresent() ? KrakenLooks.armPoints(piece, part) : new double[0];
    }

    /** M5 part E: the Kraken's head's offset from its centre, px (y up); 0 without the Kraken's looks. */
    public double krakenHeadDy() {
        return kraken == null ? 0 : kraken.headDy();
    }

    /** Rook was hit at step {@code tick}: his hull flashes white. */
    public void wingmanHit(long tick) {
        wingmanLooks.hit(tick);
    }

    /** A unit's pivot file in assets/pivots/, or null when it has none. */
    private static JsonValue pivots(Files files, String name) {
        FileHandle file = files.internal("pivots/" + name + ".json");
        return file.exists() ? new JsonReader().parse(file) : null;
    }

    /**
     * @param effects glowing animations over the flyers: explosions, impacts, death glows (those of
     *     ground units on the ground, see {@link Effects#startOnGround})
     * @param debris solid animations on the ground: wrecks, debris, husks
     * @param pieces solid death pieces over the flyers (those of ground units on the ground)
     * @param blasts glowing animations on the ground below the flyers: the Airstrike's blasts
     * @param wrecks the set pieces whose death is playing, drawn breaking up
     * @param alpha interpolation between the previous and the current step
     * @param shieldShimmer 0..1, how strongly the ship shows its last shield hit
     * @param flashReduction tone the white hit and invulnerability flashes down (Gameplay tab)
     * @param screenFlash an act boss's death flash over the play field
     */
    public void draw(
            SpriteBatch batch,
            Sortie sortie,
            Effects effects,
            Effects debris,
            Effects pieces,
            Effects blasts,
            SetPieceWrecks wrecks,
            CreditNumbers credits,
            EdgeWarnings warnings,
            float alpha,
            float shieldShimmer,
            boolean flashReduction,
            ScreenFlash screenFlash) {
        whiteFlash = flashReduction ? REDUCED_FLASH : 1;
        double lag = SimStep.SECONDS * (1 - alpha);
        double scroll = sortie.groundScroll() - sortie.groundSpeed() * lag;
        // M5 part C: between two steps script time moves at the level clock's rate (a fifth in a hold).
        double seconds = sortie.levelSeconds() - lag * sortie.scriptRate();
        clock.record(sortie.levelSeconds(), sortie.realSeconds());
        clock.at(seconds, Math.max(0, sortie.realSeconds() - lag));
        if (collapse != null) {
            collapse.update(sortie, scroll, lag);
        }
        backdrop.drawBehind(batch, scroll, clock);
        if (water != null) {
            // M5 part E: the sub layer between the deep swell and the surface's chop (E4 = c).
            water.begin(batch);
            drawSub(batch, sortie, alpha, seconds, wrecks, scroll);
            water.end(batch, sortie.realSeconds() - lag);
        }
        // The ground layer marks where the flyers' shadows may fall (not on open space or the far layer).
        shadows.clear(batch);
        shadows.beginGround(batch);
        backdrop.drawGroundTiles(batch, scroll, clock);
        convoy.drawRoad(batch, Math.round(scroll));
        // With Level 09's collapse's shadow, heap and base dust under the towers (CollapseLooks).
        backdrop.drawGroundPieces(batch, scroll, clock);
        shadows.endGround(batch);
        // M5 part E: over the chop and under the convoy: the Kraken's foreshadowing shadow (before it
        // scrolls in) and the torpedoes' lit backs and bubbles just under the surface.
        if (kraken != null && !krakenShown(sortie, wrecks)) {
            kraken.drawForeshadow(batch, seconds, sortie.tick());
        }
        drawTorpedoBacks(batch, sortie, alpha);
        bubbles.draw(batch, scroll);
        // M5 part E: the Kraken's churn on the lanes, then its grip, head and arms on the platform's sea.
        drawKraken(batch, sortie, wrecks, alpha, scroll, true);
        luna.drawSled(batch, sortie, scroll, alpha);
        luna.drawMarkers(batch, sortie, alpha);
        drawGround(batch, sortie, alpha);
        convoy.drawConvoy(batch, sortie, alpha, whiteFlash);
        // M5 part D: shuttles on their pads and low in their liftoff, lost ones gliding into far.
        shuttles.drawLow(batch, sortie, alpha, whiteFlash);
        shadows.beginGround(batch);
        backdrop.drawOverhead(batch, scroll, clock);
        shadows.endGround(batch);
        drawCreep(batch, sortie, alpha);
        debris.draw(batch, scroll);
        surfaceWater.draw(batch, scroll);
        boolean overHull = bossOffPlane(sortie);
        drawEnemies(batch, sortie, alpha, Depth.GROUND, Launched.ANY);
        drawGlints(batch, sortie, alpha);
        // M5 part E: the telegraphed lanes' marks and the slams' splashes over the convoy and the rafts.
        drawKraken(batch, sortie, wrecks, alpha, scroll, false);
        farside.darken(batch, sortie, alpha, scroll, seconds);
        farside.drawGlows(batch, sortie, looks, alpha, seconds);
        drawGroundGlows(batch, sortie, alpha);
        drawBomberShadows(batch, sortie, alpha);
        drawShadows(batch, sortie, alpha);
        drawEnemies(batch, sortie, alpha, Depth.LEAP, Launched.ANY);
        drawEnemies(batch, sortie, alpha, Depth.LOW_AIR, Launched.ANY);
        blasts.draw(batch, scroll);
        lowAir.draw(batch, scroll);
        if (collapse != null) {
            collapse.drawOver(batch);
        }
        backdrop.drawLowAir(batch, scroll, clock);
        drawBossShadows(batch, sortie, alpha);
        drawUnitGlows(batch, sortie, alpha);
        // M5 part D: the scripted loss's glow under the air layer, then the shuttles under the flyers.
        loss.drawGlow(batch, sortie, alpha, seconds);
        tows.draw(batch, sortie, alpha, seconds);
        shuttles.drawAir(batch, sortie, alpha, whiteFlash);
        drawEnemies(batch, sortie, alpha, Depth.AIR, overHull ? Launched.NOT : Launched.ANY);
        drawChains(batch, sortie, alpha);
        farside.drawSweeps(batch, sortie, alpha);
        luna.drawBlobs(batch, sortie, alpha);
        drawAirstrike(batch, sortie, alpha);
        drawSetPieces(batch, sortie, alpha, seconds, false);
        drawWrecks(batch, sortie, wrecks, alpha, seconds);
        pieces.draw(batch, scroll);
        drawDebris(batch, sortie, alpha);
        drawCranes(batch, sortie, alpha);
        if (secretGlints != null) {
            secretGlints.draw(batch, sortie, alpha);
        }
        drawPickups(batch, sortie, alpha);
        drawShots(batch, sortie, alpha, false);
        Wingman rook = sortie.wingman().orElse(null);
        if (rook != null) {
            wingmanLooks.draw(batch, rook, sortie.tick(), alpha, whiteFlash);
        }
        if (sortie.flying()) {
            drawShip(batch, sortie, alpha, shieldShimmer);
        }
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
        drawShots(batch, sortie, alpha, true);
        if (sortie.flying()) {
            drawMuzzles(batch, sortie, alpha, true);
        }
        if (rook != null) {
            wingmanLooks.drawMuzzle(batch, rook, weapons, sortie.wingmanMount(), alpha, true);
        }
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        if (sortie.flying()) {
            drawMuzzles(batch, sortie, alpha, false);
        }
        if (rook != null) {
            wingmanLooks.drawMuzzle(batch, rook, weapons, sortie.wingmanMount(), alpha, false);
        }
        effects.draw(batch, scroll);
        // M5 part D: the lance above the air layer.
        loss.drawLance(batch, sortie, alpha);
        if (rook != null) {
            // His eject pod over the explosion once it has popped out of it.
            wingmanLooks.drawOver(batch, rook, sortie.tick(), alpha);
        }
        farside.drawSmartBomb(batch, sortie, alpha, whiteFlash);
        drawSetPieces(batch, sortie, alpha, seconds, true);
        if (overHull) {
            drawEnemies(batch, sortie, alpha, Depth.AIR, Launched.ONLY);
        }
        // M5 part D: the cloaked units' shimmer on high-air, above the ship.
        drawEnemies(batch, sortie, alpha, Depth.HIGH_AIR, Launched.ANY);
        backdrop.drawFront(batch, scroll, clock);
        if (targeting != null) {
            targeting.draw(batch, sortie, alpha);
        }
        drawMines(batch, sortie, alpha);
        drawBullets(batch, sortie, alpha);
        screenFlash.draw(batch, sprites.pixel, alpha, whiteFlash);
        warnings.draw(batch, sortie.tick(), alpha);
        credits.draw(batch, font);
        drawBossBar(batch, sortie);
    }

    /** A hull boss's shadow on the play plane while it flies above it (design/art-direction, high-air rule). */
    private void drawBossShadows(SpriteBatch batch, Sortie sortie, float alpha) {
        for (int k = 0; k < sortie.setPieceCount(); k++) {
            SetPiece piece = sortie.setPiece(k);
            if (piece.boss().isPresent() && piece.present() && piece.arena().isEmpty()) {
                bossLooks.drawShadow(batch, piece, alpha);
            }
        }
    }

    /**
     * The steps of a boss's chained death (design/enemies/bosses): an act boss's from tail to head
     * over its chain length, a mid-boss's {@value BossPose#LEGACY_CHAIN_STEP_TICKS} steps a part.
     */
    public static int chainTicks(SetPiece piece) {
        double seconds = BossPose.chainSeconds(piece);
        return Double.isNaN(seconds) ? piece.partCount() * BossPose.LEGACY_CHAIN_STEP_TICKS : SimStep.ticks(seconds);
    }

    /** Whether a boss's death is the act boss's chain from tail to head (else the frigate's per-part bursts). */
    public static boolean tailToHead(SetPiece piece) {
        return !Double.isNaN(BossPose.chainSeconds(piece));
    }

    /** Whether a boss's death flashes the screen: an act boss's (mid-bosses have none). */
    public static boolean flashesAtDeath(SetPiece piece) {
        return BossPose.actBoss(piece);
    }

    /**
     * An act boss's chained death from tail to head in the pose it died in (its hull's length and
     * width from its size), the bursts' offsets at the scale it was drawn at.
     */
    public static List<DeathChain.Burst> deathChain(SetPiece piece) {
        double angle = BossPose.angle(piece, BossPose.turn(piece, 1));
        double[] xs = new double[piece.partCount()];
        double[] ys = new double[piece.partCount()];
        for (int p = 0; p < xs.length; p++) {
            xs[p] = piece.partOffsetX(p);
            ys[p] = piece.partOffsetY(p);
        }
        return DeathChain.plan(
                xs, ys, angle, piece.spec().size().height(), piece.spec().size().width(), chainTicks(piece));
    }

    /** The head end of a boss's hull in the pose it died in, px from its centre (x right, y up). */
    public static double[] deathHead(SetPiece piece) {
        double angle = BossPose.angle(piece, BossPose.turn(piece, 1));
        double length = piece.spec().size().height();
        return new double[] {DeathChain.headX(angle, length), DeathChain.headY(angle, length)};
    }

    /** The boss bar at the top of the play field while a boss is on the screen (design/ui/hud). */
    private void drawBossBar(SpriteBatch batch, Sortie sortie) {
        for (int k = 0; k < sortie.setPieceCount(); k++) {
            SetPiece piece = sortie.setPiece(k);
            if (piece.boss().isPresent() && piece.present()) {
                bossLooks.drawBar(batch, font, piece);
            }
        }
    }

    /**
     * The loot targets, intact or damaged, a hit flashing white; the beacon blinks until spent, a
     * trigger light blinks until it is shot and is off then.
     */
    private void drawGround(SpriteBatch batch, Sortie sortie, float alpha) {
        for (int i = 0; i < sortie.groundObjectCount(); i++) {
            GroundObject object = sortie.groundObject(i);
            if (object.spec().submerged()) {
                // M5 part E: a sunken trigger lies on sub (drawSub); freed, its rise breaks the surface.
                drawRising(batch, object, alpha, false);
                continue;
            }
            boolean ownLook = object.spec().trigger()
                    && triggerLooks.contains(object.spec().look());
            if (object.spec().trigger() && !ownLook && triggerLight != null && !sledClamp(sortie, object)) {
                drawTriggerLight(batch, sortie, object, alpha);
                continue;
            }
            int damaged = object.damaged() ? 1 : 0;
            TextureRegion frame;
            if (ownLook) {
                // Its own frames: hit after the first hit (with three frames), the last once spent.
                Array<AtlasRegion> frames = groundLooks.get(object.spec().look());
                int state = object.spent() ? frames.size - 1 : frames.size > 2 ? damaged : 0;
                frame = frames.get(state);
            } else if (object.spec().trigger() && sledClamp(sortie, object)) {
                // The stuck sled (Level 05): the ore canister, its clamp's beacon lit while it can be
                // hit (the rail dark), the clamp shot open once spent.
                int state = object.spent() ? 2 : !object.shut() ? 1 : 0;
                frame = groundLooks
                        .computeIfAbsent(ORE_CANISTER, sprites::frames)
                        .get(state);
            } else if (object.spec().trigger()) {
                int lit = !object.spent() && sortie.tick() / BEACON_BLINK_TICKS % 2 == 0 ? 1 : 0;
                frame = sprites.beacon.get(2 * damaged + lit);
            } else {
                frame = groundLooks.get(object.spec().look()).get(damaged);
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
            Array<AtlasRegion> collar = groundCollars.get(object.spec().look());
            if (collar != null && !object.spent()) {
                // M5 part E: an object afloat (the floating containers) in its foam collar.
                int phase = (int) object.renderX() % collar.size;
                drawCentred(
                        batch,
                        collar.get((int) ((sortie.tick() / COLLAR_FRAME_TICKS + phase) % collar.size)),
                        object.renderX(),
                        object.renderY(alpha));
            }
        }
    }

    /** Whether the trigger is the clamp of the level's stuck sled. */
    private static boolean sledClamp(Sortie sortie, GroundObject object) {
        return sortie.sled()
                .map(sled -> sled.spec().clampSecret().equals(object.spec().secret()))
                .orElse(false);
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

    /**
     * Each loot target sparkles at its top-left quarter (the key light's side) every ~2 s; not a
     * dark one (Level 06's survey cache), which only its markers' glint shows, while lit.
     */
    private void drawGlints(SpriteBatch batch, Sortie sortie, float alpha) {
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
        for (int i = 0; i < sortie.groundObjectCount(); i++) {
            GroundObject object = sortie.groundObject(i);
            if (object.spec().dark()) {
                continue;
            }
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

    /**
     * After the light pass, at full brightness: the triggers' glow frames (the data core terminal's
     * LEDs, the billboard's flicker looping by {@link GroundGlow}) until they are spent, and their
     * markers' glint (the survey cache's) only while the headlight or a flare lights them, so the
     * secret stays dark until found.
     */
    private void drawGroundGlows(SpriteBatch batch, Sortie sortie, float alpha) {
        if (groundGlows.isEmpty() && groundGlints.isEmpty()) {
            return;
        }
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
        for (int i = 0; i < sortie.groundObjectCount(); i++) {
            GroundObject object = sortie.groundObject(i);
            if (object.spent()) {
                continue;
            }
            Array<AtlasRegion> glow = groundGlows.get(object.spec().look());
            if (glow != null) {
                int frame = GroundGlow.frame(
                        sortie.tick(), glow.size, groundLooks.get(object.spec().look()).size, object.damaged());
                drawCentred(batch, glow.get(frame), object.renderX(), object.renderY(alpha));
            }
            Array<AtlasRegion> glint = groundGlints.get(object.spec().look());
            if (glint != null && sortie.lit(object)) {
                int frame = (int) (sortie.tick() / GLINT_TWINKLE_TICKS % glint.size);
                drawCentred(batch, glint.get(frame), object.renderX(), object.renderY(alpha));
            }
        }
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
    }

    /** Where an enemy is drawn in the stack. */
    enum Depth {
        /** The ground units, with the ground objects. */
        GROUND,
        /**
         * M5 part C: a pouncer leaping off the ground (the Ravager), over its pack and the flyers'
         * shadows, under the low flyers; in its air window it is on {@link #AIR}.
         */
        LEAP,
        /** The low flyers, below the low-air banks. */
        LOW_AIR,
        /** The flyers on the play plane. */
        AIR,
        /**
         * M5 part D: a cloaked unit on high-air (the Wraith before its decloak), drawn as its shimmer
         * above the ship and the effects, under the high-air layer and the bullets.
         */
        HIGH_AIR;

        /** Its depth by the layer it is on now ({@link Enemy#layer()}: a pounce's air window is air). */
        static Depth of(Enemy enemy) {
            if (enemy.cloaked()) {
                return HIGH_AIR;
            }
            Layer layer = enemy.layer();
            if (enemy.leaping()) {
                return layer == Layer.AIR ? AIR : LEAP;
            }
            if (enemy.grounded() || enemy.walking()) {
                return GROUND;
            }
            return layer == Layer.LOW_AIR ? LOW_AIR : AIR;
        }
    }

    /** Which of a depth's enemies a pass draws: all, all but a boss's launched units, or only those. */
    private enum Launched {
        ANY,
        NOT,
        ONLY
    }

    /**
     * Whether a boss is off the play plane (on high-air, descending or rising): its launched units
     * are then drawn over its hull, which is drawn over the ship.
     */
    private boolean bossOffPlane(Sortie sortie) {
        if (launches.isEmpty()) {
            return false;
        }
        for (int k = 0; k < sortie.setPieceCount(); k++) {
            SetPiece piece = sortie.setPiece(k);
            if (piece.boss().isPresent() && piece.present() && !piece.onPlane()) {
                return true;
            }
        }
        return false;
    }

    /** Whether an enemy is of a kind the boss launches (the same spec its windows hatch it from). */
    private boolean launched(Enemy enemy) {
        for (EnemySpec kind : launches) {
            if (enemy.spec() == kind) {
                return true;
            }
        }
        return false;
    }

    /** The enemies at one depth ({@code which} of them); a diver flares in its pause. */
    private void drawEnemies(SpriteBatch batch, Sortie sortie, float alpha, Depth depth, Launched which) {
        for (int i = 0; i < sortie.enemyCount(); i++) {
            Enemy enemy = sortie.enemy(i);
            if (Depth.of(enemy) != depth
                    || enemy.chain() != null
                    || (which != Launched.ANY && launched(enemy) != (which == Launched.ONLY))) {
                continue;
            }
            EnemyLooks look = looks[enemy.kind()];
            AtlasRegion frame = enemyFrame(sortie, enemy, i, alpha);
            if (depth == Depth.HIGH_AIR) {
                drawCloaked(batch, sortie, enemy, look, i, alpha, 1);
                continue;
            }
            if (LunaLooks.target(sortie, enemy)) {
                luna.drawOutline(batch, frame, enemy.renderX(alpha), enemy.renderY(alpha));
            }
            double decloak = enemy.decloak(alpha);
            if (decloak < 1) {
                drawDecloak(batch, sortie, enemy, look, frame, i, alpha, decloak);
                continue;
            }
            if (depth == Depth.GROUND && look.naval().twoLayers()) {
                // M5 part E: its surface layer over the body the sub pass drew.
                drawNaval(batch, sortie, enemy, look, alpha);
                continue;
            }
            drawCentred(batch, frame, enemy.renderX(alpha), enemy.renderY(alpha));
            if (depth == Depth.AIR && enemy.leaping()) {
                // In its air window the leap's glow goes with the body, over the low-air layer.
                drawLeapGlow(batch, enemy, look, alpha);
            }
            if (enemy.paused() && !look.flare().isEmpty()) {
                batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
                AtlasRegion flare = look.flare().get((int) (sortie.tick() / FLARE_FRAME_TICKS % look.flare().size));
                drawCentred(batch, flare, enemy.renderX(alpha), enemy.renderY(alpha));
                batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
            }
        }
    }

    /**
     * M5 part D: a cloaked unit's shimmer (the Wraith on high-air; tools/art/wraith.py), additive at
     * {@code opacity}, already at the high-air scale, a hit flashing it once more; without a shimmer
     * set its body frame stands in, faint and at the high-air scale.
     */
    private void drawCloaked(
            SpriteBatch batch, Sortie sortie, Enemy enemy, EnemyLooks look, int i, float alpha, float opacity) {
        if (opacity <= 0) {
            return;
        }
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
        double x = enemy.renderX(alpha);
        double y = enemy.renderY(alpha);
        long step = look.step(sortie.tick(), i);
        if (look.cloaks()) {
            AtlasRegion shimmer = look.cloakFrame(enemy.facing(), step);
            batch.setColor(1, 1, 1, opacity);
            drawCentred(batch, shimmer, x, y);
            if (enemy.ticksSinceHit() < HIT_FLASH_TICKS) {
                batch.setColor(1, 1, 1, opacity * whiteFlash);
                drawCentred(batch, shimmer, x, y);
            }
        } else {
            batch.setColor(1, 1, 1, PLACEHOLDER_CLOAK * opacity);
            drawScaled(batch, look.frame(enemy.facing(), step), x, y, HIGH_AIR_SCALE);
        }
        batch.setColor(Color.WHITE);
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
    }

    /**
     * M5 part D: a unit decloaking on air (its 0.4 s flash, tools/art/wraith.py): the body fading in
     * over the flash, the shimmer fading out over its first half, the violet flash over both.
     */
    private void drawDecloak(
            SpriteBatch batch,
            Sortie sortie,
            Enemy enemy,
            EnemyLooks look,
            AtlasRegion frame,
            int i,
            float alpha,
            double progress) {
        batch.setColor(1, 1, 1, EnemyLooks.bodyOpacity(progress));
        drawCentred(batch, frame, enemy.renderX(alpha), enemy.renderY(alpha));
        batch.setColor(Color.WHITE);
        drawCloaked(batch, sortie, enemy, look, i, alpha, EnemyLooks.shimmerOpacity(progress));
        int flashFrame = look.decloakFrame(progress);
        if (flashFrame >= 0) {
            batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
            drawCentred(batch, look.decloak().get(flashFrame), enemy.renderX(alpha), enemy.renderY(alpha));
            batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        }
    }

    /**
     * Enemy {@code i}'s frame: its heading and animation step, its walk cycle or the Mantis's pose; a
     * periodic spawner's iris state and pulse, a pouncer's leap frame in the air part of its leap.
     */
    private AtlasRegion enemyFrame(Sortie sortie, Enemy enemy, int i, float alpha) {
        EnemyLooks look = looks[enemy.kind()];
        AtlasRegion mantis = farside.mantisFrame(look, enemy, sortie.tick());
        if (mantis != null) {
            return mantis;
        }
        if (look.iris()) {
            return look.frames().get(EnemyLooks.irisFrame(enemy.iris(), sortie.tick(), enemy.serial()));
        }
        int step = leapStep(enemy, look, alpha);
        if (step >= 0) {
            return look.leap().get(look.leapFrame(enemy.facing(), step));
        }
        return enemy.walking()
                ? look.frames().get(look.walkFrame(enemy.facing(), enemy.walked()))
                : look.frame(enemy.facing(), look.step(sortie.tick(), i, enemy.burstSeconds()));
    }

    /**
     * M5 part E: the {@code sub} layer, in the {@link WaterLooks} pass: the Kraken's parts under the
     * water (or, before it scrolls in, its foreshadowing shadow), the sunken triggers and the freed
     * pod's rise, the naval units' plain bodies, the torpedoes and the under-water effects.
     */
    private void drawSub(
            SpriteBatch batch, Sortie sortie, float alpha, double seconds, SetPieceWrecks wrecks, double scroll) {
        long tick = sortie.tick();
        if (kraken != null) {
            for (int k = 0; k < sortie.setPieceCount(); k++) {
                SetPiece piece = sortie.setPiece(k);
                int age = wrecks.active(k) ? wrecks.age(k) : -1;
                if (piece.arena().isEmpty() || !KrakenLooks.shown(piece, age >= 0)) {
                    continue;
                }
                double y = age >= 0 ? kraken.deathY(piece, scroll) : piece.renderY(alpha);
                double x = age >= 0 ? kraken.deathX() : piece.renderX(alpha);
                kraken.drawSub(batch, piece, x, y, tick, age);
            }
        }
        for (int i = 0; i < sortie.groundObjectCount(); i++) {
            GroundObject object = sortie.groundObject(i);
            if (object.spec().submerged()) {
                drawRising(batch, object, alpha, true);
            }
        }
        double shipX = sortie.ship().renderX(alpha);
        double shipY = sortie.ship().renderY(alpha);
        for (int i = 0; i < sortie.enemyCount(); i++) {
            Enemy enemy = sortie.enemy(i);
            EnemyLooks look = looks[enemy.kind()];
            NavalLooks naval = look.naval();
            if (!naval.twoLayers() || Depth.of(enemy) != Depth.GROUND) {
                continue;
            }
            double x = enemy.renderX(alpha);
            double y = enemy.renderY(alpha);
            if (naval.rafts()) {
                drawCentred(batch, naval.raftSub().first(), x, y);
                continue;
            }
            int pulse = pulses.frame(
                    enemy.serial(),
                    tick,
                    NavalLooks.jellyFps(Math.hypot(shipX - x, shipY - y)),
                    NavalLooks.PULSE_FRAMES);
            boolean swapping = enemy.swap(alpha) >= 0;
            if (pulses.contracted()
                    && !swapping
                    && !enemy.submerged()
                    && !naval.ripple().isEmpty()) {
                // A surfaced bell's contraction leaves its ripple train on the sea.
                surfaceWater.startOnGround(
                        naval.ripple(), NavalLooks.RIPPLE_FRAME_TICKS, x, y, 0, sortie.groundScroll());
            }
            drawCentred(batch, naval.sub().get(swapping ? 0 : pulse % naval.sub().size), x, y);
        }
        for (int i = 0; i < sortie.shotCount(); i++) {
            Shot shot = sortie.shot(i);
            if (!runsUnder(shot)) {
                continue;
            }
            double left = shot.rangeLeft();
            batch.setColor(1, 1, 1, left < FADE_SHARE ? (float) (left / FADE_SHARE) : 1);
            drawCentred(batch, weapons.sprite(shot), shot.renderX(alpha), shot.renderY(alpha));
            batch.setColor(Color.WHITE);
        }
        underWater.draw(batch, scroll);
    }

    /** Whether the arena boss (the Kraken) is drawn: scrolling in, present or dying. */
    private static boolean krakenShown(Sortie sortie, SetPieceWrecks wrecks) {
        for (int k = 0; k < sortie.setPieceCount(); k++) {
            SetPiece piece = sortie.setPiece(k);
            if (piece.arena().isPresent() && KrakenLooks.shown(piece, wrecks.active(k))) {
                return true;
            }
        }
        return false;
    }

    /**
     * M5 part E: a torpedo's lit back over the chop (its body is drawn in the {@code sub} pass, where
     * the water's tint takes it to the sea's own colour): the sprite added at {@value #TORPEDO_BACK}
     * strength, so it reads as a pale streak running just under the surface; fading with its range.
     */
    private void drawTorpedoBacks(SpriteBatch batch, Sortie sortie, float alpha) {
        boolean any = false;
        for (int i = 0; i < sortie.shotCount(); i++) {
            Shot shot = sortie.shot(i);
            if (!runsUnder(shot)) {
                continue;
            }
            if (!any) {
                batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
                any = true;
            }
            double left = shot.rangeLeft();
            batch.setColor(1, 1, 1, TORPEDO_BACK * (left < FADE_SHARE ? (float) (left / FADE_SHARE) : 1));
            drawCentred(batch, weapons.sprite(shot), shot.renderX(alpha), shot.renderY(alpha));
        }
        if (any) {
            batch.setColor(Color.WHITE);
            batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        }
    }

    /** Whether a shot runs under the water and is drawn in the {@code sub} pass (a torpedo over water). */
    private boolean runsUnder(Shot shot) {
        return water != null && shot.weapon().delivery() == WeaponSpec.Delivery.TORPEDO;
    }

    /**
     * M5 part E: a sunken trigger (the CDF supply pod snagged on a reef root): under the water
     * ({@code under}) its snagged, hit or freed frame and, once freed, the first steps of its rise;
     * on the surface the rise's last steps breaking it (the crate is the pickup's then).
     */
    private void drawRising(SpriteBatch batch, GroundObject object, float alpha, boolean under) {
        Array<AtlasRegion> frames = groundLooks.get(object.spec().look());
        double x = object.renderX();
        double y = object.renderY(alpha);
        if (under && frames != null) {
            int state = object.spent() ? frames.size - 1 : frames.size > 2 && object.damaged() ? 1 : 0;
            drawCentred(batch, frames.get(state), x, y);
        }
        Array<AtlasRegion> rise = groundRises.get(object.spec().look());
        if (rise == null || !object.spent()) {
            return;
        }
        int step = object.ticksSinceHit() / RISE_FRAME_TICKS;
        if (step < rise.size && (step < RISE_UNDER_FRAMES) == under) {
            drawCentred(batch, rise.get(step), x, y);
        }
    }

    /**
     * M5 part E: a naval unit's surface layer (tools/art/driftjelly.py, reef_spitter.py): a jelly's
     * surfacing step while it swaps, its dome with its collar while surfaced (a hit flashing it), its
     * pulse's bloom through the water while submerged (additive); a raft's bob frame with its gun at
     * the frame's gun point (the recoil frame just after a volley).
     */
    private void drawNaval(SpriteBatch batch, Sortie sortie, Enemy enemy, EnemyLooks look, float alpha) {
        NavalLooks naval = look.naval();
        double x = enemy.renderX(alpha);
        double y = enemy.renderY(alpha);
        long tick = sortie.tick();
        if (naval.rafts()) {
            int bob = NavalLooks.raftFrame(tick, enemy.serial(), naval.raft().size);
            drawCentred(batch, naval.raft().get(bob), x, y);
            float[] gun = naval.raftGun().length > bob ? naval.raftGun()[bob] : NO_OFFSET;
            boolean recoil = !naval.recoil().isEmpty() && firedNear(x, y, tick, NavalLooks.RECOIL_TICKS);
            Array<AtlasRegion> set = recoil ? naval.recoil() : look.frames();
            AtlasRegion frame =
                    set.get(EnemyLooks.heading(enemy.facing(), look.headings()) * (set.size / look.headings()));
            drawUnit(batch, enemy, frame, x + gun[0], y + gun[1]);
            return;
        }
        double share = enemy.swap(alpha);
        if (share >= 0 && !naval.surface().isEmpty()) {
            drawCentred(
                    batch,
                    naval.surface().get(NavalLooks.swapFrame(share, enemy.diving(), naval.surface().size)),
                    x,
                    y);
            return;
        }
        int pulse = pulses.frame(
                enemy.serial(),
                tick,
                NavalLooks.jellyFps(Math.hypot(
                        sortie.ship().renderX(alpha) - x, sortie.ship().renderY(alpha) - y)),
                NavalLooks.PULSE_FRAMES);
        if (enemy.submerged()) {
            if (!naval.pulse().isEmpty()) {
                batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
                drawCentred(batch, naval.pulse().get(pulse % naval.pulse().size), x, y);
                batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
            }
            return;
        }
        drawUnit(batch, enemy, look.frames().get(pulse % look.frames().size), x, y);
    }

    /** A unit's frame at (x, y), flashing white just after a hit. */
    private void drawUnit(SpriteBatch batch, Enemy enemy, AtlasRegion frame, double x, double y) {
        if (enemy.ticksSinceHit() < HIT_FLASH_TICKS) {
            flash.draw(batch, frame, Math.round(X0 + x), Math.round(y), HIT_WHITE, whiteFlash);
        } else {
            drawCentred(batch, frame, x, y);
        }
    }

    /**
     * M5 part E: the arena boss (the Harbour Kraken, {@link KrakenLooks}) on the surface: first
     * ({@code first}) its lanes' churn and its grip, head and arms; second, over the convoy and the
     * ground units, its lanes' marks and splashes. Scrolling in, alive, or dying where it died.
     */
    private void drawKraken(
            SpriteBatch batch, Sortie sortie, SetPieceWrecks wrecks, float alpha, double scroll, boolean first) {
        if (kraken == null) {
            return;
        }
        long tick = sortie.tick();
        for (int k = 0; k < sortie.setPieceCount(); k++) {
            SetPiece piece = sortie.setPiece(k);
            int age = wrecks.active(k) ? wrecks.age(k) : -1;
            if (piece.arena().isEmpty() || !KrakenLooks.shown(piece, age >= 0)) {
                continue;
            }
            if (!first) {
                if (age < 0) {
                    kraken.drawLaneMarks(batch, piece, tick);
                }
                continue;
            }
            double y = age >= 0 ? kraken.deathY(piece, scroll) : piece.renderY(alpha);
            double x = age >= 0 ? kraken.deathX() : piece.renderX(alpha);
            if (age < 0) {
                kraken.update(piece, tick);
                kraken.drawLanes(batch, piece, tick);
            }
            kraken.drawSurface(batch, piece, x, y, tick, age, whiteFlash);
        }
    }

    /**
     * A pouncer's leap step at the render time ({@link EnemyLooks#leapStep}): -1 when it is not
     * leaping, has no leap frames, or is low enough to be drawn galloping.
     */
    private static int leapStep(Enemy enemy, EnemyLooks look, float alpha) {
        if (!enemy.leaping() || !look.leaps()) {
            return -1;
        }
        return EnemyLooks.leapStep(EnemyLooks.leapLift(enemy.leapProgress(alpha)));
    }

    /** A leaping pouncer's glow (its maw and eyes), additive over its leap frame. */
    private void drawLeapGlow(SpriteBatch batch, Enemy enemy, EnemyLooks look, float alpha) {
        int step = leapStep(enemy, look, alpha);
        if (step < 0 || look.leapGlow().isEmpty()) {
            return;
        }
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
        drawCentred(
                batch,
                look.leapGlow().get(look.leapFrame(enemy.facing(), step)),
                enemy.renderX(alpha),
                enemy.renderY(alpha));
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
    }

    /**
     * A pouncer's shadow in its leap (tools/art/ravager.py, the art direction's Shadows rule): its leap
     * frame's silhouette at its ground size, sliding down-right with the lift up to the air layer's
     * offset at the apex; none while it is drawn galloping.
     */
    private static void drawLeapShadow(SpriteBatch batch, Enemy enemy, EnemyLooks look, float alpha) {
        double lift = EnemyLooks.leapLift(enemy.leapProgress(alpha));
        int step = EnemyLooks.leapStep(lift);
        if (step < 0) {
            return;
        }
        Shadows.draw(
                batch,
                look.leap().get(look.leapFrame(enemy.facing(), step)),
                (float) (X0 + enemy.renderX(alpha)),
                (float) enemy.renderY(alpha),
                (int) Math.round(EnemyLooks.LEAP_SHADOW_DX * lift),
                (int) Math.round(EnemyLooks.LEAP_SHADOW_DY * lift),
                (float) (1 / (Shadows.SCALE * EnemyLooks.LEAP_SCALES[step])));
    }

    /**
     * Under each live periodic spawner (the Hive Node) its biomass creep, on the ground over the
     * ground objects (its wither after the death is an effect on the ground, started by the screen).
     */
    private void drawCreep(SpriteBatch batch, Sortie sortie, float alpha) {
        for (int i = 0; i < sortie.enemyCount(); i++) {
            Enemy enemy = sortie.enemy(i);
            EnemyLooks look = looks[enemy.kind()];
            if (!look.creep().isEmpty()) {
                drawCentred(batch, look.creep().first(), enemy.renderX(alpha), enemy.renderY(alpha));
            }
        }
    }

    /**
     * The flyers' drop shadows on the ground layer, from their frames' alpha (design/art-direction,
     * Shadows): the low flyers' at the low-air offset, the play plane's flyers' (the units, the
     * chains' segments and the ship) at the air offset, a leaping pouncer's sliding out with its lift;
     * under the low flyers and the low-air layer.
     */
    private void drawShadows(SpriteBatch batch, Sortie sortie, float alpha) {
        shadows.begin(batch);
        for (int i = 0; i < sortie.enemyCount(); i++) {
            Enemy enemy = sortie.enemy(i);
            Depth depth = Depth.of(enemy);
            // M5 part D: a cloaked unit casts no shadow; a decloaking one's fades in with its body.
            if (depth == Depth.GROUND || depth == Depth.HIGH_AIR || enemy.chain() != null) {
                continue;
            }
            float shade = EnemyLooks.bodyOpacity(enemy.decloak(alpha));
            if (shade < 1) {
                batch.setColor(0, 0, 0, Shadows.OPACITY * shade);
            }
            if (enemy.leaping()) {
                EnemyLooks look = looks[enemy.kind()];
                if (look.leaps()) {
                    drawLeapShadow(batch, enemy, look, alpha);
                }
                continue;
            }
            boolean low = depth == Depth.LOW_AIR;
            Shadows.draw(
                    batch,
                    enemyFrame(sortie, enemy, i, alpha),
                    (float) (X0 + enemy.renderX(alpha)),
                    (float) enemy.renderY(alpha),
                    low ? Shadows.LOW_AIR_DX : Shadows.AIR_DX,
                    low ? Shadows.LOW_AIR_DY : Shadows.AIR_DY,
                    1);
            if (shade < 1) {
                batch.setColor(0, 0, 0, Shadows.OPACITY);
            }
        }
        for (int c = 0; c < sortie.chainCount(); c++) {
            Chain chain = sortie.chain(c);
            for (int k = chain.size() - 1; k >= 0; k--) {
                Enemy member = chain.member(k);
                if (member != null) {
                    drawChainMember(batch, sortie, chain, member, k, alpha, true);
                }
            }
        }
        if (sortie.flying()) {
            Ship craft = sortie.ship();
            Shadows.draw(
                    batch,
                    sprites.ship.get(craft.bank() + ShipSpec.HARD_BANK),
                    Math.round(X0 + craft.renderX(alpha)),
                    Math.round(craft.renderY(alpha)),
                    Shadows.AIR_DX,
                    Shadows.AIR_DY,
                    1);
        }
        Wingman rook = sortie.wingman().orElse(null);
        if (rook != null) {
            wingmanLooks.drawShadow(batch, rook, alpha);
        }
        shuttles.drawShadows(batch, sortie, alpha);
        shadows.end(batch);
    }

    /**
     * The segment chains (design/enemies/air/coilwyrm), each from its tail to its head so the head
     * lies on top, the segments drawn at their tapering sizes; a cut part's head grows at the cut.
     */
    private void drawChains(SpriteBatch batch, Sortie sortie, float alpha) {
        for (int c = 0; c < sortie.chainCount(); c++) {
            Chain chain = sortie.chain(c);
            for (int k = chain.size() - 1; k >= 0; k--) {
                Enemy member = chain.member(k);
                if (member != null) {
                    drawChainMember(batch, sortie, chain, member, k, alpha, false);
                }
            }
            if (chain.regrowing() && chain.alive()) {
                EnemyLooks look = looks[chain.regrownKind()];
                AtlasRegion frame = look.frame(chain.headFacing(), look.step(sortie.tick(), 0));
                drawScaled(batch, frame, chain.headX(), chain.headY(), (float) Math.max(0.2, chain.regrowth()));
            }
        }
    }

    /** A chain's member {@code k} at its tapering size, or its shadow. */
    private void drawChainMember(
            SpriteBatch batch, Sortie sortie, Chain chain, Enemy member, int k, float alpha, boolean shadow) {
        double first = chain.spec().segmentBoxes().getFirst().width();
        EnemyLooks look = looks[member.kind()];
        boolean segment = k > 0
                && k < chain.spec().members() - 1
                && member.link() == k
                && member.spec() == chain.spec().segment();
        // The production segments are one set per size of the taper (the frames' phases):
        // the nearest size, unscaled; the placeholder is scaled to the member's size.
        int sizes = look.frames().size / look.headings();
        boolean sized = segment && sizes > 1;
        long step = sized ? sizeIndex(chain, member, sizes) : look.step(sortie.tick(), k);
        AtlasRegion frame = look.frame(member.facing(), step);
        float scale = segment && !sized ? (float) (member.hitbox().width() / first) : 1;
        if (shadow) {
            Shadows.draw(
                    batch,
                    frame,
                    (float) (X0 + member.renderX(alpha)),
                    (float) member.renderY(alpha),
                    Shadows.AIR_DX,
                    Shadows.AIR_DY,
                    scale);
        } else {
            drawScaled(batch, frame, member.renderX(alpha), member.renderY(alpha), scale);
        }
    }

    /**
     * A segment's size in a production set of {@code sizes} sizes tapering from the chain's first
     * segment to its last (tools/art/coilwyrm.py: size 0 the largest): the nearest to its hit box,
     * so a hard chain's 14 segments take the 12 sizes' nearest.
     */
    private static int sizeIndex(Chain chain, Enemy member, int sizes) {
        double first = chain.spec().segmentBoxes().getFirst().width();
        double last = chain.spec().segmentBoxes().getLast().width();
        if (first <= last) {
            return 0;
        }
        long index = Math.round((first - member.hitbox().width()) / (first - last) * (sizes - 1));
        return Math.clamp(index, 0, sizes - 1);
    }

    /**
     * The walkers' emissive backs, additive above the low-air layer, so the Scuttler's lime back
     * glows through the dust that hides its body (design/campaign, Level 04 hazards); a leaping
     * pouncer's maw and eyes off the ground (in its air window they are drawn with its body); a
     * periodic spawner's throat and polyps at its iris state (the Hive Node: the iris glows while it
     * opens, the spawn's telegraph).
     */
    private void drawUnitGlows(SpriteBatch batch, Sortie sortie, float alpha) {
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
        for (int i = 0; i < sortie.enemyCount(); i++) {
            Enemy enemy = sortie.enemy(i);
            EnemyLooks look = looks[enemy.kind()];
            if (look.glow().isEmpty()) {
                continue;
            }
            if (look.iris()) {
                int frame = EnemyLooks.irisFrame(enemy.iris(), sortie.tick(), enemy.serial());
                drawCentred(batch, look.glow().get(frame), enemy.renderX(alpha), enemy.renderY(alpha));
            } else if (enemy.walking()) {
                if (leapStep(enemy, look, alpha) >= 0) {
                    if (Depth.of(enemy) == Depth.LEAP) {
                        drawLeapGlow(batch, enemy, look, alpha);
                        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
                    }
                    continue;
                }
                drawCentred(
                        batch,
                        look.glow().get(look.walkFrame(enemy.facing(), enemy.walked())),
                        enemy.renderX(alpha),
                        enemy.renderY(alpha));
            }
        }
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
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
            if (!piece.present() || piece.onPlane() == high || piece.arena().isPresent()) {
                // M5 part E: an arena boss lies on the water ({@link #drawKraken}).
                continue;
            }
            if (piece.boss().isPresent()) {
                Ship ship = sortie.ship();
                bossLooks.draw(batch, piece, alpha, seconds, whiteFlash, ship.renderX(alpha), ship.renderY(alpha));
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
            SetPieceDeath death = death(k);
            SetPieceLooks look = setPieceLooks[k];
            boolean boss = look == null;
            if (!wrecks.active(k)) {
                continue;
            }
            if (death == null) {
                if (boss && bossLooks.hull != null) {
                    drawCarcass(batch, sortie.setPiece(k), wrecks, k, alpha);
                }
                continue;
            }
            float age = wrecks.age(k) + alpha;
            float x = wrecks.x(k);
            float y = wrecks.y(k);
            float scale = wrecks.scale(k);
            if (age < death.swap) {
                batch.setColor(1, 1, 1, wrecks.opacity(k));
                int sway = SetPieceLooks.sway(seconds);
                if (boss) {
                    bossLooks.drawWreck(batch, sortie.setPiece(k), x, y, scale);
                } else if (wrecks.crossing(k)) {
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

    /** A hull boss's carcass darkens over its chained death to this shade, and drifts after it, px/s. */
    private static final float CARCASS_SHADE = 0.45f;

    private static final double CARCASS_DRIFT_X = 5;
    private static final double CARCASS_DRIFT_Y = -12;

    /**
     * A hull boss without a break-up (its placeholder): the body where it died, darkening under its
     * chained death, then the dark carcass drifting slowly away (the Level 07 aftermath).
     */
    private void drawCarcass(SpriteBatch batch, SetPiece piece, SetPieceWrecks wrecks, int k, float alpha) {
        float age = wrecks.age(k) + alpha;
        int chain = chainTicks(piece);
        float t = Math.min(1, age / Math.max(1, chain));
        float shade = 1 - (1 - CARCASS_SHADE) * t;
        double drift = Math.max(0, age - chain) * SimStep.SECONDS;
        batch.setColor(shade, shade, shade, wrecks.opacity(k));
        bossLooks.drawWreck(
                batch,
                piece,
                wrecks.x(k) + CARCASS_DRIFT_X * drift,
                wrecks.y(k) + CARCASS_DRIFT_Y * drift,
                wrecks.scale(k));
        batch.setColor(Color.WHITE);
    }

    /** Set piece {@code k}'s break-up at its death, or null for none. */
    public SetPieceDeath death(int k) {
        return setPieceLooks[k] != null ? setPieceLooks[k].death : bossLooks.death;
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
            if (chunk.thrown()) {
                drawRock(batch, chunk, alpha);
                continue;
            }
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
     * A thrown rock (Level 05): one of the three shapes (by its serial), tumbling, scaled to the
     * rock's size, growing as it rises in low gravity and fading out in its last half second.
     */
    private void drawRock(SpriteBatch batch, Debris chunk, float alpha) {
        if (rocks == null) {
            return;
        }
        int shape = Math.floorMod(chunk.serial(), rocks.size / ROCK_TUMBLE);
        int tumble = Math.floorMod(chunk.life() / ROCK_TUMBLE_TICKS + chunk.serial(), ROCK_TUMBLE);
        AtlasRegion rock = rocks.get(shape * ROCK_TUMBLE + tumble);
        double size = chunk.spec().size().width();
        double rise = 1 - Math.max(0, chunk.life() - alpha) / (double) SimStep.ticks(2.5);
        float scale = (float) (size / rock.getRegionWidth() * (1 + 0.3 * Math.sin(Math.PI * Math.clamp(rise, 0, 1))));
        float fade = (float) Math.min(1, chunk.life() / (double) SimStep.ticks(0.5));
        batch.setColor(1, 1, 1, fade);
        drawScaled(batch, rock, chunk.renderX(alpha), chunk.renderY(alpha), scale);
        batch.setColor(Color.WHITE);
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
            drawCentred(batch, frames.get(frame), pickup.renderX(alpha), pickup.renderY(alpha));
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
            // The crate stands in until the special charge's own pickup sprite is made.
            case SPECIAL_CHARGE -> sprites.crate;
            // The amber orb of round 23 b (tools/art/l06_props.py).
            case DATA_CORE -> sprites.frames("pickup-data-core");
        };
    }

    /**
     * The Airstrike: its bombs shrinking as they fall to the ground point they burst on (the Bomb
     * Rack's bomb), and the two bombers (their production sprite) above them on their way up.
     */
    private void drawAirstrike(SpriteBatch batch, Sortie sortie, float alpha) {
        SpecialSlot special = sortie.special();
        if (!special.fitted()) {
            return;
        }
        for (int i = 0; i < special.bombCount(); i++) {
            AirstrikeBomb bomb = special.bomb(i);
            float scale = 1 - (1 - BOMB_LANDING_SCALE) * (float) bomb.progress(alpha);
            drawScaled(batch, airstrikeBomb, bomb.renderX(), bomb.renderY(alpha), scale);
        }
        if (!special.bombersIn()) {
            return;
        }
        double y = special.bomberRenderY(alpha) - BOMBER_HULL_DY;
        int frame = (int) (sortie.tick() / BOMBER_FRAME_TICKS % airstrikeBomber.size);
        for (int b = 0; b < SpecialSlot.BOMBERS; b++) {
            drawCentred(batch, airstrikeBomber.get(frame), special.bomberX(b), y);
        }
    }

    /** The bombers' shadows on the ground, below the low-air layer (the 50 % opacity is in the sprite). */
    private void drawBomberShadows(SpriteBatch batch, Sortie sortie, float alpha) {
        SpecialSlot special = sortie.special();
        if (!special.fitted() || !special.bombersIn()) {
            return;
        }
        double y = special.bomberRenderY(alpha) - BOMBER_HULL_DY + BOMBER_SHADOW_DY;
        for (int b = 0; b < SpecialSlot.BOMBERS; b++) {
            drawCentred(batch, airstrikeShadow, special.bomberX(b) + BOMBER_SHADOW_DX, y);
        }
    }

    /**
     * The ship: its smoke trail and engine flames below the hull, the hull (blinking white in its
     * mercy time, shimmering blue after a shield hit) with its wing pods, then its sparks and the
     * shield ring over it.
     */
    private void drawShip(SpriteBatch batch, Sortie sortie, float alpha, float shieldShimmer) {
        Ship craft = sortie.ship();
        TextureRegion hull = sprites.ship.get(craft.bank() + ShipSpec.HARD_BANK);
        float x = Math.round(X0 + craft.renderX(alpha));
        float y = Math.round(craft.renderY(alpha));
        shipLooks.drawBelow(batch, craft, sortie.tick(), alpha, x, y);
        int mercy = craft.defences().mercyTicks();
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
                craft.bank() + ShipSpec.HARD_BANK,
                x - hull.getRegionWidth() / 2f,
                y + hull.getRegionHeight() / 2f);
        shipLooks.drawAbove(batch, sortie.tick(), alpha, x, y, shieldShimmer);
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
            if (weapons.look(shot.mount()).glowingShot != glowing || runsUnder(shot)) {
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
                            1 + SHELL_ARC_SCALE * (float) (shot.arc() * Math.sin(Math.PI * shot.airProgress(alpha))));
                case MINE -> drawCentred(batch, sprite, x, y);
                case BOLT, HOMING, TURRET, TORPEDO -> {
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
            drawCentred(batch, bullets.frame(bullet, i, sortie.tick()), bullet.renderX(alpha), bullet.renderY(alpha));
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
