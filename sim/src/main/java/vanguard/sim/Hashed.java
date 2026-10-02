package vanguard.sim;

/** Part of the game state that goes into the {@link StateHash}. */
interface Hashed {
    void addTo(StateHash hash);
}
