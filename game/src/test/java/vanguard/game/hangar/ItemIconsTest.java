package vanguard.game.hangar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import vanguard.content.ContentLoader;
import vanguard.content.campaign.Catalogue;
import vanguard.content.campaign.ItemKind;

class ItemIconsTest {
    private static final Catalogue CATALOGUE = Catalogue.of(ContentLoader.fromClasspath());
    private static final Path ICONS =
            Path.of(System.getProperty("vanguard.assetsDir", "../assets"), "sprites", "icons");

    @Test
    void namesAreTheWeaponSlugOrTheKindAndTheNameAsASlug() {
        assertEquals("pulse-cannon", ItemIcons.name(CATALOGUE.item(ItemKind.FRONT, "pulse-cannon")));
        assertEquals("generator-mk-ii-arc", ItemIcons.name(CATALOGUE.item(ItemKind.GENERATOR, "Mk II \"Arc\"")));
        assertEquals("utility-pickup-magnet", ItemIcons.name(CATALOGUE.item(ItemKind.UTILITY, "Pickup magnet")));
    }

    /** tools/art/icons.py writes both sizes for every item of the catalogue, under the names the game derives. */
    @Test
    void everyItemOfTheCatalogueHasBothIcons() {
        for (ItemKind kind : ItemKind.values()) {
            for (var item : CATALOGUE.items(kind)) {
                String name = ItemIcons.name(item);
                assertTrue(Files.isRegularFile(ICONS.resolve(name + ".png")), name);
                assertTrue(Files.isRegularFile(ICONS.resolve(name + "-large.png")), name + "-large");
            }
        }
    }
}
