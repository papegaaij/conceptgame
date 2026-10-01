package vanguard.sim;

/**
 * The starting front gun's numbers, from design/player/balance-data.json
 * ({@code weapons.pulse-cannon}) as shown in design/player/weapons/pulse-cannon/README.md. M1
 * fits level 1 only (a single bolt from the front muzzle); the other levels and the data file
 * loader come in M2.
 *
 * @param shotsPerSecond volleys per second while firing
 * @param damage damage per bolt
 * @param boltSpeed bolt speed in px/s, straight up until it leaves the screen
 * @param bolt the bolt's hit box
 */
public record PulseCannon(double shotsPerSecond, double damage, double boltSpeed, Hitbox bolt) {
    public static final PulseCannon LEVEL_1 = new PulseCannon(10, 2.0, 900, new Hitbox(4, 12));

    int intervalTicks() {
        return SimStep.ticks(1 / shotsPerSecond);
    }
}
