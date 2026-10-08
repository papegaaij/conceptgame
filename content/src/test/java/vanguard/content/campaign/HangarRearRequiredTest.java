package vanguard.content.campaign;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

/**
 * M5 part D (design/ui/hangar, Launch; user decision D9 = a): Level 10's required {@code rear} trait
 * comes only from the weapon fitted in the rear slot. Homing weapons pick targets ahead of the ship and
 * Rook fires only up the screen, so his gun never brings it, whatever its traits; a rear-slot weapon
 * without the trait (the Side Splitter fires sideways) does not either.
 */
class HangarRearRequiredTest {
    private static final Content CONTENT = ContentLoader.fromClasspath();
    private static final CampaignRules RULES = CampaignRules.of(CONTENT);
    private static final Catalogue CATALOGUE = Catalogue.of(CONTENT);
    private static final List<String> REAR = List.of(Hangar.REAR);

    /** A campaign before Level 10 with the starter loadout plus {@code extra}, Rook flying with {@code gun}. */
    private static Hangar hangar(Map<LoadoutSlot, Fitted> extra, String gun) {
        Map<LoadoutSlot, Fitted> loadout = new EnumMap<>(RULES.starterLoadout());
        loadout.putAll(extra);
        Campaign campaign = Campaign.load(
                RULES,
                new SaveGame(
                        SaveFormat.VERSION,
                        Instant.EPOCH,
                        0,
                        Difficulty.MEDIUM,
                        10,
                        0,
                        0,
                        loadout,
                        Map.of(),
                        List.of(),
                        Map.of(),
                        RULES.starterArmour(),
                        new SaveGame.EscortSlot(
                                true,
                                "left",
                                Optional.of(gun),
                                List.of(new Fitted(gun, 1)),
                                RULES.escort().maxArmour()),
                        Optional.empty(),
                        Map.of(),
                        List.of(),
                        List.of(),
                        new SaveGame.Stats(0, 0)));
        return new Hangar(CATALOGUE, campaign);
    }

    @Test
    void theStarterFlightBringsNoRearWhateverRookFlies() {
        for (String gun : List.of("autocannon", "scatter", "missiles", "mortar")) {
            Hangar hangar = hangar(Map.of(), gun);
            assertFalse(hangar.sourceTraits().contains(Hangar.REAR), gun);
            assertEquals(REAR, hangar.missingRequired(REAR), gun);
        }
    }

    @Test
    void everyRearFiringWeaponInTheRearSlotBringsIt() {
        for (String weapon : List.of("tail-gun", "fan-blaster", "proximity-mines")) {
            Hangar hangar = hangar(
                    Map.of(LoadoutSlot.REAR, new Fitted(weapon, 1)),
                    RULES.escort().starterGun());
            assertTrue(hangar.sourceTraits().contains(Hangar.REAR), weapon);
            assertEquals(List.of(), hangar.missingRequired(REAR), weapon);
        }
    }

    @Test
    void theSideSplitterInTheRearSlotFiresSidewaysAndDoesNot() {
        Hangar hangar = hangar(
                Map.of(LoadoutSlot.REAR, new Fitted("side-splitter", 1)),
                RULES.escort().starterGun());
        assertEquals(REAR, hangar.missingRequired(REAR));
    }

    @Test
    void levelTensIntelRequiresRearAtEverySensorLevel() {
        var profile = CONTENT.level(CONTENT.levelKey(10).orElseThrow()).threatProfile();
        assertTrue(profile.traits().contains(Hangar.REAR));
        for (int sensor = 0; sensor <= Hangar.MAX_SENSOR; sensor++) {
            Intel intel = new Intel(10, sensor, profile, Map.of(), List.of(), List.of(), List.of(), 0, 200);
            assertEquals(REAR, intel.requiredTraits(), "sensor L" + sensor);
        }
    }
}
