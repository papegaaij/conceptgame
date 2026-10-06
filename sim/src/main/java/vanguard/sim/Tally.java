package vanguard.sim;

import java.util.Arrays;

/**
 * The score, the chain and the credits of the current attempt (design/systems/scoring,
 * design/systems/economy). Score and credits are separate: kills score bounty × kill score ×
 * chain multiplier, pickups and bonuses score their credit value × pickup score, and the
 * difficulty's score factor applies to all of it. Every credit payout is its Act 1 medium value
 * times the credit factor, rounded half to even per payout; a bounty (a kill, a part, a ground
 * target's, a burst, a shot mine) is also multiplied by the level's bounty scale before that one
 * rounding. Score uses the bounty without the scale. A failed attempt's tally is discarded.
 */
final class Tally {
    private final ScoringRules rules;
    private final int windowTicks;
    private final int[] credits = new int[CreditSource.values().length];
    private long score;
    private int chain;
    private int maxChain;
    private int chainTicks;
    /** Whether the chain window stood still in the last step: no enemy on screen to keep it going. */
    private boolean paused;

    private int kills;

    Tally(ScoringRules rules) {
        this.rules = rules;
        windowTicks = SimStep.ticks(rules.chain().windowSeconds());
    }

    /** Takes over {@code other}'s state (a boss checkpoint). */
    void copyFrom(Tally other) {
        System.arraycopy(other.credits, 0, credits, 0, credits.length);
        score = other.score;
        chain = other.chain;
        maxChain = other.maxChain;
        chainTicks = other.chainTicks;
        paused = other.paused;
        kills = other.kills;
    }

    void reset() {
        Arrays.fill(credits, 0);
        score = 0;
        chain = 0;
        maxChain = 0;
        chainTicks = 0;
        paused = false;
        kills = 0;
    }

    /**
     * Counts down the chain window while {@code enemiesOnScreen}, and pauses it otherwise (a gap in
     * the level never breaks a chain); the chain ends when the window runs out.
     */
    void step(boolean enemiesOnScreen) {
        paused = !enemiesOnScreen;
        if (!paused && chainTicks > 0 && --chainTicks == 0) {
            chain = 0;
        }
    }

    /** Whether the chain window stood still in the last step (no enemy on screen). */
    boolean paused() {
        return paused;
    }

    /** A kill: extends the chain, scores with the multiplier and pays the bounty; returns the credits. */
    int kill(int bounty) {
        return kill(bounty, CreditSource.KILLS);
    }

    /** A kill paid from {@code source} (a ground unit's bounty counts as a ground target's). */
    int kill(int bounty, CreditSource source) {
        kills++;
        chain++;
        maxChain = Math.max(maxChain, chain);
        chainTicks = windowTicks;
        score += Math.round(bounty * rules.killScore() * multiplier() * rules.scoreFactor());
        return bounty(source, bounty);
    }

    /**
     * A destroyed part of a set piece: it extends the chain, scores and pays its bounty like a kill,
     * but the kill is the whole unit's ({@link #countKill()}); returns the credits.
     */
    int partKill(int bounty) {
        chain++;
        maxChain = Math.max(maxChain, chain);
        chainTicks = windowTicks;
        score += Math.round(bounty * rules.killScore() * multiplier() * rules.scoreFactor());
        return bounty(CreditSource.KILLS, bounty);
    }

    /**
     * A bounty that is not a kill (a spawner bursting on its own): scored at the kill score without
     * the chain's multiplier, no kill, the chain untouched; returns the credits.
     */
    int unchained(int bounty) {
        score += Math.round(bounty * rules.killScore() * rules.scoreFactor());
        return bounty(CreditSource.KILLS, bounty);
    }

    /** A set piece destroyed: one kill, its parts paid already. */
    void countKill() {
        kills++;
    }

    /** Armour damage ends the chain (shield hits do not). */
    void breakChain() {
        chain = 0;
        chainTicks = 0;
    }

    /** Pays {@code baseCredits} (Act 1, medium) from {@code source}; returns the credits paid. */
    int earn(CreditSource source, int baseCredits) {
        return pay(source, baseCredits * rules.creditFactor());
    }

    /**
     * Pays {@code baseCredits} (Act 1, medium) from {@code source} times {@code bonus} (the Salvage
     * scanner's 1.1 or 1.2 on salvage and hidden crates, 1 without), rounded once; returns the credits paid.
     */
    int earn(CreditSource source, int baseCredits, double bonus) {
        return pay(source, baseCredits * rules.creditFactor() * bonus);
    }

    /**
     * Pays a bounty of {@code baseCredits} (Act 1, medium) from {@code source}: times the credit
     * factor and the level's bounty scale, rounded once; returns the credits paid.
     */
    int bounty(CreditSource source, int baseCredits) {
        return pay(source, baseCredits * rules.creditFactor() * rules.bountyScale());
    }

    private int pay(CreditSource source, double amount) {
        int paid = (int) Math.rint(amount);
        credits[source.ordinal()] += paid;
        return paid;
    }

    /** Scores a pickup or bonus worth {@code baseCredits}. */
    void scoreValue(int baseCredits) {
        score += Math.round(baseCredits * rules.pickupScore() * rules.scoreFactor());
    }

    long score() {
        return score;
    }

    int credits(CreditSource source) {
        return credits[source.ordinal()];
    }

    int credits() {
        int total = 0;
        for (int amount : credits) {
            total += amount;
        }
        return total;
    }

    int chain() {
        return chain;
    }

    double multiplier() {
        return rules.chain().multiplier(chain);
    }

    /** The share of the chain window left, 1 right after a kill. */
    double window() {
        return windowTicks == 0 ? 0 : (double) chainTicks / windowTicks;
    }

    int maxChain() {
        return maxChain;
    }

    int kills() {
        return kills;
    }

    void addTo(StateHash hash) {
        hash.add(score)
                .add(chain)
                .add(maxChain)
                .add(chainTicks)
                .add(paused ? 1 : 0)
                .add(kills);
        for (int amount : credits) {
            hash.add(amount);
        }
    }
}
