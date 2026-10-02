package vanguard.content;

import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Everything in the design tree's data files, loaded and validated by {@link ContentLoader}.
 * Weapons and enemies are keyed by their slug, levels by their path under design/campaign
 * ({@code act-1-first-contact/level-01-break-at-dawn}), acts by their directory
 * ({@code act-1-first-contact}).
 */
public record Content(
        PlayerData player,
        ShipData ship,
        ShieldData shields,
        ArmourData armour,
        GeneratorData generators,
        SystemsData systems,
        SpecialsData specials,
        WeaponRulesData weaponRules,
        Map<String, WeaponData> weapons,
        EnemyBasisData enemyBasis,
        Map<String, EnemyData> enemies,
        Map<String, ActData> acts,
        Map<String, LevelData> levels,
        EconomyData economy,
        DifficultyData difficulty,
        ScoringData scoring,
        RetryData retry) {
    public Content {
        weapons = Map.copyOf(weapons);
        enemies = Map.copyOf(enemies);
        acts = Map.copyOf(acts);
        levels = Map.copyOf(levels);
    }

    private static final Pattern LEVEL_KEY = Pattern.compile("(act-\\d+-[a-z0-9-]+)/level-(\\d{2})-[a-z0-9-]+");

    /** The key of level {@code number} (1–50), if its data file exists yet. */
    public Optional<String> levelKey(int number) {
        return levels.keySet().stream()
                .filter(key -> levelNumber(key) == number)
                .findFirst();
    }

    /** The directory and data of the act that holds level {@code number}, if its data file exists yet. */
    public Optional<Map.Entry<String, ActData>> actOf(int number) {
        return acts.entrySet().stream()
                .filter(act -> act.getValue().levels().contains(number))
                .findFirst();
    }

    /** The level number of a level key: 1 for {@code act-1-first-contact/level-01-break-at-dawn}. */
    public static int levelNumber(String key) {
        return Integer.parseInt(levelKeyMatch(key).group(2));
    }

    /** The level's name from its key: {@code level-01-break-at-dawn} is "break at dawn". */
    public static String levelName(String key) {
        return key.substring(key.lastIndexOf('/') + "/level-01-".length()).replace('-', ' ');
    }

    /** The act directory of a level key: {@code act-1-first-contact}. */
    public static String actDirectory(String key) {
        return levelKeyMatch(key).group(1);
    }

    private static Matcher levelKeyMatch(String key) {
        Matcher matcher = LEVEL_KEY.matcher(key);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("not a level key: " + key);
        }
        return matcher;
    }

    /** The weapon with this slug; the slug must exist. */
    public WeaponData weapon(String slug) {
        return lookup(weapons, slug, "weapon");
    }

    /** The level with this key ({@code act-1-first-contact/level-01-break-at-dawn}); it must exist. */
    public LevelData level(String key) {
        return lookup(levels, key, "level");
    }

    /** The enemy with this slug; the slug must exist. */
    public EnemyData enemy(String slug) {
        return lookup(enemies, slug, "enemy");
    }

    private static <T> T lookup(Map<String, T> map, String slug, String kind) {
        T value = map.get(slug);
        if (value == null) {
            throw new IllegalArgumentException("no " + kind + " '" + slug + "'");
        }
        return value;
    }
}
