package vanguard.content.campaign;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * What the hangar changes in the campaign (design/ui/hangar): the credits, the fitted item per
 * slot, the owned items that are not fitted (by kind), the charges per special, the armour and the
 * escort slot (Rook's fitted gun in the loadout, his other guns in the inventory, the rest in
 * {@link Escort}). Immutable, so a hangar visit can keep the state before each transaction for its
 * undo.
 *
 * @param armour the current armour points (repair is not automatic)
 * @param escort Rook: hired or not, his side and armour
 */
public record Gear(
        int credits,
        Map<LoadoutSlot, Fitted> loadout,
        Map<ItemKind, List<Fitted>> inventory,
        Map<String, Integer> specials,
        double armour,
        Escort escort) {
    public Gear {
        loadout = ordered(LoadoutSlot.class, loadout);
        inventory = inventoryCopy(inventory);
        specials = Collections.unmodifiableMap(new TreeMap<>(specials));
        java.util.Objects.requireNonNull(escort, "escort");
        if (!escort.hired() && (loadout.containsKey(LoadoutSlot.ESCORT) || inventory.containsKey(ItemKind.ESCORT))) {
            throw new IllegalArgumentException("guns in the escort slot of a Rook who is not hired");
        }
    }

    /** A copy in slot order, so a save lists the slots as the ship has them. */
    static <K extends Enum<K>, V> Map<K, V> ordered(Class<K> type, Map<K, V> map) {
        Map<K, V> copy = new EnumMap<>(type);
        copy.putAll(map);
        return Collections.unmodifiableMap(copy);
    }

    /** A copy of an inventory in kind order, without empty kinds. */
    static Map<ItemKind, List<Fitted>> inventoryCopy(Map<ItemKind, List<Fitted>> inventory) {
        Map<ItemKind, List<Fitted>> copy = new EnumMap<>(ItemKind.class);
        inventory.forEach((kind, items) -> {
            if (!items.isEmpty()) {
                copy.put(kind, List.copyOf(items));
            }
        });
        return Collections.unmodifiableMap(copy);
    }

    public Gear withCredits(int changed) {
        return new Gear(changed, loadout, inventory, specials, armour, escort);
    }

    public Gear withArmour(double changed) {
        return new Gear(credits, loadout, inventory, specials, changed, escort);
    }

    public Gear withEscort(Escort changed) {
        return new Gear(credits, loadout, inventory, specials, armour, changed);
    }

    /** The owned, unfitted items of a kind. */
    public List<Fitted> inventory(ItemKind kind) {
        return inventory.getOrDefault(kind, List.of());
    }

    /** The charges carried for a special. */
    public int charges(String special) {
        return specials.getOrDefault(special, 0);
    }
}
