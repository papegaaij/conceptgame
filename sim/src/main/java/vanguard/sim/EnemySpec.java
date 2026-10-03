package vanguard.sim;

import java.util.Optional;

/**
 * An enemy's stat block (design/enemies/&lt;category&gt;/&lt;slug&gt;/data.yaml) at one
 * difficulty, built by {@code vanguard.content.SimSpecs}: the HP already scaled, the movement
 * numbers its formations use, its gun and its drop rule.
 *
 * @param slug its name in the design data, e.g. {@code needler}
 * @param hp hit points in damage units
 * @param hitbox the hit box (the sprite is larger)
 * @param layer the layer it flies on
 * @param contactDamage damage of ramming the player
 * @param destroyedByRamming whether ramming the ship destroys it ({@code tiny} and {@code small} units)
 * @param bounty credits at medium in Act 1 terms
 * @param speed its own speed in px/s
 * @param snake the gap between two units of a snake
 * @param streamSpeed its speed in streams
 * @param hover how long and how far below the top edge it hovers
 * @param orbit its orbit in circle formations
 * @param gun its attack; none for rammers
 * @param drop the pickup every n-th kill drops
 * @param dive how it dives, for a diver
 * @param terrain whether it is fixed to the ground layer and moves only with the scroll
 * @param spiral how it spirals out of a whirl cluster
 * @param strafe how far below the top edge a convoy unit turns across the screen
 * @param deathBurst the puff of bullets it pops into when destroyed; none for most
 */
public record EnemySpec(
        String slug,
        double hp,
        Hitbox hitbox,
        Layer layer,
        double contactDamage,
        boolean destroyedByRamming,
        int bounty,
        double speed,
        Optional<Snake> snake,
        Optional<Double> streamSpeed,
        Optional<Hover> hover,
        Optional<Orbit> orbit,
        Optional<EnemyGun> gun,
        Optional<Drop> drop,
        Optional<Dive> dive,
        boolean terrain,
        Optional<Spiral> spiral,
        Optional<Range> strafe,
        Optional<DeathBurst> deathBurst) {

    /** A snake's units follow one another {@code spacingSeconds} apart. */
    public record Snake(double spacingSeconds) {}

    /** Hovers for {@code seconds}, {@code depth} px below the top edge of the play field. */
    public record Hover(Range seconds, Range depth) {}

    /** Circles a point at {@code radius} px and {@code degreesPerSecond}. */
    public record Orbit(double radius, double degreesPerSecond) {}

    /** Every {@code every}-th kill of this enemy in an attempt drops {@code pickup}. */
    public record Drop(PickupType pickup, int every) {}

    /**
     * Pauses {@code depth} px below the top edge for {@code pauseSeconds}, then dives at
     * {@code speed} px/s at where the ship was, firing once when it passes the ship's height or
     * {@code fireAfterSeconds} into the dive.
     */
    public record Dive(Range depth, double pauseSeconds, double speed, double fireAfterSeconds) {}

    /**
     * Circles its release point for {@code seconds} at {@code turnsPerSecond} while its radius grows
     * {@code growth} px/s and the release point drifts down {@code drift} px/s; then flies on at its
     * speed along its outward angle turned downward, bouncing off the side edges up to
     * {@code ricochets} times.
     */
    public record Spiral(double seconds, double turnsPerSecond, double growth, double drift, int ricochets) {}

    /** {@code count} bullets in a ring at {@code speed} px/s, each dealing {@code damage}. */
    public record DeathBurst(int count, double speed, double damage) {}

    /** The current constructor with the movements and bursts of Level 03 left out. */
    public EnemySpec(
            String slug,
            double hp,
            Hitbox hitbox,
            Layer layer,
            double contactDamage,
            boolean destroyedByRamming,
            int bounty,
            double speed,
            Optional<Snake> snake,
            Optional<Double> streamSpeed,
            Optional<Hover> hover,
            Optional<Orbit> orbit,
            Optional<EnemyGun> gun,
            Optional<Drop> drop,
            Optional<Dive> dive,
            boolean terrain) {
        this(
                slug,
                hp,
                hitbox,
                layer,
                contactDamage,
                destroyedByRamming,
                bounty,
                speed,
                snake,
                streamSpeed,
                hover,
                orbit,
                gun,
                drop,
                dive,
                terrain,
                Optional.empty(),
                Optional.empty(),
                Optional.empty());
    }
}
