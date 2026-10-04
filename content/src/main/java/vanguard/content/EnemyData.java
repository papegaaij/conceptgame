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
        Optional<Hooks> difficulty,
        Optional<List<ChainData>> chains,
        Optional<BossData> boss,
        Optional<SegmentChain> segmentChain) {
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
            Optional<Walk> walk,
            Optional<PathMove> path) {}

    /** Part F: the head of a segment chain flies the wave's authored path at {@code speed} px/s. */
    public record PathMove(double speed) {
        public PathMove {
            Check.positive("speed", speed);
        }
    }

    /**
     * Part F: the body of a segment chain (the Coilwyrm) between its head and its tail, following
     * the head's path history: {@code segments} segments whose sprites taper from {@code size[0]}
     * to {@code size[1]} px, hit boxes {@code hitboxShare} of the sprite, {@code spacing} × the
     * segment length apart, each with {@code hp} and paying {@code bounty}; {@code contact} the
     * contact class of the segments and the tail; {@code regrow} the rear part's new head after a
     * cut (once per chain); {@code popInterval} s per segment of the chained death.
     */
    public record SegmentChain(
            int segments,
            List<Double> size,
            double hitboxShare,
            double spacing,
            double hp,
            int bounty,
            Tier contact,
            Regrow regrow,
            double popInterval) {
        public SegmentChain {
            Check.positive("segments", segments);
            Check.that(size.size() == 2, "size is [first, last]");
            Check.positive("hitbox_share", hitboxShare);
            Check.positive("spacing", spacing);
            Check.positive("hp", hp);
            Check.notNegative("bounty", bounty);
            Check.positive("pop_interval", popInterval);
        }
    }

    /** A cut chain's rear part grows a new head over {@code seconds}, flying at {@code speed} px/s. */
    public record Regrow(double seconds, double speed, double hp, int bounty) {
        public Regrow {
            Check.positive("seconds", seconds);
            Check.positive("speed", speed);
            Check.positive("hp", hp);
            Check.notNegative("bounty", bounty);
        }
    }

    /**
     * Part F: a {@code laser-sweep}'s beam, {@code length} × {@code width} px from the eye, sweeping
     * {@code arc} ° over {@code duration} s after a {@code telegraph} of s.
     */
    public record Sweep(double arc, double duration, double telegraph, double length, double width) {
        public Sweep {
            Check.positive("arc", arc);
            Check.positive("duration", duration);
            Check.notNegative("telegraph", telegraph);
            Check.positive("length", length);
            Check.positive("width", width);
        }
    }

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
     * @param multiplier the damage it takes is multiplied by this (a weak point); 1 when not given
     */
    public record PartData(
            String name,
            Point offset,
            Size hitbox,
            double hp,
            String kind,
            int bounty,
            Optional<String> attack,
            Optional<Double> multiplier,
            Optional<Integer> firstBonus) {
        public PartData {
            firstBonus.ifPresent(b -> Check.notNegative("first_bonus", b));
            Check.positive("hp", hp);
            multiplier.ifPresent(m -> Check.positive("multiplier", m));
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

    /**
     * Hovers for {@code seconds} at a height of {@code y} px from the top of the play field; without
     * {@code seconds} it holds until it is killed (a boss). Part F: a unit entering from a side
     * edge hovers {@code edgeX} px from that edge, and {@code exit} {@code back} leaves through it.
     */
    public record Hover(Optional<Span> seconds, Span y, Optional<Double> edgeX, Optional<String> exit) {
        public Hover {
            edgeX.ifPresent(x -> Check.notNegative("edge_x", x));
            exit.ifPresent(e -> Check.that(e.equals("back"), "exit must be back, was '" + e + "'"));
        }

        /** {@code y} and {@code seconds} written as {@code [min, max]} or as one value. */
        @JsonCreator
        static Hover of(
                @com.fasterxml.jackson.annotation.JsonProperty("seconds")
                        Optional<tools.jackson.databind.JsonNode> seconds,
                @com.fasterxml.jackson.annotation.JsonProperty("y") tools.jackson.databind.JsonNode y,
                @com.fasterxml.jackson.annotation.JsonProperty("edge_x") Optional<Double> edgeX,
                @com.fasterxml.jackson.annotation.JsonProperty("exit") Optional<String> exit) {
            Check.that(y != null && span(y) != null, "y is a height or [min, max]");
            Optional<tools.jackson.databind.JsonNode> time = seconds == null
                    ? Optional.empty()
                    : seconds.filter(node -> !node.isNull() && !node.isMissingNode());
            Check.that(time.isEmpty() || span(time.get()) != null, "seconds is a time or [min, max]");
            return new Hover(
                    time.map(Hover::span),
                    span(y),
                    edgeX == null ? Optional.empty() : edgeX,
                    exit == null ? Optional.empty() : exit);
        }

        private static Span span(tools.jackson.databind.JsonNode node) {
            if (node.isNumber()) {
                return new Span(node.doubleValue(), node.doubleValue());
            }
            if (node.isArray() && node.size() == 2) {
                return new Span(node.get(0).doubleValue(), node.get(1).doubleValue());
            }
            return null;
        }
    }

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
     * @param mortar the {@code mortar} pattern's lob (its {@code bullet} is the direct hit, its {@code speed} the ring's)
     * @param burst an aimed attack's shots per volley
     * @param burstGap seconds between the shots of a burst
     * @param rotate the parts sharing the attack take turns, one volley every interval among the living ones
     * @param arms a {@code spiral}'s arms; its {@code interval} is between two bullets of an arm
     * @param duration seconds a {@code spiral} runs before it hands over
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
            Optional<Spawn> spawn,
            Optional<Mortar> mortar,
            Optional<Integer> burst,
            Optional<Double> burstGap,
            Optional<Boolean> rotate,
            Optional<Integer> arms,
            Optional<Double> duration,
            Optional<Sweep> sweep) {
        public Attack {
            Check.that(
                    pattern.equals("laser-sweep") == sweep.isPresent(), "a laser-sweep has its sweep, the others none");
            Check.that(
                    !pattern.equals("laser-sweep") || (bullet.isPresent() && speed.isEmpty()),
                    "a laser-sweep has a bullet and no speed");
            Check.that(
                    pattern.equals("aimed")
                            || pattern.equals("fan")
                            || pattern.equals("mine")
                            || pattern.equals("spawn")
                            || pattern.equals("mortar")
                            || pattern.equals("ring")
                            || pattern.equals("spiral")
                            || pattern.equals("laser-sweep"),
                    "pattern must be aimed, fan, mine, spawn, mortar, ring, spiral or laser-sweep, was '" + pattern
                            + "'");
            Check.that(pattern.equals("mine") == mine.isPresent(), "a mine attack has its mine, the others none");
            Check.that(
                    pattern.equals("mortar") == mortar.isPresent(), "a mortar attack has its mortar, the others none");
            Check.that(
                    pattern.equals("aimed") || (burst.isEmpty() && burstGap.isEmpty() && rotate.isEmpty()),
                    "only an aimed attack has a burst, a burst gap or rotates");
            burst.ifPresent(b -> Check.positive("burst", b));
            burstGap.ifPresent(g -> Check.positive("burst_gap", g));
            Check.that(
                    pattern.equals("spiral") == (arms.isPresent() && duration.isPresent()),
                    "a spiral has its arms and a duration, the others neither");
            arms.ifPresent(a -> Check.positive("arms", a));
            duration.ifPresent(d -> Check.positive("duration", d));
            Check.that(!pattern.equals("spiral") || turnRate.isPresent(), "a spiral has a turn rate");
            Check.that(pattern.equals("spawn") == spawn.isPresent(), "a spawn attack has its spawn, the others none");
            Check.that(
                    pattern.equals("laser-sweep")
                            || pattern.equals("spawn") != (bullet.isPresent() && speed.isPresent()),
                    "an attack has a bullet and a speed, a spawn attack neither");
            Check.that(!pattern.equals("spawn") || interval.isEmpty(), "a spawn attack has no interval");
            aim.ifPresent(a -> Check.that(
                    a.equals("target") || a.equals("down") || a.equals("facing"),
                    "aim must be target, down or facing, was '" + a + "'"));
            away.ifPresent(a -> Check.notNegative("away", a));
            interval.ifPresent(i -> Check.positive("interval", i));
            speed.ifPresent(s -> Check.positive("speed", s));
            firstShotDelay.ifPresent(d -> Check.notNegative("first_shot_delay", d));
            Check.that(
                    (pattern.equals("fan") || pattern.equals("ring")) == count.isPresent(),
                    "a fan or a ring has a count, the others none");
            Check.that(pattern.equals("fan") == spread.isPresent(), "a fan has a spread, the others none");
            count.ifPresent(c -> Check.positive("count", c));
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

    /**
     * A mortar's lob: the impact marker shows {@code marker} s ahead (the blob's flight), a ship
     * inside the {@code impact} px circle when it lands takes the direct hit, and it bursts into a
     * ring of {@code ring} bullets of class {@code ringBullet}. Parsed; flown with the Polyp Mortar.
     */
    public record Mortar(double marker, double impact, int ring, String ringBullet) {
        public Mortar {
            Check.positive("marker", marker);
            Check.positive("impact", impact);
            Check.positive("ring", ring);
        }
    }

    /**
     * An articulated neck: {@code segments} {@code armoured} hit boxes from the anchor {@code from}
     * on the body to the part {@code to} at its end (whose offset is the chain's rest end); each
     * segment follows the one before it {@code lag} s late, and the chain turns at most
     * {@code bend} degrees towards the player.
     */
    public record ChainData(String name, Point from, String to, int segments, Size hitbox, double lag, double bend) {
        public ChainData {
            Check.positive("segments", segments);
            Check.positive("lag", lag);
            Check.notNegative("bend", bend);
        }
    }

    /**
     * A boss's script: {@code kind} {@code boss} or {@code mid-boss} (the short bar), its bar's
     * name, the par time in s (the Boss rush bonus) and its phases in order.
     */
    public record BossData(String kind, String barName, double par, List<PhaseData> phases) {
        public BossData {
            Check.that(
                    kind.equals("boss") || kind.equals("mid-boss"),
                    "kind must be boss or mid-boss, was '" + kind + "'");
            Check.positive("par", par);
            Check.notEmpty("phases", phases);
        }

        /** Whether it is a mid-boss, with the short bar. */
        public boolean midBoss() {
            return kind.equals("mid-boss");
        }
    }

    /**
     * A boss phase: it ends when at most {@code until.left} of {@code until.parts} are alive; it
     * fires its {@code attacks} together or the {@code alternate} ones in turn, sends its
     * {@code streams}, {@code exposes} parts that took no damage before it and bends the chains
     * {@code bend} degrees.
     */
    public record PhaseData(
            String name,
            Until until,
            Optional<List<String>> attacks,
            Optional<List<String>> alternate,
            Optional<StreamData> streams,
            Optional<List<String>> exposes,
            Optional<Double> bend) {
        public PhaseData {
            Check.that(attacks.isEmpty() || alternate.isEmpty(), "a phase has attacks or an alternate list, not both");
            bend.ifPresent(b -> Check.notNegative("bend", b));
        }
    }

    /** The end of a phase: at most {@code left} of {@code parts} alive. */
    public record Until(List<String> parts, int left) {
        public Until {
            Check.notEmpty("parts", parts);
            Check.notNegative("left", left);
        }
    }

    /**
     * Streams of {@code count} units of {@code enemy}, {@code interval} s apart, from the side
     * edges ({@code left}, {@code right} or {@code alternating}): the first when the boss settles,
     * then every {@code every} s while the phase lasts.
     */
    public record StreamData(String enemy, int count, double every, double interval, String edge) {
        public StreamData {
            Check.positive("count", count);
            Check.positive("every", every);
            Check.positive("interval", interval);
            Check.that(
                    edge.equals("left") || edge.equals("right") || edge.equals("alternating"),
                    "edge must be left, right or alternating, was '" + edge + "'");
        }
    }

    /** A difficulty's change to one named attack. */
    public record AttackChange(Optional<Integer> burst, Optional<Integer> count, Optional<Double> interval) {}

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
     * @param attacks changes per attack name (a boss's {@code burst} or {@code count}, an {@code interval})
     * @param sweepArc a laser sweep's arc instead, °
     * @param segments a segment chain's segments instead
     * @param regrownFanCount a regrown head's fan bullets instead
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
            Optional<Double> spawnAfter,
            Optional<java.util.Map<String, AttackChange>> attacks,
            Optional<Double> sweepArc,
            Optional<Integer> segments,
            Optional<Integer> regrownFanCount) {}

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

    /** The part named {@code name}'s index in the part list; -1 without one. */
    public int partIndex(String name) {
        List<PartData> parts = partList.orElse(List.of());
        for (int i = 0; i < parts.size(); i++) {
            if (parts.get(i).name().equals(name)) {
                return i;
            }
        }
        return -1;
    }

    /** Whether it is a multi-part unit ({@code parts: multi} with a {@code part_list}). */
    public boolean multiPart() {
        return parts.equals("multi");
    }
}
