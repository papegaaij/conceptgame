package vanguard.content.campaign;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import vanguard.content.campaign.Catalogue.Item;
import vanguard.content.campaign.Catalogue.Stat;
import vanguard.sim.WingmanSpec;

/**
 * A hangar visit's shop and loadout rules (design/ui/hangar, design/player, design/systems/economy)
 * on the campaign's {@link Gear}: buying (fitted at once when the power budget allows, otherwise
 * into the inventory), upgrading, fitting and unfitting owned items for free, selling for the
 * sell-back share of all that was spent on an item (for all of it when the item was bought during
 * this visit), special charges, armour repair and the visit's undo, which returns every transaction
 * of the visit in reverse order for 100 %.
 *
 * <p>Rook's guns (design/player/wingmen, Escort inventory) are the {@link LoadoutSlot#ESCORT}
 * slot's items once he is hired: bought, upgraded, fitted and sold like the player's weapons, his
 * fitted gun never unfitted or sold, no power drawn. His armour has its own repair line at the same
 * cost per point, which is part of the visit's undo; his side is a setting outside it (design/ui/hangar):
 * an undo keeps the side he has now.
 *
 * <p>The power load is the sum of the fitted items' draws and may not exceed the generator's
 * output; the front gun and the core parts are always fitted. A plating swap keeps the damage: the
 * missing armour points stay missing (at least 1 point is left).
 */
public final class Hangar {
    /** Where a shop row's item is. */
    public enum State {
        /** In the selected slot. */
        FITTED,
        /** In the inventory (a special: charges carried, not selected). */
        OWNED,
        BUYABLE,
        /** Not in the shop yet: its unlock level is shown. */
        LOCKED
    }

    public enum Action {
        BUY,
        UPGRADE,
        FIT,
        UNFIT,
        SELL,
        BUY_CHARGE
    }

    /** Why a choice cannot be made. */
    public enum Refusal {
        CREDITS,
        /** The load would exceed the generator's output. */
        POWER,
        MAX_LEVEL,
        /** The special carries its most charges. */
        FULL
    }

    /**
     * Something a shop row offers.
     *
     * @param credits what it costs; negative for a refund
     * @param load the power load after it, MW
     * @param output the generator output after it, MW
     * @param fits whether a bought item is fitted at once; otherwise it goes to the inventory
     */
    public record Choice(
            Action action, int credits, double load, double output, boolean fits, Optional<Refusal> refusal) {
        public boolean allowed() {
            return refusal.isEmpty();
        }
    }

    /**
     * A shop row for a slot.
     *
     * @param level the owned level (1 for a buyable or locked item)
     * @param owned the owned item this row stands for; empty for a buyable or locked item
     * @param bought the owned item was bought during this visit, so it sells for all spent on it
     * @param isNew in the shop since the visit before
     */
    public record Offer(
            Item item,
            State state,
            int level,
            Optional<Fitted> owned,
            boolean bought,
            boolean isNew,
            List<Choice> choices) {
        public Offer {
            choices = List.copyOf(choices);
        }

        public Optional<Choice> choice(Action action) {
            return choices.stream().filter(choice -> choice.action() == action).findFirst();
        }
    }

    /** The utility module that decides the hangar intel's detail (design/player/systems). */
    public static final String SENSOR_SUITE = "Sensor suite";

    /** The highest sensor level. */
    public static final int MAX_SENSOR = 3;

    private static final double EPSILON = 1e-9;

    private final Catalogue catalogue;
    private final Campaign campaign;
    private Bought bought = Bought.NONE;
    private final Deque<Step> undo = new ArrayDeque<>();

    public Hangar(Catalogue catalogue, Campaign campaign) {
        this.catalogue = catalogue;
        this.campaign = campaign;
    }

    public Campaign campaign() {
        return campaign;
    }

    /** The item fitted in a slot. */
    public Optional<Item> fitted(LoadoutSlot slot) {
        return Optional.ofNullable(campaign.gear().loadout().get(slot)).map(fitted -> item(slot.kind(), fitted));
    }

    private Item item(ItemKind kind, Fitted fitted) {
        return catalogue.item(kind, fitted.item());
    }

    /** The fitted items' draw, MW. */
    public double load() {
        return load(campaign.gear().loadout());
    }

    private double load(Map<LoadoutSlot, Fitted> loadout) {
        return loadout.entrySet().stream()
                .mapToDouble(entry -> item(entry.getKey().kind(), entry.getValue())
                        .draw(entry.getValue().level()))
                .sum();
    }

    /** The fitted generator's output, MW. */
    public double output() {
        return output(campaign.gear().loadout());
    }

    private double output(Map<LoadoutSlot, Fitted> loadout) {
        return item(ItemKind.GENERATOR, loadout.get(LoadoutSlot.GENERATOR))
                .stats(1)
                .get(Stat.OUTPUT);
    }

    /** The intel detail: the best fitted sensor suite's level plus the difficulty's bonus, at most 3. */
    public int sensorLevel() {
        int fitted = campaign.gear().loadout().entrySet().stream()
                .filter(entry -> entry.getKey().kind() == ItemKind.UTILITY)
                .map(Map.Entry::getValue)
                .filter(module -> module.item().equals(SENSOR_SUITE))
                .mapToInt(Fitted::level)
                .max()
                .orElse(0);
        return Math.min(MAX_SENSOR, fitted + catalogue.sensorBonus(campaign.difficulty()));
    }

    /** The traits of the fitted weapons. */
    public Set<String> fittedTraits() {
        Set<String> traits = new LinkedHashSet<>();
        campaign.gear().loadout().forEach((slot, fitted) -> {
            if (slot.kind().weapon()) {
                traits.addAll(item(slot.kind(), fitted).traits());
            }
        });
        return traits;
    }

    /** The trait of the sources that damage hardened targets (design/enemies, hardened armour). */
    public static final String ANTI_GROUND = "anti-ground";

    /**
     * The specials that damage hardened targets (design/player/specials: the Airstrike's bombs and the
     * Smart Bomb's flash hit them); the Decoy Flares do not.
     */
    public static final Set<String> HARDENED_SPECIALS = Set.of("Airstrike", "Smart Bomb");

    /**
     * M5 part C (design/ui/hangar, Launch, user decision D7 = a): the traits the next flight brings
     * from every source a level's {@code required} trait counts: the fitted weapons' traits, the
     * traits of Rook's fitted gun while he flies (hired, fitted and not grounded; his Mortar is
     * {@code anti-ground}), and {@value #ANTI_GROUND} for an Airstrike or a Smart Bomb fitted with at
     * least one charge.
     */
    public Set<String> sourceTraits() {
        Set<String> traits = new LinkedHashSet<>(fittedTraits());
        campaign.escortFlight()
                .ifPresent(flight ->
                        traits.addAll(item(ItemKind.ESCORT, flight.gun()).traits()));
        Fitted special = campaign.gear().loadout().get(LoadoutSlot.SPECIAL);
        if (special != null
                && HARDENED_SPECIALS.contains(special.item())
                && campaign.gear().charges(special.item()) > 0) {
            traits.add(ANTI_GROUND);
        }
        return traits;
    }

    /** The {@code required} traits of a level that no source of the next flight brings ({@link #sourceTraits}). */
    public List<String> missingRequired(List<String> required) {
        Set<String> sources = sourceTraits();
        return required.stream().filter(trait -> !sources.contains(trait)).toList();
    }

    /**
     * Whether an item is in the shop at this visit: from its unlock level on, or unlocked early; never
     * an item that is not for sale yet (its early unlock stays in the campaign for later).
     */
    public boolean available(Item item) {
        return item.forSale()
                && (item.unlock() <= campaign.nextLevel() || campaign.unlocks().contains(item.id()));
    }

    /**
     * Whether a bought utility bay is for sale at this visit: from its unlock level (the third bay from
     * Act 3). Buying and fitting it comes with Act 3, so no visit offers it in the shop yet.
     */
    public boolean available(Catalogue.Bay bay) {
        return bay.unlock() <= campaign.nextLevel();
    }

    /**
     * The shop rows for a slot: the fitted item, the owned ones, the buyable ones by price, the locked
     * ones by unlock. An item that is not for sale is not listed unless it is owned.
     */
    public List<Offer> shop(LoadoutSlot slot) {
        ItemKind kind = slot.kind();
        Gear gear = campaign.gear();
        if (kind == ItemKind.ESCORT && !gear.escort().hired()) {
            // The escort slot opens when Rook joins (Level 08).
            return List.of();
        }
        List<Offer> offers = new ArrayList<>();
        Set<String> listed = new LinkedHashSet<>();
        Optional.ofNullable(gear.loadout().get(slot)).ifPresent(fitted -> {
            offers.add(fittedOffer(slot, fitted));
            listed.add(fitted.item());
        });
        if (kind == ItemKind.SPECIAL) {
            for (Item special : catalogue.items(kind)) {
                if (gear.charges(special.id()) > 0 && !listed.contains(special.id())) {
                    offers.add(new Offer(
                            special,
                            State.OWNED,
                            1,
                            Optional.of(new Fitted(special.id(), 1)),
                            false,
                            false,
                            List.of(fitChoice(slot, special, 1), chargeChoice(special))));
                    listed.add(special.id());
                }
            }
        }
        // Of equal owned items the first rows stand for the ones bought during the visit.
        List<Fitted> boughtHere = new ArrayList<>(bought.stored(kind));
        for (Fitted owned : gear.inventory(kind)) {
            Item item = item(kind, owned);
            boolean isBought = boughtHere.remove(owned);
            offers.add(new Offer(
                    item,
                    State.OWNED,
                    owned.level(),
                    Optional.of(owned),
                    isBought,
                    false,
                    List.of(fitChoice(slot, item, owned.level()), sellChoice(item, owned.level(), 0, isBought))));
            listed.add(owned.item());
        }
        catalogue.items(kind).stream()
                .filter(item -> item.price() > 0 && available(item) && !listed.contains(item.id()))
                .sorted(Comparator.comparingInt(Item::price).thenComparing(Item::name))
                .forEach(item -> offers.add(new Offer(
                        item,
                        State.BUYABLE,
                        1,
                        Optional.empty(),
                        false,
                        item.unlock() == campaign.nextLevel() && campaign.nextLevel() > 1,
                        List.of(kind == ItemKind.SPECIAL ? chargeChoice(item) : buyChoice(slot, item)))));
        catalogue.items(kind).stream()
                .filter(item -> item.forSale() && !available(item))
                .sorted(Comparator.comparingInt(Item::unlock).thenComparingInt(Item::price))
                .forEach(item ->
                        offers.add(new Offer(item, State.LOCKED, 1, Optional.empty(), false, false, List.of())));
        return offers;
    }

    private Offer fittedOffer(LoadoutSlot slot, Fitted fitted) {
        ItemKind kind = slot.kind();
        Item item = item(kind, fitted);
        boolean isBought = bought.fitted().contains(slot);
        List<Choice> choices = new ArrayList<>();
        if (kind == ItemKind.SPECIAL) {
            choices.add(chargeChoice(item));
        } else if (item.maxLevel() > 1) {
            choices.add(upgradeChoice(item, fitted.level()));
        }
        if (kind.optional() && kind != ItemKind.SPECIAL) {
            double draw = item.draw(fitted.level());
            choices.add(new Choice(Action.UNFIT, 0, load() - draw, output(), false, Optional.empty()));
            choices.add(sellChoice(item, fitted.level(), draw, isBought));
        }
        return new Offer(item, State.FITTED, fitted.level(), Optional.of(fitted), isBought, false, choices);
    }

    private Choice upgradeChoice(Item item, int level) {
        if (level >= item.maxLevel()) {
            return new Choice(Action.UPGRADE, 0, load(), output(), true, Optional.of(Refusal.MAX_LEVEL));
        }
        int cost = item.upgradeCost(level);
        double load = load() - item.draw(level) + item.draw(level + 1);
        return new Choice(Action.UPGRADE, cost, load, output(), true, refusal(cost, load, output()));
    }

    private Choice buyChoice(LoadoutSlot slot, Item item) {
        Map<LoadoutSlot, Fitted> fitted = withFitted(slot, new Fitted(item.id(), 1));
        double load = load(fitted);
        double output = output(fitted);
        boolean fits = load <= output + EPSILON;
        Optional<Refusal> refusal = campaign.credits() < item.price() ? Optional.of(Refusal.CREDITS) : Optional.empty();
        return new Choice(Action.BUY, item.price(), load, output, fits, refusal);
    }

    private Choice fitChoice(LoadoutSlot slot, Item item, int level) {
        Map<LoadoutSlot, Fitted> fitted = withFitted(slot, new Fitted(item.id(), level));
        double load = load(fitted);
        double output = output(fitted);
        return new Choice(Action.FIT, 0, load, output, true, refusal(0, load, output));
    }

    /** A sale refunds all spent on an item bought during this visit, the sell-back share otherwise. */
    private Choice sellChoice(Item item, int level, double draw, boolean isBought) {
        int refund = isBought ? item.spent(level) : catalogue.sellPrice(item, level);
        return new Choice(Action.SELL, -refund, load() - draw, output(), false, Optional.empty());
    }

    private Choice chargeChoice(Item special) {
        Optional<Refusal> refusal =
                campaign.gear().charges(special.id()) >= special.stats(1).get(Stat.CHARGES)
                        ? Optional.of(Refusal.FULL)
                        : campaign.credits() < special.price() ? Optional.of(Refusal.CREDITS) : Optional.empty();
        return new Choice(Action.BUY_CHARGE, special.price(), load(), output(), true, refusal);
    }

    private Optional<Refusal> refusal(int cost, double load, double output) {
        if (campaign.credits() < cost) {
            return Optional.of(Refusal.CREDITS);
        }
        return load > output + EPSILON ? Optional.of(Refusal.POWER) : Optional.empty();
    }

    private Map<LoadoutSlot, Fitted> withFitted(LoadoutSlot slot, Fitted item) {
        Map<LoadoutSlot, Fitted> loadout = new EnumMap<>(campaign.gear().loadout());
        loadout.put(slot, item);
        return loadout;
    }

    /**
     * Makes one of a row's choices in the slot the row was listed for; the rules are checked again.
     *
     * @return whether it was made
     */
    public boolean apply(LoadoutSlot slot, Offer offer, Action action) {
        Optional<Offer> current =
                shop(slot).stream().filter(row -> same(row, offer)).findFirst();
        Optional<Choice> choice = current.flatMap(row -> row.choice(action));
        if (choice.isEmpty() || !choice.get().allowed()) {
            return false;
        }
        Step before = step();
        Change change = new Change(before);
        Item item = offer.item();
        Offer row = current.get();
        change.credits -= choice.get().credits();
        switch (action) {
            case BUY -> {
                Fitted bought = new Fitted(item.id(), 1);
                if (choice.get().fits()) {
                    change.fit(slot, bought, true);
                } else {
                    change.store(slot.kind(), bought, true);
                }
            }
            case UPGRADE -> {
                Fitted fitted = before.gear().loadout().get(slot);
                change.loadout.put(slot, new Fitted(fitted.item(), fitted.level() + 1));
            }
            case FIT -> {
                Fitted owned = row.owned().orElseThrow();
                if (slot.kind() != ItemKind.SPECIAL) {
                    change.take(slot.kind(), owned, row.bought());
                }
                change.fit(slot, owned, row.bought());
            }
            case UNFIT -> change.store(slot.kind(), change.loadout.remove(slot), change.fitted.remove(slot));
            case SELL -> {
                if (row.state() == State.FITTED) {
                    change.loadout.remove(slot);
                    change.fitted.remove(slot);
                } else {
                    change.take(slot.kind(), row.owned().orElseThrow(), row.bought());
                }
            }
            case BUY_CHARGE -> {
                change.specials.merge(item.id(), 1, Integer::sum);
                change.loadout.put(slot, new Fitted(item.id(), 1));
            }
        }
        undo.push(before);
        restore(change.step());
        return true;
    }

    private static boolean same(Offer row, Offer offer) {
        return row.item().equals(offer.item())
                && row.state() == offer.state()
                && row.owned().equals(offer.owned())
                && row.bought() == offer.bought();
    }

    private Step step() {
        return new Step(campaign.gear(), bought);
    }

    /** Back to {@code step}'s gear, Rook keeping the side he has now: his side is not undone. */
    private void restore(Step step) {
        Gear restored = step.gear();
        Escort escort = restored.escort();
        campaign.gear(
                restored.withEscort(escort.withSide(campaign.gear().escort().side())));
        bought = step.bought();
    }

    /** The whole armour points missing. */
    public int missingArmour() {
        return (int) Math.ceil(campaign.maxArmour() - campaign.armour() - EPSILON);
    }

    /** Credits per armour point on the campaign's difficulty. */
    public int repairCost() {
        return catalogue.repairCost(campaign.difficulty());
    }

    /** The most points the credits repair now: "repair all" while they last. */
    public int affordableRepair() {
        int cost = repairCost();
        return cost == 0 ? missingArmour() : Math.min(missingArmour(), campaign.credits() / cost);
    }

    /**
     * Repairs armour points at the difficulty's cost per point.
     *
     * @return whether it was done: at least one point, no more than are missing or affordable
     */
    public boolean repair(int points) {
        if (points < 1 || points > affordableRepair()) {
            return false;
        }
        Gear before = campaign.gear();
        undo.push(step());
        campaign.gear(before.withCredits(before.credits() - points * repairCost())
                .withArmour(Math.min(campaign.maxArmour(), before.armour() + points)));
        return true;
    }

    /** Whether Rook is hired: the escort slot is open. */
    public boolean escortHired() {
        return campaign.gear().escort().hired();
    }

    /** Rook's whole armour points missing; 0 before he is hired. */
    public int escortMissingArmour() {
        Escort escort = campaign.gear().escort();
        return escort.hired() ? (int) Math.ceil(campaign.escortMaxArmour() - escort.armour() - EPSILON) : 0;
    }

    /** The most of Rook's points the credits repair now, at the same cost per point as the ship's. */
    public int affordableEscortRepair() {
        int cost = repairCost();
        return cost == 0 ? escortMissingArmour() : Math.min(escortMissingArmour(), campaign.credits() / cost);
    }

    /**
     * Repairs Rook's armour points at the difficulty's cost per point; any repair ends his grounding.
     *
     * @return whether it was done: at least one point, no more than are missing or affordable
     */
    public boolean repairEscort(int points) {
        if (points < 1 || points > affordableEscortRepair()) {
            return false;
        }
        Gear before = campaign.gear();
        undo.push(step());
        Escort escort = before.escort();
        campaign.gear(before.withCredits(before.credits() - points * repairCost())
                .withEscort(escort.withArmour(Math.min(campaign.escortMaxArmour(), escort.armour() + points))));
        return true;
    }

    /**
     * Sets Rook's side (the hangar toggle on the escort tile): a setting, not a transaction, so it is
     * not part of the visit's undo and no undo moves him back.
     *
     * @return whether it changed: he is hired and was on the other side
     */
    public boolean escortSide(WingmanSpec.Side side) {
        Gear before = campaign.gear();
        if (!before.escort().hired() || before.escort().side() == side) {
            return false;
        }
        campaign.gear(before.withEscort(before.escort().withSide(side)));
        return true;
    }

    public boolean canUndo() {
        return !undo.isEmpty();
    }

    /** Returns the visit's last transaction for 100 %; returns whether there was one. */
    public boolean undo() {
        if (undo.isEmpty()) {
            return false;
        }
        restore(undo.pop());
        return true;
    }

    /** The state a transaction changes: the gear and which of its items were bought during the visit. */
    private record Step(Gear gear, Bought bought) {}

    /**
     * The items bought during the visit, wherever they are now.
     *
     * @param fitted the slots they are fitted in
     * @param stored the ones in the inventory, by kind
     */
    private record Bought(Set<LoadoutSlot> fitted, Map<ItemKind, List<Fitted>> stored) {
        static final Bought NONE = new Bought(Set.of(), Map.of());

        Bought {
            fitted = Set.copyOf(fitted);
            stored = Gear.inventoryCopy(stored);
        }

        List<Fitted> stored(ItemKind kind) {
            return stored.getOrDefault(kind, List.of());
        }
    }

    /** A transaction's changes to the gear and to what the visit bought. */
    private final class Change {
        int credits;
        final Map<LoadoutSlot, Fitted> loadout;
        final Map<ItemKind, List<Fitted>> inventory = new EnumMap<>(ItemKind.class);
        final Map<String, Integer> specials;
        double armour;
        final Escort escort;
        final Set<LoadoutSlot> fitted = EnumSet.noneOf(LoadoutSlot.class);
        final Map<ItemKind, List<Fitted>> stored = new EnumMap<>(ItemKind.class);

        Change(Step step) {
            Gear gear = step.gear();
            credits = gear.credits();
            loadout = new EnumMap<>(LoadoutSlot.class);
            loadout.putAll(gear.loadout());
            gear.inventory().forEach((kind, items) -> inventory.put(kind, new ArrayList<>(items)));
            specials = new HashMap<>(gear.specials());
            armour = gear.armour();
            escort = gear.escort();
            fitted.addAll(step.bought().fitted());
            step.bought().stored().forEach((kind, items) -> stored.put(kind, new ArrayList<>(items)));
        }

        /**
         * Fits an item; the one it replaces goes to the inventory (a special's charges stay where
         * they are).
         *
         * @param isBought the item was bought during the visit
         */
        void fit(LoadoutSlot slot, Fitted item, boolean isBought) {
            Fitted old = loadout.put(slot, item);
            boolean oldBought = fitted.remove(slot);
            if (isBought) {
                fitted.add(slot);
            }
            if (slot.kind() == ItemKind.PLATING) {
                double damage = maxArmour(old) - armour;
                armour = Math.max(1, maxArmour(item) - damage);
            }
            if (old != null && slot.kind() != ItemKind.SPECIAL) {
                store(slot.kind(), old, oldBought);
            }
        }

        private double maxArmour(Fitted plating) {
            return item(ItemKind.PLATING, plating).stats(1).get(Stat.ARMOUR);
        }

        void store(ItemKind kind, Fitted item, boolean isBought) {
            inventory.computeIfAbsent(kind, k -> new ArrayList<>()).add(item);
            if (isBought) {
                stored.computeIfAbsent(kind, k -> new ArrayList<>()).add(item);
            }
        }

        void take(ItemKind kind, Fitted item, boolean isBought) {
            if (!inventory.getOrDefault(kind, new ArrayList<>()).remove(item)
                    || isBought && !stored.getOrDefault(kind, new ArrayList<>()).remove(item)) {
                throw new IllegalStateException(item + " is not in the inventory");
            }
        }

        Step step() {
            return new Step(
                    new Gear(credits, loadout, inventory, specials, armour, escort), new Bought(fitted, stored));
        }
    }
}
