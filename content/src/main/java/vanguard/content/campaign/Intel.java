package vanguard.content.campaign;

import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
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
 * @param enemies the enemy types by name, in order of appearance
 * @param waves the waves' start times, seconds
 * @param contacts the level's set pieces (its {@code set_pieces}, outside the waves), which the intel
 *     shows from sensor L2 as unknown contacts of their size tier with their silhouette, not by name
 * @param seconds the level's length
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
        double seconds) {
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

    public Intel {
        directions = Map.copyOf(directions);
        enemies = List.copyOf(enemies);
        waves = List.copyOf(waves);
        contacts = List.copyOf(contacts);
    }

    /** The intel on level {@code levelKey} at a sensor level. */
    public static Intel of(Content content, String levelKey, int sensor) {
        LevelData level = content.level(levelKey);
        Map<Entry, Integer> counts = new EnumMap<>(Entry.class);
        Set<String> enemies = new LinkedHashSet<>();
        for (LevelData.Wave wave : level.waves()) {
            for (LevelData.Group group : wave.groupList()) {
                counts.merge(wave.from(), group.count(), Integer::sum);
                enemies.add(content.enemy(group.enemy()).name());
            }
        }
        int total = counts.values().stream().mapToInt(Integer::intValue).sum();
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
                level.seconds());
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

    /** Varga's line for the sensor level. */
    public Optional<String> varga() {
        return profile.vargaLine(sensor);
    }

    /** The recommended traits the shop marks: shown from sensor L3 on, otherwise none. */
    public List<String> markedTraits() {
        return shows(Field.TRAITS) ? profile.traits() : List.of();
    }
}
