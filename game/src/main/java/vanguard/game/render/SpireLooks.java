package vanguard.game.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonValue;
import java.util.Arrays;
import vanguard.sim.BossSpec;
import vanguard.sim.EnemySpec;
import vanguard.sim.LevelScript;
import vanguard.sim.PlayField;
import vanguard.sim.SetPiece;
import vanguard.sim.SimStep;

/**
 * M5 part H: the Siege Spire, Act 2's boss rooted in Geneva Concord (design/enemies/bosses/siege-spire;
 * decisions H1, H3, H4, H5 = a; the sprites of tools/art/siege_spire.py and siege_spire_death.py,
 * registered on the anchor, the trunk's foot, by assets/pivots/siege-spire.json; the Level 14 backdrop's
 * canopy-tear and daylight-beams frames of tools/art/backdrop_l14.py). An anchored arena boss without
 * lanes: it scrolls in with the ground and stays where the scroll halts.
 *
 * <p>On the ground ({@link #drawGround}), back to front: the six roots, each alive or withered once its
 * pod is destroyed; the trunk with its claw crown shut, opening as the maw is exposed (phase 2) and open
 * from then on, or from the lift on the stump (the torn socket) in its place; the pods at the root tips,
 * the four turrets turned toward the ship at the nearest of 32 headings, the two mortars opening for a
 * lob, a destroyed pod's husk, a hit pod flashing white; the maw's lime bloom pulsing while it is open
 * and flaring at each launch. Over the ground units ({@link #drawShockwave}): the tear-free shockwave
 * at the lift, a ring of dust running out from the stump and slate shards thrown out. On the play plane
 * ({@link #drawAir}): the free spire tearing loose (its lift frames, gliding from the trunk to its
 * station) and then airborne, turning slowly through its 16 headings, a hit on its core flashing it,
 * the six crimson emitters charging through a sweep's telegraph; at its death its eight death frames
 * over the 3 s chain where it died. Above every layer with the bullets ({@link #drawSweep}): the sweep's
 * telegraph line along its first edge and its beam. Over the high-air layer ({@link #drawDaylight}):
 * from the death on, real seconds, the canopy's veins fading out from 2 to 7 s, its tear frames from 0.5
 * to 8.5 s (2 s each, tiled on the high-air scroll) and the low sun's beams ramping in from 2.5 to 5 s.
 */
final class SpireLooks {
    private static final float X0 = PixelScreen.PLAY_FIELD_X;
    /** The boss's slug: only it is drawn by these looks. */
    static final String SLUG = "siege-spire";
    /** The pods' heading sets: 32 steps clockwise from straight down. */
    static final int POD_HEADINGS = 32;
    /** The crown opens through its half-open frame over this long as the maw is exposed. */
    static final double CROWN_OPEN_SECONDS = 0.5;
    /** A mortar pod shows its open frame this long after a lob, then its opening one as long again. */
    static final int MORTAR_OPEN_TICKS = 10;
    /** A lob belongs to the mortar pod within this distance of where it left, px. */
    private static final double LOB_REACH = 30;
    /** The maw's bloom at a launch: its four frames, this many steps each. */
    static final int MAW_GLOW_FRAME_TICKS = 3;
    /** The maw's bloom while it is open between launches: this strong, pulsing over this period. */
    private static final float MAW_IDLE = 0.35f;

    private static final double MAW_PULSE_SECONDS = 1.2;
    /** The free spire turns this fast through its six-fold symmetry's 16 headings: frames a second. */
    static final double FREE_TURN_FPS = 2;
    /** The tear-free shockwave: its dust ring runs out to this radius over its length, px. */
    static final double SHOCK_RADIUS = 170;

    static final double SHOCK_SECONDS = 1.5;
    /** The shockwave's dust puffs round the ring and the shards thrown out. */
    private static final int SHOCK_PUFFS = 10;

    private static final int SHOCK_SHARDS = 16;
    /** The shards fly out at about this speed, px/s, slowing (drag), fading over their last 0.4 s. */
    private static final double SHARD_SPEED = 230;

    private static final double SHARD_DRAG = 2.5;
    /** The death frames over the boss's death chain: frames 0–3 its first half, 4–7 its second. */
    static final int DEATH_FRAMES = 8;
    /** The daylight's timeline after the death, s (tools/art/backdrop_l14.py's composite): the veins' fade. */
    static final double VEINS_FROM = 2;

    static final double VEINS_TO = 7;
    /** The tear frames' times (each shown until 1 s past its time, from 0.5 s on). */
    static final double[] TEAR_TIMES = {1.5, 3.5, 5.5, 7.5};

    static final double TEAR_FROM = 0.5;
    /** The beams ramp in over these seconds; the second frame from 6.25 s. */
    static final double BEAMS_FROM = 2.5;

    static final double BEAMS_TO = 5;
    static final double BEAMS_SECOND = 6.25;
    /** The canopy's tile period, px (the backdrop's canopy tile set and the tear frames). */
    private static final int CANOPY_PERIOD = 540;

    /**
     * Step H7b (pending the user's review): the stump is drawn blended this far towards {@link #ASH}, a
     * muted grey, desaturated and low in contrast, so the free spire's dark star 76 px above it no
     * longer merges with it (darkening it instead made both one dark mass).
     */
    static final float STUMP_ASH = 0.6f;

    private static final Color ASH = new Color(0.38f, 0.35f, 0.36f, 1);
    private static final Color CRIMSON = Color.valueOf("FF2A3C");
    private static final Color HOT = Color.valueOf("FFE0E4");

    private final FlashShader flash;
    private final TextureRegion pixel;
    private final Array<AtlasRegion> trunk;
    private final Array<AtlasRegion> roots;
    private final Array<AtlasRegion> pods;
    private final Array<AtlasRegion> mortars;
    private final Array<AtlasRegion> husks;
    private final AtlasRegion stump;
    private final Array<AtlasRegion> lift;
    private final Array<AtlasRegion> free;
    private final Array<AtlasRegion> emitter;
    private final Array<AtlasRegion> mawGlow;
    private final Array<AtlasRegion> debris;
    private final Array<AtlasRegion> dust;
    private final Array<AtlasRegion> death;
    /** The Level 14 backdrop's tear and beam frames; empty without them. */
    private final Array<AtlasRegion> tear;

    private final Array<AtlasRegion> beams;
    /** The trunk's centre, px above the anchor (y up). */
    private final double trunkUp;
    /** The maw's centre, px above the anchor (y up). */
    private final double mawUp;
    /** Per root: its sprite's centre from the anchor (x right, y up), its pod's, whether it is a mortar. */
    private final double[] rootX;

    private final double[] rootY;
    private final double[] podX;
    private final double[] podY;
    private final boolean[] mortar;
    /** Per root the boss part on its tip (the pod the data names there); -1 for none. */
    private final int[] rootPart;
    /** The free spire's emitter points per heading, px from the core (x right, y up), six a heading. */
    private final float[][] emitterX;

    private final float[][] emitterY;
    /** The maw part and the core part, by the data's names; -1 for none. */
    private final int maw;

    private final int core;
    /** The boss's death chain, steps. */
    private final int deathTicks;

    private final HitFlash hits = new HitFlash();
    /** Per part the step of its last lob ({@link #lobbed}). */
    private final long[] lobTick;
    /** The step of the last launch from the maw, the step the maw was first seen exposed, the lift's start. */
    private long launchTick = NEVER;

    private long openedTick = NEVER;
    private long liftTick = NEVER;
    /** Per attack the step its current sweep's telegraph was first seen; NEVER when none runs. */
    private long[] telegraphTick = new long[0];
    /** Where it died (screen position, y up), once its death has been seen; NaN before. */
    private double deathX = Double.NaN;

    private double deathY;

    private static final long NEVER = Long.MIN_VALUE / 2;

    private SpireLooks(
            Sprites sprites,
            FlashShader flash,
            LevelScript.SetPieceSpec spec,
            JsonValue pivots,
            Array<AtlasRegion> tear,
            Array<AtlasRegion> beams) {
        this.flash = flash;
        this.pixel = sprites.pixel;
        trunk = sprites.frames(SLUG);
        roots = sprites.frames(SLUG + "-root");
        pods = sprites.frames(SLUG + "-pod");
        mortars = sprites.frames(SLUG + "-mortar");
        husks = sprites.frames(SLUG + "-husk");
        stump = sprites.region(SLUG + "-stump");
        lift = sprites.frames(SLUG + "-lift");
        free = sprites.frames(SLUG + "-free");
        emitter = sprites.frames(SLUG + "-emitter");
        mawGlow = sprites.frames(SLUG + "-maw-glow");
        debris = sprites.frames(SLUG + "-debris");
        death = sprites.frames(SLUG + "-death");
        dust = sprites.has("root-burst-puff") ? sprites.frames("root-burst-puff") : new Array<>(0);
        this.tear = tear;
        this.beams = beams;
        trunkUp = -pivots.get("trunk").get("offset").getFloat(1);
        mawUp = -pivots.get("maw").getFloat(1);
        JsonValue rootList = pivots.get("roots");
        int n = rootList.size;
        rootX = new double[n];
        rootY = new double[n];
        podX = new double[n];
        podY = new double[n];
        mortar = new boolean[n];
        rootPart = new int[n];
        for (int r = 0; r < n; r++) {
            JsonValue root = rootList.get(r);
            rootX[r] = root.get("offset").getFloat(0);
            rootY[r] = -root.get("offset").getFloat(1);
            podX[r] = root.get("pod").getFloat(0);
            podY[r] = -root.get("pod").getFloat(1);
            mortar[r] = root.getString("kind").equals("mortar");
            rootPart[r] = nearestPart(spec.parts(), podX[r], podY[r]);
        }
        JsonValue points = pivots.get("free").get("emitters");
        emitterX = new float[points.size][];
        emitterY = new float[points.size][];
        for (int h = 0; h < points.size; h++) {
            JsonValue heading = points.get(h);
            emitterX[h] = new float[heading.size];
            emitterY[h] = new float[heading.size];
            for (int e = 0; e < heading.size; e++) {
                emitterX[h][e] = heading.get(e).getFloat(0);
                emitterY[h][e] = -heading.get(e).getFloat(1);
            }
        }
        maw = partNamed(spec.parts(), "maw");
        core = vitalPart(spec.parts());
        BossSpec boss = spec.boss().orElseThrow();
        deathTicks = Math.max(1, SimStep.ticks(boss.deathSeconds() > 0 ? boss.deathSeconds() : 3));
        lobTick = new long[spec.parts().size()];
        reset();
    }

    /**
     * The Siege Spire's looks, or null when the level's boss is not the Spire or its sprites or pivots are
     * missing.
     *
     * @param tear the backdrop's canopy-tear frames (empty without them)
     * @param beams the backdrop's daylight-beams frames (empty without them)
     */
    static SpireLooks of(
            Sprites sprites,
            FlashShader flash,
            LevelScript.SetPieceSpec boss,
            JsonValue pivots,
            Array<AtlasRegion> tear,
            Array<AtlasRegion> beams) {
        if (boss == null
                || pivots == null
                || !boss.slug().equals(SLUG)
                || boss.boss().flatMap(BossSpec::arena).isEmpty()
                || !sprites.has(SLUG + "-free")
                || !sprites.has(SLUG + "-death")) {
            return null;
        }
        return new SpireLooks(sprites, flash, boss, pivots, tear, beams);
    }

    /** Whether set piece {@code piece} is the one these looks draw. */
    static boolean drawn(SetPiece piece) {
        return piece.arena().isPresent() && piece.slug().equals(SLUG);
    }

    /** Whether it is drawn now: scrolling in, present, or dying (its death playing, its stump scrolling on). */
    static boolean shown(SetPiece piece, boolean dead) {
        return dead || piece.present() || piece.approaching();
    }

    /** The level restarts (or the fight is retried): the moments the looks time from are forgotten. */
    void reset() {
        hits.reset();
        Arrays.fill(lobTick, NEVER);
        Arrays.fill(telegraphTick, NEVER);
        launchTick = NEVER;
        openedTick = NEVER;
        liftTick = NEVER;
        deathX = Double.NaN;
    }

    /** A mortar lobbed from (x, y) at step {@code tick} (the {@code MORTAR_LOBBED} event): its pod opens. */
    void lobbed(SetPiece piece, double x, double y, long tick) {
        for (int r = 0; r < rootPart.length; r++) {
            int p = rootPart[r];
            if (mortar[r]
                    && p >= 0
                    && Math.abs(piece.partX(p) - x) < LOB_REACH
                    && Math.abs(piece.partY(p) - y) < LOB_REACH) {
                lobTick[p] = tick;
            }
        }
    }

    /** A unit left the maw at step {@code tick} (the {@code BOSS_LAUNCHED} event): its bloom flares. */
    void launched(long tick) {
        launchTick = tick;
    }

    /** Notes the moments the looks time from (once a frame, before drawing). */
    void update(SetPiece piece, long tick) {
        if (maw >= 0 && exposed(piece, maw) && openedTick == NEVER) {
            openedTick = tick;
        }
        if (piece.lifted() && liftTick == NEVER) {
            liftTick = tick;
        }
        BossSpec boss = piece.boss().orElseThrow();
        if (telegraphTick.length != boss.attacks().size()) {
            telegraphTick = new long[boss.attacks().size()];
            Arrays.fill(telegraphTick, NEVER);
        }
        for (int a = 0; a < telegraphTick.length; a++) {
            if (boss.attacks().get(a).sweep().isEmpty() || !piece.sweepTelegraphing(a)) {
                telegraphTick[a] = NEVER;
            } else if (telegraphTick[a] == NEVER) {
                telegraphTick[a] = tick;
            }
        }
    }

    // --- the ground ------------------------------------------------------------------------------

    /**
     * The rooted body on the ground (or, after the lift, the stump with the dead roots and husks),
     * scrolling in, alive, or after its death scrolling away with the ground.
     *
     * @param scrollLag the ground's scroll still to come before the current step ({@code groundScroll} −
     *     the render scroll), added to the stump's place
     */
    void drawGround(
            SpriteBatch batch,
            SetPiece piece,
            float alpha,
            long tick,
            double shipX,
            double shipY,
            double scrollLag,
            float whiteFlash) {
        boolean stumped = piece.lifted();
        double ax = stumped ? piece.stumpX() : piece.renderX(alpha);
        double ay = stumped ? piece.stumpY() + scrollLag : piece.renderY(alpha);
        for (int r = 0; r < rootX.length; r++) {
            boolean dead = rootPart[r] >= 0 && piece.partWrecked(rootPart[r]);
            centred(batch, roots.get(Math.min(roots.size - 1, 2 * r + (dead ? 1 : 0))), ax + rootX[r], ay + rootY[r]);
        }
        if (stumped) {
            // Step H7b: the stump drawn as ash, muted and flat, so the dark free spire above it reads.
            flash.draw(
                    batch,
                    stump,
                    Math.round(X0 + ax - stump.getRegionWidth() / 2.0) + stump.getRegionWidth() / 2f,
                    Math.round(ay - stump.getRegionHeight() / 2.0) + stump.getRegionHeight() / 2f,
                    ASH,
                    STUMP_ASH);
        } else {
            centred(batch, trunk.get(crownFrame(piece, tick)), ax, ay + trunkUp);
        }
        for (int r = 0; r < rootX.length; r++) {
            int p = rootPart[r];
            double px = ax + podX[r];
            double py = ay + podY[r];
            if (p < 0 || piece.partWrecked(p)) {
                centred(batch, husks.get(mortar[r] ? Math.min(1, husks.size - 1) : 0), px, py);
                continue;
            }
            AtlasRegion frame = mortar[r]
                    ? mortars.get(mortarFrame(tick - lobTick[p], mortars.size))
                    : pods.get(podHeading(shipX - px, shipY - py));
            if (!stumped && hits.on(p, tick, piece.partTicksSinceHit(p))) {
                flash.draw(batch, frame, Math.round(X0 + px), Math.round(py), Color.WHITE, whiteFlash);
            } else {
                centred(batch, frame, px, py);
            }
        }
        if (!stumped && maw >= 0 && exposed(piece, maw)) {
            batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
            int flare = mawFlareFrame(tick - launchTick, mawGlow.size);
            double mx = ax;
            double my = ay + mawUp;
            if (flare >= 0) {
                centred(batch, mawGlow.get(flare), mx, my);
            } else {
                float pulse = (float)
                        (MAW_IDLE * (0.7 + 0.3 * Math.sin(2 * Math.PI * tick * SimStep.SECONDS / MAW_PULSE_SECONDS)));
                batch.setColor(pulse, pulse, pulse, 1);
                centred(batch, mawGlow.first(), mx, my);
                batch.setColor(Color.WHITE);
            }
            if (hits.on(maw, tick, piece.partTicksSinceHit(maw))) {
                batch.setColor(whiteFlash, whiteFlash, whiteFlash, 1);
                centred(batch, mawGlow.get(mawGlow.size - 1), mx, my);
                batch.setColor(Color.WHITE);
            }
            batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        }
    }

    /** The crown's frame: shut until the maw is exposed, then half open, then open. */
    private int crownFrame(SetPiece piece, long tick) {
        if (maw < 0 || openedTick == NEVER || !(exposed(piece, maw) || piece.partWrecked(maw))) {
            return 0;
        }
        return crownFrame((tick - openedTick) * SimStep.SECONDS, trunk.size);
    }

    /** The crown's frame {@code seconds} after the maw was exposed: through its middle frames over {@value #CROWN_OPEN_SECONDS} s. */
    static int crownFrame(double seconds, int frames) {
        if (seconds < 0) {
            return 0;
        }
        return Math.clamp(1 + (int) Math.floor(seconds / CROWN_OPEN_SECONDS * (frames - 1)), 1, frames - 1);
    }

    /** A mortar pod's frame {@code since} steps after its last lob: open, then opening, then shut. */
    static int mortarFrame(long since, int frames) {
        if (since < 0 || since >= 2L * MORTAR_OPEN_TICKS) {
            return 0;
        }
        return since < MORTAR_OPEN_TICKS ? frames - 1 : Math.min(1, frames - 1);
    }

    /** The maw's flare frame {@code since} steps after a launch; -1 once it has played. */
    static int mawFlareFrame(long since, int frames) {
        if (since < 0) {
            return -1;
        }
        long frame = since / MAW_GLOW_FRAME_TICKS;
        return frame < frames ? (int) frame : -1;
    }

    /**
     * A turret pod's heading frame toward a target {@code (dx, dy)} away (x right, y up): the nearest of
     * {@value #POD_HEADINGS} steps clockwise from straight down.
     */
    static int podHeading(double dx, double dy) {
        double clockwise = Math.atan2(-dx, -dy);
        long step = Math.round(clockwise / (2 * Math.PI / POD_HEADINGS));
        return (int) Math.floorMod(step, (long) POD_HEADINGS);
    }

    /** The tear-free shockwave over the ground units: the dust ring and the shards, from the lift's start. */
    void drawShockwave(SpriteBatch batch, SetPiece piece, long tick, double scrollLag) {
        if (liftTick == NEVER) {
            return;
        }
        double t = (tick - liftTick) * SimStep.SECONDS;
        if (t < 0 || t > SHOCK_SECONDS) {
            return;
        }
        double x = piece.stumpX();
        double y = piece.stumpY() + scrollLag;
        if (!dust.isEmpty()) {
            int frame = Math.min(dust.size - 1, (int) (t / SHOCK_SECONDS * dust.size));
            double radius = shockRadius(t);
            for (int i = 0; i < SHOCK_PUFFS; i++) {
                double angle = 2 * Math.PI * (i + 0.5) / SHOCK_PUFFS;
                centred(batch, dust.get(frame), x + Math.cos(angle) * radius, y + Math.sin(angle) * radius * 0.8);
            }
        }
        float fade = (float) Math.clamp((SHOCK_SECONDS - t) / 0.4, 0, 1);
        batch.setColor(1, 1, 1, fade);
        for (int i = 0; i < SHOCK_SHARDS; i++) {
            double angle = 2 * Math.PI * i / SHOCK_SHARDS + 0.37 * (i % 3);
            double speed = SHARD_SPEED * (0.7 + 0.15 * (i % 4));
            double reach = 30 + speed * (1 - Math.exp(-SHARD_DRAG * t)) / SHARD_DRAG;
            centred(
                    batch,
                    debris.get((i + (int) (t * 12)) % debris.size),
                    x + Math.cos(angle) * reach,
                    y + Math.sin(angle) * reach);
        }
        batch.setColor(Color.WHITE);
    }

    /** The shockwave's dust ring's radius {@code t} s after the lift's start: out from the trunk, easing. */
    static double shockRadius(double t) {
        double s = Math.clamp(t / SHOCK_SECONDS, 0, 1);
        return 50 + (SHOCK_RADIUS - 50) * (1 - (1 - s) * (1 - s));
    }

    // --- the play plane --------------------------------------------------------------------------

    /**
     * The free spire on the play plane: tearing loose (its lift frames gliding from the trunk to its
     * station), airborne at its heading with its core's hit flash and its emitters through a sweep's
     * telegraph; dying ({@code age} ≥ 0, steps since its death) its death frames where it died.
     */
    void drawAir(SpriteBatch batch, SetPiece piece, float alpha, long tick, int age, float whiteFlash) {
        if (age >= 0) {
            if (Double.isNaN(deathX)) {
                deathX = piece.renderX(1);
                deathY = piece.renderY(1);
            }
            int frame = deathFrame(age, deathTicks, death.size);
            if (frame >= 0) {
                centred(batch, death.get(frame), deathX, deathY);
            }
            return;
        }
        if (!piece.lifted() || !piece.present()) {
            return;
        }
        double share = piece.liftShare(alpha);
        double x = piece.renderX(alpha);
        double y = piece.renderY(alpha);
        if (piece.lifting()) {
            centred(batch, lift.get(liftFrame(share, lift.size)), x, y + (1 - share) * trunkUp);
            return;
        }
        int heading = freeHeading(tick - liftTick, free.size);
        AtlasRegion frame = free.get(heading);
        if (core >= 0 && hits.on(core, tick, piece.partTicksSinceHit(core))) {
            flash.draw(batch, frame, Math.round(X0 + x), Math.round(y), Color.WHITE, whiteFlash);
        } else {
            centred(batch, frame, x, y);
        }
        double charge = telegraphCharge(piece, tick);
        if (charge >= 0) {
            batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
            AtlasRegion glow = emitter.get(Math.clamp((int) (charge * emitter.size), 0, emitter.size - 1));
            int h = Math.min(heading, emitterX.length - 1);
            for (int e = 0; e < emitterX[h].length; e++) {
                centred(batch, glow, x + emitterX[h][e], y + emitterY[h][e]);
            }
            batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        }
    }

    /** The free spire's shadow on the ground while it is airborne: sliding out from under it as it lifts. */
    void drawShadow(SpriteBatch batch, SetPiece piece, float alpha, long tick) {
        if (!piece.present() || !piece.lifted()) {
            return;
        }
        double share = piece.liftShare(alpha);
        double x = piece.renderX(alpha);
        double y = piece.renderY(alpha) + (1 - share) * trunkUp;
        AtlasRegion frame = piece.lifting()
                ? lift.get(liftFrame(share, lift.size))
                : free.get(freeHeading(tick - liftTick, free.size));
        Shadows.draw(
                batch,
                frame,
                (float) (X0 + x),
                (float) y,
                (int) Math.round(Shadows.AIR_DX * share),
                (int) Math.round(Shadows.AIR_DY * share),
                1);
    }

    /** The lift's frame at {@code share} of its glide. */
    static int liftFrame(double share, int frames) {
        return Math.clamp((int) Math.floor(share * frames), 0, frames - 1);
    }

    /** The airborne spire's heading frame {@code since} steps after the lift's start, turning at {@value #FREE_TURN_FPS} frames a second. */
    static int freeHeading(long since, int frames) {
        if (since < 0 || frames == 0) {
            return 0;
        }
        return (int) Math.floorMod((long) Math.floor(since * SimStep.SECONDS * FREE_TURN_FPS), (long) frames);
    }

    /** The death frame {@code age} steps into a death chain of {@code chain} steps; -1 once it is over. */
    static int deathFrame(int age, int chain, int frames) {
        if (age < 0 || age >= chain) {
            return -1;
        }
        return Math.min(frames - 1, age * frames / chain);
    }

    /** How far the running sweep's telegraph is, 0 to 1; -1 when none runs. */
    private double telegraphCharge(SetPiece piece, long tick) {
        BossSpec boss = piece.boss().orElseThrow();
        for (int a = 0; a < telegraphTick.length; a++) {
            if (telegraphTick[a] != NEVER) {
                double seconds = boss.attacks().get(a).sweep().orElseThrow().telegraphSeconds();
                return Math.clamp((tick - telegraphTick[a]) * SimStep.SECONDS / Math.max(1e-6, seconds), 0, 1);
            }
        }
        return -1;
    }

    /**
     * The sweep over every layer, with the bullets: through its telegraph a crimson line along its first
     * edge, pulsing, with its arc's end dotted; then the beam, a crimson band with a hot core and a glow
     * at its origin, sweeping across the arc.
     */
    void drawSweep(SpriteBatch batch, SetPiece piece, float alpha, long tick) {
        if (!piece.present()) {
            return;
        }
        BossSpec boss = piece.boss().orElseThrow();
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
        for (int a = 0; a < piece.attackCount() && a < boss.attacks().size(); a++) {
            EnemySpec.Sweep sweep = boss.attacks().get(a).sweep().orElse(null);
            if (sweep == null || !(piece.sweepTelegraphing(a) || piece.sweepBeaming(a))) {
                continue;
            }
            double x = piece.sweepOriginX(a);
            double y = piece.sweepOriginY(a);
            double length = sweep.length();
            if (piece.sweepTelegraphing(a)) {
                float pulse = 0.55f + 0.35f * (float) Math.abs(Math.sin(tick * 0.4));
                batch.setColor(CRIMSON.r, CRIMSON.g, CRIMSON.b, pulse);
                stroke(batch, x, y, piece.sweepFrom(a), length, 2);
                batch.setColor(CRIMSON.r, CRIMSON.g, CRIMSON.b, pulse * 0.45f);
                stroke(batch, x, y, piece.sweepTo(a), length, 1);
                for (int k = 0; k <= ARC_DOTS; k++) {
                    double heading = piece.sweepFrom(a) + (piece.sweepTo(a) - piece.sweepFrom(a)) * k / ARC_DOTS;
                    batch.draw(
                            pixel,
                            (float) (X0 + x - Math.sin(heading) * length - 1.5),
                            (float) (y - Math.cos(heading) * length - 1.5),
                            3,
                            3);
                }
            } else {
                double heading = piece.sweepBeam(a, alpha);
                float width = (float) sweep.width();
                batch.setColor(CRIMSON.r, CRIMSON.g, CRIMSON.b, 0.35f);
                stroke(batch, x, y, heading, length, width * 2);
                batch.setColor(CRIMSON.r, CRIMSON.g, CRIMSON.b, 0.9f);
                stroke(batch, x, y, heading, length, width);
                batch.setColor(HOT.r, HOT.g, HOT.b, 0.9f);
                stroke(batch, x, y, heading, length, Math.max(1, width / 3));
            }
        }
        batch.setColor(Color.WHITE);
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
    }

    /** The telegraph's arc end is dotted with this many steps. */
    private static final int ARC_DOTS = 12;

    /** A stroke from (x, y) along {@code heading} (radians clockwise from straight down), {@code length} long. */
    private void stroke(SpriteBatch batch, double x, double y, double heading, double length, float width) {
        float degrees = (float) Math.toDegrees(Math.atan2(-Math.cos(heading), -Math.sin(heading)));
        batch.draw(
                pixel, (float) (X0 + x), (float) (y - width / 2), 0, width / 2, (float) length, width, 1, 1, degrees);
    }

    // --- the daylight ----------------------------------------------------------------------------

    /** The canopy's veins' strength {@code tau} s after the Spire's death (1 before it): fading out from 2 to 7 s. */
    static float veins(double tau) {
        return tau < 0 ? 1 : 1 - ramp(tau, VEINS_FROM, VEINS_TO);
    }

    /** The canopy-tear frame {@code tau} s after the death: from 0.5 s, each until 1 s past its time; -1 for none. */
    static int tearFrame(double tau) {
        if (tau < TEAR_FROM) {
            return -1;
        }
        for (int k = 0; k < TEAR_TIMES.length; k++) {
            if (tau < TEAR_TIMES[k] + 1) {
                return k;
            }
        }
        return -1;
    }

    /** The beams' frame {@code tau} s after the death (-1 before them) and their strength. */
    static int beamFrame(double tau) {
        return tau < BEAMS_FROM ? -1 : tau < BEAMS_SECOND ? 0 : 1;
    }

    static float beamStrength(double tau) {
        return ramp(tau, BEAMS_FROM, BEAMS_TO);
    }

    /**
     * Over the high-air layer, {@code tau} s after the death (nothing before): the tearing membrane
     * tiled on the high-air scroll {@code highAirScroll} (straight alpha), then the low sun's beams
     * (additive, fixed on the screen).
     */
    void drawDaylight(SpriteBatch batch, double tau, long highAirScroll) {
        if (tau < 0) {
            return;
        }
        int k = tearFrame(tau);
        if (k >= 0 && k < tear.size) {
            AtlasRegion image = tear.get(k);
            int height = image.getRegionHeight() > 0 ? image.getRegionHeight() : CANOPY_PERIOD;
            int offset = (int) Math.floorMod(highAirScroll, (long) height);
            for (int y = -offset; y < PlayField.HEIGHT; y += height) {
                batch.draw(image, X0, y);
            }
        }
        int b = beamFrame(tau);
        if (b >= 0 && b < beams.size) {
            batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
            batch.setColor(1, 1, 1, beamStrength(tau));
            batch.draw(beams.get(b), X0, 0);
            batch.setColor(Color.WHITE);
            batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        }
    }

    /** tools/art/daylight_l14.py's ramp: 0 before {@code from}, 1 after {@code to}, smoothstep between. */
    static float ramp(double t, double from, double to) {
        double s = Math.clamp((t - from) / (to - from), 0, 1);
        return (float) (s * s * (3 - 2 * s));
    }

    // --- helpers ---------------------------------------------------------------------------------

    /** Whether part {@code p} takes hits now (exposed, not wrecked). */
    private static boolean exposed(SetPiece piece, int p) {
        return !piece.partShielded(p) && !piece.partWrecked(p);
    }

    /** The destroyable part whose offset (x right, y up) lies nearest (x, y); -1 for none within 40 px. */
    static int nearestPart(java.util.List<LevelScript.PartSpec> parts, double x, double y) {
        int best = -1;
        double nearest = 40;
        for (int p = 0; p < parts.size(); p++) {
            LevelScript.PartSpec part = parts.get(p);
            double d = Math.hypot(part.dx() - x, part.dy() - y);
            if (d < nearest) {
                nearest = d;
                best = p;
            }
        }
        return best;
    }

    private static int partNamed(java.util.List<LevelScript.PartSpec> parts, String name) {
        for (int p = 0; p < parts.size(); p++) {
            if (parts.get(p).name().equals(name)) {
                return p;
            }
        }
        return -1;
    }

    private static int vitalPart(java.util.List<LevelScript.PartSpec> parts) {
        for (int p = 0; p < parts.size(); p++) {
            if (parts.get(p).vital()) {
                return p;
            }
        }
        return -1;
    }

    private static void centred(SpriteBatch batch, TextureRegion region, double x, double y) {
        batch.draw(
                region,
                Math.round(X0 + x - region.getRegionWidth() / 2.0),
                Math.round(y - region.getRegionHeight() / 2.0));
    }
}
