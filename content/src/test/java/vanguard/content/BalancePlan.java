package vanguard.content;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import tools.jackson.databind.JsonNode;
import tools.jackson.dataformat.yaml.YAMLMapper;
import vanguard.content.campaign.Campaign;
import vanguard.content.campaign.Catalogue;
import vanguard.content.campaign.Fitted;
import vanguard.content.campaign.Hangar;
import vanguard.content.campaign.Hangar.Action;
import vanguard.content.campaign.Hangar.Offer;
import vanguard.content.campaign.Hangar.State;
import vanguard.content.campaign.ItemKind;
import vanguard.content.campaign.LoadoutSlot;

/**
 * The expected purchases of a typical medium player per hangar visit (design/player/balance-plan.yaml),
 * made through the shop's own rules ({@link Hangar}): the balance tests and the act playthrough buy
 * the plan with it, and tools/balance.py prints the same plan as the balancing sheet. The slot
 * {@code escort} is Rook's gun (M5 part C: his Mortar, bought before Level 09).
 */
public final class BalancePlan {
    private static final Map<String, LoadoutSlot> SLOTS = Map.of(
            "front", LoadoutSlot.FRONT,
            "rear", LoadoutSlot.REAR,
            "wing_l", LoadoutSlot.LEFT_WING,
            "wing_r", LoadoutSlot.RIGHT_WING,
            "escort", LoadoutSlot.ESCORT);
    private static final Map<String, LoadoutSlot> CORE = Map.of(
            "generator", LoadoutSlot.GENERATOR,
            "shield", LoadoutSlot.SHIELD,
            "armor", LoadoutSlot.ARMOUR,
            "engine", LoadoutSlot.ENGINE);

    private final Catalogue catalogue;
    private final JsonNode plan;
    private final JsonNode difficulties;
    private final int repairPoints;

    private BalancePlan(Catalogue catalogue, JsonNode tree) {
        this.catalogue = catalogue;
        this.plan = tree.path("plan");
        this.difficulties = tree.path("difficulties");
        this.repairPoints = tree.path("repair_points_per_level").asInt();
    }

    /** The plan in the design tree, with the shop's items. */
    public static BalancePlan load(Catalogue catalogue) {
        try {
            String text = Files.readString(DesignTree.ROOT.resolve("player").resolve("balance-plan.yaml"));
            return new BalancePlan(catalogue, YAMLMapper.builder().build().readTree(text));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** The armour points the static sheet repairs at every visit after the first (an estimate). */
    public int repairPoints() {
        return repairPoints;
    }

    /** The visits the plan lists, by the number of the level that follows. */
    public List<Integer> levels() {
        List<Integer> levels = new ArrayList<>();
        plan.propertyNames().forEach(key -> levels.add(Integer.parseInt(key)));
        levels.sort(null);
        return levels;
    }

    /**
     * What a visit bought.
     *
     * @param spent credits spent at the visit (sales count negative)
     * @param log the purchases in order, as the sheet prints them
     * @param problems what the shop refused or did not offer; empty when the plan was bought
     */
    public record Visit(int spent, List<String> log, List<String> problems) {}

    /**
     * Buys the plan of the visit before {@code level} in {@code hangar}: sales, weapons, refits of
     * owned items, upgrades, core parts, utility modules, then special charges, as tools/balance.py
     * does; then what the
     * plan's {@code difficulties} add on the campaign's difficulty.
     */
    public Visit buy(Hangar hangar, int level) {
        Campaign campaign = hangar.campaign();
        int before = campaign.credits();
        List<String> log = new ArrayList<>();
        List<String> problems = new ArrayList<>();
        String key = String.format("%02d", level);
        apply(hangar, plan.path(key), log, problems);
        String difficulty = campaign.difficulty().name().toLowerCase(Locale.ROOT);
        apply(hangar, difficulties.path(difficulty).path(key), log, problems);
        return new Visit(before - campaign.credits(), log, problems);
    }

    private void apply(Hangar hangar, JsonNode step, List<String> log, List<String> problems) {
        Campaign campaign = hangar.campaign();
        if (step.has("start")) {
            checkStart(campaign, step.path("start"), problems);
        }
        for (JsonNode sell : step.path("sell")) {
            LoadoutSlot slot = slot(sell.asString());
            Optional<Offer> fitted = row(hangar, slot, State.FITTED, null);
            if (fitted.isEmpty()) {
                problems.add("sell " + sell.asString() + ": nothing fitted");
                continue;
            }
            make(
                    hangar,
                    slot,
                    fitted.get(),
                    Action.SELL,
                    "sell " + fitted.get().item().name(),
                    log,
                    problems);
        }
        for (JsonNode buy : step.path("buy")) {
            LoadoutSlot slot = slot(buy.get(0).asString());
            String slug = buy.get(1).asString();
            buyItem(hangar, slot, catalogue.item(slot.kind(), slug), log, problems);
        }
        for (JsonNode fit : step.path("fit")) {
            LoadoutSlot slot = slot(fit.get(0).asString());
            Catalogue.Item item = catalogue.item(slot.kind(), fit.get(1).asString());
            if (row(hangar, slot, State.FITTED, item).isPresent()) {
                continue; // fitted already (on hard, where the plan's Mortar was not affordable)
            }
            Optional<Offer> owned = row(hangar, slot, State.OWNED, item);
            if (owned.isEmpty()) {
                problems.add("fit " + item.name() + " → " + slot + ": not owned");
                continue;
            }
            make(hangar, slot, owned.get(), Action.FIT, "fit " + item.name() + " → " + slot, log, problems);
        }
        for (JsonNode upgrade : step.path("upgrade")) {
            upgradeTo(hangar, slot(upgrade.get(0).asString()), upgrade.get(1).asInt(), log, problems);
        }
        for (JsonNode core : step.path("core")) {
            LoadoutSlot slot = CORE.get(core.get(0).asString());
            buyItem(hangar, slot, catalogue.items(slot.kind()).get(core.get(1).asInt()), log, problems);
        }
        for (JsonNode utility : step.path("utility")) {
            String name = utility.get(0).asString();
            LoadoutSlot slot = utilitySlot(campaign, name);
            if (!campaign.gear().loadout().containsKey(slot)) {
                buyItem(hangar, slot, catalogue.item(ItemKind.UTILITY, name), log, problems);
            }
            upgradeTo(hangar, slot, utility.get(1).asInt(), log, problems);
        }
        for (JsonNode charges : step.path("charges")) {
            String name = charges.get(0).asString();
            for (int i = 0; i < charges.get(1).asInt(); i++) {
                Optional<Offer> row = hangar.shop(LoadoutSlot.SPECIAL).stream()
                        .filter(offer -> offer.item().id().equals(name) && offer.state() != State.LOCKED)
                        .findFirst();
                if (row.isEmpty()) {
                    problems.add(name + " charge: not in the shop");
                    break;
                }
                make(hangar, LoadoutSlot.SPECIAL, row.get(), Action.BUY_CHARGE, name + " charge", log, problems);
            }
        }
    }

    /** The plan's starting loadout is the campaign's starter loadout. */
    private void checkStart(Campaign campaign, JsonNode start, List<String> problems) {
        Map<LoadoutSlot, Fitted> loadout = campaign.gear().loadout();
        Fitted front = loadout.get(LoadoutSlot.FRONT);
        Fitted planned = new Fitted(
                start.path("front").get(0).asString(),
                start.path("front").get(1).asInt());
        if (!planned.equals(front)) {
            problems.add("start: front " + planned + " but the campaign starts with " + front);
        }
        CORE.forEach((key, slot) -> {
            String item =
                    catalogue.items(slot.kind()).get(start.path(key).asInt()).id();
            if (!item.equals(loadout.get(slot).item())) {
                problems.add("start: " + key + " " + item + " but the campaign starts with " + loadout.get(slot));
            }
        });
    }

    private void buyItem(
            Hangar hangar, LoadoutSlot slot, Catalogue.Item item, List<String> log, List<String> problems) {
        Optional<Offer> row = row(hangar, slot, State.BUYABLE, item);
        if (row.isEmpty()) {
            problems.add(item.name() + ": not in the shop (unlock L" + item.unlock() + ")");
            return;
        }
        Hangar.Choice choice = row.get().choice(Action.BUY).orElseThrow();
        if (choice.allowed() && !choice.fits()) {
            problems.add(item.name() + ": POWER (load " + choice.load() + " MW of " + choice.output() + ")");
            return;
        }
        make(hangar, slot, row.get(), Action.BUY, "buy " + item.name() + " → " + slot, log, problems);
    }

    private void upgradeTo(Hangar hangar, LoadoutSlot slot, int level, List<String> log, List<String> problems) {
        Optional<Offer> fitted = row(hangar, slot, State.FITTED, null);
        if (fitted.isEmpty()) {
            problems.add("upgrade " + slot + ": nothing fitted");
            return;
        }
        for (int at = fitted.get().level(); at < level; at++) {
            Offer row = row(hangar, slot, State.FITTED, null).orElseThrow();
            if (!make(hangar, slot, row, Action.UPGRADE, row.item().name() + " → L" + (at + 1), log, problems)) {
                return;
            }
        }
    }

    private boolean make(
            Hangar hangar,
            LoadoutSlot slot,
            Offer row,
            Action action,
            String what,
            List<String> log,
            List<String> problems) {
        Hangar.Choice choice = row.choice(action).orElseThrow();
        int credits = hangar.campaign().credits();
        if (!choice.allowed() || !hangar.apply(slot, row, action)) {
            problems.add(what + ": " + choice.refusal().map(Enum::name).orElse("refused") + " (" + credits
                    + " credits, costs " + choice.credits() + ")");
            return false;
        }
        log.add(what + " (" + choice.credits() + ")");
        return true;
    }

    private static Optional<Offer> row(Hangar hangar, LoadoutSlot slot, State state, Catalogue.Item item) {
        return hangar.shop(slot).stream()
                .filter(offer ->
                        offer.state() == state && (item == null || offer.item().equals(item)))
                .findFirst();
    }

    private static LoadoutSlot slot(String name) {
        LoadoutSlot slot = SLOTS.get(name);
        if (slot == null) {
            throw new IllegalArgumentException("balance-plan.yaml: unknown slot '" + name + "'");
        }
        return slot;
    }

    /** The utility bay the module is fitted in, or the first free one. */
    private static LoadoutSlot utilitySlot(Campaign campaign, String name) {
        Map<LoadoutSlot, Fitted> loadout = campaign.gear().loadout();
        for (LoadoutSlot slot : List.of(LoadoutSlot.UTILITY_1, LoadoutSlot.UTILITY_2, LoadoutSlot.UTILITY_3)) {
            Fitted fitted = loadout.get(slot);
            if (fitted == null || fitted.item().equals(name)) {
                return slot;
            }
        }
        throw new IllegalStateException("no free utility bay for " + name);
    }
}
