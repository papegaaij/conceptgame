package vanguard.sim;

/** A play-field edge that shows an edge warning before a wave enters there. */
public enum WarningEdge {
    LEFT,
    RIGHT,
    BOTTOM;

    /** The bit of this edge in {@link Sortie#edgeWarnings()}. */
    public int bit() {
        return 1 << ordinal();
    }

    /** Whether this edge is in a set of warning bits. */
    public boolean in(int edges) {
        return (edges & bit()) != 0;
    }
}
