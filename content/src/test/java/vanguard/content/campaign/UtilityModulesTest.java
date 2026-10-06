package vanguard.content.campaign;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import vanguard.content.Content;
import vanguard.content.ContentLoader;
import vanguard.content.Difficulty;
import vanguard.content.SimSpecs;
import vanguard.content.campaign.Hangar.Action;
import vanguard.content.campaign.Hangar.State;
import vanguard.sim.Armament;
import vanguard.sim.Magnet;
import vanguard.sim.WingmanSpec;

/**
 * The utility modules (design/player/systems): the Pickup magnet flies at its fitted level (M4 part
 * H); the Targeting computer is in the shop from the L07 visit with the L06 data core's unlock,
 * otherwise from the L08 visit, and turns the Stormhawk's homing weapons 20 % faster but not Rook's
 * guns; the Salvage scanner's bonus flies at its fitted level (M5 part A, user decisions D6 and D7).
 * The third utility bay is data only until Act 3: for sale from the L15 visit, never before.
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
    void theTargetingComputerIsSoldFromTheL07VisitWithTheDataCoreOtherwiseFromL08() {
        Catalogue.Item computer = HangarTest.CATALOGUE.item(ItemKind.UTILITY, TARGETING);
        assertTrue(computer.forSale());
        assertEquals(8, computer.unlock(), "act 2");

        // The L07 visit after the L06 data core, and the L08 visit of its normal unlock (act 2).
        for (int level : List.of(7, 8)) {
            Campaign campaign = withCoreUnlock(level);
            Hangar hangar = new Hangar(HangarTest.CATALOGUE, campaign);
            assertTrue(hangar.available(computer), "with the core at the L" + level + " visit");
            HangarTest.row(hangar, LoadoutSlot.UTILITY_1, TARGETING, State.BUYABLE);
            assertEquals(List.of(TARGETING), campaign.save(Instant.EPOCH).unlocks(), "the save keeps the unlock");
        }
        // Without the core: not at the L07 visit (shown locked), at the L08 visit.
        Campaign seven = HangarTest.campaign(Difficulty.MEDIUM, 7, 20_000, 60);
        assertFalse(new Hangar(HangarTest.CATALOGUE, seven).available(computer));
        Campaign eight = HangarTest.campaign(Difficulty.MEDIUM, 8, 20_000, 60);
        assertTrue(new Hangar(HangarTest.CATALOGUE, eight).available(computer));
    }

    @Test
    void theTargetingComputerTurnsTheStormhawksHomingWeaponsFasterButNotRooksMissiles() {
        Campaign plain = fitted(Optional.empty());
        Campaign targeting = fitted(Optional.of(new Fitted(TARGETING, 1)));
        Flight without = Flight.of(CONTENT, HangarTest.CATALOGUE, plain);
        Flight with = Flight.of(CONTENT, HangarTest.CATALOGUE, targeting);
        assertFalse(without.targeting());
        assertTrue(with.targeting());
        assertFalse(with.notFlown().contains(TARGETING), "it flies: " + with.notFlown());

        // Micro-missiles 270°/s at L1, the Hornet 180°/s, the Swivel's slew 360°/s; the Pulse Cannon has no turn.
        Map<String, Double> base =
                Map.of("micro-missile-pod", 270.0, "hornet-launcher", 180.0, "swivel-gun", 360.0, "pulse-cannon", 0.0);
        for (int m = 0; m < with.loadout().armament().size(); m++) {
            Armament.Mount mount = with.loadout().armament().mount(m);
            String slug = mount.weapon().slug();
            double degrees = base.get(slug);
            assertEquals(
                    Math.toRadians(degrees),
                    without.loadout().armament().mount(m).weapon().turnRate(),
                    1e-12,
                    slug + " without");
            assertEquals(Math.toRadians(degrees * 1.2), mount.weapon().turnRate(), 1e-12, slug + " with");
            assertEquals(
                    without.loadout().armament().mount(m).overdrive().turnRate() * 1.2,
                    mount.overdrive().turnRate(),
                    1e-12,
                    slug + " overdrive");
        }
        assertEquals(
                Math.toRadians(432),
                with.loadout().armament().mounts().stream()
                        .filter(mount -> mount.weapon().slug().equals("swivel-gun"))
                        .findFirst()
                        .orElseThrow()
                        .weapon()
                        .turnRate(),
                1e-12,
                "the Swivel's slew 432°/s");

        // Rook's Missiles keep the Micro-missile Pod's own turn (design/player/wingmen: not Rook's guns).
        WingmanSpec rook = with.loadout().wingman().orElseThrow();
        assertEquals("micro-missile-pod", rook.gun().slug());
        assertEquals(Math.toRadians(270), rook.gun().turnRate(), 1e-12);
        assertEquals(without.loadout().wingman().orElseThrow().gun(), rook.gun());
    }

    @Test
    void theSalvageScannersBonusFliesAtItsBestFittedLevel() {
        assertEquals(
                0,
                Flight.of(CONTENT, HangarTest.CATALOGUE, fitted(Optional.empty()))
                        .loadout()
                        .salvageBonus());
        for (int level = 1; level <= 2; level++) {
            Flight flight = Flight.of(
                    CONTENT, HangarTest.CATALOGUE, fitted(Optional.of(new Fitted(SimSpecs.SALVAGE_SCANNER, level))));
            assertEquals(level == 1 ? 0.1 : 0.2, flight.loadout().salvageBonus(), 1e-12);
            assertEquals(level, flight.salvage());
            assertFalse(flight.notFlown().contains(SimSpecs.SALVAGE_SCANNER), "it flies: " + flight.notFlown());
        }
    }

    @Test
    void theLoadoutDebugOptionFitsUtilityModulesByTheirHyphenatedNames() {
        Campaign campaign = DebugFit.startAt(CampaignTest.RULES, Difficulty.MEDIUM, 3);
        DebugFit.parse("utility=targeting-computer,utility2=salvage-scanner:2").applyTo(campaign, HangarTest.CATALOGUE);
        assertEquals(new Fitted(TARGETING, 1), campaign.gear().loadout().get(LoadoutSlot.UTILITY_1));
        assertEquals(
                new Fitted(SimSpecs.SALVAGE_SCANNER, 2),
                campaign.gear().loadout().get(LoadoutSlot.UTILITY_2));
        Flight flight = Flight.of(CONTENT, HangarTest.CATALOGUE, campaign);
        assertTrue(flight.targeting());
        assertEquals(2, flight.salvage());
        assertThrows(IllegalArgumentException.class, () -> DebugFit.parse("utility=warp-drive")
                .applyTo(campaign, HangarTest.CATALOGUE));
    }

    /**
     * A campaign before Level 08 with the Hornet in front, Micro-missiles and the Swivel on the wings
     * and Rook flying his Missiles, {@code utility} in the first utility bay.
     */
    private static Campaign fitted(Optional<Fitted> utility) {
        Campaign campaign = DebugFit.startAt(CampaignTest.RULES, Difficulty.MEDIUM, 8);
        DebugFit.parse("front=hornet-launcher,left=micro-missile-pod,right=swivel-gun")
                .withEscort("rook:missiles:1")
                .applyTo(campaign, HangarTest.CATALOGUE);
        Gear gear = campaign.gear();
        Map<LoadoutSlot, Fitted> loadout = new EnumMap<>(gear.loadout());
        utility.ifPresent(item -> loadout.put(LoadoutSlot.UTILITY_1, item));
        campaign.gear(
                new Gear(gear.credits(), loadout, gear.inventory(), gear.specials(), gear.armour(), gear.escort()));
        return campaign;
    }

    @Test
    void theThirdUtilityBayIsForSaleFromActThreeAndNotBefore() {
        Catalogue.Bay third = new Catalogue.Bay(LoadoutSlot.UTILITY_3, 5000, 15);
        assertEquals(List.of(third), HangarTest.CATALOGUE.bays(), "one bought bay: the third, 5 000 cr, from L15");

        // Every hangar visit of Acts 1 and 2 (before L01 to before L14): not for sale, the save untouched.
        for (int level = 1; level <= 14; level++) {
            Campaign campaign = HangarTest.campaign(Difficulty.MEDIUM, level, 20_000, 60);
            assertFalse(new Hangar(HangarTest.CATALOGUE, campaign).available(third), "not before Act 3: L" + level);
            SaveGame save = campaign.save(Instant.EPOCH);
            assertEquals(CampaignTest.RULES.starterLoadout(), save.loadout(), "no third bay in the save");
            assertFalse(save.loadout().containsKey(LoadoutSlot.UTILITY_3));
            assertEquals(save, Campaign.load(CampaignTest.RULES, save).save(Instant.EPOCH));
        }
        Campaign actThree = HangarTest.campaign(Difficulty.MEDIUM, 15, 20_000, 60);
        assertTrue(new Hangar(HangarTest.CATALOGUE, actThree).available(third), "the hangar visit before L15");
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
