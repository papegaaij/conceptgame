package vanguard.game.hangar;

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
import vanguard.content.campaign.Campaign;
import vanguard.content.campaign.CampaignRules;
import vanguard.content.campaign.Catalogue;
import vanguard.content.campaign.Fitted;
import vanguard.content.campaign.Hangar;
import vanguard.content.campaign.Hangar.Action;
import vanguard.content.campaign.Hangar.State;
import vanguard.content.campaign.LoadoutSlot;
import vanguard.content.campaign.SaveFormat;
import vanguard.content.campaign.SaveGame;
import vanguard.game.hangar.HangarState.Command;
import vanguard.game.hangar.HangarState.Focus;
import vanguard.game.hangar.HangarState.Outcome;
import vanguard.game.hangar.HangarState.RepairLine;
import vanguard.sim.Armament;
import vanguard.sim.WingmanSpec;

/** The hangar's escort (design/ui/hangar, Escort, M5 part A): the slot, his guns, his side, his repair line, the launch warnings. */
class HangarEscortTest {
    private static final Content CONTENT = ContentLoader.fromClasspath();
    private static final CampaignRules RULES = CampaignRules.of(CONTENT);
    private static final Catalogue CATALOGUE = Catalogue.of(CONTENT);
    /** The starter plating's full armour. */
    private static final double FULL = RULES.starterArmour();

    /** A campaign before {@code level} with Rook hired (from Level 08) at {@code rookArmour}. */
    private static Campaign campaign(int level, int credits, double armour, double rookArmour) {
        boolean hired = level >= RULES.escort().joins();
        String gun = RULES.escort().starterGun();
        return Campaign.load(
                RULES,
                new SaveGame(
                        SaveFormat.VERSION,
                        Instant.EPOCH,
                        0,
                        Difficulty.MEDIUM,
                        level,
                        credits,
                        0,
                        RULES.starterLoadout(),
                        Map.of(),
                        List.of(),
                        Map.of(),
                        armour,
                        hired
                                ? new SaveGame.EscortSlot(
                                        true, "left", Optional.of(gun), List.of(new Fitted(gun, 1)), rookArmour)
                                : SaveGame.EscortSlot.NOT_HIRED,
                        Optional.empty(),
                        Map.of(),
                        List.of(),
                        List.of(),
                        new SaveGame.Stats(0, 0)));
    }

    private static HangarState state(Campaign campaign) {
        return new HangarState(new Hangar(CATALOGUE, campaign), true);
    }

    private static HangarState atEscort(Campaign campaign) {
        HangarState state = state(campaign);
        while (state.slot() != LoadoutSlot.ESCORT) {
            assertEquals(Outcome.MOVED, state.nextSlot());
        }
        return state;
    }

    @Test
    void theEscortIsATabOnlyOnceRookIsHired() {
        assertFalse(state(campaign(7, 1000, FULL, 80)).slots().contains(LoadoutSlot.ESCORT));
        HangarState state = state(campaign(8, 1000, FULL, 80));
        List<LoadoutSlot> slots = state.slots();

        assertEquals(slots.indexOf(LoadoutSlot.REAR) + 1, slots.indexOf(LoadoutSlot.ESCORT), "after the rear mount");
        assertEquals(HangarState.SLOTS.size() + 1, slots.size());
    }

    @Test
    void theEscortShopListsHisFourGunsAndTheSideRow() {
        HangarState state = atEscort(campaign(8, 1000, FULL, 80));

        assertEquals(4, state.rows().size());
        assertEquals(5, state.rowCount());
        assertEquals(State.FITTED, state.rows().getFirst().state());
        assertEquals("autocannon", state.rows().getFirst().item().id());
        assertTrue(state.rows().stream().skip(1).allMatch(offer -> offer.state() == State.BUYABLE));
        assertTrue(state.rows().stream().allMatch(offer -> offer.item().draw(offer.level()) == 0), "draw 0");
        for (int i = 0; i < 4; i++) {
            state.down();
        }
        assertTrue(state.sideSelected());
        assertTrue(state.selected().isEmpty());
        assertEquals(Outcome.NONE, state.down(), "the side row is the last");
    }

    @Test
    void leftAndRightSetHisSideOnTheSideRow() {
        Campaign campaign = campaign(8, 1000, FULL, 80);
        HangarState state = atEscort(campaign);
        for (int i = 0; i < 4; i++) {
            state.down();
        }

        assertEquals(WingmanSpec.Side.LEFT, state.escortSide());
        assertEquals(Outcome.NONE, state.left(), "already left");
        assertEquals(Outcome.MOVED, state.right());
        assertEquals(WingmanSpec.Side.RIGHT, campaign.gear().escort().side());
        assertEquals("ROOK FLIES ON YOUR RIGHT", state.message());
        state.confirm();
        assertEquals(WingmanSpec.Side.LEFT, campaign.gear().escort().side(), "confirm toggles it");
    }

    @Test
    void hisGunsAreBoughtFittedUpgradedAndSoldLikeAFrontGunsAndUndone() {
        Campaign campaign = campaign(8, 5000, FULL, 80);
        HangarState state = atEscort(campaign);
        state.down();
        String bought = state.selected().orElseThrow().item().id();
        int price = state.selected().orElseThrow().item().price();
        assertEquals(Action.BUY, state.choice().orElseThrow().action());

        assertEquals(Outcome.DONE, state.confirm());
        assertEquals(5000 - price, campaign.credits());
        assertEquals(bought, campaign.loadout().get(LoadoutSlot.ESCORT).item(), "fitted at once: no power drawn");
        assertEquals(Action.UPGRADE, state.choice().orElseThrow().action());
        assertEquals(Outcome.DONE, state.confirm());
        assertEquals(2, campaign.loadout().get(LoadoutSlot.ESCORT).level());

        assertTrue(state.hangar().undo());
        assertTrue(state.hangar().undo());
        assertEquals(5000, campaign.credits(), "the visit's undo returns them for 100 %");
        assertEquals("autocannon", campaign.loadout().get(LoadoutSlot.ESCORT).item());
    }

    @Test
    void theFittedGunCannotBeSold() {
        HangarState state = atEscort(campaign(8, 5000, FULL, 80));

        assertTrue(state.rows().getFirst().choice(Action.SELL).isEmpty());
        assertTrue(state.rows().getFirst().choice(Action.UNFIT).isEmpty());
    }

    @Test
    void theRepairPanelHasAShipAndARookLine() {
        Campaign campaign = campaign(8, 1000, FULL, 30);
        HangarState state = state(campaign);
        assertTrue(state.enabled(Command.REPAIR), "Rook's missing points alone open the repair");
        state.up();
        while (state.command() != Command.REPAIR) {
            state.left();
        }

        assertEquals(Outcome.MOVED, state.confirm());
        assertEquals(Focus.REPAIR, state.focus());
        assertEquals(RepairLine.ROOK, state.repairLine(), "the ship lacks nothing");
        assertEquals(50, state.repairPoints());
        assertEquals(Outcome.NONE, state.nextSlot(), "the ship's line has nothing to repair");
        state.left();
        assertEquals(Outcome.DONE, state.confirm());
        assertEquals(79, campaign.gear().escort().armour(), 1e-9);
        assertEquals(1000 - 49 * state.hangar().repairCost(), campaign.credits());
        assertTrue(state.message().startsWith("REPAIRED ROOK: 49 POINTS"));
    }

    @Test
    void theTabsSwitchTheRepairLines() {
        HangarState state = state(campaign(8, 1000, FULL / 2, 70));
        state.up();
        while (state.command() != Command.REPAIR) {
            state.left();
        }
        state.confirm();

        assertEquals(RepairLine.SHIP, state.repairLine());
        assertEquals(Outcome.MOVED, state.nextSlot());
        assertEquals(RepairLine.ROOK, state.repairLine());
        assertEquals(10, state.repairPoints());
        assertEquals(Outcome.MOVED, state.previousSlot());
        assertEquals(RepairLine.SHIP, state.repairLine());
    }

    @Test
    void theLaunchWarnsAboutRooksArmourBelowHalfAndAGroundedRook() {
        assertEquals(List.of(), state(campaign(8, 0, FULL, 40)).launchWarnings(Optional.empty()), "40 of 80 is half");
        assertEquals(
                List.of("ROOK'S ARMOUR 34/80"), state(campaign(8, 0, FULL, 34)).launchWarnings(Optional.empty()));
        assertEquals(
                List.of("ARMOUR BELOW 50 %", HangarState.ROOK_GROUNDED),
                state(campaign(8, 0, FULL / 4, 0)).launchWarnings(Optional.empty()));
    }

    @Test
    void aGroundedRookFliesAgainAfterAnyRepair() {
        Campaign campaign = campaign(8, 1000, FULL, 0);
        assertTrue(campaign.escortGrounded());
        HangarState state = state(campaign);
        state.up();
        while (state.command() != Command.REPAIR) {
            state.left();
        }
        state.confirm();
        for (int i = 0; i < 100; i++) {
            state.left();
        }
        assertEquals(1, state.repairPoints());
        state.confirm();

        assertFalse(campaign.escortGrounded());
        assertTrue(campaign.escortFlies());
    }

    @Test
    void theTestFireShowsHisGunFromTheFront() {
        HangarState state = atEscort(campaign(8, 5000, FULL, 80));
        state.down();
        var offer = state.selected().orElseThrow();
        TestFire.Shown shown =
                TestFire.of(CONTENT, LoadoutSlot.ESCORT, offer.item().id(), 1).orElseThrow();
        String base = CONTENT.wingmen().guns().gun(offer.item().id()).base();

        assertEquals(base, shown.weapon());
        assertEquals(Armament.Slot.FRONT, shown.slot());
        assertEquals(Optional.of(offer.item().id()), shown.escortGun());
        TestFire fire = new TestFire(CONTENT, shown);
        int shots = 0;
        for (int i = 0; i < 300; i++) {
            fire.step();
            shots = Math.max(shots, fire.sortie().shotCount());
        }
        assertTrue(shots > 0, "his gun fires in the box");
    }
}
