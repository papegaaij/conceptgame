package vanguard.content;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import vanguard.sim.PlayField;

/**
 * M5 part G's level checks (design/campaign Level 13 Polar Relay; user decisions G1–G9 = a, pending the
 * user's review): a timed halt (a section of speed 0 without an arena) only with a {@code defend} primary,
 * its ramps fitting the sections beside it and nothing waiting to scroll in at the top edge during it (a
 * ground unit, ground object or placed pickup timed inside it, unless it walks a path or a lane, enters
 * from a side or drops at a point); the defend's ally, place, time and hook modes; the {@code integrity}
 * secondary and the {@code ally-below} cues only with a defend; lanes only for lane walkers, a {@code cross}
 * from all edges with a lane per unit, pairs only in a column; a nest from a side; the drops; the
 * mid-level jingle's time.
 */
final class PartGChecks {
    private final Content content;
    private final LevelData level;
    private final BiConsumer<String, String> problem;

    PartGChecks(Content content, LevelData level, BiConsumer<String, String> problem) {
        this.content = content;
        this.level = level;
        this.problem = problem;
    }

    void run(Set<String> levelEnemies) {
        List<double[]> halts = halts();
        checkHalts(halts);
        checkDefend(halts, levelEnemies);
        checkWaves(halts);
        checkGroundTargets(halts);
        for (int i = 0; i < level.pickups().size(); i++) {
            LevelData.PlacedPickup pickup = level.pickups().get(i);
            if (!pickup.carried() && inHalt(halts, pickup.at().orElseThrow().t())) {
                problem.accept(
                        "pickups[" + i + "].at",
                        "enters at the top edge in a timed halt, where it would wait for the halt's end: drop it at a"
                                + " point (drops)");
            }
        }
        List<LevelData.Drop> drops = level.released();
        for (int i = 0; i < drops.size(); i++) {
            checkTime("drops[" + i + "].t", drops.get(i).t());
        }
        level.music().jingle().ifPresent(jingle -> checkTime("music.jingle.t", jingle.t()));
        boolean defend = level.objectives().defend().isPresent();
        for (int i = 0; i < level.radio().size(); i++) {
            if (level.radio().get(i).event().orElse(null) == LevelData.CueEvent.ALLY_BELOW && !defend) {
                problem.accept("radio[" + i + "].event", "ally-below needs a defend primary");
            }
        }
    }

    /**
     * The timed halts, {@code [start, end)} on the level clock: a run of sections of speed 0 without an
     * arena is one halt (Level 13's three defend phases).
     */
    private List<double[]> halts() {
        List<double[]> halts = new ArrayList<>();
        for (int i = 0; i < level.sections().size(); i++) {
            if (!level.sections().get(i).timedHalt()) {
                continue;
            }
            double start = level.sectionStart(i);
            double end = level.sections().get(i).end();
            if (!halts.isEmpty() && halts.getLast()[1] == start) {
                halts.getLast()[1] = end;
            } else {
                halts.add(new double[] {start, end});
            }
        }
        return halts;
    }

    private static boolean inHalt(List<double[]> halts, double t) {
        return halts.stream().anyMatch(halt -> t >= halt[0] && t < halt[1]);
    }

    /**
     * A timed halt lies between sections that scroll; the ease into it is its first section's {@code
     * ramp}, the ease out of it its last section's (as the simulation reads them), each fitting the
     * scrolling section beside it; a section inside a longer halt has no ramp.
     */
    private void checkHalts(List<double[]> halts) {
        List<LevelData.Section> sections = level.sections();
        for (int i = 0; i < sections.size(); i++) {
            LevelData.Section section = sections.get(i);
            if (!section.timedHalt()) {
                continue;
            }
            String field = "sections[" + i + "]";
            if (level.objectives().defend().isEmpty()) {
                problem.accept(field + ".speed", "a timed halt (speed 0 without an arena) needs a defend primary");
            }
            if (i == 0 || i == sections.size() - 1) {
                problem.accept(field, "a timed halt lies between two sections that scroll");
                continue;
            }
            boolean first = !sections.get(i - 1).timedHalt();
            boolean last = !sections.get(i + 1).timedHalt();
            if (!first && !last && section.ramp().isPresent()) {
                problem.accept(field + ".ramp", "only a timed halt's first and last section ease (ramp)");
            }
            double ramp = section.rampSeconds();
            if (first && ramp > sections.get(i - 1).end() - level.sectionStart(i - 1)) {
                problem.accept(field + ".ramp", "the ease into the halt (" + ramp + " s) fits the section before it");
            }
            if (last && ramp > sections.get(i + 1).end() - section.end()) {
                problem.accept(field + ".ramp", "the ease out of the halt (" + ramp + " s) fits the section after it");
            }
        }
        if (!halts.isEmpty() && (level.boss().isPresent() || level.holds().isPresent())) {
            problem.accept("sections", "a timed halt comes without a boss arena or hold zones");
        }
    }

    private void checkDefend(List<double[]> halts, Set<String> levelEnemies) {
        level.objectives().defend().ifPresent(defend -> {
            AlliesData.Ally ally = content.allies().allies().get(defend.ally());
            if (ally == null) {
                problem.accept("objectives.defend.ally", "no ally '" + defend.ally() + "'");
            } else if (!ally.standing()) {
                problem.accept("objectives.defend.ally", "a defended ally follows the ground (follows: ground)");
            }
            boolean held = halts.stream().anyMatch(halt -> defend.from() >= halt[0] && defend.end() <= halt[1]);
            if (!held) {
                problem.accept(
                        "objectives.defend",
                        "the synchronisation (from " + defend.from() + " for " + defend.duration()
                                + " s) lies inside a timed halt, where its ally stands");
            }
            if (level.convoy().isPresent()
                    || level.boss().isPresent()
                    || level.holds().isPresent()) {
                problem.accept(
                        "objectives.defend", "a defend primary has no naval convoy, boss or hold zones beside it");
            }
            checkModes("objectives.defend.targeted_by", defend.targetedBy(), levelEnemies);
            for (Difficulty difficulty : List.of(Difficulty.EASY, Difficulty.HARD)) {
                defend.change(difficulty)
                        .flatMap(LevelData.DefendChange::targetedBy)
                        .ifPresent(modes -> checkModes(
                                "objectives.defend." + difficulty.name().toLowerCase(Locale.ROOT) + ".targeted_by",
                                modes,
                                levelEnemies));
            }
        });
    }

    private void checkModes(String field, Map<String, LevelData.HookMode> modes, Set<String> levelEnemies) {
        modes.forEach((slug, mode) -> {
            if (!content.enemies().containsKey(slug)) {
                problem.accept(field, "unknown enemy '" + slug + "'");
            } else if (!levelEnemies.contains(slug)) {
                problem.accept(field, "no wave or nest of '" + slug + "' in this level");
            }
        });
    }

    private void checkWaves(List<double[]> halts) {
        for (int i = 0; i < level.waves().size(); i++) {
            LevelData.Wave wave = level.waves().get(i);
            String field = "waves[" + i + "]";
            List<LevelData.Group> groups = wave.groupList();
            boolean cross = groups.stream().anyMatch(group -> group.formation().equals("cross"));
            boolean all = wave.from() == LevelData.Entry.ALL
                    || wave.easy().flatMap(LevelData.Change::from).orElse(null) == LevelData.Entry.ALL
                    || wave.hard().flatMap(LevelData.Change::from).orElse(null) == LevelData.Entry.ALL;
            if (cross != (wave.from() == LevelData.Entry.ALL) || (!cross && all)) {
                problem.accept(field + ".from", "a cross enters from all edges (from: all), other formations do not");
            }
            boolean column = groups.stream().allMatch(group -> group.formation().equals("column"));
            if (!column
                    && (wave.easy().flatMap(LevelData.Change::pairs).isPresent()
                            || wave.hard().flatMap(LevelData.Change::pairs).isPresent())) {
                problem.accept(field, "only a column enters in pairs (pairs)");
            }
            for (LevelData.Group group : groups) {
                if (!content.enemies().containsKey(group.enemy())) {
                    continue;
                }
                EnemyData enemy = content.enemy(group.enemy());
                boolean ground = enemy.layer().equals("ground");
                boolean walker = enemy.movement().walk().isPresent();
                boolean laneWalker = ground && !walker && enemy.segmentChain().isEmpty();
                if (wave.lanes().isPresent() && !laneWalker) {
                    problem.accept(
                            field + ".lanes",
                            "only a ground unit that does not walk follows lanes, not '" + group.enemy() + "'");
                }
                if (ground && !walker && wave.lanes().isEmpty() && inHalt(halts, wave.t())) {
                    problem.accept(
                            field + ".t",
                            "a ground unit entering at the top edge in a timed halt would wait there: give it lanes");
                }
                for (Difficulty difficulty : Difficulty.values()) {
                    if (!wave.fliesOn(difficulty)) {
                        continue;
                    }
                    int count = countOn(wave, group, difficulty);
                    String on = " on " + difficulty.name().toLowerCase(Locale.ROOT);
                    if (cross && wave.lanes().isPresent() && wave.lanes().get().size() < count) {
                        problem.accept(field + ".lanes", "a cross has a lane per unit: " + count + on);
                    }
                    if (cross && wave.lanes().isEmpty() && count % 4 != 0) {
                        problem.accept(
                                field + ".count", "a cross without lanes sends as many from each edge: " + count + on);
                    }
                }
            }
        }
    }

    private int countOn(LevelData.Wave wave, LevelData.Group group, Difficulty difficulty) {
        java.util.Optional<LevelData.Change> change =
                switch (difficulty) {
                    case EASY -> wave.easy();
                    case MEDIUM -> java.util.Optional.empty();
                    case HARD -> wave.hard();
                };
        return change.flatMap(LevelData.Change::count)
                .orElseGet(() -> content.difficulty().formationSize(group.count(), difficulty));
    }

    private void checkGroundTargets(List<double[]> halts) {
        for (int i = 0; i < level.groundTargets().size(); i++) {
            LevelData.GroundTarget target = level.groundTargets().get(i);
            String field = "ground_targets[" + i + "]";
            List<LevelData.Placement> all = new ArrayList<>(target.at());
            target.easy().flatMap(LevelData.Placements::at).ifPresent(all::addAll);
            target.hard().flatMap(LevelData.Placements::at).ifPresent(all::addAll);
            for (LevelData.Placement placement : all) {
                if (target.side().isPresent()) {
                    if (placement.x() <= 0 || placement.x() >= PlayField.HEIGHT) {
                        problem.accept(
                                field + ".at",
                                "a unit from a side crosses the edge inside the play field: y=" + placement.x());
                    }
                } else if (inHalt(halts, placement.t()) && target.drift().isEmpty()) {
                    problem.accept(
                            field + ".at",
                            "t=" + placement.t()
                                    + " enters at the top edge in a timed halt, where it would wait for the halt's"
                                    + " end");
                }
            }
        }
    }

    private void checkTime(String field, double t) {
        if (t < 0 || t >= level.seconds()) {
            problem.accept(field, "t=" + t + " is outside the level (0–" + level.seconds() + ")");
        }
    }
}
