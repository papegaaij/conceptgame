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
 * @param bountyScale multiplies every bounty paid in the level (design/systems/economy), 1 if not
 *     given
 * @param crateSeconds how long an uncollected secret's crate (or data core) stays, the player's
 *     {@code pickup_seconds} if not given (Level 10's ferry cache: long enough to drift from the top
 *     edge to the ship's start line)
 * @param controlPrompts the control prompts shown in the first section
 * @param prompts contextual prompts shown at a time (design/ui/hud: one line, an action and its keys)
 * @param sections back to back from t = 0
 * @param cranes the crane hazards (Level 02: Crane Four)
 * @param debris the debris field's chunks (Level 03)
 * @param setPieces the huge set-piece units and their passes (Level 03's Leviathan)
 * @param road the road on the ground layer that an escort objective's convoy follows (Level 04)
 * @param boss the level's boss and where it arrives (Level 05's Gorgon Frigate)
 * @param sleds the mass-driver sleds on a rail (Level 05)
 * @param rocks the rocks destroyed ground units throw in low gravity (Level 05)
 * @param pickups pickups placed by the script (normal drops come from the enemies)
 * @param tows part G: friendly craft drifting on the air layer that tow a secret's crate on a
 *     cable (Level 07's lifeboat tow)
 * @param radio the radio chatter
 * @param backdrop the parallax layers behind and above the play plane
 * @param threatProfile what the hangar intel panel shows before the level (design/ui/hangar)
 * @param briefing the mission briefing before the level (design/ui/briefing)
 * @param holds M5 part C: the hold zones, where the scroll eases down until their groups are gone
 *     and the level clock (script time) slows with it (Level 09's node clusters)
 * @param collapse M5 part C: the collapse once its groups are cleared (Level 09's arcology)
 */
public record LevelData(
        double scrollSpeed,
        double launchSeconds,
        Optional<Double> bountyScale,
        Optional<Double> crateSeconds,
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
        Optional<Road> road,
        Optional<BossPlacement> boss,
        Optional<Sleds> sleds,
        Optional<Rocks> rocks,
        Objectives objectives,
        Music music,
        Difficulties difficulty,
        BackdropData backdrop,
        ThreatProfile threatProfile,
        Briefing briefing,
        Optional<Darkness> darkness,
        Optional<List<Tow>> tows,
        Optional<List<Hold>> holds,
        Optional<Collapse> collapse) {
    public LevelData {
        Check.positive("scroll_speed", scrollSpeed);
        Check.notNegative("launch_seconds", launchSeconds);
        bountyScale.ifPresent(scale -> Check.positive("bounty_scale", scale));
        crateSeconds.ifPresent(seconds -> Check.positive("crate_seconds", seconds));
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
     * Lines that must play in flight, such as an act boss's closing lines (Level 07), need a last
     * section long enough for them: the aftermath after the arena is a section like any other, as
     * long as the data makes it.
     */
    public static final double OUTRO_SECONDS = 15;

    /** The factor on every bounty paid in the level: its {@code bounty_scale}, 1 if not given. */
    public double bounties() {
        return bountyScale.orElse(1.0);
    }

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
            List<String> tiles,
            Optional<Boolean> arena) {
        public Section {
            Check.positive("end", end);
            speed.ifPresent(s -> Check.positive("speed", s));
        }

        /**
         * Whether it is the boss's arena: the level clock halts at its end while the boss lives,
         * and the next section starts at the boss's death when it dies earlier.
         */
        public boolean isArena() {
            return arena.orElse(false);
        }
    }

    /**
     * The level's boss ({@code enemy}, a stat block with a {@code boss} script): it arrives at
     * {@code t} level seconds in section {@code section}, its centre at {@code x} px from the left.
     */
    public record BossPlacement(String enemy, double t, double x, int section) {
        public BossPlacement {
            Check.notNegative("t", t);
            Check.positive("section", section);
        }
    }

    /**
     * The mass-driver sleds (design/world/luna, hazards): from {@code first} s every {@code period}
     * s until {@code until} a sled shoots up the rail at {@code x}, a line {@code width} px wide, on
     * the screen for {@code run} s after the rail {@code lights} chased; contact {@code damage}. The
     * trigger that reveals the {@code clamp} secret can only be hit while the rail is dark.
     */
    public record Sleds(
            double x,
            double width,
            double first,
            double period,
            double until,
            double lights,
            double run,
            double damage,
            Optional<String> clamp,
            Optional<SledChange> easy,
            Optional<SledChange> hard) {
        public Sleds {
            Check.positive("width", width);
            Check.positive("period", period);
            Check.positive("run", run);
            Check.positive("damage", damage);
            Check.that(period > lights + run, "a sled's period holds its lights and its run");
        }
    }

    /** A difficulty's sled period. */
    public record SledChange(double period) {
        public SledChange {
            Check.positive("period", period);
        }
    }

    /**
     * Rocks a destroyed ground unit throws in low gravity (Level 05): {@code count} {@code [min,
     * max]} of them, drifting at {@code speed} {@code [min, max]} px/s in a random direction on the
     * air layer, gone after {@code life} s; {@code hp}, contact {@code damage}, no credits; none
     * within {@code clearance} px of the ship; none on easy unless {@code onEasy}.
     */
    public record Rocks(
            Count count,
            Span speed,
            double life,
            Size size,
            double hp,
            double damage,
            double clearance,
            Optional<Boolean> onEasy) {
        public Rocks {
            Check.positive("life", life);
            Check.positive("hp", hp);
        }
    }

    /**
     * M5 part C (user decisions D2 and D3 = a; Level 09): a hold zone. When the first unit of its
     * {@code groups} (ground-target groups of the objectives) reaches {@code y} px below the top
     * edge, the scroll eases over {@code ramp} s to {@code speed} px/s ({@code easy} / {@code hard}
     * {@code speed}) and stays there until every unit of those groups is gone, then eases back. No
     * timeout. The level clock advances at the current speed ÷ the section's, so every time in the
     * level file is script time.
     */
    public record Hold(
            List<String> groups,
            double y,
            double speed,
            Optional<HoldChange> easy,
            Optional<HoldChange> hard,
            double ramp) {
        public Hold {
            Check.notEmpty("groups", groups);
            groups = List.copyOf(groups);
            Check.that(y > 0 && y < PlayField.HEIGHT, "y: inside the play field, was " + y);
            Check.positive("speed", speed);
            Check.positive("ramp", ramp);
        }

        /** Its speed on {@code difficulty}, px/s. */
        public double speedOn(Difficulty difficulty) {
            return switch (difficulty) {
                case EASY -> easy.map(HoldChange::speed).orElse(speed);
                case MEDIUM -> speed;
                case HARD -> hard.map(HoldChange::speed).orElse(speed);
            };
        }
    }

    /** A difficulty's hold speed, px/s. */
    public record HoldChange(double speed) {
        public HoldChange {
            Check.positive("speed", speed);
        }
    }

    /**
     * M5 part C (user decision D5 = a; Level 09; round 31's look c, 2026-10-07): the collapse. When
     * every unit of its {@code groups} is destroyed, the backdrop's {@code tower} (placed exactly
     * once) leans for {@code warning} s (the warning), then drops straight down in {@code drop} s; at
     * the impact its {@code blast} rolls out from the tower's foot: a ring growing from {@code
     * blast.from} to {@code blast.to} px round the footprint's centre in {@code blast.seconds} (eased
     * 1 − (1 − t)²), destroying every ground unit in the band (the tower's footprint along the scroll,
     * see {@link #collapseBand}) when it reaches it, paid and scored as an Airstrike kill; air units and
     * the player are untouched. Presentation: the {@code dust} atmosphere it raises, ramping back out
     * over its {@code seconds} after the impact, and the {@code rubble} backdrop piece it leaves; a
     * hold over its groups lasts until that dust has settled (user decision, 2026-10-07).
     */
    public record Collapse(
            List<String> groups, double warning, double drop, Blast blast, String tower, Dust dust, String rubble) {
        public Collapse {
            Check.notEmpty("groups", groups);
            groups = List.copyOf(groups);
            Check.notNegative("warning", warning);
            Check.positive("drop", drop);
            Check.that(blast != null && dust != null, "collapse: needs its blast and its dust");
            Check.that(dust.seconds() >= blast.seconds(), "collapse: the dust settles after the blast");
        }

        /** Seconds from the warning's start to the impact (the end of the drop). */
        public double impact() {
            return warning + drop;
        }

        /** Seconds from the warning's start to the blast's end (the collapse's end). */
        public double end() {
            return warning + drop + blast.seconds();
        }

        /** Seconds from the warning's start to the dust's settling (a hold over its groups ends). */
        public double settled() {
            return warning + drop + dust.seconds();
        }
    }

    /**
     * The collapse's blast: its kill ring's radius grows from {@code from} to {@code to} px round the
     * tower's footprint's centre over {@code seconds} from the impact.
     */
    public record Blast(double seconds, double from, double to) {
        public Blast {
            Check.positive("seconds", seconds);
            Check.notNegative("from", from);
            Check.that(to > from, "blast: the ring grows (to > from)");
        }
    }

    /**
     * A collapse's band on the ground: {@code bottom} to {@code top}, ground positions (px from the
     * screen's bottom edge at the level start, as {@link #pieceCentre}), under a tower whose
     * footprint's centre is {@code x} px from the play field's left edge.
     */
    public record Band(double bottom, double top, double x) {
        /** The footprint's centre on the ground, a ground position. */
        public double centre() {
            return (bottom + top) / 2;
        }
    }

    /**
     * The collapse's band: its tower's footprint along the scroll, centred where the placed tower's
     * footprint lies on the ground.
     */
    public Optional<Band> collapseBand() {
        return collapse.map(c -> {
            BackdropData.PlacedPiece placed = backdrop.placements().stream()
                    .filter(p -> p.piece().equals(c.tower()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("collapse.tower: '" + c.tower() + "' is not placed"));
            double centre = pieceCentre(placed);
            double half = backdrop.pieces().get(c.tower()).size().height() / 2;
            return new Band(centre - half, centre + half, placed.x());
        });
    }

    /** An event-triggered atmosphere peak: {@code atmosphere} at once, ramping back out over {@code seconds}. */
    public record Dust(Atmosphere atmosphere, double seconds) {
        public Dust {
            Check.positive("seconds", seconds);
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
    public record Prompt(
            double t, String action, String keys, double seconds, Optional<String> skip, Optional<String> requires) {
        public Prompt {
            Check.notNegative("t", t);
            Check.positive("seconds", seconds);
            skip.ifPresent(Layers::of);
            requires.ifPresent(Requirement::checkPrompt);
        }

        /** Whether it shows only with a special fitted (Level 04's {@code SPECIAL} · {@code CALL HAMMER}). */
        public boolean requiresSpecial() {
            return requires.isPresent();
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

    /**
     * Part G: a friendly craft drifting on the air layer that tows a secret's crate on a cable
     * (Level 07's lifeboat tow). The {@code boat} (its sprite box, px) enters at the top edge at
     * {@code t} with its centre at {@code x} and drifts at {@code drift} {@code [x, y]} px/s (y up: a
     * negative y drifts down the screen); the {@code pod} (its box) hangs at {@code tether}
     * {@code [dx, dy]} px from the boat's centre (y up: a positive dy trails above it). Shots and
     * bullets pass the boat and the pod, and the ship flies under them; only the {@code cable}, a hit
     * box midway between the two, takes the player's shots: {@code hits} of them cut it and the pod
     * falls free as the crate of the secret it {@code reveals}.
     */
    public record Tow(
            double t, double x, Point drift, Size boat, Size pod, Point tether, Size cable, int hits, String reveals) {
        public Tow {
            Check.notNegative("t", t);
            Check.that(drift.y() < 0, "drift: a tow drifts down the screen (a negative y)");
            Check.positive("hits", hits);
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
     * @param paths a walker wave's ground paths, one list of {@code [x, y]} points per unit (y below
     *     the top edge, at the wave's {@code t}; they then scroll with the ground)
     * @param easy changes on easy
     * @param hard changes on hard
     * @param skip the difficulties the wave is left out on (Level 06's hard-only Coilwyrm pair)
     * @param tag M5 part C: a name a secondary {@code escapes} objective may be scoped to (Level 09's
     *     {@code bridge} packs)
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
            Optional<List<List<Point>>> paths,
            Optional<LoopBack> loopBack,
            Optional<Change> easy,
            Optional<Change> hard,
            Optional<List<String>> skip,
            Optional<String> tag) {
        public Wave {
            Check.notNegative("t", t);
            tag.ifPresent(name -> Check.that(!name.isBlank(), "tag: a name"));
            skip.ifPresent(names -> names.forEach(Difficulty::of));
            paths.ifPresent(p -> {
                Check.that(!p.isEmpty(), "paths: at least one path");
                p.forEach(path -> Check.that(!path.isEmpty(), "paths: every path has a point"));
            });
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

        /** Whether it flies on {@code difficulty}. */
        public boolean fliesOn(Difficulty difficulty) {
            return skip.map(names -> names.stream().map(Difficulty::of).noneMatch(difficulty::equals))
                    .orElse(true);
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

    /**
     * A difficulty's changes to a wave.
     *
     * @param hold part G: another hold, s (Level 07's easy Mantis pincer holding 4 s)
     * @param loops M5 part D: a swarm's loop-backs instead of its {@code loop_back.count} (Level 10's
     *     hard: 2)
     */
    public record Change(
            Optional<Entry> from,
            Optional<Edge> edge,
            Optional<Integer> count,
            Optional<Integer> breakGroup,
            Optional<Double> warning,
            Optional<Double> hold,
            Optional<Integer> loops) {
        public Change {
            hold.ifPresent(h -> Check.notNegative("hold", h));
            loops.ifPresent(n -> Check.positive("loops", n));
        }
    }

    /**
     * Part F: a segment chain's loop-back: {@code after} s past the end of its path (off the
     * screen) its head re-enters on {@code path} ({@code [x, y]} points, y px below the top edge),
     * shifted sideways to start at the head's x; without a path straight up from the bottom edge.
     * Its bottom-edge warning lasts the wave's {@code warning} (at least the 3 s minimum). M5 part
     * D: a swarm's leader point loops back {@code count} times (1 when left out), each re-entry
     * {@code after} s past the previous path's end; a chain loops back once.
     */
    public record LoopBack(double after, Optional<List<Point>> path, Optional<Integer> count) {
        public LoopBack {
            Check.positive("after", after);
            count.ifPresent(n -> Check.positive("count", n));
        }
    }

    /**
     * Part F: a dark level (Level 06, Darkness rules). {@code headlight}: the ship's cone, on from
     * {@code from} s, {@code length} px (an {@code easy} length), {@code angle} ° wide.
     * {@code flares}: fired at {@code t} at {@code x}, starting {@code y} px below the top edge
     * (each may be left out on a difficulty with {@code skip}), falling {@code flare.seconds}
     * ({@code easy_seconds}) with a pool of {@code flare.radius} px drifting {@code flare.drift}
     * px/s down the screen. {@code lights}: static pools on the ground (dome and rail lamps),
     * entering at the top edge at {@code t}. {@code ambient}: the ground's brightness outside light.
     */
    public record Darkness(
            Headlight headlight, FlareFall flare, List<Flare> flares, Optional<List<Light>> lights, double ambient) {
        public Darkness {
            Check.that(ambient >= 0 && ambient <= 1, "ambient is between 0 and 1");
        }
    }

    public record Headlight(double from, double length, Optional<Double> easy, double angle) {
        public Headlight {
            Check.notNegative("from", from);
            Check.positive("length", length);
            Check.positive("angle", angle);
        }
    }

    public record FlareFall(double seconds, Optional<Double> easySeconds, double radius, double drift) {
        public FlareFall {
            Check.positive("seconds", seconds);
            Check.positive("radius", radius);
            Check.notNegative("drift", drift);
        }
    }

    public record Flare(double t, double x, double y, Optional<List<String>> skip) {
        public Flare {
            Check.notNegative("t", t);
            skip.ifPresent(names -> names.forEach(Difficulty::of));
        }

        /** Whether it is fired on {@code difficulty}. */
        public boolean firedOn(Difficulty difficulty) {
            return skip.map(names -> names.stream().map(Difficulty::of).noneMatch(difficulty::equals))
                    .orElse(true);
        }
    }

    public record Light(double t, double x, double radius) {
        public Light {
            Check.notNegative("t", t);
            Check.positive("radius", radius);
        }
    }

    /**
     * A ground target: a destructible ({@code hp}, {@code bounty}, {@code drop}; one that
     * {@code reveals} a secret drops its hidden crate when destroyed), a trigger hit
     * {@code hits} times that {@code reveals} a secret, or ground enemies of a stat block
     * ({@code enemy}, such as a Spine Turret nest), which may belong to a {@code group} of the
     * secondary objective.
     *
     * @param layer and {@code size}: a destructible's or trigger's layer and hit box in px; an
     *     enemy's come from its stat block
     * @param at where each of them is placed
     * @param hardened only {@code anti-ground} weapons damage it (design/enemies, layer rules)
     * @param bonusDrop a second pickup it drops with its {@code drop} (Level 04's supply drop: a
     *     special charge, only with a special fitted)
     * @param easy another placement list on easy (an enemy nest's size)
     * @param hard another placement list on hard
     * @param sprite a destructible's sprite set: {@code <sprite>_0..2} (intact, damaged, wrecked)
     *     and {@code <sprite>-break_<n>}; Level 01's cargo container if left out
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
            Optional<Pickup> bonusDrop,
            Optional<String> enemy,
            Optional<String> group,
            Optional<Placements> easy,
            Optional<Placements> hard,
            Optional<String> sprite,
            Optional<Boolean> dark) {
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
                Check.that(hits.isEmpty() || reveals.isPresent(), "a trigger (hits) reveals a secret");
                Check.that(bonusDrop.isEmpty() || drop.isPresent(), "a bonus_drop comes with a drop");
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
    /**
     * A secret: its hidden crate's credits, or (part F) a {@code data_core} that pays none and
     * unlocks a shop item early; its radio line plays when it is found (a data core's when it is
     * collected).
     */
    public record Secret(String name, int crate, RadioLine radio, Optional<DataCore> dataCore) {
        public Secret {
            if (dataCore.isPresent()) {
                Check.notNegative("crate", crate);
            } else {
                Check.positive("crate", crate);
            }
        }
    }

    /** A data core: the shop item it {@code unlocks} (design/systems/economy, Data cores). */
    public record DataCore(String unlocks) {}

    /**
     * A pickup carried by a unit of the wave starting at {@code droppedBy.wave}, dropped when it is
     * destroyed, by the last unit of the ground-target group {@code droppedBy.group}, or by one of
     * the boss's {@code droppedBy.parts}.
     *
     * @param skip part G: the difficulties it is left out on (Level 07's hard: no armour patch
     *     before the boss)
     */
    public record PlacedPickup(Pickup pickup, Carrier droppedBy, Optional<List<String>> skip) {
        public PlacedPickup {
            skip.ifPresent(names -> names.forEach(Difficulty::of));
        }

        /** Whether it drops on {@code difficulty}. */
        public boolean dropsOn(Difficulty difficulty) {
            return skip.map(names -> names.stream().map(Difficulty::of).noneMatch(difficulty::equals))
                    .orElse(true);
        }
    }

    /**
     * The {@code unit} ({@code first}, {@code second} or {@code last}) of the wave starting at
     * {@code wave} seconds, the {@code last} unit of a ground-target {@code group} to die (when the
     * group is cleared), or (part G) the {@code unit}-th of the level boss's {@code parts} to be
     * shot off (Level 07's first destroyed bay sac); parts lost in the boss's death drop nothing.
     */
    public record Carrier(
            Optional<Double> wave, Optional<String> group, Optional<List<String>> parts, CarrierUnit unit) {
        public Carrier {
            Check.that(
                    (wave.isPresent() ? 1 : 0) + (group.isPresent() ? 1 : 0) + (parts.isPresent() ? 1 : 0) == 1,
                    "dropped_by: give a wave, a group or the boss's parts");
            Check.that(
                    group.isEmpty() || unit == CarrierUnit.LAST,
                    "dropped_by: a group's pickup comes from its last unit");
            parts.ifPresent(names -> {
                Check.notEmpty("parts", names);
                Check.that(
                        unit != CarrierUnit.SECOND || names.size() >= 2,
                        "dropped_by: the second of the parts needs two of them");
            });
        }

        /** The carrier wave's time; NaN for a group's pickup. */
        public double waveT() {
            return wave.orElse(Double.NaN);
        }
    }

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
     * @param shout whether the voice shouts the line (design/audio/voice), apart from how it queues
     * @param easy changes on easy
     * @param hard changes on hard
     * @param requires what has to be fitted for it to play: {@code special} or (part G)
     *     {@code homing}, a weapon with homing delivery, or (M5 part C) {@code escort}: an escort
     *     flying (hired, fitted and not ejected; Rook's scripted lines), asked when it is due
     * @param requiresNot part G: what must not be fitted for it to play (Level 07's "You can't touch
     *     it up there" without a homing weapon)
     * @param timeout part G, a boss-phase cue: true plays it only when the phase before ended on
     *     its timeout with parts it waited for still alive (Level 07's "Forget the sacs")
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
            Optional<Boolean> shout,
            Optional<RadioChange> easy,
            Optional<RadioChange> hard,
            Optional<String> requires,
            Optional<Count> allies,
            Optional<String> phase,
            Optional<String> requiresNot,
            Optional<Boolean> timeout) {
        public RadioCue {
            Check.that(
                    phase.isPresent() == (event.orElse(null) == CueEvent.BOSS_PHASE),
                    "a boss-phase event names its phase, other triggers do not");
            Check.that(t.isPresent() != event.isPresent(), "give the trigger as t or as event");
            requires.ifPresent(Requirement::check);
            requiresNot.ifPresent(Requirement::check);
            Check.that(
                    requires.isEmpty() || !requires.equals(requiresNot),
                    "requires and requires_not name different things");
            Check.that(
                    timeout.isEmpty() || event.orElse(null) == CueEvent.BOSS_PHASE,
                    "only a boss-phase cue waits for a timeout");
            Check.that(
                    allies.isEmpty() || event.orElse(null) == CueEvent.LEVEL_END,
                    "only a level-end cue names the allies home");
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

    /**
     * What a radio cue requires: {@code special} (a special fitted), {@code homing} (a weapon with
     * homing delivery fitted) or (M5 part C) {@code escort} (an escort flying); a prompt knows only
     * {@code special}.
     */
    static final class Requirement {
        /** A special fitted. */
        static final String SPECIAL = "special";
        /** A weapon with homing delivery fitted. */
        static final String HOMING = "homing";
        /** M5 part C: an escort flying (hired, fitted and not ejected). */
        static final String ESCORT = "escort";

        private Requirement() {}

        static void check(String requires) {
            Check.that(
                    requires.equals(SPECIAL) || requires.equals(HOMING) || requires.equals(ESCORT),
                    "requires: 'special', 'homing' or 'escort' (on a prompt only 'special' is known), was '" + requires
                            + "'");
        }

        static void checkPrompt(String requires) {
            Check.that(
                    requires.equals(SPECIAL), "requires: on a prompt only 'special' is known, was '" + requires + "'");
        }
    }

    /** A whole-number range, written {@code [min, max]}. */
    @JsonFormat(shape = JsonFormat.Shape.ARRAY)
    public record Count(int min, int max) {
        public Count {
            Check.that(
                    0 <= min && min <= max,
                    "a count range is written [min, max] from 0, was [" + min + ", " + max + "]");
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
        ENEMY_ESCAPED,
        /** The first hit on a convoy unit; the line may name it as {@code {ally}} ("Three"). */
        @JsonProperty("first-ally-hit")
        FIRST_ALLY_HIT,
        /** The first convoy unit lost; the line may name it as {@code {ally}}. */
        @JsonProperty("first-ally-lost")
        FIRST_ALLY_LOST,
        /**
         * M5 part B (design/player/wingmen, Scripted lines about him): the first kill in the attempt
         * whose killing shot or blast is the escort's (Rook's), while he flies; never after he ejects,
         * nor in a level he does not fly in. Names no enemy.
         */
        @JsonProperty("escort-first-kill")
        ESCORT_FIRST_KILL,
        /** The primary objective failed: the line on the mission failed screen (not on the radio). */
        @JsonProperty("mission-failed")
        MISSION_FAILED,
        /** The boss entered a later phase; the cue names the phase as {@code phase}. */
        @JsonProperty("boss-phase")
        BOSS_PHASE,
        /** The boss was destroyed. */
        @JsonProperty("boss-destroyed")
        BOSS_DESTROYED,
        /** M5 part C: the level's first hold zone starts to ease down (once). */
        @JsonProperty("hold-start")
        HOLD_START,
        /** M5 part C: the attempt's first pounce takes off. Names no enemy. */
        @JsonProperty("first-pounce")
        FIRST_POUNCE,
        /** M5 part C: the collapse's shadow starts. */
        @JsonProperty("collapse")
        COLLAPSE,
        /**
         * M5 part D: every loss of a convoy unit the player could have saved (the first too, after the
         * {@code first-ally-lost} cue); the line may name it as {@code {ally}}.
         */
        @JsonProperty("ally-lost")
        ALLY_LOST,
        /** M5 part D: the scripted loss's lance took its unit. */
        @JsonProperty("scripted-loss")
        SCRIPTED_LOSS,
        /** M5 part D: the attempt's first decloak (a cloaked enemy). */
        @JsonProperty("first-decloak")
        FIRST_DECLOAK,
        /** M5 part D: the attempt's first swarm loop-back. */
        @JsonProperty("first-loop-back")
        FIRST_LOOP_BACK
    }

    /**
     * The primary objective's kind ({@code reach-end}, {@code escort} with its {@code escort} block,
     * or {@code destroy-targets} with the ground-target groups it names as {@code targets}) and the
     * optional secondary objective.
     *
     * @param targets the groups (ground targets' {@code group}) a destroy-targets primary needs
     *     destroyed; it fails as soon as a unit of one leaves the screen alive (Level 05)
     */
    public record Objectives(
            String primary, Optional<Escort> escort, Optional<List<String>> targets, Optional<Secondary> secondary) {
        public Objectives {
            Check.that(
                    primary.equals("reach-end") || primary.equals("escort") || primary.equals("destroy-targets"),
                    "primary: reach-end, escort or destroy-targets, was '" + primary + "'");
            Check.that(
                    escort.isPresent() == primary.equals("escort"),
                    "an escort primary has its escort block, other primaries do not");
            Check.that(
                    targets.isPresent() == primary.equals("destroy-targets"),
                    "a destroy-targets primary names its targets, other primaries do not");
            targets.ifPresent(t -> Check.notEmpty("targets", t));
            Check.that(
                    targets.isEmpty() || secondary.flatMap(Secondary::groups).isEmpty(),
                    "the groups belong to the primary (targets) or to the secondary, not both");
        }

        /** The ground-target groups the level names: the primary's targets or the secondary's groups. */
        public List<String> groups() {
            return targets.or(() -> secondary.flatMap(Secondary::groups)).orElse(List.of());
        }
    }

    /**
     * The {@code escort} primary objective (design/allies; Level 04): a convoy of {@code ally}
     * units, one per height in {@code y} (centres, px below the top edge, the leading unit first),
     * rolling in from the bottom edge as {@code enter} says and following the level's road. Each
     * unit alive at the end pays {@code credits}; the objective fails when every unit is lost.
     *
     * <p>M5 part D (Level 10, user decisions D1, D4 and D5 = a): an air escort (an ally that {@code
     * follows: lanes}) gives {@code stations} instead of {@code y}, an optional {@code liftoff}
     * instead of {@code enter} and no {@code hook}; an optional {@code climb} (the climb-out) and
     * {@code scripted_loss}. Then each <em>saveable</em> unit home (every unit but the scripted
     * loss's) pays {@code credits}, and the objective fails at once when every saveable unit is lost.
     *
     * @param y a ground convoy's heights
     * @param enter a ground convoy's roll-in
     * @param hook the target-the-objective hook: which enemies' aimed attacks go for the convoy (a
     *     ground convoy's; required there)
     * @param easy the units' HP on easy
     * @param hard the units' HP on hard
     * @param stations M5 part D: an air escort's stations, the leading unit first
     * @param liftoff M5 part D: an air escort's liftoff from its pads
     * @param climb M5 part D: an air escort's climb-out
     * @param scriptedLoss M5 part D: the unit lost in a scripted event (Level 10's Lifeline Three)
     */
    public record Escort(
            String ally,
            Optional<List<Double>> y,
            int credits,
            Optional<Enter> enter,
            Optional<Hook> hook,
            Optional<AllyChange> easy,
            Optional<AllyChange> hard,
            Optional<List<Station>> stations,
            Optional<Liftoff> liftoff,
            Optional<Climb> climb,
            Optional<ScriptedLoss> scriptedLoss) {
        public Escort {
            Check.notNegative("credits", credits);
            Check.that(
                    y.isPresent() != stations.isPresent(),
                    "give the convoy's y (on the road) or its stations (in the air)");
            y = y.map(List::copyOf);
            y.ifPresent(heights -> Check.notEmpty("y", heights));
            stations = stations.map(List::copyOf);
            stations.ifPresent(list -> Check.notEmpty("stations", list));
            if (y.isPresent()) {
                Check.that(enter.isPresent(), "a convoy on the road gives its enter");
                Check.that(hook.isPresent(), "a convoy on the road gives its hook");
                Check.that(
                        liftoff.isEmpty() && climb.isEmpty() && scriptedLoss.isEmpty(),
                        "liftoff, climb and scripted_loss belong to an air escort (stations)");
            } else {
                Check.that(enter.isEmpty(), "an air escort lifts off (liftoff) instead of rolling in (enter)");
                Check.that(hook.isEmpty(), "an air escort has no hook: every enemy bullet and contact hurts it");
                int units = stations.orElseThrow().size();
                liftoff.ifPresent(l -> Check.that(
                        l.pads().size() == units,
                        "liftoff.pads: one per station, " + units + ", not "
                                + l.pads().size()));
                scriptedLoss.ifPresent(loss -> Check.that(
                        loss.unit() >= 1 && loss.unit() <= units,
                        "scripted_loss.unit: one of the units, 1 to " + units + ", was " + loss.unit()));
            }
        }

        /** The number of units. */
        public int units() {
            return y.map(List::size).orElseGet(() -> stations.orElseThrow().size());
        }

        /** Whether it is an air escort (stations in the band). */
        public boolean air() {
            return stations.isPresent();
        }

        /** The units the player can save: every unit but the scripted loss's. */
        public int saveable() {
            return units() - (scriptedLoss.isPresent() ? 1 : 0);
        }
    }

    /**
     * M5 part D: an air escort unit's station: {@code at} ({@code [x, y]}, px from the play field's
     * left edge, px below the top edge) with the lane sway {@code sway} ({@code [ax, ay]} px): the
     * unit flies at {@code at} + (ax sin θ, ay sin 2θ), θ = 2π (t ÷ {@code period} + {@code phase}),
     * t the level clock.
     */
    public record Station(Point at, Point sway, double period, double phase) {
        public Station {
            Check.positive("period", period);
            Check.that(phase >= 0 && phase <= 1, "phase: 0 to 1, was " + phase);
            Check.notNegative("sway", sway.x());
            Check.notNegative("sway", sway.y());
        }
    }

    /**
     * M5 part D: an air escort's liftoff: at {@code t} each unit stands on its pad (one {@code [x, y]}
     * screen point per unit, px below the top edge) and eases to its station over {@code seconds}.
     */
    public record Liftoff(double t, double seconds, List<Point> pads) {
        public Liftoff {
            Check.notNegative("t", t);
            Check.positive("seconds", seconds);
            pads = List.copyOf(pads);
        }
    }

    /** M5 part D: the climb-out: from {@code t} the units alive climb off the top edge over {@code seconds}, home. */
    public record Climb(double t, double seconds) {
        public Climb {
            Check.positive("t", t);
            Check.positive("seconds", seconds);
        }
    }

    /**
     * M5 part D: the scripted loss: unit {@code unit} (1-based) is untouchable until {@code t}; its
     * glow starts {@code glow} s before, the lance takes it at {@code t}.
     */
    public record ScriptedLoss(int unit, double t, double glow) {
        public ScriptedLoss {
            Check.positive("t", t);
            Check.notNegative("glow", glow);
            Check.that(glow <= t, "glow: starts in the level, at most t");
        }
    }

    /** The convoy rolls in: the first unit at {@code t} s, then one every {@code interval} s, at {@code speed} px/s up the screen. */
    public record Enter(double t, double interval, double speed) {
        public Enter {
            Check.notNegative("t", t);
            Check.notNegative("interval", interval);
            Check.positive("speed", speed);
        }
    }

    /**
     * The target-the-objective hook (design/enemies): in {@code mode} (only {@code nearest} so far)
     * the aimed attacks of the {@code enemies} go at the ship or the nearest convoy unit.
     */
    public record Hook(String mode, List<String> enemies) {
        public Hook {
            Check.that(mode.equals("nearest"), "mode: only 'nearest' is implemented, was '" + mode + "'");
            Check.notEmpty("enemies", enemies);
            enemies = List.copyOf(enemies);
        }
    }

    /** A difficulty's change to the convoy units: their HP. */
    public record AllyChange(double hp) {
        public AllyChange {
            Check.positive("hp", hp);
        }
    }

    /**
     * The road on the ground layer (Level 04): a curve through {@code points}, one {@code [t, x]}
     * each ({@code t} when it passes the middle of the screen, as for placed backdrop pieces),
     * drawn as a ribbon {@code width} px wide with the backdrop image {@code texture}
     * ({@code assets/backdrop/level-NN/<texture>.png}; a flat placeholder colour without one).
     */
    public record Road(double width, Optional<String> texture, List<RoadPoint> points) {
        public Road {
            Check.positive("width", width);
            Check.that(points.size() >= 2, "points: a road has at least two");
            for (int i = 1; i < points.size(); i++) {
                Check.that(points.get(i).t() > points.get(i - 1).t(), "points: in time order, point " + i);
            }
            points = List.copyOf(points);
        }
    }

    /** A road point: {@code t} when it passes the middle of the screen (may lie before the start), its {@code x}. */
    @JsonFormat(shape = JsonFormat.Shape.ARRAY)
    public record RoadPoint(double t, double x) {}

    /**
     * Destroy at least {@code killRatio} of all enemies for {@code credits}, clear the ground
     * enemies of every one of the {@code groups} (named by its ground targets' {@code group}) for
     * {@code credits} each, let none of the enemy {@code escapes} leave the screen alive ("nothing
     * gets through", Level 03) for {@code credits}, destroy every unit of the enemies
     * {@code killAll} (Level 05's "Scorched crater", the tracker's {@code label}) for {@code credits},
     * or (part G) shoot off every one of the level boss's {@code parts} before its phase
     * {@code before} ends (Level 07's "Gut the bays": all eight sacs before the broadside phase
     * times out; the tracker's {@code label}) for {@code credits}. M5 part C (user decision D6 = a):
     * an {@code escapes} objective's {@code tag} scopes it to the units of the waves with that tag
     * (Level 09's two {@code bridge} packs). An optional {@code name} names the objective where the
     * generated wording would mislead (Level 09's "Hold the bridge": only the bridge packs count):
     * the briefing's bonus line and the README's credit table use it.
     */
    public record Secondary(
            Optional<Double> killRatio,
            Optional<List<String>> groups,
            Optional<String> escapes,
            Optional<List<String>> killAll,
            Optional<List<String>> parts,
            Optional<String> before,
            Optional<String> label,
            int credits,
            Optional<String> tag,
            Optional<String> name) {
        /** Without M5 part C's wave tag and name. */
        public Secondary(
                Optional<Double> killRatio,
                Optional<List<String>> groups,
                Optional<String> escapes,
                Optional<List<String>> killAll,
                Optional<List<String>> parts,
                Optional<String> before,
                Optional<String> label,
                int credits) {
            this(
                    killRatio,
                    groups,
                    escapes,
                    killAll,
                    parts,
                    before,
                    label,
                    credits,
                    Optional.empty(),
                    Optional.empty());
        }

        public Secondary {
            name.ifPresent(n -> Check.that(!n.isBlank(), "name: give the objective's name"));
            Check.that(tag.isEmpty() || escapes.isPresent(), "only an escapes objective is scoped to a wave tag");
            Check.that(
                    (killRatio.isPresent() ? 1 : 0)
                                    + (groups.isPresent() ? 1 : 0)
                                    + (escapes.isPresent() ? 1 : 0)
                                    + (killAll.isPresent() ? 1 : 0)
                                    + (parts.isPresent() ? 1 : 0)
                            == 1,
                    "give kill_ratio, groups, escapes, kill_all or parts");
            killRatio.ifPresent(r -> Check.share("kill_ratio", r));
            groups.ifPresent(g -> Check.notEmpty("groups", g));
            killAll.ifPresent(k -> Check.notEmpty("kill_all", k));
            parts.ifPresent(p -> Check.notEmpty("parts", p));
            Check.that(parts.isPresent() == before.isPresent(), "a parts objective names the phase they die before");
            Check.that(
                    killAll.isPresent() || parts.isPresent() ? label.isPresent() : label.isEmpty(),
                    "a kill_all or parts objective has its tracker label, the others none");
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
     * @param bossSting the sting that cuts in over the level track when the boss arrives
     *     ({@code miniboss-sting}, Level 05), the track returning after it
     * @param ambienceFrom the level time from which only the ambience plays: the theme fades out
     *     (Level 06's last seconds)
     * @param voiceLoop a radio line looping faintly under a section (Level 06's perimeter beacon)
     * @param bossWarning part G: the warning track (by file name, {@code boss-warning}: track 22 with
     *     the klaxon and the warning banner) when an act boss arrives, the theme crossfading out
     * @param bossTrack part G: the boss theme (by file name, {@code choir-descends}: track 18) that
     *     comes in after the warning and fades out at the kill, leaving the ambience
     * @param fullOn M5 part C: run-time events that play the full mix: {@code hold} while a hold zone
     *     runs (fading in over 1 s as it starts, out over 4 s after it ends), {@code collapse} from
     *     the collapse to the level's end
     * @param duck M5 part D: the theme's duck on a run-time event (Level 10's scripted loss)
     * @param ambienceChanges M5 part D: the ambience crossfading to another at a section's start
     */
    public record Music(
            int track,
            int startSection,
            Optional<Double> startDb,
            int fullSection,
            Optional<Map<Integer, Stems>> stems,
            String ambience,
            String endJingle,
            Optional<String> bossSting,
            Optional<Double> ambienceFrom,
            Optional<VoiceLoop> voiceLoop,
            Optional<String> bossWarning,
            Optional<String> bossTrack,
            Optional<List<FullOn>> fullOn,
            Optional<Duck> duck,
            Optional<List<AmbienceChange>> ambienceChanges) {
        /**
         * M5 part D: on the event {@code on} the theme ducks by {@code db} for {@code seconds}, no sting;
         * with the radio's duck the lower applies (they do not add up).
         */
        public record Duck(DuckOn on, double db, double seconds) {
            public Duck {
                Check.that(db < 0, "db: a duck lowers the theme, below 0, was " + db);
                Check.positive("seconds", seconds);
            }
        }

        /** M5 part D: the run-time events a duck can follow. */
        public enum DuckOn {
            @JsonProperty("scripted-loss")
            SCRIPTED_LOSS
        }

        /** M5 part D: at section {@code section}'s start (1-based) the ambience crossfades to {@code ambience} over {@code crossfade} s. */
        public record AmbienceChange(int section, String ambience, double crossfade) {
            public AmbienceChange {
                Check.that(section >= 2, "section: a later section than the first, was " + section);
                Check.that(!ambience.isEmpty(), "ambience: an ambience key");
                Check.positive("crossfade", crossfade);
            }
        }

        /** Without M5 part D's duck and ambience changes. */
        public Music(
                int track,
                int startSection,
                Optional<Double> startDb,
                int fullSection,
                Optional<Map<Integer, Stems>> stems,
                String ambience,
                String endJingle,
                Optional<String> bossSting,
                Optional<Double> ambienceFrom,
                Optional<VoiceLoop> voiceLoop,
                Optional<String> bossWarning,
                Optional<String> bossTrack,
                Optional<List<FullOn>> fullOn) {
            this(
                    track,
                    startSection,
                    startDb,
                    fullSection,
                    stems,
                    ambience,
                    endJingle,
                    bossSting,
                    ambienceFrom,
                    voiceLoop,
                    bossWarning,
                    bossTrack,
                    fullOn,
                    Optional.empty(),
                    Optional.empty());
        }

        /** The ambience changes, in section order; empty for none. */
        public List<AmbienceChange> ambienceChangeList() {
            return ambienceChanges.orElse(List.of());
        }

        /**
         * The timed radio line of {@code speaker} (its first), looping at {@code db} (below full)
         * while section {@code section} (1-based) plays; silent while the speaker has no voice file.
         */
        public record VoiceLoop(String speaker, int section, double db) {
            public VoiceLoop {
                Check.positive("section", section);
                Check.that(db <= 0, "db: must not be above full level");
            }
        }

        /** M5 part C: a run-time event that plays the full mix ({@code full_on}). */
        public enum FullOn {
            @JsonProperty("hold")
            HOLD,
            @JsonProperty("collapse")
            COLLAPSE
        }

        /** Whether {@code event} plays the full mix (its {@code full_on} names it). */
        public boolean fullOn(FullOn event) {
            return fullOn.map(events -> events.contains(event)).orElse(false);
        }

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
            ambienceFrom.ifPresent(t -> Check.positive("ambience_from", t));
            Check.that(bossTrack.isEmpty() || bossWarning.isPresent(), "a boss_track comes in after its boss_warning");
            fullOn.ifPresent(events -> {
                Check.notEmpty("full_on", events);
                Check.that(events.size() == new java.util.HashSet<>(events).size(), "full_on: each event once");
            });
            ambienceChanges = ambienceChanges.map(List::copyOf);
            ambienceChanges.ifPresent(changes -> {
                Check.notEmpty("ambience_changes", changes);
                for (int i = 1; i < changes.size(); i++) {
                    Check.that(
                            changes.get(i).section() > changes.get(i - 1).section(),
                            "ambience_changes: in section order, one per section");
                }
            });
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
     * @param objective the OBJECTIVE field shown from sensor L1 ("ESCORT 5 CRAWLERS"); none without
     * @param varga Dr. Varga's intel line for each sensor level, all four: {@code none}, {@code l1},
     *     {@code l2}, {@code l3}
     * @param required M5 part C (user decision D7 = a): the traits among {@code traits} the primary
     *     objective cannot be met without (Level 09's {@code anti-ground}): a launch warning at every
     *     sensor level
     */
    public record ThreatProfile(
            String setting,
            List<String> layers,
            int density,
            List<String> traits,
            List<String> hazards,
            String boss,
            Optional<String> specials,
            Optional<String> objective,
            Map<String, String> varga,
            Optional<List<String>> required) {
        /** The keys of Varga's lines, from no sensor suite to L3. */
        public static final List<String> SENSOR_KEYS = List.of("none", "l1", "l2", "l3");

        public ThreatProfile {
            Check.that(density >= 1 && density <= 5, "density: 1–5, was " + density);
            Check.that(
                    varga.size() == SENSOR_KEYS.size() && SENSOR_KEYS.containsAll(varga.keySet()),
                    "varga: a line for each of " + SENSOR_KEYS + ", found " + varga.keySet());
            layers = List.copyOf(layers);
            traits = List.copyOf(traits);
            hazards = List.copyOf(hazards);
            varga = Map.copyOf(varga);
            required = required.map(List::copyOf);
            List<String> recommended = traits;
            required.ifPresent(names -> {
                Check.notEmpty("required", names);
                for (String name : names) {
                    Check.that(
                            recommended.contains(name),
                            "required: '" + name + "' is not one of the traits " + recommended);
                }
            });
        }

        /** Without M5 part C's required traits. */
        public ThreatProfile(
                String setting,
                List<String> layers,
                int density,
                List<String> traits,
                List<String> hazards,
                String boss,
                Optional<String> specials,
                Optional<String> objective,
                Map<String, String> varga) {
            this(setting, layers, density, traits, hazards, boss, specials, objective, varga, Optional.empty());
        }

        /** The traits the primary objective cannot be met without; empty for none. */
        public List<String> requiredTraits() {
            return required.orElse(List.of());
        }

        /** Varga's line for a sensor level, 0 (no sensor suite) to 3. */
        public String vargaLine(int sensor) {
            return varga.get(SENSOR_KEYS.get(sensor));
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

    /** Aimed shots fired in bursts of {@code burst}; walking at {@code speedFactor} times its speed. */
    public record EnemyChange(Optional<Integer> burst, Optional<Double> speedFactor) {
        public EnemyChange {
            burst.ifPresent(b -> Check.positive("burst", b));
            speedFactor.ifPresent(f -> Check.positive("speed_factor", f));
        }

        /** Only a burst change. */
        public EnemyChange(Optional<Integer> burst) {
            this(burst, Optional.empty());
        }
    }
}
