package vanguard.content.campaign;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;
import vanguard.content.Content;
import vanguard.content.ContentLoader;
import vanguard.content.Difficulty;
import vanguard.content.SimSpecs;
import vanguard.content.campaign.Hangar.Action;
import vanguard.content.campaign.Hangar.State;
import vanguard.sim.Armament;
import vanguard.sim.WeaponSpec;

class FlightTest {
    private static final Content CONTENT = ContentLoader.fromClasspath();

    @Test
    void theStarterCampaignFliesTheStarterLoadout() {
        Flight flight = Flight.of(CONTENT, HangarTest.CATALOGUE, Campaign.start(CampaignTest.RULES, Difficulty.HARD));

        assertEquals(SimSpecs.starterLoadout(CONTENT, Difficulty.HARD), flight.loadout());
        assertEquals(List.of(new Flight.Weapon(Armament.Slot.FRONT, "Pulse Cannon", 1)), flight.weapons());
        assertEquals(4, flight.sparePower());
        assertEquals(List.of(), flight.notFlown());
    }

    @Test
    void theFittedPulseCannonLevelShieldPlatingAndEngineFly() {
        Campaign campaign = HangarTest.campaign(Difficulty.MEDIUM, 2, 20_000, 60);
        Hangar hangar = new Hangar(HangarTest.CATALOGUE, campaign);
        hangar.apply(
                LoadoutSlot.GENERATOR,
                HangarTest.row(hangar, LoadoutSlot.GENERATOR, "Mk II \"Arc\"", State.BUYABLE),
                Action.BUY);
        hangar.apply(
                LoadoutSlot.FRONT,
                HangarTest.row(hangar, LoadoutSlot.FRONT, "Pulse Cannon", State.FITTED),
                Action.UPGRADE);
        for (LoadoutSlot slot : List.of(LoadoutSlot.SHIELD, LoadoutSlot.ENGINE)) {
            hangar.apply(slot, HangarTest.row(hangar, slot, "Mk II", State.BUYABLE), Action.BUY);
        }
        hangar.apply(
                LoadoutSlot.ARMOUR,
                HangarTest.row(hangar, LoadoutSlot.ARMOUR, "Composite I", State.BUYABLE),
                Action.BUY);

        Flight flight = Flight.of(CONTENT, HangarTest.CATALOGUE, campaign);

        assertEquals(List.of(new Flight.Weapon(Armament.Slot.FRONT, "Pulse Cannon", 2)), flight.weapons());
        WeaponSpec pulse = flight.loadout().armament().mount(0).weapon();
        assertEquals(
                List.of(-5.0, 5.0),
                pulse.muzzles().stream().map(WeaponSpec.Muzzle::dx).toList());
        assertEquals(1.6, pulse.damage());
        assertEquals(2.2, flight.loadout().armament().mount(0).overdrive().damage(), "the L3 pattern");
        assertEquals(30, flight.loadout().shield().capacity());
        // Mk II "Arc" 11 MW less the Pulse Cannon L2 2.5, the Mk II shield 3 and the Mk II engine 1.
        assertEquals(4.5, flight.sparePower());
        assertEquals(3 * 1.45, flight.loadout().shield().regenPerSecond(), 1e-12);
        assertEquals(80, flight.loadout().plating().maxArmour());
        assertEquals(290, flight.loadout().ship().speed());
    }

    @Test
    void theFittedWeaponsFlyInTheirSlots() {
        Campaign campaign = HangarTest.campaign(Difficulty.MEDIUM, 2, 20_000, 60);
        Hangar hangar = new Hangar(HangarTest.CATALOGUE, campaign);
        hangar.apply(
                LoadoutSlot.FRONT,
                HangarTest.row(hangar, LoadoutSlot.FRONT, "Pulse Cannon", State.FITTED),
                Action.UPGRADE);
        hangar.apply(
                LoadoutSlot.FRONT,
                HangarTest.row(hangar, LoadoutSlot.FRONT, "Scatter Vulcan", State.BUYABLE),
                Action.BUY);
        hangar.apply(
                LoadoutSlot.RIGHT_WING,
                HangarTest.row(hangar, LoadoutSlot.RIGHT_WING, "Autocannon Pod", State.BUYABLE),
                Action.BUY);

        Flight flight = Flight.of(CONTENT, HangarTest.CATALOGUE, campaign);

        assertEquals(
                List.of(
                        new Flight.Weapon(Armament.Slot.FRONT, "Scatter Vulcan", 1),
                        new Flight.Weapon(Armament.Slot.RIGHT_WING, "Autocannon Pod", 1)),
                flight.weapons());
        assertEquals(
                "scatter-vulcan", flight.loadout().armament().mount(0).weapon().slug());
        assertEquals(
                Armament.Slot.RIGHT_WING, flight.loadout().armament().mount(1).slot());
        assertEquals(List.of(), flight.notFlown());
    }
}
