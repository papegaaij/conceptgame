package vanguard.game.settings;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class GameplaySettingsTest {
    @Test
    void briefingsTypeAtTwiceTheRadioSpeed() {
        assertEquals(60, GameplaySettings.defaults().briefingTextSpeed());
        assertEquals(20, GameplaySettings.defaults().withTextSpeed(10).briefingTextSpeed());
    }
}
