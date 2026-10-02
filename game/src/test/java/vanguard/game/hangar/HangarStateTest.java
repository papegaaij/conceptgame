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
import vanguard.content.campaign.Intel;
import vanguard.content.campaign.LoadoutSlot;
import vanguard.content.campaign.SaveFormat;
import vanguard.content.campaign.SaveGame;
import vanguard.game.hangar.HangarState.Command;
import vanguard.game.hangar.HangarState.Focus;
import vanguard.game.hangar.HangarState.Outcome;

class HangarStateTest {
    private static final Content CONTENT = ContentLoader.fromClasspath();
    private static final CampaignRules RULES = CampaignRules.of(CONTENT);
    private static final Catalogue CATALOGUE = Catalogue.of(CONTENT);

    private static Campaign campaign(int level, int credits, double armour) {
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
                        Optional.empty(),
                        Map.of(),
                        List.of(),
                        List.of(),
                        new SaveGame.Stats(0, 0)));
    }

    private static HangarState state(Campaign campaign, boolean launchable) {
        return new HangarState(new Hangar(CATALOGUE, campaign), launchable);
    }

    @Test
    void theHangarOpensOnTheFrontGunsFittedItemAndItsUpgrade() {
        HangarState state = state(Campaign.start(RULES, Difficulty.MEDIUM), true);

        assertEquals(LoadoutSlot.FRONT, state.slot());
        assertEquals(Focus.SHOP, state.focus());
        assertEquals("Pulse Cannon", state.selected().orElseThrow().item().name());
        assertEquals(Action.UPGRADE, state.choice().orElseThrow().action());
    }

    @Test
    void confirmUpgradesAndTheUndoCommandReturnsIt() {
        Campaign campaign = Campaign.start(RULES, Difficulty.MEDIUM);
        HangarState state = state(campaign, true);

        assertEquals(Outcome.DONE, state.confirm());
        assertEquals(new Fitted("pulse-cannon", 2), campaign.loadout().get(LoadoutSlot.FRONT));
        assertEquals("PULSE CANNON UPGRADED TO L2", state.message());

        assertEquals(Outcome.MOVED, state.up());
        assertEquals(Focus.COMMANDS, state.focus());
        assertEquals(Command.LAUNCH, state.command());
        while (state.command() != Command.UNDO) {
            state.left();
        }
        assertEquals(Outcome.DONE, state.confirm());
        assertEquals(300, campaign.credits());
        assertFalse(state.enabled(Command.UNDO));
    }

    @Test
    void aRefusedChoiceSaysWhy() {
        HangarState state = state(Campaign.start(RULES, Difficulty.MEDIUM), true);
        state.confirm();

        assertEquals(Outcome.REFUSED, state.confirm(), "the L3 upgrade costs 600");
        assertEquals("NOT ENOUGH CREDITS", state.message());
    }

    @Test
    void theTabsCycleThroughTheSlotsAndResetTheRow() {
        HangarState state = state(campaign(2, 1000, 60), true);
        state.down();

        assertEquals(Outcome.MOVED, state.nextSlot());

        assertEquals(LoadoutSlot.LEFT_WING, state.slot());
        assertEquals(0, state.row());
        state.previousSlot();
        state.previousSlot();
        assertEquals(LoadoutSlot.UTILITY_2, state.slot(), "the tabs wrap");
    }

    @Test
    void sellingAsksFirstAndSellsOnTheAnswer() {
        Campaign campaign = campaign(2, 1000, 60);
        HangarState state = state(campaign, true);
        state.nextSlot();
        assertEquals(Outcome.DONE, state.confirm(), "buys the Autocannon Pod");
        assertEquals(500, campaign.credits());
        while (state.choice().orElseThrow().action() != Action.SELL) {
            state.right();
        }

        assertEquals(Outcome.SELL, state.confirm());
        assertEquals(500, campaign.credits(), "nothing is sold before the answer");
        assertEquals(Outcome.DONE, state.sell());

        assertEquals(800, campaign.credits());
        assertEquals(Optional.empty(), Optional.ofNullable(campaign.loadout().get(LoadoutSlot.LEFT_WING)));
    }

    @Test
    void theRepairPanelStartsOnWhatTheCreditsRepairAndBackClosesIt() {
        Campaign campaign = campaign(2, 100, 35);
        HangarState state = state(campaign, true);
        state.up();
        while (state.command() != Command.REPAIR) {
            state.left();
        }

        assertEquals(Outcome.MOVED, state.confirm());
        assertEquals(Focus.REPAIR, state.focus());
        assertEquals(20, state.repairPoints(), "100 credits at 5 a point");
        state.left();
        state.down();
        assertEquals(9, state.repairPoints());
        assertTrue(state.back());
        assertEquals(Focus.COMMANDS, state.focus());
        state.confirm();
        state.down();
        state.down();
        assertEquals(1, state.repairPoints(), "at least one point");
        state.up();
        assertEquals(Outcome.DONE, state.confirm());
        assertEquals(46, campaign.armour());
        assertEquals(45, campaign.credits());
    }

    @Test
    void launchIsOnlyOfferedForABuiltLevelAndWarnsAsTheDocAsks() {
        HangarState unbuilt = state(campaign(2, 0, 60), false);
        unbuilt.up();
        assertEquals(Command.SAVE, unbuilt.command(), "the cursor skips the disabled launch");
        assertEquals(Outcome.SAVE, unbuilt.confirm());

        Campaign damaged = Campaign.start(RULES, Difficulty.MEDIUM);
        HangarState state = state(damaged, true);
        Intel intel = Intel.of(CONTENT, "act-1-first-contact/level-01-break-at-dawn", 0);
        assertEquals(List.of(), state.launchWarnings(Optional.of(intel)), "the Pulse Cannon is a forward weapon");
        state.up();
        assertEquals(Outcome.LAUNCH, state.confirm());
        assertEquals(
                List.of("ARMOUR BELOW 50 %"), state(campaign(1, 0, 29), true).launchWarnings(Optional.of(intel)));
    }
}
