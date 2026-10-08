package vanguard.game.hangar;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import vanguard.content.LevelData;
import vanguard.content.campaign.Campaign;
import vanguard.content.campaign.CampaignRules;
import vanguard.content.campaign.Catalogue;
import vanguard.content.campaign.Fitted;
import vanguard.content.campaign.Hangar;
import vanguard.content.campaign.Intel;
import vanguard.content.campaign.LoadoutSlot;
import vanguard.content.campaign.SaveFormat;
import vanguard.content.campaign.SaveGame;

/**
 * M5 part D (design/ui/hangar, Launch; user decision D9 = a): Level 10's {@code required: [rear]}
 * warns at launch at every sensor level, {@code NO REAR WEAPON FITTED}, until a rear-firing weapon is
 * in the rear slot; Rook's gun never silences it.
 */
class LaunchRearWarningTest {
    private static final Content CONTENT = ContentLoader.fromClasspath();
    private static final CampaignRules RULES = CampaignRules.of(CONTENT);
    private static final Catalogue CATALOGUE = Catalogue.of(CONTENT);
    private static final LevelData.ThreatProfile PROFILE =
            CONTENT.level(CONTENT.levelKey(10).orElseThrow()).threatProfile();
    private static final String WARNING = "NO REAR WEAPON FITTED";

    private static Intel intel(int sensor) {
        return new Intel(10, sensor, PROFILE, Map.of(), List.of(), List.of(), List.of(), 0, 200);
    }

    /** Before Level 10 with the starter loadout plus {@code extra}, Rook flying with {@code gun}. */
    private static HangarState state(Map<LoadoutSlot, Fitted> extra, String gun) {
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
        return new HangarState(new Hangar(CATALOGUE, campaign), true);
    }

    @Test
    void levelTenRequiresARearWeapon() {
        assertEquals(List.of(Hangar.REAR), PROFILE.requiredTraits());
        assertEquals(WARNING, HangarState.requiredWarning(Hangar.REAR));
        assertTrue(WARNING.length() <= "NO ANTI-GROUND SOURCE FITTED".length(), "fits Level 09's line");
    }

    @Test
    void withoutARearWeaponItWarnsAtEverySensorLevelWhateverRookFlies() {
        for (String gun : List.of("autocannon", "scatter", "missiles", "mortar")) {
            HangarState state = state(Map.of(), gun);
            for (int sensor = 0; sensor <= Hangar.MAX_SENSOR; sensor++) {
                List<String> warnings = state.launchWarnings(Optional.of(intel(sensor)));
                assertEquals(WARNING, warnings.getFirst(), gun + ", sensor L" + sensor);
                assertEquals(1, warnings.stream().filter(WARNING::equals).count(), "once");
            }
        }
    }

    @Test
    void aTailGunInTheRearSlotSilencesIt() {
        for (String rear : List.of("tail-gun", "fan-blaster", "proximity-mines")) {
            HangarState state = state(
                    Map.of(LoadoutSlot.REAR, new Fitted(rear, 1)),
                    RULES.escort().starterGun());
            for (int sensor = 0; sensor <= Hangar.MAX_SENSOR; sensor++) {
                assertTrue(
                        !state.launchWarnings(Optional.of(intel(sensor))).contains(WARNING),
                        rear + ", sensor L" + sensor);
            }
        }
    }

    @Test
    void aSideSplitterInTheRearSlotFiresSidewaysAndDoesNot() {
        HangarState state = state(
                Map.of(LoadoutSlot.REAR, new Fitted("side-splitter", 1)),
                RULES.escort().starterGun());
        assertEquals(WARNING, state.launchWarnings(Optional.of(intel(0))).getFirst());
    }
}
