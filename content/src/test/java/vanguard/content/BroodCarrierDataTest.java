package vanguard.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import vanguard.sim.BossSpec;
import vanguard.sim.Layer;
import vanguard.sim.LevelScript;
import vanguard.sim.PlayField;

/**
 * The boss keys of M4 part G read from data and mapped by {@link SimSpecs#boss}: a minimal Brood
 * Carrier written here (its real data.yaml comes with Level 07's data) is loaded beside the design
 * tree, with its poses, timed phase, move, windows and spawns, the fan, a spiral without a duration,
 * a fire-only turret and the difficulty hooks {@code arms} and {@code spawns}.
 */
class BroodCarrierDataTest {
    private static final String PATH = "enemies/bosses/test-carrier/data.yaml";

    static final String CARRIER = """
            name: Test Carrier
            faction: Vrell
            layer: high-air
            tier: huge
            size: [288, 626]
            hitbox: [200, 560]
            parts: multi
            part_list:
              - {name: bay 1 left, offset: [-60, -150], hitbox: [40, 40], hp: 180, kind: destroyable, bounty: 25, multiplier: 1.5}
              - {name: bay 1 right, offset: [60, -150], hitbox: [40, 40], hp: 180, kind: destroyable, bounty: 25, multiplier: 1.5}
              - {name: bay 2 left, offset: [-60, 50], hitbox: [40, 40], hp: 180, kind: destroyable, bounty: 25, multiplier: 1.5}
              - {name: bay 2 right, offset: [60, 50], hitbox: [40, 40], hp: 180, kind: destroyable, bounty: 25, multiplier: 1.5}
              - {name: core, offset: [0, 150], hitbox: [60, 60], hp: 2400, kind: vital, bounty: 250, multiplier: 2}
              - {name: mandibles, offset: [0, -290], hitbox: [30, 30], kind: armoured, attack: mandible-fan}
            orientation: fixed
            hp: 3120
            armour: hull and turrets armoured
            speed: 40
            movement:
              straight: {speed: 40}
              hover: {y: 270}
            attacks:
              - {pattern: fan, name: mandible-fan, bullet: small, interval: 2.4, speed: 150, count: 5, spread: 50}
              - {pattern: spiral, name: core-spiral, bullet: small, arms: 3, interval: 0.375, turn_rate: 90, speed: 120}
              - {pattern: ring, name: core-ring, bullet: small, count: 16, interval: 4, speed: 110}
            formations:
              - {name: carrier + escorts}
            weak_points:
              - {name: lime bay sacs, multiplier: 1.5}
            drops: []
            traits: [piercing, spread, homing]
            bounty: 350
            first_level: 7
            difficulty:
              easy:
                spawns: {pass-skitters: 3, pass-needlers: 1}
              hard:
                attacks: {core-spiral: {arms: 4}}
                spawns: {pass-skitters: 5}
            boss:
              kind: boss
              bar_name: TEST CARRIER
              par: 150
              engages_on_arrival: true
              death_seconds: 3
              poses:
                - name: broadside
                  hitbox: [560, 200]
                  offsets:
                    bay 1 left: [150, -60]
                    bay 1 right: [150, 60]
                    bay 2 left: [-50, -60]
                    bay 2 right: [-50, 60]
                    core: [-150, 0]
                    mandibles: [290, 0]
              phases:
                - name: Overhead
                  until: {seconds: 25}
                  windows:
                    groups: [[bay 1 left, bay 1 right], [bay 2 left, bay 2 right]]
                    every: 2.5
                    open: 2
                    offset: 6
                    spawns:
                      - {name: pass-skitters, enemy: skitter, count: 4, speed: 150, arc: 90}
                      - {name: pass-needlers, enemy: needler, count: 2, speed: 100, arc: 60, glide: 1}
                - name: Broadside
                  until: {parts: [bay 1 left, bay 1 right, bay 2 left, bay 2 right], seconds: 70}
                  move: {to: [170, 150], layer: air, descend: 2, pose: broadside, turn: 2}
                  attacks: [mandible-fan]
                  windows:
                    groups: [[bay 1 left, bay 1 right], [bay 2 left, bay 2 right]]
                    every: 2.4
                    open: 2
                    offset: 0.2
                    spawns:
                      - {name: broadside-skitters, enemy: skitter, count: 2, speed: 150}
                - name: Core
                  until: {parts: [core]}
                  exposes: [core]
                  delay: 1
                  attacks: [core-spiral, core-ring]
                  windows:
                    groups: [[bay 1 left, bay 1 right], [bay 2 left, bay 2 right]]
                    every: 6
                    open: 2
                    all: true
                    spawns:
                      - {name: timeout-skitters, enemy: skitter, count: 2, speed: 150, arc: 40}
            """;

    private static Content load(String text) {
        List<DataFile> files = new ArrayList<>(DesignTree.dataFiles());
        files.add(new DataFile(PATH, text));
        return ContentLoader.load(files);
    }

    private static LevelScript.SetPieceSpec carrier(Difficulty difficulty) {
        return SimSpecs.boss(load(CARRIER), new LevelData.BossPlacement("test-carrier", 100, 240, 4), difficulty);
    }

    @Test
    void theBossScriptMapsOntoTheEngine() {
        LevelScript.SetPieceSpec spec = carrier(Difficulty.MEDIUM);
        BossSpec boss = spec.boss().orElseThrow();
        assertEquals(Layer.HIGH_AIR, boss.layer());
        assertTrue(boss.engagesOnArrival());
        assertEquals(3, boss.deathSeconds(), 1e-9);
        assertEquals(PlayField.HEIGHT - 270, boss.hoverY(), 1e-9);
        assertEquals(List.of(5), boss.armoured(), "the mandibles are fire-only");
        assertEquals(0, spec.parts().get(5).bounty());

        assertEquals(
                List.of("arrival", "broadside"),
                boss.poses().stream().map(BossSpec.Pose::name).toList());
        assertEquals(290, boss.poses().get(1).offsets().get(5).dx(), 1e-9);
        assertEquals(-290, boss.poses().get(0).offsets().get(5).dy(), 1e-9);

        BossSpec.Phase overhead = boss.phases().get(0);
        assertEquals(25, overhead.seconds(), 1e-9);
        assertTrue(overhead.untilParts().isEmpty());
        assertEquals(0, overhead.delaySeconds(), 1e-9);
        BossSpec.Windows pass = overhead.windows().orElseThrow();
        assertEquals(List.of(List.of(0, 1), List.of(2, 3)), pass.groups());
        assertEquals(6, pass.offsetSeconds(), 1e-9);
        assertEquals(
                List.of(4, 2), pass.spawns().stream().map(BossSpec.Spawn::count).toList());
        assertEquals("needler", pass.spawns().get(1).enemy().slug());
        assertEquals(1, pass.spawns().get(1).glideSeconds(), 1e-9);
        assertEquals(Math.toRadians(90), pass.spawns().get(0).arcRadians(), 1e-9);

        BossSpec.Phase broadside = boss.phases().get(1);
        assertEquals(70, broadside.seconds(), 1e-9);
        assertEquals(0, broadside.left());
        assertEquals(0, broadside.delaySeconds(), 1e-9, "a later phase that fires together has no default delay");
        BossSpec.Move move = broadside.move().orElseThrow();
        assertEquals(170, move.x(), 1e-9);
        assertEquals(PlayField.HEIGHT - 150, move.y(), 1e-9, "to: [x, y] px below the top edge");
        assertEquals(Layer.AIR, move.layer());
        assertEquals(1, move.pose());
        assertEquals(2, move.descendSeconds(), 1e-9);
        assertEquals(2, move.turnSeconds(), 1e-9);

        BossSpec.Attack fan = boss.attacks().get(0);
        assertEquals(BossSpec.Pattern.FAN, fan.pattern());
        assertEquals(5, fan.gun().fan());
        assertEquals(Math.toRadians(50), fan.gun().spreadRadians(), 1e-9);
        assertEquals(List.of(5), fan.parts());
        assertEquals(3, boss.attacks().get(1).arms());

        BossSpec.Phase core = boss.phases().get(2);
        assertFalse(core.timed());
        assertEquals(1, core.delaySeconds(), 1e-9);
        assertTrue(core.windows().orElseThrow().all());
        assertEquals(
                List.of("skitter", "needler"),
                boss.spawnKinds().stream().map(e -> e.slug()).toList());
    }

    @Test
    void theDifficultyHooksChangeTheSpawnsAndTheArms() {
        BossSpec easy = carrier(Difficulty.EASY).boss().orElseThrow();
        assertEquals(
                List.of(3, 1),
                easy.phases().get(0).windows().orElseThrow().spawns().stream()
                        .map(BossSpec.Spawn::count)
                        .toList());
        BossSpec hard = carrier(Difficulty.HARD).boss().orElseThrow();
        assertEquals(4, hard.attacks().get(1).arms());
        assertEquals(
                5, hard.phases().get(0).windows().orElseThrow().spawns().get(0).count());
        assertEquals(1, carrier(Difficulty.HARD).parts().get(5).hp(), 1e-9, "a fire-only part's HP is not scaled");
    }

    @Test
    void theNewNamesAreChecked() {
        var e = assertThrows(ContentException.class, () -> load(CARRIER.replace("pose: broadside", "pose: sideways")));
        assertTrue(e.getMessage().contains("no pose named 'sideways'"), e.getMessage());

        e = assertThrows(ContentException.class, () -> load(CARRIER.replace("pass-needlers: 1", "pass-gunners: 1")));
        assertTrue(e.getMessage().contains("no spawn named 'pass-gunners'"), e.getMessage());

        e = assertThrows(
                ContentException.class,
                () -> load(
                        CARRIER.replace("attacks: [core-spiral, core-ring]", "alternate: [core-spiral, core-ring]")));
        assertTrue(e.getMessage().contains("alternates without a duration"), e.getMessage());

        e = assertThrows(
                ContentException.class,
                () -> load(CARRIER.replace("kind: armoured, attack", "kind: armoured, hp: 10, attack")));
        assertTrue(e.getMessage().contains("an armoured part has no hp and no bounty"), e.getMessage());
    }
}
