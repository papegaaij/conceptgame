package vanguard.sim;

/** 64-bit FNV-1a over the bit patterns of the game state, used to compare replays across OSes. */
final class StateHash {
    private static final long OFFSET_BASIS = 0xCBF29CE484222325L;
    private static final long PRIME = 0x100000001B3L;

    private long hash = OFFSET_BASIS;

    StateHash add(long value) {
        for (int shift = 0; shift < 64; shift += 8) {
            hash ^= (value >>> shift) & 0xFF;
            hash *= PRIME;
        }
        return this;
    }

    StateHash add(double value) {
        return add(Double.doubleToLongBits(value));
    }

    long value() {
        return hash;
    }
}
