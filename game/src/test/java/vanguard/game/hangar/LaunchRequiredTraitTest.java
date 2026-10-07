package vanguard.game.hangar;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
 * M5 part C (design/ui/hangar, Launch; user decision D7 = a): a level's {@code required} trait (Level
 * 09's {@code anti-ground}) warns at launch at every sensor level when no source of the flight brings
 * it; a recommended trait that is not required keeps the sensor-L3 rule, and a required one is not
 * warned about twice.
 */
class LaunchRequiredTraitTest {
    private static final Content CONTENT = ContentLoader.fromClasspath();
    private static final CampaignRules RULES = CampaignRules.of(CONTENT);
    private static final Catalogue CATALOGUE = Catalogue.of(CONTENT);

    /** Level 09's profile as its data marks it: {@code anti-ground} required, {@code spread} recommended. */
    private static final LevelData.ThreatProfile PROFILE = new LevelData.ThreatProfile(
            "earth-megacity",
            List.of("ground", "air"),
            3,
            List.of("anti-ground", "spread"),
            List.of(),
            "none",
            Optional.empty(),
            Optional.of("DESTROY 6 HIVE NODES"),
            Map.of("none", "a", "l1", "b", "l2", "c", "l3", "d"),
            Optional.of(List.of("anti-ground")));

    private static Intel intel(int sensor) {
        return new Intel(9, sensor, PROFILE, Map.of(), List.of(), List.of(), List.of(), 0, 200);
    }

    /** Before Level 09 with the starter loadout plus {@code extra}, Rook flying with {@code gun}. */
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
                        9,
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
    void aMissingRequiredTraitWarnsAtEverySensorLevel() {
        HangarState starter = state(Map.of(), RULES.escort().starterGun());
        for (int sensor = 0; sensor < Intel.Field.TRAITS.sensor(); sensor++) {
            assertEquals(
                    List.of("NO ANTI-GROUND SOURCE FITTED"),
                    starter.launchWarnings(Optional.of(intel(sensor))),
                    "sensor L" + sensor);
        }
        // At L3 the recommended spread warns too, the required trait only once.
        assertEquals(
                List.of("NO ANTI-GROUND SOURCE FITTED", "NO SPREAD WEAPON FITTED"),
                starter.launchWarnings(Optional.of(intel(3))));
    }

    @Test
    void rooksMortarOrABombRackSilencesIt() {
        assertEquals(List.of(), state(Map.of(), "mortar").launchWarnings(Optional.of(intel(0))));
        assertEquals(
                List.of(),
                state(
                                Map.of(LoadoutSlot.LEFT_WING, new Fitted("bomb-rack", 1)),
                                RULES.escort().starterGun())
                        .launchWarnings(Optional.of(intel(1))));
    }

    @Test
    void theWarningFitsTheSameLineAsAWeaponsWarning() {
        assertEquals("NO ANTI-GROUND SOURCE FITTED", HangarState.requiredWarning("anti-ground"));
        assertEquals(
                "NO ANTI-GROUND WEAPON FITTED".length(),
                HangarState.requiredWarning("anti-ground").length());
    }
}
