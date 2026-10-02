package vanguard.content;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import vanguard.sim.Hitbox;
import vanguard.sim.Layer;
import vanguard.sim.Loadout;
import vanguard.sim.Plating;
import vanguard.sim.PulseCannon;
import vanguard.sim.ShieldModel;
import vanguard.sim.ShipSpec;
import vanguard.sim.SkitterSpec;

/** The specs built from the design data carry the numbers the M1 simulation was written with. */
class SimSpecsTest {
    private final Content content = ContentLoader.fromClasspath();

    @Test
    void theStarterLoadoutComesFromTheShipAndItsStarterParts() {
        assertEquals(
                new Loadout(
                        new ShipSpec(270, 0.08, 0.06, 0.5, 48, 12, new Hitbox(9, 9), 0.25, 21, 3),
                        new PulseCannon(10, 2.0, 900, new Hitbox(4, 12)),
                        new ShieldModel(20, 2, 2.0, 1.0),
                        new Plating(60)),
                SimSpecs.starterLoadout(content));
    }

    @Test
    void theSkitterComesFromItsStatBlockAndTheBalancingBasis() {
        assertEquals(new SkitterSpec(1, new Hitbox(16, 16), 190, 0.25, 6, Layer.AIR), SimSpecs.skitter(content));
    }
}
