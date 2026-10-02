package vanguard.content;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import vanguard.sim.PlayField;

/**
 * The checks across files, after every file has parsed: names resolve (a wave's enemy and
 * formation, an attack's bullet class, an item's availability), starters come first in their
 * lists, and each level's script fits its sections.
 */
final class ContentValidator {
    private final Content content;
    private final Map<Object, String> paths;
    private final List<String> problems = new ArrayList<>();

    ContentValidator(Content content, Map<Object, String> paths) {
        this.content = content;
        this.paths = paths;
    }

    List<String> problems() {
        checkItems();
        content.enemies().values().forEach(this::checkEnemy);
        content.levels().values().forEach(this::checkLevel);
        return problems;
    }

    private void checkItems() {
        ShieldData shields = content.shields();
        for (int i = 0; i < shields.models().size(); i++) {
            ShieldData.Model m = shields.models().get(i);
            checkItem(shields, "models[" + i + "]", i, m.price(), m.available());
        }
        ArmourData armour = content.armour();
        for (int i = 0; i < armour.plating().size(); i++) {
            ArmourData.Plating p = armour.plating().get(i);
            checkItem(armour, "plating[" + i + "]", i, p.price(), p.available());
        }
        GeneratorData generators = content.generators();
        for (int i = 0; i < generators.models().size(); i++) {
            GeneratorData.Model m = generators.models().get(i);
            checkItem(generators, "models[" + i + "]", i, m.price(), m.available());
        }
        SystemsData systems = content.systems();
        for (int i = 0; i < systems.engines().size(); i++) {
            SystemsData.Engine e = systems.engines().get(i);
            checkItem(systems, "engines[" + i + "]", i, e.price(), e.available());
        }
        for (int i = 0; i < systems.utility().size(); i++) {
            checkAvailable(
                    systems,
                    "utility[" + i + "].available",
                    systems.utility().get(i).available());
        }
    }

    /** The first model of a list is the starter: free and available from the start. */
    private void checkItem(Object file, String field, int index, int price, String available) {
        checkAvailable(file, field + ".available", available);
        if (index == 0 && (price != 0 || !available.equals("start"))) {
            problem(file, field, "the first entry is the starter: price 0, available start");
        }
    }

    private void checkAvailable(Object file, String field, String available) {
        if (!content.player().knows(available)) {
            problem(
                    file,
                    field,
                    "unknown availability '" + available + "' (known: Lnn or "
                            + String.join(
                                    ", ",
                                    new TreeSet<>(
                                            content.player().availability().keySet())) + ")");
        }
    }

    private void checkEnemy(EnemyData enemy) {
        for (int i = 0; i < enemy.formations().size(); i++) {
            checkFormation(
                    enemy,
                    "formations[" + i + "].name",
                    enemy.formations().get(i).name());
        }
        for (int i = 0; i < enemy.attacks().size(); i++) {
            String bullet = enemy.attacks().get(i).bullet();
            if (!content.enemyBasis().knowsBullet(bullet)) {
                problem(enemy, "attacks[" + i + "].bullet", "unknown bullet class '" + bullet + "'");
            }
        }
        enemy.difficulty().flatMap(EnemyData.Hooks::hard).ifPresent(hook -> hook.leadsTargetIn()
                .forEach(name -> checkFormation(enemy, "difficulty.hard.leads_target_in", name)));
    }

    private void checkFormation(Object file, String field, String name) {
        if (!content.enemyBasis().formations().containsKey(name)) {
            problem(file, field, "unknown formation '" + name + "' (see design/enemies/data.yaml, formations)");
        }
    }

    private void checkLevel(LevelData level) {
        Set<Double> waveTimes = new TreeSet<>();
        Set<String> levelEnemies = new TreeSet<>();
        double previous = 0;
        for (int i = 0; i < level.waves().size(); i++) {
            LevelData.Wave wave = level.waves().get(i);
            String field = "waves[" + i + "]";
            checkTime(level, field + ".t", wave.t());
            if (wave.t() < previous) {
                problem(level, field + ".t", "waves are listed in time order");
            }
            previous = wave.t();
            waveTimes.add(wave.t());
            List<LevelData.Group> groups = wave.groupList();
            for (int g = 0; g < groups.size(); g++) {
                LevelData.Group group = groups.get(g);
                String groupField = wave.enemy().isPresent() ? field : field + ".groups[" + g + "]";
                checkFormation(level, groupField + ".formation", group.formation());
                checkEnemyName(level, groupField + ".enemy", group.enemy());
                levelEnemies.add(group.enemy());
            }
        }
        Set<String> secrets =
                level.secrets().stream().map(LevelData.Secret::name).collect(Collectors.toSet());
        for (int i = 0; i < level.groundTargets().size(); i++) {
            LevelData.GroundTarget target = level.groundTargets().get(i);
            String field = "ground_targets[" + i + "]";
            if (target.section() < 1 || target.section() > level.sections().size()) {
                problem(level, field + ".section", "no section " + target.section());
            } else {
                for (int p = 0; p < target.at().size(); p++) {
                    LevelData.Placement placement = target.at().get(p);
                    if (level.sectionAt(placement.t()) != target.section()) {
                        problem(
                                level,
                                field + ".at[" + p + "]",
                                "t=" + placement.t() + " is not in section " + target.section());
                    }
                    if (placement.x() < 0 || placement.x() > PlayField.WIDTH) {
                        problem(level, field + ".at[" + p + "]", "x=" + placement.x() + " is outside the play field");
                    }
                }
            }
            target.reveals().ifPresent(name -> {
                if (!secrets.contains(name)) {
                    problem(level, field + ".reveals", "no secret '" + name + "'");
                }
            });
        }
        checkCarriers(level, "pickups", level.pickups(), waveTimes);
        level.difficulty()
                .easy()
                .flatMap(LevelData.Variant::extraPickups)
                .ifPresent(pickups -> checkCarriers(level, "difficulty.easy.extra_pickups", pickups, waveTimes));
        level.difficulty()
                .hard()
                .flatMap(LevelData.Variant::extraPickups)
                .ifPresent(pickups -> checkCarriers(level, "difficulty.hard.extra_pickups", pickups, waveTimes));
        for (int i = 0; i < level.radio().size(); i++) {
            LevelData.RadioCue cue = level.radio().get(i);
            String field = "radio[" + i + "]";
            cue.t().ifPresent(t -> checkTime(level, field + ".t", t));
            cue.enemy().ifPresent(enemy -> {
                if (!levelEnemies.contains(enemy)) {
                    problem(level, field + ".enemy", "no wave of '" + enemy + "' in this level");
                }
            });
        }
        LevelData.Music music = level.music();
        if (music.fullSection() > level.sections().size()) {
            problem(level, "music.full_section", "no section " + music.fullSection());
        }
        for (var variant : List.of(level.difficulty().easy(), level.difficulty().hard())) {
            variant.flatMap(LevelData.Variant::enemies)
                    .ifPresent(changes -> changes.keySet().forEach(slug -> checkEnemyName(level, "difficulty", slug)));
        }
    }

    private void checkCarriers(
            LevelData level, String field, List<LevelData.PlacedPickup> pickups, Set<Double> waveTimes) {
        for (int i = 0; i < pickups.size(); i++) {
            double wave = pickups.get(i).droppedBy().wave();
            if (!waveTimes.contains(wave)) {
                problem(level, field + "[" + i + "].dropped_by.wave", "no wave at t=" + wave);
            }
        }
    }

    private void checkTime(LevelData level, String field, double t) {
        if (t >= level.seconds()) {
            problem(level, field, "t=" + t + " is after the level end at " + level.seconds() + " s");
        }
    }

    private void checkEnemyName(Object file, String field, String slug) {
        if (!content.enemies().containsKey(slug)) {
            problem(
                    file,
                    field,
                    "unknown enemy '" + slug + "' (known: "
                            + String.join(", ", new TreeSet<>(content.enemies().keySet())) + ")");
        }
    }

    private void problem(Object file, String field, String message) {
        problems.add("design/" + paths.get(file) + ": " + field + ": " + message);
    }
}
