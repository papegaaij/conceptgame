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
 * @param water M5 part E (user decision E1 = a): the whole play field is water (Levels 11–13), where
 *     the torpedo runs; false if not given
 * @param convoy M5 part E: a naval convoy outside the objectives (Level 11's cargo ships and frigate)
 * @param weather M5 part F: the level's wind, lightning, cloud-bank occlusion and rain (Level 12's storm)
 * @param sondes M5 part F: the weather sondes to fly through (Level 12)
 * @param dropFactor M5 part F: each enemy's drop {@code every} is divided by it, rounded to the nearest, at
 *     least 1 (Level 12's shield cells, 1.5); 1 if not given
 * @param pointDrops M5 part G (design/allies, CDF supply drone; user decision G6 = a): the data's {@code
 *     drops}, pickups released at a point in the field at a time (Level 13's supply drones' armour
 *     patches); named so beside {@link #drops()}, the drop factor
 * @param waterSplash M5 part G (user decision G5 = a): which bursts on a {@code water: true} level play the
 *     water splash: {@code all} (if not given, Levels 11 and 12) or {@code naval-only} (Level 13's ice: only
 *     a burst on a naval unit and a naval unit's death); presentation only
 * @param rootBursts M5 part H (design/campaign Level 14, Hazards; decision H2 = a): the root bursts, ground
 *     units grown at a point of the field after a telegraph instead of entering at the top edge
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
        Optional<Collapse> collapse,
        Optional<Boolean> water,
        Optional<Convoy> convoy,
        Optional<Weather> weather,
        Optional<SondesData> sondes,
        Optional<Double> dropFactor,
        @JsonProperty("drops") Optional<List<Drop>> pointDrops,
        Optional<String> waterSplash,
        Optional<RootBursts> rootBursts) {
    public LevelData {
        dropFactor.ifPresent(factor -> Check.positive("drop_factor", factor));
        waterSplash.ifPresent(splash -> Check.that(
                WATER_SPLASH_ALL.equals(splash) || WATER_SPLASH_NAVAL.equals(splash),
                "water_splash: 'all' or 'naval-only', was '" + splash + "'"));
        Check.that(waterSplash.isEmpty() || water.orElse(false), "water_splash: only on a water: true level");
        pointDrops = pointDrops.map(List::copyOf);
        pointDrops.ifPresent(list -> {
            for (int i = 1; i < list.size(); i++) {
                Check.that(list.get(i).t() >= list.get(i - 1).t(), "drops[" + i + "]: listed in time order");
            }
        });
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

    /** M5 part E: whether the whole play field is water, its {@code water}; false if not given. */
    public boolean overWater() {
        return water.orElse(false);
    }

    /** M5 part G: {@code water_splash: all}, every burst on a water level is the water splash. */
    public static final String WATER_SPLASH_ALL = "all";

    /** M5 part G: {@code water_splash: naval-only}, only a burst on a naval unit and its death (Level 13). */
    public static final String WATER_SPLASH_NAVAL = "naval-only";

    /**
     * M5 part G (user decision G5 = a): whether only a burst on a naval unit (and a naval unit's death)
     * plays the water splash on this water level, every other burst the ground burst (Level 13's ice).
     */
    public boolean navalSplashOnly() {
        return waterSplash.filter(WATER_SPLASH_NAVAL::equals).isPresent();
    }

    /** M5 part G: the pickups released at a point in the field (the data's {@code drops}), in time order. */
    public List<Drop> released() {
        return pointDrops.orElse(List.of());
    }

    /** M5 part H: the level's root bursts, in time order; none if not given. */
    public List<RootBurst> bursts() {
        return rootBursts.map(RootBursts::bursts).orElse(List.of());
    }

    /** M5 part F: the level's drop factor, its {@code drop_factor}; 1 if not given. */
    public double drops() {
        return dropFactor.orElse(1.0);
    }

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
     * end the first and the last section's speed carry on. M5 part G: around a timed halt (a section of
     * speed 0 without an arena) the scroll eases down to 0 over the halt's {@code ramp} before it and
     * back up over as long after it (smoothstep, as the simulation's {@code Sortie.timedHaltSpeed}, here
     * integrated exactly: within a pixel of the simulation's sum over its steps).
     */
    public double scrollAt(double t) {
        double scroll = 0;
        for (int i = 0; i < sections.size(); i++) {
            Section section = sections.get(i);
            double start = sectionStart(i);
            if (t < section.end() || i == sections.size() - 1) {
                return scroll + travelled(i, start, t);
            }
            scroll += travelled(i, start, section.end());
        }
        throw new IllegalStateException("a level has sections");
    }

    /** M5 part G: the px section {@code i} scrolls from {@code from} to {@code to} s (inside it, or past its ends). */
    private double travelled(int i, double from, double to) {
        Section section = sections.get(i);
        double speed = speedOf(section);
        double start = sectionStart(i);
        double end = section.end();
        double px = (to - from) * speed;
        if (speed == 0) {
            return px;
        }
        if (i + 1 < sections.size() && sections.get(i + 1).timedHalt()) {
            double ramp = Math.min(sections.get(i + 1).rampSeconds(), end - start);
            // eased down over [end - ramp, end]: speed × (1 - S(u)) instead of speed
            px -= speed * ramp * (easeIn(clampedU(to, end - ramp, ramp)) - easeIn(clampedU(from, end - ramp, ramp)));
        }
        if (i > 0 && sections.get(i - 1).timedHalt()) {
            double ramp = Math.min(sections.get(i - 1).rampSeconds(), end - start);
            // eased up over [start, start + ramp]: speed × S(u) instead of speed
            px -= speed * ramp * (easeOut(clampedU(to, start, ramp)) - easeOut(clampedU(from, start, ramp)));
        }
        return px;
    }

    private static double clampedU(double t, double from, double ramp) {
        return ramp > 0 ? Math.clamp((t - from) / ramp, 0, 1) : (t >= from ? 1 : 0);
    }

    /** ∫₀ᵘ S(v) dv for the smoothstep S(v) = 3v² − 2v³: what an ease down loses against the full speed. */
    private static double easeIn(double u) {
        return u * u * u - u * u * u * u / 2;
    }

    /** ∫₀ᵘ (1 − S(v)) dv: what an ease up loses against the full speed. */
    private static double easeOut(double u) {
        return u - u * u * u + u * u * u * u / 2;
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
        return atmosphereStretches(false);
    }

    /**
     * As {@link #atmosphereStretches()}; on {@code easy} a peak with an {@code easy} length is that long,
     * centred on its authored stretch (M5 part G: Level 13's whiteout, 6 s on easy; presentation only).
     */
    public List<Stretch> atmosphereStretches(boolean easy) {
        return atmosphereStretches(sections, easy);
    }

    /** As {@link #atmosphereStretches(boolean)} for back-to-back {@code sections} from t = 0. */
    public static List<Stretch> atmosphereStretches(List<Section> sections, boolean easy) {
        List<Stretch> out = new ArrayList<>();
        for (int i = 0; i < sections.size(); i++) {
            Section section = sections.get(i);
            double start = i == 0 ? 0 : sections.get(i - 1).end();
            if (section.peak().isPresent()) {
                // M5 part G: a peak may run to its section's end (Level 13's fog from t=140, an
                // atmosphere split at a time); no empty stretch then.
                Peak peak =
                        easy ? section.peak().get().onEasy() : section.peak().get();
                if (peak.from() > start) {
                    out.add(new Stretch(start, peak.from(), section.atmosphere()));
                }
                out.add(new Stretch(peak.from(), peak.to(), peak.atmosphere()));
                if (peak.to() < section.end()) {
                    out.add(new Stretch(peak.to(), section.end(), section.atmosphere()));
                }
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
     * @param ramp M5 part G, a timed halt's (speed 0 without an arena): s the scroll eases down to 0 before
     *     it and back up after it ({@link #rampSeconds()}, 2 if not given)
     */
    public record Section(
            String name,
            double end,
            Optional<Double> speed,
            Atmosphere atmosphere,
            Optional<Peak> peak,
            List<String> tiles,
            Optional<Boolean> arena,
            Optional<Double> ramp) {
        public Section {
            Check.positive("end", end);
            // M5 part E: 0 only on the arena of a level whose boss is anchored (ContentValidator);
            // M5 part G: or on a timed halt, only with a defend primary.
            speed.ifPresent(s -> Check.notNegative("speed", s));
            ramp.ifPresent(r -> Check.positive("ramp", r));
            Check.that(
                    ramp.isEmpty() || (speed.filter(s -> s == 0).isPresent() && !arena.orElse(false)),
                    "ramp: only on a timed halt (speed 0 without an arena)");
        }

        /**
         * M5 part G (design/campaign Level 13, The halt): whether it is a timed halt, a section of speed 0
         * without an arena, where the level clock runs on and the scroll stands.
         */
        public boolean timedHalt() {
            return speed.filter(s -> s == 0).isPresent() && !isArena();
        }

        /** M5 part G: a timed halt's eases, s: its {@code ramp}, 2 if not given. */
        public double rampSeconds() {
            return ramp.orElse(vanguard.sim.LevelScript.Section.TIMED_HALT_RAMP_SECONDS);
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

    /**
     * A stretch inside a section whose atmosphere peaks at {@code atmosphere} (Level 02's coolant cloud).
     *
     * @param easy M5 part G: the peak's length on easy, s, centred on {@code from}–{@code to} (Level 13's
     *     whiteout: 6 s); the authored stretch on every difficulty if not given (presentation only)
     * @param ambienceDb M5 part G: the setting's ambience is this much louder through the peak, dB, ramping
     *     in and out with the atmosphere (Level 13's wind in the whiteout, +3); none if not given
     */
    public record Peak(
            Atmosphere atmosphere, double from, double to, Optional<Double> easy, Optional<Double> ambienceDb) {
        public Peak {
            Check.that(to > from, "a peak ends after it starts");
            easy.ifPresent(seconds -> Check.that(
                    seconds > 0 && seconds <= to - from, "easy: a peak's easy length is within it, was " + seconds));
        }

        /** Without M5 part G's easy length and ambience level. */
        public Peak(Atmosphere atmosphere, double from, double to) {
            this(atmosphere, from, to, Optional.empty(), Optional.empty());
        }

        /** The peak as easy shows it: its {@code easy} length centred on it, or itself without one. */
        public Peak onEasy() {
            if (easy.isEmpty()) {
                return this;
            }
            double centre = (from + to) / 2;
            double half = easy.get() / 2;
            return new Peak(atmosphere, centre - half, centre + half, easy, ambienceDb);
        }

        /** The ambience's rise through the peak, dB: its {@code ambience_db}, 0 if not given. */
        public double ambienceRise() {
            return ambienceDb.orElse(0.0);
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
            double t,
            String action,
            String keys,
            double seconds,
            Optional<String> skip,
            Optional<String> requires,
            Optional<PromptEvent> event) {
        public Prompt {
            Check.that(event.isPresent() == Double.isNaN(t), "a prompt is shown at a t or on an event, not both");
            if (event.isEmpty()) {
                Check.notNegative("t", t);
            }
            Check.positive("seconds", seconds);
            skip.ifPresent(Layers::of);
            requires.ifPresent(Requirement::checkPrompt);
        }

        /** A timed prompt. */
        public Prompt(
                double t,
                String action,
                String keys,
                double seconds,
                Optional<String> skip,
                Optional<String> requires) {
            this(t, action, keys, seconds, skip, requires, Optional.empty());
        }

        /**
         * From the data: a timed prompt's {@code t}, or (M5 part F) an event prompt's {@code event}
         * instead, its {@code t} then NaN (it is never due by the clock).
         */
        @com.fasterxml.jackson.annotation.JsonCreator
        public static Prompt of(
                @JsonProperty("t") Optional<Double> t,
                @JsonProperty("action") String action,
                @JsonProperty("keys") String keys,
                @JsonProperty("seconds") double seconds,
                @JsonProperty("skip") Optional<String> skip,
                @JsonProperty("requires") Optional<String> requires,
                @JsonProperty("event") Optional<PromptEvent> event) {
            Optional<Double> time = t == null ? Optional.empty() : t;
            Optional<PromptEvent> on = event == null ? Optional.empty() : event;
            Check.that(time.isPresent() != on.isPresent(), "a prompt has its t or its event");
            return new Prompt(
                    time.orElse(Double.NaN),
                    action,
                    keys,
                    seconds,
                    skip == null ? Optional.empty() : skip,
                    requires == null ? Optional.empty() : requires,
                    on);
        }

        /** M5 part F: whether it is shown on an event (the shake-off prompt) rather than at its time. */
        public boolean onEvent() {
            return event.isPresent();
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

    /** M5 part F: the run-time events a prompt can be shown on (until the event ends or for its seconds). */
    public enum PromptEvent {
        /** The attempt's first latch of a Lamprey, shown until the latch ends. */
        @JsonProperty("first-latch")
        FIRST_LATCH
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
     * @param x M5 part E, a {@code field} wave: its area's centre, px from the left edge
     * @param size M5 part E, a {@code field} wave: its area {@code [w, h]}, px; its leading (bottom)
     *     edge enters at the top edge at {@code t}
     * @param spacing M5 part E, a {@code field} wave: the least distance between two units, px (1.5 ×
     *     the enemy's hit box width if not given)
     * @param current M5 part E, a {@code field} wave: the current its units drift along, ° from
     *     straight down, positive to the right (0 if not given)
     * @param clockwise M5 part F, a {@code circle} wave: it orbits clockwise on the screen (Level 12's
     *     eye); counter-clockwise if not given
     * @param lanes M5 part G (design/enemies/naval/skimmer, user decision G4 = a): a lane walker wave's
     *     lanes, one list of {@code [x, y]} points each (y below the top edge, at the wave's {@code t};
     *     outside the play field for an entry from a side or the rear; they scroll with the ground and
     *     stand still in a timed halt): unit i on lane i, wrapping; a {@code cross} has a lane per unit.
     *     M5 part G also: a {@code circle}'s {@code at} is its centre (Level 13's Needlers above the relay)
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
            Optional<String> tag,
            Optional<Double> x,
            Optional<Size> size,
            Optional<Double> spacing,
            Optional<Double> current,
            Optional<Boolean> clockwise,
            Optional<List<List<Point>>> lanes) {
        public Wave {
            lanes.ifPresent(l -> {
                Check.that(!l.isEmpty(), "lanes: at least one lane");
                l.forEach(lane -> Check.that(lane.size() >= 2, "lanes: every lane has two points or more"));
            });
            Check.that(lanes.isEmpty() || paths.isEmpty(), "a wave walks paths or lanes, not both");
            spacing.ifPresent(d -> Check.positive("spacing", d));
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
            boolean field = groups.orElseThrow().stream()
                    .anyMatch(group -> group.formation().equals(FIELD));
            Check.that(
                    !field || (single && x.isPresent() && size.isPresent()),
                    "a field wave is one group with its area's x and size");
            Check.that(
                    field || (x.isEmpty() && size.isEmpty() && spacing.isEmpty() && current.isEmpty()),
                    "only a field wave has an x, a size, a spacing or a current");
            Check.that(
                    clockwise.isEmpty()
                            || groups.orElseThrow().stream()
                                    .allMatch(group -> group.formation().equals("circle")),
                    "only a circle wave turns clockwise");
        }

        /** M5 part F: whether a circle wave orbits clockwise on the screen. */
        public boolean turnsClockwise() {
            return clockwise.orElse(false);
        }

        /** M5 part E: the formation of units scattered over an area that enters with the sea. */
        public static final String FIELD = "field";

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
        REAR,
        /** M5 part G: a {@code cross} wave's, from all four edges (its units' lanes say which). */
        ALL
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
     * @param pairs M5 part G: a {@code column}'s units enter in pairs side by side, the pairs this many s
     *     apart (Level 13's hard Stingers at t=210)
     */
    public record Change(
            Optional<Entry> from,
            Optional<Edge> edge,
            Optional<Integer> count,
            Optional<Integer> breakGroup,
            Optional<Double> warning,
            Optional<Double> hold,
            Optional<Integer> loops,
            Optional<Double> pairs) {
        public Change {
            hold.ifPresent(h -> Check.notNegative("hold", h));
            loops.ifPresent(n -> Check.positive("loops", n));
            pairs.ifPresent(p -> Check.positive("pairs", p));
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
     * secondary objective. M5 part E: a destructible may belong to a {@code group} of its own (not
     * the objectives'), whose last one destroyed drops a placed pickup ({@code dropped_by: {group,
     * unit: last}}: Level 11's floating containers), and a trigger's {@code easy} / {@code hard}
     * change may give its {@code hits} (Level 11's sunken pod: hard 6).
     *
     * @param layer and {@code size}: a destructible's or trigger's layer and hit box in px; an
     *     enemy's come from its stat block
     * @param at where each of them is placed
     * @param hardened only {@code anti-ground} weapons damage it (design/enemies, layer rules)
     * @param bonusDrop a second pickup it drops with its {@code drop} (Level 04's supply drop: a
     *     special charge, only with a special fitted)
     * @param easy another placement list on easy (an enemy nest's size), or a trigger's hits on easy
     * @param hard another placement list on hard, or a trigger's hits on hard
     * @param sprite a destructible's sprite set: {@code <sprite>_0..2} (intact, damaged, wrecked)
     *     and {@code <sprite>-break_<n>}; Level 01's cargo container if left out
     * @param current M5 part E: an enemy nest's current, ° from straight down (positive to the
     *     right), along which units whose {@code terrain} drifts drift (Level 11's rafts); 0 if not given
     * @param drift M5 part F: a trigger on {@code low-air} drifting at {@code [x, y]} px/s in screen space
     *     (y up, as a tow's: negative drifts down the screen) from where it enters at the top edge,
     *     instead of scrolling with the ground (Level 12's canister)
     * @param side M5 part G (design/campaign Level 13, Ground targets): an enemy nest entering at a side
     *     edge and drifting across (the mortar floe); its {@code at} placements are then {@code [t, y]},
     *     y px from the bottom edge (y up), where each unit crosses the edge
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
            Optional<Boolean> dark,
            Optional<Double> current,
            Optional<Point> drift,
            Optional<SideEntry> side) {
        public GroundTarget {
            Check.that(side.isEmpty() || enemy.isPresent(), "only an enemy nest enters from a side (side)");
            Check.that(side.isEmpty() || current.isEmpty(), "a nest from a side drifts across it, no current");
            Check.that(
                    drift.isEmpty()
                            || (hits.isPresent()
                                    && layer.filter("low-air"::equals).isPresent()),
                    "only a trigger on low-air drifts");
            drift.ifPresent(d -> Check.that(d.y() < 0, "drift: it drifts down the screen (a negative y)"));
            layer.ifPresent(Layers::of);
            Check.that(current.isEmpty() || enemy.isPresent(), "only an enemy nest has a current");
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
                Check.that(group.isEmpty() || hits.isEmpty(), "only enemies and destructibles have a group");
                for (Optional<Placements> change : List.of(easy, hard)) {
                    Check.that(
                            change.isEmpty()
                                    || (hits.isPresent() && change.get().at().isEmpty()),
                            "only enemies have easy/hard placements; a trigger's easy/hard gives its hits");
                }
            }
            for (Optional<Placements> change : List.of(easy, hard)) {
                change.ifPresent(c -> Check.that(
                        c.at().isPresent() != c.hits().isPresent(),
                        "an easy/hard change gives at (an enemy nest's placements) or hits (a trigger's)"));
                change.flatMap(Placements::hits).ifPresent(n -> Check.positive("hits", n));
            }
            Check.that(
                    enemy.isPresent()
                            || easy.flatMap(Placements::at).isEmpty()
                                    && hard.flatMap(Placements::at).isEmpty(),
                    "only enemies have easy/hard placements");
        }
    }

    /**
     * A difficulty's change to a ground target: an enemy nest's placements ({@code at}) or (M5 part
     * E) a trigger's {@code hits}.
     */
    public record Placements(Optional<List<Placement>> at, Optional<Integer> hits) {
        /** An enemy nest's placements. */
        public Placements(List<Placement> at) {
            this(Optional.of(at), Optional.empty());
        }
    }

    /** A ground object entering at the top edge at {@code t} seconds, {@code x} px from the left. */
    @JsonFormat(shape = JsonFormat.Shape.ARRAY)
    public record Placement(double t, double x) {
        public Placement {
            Check.notNegative("t", t);
        }
    }

    /**
     * M5 part G (design/campaign Level 13, Ground targets): an enemy nest's entry at a side edge: its units
     * appear just outside the {@code from} edge ({@code left} or {@code right}) and drift across toward the
     * other one at {@code drift} px/s on top of the scroll (the mortar floe), warned 3 s ahead as a side
     * wave's edge.
     */
    public record SideEntry(WindSide from, double drift) {
        public SideEntry {
            Check.positive("drift", drift);
        }
    }

    /**
     * M5 part G (design/allies, CDF supply drone; user decision G6 = a): a pickup released at the point
     * ({@code x}, {@code y}) at {@code t} (x px from the left edge, y px from the bottom edge, y up), then an
     * ordinary pickup; the drone that carries it there is presentation only: its {@code sprite} set (Level
     * 13's {@code supply-drone}), its arc from the defended ally's helipad ending at the point at {@code t}.
     */
    public record Drop(double t, double x, double y, Pickup pickup, Optional<String> sprite) {
        public Drop {
            Check.notNegative("t", t);
            Check.that(x >= 0 && x <= PlayField.WIDTH, "x: on the play field, was " + x);
            Check.that(y >= 0 && y < PlayField.HEIGHT, "y: inside the play field (below the top edge), was " + y);
        }
    }

    /**
     * M5 part H, a level's root bursts (design/campaign Level 14, Hazards; design/enemies, Boss attacks and
     * root bursts; decision H2 = a): each burst's paving cracks and its dust rises for {@code telegraph} s
     * from its {@code t} at its point, then its growths (ground units of its {@code growths} on the
     * difficulty) stand there {@code spacing} px apart, centred on the point, and scroll with the ground;
     * a growth takes no damage and fires nothing for {@code first_shot_delay} s after it appeared (hard's
     * own). The growths count in the objectives' group {@code group}.
     */
    public record RootBursts(
            double telegraph,
            double firstShotDelay,
            double spacing,
            String group,
            Optional<BurstChange> easy,
            Optional<BurstChange> hard,
            List<RootBurst> bursts) {
        public RootBursts {
            Check.notNegative("telegraph", telegraph);
            Check.notNegative("first_shot_delay", firstShotDelay);
            Check.positive("spacing", spacing);
            Check.notEmpty("bursts", bursts);
            bursts = List.copyOf(bursts);
            for (int i = 1; i < bursts.size(); i++) {
                Check.that(bursts.get(i).t() >= bursts.get(i - 1).t(), "bursts[" + i + "]: listed in time order");
            }
        }

        /** The first shot's delay on {@code difficulty}: hard's own if given. */
        public double firstShotDelay(Difficulty difficulty) {
            Optional<BurstChange> change =
                    switch (difficulty) {
                        case EASY -> easy;
                        case MEDIUM -> Optional.empty();
                        case HARD -> hard;
                    };
            return change.flatMap(BurstChange::firstShotDelay).orElse(firstShotDelay);
        }
    }

    /** M5 part H: a difficulty's change to the root bursts: the growths' first-shot delay (hard's 1.0 s). */
    public record BurstChange(Optional<Double> firstShotDelay) {
        public BurstChange {
            firstShotDelay.ifPresent(d -> Check.notNegative("first_shot_delay", d));
        }
    }

    /**
     * M5 part H, one root burst: at {@code t} (its telegraph's start) at ({@code x}, {@code y}), px from the
     * left edge and below the top edge then (100–400 px below it); its {@code growths} by enemy slug, left
     * to right, and an easy or hard list instead.
     */
    public record RootBurst(
            double t,
            double x,
            double y,
            List<String> growths,
            Optional<Growths> easy,
            Optional<Growths> hard,
            Optional<String> notes) {
        public RootBurst {
            Check.notNegative("t", t);
            Check.that(x >= 0 && x <= PlayField.WIDTH, "x: on the play field, was " + x);
            Check.notEmpty("growths", growths);
            growths = List.copyOf(growths);
        }

        /** Its growths on {@code difficulty}. */
        public List<String> growthsOn(Difficulty difficulty) {
            Optional<Growths> change =
                    switch (difficulty) {
                        case EASY -> easy;
                        case MEDIUM -> Optional.empty();
                        case HARD -> hard;
                    };
            return change.map(Growths::growths).orElse(growths);
        }
    }

    /** M5 part H: a root burst's growths on a difficulty. */
    public record Growths(List<String> growths) {
        public Growths {
            Check.notEmpty("growths", growths);
            growths = List.copyOf(growths);
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
     * @param at M5 part F: instead of a carrier, where it is placed: it enters at the top edge at
     *     {@code [t, x]} and drifts like any pickup (Level 12's armour patch in the eye)
     */
    public record PlacedPickup(
            Pickup pickup, Optional<Carrier> droppedBy, Optional<List<String>> skip, Optional<Placement> at) {
        public PlacedPickup {
            skip.ifPresent(names -> names.forEach(Difficulty::of));
            Check.that(droppedBy.isPresent() != at.isPresent(), "a pickup is dropped_by a carrier or placed at [t, x]");
        }

        /** A pickup a carrier drops. */
        public PlacedPickup(Pickup pickup, Carrier droppedBy, Optional<List<String>> skip) {
            this(pickup, Optional.of(droppedBy), skip, Optional.empty());
        }

        /** Its carrier; only for a pickup that is not placed ({@link #carried()}). */
        public Carrier carrier() {
            return droppedBy.orElseThrow(() -> new IllegalStateException("a placed pickup has no carrier"));
        }

        /** Whether a carrier drops it (not placed at a time and x). */
        public boolean carried() {
            return droppedBy.isPresent();
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
     * @param below M5 part G, an {@code ally-below} cue: the share of the defended ally's integrity it
     *     dropped below (Level 13's 0.5 and 0.25)
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
            Optional<Boolean> timeout,
            Optional<List<String>> parts,
            Optional<Double> below) {
        public RadioCue {
            Check.that(
                    below.isPresent() == (event.orElse(null) == CueEvent.ALLY_BELOW),
                    "an ally-below event names its share (below), other triggers do not");
            below.ifPresent(share -> Check.that(share > 0 && share < 1, "below: a share between 0 and 1"));
            Check.that(
                    parts.isPresent() == (event.orElse(null) == CueEvent.BOSS_PART_DESTROYED),
                    "a boss-part-destroyed event names its parts, other triggers do not");
            parts.ifPresent(list -> Check.notEmpty("parts", list));
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
        FIRST_LOOP_BACK,
        /**
         * M5 part E: each convoy unit's first hit (it starts again for each unit, as {@code
         * ally-lost}); the line may name it as {@code {ally}}, a naval convoy unit by its name.
         */
        @JsonProperty("ally-hit")
        ALLY_HIT,
        /** M5 part E: the attempt's first lane telegraph of the level's arena boss (once). */
        @JsonProperty("first-telegraph")
        FIRST_TELEGRAPH,
        /** M5 part E: the first of the level boss's {@code parts} the cue names shot off (once). */
        @JsonProperty("boss-part-destroyed")
        BOSS_PART_DESTROYED,
        /** M5 part F: the attempt's first latch of a Lamprey (once); a scripted escort line for the bark spacing. */
        @JsonProperty("first-latch")
        FIRST_LATCH,
        /**
         * M5 part G: the defend primary's ally dropped below the cue's {@code below} share of its integrity
         * for the first time in the attempt (Level 13's relay below 50 % and 25 %; once each).
         */
        @JsonProperty("ally-below")
        ALLY_BELOW
    }

    /**
     * The primary objective's kind ({@code reach-end}, {@code escort} with its {@code escort} block,
     * or {@code destroy-targets} with the ground-target groups it names as {@code targets}) and the
     * optional secondary objective.
     *
     * @param targets the groups (ground targets' {@code group}) a destroy-targets primary needs
     *     destroyed; it fails as soon as a unit of one leaves the screen alive (Level 05)
     * @param defend M5 part G: a {@code defend} primary's block (Level 13's Nansen Relay)
     */
    public record Objectives(
            String primary,
            Optional<Escort> escort,
            Optional<List<String>> targets,
            Optional<Secondary> secondary,
            Optional<Defend> defend) {
        /** M5 part G: the primary that defends an ally for a while (Level 13). */
        public static final String DEFEND = "defend";

        /** Without M5 part G's defend. */
        public Objectives(
                String primary,
                Optional<Escort> escort,
                Optional<List<String>> targets,
                Optional<Secondary> secondary) {
            this(primary, escort, targets, secondary, Optional.empty());
        }

        public Objectives {
            Check.that(
                    primary.equals("reach-end")
                            || primary.equals("escort")
                            || primary.equals("destroy-targets")
                            || primary.equals(DEFEND),
                    "primary: reach-end, escort, destroy-targets or defend, was '" + primary + "'");
            Check.that(
                    defend.isPresent() == primary.equals(DEFEND),
                    "a defend primary has its defend block, other primaries do not");
            Check.that(
                    secondary.flatMap(Secondary::integrity).isEmpty() || defend.isPresent(),
                    "an integrity secondary needs a defend primary");
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
     * M5 part G, the {@code defend} primary (design/campaign Level 13, Objective; design/allies, Nansen
     * Relay; user decisions G1, G2 and G6 = a): one {@code ally} (one that {@code follows: ground}) at
     * {@code at} {@code [x, y]} (px from the left edge, px from the bottom edge, y up) when the level clock
     * reaches {@code from}: it scrolls in with the ground before and stands there because the ground halts
     * (a timed halt). It must survive the {@code duration} s from then (the synchronisation); its loss fails
     * the mission; it pays no credits and does not regenerate. {@code targeted_by}: per enemy slug the
     * target-the-objective hook's mode ({@code nearest}, {@code alternate}, {@code in-arc}, {@code always} or
     * {@code none}; enemies not listed are not hooked); {@code easy} / {@code hard}: its integrity ({@code
     * hp}) and modes changed on that difficulty (merged over {@code targeted_by}).
     */
    public record Defend(
            String ally,
            Point at,
            double from,
            double duration,
            Map<String, HookMode> targetedBy,
            Optional<DefendChange> easy,
            Optional<DefendChange> hard) {
        public Defend {
            Check.notNegative("from", from);
            Check.positive("duration", duration);
            Check.that(
                    at.x() >= 0 && at.x() <= PlayField.WIDTH && at.y() >= 0 && at.y() <= PlayField.HEIGHT,
                    "at: on the play field, was [" + at.x() + ", " + at.y() + "]");
            targetedBy = Map.copyOf(targetedBy);
        }

        /** When the synchronisation completes, s on the level clock. */
        public double end() {
            return from + duration;
        }

        /** The change of {@code difficulty}, if any. */
        public Optional<DefendChange> change(Difficulty difficulty) {
            return switch (difficulty) {
                case EASY -> easy;
                case MEDIUM -> Optional.empty();
                case HARD -> hard;
            };
        }

        /** Per enemy slug its hook mode on {@code difficulty}: {@code targeted_by} with the difficulty's merged over it. */
        public Map<String, HookMode> modes(Difficulty difficulty) {
            Map<String, HookMode> modes = new java.util.TreeMap<>(targetedBy);
            change(difficulty).flatMap(DefendChange::targetedBy).ifPresent(modes::putAll);
            return modes;
        }
    }

    /** M5 part G: a difficulty's change to a defend primary: the ally's integrity and hook modes. */
    public record DefendChange(Optional<Double> hp, Optional<Map<String, HookMode>> targetedBy) {
        public DefendChange {
            hp.ifPresent(h -> Check.positive("hp", h));
            targetedBy = targetedBy.map(Map::copyOf);
        }
    }

    /**
     * M5 part G, the target-the-objective hook's modes (design/enemies, Target-the-objective hook; design/
     * campaign Level 13, What hurts the relay; user decision G1 = a).
     */
    public enum HookMode {
        @JsonProperty("none")
        NONE,
        @JsonProperty("nearest")
        NEAREST,
        @JsonProperty("alternate")
        ALTERNATE,
        @JsonProperty("in-arc")
        IN_ARC,
        @JsonProperty("always")
        ALWAYS
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
     * the briefing's bonus line and the README's credit table use it. M5 part E (user decision E8 =
     * a): {@code afloat: true}, every naval convoy unit that can be damaged afloat at the level boss's
     * death (Level 11's "Convoy afloat"; the tracker's {@code label}), failed at the first sinking.
     * M5 part F: {@code collect: sondes}, every weather sonde of the level flown through (Level 12's
     * "Sondes"; the tracker's {@code label}), met at the last, failed when one leaves the bottom edge
     * uncollected.
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
            Optional<String> name,
            Optional<Boolean> afloat,
            Optional<String> collect,
            Optional<Double> integrity) {
        /** M5 part F: what a {@code collect} objective counts, the level's weather sondes. */
        public static final String SONDES = "sondes";

        /** Without M5 part G's integrity. */
        public Secondary(
                Optional<Double> killRatio,
                Optional<List<String>> groups,
                Optional<String> escapes,
                Optional<List<String>> killAll,
                Optional<List<String>> parts,
                Optional<String> before,
                Optional<String> label,
                int credits,
                Optional<String> tag,
                Optional<String> name,
                Optional<Boolean> afloat,
                Optional<String> collect) {
            this(
                    killRatio,
                    groups,
                    escapes,
                    killAll,
                    parts,
                    before,
                    label,
                    credits,
                    tag,
                    name,
                    afloat,
                    collect,
                    Optional.empty());
        }

        /** Without M5 part F's collect. */
        public Secondary(
                Optional<Double> killRatio,
                Optional<List<String>> groups,
                Optional<String> escapes,
                Optional<List<String>> killAll,
                Optional<List<String>> parts,
                Optional<String> before,
                Optional<String> label,
                int credits,
                Optional<String> tag,
                Optional<String> name,
                Optional<Boolean> afloat) {
            this(
                    killRatio,
                    groups,
                    escapes,
                    killAll,
                    parts,
                    before,
                    label,
                    credits,
                    tag,
                    name,
                    afloat,
                    Optional.empty());
        }

        /** Without M5 part E's afloat. */
        public Secondary(
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
            this(killRatio, groups, escapes, killAll, parts, before, label, credits, tag, name, Optional.empty());
        }

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
                                    + (afloat.isPresent() ? 1 : 0)
                                    + (collect.isPresent() ? 1 : 0)
                                    + (integrity.isPresent() ? 1 : 0)
                            == 1,
                    "give kill_ratio, groups, escapes, kill_all, parts, afloat, collect or integrity");
            integrity.ifPresent(share -> Check.that(share > 0 && share < 1, "integrity: a share between 0 and 1"));
            collect.ifPresent(what -> Check.that(SONDES.equals(what), "collect: sondes, was '" + what + "'"));
            Check.that(afloat.orElse(true), "afloat: true, or leave it out");
            killRatio.ifPresent(r -> Check.share("kill_ratio", r));
            groups.ifPresent(g -> Check.notEmpty("groups", g));
            killAll.ifPresent(k -> Check.notEmpty("kill_all", k));
            parts.ifPresent(p -> Check.notEmpty("parts", p));
            Check.that(parts.isPresent() == before.isPresent(), "a parts objective names the phase they die before");
            // M5 part H: a groups objective may name its tracker (Level 14's root-burst growths, ROOTS n/8).
            Check.that(
                    killAll.isPresent()
                                    || parts.isPresent()
                                    || afloat.isPresent()
                                    || collect.isPresent()
                                    || integrity.isPresent()
                            ? label.isPresent()
                            : label.isEmpty() || groups.isPresent(),
                    "a kill_all, parts, afloat, collect or integrity objective has its tracker label, a groups one"
                            + " may have one, the others none");
            Check.notNegative("credits", credits);
        }
    }

    /**
     * M5 part E: a naval convoy outside the objectives (design/allies, convoy cargo ship and escort
     * frigate; Level 11): its {@code units} in order; {@code glide} s of the moves at the arena's
     * halt and after the boss (on the real steps); {@code lane_y} px below the top edge, a unit's
     * centre in its lane; {@code hold_clear} the px the sea scrolls after the halt before the units
     * glide back to their stations (Level 11: Platform Tiamat passes them; 0 or absent: at once),
     * each holding in its lane, away or at its {@code hold} point meanwhile; {@code easy} / {@code
     * hard} the HP of its units that can be damaged.
     */
    public record Convoy(
            List<ConvoyUnit> units,
            double glide,
            Optional<Double> laneY,
            Optional<Double> holdClear,
            Optional<AllyChange> easy,
            Optional<AllyChange> hard) {
        public Convoy {
            Check.notEmpty("units", units);
            units = List.copyOf(units);
            Check.positive("glide", glide);
            holdClear.ifPresent(px -> Check.that(px >= 0, "hold_clear: 0 px or more, was " + px));
            laneY.ifPresent(y -> Check.that(y > 0 && y < PlayField.HEIGHT, "lane_y: on the play field, was " + y));
            Check.that(
                    units.stream().noneMatch(unit -> unit.lane().isPresent()) || laneY.isPresent(),
                    "a convoy whose units take lanes gives their lane_y");
        }
    }

    /**
     * M5 part E: a naval convoy's unit: an {@code ally} slug (one that {@code follows: stations}), its
     * {@code name} ({@code {ally}} in a radio line: "Halvorsen"), its {@code station} {@code [x, y]}
     * (px from the play field's left edge, px below the top edge, held at the scroll speed) and either
     * the arena {@code lane} it glides to at the halt (1-based, of the boss's lanes) or {@code leaves:
     * true} (it drops back off the bottom edge at the halt and returns after the boss); its {@code
     * hold} point {@code [x, y]} (as the station's; below the bottom edge for off it) it glides to when
     * the halt ends, holding where it is without one, until its convoy's {@code hold_clear}.
     */
    public record ConvoyUnit(
            String ally,
            String name,
            Point station,
            Optional<Integer> lane,
            Optional<Boolean> leaves,
            Optional<Point> hold) {
        public ConvoyUnit {
            Check.that(!name.isBlank(), "name: the unit's name");
            Check.that(lane.isPresent() != leaves.orElse(false), "give the unit's lane or leaves: true, one of them");
            lane.ifPresent(l -> Check.positive("lane", l));
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
     * @param sectionDb M5 part F: the theme's level by section (1-based section: dB, at most 0), from that
     *     section until the next entry, over a 1 s ramp (Level 12's eye at -6); full where none is given
     * @param jingle M5 part G (user decision G8 = a): the {@code end_jingle} plays inside the level at
     *     {@code jingle.t} instead of at the debrief (Level 13's "Grid sync complete"): the theme fades
     *     out over {@code fade} s (1 if not given), the jingle plays once, then the ambience alone; the
     *     debrief then starts without it
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
            Optional<List<AmbienceChange>> ambienceChanges,
            Optional<Map<Integer, Double>> sectionDb,
            Optional<Jingle> jingle) {
        /** M5 part G: the jingle inside the level at {@code t}, the theme fading out over {@code fade} s first. */
        public record Jingle(double t, Optional<Double> fade) {
            public Jingle {
                Check.notNegative("t", t);
                fade.ifPresent(f -> Check.positive("fade", f));
            }

            /** The theme's fade before it, s: its {@code fade}, 1 if not given. */
            public double fadeSeconds() {
                return fade.orElse(1.0);
            }
        }

        /** Without M5 part G's mid-level jingle. */
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
                Optional<List<FullOn>> fullOn,
                Optional<Duck> duck,
                Optional<List<AmbienceChange>> ambienceChanges,
                Optional<Map<Integer, Double>> sectionDb) {
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
                    duck,
                    ambienceChanges,
                    sectionDb,
                    Optional.empty());
        }

        /** M5 part G: whether the debrief plays the end jingle (not when it played inside the level). */
        public boolean debriefJingle() {
            return jingle.isEmpty();
        }

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

        /** Without M5 part F's section levels. */
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
                Optional<List<FullOn>> fullOn,
                Optional<Duck> duck,
                Optional<List<AmbienceChange>> ambienceChanges) {
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
                    duck,
                    ambienceChanges,
                    Optional.empty());
        }

        /**
         * M5 part F: the theme's level in section {@code section} (1-based), dB: the last {@code
         * section_db} entry at or before it; 0 (full) without one.
         */
        public double sectionLevel(int section) {
            double db = 0;
            int best = 0;
            for (Map.Entry<Integer, Double> entry : sectionDb.orElse(Map.of()).entrySet()) {
                if (entry.getKey() <= section && entry.getKey() > best) {
                    best = entry.getKey();
                    db = entry.getValue();
                }
            }
            return db;
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
            sectionDb = sectionDb.map(Map::copyOf);
            sectionDb.ifPresent(levels -> {
                Check.that(!levels.isEmpty(), "section_db: at least one section");
                levels.forEach((section, db) -> {
                    Check.that(section >= 1, "section_db: sections count from 1, was " + section);
                    Check.that(db <= 0, "section_db: must not be above full level, was " + db);
                });
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

    /**
     * M5 part F, a level's {@code weather} (design/campaign Level 12, Weather): the wind's {@code
     * gusts} and the {@code drift} through a section (added to the positions of the ship, the escort,
     * the units with a latch and the Whirl Seeds; never to shots, mines, spores, pickups, sondes or
     * drifting triggers), the {@code lightning} flashes (the simulation's {@code LIGHTNING} event, the
     * banks lit for {@code reveal} s), {@code occlusion} (the banks on {@code low-air} hide that layer's
     * bodies: draw order only) and {@code rain} ({@code [t, density]} keys, 0–1, linear between them:
     * presentation only).
     */
    public record Weather(
            Optional<Gusts> gusts,
            Optional<WallDrift> drift,
            Optional<Lightning> lightning,
            Optional<Boolean> occlusion,
            Optional<List<RainKey>> rain) {
        public Weather {
            rain = rain.map(List::copyOf);
            rain.ifPresent(keys -> {
                Check.notEmpty("rain", keys);
                for (int i = 1; i < keys.size(); i++) {
                    Check.that(keys.get(i).t() > keys.get(i - 1).t(), "rain: keys in time order");
                }
            });
        }

        /** Whether the banks on {@code low-air} hide that layer's bodies ({@code occlusion: true}). */
        public boolean occludes() {
            return occlusion.orElse(false);
        }

        /** The rain's density at {@code t}, 0–1: linear between the keys, the first and last held; 0 without rain. */
        public double rainAt(double t) {
            List<RainKey> keys = rain.orElse(List.of());
            if (keys.isEmpty()) {
                return 0;
            }
            if (t <= keys.getFirst().t()) {
                return keys.getFirst().density();
            }
            for (int i = 1; i < keys.size(); i++) {
                RainKey a = keys.get(i - 1);
                RainKey b = keys.get(i);
                if (t <= b.t()) {
                    return a.density() + (b.density() - a.density()) * (t - a.t()) / (b.t() - a.t());
                }
            }
            return keys.getLast().density();
        }
    }

    /**
     * M5 part F: the gusts {@code at} their times (each pushing from its side from its {@code t},
     * telegraphed {@code telegraph} s before), each lasting {@code seconds} including the {@code ramp}
     * s up and down, pushing {@code push} px/s; {@code easy} / {@code hard} change the push and the
     * telegraph, and hard's {@code extra} gusts come on top.
     */
    public record Gusts(
            List<GustAt> at,
            double seconds,
            double ramp,
            double push,
            double telegraph,
            Optional<GustChange> easy,
            Optional<GustChange> hard) {
        public Gusts {
            at = List.copyOf(at);
            Check.notEmpty("at", at);
            Check.notNegative("ramp", ramp);
            Check.that(seconds > 2 * ramp, "seconds: longer than the ramps up and down");
            Check.notNegative("push", push);
            Check.notNegative("telegraph", telegraph);
            for (int i = 1; i < at.size(); i++) {
                Check.that(at.get(i).t() >= at.get(i - 1).t(), "at: gusts in time order");
            }
        }
    }

    /** M5 part F: a gust starting to push at {@code t}, blowing {@code from} the left or the right edge. */
    public record GustAt(double t, WindSide from) {
        public GustAt {
            Check.notNegative("t", t);
        }
    }

    /** M5 part F: a difficulty's gusts: another push or telegraph, and (hard) {@code extra} gusts on top. */
    public record GustChange(Optional<Double> push, Optional<Double> telegraph, Optional<List<GustAt>> extra) {
        public GustChange {
            push.ifPresent(p -> Check.notNegative("push", p));
            telegraph.ifPresent(t -> Check.notNegative("telegraph", t));
        }
    }

    /**
     * M5 part F: a steady drift of {@code push} px/s {@code from} a side through section {@code section}
     * (1-based; Level 12's storm wall), with the gusts' ramps; {@code easy} / {@code hard} another push, 0
     * for none.
     */
    public record WallDrift(
            int section, double push, WindSide from, Optional<DriftChange> easy, Optional<DriftChange> hard) {
        public WallDrift {
            Check.positive("section", section);
            Check.notNegative("push", push);
        }
    }

    /** M5 part F: a difficulty's drift push, px/s; 0 for none. */
    public record DriftChange(double push) {
        public DriftChange {
            Check.notNegative("push", push);
        }
    }

    /** M5 part F: the lightning flashes {@code at} their times (in order), each lighting the banks for {@code reveal} s. */
    public record Lightning(List<Double> at, double reveal) {
        public Lightning {
            at = List.copyOf(at);
            Check.notEmpty("at", at);
            Check.positive("reveal", reveal);
            for (int i = 1; i < at.size(); i++) {
                Check.that(at.get(i) > at.get(i - 1), "at: flashes in time order");
            }
        }
    }

    /** M5 part F: the rain's {@code density} (0–1) at {@code t}. */
    @JsonFormat(shape = JsonFormat.Shape.ARRAY)
    public record RainKey(double t, double density) {
        public RainKey {
            Check.notNegative("t", t);
            Check.share("density", density);
        }
    }

    /** M5 part F: the side a wind blows from. */
    public enum WindSide {
        LEFT,
        RIGHT
    }

    /**
     * M5 part F, the weather sondes (design/campaign Level 12, user decision F7 = b): each enters at the
     * top edge at its {@code [t, x]} and drifts straight down at {@code speed} px/s on the play plane; the
     * ship collects one when its centre comes within {@code radius} px of the sonde's. Not shot, not
     * pushed by the wind, not pulled by the magnet, not collected by the escort.
     */
    public record SondesData(double speed, double radius, List<Placement> at) {
        public SondesData {
            at = List.copyOf(at);
            Check.positive("speed", speed);
            Check.positive("radius", radius);
            Check.notEmpty("at", at);
            for (int i = 1; i < at.size(); i++) {
                Check.that(at.get(i).t() >= at.get(i - 1).t(), "at: sondes in time order");
            }
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
