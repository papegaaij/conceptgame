package vanguard.content.campaign;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import vanguard.content.Content;
import vanguard.content.SimSpecs;
import vanguard.sim.Armament;
import vanguard.sim.Loadout;

/**
 * What a sortie flies of the campaign's loadout: the fitted weapons the simulation flies (the Act 1
 * arsenal, see {@link SimSpecs#flies}), the shield with the spare-power bonus, the plating and
 * the engine (the generator's output gives the spare power), and the fitted special with the
 * charges carried when the simulation flies it (the Airstrike, {@link SimSpecs#fliesSpecial}), and
 * the Pickup magnet at the best level fitted in a utility bay ({@link SimSpecs#fliesUtility}), the
 * Targeting computer (its turn bonus on the Stormhawk's homing weapons, its HP bars and weak-point
 * brackets for the renderer) and the Salvage scanner's bonus at its best fitted level, and the
 * sensor suite's best level for the HUD's threat arrows (L2+; it always gave the hangar intel), and
 * Rook in the escort slot with his fitted gun when he flies ({@link Campaign#escortFlight()}). The
 * other specials and utility modules are bought, fitted and saved but fly later; they and any weapon
 * that does not fly yet are listed for the HUD.
 *
 * @param weapons the flown weapons, in the order of the loadout's {@link Armament} mounts
 * @param sparePower the generator's output minus the fitted items' draw, MW (negative when a debug
 *     fit exceeds it)
 * @param notFlown the names of the fitted items the sortie leaves out, in slot order
 * @param sensor the best fitted sensor suite's level, 0 without one (the difficulty's intel bonus not
 *     counted: it is the hangar intel's)
 * @param targeting whether a Targeting computer is fitted: the HUD's HP bars and weak-point brackets
 * @param salvage the best fitted Salvage scanner's level, 0 without one: the secrets' glint
 */
public record Flight(
        Loadout loadout,
        List<Weapon> weapons,
        double sparePower,
        List<String> notFlown,
        int sensor,
        boolean targeting,
        int salvage) {
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
        int magnet = 0;
        int sensor = 0;
        boolean targeting = false;
        int salvage = 0;
        for (var entry : loadout.entrySet()) {
            LoadoutSlot slot = entry.getKey();
            Fitted item = entry.getValue();
            Catalogue.Item info = catalogue.item(slot.kind(), item.item());
            load += info.draw(item.level());
            if (!flies(content, slot, item)) {
                notFlown.add(info.name());
            } else if (slot.kind() == ItemKind.UTILITY && item.item().equals(Hangar.SENSOR_SUITE)) {
                sensor = Math.max(sensor, item.level());
            } else if (slot.kind() == ItemKind.UTILITY && item.item().equals(SimSpecs.TARGETING_COMPUTER)) {
                targeting = true;
            } else if (slot.kind() == ItemKind.UTILITY && item.item().equals(SimSpecs.SALVAGE_SCANNER)) {
                salvage = Math.max(salvage, item.level());
            } else if (slot.kind() == ItemKind.UTILITY && item.item().equals(SimSpecs.PICKUP_MAGNET)) {
                magnet = Math.max(magnet, item.level());
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
                campaign.difficulty(),
                targeting ? SimSpecs.targeting(content).turnBonus() : 0);
        Fitted special = loadout.get(LoadoutSlot.SPECIAL);
        if (special != null && SimSpecs.fliesSpecial(special.item())) {
            flown = flown.withSpecial(
                    SimSpecs.special(content, special.item(), campaign.gear().charges(special.item())));
        }
        if (magnet > 0) {
            flown = flown.withMagnet(SimSpecs.magnet(content, magnet));
        }
        if (salvage > 0) {
            flown = flown.withSalvage(SimSpecs.salvageBonus(content, salvage));
        }
        Optional<Campaign.EscortFlight> rook = campaign.escortFlight();
        if (rook.isPresent()) {
            Campaign.EscortFlight escort = rook.get();
            flown = flown.withWingman(SimSpecs.wingman(
                    content, escort.gun().item(), escort.gun().level(), escort.side(), escort.armour()));
        }
        return new Flight(flown, weapons, spare, notFlown, sensor, targeting, salvage);
    }

    /** Whether the simulation flies the item in this slot. */
    public static boolean flies(Content content, LoadoutSlot slot, Fitted fitted) {
        return switch (slot.kind()) {
            case FRONT, REAR, WING -> SimSpecs.flies(content, fitted.item());
            case GENERATOR, SHIELD, PLATING, ENGINE, ESCORT -> true;
            case SPECIAL -> SimSpecs.fliesSpecial(fitted.item());
            case UTILITY ->
                SimSpecs.fliesUtility(fitted.item()) || fitted.item().equals(Hangar.SENSOR_SUITE);
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
