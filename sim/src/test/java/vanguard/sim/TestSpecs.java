package vanguard.sim;

/**
 * The starter loadout and the Skitter with the M1 numbers, for the rule tests here. The game builds
 * them from the design data (vanguard.content.SimSpecs, whose test checks the same numbers).
 */
final class TestSpecs {
    static final ShipSpec SHIP = new ShipSpec(270, 0.08, 0.06, 0.5, 48, 12, new Hitbox(9, 9), 0.25, 21, 3);
    static final Loadout LOADOUT = new Loadout(
            SHIP, new PulseCannon(10, 2.0, 900, new Hitbox(4, 12)), new ShieldModel(20, 2, 2.0, 1.0), new Plating(60));
    static final SkitterSpec SKITTER = new SkitterSpec(1, new Hitbox(16, 16), 190, 0.25, 6, Layer.AIR);

    private TestSpecs() {}

    static Sortie sortie(long seed) {
        return new Sortie(seed, LOADOUT, SKITTER);
    }
}
