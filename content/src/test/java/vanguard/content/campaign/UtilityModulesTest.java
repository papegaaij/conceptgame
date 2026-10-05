package vanguard.content.campaign;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import vanguard.content.Content;
import vanguard.content.ContentLoader;
import vanguard.content.Difficulty;
import vanguard.content.campaign.Hangar.Action;
import vanguard.content.campaign.Hangar.State;
import vanguard.sim.Magnet;

/**
 * The utility modules of M4 (design/player/systems; user decision D2 = A of M4 part H): the Pickup
 * magnet flies at its fitted level, and the Targeting computer stays out of the shop until M5 while
 * the L06 data core's unlock of it stays in the campaign.
 */
class UtilityModulesTest {
    private static final Content CONTENT = ContentLoader.fromClasspath();
    private static final String MAGNET = "Pickup magnet";
    private static final String TARGETING = "Targeting computer";

    @Test
    void theFittedPickupMagnetFliesAtItsLevel() {
        Campaign campaign = HangarTest.campaign(Difficulty.MEDIUM, 2, 20_000, 60);
        Hangar hangar = new Hangar(HangarTest.CATALOGUE, campaign);
        assertEquals(
                Optional.empty(),
                Flight.of(CONTENT, HangarTest.CATALOGUE, campaign).loadout().magnet());

        hangar.apply(
                LoadoutSlot.UTILITY_1,
                HangarTest.row(hangar, LoadoutSlot.UTILITY_1, MAGNET, State.BUYABLE),
                Action.BUY);
        Flight flight = Flight.of(CONTENT, HangarTest.CATALOGUE, campaign);
        assertEquals(Optional.of(new Magnet(72, 240)), flight.loadout().magnet());
        assertFalse(flight.notFlown().contains(MAGNET), "it flies: " + flight.notFlown());

        for (int level = 2; level <= 3; level++) {
            hangar.apply(
                    LoadoutSlot.UTILITY_1,
                    HangarTest.row(hangar, LoadoutSlot.UTILITY_1, MAGNET, State.FITTED),
                    Action.UPGRADE);
        }
        assertEquals(
                Optional.of(new Magnet(144, 360)),
                Flight.of(CONTENT, HangarTest.CATALOGUE, campaign).loadout().magnet());
    }

    @Test
    void theTargetingComputerIsNotForSaleButItsUnlockStaysRecorded() {
        Catalogue.Item computer = HangarTest.CATALOGUE.item(ItemKind.UTILITY, TARGETING);
        assertFalse(computer.forSale());
        assertTrue(HangarTest.CATALOGUE.item(ItemKind.UTILITY, MAGNET).forSale());

        // The L07 visit after the L06 data core, and the L08 visit of its normal unlock (act 2).
        for (int level : List.of(7, 8)) {
            Campaign campaign = withCoreUnlock(level);
            Hangar hangar = new Hangar(HangarTest.CATALOGUE, campaign);

            assertFalse(hangar.available(computer));
            assertTrue(
                    hangar.shop(LoadoutSlot.UTILITY_1).stream()
                            .noneMatch(offer -> offer.item().id().equals(TARGETING)),
                    "neither buyable nor locked in the shop at the L" + level + " visit");
            HangarTest.row(hangar, LoadoutSlot.UTILITY_1, MAGNET, State.BUYABLE);
            assertEquals(List.of(TARGETING), campaign.unlocks());
            assertEquals(List.of(TARGETING), campaign.save(Instant.EPOCH).unlocks(), "the save keeps the unlock");
        }
    }

    /** A campaign before {@code level} whose save holds the L06 data core and its unlock. */
    private static Campaign withCoreUnlock(int level) {
        return Campaign.load(
                CampaignTest.RULES,
                new SaveGame(
                        SaveFormat.VERSION,
                        Instant.EPOCH,
                        0,
                        Difficulty.MEDIUM,
                        level,
                        20_000,
                        0,
                        CampaignTest.RULES.starterLoadout(),
                        Map.of(),
                        List.of(TARGETING),
                        Map.of(),
                        60,
                        CampaignTest.RULES.retries(Difficulty.MEDIUM),
                        Map.of(),
                        List.of("settlement log"),
                        List.of(),
                        new SaveGame.Stats(0, 0)));
    }
}
