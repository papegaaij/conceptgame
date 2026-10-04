package vanguard.content.campaign;

import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import vanguard.content.Difficulty;
import vanguard.content.SpecialsData;
import vanguard.sim.LevelResult;

/**
 * The campaign state (design/systems/saves, design/systems/retry, design/systems/economy): plain
 * Java, saved as a {@link SaveGame} between levels. Nothing changes while a level is flown, so the
 * state is the level-start snapshot a retry returns to: credits earned in a level are banked only
 * when it is won, and a failed attempt's earnings are lost.
 *
 * <p>Retries: a failure ends the campaign when no retry is left (hard: 3 per level); otherwise it
 * uses one at once and the level is retried, at once or after the hangar, with its level-start
 * armour but at least the armour floor (50 %). A restart and an abort to the hangar each use one of
 * the level's retries on hard too. A game over returns to the hangar before the level: the gear
 * of the last launch with the level's retries renewed, which the autosave written at the failure
 * then holds, so Continue starts the level over instead of resuming it with none left.
 */
public final class Campaign {
    /** What a failed attempt leads to. */
    public enum Failure {
        /** The mission failed screen: retry, back to the hangar or quit. */
        MISSION_FAILED,
        /** No retry left: the campaign ends (hard). */
        GAME_OVER
    }

    private final CampaignRules rules;
    private final Difficulty difficulty;
    private double playtime;
    private int nextLevel;
    private long score;
    private Gear gear;
    /** The gear the level was last launched with: what a game over returns to. */
    private Gear launched;

    private final List<String> unlocks;
    private Optional<Integer> retriesLeft;
    private final Map<Integer, String> grades;
    private final List<String> dataCores;
    private final List<String> storyFlags;
    private int kills;
    private int deaths;
    /** The replay this campaign flies, if it is one: a throwaway copy of a save. */
    private Optional<Replay> replay = Optional.empty();

    /**
     * A replay of a level the campaign has flown (design/ui/mission-select): it flies with the save's
     * loadout and special charges at full armour, its retries are unlimited, and nothing of it is
     * kept but a better grade, which the mission select writes into {@code slot}.
     *
     * @param slot the save slot the replay was started from
     */
    public record Replay(SaveSlots.Slot slot, int level) {}

    private Campaign(CampaignRules rules, SaveGame save) {
        this.rules = rules;
        difficulty = save.difficulty();
        playtime = save.playtime();
        nextLevel = save.nextLevel();
        score = save.score();
        gear = new Gear(save.credits(), save.loadout(), save.inventory(), save.specials(), save.armour());
        launched = gear;
        unlocks = new ArrayList<>(save.unlocks());
        retriesLeft = save.retriesLeft();
        grades = new TreeMap<>(save.grades());
        dataCores = new ArrayList<>(save.dataCores());
        storyFlags = new ArrayList<>(save.storyFlags());
        kills = save.stats().kills();
        deaths = save.stats().deaths();
    }

    /** A new campaign: Level 01 next, the starting credits, the starter loadout at full armour. */
    public static Campaign start(CampaignRules rules, Difficulty difficulty) {
        return new Campaign(
                rules,
                new SaveGame(
                        SaveFormat.VERSION,
                        Instant.EPOCH,
                        0,
                        difficulty,
                        1,
                        rules.startingCredits(),
                        0,
                        rules.starterLoadout(),
                        Map.of(),
                        List.of(),
                        Map.of(),
                        rules.starterArmour(),
                        rules.retries(difficulty),
                        Map.of(),
                        List.of(),
                        List.of(),
                        new SaveGame.Stats(0, 0)));
    }

    /**
     * A replay of a level the save has flown: a copy of the save's campaign that is never saved,
     * earns no credits and leaves the save's progress alone (main-agent choice, for the user to
     * confirm: design/ui/mission-select).
     */
    public static Campaign replay(CampaignRules rules, SaveSlots.Slot slot, SaveGame save, int level) {
        if (!Missions.played(save, level)) {
            throw new IllegalArgumentException("level " + level + " has not been flown yet");
        }
        Campaign campaign = new Campaign(rules, save);
        campaign.replay = Optional.of(new Replay(slot, level));
        campaign.nextLevel = level;
        campaign.retriesLeft = Optional.empty();
        campaign.gear = campaign.gear.withArmour(campaign.maxArmour());
        campaign.launched = campaign.gear;
        return campaign;
    }

    /** The replay this campaign flies; empty for the campaign itself. */
    public Optional<Replay> replay() {
        return replay;
    }

    /** The campaign of a save. */
    public static Campaign load(CampaignRules rules, SaveGame save) {
        return new Campaign(rules, save);
    }

    /** The state as a save written at {@code now}; a replay is never saved. */
    public SaveGame save(Instant now) {
        if (replay.isPresent()) {
            throw new IllegalStateException("a replay is never saved");
        }
        return new SaveGame(
                SaveFormat.VERSION,
                now,
                playtime,
                difficulty,
                nextLevel,
                gear.credits(),
                score,
                gear.loadout(),
                gear.inventory(),
                unlocks,
                gear.specials(),
                gear.armour(),
                retriesLeft,
                grades,
                dataCores,
                storyFlags,
                new SaveGame.Stats(kills, deaths));
    }

    /** The story flag that records a special's free charges as given (a save flag). */
    static String freeChargesFlag(String special) {
        return "free-charges:" + special;
    }

    /**
     * Free charges given at a special's unlock (design/player/specials: the first Airstrike charge).
     *
     * @param fitted whether the special was fitted into the empty special slot
     */
    public record FreeCharges(String special, int charges, boolean fitted) {}

    /**
     * Gives the free charges of every special that is unlocked by the next level and has not given
     * them yet, once per campaign (a story flag), up to its most charges; a special that gave some
     * is fitted when the special slot is empty. Called as the hangar opens, before its autosave.
     *
     * @return what was given, for the hangar's notice; empty when nothing was
     */
    public List<FreeCharges> giveFreeCharges(SpecialsData specials) {
        List<FreeCharges> given = new ArrayList<>();
        Map<String, Integer> charges = new HashMap<>(gear.specials());
        Map<LoadoutSlot, Fitted> loadout = new EnumMap<>(LoadoutSlot.class);
        loadout.putAll(gear.loadout());
        for (SpecialsData.Special special : specials.specials()) {
            String flag = freeChargesFlag(special.name());
            if (special.free() == 0 || special.unlock() > nextLevel || storyFlags.contains(flag)) {
                continue;
            }
            storyFlags.add(flag);
            int carried = charges.getOrDefault(special.name(), 0);
            int added = Math.min(special.free(), special.maxCharges() - carried);
            if (added <= 0) {
                continue;
            }
            charges.put(special.name(), carried + added);
            boolean fit = !loadout.containsKey(LoadoutSlot.SPECIAL);
            if (fit) {
                loadout.put(LoadoutSlot.SPECIAL, new Fitted(special.name(), 1));
            }
            given.add(new FreeCharges(special.name(), added, fit));
        }
        if (!given.isEmpty()) {
            gear = new Gear(gear.credits(), loadout, gear.inventory(), charges, gear.armour());
        }
        return given;
    }

    /** Adds time played; every campaign screen counts its frames. */
    public void play(double seconds) {
        playtime += seconds;
    }

    /** Whether a failed or abandoned attempt can be tried again: always, except on hard with no retry left. */
    public boolean canRetry() {
        return retriesLeft.map(left -> left > 0).orElse(true);
    }

    /** A sortie of the next level leaves the hangar: its gear is what a game over returns to. */
    public void launch() {
        launched = gear;
    }

    /**
     * The Stormhawk was destroyed: counts the death and says whether the campaign goes on. When it
     * does, the attempt's retry is used at once (hard), so it is gone even if the player quits from
     * the mission failed screen, and the next attempt starts with at least the armour floor. When
     * no retry is left (game over), the state returns to the last launch's gear with the level's
     * retries renewed: the hangar before the level, as the pre-launch save had it.
     */
    public Failure fail() {
        deaths++;
        if (!canRetry()) {
            gear = launched;
            retriesLeft = rules.retries(difficulty);
            return Failure.GAME_OVER;
        }
        useRetry();
        return Failure.MISSION_FAILED;
    }

    /**
     * An attempt is abandoned to try the level again from its start state, at once (the pause
     * menu's restart) or after the hangar (abort): uses a retry on hard and raises the armour to
     * the floor.
     *
     * @return the armour the next attempt starts with
     */
    public double retry() {
        if (!canRetry()) {
            throw new IllegalStateException("no retry left");
        }
        useRetry();
        return gear.armour();
    }

    private void useRetry() {
        retriesLeft = retriesLeft.map(left -> left - 1);
        gear = gear.withArmour(Math.max(gear.armour(), rules.armourFloor() * maxArmour()));
    }

    /**
     * The level was won: banks the credits it earned with the grade bonus, adds its score and kills,
     * keeps the armour that is left, records a better grade and moves on to the next level with
     * the retries renewed.
     *
     * @param armourLeft the ship's armour at the end of the level
     * @return whether the grade is a new best for the level
     */
    public boolean complete(LevelResult result, double armourLeft) {
        return complete(result, armourLeft, 0, 0);
    }

    /**
     * The level was won, as {@link #complete(LevelResult, double)}, and the fitted special's charges
     * change by what the level used and found. A failed attempt changes nothing, so a retry starts
     * with the level-start charges (design/player/specials).
     *
     * @param chargesUsed the fitted special's charges used in the winning attempt
     * @param chargesFound its charges found there (already limited to its most)
     */
    public boolean complete(LevelResult result, double armourLeft, int chargesUsed, int chargesFound) {
        if (!(armourLeft > 0)) {
            throw new IllegalArgumentException("a won level leaves armour, not " + armourLeft);
        }
        if (replay.isPresent()) {
            // A replay banks nothing: no credits, score, kills or charges, and no progress.
            return recordGrade(result.grade().letter());
        }
        gear = gear.withCredits(gear.credits() + result.credits().total() + result.gradeBonus())
                .withArmour(Math.min(armourLeft, maxArmour()));
        Fitted special = gear.loadout().get(LoadoutSlot.SPECIAL);
        if (special != null && (chargesUsed != 0 || chargesFound != 0)) {
            Map<String, Integer> charges = new HashMap<>(gear.specials());
            charges.put(special.item(), Math.max(0, gear.charges(special.item()) - chargesUsed + chargesFound));
            gear = new Gear(gear.credits(), gear.loadout(), gear.inventory(), charges, gear.armour());
        }
        // A data core collected in a won level: the core and its unlock are kept at once
        // (design/systems/economy, Data cores), even before the item exists.
        for (LevelResult.DataCore core : result.dataCores()) {
            if (!dataCores.contains(core.name())) {
                dataCores.add(core.name());
            }
            if (!unlocks.contains(core.unlocks())) {
                unlocks.add(core.unlocks());
            }
        }
        score += result.score();
        kills += result.kills();
        boolean newBest = recordGrade(result.grade().letter());
        nextLevel++;
        retriesLeft = rules.retries(difficulty);
        launched = gear;
        return newBest;
    }

    /** Records the next level's grade when it beats the best; returns whether it did. */
    private boolean recordGrade(String grade) {
        String best = grades.get(nextLevel);
        boolean newBest =
                best == null || rules.grades().indexOf(grade) < rules.grades().indexOf(best);
        if (newBest) {
            grades.put(nextLevel, grade);
        }
        return newBest;
    }

    /** The grades from best to worst. */
    public List<String> gradeOrder() {
        return rules.grades();
    }

    public Difficulty difficulty() {
        return difficulty;
    }

    /** The level the campaign goes on with, 1–50. */
    public int nextLevel() {
        return nextLevel;
    }

    public int credits() {
        return gear.credits();
    }

    public long score() {
        return score;
    }

    /** The armour the next level starts with. */
    public double armour() {
        return gear.armour();
    }

    /** The fitted plating's maximum armour. */
    public double maxArmour() {
        return rules.maxArmour(gear.loadout().get(LoadoutSlot.ARMOUR).item());
    }

    /** What the hangar changes: credits, loadout, inventory, special charges and armour. */
    public Gear gear() {
        return gear;
    }

    /** Replaces the hangar's state: a transaction, or its undo. */
    void gear(Gear changed) {
        gear = changed;
    }

    /** The story state, saved as the save's story flags (among them the free charges given). */
    public List<String> storyFlags() {
        return List.copyOf(storyFlags);
    }

    /** Shop items unlocked ahead of their normal unlock (data cores, story), by id. */
    public List<String> unlocks() {
        return List.copyOf(unlocks);
    }

    /** Retries left in the current level; empty where they are unlimited. */
    public Optional<Integer> retriesLeft() {
        return retriesLeft;
    }

    /** The retries a level starts with; empty where they are unlimited. */
    public Optional<Integer> retriesPerLevel() {
        return rules.retries(difficulty);
    }

    public Map<LoadoutSlot, Fitted> loadout() {
        return gear.loadout();
    }

    /** The best grade of a completed level. */
    public Optional<String> grade(int level) {
        return Optional.ofNullable(grades.get(level));
    }

    public double playtime() {
        return playtime;
    }

    public int deaths() {
        return deaths;
    }

    public int kills() {
        return kills;
    }
}
