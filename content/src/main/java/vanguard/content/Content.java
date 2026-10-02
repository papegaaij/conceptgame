package vanguard.content;

import java.util.Map;

/**
 * Everything in the design tree's data files, loaded and validated by {@link ContentLoader}.
 * Weapons and enemies are keyed by their slug, levels by their path under design/campaign
 * ({@code act-1-first-contact/level-01-break-at-dawn}).
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
        Map<String, LevelData> levels,
        EconomyData economy,
        DifficultyData difficulty,
        ScoringData scoring) {
    public Content {
        weapons = Map.copyOf(weapons);
        enemies = Map.copyOf(enemies);
        levels = Map.copyOf(levels);
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
