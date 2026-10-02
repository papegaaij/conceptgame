package vanguard.content.campaign;

/**
 * An owned item at its upgrade level.
 *
 * @param item a weapon's slug, or a core part's name in its data file ({@code Mk I}, {@code Standard})
 * @param level the upgrade level, from 1
 */
public record Fitted(String item, int level) {
    public Fitted {
        if (item.isBlank() || level < 1) {
            throw new IllegalArgumentException("invalid item " + item + " L" + level);
        }
    }
}
