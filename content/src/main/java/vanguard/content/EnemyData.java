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
 * @param cloak M5 part D: its cloak (the Wraith): its {@code layer} is the cloaked one
 * @param submerge M5 part E: how it surfaces and submerges (the Driftjelly), a unit on {@code ground}
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
        Optional<SegmentChain> segmentChain,
        Optional<Cloak> cloak,
        Optional<Submerge> submerge) {
    public EnemyData {
        Layers.of(layer);
        Check.positive("hp", hp);
        Check.notNegative("speed", speed);
        Check.notNegative("bounty", bounty);
        Check.positive("first_level", firstLevel);
    }

    /** The movement patterns it uses, with their parameters (design/enemies/README.md, Movement pattern vocabulary). */
    /**
     * The armour: a text ({@code none}, {@code hardened}, ...), or a mapping with {@code front_arc},
     * the degrees each side of the facing from which direct shots glance off (dropped bombs, lobbed
     * shells and the specials ignore it; a walker's, Level 04). M5 part C: {@code hardened} flies for
     * an enemy (design/enemies/ground/hive-node): only anti-ground deliveries, the Airstrike and the
     * Smart Bomb damage it.
     */
    public record Armour(Optional<String> text, Optional<Double> frontArc) {
        @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
        public Armour {
            frontArc.ifPresent(arc -> Check.notNegative("front_arc", arc));
        }

        /** Whether it is {@code hardened}. */
        public boolean hardened() {
            return text.filter("hardened"::equals).isPresent();
        }

        /** The armour written as a text. */
        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        public static Armour of(String text) {
            return new Armour(Optional.of(text), Optional.empty());
        }
    }

    /**
     * @param ambush M5 part D: the path of a {@code rear ambush} wave's unit (the Wraith)
     * @param flock M5 part D: how a {@code swarm} wave's members steer (the Mote Swarm)
     * @param anchored M5 part E: an arena boss's anchored arrival (the Harbour Kraken)
     */
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
            Optional<PathMove> path,
            Optional<Ambush> ambush,
            Optional<Flock> flock,
            Optional<Anchored> anchored) {}

    /**
     * M5 part E, an arena boss's arrival (design/enemies/bosses/harbour-kraken): no descent and no
     * hover; its centre stops {@code y} px below the top edge when the scroll halts in its arena. It
     * lies on the ground layer that far up the scroll from the halt, scrolls in with the ground
     * (invulnerable, without its bar) and engages at the halt.
     */
    public record Anchored(double y) {
        public Anchored {
            Check.positive("y", y);
        }
    }

    /**
     * M5 part E, surfacing and submerging (design/enemies/naval/driftjelly; the stated defaults of
     * 2026-10-08): every {@code every} {@code [min, max]} s, drawn per unit from its own seed on the
     * simulation's real steps, it swaps between the surface ({@code ground}) and {@code sub} over
     * {@code swap} s, its layer flipping at the swap's middle; a {@code field} wave starts the share
     * {@code start} of its units submerged.
     */
    public record Submerge(Span every, double swap, double start) {
        public Submerge {
            Check.positive("every", every.min());
            Check.positive("swap", swap);
            Check.share("start", start);
            Check.that(swap < every.min(), "swap: shorter than the least time between swaps");
        }
    }

    /**
     * M5 part D, a cloak (design/enemies/air/wraith): the stat block's {@code layer} is the cloaked
     * one; from its decloak at its hold point on it is on {@code layer}, the {@code flash} (s) of
     * its decloak starting there, its gun silent until the flash ends.
     */
    public record Cloak(String layer, double flash) {
        public Cloak {
            Layers.of(layer);
            Check.notNegative("flash", flash);
        }
    }

    /**
     * M5 part D, the path of a {@code rear ambush} wave's unit (design/enemies/air/wraith): cloaked
     * straight down its lane at the stat block's {@code speed} past the ship and off the bottom
     * edge, {@code gap} s below it, back up to its {@code hover.y} at that speed, the decloak and its
     * {@code hover.seconds} hold, then out up the nearer side lane ({@code lane} px from that edge, a
     * tie to the left) at its {@code straight.speed}.
     */
    public record Ambush(double gap, double lane) {
        public Ambush {
            Check.notNegative("gap", gap);
            Check.notNegative("lane", lane);
        }
    }

    /**
     * M5 part D, a flock (design/enemies/air/mote-swarm, user decision D8 = a): a {@code swarm}
     * wave's members keep {@code separation} px apart, steer by {@code alignment} and {@code
     * cohesion} among the members within {@code radius} px and toward the wave's leader point by
     * {@code leader} (the weights), at {@code speed} px/s ({@code dive_speed} after a loop-back),
     * turning at most {@code turn_rate} °/s; at most {@code max} members (24).
     */
    public record Flock(
            double separation,
            double radius,
            double alignment,
            double cohesion,
            double leader,
            double speed,
            double diveSpeed,
            double turnRate,
            int max) {
        public Flock {
            Check.positive("separation", separation);
            Check.that(radius >= separation, "radius: the neighbourhood reaches at least the separation");
            Check.notNegative("alignment", alignment);
            Check.notNegative("cohesion", cohesion);
            Check.positive("leader", leader);
            Check.positive("speed", speed);
            Check.positive("dive_speed", diveSpeed);
            Check.positive("turn_rate", turnRate);
            Check.that(max >= 1 && max <= 24, "max: 1 to 24 members");
        }
    }

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
     * {@code arc} ° over {@code duration} s after a {@code telegraph} of s. The eye is at {@code
     * origin} {@code [in, down]} px from the unit's centre: toward the field (mirrored on the right
     * edge) and down the screen; the centre when it is left out. The beam, its hit test and its
     * telegraph start there.
     */
    public record Sweep(
            double arc, double duration, double telegraph, double length, double width, Optional<Point> origin) {
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
     * @param kind {@code armoured}, {@code destroyable} or {@code vital} (destroying it destroys the
     *     rest); part G: a boss's {@code armoured} part is fire-only (it fires its attack, never
     *     takes damage, is not in the bar and pays nothing), written without {@code hp} and
     *     {@code bounty} (0 here)
     * @param attack the name of the unit's attack it fires
     * @param multiplier the damage it takes is multiplied by this (a weak point); 1 when not given
     * @param layer M5 part E: an arena boss's part's starting layer ({@code sub} or {@code ground});
     *     its script moves it
     * @param spots M5 part E: weak spots on it (the Kraken's eyes)
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
            Optional<Integer> firstBonus,
            Optional<String> layer,
            Optional<List<SpotData>> spots) {
        public PartData {
            firstBonus.ifPresent(b -> Check.notNegative("first_bonus", b));
            layer.ifPresent(l -> Check.that(
                    l.equals("sub") || l.equals("ground"), "a part's layer is sub or ground, was '" + l + "'"));
            Check.that(
                    kind.equals("destroyable") || kind.equals("vital") || kind.equals("armoured"),
                    "kind must be armoured, destroyable or vital, was '" + kind + "'");
            if (kind.equals("armoured")) {
                Check.that(hp == 0 && bounty == 0, "an armoured part has no hp and no bounty");
            } else {
                Check.positive("hp", hp);
            }
            multiplier.ifPresent(m -> Check.positive("multiplier", m));
        }

        /** A part as written: an {@code armoured} one without {@code hp} and {@code bounty}. */
        @JsonCreator
        static PartData of(
                @com.fasterxml.jackson.annotation.JsonProperty("name") String name,
                @com.fasterxml.jackson.annotation.JsonProperty("offset") Point offset,
                @com.fasterxml.jackson.annotation.JsonProperty("hitbox") Size hitbox,
                @com.fasterxml.jackson.annotation.JsonProperty("hp") Optional<Double> hp,
                @com.fasterxml.jackson.annotation.JsonProperty("kind") String kind,
                @com.fasterxml.jackson.annotation.JsonProperty("bounty") Optional<Integer> bounty,
                @com.fasterxml.jackson.annotation.JsonProperty("attack") Optional<String> attack,
                @com.fasterxml.jackson.annotation.JsonProperty("multiplier") Optional<Double> multiplier,
                @com.fasterxml.jackson.annotation.JsonProperty("first_bonus") Optional<Integer> firstBonus,
                @com.fasterxml.jackson.annotation.JsonProperty("layer") Optional<String> layer,
                @com.fasterxml.jackson.annotation.JsonProperty("spots") Optional<List<SpotData>> spots) {
            Check.that(
                    name != null && offset != null && hitbox != null && kind != null,
                    "a part has a name, an offset, a hit box and a kind");
            boolean armoured = kind.equals("armoured");
            Optional<Double> hpValue = hp == null ? Optional.empty() : hp;
            Optional<Integer> bountyValue = bounty == null ? Optional.empty() : bounty;
            Check.that(armoured || (hpValue.isPresent() && bountyValue.isPresent()), "a part has hp and a bounty");
            Check.that(
                    !armoured || (hpValue.isEmpty() && bountyValue.isEmpty()),
                    "an armoured part has no hp and no bounty");
            return new PartData(
                    name,
                    offset,
                    hitbox,
                    hpValue.orElse(0.0),
                    kind,
                    bountyValue.orElse(0),
                    attack == null ? Optional.empty() : attack,
                    multiplier == null ? Optional.empty() : multiplier,
                    firstBonus == null ? Optional.empty() : firstBonus,
                    layer == null ? Optional.empty() : layer,
                    spots == null ? Optional.empty() : spots);
        }

        /** Whether it is a fire-only part. */
        public boolean armoured() {
            return kind.equals("armoured");
        }
    }

    /**
     * M5 part E, a weak spot on a part (the Kraken's eyes): a box of {@code hitbox} at {@code offset}
     * ({@code [dx, dy]} px from the part's centre, dx right, dy up) that routes the damage to the part
     * at its {@code multiplier}; with {@code while: open} only while the part's surfacing window is
     * open (the part's own multiplier elsewhere).
     */
    public record SpotData(
            String name,
            Point offset,
            Size hitbox,
            double multiplier,

            @com.fasterxml.jackson.annotation.JsonProperty("while")
            Optional<String> during) {
        public SpotData {
            Check.positive("multiplier", multiplier);
            during.ifPresent(w -> Check.that(w.equals("open"), "while must be open, was '" + w + "'"));
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

    /**
     * Fixed to the ground layer, moving only with the scroll; M5 part E: with a {@code drift}, px/s
     * along its nest's {@code current} on top of the scroll (a raft on the water, the Reef Spitter).
     */
    public record Terrain(Optional<Double> drift) {
        public Terrain {
            drift.ifPresent(d -> Check.positive("drift", d));
        }
    }

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
     * @param spread a {@code fan}'s angle from its first to its last bullet, degrees; M5 part D: an aimed
     *     attack's with {@code aim: up}, from its burst's first shot to its last
     * @param turnRate a turret's barrel turn rate, °/s; the shots leave along the barrel
     * @param arc a turret fires while the player is within this many degrees of its facing (down the screen)
     */
    /**
     * @param aim where a fan points, {@code target} (the default), {@code down} or {@code facing}; a walker's fan
     *     (M5 part B) is aimed at the player with {@code target} and along its facing with {@code facing};
     *     planned (part D) for the other units; M5 part D: an aimed attack's {@code up} (the Wraith, user
     *     decision of 2026-10-08) sends its bursts straight up the screen as a fixed fan of {@code spread},
     *     its shots in turn from the left edge to the right, aimed at nobody and fired however close the
     *     ship is
     * @param away planned (part D): an aimed attack fires only while the player is more than this many ° off its facing
     * @param spawn planned (part D): the {@code spawn} pattern's release, which has no bullet, interval or speed
     * @param mortar the {@code mortar} pattern's lob (its {@code bullet} is the direct hit, its {@code speed} the ring's)
     * @param burst an aimed attack's shots per volley
     * @param burstGap seconds between the shots of a burst
     * @param rotate the parts sharing the attack take turns, one volley every interval among the living ones
     * @param arms a {@code spiral}'s arms; its {@code interval} is between two bullets of an arm
     * @param duration seconds a {@code spiral} runs before it hands over in an alternation; part G:
     *     without it a spiral fires for as long as its phase lasts (it cannot alternate)
     * @param stagger M5 part B, a walker's fan (design/enemies/ground/creeper): the units of one wave
     *     share a volley clock, unit i (in the order they enter) firing i × this many seconds after
     *     the first; a unit off the screen skips its turn
     * @param pounce M5 part C, the {@code pounce} pattern's leap (design/enemies/ground/ravager): no
     *     bullet and no speed; its {@code interval} is the s from landing until it may leap again
     * @param within M5 part E, a {@code ring}'s reach (design/enemies/naval/driftjelly): it fires only
     *     while the ship's centre is this close, px, on either of its layers; its {@code interval}
     *     the cooldown from the last ring
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
            Optional<Sweep> sweep,
            Optional<Double> stagger,
            Optional<Pounce> pounce,
            Optional<Double> within) {
        public Attack {
            Check.that(pattern.equals("ring") || within.isEmpty(), "only a ring fires within a reach");
            within.ifPresent(w -> Check.positive("within", w));
            Check.that(within.isEmpty() || interval.isPresent(), "a ring fired within a reach has its interval");
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
                            || pattern.equals("laser-sweep")
                            || pattern.equals("pounce"),
                    "pattern must be aimed, fan, mine, spawn, mortar, ring, spiral, laser-sweep or pounce, was '"
                            + pattern + "'");
            Check.that(pattern.equals("pounce") == pounce.isPresent(), "a pounce has its pounce, the others none");
            Check.that(
                    !pattern.equals("pounce") || (bullet.isEmpty() && speed.isEmpty() && interval.isPresent()),
                    "a pounce has an interval and no bullet or speed");
            Check.that(pattern.equals("mine") == mine.isPresent(), "a mine attack has its mine, the others none");
            Check.that(
                    pattern.equals("mortar") == mortar.isPresent(), "a mortar attack has its mortar, the others none");
            Check.that(
                    pattern.equals("aimed") || (burst.isEmpty() && burstGap.isEmpty() && rotate.isEmpty()),
                    "only an aimed attack has a burst, a burst gap or rotates");
            burst.ifPresent(b -> Check.positive("burst", b));
            burstGap.ifPresent(g -> Check.positive("burst_gap", g));
            Check.that(pattern.equals("spiral") == arms.isPresent(), "a spiral has its arms, the others none");
            Check.that(pattern.equals("spiral") || duration.isEmpty(), "only a spiral has a duration");
            arms.ifPresent(a -> Check.positive("arms", a));
            duration.ifPresent(d -> Check.positive("duration", d));
            Check.that(!pattern.equals("spiral") || turnRate.isPresent(), "a spiral has a turn rate");
            Check.that(pattern.equals("spawn") == spawn.isPresent(), "a spawn attack has its spawn, the others none");
            Check.that(
                    pattern.equals("laser-sweep")
                            || pattern.equals("pounce")
                            || pattern.equals("spawn") != (bullet.isPresent() && speed.isPresent()),
                    "an attack has a bullet and a speed, a spawn attack neither");
            Check.that(!pattern.equals("spawn") || interval.isEmpty(), "a spawn attack has no interval");
            aim.ifPresent(a -> Check.that(
                    a.equals("target") || a.equals("down") || a.equals("facing") || a.equals("up"),
                    "aim must be target, down, facing or up, was '" + a + "'"));
            away.ifPresent(a -> Check.notNegative("away", a));
            interval.ifPresent(i -> Check.positive("interval", i));
            speed.ifPresent(s -> Check.positive("speed", s));
            firstShotDelay.ifPresent(d -> Check.notNegative("first_shot_delay", d));
            Check.that(
                    (pattern.equals("fan") || pattern.equals("ring")) == count.isPresent(),
                    "a fan or a ring has a count, the others none");
            boolean up = aim.filter("up"::equals).isPresent();
            Check.that(!up || pattern.equals("aimed"), "only an aimed attack fires straight up");
            Check.that(
                    (pattern.equals("fan") || up) == spread.isPresent(),
                    "a fan or an aimed attack fired up has a spread, the others none");
            count.ifPresent(c -> Check.positive("count", c));
            Check.that(pattern.equals("fan") || stagger.isEmpty(), "only a fan has a stagger");
            stagger.ifPresent(st -> Check.positive("stagger", st));
        }
    }

    /**
     * A spawner's release: {@code count} units of {@code enemy} flying out at {@code speed} in an
     * {@code arc} of ° toward the ship. A brood (Level 04's Brood Pod) releases them when it is
     * killed or {@code after} s from entering (a self-burst paying {@code burst_bounty}, not a kill),
     * with a {@code telegraph} of s before it. M5 part C, a periodic spawner (the Hive Node): instead
     * of {@code after} and {@code burst_bounty}, {@code every} s from its centre crossing the top
     * edge, its iris opening over the {@code telegraph}, and an opening due while the ship's centre
     * is within {@code shut_within} px skipped; it never bursts on its own.
     */
    public record Spawn(
            String enemy,
            int count,
            Optional<Double> after,
            Optional<Double> every,
            double telegraph,
            double arc,
            double speed,
            Optional<Integer> burstBounty,
            Optional<Double> shutWithin) {
        public Spawn {
            Check.positive("count", count);
            Check.that(after.isPresent() != every.isPresent(), "a spawn has an after (a brood) or an every, not both");
            after.ifPresent(a -> Check.positive("after", a));
            every.ifPresent(e -> Check.positive("every", e));
            Check.notNegative("telegraph", telegraph);
            Check.positive("arc", arc);
            Check.positive("speed", speed);
            Check.that(
                    after.isPresent() == burstBounty.isPresent(),
                    "a brood has a burst_bounty, a periodic spawn none (it never bursts)");
            burstBounty.ifPresent(b -> Check.notNegative("burst_bounty", b));
            Check.that(every.isPresent() || shutWithin.isEmpty(), "only a periodic spawn has a shut_within");
            shutWithin.ifPresent(d -> Check.notNegative("shut_within", d));
            every.ifPresent(e -> Check.that(telegraph < e, "the telegraph lies inside the cycle (every)"));
        }

        /** Whether it is a periodic spawn (with {@code every}). */
        public boolean periodic() {
            return every.isPresent();
        }
    }

    /**
     * M5 part C, a pounce's leap (design/enemies/ground/ravager): within {@code range} px of the
     * ship (centre to centre, on the screen) it leaps at the ship's position over {@code leap} s;
     * for the middle {@code air} s it is on the {@code air} layer; drawn {@code scale} times its size
     * at the apex.
     */
    public record Pounce(double range, double leap, double air, double scale) {
        public Pounce {
            Check.positive("range", range);
            Check.positive("leap", leap);
            Check.notNegative("air", air);
            Check.that(air <= leap, "the air window lies inside the leap");
            Check.positive("scale", scale);
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
    public record ChainData(
            String name,
            Point from,
            String to,
            int segments,
            Size hitbox,
            double lag,
            double bend,
            Optional<String> motion) {
        public ChainData {
            Check.positive("segments", segments);
            Check.positive("lag", lag);
            Check.notNegative("bend", bend);
            motion.ifPresent(m -> Check.that(m.equals("slam"), "motion must be slam, was '" + m + "'"));
        }

        /** M5 part E: whether it is a slam arm ({@code motion: slam}): it follows the slam cycle instead of bending. */
        public boolean slams() {
            return motion.isPresent();
        }
    }

    /**
     * A boss's script: {@code kind} {@code boss} or {@code mid-boss} (the short bar), its bar's
     * name, the par time in s (the Boss rush bonus) and its phases in order.
     *
     * @param engagesOnArrival part G: its first phase starts when it arrives (the bar appearing),
     *     its entrance part of the fight; otherwise when it settles at its hover height
     * @param deathSeconds part G: how long its chained death runs, s (the act boss's 3 s, tail to
     *     head); left out, the mid-boss's quick chain
     * @param poses part G: its further part layouts (the {@code part_list} offsets and the
     *     {@code hitbox} are the arrival pose), which a phase's {@code move} turns it into
     * @param lanes M5 part E: an arena boss's slam lanes (the Harbour Kraken)
     * @param slam M5 part E: its slam cycle (only with lanes)
     */
    public record BossData(
            String kind,
            String barName,
            double par,
            List<PhaseData> phases,
            Optional<Boolean> engagesOnArrival,
            Optional<Double> deathSeconds,
            Optional<List<PoseData>> poses,
            Optional<Lanes> lanes,
            Optional<SlamData> slam) {
        public BossData {
            Check.that(
                    kind.equals("boss") || kind.equals("mid-boss"),
                    "kind must be boss or mid-boss, was '" + kind + "'");
            Check.positive("par", par);
            Check.notEmpty("phases", phases);
            deathSeconds.ifPresent(d -> Check.notNegative("death_seconds", d));
            Check.that(lanes.isPresent() == slam.isPresent(), "a boss with lanes has a slam cycle, and only it");
            poses.ifPresent(list -> list.forEach(pose -> Check.that(
                    list.stream()
                                    .filter(other -> other.name().equals(pose.name()))
                                    .count()
                            == 1,
                    "pose names are unique, '" + pose.name() + "' is not")));
        }

        /** The index of pose {@code name} in the boss's poses (0 the arrival pose, the further ones from 1); -1 without one. */
        public int poseIndex(String name) {
            List<PoseData> list = poses.orElse(List.of());
            for (int i = 0; i < list.size(); i++) {
                if (list.get(i).name().equals(name)) {
                    return i + 1;
                }
            }
            return -1;
        }

        /** Whether it is a mid-boss, with the short bar. */
        public boolean midBoss() {
            return kind.equals("mid-boss");
        }
    }

    /**
     * M5 part E, an arena boss's slam lanes (design/enemies/bosses/harbour-kraken, user decision E5 =
     * a): {@code count} lanes {@code width} px wide from the play field's left edge, lane 1 first;
     * {@code arms}, each slam arm's part name with the lanes (1-based) it owns.
     */
    public record Lanes(int count, double width, Optional<java.util.Map<String, List<Integer>>> arms) {
        public Lanes {
            Check.positive("count", count);
            Check.positive("width", width);
            Check.that(
                    count * width <= vanguard.sim.PlayField.WIDTH + 1e-9,
                    "the lanes lie on the play field: " + count + " × " + width + " px");
            arms.ifPresent(map -> map.values()
                    .forEach(owned -> owned.forEach(lane -> Check.that(
                            lane >= 1 && lane <= count, "arms: lane " + lane + " is not one of the " + count))));
        }
    }

    /**
     * M5 part E, an arena boss's slam cycle (design/enemies/bosses/harbour-kraken): the lane's
     * {@code telegraph}, the arm's {@code rise}, {@code awash} and {@code sink} times (s); the
     * impact's {@code damage} class on the ship and the escort in the lane (once per slam); the
     * {@code splash} bullets from along the arm; {@code choose: alternate} (the ship's lane and the
     * nearest convoy ship's in turn, the only rule); the hooks {@code telegraph} and {@code lanes} change
     * the telegraph and the lanes a volley slams (one per arm when not given).
     */
    public record SlamData(
            double telegraph, double rise, double awash, double sink, String damage, Splash splash, String choose) {
        public SlamData {
            Check.positive("telegraph", telegraph);
            Check.positive("rise", rise);
            Check.positive("awash", awash);
            Check.positive("sink", sink);
            Check.that(choose.equals("alternate"), "choose must be alternate, was '" + choose + "'");
        }
    }

    /** The slam's splash: {@code count} bullets of class {@code bullet} at {@code speed} px/s. */
    public record Splash(int count, double speed, String bullet) {
        public Splash {
            Check.notNegative("count", count);
            Check.positive("speed", speed);
        }
    }

    /**
     * M5 part E, how a phase slams: {@code chain} (one at a time, the next telegraph when the arm has
     * sunk), {@code after_dive} (one after each dive) or {@code volley} with {@code every} s (one per
     * living arm at once); written as the mode alone or as {@code {mode, every}}.
     */
    public record SlammingData(String mode, Optional<Double> every) {
        public SlammingData {
            Check.that(
                    mode.equals("chain") || mode.equals("after_dive") || mode.equals("volley"),
                    "slamming is chain, after_dive or volley, was '" + mode + "'");
            Check.that(every.isPresent() == mode.equals("volley"), "a volley, and only it, has its every");
            every.ifPresent(e -> Check.positive("every", e));
        }

        @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
        static SlammingData of(
                @com.fasterxml.jackson.annotation.JsonProperty("mode") String mode,
                @com.fasterxml.jackson.annotation.JsonProperty("every") Optional<Double> every) {
            Check.that(mode != null, "slamming has a mode");
            return new SlammingData(mode, every == null ? Optional.empty() : every);
        }

        /** The mode written alone. */
        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        static SlammingData of(String mode) {
            return new SlammingData(mode, Optional.empty());
        }
    }

    /**
     * M5 part E, a phase's surfacing part (the Kraken's head): it rises crown first over {@code rise}
     * s (its layer flipping at the middle), stays {@code open} s (its weak spots open), dives over
     * {@code dive} s; {@code glow} s is the tell before each of the phase's attacks; {@code stay: true}
     * keeps it up (no open or dive time then).
     */
    public record SurfaceData(
            String part,
            double rise,
            Optional<Double> open,
            Optional<Double> dive,
            Optional<Double> glow,
            Optional<Boolean> stay) {
        public SurfaceData {
            Check.positive("rise", rise);
            boolean stays = stay.orElse(false);
            Check.that(
                    stays ? open.isEmpty() && dive.isEmpty() : open.isPresent() && dive.isPresent(),
                    "a surfacing that stays up has no open or dive time; one that dives has both");
            open.ifPresent(o -> Check.positive("open", o));
            dive.ifPresent(d -> Check.positive("dive", d));
            glow.ifPresent(g -> Check.notNegative("glow", g));
        }
    }

    /**
     * M5 part E, units an arena boss releases once (the arena's Driftjelly field): {@code count}
     * units of {@code enemy} as a {@code field} over {@code lanes}, {@code on: first-surface} (its
     * surfacing part's first rise); they count among the level's enemies as they are released.
     */
    public record ReleaseData(String enemy, int count, String formation, List<Integer> lanes, String on) {
        public ReleaseData {
            Check.positive("count", count);
            Check.notEmpty("lanes", lanes);
            Check.that(formation.equals("field"), "a release is a field, was '" + formation + "'");
            Check.that(on.equals("first-surface"), "a release comes on first-surface, was '" + on + "'");
        }
    }

    /**
     * A boss phase: it ends when at most {@code until.left} of {@code until.parts} are alive or
     * {@code until.seconds} after it engaged; it fires its {@code attacks} together or the
     * {@code alternate} ones in turn, sends its {@code streams}, {@code exposes} parts that took no
     * damage before it and bends the chains {@code bend} degrees.
     *
     * @param delay part G: s from the phase engaging to its first attack and window (a concurrent
     *     attack's first volley one interval after that); left out, 1 s for a later phase that
     *     alternates (the frigate's crown opening), else 0
     * @param move part G: its opening move, invulnerable: the glide to a station and a layer, then a
     *     turn into a pose
     * @param windows part G: the parts that open and shut in turn (vulnerable only while open), and
     *     what each opening releases
     * @param slamming M5 part E: how it slams
     * @param surface M5 part E: its surfacing part
     * @param release M5 part E: what it releases once
     * @param drop M5 part E: the pickup dropped as it begins (the shield cell)
     */
    public record PhaseData(
            String name,
            Until until,
            Optional<List<String>> attacks,
            Optional<List<String>> alternate,
            Optional<StreamData> streams,
            Optional<List<String>> exposes,
            Optional<Double> bend,
            Optional<Double> delay,
            Optional<MoveData> move,
            Optional<WindowData> windows,
            Optional<SlammingData> slamming,
            Optional<SurfaceData> surface,
            Optional<ReleaseData> release,
            Optional<Pickup> drop) {
        public PhaseData {
            Check.that(attacks.isEmpty() || alternate.isEmpty(), "a phase has attacks or an alternate list, not both");
            bend.ifPresent(b -> Check.notNegative("bend", b));
            delay.ifPresent(d -> Check.notNegative("delay", d));
        }
    }

    /**
     * The end of a phase: at most {@code left} (0 when left out) of {@code parts} alive, or part G
     * {@code seconds} after the phase engaged (after its move), whichever comes first; at least one
     * of the two. M5 part E: or after {@code slams} impacts, or once {@code parts} hold less than the
     * share {@code below} of their HP (instead of {@code left}).
     */
    public record Until(
            Optional<List<String>> parts,
            Optional<Integer> left,
            Optional<Double> seconds,
            Optional<Integer> slams,
            Optional<Double> below) {
        public Until {
            Check.that(
                    parts.isPresent() || seconds.isPresent() || slams.isPresent(), "until has parts, seconds or slams");
            parts.ifPresent(list -> Check.notEmpty("parts", list));
            left.ifPresent(l -> Check.notNegative("left", l));
            Check.that(left.isEmpty() || parts.isPresent(), "until has a left only with its parts");
            Check.that(
                    below.isEmpty() || (parts.isPresent() && left.isEmpty()),
                    "until has a below with its parts, not a left");
            seconds.ifPresent(s -> Check.positive("seconds", s));
            slams.ifPresent(n -> Check.positive("slams", n));
            below.ifPresent(b -> Check.that(b > 0 && b < 1, "below is a share between 0 and 1, was " + b));
        }
    }

    /**
     * Part G: a further part layout of a boss: its {@code name}, its armoured body's {@code hitbox}
     * (the stat block's when left out) and its parts' {@code offsets} by part name ({@code [dx, dy]}
     * px from the centre, dx right, dy up; a part left out keeps its {@code part_list} offset).
     */
    public record PoseData(String name, Optional<Size> hitbox, java.util.Map<String, Point> offsets) {}

    /**
     * Part G: a phase's opening move: over {@code descend} s the boss glides to {@code to}
     * ({@code [x, y]}: px from the left edge, px below the top edge, as {@code hover.y}) and sinks or
     * rises to {@code layer}, then over {@code turn} s it turns in place into {@code pose}; each
     * left out keeps what it has (0 s for the times). It takes no damage and holds its fire until the
     * move ends.
     */
    public record MoveData(
            Optional<Point> to,
            Optional<String> layer,
            Optional<Double> descend,
            Optional<String> pose,
            Optional<Double> turn) {
        public MoveData {
            layer.ifPresent(Layers::of);
            descend.ifPresent(d -> Check.notNegative("descend", d));
            turn.ifPresent(t -> Check.notNegative("turn", t));
        }
    }

    /**
     * Part G: a phase's windows: every {@code every} s the next of the {@code groups} (part names, in
     * the order the windows cycle) with a living part on the field opens for {@code open} s (every
     * such group at once when {@code all}), the first {@code offset} s after the phase's delay. A
     * part in a group takes damage only while open. Each opening releases the next of the
     * {@code spawns} in turn from every group it opened.
     */
    public record WindowData(
            List<List<String>> groups,
            double every,
            double open,
            Optional<Double> offset,
            Optional<Boolean> all,
            Optional<List<SpawnData>> spawns) {
        public WindowData {
            Check.notEmpty("groups", groups);
            groups.forEach(group -> Check.notEmpty("groups[]", group));
            Check.positive("every", every);
            Check.positive("open", open);
            offset.ifPresent(o -> Check.notNegative("offset", o));
        }
    }

    /**
     * Part G: what an opened window group releases: {@code count} units of {@code enemy} (a group
     * with parts destroyed its share, rounded up) from its open parts, spread over {@code arc} °
     * centred on the ship, flying out at {@code speed} px/s; with a {@code glide} of s they then
     * hold for their hover time, firing, and leave down the screen. The difficulty hook
     * {@code spawns} changes the count by {@code name}.
     */
    public record SpawnData(
            String name, String enemy, int count, double speed, Optional<Double> arc, Optional<Double> glide) {
        public SpawnData {
            Check.notNegative("count", count);
            Check.positive("speed", speed);
            arc.ifPresent(a -> Check.notNegative("arc", a));
            glide.ifPresent(g -> Check.notNegative("glide", g));
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
    public record AttackChange(
            Optional<Integer> burst,
            Optional<Integer> count,
            Optional<Double> interval,
            Optional<Integer> arms,
            Optional<Double> within) {
        public AttackChange {
            arms.ifPresent(a -> Check.positive("arms", a));
            within.ifPresent(w -> Check.positive("within", w));
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
     * @param attacks changes per attack name (a boss's {@code burst} or {@code count}, an {@code interval}, a spiral's {@code arms})
     * @param sweepArc a laser sweep's arc instead, °
     * @param segments a segment chain's segments instead
     * @param regrownFanCount a regrown head's fan bullets instead
     * @param spawns part G: a boss's window spawns' counts instead, by spawn name
     * @param hoverSeconds M5 part D: the hover's time instead, s (the Wraith's hard hold)
     * @param telegraph M5 part E: an arena boss's slam telegraph instead, s (hard's 0.8)
     * @param lanes M5 part E: the lanes an arena boss's volley slams instead (hard's 3)
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
            Optional<Integer> regrownFanCount,
            Optional<java.util.Map<String, Integer>> spawns,
            Optional<Double> hoverSeconds,
            Optional<Double> telegraph,
            Optional<Integer> lanes) {
        public Hook {
            hoverSeconds.ifPresent(h -> Check.positive("hover_seconds", h));
            telegraph.ifPresent(t -> Check.positive("telegraph", t));
            lanes.ifPresent(n -> Check.positive("lanes", n));
        }
    }

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
