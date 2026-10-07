package vanguard.sim;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * The level's waves planned unit by unit ({@link Formations}), in the order they enter, and the
 * edge warnings for the waves that enter from the sides or the rear (design/enemies, bullet
 * readability rules: an arrow at that edge at least 1.5 s ahead). Built once per level; an
 * attempt walks through it with a cursor.
 */
final class WaveSchedule {
    /** The shortest edge warning. */
    static final double EDGE_WARNING_SECONDS = 3;

    private final List<EnemySpec> kinds;
    private final Spawn[] spawns;
    /** Per planned unit, the edge its wave enters from. */
    private final WaveSpec.Entry[] entries;
    /**
     * Per planned unit, its wave's volley clock (M5 part B: a walker wave with a fan stagger, design/
     * enemies/ground/creeper) and its place in the wave; -1 and 0 for a unit without one.
     */
    private final int[] volleyGroups;

    private final int[] volleyUnits;
    /** The number of volley clocks: one per staggered walker wave. */
    private final int volleyClocks;

    private final int[] warningStarts;
    private final int[] warningEnds;
    private final int[] warningEdges;
    private int next;

    /** @param kinds the level's distinct enemies, every wave's among them; a spawn's kind indexes it */
    WaveSchedule(List<WaveSpec> waves, List<EnemySpec> kinds, SplitMix64 rng) {
        this.kinds = kinds;
        List<Spawn> planned = new ArrayList<>();
        List<WaveSpec> warned = new ArrayList<>();
        Map<Spawn, WaveSpec.Entry> entryOf = new IdentityHashMap<>();
        Map<Spawn, int[]> volleyOf = new IdentityHashMap<>();
        int clocks = 0;
        for (WaveSpec wave : waves) {
            int before = planned.size();
            Formations.plan(wave, kinds.indexOf(wave.enemy()), rng, planned);
            for (int i = before; i < planned.size(); i++) {
                entryOf.put(planned.get(i), wave.entry());
            }
            if (staggered(wave)) {
                for (int i = before; i < planned.size(); i++) {
                    volleyOf.put(planned.get(i), new int[] {clocks, i - before});
                }
                clocks++;
            }
            if (wave.entry() != WaveSpec.Entry.FRONT) {
                warned.add(wave);
            }
        }
        planned.sort(Comparator.comparingInt(Spawn::tick));
        spawns = planned.toArray(Spawn[]::new);
        entries = new WaveSpec.Entry[spawns.length];
        volleyGroups = new int[spawns.length];
        volleyUnits = new int[spawns.length];
        for (int i = 0; i < spawns.length; i++) {
            entries[i] = entryOf.getOrDefault(spawns[i], WaveSpec.Entry.FRONT);
            int[] volley = volleyOf.getOrDefault(spawns[i], new int[] {-1, 0});
            volleyGroups[i] = volley[0];
            volleyUnits[i] = volley[1];
        }
        volleyClocks = clocks;
        // A chain's loop-back re-enters from the bottom edge: warned like a rear entry.
        List<Spawn> loops =
                planned.stream().filter(spawn -> spawn.loop().isPresent()).toList();
        int warnings = warned.size() + loops.size();
        warningStarts = new int[warnings];
        warningEnds = new int[warnings];
        warningEdges = new int[warnings];
        for (int i = 0; i < warned.size(); i++) {
            WaveSpec wave = warned.get(i);
            double lead = Math.max(EDGE_WARNING_SECONDS, wave.warningSeconds().orElse(0.0));
            warningStarts[i] = SimStep.ticks(wave.t() - lead);
            warningEnds[i] = SimStep.ticks(wave.t());
            warningEdges[i] = edges(wave);
        }
        for (int i = 0; i < loops.size(); i++) {
            Spawn spawn = loops.get(i);
            int end = spawn.reentryTick();
            warningStarts[warned.size() + i] =
                    end - SimStep.ticks(spawn.loop().orElseThrow().warningSeconds());
            warningEnds[warned.size() + i] = end;
            warningEdges[warned.size() + i] = WarningEdge.BOTTOM.bit();
        }
    }

    /**
     * Whether a wave's units share a staggered volley clock (a walker with a fan stagger); its last
     * unit's turn must come before the next volley, so a volley never overlaps the next.
     */
    private static boolean staggered(WaveSpec wave) {
        EnemySpec enemy = wave.enemy();
        if (enemy.walker().isEmpty()
                || !enemy.walker().get().staggered()
                || enemy.gun().isEmpty()) {
            return false;
        }
        double stagger = enemy.walker().get().staggerSeconds();
        double interval = enemy.gun().get().intervalSeconds();
        if (SimStep.ticks((wave.count() - 1) * stagger) >= SimStep.ticks(interval)) {
            throw new IllegalArgumentException(enemy.slug() + ": a wave of " + wave.count() + " staggered " + stagger
                    + " s apart does not fit its " + interval + " s volley interval");
        }
        return true;
    }

    private static int edges(WaveSpec wave) {
        if (wave.entry() == WaveSpec.Entry.REAR) {
            return WarningEdge.BOTTOM.bit();
        }
        return switch (wave.edge()) {
            case LEFT -> WarningEdge.LEFT.bit();
            case RIGHT -> WarningEdge.RIGHT.bit();
            case NONE, ALTERNATING -> WarningEdge.LEFT.bit() | WarningEdge.RIGHT.bit();
        };
    }

    /** Back to the level start. */
    void reset() {
        next = 0;
    }

    /** The next unit's index in the plan. */
    int next() {
        return next;
    }

    /** Back to the plan's unit {@code index} (a boss checkpoint). */
    void next(int index) {
        next = index;
    }

    /** The next unit if it enters at or before {@code tick}, advancing past it; otherwise {@code null}. */
    Spawn due(int tick) {
        return next < spawns.length && spawns[next].tick() <= tick ? spawns[next++] : null;
    }

    /** The edge the wave of the unit {@link #due} returned last enters from. */
    WaveSpec.Entry lastEntry() {
        return entries[next - 1];
    }

    /** The volley clock of the unit {@link #due} returned last; -1 without one. */
    int lastVolleyGroup() {
        return volleyGroups[next - 1];
    }

    /** The place in its wave (from 0, in entry order) of the unit {@link #due} returned last. */
    int lastVolleyUnit() {
        return volleyUnits[next - 1];
    }

    /** The number of volley clocks: one per staggered walker wave. */
    int volleyClocks() {
        return volleyClocks;
    }

    /** The edges with a warning showing at {@code tick}, as {@link WarningEdge} bits. */
    int warnings(int tick) {
        int edges = 0;
        for (int i = 0; i < warningStarts.length; i++) {
            if (tick >= warningStarts[i] && tick < warningEnds[i]) {
                edges |= warningEdges[i];
            }
        }
        return edges;
    }

    /** The units of the enemy {@code slug} the level sends. */
    int unitsOf(String slug) {
        int count = 0;
        for (Spawn spawn : spawns) {
            count += spawn.enemy().slug().equals(slug) ? 1 : 0;
        }
        return count;
    }

    /** Every unit the level sends. */
    int units() {
        return spawns.length;
    }

    /** The distinct enemies of the level; a spawn's kind indexes this list. */
    List<EnemySpec> kinds() {
        return kinds;
    }
}
