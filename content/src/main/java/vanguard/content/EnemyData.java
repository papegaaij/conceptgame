package vanguard.content;

import com.fasterxml.jackson.annotation.JsonCreator;
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
        Armour armour,
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
    /**
     * The armour: a text ({@code none}, {@code hardened}, ...), or planned (part D) a mapping with
     * {@code front_arc}, the degrees each side of the facing from which direct shots glance off
     * (dropped bombs, lobbed shells and the specials ignore it). Read, not flown yet.
     */
    public record Armour(Optional<String> text, Optional<Double> frontArc) {
        @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
        public Armour {
            frontArc.ifPresent(arc -> Check.notNegative("front_arc", arc));
        }

        /** The armour written as a text. */
        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        public static Armour of(String text) {
            return new Armour(Optional.of(text), Optional.empty());
        }
    }

    public record Movement(
            Optional<Snake> snake,
            Optional<Swoop> swoop,
            Optional<Straight> straight,
            Optional<Hover> hover,
            Optional<Orbit> orbit,
            Optional<Dive> dive,
            Optional<Terrain> terrain,
            Optional<Strafe> strafe,
            Optional<SpiralOut> spiralOut,
            Optional<Drift> drift,
            Optional<Sine> sine,
            Optional<Walk> walk) {}

    /** Planned (part D): straight down the screen at {@code speed} px/s, with no intent. Read, not flown yet. */
    public record Drift(double speed) {
        public Drift {
            Check.positive("speed", speed);
        }
    }

    /** Planned (part D): a side-to-side offset of {@code amplitude} px, {@code period} s per swing. Read, not flown yet. */
    public record Sine(double amplitude, double period) {
        public Sine {
            Check.notNegative("amplitude", amplitude);
            Check.positive("period", period);
        }
    }

    /**
     * Planned (part D): a walker on a ground path at {@code speed} px/s, turning at {@code turn_rate}
     * °/s, {@code stride} px of ground per walk cycle. Read, not flown yet.
     */
    public record Walk(double speed, double turnRate, double stride) {
        public Walk {
            Check.positive("speed", speed);
            Check.positive("turn_rate", turnRate);
            Check.positive("stride", stride);
        }
    }

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
    /**
     * @param aim planned (part D): where a fan points, {@code target} (the default), {@code down} or {@code facing}
     * @param away planned (part D): an aimed attack fires only while the player is more than this many ° off its facing
     * @param spawn planned (part D): the {@code spawn} pattern's release, which has no bullet, interval or speed
     */
    public record Attack(
            String pattern,
            Optional<String> name,
            Optional<String> bullet,
            Optional<Double> interval,
            Optional<Double> speed,
            Optional<Double> firstShotDelay,
            Optional<Integer> count,
            Optional<Double> spread,
            Optional<Double> turnRate,
            Optional<Double> arc,
            Optional<Mine> mine,
            Optional<String> aim,
            Optional<Double> away,
            Optional<Spawn> spawn) {
        public Attack {
            Check.that(
                    pattern.equals("aimed")
                            || pattern.equals("fan")
                            || pattern.equals("mine")
                            || pattern.equals("spawn"),
                    "pattern must be aimed, fan, mine or spawn, was '" + pattern + "'");
            Check.that(pattern.equals("mine") == mine.isPresent(), "a mine attack has its mine, the others none");
            Check.that(pattern.equals("spawn") == spawn.isPresent(), "a spawn attack has its spawn, the others none");
            Check.that(
                    pattern.equals("spawn") != (bullet.isPresent() && speed.isPresent()),
                    "an attack has a bullet and a speed, a spawn attack neither");
            Check.that(!pattern.equals("spawn") || interval.isEmpty(), "a spawn attack has no interval");
            aim.ifPresent(a -> Check.that(
                    a.equals("target") || a.equals("down") || a.equals("facing"),
                    "aim must be target, down or facing, was '" + a + "'"));
            away.ifPresent(a -> Check.notNegative("away", a));
            interval.ifPresent(i -> Check.positive("interval", i));
            speed.ifPresent(s -> Check.positive("speed", s));
            firstShotDelay.ifPresent(d -> Check.notNegative("first_shot_delay", d));
            Check.that(pattern.equals("fan") == count.isPresent(), "a fan has a count, an aimed attack none");
            Check.that(count.isPresent() == spread.isPresent(), "a fan has a count and a spread");
        }
    }

    /** A formation it appears in, with the unit count: {@code [n]} or {@code [min, max]}. */
    /**
     * Planned (part D): a spawner's release. {@code count} units of {@code enemy} when it is killed or
     * {@code after} s from entering (a self-burst paying {@code burst_bounty}, not a kill), with a
     * {@code telegraph} of s before it, flying out at {@code speed} in an {@code arc} of °. Read, not
     * flown yet.
     */
    public record Spawn(
            String enemy, int count, double after, double telegraph, double arc, double speed, int burstBounty) {
        public Spawn {
            Check.positive("count", count);
            Check.positive("after", after);
            Check.notNegative("telegraph", telegraph);
            Check.positive("arc", arc);
            Check.positive("speed", speed);
            Check.notNegative("burst_bounty", burstBounty);
        }
    }

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
            Optional<DeathBurst> deathBurst,
            Optional<Integer> spawnCount,
            Optional<Double> spawnAfter) {}

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
