package vanguard.game.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import vanguard.content.Tier;

/**
 * M5 part E: how a unit meets the water (design/art-direction, Water; user decision E4 = c), drawn in
 * two layers: its plain under-water body through the {@code sub} pass ({@link WaterLooks}) and over it,
 * on the surface, the part above the waterline with its foam collar (the unit's own frames). The
 * Driftjelly (tools/art/driftjelly.py): {@code -sub} at its four pulse frames, {@code -surface} the
 * 0.6 s swap (forward to surface, backward to dive; over {@code -sub_0}), {@code -pulse} its lime
 * veins' bloom (additive, over a submerged jelly: the warning through the water) and {@code -ripple}
 * the ring train of each contraction on the sea. Its pulse has its own rate, quickening as the ship
 * closes in ({@link #jellyFps}), not a radial spinner's. The Reef Spitter (tools/art/reef_spitter.py):
 * {@code -raft-sub} under its bobbing {@code -raft} frames, the gun at each frame's gun point, the
 * {@code -recoil} frame for {@value #RECOIL_TICKS} steps after a fan, and at its death the raft's
 * {@code -sink}. Every unit also has its kills on the water: the {@code explosion-water-*} rung of
 * its tier on the surface and the {@code explosion-under-*} one under it (tools/art/water_fx.py).
 *
 * @param sub the plain body for the {@code sub} pass (a jelly's per pulse frame); empty for none
 * @param surface the surfacing steps; empty for none
 * @param pulse the additive pulse over a submerged body, per pulse frame; empty for none
 * @param ripple the ripple train left on the sea at each contraction; empty for none
 * @param raft the raft's bob frames above the waterline; empty for none
 * @param raftSub the raft's plain under-water frame; empty for none
 * @param sink the gunless raft's sinking at the death; empty for none
 * @param recoil the gun's recoil frames, indexed like the unit's frames; empty for none
 * @param raftGun per raft frame, the gun's point, px from the frame's centre (x right, y up)
 * @param water the kill's burst on the water surface (its tier's rung)
 * @param under the kill's burst under the water (its tier's rung)
 */
public record NavalLooks(
        Array<AtlasRegion> sub,
        Array<AtlasRegion> surface,
        Array<AtlasRegion> pulse,
        Array<AtlasRegion> ripple,
        Array<AtlasRegion> raft,
        Array<AtlasRegion> raftSub,
        Array<AtlasRegion> sink,
        Array<AtlasRegion> recoil,
        float[][] raftGun,
        Array<AtlasRegion> water,
        Array<AtlasRegion> under) {
    /** A jelly's pulse frames (rest, contracting, contracted, relaxing) and the one its ripple starts at. */
    static final int PULSE_FRAMES = 4;

    static final int CONTRACTED = 2;
    /** The jelly's resting pulse: one contraction a second (4 frames a second). */
    static final double JELLY_FPS = 4;
    /** ... quickening to three a second as the ship closes in from {@value #QUICKEN_FROM} px to its ring's reach. */
    static final double JELLY_QUICK_FPS = 12;

    static final double QUICKEN_FROM = 180;
    /** The ring's reach at medium (design/enemies/naval/driftjelly: {@code within: 96}). */
    static final double RING_REACH = 96;
    /** A jelly's ripple train shows each frame this many steps (tools/art/driftjelly.py: 1.6 s). */
    static final int RIPPLE_FRAME_TICKS = 8;
    /** A raft bobs a frame every 12 steps (7.5 fps, one 1.6 s bob over 8 frames); it sinks 6 steps a frame. */
    static final int RAFT_FRAME_TICKS = 12;

    static final int SINK_FRAME_TICKS = 6;
    /** The gun shows its recoil frame this many steps after a fan. */
    static final int RECOIL_TICKS = 6;

    private static final Array<AtlasRegion> NONE = new Array<>(0);

    /** The looks of a unit without water frames: only its kills' water bursts. */
    static NavalLooks none(Sprites sprites, Tier tier) {
        return new NavalLooks(
                NONE,
                NONE,
                NONE,
                NONE,
                NONE,
                NONE,
                NONE,
                NONE,
                new float[0][],
                water(sprites, tier),
                under(sprites, tier));
    }

    /** A unit's water looks from its {@code slug}'s sprites, those it has (none for a unit off the water). */
    static NavalLooks of(Sprites sprites, String slug, Tier tier) {
        Array<AtlasRegion> raft = optional(sprites, slug + "-raft");
        return new NavalLooks(
                optional(sprites, slug + "-sub"),
                optional(sprites, slug + "-surface"),
                optional(sprites, slug + "-pulse"),
                optional(sprites, slug + "-ripple"),
                raft,
                optional(sprites, slug + "-raft-sub"),
                optional(sprites, slug + "-sink"),
                optional(sprites, slug + "-recoil"),
                raft.isEmpty() ? new float[0][] : raftGun(slug, raft),
                water(sprites, tier),
                under(sprites, tier));
    }

    private static Array<AtlasRegion> optional(Sprites sprites, String name) {
        return sprites.has(name) ? sprites.frames(name) : NONE;
    }

    /** The surface burst of a kill on the water for a tier: {@code explosion-water-<rung>}; empty without it. */
    private static Array<AtlasRegion> water(Sprites sprites, Tier tier) {
        return optional(sprites, "explosion-water-" + rung(tier));
    }

    private static Array<AtlasRegion> under(Sprites sprites, Tier tier) {
        return optional(sprites, "explosion-under-" + rung(tier));
    }

    /** The explosion ladder's rung of a size tier (a huge unit's death takes the large rung). */
    static String rung(Tier tier) {
        return switch (tier) {
            case TINY -> "tiny";
            case SMALL -> "small";
            case MEDIUM -> "medium";
            case LARGE, HUGE -> "large";
        };
    }

    /**
     * Per raft frame the gun's point (assets/pivots/{@code <slug>}.json, {@code raft_gun}: px from the
     * frame's top left), as an offset from the frame's centre (y up); the centre without the file.
     */
    private static float[][] raftGun(String slug, Array<AtlasRegion> raft) {
        float[][] points = new float[raft.size][2];
        FileHandle file = Gdx.files == null ? null : Gdx.files.internal("pivots/" + slug + ".json");
        if (file == null || !file.exists()) {
            return points;
        }
        JsonValue gun = new JsonReader().parse(file).get("raft_gun");
        for (int i = 0; gun != null && i < raft.size && i < gun.size; i++) {
            float[] at = gun.get(i).asFloatArray();
            points[i][0] = at[0] - raft.get(i).getRegionWidth() / 2f;
            points[i][1] = raft.get(i).getRegionHeight() / 2f - at[1];
        }
        return points;
    }

    /** Whether it is drawn in two layers: a plain body under the water as well as its surface frames. */
    boolean twoLayers() {
        return !sub.isEmpty() || !raftSub.isEmpty();
    }

    /** Whether it is a raft (its gun drawn over a bobbing raft). */
    boolean rafts() {
        return !raft.isEmpty();
    }

    /**
     * A jelly's pulse rate with the ship {@code distance} px away: {@value #JELLY_FPS} frames a second,
     * quickening evenly to {@value #JELLY_QUICK_FPS} from {@value #QUICKEN_FROM} px in to its ring's reach.
     */
    static double jellyFps(double distance) {
        double closing = (QUICKEN_FROM - distance) / (QUICKEN_FROM - RING_REACH);
        return JELLY_FPS + (JELLY_QUICK_FPS - JELLY_FPS) * Math.clamp(closing, 0, 1);
    }

    /**
     * The surfacing step of a swap {@code share} (0..1) through: the frames forward when it surfaces,
     * backward when it dives (tools/art/driftjelly.py: the layer flips at the middle).
     */
    static int swapFrame(double share, boolean diving, int frames) {
        int forward = Math.clamp((int) Math.floor(share * frames), 0, frames - 1);
        return diving ? frames - 1 - forward : forward;
    }

    /** A raft's bob frame at step {@code tick}, each raft at its own {@code phase}. */
    static int raftFrame(long tick, int phase, int frames) {
        return (int) Math.floorMod(tick / RAFT_FRAME_TICKS + phase, (long) frames);
    }
}
