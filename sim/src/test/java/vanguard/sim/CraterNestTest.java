package vanguard.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Level 05's rules: the Polyp Mortar's lob (marker, ring, direct hit), the mass-driver sleds (the
 * lights, the strike, the blocking, the stuck sled's clamp), the destroy-targets primary that fails
 * as soon as a battery's unit gets away, the group drop and the low-gravity rocks.
 */
class CraterNestTest {
    private static final int FIRE = Command.FIRE.bit();

    static final EnemyGun MORTAR_GUN = new EnemyGun(
            3.5,
            1.5,
            1,
            110,
            10,
            false,
            1,
            0,
            Double.POSITIVE_INFINITY,
            Double.POSITIVE_INFINITY,
            Optional.empty(),
            Optional.of(new EnemyGun.MortarSpec(1.0, 16, 8, 4)));
    static final EnemySpec MORTAR = new EnemySpec(
            "polyp-mortar",
            10,
            new Hitbox(32, 32),
            Layer.GROUND,
            10,
            true,
            15,
            0,
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.of(MORTAR_GUN),
            Optional.empty(),
            Optional.empty(),
            true);

    private static final LevelScript.SledSpec SLED =
            new LevelScript.SledSpec(432, 24, 2, 5, 30, 1.5, 0.4, 15, "stuck sled");
    private static final LevelScript.RockSpec ROCKS =
            new LevelScript.RockSpec(2, 3, 40, 60, 2.5, new Hitbox(16, 16), 1, 6, 72);

    private static LevelScript level(
            List<LevelScript.GroundUnit> units,
            List<LevelScript.GroundObjectSpec> objects,
            List<String> targets,
            Optional<LevelScript.SledSpec> sled,
            Optional<LevelScript.RockSpec> rocks,
            List<LevelScript.GroupDrop> drops) {
        return new LevelScript(
                5,
                1,
                0,
                List.of(new LevelScript.Section(40, 150)),
                List.of(),
                objects,
                units,
                (int) objects.stream()
                        .filter(LevelScript.GroundObjectSpec::trigger)
                        .count(),
                List.of(new LevelScript.RadioCue(
                        LevelScript.CueTrigger.MISSION_FAILED,
                        0,
                        "",
                        "Okafor",
                        "{group} is behind you.",
                        false,
                        "grim")),
                new LevelScript.Secondary(0, 40, List.of(), "", List.of("polyp-mortar"), "NEST"),
                List.of(),
                List.of(),
                List.of(),
                Optional.empty(),
                Optional.empty(),
                targets,
                sled,
                rocks,
                drops);
    }

    private static LevelScript mortarAt(double x) {
        return level(
                List.of(new LevelScript.GroundUnit(0, x, MORTAR, -1)),
                List.of(),
                List.of(),
                Optional.empty(),
                Optional.empty(),
                List.of());
    }

    private static Sortie sortie(LevelScript level) {
        return new Sortie(1, TestSpecs.LOADOUT, level, TestSpecs.RULES, TestSpecs.FULL_ARMOUR);
    }

    @Test
    void aMortarLobsAtWhereTheShipWasAndItsBlobBurstsIntoARingAndHitsTheShipThatStayed() {
        // Far to the side, so the ship's guns leave it alone.
        Sortie sortie = sortie(mortarAt(40));
        double lobbedAt = -1;
        double landedAt = -1;
        int direct = -1;
        double markerX = 0;
        double markerY = 0;
        int ringBullets = 0;
        double armourBefore =
                sortie.ship().defences().armour() + sortie.ship().defences().shield();
        for (int i = 0; i < SimStep.ticks(4) && landedAt < 0; i++) {
            sortie.step(Command.NONE);
            SimEvents events = sortie.events();
            for (int e = 0; e < events.size(); e++) {
                if (events.type(e) == SimEvents.Type.MORTAR_LOBBED) {
                    lobbedAt = sortie.levelSeconds();
                    markerX = sortie.lob(0).targetX();
                    markerY = sortie.lob(0).targetY();
                } else if (events.type(e) == SimEvents.Type.MORTAR_IMPACT) {
                    landedAt = sortie.levelSeconds();
                    direct = events.value(e);
                    ringBullets = sortie.bulletCount();
                }
            }
        }

        assertEquals(1.5, lobbedAt, 0.05, "its first lob 1.5 s after it enters");
        assertEquals(Ship.START_X, markerX, 1e-9, "the marker is where the ship was");
        assertEquals(Ship.START_Y, markerY, 1e-9);
        assertEquals(lobbedAt + 1, landedAt, 0.02, "the blob flies the marker's second");
        assertEquals(1, direct, "the ship stayed inside the impact circle");
        assertEquals(8, ringBullets);
        assertTrue(sortie.ship().defences().armour() + sortie.ship().defences().shield() <= armourBefore - 10 + 1e-9);
    }

    @Test
    void aShipThatMovesOffTheMarkerTakesOnlyTheRing() {
        Sortie sortie = sortie(mortarAt(40));
        int direct = -1;
        boolean lobbed = false;
        for (int i = 0; i < SimStep.ticks(4) && direct < 0; i++) {
            sortie.step(lobbed ? Command.RIGHT.bit() : Command.NONE);
            SimEvents events = sortie.events();
            for (int e = 0; e < events.size(); e++) {
                lobbed |= events.type(e) == SimEvents.Type.MORTAR_LOBBED;
                if (events.type(e) == SimEvents.Type.MORTAR_IMPACT) {
                    direct = events.value(e);
                }
            }
        }
        assertEquals(0, direct, "a second is time enough to leave the 32 px circle");
    }

    @Test
    void theSledsLightTheRailThenStrikeTheShipOnceAndBlockShots() {
        LevelScript level = level(List.of(), List.of(), List.of(), Optional.of(SLED), Optional.empty(), List.of());
        Sortie sortie = sortie(level);
        Sled sled = sortie.sled().orElseThrow();
        List<Double> lights = new ArrayList<>();
        List<Double> launches = new ArrayList<>();
        int hits = 0;
        int glanced = 0;
        for (int i = 0; i < SimStep.ticks(13); i++) {
            // Onto the rail and firing up it.
            boolean onRail = sortie.ship().x() < SLED.x();
            sortie.step(FIRE | (onRail ? Command.RIGHT.bit() : Command.NONE));
            SimEvents events = sortie.events();
            for (int e = 0; e < events.size(); e++) {
                switch (events.type(e)) {
                    case SLED_LIGHTS -> lights.add(sortie.levelSeconds());
                    case SLED_LAUNCHED -> launches.add(sortie.levelSeconds());
                    case SLED_HIT -> hits++;
                    case SHOT_GLANCED -> glanced++;
                    default -> {}
                }
            }
            if (sortie.levelSeconds() > 2.05 && sortie.levelSeconds() < 2.35) {
                assertTrue(sled.running() && sled.lit());
            }
            if (sortie.levelSeconds() > 3 && sortie.levelSeconds() < 5) {
                assertTrue(!sled.lit(), "dark between the sleds");
            }
        }

        assertEquals(
                List.of(0.5, 5.5, 10.5),
                lights.stream().map(t -> Math.round(t * 100) / 100.0).toList());
        assertEquals(
                List.of(2.0, 7.0, 12.0),
                launches.stream().map(t -> Math.round(t * 100) / 100.0).toList());
        assertEquals(launches.size(), hits, "once per sled, whatever the ship's layer");
        assertTrue(glanced > 0, "a running sled stops the shots");
    }

    @Test
    void theStuckSledsClampTakesHitsOnlyWhileTheRailIsDark() {
        LevelScript.GroundObjectSpec clamp = new LevelScript.GroundObjectSpec(
                0, 432, new Hitbox(40, 28), 0, 0, Optional.empty(), 3, 65, "stuck sled", false, 0, 1, Optional.empty());
        LevelScript level = level(List.of(), List.of(clamp), List.of(), Optional.of(SLED), Optional.empty(), List.of());
        Sortie sortie = sortie(level);
        boolean shutWhileLit = true;
        boolean openWhileDark = true;
        for (int i = 0; i < SimStep.ticks(3); i++) {
            sortie.step(Command.NONE);
            if (sortie.groundObjectCount() > 0) {
                GroundObject object = sortie.groundObject(0);
                boolean lit = sortie.sled().orElseThrow().lit();
                shutWhileLit &= !lit || object.shut();
                openWhileDark &= lit || !object.shut();
            }
        }
        assertTrue(shutWhileLit);
        assertTrue(openWhileDark);
    }

    @Test
    void aBatteryUnitThatGetsAwayFailsThePrimaryAtOnce() {
        LevelScript level = level(
                List.of(new LevelScript.GroundUnit(0, 40, MORTAR, 0), new LevelScript.GroundUnit(0, 440, MORTAR, 0)),
                List.of(),
                List.of("Battery A"),
                Optional.empty(),
                Optional.empty(),
                List.of());
        Sortie sortie = new Sortie(1, TestSpecs.LOADOUT, level, TestSpecs.RULES, TestSpecs.FULL_ARMOUR);
        double failedAt = -1;
        boolean line = false;
        for (int i = 0; i < SimStep.ticks(8) && failedAt < 0; i++) {
            sortie.step(Command.NONE);
            SimEvents events = sortie.events();
            for (int e = 0; e < events.size(); e++) {
                if (events.type(e) == SimEvents.Type.PRIMARY_FAILED) {
                    failedAt = sortie.levelSeconds();
                    assertEquals(0, events.value(e));
                } else if (events.type(e) == SimEvents.Type.RADIO) {
                    line = sortie.script().radio().get(events.value(e)).trigger()
                            == LevelScript.CueTrigger.MISSION_FAILED;
                }
            }
        }
        // 540 px of screen and the unit's 32 px at 150 px/s: it leaves after about 3.8 s.
        assertEquals((540 + 32) / 150.0, failedAt, 0.05);
        assertTrue(sortie.primaryFailed());
        assertEquals(0, sortie.failedGroup());
        assertEquals(2, sortie.groupState(0), "lost");
        assertTrue(line, "the level's mission-failed line, with the battery's name filled in by the game");

        // The debug option for captures (--invulnerable) sees the level to its end.
        Sortie capture =
                new Sortie(1, TestSpecs.LOADOUT, level, TestSpecs.RULES.withInvulnerableShip(), TestSpecs.FULL_ARMOUR);
        for (int i = 0; i < SimStep.ticks(8); i++) {
            capture.step(Command.NONE);
        }
        assertTrue(!capture.primaryFailed());
        assertEquals(2, capture.groupState(0), "lost all the same");
    }

    @Test
    void aClearedBatteryDropsItsPickupWhereItsLastUnitDiedAndPaysNoGroupCredits() {
        LevelScript level = level(
                List.of(new LevelScript.GroundUnit(0, Ship.START_X, MORTAR, 0)),
                List.of(),
                List.of("Battery A"),
                Optional.empty(),
                Optional.of(ROCKS),
                List.of(new LevelScript.GroupDrop(0, PickupType.ARMOUR_PATCH)));
        Sortie sortie =
                new Sortie(1, TestSpecs.LOADOUT, level, TestSpecs.RULES.withInvulnerableShip(), TestSpecs.FULL_ARMOUR);
        int cleared = 0;
        int rocks = 0;
        int rocksLeft = -1;
        double clearedAt = -1;
        for (int i = 0; i < SimStep.ticks(8); i++) {
            sortie.step(FIRE);
            SimEvents events = sortie.events();
            for (int e = 0; e < events.size(); e++) {
                if (events.type(e) == SimEvents.Type.GROUP_CLEARED) {
                    cleared++;
                    clearedAt = sortie.levelSeconds();
                } else if (events.type(e) == SimEvents.Type.ROCK_THROWN) {
                    rocks++;
                }
            }
            if (clearedAt > 0 && Math.abs(sortie.levelSeconds() - clearedAt - 2.6) < 0.01) {
                rocksLeft = sortie.debrisCount();
            }
        }
        assertEquals(1, cleared);
        assertEquals(1, sortie.groupState(0));
        assertTrue(!sortie.primaryFailed());
        assertEquals(
                40,
                sortie.result().credits().objectives(),
                "only the kill-all secondary: a battery pays no group credits");
        assertTrue(rocks >= 2 && rocks <= 3, rocks + " rocks");
        assertEquals(0, rocksLeft, "the rocks vanish after 2.5 s");
        assertTrue(sortie.secondaryMet(), "every mortar destroyed: the kill-all secondary");
    }
}
