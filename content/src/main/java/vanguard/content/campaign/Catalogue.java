package vanguard.content.campaign;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import vanguard.content.Content;
import vanguard.content.Difficulty;
import vanguard.content.WeaponData;
import vanguard.content.WeaponRulesData;

/**
 * Everything the hangar shop can sell (design/ui/hangar, design/systems/economy), from the design
 * data: the weapons, the core parts (generators, shields, plating, engines), the utility modules
 * and the specials' charges, each with its price, upgrade costs, power draw per level, the level
 * whose hangar visit it enters the shop at, its traits and the numbers the shop compares. Also the
 * shop's rules: the sell-back share and the repair cost per armour point.
 */
public final class Catalogue {
    /** A number the shop shows and compares with the fitted item. */
    public enum Stat {
        /** Volley damage per second: every projectile of a volley (both sides of a mirrored pattern). */
        DPS(true),
        /** Generator output, MW. */
        OUTPUT(true),
        CAPACITY(true),
        /** Shield points per second. */
        REGEN(true),
        /** Seconds without hits before the shield regenerates. */
        DELAY(false),
        /** Maximum armour points. */
        ARMOUR(true),
        /** Top speed, px/s. */
        SPEED(true),
        /** The most charges a special carries. */
        CHARGES(true);

        private final boolean higherIsBetter;

        Stat(boolean higherIsBetter) {
            this.higherIsBetter = higherIsBetter;
        }

        public boolean higherIsBetter() {
            return higherIsBetter;
        }
    }

    /**
     * A shop item.
     *
     * @param id a weapon's slug, or the part's name in its data file
     * @param price the purchase price (for a special: one charge); 0 for a starter part
     * @param upgrades the cost of each upgrade, to L2 and on
     * @param draws the power draw at each level from L1, MW
     * @param unlock the level from whose hangar visit the item is in the shop
     * @param stats the shown numbers at each level from L1
     * @param forSale false keeps it out of the shop although it is unlocked: a module whose effects
     *     come later (the Targeting computer until M5, design/systems/economy)
     */
    public record Item(
            ItemKind kind,
            String id,
            String name,
            int price,
            List<Integer> upgrades,
            List<Double> draws,
            int unlock,
            List<String> traits,
            List<Map<Stat, Double>> stats,
            boolean forSale) {
        /** An item the shop sells once it is unlocked. */
        public Item(
                ItemKind kind,
                String id,
                String name,
                int price,
                List<Integer> upgrades,
                List<Double> draws,
                int unlock,
                List<String> traits,
                List<Map<Stat, Double>> stats) {
            this(kind, id, name, price, upgrades, draws, unlock, traits, stats, true);
        }

        public Item {
            upgrades = List.copyOf(upgrades);
            draws = List.copyOf(draws);
            traits = List.copyOf(traits);
            stats = stats.stream().map(Map::copyOf).toList();
            if (draws.size() != upgrades.size() + 1 || stats.size() != draws.size()) {
                throw new IllegalArgumentException(id + ": one draw and one set of stats per level");
            }
        }

        public int maxLevel() {
            return draws.size();
        }

        public double draw(int level) {
            return draws.get(level - 1);
        }

        public Map<Stat, Double> stats(int level) {
            return stats.get(level - 1);
        }

        /** The cost of the upgrade from {@code level} to the next. */
        public int upgradeCost(int level) {
            return upgrades.get(level - 1);
        }

        /** What an item at {@code level} cost in all: its price plus its upgrades. */
        public int spent(int level) {
            return price
                    + upgrades.subList(0, level - 1).stream()
                            .mapToInt(Integer::intValue)
                            .sum();
        }
    }

    private final Map<ItemKind, List<Item>> items;
    private final double sellBack;
    private final Map<Difficulty, Integer> repairCost;
    private final Map<Difficulty, Integer> sensorBonus;

    private Catalogue(
            Map<ItemKind, List<Item>> items,
            double sellBack,
            Map<Difficulty, Integer> repairCost,
            Map<Difficulty, Integer> sensorBonus) {
        this.items = items;
        this.sellBack = sellBack;
        this.repairCost = repairCost;
        this.sensorBonus = sensorBonus;
    }

    public static Catalogue of(Content content) {
        Map<ItemKind, List<Item>> items = new EnumMap<>(ItemKind.class);
        for (ItemKind kind : ItemKind.values()) {
            items.put(kind, new ArrayList<>());
        }
        WeaponRulesData rules = content.weaponRules();
        content.weapons().entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(weapon -> {
            Item item = weapon(weapon.getKey(), weapon.getValue(), rules);
            items.get(item.kind()).add(item);
        });
        var player = content.player();
        for (var model : content.generators().models()) {
            items.get(ItemKind.GENERATOR)
                    .add(part(
                            ItemKind.GENERATOR,
                            model.name(),
                            model.price(),
                            0,
                            player.firstLevel(model.available()),
                            Map.of(Stat.OUTPUT, model.output())));
        }
        for (var model : content.shields().models()) {
            items.get(ItemKind.SHIELD)
                    .add(part(
                            ItemKind.SHIELD,
                            model.name(),
                            model.price(),
                            model.draw(),
                            player.firstLevel(model.available()),
                            Map.of(
                                    Stat.CAPACITY,
                                    model.capacity(),
                                    Stat.REGEN,
                                    model.regen(),
                                    Stat.DELAY,
                                    model.delay())));
        }
        for (var plating : content.armour().plating()) {
            items.get(ItemKind.PLATING)
                    .add(part(
                            ItemKind.PLATING,
                            plating.name(),
                            plating.price(),
                            0,
                            player.firstLevel(plating.available()),
                            Map.of(Stat.ARMOUR, plating.max())));
        }
        for (var engine : content.systems().engines()) {
            items.get(ItemKind.ENGINE)
                    .add(part(
                            ItemKind.ENGINE,
                            engine.name(),
                            engine.price(),
                            engine.draw(),
                            player.firstLevel(engine.available()),
                            Map.of(Stat.SPEED, engine.speed())));
        }
        for (var module : content.systems().utility()) {
            int levels = module.prices().size();
            items.get(ItemKind.UTILITY)
                    .add(new Item(
                            ItemKind.UTILITY,
                            module.name(),
                            module.name(),
                            module.prices().getFirst(),
                            module.prices().subList(1, levels),
                            IntStream.range(0, levels)
                                    .mapToObj(i -> module.draw())
                                    .toList(),
                            player.firstLevel(module.available()),
                            List.of(),
                            IntStream.range(0, levels)
                                    .mapToObj(i -> Map.<Stat, Double>of())
                                    .toList(),
                            module.sold()));
        }
        for (var special : content.specials().specials()) {
            items.get(ItemKind.SPECIAL)
                    .add(part(
                            ItemKind.SPECIAL,
                            special.name(),
                            special.chargePrice(),
                            0,
                            special.unlock(),
                            Map.of(Stat.CHARGES, (double) special.maxCharges())));
        }
        items.replaceAll((kind, list) -> List.copyOf(list));
        var difficulty = content.difficulty();
        Map<Difficulty, Integer> repair = new EnumMap<>(Difficulty.class);
        Map<Difficulty, Integer> sensors = new EnumMap<>(Difficulty.class);
        for (Difficulty level : Difficulty.values()) {
            repair.put(level, difficulty.repairCost().of(level));
            sensors.put(level, difficulty.sensorBonus().of(level));
        }
        return new Catalogue(items, content.economy().sellBack(), repair, sensors);
    }

    private static Item weapon(String slug, WeaponData weapon, WeaponRulesData rules) {
        ItemKind kind =
                switch (weapon.slot()) {
                    case FRONT -> ItemKind.FRONT;
                    case REAR -> ItemKind.REAR;
                    case WING -> ItemKind.WING;
                };
        int base = weapon.upgradeBase().orElse(weapon.price());
        List<Integer> upgrades = rules.upgradeCostFactors().stream()
                .map(factor -> (int) Math.rint(base * factor))
                .toList();
        int sides = weapon.mirrored().orElse(false) ? 2 : 1;
        List<Map<Stat, Double>> stats = weapon.levels().stream()
                .map(level -> Map.of(Stat.DPS, sides * level.pattern().size() * level.damage() * level.rate()))
                .toList();
        List<Double> draws = IntStream.rangeClosed(1, weapon.levels().size())
                .mapToObj(level -> draw(weapon, level, rules.drawRound()))
                .toList();
        return new Item(
                kind, slug, weapon.name(), weapon.price(), upgrades, draws, weapon.unlock(), weapon.traits(), stats);
    }

    /** design/player/weapons, Common rules: linear from L1 to L5, rounded (half up) to the draw step. */
    static double draw(WeaponData weapon, int level, double step) {
        double min = weapon.draw().min();
        double exact = min
                + (weapon.draw().max() - min) * (level - 1) / (weapon.levels().size() - 1);
        return Math.floor(exact / step + 0.5 + 1e-9) * step;
    }

    private static Item part(ItemKind kind, String name, int price, double draw, int unlock, Map<Stat, Double> stats) {
        return new Item(kind, name, name, price, List.of(), List.of(draw), unlock, List.of(), List.of(stats));
    }

    /** The items of a kind, in the order of their data files (weapons by slug). */
    public List<Item> items(ItemKind kind) {
        return items.get(kind);
    }

    /** The item of this kind and id; it must exist. */
    public Item item(ItemKind kind, String id) {
        return items.get(kind).stream()
                .filter(item -> item.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("no " + kind + " item '" + id + "'"));
    }

    /** What selling an item at {@code level} returns: the sell-back share of all spent on it, rounded half to even. */
    public int sellPrice(Item item, int level) {
        return (int) Math.rint(item.spent(level) * sellBack);
    }

    /** Credits per armour point repaired in the hangar. */
    public int repairCost(Difficulty difficulty) {
        return repairCost.get(difficulty);
    }

    /** Extra sensor levels for the hangar intel. */
    public int sensorBonus(Difficulty difficulty) {
        return sensorBonus.get(difficulty);
    }
}
