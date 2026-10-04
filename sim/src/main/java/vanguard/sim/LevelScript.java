package vanguard.sim;

import java.util.List;
import java.util.Optional;

/**
 * A level's script at one difficulty (design/campaign/&lt;act&gt;/&lt;level&gt;/data.yaml), built
 * by {@code vanguard.content.SimSpecs}: the scroll, the waves, the ground objects, the radio
 * cues and the objectives. The level ends when the scroll reaches the end of the last section
 * (the {@code reach-end} primary objective).
 *
 * @param number the global level number (01–50)
 * @param act the act the level belongs to
 * @param launchSeconds the non-playable launch at the start
 * @param sections back to back from t = 0
 * @param waves in time order
 * @param groundObjects every placed ground object, in time order
 * @param groundUnits every enemy fixed to the ground, in time order
 * @param secrets the number of secrets in the level
 * @param radio the radio chatter cues
 * @param secondary the secondary objective
 * @param cranes the crane hazards
 * @param debris the debris chunks drifting on the air layer, in time order
 * @param setPieces the huge set-piece units flying their passes (Level 03's Leviathan)
 * @param escort the {@code escort} primary objective's convoy, instead of only reaching the end
 * @param road the road on the ground layer that a convoy follows
 * @param targets the {@code destroy-targets} primary objective's groups (Level 05's batteries):
 *     the ground units' {@link GroundUnit#group()} indexes them; empty for another primary
 * @param sled the mass-driver sleds (Level 05)
 * @param rocks the rocks a destroyed ground unit throws in low gravity (Level 05)
 * @param groupDrops pickups dropped where a group's last unit dies when it is cleared
 */
public record LevelScript(
        int number,
        int act,
        double launchSeconds,
        List<Section> sections,
        List<WaveSpec> waves,
        List<GroundObjectSpec> groundObjects,
        List<GroundUnit> groundUnits,
        int secrets,
        List<RadioCue> radio,
        Secondary secondary,
        List<CraneSpec> cranes,
        List<DebrisSpec> debris,
        List<SetPieceSpec> setPieces,
        Optional<Escort> escort,
        Optional<Road> road,
        List<String> targets,
        Optional<SledSpec> sled,
        Optional<RockSpec> rocks,
        List<GroupDrop> groupDrops,
        Optional<Darkness> darkness) {
    public LevelScript {
        targets = List.copyOf(targets);
        groupDrops = List.copyOf(groupDrops);
        if (!targets.isEmpty() && secondary.byGroups()) {
            throw new IllegalArgumentException("groups belong to the primary or to the secondary objective");
        }
        sections = List.copyOf(sections);
        waves = List.copyOf(waves);
        groundObjects = List.copyOf(groundObjects);
        groundUnits = List.copyOf(groundUnits);
        radio = List.copyOf(radio);
        cranes = List.copyOf(cranes);
        debris = List.copyOf(debris);
        setPieces = List.copyOf(setPieces);
        if (sections.isEmpty()) {
            throw new IllegalArgumentException("a level needs at least one section");
        }
        if (escort.isPresent() && road.isEmpty()) {
            throw new IllegalArgumentException("a convoy follows the level's road");
        }
    }

    /** A level without Level 06's darkness. */
    public LevelScript(
            int number,
            int act,
            double launchSeconds,
            List<Section> sections,
            List<WaveSpec> waves,
            List<GroundObjectSpec> groundObjects,
            List<GroundUnit> groundUnits,
            int secrets,
            List<RadioCue> radio,
            Secondary secondary,
            List<CraneSpec> cranes,
            List<DebrisSpec> debris,
            List<SetPieceSpec> setPieces,
            Optional<Escort> escort,
            Optional<Road> road,
            List<String> targets,
            Optional<SledSpec> sled,
            Optional<RockSpec> rocks,
            List<GroupDrop> groupDrops) {
        this(
                number,
                act,
                launchSeconds,
                sections,
                waves,
                groundObjects,
                groundUnits,
                secrets,
                radio,
                secondary,
                cranes,
                debris,
                setPieces,
                escort,
                road,
                targets,
                sled,
                rocks,
                groupDrops,
                Optional.empty());
    }

    /**
     * A dark level (design/campaign Level 06, Darkness rules): only the ground layer and the
     * ground units are dark outside the light pools. The ship's headlight, a cone
     * {@code headlightLength} px long and {@code headlightAngle} radians wide ahead of it, is on
     * from {@code headlightFrom} s; each scripted flare falls {@code flareSeconds}, its pool of
     * {@code flareRadius} px drifting down the screen at {@code flareDrift} px/s from where it
     * starts; the {@code lights} are static pools on the ground (dome and rail lamps), entering at
     * the top edge with the scroll. {@code ambient} is how bright the ground stays outside the
     * light (presentation only). Triggers marked dark can only be hit while lit.
     */
    public record Darkness(
            double headlightFrom,
            double headlightLength,
            double headlightAngle,
            List<Flare> flares,
            double flareSeconds,
            double flareRadius,
            double flareDrift,
            List<Light> lights,
            double ambient) {
        public Darkness {
            flares = List.copyOf(flares);
            lights = List.copyOf(lights);
        }

        /** A flare fired at {@code t} s, starting {@code depth} px below the top edge at {@code x}. */
        public record Flare(double t, double x, double depth) {}

        /** Flare {@code i}'s pool centre's y at {@code levelSeconds} (meaningful while it burns). */
        public double flareY(int i, double levelSeconds) {
            Flare flare = flares.get(i);
            return PlayField.HEIGHT - flare.depth() - flareDrift * (levelSeconds - flare.t());
        }

        /** Whether flare {@code i} burns at {@code levelSeconds}. */
        public boolean flareBurning(int i, double levelSeconds) {
            double into = levelSeconds - flares.get(i).t();
            return into >= 0 && into <= flareSeconds;
        }

        /** A static light pool of {@code radius} px on the ground, entering at the top edge at {@code t} s at {@code x}. */
        public record Light(double t, double x, double radius) {}

        /** Whether the headlight is on at {@code levelSeconds}. */
        public boolean headlightOn(double levelSeconds) {
            return levelSeconds >= headlightFrom;
        }

        /**
         * Whether the box {@code size} around (x, y) is lit for the simulation: in the headlight
         * cone of the ship at (shipX, shipY) or in a burning flare's pool at {@code levelSeconds}.
         * The static pools light only the picture (no dark trigger lies in one).
         */
        public boolean lit(double x, double y, Hitbox size, double shipX, double shipY, double levelSeconds) {
            double reach = Math.max(size.width(), size.height()) / 2;
            if (headlightOn(levelSeconds)) {
                double dx = x - shipX;
                double dy = y - shipY;
                double d = Math.sqrt(dx * dx + dy * dy);
                if (d <= headlightLength + reach
                        && dy > -reach
                        && (d <= reach
                                || Math.abs(StrictMath.atan2(dx, dy))
                                        <= headlightAngle / 2 + StrictMath.atan2(reach, d))) {
                    return true;
                }
            }
            for (int i = 0; i < flares.size(); i++) {
                Flare flare = flares.get(i);
                double into = levelSeconds - flare.t();
                if (into < 0 || into > flareSeconds) {
                    continue;
                }
                double fx = flare.x();
                double fy = PlayField.HEIGHT - flare.depth() - flareDrift * into;
                if (near(x, y, fx, fy, flareRadius + reach)) {
                    return true;
                }
            }
            return false;
        }

        private static boolean near(double x, double y, double cx, double cy, double radius) {
            double dx = x - cx;
            double dy = y - cy;
            return dx * dx + dy * dy <= radius * radius;
        }
    }

    /** A level without the destroy-targets primary, sleds or rocks. */
    public LevelScript(
            int number,
            int act,
            double launchSeconds,
            List<Section> sections,
            List<WaveSpec> waves,
            List<GroundObjectSpec> groundObjects,
            List<GroundUnit> groundUnits,
            int secrets,
            List<RadioCue> radio,
            Secondary secondary,
            List<CraneSpec> cranes,
            List<DebrisSpec> debris,
            List<SetPieceSpec> setPieces,
            Optional<Escort> escort,
            Optional<Road> road) {
        this(
                number,
                act,
                launchSeconds,
                sections,
                waves,
                groundObjects,
                groundUnits,
                secrets,
                radio,
                secondary,
                cranes,
                debris,
                setPieces,
                escort,
                road,
                List.of(),
                Optional.empty(),
                Optional.empty(),
                List.of());
    }

    /** A level without a convoy or a road. */
    public LevelScript(
            int number,
            int act,
            double launchSeconds,
            List<Section> sections,
            List<WaveSpec> waves,
            List<GroundObjectSpec> groundObjects,
            List<GroundUnit> groundUnits,
            int secrets,
            List<RadioCue> radio,
            Secondary secondary,
            List<CraneSpec> cranes,
            List<DebrisSpec> debris,
            List<SetPieceSpec> setPieces) {
        this(
                number,
                act,
                launchSeconds,
                sections,
                waves,
                groundObjects,
                groundUnits,
                secrets,
                radio,
                secondary,
                cranes,
                debris,
                setPieces,
                Optional.empty(),
                Optional.empty());
    }

    /** A level without debris or set pieces. */
    public LevelScript(
            int number,
            int act,
            double launchSeconds,
            List<Section> sections,
            List<WaveSpec> waves,
            List<GroundObjectSpec> groundObjects,
            List<GroundUnit> groundUnits,
            int secrets,
            List<RadioCue> radio,
            Secondary secondary,
            List<CraneSpec> cranes) {
        this(
                number,
                act,
                launchSeconds,
                sections,
                waves,
                groundObjects,
                groundUnits,
                secrets,
                radio,
                secondary,
                cranes,
                List.of(),
                List.of());
    }

    /** The level's length in seconds. */
    public double seconds() {
        return sections.getLast().end();
    }

    /** The names of the groups the ground units' {@link GroundUnit#group()} indexes: the primary's or the secondary's. */
    public List<String> groups() {
        return targets.isEmpty() ? secondary.groups() : targets;
    }

    /**
     * The mass-driver sleds (design/world/luna, hazards): every {@code periodSeconds} from
     * {@code firstSeconds} until {@code untilSeconds} a sled shoots up the rail at {@code x}, a line
     * {@code width} px wide across the field, for {@code runSeconds}, after its lights chased for
     * {@code lightsSeconds}. It hits the ship for {@code damage} (once per sled) and blocks shots
     * and enemy bullets while it runs; the trigger that reveals {@code clampSecret} can only be hit
     * while the rail is dark.
     */
    public record SledSpec(
            double x,
            double width,
            double firstSeconds,
            double periodSeconds,
            double untilSeconds,
            double lightsSeconds,
            double runSeconds,
            double damage,
            String clampSecret) {
        public SledSpec {
            if (!(periodSeconds > lightsSeconds + runSeconds) || !(width > 0) || !(damage > 0)) {
                throw new IllegalArgumentException("a sled's period holds its lights and its run");
            }
        }
    }

    /**
     * Rocks a destroyed ground unit throws in low gravity (design/campaign, Level 05): {@code min}
     * to {@code max} of them (from the simulation's random numbers) on the air layer, each drifting
     * at {@code minSpeed}..{@code maxSpeed} px/s in a random direction until it vanishes after
     * {@code lifeSeconds}; {@code hp}, contact {@code damage}, no credits. None is thrown when the
     * unit lies within {@code clearance} px of the ship.
     */
    public record RockSpec(
            int min,
            int max,
            double minSpeed,
            double maxSpeed,
            double lifeSeconds,
            Hitbox size,
            double hp,
            double damage,
            double clearance) {
        public RockSpec {
            if (min < 0 || max < min || !(lifeSeconds > 0)) {
                throw new IllegalArgumentException("rocks: min <= max, a life");
            }
        }

        /** The debris chunk a rock is. */
        public DebrisSpec chunk() {
            return new DebrisSpec(0, 0, "rock", size, 0, -1, hp, damage, clearance);
        }
    }

    /** A pickup dropped where the last unit of group {@code group} died, when the group is cleared. */
    public record GroupDrop(int group, PickupType pickup) {}

    /**
     * The {@code escort} primary objective (design/allies; design/campaign, Level 04): a convoy of
     * {@code ally} units, one per station, rolling in from the bottom edge one every
     * {@code enterInterval} seconds from {@code enterSeconds} at {@code enterSpeed} px/s up the
     * screen to their stations, then following the road. Each unit alive at the level end pays
     * {@code credits} (before the credit factor); the objective fails when every unit is lost.
     *
     * @param stations the units' centre heights, px from the bottom edge (y up), the leading unit first
     * @param targetedBy the enemies whose aimed attacks use the target-the-objective hook in mode
     *     {@code nearest}: each aimed shot goes at the ship or the nearest unit, whichever is closer
     */
    public record Escort(
            AllySpec ally,
            List<Double> stations,
            double enterSeconds,
            double enterInterval,
            double enterSpeed,
            int credits,
            List<String> targetedBy) {
        public Escort {
            stations = List.copyOf(stations);
            targetedBy = List.copyOf(targetedBy);
            if (stations.isEmpty() || !(enterSpeed > 0) || enterInterval < 0 || credits < 0) {
                throw new IllegalArgumentException("a convoy has units that roll in at a speed");
            }
        }
    }

    /**
     * A stretch of the scroll ending at {@code end} seconds, scrolling at {@code speed} px/s. In a
     * boss {@code arena} the level clock halts at the section's end while the boss lives, and jumps
     * to that end when the boss dies earlier (design/enemies/bosses, the arena).
     */
    public record Section(double end, double speed, boolean arena) {
        public Section(double end, double speed) {
            this(end, speed, false);
        }
    }

    /**
     * The secondary objective: destroy at least {@code killRatio} of all enemies for
     * {@code credits} (before the credit factor), or, with {@code groups}, clear every ground
     * unit of a group (Level 02's docks) before the last of them leaves the screen, for
     * {@code credits} per group; the objective is met when every group is cleared.
     *
     * @param groups the groups' names (the ground units' {@link GroundUnit#group()} indexes them); empty for a kill ratio
     * @param escapes the slug of the enemy none of which may leave the screen alive ("nothing gets
     *     through", Level 03): met when all are destroyed, failed when one gets away; empty for none
     * @param killAll the enemies every unit of which must be destroyed (Level 05's "Scorched
     *     crater"), met and failed as {@code escapes}; empty for none
     * @param label the tracker's label of a kill-all objective ("NEST")
     */
    public record Secondary(
            double killRatio, int credits, List<String> groups, String escapes, List<String> killAll, String label) {
        public Secondary {
            groups = List.copyOf(groups);
            killAll = List.copyOf(killAll);
        }

        public Secondary(double killRatio, int credits, List<String> groups, String escapes) {
            this(killRatio, credits, groups, escapes, List.of(), "");
        }

        public Secondary(double killRatio, int credits, List<String> groups) {
            this(killRatio, credits, groups, "");
        }

        public Secondary(double killRatio, int credits) {
            this(killRatio, credits, List.of(), "");
        }

        /** Whether the objective is about groups rather than the kill ratio. */
        public boolean byGroups() {
            return !groups.isEmpty();
        }

        /**
         * Whether the objective is that none of an enemy gets away, or that every unit of the
         * {@code killAll} enemies is destroyed (Level 05's "Scorched crater").
         */
        public boolean byEscapes() {
            return !escapes.isEmpty() || !killAll.isEmpty();
        }

        /** Whether a unit of {@code slug} counts towards an escapes or kill-all objective. */
        public boolean counts(String slug) {
            return escapes.equals(slug) || killAll.contains(slug);
        }
    }

    /**
     * An enemy fixed to the ground layer (a turret), entering at the top edge at {@code t} at
     * {@code x}, in the secondary objective's group {@code group} (-1 for none).
     */
    public record GroundUnit(double t, double x, EnemySpec enemy, int group) {}

    /**
     * A crane hazard (design/campaign, Level 02: Crane Four): an arm hanging from a pivot above the
     * play field that swings between two angles, blinking its lights for the telegraph before each
     * swing. It is lowered from along the gantry during the telegraph before its first swing and
     * raised back after its last one. The arm deals contact damage, at most once per
     * {@link #HIT_INTERVAL_SECONDS}, and blocks every shot; a clamp at its tip counts the player's
     * hits while the arm swings and releases a secret's hidden crate after enough of them.
     *
     * @param pivotX the pivot in play-field px
     * @param pivotY the pivot, above the top edge
     * @param length the arm, px
     * @param width the arm's thickness, px
     * @param fromRadians the angle the first swing starts at, from straight down, positive to the right
     * @param toRadians the angle the first swing ends at; swings alternate between the two
     * @param swings the times the swings start, s from the level start
     * @param swingSeconds how long a swing takes
     * @param telegraphSeconds how long the lights blink before a swing
     * @param damage contact damage (shield first)
     * @param clampHits the player's hits on the clamp that release the crate; 0 for no clamp
     * @param crateCredits the crate's credits
     * @param secret the secret's name, for its radio cue
     */
    public record CraneSpec(
            double pivotX,
            double pivotY,
            double length,
            double width,
            double fromRadians,
            double toRadians,
            List<Double> swings,
            double swingSeconds,
            double telegraphSeconds,
            double damage,
            int clampHits,
            int crateCredits,
            String secret) {
        /** The arm hits the ship at most once in this time. */
        public static final double HIT_INTERVAL_SECONDS = 1;

        public CraneSpec {
            swings = List.copyOf(swings);
            if (swings.isEmpty()) {
                throw new IllegalArgumentException("a crane swings at least once");
            }
        }
    }

    /**
     * An object on the ground layer, entering at the top edge at {@code t} and scrolling with the
     * ground: a destructible ({@code hp}, {@code bounty}, {@code drop}) or a trigger that releases a
     * secret's hidden crate after {@code hits} hits.
     *
     * @param x its position in the play field
     * @param size its hit box
     * @param crateCredits the hidden crate's credits (triggers only)
     * @param secret the secret's name (triggers only), for its radio cue
     * @param hardened only {@code anti-ground} weapons damage it; other shots glance off
     * @param secretIndex a trigger's secret, its index among the level's secrets
     * @param secretTriggers how many triggers reveal that secret together (Level 03's four lifeboat
     *     lights): the crate drops when the last of them is spent; 1 for a trigger of its own
     * @param bonusDrop a second pickup a destructible drops with its {@code drop} (Level 04's supply
     *     drop: a special charge, which drops only with a special fitted)
     * @param look a destructible's sprite set, which the game draws it with (the simulation does not
     *     read it)
     */
    public record GroundObjectSpec(
            double t,
            double x,
            Hitbox size,
            double hp,
            int bounty,
            Optional<PickupType> drop,
            int hits,
            int crateCredits,
            String secret,
            boolean hardened,
            int secretIndex,
            int secretTriggers,
            Optional<PickupType> bonusDrop,
            String look,
            boolean dark,
            Optional<LevelResult.DataCore> core) {
        /** Level 01's cargo container, the look of a destructible that names none. */
        public static final String CARGO_CONTAINER = "cargo-container";

        /** Without Level 06's darkness and data core. */
        public GroundObjectSpec(
                double t,
                double x,
                Hitbox size,
                double hp,
                int bounty,
                Optional<PickupType> drop,
                int hits,
                int crateCredits,
                String secret,
                boolean hardened,
                int secretIndex,
                int secretTriggers,
                Optional<PickupType> bonusDrop,
                String look) {
            this(
                    t,
                    x,
                    size,
                    hp,
                    bounty,
                    drop,
                    hits,
                    crateCredits,
                    secret,
                    hardened,
                    secretIndex,
                    secretTriggers,
                    bonusDrop,
                    look,
                    false,
                    Optional.empty());
        }

        public GroundObjectSpec {
            if (hits > 0 && (secretTriggers < 1 || secretIndex < 0)) {
                throw new IllegalArgumentException("a trigger reveals a secret, alone or with others");
            }
        }

        /** With the cargo container's look. */
        public GroundObjectSpec(
                double t,
                double x,
                Hitbox size,
                double hp,
                int bounty,
                Optional<PickupType> drop,
                int hits,
                int crateCredits,
                String secret,
                boolean hardened,
                int secretIndex,
                int secretTriggers,
                Optional<PickupType> bonusDrop) {
            this(
                    t,
                    x,
                    size,
                    hp,
                    bounty,
                    drop,
                    hits,
                    crateCredits,
                    secret,
                    hardened,
                    secretIndex,
                    secretTriggers,
                    bonusDrop,
                    CARGO_CONTAINER);
        }

        /** Without a bonus drop. */
        public GroundObjectSpec(
                double t,
                double x,
                Hitbox size,
                double hp,
                int bounty,
                Optional<PickupType> drop,
                int hits,
                int crateCredits,
                String secret,
                boolean hardened,
                int secretIndex,
                int secretTriggers) {
            this(
                    t,
                    x,
                    size,
                    hp,
                    bounty,
                    drop,
                    hits,
                    crateCredits,
                    secret,
                    hardened,
                    secretIndex,
                    secretTriggers,
                    Optional.empty());
        }

        /** A destructible, or a trigger that reveals the level's first secret on its own. */
        public GroundObjectSpec(
                double t,
                double x,
                Hitbox size,
                double hp,
                int bounty,
                Optional<PickupType> drop,
                int hits,
                int crateCredits,
                String secret,
                boolean hardened) {
            this(t, x, size, hp, bounty, drop, hits, crateCredits, secret, hardened, hits > 0 ? 0 : -1, 1);
        }

        /** Whether it is a trigger rather than a destructible. */
        public boolean trigger() {
            return hits > 0;
        }
    }

    /**
     * A debris chunk (design/world/earth-orbit, hazards): it enters at the top edge at {@code t}
     * at {@code x} (moved sideways when that is too close to the ship) and drifts across the
     * screen at ({@code vx}, {@code vy}) px/s on the air layer, y up. It blocks every shot and
     * enemy bullet that touches it; a large chunk is indestructible and deals contact damage, at
     * most once per {@link #HIT_INTERVAL_SECONDS}; a small one breaks after {@code hp} damage and
     * pays nothing.
     *
     * @param chunk the chunk's name in the level ({@code large-a}); its sprite is {@code debris-<chunk>}
     * @param hp a small chunk's HP; infinite for a large one
     * @param damage a large chunk's contact damage; 0 for a small one
     * @param clearance the least distance from the ship it enters at, px
     */
    public record DebrisSpec(
            double t,
            double x,
            String chunk,
            Hitbox size,
            double vx,
            double vy,
            double hp,
            double damage,
            double clearance) {
        /** A large chunk hits the ship at most once in this time. */
        public static final double HIT_INTERVAL_SECONDS = 1;

        public DebrisSpec {
            if (!(vy < 0)) {
                throw new IllegalArgumentException("a debris chunk drifts down the screen");
            }
        }

        /** Whether it is a large, indestructible chunk. */
        public boolean large() {
            return Double.isInfinite(hp);
        }

        /** Its sprite's name: {@code debris-large-a}. */
        public String sprite() {
            return "debris-" + chunk;
        }
    }

    /**
     * A huge multi-part unit that flies its {@code passes} one after another (design/enemies/space/
     * leviathan): an armoured {@code body} that makes shots glance and deals {@code contactDamage}
     * on the player's layer, at most once per {@link #HIT_INTERVAL_SECONDS}, and parts at fixed
     * offsets that take the hits. Destroying the vital part destroys the rest and the unit, which
     * drops its {@code drop}; a unit that ends its last pass alive has escaped.
     *
     * @param slug its enemy's directory name
     * @param size its sprite, px, facing down; the unit is gone when this box around it has left the screen
     * @param body the armoured body's hit box around its centre
     */
    public record SetPieceSpec(
            String slug,
            Hitbox size,
            Hitbox body,
            double contactDamage,
            List<PartSpec> parts,
            List<Pass> passes,
            Optional<PickupType> drop,
            Optional<BossSpec> boss) {
        /** The body hits the ship at most once in this time. */
        public static final double HIT_INTERVAL_SECONDS = 1;
        /** The most parts a set piece can have (homing locks and pierce keys encode the part in this). */
        public static final int MAX_PARTS = 32;

        public SetPieceSpec {
            parts = List.copyOf(parts);
            passes = List.copyOf(passes);
            if (parts.isEmpty() || parts.size() > MAX_PARTS || passes.isEmpty() == boss.isEmpty()) {
                throw new IllegalArgumentException(
                        slug + ": a set piece has 1–" + MAX_PARTS + " parts and passes, or a boss script");
            }
            if (parts.stream().noneMatch(PartSpec::vital)) {
                throw new IllegalArgumentException(slug + ": a set piece has a vital part");
            }
        }

        /** A set piece flying passes, without a boss script. */
        public SetPieceSpec(
                String slug,
                Hitbox size,
                Hitbox body,
                double contactDamage,
                List<PartSpec> parts,
                List<Pass> passes,
                Optional<PickupType> drop) {
            this(slug, size, body, contactDamage, parts, passes, drop, Optional.empty());
        }

        /** Whether it is a boss (it has a boss script rather than passes). */
        public boolean isBoss() {
            return boss.isPresent();
        }

        /** Its bounty: the parts' together. */
        public int bounty() {
            return parts.stream().mapToInt(PartSpec::bounty).sum();
        }
    }

    /**
     * A part of a set piece: a hit box at ({@code dx}, {@code dy}) px from the centre in the unit's
     * own frame (facing down the screen: dx to the right, dy up towards its tail), turned with the
     * pass's heading.
     *
     * @param vital destroying it destroys the rest of the unit
     * @param gun the attack it fires while the unit is on the player's layer
     * @param firstShotSeconds when it first fires after the unit has reached the player's layer
     * @param multiplier the damage it takes is multiplied by this (a weak point: a boss's lime eyes)
     */
    public record PartSpec(
            String name,
            double dx,
            double dy,
            Hitbox box,
            double hp,
            boolean vital,
            int bounty,
            Optional<EnemyGun> gun,
            double firstShotSeconds,
            double multiplier) {
        public PartSpec(
                String name,
                double dx,
                double dy,
                Hitbox box,
                double hp,
                boolean vital,
                int bounty,
                Optional<EnemyGun> gun,
                double firstShotSeconds) {
            this(name, dx, dy, box, hp, vital, bounty, gun, firstShotSeconds, 1);
        }
    }

    /**
     * One pass of a set piece: from the first waypoint's time it flies along {@code path} on
     * {@code layer} with a fixed heading. A pass on the player's layer arrives on {@code high-air}
     * and descends at {@code descendAt} over {@code descentSeconds}; at {@code leaveAt} it rises
     * back to {@code high-air} over the same time and leaves straight up through the top edge at
     * {@code leaveSpeed}. Until then it holds at the last waypoint once the path is flown. A pass
     * that does not descend ends with its path.
     *
     * @param headingRadians where it faces, radians clockwise from straight down the screen (as
     *     {@link Enemy#facing()}: facing right is -pi/2)
     * @param descendAt level seconds; infinite for a pass that does not descend
     * @param leaveAt level seconds; infinite for a pass that does not descend
     */
    public record Pass(
            String name,
            Layer layer,
            double headingRadians,
            List<Waypoint> path,
            double descendAt,
            double descentSeconds,
            double leaveAt,
            double leaveSpeed) {
        public Pass {
            path = List.copyOf(path);
            if (path.isEmpty()) {
                throw new IllegalArgumentException(name + ": a pass has a path");
            }
            for (int i = 1; i < path.size(); i++) {
                if (!(path.get(i).t() > path.get(i - 1).t())) {
                    throw new IllegalArgumentException(name + ": the waypoints are in time order");
                }
            }
            if (Double.isFinite(descendAt) && !(leaveAt > descendAt + descentSeconds && leaveSpeed > 0)) {
                throw new IllegalArgumentException(name + ": it leaves after its descent, at a speed");
            }
        }

        /** A pass that flies its path on one layer and ends with it. */
        public static Pass flight(String name, Layer layer, double headingRadians, List<Waypoint> path) {
            return new Pass(
                    name, layer, headingRadians, path, Double.POSITIVE_INFINITY, 0, Double.POSITIVE_INFINITY, 0);
        }

        /** Whether it descends to the player's layer and leaves again. */
        public boolean descends() {
            return Double.isFinite(descendAt);
        }

        /** When it starts, level seconds. */
        public double start() {
            return path.getFirst().t();
        }
    }

    /** Where a set piece's centre is at {@code t} level seconds: play-field px, y up. */
    public record Waypoint(double t, double x, double y) {}

    /**
     * A radio chatter line and what triggers it.
     *
     * @param t seconds from the level start, for {@link CueTrigger#TIME}
     * @param subject the enemy slug of a first kill, the secret's name or the group's name; empty otherwise
     * @param expression the speaker's portrait expression ({@code neutral}, {@code grim}, {@code fierce});
     *     presentation only, nothing in the simulation reads it
     * @param portrait the portrait's speaker when it is not the speaker's own (a generic one);
     *     presentation only
     * @param requiresSpecial it starts only with a special fitted
     * @param alliesMin a level-end cue starts only with at least this many convoy units home
     * @param alliesMax ... and at most this many
     */
    public record RadioCue(
            CueTrigger trigger,
            double t,
            String subject,
            String speaker,
            String line,
            boolean distorted,
            String expression,
            String portrait,
            boolean requiresSpecial,
            int alliesMin,
            int alliesMax) {
        public RadioCue(
                CueTrigger trigger,
                double t,
                String subject,
                String speaker,
                String line,
                boolean distorted,
                String expression,
                String portrait) {
            this(trigger, t, subject, speaker, line, distorted, expression, portrait, false, 0, Integer.MAX_VALUE);
        }

        public RadioCue(
                CueTrigger trigger,
                double t,
                String subject,
                String speaker,
                String line,
                boolean distorted,
                String expression) {
            this(trigger, t, subject, speaker, line, distorted, expression, speaker);
        }
    }

    public enum CueTrigger {
        TIME,
        FIRST_KILL,
        SECRET,
        SECONDARY_OBJECTIVE,
        LEVEL_END,
        /** A group of the secondary objective was cleared; the subject is its name. */
        GROUP_CLEARED,
        /** A group was lost; the subject is its name. */
        GROUP_LOST,
        /** The first group of the attempt was lost. */
        FIRST_GROUP_LOST,
        /** The first unit of an enemy left the screen alive (a set piece: at the end of its last pass); the subject is its slug. */
        ENEMY_ESCAPED,
        /** The first hit on a convoy unit in the attempt; its line may name the unit ({@code {ally}}). */
        FIRST_ALLY_HIT,
        /** The first convoy unit lost in the attempt; its line may name the unit ({@code {ally}}). */
        FIRST_ALLY_LOST,
        /** The primary objective failed: the line the mission failed screen shows, not played on the radio. */
        MISSION_FAILED,
        /** A boss entered a phase after its first; the subject is the phase's name. */
        BOSS_PHASE,
        /** A boss was destroyed; the subject is its slug. */
        BOSS_DESTROYED
    }
}
