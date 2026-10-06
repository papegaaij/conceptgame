package vanguard.content.campaign;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import vanguard.content.Difficulty;

/**
 * A save (design/systems/saves, Contents): the campaign state between levels, written as a
 * versioned JSON document by {@link SaveFormat}.
 *
 * @param version the save format's version, {@link SaveFormat#VERSION} when written by this game
 * @param created when the save was written
 * @param playtime seconds played in the campaign
 * @param nextLevel the level the campaign goes on with, 1–50
 * @param loadout the fitted item per slot (Rook's gun is in {@code escort})
 * @param inventory owned items that are not fitted, by kind (Rook's guns are in {@code escort})
 * @param unlocks shop items unlocked ahead of their normal unlock (data cores, story)
 * @param specials charges per special
 * @param armour the current armour points (repair is not automatic)
 * @param escort the escort slot: Rook, his guns and his armour (format version 3)
 * @param retriesLeft retries left in the next level on hard; empty where retries are unlimited
 * @param grades the best grade per completed level, by level number
 * @param dataCores the data cores found
 * @param storyFlags the story state
 */
public record SaveGame(
        int version,
        Instant created,
        double playtime,
        Difficulty difficulty,
        int nextLevel,
        int credits,
        long score,
        Map<LoadoutSlot, Fitted> loadout,
        Map<ItemKind, List<Fitted>> inventory,
        List<String> unlocks,
        Map<String, Integer> specials,
        double armour,
        EscortSlot escort,
        Optional<Integer> retriesLeft,
        Map<Integer, String> grades,
        List<String> dataCores,
        List<String> storyFlags,
        Stats stats) {
    public SaveGame {
        Objects.requireNonNull(created, "created");
        Objects.requireNonNull(difficulty, "difficulty");
        Objects.requireNonNull(stats, "stats");
        Objects.requireNonNull(retriesLeft, "retriesLeft");
        Objects.requireNonNull(escort, "escort");
        loadout = Gear.ordered(LoadoutSlot.class, loadout);
        inventory = Gear.inventoryCopy(inventory);
        if (loadout.containsKey(LoadoutSlot.ESCORT) || inventory.containsKey(ItemKind.ESCORT)) {
            throw new IllegalArgumentException("invalid save: Rook's guns belong in escort");
        }
        unlocks = List.copyOf(unlocks);
        specials = Map.copyOf(specials);
        grades = grades.entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, grade -> renamed(grade.getValue())));
        dataCores = List.copyOf(dataCores);
        storyFlags = List.copyOf(storyFlags);
        if (nextLevel < 1 || credits < 0 || armour <= 0 || playtime < 0) {
            throw new IllegalArgumentException("invalid save: level " + nextLevel + ", credits " + credits + ", armour "
                    + armour + ", playtime " + playtime);
        }
    }

    /** A save of the fields before the escort slot: Rook is not hired (a new campaign's escort). */
    public SaveGame(
            int version,
            Instant created,
            double playtime,
            Difficulty difficulty,
            int nextLevel,
            int credits,
            long score,
            Map<LoadoutSlot, Fitted> loadout,
            Map<ItemKind, List<Fitted>> inventory,
            List<String> unlocks,
            Map<String, Integer> specials,
            double armour,
            Optional<Integer> retriesLeft,
            Map<Integer, String> grades,
            List<String> dataCores,
            List<String> storyFlags,
            Stats stats) {
        this(
                version,
                created,
                playtime,
                difficulty,
                nextLevel,
                credits,
                score,
                loadout,
                inventory,
                unlocks,
                specials,
                armour,
                EscortSlot.NOT_HIRED,
                retriesLeft,
                grades,
                dataCores,
                storyFlags,
                stats);
    }

    /**
     * The escort slot in a save (design/systems/saves, format version 3; design/player/wingmen).
     *
     * @param hired Rook has joined
     * @param side {@code left} or {@code right}
     * @param fitted the id of his fitted gun, one of {@code guns}; none before he joins
     * @param guns every gun he owns with its level, the fitted one included
     * @param armour his current armour (repair is not automatic); 0 after an ejection
     */
    public record EscortSlot(boolean hired, String side, Optional<String> fitted, List<Fitted> guns, double armour) {
        /** Before Rook joins, as a version 2 save is migrated (his armour as he will join). */
        public static final EscortSlot NOT_HIRED =
                new EscortSlot(false, SaveFormat.ESCORT_SIDE, Optional.empty(), List.of(), SaveFormat.ESCORT_ARMOUR);

        public EscortSlot {
            Objects.requireNonNull(fitted, "fitted");
            guns = List.copyOf(guns);
            if (!side.equals("left") && !side.equals("right")) {
                throw new IllegalArgumentException("invalid escort: side " + side);
            }
            if (!(armour >= 0)) {
                throw new IllegalArgumentException("invalid escort: armour " + armour);
            }
            List<Fitted> owned = guns;
            boolean fittedOwned = fitted.filter(
                            id -> owned.stream().anyMatch(gun -> gun.item().equals(id)))
                    .isPresent();
            if (hired != fittedOwned || (!hired && (fitted.isPresent() || !guns.isEmpty()))) {
                throw new IllegalArgumentException("invalid escort: a hired Rook has his fitted gun among his guns,"
                        + " one not hired none (hired " + hired + ", fitted " + fitted + ", guns " + guns + ")");
            }
            if (guns.stream().map(Fitted::item).distinct().count() != guns.size()) {
                throw new IllegalArgumentException("invalid escort: a gun owned twice in " + guns);
            }
        }
    }

    /** The top grade was "S" until 2026-10-04 and is "A+" since (design/systems/scoring); an old save keeps its best. */
    private static String renamed(String grade) {
        return grade.equals("S") ? "A+" : grade;
    }

    /**
     * The campaign's statistics, for the debrief and an eventual stats screen.
     *
     * @param kills enemies destroyed in completed levels
     * @param deaths times the Stormhawk was destroyed
     * @param levels what each won level brought, by level number, for the act summary of an act's
     *     last debrief (format version 2; a version 1 save has none recorded)
     */
    public record Stats(int kills, int deaths, Map<Integer, LevelStats> levels) {
        public Stats {
            levels = Map.copyOf(levels);
        }

        /** Statistics without any level recorded. */
        public Stats(int kills, int deaths) {
            this(kills, deaths, Map.of());
        }
    }

    /**
     * What a won level brought the campaign (design/ui/debrief, act summary).
     *
     * @param credits the credits banked: the level's earnings with the grade bonus
     * @param kills the enemies destroyed
     */
    public record LevelStats(int credits, int kills) {
        public LevelStats {
            if (credits < 0 || kills < 0) {
                throw new IllegalArgumentException("invalid level stats: credits " + credits + ", kills " + kills);
            }
        }
    }
}
