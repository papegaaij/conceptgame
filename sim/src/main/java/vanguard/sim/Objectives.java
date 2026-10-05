package vanguard.sim;

import java.util.Arrays;
import java.util.List;

/**
 * What counts towards the level's objectives in an attempt: the kills of every enemy kind, the
 * secrets found and the secondary objective: destroy a share of all enemies (design/systems/scoring),
 * clear groups of ground units (Level 02's docks), let none of an enemy get away, destroy every unit
 * of some enemies, or shoot off a boss's parts in time (Level 07). A group's outcome is decided when the last
 * of its units is gone: cleared if every one was destroyed, lost if any left the screen alive. The
 * primary objective, reaching the end of the scroll, is the {@link Sortie}'s.
 */
final class Objectives {
    /** A group's state: still open, cleared, lost. */
    static final int OPEN = 0;

    static final int CLEARED = 1;
    static final int LOST = 2;

    private final boolean byGroups;
    private final LevelScript.Secondary secondary;
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
     * @param groups the groups the ground units belong to: the secondary's or the destroy-targets
     *     primary's ({@link LevelScript#groups()})
     * @param kinds the number of distinct enemies in the level
     * @param units every enemy the level sends
     * @param escapesTotal the units of the enemies of an escapes or kill-all objective the level
     *     sends, or the boss parts of a parts objective
     */
    Objectives(
            LevelScript.Secondary secondary,
            int groups,
            int kinds,
            int units,
            List<LevelScript.GroundUnit> groundUnits,
            int escapesTotal) {
        this.secondary = secondary;
        byGroups = secondary.byGroups();
        this.escapesTotal = escapesTotal;
        killsByKind = new int[kinds];
        requiredKills = byGroups || secondary.byEscapes() ? 0 : (int) Math.ceil(secondary.killRatio() * units - 1e-9);
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

    /** Takes over {@code other}'s tallies (a boss checkpoint); both are of the same level. */
    void copyFrom(Objectives other) {
        System.arraycopy(other.killsByKind, 0, killsByKind, 0, killsByKind.length);
        System.arraycopy(other.groupDestroyed, 0, groupDestroyed, 0, groupDestroyed.length);
        System.arraycopy(other.groupEscaped, 0, groupEscaped, 0, groupEscaped.length);
        System.arraycopy(other.groupState, 0, groupState, 0, groupState.length);
        secretsFound = other.secretsFound;
        groupsCleared = other.groupsCleared;
        groupsLost = other.groupsLost;
        escapesDestroyed = other.escapesDestroyed;
        escapesFailed = other.escapesFailed;
        secondaryMet = other.secondaryMet;
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
        if (!secondary.counts(slug) || escapesFailed) {
            return false;
        }
        escapesDestroyed++;
        if (escapesDestroyed == escapesTotal) {
            secondaryMet = true;
            return true;
        }
        return false;
    }

    /** A part of a parts objective was shot off; returns whether that met the objective. */
    boolean partShotOff() {
        if (!secondary.byParts() || escapesFailed || secondaryMet) {
            return false;
        }
        escapesDestroyed++;
        if (escapesDestroyed == escapesTotal) {
            secondaryMet = true;
            return true;
        }
        return false;
    }

    /** A parts objective's phase ended (or its boss died) with a part alive; returns whether that failed the objective. */
    boolean partsSurvived() {
        if (!secondary.byParts() || escapesFailed || secondaryMet) {
            return false;
        }
        escapesFailed = true;
        return true;
    }

    /** A unit of {@code slug} got away; returns whether that failed an escapes objective. */
    boolean escapeLost(String slug) {
        if (!secondary.counts(slug) || escapesFailed || secondaryMet) {
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
                secondaryMet |= byGroups && groupsCleared == groupState.length;
            } else {
                groupState[g] = LOST;
                groupsLost++;
            }
        }
        return groupState[g];
    }

    /** The groups cleared in this attempt. */
    int groupsCleared() {
        return groupsCleared;
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

    /**
     * Whether {@code kills} in all meet a kill-ratio secondary, the first time they do. It is asked
     * once, when the level is complete: the share is judged at the end, never mid-level, so its
     * credits and line come with the level-end lines (design/systems/scoring). A level that sends
     * no enemies has no share to meet.
     */
    boolean meetsKillRatio(int kills) {
        if (byGroups || secondary.byEscapes() || secondaryMet || requiredKills == 0 || kills < requiredKills) {
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
