package vanguard.game.audio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import vanguard.content.Tier;
import vanguard.content.campaign.Hangar;
import vanguard.sim.PickupType;

/**
 * The sound effects against their files and design/audio/sfx: every {@link Sfx} file is in the
 * assets, and every chosen sound of the README's Concept art tables is either played (an {@link Sfx}
 * entry) or listed with its reason under "Chosen sounds the game does not play"; the game plays no
 * other sound but a proposed one of an open concept round, provisionally until the user's choice.
 */
class SfxFilesTest {
    private static final Path ASSETS = Path.of(System.getProperty("vanguard.assetsDir", "../assets"));
    private static final Path README = ASSETS.resolveSibling("design/audio/sfx/README.md");
    private static final Pattern CHOSEN_ROW =
            Pattern.compile("^\\| \\[concept/([^/\\]]+\\.ogg)\\]\\(concept/[^)]+\\) \\|.*\\| (chosen[^|]*) \\|$");
    private static final Pattern PROPOSED_ROW =
            Pattern.compile("^\\| \\[concept/([^/\\]]+\\.ogg)\\]\\(concept/[^)]+\\) \\|.*\\| proposed \\|$");
    private static final Pattern UNPLAYED_ROW =
            Pattern.compile("^\\| \\[([^\\]]+\\.ogg)\\]\\(concept/[^)]+\\) \\| (.*) \\|$");
    private static final String UNPLAYED_HEADING = "### Chosen sounds the game does not play";

    @Test
    void everyEffectsFileIsInTheAssets() {
        for (Sfx sfx : Sfx.values()) {
            assertTrue(Files.isRegularFile(ASSETS.resolve(sfx.path())), sfx + ": " + sfx.path());
        }
    }

    @Test
    void everyChosenSoundIsPlayedOrListedWithItsReason() throws IOException {
        List<String> lines = Files.readAllLines(README);
        List<String> chosen = new ArrayList<>();
        Set<String> proposed = new HashSet<>();
        for (String line : lines) {
            Matcher row = CHOSEN_ROW.matcher(line);
            if (row.matches()) {
                chosen.add(row.group(1));
            }
            Matcher open = PROPOSED_ROW.matcher(line);
            if (open.matches()) {
                proposed.add(open.group(1));
            }
        }
        Map<String, String> unplayed = unplayed(lines);
        Set<String> played = Arrays.stream(Sfx.values())
                .map(Sfx::path)
                .filter(path -> path.startsWith("sfx/"))
                .map(path -> path.substring("sfx/".length()))
                .collect(Collectors.toSet());

        assertTrue(chosen.size() > 100, "the Concept art tables were read: " + chosen.size());
        for (String file : chosen) {
            assertTrue(played.contains(file) || unplayed.containsKey(file), file + " is neither played nor listed");
            assertFalse(played.contains(file) && unplayed.containsKey(file), file + " is played and listed as not");
        }
        for (var entry : unplayed.entrySet()) {
            assertTrue(chosen.contains(entry.getKey()), entry.getKey() + " is listed but not a chosen sound");
            assertFalse(entry.getValue().isBlank(), entry.getKey() + " has no reason");
        }
        for (String file : played) {
            assertTrue(
                    chosen.contains(file) || proposed.contains(file),
                    file + " is played but neither a chosen nor a proposed sound of the README");
        }
    }

    /** The rows of the README's table of chosen sounds the game does not play: file to reason. */
    private static Map<String, String> unplayed(List<String> lines) {
        Map<String, String> found = new LinkedHashMap<>();
        boolean inSection = false;
        for (String line : lines) {
            if (line.startsWith("#")) {
                inSection = line.equals(UNPLAYED_HEADING);
                continue;
            }
            Matcher row = UNPLAYED_ROW.matcher(line);
            if (inSection && row.matches()) {
                found.put(row.group(1), row.group(2).trim());
            }
        }
        assertFalse(found.isEmpty(), "no table under " + UNPLAYED_HEADING);
        return found;
    }

    @Test
    void anOggFilesLengthIsReadFromItsLastPage() throws IOException {
        long nanos = SfxBank.oggNanos(Files.readAllBytes(ASSETS.resolve(Sfx.EXPLOSION_HUGE_A.path())));
        assertEquals(6.0, nanos / 1e9, 0.01);
        assertEquals(SfxBank.UNKNOWN_NANOS, SfxBank.oggNanos(new byte[] {1, 2, 3}));
    }

    @Test
    void enemiesExplodeOnTheirSizeRung() {
        for (Tier tier : Tier.values()) {
            Sfx a = Sfx.explosion(tier, false);
            Sfx b = Sfx.explosion(tier, true);
            assertFalse(a == b, tier + " alternates two files");
            assertEquals(Sfx.Priority.EXPLOSION, a.priority());
            assertTrue(a.name().contains(tier.name()), tier + " plays " + a);
        }
    }

    @Test
    void everySettingKeyPlaysItsAmbienceLoop() {
        assertEquals(Sfx.AMBIENCE_ORBIT, Sfx.ambience("earth-orbit"));
        assertEquals(Sfx.AMBIENCE_LUNA, Sfx.ambience("luna"));
        // M5 part B: Level 08's megacity.
        assertEquals(Sfx.AMBIENCE_CITY, Sfx.ambience("earth-megacity"));
        assertEquals(Sfx.Priority.AMBIENCE, Sfx.ambience("earth-megacity").priority());
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> Sfx.ambience("europa"));
    }

    @Test
    void everyShopActionHasItsSound() {
        assertEquals(Sfx.SHOP_BUY, Sfx.shop(Hangar.Action.BUY));
        assertEquals(Sfx.SHOP_BUY, Sfx.shop(Hangar.Action.BUY_CHARGE));
        assertEquals(Sfx.SHOP_SELL, Sfx.shop(Hangar.Action.SELL));
        assertEquals(Sfx.EQUIP, Sfx.shop(Hangar.Action.FIT));
        assertEquals(Sfx.EQUIP, Sfx.shop(Hangar.Action.UNFIT));
        assertEquals(Sfx.UPGRADE, Sfx.shop(Hangar.Action.UPGRADE));
    }

    @Test
    void everyPickupHasItsOwnSoundButTheLargeSalvageAndTheCrate() {
        for (PickupType type : PickupType.values()) {
            assertEquals(Sfx.Priority.PICKUP, FlightSounds.pickupSound(type).priority(), type.name());
        }
        assertEquals(
                PickupType.values().length - 1,
                Arrays.stream(PickupType.values())
                        .map(FlightSounds::pickupSound)
                        .distinct()
                        .count());
        assertEquals(Sfx.SALVAGE_MEDIUM, FlightSounds.pickupSound(PickupType.MEDIUM_SALVAGE));
        assertEquals(Sfx.SPECIAL_CHARGE, FlightSounds.pickupSound(PickupType.SPECIAL_CHARGE));
        assertEquals(Sfx.DATA_CORE, FlightSounds.pickupSound(PickupType.DATA_CORE));
    }

    @Test
    void theLowArmourWarningBeepsSlowlyAtThirtyAndFastAtFifteenPercent() {
        assertEquals(0, FlightSounds.beepPeriod(100, 100));
        assertEquals(0, FlightSounds.beepPeriod(31, 100));
        int slow = FlightSounds.beepPeriod(30, 100);
        int fast = FlightSounds.beepPeriod(15, 100);
        assertEquals(vanguard.sim.SimStep.ticks(FlightSounds.SLOW_BEEP_SECONDS), slow);
        assertEquals(vanguard.sim.SimStep.ticks(FlightSounds.FAST_BEEP_SECONDS), fast);
        assertEquals(slow, FlightSounds.beepPeriod(16, 100));
        assertEquals(fast, FlightSounds.beepPeriod(1, 100));
        assertEquals(0, FlightSounds.beepPeriod(0, 100), "a wreck does not beep");
        assertTrue(fast < slow);
    }

    @Test
    void theBeepsChangeAtTheHudsLowArmourThresholds() {
        for (int armour = 1; armour <= 100; armour++) {
            int period = FlightSounds.beepPeriod(armour, 100);
            int expected =
                    switch (vanguard.game.level.LowArmour.of(armour, 100)) {
                        case NONE -> 0;
                        case LOW -> vanguard.sim.SimStep.ticks(FlightSounds.SLOW_BEEP_SECONDS);
                        case CRITICAL -> vanguard.sim.SimStep.ticks(FlightSounds.FAST_BEEP_SECONDS);
                    };
            assertEquals(expected, period, "armour " + armour);
        }
    }

    @Test
    void aMediumBulletsShotSoundsHeavy() {
        // The ENEMY_FIRED value is the bullet's damage: small bullets deal 1 to 5, medium ones 6.
        assertFalse(FlightSounds.heavyShot(0));
        assertFalse(FlightSounds.heavyShot(5));
        assertTrue(FlightSounds.heavyShot(6));
        assertTrue(FlightSounds.heavyShot(12));
    }
}
