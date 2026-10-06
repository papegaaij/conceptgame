package vanguard.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;
import vanguard.sim.WeaponSpec;
import vanguard.sim.WingmanSpec;

/** design/player/wingmen/data.yaml as the simulation flies it (M5 part A). */
class WingmenDataTest {
    private static final Content CONTENT = ContentLoader.fromClasspath();

    private static double dps(WeaponSpec gun) {
        return gun.muzzles().size() * gun.damage() * gun.rate();
    }

    @Test
    void rooksCraftAndAiAreTheDesignsNumbers() {
        WingmanSpec rook = SimSpecs.wingman(CONTENT, "autocannon", 1, WingmanSpec.Side.LEFT, 80);

        assertEquals(80, rook.craft().maxArmour());
        assertEquals(new vanguard.sim.Hitbox(11, 11), rook.craft().hitbox());
        assertEquals(250, rook.craft().speed());
        assertEquals(33, rook.craft().edgeLimit());
        assertEquals(0.3, rook.craft().lowArmour());
        assertEquals(new WingmanSpec.Offset(64, 28), rook.ai().wing());
        assertEquals(new WingmanSpec.Offset(40, 90), rook.ai().trail());
        assertEquals(Math.toRadians(15), rook.ai().coneHalfAngle(), 1e-12);
        assertEquals(360, rook.ai().range());
    }

    /** design/player/wingmen, Rook's guns: the DPS column is the base weapon's volley DPS times the scale. */
    @Test
    void hisGunsAreTheBaseWeaponsScaled() {
        assertEquals(
                12,
                dps(SimSpecs.wingman(CONTENT, "autocannon", 1, WingmanSpec.Side.LEFT, 80)
                        .gun()),
                1e-9);
        assertEquals(
                37.2,
                dps(SimSpecs.wingman(CONTENT, "autocannon", 5, WingmanSpec.Side.LEFT, 80)
                        .gun()),
                1e-9);
        assertEquals(
                10.8,
                dps(SimSpecs.wingman(CONTENT, "scatter", 1, WingmanSpec.Side.LEFT, 80)
                        .gun()),
                1e-9);
        assertEquals(
                42,
                dps(SimSpecs.wingman(CONTENT, "missiles", 5, WingmanSpec.Side.LEFT, 80)
                        .gun()),
                1e-9);
        assertEquals(
                54,
                dps(SimSpecs.wingman(CONTENT, "mortar", 5, WingmanSpec.Side.LEFT, 80)
                        .gun()),
                1e-9);
    }

    /** One muzzle at his nose; a pod's pattern as the pod on his side has it: the Missiles launch outward. */
    @Test
    void heFiresFromHisNoseAndHisMissilesLaunchOutward() {
        WeaponSpec right = SimSpecs.wingman(CONTENT, "missiles", 1, WingmanSpec.Side.RIGHT, 80)
                .gun();
        WeaponSpec left = SimSpecs.wingman(CONTENT, "missiles", 1, WingmanSpec.Side.LEFT, 80)
                .gun();

        assertEquals(List.of(new WeaponSpec.Muzzle(0, 18, Math.toRadians(30))), right.muzzles());
        assertEquals(List.of(new WeaponSpec.Muzzle(0, 18, -Math.toRadians(30))), left.muzzles());
        assertEquals(WeaponSpec.Delivery.HOMING, left.delivery());
        assertEquals(
                WeaponSpec.Delivery.LOBBED,
                SimSpecs.wingman(CONTENT, "mortar", 1, WingmanSpec.Side.LEFT, 80)
                        .gun()
                        .delivery());
    }

    @Test
    void anUnknownGunOrLevelIsRefused() {
        assertThrows(
                IllegalArgumentException.class, () -> SimSpecs.wingman(CONTENT, "laser", 1, WingmanSpec.Side.LEFT, 80));
        assertThrows(
                IllegalArgumentException.class,
                () -> SimSpecs.wingman(CONTENT, "scatter", 6, WingmanSpec.Side.LEFT, 80));
        assertThrows(
                IllegalArgumentException.class,
                () -> SimSpecs.wingman(CONTENT, "scatter", 1, WingmanSpec.Side.LEFT, 0));
    }

    @Test
    void theBarksAreLoaded() {
        WingmenData.Barks barks = CONTENT.wingmen().barks();

        assertEquals("Rook", barks.speaker());
        assertEquals(8, barks.triggers().size());
        assertEquals("boss-warning", barks.triggers().getFirst().trigger());
        assertEquals(1.5, barks.bark("rear-wave").orElseThrow().ahead().orElseThrow());
        assertEquals(10, barks.bark("kill-streak").orElseThrow().kills().orElseThrow());
    }
}
