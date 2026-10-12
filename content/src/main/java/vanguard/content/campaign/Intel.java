package vanguard.content.campaign;

import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import vanguard.content.Content;
import vanguard.content.LevelData;
import vanguard.content.LevelData.Entry;
import vanguard.content.Tier;

/**
 * The hangar intel on the next level (design/ui/hangar, Intel; design/player/systems, Sensor levels
 * and hangar intel): its threat profile with the attack directions, enemy types, wave times and
 * secret count derived from the script, and how much of it the sensor level shows.
 *
 * @param number the level number
 * @param sensor the sensor level, 0 (no sensor suite) to 3
 * @param directions each entry's share of the enemies (medium), whole per cent, in the order front, sides, rear
 *     (M5 part G: a {@code cross} wave's units split evenly over the four edges)
 * @param enemies the enemy types by name, in order of appearance
 * @param waves the waves' start times, seconds
 * @param contacts the level's set pieces (its {@code set_pieces}, outside the waves), which the intel
 *     shows from sensor L2 as unknown contacts of their size tier with their silhouette, not by name
 * @param seconds the level's length
 * @param marks M5 part G: the spans the wave strip marks (Level 13's three defend phases, the halt's
 *     sections, and its whiteout, the heavy peak in them); empty for a level without a defend
 */
public record Intel(
        int number,
        int sensor,
        LevelData.ThreatProfile profile,
        Map<Entry, Integer> directions,
        List<String> enemies,
        List<Double> waves,
        List<Contact> contacts,
        int secrets,
        double seconds,
        List<Mark> marks) {
    /** An intel item and the sensor level that shows it. */
    public enum Field {
        SETTING(0),
        LAYERS(0),
        MAIN_DIRECTION(0),
        DIRECTIONS(1),
        DENSITY(1),
        HAZARDS(1),
        /** The primary objective when it is more than reaching the end ("ESCORT 5 CRAWLERS"). */
        OBJECTIVE(1),
        ENEMIES(2),
        BOSS(2),
        CONTACTS(2),
        SPECIALS(2),
        TRAITS(3),
        WAVES(3),
        SECRETS(3);

        private final int sensor;

        Field(int sensor) {
            this.sensor = sensor;
        }

        /** The sensor level from which the field is shown. */
        public int sensor() {
            return sensor;
        }
    }

    /**
     * A set piece as the sensors see it: an unknown contact of its size tier.
     *
     * @param enemy the unit's slug (its stat block), which names its silhouette
     */
    public record Contact(String enemy, Tier tier) {
        /** What the intel calls it: {@code unknown huge contact}. */
        public String label() {
            return "unknown " + tier.name().toLowerCase(Locale.ROOT) + " contact";
        }
    }

    /** M5 part G: a span the wave strip marks: a defend phase (a timed halt's section) or its whiteout (a heavy peak). */
    public record Mark(double from, double to, boolean hazard) {}

    /** Without M5 part G's marks. */
    public Intel(
            int number,
            int sensor,
            LevelData.ThreatProfile profile,
            Map<Entry, Integer> directions,
            List<String> enemies,
            List<Double> waves,
            List<Contact> contacts,
            int secrets,
            double seconds) {
        this(number, sensor, profile, directions, enemies, waves, contacts, secrets, seconds, List.of());
    }

    public Intel {
        marks = List.copyOf(marks);
        directions = Map.copyOf(directions);
        enemies = List.copyOf(enemies);
        waves = List.copyOf(waves);
        contacts = List.copyOf(contacts);
    }

    /** The intel on level {@code levelKey} at a sensor level. */
    public static Intel of(Content content, String levelKey, int sensor) {
        LevelData level = content.level(levelKey);
        Map<Entry, Double> counts = new EnumMap<>(Entry.class);
        Set<String> enemies = new LinkedHashSet<>();
        for (LevelData.Wave wave : level.waves()) {
            for (LevelData.Group group : wave.groupList()) {
                if (wave.from() == Entry.ALL) {
                    // M5 part G: a cross from all four edges, split evenly over them (two are the sides).
                    counts.merge(Entry.FRONT, group.count() / 4.0, Double::sum);
                    counts.merge(Entry.SIDES, group.count() / 2.0, Double::sum);
                    counts.merge(Entry.REAR, group.count() / 4.0, Double::sum);
                } else {
                    counts.merge(wave.from(), (double) group.count(), Double::sum);
                }
                enemies.add(content.enemy(group.enemy()).name());
            }
        }
        double total = counts.values().stream().mapToDouble(Double::doubleValue).sum();
        Map<Entry, Integer> shares = new EnumMap<>(Entry.class);
        counts.forEach((entry, count) -> shares.put(entry, (int) Math.rint(100.0 * count / total)));
        return new Intel(
                Content.levelNumber(levelKey),
                sensor,
                level.threatProfile(),
                shares,
                List.copyOf(enemies),
                level.waves().stream().map(LevelData.Wave::t).toList(),
                level.setPieces().orElse(List.of()).stream()
                        .map(piece -> new Contact(
                                piece.enemy(), content.enemy(piece.enemy()).tier()))
                        .toList(),
                level.secrets().size(),
                level.seconds(),
                marks(level));
    }

    /**
     * M5 part G: a defend level's marks: each timed halt's section (the defend's phases) and a heavy peak
     * inside one (the whiteout); M5 part H: a level with root bursts marks its root field as a hazard; none
     * for another level.
     */
    static List<Mark> marks(LevelData level) {
        if (!level.bursts().isEmpty()) {
            // M5 part H: the root field, the sections the root bursts lie in, as a hazard (Level 14, 60–95 s).
            int first = level.sectionAt(level.bursts().getFirst().t());
            int last = level.sectionAt(level.bursts().getLast().t());
            return List.of(new Mark(
                    level.sectionStart(first - 1),
                    level.sections().get(last - 1).end(),
                    true));
        }
        if (level.objectives().defend().isEmpty()) {
            return List.of();
        }
        List<Mark> marks = new java.util.ArrayList<>();
        for (int i = 0; i < level.sections().size(); i++) {
            LevelData.Section section = level.sections().get(i);
            if (!section.timedHalt()) {
                continue;
            }
            marks.add(new Mark(level.sectionStart(i), section.end(), false));
            section.peak()
                    .filter(peak -> peak.atmosphere() == LevelData.Atmosphere.HEAVY)
                    .ifPresent(peak -> marks.add(new Mark(peak.from(), peak.to(), true)));
        }
        return marks;
    }

    /** Whether the sensor level shows a field. */
    public boolean shows(Field field) {
        return sensor >= field.sensor();
    }

    /** The direction most of the enemies come from. */
    public Entry mainDirection() {
        return directions.entrySet().stream()
                .max(Map.Entry.<Entry, Integer>comparingByValue()
                        .thenComparing(Map.Entry.comparingByKey((a, b) -> b.compareTo(a))))
                .orElseThrow()
                .getKey();
    }

    /**
     * Varga's line for the sensor level: what her sensors reveal at that level, from the no-sensor
     * teaser to the L3 tip (design/ui/hangar, Intel).
     */
    public String varga() {
        return profile.vargaLine(sensor);
    }

    /** The recommended traits the shop marks: shown from sensor L3 on, otherwise none. */
    public List<String> markedTraits() {
        return shows(Field.TRAITS) ? profile.traits() : List.of();
    }

    /**
     * M5 part C (user decision D7 = a): the traits the primary objective cannot be met without (Level
     * 09's {@code anti-ground}), known at every sensor level (the briefing says them); none for most
     * levels.
     */
    public List<String> requiredTraits() {
        return profile.requiredTraits();
    }
}
