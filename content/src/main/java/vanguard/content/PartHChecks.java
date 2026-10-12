package vanguard.content;

import java.util.List;
import java.util.function.BiConsumer;
import vanguard.sim.LevelScript;

/**
 * M5 part H's checks (design/campaign Level 14 Siege Spire, design/enemies/bosses/siege-spire; decisions
 * H1–H10 = a, pending the user's review). A boss's: a {@code staggered} attack only on a boss, fired by more
 * than one part; a {@code mortar} or {@code laser-sweep} attack of a boss fired by its parts; an ambush
 * launch only of a unit that flies the rear-ambush path; a sway on an anchored boss only with a phase
 * that lifts it. A level's root bursts: their group one of the objectives' groups, their growths units that
 * stand on the ground, each burst 100–400 px below the top edge with its growths on the field on every
 * difficulty, timed in a section that scrolls and grown before the level's arena starts.
 */
final class PartHChecks {
    private PartHChecks() {}

    /** A boss's M5 part H keys. */
    static void enemy(Content content, EnemyData enemy, BiConsumer<String, String> problem) {
        for (int i = 0; i < enemy.attacks().size(); i++) {
            EnemyData.Attack attack = enemy.attacks().get(i);
            String field = "attacks[" + i + "]";
            if (attack.staggered().orElse(false)) {
                long firing = enemy.partList().orElse(List.of()).stream()
                        .filter(part -> part.attack().equals(attack.name()))
                        .count();
                if (enemy.boss().isEmpty() || firing < 2) {
                    problem.accept(field + ".staggered", "only a boss's attack fired by several parts is staggered");
                }
            }
        }
        if (enemy.boss().isEmpty()) {
            return;
        }
        EnemyData.BossData boss = enemy.boss().get();
        boolean lifts = false;
        for (int f = 0; f < boss.phases().size(); f++) {
            EnemyData.PhaseData phase = boss.phases().get(f);
            lifts |= phase.move().isPresent();
            List<EnemyData.SpawnData> spawns =
                    phase.windows().flatMap(EnemyData.WindowData::spawns).orElse(List.of());
            for (int s = 0; s < spawns.size(); s++) {
                EnemyData.SpawnData spawn = spawns.get(s);
                if (spawn.ambush().isPresent()
                        && content.enemies().containsKey(spawn.enemy())
                        && (content.enemy(spawn.enemy()).movement().ambush().isEmpty()
                                || content.enemy(spawn.enemy())
                                        .movement()
                                        .hover()
                                        .isEmpty())) {
                    problem.accept(
                            "boss.phases[" + f + "].windows.spawns[" + s + "].ambush",
                            "an ambush launch flies its enemy's rear-ambush path: '" + spawn.enemy()
                                    + "' has no ambush and hover movement");
                }
            }
        }
        if (enemy.movement().anchored().isPresent()
                && enemy.movement().sine().filter(sine -> sine.amplitude() > 0).isPresent()
                && !lifts) {
            problem.accept("movement.sine", "an anchored boss sways only after a phase's move lifts it");
        }
    }

    /** A level's root bursts. */
    static void level(Content content, LevelData level, BiConsumer<String, String> problem) {
        if (level.rootBursts().isEmpty()) {
            return;
        }
        LevelData.RootBursts block = level.rootBursts().get();
        if (!level.objectives().groups().contains(block.group())) {
            problem.accept(
                    "root_bursts.group",
                    "no group '" + block.group() + "' in the objectives' groups "
                            + level.objectives().groups());
        }
        double arena = Double.POSITIVE_INFINITY;
        for (int i = 0; i < level.sections().size(); i++) {
            if (level.sections().get(i).isArena()) {
                arena = level.sectionStart(i);
            }
        }
        for (int i = 0; i < block.bursts().size(); i++) {
            LevelData.RootBurst burst = block.bursts().get(i);
            String field = "root_bursts.bursts[" + i + "]";
            if (burst.y() < LevelScript.RootBurst.MIN_DEPTH || burst.y() > LevelScript.RootBurst.MAX_DEPTH) {
                problem.accept(
                        field + ".y",
                        "a burst lies " + LevelScript.RootBurst.MIN_DEPTH + "–" + LevelScript.RootBurst.MAX_DEPTH
                                + " px below the top edge, was " + burst.y());
            }
            if (burst.t() >= level.seconds()) {
                problem.accept(field + ".t", "t=" + burst.t() + " is after the level's end");
                continue;
            }
            int section = level.sectionAt(burst.t());
            if (level.sections()
                    .get(section - 1)
                    .speed()
                    .filter(speed -> speed == 0)
                    .isPresent()) {
                problem.accept(field + ".t", "a burst scrolls with the ground: not in a section of speed 0");
            }
            if (burst.t() + block.telegraph() >= arena) {
                problem.accept(field + ".t", "the growths appear before the arena starts (" + arena + ")");
            }
            for (Difficulty difficulty : Difficulty.values()) {
                if (!PartHRules.onField(block, burst, difficulty)) {
                    problem.accept(field + ".x", "its growths leave the play field on " + difficulty);
                }
                for (String slug : burst.growthsOn(difficulty)) {
                    if (!content.enemies().containsKey(slug)) {
                        problem.accept(field + ".growths", "no enemy '" + slug + "'");
                    } else if (content.enemy(slug).movement().terrain().isEmpty()) {
                        problem.accept(
                                field + ".growths",
                                "'" + slug + "' does not stand on the ground (no terrain movement)");
                    }
                }
            }
        }
    }
}
