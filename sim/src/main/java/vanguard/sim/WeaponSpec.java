package vanguard.sim;

import java.util.List;

/**
 * One fitted weapon at one upgrade level, from design/player/weapons/&lt;slug&gt;/data.yaml (built
 * by {@code vanguard.content.SimSpecs}): how often it fires, what each projectile does, and where
 * the projectiles of a volley leave the ship.
 *
 * @param slug the weapon's directory name, e.g. {@code scatter-vulcan}
 * @param vfx its projectile family (design/player/weapons, concept round 08), for the presentation
 * @param sfx its sound family (design/audio/sfx), for the presentation
 * @param delivery how its projectiles travel, which decides the layers they reach
 * @param antiGround whether it has the {@code anti-ground} trait: it damages hardened ground
 *     targets, and a {@link Delivery#BOLT} or {@link Delivery#HOMING} weapon does double damage on
 *     the ground layer (a ground-only weapon's damage already includes it)
 * @param rate volleys per second
 * @param damage damage per projectile
 * @param speed px/s; 0 for dropped and lobbed projectiles
 * @param size a projectile's hit box
 * @param range px a bolt flies before it is gone ({@link Double#POSITIVE_INFINITY}: until it leaves
 *     the screen); a homing weapon's seek radius; how far ahead of the muzzle a lobbed shell lands
 * @param lifetimeSeconds how long a homing projectile flies
 * @param pierce how many targets a bolt passes through before it is gone (1: the first stops it)
 * @param blast the blast radius of a dropped or lobbed projectile, px
 * @param turnRate a homing projectile's turn rate, radians per second
 * @param coneHalfAngle a homing projectile picks targets within this angle either side of its heading
 * @param airSeconds how long a dropped bomb falls or a lobbed shell flies
 * @param snap a lobbed shell lands on the nearest destructible ground target this close to its
 *     landing point, px
 * @param muzzles one per projectile of a volley
 */
public record WeaponSpec(
        String slug,
        String vfx,
        String sfx,
        Delivery delivery,
        boolean antiGround,
        double rate,
        double damage,
        double speed,
        Hitbox size,
        double range,
        double lifetimeSeconds,
        int pierce,
        double blast,
        double turnRate,
        double coneHalfAngle,
        double airSeconds,
        double snap,
        List<Muzzle> muzzles) {
    public WeaponSpec {
        muzzles = List.copyOf(muzzles);
        if (muzzles.isEmpty()) {
            throw new IllegalArgumentException("a volley has at least one projectile");
        }
        if (!(rate > 0) || pierce < 1) {
            throw new IllegalArgumentException(slug + ": the rate must be > 0 and the pierce at least 1");
        }
    }

    /**
     * Where a projectile leaves the ship and where it heads.
     *
     * @param dx px right of the ship's centre
     * @param dy px above the ship's centre
     * @param angle radians clockwise from straight up the screen (pi / 2 = right)
     */
    public record Muzzle(double dx, double dy, double angle) {}

    /** How projectiles travel (design/enemies, layer rules). */
    public enum Delivery {
        /** Flies straight; hits {@code air}, {@code low-air} and {@code ground}. */
        BOLT,
        /** Seeks the nearest enemy in its cone; hits every layer a bolt does and {@code high-air}. */
        HOMING,
        /** Falls onto the ground below its release point; hits only the ground layer, in a blast. */
        DROPPED,
        /** Lobbed onto the ground ahead; hits only the ground layer, in a blast. */
        LOBBED;

        /** Whether its projectiles hit enemies on {@code layer} on their way (blasts aside). */
        public boolean reaches(Layer layer) {
            return switch (this) {
                case BOLT -> layer.hitByStandardShots();
                case HOMING -> true;
                case DROPPED, LOBBED -> false;
            };
        }

        /** Whether its projectiles land and burst on the ground rather than fly through. */
        public boolean landing() {
            return this == DROPPED || this == LOBBED;
        }
    }

    int intervalTicks() {
        return Math.max(1, SimStep.ticks(1 / rate));
    }
}
