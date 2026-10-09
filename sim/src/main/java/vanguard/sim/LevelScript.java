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
 * @param darkness Level 06's darkness
 * @param tows part G: friendly craft towing a secret's crate on a cable (Level 07's lifeboat tow)
 * @param partDrops part G: pickups dropped by a set piece's parts when they are shot off (Level
 *     07's first destroyed bay sac)
 * @param holds M5 part C: the hold zones (Level 09's node clusters), where the scroll eases down
 *     until their groups are destroyed and the level clock slows with it
 * @param collapse M5 part C: the collapse (Level 09's arcology) once its groups are cleared
 * @param water M5 part E (user decision E1 = a): the whole play field is water (Levels 11–13): the
 *     torpedo ({@link WeaponSpec.Delivery#TORPEDO}) runs only here; elsewhere its mount fires nothing
 * @param convoy M5 part E: a naval convoy outside the objectives (Level 11's cargo ships and frigate),
 *     which never fails the mission; never beside an {@code escort}
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
        Optional<Darkness> darkness,
        List<TowSpec> tows,
        List<PartDrop> partDrops,
        List<Hold> holds,
        Optional<Collapse> collapse,
        boolean water,
        Optional<Naval> convoy) {
    public LevelScript {
        holds = List.copyOf(holds);
        int groupCount = groups(targets, secondary).size();
        for (Hold hold : holds) {
            for (int group : hold.groups()) {
                if (group < 0 || group >= groupCount) {
                    throw new IllegalArgumentException("a hold waits for groups of the level, not " + group);
                }
            }
        }
        for (int group : collapse.map(Collapse::groups).orElse(List.of())) {
            if (group < 0 || group >= groupCount) {
                throw new IllegalArgumentException("a collapse waits for groups of the level, not " + group);
            }
        }
        targets = List.copyOf(targets);
        groupDrops = List.copyOf(groupDrops);
        tows = List.copyOf(tows);
        partDrops = List.copyOf(partDrops);
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
        if (escort.isPresent() && escort.get().air().isEmpty() && road.isEmpty()) {
            throw new IllegalArgumentException("a convoy follows the level's road");
        }
        if (convoy.isPresent() && escort.isPresent()) {
            throw new IllegalArgumentException("a naval convoy is not an escort: a level has one or the other");
        }
    }

    /** A level without M5 part E's naval convoy. */
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
            List<GroupDrop> groupDrops,
            Optional<Darkness> darkness,
            List<TowSpec> tows,
            List<PartDrop> partDrops,
            List<Hold> holds,
            Optional<Collapse> collapse,
            boolean water) {
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
                darkness,
                tows,
                partDrops,
                holds,
                collapse,
                water,
                Optional.empty());
    }

    /** A level over land: without M5 part E's water. */
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
            List<GroupDrop> groupDrops,
            Optional<Darkness> darkness,
            List<TowSpec> tows,
            List<PartDrop> partDrops,
            List<Hold> holds,
            Optional<Collapse> collapse) {
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
                darkness,
                tows,
                partDrops,
                holds,
                collapse,
                false);
    }

    /** M5 part E: this level over water ({@code true}) or over land. */
    public LevelScript withWater(boolean overWater) {
        return new LevelScript(
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
                darkness,
                tows,
                partDrops,
                holds,
                collapse,
                overWater,
                convoy);
    }

    /** M5 part E: this level with the naval convoy {@code naval} (a test's own level). */
    public LevelScript withConvoy(Naval naval) {
        return new LevelScript(
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
                darkness,
                tows,
                partDrops,
                holds,
                collapse,
                water,
                Optional.of(naval));
    }

    /** A level without M5 part C's hold zones and collapse. */
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
            List<GroupDrop> groupDrops,
            Optional<Darkness> darkness,
            List<TowSpec> tows,
            List<PartDrop> partDrops) {
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
                darkness,
                tows,
                partDrops,
                List.of(),
                Optional.empty());
    }

    /**
     * M5 part C (design/campaign Level 09, user decisions D2 and D3 = a): a hold zone. When the first
     * unit of its {@code groups} reaches {@code depth} px below the top edge, the scroll eases over
     * {@code rampSeconds} to {@code speed} px/s and stays there until every unit of those groups is
     * gone (a hold over the {@link Collapse collapse}'s groups whose clearing started it: until its
     * fall ends); then it eases back to the section's speed. There is no timeout: a unit that leaves
     * the screen alive fails a destroy-targets primary at once.
     *
     * @param groups indexes into {@link LevelScript#groups()}
     */
    public record Hold(List<Integer> groups, double depth, double speed, double rampSeconds) {
        public Hold {
            groups = List.copyOf(groups);
            if (groups.isEmpty()) {
                throw new IllegalArgumentException("a hold waits for at least one group");
            }
            if (!(speed > 0) || !(rampSeconds > 0)) {
                throw new IllegalArgumentException("a hold eases to a speed over a ramp");
            }
        }

        /** Whether it waits for group {@code group}. */
        public boolean waitsFor(int group) {
            for (int i = 0; i < groups.size(); i++) {
                if (groups.get(i) == group) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * M5 part C (design/campaign Level 09, user decision D5 = a; round 31's look c): the collapse.
     * When every unit of its {@code groups} is destroyed (none got away), its tower leans for {@code
     * warningSeconds} (the warning), then drops for {@code dropSeconds}; at the impact its blast rolls
     * out from the tower's foot: a ring round the footprint's centre ({@code x}, half way between
     * {@code bottom} and {@code top}) whose radius grows from {@code blastFrom} to {@code blastTo} px
     * over {@code blastSeconds}, eased 1 − (1 − t)² (the dust blast's front). Every ground unit in the
     * band (from {@code bottom} to {@code top}, the whole width) whose centre the ring reaches is
     * destroyed, paying and scoring as an Airstrike kill; air units and the player are untouched. Its
     * dust settles {@code settleSeconds} after the impact (the heavy dust's ramp out, at least the
     * blast's time): a hold over its groups lasts until then (user decision, 2026-10-07). The
     * band lies on the ground (ground positions: px from the screen's bottom edge at the level start,
     * as the backdrop's pieces), so it scrolls with the ground and covers the same ground (the tower
     * that falls) whenever its groups are cleared. The real clock times it, not the level clock.
     *
     * @param groups indexes into {@link LevelScript#groups()}
     * @param x the footprint's centre, px from the play field's left edge
     */
    public record Collapse(
            List<Integer> groups,
            double warningSeconds,
            double dropSeconds,
            double blastSeconds,
            double blastFrom,
            double blastTo,
            double settleSeconds,
            double x,
            double bottom,
            double top) {
        public Collapse {
            groups = List.copyOf(groups);
            if (groups.isEmpty()) {
                throw new IllegalArgumentException("a collapse waits for at least one group");
            }
            if (!(warningSeconds >= 0) || !(dropSeconds > 0) || !(blastSeconds > 0)) {
                throw new IllegalArgumentException("a collapse drops after its warning, then its blast rolls out");
            }
            if (!(blastFrom >= 0) || !(blastTo > blastFrom)) {
                throw new IllegalArgumentException("a collapse's blast grows outward");
            }
            if (!(settleSeconds >= blastSeconds)) {
                throw new IllegalArgumentException("a collapse's dust settles after its blast");
            }
            if (!(top > bottom)) {
                throw new IllegalArgumentException("a collapse's band has its top ahead of its bottom");
            }
        }

        /** The blast's kill radius {@code seconds} after the impact, px: eased out from {@code blastFrom} to {@code blastTo}. */
        public double blastRadius(double seconds) {
            double t = Math.clamp(seconds / blastSeconds, 0, 1);
            return blastFrom + (blastTo - blastFrom) * (1 - (1 - t) * (1 - t));
        }
    }

    /** A level without part G's tows and part drops. */
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
            List<GroupDrop> groupDrops,
            Optional<Darkness> darkness) {
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
                darkness,
                List.of(),
                List.of());
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
        return groups(targets, secondary);
    }

    private static List<String> groups(List<String> targets, Secondary secondary) {
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
     * Part G: a pickup dropped where the {@code nth} (from 1) of the {@code parts} of the set piece
     * {@code slug} to be shot off breaks (Level 07: the Brood Carrier's first destroyed bay sac).
     * Parts lost with the vital part, in the unit's death, drop nothing.
     *
     * @param nth from 1; {@link Integer#MAX_VALUE} for the last of them
     * @param parts indexes into the set piece's parts
     */
    public record PartDrop(String slug, List<Integer> parts, int nth, PickupType pickup) {
        public PartDrop {
            parts = List.copyOf(parts);
            if (parts.isEmpty() || nth < 1) {
                throw new IllegalArgumentException(slug + ": a part drop names its parts and which of them drops it");
            }
        }

        /** The {@code nth} for the last of the parts. */
        public static final int LAST = Integer.MAX_VALUE;

        /** Which shot-off part drops it: from 1, the last one for {@link #LAST}. */
        public int dropsAt() {
            return nth == LAST ? parts.size() : nth;
        }
    }

    /**
     * Part G: a friendly craft drifting on the air layer that tows a secret's crate on a cable
     * (Level 07's lifeboat tow). The {@code boat} enters at the top edge at {@code t} with its centre
     * at {@code x} and drifts at ({@code vx}, {@code vy}) px/s, y up; the {@code pod} hangs at
     * ({@code podDx}, {@code podDy}) from the boat's centre. Shots, bullets and the ship pass the boat
     * and the pod; only the {@code cable}, a hit box midway between them, takes the player's shots,
     * and {@code hits} of them cut it: the pod falls free as the hidden crate of {@code secret}.
     *
     * @param boat the boat's box (its sprite), px
     * @param pod the pod's box, px
     * @param cable the cable's hit box, px, centred midway between the boat's and the pod's centres
     * @param crateCredits the crate's credits
     * @param secret the secret's name, for its radio cue
     */
    public record TowSpec(
            double t,
            double x,
            double vx,
            double vy,
            Hitbox boat,
            Hitbox pod,
            double podDx,
            double podDy,
            Hitbox cable,
            int hits,
            int crateCredits,
            String secret) {
        public TowSpec {
            if (!(vy < 0) || hits < 1) {
                throw new IllegalArgumentException(secret + ": a tow drifts down the screen and its cable takes hits");
            }
        }
    }

    /**
     * The {@code escort} primary objective (design/allies; design/campaign, Level 04): a convoy of
     * {@code ally} units, one per station, rolling in from the bottom edge one every
     * {@code enterInterval} seconds from {@code enterSeconds} at {@code enterSpeed} px/s up the
     * screen to their stations, then following the road. Each unit alive at the level end pays
     * {@code credits} (before the credit factor); the objective fails when every unit is lost. M5
     * part D: with {@code air}, an air escort instead (Level 10's shuttles): the units hold stations
     * in a band with no road, see {@link Air}; then {@code stations} are the stations' heights and
     * the {@code enter} numbers are not used ({@link #air(AllySpec, Air, int)}).
     *
     * @param stations the units' centre heights, px from the bottom edge (y up), the leading unit first
     * @param targetedBy the enemies whose aimed attacks use the target-the-objective hook in mode
     *     {@code nearest}: each aimed shot goes at the ship or the nearest unit, whichever is closer
     * @param air M5 part D: the air escort's stations, liftoff, climb-out and scripted loss
     */
    public record Escort(
            AllySpec ally,
            List<Double> stations,
            double enterSeconds,
            double enterInterval,
            double enterSpeed,
            int credits,
            List<String> targetedBy,
            Optional<Air> air) {
        public Escort {
            stations = List.copyOf(stations);
            targetedBy = List.copyOf(targetedBy);
            if (stations.isEmpty() || !(enterSpeed > 0) || enterInterval < 0 || credits < 0) {
                throw new IllegalArgumentException("a convoy has units that roll in at a speed");
            }
            if (air.isPresent() && air.get().stations().size() != stations.size()) {
                throw new IllegalArgumentException("an air escort has one station per unit");
            }
            if (air.isPresent() && !targetedBy.isEmpty()) {
                throw new IllegalArgumentException("an air escort has no target-the-objective hook");
            }
        }

        /** A ground convoy (Level 04). */
        public Escort(
                AllySpec ally,
                List<Double> stations,
                double enterSeconds,
                double enterInterval,
                double enterSpeed,
                int credits,
                List<String> targetedBy) {
            this(ally, stations, enterSeconds, enterInterval, enterSpeed, credits, targetedBy, Optional.empty());
        }

        /** M5 part D: an air escort of {@code ally} units flying {@code air}, each saveable unit home paying {@code credits}. */
        public static Escort air(AllySpec ally, Air air, int credits) {
            return new Escort(
                    ally,
                    air.stations().stream().map(Station::y).toList(),
                    0,
                    0,
                    1,
                    credits,
                    List.of(),
                    Optional.of(air));
        }

        /** The number of units. */
        public int units() {
            return stations.size();
        }

        /** The unit lost in the scripted loss, from 0; -1 for none. */
        public int scriptedUnit() {
            return air.flatMap(Air::scriptedLoss).map(ScriptedLoss::unit).orElse(-1);
        }

        /**
         * The units the player can save: every unit but the scripted loss's (M5 part D). They pay at
         * the level end, they count for the level-end cues, and the objective fails when all are lost.
         */
        public int saveable() {
            return units() - (scriptedUnit() >= 0 ? 1 : 0);
        }
    }

    /**
     * M5 part E (design/allies, convoy cargo ship and escort frigate; design/campaign Level 11; user
     * decision E8 = a and the stated defaults of 2026-10-08): a naval convoy outside the objectives.
     * Its units hold screen-space stations at the scroll speed; when the scroll halts in the boss's
     * arena each glides over {@code glideSeconds} (on the real steps) to its lane, centred at
     * {@code laneY}, or, a unit that {@code leaves}, off the bottom edge; when the halt ends (the boss
     * died) they hold there, or at a unit's hold point, until the sea has scrolled {@code holdClear}
     * px (Level 11: Platform Tiamat's deck has passed the bottom edge), then glide back to their
     * stations. Only what its {@link AllySpec} lists hurts a unit (the boss's slams); it never fails
     * the mission.
     *
     * @param laneY px from the bottom edge (y up): a unit's centre in its lane
     * @param holdClear px of scroll after the halt before the units glide back; 0 for at once
     * @param lanes the arena's lanes (the boss's); 0 without lanes
     * @param laneWidth px; lane 1 starts at the left edge
     */
    public record Naval(
            List<NavalUnit> units, double glideSeconds, double laneY, int lanes, double laneWidth, double holdClear) {
        public Naval {
            units = List.copyOf(units);
            if (units.isEmpty() || !(glideSeconds > 0) || lanes < 0 || (lanes > 0 && !(laneWidth > 0))) {
                throw new IllegalArgumentException("a naval convoy has units, a glide and lanes of a width");
            }
            if (!(holdClear >= 0)) {
                throw new IllegalArgumentException("a naval convoy holds clear for 0 px or more");
            }
            if (lanes * laneWidth > PlayField.WIDTH + 1e-9) {
                throw new IllegalArgumentException("the lanes lie on the play field");
            }
            for (NavalUnit unit : units) {
                if (unit.lane() > lanes) {
                    throw new IllegalArgumentException(unit.name() + ": no lane " + unit.lane());
                }
            }
        }

        /** Without a hold after the halt: the units glide back to their stations when it ends. */
        public Naval(List<NavalUnit> units, double glideSeconds, double laneY, int lanes, double laneWidth) {
            this(units, glideSeconds, laneY, lanes, laneWidth, 0);
        }

        /** The centre x of lane {@code lane} (1-based), px from the left edge. */
        public double laneX(int lane) {
            return (lane - 0.5) * laneWidth;
        }

        /** The units that can be damaged (the frigate cannot): the ones an afloat objective counts. */
        public int damageable() {
            int count = 0;
            for (NavalUnit unit : units) {
                count += unit.ally().damageable() ? 1 : 0;
            }
            return count;
        }
    }

    /**
     * M5 part E: a naval convoy's unit: an {@code ally} named {@code name} (what {@code {ally}} becomes
     * in a radio line: "Halvorsen") at its station ({@code x} px from the left edge, {@code y} px from
     * the bottom edge, y up), gliding to its arena {@code lane} (1-based) at the halt, or, with {@code
     * lane} 0, off the bottom edge ({@code leaves: true}) until the boss is down. When the halt ends it
     * holds where it is, or glides to its hold point ({@code holdX}, {@code holdY}, y up; NaN for none),
     * until the sea has scrolled its convoy's {@link Naval#holdClear()}.
     */
    public record NavalUnit(AllySpec ally, String name, double x, double y, int lane, double holdX, double holdY) {
        public NavalUnit {
            if (name.isBlank() || lane < 0) {
                throw new IllegalArgumentException("a naval convoy unit has a name and a lane from 1, or 0 to leave");
            }
            if (Double.isNaN(holdX) != Double.isNaN(holdY)) {
                throw new IllegalArgumentException(name + ": a hold point has an x and a y");
            }
        }

        /** Without a hold point: it holds where it is after the halt. */
        public NavalUnit(AllySpec ally, String name, double x, double y, int lane) {
            this(ally, name, x, y, lane, Double.NaN, Double.NaN);
        }

        /** Whether it glides to a hold point when the halt ends. */
        public boolean holds() {
            return !Double.isNaN(holdX);
        }

        /** Whether it drops back off the bottom edge at the halt instead of taking a lane. */
        public boolean leaves() {
            return lane == 0;
        }
    }

    /**
     * M5 part D (design/allies, evacuation shuttle; design/campaign Level 10; user decisions D1, D4
     * and D5 = a): an air escort. Each unit holds its {@link Station} in the band, drifting on its
     * lane sway on the level clock and never reacting to threats; there is no road and no hook. With a
     * {@code liftoff} the units stand on their pads (scrolling with the ground) until it and then
     * climb to their stations; with a {@code climb} they climb out off the top edge at its end. A
     * unit is untouchable (enemy fire and contact pass through it) during the liftoff and the
     * climb-out and, the scripted loss's unit, until its loss.
     */
    public record Air(
            List<Station> stations,
            Optional<Liftoff> liftoff,
            Optional<Climb> climb,
            Optional<ScriptedLoss> scriptedLoss) {
        public Air {
            stations = List.copyOf(stations);
            if (stations.isEmpty()) {
                throw new IllegalArgumentException("an air escort has at least one station");
            }
            if (liftoff.isPresent() && liftoff.get().pads().size() != stations.size()) {
                throw new IllegalArgumentException("a liftoff has one pad per unit");
            }
            if (scriptedLoss.isPresent()
                    && (scriptedLoss.get().unit() < 0 || scriptedLoss.get().unit() >= stations.size())) {
                throw new IllegalArgumentException("a scripted loss takes one of the units");
            }
        }
    }

    /**
     * An air escort unit's station: it flies at ({@code x} + {@code swayX} sin θ, {@code y} −
     * {@code swayY} sin 2θ), θ = 2π (t ÷ {@code period} + {@code phase}), t the level clock: a lazy
     * figure-eight round the station (the data's sway is down the screen, so it is subtracted here),
     * the sines from {@link Trig} (deterministic and allocation-free).
     *
     * @param x px from the play field's left edge
     * @param y px from the bottom edge (y up)
     * @param swayX px either side
     * @param swayY px up and down (positive: down the screen at θ = 45°, as in the data)
     * @param period s of one figure-eight
     * @param phase 0–1, its start in the figure-eight
     */
    public record Station(double x, double y, double swayX, double swayY, double period, double phase) {
        public Station {
            if (!(period > 0)) {
                throw new IllegalArgumentException("a station's sway has a period");
            }
        }

        /** The angle θ at {@code seconds} on the level clock. */
        private double theta(double seconds) {
            return 2 * StrictMath.PI * (seconds / period + phase);
        }

        /** Its x at {@code seconds} on the level clock. */
        public double xAt(double seconds) {
            return x + swayX * Trig.sin(theta(seconds));
        }

        /** Its y (up) at {@code seconds} on the level clock. */
        public double yAt(double seconds) {
            return y - swayY * Trig.sin(2 * theta(seconds));
        }
    }

    /**
     * The air escort's liftoff: until {@code t} each unit stands on its pad, scrolling with the
     * ground, so that it is at its pad's screen point at {@code t}; from there its screen position
     * eases (smoothstep) to its station over {@code seconds}, rising from the ground layer's scale
     * to the air scale.
     *
     * @param pads one screen point per unit at {@code t}
     */
    public record Liftoff(double t, double seconds, List<Pad> pads) {
        public Liftoff {
            pads = List.copyOf(pads);
            if (t < 0 || !(seconds > 0)) {
                throw new IllegalArgumentException("a liftoff starts in the level and takes a while");
            }
        }
    }

    /** A pad's screen point: px from the left edge, px from the bottom edge (y up). */
    public record Pad(double x, double y) {}

    /** The climb-out: from {@code t} the units alive climb off the top edge over {@code seconds}; each is home. */
    public record Climb(double t, double seconds) {
        public Climb {
            if (t < 0 || !(seconds > 0)) {
                throw new IllegalArgumentException("a climb-out starts in the level and takes a while");
            }
        }
    }

    /**
     * The scripted loss (Level 10's Lifeline Three, user decision D4 = a): the unit {@code unit}
     * (from 0) is untouchable until {@code t}; at {@code t} − {@code glow} the glow starts over it
     * ({@link SimEvents.Type#LOSS_GLOW}), at {@code t} the lance takes it ({@link
     * SimEvents.Type#SCRIPTED_LOSS}): it cannot be prevented, costs no pay, does not count toward the
     * fail and raises no loss cue.
     */
    public record ScriptedLoss(int unit, double t, double glow) {
        public ScriptedLoss {
            if (glow < 0 || glow > t) {
                throw new IllegalArgumentException("a scripted loss's glow starts in the level");
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
     * @param label the tracker's label of a kill-all or parts objective ("NEST")
     * @param partsOf part G: the set piece ("brood-carrier") whose {@code parts} must all be shot off
     *     before its boss phase {@code beforePhase} ends (Level 07's "Gut the bays"): met when the
     *     last of them is shot off, failed when the boss enters a later phase, or dies, with one of
     *     them alive; empty for none
     * @param parts indexes into that set piece's parts
     * @param beforePhase the index of the boss phase they must die in or before
     * @param tag M5 part C (user decision D6 = a): scopes {@code escapes} to the units of the waves
     *     with this tag (Level 09's {@code bridge} packs); empty for every unit of the enemy
     * @param afloat M5 part E (user decision E8 = a, Level 11's "Convoy afloat"): met at the level
     *     boss's death with every naval convoy unit that can be damaged afloat, failed at the first
     *     sinking; {@code label} is the tracker's
     */
    public record Secondary(
            double killRatio,
            int credits,
            List<String> groups,
            String escapes,
            List<String> killAll,
            String label,
            String partsOf,
            List<Integer> parts,
            int beforePhase,
            String tag,
            boolean afloat) {
        public Secondary {
            groups = List.copyOf(groups);
            killAll = List.copyOf(killAll);
            parts = List.copyOf(parts);
            if (parts.isEmpty() != partsOf.isEmpty()) {
                throw new IllegalArgumentException("a parts objective names its set piece and its parts");
            }
            if (!tag.isEmpty() && escapes.isEmpty()) {
                throw new IllegalArgumentException("only an escapes objective is scoped to a wave tag");
            }
            if (afloat && (!groups.isEmpty() || !escapes.isEmpty() || !killAll.isEmpty() || !parts.isEmpty())) {
                throw new IllegalArgumentException("an afloat objective counts the convoy, nothing else");
            }
        }

        /** Without M5 part E's afloat. */
        public Secondary(
                double killRatio,
                int credits,
                List<String> groups,
                String escapes,
                List<String> killAll,
                String label,
                String partsOf,
                List<Integer> parts,
                int beforePhase,
                String tag) {
            this(killRatio, credits, groups, escapes, killAll, label, partsOf, parts, beforePhase, tag, false);
        }

        /**
         * M5 part E (user decision E8 = a): every naval convoy unit that can be damaged afloat at the
         * level boss's death, for {@code credits}, with the tracker's {@code label}.
         */
        public static Secondary afloat(int credits, String label) {
            return new Secondary(0, credits, List.of(), "", List.of(), label, "", List.of(), -1, "", true);
        }

        /** Without M5 part C's wave tag. */
        public Secondary(
                double killRatio,
                int credits,
                List<String> groups,
                String escapes,
                List<String> killAll,
                String label,
                String partsOf,
                List<Integer> parts,
                int beforePhase) {
            this(killRatio, credits, groups, escapes, killAll, label, partsOf, parts, beforePhase, "");
        }

        public Secondary(
                double killRatio,
                int credits,
                List<String> groups,
                String escapes,
                List<String> killAll,
                String label) {
            this(killRatio, credits, groups, escapes, killAll, label, "", List.of(), -1);
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

        /**
         * M5 part D (Level 10, user decision D5 = a): no secondary objective. A kill ratio of 0 for no
         * credits, so it is never met (nor paid, nor cued); the tracker and the briefing show none.
         */
        public static final Secondary NONE = new Secondary(0, 0);

        /** M5 part D: whether the level has no secondary objective ({@link #NONE}). */
        public boolean none() {
            return killRatio == 0 && credits == 0 && !byGroups() && !byEscapes() && !afloat;
        }

        /** Whether the objective is about groups rather than the kill ratio. */
        public boolean byGroups() {
            return !groups.isEmpty();
        }

        /**
         * Whether the objective is that none of an enemy gets away, that every unit of the
         * {@code killAll} enemies is destroyed (Level 05's "Scorched crater"), or that every one of
         * a boss's {@code parts} is shot off in time (Level 07): a count of the units (parts)
         * destroyed of all, met when the last is destroyed, failed when one gets away (survives).
         */
        public boolean byEscapes() {
            return !escapes.isEmpty() || !killAll.isEmpty() || byParts();
        }

        /** Whether the objective is to shoot off a boss's parts before a phase ends (Level 07's "Gut the bays"). */
        public boolean byParts() {
            return !parts.isEmpty();
        }

        /**
         * Whether a unit of {@code slug} counts towards an escapes or kill-all objective (an escapes
         * objective scoped to a tag counts only the units of the waves with that tag).
         */
        public boolean counts(String slug) {
            return (escapes.equals(slug) && tag.isEmpty()) || killAll.contains(slug);
        }

        /** Whether a unit of {@code slug} of a wave tagged {@code waveTag} ("" for none) counts towards it. */
        public boolean counts(String slug, String waveTag) {
            return (escapes.equals(slug) && (tag.isEmpty() || tag.equals(waveTag))) || killAll.contains(slug);
        }
    }

    /**
     * An enemy fixed to the ground layer (a turret), entering at the top edge at {@code t} at
     * {@code x}, in the secondary objective's group {@code group} (-1 for none). M5 part E: a unit
     * whose stat block drifts (a raft) drifts along its nest's current, {@code currentRadians} from
     * straight down, positive to the right.
     */
    public record GroundUnit(double t, double x, EnemySpec enemy, int group, double currentRadians) {
        /** A unit whose nest gives no current (straight down, with the scroll). */
        public GroundUnit(double t, double x, EnemySpec enemy, int group) {
            this(t, x, enemy, group, 0);
        }
    }

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
     * @param submerged M5 part E: a trigger on the {@code sub} layer (Level 11's sunken supply pod):
     *     only torpedoes ({@link WeaponSpec.Delivery#TORPEDO}) hit it, and a Smart Bomb's ring spends
     *     it at once; every other shot, blast and strike passes over it
     * @param group M5 part E: a destructible's group (Level 11's floating containers), its index among
     *     the level's destructible groups, -1 for none
     * @param groupSize how many destructibles the group has
     * @param groupDrop the pickup the group's last destructible drops where it is destroyed, once
     *     every destructible of the group is (the containers' armour patch)
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
            Optional<LevelResult.DataCore> core,
            boolean submerged,
            int group,
            int groupSize,
            Optional<PickupType> groupDrop) {
        /** Level 01's cargo container, the look of a destructible that names none. */
        public static final String CARGO_CONTAINER = "cargo-container";

        /** Without M5 part E's destructible group. */
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
                String look,
                boolean dark,
                Optional<LevelResult.DataCore> core,
                boolean submerged) {
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
                    dark,
                    core,
                    submerged,
                    -1,
                    0,
                    Optional.empty());
        }

        /** On the ground layer: without M5 part E's sunken trigger. */
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
                String look,
                boolean dark,
                Optional<LevelResult.DataCore> core) {
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
                    dark,
                    core,
                    false);
        }

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
            if (submerged && hits <= 0) {
                throw new IllegalArgumentException("only a trigger lies under the water");
            }
            if (group >= 0 && (hits > 0 || groupSize < 1)) {
                throw new IllegalArgumentException("a group is of destructibles, at least one");
            }
            if (group < 0 && groupDrop.isPresent()) {
                throw new IllegalArgumentException("only a group's last destructible drops the group's pickup");
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
     * @param requires part G: it starts only with all of these fitted, as {@link #FITTED_SPECIAL}
     *     and {@link #FITTED_HOMING} bits (with {@code requiresSpecial}, {@link #FITTED_SPECIAL} is set);
     *     M5 part C: {@link #FITTED_ESCORT}, it starts only while an escort flies (Rook's scripted lines)
     * @param requiresNot part G: it starts only with none of these fitted (Level 07's line for a
     *     ship without a homing weapon)
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
            int alliesMax,
            int requires,
            int requiresNot) {
        /** What is fitted: a special. */
        public static final int FITTED_SPECIAL = 1;
        /** What is fitted: a weapon with homing delivery. */
        public static final int FITTED_HOMING = 2;
        /**
         * M5 part C: an escort flies (hired, fitted and not ejected; Rook): it changes during the
         * attempt, so the radio asks for it when a cue is due.
         */
        public static final int FITTED_ESCORT = 4;

        public RadioCue {
            requires |= requiresSpecial ? FITTED_SPECIAL : 0;
            requiresSpecial = (requires & FITTED_SPECIAL) != 0;
            if ((requires & requiresNot) != 0) {
                throw new IllegalArgumentException("a radio cue cannot require what it requires not to be fitted");
            }
        }

        /** Without part G's requirements beyond a special. */
        public RadioCue(
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
            this(
                    trigger,
                    t,
                    subject,
                    speaker,
                    line,
                    distorted,
                    expression,
                    portrait,
                    requiresSpecial,
                    alliesMin,
                    alliesMax,
                    0,
                    0);
        }

        /** Whether it may start with {@code fitted} ({@link #FITTED_SPECIAL}, {@link #FITTED_HOMING} bits) on the ship. */
        public boolean allowedWith(int fitted) {
            return (fitted & requires) == requires && (fitted & requiresNot) == 0;
        }

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
        /**
         * M5 part B: the first kill in the attempt by the wingman's shot or blast ({@link
         * Sortie#wingmanMount()}), while he flies; never after he ejects. No subject.
         */
        ESCORT_FIRST_KILL,
        /** The primary objective failed: the line the mission failed screen shows, not played on the radio. */
        MISSION_FAILED,
        /** A boss entered a phase after its first; the subject is the phase's name. */
        BOSS_PHASE,
        /** A boss was destroyed; the subject is its slug. */
        BOSS_DESTROYED,
        /**
         * Part G: a boss entered a phase because the phase before it ran out of time with parts it
         * waited for still alive (Level 07's broadside phase timing out); the subject is the name of
         * the phase it entered. Its {@link #BOSS_PHASE} cues start as well.
         */
        BOSS_TIMEOUT,
        /** M5 part C: the level's first hold zone starts to ease down; once. No subject. */
        HOLD_START,
        /** M5 part C: the attempt's first pounce takes off ({@link SimEvents.Type#POUNCE}). No subject. */
        FIRST_POUNCE,
        /** M5 part C: the collapse's shadow starts. No subject. */
        COLLAPSE,
        /**
         * M5 part D: a convoy unit the player could have saved was lost (every such loss, the first
         * too, after its {@link #FIRST_ALLY_LOST} cues); its line may name the unit ({@code {ally}},
         * {@link Sortie#lastAllyLost()}). Unlike the other events it starts again on every loss.
         */
        ALLY_LOST,
        /** M5 part D: the scripted loss's lance took its unit ({@link SimEvents.Type#SCRIPTED_LOSS}). No subject. */
        SCRIPTED_LOSS,
        /** M5 part D: the attempt's first decloak ({@link SimEvents.Type#DECLOAK}). No subject. */
        FIRST_DECLOAK,
        /** M5 part D: the attempt's first swarm loop-back ({@link SimEvents.Type#LOOP_BACK}). No subject. */
        FIRST_LOOP_BACK,
        /**
         * M5 part E: a convoy unit's first hit (each unit's; Level 11's cargo ships); its line may name
         * the unit ({@code {ally}}, {@link Sortie#lastAllyHit()}). It starts again for each unit, as
         * {@link #ALLY_LOST}.
         */
        ALLY_HIT,
        /**
         * M5 part E: the attempt's first lane telegraph of an arena boss ({@link SimEvents.Type#TELEGRAPH});
         * once. No subject.
         */
        FIRST_TELEGRAPH,
        /**
         * M5 part E: the first of some of the level boss's parts shot off (not lost in its death); once.
         * The cue's subject names the parts it waits for, separated by {@link #PART_SEPARATOR}; the
         * event's subject is the part shot off.
         */
        BOSS_PART_DESTROYED;

        /** Between the part names of a {@link #BOSS_PART_DESTROYED} cue's subject. */
        public static final String PART_SEPARATOR = "|";

        /** Whether its cues start again each time the event happens, not once per attempt. */
        public boolean repeats() {
            return this == ALLY_LOST || this == ALLY_HIT;
        }
    }
}
