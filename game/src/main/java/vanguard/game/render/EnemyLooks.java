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
 * @param remains left on the ground where a ground unit was destroyed; empty for none
 * @param deathGlow drawn additively with its explosion (a Vrell's spore cloud or glint); no frames for none
 * @param deathPieces solid pieces scattering where an air unit was destroyed (membrane tatters, a
 *     split husk), drawn under the glows; no frames for none
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
        DeathEffect deathPieces) {
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

    public EnemyLooks {
        if (frames.size % headings != 0) {
            throw new IllegalArgumentException(
                    frames.size + " frames do not split into " + headings + " headings of equal animations");
        }
    }

    /** The looks of the level's enemy kinds, indexed like {@code Sortie.enemyKinds()}. */
    public static EnemyLooks[] of(List<EnemySpec> kinds, Sprites sprites, Content content) {
        return kinds.stream()
                .map(kind -> of(kind.slug(), sprites, content.enemy(kind.slug())))
                .toArray(EnemyLooks[]::new);
    }

    /**
     * A death animation started with the unit's explosion, centred on the unit.
     *
     * @param ticksPerFrame simulation steps each frame shows
     * @param delayTicks steps after the explosion's start that it shows
     */
    public record DeathEffect(Array<AtlasRegion> frames, int ticksPerFrame, int delayTicks) {}

    /** A death animation's sprite names: the glow additive, the pieces solid. */
    private static final List<String> DEATH_GLOW = List.of("-death");

    private static final List<String> DEATH_PIECES = List.of("-tatters", "-husk");
    /**
     * A tiny unit's death frames show 2 steps each, a larger one's 4 (the art track's sets: a Whirl
     * Seed's husk and glint, a Spore Bomber's tatters and cloud); a tiny unit's glow shows 4 steps
     * after its pop, so the glint follows the flash.
     */
    private static final int TINY_DEATH_FRAME_TICKS = 2;

    private static final int DEATH_FRAME_TICKS = 4;
    private static final int TINY_GLOW_DELAY_TICKS = 4;

    /**
     * A unit's frames are its slug's on the sprite pages; its explosion follows its size tier; its
     * death animations are its slug's {@code -death} (glow) and {@code -tatters} or {@code -husk}
     * (pieces), played at its tier's frame rate.
     */
    private static EnemyLooks of(String slug, Sprites sprites, EnemyData data) {
        Orientation orientation = data.orientation();
        Array<AtlasRegion> none = new Array<>();
        return new EnemyLooks(
                sprites.frames(slug),
                orientation.headings(),
                orientation == Orientation.TILT_30,
                orientation == Orientation.RADIAL
                        ? SPIN_TURNS_PER_SECOND * SPIN_SYMMETRY * sprites.frames(slug).size
                        : ORGANIC_FPS,
                switch (data.tier()) {
                    case TINY -> sprites.explosionTiny;
                    case SMALL -> sprites.explosionSmall;
                    default -> sprites.frames("explosion-medium");
                },
                data.tier() == Tier.TINY ? Sfx.EXPLOSION_TINY_A : Sfx.EXPLOSION_SMALL_A,
                data.tier() == Tier.TINY ? Sfx.EXPLOSION_TINY_B : Sfx.EXPLOSION_SMALL_B,
                sprites.has(slug + "-flare") ? sprites.frames(slug + "-flare") : none,
                sprites.has(slug + "-stump") ? sprites.frames(slug + "-stump") : none,
                death(sprites, slug, DEATH_GLOW, data.tier(), true),
                death(sprites, slug, DEATH_PIECES, data.tier(), false));
    }

    private static DeathEffect death(Sprites sprites, String slug, List<String> suffixes, Tier tier, boolean glow) {
        boolean tiny = tier == Tier.TINY;
        Array<AtlasRegion> frames = suffixes.stream()
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
     * The frame to draw: the heading nearest {@code facing} (radians clockwise from straight down),
     * at animation frame {@code step} of its cycle; a tilt set clamps the facing to its range.
     */
    public AtlasRegion frame(double facing, long step) {
        int phases = frames.size / headings;
        int heading = tilt ? tiltHeading(facing, headings) : heading(facing, headings);
        return frames.get(heading * phases + (int) (step % phases));
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
