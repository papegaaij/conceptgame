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
import vanguard.content.LevelData;

/**
 * M5 part C (design/ui/hangar, Launch; user decision D7 = a): a level's {@code required} trait counts
 * every source of the next flight: a weapon with the trait in any slot, Rook's gun while he flies
 * (hired, fitted and not grounded), and for {@code anti-ground} an Airstrike or Smart Bomb fitted
 * with a charge; the intel knows the required traits at every sensor level.
 */
class HangarRequiredTraitTest {
    private static final Content CONTENT = ContentLoader.fromClasspath();
    private static final CampaignRules RULES = CampaignRules.of(CONTENT);
    private static final Catalogue CATALOGUE = Catalogue.of(CONTENT);
    private static final List<String> ANTI_GROUND = List.of(Hangar.ANTI_GROUND);

    /**
     * A campaign before Level 09 with the starter loadout plus {@code extra}, Rook hired with {@code
     * gun} at {@code rookArmour} (0: grounded) and {@code charges} of the fitted special.
     */
    private static Hangar hangar(Map<LoadoutSlot, Fitted> extra, String gun, double rookArmour, int charges) {
        Map<LoadoutSlot, Fitted> loadout = new EnumMap<>(RULES.starterLoadout());
        loadout.putAll(extra);
        Map<String, Integer> specials = extra.containsKey(LoadoutSlot.SPECIAL)
                ? Map.of(extra.get(LoadoutSlot.SPECIAL).item(), charges)
                : Map.of();
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
                        specials,
                        RULES.starterArmour(),
                        new SaveGame.EscortSlot(
                                true, "left", Optional.of(gun), List.of(new Fitted(gun, 1)), rookArmour),
                        Optional.empty(),
                        Map.of(),
                        List.of(),
                        List.of(),
                        new SaveGame.Stats(0, 0)));
        return new Hangar(CATALOGUE, campaign);
    }

    private static double rookFull() {
        return RULES.escort().maxArmour();
    }

    @Test
    void theStarterFlightWithRooksAutocannonBringsNoAntiGround() {
        Hangar hangar = hangar(Map.of(), RULES.escort().starterGun(), rookFull(), 0);
        assertFalse(hangar.sourceTraits().contains(Hangar.ANTI_GROUND));
        assertEquals(ANTI_GROUND, hangar.missingRequired(ANTI_GROUND));
    }

    @Test
    void aBombRackInAnySlotIsASource() {
        Hangar hangar = hangar(
                Map.of(LoadoutSlot.LEFT_WING, new Fitted("bomb-rack", 1)),
                RULES.escort().starterGun(),
                rookFull(),
                0);
        assertEquals(List.of(), hangar.missingRequired(ANTI_GROUND));
    }

    @Test
    void rooksMortarCountsWhileHeFliesNotWhileGrounded() {
        assertEquals(List.of(), hangar(Map.of(), "mortar", rookFull(), 0).missingRequired(ANTI_GROUND));
        assertEquals(ANTI_GROUND, hangar(Map.of(), "mortar", 0, 0).missingRequired(ANTI_GROUND), "grounded");
    }

    @Test
    void anAirstrikeOrASmartBombCountsWithACharge() {
        for (String special : Hangar.HARDENED_SPECIALS) {
            Map<LoadoutSlot, Fitted> fitted = Map.of(LoadoutSlot.SPECIAL, new Fitted(special, 1));
            assertEquals(
                    List.of(),
                    hangar(fitted, RULES.escort().starterGun(), rookFull(), 1).missingRequired(ANTI_GROUND),
                    special + " with a charge");
            assertEquals(
                    ANTI_GROUND,
                    hangar(fitted, RULES.escort().starterGun(), rookFull(), 0).missingRequired(ANTI_GROUND),
                    special + " without a charge");
        }
        // Only a weapon's trait counts for the other traits: the specials only for anti-ground.
        Map<LoadoutSlot, Fitted> airstrike = Map.of(LoadoutSlot.SPECIAL, new Fitted("Airstrike", 1));
        assertEquals(
                List.of("anti-sub"),
                hangar(airstrike, RULES.escort().starterGun(), rookFull(), 2).missingRequired(List.of("anti-sub")));
    }

    @Test
    void theIntelKnowsTheRequiredTraitsAtEverySensorLevel() {
        LevelData.ThreatProfile profile = new LevelData.ThreatProfile(
                "earth-megacity",
                List.of("ground", "air"),
                3,
                List.of("anti-ground", "spread"),
                List.of(),
                "none",
                Optional.empty(),
                Optional.of("DESTROY 6 HIVE NODES"),
                Map.of("none", "a", "l1", "b", "l2", "c", "l3", "d"),
                Optional.of(ANTI_GROUND));
        for (int sensor = 0; sensor <= Hangar.MAX_SENSOR; sensor++) {
            Intel intel = new Intel(9, sensor, profile, Map.of(), List.of(), List.of(), List.of(), 0, 200);
            assertEquals(ANTI_GROUND, intel.requiredTraits(), "sensor L" + sensor);
            assertEquals(
                    sensor >= Intel.Field.TRAITS.sensor(), !intel.markedTraits().isEmpty());
        }
        assertTrue(new Intel(
                        1,
                        3,
                        CONTENT.level("act-1-first-contact/level-01-break-at-dawn")
                                .threatProfile(),
                        Map.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        0,
                        100)
                .requiredTraits()
                .isEmpty());
    }
}
