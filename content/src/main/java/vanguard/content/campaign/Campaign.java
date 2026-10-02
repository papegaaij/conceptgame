package vanguard.content.campaign;

import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import vanguard.content.Difficulty;
import vanguard.sim.LevelResult;

/**
 * The campaign state (design/systems/saves, design/systems/retry, design/systems/economy): plain
 * Java, saved as a {@link SaveGame} between levels. Nothing changes while a level is flown, so the
 * state is the level-start snapshot a retry returns to: credits earned in a level are banked only
 * when it is won, and a failed attempt's earnings are lost.
 *
 * <p>Retries: a failure ends the campaign when no retry is left (hard: 3 per level); otherwise the
 * level is retried, at once or after the hangar, with its level-start armour but at least the
 * armour floor (50 %). A retry, a restart and an abort to the hangar each use one of the level's
 * retries on hard.
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
    private int credits;
    private long score;
    private final Map<LoadoutSlot, Fitted> loadout;
    private final List<Fitted> inventory;
    private final List<String> unlocks;
    private final Map<String, Integer> specials;
    private double armour;
    private Optional<Integer> retriesLeft;
    private final Map<Integer, String> grades;
    private final List<String> dataCores;
    private final List<String> storyFlags;
    private int kills;
    private int deaths;

    private Campaign(CampaignRules rules, SaveGame save) {
        this.rules = rules;
        difficulty = save.difficulty();
        playtime = save.playtime();
        nextLevel = save.nextLevel();
        credits = save.credits();
        score = save.score();
        loadout = new EnumMap<>(LoadoutSlot.class);
        loadout.putAll(save.loadout());
        inventory = new ArrayList<>(save.inventory());
        unlocks = new ArrayList<>(save.unlocks());
        specials = new TreeMap<>(save.specials());
        armour = save.armour();
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
                        List.of(),
                        List.of(),
                        Map.of(),
                        rules.starterArmour(),
                        rules.retries(difficulty),
                        Map.of(),
                        List.of(),
                        List.of(),
                        new SaveGame.Stats(0, 0)));
    }

    /** The campaign of a save. */
    public static Campaign load(CampaignRules rules, SaveGame save) {
        return new Campaign(rules, save);
    }

    /** The state as a save written at {@code now}. */
    public SaveGame save(Instant now) {
        return new SaveGame(
                SaveFormat.VERSION,
                now,
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
                retriesLeft,
                grades,
                dataCores,
                storyFlags,
                new SaveGame.Stats(kills, deaths));
    }

    /** Adds time played; every campaign screen counts its frames. */
    public void play(double seconds) {
        playtime += seconds;
    }

    /** Whether a failed or abandoned attempt can be tried again: always, except on hard with no retry left. */
    public boolean canRetry() {
        return retriesLeft.map(left -> left > 0).orElse(true);
    }

    /** The Stormhawk was destroyed: counts the death and says whether the campaign goes on. */
    public Failure fail() {
        deaths++;
        return canRetry() ? Failure.MISSION_FAILED : Failure.GAME_OVER;
    }

    /**
     * The level is tried again from its start state, at once (retry, restart) or after the hangar
     * (back to the hangar, abort): uses a retry on hard and raises the armour to the floor.
     *
     * @return the armour the next attempt starts with
     */
    public double retry() {
        if (!canRetry()) {
            throw new IllegalStateException("no retry left");
        }
        retriesLeft = retriesLeft.map(left -> left - 1);
        armour = Math.max(armour, rules.armourFloor() * maxArmour());
        return armour;
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
        credits += result.credits().total() + result.gradeBonus();
        score += result.score();
        kills += result.kills();
        if (!(armourLeft > 0)) {
            throw new IllegalArgumentException("a won level leaves armour, not " + armourLeft);
        }
        armour = Math.min(armourLeft, maxArmour());
        String grade = result.grade().letter();
        String best = grades.get(nextLevel);
        boolean newBest =
                best == null || rules.grades().indexOf(grade) < rules.grades().indexOf(best);
        if (newBest) {
            grades.put(nextLevel, grade);
        }
        nextLevel++;
        retriesLeft = rules.retries(difficulty);
        return newBest;
    }

    public Difficulty difficulty() {
        return difficulty;
    }

    /** The level the campaign goes on with, 1–50. */
    public int nextLevel() {
        return nextLevel;
    }

    public int credits() {
        return credits;
    }

    public long score() {
        return score;
    }

    /** The armour the next level starts with. */
    public double armour() {
        return armour;
    }

    /** The fitted plating's maximum armour; only the starter plating exists so far. */
    public double maxArmour() {
        return rules.starterArmour();
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
        return Map.copyOf(loadout);
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
