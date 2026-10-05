package vanguard.game.render;

import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.utils.Array;
import java.util.List;
import vanguard.content.Content;
import vanguard.content.EnemyData;
import vanguard.content.Orientation;
import vanguard.content.Tier;
import vanguard.game.audio.Sfx;
import vanguard.sim.EnemySpec;
import vanguard.sim.SimStep;

/**
 * How an enemy looks and sounds: its animation, its explosion by size tier (design/art-direction,
 * explosion size ladder; design/audio/sfx, explosion families), its pause flare and its remains.
 *
 * @param frames the animation, or for a unit that turns to face its flight an angle set indexed
 *     {@code heading * phases + phase}, heading 0 flying down the screen and turning clockwise
 * @param headings the headings of the angle set, from the stat block's orientation; 1 when fixed
 * @param tilt whether the headings are a tilt set (-30 to +30 degrees around straight down)
 *     rather than a full turn
 * @param fps animation frames per second: 10 for the Vrell's organic motion; a radial spinner's
 *     frames cover one turn of its symmetry, so they play at its spin
 * @param flare drawn over a diver in its pause, additive; empty for none
 * @param remains left on the ground where a ground unit was destroyed, its frames spread over the
 *     remains' time (a stump, an acid splash decal); empty for none
 * @param deathGlow drawn additively with its explosion (a Vrell's spore cloud or glint); no frames for none
 * @param deathPieces solid pieces scattering where an air unit was destroyed (membrane tatters, a
 *     split husk), drawn under the glows; no frames for none
 * @param glow a walker's emissive back, drawn additively over its frame above the low-air layer
 *     (the Scuttler's lime back through the dust), indexed like {@code frames}; empty for none
 * @param husks a walker's remains, one frame per heading (the Scuttler's legless husk, left at its
 *     last heading like a turret's stump); empty for none
 * @param walkFrames a walker's frames per heading: one walk cycle; 1 for other units
 * @param stride a walker's ground distance per walk cycle, px
 * @param telegraphSeconds a spawner's telegraph: its pulse speeds up over these last seconds before it
 *     bursts on its own (the Brood Pod); 0 for none
 */
public record EnemyLooks(
        Array<AtlasRegion> frames,
        int headings,
        boolean tilt,
        double fps,
        Array<AtlasRegion> explosion,
        Sfx explosionA,
        Sfx explosionB,
        Array<AtlasRegion> flare,
        Array<AtlasRegion> remains,
        DeathEffect deathGlow,
        DeathEffect deathPieces,
        Array<AtlasRegion> glow,
        Array<AtlasRegion> husks,
        int walkFrames,
        double stride,
        double telegraphSeconds) {
    /** Vrell organic motion runs at 8-12 fps: 10 fps. */
    private static final double ORGANIC_FPS = 10;
    /**
     * A radial spinner's turns per second and the symmetry its frames cover: the Whirl Seed, the only
     * one yet, spins 1.5 turns/s and its 8 frames cover 60 degrees of its six-fold body
     * (design/enemies/air/whirl-seed), so they play at 1.5 x 6 x 8 = 72 frames/s.
     */
    private static final double SPIN_TURNS_PER_SECOND = 1.5;

    private static final int SPIN_SYMMETRY = 6;
    /** The tilt set's step between two headings. */
    private static final double TILT_STEP = Math.toRadians(10);
    /**
     * A segment chain's production sprites (tools/art/coilwyrm.py) turn in 48 headings, finer than
     * the stat block's 16, so the chain's members on a curve turn smoothly.
     */
    static final int CHAIN_HEADINGS = 48;

    public EnemyLooks {
        if (frames.size % headings != 0) {
            throw new IllegalArgumentException(
                    frames.size + " frames do not split into " + headings + " headings of equal animations");
        }
    }

    /** The looks of the level's enemy kinds, indexed like {@code Sortie.enemyKinds()}. */
    public static EnemyLooks[] of(List<EnemySpec> kinds, Sprites sprites, Content content) {
        return kinds.stream().map(kind -> of(kind.slug(), sprites, content)).toArray(EnemyLooks[]::new);
    }

    /**
     * A kind's looks: a segment chain's parts ({@code coilwyrm-segment}, {@code -tail},
     * {@code -regrown}) take their unit's stat block, its segments and tail popping as {@code tiny}
     * units; a unit without frames yet gets placeholder shapes.
     */
    private static EnemyLooks of(String slug, Sprites sprites, Content content) {
        String unit = slug;
        while (!content.enemies().containsKey(unit) && unit.contains("-")) {
            unit = unit.substring(0, unit.lastIndexOf('-'));
        }
        EnemyData data = content.enemy(unit);
        boolean part = !unit.equals(slug) && !slug.endsWith("-regrown");
        return of(slug, sprites, data, part);
    }

    /**
     * A death animation started with the unit's explosion, centred on the unit.
     *
     * @param ticksPerFrame simulation steps each frame shows
     * @param delayTicks steps after the explosion's start that it shows
     */
    public record DeathEffect(Array<AtlasRegion> frames, int ticksPerFrame, int delayTicks) {}

    /**
     * A death animation's sprite names: the glow additive ({@code -death}, or the Brood Pod's wet
     * {@code -burst}), the pieces solid. A walker's {@code -husk} is not a death animation but its
     * remains, one frame per heading ({@link #husks}).
     */
    private static final List<String> DEATH_GLOW = List.of("-death", "-burst");

    private static final List<String> DEATH_PIECES = List.of("-tatters", "-husk");
    /** How much faster a spawner's pulse plays at the end of its telegraph than before it: ×3. */
    private static final double TELEGRAPH_SPEED_UP = 2;
    /**
     * A tiny unit's death frames show 2 steps each, a larger one's 4 (the art track's sets: a Whirl
     * Seed's husk and glint, a Spore Bomber's tatters and cloud); a tiny unit's glow shows 4 steps
     * after its pop, so the glint follows the flash.
     */
    private static final int TINY_DEATH_FRAME_TICKS = 2;

    private static final int DEATH_FRAME_TICKS = 4;
    private static final int TINY_GLOW_DELAY_TICKS = 4;
    /** A splash decal's frame for each second of the remains' 10 s. */
    private static final int[] SPLASH_TIMELINE = {0, 1, 1, 1, 1, 1, 1, 1, 2, 2};

    /**
     * A unit's frames are its slug's on the sprite pages; its explosion follows its size tier; its
     * death animations are its slug's {@code -death} (glow) and {@code -tatters} or {@code -husk}
     * (pieces), played at its tier's frame rate.
     */
    private static EnemyLooks of(String slug, Sprites sprites, EnemyData data, boolean tinyPart) {
        Orientation orientation = data.orientation();
        Array<AtlasRegion> none = new Array<>();
        boolean walker = data.movement().walk().isPresent();
        int walkFrames = walker ? sprites.frames(slug).size / orientation.headings() : 1;
        Array<AtlasRegion> frames =
                sprites.has(slug) ? sprites.frames(slug) : Placeholders.frames(slug, orientation.headings());
        int headings = data.segmentChain().isPresent() && sprites.has(slug) ? CHAIN_HEADINGS : orientation.headings();
        Tier tier = tinyPart ? Tier.TINY : data.tier();
        // A segment chain (the Coilwyrm, the only one) bursts wet: its segments and tail with the
        // segment burst (shot or in the chained death's ripple), its heads with the deeper one.
        boolean chain = data.segmentChain().isPresent();
        Sfx chainBurst = tinyPart ? Sfx.COILWYRM_BURST : Sfx.COILWYRM_HEAD_BURST;
        return new EnemyLooks(
                frames,
                headings,
                orientation == Orientation.TILT_30,
                orientation == Orientation.RADIAL ? SPIN_TURNS_PER_SECOND * SPIN_SYMMETRY * frames.size : ORGANIC_FPS,
                switch (tier) {
                    case TINY -> sprites.explosionTiny;
                    case SMALL -> sprites.explosionSmall;
                    default -> sprites.frames("explosion-medium");
                },
                chain ? chainBurst : tier == Tier.TINY ? Sfx.EXPLOSION_TINY_A : Sfx.EXPLOSION_SMALL_A,
                chain ? chainBurst : tier == Tier.TINY ? Sfx.EXPLOSION_TINY_B : Sfx.EXPLOSION_SMALL_B,
                sprites.has(slug + "-flare") ? sprites.frames(slug + "-flare") : none,
                remains(sprites, slug, none),
                death(sprites, slug, DEATH_GLOW, tier, true, walker),
                death(sprites, slug, DEATH_PIECES, tier, false, walker),
                sprites.has(slug + "-glow") ? sprites.frames(slug + "-glow") : none,
                walker && sprites.has(slug + "-husk") ? sprites.frames(slug + "-husk") : none,
                walkFrames,
                data.movement().walk().map(EnemyData.Walk::stride).orElse(1.0),
                data.attacks().stream()
                        .flatMap(attack -> attack.spawn().stream())
                        .mapToDouble(EnemyData.Spawn::telegraph)
                        .findFirst()
                        .orElse(0));
    }

    /**
     * A ground unit's remains, together shown for {@code REMAINS} (10 s, LevelScreen): a turret's
     * {@code -stump}, or a decal's {@code -splash} frames (fresh, after 1 s, fading) laid out on a
     * 1 s timeline: fresh 1 s, then 7 s, fading over the last 2 s (the Polyp Mortar's acid splash).
     */
    private static Array<AtlasRegion> remains(Sprites sprites, String slug, Array<AtlasRegion> none) {
        if (sprites.has(slug + "-stump")) {
            return sprites.frames(slug + "-stump");
        }
        if (!sprites.has(slug + "-splash")) {
            return none;
        }
        Array<AtlasRegion> splash = sprites.frames(slug + "-splash");
        Array<AtlasRegion> timeline = new Array<>();
        for (int i : SPLASH_TIMELINE) {
            timeline.add(splash.get(Math.min(i, splash.size - 1)));
        }
        return timeline;
    }

    private static DeathEffect death(
            Sprites sprites, String slug, List<String> suffixes, Tier tier, boolean glow, boolean walker) {
        boolean tiny = tier == Tier.TINY;
        Array<AtlasRegion> frames = suffixes.stream()
                .filter(suffix -> !(walker && suffix.equals("-husk")))
                .filter(suffix -> sprites.has(slug + suffix))
                .findFirst()
                .map(suffix -> sprites.frames(slug + suffix))
                .orElseGet(Array::new);
        return new DeathEffect(
                frames, tiny ? TINY_DEATH_FRAME_TICKS : DEATH_FRAME_TICKS, tiny && glow ? TINY_GLOW_DELAY_TICKS : 0);
    }

    /** The animation step at simulation step {@code tick} for the unit at {@code phase} of its cycle. */
    public long step(long tick, int phase) {
        return (long) Math.floor(tick * fps / SimStep.PER_SECOND) + phase;
    }

    /**
     * A spawner's animation step with its telegraph (the Brood Pod's pulse): over the last
     * {@link #telegraphSeconds} before it bursts on its own its pulse speeds up evenly to three
     * times its rate, so the frames it gained are the integral of the extra rate.
     *
     * @param burstSeconds the seconds left before it bursts ({@code Enemy.burstSeconds()})
     */
    public long step(long tick, int phase, double burstSeconds) {
        double into = telegraphSeconds - burstSeconds;
        if (telegraphSeconds <= 0 || into <= 0) {
            return step(tick, phase);
        }
        double gained = fps * TELEGRAPH_SPEED_UP * into * into / (2 * telegraphSeconds);
        return (long) Math.floor(tick * fps / SimStep.PER_SECOND + gained) + phase;
    }

    /**
     * A walker's frame: the heading nearest {@code facing} at the walk frame its distance walked
     * reaches, one cycle per stride.
     */
    public int walkFrame(double facing, double walked) {
        int frame = (int) Math.floorMod((long) Math.floor(walked / stride * walkFrames), (long) walkFrames);
        return heading(facing, headings) * walkFrames + frame;
    }

    /** A walker's husk at its last heading, as a one-frame animation; empty without husks. */
    public Array<AtlasRegion> husk(double facing) {
        if (husks.isEmpty()) {
            return husks;
        }
        Array<AtlasRegion> one = new Array<>(1);
        one.add(husks.get(heading(facing, husks.size)));
        return one;
    }

    /**
     * The frame to draw: the heading nearest {@code facing} (radians clockwise from straight down),
     * at animation frame {@code step} of its cycle; a tilt set clamps the facing to its range.
     */
    public AtlasRegion frame(double facing, long step) {
        int phases = frames.size / headings;
        int heading = tilt ? tiltHeading(facing, headings) : heading(facing, headings);
        return frames.get(heading * phases + (int) (step % phases));
    }

    /**
     * The glow frame drawn over {@link #frame}: the same heading and animation frame of the
     * {@code -glow} set, which is indexed like the frames (a turret's or mortar's emissive parts,
     * drawn over the darkness of Level 06).
     */
    public AtlasRegion glowFrame(double facing, long step) {
        int phases = glow.size / headings;
        int heading = tilt ? tiltHeading(facing, headings) : heading(facing, headings);
        return glow.get(heading * phases + (int) (step % phases));
    }

    /** The index of the heading nearest {@code facing} in a set of {@code headings}. */
    static int heading(double facing, int headings) {
        return (int) Math.floorMod(Math.round(facing * headings / (2 * Math.PI)), (long) headings);
    }

    /** The index in a tilt set of {@code headings} 10 degrees apart, centred on straight down. */
    static int tiltHeading(double facing, int headings) {
        int middle = headings / 2;
        double wrapped = Math.IEEEremainder(facing, 2 * Math.PI);
        return Math.clamp(Math.round(wrapped / TILT_STEP) + middle, 0, headings - 1);
    }
}
