package vanguard.sim;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * M5 part H, an anchored boss's ambush launches (design/enemies/bosses/siege-spire, the maw's Wraiths;
 * decision H3 = a), planned once when the sortie is made so that a launch allocates nothing: per ambush
 * {@link BossSpec.Spawn}, per window part that launches it and per number of units in a launch, {@link
 * #SETS} launches of rear paths, the openings taking them in turn.
 *
 * <p>A launched unit flies cloaked from the window part's centre at the halt straight to its lane below
 * the bottom edge, past the ship, at the spawn's speed; its lanes lie evenly across the field as a rear
 * ambush wave's ({@code (i + 1) × 480 ÷ (n + 1)}); after its enemy's gap it comes back up its lane to its
 * hold point (a height in its hover range), decloaks, holds its hover time and leaves up the nearer side
 * lane, as Level 10's Wraiths (design/enemies/air/wraith). Its hold heights and times are drawn by a
 * generator of its own, so the planning leaves the sortie's sequence alone.
 */
final class Launches {
    /** The planned launches a spawn takes in turn from a part. */
    static final int SETS = 4;
    /** Salt for the launches' own generator. */
    private static final long SALT = 0x4D41574C41554EL;

    private final BossSpec.Spawn[] spawns;
    /** Per window part, its slot among the launching parts; -1 for one that launches nothing. */
    private final int[] partSlot;

    private final int slots;
    /** The most units in one launch (the largest spawn count). */
    private final int most;
    /** [spawn][part slot][set][units - 1][unit] */
    private final Spawn[][][][][] plans;
    /** The steps from a launch to the unit's re-entry at the bottom edge, as {@link #plans}. */
    private final int[][][][][] reentries;

    private Launches(
            BossSpec.Spawn[] spawns,
            int[] partSlot,
            int slots,
            int most,
            Spawn[][][][][] plans,
            int[][][][][] reentries) {
        this.spawns = spawns;
        this.partSlot = partSlot;
        this.slots = slots;
        this.most = most;
        this.plans = plans;
        this.reentries = reentries;
    }

    /**
     * The launches of {@code piece}'s boss, an anchored one halted with its centre at its {@code hoverY},
     * its window parts where its first pose places them; null when none of its windows launches an
     * ambush. {@code kinds} are the level's enemy kinds.
     */
    static Launches plan(SetPiece piece, List<EnemySpec> kinds, long seed) {
        BossSpec boss = piece.boss().orElseThrow();
        List<BossSpec.Spawn> ambushes = new ArrayList<>();
        int parts = piece.partCount();
        int[] partSlot = new int[parts];
        java.util.Arrays.fill(partSlot, -1);
        int slots = 0;
        int most = 0;
        for (BossSpec.Phase phase : boss.phases()) {
            if (phase.windows().isEmpty()) {
                continue;
            }
            BossSpec.Windows windows = phase.windows().get();
            boolean launches = false;
            for (BossSpec.Spawn spawn : windows.spawns()) {
                if (spawn.ambush()) {
                    launches = true;
                    most = Math.max(most, spawn.count());
                    if (!ambushes.contains(spawn)) {
                        ambushes.add(spawn);
                    }
                }
            }
            if (!launches) {
                continue;
            }
            for (List<Integer> group : windows.groups()) {
                for (int p : group) {
                    if (partSlot[p] < 0) {
                        partSlot[p] = slots++;
                    }
                }
            }
        }
        if (ambushes.isEmpty()) {
            return null;
        }
        SplitMix64 luck = new SplitMix64(seed ^ SALT);
        double[] partX = new double[parts];
        double[] partY = new double[parts];
        for (int p = 0; p < parts; p++) {
            LevelScript.PartSpec part = piece.spec().parts().get(p);
            BossSpec.Offset offset = boss.poses().isEmpty()
                    ? new BossSpec.Offset(part.dx(), part.dy())
                    : boss.poses().getFirst().offsets().get(p);
            partX[p] = boss.x() + offset.dx();
            partY[p] = boss.hoverY() + offset.dy();
        }
        int count = Math.max(1, most);
        Spawn[][][][][] plans = new Spawn[ambushes.size()][slots][SETS][count][];
        int[][][][][] reentries = new int[ambushes.size()][slots][SETS][count][];
        for (int s = 0; s < ambushes.size(); s++) {
            BossSpec.Spawn spawn = ambushes.get(s);
            EnemySpec enemy = spawn.enemy();
            int kind = kinds.indexOf(enemy);
            EnemySpec.Ambush ambush = enemy.ambush().orElseThrow();
            EnemySpec.Hover hover = enemy.hover().orElseThrow();
            double flash = enemy.cloak().map(EnemySpec.Cloak::flashSeconds).orElse(0.0);
            for (int p = 0; p < parts; p++) {
                if (partSlot[p] < 0) {
                    continue;
                }
                for (int set = 0; set < SETS; set++) {
                    for (int n = 1; n <= count; n++) {
                        Spawn[] units = new Spawn[n];
                        int[] reentry = new int[n];
                        for (int i = 0; i < n; i++) {
                            units[i] = unit(spawn, enemy, kind, ambush, hover, flash, partX[p], partY[p], i, n, luck);
                            reentry[i] = units[i].ambushReentryTick() - units[i].tick();
                        }
                        plans[s][partSlot[p]][set][n - 1] = units;
                        reentries[s][partSlot[p]][set][n - 1] = reentry;
                    }
                }
            }
        }
        return new Launches(ambushes.toArray(BossSpec.Spawn[]::new), partSlot, slots, count, plans, reentries);
    }

    /** Unit {@code i} of {@code n} launched from (fromX, fromY): its swoop, its rise, its hold and its exit. */
    private static Spawn unit(
            BossSpec.Spawn spawn,
            EnemySpec enemy,
            int kind,
            EnemySpec.Ambush ambush,
            EnemySpec.Hover hover,
            double flash,
            double fromX,
            double fromY,
            int i,
            int n,
            SplitMix64 luck) {
        double width = PlayField.WIDTH;
        double x = (i + 1) * width / (n + 1);
        double holdY = PlayField.HEIGHT - hover.depth().pick(luck);
        boolean left = x <= width / 2;
        double laneX = left ? ambush.lane() : width - ambush.lane();
        FlightPath down = FlightPath.through(fromX, fromY, x, -Formations.OUTSIDE);
        FlightPath rise = FlightPath.through(x, -Formations.OUTSIDE, x, holdY);
        FlightPath exit = FlightPath.through(
                x,
                holdY,
                laneX,
                holdY + Formations.AMBUSH_EXIT_RISE,
                laneX,
                holdY + 2 * Formations.AMBUSH_EXIT_RISE,
                laneX,
                PlayField.HEIGHT + Formations.OUTSIDE);
        return new Spawn(
                0,
                kind,
                enemy,
                down,
                spawn.speed(),
                0,
                Optional.empty(),
                Spawn.Exit.DOWN,
                false,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.of(new Spawn.Ambush(
                        rise,
                        ambush.gapSeconds(),
                        flash,
                        hover.seconds().pick(luck),
                        exit,
                        ambush.exitSpeed(),
                        WaveSchedule.EDGE_WARNING_SECONDS)),
                Optional.empty());
    }

    /** The index of {@code spawn} among the planned ones; -1 for one without a plan. */
    private int spawnIndex(BossSpec.Spawn spawn) {
        for (int s = 0; s < spawns.length; s++) {
            if (spawns[s] == spawn) {
                return s;
            }
        }
        return -1;
    }

    /**
     * The plan of unit {@code unit} of {@code units} of {@code spawn}'s launch number {@code opening}
     * from window part {@code part}; null when it has none.
     */
    Spawn plan(BossSpec.Spawn spawn, int part, int opening, int unit, int units) {
        int s = spawnIndex(spawn);
        if (s < 0 || part < 0 || part >= partSlot.length || partSlot[part] < 0 || units < 1 || units > most) {
            return null;
        }
        return plans[s][partSlot[part]][Math.floorMod(opening, SETS)][units - 1][unit];
    }

    /** As {@link #plan}: the steps from the launch to that unit's re-entry at the bottom edge. */
    int reentry(BossSpec.Spawn spawn, int part, int opening, int unit, int units) {
        return reentries[spawnIndex(spawn)][partSlot[part]][Math.floorMod(opening, SETS)][units - 1][unit];
    }

    /** The window parts that launch ambushes. */
    int slots() {
        return slots;
    }
}
