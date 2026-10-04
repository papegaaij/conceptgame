package vanguard.sim;

/**
 * The Smart Bomb's numbers (design/player/specials, Smart Bomb (L06)): a white flash and a ring
 * expanding from the ship to cover the whole play field in {@code ringSeconds}; every enemy bullet
 * goes at once, and bullets spawned while the ring expands go as it passes them; every enemy on the
 * screen takes {@code damage} once as the ring passes it (a boss part {@code bossPartDamage}); the
 * ship is invulnerable for {@code invulnerableSeconds}; the next bomb can follow after
 * {@code repeatSeconds}.
 *
 * @param flashSeconds how long the flash holds at {@code flashOpacity} before it fades over {@code fadeSeconds}
 */
public record SmartBombSpec(
        double ringSeconds,
        double damage,
        double bossPartDamage,
        double invulnerableSeconds,
        double repeatSeconds,
        double flashSeconds,
        double flashOpacity,
        double fadeSeconds) {
    public SmartBombSpec {
        if (!(ringSeconds > 0) || !(repeatSeconds > 0)) {
            throw new IllegalArgumentException("a Smart Bomb needs a ring time and a repeat time");
        }
    }
}
