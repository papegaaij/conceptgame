package vanguard.content;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.util.List;
import java.util.Optional;

/**
 * design/player/weapons/&lt;slug&gt;/data.yaml: one weapon with its five upgrade levels and the
 * overdrive pattern.
 *
 * @param price the purchase price; 0 for the starter
 * @param upgradeBase what the upgrade cost factors multiply; the price when absent
 * @param unlock the level from whose hangar visit the weapon is in the shop
 * @param draw the power draw at L1 and L5 in MW
 * @param hits which targets it can hit: {@code standard}, {@code homing}, {@code ground-only}, …
 * @param speed the projectile speed in px/s; none for lobbed and dropped projectiles
 * @param size the projectile's hit box
 * @param range how far the projectiles fly; none for mines, which have a lifetime instead; a
 *     homing weapon's seek radius
 * @param lifetime how long a projectile lives, in seconds
 * @param converge degrees a pod's shots turn in towards the ship's centre line
 * @param fall seconds a dropped bomb falls to the ground
 * @param flight seconds a lobbed shell flies to its landing point
 * @param snap how far from its landing point a lobbed shell finds a ground target, px
 * @param cone the angle ahead in which a homing shot picks its target, degrees
 * @param mirrored fires the pattern to the left too (numbers per side)
 * @param pod a wing pod (numbers per pod)
 * @param seek homing, lobbed or dropped: every projectile counts as a single-target hit
 */
public record WeaponData(
        String name,
        Slot slot,
        List<String> traits,
        int price,
        Optional<Integer> upgradeBase,
        int unlock,
        Span draw,
        String sfx,
        String vfx,
        String hits,
        Optional<Speed> speed,
        Size size,
        Optional<Range> range,
        Optional<Double> lifetime,
        Optional<Double> converge,
        Optional<Double> fall,
        Optional<Double> flight,
        Optional<Double> snap,
        Optional<Double> cone,
        Optional<Boolean> mirrored,
        Optional<Boolean> pod,
        Optional<Boolean> seek,
        List<Level> levels,
        Level overdrive) {
    public WeaponData {
        Check.notNegative("price", price);
        Check.positive("unlock", unlock);
        Check.that(levels.size() == 5, "levels: one entry for each of L1–L5, found " + levels.size());
        Check.that(range.isPresent() || lifetime.isPresent(), "range or lifetime is required");
    }

    public enum Slot {
        FRONT,
        REAR,
        WING
    }

    /** Projectile speed in px/s, written as one number or {@code [start, end]} for accelerating shots. */
    public record Speed(double start, double end) {
        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        static Speed of(Object value) {
            if (value instanceof Number n) {
                return new Speed(n.doubleValue(), n.doubleValue());
            }
            if (value instanceof List<?> list
                    && list.size() == 2
                    && list.get(0) instanceof Number start
                    && list.get(1) instanceof Number end) {
                return new Speed(start.doubleValue(), end.doubleValue());
            }
            throw new IllegalArgumentException("speed must be px/s or [start, end], was " + value);
        }
    }

    /** How far a weapon's projectiles fly: to the screen edge, straight down, or a distance. */
    public record Range(Kind kind, double px) {
        public enum Kind {
            SCREEN,
            DROP,
            DISTANCE
        }

        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        static Range of(Object value) {
            return switch (value) {
                case Number n -> new Range(Kind.DISTANCE, Check.positive("range", n.doubleValue()));
                case String s when s.equals("screen") -> new Range(Kind.SCREEN, 0);
                case String s when s.equals("drop") -> new Range(Kind.DROP, 0);
                default -> throw new IllegalArgumentException("range must be screen, drop or px, was " + value);
            };
        }
    }

    /**
     * One upgrade level.
     *
     * @param pattern one shot per projectile of a volley
     * @param rate volleys per second
     * @param damage damage per projectile
     * @param blast blast radius in px
     * @param turn homing turn rate in °/s
     * @param maxLive most mines alive at once
     */
    public record Level(
            List<Shot> pattern,
            double rate,
            double damage,
            Optional<Integer> pierce,
            Optional<Double> blast,
            Optional<Double> turn,
            Optional<Integer> maxLive) {
        public Level {
            Check.notEmpty("pattern", pattern);
            Check.positive("rate", rate);
            Check.positive("damage", damage);
        }
    }

    /**
     * A projectile of a volley, written {@code [x, angle]}: x offset from the muzzle in px, angle in
     * degrees (0 = straight up the screen, 90 = right, 180 = straight back).
     */
    @JsonFormat(shape = JsonFormat.Shape.ARRAY)
    public record Shot(double x, double angle) {}
}
