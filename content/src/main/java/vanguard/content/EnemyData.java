package vanguard.content;

import java.util.List;
import java.util.Optional;

/**
 * design/enemies/&lt;category&gt;/&lt;slug&gt;/data.yaml: an enemy's stat block at medium
 * (design/enemies/README.md, Stat block template).
 *
 * @param hp hit points in damage units
 * @param speed the default speed in px/s; movement patterns may set their own
 * @param bounty credits at medium in Act 1 terms
 * @param firstLevel the level it is introduced in
 */
public record EnemyData(
        String name,
        String faction,
        String layer,
        Tier tier,
        Size size,
        Size hitbox,
        String parts,
        String orientation,
        double hp,
        String armour,
        double speed,
        Movement movement,
        List<Attack> attacks,
        List<FormationUse> formations,
        List<WeakPoint> weakPoints,
        List<String> traits,
        int bounty,
        int firstLevel,
        Optional<Hooks> difficulty) {
    public EnemyData {
        Layers.of(layer);
        Check.positive("hp", hp);
        Check.notNegative("speed", speed);
        Check.notNegative("bounty", bounty);
        Check.positive("first_level", firstLevel);
    }

    /** The movement patterns it uses, with their parameters (design/enemies/README.md, Movement pattern vocabulary). */
    public record Movement(
            Optional<Snake> snake,
            Optional<Swoop> swoop,
            Optional<Straight> straight,
            Optional<Hover> hover,
            Optional<Orbit> orbit) {}

    /** Along an authored path, {@code spacing} seconds between two units. */
    public record Snake(double spacing) {
        public Snake {
            Check.positive("spacing", spacing);
        }
    }

    /** An entry curve of {@code radius} px, reaching {@code topSpeed} px/s. */
    public record Swoop(Optional<Span> radius, Optional<Double> topSpeed) {}

    /** Straight at {@code speed} px/s. */
    public record Straight(double speed) {
        public Straight {
            Check.positive("speed", speed);
        }
    }

    /** Hovers for {@code seconds} at a height of {@code y} px from the top of the play field. */
    public record Hover(Span seconds, Span y) {}

    /** Orbits a point at {@code radius} px and {@code turnRate} °/s. */
    public record Orbit(double radius, double turnRate) {
        public Orbit {
            Check.positive("radius", radius);
            Check.positive("turn_rate", turnRate);
        }
    }

    /**
     * An attack pattern (design/enemies/README.md, Attack pattern vocabulary).
     *
     * @param bullet the bullet class, which sets the damage
     * @param interval seconds between shots
     * @param speed bullet speed in px/s
     * @param firstShotDelay seconds from stopping to the first shot
     */
    public record Attack(String pattern, String bullet, double interval, double speed, double firstShotDelay) {
        public Attack {
            Check.positive("interval", interval);
            Check.positive("speed", speed);
            Check.notNegative("first_shot_delay", firstShotDelay);
        }
    }

    /** A formation it appears in, with the unit count: {@code [n]} or {@code [min, max]}. */
    public record FormationUse(String name, Optional<List<Integer>> size) {
        public FormationUse {
            size.ifPresent(s -> Check.that(s.size() == 1 || s.size() == 2, "size must be [n] or [min, max]"));
        }
    }

    /** A hit box part that takes {@code multiplier} times the damage. */
    public record WeakPoint(String name, double multiplier) {
        public WeakPoint {
            Check.positive("multiplier", multiplier);
        }
    }

    /** Overrides of the global difficulty levers. */
    public record Hooks(Optional<Hook> easy, Optional<Hook> hard) {}

    /** Selected units in the formations {@code leadsTargetIn} lead the target. */
    public record Hook(List<String> leadsTargetIn) {}
}
