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
        List<SetPieceSpec> setPieces) {
    public LevelScript {
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

    /** A stretch of the scroll ending at {@code end} seconds, scrolling at {@code speed} px/s. */
    public record Section(double end, double speed) {}

    /**
     * The secondary objective: destroy at least {@code killRatio} of all enemies for
     * {@code credits} (before the credit factor), or, with {@code groups}, clear every ground
     * unit of a group (Level 02's docks) before the last of them leaves the screen, for
     * {@code credits} per group; the objective is met when every group is cleared.
     *
     * @param groups the groups' names (the ground units' {@link GroundUnit#group()} indexes them); empty for a kill ratio
     * @param escapes the slug of the enemy none of which may leave the screen alive ("nothing gets
     *     through", Level 03): met when all are destroyed, failed when one gets away; empty for none
     */
    public record Secondary(double killRatio, int credits, List<String> groups, String escapes) {
        public Secondary {
            groups = List.copyOf(groups);
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

        /** Whether the objective is that none of an enemy gets away. */
        public boolean byEscapes() {
            return !escapes.isEmpty();
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
            int secretTriggers) {
        public GroundObjectSpec {
            if (hits > 0 && (secretTriggers < 1 || secretIndex < 0)) {
                throw new IllegalArgumentException("a trigger reveals a secret, alone or with others");
            }
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
            Optional<PickupType> drop) {
        /** The body hits the ship at most once in this time. */
        public static final double HIT_INTERVAL_SECONDS = 1;
        /** The most parts a set piece can have (homing locks and pierce keys encode the part in this). */
        public static final int MAX_PARTS = 32;

        public SetPieceSpec {
            parts = List.copyOf(parts);
            passes = List.copyOf(passes);
            if (parts.isEmpty() || parts.size() > MAX_PARTS || passes.isEmpty()) {
                throw new IllegalArgumentException(slug + ": a set piece has 1–" + MAX_PARTS + " parts and a pass");
            }
            if (parts.stream().noneMatch(PartSpec::vital)) {
                throw new IllegalArgumentException(slug + ": a set piece has a vital part");
            }
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
            double firstShotSeconds) {}

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
     */
    public record RadioCue(
            CueTrigger trigger,
            double t,
            String subject,
            String speaker,
            String line,
            boolean distorted,
            String expression,
            String portrait) {
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
        ENEMY_ESCAPED
    }
}
