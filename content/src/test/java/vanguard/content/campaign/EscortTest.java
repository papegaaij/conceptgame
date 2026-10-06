package vanguard.content.campaign;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;
import vanguard.content.Content;
import vanguard.content.ContentLoader;
import vanguard.content.Difficulty;
import vanguard.content.campaign.Hangar.Action;
import vanguard.content.campaign.Hangar.Offer;
import vanguard.content.campaign.Hangar.State;
import vanguard.sim.WingmanSpec;

/**
 * The escort slot in the campaign (design/player/wingmen, M5 part A; user decisions D3, D4): Rook
 * hired at the Level 08 visit, his guns owned like the player's weapons, his repair line and side,
 * the save's {@code escort} (format version 3, migrated from 2), and where he flies.
 */
class EscortTest {
    private static final Content CONTENT = ContentLoader.fromClasspath();
    private static final Catalogue CATALOGUE = HangarTest.CATALOGUE;

    private static Campaign before(int level, int credits) {
        return HangarTest.campaign(Difficulty.MEDIUM, level, credits, 60);
    }

    private static Offer row(Hangar hangar, String name, State state) {
        return HangarTest.row(hangar, LoadoutSlot.ESCORT, name, state);
    }

    @Test
    void rookJoinsWhenTheHangarOpensBeforeLevel08WithTheFreeAutocannon() {
        Campaign campaign = before(7, 1000);
        assertFalse(campaign.gear().escort().hired());
        assertEquals(List.of(), new Hangar(CATALOGUE, campaign).shop(LoadoutSlot.ESCORT));

        campaign.complete(CampaignTest.won("A", 80, 1000), 50);

        Escort escort = campaign.gear().escort();
        assertTrue(escort.hired());
        assertEquals(WingmanSpec.Side.LEFT, escort.side());
        assertEquals(80, escort.armour());
        assertEquals(new Fitted("autocannon", 1), campaign.gear().loadout().get(LoadoutSlot.ESCORT));
        assertTrue(campaign.escortFlies());
    }

    @Test
    void hisGunsCostSixtyPercentOfTheirBaseWeaponsAndDrawNoPower() {
        assertEquals(0, CATALOGUE.item(ItemKind.ESCORT, "autocannon").price());
        assertEquals(720, CATALOGUE.item(ItemKind.ESCORT, "scatter").price());
        assertEquals(480, CATALOGUE.item(ItemKind.ESCORT, "missiles").price());
        assertEquals(900, CATALOGUE.item(ItemKind.ESCORT, "mortar").price());
        assertEquals(
                List.of(150, 300, 600, 1200),
                CATALOGUE.item(ItemKind.ESCORT, "autocannon").upgrades());
        assertEquals(
                List.of(360, 720, 1440, 2880),
                CATALOGUE.item(ItemKind.ESCORT, "scatter").upgrades());
        assertEquals(
                List.of(0.0, 0.0, 0.0, 0.0, 0.0),
                CATALOGUE.item(ItemKind.ESCORT, "mortar").draws());
        assertEquals(8, CATALOGUE.item(ItemKind.ESCORT, "missiles").unlock());
        assertEquals("micro-missile-pod", CATALOGUE.escortBase("missiles"));
        // The DPS column of design/player/wingmen: 12 -> 37 for the Autocannon, 15 -> 54 for the Mortar.
        assertEquals(12, CATALOGUE.item(ItemKind.ESCORT, "autocannon").stats(1).get(Catalogue.Stat.DPS), 1e-9);
        assertEquals(
                37.2, CATALOGUE.item(ItemKind.ESCORT, "autocannon").stats(5).get(Catalogue.Stat.DPS), 1e-9);
        assertEquals(54, CATALOGUE.item(ItemKind.ESCORT, "mortar").stats(5).get(Catalogue.Stat.DPS), 1e-9);
    }

    @Test
    void hisGunsAreBoughtUpgradedFittedSoldAndUndoneLikeThePlayersWeapons() {
        Campaign campaign = before(8, 5000);
        Hangar hangar = new Hangar(CATALOGUE, campaign);
        double load = hangar.load();

        // The fitted gun: an upgrade, never an unfit or a sale.
        Offer fitted = row(hangar, "Autocannon", State.FITTED);
        assertEquals(Optional.empty(), fitted.choice(Action.SELL));
        assertEquals(Optional.empty(), fitted.choice(Action.UNFIT));
        assertTrue(hangar.apply(LoadoutSlot.ESCORT, fitted, Action.UPGRADE));
        assertEquals(5000 - 150, campaign.credits());

        // Buying fits it at once (no power drawn); the Autocannon goes to his inventory at its level.
        Offer scatter = row(hangar, "Scatter", State.BUYABLE);
        assertTrue(scatter.isNew());
        assertTrue(hangar.apply(LoadoutSlot.ESCORT, scatter, Action.BUY));
        assertEquals(5000 - 150 - 720, campaign.credits());
        assertEquals(new Fitted("scatter", 1), campaign.gear().loadout().get(LoadoutSlot.ESCORT));
        assertEquals(List.of(new Fitted("autocannon", 2)), campaign.gear().inventory(ItemKind.ESCORT));
        assertEquals(load, hangar.load(), 1e-9);

        // Fitting the Autocannon back is free; the Scatter, bought in this visit, sells for all of it.
        assertTrue(hangar.apply(LoadoutSlot.ESCORT, row(hangar, "Autocannon", State.OWNED), Action.FIT));
        Offer owned = row(hangar, "Scatter", State.OWNED);
        assertEquals(-720, owned.choice(Action.SELL).orElseThrow().credits());
        assertTrue(hangar.apply(LoadoutSlot.ESCORT, owned, Action.SELL));
        assertEquals(5000 - 150, campaign.credits());

        // Undo returns the visit's transactions one by one.
        assertTrue(hangar.undo());
        assertTrue(hangar.undo());
        assertTrue(hangar.undo());
        assertTrue(hangar.undo());
        assertEquals(5000, campaign.credits());
        assertEquals(new Fitted("autocannon", 1), campaign.gear().loadout().get(LoadoutSlot.ESCORT));
        assertEquals(List.of(), campaign.gear().inventory(ItemKind.ESCORT));
    }

    @Test
    void aGunKeptFromAnEarlierVisitSellsForTheSellBackShare() {
        Campaign campaign = before(8, 5000);
        Hangar first = new Hangar(CATALOGUE, campaign);
        first.apply(LoadoutSlot.ESCORT, row(first, "Missiles", State.BUYABLE), Action.BUY);
        first.apply(LoadoutSlot.ESCORT, row(first, "Missiles", State.FITTED), Action.UPGRADE);
        first.apply(LoadoutSlot.ESCORT, row(first, "Autocannon", State.OWNED), Action.FIT);

        Hangar next = new Hangar(CATALOGUE, campaign);
        Offer missiles = row(next, "Missiles", State.OWNED);

        // 60 % of 480 + 240.
        assertEquals(-432, missiles.choice(Action.SELL).orElseThrow().credits());
    }

    @Test
    void hisArmourHasItsOwnRepairLineAtTheDifficultysCostAndGroundsHimAtZero() {
        Campaign campaign = before(8, 2000);
        campaign.complete(CampaignTest.won("A", 80, 1000), 50, 0, 0, OptionalDouble.of(0));
        int credits = campaign.credits();
        assertTrue(campaign.escortGrounded());
        assertTrue(campaign.escortArmourLow());
        assertFalse(campaign.escortFlies());

        Hangar hangar = new Hangar(CATALOGUE, campaign);
        assertEquals(80, hangar.escortMissingArmour());
        assertEquals(5, hangar.repairCost());
        assertTrue(hangar.repairEscort(1));
        assertEquals(credits - 5, campaign.credits());
        assertFalse(campaign.escortGrounded());
        assertTrue(campaign.escortFlies());
        assertTrue(campaign.escortArmourLow());
        assertTrue(hangar.repairEscort(39));
        assertFalse(campaign.escortArmourLow());
        assertFalse(hangar.repairEscort(41));

        assertTrue(hangar.undo());
        assertEquals(1, campaign.gear().escort().armour());
    }

    @Test
    void aRetryRaisesHisArmourToTheFloorButLeavesAGroundedRookHome() {
        Campaign campaign = before(8, 0);
        campaign.complete(CampaignTest.won("A", 80, 1000), 50, 0, 0, OptionalDouble.of(10));
        campaign.retry();
        assertEquals(40, campaign.gear().escort().armour());

        Campaign grounded = before(8, 0);
        grounded.complete(CampaignTest.won("A", 80, 1000), 50, 0, 0, OptionalDouble.of(0));
        grounded.retry();
        assertEquals(0, grounded.gear().escort().armour());
    }

    @Test
    void hisSideIsAHangarSettingOutsideTheUndo() {
        Campaign campaign = before(8, 3000);
        Hangar hangar = new Hangar(CATALOGUE, campaign);

        assertFalse(hangar.escortSide(WingmanSpec.Side.LEFT));
        assertTrue(hangar.escortSide(WingmanSpec.Side.RIGHT));
        assertEquals(WingmanSpec.Side.RIGHT, campaign.gear().escort().side());
        assertFalse(hangar.canUndo(), "a side change is no transaction");
        assertFalse(hangar.undo());
        assertEquals(WingmanSpec.Side.RIGHT, campaign.gear().escort().side());

        // Undoing a purchase made before the next side change keeps the side he has now.
        int credits = campaign.credits();
        hangar.apply(LoadoutSlot.ESCORT, row(hangar, "Mortar", State.BUYABLE), Action.BUY);
        assertTrue(hangar.escortSide(WingmanSpec.Side.LEFT));
        assertTrue(hangar.undo());
        assertEquals(credits, campaign.credits());
        assertEquals(WingmanSpec.Side.LEFT, campaign.gear().escort().side());
        assertFalse(hangar.canUndo());
    }

    @Test
    void theSaveKeepsHimInItsEscortFieldThroughTheRoundTrip() throws SaveException {
        Campaign campaign = before(8, 3000);
        Hangar hangar = new Hangar(CATALOGUE, campaign);
        hangar.apply(LoadoutSlot.ESCORT, row(hangar, "Mortar", State.BUYABLE), Action.BUY);
        hangar.escortSide(WingmanSpec.Side.RIGHT);
        campaign.complete(CampaignTest.won("A", 80, 1000), 50, 0, 0, OptionalDouble.of(33));

        SaveGame save = campaign.save(Instant.EPOCH);
        String json = SaveFormat.write(save);

        assertEquals(
                new SaveGame.EscortSlot(
                        true,
                        "right",
                        Optional.of("mortar"),
                        List.of(new Fitted("mortar", 1), new Fitted("autocannon", 1)),
                        33),
                save.escort());
        assertFalse(save.loadout().containsKey(LoadoutSlot.ESCORT));
        assertTrue(json.contains("\"fitted\" : \"mortar\""), json);
        SaveGame read = SaveFormat.read(json);
        assertEquals(save, read);
        Campaign loaded = Campaign.load(CampaignTest.RULES, read);
        assertEquals(campaign.gear(), loaded.gear());
    }

    @Test
    void aNewCampaignsEscortIsNotHired() throws SaveException {
        SaveGame save = Campaign.start(CampaignTest.RULES, Difficulty.EASY).save(Instant.EPOCH);
        String json = SaveFormat.write(save);

        assertTrue(json.contains("\"hired\" : false"), json);
        assertTrue(json.contains("\"fitted\" : null"), json);
        assertEquals(save, SaveFormat.read(json));
        assertThrows(SaveException.class, () -> SaveFormat.read(json.replace("\"hired\" : false", "\"hired\" : true")));
    }

    /** A version 2 save (M4) has no escort: Rook is hired by the migration when it is past Level 07. */
    @Test
    void aVersion2SaveIsMigratedWithRookHiredFromLevel08() throws SaveException {
        for (int level : new int[] {7, 8, 9}) {
            ObjectNode tree = (ObjectNode) JsonMapper.builder()
                    .build()
                    .readTree(SaveFormat.write(before(level, 100).save(Instant.EPOCH)));
            tree.put("version", 2);
            tree.remove("escort");

            SaveGame migrated = SaveFormat.read(tree.toString());

            assertEquals(SaveFormat.VERSION, migrated.version());
            SaveGame.EscortSlot escort = migrated.escort();
            assertEquals(level >= 8, escort.hired(), "level " + level);
            assertEquals("left", escort.side());
            assertEquals(level >= 8 ? Optional.of("autocannon") : Optional.empty(), escort.fitted());
            assertEquals(level >= 8 ? List.of(new Fitted("autocannon", 1)) : List.of(), escort.guns());
            assertEquals(80, escort.armour());
            assertEquals(level >= 8, Campaign.load(CampaignTest.RULES, migrated).escortFlies(), "level " + level);
        }
    }

    @Test
    void aVersion3SaveWithoutItsEscortIsUnreadable() {
        String json = SaveFormat.write(before(9, 0).save(Instant.EPOCH));
        ObjectNode tree = (ObjectNode) JsonMapper.builder().build().readTree(json);
        tree.remove("escort");

        assertThrows(SaveException.class, () -> SaveFormat.read(tree.toString()));
    }

    @Test
    void heFliesFromLevel08ButNotInAnAct1Replay() {
        Campaign campaign = before(9, 0);
        Flight flight = Flight.of(CONTENT, CATALOGUE, campaign);
        assertTrue(flight.loadout().wingman().isPresent());
        assertEquals(WingmanSpec.Side.LEFT, flight.loadout().wingman().get().side());
        assertEquals("autocannon-pod", flight.loadout().wingman().get().gun().slug());

        SaveGame save = campaign.save(Instant.EPOCH);
        Campaign replay = Campaign.replay(CampaignTest.RULES, SaveSlots.Slot.AUTOSAVE, save, 3);
        assertFalse(Flight.of(CONTENT, CATALOGUE, replay).loadout().wingman().isPresent());
        Campaign act2Replay = Campaign.replay(CampaignTest.RULES, SaveSlots.Slot.AUTOSAVE, save, 8);
        assertTrue(Flight.of(CONTENT, CATALOGUE, act2Replay).loadout().wingman().isPresent());

        assertFalse(
                Flight.of(CONTENT, CATALOGUE, before(5, 0)).loadout().wingman().isPresent());
    }

    @Test
    void theEscortDebugOptionFliesHimOnAnyLevelOrKeepsHimHome() {
        Campaign act1 = DebugFit.startAt(CampaignTest.RULES, Difficulty.MEDIUM, 2);
        DebugFit.NONE.withEscort("rook:missiles:3,side=right").applyTo(act1, CATALOGUE);

        WingmanSpec rook =
                Flight.of(CONTENT, CATALOGUE, act1).loadout().wingman().orElseThrow();
        assertEquals(WingmanSpec.Side.RIGHT, rook.side());
        assertEquals("micro-missile-pod", rook.gun().slug());
        assertEquals(80, rook.armour());

        Campaign act2 = DebugFit.startAt(CampaignTest.RULES, Difficulty.MEDIUM, 9);
        assertTrue(Flight.of(CONTENT, CATALOGUE, act2).loadout().wingman().isPresent());
        DebugFit.NONE.withEscort("none").applyTo(act2, CATALOGUE);
        assertFalse(Flight.of(CONTENT, CATALOGUE, act2).loadout().wingman().isPresent());

        assertThrows(IllegalArgumentException.class, () -> DebugFit.NONE.withEscort("rook:missiles:6"));
        assertThrows(IllegalArgumentException.class, () -> DebugFit.NONE.withEscort("warden:cannon:1"));
        assertThrows(IllegalArgumentException.class, () -> DebugFit.NONE.withEscort("rook:missiles,side=up"));
        assertThrows(
                IllegalArgumentException.class,
                () -> DebugFit.NONE.withEscort("rook:laser:1").applyTo(act1, CATALOGUE));
    }

    @Test
    void theEscortDebugOptionIsNeverSavedNorKept() throws SaveException {
        // A Level 02 test run: Rook flies, but the gear (and so the autosave) never hires him.
        Campaign act1 = DebugFit.startAt(CampaignTest.RULES, Difficulty.MEDIUM, 2);
        SaveGame.EscortSlot before = act1.save(Instant.EPOCH).escort();
        DebugFit.NONE.withEscort("rook:missiles:3,side=right").applyTo(act1, CATALOGUE);
        assertTrue(act1.escortFlies());
        assertFalse(act1.gear().escort().hired());

        // The failure's autosave, the retry's, and the one after a won level.
        assertEquals(Campaign.Failure.MISSION_FAILED, act1.fail());
        assertEquals(
                before,
                SaveFormat.read(SaveFormat.write(act1.save(Instant.EPOCH))).escort());
        act1.retry();
        assertEquals(before, act1.save(Instant.EPOCH).escort());
        assertTrue(Flight.of(CONTENT, CATALOGUE, act1).loadout().wingman().isPresent(), "every attempt");
        act1.complete(CampaignTest.won("A", 80, 1000), 50, 0, 0, OptionalDouble.of(12));
        SaveGame save = act1.save(Instant.EPOCH);
        assertEquals(before, save.escort());
        assertFalse(save.loadout().containsKey(LoadoutSlot.ESCORT));
        assertFalse(Campaign.load(CampaignTest.RULES, save).escortFlies(), "a loaded Level 03 campaign");

        // From Level 08 on he is hired by the rules: the debug gun, side and armour stay out of the gear.
        Campaign act2 = DebugFit.startAt(CampaignTest.RULES, Difficulty.MEDIUM, 9);
        SaveGame hired = act2.save(Instant.EPOCH);
        DebugFit.NONE.withEscort("rook:missiles:3,side=right").applyTo(act2, CATALOGUE);
        assertEquals(
                "micro-missile-pod",
                Flight.of(CONTENT, CATALOGUE, act2)
                        .loadout()
                        .wingman()
                        .orElseThrow()
                        .gun()
                        .slug());
        act2.complete(CampaignTest.won("A", 80, 1000), 50, 0, 0, OptionalDouble.of(12));
        assertEquals(hired.escort(), act2.save(Instant.EPOCH).escort(), "his armour is the gear's, not 12");
    }
}
