package vanguard.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

class PoolTest {
    private static final class Item {
    }

    @Test
    void obtainReturnsNullWhenExhausted() {
        Pool<Item> pool = new Pool<>(2, Item::new, Item[]::new);
        pool.obtain();
        pool.obtain();
        assertNull(pool.obtain());
        assertEquals(2, pool.size());
    }

    @Test
    void freeMovesLastItemIntoTheGapAndRecyclesTheFreedInstance() {
        Pool<Item> pool = new Pool<>(3, Item::new, Item[]::new);
        Item first = pool.obtain();
        pool.obtain();
        Item third = pool.obtain();

        pool.free(0);

        assertEquals(2, pool.size());
        assertSame(third, pool.get(0));
        assertSame(first, pool.obtain());
    }
}
