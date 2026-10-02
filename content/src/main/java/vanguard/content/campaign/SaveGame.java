package vanguard.content.campaign;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import vanguard.content.Difficulty;

/**
 * A save (design/systems/saves, Contents): the campaign state between levels, written as a
 * versioned JSON document by {@link SaveFormat}. The escort field follows with Rook (Act 2).
 *
 * @param version the save format's version, {@link SaveFormat#VERSION} when written by this game
 * @param created when the save was written
 * @param playtime seconds played in the campaign
 * @param nextLevel the level the campaign goes on with, 1–50
 * @param loadout the fitted item per slot
 * @param inventory owned items that are not fitted
 * @param unlocks shop items unlocked ahead of their normal unlock (data cores, story)
 * @param specials charges per special
 * @param armour the current armour points (repair is not automatic)
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
        List<Fitted> inventory,
        List<String> unlocks,
        Map<String, Integer> specials,
        double armour,
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
        loadout = Map.copyOf(loadout);
        inventory = List.copyOf(inventory);
        unlocks = List.copyOf(unlocks);
        specials = Map.copyOf(specials);
        grades = Map.copyOf(grades);
        dataCores = List.copyOf(dataCores);
        storyFlags = List.copyOf(storyFlags);
        if (nextLevel < 1 || credits < 0 || armour <= 0 || playtime < 0) {
            throw new IllegalArgumentException("invalid save: level " + nextLevel + ", credits " + credits + ", armour "
                    + armour + ", playtime " + playtime);
        }
    }

    /**
     * The campaign's statistics, for the debrief and an eventual stats screen.
     *
     * @param kills enemies destroyed in completed levels
     * @param deaths times the Stormhawk was destroyed
     */
    public record Stats(int kills, int deaths) {}
}
