package vanguard.game.render;

import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.utils.Array;
import vanguard.sim.EnemyBullet;

/**
 * Which sprite an enemy bullet is drawn with (design/enemies, bullet readability rules: shape
 * encodes threat): the standard orb for {@code small} bullets (tools/art/enemy_bullets.py) and the
 * large pulsing orb for {@code medium} ones (tools/art/bullet_medium.py; the Leviathan's and the
 * Scuttler's aimed shots), told apart by the damage the bullet class gives them. The Spore Bomber's
 * {@code medium} is its spore, drawn as the mine. The yellow needle is the fast speed class's look;
 * no Act 1 attack is in that class, so it has no user yet.
 */
public final class BulletLooks {
    /**
     * The {@code medium} bullet class's damage (design/enemies/data.yaml, player damage; checked
     * against the data by a test): a bullet that deals this much or more is a large orb, and its
     * shot sounds heavy ({@code FlightSounds}).
     */
    public static final double MEDIUM_DAMAGE = 6;
    /** The small orb pulses its core at 15 fps, the large orb at 10 fps; each bullet at its own phase. */
    private static final int SMALL_FRAME_TICKS = 4;

    private static final int MEDIUM_FRAME_TICKS = 6;

    private final Array<AtlasRegion> small;
    private final Array<AtlasRegion> medium;

    BulletLooks(Sprites sprites) {
        small = sprites.orb;
        medium = sprites.frames("orb-medium");
    }

    /** The frame of bullet number {@code index} at step {@code tick}. */
    AtlasRegion frame(EnemyBullet bullet, int index, long tick) {
        if (bullet.damage() >= MEDIUM_DAMAGE) {
            return medium.get((int) ((tick / MEDIUM_FRAME_TICKS + index) % medium.size));
        }
        return small.get((int) ((tick / SMALL_FRAME_TICKS + index) % small.size));
    }
}
