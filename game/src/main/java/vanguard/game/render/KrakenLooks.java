package vanguard.game.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonValue;
import java.util.Arrays;
import vanguard.sim.LevelScript;
import vanguard.sim.SetPiece;
import vanguard.sim.SlamArena;

/**
 * M5 part E: the Harbour Kraken wrapped round Platform Tiamat (design/enemies/bosses/harbour-kraken;
 * user decisions E4 = c and E7 = a; the sprites of tools/art/harbour_kraken.py, registered on the
 * platform's centre by assets/pivots/harbour-kraken.json, and the lane looks of tools/art/water_fx.py).
 * The platform itself is the level's backdrop piece; the boss lies on the ground layer at its centre.
 *
 * <p>Under the water, in the {@code sub} pass ({@link #drawSub}): the two idle arms trailing toward the
 * lower corners (a shadow drawn at 2×, squirming; the foreshadowing silhouette is drawn the same way
 * but over the chop, {@link #drawForeshadow}), the gripping
 * arms' under-water stretch, the slam arms' segments that are under the surface, and the plain head
 * and mantle (always: what of it is under the surface). On the surface ({@link #drawSurface}): the
 * grip overlay on the deck, the head's surface layer (a surfacing or diving step, or the up frame by
 * its eyes and beak with its foam collar), then the slam arms' segments above the water, tip first and
 * root on top, with a foam clump where an arm breaks the surface and foam strips along an arm lying
 * awash. The slam arms are runtime chains drawn from the tapered segment sprites at 32 headings, laid
 * along a path that follows the simulation's {@link SlamArena}: at rest a curl under the mantle (the
 * pivots' rest path); in the telegraph moving under the water toward its lane; rising base to tip
 * along the lane from the deck's edge (the stretch from its root to the deck's edge stays under the
 * water), in a travelling S-bend that whips toward the tip ({@link #whip}); lying awash as the bend
 * settles; sinking tip first and drifting back to rest. A severed arm is gone (its
 * segments pop, {@link #armPoints}).
 *
 * <p>The lanes ({@link #drawLanes}, {@link #drawLaneMarks}): a telegraphed lane's churn tiled down the
 * lane (fading in over 0.3 s, lasting until 0.2 s after its impact) and its red marks blinking at 5 Hz,
 * additive; at the impact the splash laid along the arm every {@value #SPLASH_STEP} px from its base,
 * each segment a frame after the one before. The round-33 a/b sizes are read from the atlas: a churn
 * narrower than the lane runs down its centre, a full-width one fills it; a mark taller than wide is a
 * dash drawn down both lane edges, a wider one a chevron crawling down the centre.
 *
 * <p>At its death the head sinks in its foam ring over its fading plain frame, the grip slides off
 * the deck, the shadows fade, and all of it scrolls on with the ground from where it died.
 */
final class KrakenLooks {
    private static final float X0 = PixelScreen.PLAY_FIELD_X;
    /**
     * A part's hit flash: at most one each {@value HitFlash#COOLDOWN_TICKS} steps ({@link HitFlash}),
     * only on the part hit (the head's frame, or the hit arm's segments out of the water; never the
     * grip, the collar, the shadows or the other parts), and only this far toward a pale flesh tint:
     * a subtle blink, not the whole animal turning white (round 33, user, 2026-10-09).
     */
    static final float PART_FLASH = 0.3f;

    static final Color FLASH_TINT = new Color(1f, 0.88f, 0.82f, 1f);
    /**
     * A slam arm along its lane is not a straight line (round 33, user): a travelling S-bend whips
     * from the deck's edge toward the tip as it rises ({@value #WHIP_PX} px either side, growing
     * toward the tip, {@value #WHIP_WAVES} waves along the lane, travelling {@value #WHIP_TRAVEL} waves
     * during the rise), then settles while it lies awash toward a slow residual curl of
     * {@value #WHIP_REST_PX} px. Presentation only: the simulation's hit area stays the lane's line,
     * and the bend stays well inside the 120 px lane.
     */
    static final double WHIP_PX = 22;

    static final double WHIP_REST_PX = 6;
    static final double WHIP_WAVES = 1.25;
    static final double WHIP_TRAVEL = 1.5;
    private static final double WHIP_DRIFT = 0.3;
    private static final double WHIP_SETTLE = 3.5;
    /** The points of an arm's lane leg, from the deck's edge to its tip. */
    static final int LANE_POINTS = 13;
    /** The animations' steps a frame (the pivots' {@code steps}). */
    private static final int SWAY_TICKS = 12;

    private static final int COLLAR_TICKS = 8;
    private static final int SINK_TICKS = 12;
    private static final int SLIDE_TICKS = 12;
    private static final int FOAM_TICKS = 6;
    private static final int SHADOW_TICKS = 15;
    /** The surfacing's 12 frames played in time with the simulation's share (2 s rising). */
    private static final int SURFACE_FRAMES = 12;
    /** The up frames: eyes shut, half open, open, beak glowing half, full. */
    static final int EYES_SHUT = 0;

    static final int EYES_HALF = 1;
    static final int EYES_OPEN = 2;
    static final int GLOW_HALF = 3;
    static final int GLOW_FULL = 4;
    /** The eyes show half open this long after the head is up; the glow half its first half. */
    static final int HALF_OPEN_TICKS = 9;

    static final int GLOW_HALF_TICKS = 15;
    /** The idle and foreshadow shadows are rendered at half size and drawn at 2× (the pivots' {@code shadow_scale}). */
    private static final float SHADOW_SCALE = 2;
    /** The plain head under the water while it is down: darker, deep (opaque, {@link #headShade}). */
    private static final float DEEP_HEAD = 0.75f;
    /** The arm segments: 32 headings, 7 tapers (42 px at the root to 14 at the tip), 2 tip sizes. */
    static final int HEADINGS = 32;

    static final int[] TAPERS = {42, 37, 32, 27, 22, 18, 14};
    static final int[] TIPS = {34, 22};
    /** Segment sprites lie half their size apart along the arm. */
    static final double SPACING = 0.5;
    /** The most sprites one arm path takes. */
    private static final int MAX_SPRITES = 96;
    /** Points of the paths an arm moves between (resampled so they can be blended). */
    private static final int PATH_POINTS = 16;
    /** The churn fades in over this many steps; it lasts this many after the impact. */
    static final int CHURN_FADE_TICKS = 18;

    static final int CHURN_TAIL_TICKS = 12;
    private static final int CHURN_FRAME_TICKS = 6;
    /** The marks blink lit and dim at 5 Hz; a dash every 24 px down each edge, a chevron every 40 px. */
    private static final int MARK_BLINK_TICKS = 6;

    static final int DASH_STEP = 24;
    static final int CHEVRON_STEP = 40;
    private static final int EDGE_INSET = 3;
    /** The splash: a segment every 48 px from the arm's base, each a frame later; 3 steps a frame (20 fps). */
    static final int SPLASH_STEP = 48;

    static final int SPLASH_FRAME_TICKS = 3;
    /** Foam strips lie along an arm awash this far either side of its line, every 24 px. */
    private static final int STRIP_SIDE = 16;

    private static final int STRIP_STEP = 24;
    private static final int STRIP_FRAME_TICKS = 9;
    /**
     * The foreshadowing (design/campaign Level 11, section 2): at about t = 55 the whole animal's
     * shadow slides up under the convoy and fades into the deep, scenery and not the boss.
     */
    static final double FORESHADOW_START = 52;

    static final double FORESHADOW_SECONDS = 7;
    private static final double FORESHADOW_FROM_X = 330;
    private static final double FORESHADOW_TO_X = 170;
    private static final double FORESHADOW_FROM_Y = -140;
    private static final double FORESHADOW_TO_Y = 380;
    private static final float FORESHADOW_TURN = 18;
    private static final float FORESHADOW_OPACITY = 0.9f;

    private final FlashShader flash;
    private final AtlasRegion subHead;
    private final Array<AtlasRegion> up;
    private final Array<AtlasRegion> collar;
    private final Array<AtlasRegion> surface;
    private final Array<AtlasRegion> arm;
    private final Array<AtlasRegion> tip;
    private final Array<AtlasRegion> foam;
    private final Array<AtlasRegion> grip;
    private final Array<AtlasRegion> gripSub;
    private final Array<AtlasRegion> gripSlide;
    private final Array<AtlasRegion> idleLeft;
    private final Array<AtlasRegion> idleRight;
    private final Array<AtlasRegion> shadow;
    private final Array<AtlasRegion> sink;
    /** The lane looks (round 33's a/b, whichever the atlas holds); empty without them. */
    private final Array<AtlasRegion> churn;

    private final Array<AtlasRegion> mark;
    private final Array<AtlasRegion> splash;
    private final Array<AtlasRegion> strip;
    /** The registration, px from the platform's centre, y up. */
    private final float headDy;

    private final float gripDy;
    private final float gripSubDy;
    private final float idleLeftDx;
    private final float idleRightDx;
    private final float idleDy;
    /** Per side (0 left, 1 right): the slam arm's root and its rest path after the root, y up. */
    private final float[][] restX = new float[2][];

    private final float[][] restY = new float[2][];
    /** Per lane (1-based): the step its telegraph started and its last impact; MIN_VALUE for none. */
    private long[] telegraphTick = new long[0];

    private long[] impactTick = new long[0];
    private int telegraphedBefore;
    private SlamArena.SurfaceState surfaceBefore = SlamArena.SurfaceState.DOWN;
    private long upSince;
    private boolean glowBefore;
    private long glowSince;
    /** Where it died and the ground's scroll then; NaN while it lives. */
    private double deathX = Double.NaN;

    private final HitFlash hits = new HitFlash();

    private double deathY;
    private double deathScroll;
    /** Scratch: an arm's path and its sprites (x, y, heading index, size, above). */
    private final float[] pathX = new float[PATH_POINTS + 4];

    private final float[] pathY = new float[PATH_POINTS + 4];
    private final float[] blendX = new float[PATH_POINTS];
    private final float[] blendY = new float[PATH_POINTS];
    private final float[] spriteX = new float[MAX_SPRITES];
    private final float[] spriteY = new float[MAX_SPRITES];
    private final int[] spriteHeading = new int[MAX_SPRITES];
    private final float[] spriteSize = new float[MAX_SPRITES];
    private final float[] spriteArc = new float[MAX_SPRITES];
    private int sprites;
    private int pathPoints;

    private KrakenLooks(Sprites atlas, FlashShader flash, JsonValue pivots) {
        this.flash = flash;
        String slug = "harbour-kraken";
        subHead = atlas.frames(slug + "-sub").first();
        up = atlas.frames(slug);
        collar = atlas.frames(slug + "-collar");
        surface = atlas.frames(slug + "-surface");
        arm = atlas.frames(slug + "-arm");
        tip = atlas.frames(slug + "-tip");
        foam = atlas.frames(slug + "-foam");
        grip = atlas.frames(slug + "-grip");
        gripSub = atlas.frames(slug + "-grip-sub");
        gripSlide = atlas.frames(slug + "-grip-slide");
        idleLeft = atlas.frames(slug + "-idle-left");
        idleRight = atlas.frames(slug + "-idle-right");
        shadow = atlas.frames(slug + "-shadow");
        sink = atlas.frames(slug + "-sink");
        churn = optional(atlas, slug + "-lane-churn");
        mark = optional(atlas, slug + "-lane-mark");
        splash = optional(atlas, slug + "-lane-splash");
        strip = optional(atlas, "water-foam-strip");
        headDy = -pivots.get("head").getFloat(1);
        gripDy = -pivots.get("grip").getFloat(1);
        gripSubDy = -pivots.get("grip_sub").getFloat(1);
        idleLeftDx = pivots.get("idle_left").getFloat(0);
        idleRightDx = pivots.get("idle_right").getFloat(0);
        idleDy = -pivots.get("idle_left").getFloat(1);
        JsonValue slam = pivots.get("slam_arms");
        for (int side = 0; side < 2; side++) {
            JsonValue one = slam.get(side == 0 ? "left" : "right");
            JsonValue rest = one.get("rest_path");
            restX[side] = new float[rest.size + 1];
            restY[side] = new float[rest.size + 1];
            restX[side][0] = one.get("root").getFloat(0);
            restY[side][0] = -one.get("root").getFloat(1);
            for (int i = 0; i < rest.size; i++) {
                restX[side][i + 1] = rest.get(i).getFloat(0);
                restY[side][i + 1] = -rest.get(i).getFloat(1);
            }
        }
    }

    /**
     * The looks of the level's arena boss, if it is one with its production sprites and pivots
     * (the Harbour Kraken); null otherwise.
     */
    static KrakenLooks of(Sprites atlas, FlashShader flash, LevelScript.SetPieceSpec boss, JsonValue pivots) {
        if (boss == null
                || pivots == null
                || boss.boss().flatMap(spec -> spec.arena()).isEmpty()
                || !atlas.has(boss.slug() + "-sub")
                || !boss.slug().equals("harbour-kraken")) {
            return null;
        }
        return new KrakenLooks(atlas, flash, pivots);
    }

    private static Array<AtlasRegion> optional(Sprites atlas, String name) {
        return atlas.has(name) ? atlas.frames(name) : new Array<>(0);
    }

    /** The level restarts (or the fight is retried): the lanes, the head's moments and the death are forgotten. */
    void reset() {
        Arrays.fill(telegraphTick, Long.MIN_VALUE);
        Arrays.fill(impactTick, Long.MIN_VALUE);
        telegraphedBefore = 0;
        surfaceBefore = SlamArena.SurfaceState.DOWN;
        glowBefore = false;
        deathX = Double.NaN;
        hits.reset();
    }

    /** Whether it is drawn: scrolling in, present or dying. */
    static boolean shown(SetPiece piece, boolean dead) {
        return dead || piece.present() || piece.approaching();
    }

    /**
     * Notes the moments the looks time from (once a frame, before drawing): each lane's telegraph start
     * and impact (its telegraph bit set and cleared), the head coming up, the beak's glow starting.
     */
    void update(SetPiece piece, long tick) {
        SlamArena arena = piece.arena().orElseThrow();
        if (telegraphTick.length != arena.laneCount() + 1) {
            telegraphTick = new long[arena.laneCount() + 1];
            impactTick = new long[arena.laneCount() + 1];
            Arrays.fill(telegraphTick, Long.MIN_VALUE);
            Arrays.fill(impactTick, Long.MIN_VALUE);
        }
        int mask = arena.telegraphed();
        for (int lane = 1; lane <= arena.laneCount(); lane++) {
            boolean now = (mask & (1 << lane)) != 0;
            boolean before = (telegraphedBefore & (1 << lane)) != 0;
            if (now && !before) {
                telegraphTick[lane] = tick;
            } else if (!now && before) {
                impactTick[lane] = tick;
            }
        }
        telegraphedBefore = mask;
        SlamArena.SurfaceState state = arena.surfaceState();
        if (state == SlamArena.SurfaceState.UP && surfaceBefore != SlamArena.SurfaceState.UP) {
            upSince = tick;
        }
        surfaceBefore = state;
        boolean glowing = arena.glowing();
        if (glowing && !glowBefore) {
            glowSince = tick;
        }
        glowBefore = glowing;
    }

    // --- under the water ------------------------------------------------------------------------

    /**
     * The {@code sub} layer's parts at the platform centre ({@code x}, {@code y}); {@code deathAge}
     * the steps since its death, -1 while it lives.
     */
    void drawSub(SpriteBatch batch, SetPiece piece, double x, double y, long tick, int deathAge) {
        int sway = (int) (tick / SWAY_TICKS % idleLeft.size);
        float fade = deathAge < 0 ? 1 : Math.max(0, 1 - deathAge / 60f);
        if (fade > 0) {
            batch.setColor(1, 1, 1, fade);
            drawScaled(batch, idleLeft.get(sway), x + idleLeftDx, y + idleDy, SHADOW_SCALE, 0);
            drawScaled(batch, idleRight.get(sway), x + idleRightDx, y + idleDy, SHADOW_SCALE, 0);
            drawCentred(batch, gripSub.get(sway % gripSub.size), x, y + gripSubDy);
            batch.setColor(Color.WHITE);
        }
        if (deathAge < 0) {
            SlamArena arena = piece.arena().orElseThrow();
            for (int a = 0; a < arena.armCount(); a++) {
                if (layArm(piece, arena, a, x, y)) {
                    drawArm(batch, false, 0);
                }
            }
        }
        float head = deathAge < 0 ? 1 : Math.max(0, 1 - deathAge / (float) (SINK_TICKS * sink.size));
        if (head > 0) {
            float shade = headShade(piece.arena().orElseThrow().surfaceState());
            batch.setColor(shade, shade, shade, head);
            drawCentred(batch, subHead, x, y + headDy);
            batch.setColor(Color.WHITE);
        }
    }

    /**
     * The plain head's shade under the water: deep and darker while it is down. It stays opaque (alive),
     * so the gripping arms' under-water stretches and the slam arms' roots behind it never show through
     * the mantle (round 33's capture, Decisions 2026-10-09).
     */
    static float headShade(SlamArena.SurfaceState state) {
        return state == SlamArena.SurfaceState.DOWN ? DEEP_HEAD : 1;
    }

    /**
     * The foreshadowing silhouette at script time {@code seconds}, over the chop and under the convoy
     * (in the {@code sub} pass the water's tint took its dark to the sea's own colour): crown first,
     * sliding up under the convoy from the lower right, turned toward its way, fading in and then into
     * the deep; nothing outside its {@value #FORESHADOW_SECONDS} s.
     */
    void drawForeshadow(SpriteBatch batch, double seconds, long tick) {
        double share = foreshadowShare(seconds);
        if (share < 0) {
            return;
        }
        float opacity = foreshadowOpacity(share);
        double x = FORESHADOW_FROM_X + (FORESHADOW_TO_X - FORESHADOW_FROM_X) * share;
        double y = FORESHADOW_FROM_Y + (FORESHADOW_TO_Y - FORESHADOW_FROM_Y) * share;
        batch.setColor(1, 1, 1, opacity);
        drawScaled(batch, shadow.get((int) (tick / SHADOW_TICKS % shadow.size)), x, y, SHADOW_SCALE, FORESHADOW_TURN);
        batch.setColor(Color.WHITE);
    }

    /** How far through the foreshadowing the script time is, 0..1; -1 outside it. */
    static double foreshadowShare(double seconds) {
        double share = (seconds - FORESHADOW_START) / FORESHADOW_SECONDS;
        return share < 0 || share > 1 ? -1 : share;
    }

    /** Its opacity at {@code share}: in over the first seventh, out into the deep over the last 40 %. */
    static float foreshadowOpacity(double share) {
        double in = Math.min(1, share * 7);
        double out = Math.min(1, (1 - share) / 0.4);
        return (float) (FORESHADOW_OPACITY * Math.min(in, out));
    }

    // --- on the surface -------------------------------------------------------------------------

    /** The grip overlay, the head's surface layer and the slam arms above the water. */
    void drawSurface(SpriteBatch batch, SetPiece piece, double x, double y, long tick, int deathAge, float whiteFlash) {
        if (deathAge >= 0) {
            int slide = deathAge / SLIDE_TICKS;
            if (slide < gripSlide.size) {
                drawCentred(batch, gripSlide.get(slide), x, y + gripDy);
            }
            int step = deathAge / SINK_TICKS;
            if (step < sink.size) {
                drawCentred(batch, sink.get(step), x, y + headDy);
            }
            return;
        }
        drawCentred(batch, grip.get((int) (tick / SWAY_TICKS % grip.size)), x, y + gripDy);
        SlamArena arena = piece.arena().orElseThrow();
        int head = arena.surfacePart() >= 0 ? arena.surfacePart() : 0;
        switch (arena.surfaceState()) {
            case RISING, DIVING -> drawCentred(batch, surface.get(surfaceFrame(arena.surfaceShare())), x, y + headDy);
            case UP -> {
                AtlasRegion frame = up.get(upFrame(tick - upSince, arena.glowing(), tick - glowSince));
                float headY = (float) (y + headDy);
                if (hits.on(head, tick, piece.partTicksSinceHit(head))) {
                    flash.draw(
                            batch, frame, Math.round(X0 + x), Math.round(headY), FLASH_TINT, PART_FLASH * whiteFlash);
                } else {
                    drawCentred(batch, frame, x, headY);
                }
                drawCentred(batch, collar.get((int) (tick / COLLAR_TICKS % collar.size)), x, headY);
            }
            case DOWN -> {}
        }
        for (int a = 0; a < arena.armCount(); a++) {
            if (layArm(piece, arena, a, x, y)) {
                int part = arena.armPart(a);
                drawArm(batch, true, hits.on(part, tick, piece.partTicksSinceHit(part)) ? PART_FLASH * whiteFlash : 0);
                drawFoam(batch, arena, a, tick);
            }
        }
    }

    /** The surfacing frame of a rise or dive {@code share} (0 down to 1 up): played forward rising, backward diving. */
    static int surfaceFrame(double share) {
        return Math.clamp((int) Math.floor(share * SURFACE_FRAMES), 0, SURFACE_FRAMES - 1);
    }

    /**
     * The up frame: the eyes half open just after it is up, then open; the beak's glow half then full
     * while the tell runs ({@code sinceGlow} steps into it).
     */
    static int upFrame(long sinceUp, boolean glowing, long sinceGlow) {
        if (glowing) {
            return sinceGlow < GLOW_HALF_TICKS ? GLOW_HALF : GLOW_FULL;
        }
        return sinceUp < HALF_OPEN_TICKS ? EYES_HALF : EYES_OPEN;
    }

    /** The foam where an arm breaks the surface, and foam strips along it while it lies awash. */
    private void drawFoam(SpriteBatch batch, SlamArena arena, int a, long tick) {
        SlamArena.ArmState state = arena.armState(a);
        double share = arena.armShare(a);
        float base = baseArc();
        float total = spriteArc[sprites - 1];
        float front = frontArc(state, share, base, total);
        int variant = a % 2;
        int frames = foam.size / 2;
        if (front > base) {
            int f = (int) (tick / FOAM_TICKS % frames);
            drawCentred(batch, foam.get(variant * frames + f), pathAt(base, true), pathAt(base, false));
            if (front < total) {
                drawCentred(
                        batch,
                        foam.get(variant * frames + (f + 3) % frames),
                        pathAt(front, true),
                        pathAt(front, false));
            }
        }
        if (state == SlamArena.ArmState.AWASH && !strip.isEmpty()) {
            int lane = arena.armLane(a);
            double laneX = arena.laneWidth() * (lane - 0.5);
            int f = (int) (tick / STRIP_FRAME_TICKS);
            double length = whipLaneTop - SlamArena.TIP_MARGIN;
            for (double yy = arena.laneTop() - STRIP_STEP / 2.0; yy > SlamArena.TIP_MARGIN; yy -= STRIP_STEP) {
                // The strips follow the arm's settling bend.
                double bent = laneX + whip((whipLaneTop - yy) / length, whipAt, whipSign);
                drawRotated(batch, strip.get(f % strip.size), bent - STRIP_SIDE, yy);
                drawRotated(batch, strip.get((f + 2) % strip.size), bent + STRIP_SIDE, yy);
            }
        }
    }

    // --- the arms -------------------------------------------------------------------------------

    /**
     * Lays slam arm {@code a}'s sprites along its path at the platform centre ({@code x}, {@code y});
     * false for a severed arm (not drawn).
     */
    private boolean layArm(SetPiece piece, SlamArena arena, int a, double x, double y) {
        int part = arena.armPart(a);
        if (piece.partWrecked(part)) {
            return false;
        }
        int side = piece.spec().parts().get(part).dx() < 0 ? 0 : 1;
        SlamArena.ArmState state = arena.armState(a);
        double share = arena.armShare(a);
        int lane = arena.armLane(a) > 0 ? arena.armLane(a) : arena.armFirstLane(a);
        double laneX = arena.laneWidth() * (lane - 0.5);
        double sign = side == 0 ? 1 : -1;
        whipSign = sign;
        whipLaneTop = arena.laneTop();
        whipAt = whipTime(state, share);
        switch (state) {
            case IDLE -> restPath(side, x, y);
            case TELEGRAPH -> blendPaths(side, x, y, laneX, arena.laneTop(), whipAt, sign, smooth(share));
            case RISE, AWASH -> lanePath(side, x, y, laneX, arena.laneTop(), whipAt, sign);
            case SINK -> {
                if (share < 0.5) {
                    lanePath(side, x, y, laneX, arena.laneTop(), whipAt, sign);
                } else {
                    blendPaths(side, x, y, laneX, arena.laneTop(), whipAt, sign, smooth(1 - (share - 0.5) * 2));
                }
            }
        }
        place();
        float base =
                state == SlamArena.ArmState.IDLE || state == SlamArena.ArmState.TELEGRAPH ? Float.MAX_VALUE : baseArc();
        float front = frontArc(state, share, base, spriteArc[sprites - 1]);
        for (int i = 0; i < sprites; i++) {
            above[i] = spriteArc[i] >= base && spriteArc[i] <= front;
        }
        return true;
    }

    private final boolean[] above = new boolean[MAX_SPRITES];
    /** The arm laid last ({@link #layArm}): its whip's time and side, its lane, for its foam strips. */
    private double whipAt;

    private double whipSign = 1;
    private double whipLaneTop;

    /**
     * The whip's time of an arm in {@code state} at {@code share} of it: 0 in the telegraph (the bend
     * it rises with), 0..1 through the rise, 1..2 lying awash, 2..2.5 the sink's first half.
     */
    static double whipTime(SlamArena.ArmState state, double share) {
        return switch (state) {
            case IDLE, TELEGRAPH -> 0;
            case RISE -> share;
            case AWASH -> 1 + share;
            case SINK -> 2 + Math.min(share, 0.5);
        };
    }

    /**
     * The arm's sideways offset, px (times {@code sign}: the right arm's mirrored), at {@code u} along
     * its lane (0 at the deck's edge, 1 at its tip) and whip time {@code w} ({@link #whipTime}): a
     * travelling S-bend, nothing at the deck's edge and growing toward the tip, whipping through the
     * rise and settling awash.
     */
    static double whip(double u, double w, double sign) {
        double amplitude =
                w <= 1 ? WHIP_PX : WHIP_REST_PX + (WHIP_PX - WHIP_REST_PX) * Math.exp(-WHIP_SETTLE * (w - 1));
        double phase = w <= 1 ? WHIP_TRAVEL * w : WHIP_TRAVEL + WHIP_DRIFT * (w - 1);
        double envelope = 1 - (1 - u) * (1 - u);
        return sign * amplitude * envelope * Math.sin(2 * Math.PI * (WHIP_WAVES * u - phase));
    }

    /** Where along the arm (arc px from the root) the part above the water ends now. */
    private static float frontArc(SlamArena.ArmState state, double share, float base, float total) {
        return switch (state) {
            case IDLE, TELEGRAPH -> -1;
            case RISE -> (float) (base + (total - base) * share);
            case AWASH -> total;
            case SINK -> share < 0.5 ? (float) (total - (total - base) * share * 2) : -1;
        };
    }

    /** The arc from the root to the deck's edge on the lane path: the first lane point's (point 1). */
    private float baseArc() {
        return (float) Math.hypot(pathX[1] - pathX[0], pathY[1] - pathY[0]);
    }

    /** The arm at rest: its root and the pivots' rest path, under the water. */
    private void restPath(int side, double x, double y) {
        float[] rx = restX[side];
        float[] ry = restY[side];
        for (int i = 0; i < rx.length; i++) {
            pathX[i] = (float) (x + rx[i]);
            pathY[i] = (float) (y + ry[i]);
        }
        pathPoints = rx.length;
    }

    /**
     * The arm along its lane: from its root to the deck's edge on the lane's centre line, then down to
     * its tip in the whip's bend at time {@code w} ({@link #whip}).
     */
    private void lanePath(int side, double x, double y, double laneX, double laneTop, double w, double sign) {
        pathX[0] = (float) (x + restX[side][0]);
        pathY[0] = (float) (y + restY[side][0]);
        lanePoints(laneX, laneTop, w, sign, pathX, pathY, 1);
        pathPoints = 1 + LANE_POINTS;
    }

    /** The lane leg's {@value #LANE_POINTS} points from the deck's edge to the tip, into the arrays from {@code at}. */
    static void lanePoints(double laneX, double laneTop, double w, double sign, float[] xs, float[] ys, int at) {
        for (int i = 0; i < LANE_POINTS; i++) {
            double u = i / (double) (LANE_POINTS - 1);
            xs[at + i] = (float) (laneX + whip(u, w, sign));
            ys[at + i] = (float) (laneTop + (SlamArena.TIP_MARGIN - laneTop) * u);
        }
    }

    /** Between the rest path ({@code t} 0) and the lane path ({@code t} 1), both resampled evenly. */
    private void blendPaths(
            int side, double x, double y, double laneX, double laneTop, double w, double sign, double t) {
        restPath(side, x, y);
        resample(blendX, blendY);
        lanePath(side, x, y, laneX, laneTop, w, sign);
        resample(pathX, pathY);
        for (int i = 0; i < PATH_POINTS; i++) {
            pathX[i] = (float) (blendX[i] + (pathX[i] - blendX[i]) * t);
            pathY[i] = (float) (blendY[i] + (pathY[i] - blendY[i]) * t);
        }
        pathPoints = PATH_POINTS;
    }

    private final float[] resampleX = new float[PATH_POINTS + 4];
    private final float[] resampleY = new float[PATH_POINTS + 4];

    /** The current path resampled to {@value #PATH_POINTS} points evenly along it, into (outX, outY). */
    private void resample(float[] outX, float[] outY) {
        System.arraycopy(pathX, 0, resampleX, 0, pathPoints);
        System.arraycopy(pathY, 0, resampleY, 0, pathPoints);
        double length = 0;
        for (int i = 1; i < pathPoints; i++) {
            length += Math.hypot(resampleX[i] - resampleX[i - 1], resampleY[i] - resampleY[i - 1]);
        }
        int segment = 1;
        double start = 0;
        for (int k = 0; k < PATH_POINTS; k++) {
            double at = length * k / (PATH_POINTS - 1);
            double piece = Math.hypot(
                    resampleX[segment] - resampleX[segment - 1], resampleY[segment] - resampleY[segment - 1]);
            while (segment < pathPoints - 1 && start + piece < at) {
                start += piece;
                segment++;
                piece = Math.hypot(
                        resampleX[segment] - resampleX[segment - 1], resampleY[segment] - resampleY[segment - 1]);
            }
            double f = piece > 0 ? Math.clamp((at - start) / piece, 0, 1) : 0;
            outX[k] = (float) (resampleX[segment - 1] + (resampleX[segment] - resampleX[segment - 1]) * f);
            outY[k] = (float) (resampleY[segment - 1] + (resampleY[segment] - resampleY[segment - 1]) * f);
        }
    }

    /**
     * Places the arm's sprites along the path from its root: each half its size after the one before,
     * tapering evenly from the root's 42 px to the tip's 14, the tip's curl at the path's end.
     */
    private void place() {
        double length = 0;
        for (int i = 1; i < pathPoints; i++) {
            length += Math.hypot(pathX[i] - pathX[i - 1], pathY[i] - pathY[i - 1]);
        }
        sprites = 0;
        double s = 0;
        while (sprites < MAX_SPRITES - 1 && s < length) {
            float size = taperedSize(s / Math.max(1, length));
            put(s, size);
            s += SPACING * size;
        }
        put(length, TAPERS[TAPERS.length - 1]);
    }

    /** The segment's size at {@code u} (0 at the root, 1 at the tip) along the arm. */
    static float taperedSize(double u) {
        double clamped = Math.clamp(u, 0, 1);
        return (float) (TAPERS[0] + (TAPERS[TAPERS.length - 1] - TAPERS[0]) * clamped);
    }

    private void put(double s, float size) {
        spriteArc[sprites] = (float) s;
        spriteX[sprites] = pathAt((float) s, true);
        spriteY[sprites] = pathAt((float) s, false);
        // The heading toward the tip: the path's direction a little further on.
        double ahead = Math.min(s + 2, totalLength());
        double back = Math.max(0, ahead - 4);
        float dx = pathAt((float) ahead, true) - pathAt((float) back, true);
        float dy = pathAt((float) ahead, false) - pathAt((float) back, false);
        spriteHeading[sprites] = headingIndex(dx, dy);
        spriteSize[sprites] = size;
        sprites++;
    }

    private double totalLength() {
        double length = 0;
        for (int i = 1; i < pathPoints; i++) {
            length += Math.hypot(pathX[i] - pathX[i - 1], pathY[i] - pathY[i - 1]);
        }
        return length;
    }

    /** The path's x (or y) at arc {@code s} px from the root. */
    private float pathAt(float s, boolean xCoordinate) {
        double start = 0;
        for (int i = 1; i < pathPoints; i++) {
            double piece = Math.hypot(pathX[i] - pathX[i - 1], pathY[i] - pathY[i - 1]);
            if (s <= start + piece || i == pathPoints - 1) {
                double f = piece > 0 ? Math.clamp((s - start) / piece, 0, 1) : 0;
                return (float)
                        (xCoordinate
                                ? pathX[i - 1] + (pathX[i] - pathX[i - 1]) * f
                                : pathY[i - 1] + (pathY[i] - pathY[i - 1]) * f);
            }
            start += piece;
        }
        return xCoordinate ? pathX[0] : pathY[0];
    }

    /**
     * The heading index of a segment pointing along (dx, dy) (y up): k × 11.25° clockwise on the screen
     * from straight down (tools/art/harbour_kraken.py, {@code heading_index}).
     */
    static int headingIndex(double dx, double dy) {
        double screen = Math.atan2(-dy, dx);
        double turn = (screen - Math.PI / 2) % (2 * Math.PI);
        if (turn < 0) {
            turn += 2 * Math.PI;
        }
        return (int) Math.round(turn / (2 * Math.PI) * HEADINGS) % HEADINGS;
    }

    /** The taper index nearest a segment's size. */
    static int taperIndex(double size) {
        int best = 0;
        for (int i = 1; i < TAPERS.length; i++) {
            if (Math.abs(size - TAPERS[i]) < Math.abs(size - TAPERS[best])) {
                best = i;
            }
        }
        return best;
    }

    /**
     * The arm's sprites above ({@code aboveWater}) or under the water, tip first and root on top, those
     * above flashing {@code whiteFlash} toward the flash tint (0: not flashing).
     */
    private void drawArm(SpriteBatch batch, boolean aboveWater, float whiteFlash) {
        boolean hit = aboveWater && whiteFlash > 0;
        for (int i = sprites - 1; i >= 0; i--) {
            if (above[i] != aboveWater) {
                continue;
            }
            TextureRegion region = i == sprites - 1
                    ? tip.get(spriteHeading[i] * TIPS.length + (spriteSize[i] > 26 ? 0 : 1))
                    : arm.get(spriteHeading[i] * TAPERS.length + taperIndex(spriteSize[i]));
            if (hit) {
                flash.draw(batch, region, Math.round(X0 + spriteX[i]), Math.round(spriteY[i]), FLASH_TINT, whiteFlash);
            } else {
                drawCentred(batch, region, spriteX[i], spriteY[i]);
            }
        }
    }

    /**
     * Where a severed arm's segments are (its pops, rootward first): the simulation's chain segments of
     * arm part {@code part}, px from the platform's centre, as (dx0, dy0, dx1, dy1, …); empty for a part
     * that is no slam arm.
     */
    static double[] armPoints(SetPiece piece, int part) {
        var boss = piece.boss().orElseThrow();
        int start = 0;
        for (int c = 0; c < boss.chains().size(); c++) {
            var chain = boss.chains().get(c);
            if (chain.part() == part) {
                double[] points = new double[2 * chain.segments()];
                for (int k = 0; k < chain.segments(); k++) {
                    points[2 * k] = piece.segmentOffsetX(start + k);
                    points[2 * k + 1] = piece.segmentOffsetY(start + k);
                }
                return points;
            }
            start += chain.segments();
        }
        return new double[0];
    }

    // --- the lanes ------------------------------------------------------------------------------

    /** The churn of the telegraphed lanes on the surface (under the convoy). */
    void drawLanes(SpriteBatch batch, SetPiece piece, long tick) {
        if (churn.isEmpty()) {
            return;
        }
        SlamArena arena = piece.arena().orElseThrow();
        AtlasRegion frame = churn.get((int) (tick / CHURN_FRAME_TICKS % churn.size));
        for (int lane = 1; lane < telegraphTick.length; lane++) {
            float strength = churnStrength(tick, telegraphTick[lane], impactTick[lane]);
            if (strength <= 0) {
                continue;
            }
            batch.setColor(1, 1, 1, strength);
            double laneX = arena.laneWidth() * (lane - 0.5);
            int h = frame.getRegionHeight();
            for (double yy = arena.laneTop() - h / 2.0; yy > -h / 2.0; yy -= h) {
                drawCentred(batch, frame, laneX, yy);
            }
        }
        batch.setColor(Color.WHITE);
    }

    /**
     * A lane's churn's strength at {@code tick}: fading in over {@value #CHURN_FADE_TICKS} steps from its
     * telegraph, lasting {@value #CHURN_TAIL_TICKS} steps past its impact; 0 outside.
     */
    static float churnStrength(long tick, long telegraph, long impact) {
        if (telegraph == Long.MIN_VALUE || tick < telegraph) {
            return 0;
        }
        if (impact >= telegraph && tick >= impact + CHURN_TAIL_TICKS) {
            return 0;
        }
        return (float) Math.min(1, (tick - telegraph + 1) / (double) CHURN_FADE_TICKS);
    }

    /** The telegraphed lanes' red marks (additive) and the impacts' splashes, over the convoy. */
    void drawLaneMarks(SpriteBatch batch, SetPiece piece, long tick) {
        SlamArena arena = piece.arena().orElseThrow();
        int mask = arena.telegraphed();
        if (!mark.isEmpty() && mask != 0) {
            AtlasRegion frame = mark.get(tick / MARK_BLINK_TICKS % 2 == 0 ? 0 : Math.min(1, mark.size - 1));
            boolean dashes = frame.getRegionHeight() >= frame.getRegionWidth();
            batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
            for (int lane = 1; lane <= arena.laneCount(); lane++) {
                if ((mask & (1 << lane)) == 0) {
                    continue;
                }
                double laneX = arena.laneWidth() * (lane - 0.5);
                double top = arena.laneTop();
                if (dashes) {
                    double edge = arena.laneWidth() / 2 - EDGE_INSET;
                    for (double yy = top - 8; yy > 0; yy -= DASH_STEP) {
                        drawCentred(batch, frame, laneX - edge, yy);
                        drawCentred(batch, frame, laneX + edge, yy);
                    }
                } else {
                    double crawl = tick % 8 * 2;
                    for (double yy = top - 16 - crawl; yy > 0; yy -= CHEVRON_STEP) {
                        drawCentred(batch, frame, laneX, yy);
                    }
                }
            }
            batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        }
        if (splash.isEmpty()) {
            return;
        }
        for (int lane = 1; lane < impactTick.length; lane++) {
            if (impactTick[lane] == Long.MIN_VALUE) {
                continue;
            }
            long age = tick - impactTick[lane];
            double laneX = arena.laneWidth() * (lane - 0.5);
            int h = splash.first().getRegionHeight();
            for (int k = 0; ; k++) {
                double yy = arena.laneTop() - k * SPLASH_STEP - h / 2.0;
                if (yy < -h / 2.0) {
                    break;
                }
                int f = splashFrame(age, k);
                if (f >= 0 && f < splash.size) {
                    drawCentred(batch, splash.get(f), laneX, yy);
                }
            }
        }
    }

    /** The splash frame of segment {@code k} (from the arm's base) {@code age} steps after the impact; -1 before it starts. */
    static int splashFrame(long age, int k) {
        long frame = age / SPLASH_FRAME_TICKS - k;
        return frame < 0 || frame > Integer.MAX_VALUE ? -1 : (int) frame;
    }

    // --- its death ------------------------------------------------------------------------------

    /**
     * Where the dead Kraken is drawn: where it died, scrolled on with the ground since ({@code scroll}
     * now); notes the place on the first call after its death. Returns its y; {@link #deathX()} its x.
     */
    double deathY(SetPiece piece, double scroll) {
        if (Double.isNaN(deathX)) {
            deathX = piece.renderX(1);
            deathY = piece.renderY(1);
            deathScroll = scroll;
        }
        return deathY - (scroll - deathScroll);
    }

    double deathX() {
        return deathX;
    }

    /** The head's centre offset from the platform's, px (y up): where its death burst and sinking are. */
    double headDy() {
        return headDy;
    }

    // --- drawing helpers ------------------------------------------------------------------------

    private static void drawCentred(SpriteBatch batch, TextureRegion region, double x, double y) {
        batch.draw(
                region,
                Math.round(X0 + x - region.getRegionWidth() / 2.0),
                Math.round(y - region.getRegionHeight() / 2.0));
    }

    private static void drawScaled(
            SpriteBatch batch, TextureRegion region, double x, double y, float scale, float turn) {
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
                turn);
    }

    /** A foam strip turned to lie along a lane (its long side down the lane). */
    private static void drawRotated(SpriteBatch batch, TextureRegion region, double x, double y) {
        drawScaled(batch, region, x, y, 1, 90);
    }

    private static double smooth(double t) {
        double c = Math.clamp(t, 0, 1);
        return c * c * (3 - 2 * c);
    }
}
