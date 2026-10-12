package vanguard.content;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import vanguard.sim.BossSpec;
import vanguard.sim.EnemyGun;
import vanguard.sim.EnemySpec;
import vanguard.sim.LevelScript;
import vanguard.sim.PlayField;

/**
 * M5 part H's rules for the simulation (design/campaign Level 14 Siege Spire, design/enemies/bosses/siege-spire;
 * decisions H1–H10 = a of 2026-10-11 and the stated defaults, pending the user's review in the combined
 * round): a boss's {@code mortar} attack as a lob from its parts and its {@code laser-sweep} as a sweep from
 * a part, and a level's root bursts as placed ground units. Javadoc "M5 part H": M4's part H is the intel
 * and act summary work.
 */
final class PartHRules {
    private PartHRules() {}

    /**
     * A boss's {@code mortar} ({@link BossSpec.Pattern#LOB}) or {@code laser-sweep} ({@link
     * BossSpec.Pattern#SWEEP}) attack at {@code difficulty}, fired by {@code firing}: the lob's mortar (its
     * marker's time, its impact circle's radius, its ring by the hook's {@code ring}, the ring's bullets at
     * the attack's speed by the bullet-speed lever, the blob's direct hit) and the sweep's arc (the hook's
     * {@code sweep_arc}), times, beam and origin; {@code interval} already by the fire-rate lever, a sweep's
     * the turn it takes in its phase's alternation.
     */
    static BossSpec.Attack bossAttack(
            Content content,
            String name,
            BossSpec.Pattern pattern,
            EnemyData.Attack attack,
            Optional<EnemyData.Hook> hook,
            double interval,
            double damage,
            List<Integer> firing,
            Difficulty difficulty) {
        boolean staggered = attack.staggered().orElse(false);
        if (pattern == BossSpec.Pattern.LOB) {
            EnemyData.Mortar lob = attack.mortar().orElseThrow();
            EnemyGun gun = new EnemyGun(
                    interval,
                    0,
                    1,
                    attack.speed().orElseThrow()
                            * content.difficulty().enemyBulletSpeed().of(difficulty),
                    damage,
                    false,
                    1,
                    0,
                    Double.POSITIVE_INFINITY,
                    Double.POSITIVE_INFINITY,
                    Optional.empty(),
                    Optional.of(new EnemyGun.MortarSpec(
                            lob.marker(),
                            lob.impact() / 2,
                            hook.flatMap(EnemyData.Hook::ring).orElse(lob.ring()),
                            SimSpecs.bulletDamage(content, lob.ringBullet()))));
            return new BossSpec.Attack(
                    name,
                    pattern,
                    gun,
                    EnemyGun.BURST_GAP_SECONDS,
                    false,
                    1,
                    0,
                    0,
                    0,
                    firing,
                    staggered,
                    Optional.empty());
        }
        EnemyData.Sweep sweep = attack.sweep().orElseThrow();
        EnemySpec.Sweep beam = new EnemySpec.Sweep(
                Math.toRadians(hook.flatMap(EnemyData.Hook::sweepArc).orElse(sweep.arc())),
                sweep.duration(),
                sweep.telegraph(),
                sweep.length(),
                sweep.width(),
                interval,
                0,
                damage,
                sweep.origin().map(Point::x).orElse(0.0),
                sweep.origin().map(Point::y).orElse(0.0));
        return new BossSpec.Attack(
                name,
                pattern,
                EnemyGun.aimed(interval, 0, 1, 1, damage, false),
                EnemyGun.BURST_GAP_SECONDS,
                false,
                1,
                0,
                0,
                0,
                firing,
                staggered,
                Optional.of(beam));
    }

    /**
     * The level's root bursts at {@code difficulty} as ground units, in the order they appear (decision H2 =
     * a): burst i at its time, point and the telegraph, its growths on the difficulty {@code spacing} px
     * apart centred on its x, in the objectives' group the block names, firing first after the difficulty's
     * delay; each growth with the act HP factor of Level {@code number} of Act {@code act}.
     */
    static List<LevelScript.GroundUnit> rootBurstUnits(
            Content content, LevelData level, Difficulty difficulty, int act, int number) {
        if (level.rootBursts().isEmpty()) {
            return List.of();
        }
        LevelData.RootBursts block = level.rootBursts().get();
        int group = level.objectives().groups().indexOf(block.group());
        List<LevelScript.GroundUnit> units = new ArrayList<>();
        for (int i = 0; i < block.bursts().size(); i++) {
            LevelData.RootBurst data = block.bursts().get(i);
            LevelScript.RootBurst burst = burst(block, data, i, difficulty);
            List<String> growths = data.growthsOn(difficulty);
            for (int g = 0; g < growths.size(); g++) {
                EnemySpec enemy = SimSpecs.enemy(content, growths.get(g), difficulty, Optional.empty(), act, number);
                units.add(LevelScript.GroundUnit.fromBurst(burst, dx(block, growths.size(), g), enemy, group));
            }
        }
        return units;
    }

    /** Burst {@code index} of the level for the simulation. */
    static LevelScript.RootBurst burst(
            LevelData.RootBursts block, LevelData.RootBurst data, int index, Difficulty difficulty) {
        return new LevelScript.RootBurst(
                index, data.t(), data.x(), data.y(), block.telegraph(), block.firstShotDelay(difficulty));
    }

    /** Growth {@code g} of {@code n}'s place right of its burst's centre, px. */
    static double dx(LevelData.RootBursts block, int n, int g) {
        return (g - (n - 1) / 2.0) * block.spacing();
    }

    /** Whether every growth of the burst lies on the play field (its centre inside the side edges). */
    static boolean onField(LevelData.RootBursts block, LevelData.RootBurst burst, Difficulty difficulty) {
        int n = burst.growthsOn(difficulty).size();
        return burst.x() + dx(block, n, 0) >= 0 && burst.x() + dx(block, n, n - 1) <= PlayField.WIDTH;
    }
}
