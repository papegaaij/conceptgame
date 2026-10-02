package vanguard.sim;

/** Helpers for the sortie's {@link Pool}s. */
final class Pools {
    private Pools() {}

    /** Frees every active item. */
    static void clear(Pool<?> pool) {
        while (pool.size() > 0) {
            pool.free(pool.size() - 1);
        }
    }

    /** Adds the size and every active item to the hash. */
    static void addAll(StateHash hash, Pool<? extends Hashed> pool) {
        hash.add(pool.size());
        for (int i = 0; i < pool.size(); i++) {
            pool.get(i).addTo(hash);
        }
    }
}
