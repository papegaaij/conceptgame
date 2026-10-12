package vanguard.content.campaign;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import vanguard.content.Content;
import vanguard.content.ContentLoader;
import vanguard.content.Difficulty;

/** The {@code --loadout} debug option's core parts (M5 part H, step H7b): generator, shield and armour plating. */
class DebugFitTest {
    private static final Content CONTENT = ContentLoader.fromClasspath();
    private static final CampaignRules RULES = CampaignRules.of(CONTENT);
    private static final Catalogue CATALOGUE = Catalogue.of(CONTENT);

    private static Campaign fitted(String spec) {
        Campaign campaign = DebugFit.startAt(RULES, Difficulty.MEDIUM, 14);
        DebugFit.parse(spec).applyTo(campaign, CATALOGUE);
        return campaign;
    }

    @Test
    void corePartsAreFittedByNameOrByTheirTier() {
        Campaign campaign = fitted("front=pulse-cannon:3,generator=mk-ii,shield=mk-iii:1,armor=composite-ii");
        assertEquals(new Fitted("Mk II \"Arc\"", 1), campaign.loadout().get(LoadoutSlot.GENERATOR));
        assertEquals(new Fitted("Mk III", 1), campaign.loadout().get(LoadoutSlot.SHIELD));
        assertEquals(new Fitted("Composite II", 1), campaign.loadout().get(LoadoutSlot.ARMOUR));
        assertEquals(new Fitted("pulse-cannon", 3), campaign.loadout().get(LoadoutSlot.FRONT));
        assertEquals(100, campaign.gear().armour(), 1e-9, "the plating at its full armour");

        Campaign tiers = fitted("generator=mk-iii-fusion,shield=1,armour=2");
        assertEquals(new Fitted("Mk III \"Fusion\"", 1), tiers.loadout().get(LoadoutSlot.GENERATOR));
        assertEquals(new Fitted("Mk II", 1), tiers.loadout().get(LoadoutSlot.SHIELD));
        assertEquals(new Fitted("Composite II", 1), tiers.loadout().get(LoadoutSlot.ARMOUR));
    }

    @Test
    void anUnknownCorePartOrALevelAboveOneIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> fitted("shield=mk-ix"));
        assertThrows(IllegalArgumentException.class, () -> fitted("armor=composite-ii:2"));
        assertThrows(IllegalArgumentException.class, () -> fitted("generator=99"));
        assertThrows(IllegalArgumentException.class, () -> DebugFit.parse("engine=standard"));
    }
}
