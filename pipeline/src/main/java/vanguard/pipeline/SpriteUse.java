package vanguard.pipeline;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
 * Which atlases a sprite goes in (design/art-direction/production, Budgets): the game-wide sprites
 * go in the shared pages ({@code sprites}), which the game keeps loaded; a sprite only levels use
 * goes in the unit atlas ({@code level-NN}) of every level that uses it, duplicated in the build
 * output (user decision D10 = a, M5 part C), so the shared pages never grow with the levels' units
 * and a level finds all its units in its own atlas. The levels' use is derived from their data: the
 * enemies of their waves, ground targets, set pieces and boss (and the enemies those spawn), the
 * ground targets' looks, the escorted ally and the level's features the game draws with sprites of
 * its own (cranes, debris, sleds, rocks, the darkness's flares, the tows' lifeboat, pod and cable, the
 * collapse's dust puffs). A
 * sprite belongs to a root when its name is the root or starts with the root and a hyphen
 * ({@code leviathan} owns {@code leviathan-fin-left}). The shared roots are the weapons, the specials
 * and the game's own effects ({@link #GAME_WIDE}); an enemy no level uses yet goes in its first level's
 * unit atlas (its art can land before its level's data); a sprite no root claims fails the build.
 */
final class SpriteUse {
    /** The shared atlas, always loaded. */
    static final String SHARED = "sprites";

    /**
     * The sprites the game draws in any level whatever its data: the ship and its engine flame, Rook's
     * craft with its flame and eject pod (he flies wherever the escort slot does), the pickups,
     * explosions and weapon effects, the wing-mount pods, the enemies' bullets and the loot targets'
     * beacon and glint.
     */
    static final List<String> GAME_WIDE = List.of(
            "ship",
            "engine-flame",
            "rook",
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

    /** The sprites of a level's tows: {@code lifeboat}, {@code lifeboat-pod}, {@code lifeboat-cable} and their {@code -glow}s. */
    static final String TOW_ROOT = "lifeboat";

    /** The dust puffs of a level's collapse: {@code collapse-puff_0..7} (Level 09's arcology). */
    static final String COLLAPSE_ROOT = "collapse-puff";

    /** The look of a destructible ground target that names none (Level 01's cargo container). */
    private static final String DEFAULT_LOOK = "cargo-container";

    private SpriteUse() {}

    /**
     * The atlases of every sprite name: {@link #SHARED} alone, or the {@code level-NN} unit atlas of
     * every level that uses it.
     */
    static Map<String, Set<String>> atlases(Content content, Collection<String> sprites) {
        Set<String> shared = new TreeSet<>(GAME_WIDE);
        content.weapons().keySet().forEach(shared::add);
        content.specials().specials().forEach(special -> shared.add(slug(special.name())));
        Map<String, Set<String>> levels = new TreeMap<>();
        content.levels().forEach((key, level) -> levels.put(atlasOf(key), roots(content, level)));
        // An enemy no level's data uses yet (its art lands before its level) goes in the unit atlas
        // of its stat block's first level, where that level's waves will claim it.
        Set<String> used = new TreeSet<>();
        levels.values().forEach(used::addAll);
        content.enemies().forEach((slug, enemy) -> {
            if (!used.contains(slug)) {
                levels.computeIfAbsent(atlasOf(enemy.firstLevel()), level -> new TreeSet<>())
                        .add(slug);
            }
        });
        Map<String, Set<String>> atlases = new TreeMap<>();
        List<String> orphans = new ArrayList<>();
        for (String sprite : sprites) {
            Set<String> users = new TreeSet<>();
            levels.forEach((level, roots) -> {
                if (claims(roots, sprite)) {
                    users.add(level);
                }
            });
            if (claims(shared, sprite)) {
                atlases.put(sprite, Set.of(SHARED));
            } else if (!users.isEmpty()) {
                atlases.put(sprite, Collections.unmodifiableSet(users));
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
        return atlasOf(Content.levelNumber(levelKey));
    }

    /** The unit atlas of level {@code number}: {@code level-NN}. */
    static String atlasOf(int number) {
        return String.format(Locale.ROOT, "level-%02d", number);
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
        // M5 part C: a collapse's dust is drawn with its puff sprites (CollapseLooks).
        level.collapse().ifPresent(collapse -> roots.add(COLLAPSE_ROOT));
        level.darkness().ifPresent(darkness -> {
            roots.add("flare");
            roots.add("headlight");
        });
        // A tow is drawn as the lifeboat, its cargo pod and the cable (TowLooks), whatever its data.
        level.tows().filter(tows -> !tows.isEmpty()).ifPresent(tows -> roots.add(TOW_ROOT));
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
