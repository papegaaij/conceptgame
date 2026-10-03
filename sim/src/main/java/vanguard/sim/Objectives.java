package vanguard.sim;

import java.util.Arrays;
import java.util.List;

/**
 * What counts towards the level's objectives in an attempt: the kills of every enemy kind, the
 * secrets found and the secondary objective: destroy a share of all enemies (design/systems/scoring),
 * or clear groups of ground units (Level 02's docks). A group's outcome is decided when the last
 * of its units is gone: cleared if every one was destroyed, lost if any left the screen alive. The
 * primary objective, reaching the end of the scroll, is the {@link Sortie}'s.
 */
final class Objectives {
    /** A group's state: still open, cleared, lost. */
    static final int OPEN = 0;

    static final int CLEARED = 1;
    static final int LOST = 2;

    private final boolean byGroups;
    private final String escapes;
    private final int escapesTotal;
    private int escapesDestroyed;
    private boolean escapesFailed;
    private final int[] killsByKind;
    private final int requiredKills;
    private final int[] groupUnits;
    private final int[] groupDestroyed;
    private final int[] groupEscaped;
    private final int[] groupState;
    private int secretsFound;
    private int groupsCleared;
    private int groupsLost;
    private boolean secondaryMet;

    /**
     * @param kinds the number of distinct enemies in the level
     * @param units every enemy the level sends
     */
    /** @param escapesTotal the units of the enemy of an escapes objective the level sends */
    Objectives(
            LevelScript.Secondary secondary,
            int kinds,
            int units,
            List<LevelScript.GroundUnit> groundUnits,
            int escapesTotal) {
        byGroups = secondary.byGroups();
        escapes = secondary.escapes();
        this.escapesTotal = escapesTotal;
        killsByKind = new int[kinds];
        requiredKills = byGroups || secondary.byEscapes() ? 0 : (int) Math.ceil(secondary.killRatio() * units - 1e-9);
        int groups = secondary.groups().size();
        groupUnits = new int[groups];
        groupDestroyed = new int[groups];
        groupEscaped = new int[groups];
        groupState = new int[groups];
        for (LevelScript.GroundUnit unit : groundUnits) {
            if (unit.group() >= 0) {
                groupUnits[unit.group()]++;
            }
        }
    }

    /** Back to the level start. */
    void reset() {
        Arrays.fill(killsByKind, 0);
        Arrays.fill(groupDestroyed, 0);
        Arrays.fill(groupEscaped, 0);
        Arrays.fill(groupState, OPEN);
        secretsFound = 0;
        groupsCleared = 0;
        groupsLost = 0;
        escapesDestroyed = 0;
        escapesFailed = false;
        secondaryMet = false;
    }

    /** A unit of {@code slug} was destroyed; returns whether that met an escapes objective. */
    boolean escapeDestroyed(String slug) {
        if (escapes.isEmpty() || !escapes.equals(slug) || escapesFailed) {
            return false;
        }
        escapesDestroyed++;
        if (escapesDestroyed == escapesTotal) {
            secondaryMet = true;
            return true;
        }
        return false;
    }

    /** A unit of {@code slug} got away; returns whether that failed an escapes objective. */
    boolean escapeLost(String slug) {
        if (escapes.isEmpty() || !escapes.equals(slug) || escapesFailed || secondaryMet) {
            return false;
        }
        escapesFailed = true;
        return true;
    }

    int escapesDestroyed() {
        return escapesDestroyed;
    }

    int escapesTotal() {
        return escapesTotal;
    }

    boolean escapesFailed() {
        return escapesFailed;
    }

    /** A unit of group {@code g} was destroyed; returns the group's state after it. */
    int groupUnitDestroyed(int g) {
        groupDestroyed[g]++;
        return decide(g);
    }

    /** A unit of group {@code g} left the screen alive; returns the group's state after it. */
    int groupUnitEscaped(int g) {
        groupEscaped[g]++;
        return decide(g);
    }

    private int decide(int g) {
        if (groupState[g] == OPEN && groupDestroyed[g] + groupEscaped[g] == groupUnits[g]) {
            if (groupEscaped[g] == 0) {
                groupState[g] = CLEARED;
                groupsCleared++;
                secondaryMet = groupsCleared == groupState.length;
            } else {
                groupState[g] = LOST;
                groupsLost++;
            }
        }
        return groupState[g];
    }

    /** The groups lost in this attempt. */
    int groupsLost() {
        return groupsLost;
    }

    int groupCount() {
        return groupState.length;
    }

    int groupState(int g) {
        return groupState[g];
    }

    /** Counts a kill of an enemy of {@code kind}; returns the kills of that kind in this attempt. */
    int kill(int kind) {
        return ++killsByKind[kind];
    }

    /** Whether {@code kills} in all meet the secondary objective, the first time they do. */
    boolean meetsSecondary(int kills) {
        if (byGroups || !escapes.isEmpty() || secondaryMet || kills < requiredKills) {
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
        for (int g = 0; g < groupState.length; g++) {
            hash.add(groupDestroyed[g]).add(groupEscaped[g]).add(groupState[g]);
        }
        hash.add(escapesDestroyed).add(escapesFailed ? 1 : 0);
    }
}
