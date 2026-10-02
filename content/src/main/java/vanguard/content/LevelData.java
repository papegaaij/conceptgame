package vanguard.content;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import vanguard.sim.PlayField;

/**
 * design/campaign/&lt;act&gt;/&lt;level&gt;/data.yaml: a level's script. Times are seconds from
 * the level start.
 *
 * @param scrollSpeed px/s unless a section sets its own
 * @param launchSeconds the non-playable launch before control starts
 * @param controlPrompts the control prompts shown in the first section
 * @param sections back to back from t = 0
 * @param pickups pickups placed by the script (normal drops come from the enemies)
 * @param radio the radio chatter
 * @param backdrop the parallax layers behind and above the play plane
 */
public record LevelData(
        double scrollSpeed,
        double launchSeconds,
        List<String> controlPrompts,
        List<Section> sections,
        List<Wave> waves,
        List<GroundTarget> groundTargets,
        List<Secret> secrets,
        List<PlacedPickup> pickups,
        List<RadioCue> radio,
        Objectives objectives,
        Music music,
        Difficulties difficulty,
        BackdropData backdrop) {
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
     * After the level end the scroll runs on this long under the last radio line before the debrief
     * (the outro), so the backdrop has to hold until then.
     */
    public static final double OUTRO_SECONDS = 5;

    /** The level's length in seconds: the end of the last section. */
    public double seconds() {
        return sections.getLast().end();
    }

    /** When the outro ends and the debrief takes over: the last moment the backdrop is on screen. */
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
    public record Section(String name, double end, Optional<Double> speed, Atmosphere atmosphere, List<String> tiles) {
        public Section {
            Check.positive("end", end);
            speed.ifPresent(s -> Check.positive("speed", s));
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
     * A ground target: a destructible ({@code hp}, {@code bounty}, {@code drop}) or a trigger hit
     * {@code hits} times that {@code reveals} a secret.
     *
     * @param size its hit box in px
     * @param at where each of them is placed
     */
    public record GroundTarget(
            String target,
            int section,
            Optional<Integer> count,
            String layer,
            Size size,
            List<Placement> at,
            Optional<Double> hp,
            Optional<Integer> bounty,
            Optional<Pickup> drop,
            Optional<Integer> hits,
            Optional<String> reveals) {
        public GroundTarget {
            Layers.of(layer);
            Check.that(hp.isPresent() != hits.isPresent(), "give hp (destructible) or hits (trigger)");
            Check.that(reveals.isPresent() == hits.isPresent(), "a trigger (hits) reveals a secret");
            Check.that(at.size() == count.orElse(1), "at: one placement per target (count, or 1 for a trigger)");
        }
    }

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

    /** The {@code unit} ({@code first} or {@code last}) of the wave starting at {@code wave} seconds. */
    public record Carrier(double wave, CarrierUnit unit) {}

    public enum CarrierUnit {
        FIRST,
        LAST
    }

    /** A spoken line; {@code distorted} for transmissions such as the Choir's. */
    public record RadioLine(String speaker, String line, Optional<Boolean> distorted) {}

    /**
     * A radio chatter cue, triggered at {@code t} seconds or by an {@code event}.
     *
     * @param enemy the enemy of a {@code first-kill} event
     */
    public record RadioCue(
            Optional<Double> t,
            Optional<CueEvent> event,
            Optional<String> enemy,
            String speaker,
            String line,
            Optional<Boolean> distorted) {
        public RadioCue {
            Check.that(t.isPresent() != event.isPresent(), "give the trigger as t or as event");
            Check.that(
                    enemy.isPresent() == (event.orElse(null) == CueEvent.FIRST_KILL),
                    "a first-kill event names its enemy, other triggers do not");
        }
    }

    public enum CueEvent {
        @JsonProperty("first-kill")
        FIRST_KILL,
        @JsonProperty("secondary-objective")
        SECONDARY_OBJECTIVE,
        @JsonProperty("level-end")
        LEVEL_END
    }

    /** The primary objective's kind and the optional secondary objective. */
    public record Objectives(String primary, Optional<Secondary> secondary) {}

    /** Destroy at least {@code killRatio} of all enemies for {@code credits}. */
    public record Secondary(double killRatio, int credits) {
        public Secondary {
            Check.share("kill_ratio", killRatio);
            Check.notNegative("credits", credits);
        }
    }

    /**
     * The music cues.
     *
     * @param track the track number of design/audio/music
     * @param startSection the section the music starts in (ambience only before)
     * @param fullSection the section from which all stems play (the base stem before)
     */
    public record Music(int track, int startSection, int fullSection, String ambience, String endJingle) {
        public Music {
            Check.that(startSection <= fullSection, "full_section must not come before start_section");
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
