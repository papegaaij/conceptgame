package vanguard.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import vanguard.content.campaign.Campaign;
import vanguard.content.campaign.CampaignRules;
import vanguard.content.campaign.Catalogue;
import vanguard.content.campaign.Hangar;
import vanguard.content.campaign.Hangar.Action;
import vanguard.content.campaign.Hangar.Offer;
import vanguard.content.campaign.Hangar.State;
import vanguard.content.campaign.LoadoutSlot;
import vanguard.sim.Sortie;

/**
 * The economy from the first hangar visit to the second (design/systems/economy): the 300 starting
 * credits buy the Pulse Cannon's L2 upgrade, the doc's typical first purchase, but not the sensor
 * suite, as Level 01's threat profile says; a Level 01 flown with it pays for the first wing pod.
 */
class EconomyTest {
    private final Content content = ContentLoader.fromClasspath();
    private final Catalogue catalogue = Catalogue.of(content);

    private static Offer row(Hangar hangar, LoadoutSlot slot, String name, State state) {
        return hangar.shop(slot).stream()
                .filter(offer -> offer.item().name().equals(name) && offer.state() == state)
                .findFirst()
                .orElseThrow();
    }

    @Test
    void theStartingCreditsBuyTheFirstUpgradeAndLevel01PaysForAWingPod() {
        Campaign campaign = Campaign.start(CampaignRules.of(content), Difficulty.MEDIUM);
        Hangar first = new Hangar(catalogue, campaign);
        assertEquals(
                Optional.of(Hangar.Refusal.CREDITS),
                row(first, LoadoutSlot.UTILITY_1, "Sensor suite", State.BUYABLE)
                        .choice(Action.BUY)
                        .orElseThrow()
                        .refusal(),
                "the sensor suite is out of reach of the starting credits");
        assertTrue(first.apply(
                LoadoutSlot.FRONT, row(first, LoadoutSlot.FRONT, "Pulse Cannon", State.FITTED), Action.UPGRADE));
        assertEquals(0, campaign.credits());

        Sortie sortie = new Sortie(
                2185,
                vanguard.content.campaign.Flight.of(content, catalogue, campaign)
                        .loadout(),
                SimSpecs.level(content, Level01Test.LEVEL, Difficulty.MEDIUM),
                SimSpecs.rules(content, Level01Test.LEVEL, Difficulty.MEDIUM),
                campaign.armour());
        while (!sortie.complete()) {
            Level01Test.step(sortie);
        }
        campaign.complete(sortie.result(), sortie.ship().defences().armour());

        Hangar second = new Hangar(catalogue, campaign);
        Offer pod = row(second, LoadoutSlot.LEFT_WING, "Autocannon Pod", State.BUYABLE);
        assertTrue(pod.isNew());
        assertTrue(pod.choice(Action.BUY).orElseThrow().allowed(), campaign.credits() + " credits after Level 01");
    }
}
