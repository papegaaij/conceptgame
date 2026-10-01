package vanguard.sim;

import java.util.function.IntFunction;
import java.util.function.Supplier;

/**
 * Fixed-capacity object pool with a dense active range: all instances are created up front, so
 * obtaining and freeing never allocate. Freeing swaps the last active item into the freed slot,
 * which keeps iteration order deterministic. Iterate backwards when freeing during iteration.
 */
public final class Pool<T> {
    private final T[] items;
    private int size;

    public Pool(int capacity, Supplier<T> factory, IntFunction<T[]> arrayFactory) {
        items = arrayFactory.apply(capacity);
        for (int i = 0; i < capacity; i++) {
            items[i] = factory.get();
        }
    }

    /** Returns a free instance (with stale field values), or {@code null} when the pool is exhausted. */
    public T obtain() {
        return size < items.length ? items[size++] : null;
    }

    /** Returns the instance at {@code index} to the pool. */
    public void free(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException(index);
        }
        int last = --size;
        T freed = items[index];
        items[index] = items[last];
        items[last] = freed;
    }

    public T get(int index) {
        return items[index];
    }

    public int size() {
        return size;
    }

    public int capacity() {
        return items.length;
    }
}
