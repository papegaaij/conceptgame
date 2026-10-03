package vanguard.content.campaign;

import java.time.Instant;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import vanguard.content.Difficulty;

/**
 * The {@code --loadout} debug option, for testing only: weapons fitted into a new campaign before
 * its first level, so a test run can fly items that are not in the shop yet. Written
 * {@code front=scatter-vulcan:3,left=bomb-rack:2,right=micro-missile-pod,rear=side-splitter:2}:
 * a slot ({@code front}, {@code rear}, {@code left}, {@code right}), a weapon's slug and its level
 * (1 if left out). The power cap is not checked. The {@code --special} debug option adds a special
 * with its charges, written {@code airstrike:2} (the special's name in lower case, hyphenated; 1
 * charge if left out, at most its most charges).
 *
 * @param weapons the weapon fitted per slot
 * @param special the special fitted, with its charges as the level
 */
public record DebugFit(Map<LoadoutSlot, Fitted> weapons, Optional<Fitted> special) {
    private static final Map<String, LoadoutSlot> SLOTS = Map.of(
            "front", LoadoutSlot.FRONT,
            "rear", LoadoutSlot.REAR,
            "left", LoadoutSlot.LEFT_WING,
            "right", LoadoutSlot.RIGHT_WING);
    private static final int MAX_LEVEL = 5;

    public DebugFit {
        weapons = Gear.ordered(LoadoutSlot.class, weapons);
    }

    /** No weapons and no special: for a {@code --special} without {@code --loadout}. */
    public static final DebugFit NONE = new DebugFit(Map.of(), Optional.empty());

    /** This fit with the special of {@code spec}, written {@code airstrike:2}. */
    public DebugFit withSpecial(String spec) {
        String[] nameAndCharges = spec.trim().split(":", 2);
        int charges = nameAndCharges.length > 1 ? Integer.parseInt(nameAndCharges[1].trim()) : 1;
        if (charges < 0) {
            throw new IllegalArgumentException("--special: " + charges + " charges");
        }
        return new DebugFit(weapons, Optional.of(new Fitted(nameAndCharges[0].trim(), charges)));
    }

    public static DebugFit parse(String spec) {
        Map<LoadoutSlot, Fitted> weapons = new EnumMap<>(LoadoutSlot.class);
        for (String part : spec.split(",")) {
            String[] slotAndItem = part.trim().split("=", 2);
            LoadoutSlot slot = SLOTS.get(slotAndItem[0].trim().toLowerCase(Locale.ROOT));
            if (slot == null || slotAndItem.length < 2) {
                throw new IllegalArgumentException(
                        "--loadout: '" + part + "' is not <front|rear|left|right>=<weapon>[:<level>]");
            }
            String[] itemAndLevel = slotAndItem[1].trim().split(":", 2);
            int level = itemAndLevel.length > 1 ? Integer.parseInt(itemAndLevel[1].trim()) : 1;
            if (level < 1 || level > MAX_LEVEL) {
                throw new IllegalArgumentException("--loadout: level " + level + " outside 1.." + MAX_LEVEL);
            }
            weapons.put(slot, new Fitted(itemAndLevel[0].trim(), level));
        }
        return new DebugFit(new HashMap<>(weapons), Optional.empty());
    }

    /**
     * A new campaign at {@code difficulty} whose next level is {@code level} (the {@code --level}
     * debug option, for testing only): the starting credits and gear, as if it began there.
     */
    public static Campaign startAt(CampaignRules rules, Difficulty difficulty, int level) {
        SaveGame start = Campaign.start(rules, difficulty).save(Instant.EPOCH);
        return Campaign.load(
                rules,
                new SaveGame(
                        start.version(),
                        start.created(),
                        start.playtime(),
                        start.difficulty(),
                        level,
                        start.credits(),
                        start.score(),
                        start.loadout(),
                        start.inventory(),
                        start.unlocks(),
                        start.specials(),
                        start.armour(),
                        start.retriesLeft(),
                        start.grades(),
                        start.dataCores(),
                        start.storyFlags(),
                        start.stats()));
    }

    /**
     * Fits the weapons into the campaign's loadout, each checked against the catalogue for its slot,
     * and the special with its charges (up to its most).
     */
    public void applyTo(Campaign campaign, Catalogue catalogue) {
        Gear gear = campaign.gear();
        Map<LoadoutSlot, Fitted> loadout = new EnumMap<>(LoadoutSlot.class);
        loadout.putAll(gear.loadout());
        weapons.forEach((slot, fitted) -> {
            catalogue.item(slot.kind(), fitted.item());
            loadout.put(slot, fitted);
        });
        Map<String, Integer> charges = new HashMap<>(gear.specials());
        special.ifPresent(fitted -> {
            Catalogue.Item item = catalogue.items(ItemKind.SPECIAL).stream()
                    .filter(candidate -> slug(candidate.id()).equals(slug(fitted.item())))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("--special: no special '" + fitted.item() + "'"));
            int most = item.stats(1).get(Catalogue.Stat.CHARGES).intValue();
            loadout.put(LoadoutSlot.SPECIAL, new Fitted(item.id(), 1));
            charges.put(item.id(), Math.min(fitted.level(), most));
        });
        campaign.gear(new Gear(gear.credits(), loadout, gear.inventory(), charges, gear.armour()));
    }

    /** A name in lower case, hyphenated: "Smart Bomb" is {@code smart-bomb}. */
    private static String slug(String name) {
        return name.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-");
    }
}
