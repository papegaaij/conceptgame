package vanguard.sim;

import java.util.Arrays;

/**
 * What counts towards the level's objectives in an attempt: the kills of every enemy kind, the
 * secrets found and the secondary objective (destroy a share of all enemies, design/systems/scoring).
 * The primary objective, reaching the end of the scroll, is the {@link Sortie}'s.
 */
final class Objectives {
    private final int[] killsByKind;
    private final int requiredKills;
    private int secretsFound;
    private boolean secondaryMet;

    /**
     * @param kinds the number of distinct enemies in the level
     * @param units every enemy the level sends
     */
    Objectives(LevelScript.Secondary secondary, int kinds, int units) {
        killsByKind = new int[kinds];
        requiredKills = (int) Math.ceil(secondary.killRatio() * units - 1e-9);
    }

    /** Back to the level start. */
    void reset() {
        Arrays.fill(killsByKind, 0);
        secretsFound = 0;
        secondaryMet = false;
    }

    /** Counts a kill of an enemy of {@code kind}; returns the kills of that kind in this attempt. */
    int kill(int kind) {
        return ++killsByKind[kind];
    }

    /** Whether {@code kills} in all meet the secondary objective, the first time they do. */
    boolean meetsSecondary(int kills) {
        if (secondaryMet || kills < requiredKills) {
            return false;
        }
        secondaryMet = true;
        return true;
    }

    void secretFound() {
        secretsFound++;
    }

    int secretsFound() {
        return secretsFound;
    }

    boolean secondaryMet() {
        return secondaryMet;
    }

    int requiredKills() {
        return requiredKills;
    }

    void addKillsTo(StateHash hash) {
        for (int kills : killsByKind) {
            hash.add(kills);
        }
    }
}
