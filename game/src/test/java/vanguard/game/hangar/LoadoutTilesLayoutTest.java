package vanguard.game.hangar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import vanguard.content.ContentLoader;
import vanguard.content.campaign.Catalogue;
import vanguard.content.campaign.Catalogue.Item;
import vanguard.content.campaign.ItemKind;

/**
 * Every part a module tile can show fits the tile: its name in the 8 px label font is no wider
 * than the 48 px tile, and the parts of Acts 1–2 (unlocked by Level 14) get there by their model
 * name or an abbreviation, never by the fallback that cuts a name mid-word.
 */
class LoadoutTilesLayoutTest {
    private static final Catalogue CATALOGUE = Catalogue.of(ContentLoader.fromClasspath());
    private static final List<ItemKind> TILE_KINDS = List.of(
            ItemKind.GENERATOR, ItemKind.SHIELD, ItemKind.PLATING, ItemKind.ENGINE, ItemKind.SPECIAL, ItemKind.UTILITY);
    /** The label font's advance. */
    private static final int LABEL_ADVANCE = 8;

    private static final int LAST_ACT_2_LEVEL = 14;

    @Test
    void everyTileNameFitsItsTile() {
        assertEquals(LoadoutPanel.TILE_WIDTH, Names.TILE_CHARS * LABEL_ADVANCE);
        for (ItemKind kind : TILE_KINDS) {
            for (Item item : CATALOGUE.items(kind)) {
                String tile = Names.tile(item.name());
                assertTrue(
                        tile.length() * LABEL_ADVANCE <= LoadoutPanel.TILE_WIDTH,
                        () -> item.name() + " is " + tile + ", wider than its tile");
                assertFalse(tile.isBlank(), () -> item.name() + " has no tile name");
            }
        }
    }

    @Test
    void actOneAndTwoNamesAreNotCut() {
        for (ItemKind kind : TILE_KINDS) {
            for (Item item : CATALOGUE.items(kind)) {
                if (item.unlock() > LAST_ACT_2_LEVEL) {
                    continue;
                }
                String abbreviated = Names.abbreviated(item.name());
                assertTrue(
                        abbreviated.length() <= Names.TILE_CHARS,
                        () -> item.name() + " needs the fallback cut: " + abbreviated);
                assertEquals(abbreviated, Names.tile(item.name()));
            }
        }
    }

    @Test
    void theTilesAbbreviations() {
        assertEquals("MK II", Names.tile("Mk II \"Arc\""));
        assertEquals("STD", Names.tile("Standard"));
        assertEquals("CMP I", Names.tile("Composite I"));
        assertEquals("CMP II", Names.tile("Composite II"));
        assertEquals("CMPIII", Names.tile("Composite III"));
        assertEquals("STRIKE", Names.tile("Airstrike"));
        assertEquals("S-BOMB", Names.tile("Smart Bomb"));
        assertEquals("FLARES", Names.tile("Decoy Flares"));
        assertEquals("SENSOR", Names.tile("Sensor suite"));
        assertEquals("MAGNET", Names.tile("Pickup magnet"));
        assertEquals("TARGET", Names.tile("Targeting computer"));
        assertEquals("SALVGE", Names.tile("Salvage scanner"));
        assertEquals("EVASIV", Names.tile("Evasive thrusters"));
    }

    @Test
    void theCalloutsKeepTheWeaponNames() {
        assertEquals("MICRO-MSL", Names.callout("Micro-missile Pod"));
        assertEquals("MK II", Names.callout("Mk II \"Arc\""));
    }
}
