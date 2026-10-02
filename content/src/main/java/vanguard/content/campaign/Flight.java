package vanguard.content.campaign;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import vanguard.content.Content;
import vanguard.content.SimSpecs;
import vanguard.sim.Loadout;

/**
 * What a sortie flies of the campaign's loadout. The simulation flies the Pulse Cannon at its
 * level, the shield, the plating and the engine (the generator only limits what the hangar fits);
 * the other weapons, the specials and the utility modules are bought, fitted and saved, but fly from
 * M4 on. Until then the front gun is the Pulse Cannon: the fitted one, else the owned one, else
 * the starter at L1; the items that do not fly are listed for the HUD.
 *
 * @param pulseLevel the level of the Pulse Cannon the sortie flies
 * @param notFlown the names of the fitted items the sortie leaves out, in slot order
 */
public record Flight(Loadout loadout, int pulseLevel, List<String> notFlown) {
    /** The Pulse Cannon's slug. */
    public static final String PULSE_CANNON = "pulse-cannon";

    public Flight {
        notFlown = List.copyOf(notFlown);
    }

    public static Flight of(Content content, Catalogue catalogue, Campaign campaign) {
        Gear gear = campaign.gear();
        Map<LoadoutSlot, Fitted> loadout = gear.loadout();
        int pulse = pulseLevel(gear);
        List<String> notFlown = new ArrayList<>();
        loadout.forEach((slot, fitted) -> {
            if (!flies(slot, fitted)) {
                notFlown.add(catalogue.item(slot.kind(), fitted.item()).name());
            }
        });
        return new Flight(
                SimSpecs.loadout(
                        content,
                        loadout.get(LoadoutSlot.ENGINE).item(),
                        pulse,
                        loadout.get(LoadoutSlot.SHIELD).item(),
                        loadout.get(LoadoutSlot.ARMOUR).item(),
                        campaign.difficulty()),
                pulse,
                notFlown);
    }

    /** Whether the simulation flies the item in this slot. */
    public static boolean flies(LoadoutSlot slot, Fitted fitted) {
        return switch (slot.kind()) {
            case FRONT -> fitted.item().equals(PULSE_CANNON);
            case GENERATOR, SHIELD, PLATING, ENGINE -> true;
            case REAR, WING, UTILITY, SPECIAL -> false;
        };
    }

    private static int pulseLevel(Gear gear) {
        Fitted front = gear.loadout().get(LoadoutSlot.FRONT);
        if (front.item().equals(PULSE_CANNON)) {
            return front.level();
        }
        return gear.inventory(ItemKind.FRONT).stream()
                .filter(owned -> owned.item().equals(PULSE_CANNON))
                .mapToInt(Fitted::level)
                .max()
                .orElse(1);
    }
}
