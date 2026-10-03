package vanguard.content.campaign;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import vanguard.content.Content;
import vanguard.content.SimSpecs;
import vanguard.sim.Armament;
import vanguard.sim.Loadout;

/**
 * What a sortie flies of the campaign's loadout: the fitted weapons the simulation flies (the Act 1
 * arsenal, see {@link SimSpecs#flies}), the shield with the spare-power bonus, the plating and
 * the engine (the generator's output gives the spare power), and the fitted special with the
 * charges carried when the simulation flies it (the Airstrike, {@link SimSpecs#fliesSpecial}). The
 * other specials and the utility modules are bought, fitted and saved but fly later in M4; they and
 * any weapon that does not fly yet are listed for the HUD.
 *
 * @param weapons the flown weapons, in the order of the loadout's {@link Armament} mounts
 * @param sparePower the generator's output minus the fitted items' draw, MW (negative when a debug
 *     fit exceeds it)
 * @param notFlown the names of the fitted items the sortie leaves out, in slot order
 */
public record Flight(Loadout loadout, List<Weapon> weapons, double sparePower, List<String> notFlown) {
    /** The Pulse Cannon's slug. */
    public static final String PULSE_CANNON = SimSpecs.PULSE_CANNON;

    public Flight {
        weapons = List.copyOf(weapons);
        notFlown = List.copyOf(notFlown);
    }

    /** A flown weapon for the HUD: its slot, name and upgrade level. */
    public record Weapon(Armament.Slot slot, String name, int level) {}

    public static Flight of(Content content, Catalogue catalogue, Campaign campaign) {
        Map<LoadoutSlot, Fitted> loadout = campaign.gear().loadout();
        List<SimSpecs.FittedWeapon> fitted = new ArrayList<>();
        List<Weapon> weapons = new ArrayList<>();
        List<String> notFlown = new ArrayList<>();
        double load = 0;
        for (var entry : loadout.entrySet()) {
            LoadoutSlot slot = entry.getKey();
            Fitted item = entry.getValue();
            Catalogue.Item info = catalogue.item(slot.kind(), item.item());
            load += info.draw(item.level());
            if (!flies(content, slot, item)) {
                notFlown.add(info.name());
            } else if (weaponSlot(slot) != null) {
                fitted.add(new SimSpecs.FittedWeapon(weaponSlot(slot), item.item(), item.level()));
                weapons.add(new Weapon(weaponSlot(slot), info.name(), item.level()));
            }
        }
        double output = catalogue
                .item(ItemKind.GENERATOR, loadout.get(LoadoutSlot.GENERATOR).item())
                .stats(1)
                .get(Catalogue.Stat.OUTPUT);
        double spare = output - load;
        Loadout flown = SimSpecs.loadout(
                content,
                loadout.get(LoadoutSlot.ENGINE).item(),
                fitted,
                loadout.get(LoadoutSlot.SHIELD).item(),
                loadout.get(LoadoutSlot.ARMOUR).item(),
                spare,
                campaign.difficulty());
        Fitted special = loadout.get(LoadoutSlot.SPECIAL);
        if (special != null && SimSpecs.fliesSpecial(special.item())) {
            flown = flown.withSpecial(
                    SimSpecs.special(content, special.item(), campaign.gear().charges(special.item())));
        }
        return new Flight(flown, weapons, spare, notFlown);
    }

    /** Whether the simulation flies the item in this slot. */
    public static boolean flies(Content content, LoadoutSlot slot, Fitted fitted) {
        return switch (slot.kind()) {
            case FRONT, REAR, WING -> SimSpecs.flies(content, fitted.item());
            case GENERATOR, SHIELD, PLATING, ENGINE -> true;
            case SPECIAL -> SimSpecs.fliesSpecial(fitted.item());
            case UTILITY -> false;
        };
    }

    /** The simulation's weapon slot of a loadout slot; {@code null} for the other slots. */
    private static Armament.Slot weaponSlot(LoadoutSlot slot) {
        return switch (slot) {
            case FRONT -> Armament.Slot.FRONT;
            case REAR -> Armament.Slot.REAR;
            case LEFT_WING -> Armament.Slot.LEFT_WING;
            case RIGHT_WING -> Armament.Slot.RIGHT_WING;
            default -> null;
        };
    }
}
