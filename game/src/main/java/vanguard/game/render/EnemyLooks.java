package vanguard.game.render;

import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.utils.Array;
import java.util.List;
import vanguard.game.audio.Sfx;
import vanguard.sim.EnemySpec;

/**
 * How an enemy looks and sounds: its animation, its explosion by size tier (design/art-direction,
 * explosion size ladder; design/audio/sfx, explosion families) and its hit sound.
 *
 * @param frameTicks simulation steps per animation frame
 */
public record EnemyLooks(
        Array<AtlasRegion> frames, int frameTicks, Array<AtlasRegion> explosion, Sfx explosionA, Sfx explosionB) {
    /** Vrell organic motion runs at 8-12 fps: 10 fps. */
    private static final int ORGANIC_FRAME_TICKS = 6;

    /** The looks of the level's enemy kinds, indexed like {@code Sortie.enemyKinds()}. */
    public static EnemyLooks[] of(List<EnemySpec> kinds, Sprites sprites) {
        return kinds.stream().map(kind -> of(kind.slug(), sprites)).toArray(EnemyLooks[]::new);
    }

    private static EnemyLooks of(String slug, Sprites sprites) {
        return switch (slug) {
            case "skitter" ->
                new EnemyLooks(
                        sprites.skitter,
                        ORGANIC_FRAME_TICKS,
                        sprites.explosionTiny,
                        Sfx.EXPLOSION_TINY_A,
                        Sfx.EXPLOSION_TINY_B);
            case "needler" ->
                new EnemyLooks(
                        sprites.needler,
                        ORGANIC_FRAME_TICKS,
                        sprites.explosionSmall,
                        Sfx.EXPLOSION_SMALL_A,
                        Sfx.EXPLOSION_SMALL_B);
            default -> throw new IllegalArgumentException("no sprites for the enemy '" + slug + "'");
        };
    }
}
