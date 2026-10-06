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
import vanguard.sim.WingmanSpec;

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
 *
 * <p>The escort slot (design/player/wingmen, M5 part A): Rook is hired when the campaign reaches
 * the hangar before Level 08 (or is loaded past it), with the free Autocannon at L1 and full armour.
 * He flies from Level 08 on while his armour is above 0 (grounded after an ejection until a repair);
 * his armour is kept as a won level ended it, a retry raises it to the armour floor like the
 * player's, and a failed attempt leaves it as he launched.
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
    /** What each won level brought, by level number (the act summary). */
    private final Map<Integer, SaveGame.LevelStats> levelStats;
    /** A debug option: the level whose win ends its act early ({@code --act-end}); never saved. */
    private Optional<Integer> debugActEnd = Optional.empty();
    /**
     * A debug option ({@code --escort}): the Rook every flight takes whatever the level, or none
     * (empty: the gear's, by the rules). It applies to the flights only: the gear, and so every save,
     * never holds it.
     */
    private Optional<DebugEscort> debugEscort = Optional.empty();

    /**
     * The {@code --escort} debug option's Rook.
     *
     * @param gun his gun at its level; empty for {@code none}: he does not fly
     * @param side his side; empty for the gear's
     */
    private record DebugEscort(Optional<Fitted> gun, Optional<WingmanSpec.Side> side) {}

    /**
     * Rook as a flight takes him (design/player/wingmen).
     *
     * @param gun his gun at its level
     * @param side his side
     * @param armour his armour at the start of the attempt
     */
    public record EscortFlight(Fitted gun, WingmanSpec.Side side, double armour) {}
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
        gear = gear(save);
        hireWhenDue();
        launched = gear;
        unlocks = new ArrayList<>(save.unlocks());
        retriesLeft = save.retriesLeft();
        grades = new TreeMap<>(save.grades());
        dataCores = new ArrayList<>(save.dataCores());
        storyFlags = new ArrayList<>(save.storyFlags());
        kills = save.stats().kills();
        deaths = save.stats().deaths();
        levelStats = new TreeMap<>(save.stats().levels());
    }

    /** The gear of a save: Rook's fitted gun into the escort slot, his other guns into the escort inventory. */
    private static Gear gear(SaveGame save) {
        SaveGame.EscortSlot slot = save.escort();
        Map<LoadoutSlot, Fitted> loadout = new EnumMap<>(LoadoutSlot.class);
        loadout.putAll(save.loadout());
        Map<ItemKind, List<Fitted>> inventory = new EnumMap<>(ItemKind.class);
        inventory.putAll(save.inventory());
        List<Fitted> guns = new ArrayList<>();
        for (Fitted gun : slot.guns()) {
            if (slot.fitted().filter(gun.item()::equals).isPresent()) {
                loadout.put(LoadoutSlot.ESCORT, gun);
            } else {
                guns.add(gun);
            }
        }
        inventory.put(ItemKind.ESCORT, guns);
        Escort escort = new Escort(slot.hired(), side(slot.side()), slot.armour());
        return new Gear(save.credits(), loadout, inventory, save.specials(), save.armour(), escort);
    }

    private static WingmanSpec.Side side(String saved) {
        return WingmanSpec.Side.valueOf(saved.toUpperCase(java.util.Locale.ROOT));
    }

    private static String side(WingmanSpec.Side side) {
        return side.name().toLowerCase(java.util.Locale.ROOT);
    }

    /** The save's escort slot of the gear. */
    private static SaveGame.EscortSlot escortSlot(Gear gear) {
        Optional<Fitted> fitted = Optional.ofNullable(gear.loadout().get(LoadoutSlot.ESCORT));
        List<Fitted> guns = new ArrayList<>();
        fitted.ifPresent(guns::add);
        guns.addAll(gear.inventory(ItemKind.ESCORT));
        Escort escort = gear.escort();
        return new SaveGame.EscortSlot(
                escort.hired(), side(escort.side()), fitted.map(Fitted::item), guns, escort.armour());
    }

    /** The gear without Rook's guns, for the save's loadout and inventory. */
    private static Map<LoadoutSlot, Fitted> shipLoadout(Gear gear) {
        Map<LoadoutSlot, Fitted> loadout = new EnumMap<>(LoadoutSlot.class);
        loadout.putAll(gear.loadout());
        loadout.remove(LoadoutSlot.ESCORT);
        return loadout;
    }

    private static Map<ItemKind, List<Fitted>> shipInventory(Gear gear) {
        Map<ItemKind, List<Fitted>> inventory = new EnumMap<>(ItemKind.class);
        inventory.putAll(gear.inventory());
        inventory.remove(ItemKind.ESCORT);
        return inventory;
    }

    /**
     * Rook joins when the hangar opens with his level (or a later one) next: the free gun at L1
     * fitted, full armour, on the side the campaign has (a new campaign's: the data's).
     */
    private void hireWhenDue() {
        CampaignRules.EscortRules escort = rules.escort();
        if (gear.escort().hired() || nextLevel < escort.joins()) {
            return;
        }
        Map<LoadoutSlot, Fitted> loadout = new EnumMap<>(LoadoutSlot.class);
        loadout.putAll(gear.loadout());
        loadout.put(LoadoutSlot.ESCORT, new Fitted(escort.starterGun(), 1));
        gear = new Gear(
                gear.credits(),
                loadout,
                gear.inventory(),
                gear.specials(),
                gear.armour(),
                new Escort(true, gear.escort().side(), escort.maxArmour()));
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
                        new SaveGame.EscortSlot(
                                false,
                                side(rules.escort().side()),
                                Optional.empty(),
                                List.of(),
                                rules.escort().maxArmour()),
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
                shipLoadout(gear),
                shipInventory(gear),
                unlocks,
                gear.specials(),
                gear.armour(),
                escortSlot(gear),
                retriesLeft,
                grades,
                dataCores,
                storyFlags,
                new SaveGame.Stats(kills, deaths, levelStats));
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
            gear = new Gear(gear.credits(), loadout, gear.inventory(), charges, gear.armour(), gear.escort());
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
        Escort escort = gear.escort();
        if (escort.hired() && escort.armour() > 0) {
            // Rook too (design/player/wingmen): a grounded Rook stays home until a repair.
            gear = gear.withEscort(
                    escort.withArmour(Math.max(escort.armour(), rules.armourFloor() * escortMaxArmour())));
        }
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
        return complete(result, armourLeft, chargesUsed, chargesFound, java.util.OptionalDouble.empty());
    }

    /**
     * The level was won, as {@link #complete(LevelResult, double, int, int)}, and Rook's armour is
     * kept as the level ended it (0 after an ejection: grounded until a repair).
     *
     * @param escortArmourLeft Rook's armour at the end of the level; empty when he did not fly
     */
    public boolean complete(
            LevelResult result,
            double armourLeft,
            int chargesUsed,
            int chargesFound,
            java.util.OptionalDouble escortArmourLeft) {
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
            gear = new Gear(gear.credits(), gear.loadout(), gear.inventory(), charges, gear.armour(), gear.escort());
        }
        if (escortArmourLeft.isPresent() && gear.escort().hired() && debugEscort.isEmpty()) {
            // The --escort debug option's Rook is not the gear's: his armour is not kept.
            gear = gear.withEscort(
                    gear.escort().withArmour(Math.clamp(escortArmourLeft.getAsDouble(), 0, escortMaxArmour())));
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
        levelStats.put(
                nextLevel, new SaveGame.LevelStats(result.credits().total() + result.gradeBonus(), result.kills()));
        boolean newBest = recordGrade(result.grade().letter());
        nextLevel++;
        retriesLeft = rules.retries(difficulty);
        hireWhenDue();
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

    /** Rook's full armour. */
    public double escortMaxArmour() {
        return rules.escort().maxArmour();
    }

    /** Whether Rook is hired and grounded: his armour is 0 after an ejection, he stays home until a repair. */
    public boolean escortGrounded() {
        return gear.escort().hired() && gear.escort().armour() <= 0;
    }

    /** Whether the launch warns about Rook's armour: he is hired and below the warning share (grounded too). */
    public boolean escortArmourLow() {
        return gear.escort().hired() && gear.escort().armour() < rules.escort().launchWarning() * escortMaxArmour();
    }

    /**
     * Whether Rook flies the next level: hired, not grounded, and the level is his joining level or
     * later (not an Act 1 replay); the {@code --escort} debug option decides instead when given.
     */
    public boolean escortFlies() {
        return escortFlight().isPresent();
    }

    /**
     * Rook as the next flight takes him: the gear's, with his fitted gun, side and armour, when he
     * flies by the rules; the {@code --escort} debug option's instead when given (his gun, the given
     * side or the gear's, full armour, whatever the level; none for {@code --escort none}).
     */
    public Optional<EscortFlight> escortFlight() {
        if (debugEscort.isPresent()) {
            DebugEscort debug = debugEscort.get();
            return debug.gun()
                    .map(gun -> new EscortFlight(
                            gun, debug.side().orElse(gear.escort().side()), escortMaxArmour()));
        }
        Escort escort = gear.escort();
        Fitted gun = gear.loadout().get(LoadoutSlot.ESCORT);
        if (!escort.hired()
                || !(escort.armour() > 0)
                || gun == null
                || nextLevel < rules.escort().joins()) {
            return Optional.empty();
        }
        return Optional.of(new EscortFlight(gun, escort.side(), Math.min(escort.armour(), escortMaxArmour())));
    }

    /**
     * A debug option for testing ({@code --escort}): Rook with {@code gun} at its level and full
     * armour, on the given side or the gear's, flies every level whatever its number; or, with
     * {@code gun} empty ({@code --escort none}), he does not fly. It applies to the flights only: the
     * gear is left alone (he is not hired by it, his guns and armour are the gear's as before), so no
     * save holds it.
     */
    public void debugEscort(Optional<Fitted> gun, Optional<WingmanSpec.Side> side) {
        debugEscort = Optional.of(new DebugEscort(gun, side));
    }

    /** The story state, saved as the save's story flags (among them the free charges given). */
    public List<String> storyFlags() {
        return List.copyOf(storyFlags);
    }

    /** The data cores found, by name, in the order they were found. */
    public List<String> dataCores() {
        return List.copyOf(dataCores);
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

    /** What a won level brought the campaign, if it was recorded (saves of format version 1 have none). */
    public Optional<SaveGame.LevelStats> levelStats(int level) {
        return Optional.ofNullable(levelStats.get(level));
    }

    /**
     * A debug option for testing ({@code --act-end}): winning the next level ends its act as the
     * act's last level would, with the act summary and the outro. It is not saved.
     */
    public void debugActEnd() {
        debugActEnd = Optional.of(nextLevel);
    }

    /** The level whose win ends its act early, see {@link #debugActEnd()}. */
    Optional<Integer> debugActEndLevel() {
        return debugActEnd;
    }
}
