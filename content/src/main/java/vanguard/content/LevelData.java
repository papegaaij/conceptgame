package vanguard.content;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import vanguard.sim.Layer;
import vanguard.sim.PlayField;

/**
 * design/campaign/&lt;act&gt;/&lt;level&gt;/data.yaml: a level's script. Times are seconds from
 * the level start.
 *
 * @param scrollSpeed px/s unless a section sets its own
 * @param launchSeconds the non-playable launch before control starts
 * @param controlPrompts the control prompts shown in the first section
 * @param prompts contextual prompts shown at a time (design/ui/hud: one line, an action and its keys)
 * @param sections back to back from t = 0
 * @param cranes the crane hazards (Level 02: Crane Four)
 * @param debris the debris field's chunks (Level 03)
 * @param setPieces the huge set-piece units and their passes (Level 03's Leviathan)
 * @param pickups pickups placed by the script (normal drops come from the enemies)
 * @param radio the radio chatter
 * @param backdrop the parallax layers behind and above the play plane
 * @param threatProfile what the hangar intel panel shows before the level (design/ui/hangar)
 * @param briefing the mission briefing before the level (design/ui/briefing)
 */
public record LevelData(
        double scrollSpeed,
        double launchSeconds,
        List<String> controlPrompts,
        Optional<List<Prompt>> prompts,
        List<Section> sections,
        List<Wave> waves,
        List<GroundTarget> groundTargets,
        List<Secret> secrets,
        List<PlacedPickup> pickups,
        List<RadioCue> radio,
        Optional<List<CraneData>> cranes,
        Optional<DebrisField> debris,
        Optional<List<SetPieceData>> setPieces,
        Objectives objectives,
        Music music,
        Difficulties difficulty,
        BackdropData backdrop,
        ThreatProfile threatProfile,
        Briefing briefing) {
    public LevelData {
        Check.positive("scroll_speed", scrollSpeed);
        Check.notNegative("launch_seconds", launchSeconds);
        Check.notEmpty("sections", sections);
        for (int i = 1; i < sections.size(); i++) {
            Check.that(
                    sections.get(i).end() > sections.get(i - 1).end(),
                    "sections[" + i + "]: must end after the section before it");
        }
    }

    /**
     * After the level end the scroll runs on under the radio until its last message has been shown
     * (the outro), at most this long before the debrief, so the backdrop has to hold until then.
     */
    public static final double OUTRO_SECONDS = 15;

    /** The level's length in seconds: the end of the last section. */
    public double seconds() {
        return sections.getLast().end();
    }

    /** When the longest outro ends and the debrief takes over: the last moment the backdrop is on screen. */
    public double outroEnd() {
        return seconds() + OUTRO_SECONDS;
    }

    /** The time section {@code index} (0-based) starts. */
    public double sectionStart(int index) {
        return index == 0 ? 0 : sections.get(index - 1).end();
    }

    /**
     * The ground layer's scroll distance at {@code t} seconds, px; before the start and after the
     * end the first and the last section's speed carry on.
     */
    public double scrollAt(double t) {
        double scroll = 0;
        for (int i = 0; i < sections.size(); i++) {
            Section section = sections.get(i);
            if (t < section.end() || i == sections.size() - 1) {
                return scroll + (t - sectionStart(i)) * speedOf(section);
            }
            scroll += (section.end() - sectionStart(i)) * speedOf(section);
        }
        throw new IllegalStateException("a level has sections");
    }

    private double speedOf(Section section) {
        return section.speed().isPresent() ? section.speed().get() : scrollSpeed;
    }

    /**
     * Where the centre of a placed set piece lies on its layer: px from the screen's bottom edge
     * at the level start, so that it passes the middle of the screen at its time.
     */
    public double pieceCentre(BackdropData.PlacedPiece placed) {
        BackdropLayer layer = backdrop.pieces().get(placed.piece()).layer();
        return scrollAt(placed.t()) * backdrop.factor(layer) + PlayField.HEIGHT / 2.0;
    }

    /**
     * Where section {@code index}'s tile set begins on {@code layer} (px from the screen's bottom
     * edge at the level start): it enters at the top edge when the section starts; the first
     * section's reaches back without end.
     */
    public double seam(BackdropLayer layer, int index) {
        return index == 0
                ? Double.NEGATIVE_INFINITY
                : scrollAt(sectionStart(index)) * backdrop.factor(layer) + PlayField.HEIGHT;
    }

    /** A stretch of the level with one atmosphere: a section, or the parts of it around its peak. */
    public record Stretch(double start, double end, Atmosphere atmosphere) {}

    /** The atmosphere along the level: every section's, split around its peak, in time order. */
    public List<Stretch> atmosphereStretches() {
        List<Stretch> out = new ArrayList<>();
        for (int i = 0; i < sections.size(); i++) {
            Section section = sections.get(i);
            double start = sectionStart(i);
            if (section.peak().isPresent()) {
                Peak peak = section.peak().get();
                out.add(new Stretch(start, peak.from(), section.atmosphere()));
                out.add(new Stretch(peak.from(), peak.to(), peak.atmosphere()));
                out.add(new Stretch(peak.to(), section.end(), section.atmosphere()));
            } else {
                out.add(new Stretch(start, section.end(), section.atmosphere()));
            }
        }
        return out;
    }

    /** The 1-based number of the section that is running at {@code t}, or 0 after the level end. */
    public int sectionAt(double t) {
        for (int i = 0; i < sections.size(); i++) {
            if (t < sections.get(i).end()) {
                return i + 1;
            }
        }
        return 0;
    }

    /**
     * A stretch of the scroll; it starts where the one before it ends.
     *
     * @param tiles the backdrop's tile sets in this section, at most one per layer
     */
    public record Section(
            String name,
            double end,
            Optional<Double> speed,
            Atmosphere atmosphere,
            Optional<Peak> peak,
            List<String> tiles) {
        public Section {
            Check.positive("end", end);
            speed.ifPresent(s -> Check.positive("speed", s));
        }
    }

    /** A stretch inside a section whose atmosphere peaks at {@code atmosphere} (Level 02's coolant cloud). */
    public record Peak(Atmosphere atmosphere, double from, double to) {
        public Peak {
            Check.that(to > from, "a peak ends after it starts");
        }
    }

    /**
     * A contextual prompt (design/ui/hud, control prompts): shown from {@code t} for {@code seconds}
     * in the prompts' well, the {@code action} on the left and its {@code keys} on the right.
     *
     * @param skip a layer: the prompt leaves early, done what it says, once an enemy on that layer
     *     is destroyed (Level 03's {@code LOW-AIR} prompt); without it, it stays for its seconds
     */
    public record Prompt(double t, String action, String keys, double seconds, Optional<String> skip) {
        public Prompt {
            Check.notNegative("t", t);
            Check.positive("seconds", seconds);
            skip.ifPresent(Layers::of);
        }

        /** The layer whose first destroyed enemy makes the prompt leave, if any. */
        public Optional<Layer> skipLayer() {
            return skip.map(Layers::of);
        }
    }

    /**
     * The debris field (design/world/earth-orbit, hazards): drifting wreck chunks on the air layer
     * that block shots and enemy bullets. Each placed chunk enters at the top edge at its time.
     *
     * @param chunks the chunk kinds by name; the sprite of {@code large-a} is {@code debris-large-a}
     * @param clearance a chunk never enters closer to the ship than this, px (it moves sideways)
     * @param placed in time order
     * @param maxLarge the most large chunks on the screen at once (checked by the loader on every difficulty)
     * @param easy which large chunks are left out on easy
     * @param hard how much faster they drift on hard
     */
    public record DebrisField(
            Map<String, Chunk> chunks,
            double clearance,
            List<PlacedChunk> placed,
            Optional<Integer> maxLarge,
            Optional<DebrisEasy> easy,
            Optional<DebrisHard> hard) {
        public DebrisField {
            Check.notEmpty("chunks", new ArrayList<>(chunks.keySet()));
            Check.notNegative("clearance", clearance);
            for (int i = 1; i < placed.size(); i++) {
                Check.that(
                        placed.get(i).t() >= placed.get(i - 1).t(),
                        "placed[" + i + "]: chunks are listed in time order");
            }
        }

        /** The chunks entering on {@code difficulty}, in time order. */
        public List<PlacedChunk> placedOn(Difficulty difficulty) {
            if (difficulty != Difficulty.EASY || easy.isEmpty()) {
                return placed;
            }
            int every = easy.get().leaveOutLarge();
            List<PlacedChunk> kept = new ArrayList<>();
            int large = 0;
            for (PlacedChunk chunk : placed) {
                if (chunks.get(chunk.chunk()).large() && ++large % every == 0) {
                    continue;
                }
                kept.add(chunk);
            }
            return kept;
        }

        /** The factor on the drift on {@code difficulty}. */
        public double driftFactor(Difficulty difficulty) {
            return difficulty == Difficulty.HARD
                    ? hard.map(DebrisHard::driftFactor).orElse(1.0)
                    : 1;
        }
    }

    /**
     * A chunk kind: a large one is indestructible and deals contact {@code damage} (once per
     * second), a small one breaks after {@code hp} damage and pays nothing.
     */
    public record Chunk(Size size, Optional<Double> damage, Optional<Double> hp) {
        public Chunk {
            Check.that(damage.isPresent() != hp.isPresent(), "give damage (a large chunk) or hp (a small one)");
            damage.ifPresent(d -> Check.positive("damage", d));
            hp.ifPresent(h -> Check.positive("hp", h));
        }

        public boolean large() {
            return damage.isPresent();
        }
    }

    /** A chunk entering at the top edge at {@code t} at {@code x}, drifting at {@code drift} [x, y] px/s (y up: negative is down). */
    public record PlacedChunk(double t, double x, String chunk, Point drift) {
        public PlacedChunk {
            Check.notNegative("t", t);
            Check.that(drift.y() < 0, "drift: a chunk drifts down the screen (a negative y)");
        }
    }

    /** On easy every {@code leaveOutLarge}-th large chunk is left out. */
    public record DebrisEasy(int leaveOutLarge) {
        public DebrisEasy {
            Check.that(leaveOutLarge >= 2, "leave_out_large: 2 or more");
        }
    }

    /** On hard the chunks drift {@code driftFactor} times as fast. */
    public record DebrisHard(double driftFactor) {
        public DebrisHard {
            Check.positive("drift_factor", driftFactor);
        }
    }

    /** A huge set-piece unit ({@code enemy}, a multi-part stat block) flying its {@code passes} in order. */
    public record SetPieceData(String enemy, List<PassData> passes) {
        public SetPieceData {
            Check.notEmpty("passes", passes);
        }
    }

    /**
     * A pass of a set piece: its centre flies along {@code path} on {@code layer} facing
     * {@code heading} (degrees from straight down, positive to the right; fixed for the pass). A
     * pass on the player's layer arrives on {@code high-air}, {@code descend}s, holds until
     * {@code hold} seconds after its descent began and then rises and leaves straight up through
     * the top edge at {@code leave_speed}; a pass without a descent ends with its path.
     *
     * @param section the section it belongs to (the README's waves table)
     * @param path one {@code [t, x, y]} per waypoint: level seconds, px from the left, px below the top edge
     */
    public record PassData(
            String name,
            int section,
            String layer,
            double heading,
            List<PathPoint> path,
            Optional<Descent> descend,
            Optional<Double> hold,
            Optional<Double> leaveSpeed,
            Optional<PassChange> easy,
            Optional<PassChange> hard) {
        public PassData {
            Layers.of(layer);
            Check.notEmpty("path", path);
            for (int i = 1; i < path.size(); i++) {
                Check.that(path.get(i).t() > path.get(i - 1).t(), "path[" + i + "]: waypoints are in time order");
            }
            Check.that(
                    descend.isPresent() == hold.isPresent() && hold.isPresent() == leaveSpeed.isPresent(),
                    "a descending pass gives descend, hold and leave_speed, a flight none of them");
            hold.ifPresent(h -> Check.that(h > descend.orElseThrow().seconds(), "hold: longer than the descent"));
            leaveSpeed.ifPresent(v -> Check.positive("leave_speed", v));
        }

        /** Its hold on {@code difficulty}, from the start of the descent. */
        public Optional<Double> holdOn(Difficulty difficulty) {
            Optional<PassChange> change =
                    switch (difficulty) {
                        case EASY -> easy;
                        case MEDIUM -> Optional.empty();
                        case HARD -> hard;
                    };
            return change.flatMap(PassChange::hold).or(() -> hold);
        }
    }

    /** A waypoint: level seconds, px from the left, px below the top edge. */
    @JsonFormat(shape = JsonFormat.Shape.ARRAY)
    public record PathPoint(double t, double x, double y) {}

    /** The descent from {@code high-air} to the player's layer: starts {@code at} level seconds, takes {@code seconds}. */
    public record Descent(double at, double seconds) {
        public Descent {
            Check.positive("seconds", seconds);
        }
    }

    /** A difficulty's change to a pass: another hold. */
    public record PassChange(Optional<Double> hold) {}

    /**
     * A crane hazard ({@code sim.LevelScript.CraneSpec}): an arm of {@code length} x {@code width}
     * px hanging from ({@code x}, {@code y}) above the top edge, swinging between {@code from} and
     * {@code to} (degrees from straight down, positive to the right) at the {@code swings} times,
     * each swing {@code swing_seconds} long after a {@code telegraph} of blinking lights; contact
     * {@code damage}; a clamp at its tip that {@code reveals} a secret after {@code hits} hits.
     */
    public record CraneData(
            String name,
            double x,
            double y,
            double length,
            double width,
            double from,
            double to,
            List<Double> swings,
            double swingSeconds,
            double telegraph,
            double damage,
            Optional<Clamp> clamp,
            Optional<Swings> easy,
            Optional<Swings> hard) {
        public CraneData {
            Check.positive("length", length);
            Check.positive("width", width);
            Check.notEmpty("swings", swings);
            Check.positive("swing_seconds", swingSeconds);
            Check.positive("telegraph", telegraph);
            Check.positive("damage", damage);
        }
    }

    /** A crane's clamp: {@code hits} hits while the arm swings release the secret it {@code reveals}. */
    public record Clamp(int hits, String reveals) {
        public Clamp {
            Check.positive("hits", hits);
        }
    }

    /** A difficulty's swing times. */
    public record Swings(List<Double> swings) {
        public Swings {
            Check.notEmpty("swings", swings);
        }
    }

    /** Low-air cloud and haze intensity (design/art-direction/README.md). */
    public enum Atmosphere {
        CLEAR,
        LIGHT,
        MEDIUM,
        HEAVY
    }

    /**
     * A wave: one group ({@code formation}, {@code enemy}, {@code count}) or a mixed wave's
     * {@code groups}; {@link #groups()} gives both as a list.
     *
     * @param from the edge it enters from
     * @param edge which side of that edge; {@code sides} without an edge means both side edges
     * @param hold seconds the formation holds (pincer at the edge, circle orbiting)
     * @param warning seconds of radio warning before a rear wave
     * @param breakGroup how many units of a circle break toward the player together
     * @param speed px/s instead of the enemy's own speed
     * @param interval seconds between two units of a stream
     * @param at a whirl cluster's release point: {@code [x, y]}, px from the left and below the top edge
     * @param easy changes on easy
     * @param hard changes on hard
     */
    public record Wave(
            double t,
            Optional<String> formation,
            Optional<String> enemy,
            Optional<Integer> count,
            Optional<List<Group>> groups,
            Entry from,
            Optional<Edge> edge,
            Optional<Double> hold,
            Optional<Double> warning,
            Optional<Integer> breakGroup,
            Optional<Double> speed,
            Optional<Double> interval,
            Optional<Point> at,
            Optional<Change> easy,
            Optional<Change> hard) {
        public Wave {
            Check.notNegative("t", t);
            speed.ifPresent(s -> Check.positive("speed", s));
            interval.ifPresent(i -> Check.positive("interval", i));
            boolean single = formation.isPresent() || enemy.isPresent() || count.isPresent();
            Check.that(
                    single != groups.isPresent(),
                    "give either formation, enemy and count, or the groups of a mixed wave");
            if (single) {
                groups = Optional.of(List.of(new Group(
                        formation.orElseThrow(() -> new IllegalArgumentException("formation is required")),
                        enemy.orElseThrow(() -> new IllegalArgumentException("enemy is required")),
                        count.orElseThrow(() -> new IllegalArgumentException("count is required")))));
            }
        }

        /** The wave's groups; one for a plain wave. */
        public List<Group> groupList() {
            return groups.orElseThrow();
        }
    }

    /** {@code count} units of {@code enemy} flying {@code formation}. */
    public record Group(String formation, String enemy, int count) {
        public Group {
            Check.positive("count", count);
        }
    }

    public enum Entry {
        FRONT,
        SIDES,
        REAR
    }

    public enum Edge {
        LEFT,
        RIGHT,
        ALTERNATING
    }

    /** A difficulty's changes to a wave. */
    public record Change(
            Optional<Entry> from, Optional<Edge> edge, Optional<Integer> count, Optional<Integer> breakGroup) {}

    /**
     * A ground target: a destructible ({@code hp}, {@code bounty}, {@code drop}), a trigger hit
     * {@code hits} times that {@code reveals} a secret, or ground enemies of a stat block
     * ({@code enemy}, such as a Spine Turret nest), which may belong to a {@code group} of the
     * secondary objective.
     *
     * @param layer and {@code size}: a destructible's or trigger's layer and hit box in px; an
     *     enemy's come from its stat block
     * @param at where each of them is placed
     * @param hardened only {@code anti-ground} weapons damage it (design/enemies, layer rules)
     * @param easy another placement list on easy (an enemy nest's size)
     * @param hard another placement list on hard
     */
    public record GroundTarget(
            String target,
            int section,
            Optional<Integer> count,
            Optional<String> layer,
            Optional<Size> size,
            List<Placement> at,
            Optional<Double> hp,
            Optional<Integer> bounty,
            Optional<Pickup> drop,
            Optional<Integer> hits,
            Optional<String> reveals,
            Optional<Boolean> hardened,
            Optional<String> enemy,
            Optional<String> group,
            Optional<Placements> easy,
            Optional<Placements> hard) {
        public GroundTarget {
            layer.ifPresent(Layers::of);
            Check.that(at.size() == count.orElse(1), "at: one placement per target (count, or 1 for a trigger)");
            if (enemy.isPresent()) {
                Check.that(
                        hp.isEmpty() && hits.isEmpty() && bounty.isEmpty() && layer.isEmpty() && size.isEmpty(),
                        "an enemy's hp, bounty, layer and size come from its stat block");
            } else {
                Check.that(layer.isPresent() && size.isPresent(), "a destructible or trigger needs layer and size");
                Check.that(hp.isPresent() != hits.isPresent(), "give hp (destructible) or hits (trigger)");
                Check.that(reveals.isPresent() == hits.isPresent(), "a trigger (hits) reveals a secret");
                Check.that(
                        group.isEmpty() && easy.isEmpty() && hard.isEmpty(),
                        "only enemies have a group or easy/hard placements");
            }
        }
    }

    /** A difficulty's placements of a ground target. */
    public record Placements(List<Placement> at) {}

    /** A ground object entering at the top edge at {@code t} seconds, {@code x} px from the left. */
    @JsonFormat(shape = JsonFormat.Shape.ARRAY)
    public record Placement(double t, double x) {
        public Placement {
            Check.notNegative("t", t);
        }
    }

    /** A hidden crate worth {@code crate} credits, with Rook's (or anyone's) line when found. */
    public record Secret(String name, int crate, RadioLine radio) {
        public Secret {
            Check.positive("crate", crate);
        }
    }

    /** A pickup carried by a unit of the wave starting at {@code droppedBy.wave}, dropped when it is destroyed. */
    public record PlacedPickup(Pickup pickup, Carrier droppedBy) {}

    /** The {@code unit} ({@code first}, {@code second} or {@code last}) of the wave starting at {@code wave} seconds. */
    public record Carrier(double wave, CarrierUnit unit) {}

    public enum CarrierUnit {
        FIRST,
        SECOND,
        LAST
    }

    /**
     * A spoken line; {@code distorted} for transmissions such as the Choir's.
     *
     * @param expression the speaker's portrait expression, neutral when not given
     */
    public record RadioLine(
            String speaker, String line, Optional<Boolean> distorted, Optional<Expression> expression) {}

    /**
     * A radio chatter cue, triggered at {@code t} seconds or by an {@code event}.
     *
     * @param enemy the enemy of a {@code first-kill} or {@code enemy-escaped} event
     * @param expression the speaker's portrait expression, neutral when not given
     * @param easy changes on easy
     * @param hard changes on hard
     */
    public record RadioCue(
            Optional<Double> t,
            Optional<CueEvent> event,
            Optional<String> enemy,
            Optional<String> group,
            String speaker,
            Optional<String> portrait,
            String line,
            Optional<Boolean> distorted,
            Optional<Expression> expression,
            Optional<RadioChange> easy,
            Optional<RadioChange> hard) {
        public RadioCue {
            Check.that(t.isPresent() != event.isPresent(), "give the trigger as t or as event");
            boolean byEnemy = event.orElse(null) == CueEvent.FIRST_KILL || event.orElse(null) == CueEvent.ENEMY_ESCAPED;
            Check.that(
                    enemy.isPresent() == byEnemy,
                    "a first-kill or enemy-escaped event names its enemy, other triggers do not");
            boolean byGroup = event.orElse(null) == CueEvent.GROUP_CLEARED || event.orElse(null) == CueEvent.GROUP_LOST;
            Check.that(
                    group.isPresent() == byGroup,
                    "a group-cleared or group-lost event names its group, other triggers do not");
        }
    }

    /** A difficulty's changes to a radio cue: another line, as when a wave enters elsewhere. */
    public record RadioChange(String line) {}

    public enum CueEvent {
        @JsonProperty("first-kill")
        FIRST_KILL,
        @JsonProperty("secondary-objective")
        SECONDARY_OBJECTIVE,
        @JsonProperty("level-end")
        LEVEL_END,
        @JsonProperty("group-cleared")
        GROUP_CLEARED,
        @JsonProperty("group-lost")
        GROUP_LOST,
        @JsonProperty("first-group-lost")
        FIRST_GROUP_LOST,
        /** The first unit of the enemy left the screen alive (a set piece: at the end of its last pass). */
        @JsonProperty("enemy-escaped")
        ENEMY_ESCAPED
    }

    /** The primary objective's kind and the optional secondary objective. */
    public record Objectives(String primary, Optional<Secondary> secondary) {}

    /**
     * Destroy at least {@code killRatio} of all enemies for {@code credits}, clear the ground
     * enemies of every one of the {@code groups} (named by its ground targets' {@code group}) for
     * {@code credits} each, or let none of the enemy {@code escapes} leave the screen alive
     * ("nothing gets through", Level 03) for {@code credits}.
     */
    public record Secondary(
            Optional<Double> killRatio, Optional<List<String>> groups, Optional<String> escapes, int credits) {
        public Secondary {
            Check.that(
                    (killRatio.isPresent() ? 1 : 0) + (groups.isPresent() ? 1 : 0) + (escapes.isPresent() ? 1 : 0) == 1,
                    "give kill_ratio, groups or escapes");
            killRatio.ifPresent(r -> Check.share("kill_ratio", r));
            groups.ifPresent(g -> Check.notEmpty("groups", g));
            Check.notNegative("credits", credits);
        }
    }

    /**
     * The music cues.
     *
     * @param track the track number of design/audio/music
     * @param startSection the section the music starts in (ambience only before)
     * @param startDb the theme's level through its start section, dB (full when absent); it rises
     *     to full at the next section
     * @param fullSection the section from which all stems play (the base stem before)
     * @param stems sections that override that: {@code base} drops to the base stem (Level 03's
     *     first Leviathan pass), {@code full} forces every stem on
     */
    public record Music(
            int track,
            int startSection,
            Optional<Double> startDb,
            int fullSection,
            Optional<Map<Integer, Stems>> stems,
            String ambience,
            String endJingle) {
        /** The stems a section plays. */
        public enum Stems {
            @JsonProperty("base")
            BASE,
            @JsonProperty("full")
            FULL
        }

        /** Whether section {@code section} (1-based) plays every stem. */
        public boolean full(int section) {
            Stems override = stems.map(map -> map.get(section)).orElse(null);
            return override != null ? override == Stems.FULL : section >= fullSection;
        }

        public Music {
            Check.that(startSection <= fullSection, "full_section must not come before start_section");
            startDb.ifPresent(db -> Check.that(db <= 0, "start_db: must not be above full level"));
        }
    }

    /**
     * The level's threat profile (design/campaign, level template; design/player/systems, Sensor
     * levels and hangar intel); the attack directions, enemy types, waves and secrets are derived
     * from the script.
     *
     * @param layers the dominant gameplay layers
     * @param density 1–5
     * @param traits the recommended weapon traits
     * @param boss the boss or mid-boss, {@code none} without one
     * @param specials the special availability, where the level limits it ("No air support under the ice")
     * @param varga Dr. Varga's intel line per sensor level: {@code none}, {@code l1}, {@code l2}, {@code l3}
     */
    public record ThreatProfile(
            String setting,
            List<String> layers,
            int density,
            List<String> traits,
            List<String> hazards,
            String boss,
            Optional<String> specials,
            Map<String, String> varga) {
        /** The keys of Varga's lines, from no sensor suite to L3. */
        public static final List<String> SENSOR_KEYS = List.of("none", "l1", "l2", "l3");

        public ThreatProfile {
            Check.that(density >= 1 && density <= 5, "density: 1–5, was " + density);
            Check.that(
                    SENSOR_KEYS.containsAll(varga.keySet()),
                    "varga: keys are " + SENSOR_KEYS + ", found " + varga.keySet());
            layers = List.copyOf(layers);
            traits = List.copyOf(traits);
            hazards = List.copyOf(hazards);
            varga = Map.copyOf(varga);
        }

        /** Varga's line for a sensor level (0–3): the one of the highest level at or below it. */
        public Optional<String> vargaLine(int sensor) {
            for (int level = sensor; level >= 0; level--) {
                String line = varga.get(SENSOR_KEYS.get(level));
                if (line != null) {
                    return Optional.of(line);
                }
            }
            return Optional.empty();
        }
    }

    /**
     * The mission briefing (design/ui/briefing): its pages, and the hangar teaser shown with the
     * level's intel on the hangar visit before it.
     */
    public record Briefing(List<BriefingPage> pages, BriefingPage teaser) {
        public Briefing {
            Check.notEmpty("pages", pages);
        }
    }

    /** The level-wide changes on easy and hard. */
    public record Difficulties(Optional<Variant> easy, Optional<Variant> hard) {}

    /** Changes to enemies (by slug) and extra placed pickups. */
    public record Variant(Optional<Map<String, EnemyChange>> enemies, Optional<List<PlacedPickup>> extraPickups) {}

    /** Aimed shots fired in bursts of {@code burst}. */
    public record EnemyChange(Optional<Integer> burst) {
        public EnemyChange {
            burst.ifPresent(b -> Check.positive("burst", b));
        }
    }
}
