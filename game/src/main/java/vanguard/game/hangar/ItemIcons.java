package vanguard.game.hangar;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import java.util.IdentityHashMap;
import java.util.Locale;
import java.util.Map;
import vanguard.content.campaign.Catalogue;
import vanguard.content.campaign.Catalogue.Item;
import vanguard.content.campaign.ItemKind;
import vanguard.game.render.Sprites;

/**
 * The hangar's equipment icons (tools/art/icons.py, {@code icons/} on the sprite pages): every shop
 * item's 16 px icon for the shop rows and 24 px icon for the selected item and the schematic's
 * callouts, and Rook's for the escort slot. Looked up once; a missing icon fails here, not while
 * drawing. Items are the catalogue's own instances, so they are keyed by identity (a record's hash
 * would walk its lists on every lookup).
 */
final class ItemIcons {
    /** An item's two icons. */
    record Icon(TextureRegion small, TextureRegion large) {}

    private final Map<Item, Icon> icons = new IdentityHashMap<>();
    private final Icon escort;

    ItemIcons(Sprites sprites, Catalogue catalogue) {
        for (ItemKind kind : ItemKind.values()) {
            for (Item item : catalogue.items(kind)) {
                icons.put(item, icon(sprites, name(item)));
            }
        }
        escort = icon(sprites, "escort-rook");
    }

    private static Icon icon(Sprites sprites, String name) {
        return new Icon(sprites.region("icons/" + name), sprites.region("icons/" + name + "-large"));
    }

    /** The icon's name: a weapon's slug, otherwise the kind and the item's name, lower case, hyphenated. */
    static String name(Item item) {
        String id = item.kind().weapon() ? item.id() : item.kind().name() + "-" + item.id();
        return id.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
    }

    /** The icon of one of the catalogue's items. */
    Icon of(Item item) {
        Icon icon = icons.get(item);
        if (icon == null) {
            throw new IllegalArgumentException("not an item of the catalogue: " + item.id());
        }
        return icon;
    }

    /** Rook's craft, the escort slot's. */
    Icon escort() {
        return escort;
    }
}
