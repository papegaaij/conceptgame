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

class FlightTest {
    private static final Content CONTENT = ContentLoader.fromClasspath();

    @Test
    void theStarterCampaignFliesTheStarterLoadout() {
        Flight flight = Flight.of(CONTENT, HangarTest.CATALOGUE, Campaign.start(CampaignTest.RULES, Difficulty.HARD));

        assertEquals(SimSpecs.starterLoadout(CONTENT, Difficulty.HARD), flight.loadout());
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

        assertEquals(2, flight.pulseLevel());
        assertEquals(List.of(-5.0, 5.0), flight.loadout().gun().pattern());
        assertEquals(1.6, flight.loadout().gun().damage());
        assertEquals(30, flight.loadout().shield().capacity());
        assertEquals(80, flight.loadout().plating().maxArmour());
        assertEquals(290, flight.loadout().ship().speed());
    }

    @Test
    void untilM4AnotherFrontGunFliesAsTheOwnedPulseCannonAndTheRestIsListed() {
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

        assertEquals(2, flight.pulseLevel());
        assertEquals(List.of("Scatter Vulcan", "Autocannon Pod"), flight.notFlown());
    }
}
