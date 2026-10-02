package vanguard.game.ui;

import java.util.List;
import vanguard.game.input.MenuInput;

/**
 * A vertical menu list: items that can be disabled (shown, but the cursor passes over them), the
 * cursor on one enabled item, moved with up and down and wrapping at the ends.
 */
public final class Menu<T> {
    /** One entry; {@code id} tells the screen what was chosen. */
    public record Item<I>(I id, String label, boolean enabled) {
        public static <I> Item<I> of(I id, String label) {
            return new Item<>(id, label, true);
        }

        public static <I> Item<I> disabled(I id, String label) {
            return new Item<>(id, label, false);
        }
    }

    private final List<Item<T>> items;
    private int selected;

    public Menu(List<Item<T>> items) {
        this.items = List.copyOf(items);
        selected = this.items.indexOf(
                this.items.stream().filter(Item::enabled).findFirst().orElseThrow());
    }

    public List<Item<T>> items() {
        return items;
    }

    public int selected() {
        return selected;
    }

    public T selectedId() {
        return items.get(selected).id();
    }

    /** Moves the cursor by one enabled item; returns whether it moved. */
    public boolean move(int direction) {
        int before = selected;
        do {
            selected = Math.floorMod(selected + direction, items.size());
        } while (!items.get(selected).enabled());
        return selected != before;
    }

    /** Moves the cursor on up or down; returns whether it moved. */
    public boolean navigate(MenuInput input) {
        if (input.up()) {
            return move(-1);
        }
        if (input.down()) {
            return move(1);
        }
        return false;
    }
}
