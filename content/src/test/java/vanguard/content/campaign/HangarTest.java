package vanguard.content.campaign;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import vanguard.content.ContentLoader;
import vanguard.content.Difficulty;
import vanguard.content.campaign.Hangar.Action;
import vanguard.content.campaign.Hangar.Offer;
import vanguard.content.campaign.Hangar.Refusal;
import vanguard.content.campaign.Hangar.State;

class HangarTest {
    static final Catalogue CATALOGUE = Catalogue.of(ContentLoader.fromClasspath());

    /** A campaign before {@code level} with these credits and armour and the starter loadout. */
    static Campaign campaign(Difficulty difficulty, int level, int credits, double armour) {
        return Campaign.load(
                CampaignTest.RULES,
                new SaveGame(
                        SaveFormat.VERSION,
                        Instant.EPOCH,
                        0,
                        difficulty,
                        level,
                        credits,
                        0,
                        CampaignTest.RULES.starterLoadout(),
                        Map.of(),
                        List.of(),
                        Map.of(),
                        armour,
                        CampaignTest.RULES.retries(difficulty),
                        Map.of(),
                        List.of(),
                        List.of(),
                        new SaveGame.Stats(0, 0)));
    }

    static Offer row(Hangar hangar, LoadoutSlot slot, String name, State state) {
        return hangar.shop(slot).stream()
                .filter(offer -> offer.item().name().equals(name) && offer.state() == state)
                .findFirst()
                .orElseThrow(() -> new AssertionError("no " + state + " " + name + " in " + hangar.shop(slot)));
    }

    private static Hangar visit(Campaign campaign) {
        return new Hangar(CATALOGUE, campaign);
    }

    @Test
    void theFirstVisitAffordsThePulseCannonsL2ButNotTheSensorSuite() {
        Hangar hangar = visit(Campaign.start(CampaignTest.RULES, Difficulty.MEDIUM));
        Offer cannon = row(hangar, LoadoutSlot.FRONT, "Pulse Cannon", State.FITTED);
        Offer sensor = row(hangar, LoadoutSlot.UTILITY_1, "Sensor suite", State.BUYABLE);

        assertEquals(300, cannon.choice(Action.UPGRADE).orElseThrow().credits());
        assertTrue(cannon.choice(Action.UPGRADE).orElseThrow().allowed());
        assertEquals(800, sensor.choice(Action.BUY).orElseThrow().credits());
        assertEquals(
                Optional.of(Refusal.CREDITS),
                sensor.choice(Action.BUY).orElseThrow().refusal());
    }

    @Test
    void theShopListsTheFittedItemThenTheLockedOnesByUnlock() {
        Hangar hangar = visit(Campaign.start(CampaignTest.RULES, Difficulty.MEDIUM));

        List<Offer> front = hangar.shop(LoadoutSlot.FRONT);

        assertEquals(State.FITTED, front.getFirst().state());
        assertEquals(
                List.of("Scatter Vulcan", "Lance Laser", "Hammer Mortar", "Hornet Launcher"),
                front.subList(1, front.size()).stream()
                        .map(offer -> offer.item().name())
                        .toList());
        assertTrue(front.subList(1, front.size()).stream().allMatch(offer -> offer.state() == State.LOCKED));
        assertEquals(Optional.empty(), front.getFirst().choice(Action.SELL), "the front gun is always fitted");
    }

    @Test
    void anUpgradeRaisesTheLevelAndTheDrawAndTheUndoReturnsIt() {
        Campaign campaign = Campaign.start(CampaignTest.RULES, Difficulty.MEDIUM);
        Hangar hangar = visit(campaign);
        assertEquals(4, hangar.load(), "Pulse Cannon L1 2 + shield Mk I 2 + engine Mk I 0");

        assertTrue(hangar.apply(
                LoadoutSlot.FRONT, row(hangar, LoadoutSlot.FRONT, "Pulse Cannon", State.FITTED), Action.UPGRADE));

        assertEquals(0, campaign.credits());
        assertEquals(new Fitted("pulse-cannon", 2), campaign.loadout().get(LoadoutSlot.FRONT));
        assertEquals(4.5, hangar.load());
        assertTrue(hangar.undo());
        assertEquals(300, campaign.credits());
        assertEquals(new Fitted("pulse-cannon", 1), campaign.loadout().get(LoadoutSlot.FRONT));
        assertFalse(hangar.canUndo());
    }

    @Test
    void itemsNewAtThisVisitAreMarked() {
        Hangar hangar = visit(campaign(Difficulty.MEDIUM, 2, 1000, 60));

        Offer pod = row(hangar, LoadoutSlot.LEFT_WING, "Autocannon Pod", State.BUYABLE);

        assertTrue(pod.isNew());
        assertFalse(row(hangar, LoadoutSlot.UTILITY_1, "Sensor suite", State.BUYABLE)
                .isNew());
    }

    @Test
    void aBoughtItemIsFittedWhenThePowerAllowsAndSellsBackAtSixtyPercentOnALaterVisit() {
        Campaign campaign = campaign(Difficulty.MEDIUM, 2, 1000, 60);
        Hangar hangar = visit(campaign);

        assertTrue(hangar.apply(
                LoadoutSlot.LEFT_WING,
                row(hangar, LoadoutSlot.LEFT_WING, "Autocannon Pod", State.BUYABLE),
                Action.BUY));

        assertEquals(500, campaign.credits());
        assertEquals(new Fitted("autocannon-pod", 1), campaign.loadout().get(LoadoutSlot.LEFT_WING));
        assertEquals(5, hangar.load());
        Hangar next = visit(campaign);
        assertFalse(next.canUndo(), "the undo is the visit's");
        Offer fitted = row(next, LoadoutSlot.LEFT_WING, "Autocannon Pod", State.FITTED);
        assertEquals(-300, fitted.choice(Action.SELL).orElseThrow().credits());
        assertTrue(next.apply(LoadoutSlot.LEFT_WING, fitted, Action.SELL));
        assertEquals(800, campaign.credits());
        assertEquals(Optional.empty(), Optional.ofNullable(campaign.loadout().get(LoadoutSlot.LEFT_WING)));
    }

    @Test
    void aSellBackCountsTheUpgradesToo() {
        Campaign campaign = campaign(Difficulty.MEDIUM, 2, 2000, 60);
        Hangar hangar = visit(campaign);
        hangar.apply(
                LoadoutSlot.LEFT_WING, row(hangar, LoadoutSlot.LEFT_WING, "Autocannon Pod", State.BUYABLE), Action.BUY);
        hangar.apply(
                LoadoutSlot.LEFT_WING,
                row(hangar, LoadoutSlot.LEFT_WING, "Autocannon Pod", State.FITTED),
                Action.UPGRADE);

        Offer pod = row(visit(campaign), LoadoutSlot.LEFT_WING, "Autocannon Pod", State.FITTED);

        assertEquals(2, pod.level());
        assertEquals(-450, pod.choice(Action.SELL).orElseThrow().credits(), "60 % of 500 + 250");
    }

    @Test
    void aFitOverThePowerBudgetIsRefusedAndABuyGoesToTheInventory() {
        Campaign campaign = campaign(Difficulty.MEDIUM, 2, 20_000, 60);
        Hangar hangar = visit(campaign);
        for (LoadoutSlot wing : List.of(LoadoutSlot.LEFT_WING, LoadoutSlot.RIGHT_WING)) {
            hangar.apply(wing, row(hangar, wing, "Autocannon Pod", State.BUYABLE), Action.BUY);
        }
        hangar.apply(LoadoutSlot.FRONT, row(hangar, LoadoutSlot.FRONT, "Scatter Vulcan", State.BUYABLE), Action.BUY);
        hangar.apply(LoadoutSlot.ENGINE, row(hangar, LoadoutSlot.ENGINE, "Mk II", State.BUYABLE), Action.BUY);
        assertEquals(8, hangar.load(), "Vulcan 3 + pods 1 + 1 + shield 2 + engine 1");
        assertEquals(8, hangar.output());

        Offer shield = row(hangar, LoadoutSlot.SHIELD, "Mk II", State.BUYABLE);
        assertFalse(shield.choice(Action.BUY).orElseThrow().fits());
        assertTrue(hangar.apply(LoadoutSlot.SHIELD, shield, Action.BUY));

        assertEquals(new Fitted("Mk I", 1), campaign.loadout().get(LoadoutSlot.SHIELD));
        Offer owned = row(hangar, LoadoutSlot.SHIELD, "Mk II", State.OWNED);
        assertEquals(
                Optional.of(Refusal.POWER),
                owned.choice(Action.FIT).orElseThrow().refusal());
        assertEquals(9, owned.choice(Action.FIT).orElseThrow().load(), "1 MW over the 8 MW output");
        assertFalse(hangar.apply(LoadoutSlot.SHIELD, owned, Action.FIT));
    }

    @Test
    void aBiggerGeneratorMakesRoomAndTheOldOneGoesToTheInventory() {
        Campaign campaign = campaign(Difficulty.MEDIUM, 2, 20_000, 60);
        Hangar hangar = visit(campaign);
        hangar.apply(
                LoadoutSlot.GENERATOR, row(hangar, LoadoutSlot.GENERATOR, "Mk II \"Arc\"", State.BUYABLE), Action.BUY);
        hangar.apply(LoadoutSlot.FRONT, row(hangar, LoadoutSlot.FRONT, "Scatter Vulcan", State.BUYABLE), Action.BUY);
        hangar.apply(LoadoutSlot.SHIELD, row(hangar, LoadoutSlot.SHIELD, "Mk II", State.BUYABLE), Action.BUY);
        for (LoadoutSlot wing : List.of(LoadoutSlot.LEFT_WING, LoadoutSlot.RIGHT_WING)) {
            hangar.apply(wing, row(hangar, wing, "Autocannon Pod", State.BUYABLE), Action.BUY);
        }
        assertEquals(11, hangar.output());
        assertEquals(8, hangar.load());
        assertEquals(List.of(new Fitted("Mk I \"Spark\"", 1)), campaign.gear().inventory(ItemKind.GENERATOR));

        Offer spark = row(hangar, LoadoutSlot.GENERATOR, "Mk I \"Spark\"", State.OWNED);
        assertTrue(spark.choice(Action.FIT).orElseThrow().allowed(), "8 MW fits the 8 MW Spark exactly");
        assertTrue(
                hangar.apply(LoadoutSlot.ENGINE, row(hangar, LoadoutSlot.ENGINE, "Mk II", State.BUYABLE), Action.BUY));
        assertEquals(
                Optional.of(Refusal.POWER),
                row(hangar, LoadoutSlot.GENERATOR, "Mk I \"Spark\"", State.OWNED)
                        .choice(Action.FIT)
                        .orElseThrow()
                        .refusal());
    }

    @Test
    void theFrontGunAndTheCorePartsCannotBeUnfittedOrSoldWhileFitted() {
        Hangar hangar = visit(campaign(Difficulty.MEDIUM, 2, 1000, 60));

        for (LoadoutSlot slot : List.of(
                LoadoutSlot.FRONT, LoadoutSlot.GENERATOR, LoadoutSlot.SHIELD, LoadoutSlot.ARMOUR, LoadoutSlot.ENGINE)) {
            Offer fitted = hangar.shop(slot).getFirst();
            assertEquals(State.FITTED, fitted.state());
            assertEquals(Optional.empty(), fitted.choice(Action.UNFIT), slot.name());
            assertEquals(Optional.empty(), fitted.choice(Action.SELL), slot.name());
        }
    }

    @Test
    void anUnfittedItemWaitsInTheInventoryAndRefitsForFree() {
        Campaign campaign = campaign(Difficulty.MEDIUM, 2, 1000, 60);
        Hangar hangar = visit(campaign);
        hangar.apply(
                LoadoutSlot.LEFT_WING, row(hangar, LoadoutSlot.LEFT_WING, "Autocannon Pod", State.BUYABLE), Action.BUY);

        hangar.apply(
                LoadoutSlot.LEFT_WING,
                row(hangar, LoadoutSlot.LEFT_WING, "Autocannon Pod", State.FITTED),
                Action.UNFIT);
        assertEquals(List.of(new Fitted("autocannon-pod", 1)), campaign.gear().inventory(ItemKind.WING));
        assertTrue(hangar.apply(
                LoadoutSlot.RIGHT_WING,
                row(hangar, LoadoutSlot.RIGHT_WING, "Autocannon Pod", State.OWNED),
                Action.FIT));

        assertEquals(500, campaign.credits());
        assertEquals(new Fitted("autocannon-pod", 1), campaign.loadout().get(LoadoutSlot.RIGHT_WING));
        assertEquals(List.of(), campaign.gear().inventory(ItemKind.WING));
    }

    @Test
    void theUndoReturnsTheVisitsTransactionsInReverseOrder() {
        Campaign campaign = campaign(Difficulty.MEDIUM, 2, 1000, 41);
        Gear before = campaign.gear();
        Hangar hangar = visit(campaign);
        hangar.apply(
                LoadoutSlot.LEFT_WING, row(hangar, LoadoutSlot.LEFT_WING, "Autocannon Pod", State.BUYABLE), Action.BUY);
        hangar.apply(
                LoadoutSlot.LEFT_WING, row(hangar, LoadoutSlot.LEFT_WING, "Autocannon Pod", State.FITTED), Action.SELL);
        hangar.repair(10);

        while (hangar.undo()) {
            // back to the visit's start
        }

        assertEquals(before, campaign.gear());
    }

    @Test
    void aRepairCostsTheDifficultysPricePerPoint() {
        Campaign medium = campaign(Difficulty.MEDIUM, 2, 1000, 41.5);
        Hangar hangar = visit(medium);
        assertEquals(19, hangar.missingArmour());
        assertEquals(5, hangar.repairCost());

        assertTrue(hangar.repair(19));

        assertEquals(1000 - 95, medium.credits());
        assertEquals(60, medium.armour());
        assertEquals(0, visit(campaign(Difficulty.EASY, 2, 0, 30)).repairCost());
        assertEquals(30, visit(campaign(Difficulty.EASY, 2, 0, 30)).affordableRepair(), "free on easy");
        assertEquals(10, visit(campaign(Difficulty.HARD, 2, 55, 30)).repairCost());
        assertEquals(5, visit(campaign(Difficulty.HARD, 2, 55, 30)).affordableRepair(), "55 credits buy 5 points");
        assertFalse(visit(campaign(Difficulty.HARD, 2, 55, 30)).repair(6));
    }

    @Test
    void aPlatingSwapKeepsTheMissingArmourPoints() {
        Campaign campaign = campaign(Difficulty.MEDIUM, 2, 1000, 41);
        Hangar hangar = visit(campaign);

        hangar.apply(LoadoutSlot.ARMOUR, row(hangar, LoadoutSlot.ARMOUR, "Composite I", State.BUYABLE), Action.BUY);

        assertEquals(80, campaign.maxArmour());
        assertEquals(61, campaign.armour());
    }

    @Test
    void specialChargesAreBoughtUpToTheirMaximumAndFitTheSpecial() {
        Campaign campaign = campaign(Difficulty.MEDIUM, 4, 5000, 60);
        Hangar hangar = visit(campaign);
        assertEquals(
                State.LOCKED,
                row(hangar, LoadoutSlot.SPECIAL, "Smart Bomb", State.LOCKED).state());

        for (int charge = 0; charge < 4; charge++) {
            Offer airstrike = hangar.shop(LoadoutSlot.SPECIAL).getFirst();
            assertTrue(hangar.apply(LoadoutSlot.SPECIAL, airstrike, Action.BUY_CHARGE));
        }

        assertEquals(new Fitted("Airstrike", 1), campaign.loadout().get(LoadoutSlot.SPECIAL));
        assertEquals(4, campaign.gear().charges("Airstrike"));
        assertEquals(5000 - 4 * 300, campaign.credits());
        Offer full = row(hangar, LoadoutSlot.SPECIAL, "Airstrike", State.FITTED);
        assertEquals(
                Optional.of(Refusal.FULL),
                full.choice(Action.BUY_CHARGE).orElseThrow().refusal());
    }

    @Test
    void theSensorLevelComesFromTheFittedSuiteAndTheDifficultysBonus() {
        Campaign campaign = campaign(Difficulty.MEDIUM, 2, 5000, 60);
        Hangar hangar = visit(campaign);
        assertEquals(0, hangar.sensorLevel());
        assertEquals(1, visit(campaign(Difficulty.EASY, 2, 0, 60)).sensorLevel());

        hangar.apply(
                LoadoutSlot.UTILITY_1, row(hangar, LoadoutSlot.UTILITY_1, "Sensor suite", State.BUYABLE), Action.BUY);
        hangar.apply(
                LoadoutSlot.UTILITY_1,
                row(hangar, LoadoutSlot.UTILITY_1, "Sensor suite", State.FITTED),
                Action.UPGRADE);

        assertEquals(2, hangar.sensorLevel());
        assertEquals(5000 - 800 - 2000, campaign.credits());
    }

    @Test
    void aFittedLoadoutSurvivesTheSaveRoundTrip() throws SaveException {
        Campaign campaign = campaign(Difficulty.HARD, 2, 5000, 50);
        Hangar hangar = visit(campaign);
        hangar.apply(
                LoadoutSlot.LEFT_WING, row(hangar, LoadoutSlot.LEFT_WING, "Autocannon Pod", State.BUYABLE), Action.BUY);
        hangar.apply(LoadoutSlot.FRONT, row(hangar, LoadoutSlot.FRONT, "Scatter Vulcan", State.BUYABLE), Action.BUY);
        hangar.apply(LoadoutSlot.ARMOUR, row(hangar, LoadoutSlot.ARMOUR, "Composite I", State.BUYABLE), Action.BUY);
        SaveGame save = campaign.save(Instant.parse("2026-10-02T12:00:00Z"));

        SaveGame read = SaveFormat.read(SaveFormat.write(save));

        assertEquals(save, read);
        Campaign loaded = Campaign.load(CampaignTest.RULES, read);
        assertEquals(new Fitted("scatter-vulcan", 1), loaded.loadout().get(LoadoutSlot.FRONT));
        assertEquals(List.of(new Fitted("pulse-cannon", 1)), loaded.gear().inventory(ItemKind.FRONT));
        assertEquals(List.of(new Fitted("Standard", 1)), loaded.gear().inventory(ItemKind.PLATING));
        assertEquals(80, loaded.maxArmour());
        assertEquals(70, loaded.armour());
        assertEquals(campaign.credits(), loaded.credits());
    }
}
