package vanguard.game.render;

import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.utils.Array;
import java.util.List;
import vanguard.content.Content;
import vanguard.game.audio.Sfx;
import vanguard.sim.EnemySpec;

/**
 * How an enemy looks and sounds: its animation, its explosion by size tier (design/art-direction,
 * explosion size ladder; design/audio/sfx, explosion families) and its hit sound.
 *
 * @param frames the animation, or for a unit that turns to face its flight an angle set indexed
 *     {@code heading * phases + phase}, heading 0 flying down the screen and turning clockwise
 * @param headings the headings of the angle set, from the stat block's orientation; 1 when fixed
 * @param frameTicks simulation steps per animation frame
 */
public record EnemyLooks(
        Array<AtlasRegion> frames,
        int headings,
        int frameTicks,
        Array<AtlasRegion> explosion,
        Sfx explosionA,
        Sfx explosionB) {
    /** Vrell organic motion runs at 8-12 fps: 10 fps. */
    private static final int ORGANIC_FRAME_TICKS = 6;

    public EnemyLooks {
        if (frames.size % headings != 0) {
            throw new IllegalArgumentException(
                    frames.size + " frames do not split into " + headings + " headings of equal animations");
        }
    }

    /** The looks of the level's enemy kinds, indexed like {@code Sortie.enemyKinds()}. */
    public static EnemyLooks[] of(List<EnemySpec> kinds, Sprites sprites, Content content) {
        return kinds.stream()
                .map(kind -> of(
                        kind.slug(),
                        sprites,
                        content.enemy(kind.slug()).orientation().headings()))
                .toArray(EnemyLooks[]::new);
    }

    private static EnemyLooks of(String slug, Sprites sprites, int headings) {
        return switch (slug) {
            case "skitter" ->
                new EnemyLooks(
                        sprites.skitter,
                        headings,
                        ORGANIC_FRAME_TICKS,
                        sprites.explosionTiny,
                        Sfx.EXPLOSION_TINY_A,
                        Sfx.EXPLOSION_TINY_B);
            case "needler" ->
                new EnemyLooks(
                        sprites.needler,
                        headings,
                        ORGANIC_FRAME_TICKS,
                        sprites.explosionSmall,
                        Sfx.EXPLOSION_SMALL_A,
                        Sfx.EXPLOSION_SMALL_B);
            default -> throw new IllegalArgumentException("no sprites for the enemy '" + slug + "'");
        };
    }

    /**
     * The frame to draw: the heading nearest {@code facing} (radians clockwise from straight down),
     * at animation frame {@code step} of its cycle.
     */
    public AtlasRegion frame(double facing, long step) {
        int phases = frames.size / headings;
        return frames.get(heading(facing, headings) * phases + (int) (step % phases));
    }

    /** The index of the heading nearest {@code facing} in a set of {@code headings}. */
    static int heading(double facing, int headings) {
        return (int) Math.floorMod(Math.round(facing * headings / (2 * Math.PI)), (long) headings);
    }
}
