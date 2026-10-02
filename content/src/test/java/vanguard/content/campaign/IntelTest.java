package vanguard.content.campaign;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import vanguard.content.Content;
import vanguard.content.ContentLoader;
import vanguard.content.LevelData.Entry;
import vanguard.content.campaign.Intel.Field;

class IntelTest {
    private static final Content CONTENT = ContentLoader.fromClasspath();
    private static final String LEVEL_01 = "act-1-first-contact/level-01-break-at-dawn";

    @Test
    void withoutASensorSuiteLevel01ShowsTheSettingTheLayersAndTheMainDirection() {
        Intel intel = Intel.of(CONTENT, LEVEL_01, 0);

        assertEquals("Earth orbit", intel.profile().setting());
        assertEquals(List.of("air"), intel.profile().layers());
        assertEquals(Entry.FRONT, intel.mainDirection());
        assertTrue(intel.shows(Field.MAIN_DIRECTION));
        assertFalse(intel.shows(Field.DENSITY));
        assertEquals(Optional.of("Our scans are patchy, Lancer. Small, fast, lots of them."), intel.varga());
        assertEquals(List.of(), intel.markedTraits(), "the shop marks traits from sensor L3 on");
    }

    @Test
    void theDirectionsAreTheEnemiesSharesAsInTheThreatProfile() {
        Intel intel = Intel.of(CONTENT, LEVEL_01, 1);

        assertEquals(Map.of(Entry.FRONT, 71, Entry.SIDES, 23, Entry.REAR, 6), intel.directions());
        assertTrue(intel.shows(Field.DENSITY));
        assertEquals(1, intel.profile().density());
    }

    @Test
    void theHigherSensorLevelsAddEnemiesTraitsWavesAndSecrets() {
        Intel intel = Intel.of(CONTENT, LEVEL_01, 3);

        assertEquals(List.of("Skitter", "Needler"), intel.enemies());
        assertEquals(List.of("forward"), intel.markedTraits());
        assertEquals(1, intel.secrets());
        assertEquals(
                Optional.of("Our scans are patchy, Lancer. Small, fast, lots of them."),
                intel.varga(),
                "the line of the highest level at or below the sensor's");
    }
}
