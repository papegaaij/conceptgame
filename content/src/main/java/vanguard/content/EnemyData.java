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
        Optional<List<PartData>> partList,
        Orientation orientation,
        double hp,
        String armour,
        double speed,
        Movement movement,
        List<Attack> attacks,
        List<FormationUse> formations,
        List<WeakPoint> weakPoints,
        List<Drop> drops,
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
            Optional<Orbit> orbit,
            Optional<Dive> dive,
            Optional<Terrain> terrain,
            Optional<Strafe> strafe,
            Optional<SpiralOut> spiralOut) {}

    /** A convoy unit turns across the screen at a height in {@code y}, px from the top. */
    public record Strafe(Span y) {}

    /**
     * Spirals out from its release point for {@code seconds} ({@code turns} per second, the radius
     * growing {@code growth} px/s, the release point drifting down {@code drift} px/s), then flies on
     * along its outward angle turned downward, bouncing off the side edges up to {@code ricochets} times.
     */
    public record SpiralOut(double seconds, double turns, double growth, double drift, int ricochets) {
        public SpiralOut {
            Check.positive("seconds", seconds);
            Check.notNegative("ricochets", ricochets);
        }
    }

    /**
     * A part of a multi-part unit ({@code parts: multi}): a hit box around the unit's centre in its
     * own frame (facing down the screen: dx right, dy up towards its tail).
     *
     * @param kind {@code armoured}, {@code destroyable} or {@code vital} (destroying it destroys the rest)
     * @param attack the name of the unit's attack it fires
     */
    public record PartData(
            String name, Point offset, Size hitbox, double hp, String kind, int bounty, Optional<String> attack) {
        public PartData {
            Check.positive("hp", hp);
            Check.that(
                    kind.equals("destroyable") || kind.equals("vital") || kind.equals("armoured"),
                    "kind must be armoured, destroyable or vital, was '" + kind + "'");
        }
    }

    /**
     * Enters to a height of {@code y} px below the top of the play field, pauses for {@code pause}
     * seconds (its telegraph), then dives at {@code speed} px/s towards where the player was when
     * the pause ended, and flies on off the screen.
     *
     * @param fireAfter seconds into the dive at which it fires, unless it passes the player's
     *     height first
     */
    public record Dive(Span y, double pause, double speed, double fireAfter) {
        public Dive {
            Check.notNegative("pause", pause);
            Check.positive("speed", speed);
            Check.positive("fire_after", fireAfter);
        }
    }

    /** Fixed to the ground layer, moving only with the scroll. */
    public record Terrain() {}

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
     * @param count a {@code fan}'s bullets
     * @param spread a {@code fan}'s angle from its first to its last bullet, degrees
     * @param turnRate a turret's barrel turn rate, °/s; the shots leave along the barrel
     * @param arc a turret fires while the player is within this many degrees of its facing (down the screen)
     */
    public record Attack(
            String pattern,
            Optional<String> name,
            String bullet,
            Optional<Double> interval,
            double speed,
            Optional<Double> firstShotDelay,
            Optional<Integer> count,
            Optional<Double> spread,
            Optional<Double> turnRate,
            Optional<Double> arc,
            Optional<Mine> mine) {
        public Attack {
            Check.that(
                    pattern.equals("aimed") || pattern.equals("fan") || pattern.equals("mine"),
                    "pattern must be aimed, fan or mine, was '" + pattern + "'");
            Check.that(pattern.equals("mine") == mine.isPresent(), "a mine attack has its mine, the others none");
            interval.ifPresent(i -> Check.positive("interval", i));
            Check.positive("speed", speed);
            firstShotDelay.ifPresent(d -> Check.notNegative("first_shot_delay", d));
            Check.that(pattern.equals("fan") == count.isPresent(), "a fan has a count, an aimed attack none");
            Check.that(count.isPresent() == spread.isPresent(), "a fan has a count and a spread");
        }
    }

    /** A formation it appears in, with the unit count: {@code [n]} or {@code [min, max]}. */
    public record FormationUse(String name, Optional<List<Integer>> size) {
        public FormationUse {
            size.ifPresent(s -> Check.that(s.size() == 1 || s.size() == 2, "size must be [n] or [min, max]"));
        }
    }

    /**
     * The glowing weak point; only a part of a multi-part unit has a damage {@code multiplier}
     * (design/enemies, stat block template), a single-part unit's is drawn only.
     */
    public record WeakPoint(String name, Optional<Double> multiplier) {
        public WeakPoint {
            multiplier.ifPresent(m -> Check.positive("multiplier", m));
        }
    }

    /** Every {@code every}-th kill of this enemy in a level drops {@code pickup}. */
    public record Drop(Pickup pickup, int every) {
        public Drop {
            Check.positive("every", every);
        }
    }

    /**
     * A dropped spore mine: it arms (rises to the player plane) after {@code arm} s, drifts
     * {@code drift} px/s in a random direction, has {@code hp}, and bursts after {@code life} s into
     * a {@code ring} of {@code ringBullet} bullets at the attack's speed; destroyed it pays {@code credits}.
     */
    public record Mine(double arm, double life, double drift, double hp, int ring, String ringBullet, int credits) {
        public Mine {
            Check.notNegative("arm", arm);
            Check.positive("life", life);
            Check.positive("hp", hp);
        }
    }

    /** Overrides of the global difficulty levers. */
    public record Hooks(Optional<Hook> easy, Optional<Hook> hard) {}

    /**
     * A difficulty's changes to the stat block.
     *
     * @param leadsTargetIn selected units in these formations lead the target
     * @param fanCount a fan's bullets instead
     * @param divePause a dive's pause instead, s
     * @param burst shots per volley instead of one
     * @param ring a mine's ring bullets instead
     * @param mineBursts false: mines never burst on their own
     * @param deathBurst the puff of bullets it pops into when destroyed
     */
    public record Hook(
            Optional<List<String>> leadsTargetIn,
            Optional<Integer> fanCount,
            Optional<Double> divePause,
            Optional<Integer> burst,
            Optional<Integer> ring,
            Optional<Boolean> mineBursts,
            Optional<DeathBurst> deathBurst) {}

    /** {@code count} bullets of class {@code bullet} in a ring at {@code speed} px/s. */
    public record DeathBurst(int count, double speed, String bullet) {
        public DeathBurst {
            Check.positive("count", count);
            Check.positive("speed", speed);
        }
    }

    /** The attack named {@code name} (a part's {@code attack}). */
    public Optional<Attack> attack(String name) {
        return attacks.stream()
                .filter(attack -> attack.name().filter(name::equals).isPresent())
                .findFirst();
    }

    /** Whether it is a multi-part unit ({@code parts: multi} with a {@code part_list}). */
    public boolean multiPart() {
        return parts.equals("multi");
    }
}
