package vanguard.content;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import vanguard.sim.LevelScript;
import vanguard.sim.PlayField;
import vanguard.sim.Road;
import vanguard.sim.SimStep;

/**
 * The checks across files, after every file has parsed: names resolve (a wave's enemy and
 * formation, an attack's bullet class, an item's availability), starters come first in their
 * lists, and each level's script fits its sections and its backdrop follows the art direction
 * ({@link BackdropCheck}).
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
        checkWingmen();
        content.enemies().values().forEach(this::checkEnemy);
        content.levels().values().forEach(this::checkLevel);
        content.levels().forEach(this::checkAct);
        return problems;
    }

    /** A level's act has its data file, and the act's levels include the level. */
    private void checkAct(String key, LevelData level) {
        ActData act = content.acts().get(Content.actDirectory(key));
        int number = Content.levelNumber(key);
        if (act == null) {
            problem(level, "level", "its act " + Content.actDirectory(key) + " has no data file");
        } else if (!act.levels().contains(number)) {
            problem(act, "levels", "does not include level " + number + " (" + key + ")");
        }
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
        for (int i = 0; i < systems.bays().extra().size(); i++) {
            checkAvailable(
                    systems,
                    "bays.extra[" + i + "].available",
                    systems.bays().extra().get(i).available());
        }
        for (int i = 0; i < systems.utility().size(); i++) {
            checkAvailable(
                    systems,
                    "utility[" + i + "].available",
                    systems.utility().get(i).available());
        }
    }

    /**
     * Rook's guns are based on player weapons the simulation flies, each with a price, so his
     * escort shop can derive theirs.
     */
    private void checkWingmen() {
        WingmenData wingmen = content.wingmen();
        var guns = wingmen.guns().list();
        for (int i = 0; i < guns.size(); i++) {
            WingmenData.Gun gun = guns.get(i);
            String field = "guns.list[" + i + "]";
            checkAvailable(wingmen, field + ".available", gun.available());
            WeaponData weapon = content.weapons().get(gun.base());
            if (weapon == null) {
                problem(wingmen, field + ".base", "no player weapon '" + gun.base() + "'");
            } else if (!SimSpecs.flies(content, gun.base())) {
                problem(wingmen, field + ".base", "its weapon's hits '" + weapon.hits() + "' do not fly");
            }
        }
        if (wingmen.barks()
                .bark(WingmenData.ROOK_ARMOUR)
                .flatMap(WingmenData.Bark::below)
                .isEmpty()) {
            problem(wingmen, "barks.triggers", "no " + WingmenData.ROOK_ARMOUR + " trigger with its share below");
        }
        if (!content.voices().speakers().containsKey(wingmen.barks().speaker().toLowerCase(Locale.ROOT))) {
            problem(
                    wingmen,
                    "barks.speaker",
                    "no voice speaker '" + wingmen.barks().speaker() + "'");
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
            EnemyData.Attack attack = enemy.attacks().get(i);
            String field = "attacks[" + i + "]";
            attack.bullet().ifPresent(bullet -> checkBullet(enemy, field + ".bullet", bullet));
            boolean walker = enemy.movement().walk().isPresent();
            if (attack.stagger().isPresent() && !walker) {
                // M5 part B: only a walker wave shares a staggered volley clock so far.
                problem(enemy, field + ".stagger", "only a walker's fan is staggered");
            }
            if (walker
                    && attack.pattern().equals("fan")
                    && attack.aim().filter("down"::equals).isPresent()) {
                problem(enemy, field + ".aim", "a walker's fan aims at the target or along its facing");
            }
            if (attack.pounce().isPresent() && !walker) {
                // M5 part C: a pounce leaps off a walker's path and rejoins it.
                problem(enemy, field + ".pounce", "only a walker pounces");
            }
            attack.spawn().ifPresent(spawn -> checkEnemyName(enemy, field + ".spawn.enemy", spawn.enemy()));
            attack.mine().ifPresent(mine -> checkBullet(enemy, field + ".mine.ring_bullet", mine.ringBullet()));
            attack.mortar().ifPresent(mortar -> checkBullet(enemy, field + ".mortar.ring_bullet", mortar.ringBullet()));
            attack.sweep().flatMap(EnemyData.Sweep::origin).ifPresent(origin -> {
                if (Math.abs(origin.x()) > enemy.size().width() / 2
                        || Math.abs(origin.y()) > enemy.size().height() / 2) {
                    problem(
                            enemy,
                            field + ".sweep.origin",
                            "the eye lies outside the " + enemy.size().width() + "×"
                                    + enemy.size().height() + " sprite");
                }
            });
        }
        for (var hook : List.of(
                enemy.difficulty().flatMap(EnemyData.Hooks::easy),
                enemy.difficulty().flatMap(EnemyData.Hooks::hard))) {
            hook.flatMap(EnemyData.Hook::deathBurst)
                    .ifPresent(puff -> checkBullet(enemy, "difficulty.death_burst.bullet", puff.bullet()));
        }
        if (enemy.attacks().stream()
                        .filter(attack -> attack.spawn().isPresent())
                        .count()
                > 1) {
            // M5 part C: a brood or a periodic spawn, one per unit.
            problem(enemy, "attacks", "a unit has at most one spawn");
        }
        if (enemy.attacks().stream()
                        .filter(attack -> attack.pounce().isPresent())
                        .count()
                > 1) {
            problem(enemy, "attacks", "a unit has at most one pounce");
        }
        checkParts(enemy);
        checkBoss(enemy);
        checkPartD(enemy);
        enemy.difficulty()
                .flatMap(EnemyData.Hooks::hard)
                .flatMap(EnemyData.Hook::leadsTargetIn)
                .ifPresent(
                        names -> names.forEach(name -> checkFormation(enemy, "difficulty.hard.leads_target_in", name)));
    }

    /**
     * A boss's chains end on its parts, its phases name its parts and attacks, its streams known
     * enemies, an alternating spiral has a duration; a difficulty's attack changes name its attacks
     * and its spawn counts its spawns; its poses place its parts (part G).
     */
    private void checkBoss(EnemyData enemy) {
        List<EnemyData.ChainData> chains = enemy.chains().orElse(List.of());
        for (int i = 0; i < chains.size(); i++) {
            checkPartName(enemy, "chains[" + i + "].to", chains.get(i).to());
        }
        for (var hook : List.of(
                enemy.difficulty().flatMap(EnemyData.Hooks::easy),
                enemy.difficulty().flatMap(EnemyData.Hooks::hard))) {
            hook.flatMap(EnemyData.Hook::attacks)
                    .ifPresent(changes -> changes.keySet().forEach(name -> {
                        if (enemy.attack(name).isEmpty()) {
                            problem(enemy, "difficulty.attacks", "no attack named '" + name + "'");
                        }
                    }));
        }
        if (enemy.boss().isEmpty()) {
            return;
        }
        if (enemy.partList().isEmpty()) {
            problem(enemy, "boss", "a boss is a multi-part unit (part_list)");
        }
        List<EnemyData.PhaseData> phases = enemy.boss().get().phases();
        for (int i = 0; i < phases.size(); i++) {
            EnemyData.PhaseData phase = phases.get(i);
            String field = "boss.phases[" + i + "]";
            phase.until()
                    .parts()
                    .ifPresent(parts -> parts.forEach(part -> checkPartName(enemy, field + ".until.parts", part)));
            phase.exposes().ifPresent(parts -> parts.forEach(part -> checkPartName(enemy, field + ".exposes", part)));
            phase.attacks()
                    .or(phase::alternate)
                    .ifPresent(names -> names.forEach(name -> {
                        if (enemy.attack(name).isEmpty()) {
                            problem(enemy, field + ".attacks", "no attack named '" + name + "'");
                        }
                    }));
            phase.alternate()
                    .ifPresent(names -> names.forEach(name -> enemy.attack(name)
                            .filter(attack -> attack.pattern().equals("spiral")
                                    && attack.duration().isEmpty())
                            .ifPresent(attack -> problem(
                                    enemy,
                                    field + ".alternate",
                                    "spiral '" + name + "' alternates without a duration"))));
            phase.streams().ifPresent(stream -> checkEnemyName(enemy, field + ".streams.enemy", stream.enemy()));
            checkBossPartG(enemy, field, phase);
        }
        EnemyData.BossData boss = enemy.boss().get();
        for (var hook : List.of(
                enemy.difficulty().flatMap(EnemyData.Hooks::easy),
                enemy.difficulty().flatMap(EnemyData.Hooks::hard))) {
            hook.flatMap(EnemyData.Hook::spawns)
                    .ifPresent(counts -> counts.keySet().forEach(name -> {
                        boolean known = boss.phases().stream()
                                .flatMap(f -> f.windows().flatMap(EnemyData.WindowData::spawns).stream())
                                .flatMap(List::stream)
                                .anyMatch(spawn -> spawn.name().equals(name));
                        if (!known) {
                            problem(enemy, "difficulty.spawns", "no spawn named '" + name + "'");
                        }
                    }));
        }
        boss.poses().ifPresent(poses -> {
            for (int i = 0; i < poses.size(); i++) {
                for (String part : poses.get(i).offsets().keySet()) {
                    checkPartName(enemy, "boss.poses[" + i + "].offsets", part);
                }
            }
        });
    }

    /**
     * Part G: a phase's move turns into a pose of the boss, and its windows hold its parts and
     * release known enemies.
     */
    private void checkBossPartG(EnemyData enemy, String field, EnemyData.PhaseData phase) {
        EnemyData.BossData boss = enemy.boss().orElseThrow();
        phase.move().flatMap(EnemyData.MoveData::pose).ifPresent(pose -> {
            if (boss.poseIndex(pose) < 0) {
                problem(enemy, field + ".move.pose", "no pose named '" + pose + "'");
            }
        });
        phase.windows().ifPresent(windows -> {
            windows.groups()
                    .forEach(group -> group.forEach(part -> checkPartName(enemy, field + ".windows.groups", part)));
            windows.spawns()
                    .ifPresent(spawns -> spawns.forEach(
                            spawn -> checkEnemyName(enemy, field + ".windows.spawns.enemy", spawn.enemy())));
        });
    }

    private void checkPartName(EnemyData enemy, String field, String part) {
        if (enemy.partIndex(part) < 0) {
            problem(enemy, field, "no part named '" + part + "'");
        }
    }

    private void checkBullet(Object file, String field, String bullet) {
        if (!content.enemyBasis().knowsBullet(bullet)) {
            problem(file, field, "unknown bullet class '" + bullet + "'");
        }
    }

    /**
     * A multi-part unit lists its parts: their HP and bounties add up to the unit's, a part's
     * attack names one of the unit's attacks, and it has a vital part.
     */
    private void checkParts(EnemyData enemy) {
        if (enemy.multiPart() != enemy.partList().isPresent()) {
            problem(enemy, "part_list", "a unit with parts: multi lists its parts, a single one none");
        }
        if (enemy.partList().isEmpty()) {
            return;
        }
        List<EnemyData.PartData> parts = enemy.partList().get();
        // A segment chain's body adds its segments to the parts.
        double hp = parts.stream().mapToDouble(EnemyData.PartData::hp).sum()
                + enemy.segmentChain().map(c -> c.segments() * c.hp()).orElse(0.0);
        if (hp != enemy.hp()) {
            problem(enemy, "hp", "is " + enemy.hp() + ", the parts' HP add up to " + hp);
        }
        int bounty = parts.stream().mapToInt(EnemyData.PartData::bounty).sum()
                + enemy.segmentChain().map(c -> c.segments() * c.bounty()).orElse(0);
        if (bounty != enemy.bounty()) {
            problem(enemy, "bounty", "is " + enemy.bounty() + ", the parts' bounties add up to " + bounty);
        }
        if (parts.stream().noneMatch(part -> part.kind().equals("vital"))) {
            problem(enemy, "part_list", "has no vital part");
        }
        for (int i = 0; i < parts.size(); i++) {
            EnemyData.PartData part = parts.get(i);
            int index = i;
            part.attack().ifPresent(name -> {
                if (enemy.attack(name).isEmpty()) {
                    problem(enemy, "part_list[" + index + "].attack", "no attack named '" + name + "'");
                }
            });
        }
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
                if (group.formation().equals("whirl cluster") && wave.at().isEmpty()) {
                    problem(level, field + ".at", "a whirl cluster needs its release point");
                }
            }
            wave.at().ifPresent(at -> {
                if (at.x() < 0 || at.x() > PlayField.WIDTH || at.y() < 0 || at.y() > PlayField.HEIGHT) {
                    problem(level, field + ".at", "[" + at.x() + ", " + at.y() + "] is outside the play field");
                }
            });
        }
        checkSetPieces(level, levelEnemies);
        level.boss().ifPresent(boss -> {
            checkEnemyName(level, "boss.enemy", boss.enemy());
            levelEnemies.add(boss.enemy());
            if (content.enemies().containsKey(boss.enemy())
                    && content.enemy(boss.enemy()).boss().isEmpty()) {
                problem(level, "boss.enemy", "'" + boss.enemy() + "' has no boss script");
            }
            if (boss.section() > level.sections().size()) {
                problem(level, "boss.section", "no section " + boss.section());
            }
            checkTime(level, "boss.t", boss.t());
        });
        if (level.sections().stream().filter(LevelData.Section::isArena).count() > 1
                || (level.sections().stream().anyMatch(LevelData.Section::isArena)
                        && level.boss().isEmpty())) {
            problem(level, "sections", "a level has at most one arena, and only with a boss");
        }
        checkDebris(level);
        checkLevelRules(level);
        level.prompts().ifPresent(prompts -> {
            for (int i = 0; i < prompts.size(); i++) {
                checkTime(level, "prompts[" + i + "].t", prompts.get(i).t());
            }
        });
        level.objectives().secondary().flatMap(LevelData.Secondary::escapes).ifPresent(slug -> {
            if (!levelEnemies.contains(slug)) {
                problem(level, "objectives.secondary.escapes", "no wave of '" + slug + "' in this level");
            }
        });
        level.crateSeconds().ifPresent(seconds -> {
            if (level.secrets().isEmpty()) {
                problem(level, "crate_seconds", "a level without secrets has no crate");
            } else if (seconds < content.player().pickupSeconds()) {
                problem(
                        level,
                        "crate_seconds",
                        "shorter than the player's pickup_seconds ("
                                + content.player().pickupSeconds() + ")");
            }
        });
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
            target.enemy().ifPresent(slug -> {
                checkEnemyName(level, field + ".enemy", slug);
                levelEnemies.add(slug);
                if (content.enemies().containsKey(slug)
                        && content.enemy(slug).movement().terrain().isEmpty()) {
                    problem(
                            level,
                            field + ".enemy",
                            "'" + slug + "' does not stand on the ground (no terrain movement)");
                }
            });
            target.group().ifPresent(group -> checkGroup(level, field + ".group", group));
        }
        for (int i = 0; i < level.cranes().orElse(List.of()).size(); i++) {
            LevelData.CraneData crane = level.cranes().get().get(i);
            String field = "cranes[" + i + "]";
            crane.swings().forEach(t -> checkTime(level, field + ".swings", t));
            crane.clamp().ifPresent(clamp -> {
                if (!secrets.contains(clamp.reveals())) {
                    problem(level, field + ".clamp.reveals", "no secret '" + clamp.reveals() + "'");
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
            cue.group().ifPresent(group -> checkGroup(level, field + ".group", group));
        }
        checkEscort(level, levelEnemies);
        LevelData.Music music = level.music();
        if (music.fullSection() > level.sections().size()) {
            problem(level, "music.full_section", "no section " + music.fullSection());
        }
        music.stems().ifPresent(stems -> stems.keySet().forEach(section -> {
            if (section < 1 || section > level.sections().size()) {
                problem(level, "music.stems", "no section " + section);
            }
        }));
        music.voiceLoop().ifPresent(loop -> {
            if (loop.section() > level.sections().size()) {
                problem(level, "music.voice_loop.section", "no section " + loop.section());
            }
            if (level.radio().stream()
                    .noneMatch(cue -> cue.t().isPresent() && cue.speaker().equals(loop.speaker()))) {
                problem(level, "music.voice_loop.speaker", "no timed radio line of " + loop.speaker());
            }
        });
        music.ambienceFrom().ifPresent(t -> checkTime(level, "music.ambience_from", t));
        for (var variant : List.of(level.difficulty().easy(), level.difficulty().hard())) {
            variant.flatMap(LevelData.Variant::enemies)
                    .ifPresent(changes -> changes.keySet().forEach(slug -> checkEnemyName(level, "difficulty", slug)));
        }
        checkPartC(level, levelEnemies);
        checkPartD(level);
        new BackdropCheck(level, (field, message) -> problem(level, field, message)).run();
    }

    /**
     * M5 part C's level keys (Level 09): hold zones wait for the objectives' groups, each group in
     * one hold at most, slower than every section on every difficulty, and not in a level with the
     * set pieces, cranes, sleds, tows or boss that the level clock's slowing was not built for; the
     * collapse waits for groups, falls with a tower placed once and leaves a backdrop piece; a scoped escapes objective has waves of
     * its enemy with its tag; a pack has a path for each unit on every difficulty; the radio's new
     * events, {@code music.full_on} and the threat profile's required traits have what they name.
     */
    private void checkPartC(LevelData level, Set<String> levelEnemies) {
        List<LevelData.Hold> holds = level.holds().orElse(List.of());
        Set<String> held = new TreeSet<>();
        double slowest = level.sections().stream()
                .mapToDouble(section -> section.speed().orElse(level.scrollSpeed()))
                .min()
                .orElseThrow();
        for (int i = 0; i < holds.size(); i++) {
            LevelData.Hold hold = holds.get(i);
            String field = "holds[" + i + "]";
            for (String group : hold.groups()) {
                checkGroup(level, field + ".groups", group);
                if (!held.add(group)) {
                    problem(level, field + ".groups", "group '" + group + "' is in another hold already");
                }
            }
            for (Difficulty difficulty : Difficulty.values()) {
                if (hold.speedOn(difficulty) >= slowest) {
                    problem(
                            level,
                            field + ".speed",
                            hold.speedOn(difficulty) + " px/s on "
                                    + difficulty.name().toLowerCase(Locale.ROOT)
                                    + " is not below every section's speed (" + slowest + ")");
                }
            }
        }
        if (!holds.isEmpty()) {
            List<String> unsupported = new ArrayList<>();
            level.setPieces().filter(pieces -> !pieces.isEmpty()).ifPresent(pieces -> unsupported.add("set_pieces"));
            level.cranes().filter(cranes -> !cranes.isEmpty()).ifPresent(cranes -> unsupported.add("cranes"));
            level.sleds().ifPresent(sleds -> unsupported.add("sleds"));
            level.tows().filter(tows -> !tows.isEmpty()).ifPresent(tows -> unsupported.add("tows"));
            level.boss().ifPresent(boss -> unsupported.add("boss"));
            if (!unsupported.isEmpty()) {
                problem(level, "holds", "hold zones are not built for a level with " + unsupported);
            }
        }
        level.collapse().ifPresent(collapse -> {
            collapse.groups().forEach(group -> checkGroup(level, "collapse.groups", group));
            if (!level.backdrop().pieces().containsKey(collapse.rubble())) {
                problem(level, "collapse.rubble", "no backdrop piece '" + collapse.rubble() + "'");
            }
            BackdropData.Piece tower = level.backdrop().pieces().get(collapse.tower());
            long placed = level.backdrop().placements().stream()
                    .filter(p -> p.piece().equals(collapse.tower()))
                    .count();
            if (tower == null || tower.tower().isEmpty()) {
                problem(level, "collapse.tower", "no backdrop tower piece '" + collapse.tower() + "'");
            } else if (placed != 1) {
                problem(level, "collapse.tower", "'" + collapse.tower() + "' is placed " + placed + " times, not once");
            }
        });
        level.objectives().secondary().ifPresent(secondary -> secondary.tag().ifPresent(tag -> {
            String slug = secondary.escapes().orElseThrow();
            boolean tagged = level.waves().stream()
                    .anyMatch(wave -> wave.tag().filter(tag::equals).isPresent()
                            && wave.groupList().stream()
                                    .anyMatch(group -> group.enemy().equals(slug)));
            if (!tagged) {
                problem(level, "objectives.secondary.tag", "no wave of '" + slug + "' tagged '" + tag + "'");
            }
        }));
        for (int i = 0; i < level.waves().size(); i++) {
            LevelData.Wave wave = level.waves().get(i);
            for (LevelData.Group group : wave.groupList()) {
                if (!group.formation().equals("pack")) {
                    continue;
                }
                int paths = wave.paths().map(List::size).orElse(0);
                int most = 0;
                for (Difficulty difficulty : Difficulty.values()) {
                    most = Math.max(most, wave.fliesOn(difficulty) ? countOn(wave, group, difficulty) : 0);
                }
                if (paths < most) {
                    problem(
                            level,
                            "waves[" + i + "].paths",
                            "a pack needs a path per unit, as many as its largest difficulty's count: " + paths
                                    + " for " + most);
                }
            }
        }
        for (int i = 0; i < level.radio().size(); i++) {
            LevelData.CueEvent event = level.radio().get(i).event().orElse(null);
            String field = "radio[" + i + "].event";
            if (event == LevelData.CueEvent.HOLD_START && holds.isEmpty()) {
                problem(level, field, "hold-start in a level without holds");
            } else if (event == LevelData.CueEvent.COLLAPSE && level.collapse().isEmpty()) {
                problem(level, field, "collapse in a level without a collapse");
            } else if (event == LevelData.CueEvent.FIRST_POUNCE
                    && levelEnemies.stream().noneMatch(this::pounces)) {
                problem(level, field, "first-pounce in a level without an enemy that pounces");
            }
        }
        level.music().fullOn().ifPresent(events -> {
            if (events.contains(LevelData.Music.FullOn.HOLD) && holds.isEmpty()) {
                problem(level, "music.full_on", "hold in a level without holds");
            }
            if (events.contains(LevelData.Music.FullOn.COLLAPSE)
                    && level.collapse().isEmpty()) {
                problem(level, "music.full_on", "collapse in a level without a collapse");
            }
        });
    }

    /**
     * M5 part D's units (design/enemies/air/wraith, mote-swarm): a cloak is flown on an ambush's path
     * and decloaks to another layer; an ambush unit hovers a while (its hold) and leaves at its
     * straight speed; the hard hook's hover time needs a hover; an ambush unit flies {@code rear
     * ambush}, a flock {@code swarm}, among its formations.
     */
    private void checkPartD(EnemyData enemy) {
        EnemyData.Movement movement = enemy.movement();
        enemy.cloak().ifPresent(cloak -> {
            if (movement.ambush().isEmpty()) {
                problem(enemy, "cloak", "a cloak is flown on an ambush's path (movement.ambush)");
            }
            if (cloak.layer().equals(enemy.layer())) {
                problem(enemy, "cloak.layer", "it decloaks to another layer than its cloaked " + enemy.layer());
            }
        });
        movement.ambush().ifPresent(ambush -> {
            if (movement.hover().flatMap(EnemyData.Hover::seconds).isEmpty()) {
                problem(enemy, "movement.ambush", "an ambush holds its hover's seconds at its hover's y");
            }
            if (movement.straight().isEmpty()) {
                problem(enemy, "movement.ambush", "an ambush leaves at its straight speed");
            }
            if (ambush.lane() > PlayField.WIDTH / 2.0) {
                problem(enemy, "movement.ambush.lane", "its exit lane lies inside the play field's nearer half");
            }
            checkFlies(enemy, "rear ambush");
        });
        movement.flock().ifPresent(flock -> checkFlies(enemy, "swarm"));
        for (var hook : List.of(
                enemy.difficulty().flatMap(EnemyData.Hooks::easy),
                enemy.difficulty().flatMap(EnemyData.Hooks::hard))) {
            if (hook.flatMap(EnemyData.Hook::hoverSeconds).isPresent()
                    && movement.hover().isEmpty()) {
                problem(enemy, "difficulty.hover_seconds", "a hover time without a hover");
            }
        }
    }

    /** {@code enemy}'s formations list {@code formation}. */
    private void checkFlies(EnemyData enemy, String formation) {
        if (enemy.formations().stream().noneMatch(use -> use.name().equals(formation))) {
            problem(enemy, "formations", "its movement flies '" + formation + "', which its formations do not list");
        }
    }

    /**
     * M5 part D's wave keys (Level 10): a swarm's enemy has a flock, its wave one route ({@code
     * paths}) and at most the flock's members on every difficulty; a rear ambush's enemy has an
     * ambush, its wave enters {@code from: rear} with 1–4 units on every difficulty and no paths; a
     * flock or an ambush unit flies only its own formation; only a swarm loops back more than once
     * ({@code loop_back.count}, a change's {@code loops}); a snake's {@code paths} is one route, as a
     * stream's (its left-edge units' route, mirrored for the right-edge ones).
     */
    private void checkPartD(LevelData level) {
        for (int i = 0; i < level.waves().size(); i++) {
            LevelData.Wave wave = level.waves().get(i);
            String field = "waves[" + i + "]";
            boolean swarm = false;
            for (LevelData.Group group : wave.groupList()) {
                if (!content.enemies().containsKey(group.enemy())) {
                    continue;
                }
                EnemyData enemy = content.enemy(group.enemy());
                Optional<EnemyData.Flock> flock = enemy.movement().flock();
                boolean ambush = enemy.movement().ambush().isPresent();
                swarm |= group.formation().equals("swarm");
                if (group.formation().equals("swarm") != flock.isPresent()) {
                    problem(
                            level,
                            field + ".formation",
                            flock.isPresent()
                                    ? "'" + group.enemy() + "' flies only a swarm"
                                    : "a swarm's enemy has a flock (movement.flock), '" + group.enemy() + "' has none");
                }
                if (group.formation().equals("rear ambush") != ambush) {
                    problem(
                            level,
                            field + ".formation",
                            ambush
                                    ? "'" + group.enemy() + "' flies only a rear ambush"
                                    : "a rear ambush's enemy has an ambush (movement.ambush), '" + group.enemy()
                                            + "' has none");
                }
                for (Difficulty difficulty : Difficulty.values()) {
                    if (!wave.fliesOn(difficulty)) {
                        continue;
                    }
                    int count = countOn(wave, group, difficulty);
                    String on = " on " + difficulty.name().toLowerCase(Locale.ROOT);
                    if (flock.isPresent() && count > flock.get().max()) {
                        problem(
                                level,
                                field + ".count",
                                count + " members" + on + ", the flock holds "
                                        + flock.get().max());
                    }
                    if (group.formation().equals("rear ambush") && (count < 1 || count > 4)) {
                        problem(level, field + ".count", "a rear ambush has 1 to 4 units, not " + count + on);
                    }
                }
                if (group.formation().equals("swarm")
                        && wave.paths().map(List::size).orElse(0) != 1) {
                    problem(level, field + ".paths", "a swarm flies one route: one path");
                }
                if (group.formation().equals("rear ambush")) {
                    if (wave.from() != LevelData.Entry.REAR
                            || wave.easy()
                                    .flatMap(LevelData.Change::from)
                                    .filter(f -> f != LevelData.Entry.REAR)
                                    .isPresent()
                            || wave.hard()
                                    .flatMap(LevelData.Change::from)
                                    .filter(f -> f != LevelData.Entry.REAR)
                                    .isPresent()) {
                        problem(level, field + ".from", "a rear ambush is from: rear");
                    }
                    if (wave.paths().isPresent()) {
                        problem(level, field + ".paths", "a rear ambush flies its lanes, no paths");
                    }
                }
                if (group.formation().equals("snake")
                        && wave.paths().map(List::size).orElse(1) != 1) {
                    problem(level, field + ".paths", "a snake flies one route: one path");
                }
                if (group.formation().equals("stream")
                        && wave.paths().map(List::size).orElse(1) != 1) {
                    // M5 part D: a unit from the right flies the route mirrored.
                    problem(level, field + ".paths", "a stream flies one route (from the left): one path");
                }
            }
            boolean loops = wave.loopBack()
                            .flatMap(LevelData.LoopBack::count)
                            .filter(n -> n > 1)
                            .isPresent()
                    || wave.easy().flatMap(LevelData.Change::loops).isPresent()
                    || wave.hard().flatMap(LevelData.Change::loops).isPresent();
            if (loops && !swarm) {
                problem(level, field, "only a swarm loops back more than once (loop_back.count, loops)");
            }
            if ((wave.easy().flatMap(LevelData.Change::loops).isPresent()
                            || wave.hard().flatMap(LevelData.Change::loops).isPresent())
                    && wave.loopBack().isEmpty()) {
                problem(level, field, "loops change a loop_back the wave does not have");
            }
        }
        checkPartDEvents(level);
    }

    /**
     * M5 part D's radio events and music keys have what they name: {@code scripted-loss} and the
     * music's duck a scripted loss, {@code first-decloak} a cloaked enemy, {@code first-loop-back} a
     * swarm that loops back; the ambience changes lie in the level's later sections.
     */
    private void checkPartDEvents(LevelData level) {
        boolean scripted = level.objectives()
                .escort()
                .flatMap(LevelData.Escort::scriptedLoss)
                .isPresent();
        boolean cloaked = level.waves().stream()
                .flatMap(wave -> wave.groupList().stream())
                .anyMatch(group -> content.enemies().containsKey(group.enemy())
                        && content.enemy(group.enemy()).cloak().isPresent());
        boolean loops = level.waves().stream()
                .anyMatch(wave -> wave.loopBack().isPresent()
                        && wave.groupList().stream()
                                .anyMatch(group -> group.formation().equals("swarm")));
        for (int i = 0; i < level.radio().size(); i++) {
            LevelData.CueEvent event = level.radio().get(i).event().orElse(null);
            String field = "radio[" + i + "].event";
            if (event == LevelData.CueEvent.SCRIPTED_LOSS && !scripted) {
                problem(
                        level,
                        field,
                        "scripted-loss in a level without a scripted loss (objectives.escort.scripted_loss)");
            } else if (event == LevelData.CueEvent.FIRST_DECLOAK && !cloaked) {
                problem(level, field, "first-decloak in a level without a cloaked enemy");
            } else if (event == LevelData.CueEvent.FIRST_LOOP_BACK && !loops) {
                problem(level, field, "first-loop-back in a level without a swarm that loops back");
            }
        }
        LevelData.Music music = level.music();
        if (music.duck().isPresent() && !scripted) {
            problem(level, "music.duck.on", "scripted-loss in a level without a scripted loss");
        }
        for (int i = 0; i < music.ambienceChangeList().size(); i++) {
            int section = music.ambienceChangeList().get(i).section();
            if (section > level.sections().size()) {
                problem(
                        level,
                        "music.ambience_changes[" + i + "].section",
                        "the level has " + level.sections().size() + " sections, not " + section);
            }
        }
    }

    /** The units of {@code group} of {@code wave} on {@code difficulty}, as {@link SimSpecs} plans them. */
    private int countOn(LevelData.Wave wave, LevelData.Group group, Difficulty difficulty) {
        Optional<LevelData.Change> change =
                switch (difficulty) {
                    case EASY -> wave.easy();
                    case MEDIUM -> Optional.empty();
                    case HARD -> wave.hard();
                };
        return change.flatMap(LevelData.Change::count)
                .orElseGet(() -> content.difficulty().formationSize(group.count(), difficulty));
    }

    /** Whether the enemy {@code slug} has a pounce. */
    private boolean pounces(String slug) {
        return content.enemies().containsKey(slug)
                && content.enemy(slug).attacks().stream()
                        .anyMatch(attack -> attack.pounce().isPresent());
    }

    /**
     * A set piece is a multi-part enemy; its passes fit their sections and the level, a descent
     * lies on its path, and no wave starts while it is on the player's layer on any difficulty
     * (design/campaign, Level 03: the fight has the screen to itself).
     */
    private void checkSetPieces(LevelData level, Set<String> levelEnemies) {
        List<LevelData.SetPieceData> pieces = level.setPieces().orElse(List.of());
        for (int i = 0; i < pieces.size(); i++) {
            LevelData.SetPieceData piece = pieces.get(i);
            String field = "set_pieces[" + i + "]";
            checkEnemyName(level, field + ".enemy", piece.enemy());
            levelEnemies.add(piece.enemy());
            if (content.enemies().containsKey(piece.enemy())
                    && content.enemy(piece.enemy()).partList().isEmpty()) {
                problem(level, field + ".enemy", "'" + piece.enemy() + "' is no multi-part unit (no part_list)");
            }
            for (int p = 0; p < piece.passes().size(); p++) {
                LevelData.PassData pass = piece.passes().get(p);
                String passField = field + ".passes[" + p + "]";
                if (pass.section() < 1 || pass.section() > level.sections().size()) {
                    problem(level, passField + ".section", "no section " + pass.section());
                }
                if (p > 0
                        && pass.path().getFirst().t()
                                < piece.passes().get(p - 1).path().getLast().t()) {
                    problem(level, passField + ".path", "starts before the pass before it ends");
                }
                pass.path().forEach(point -> checkTime(level, passField + ".path", point.t()));
                pass.descend().ifPresent(descent -> {
                    if (descent.at() < pass.path().getFirst().t()
                            || descent.at() > pass.path().getLast().t()) {
                        problem(level, passField + ".descend.at", "t=" + descent.at() + " is not on the pass's path");
                    }
                    double from = descent.at() + descent.seconds();
                    double to = descent.at();
                    for (Difficulty difficulty : Difficulty.values()) {
                        to = Math.max(to, descent.at() + pass.holdOn(difficulty).orElseThrow());
                    }
                    for (int w = 0; w < level.waves().size(); w++) {
                        double t = level.waves().get(w).t();
                        if (t >= from && t < to) {
                            problem(
                                    level,
                                    "waves[" + w + "].t",
                                    "t=" + t + " while " + piece.enemy() + " is on the player's layer");
                        }
                    }
                });
            }
        }
    }

    /**
     * The debris chunks resolve, enter inside the play field and within the level, and no more
     * large ones than allowed are on the screen at once, on any difficulty.
     */
    /**
     * Part G's level rules (Level 07): a tow reveals a secret of the level and enters within it, and
     * a parts objective names parts and a phase of the level's boss.
     */
    private void checkLevelRules(LevelData level) {
        Set<String> secrets =
                level.secrets().stream().map(LevelData.Secret::name).collect(Collectors.toSet());
        List<LevelData.Tow> tows = level.tows().orElse(List.of());
        for (int i = 0; i < tows.size(); i++) {
            LevelData.Tow tow = tows.get(i);
            checkTime(level, "tows[" + i + "].t", tow.t());
            if (!secrets.contains(tow.reveals())) {
                problem(level, "tows[" + i + "].reveals", "no secret '" + tow.reveals() + "'");
            }
            if (tow.x() < 0 || tow.x() > PlayField.WIDTH) {
                problem(level, "tows[" + i + "].x", "x=" + tow.x() + " is outside the play field");
            }
        }
        level.objectives().secondary().ifPresent(secondary -> {
            secondary.parts().ifPresent(parts -> checkBossParts(level, "objectives.secondary.parts", parts));
            secondary.before().ifPresent(before -> level.boss()
                    .filter(boss -> content.enemies().containsKey(boss.enemy()))
                    .flatMap(boss -> content.enemy(boss.enemy()).boss())
                    .filter(script -> script.phases().stream()
                            .noneMatch(phase -> phase.name().equals(before)))
                    .ifPresent(
                            script -> problem(level, "objectives.secondary.before", "no boss phase '" + before + "'")));
        });
    }

    /** Parts named by a level (a part drop, a parts objective) are parts of its boss. */
    private void checkBossParts(LevelData level, String field, List<String> parts) {
        if (level.boss().isEmpty()) {
            problem(level, field, "boss parts need the level's boss");
            return;
        }
        String slug = level.boss().get().enemy();
        if (!content.enemies().containsKey(slug)) {
            return;
        }
        for (String part : parts) {
            if (content.enemy(slug).partIndex(part) < 0) {
                problem(level, field, "'" + slug + "' has no part '" + part + "'");
            }
        }
    }

    private void checkDebris(LevelData level) {
        if (level.debris().isEmpty()) {
            return;
        }
        LevelData.DebrisField field = level.debris().get();
        List<LevelData.PlacedChunk> placed = field.placed();
        for (int i = 0; i < placed.size(); i++) {
            LevelData.PlacedChunk chunk = placed.get(i);
            String at = "debris.placed[" + i + "]";
            checkTime(level, at + ".t", chunk.t());
            if (!field.chunks().containsKey(chunk.chunk())) {
                problem(
                        level,
                        at + ".chunk",
                        "no chunk '" + chunk.chunk() + "' in debris.chunks "
                                + field.chunks().keySet());
            } else if (chunk.x() < 0 || chunk.x() > PlayField.WIDTH) {
                problem(level, at + ".x", "x=" + chunk.x() + " is outside the play field");
            }
        }
        if (field.maxLarge().isEmpty()
                || !placed.stream().allMatch(c -> field.chunks().containsKey(c.chunk()))) {
            return;
        }
        int max = field.maxLarge().get();
        for (Difficulty difficulty : Difficulty.values()) {
            double factor = field.driftFactor(difficulty);
            List<LevelData.PlacedChunk> chunks = field.placedOn(difficulty);
            for (double t = 0; t < level.seconds(); t += 0.05) {
                int large = 0;
                for (LevelData.PlacedChunk chunk : chunks) {
                    LevelData.Chunk kind = field.chunks().get(chunk.chunk());
                    if (kind.large() && onScreen(chunk, kind.size(), factor, t)) {
                        large++;
                    }
                }
                if (large > max) {
                    problem(
                            level,
                            "debris.placed",
                            large + " large chunks on the screen at t=" + Math.round(t * 100) / 100.0 + " on "
                                    + difficulty.name().toLowerCase(Locale.ROOT) + " (at most " + max + ")");
                    break;
                }
            }
        }
    }

    /** Whether a chunk drifting from the top edge is on the screen at {@code t}. */
    private static boolean onScreen(LevelData.PlacedChunk chunk, Size size, double factor, double t) {
        if (t < chunk.t()) {
            return false;
        }
        double age = t - chunk.t();
        double x = chunk.x() + chunk.drift().x() * factor * age;
        double y = PlayField.HEIGHT + size.height() / 2 + chunk.drift().y() * factor * age;
        return y + size.height() / 2 > 0
                && y - size.height() / 2 < PlayField.HEIGHT
                && x + size.width() / 2 > 0
                && x - size.width() / 2 < PlayField.WIDTH;
    }

    /** A group name must be one of the objectives' groups (the primary's targets or the secondary's groups). */
    private void checkGroup(LevelData level, String field, String group) {
        List<String> groups = level.objectives().groups();
        if (!groups.contains(group)) {
            problem(level, field, "no group '" + group + "' in the objectives' groups " + groups);
        }
    }

    private void checkCarriers(
            LevelData level, String field, List<LevelData.PlacedPickup> pickups, Set<Double> waveTimes) {
        for (int i = 0; i < pickups.size(); i++) {
            LevelData.Carrier carrier = pickups.get(i).droppedBy();
            if (carrier.parts().isPresent()) {
                checkBossParts(
                        level,
                        field + "[" + i + "].dropped_by.parts",
                        carrier.parts().get());
                continue;
            }
            if (carrier.group().isPresent()) {
                checkGroup(
                        level,
                        field + "[" + i + "].dropped_by.group",
                        carrier.group().get());
                continue;
            }
            double wave = carrier.waveT();
            if (!waveTimes.contains(wave)) {
                problem(level, field + "[" + i + "].dropped_by.wave", "no wave at t=" + wave);
            }
        }
    }

    /**
     * An escort objective's convoy (design/allies; Level 04): its ally exists and follows the
     * level's road, its column fits the screen without units overlapping, its hooked enemies fly in
     * the level, and the ally events and level-end ranges are used only with a convoy. The road keeps
     * its ribbon inside the play field and bends no further than the ally's rendered headings.
     */
    private void checkEscort(LevelData level, Set<String> levelEnemies) {
        Optional<LevelData.Escort> escort = level.objectives().escort();
        for (int i = 0; i < level.radio().size(); i++) {
            LevelData.RadioCue cue = level.radio().get(i);
            String field = "radio[" + i + "]";
            boolean allyEvent = cue.event()
                    .map(event -> event == LevelData.CueEvent.FIRST_ALLY_HIT
                            || event == LevelData.CueEvent.FIRST_ALLY_LOST
                            || event == LevelData.CueEvent.ALLY_LOST)
                    .orElse(false);
            if ((allyEvent || cue.allies().isPresent()) && escort.isEmpty()) {
                problem(level, field, "convoy events and allies ranges need an escort objective");
            }
            boolean canFail = escort.isPresent() || level.objectives().targets().isPresent();
            if (cue.event().orElse(null) == LevelData.CueEvent.MISSION_FAILED && !canFail) {
                problem(
                        level,
                        field,
                        "a mission-failed line needs a primary objective that can fail (escort, destroy-targets)");
            }
            // M5 part D: a level-end cue counts the saveable units home (all but a scripted loss's).
            int units = escort.map(LevelData.Escort::saveable).orElse(0);
            String kind = escort.flatMap(LevelData.Escort::scriptedLoss).isPresent() ? " saveable units" : " units";
            cue.allies().ifPresent(range -> {
                if (range.max() > units) {
                    problem(level, field + ".allies", "the convoy has " + units + kind);
                }
            });
        }
        if (level.radio().stream()
                        .filter(cue -> cue.event().orElse(null) == LevelData.CueEvent.MISSION_FAILED)
                        .count()
                > 1) {
            problem(level, "radio", "at most one mission-failed line");
        }
        level.road().ifPresent(road -> checkRoad(level, road, escort));
        if (escort.isEmpty()) {
            return;
        }
        LevelData.Escort convoy = escort.get();
        AlliesData.Ally ally = content.allies().allies().get(convoy.ally());
        if (ally == null) {
            problem(level, "objectives.escort.ally", "unknown ally '" + convoy.ally() + "'");
            return;
        }
        if (ally.air() != convoy.air()) {
            problem(
                    level,
                    "objectives.escort",
                    ally.air()
                            ? "a " + convoy.ally() + " follows lanes: give its stations, not y"
                            : "a " + convoy.ally() + " follows the road: give its y, not stations");
            return;
        }
        if (convoy.air()) {
            checkAirEscort(level, convoy, ally);
            return;
        }
        if (level.road().isEmpty()) {
            problem(level, "objectives.escort", "a " + convoy.ally() + " follows the level's road: give road");
        }
        double length = ally.size().height();
        List<Double> heights = convoy.y().orElseThrow();
        for (int k = 0; k < heights.size(); k++) {
            double y = heights.get(k);
            if (y - length / 2 < 0 || y + length / 2 > PlayField.HEIGHT) {
                problem(level, "objectives.escort.y[" + k + "]", "y=" + y + " puts the unit off the screen");
            }
            if (k > 0 && y - heights.get(k - 1) < length) {
                problem(level, "objectives.escort.y[" + k + "]", "units overlap: less than " + length + " px apart");
            }
        }
        checkTime(
                level, "objectives.escort.enter.t", convoy.enter().orElseThrow().t());
        for (String slug : convoy.hook().orElseThrow().enemies()) {
            checkEnemyName(level, "objectives.escort.hook.enemies", slug);
            if (content.enemies().containsKey(slug) && !levelEnemies.contains(slug)) {
                problem(level, "objectives.escort.hook.enemies", "no '" + slug + "' in this level");
            }
        }
    }

    /**
     * M5 part D (design/allies, evacuation shuttle; Level 10): an air escort. Sampled every step of the
     * level, each unit's hit box stays inside the play field at its station with the sway and never
     * overlaps another's; the pads lie on the screen; the liftoff ends before the scripted loss's glow
     * starts and the climb-out starts after the loss, both inside the level; the level has no road.
     */
    private void checkAirEscort(LevelData level, LevelData.Escort convoy, AlliesData.Ally ally) {
        String field = "objectives.escort";
        if (level.road().isPresent()) {
            problem(level, "road", "an air escort follows no road");
        }
        List<LevelScript.Station> stations = convoy.stations().orElseThrow().stream()
                .map(PartDRules::station)
                .toList();
        double w = ally.hitbox().width();
        double h = ally.hitbox().height();
        int steps = SimStep.ticks(level.seconds());
        boolean[] reported = new boolean[stations.size()];
        boolean overlapReported = false;
        for (int tick = 0; tick <= steps; tick++) {
            double t = tick * SimStep.SECONDS;
            for (int k = 0; k < stations.size(); k++) {
                double x = stations.get(k).xAt(t);
                double y = stations.get(k).yAt(t);
                if (!reported[k]
                        && (x - w / 2 < 0
                                || x + w / 2 > PlayField.WIDTH
                                || y - h / 2 < 0
                                || y + h / 2 > PlayField.HEIGHT)) {
                    reported[k] = true;
                    problem(
                            level,
                            field + ".stations[" + k + "]",
                            "its hit box leaves the play field at t=" + round(t));
                }
                for (int j = 0; j < k && !overlapReported; j++) {
                    if (Math.abs(stations.get(j).xAt(t) - x) < w
                            && Math.abs(stations.get(j).yAt(t) - y) < h) {
                        overlapReported = true;
                        problem(
                                level,
                                field + ".stations[" + k + "]",
                                "its hit box overlaps station " + j + "'s at t=" + round(t));
                    }
                }
            }
        }
        double liftEnd = 0;
        if (convoy.liftoff().isPresent()) {
            LevelData.Liftoff liftoff = convoy.liftoff().get();
            liftEnd = liftoff.t() + liftoff.seconds();
            checkTime(level, field + ".liftoff.t", liftEnd);
            for (int k = 0; k < liftoff.pads().size(); k++) {
                Point pad = liftoff.pads().get(k);
                if (pad.x() < 0 || pad.x() > PlayField.WIDTH || pad.y() < 0 || pad.y() > PlayField.HEIGHT) {
                    problem(level, field + ".liftoff.pads[" + k + "]", "the pad is off the screen");
                }
            }
        }
        double lossAt = liftEnd;
        if (convoy.scriptedLoss().isPresent()) {
            LevelData.ScriptedLoss loss = convoy.scriptedLoss().get();
            checkTime(level, field + ".scripted_loss.t", loss.t());
            if (loss.t() - loss.glow() < liftEnd) {
                problem(
                        level,
                        field + ".scripted_loss",
                        "its glow starts before the liftoff ends at " + liftEnd + " s");
            }
            lossAt = loss.t();
        }
        if (convoy.climb().isPresent()) {
            LevelData.Climb climb = convoy.climb().get();
            checkTime(level, field + ".climb.t", climb.t() + climb.seconds());
            if (climb.t() <= lossAt) {
                problem(level, field + ".climb.t", "the climb-out starts after the liftoff and the scripted loss");
            }
        }
    }

    private static double round(double seconds) {
        return Math.round(seconds * 100) / 100.0;
    }

    /** The road's ribbon stays inside the play field and bends no further than the convoy's headings. */
    private void checkRoad(LevelData level, LevelData.Road data, Optional<LevelData.Escort> escort) {
        Road road = SimSpecs.road(level, data);
        double most = escort.map(e -> content.allies().allies().get(e.ally()))
                .map(ally -> (ally.headings().count() - 1) / 2 * ally.headings().step())
                .orElse(90.0);
        double half = data.width() / 2;
        for (double along = road.start(); along <= road.end(); along += 1) {
            double x = road.x(along);
            if (x - half < 0 || x + half > PlayField.WIDTH) {
                problem(level, "road.points", "the ribbon leaves the play field at x=" + Math.round(x));
                return;
            }
            if (Math.abs(road.headingDegrees(along)) > most + 1e-6) {
                problem(
                        level,
                        "road.points",
                        "bends " + Math.round(Math.abs(road.headingDegrees(along))) + "° from straight up, more than "
                                + most + "°");
                return;
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
