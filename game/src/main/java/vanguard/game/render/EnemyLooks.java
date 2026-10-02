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

/**
 * How an enemy looks and sounds: its animation, its explosion by size tier (design/art-direction,
 * explosion size ladder; design/audio/sfx, explosion families), its pause flare and its remains.
 *
 * @param frames the animation, or for a unit that turns to face its flight an angle set indexed
 *     {@code heading * phases + phase}, heading 0 flying down the screen and turning clockwise
 * @param headings the headings of the angle set, from the stat block's orientation; 1 when fixed
 * @param tilt whether the headings are a tilt set (-30 to +30 degrees around straight down)
 *     rather than a full turn
 * @param frameTicks simulation steps per animation frame
 * @param flare drawn over a diver in its pause, additive; empty for none
 * @param remains left on the ground where a ground unit was destroyed; empty for none
 */
public record EnemyLooks(
        Array<AtlasRegion> frames,
        int headings,
        boolean tilt,
        int frameTicks,
        Array<AtlasRegion> explosion,
        Sfx explosionA,
        Sfx explosionB,
        Array<AtlasRegion> flare,
        Array<AtlasRegion> remains) {
    /** Vrell organic motion runs at 8-12 fps: 10 fps. */
    private static final int ORGANIC_FRAME_TICKS = 6;
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

    /** A unit's frames are its slug's on the sprite pages; its explosion follows its size tier. */
    private static EnemyLooks of(String slug, Sprites sprites, EnemyData data) {
        Orientation orientation = data.orientation();
        Array<AtlasRegion> none = new Array<>();
        return new EnemyLooks(
                sprites.frames(slug),
                orientation.headings(),
                orientation == Orientation.TILT_30,
                ORGANIC_FRAME_TICKS,
                switch (data.tier()) {
                    case TINY -> sprites.explosionTiny;
                    case SMALL -> sprites.explosionSmall;
                    default -> sprites.frames("explosion-medium");
                },
                data.tier() == Tier.TINY ? Sfx.EXPLOSION_TINY_A : Sfx.EXPLOSION_SMALL_A,
                data.tier() == Tier.TINY ? Sfx.EXPLOSION_TINY_B : Sfx.EXPLOSION_SMALL_B,
                sprites.has(slug + "-flare") ? sprites.frames(slug + "-flare") : none,
                sprites.has(slug + "-stump") ? sprites.frames(slug + "-stump") : none);
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
