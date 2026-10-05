package vanguard.pipeline;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import vanguard.content.Content;
import vanguard.content.EnemyData;
import vanguard.content.LevelData;

/**
 * Which atlas a sprite goes in (design/art-direction/production, Budgets): a sprite only one level
 * uses goes in that level's unit atlas ({@code level-NN}), everything else in the shared pages
 * ({@code sprites}), which the game keeps loaded. The levels' use is derived from their data: the
 * enemies of their waves, ground targets, set pieces and boss (and the enemies those spawn), the
 * ground targets' looks, the escorted ally and the level's features the game draws with sprites of
 * its own. A sprite belongs to a root when its name is the root or starts with the root and a
 * hyphen ({@code leviathan} owns {@code leviathan-fin-left}). The shared roots are the weapons, the
 * specials and the game's own effects ({@link #GAME_WIDE}); a sprite no root claims fails the build.
 */
final class SpriteUse {
    /** The shared atlas, always loaded. */
    static final String SHARED = "sprites";

    /**
     * The sprites the game draws in any level whatever its data: the ship and its engine flame, the
     * pickups, explosions and weapon effects, the wing-mount pods, the enemies' bullets and the loot
     * targets' beacon and glint.
     */
    static final List<String> GAME_WIDE = List.of(
            "ship",
            "engine-flame",
            "pickup",
            "explosion",
            "ballistic",
            "explosive",
            "launcher",
            "pulse",
            "pod",
            "orb",
            "needle",
            "beacon",
            "glint");

    /** The sprites an enemy's attack pattern draws besides the enemy (the game's names). */
    private static final Map<String, String> PATTERN_ROOTS = Map.of("mortar", "mortar", "mine", "spore-mine");

    /** The look of a destructible ground target that names none (Level 01's cargo container). */
    private static final String DEFAULT_LOOK = "cargo-container";

    private SpriteUse() {}

    /** The atlas of every sprite name: {@link #SHARED} or {@code level-NN}. */
    static Map<String, String> atlases(Content content, Collection<String> sprites) {
        Set<String> shared = new TreeSet<>(GAME_WIDE);
        content.weapons().keySet().forEach(shared::add);
        content.specials().specials().forEach(special -> shared.add(slug(special.name())));
        Map<String, Set<String>> levels = new TreeMap<>();
        content.levels().forEach((key, level) -> levels.put(atlasOf(key), roots(content, level)));
        Map<String, String> atlases = new TreeMap<>();
        List<String> orphans = new ArrayList<>();
        for (String sprite : sprites) {
            Set<String> users = new TreeSet<>();
            levels.forEach((level, roots) -> {
                if (claims(roots, sprite)) {
                    users.add(level);
                }
            });
            if (claims(shared, sprite) || users.size() > 1) {
                atlases.put(sprite, SHARED);
            } else if (users.size() == 1) {
                atlases.put(sprite, users.iterator().next());
            } else {
                orphans.add(sprite);
            }
        }
        if (!orphans.isEmpty()) {
            throw new IllegalStateException("sprites no level, weapon, special or game effect uses (see "
                    + SpriteUse.class.getSimpleName() + "): " + orphans);
        }
        return atlases;
    }

    /** The unit atlas of a level key: {@code level-NN}. */
    static String atlasOf(String levelKey) {
        return String.format(Locale.ROOT, "level-%02d", Content.levelNumber(levelKey));
    }

    /** The sprite roots a level uses. */
    static Set<String> roots(Content content, LevelData level) {
        Set<String> enemies = new TreeSet<>();
        level.waves().forEach(wave -> wave.groupList().forEach(group -> enemies.add(group.enemy())));
        level.setPieces().ifPresent(pieces -> pieces.forEach(piece -> enemies.add(piece.enemy())));
        level.boss().ifPresent(boss -> enemies.add(boss.enemy()));
        Set<String> roots = new TreeSet<>();
        for (LevelData.GroundTarget target : level.groundTargets()) {
            target.enemy().ifPresent(enemies::add);
            if (target.enemy().isEmpty() && target.hits().isEmpty()) {
                roots.add(target.sprite().orElse(DEFAULT_LOOK));
            } else if (target.enemy().isEmpty()) {
                // A trigger with a look of its own (Level 06's survey cache and terminal); others
                // are drawn as the beacon or the level's trigger light.
                target.sprite().ifPresent(roots::add);
            }
        }
        List<String> open = new ArrayList<>(enemies);
        while (!open.isEmpty()) {
            EnemyData enemy = content.enemy(open.removeLast());
            for (EnemyData.Attack attack : enemy.attacks()) {
                attack.spawn().map(EnemyData.Spawn::enemy).filter(enemies::add).ifPresent(open::add);
                String root = PATTERN_ROOTS.get(attack.pattern());
                if (root != null) {
                    roots.add(root);
                }
            }
        }
        roots.addAll(enemies);
        level.objectives().escort().ifPresent(escort -> roots.add(escort.ally()));
        level.cranes().ifPresent(cranes -> roots.add("crane-four"));
        level.debris().ifPresent(debris -> debris.chunks().keySet().forEach(chunk -> roots.add("debris-" + chunk)));
        level.sleds().ifPresent(sleds -> {
            roots.add("sled");
            roots.add("ore-canister");
        });
        level.rocks().ifPresent(rocks -> roots.add("rock"));
        level.darkness().ifPresent(darkness -> {
            roots.add("flare");
            roots.add("headlight");
        });
        return roots;
    }

    private static boolean claims(Set<String> roots, String sprite) {
        return roots.stream().anyMatch(root -> sprite.equals(root) || sprite.startsWith(root + "-"));
    }

    /** A display name as a sprite root: {@code Airstrike} is {@code airstrike}. */
    private static String slug(String name) {
        return name.toLowerCase(Locale.ROOT).replace(' ', '-');
    }
}
