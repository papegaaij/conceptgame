package vanguard.content;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import vanguard.sim.PlayField;
import vanguard.sim.Road;

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
            EnemyData.Attack attack = enemy.attacks().get(i);
            String field = "attacks[" + i + "]";
            attack.bullet().ifPresent(bullet -> checkBullet(enemy, field + ".bullet", bullet));
            attack.mine().ifPresent(mine -> checkBullet(enemy, field + ".mine.ring_bullet", mine.ringBullet()));
        }
        for (var hook : List.of(
                enemy.difficulty().flatMap(EnemyData.Hooks::easy),
                enemy.difficulty().flatMap(EnemyData.Hooks::hard))) {
            hook.flatMap(EnemyData.Hook::deathBurst)
                    .ifPresent(puff -> checkBullet(enemy, "difficulty.death_burst.bullet", puff.bullet()));
        }
        checkParts(enemy);
        enemy.difficulty()
                .flatMap(EnemyData.Hooks::hard)
                .flatMap(EnemyData.Hook::leadsTargetIn)
                .ifPresent(
                        names -> names.forEach(name -> checkFormation(enemy, "difficulty.hard.leads_target_in", name)));
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
        double hp = parts.stream().mapToDouble(EnemyData.PartData::hp).sum();
        if (hp != enemy.hp()) {
            problem(enemy, "hp", "is " + enemy.hp() + ", the parts' HP add up to " + hp);
        }
        int bounty = parts.stream().mapToInt(EnemyData.PartData::bounty).sum();
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
        checkDebris(level);
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
        for (var variant : List.of(level.difficulty().easy(), level.difficulty().hard())) {
            variant.flatMap(LevelData.Variant::enemies)
                    .ifPresent(changes -> changes.keySet().forEach(slug -> checkEnemyName(level, "difficulty", slug)));
        }
        new BackdropCheck(level, (field, message) -> problem(level, field, message)).run();
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

    /** A group name must be one of the secondary objective's groups. */
    private void checkGroup(LevelData level, String field, String group) {
        List<String> groups = level.objectives()
                .secondary()
                .flatMap(LevelData.Secondary::groups)
                .orElse(List.of());
        if (!groups.contains(group)) {
            problem(level, field, "no group '" + group + "' in objectives.secondary.groups " + groups);
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
                            || event == LevelData.CueEvent.MISSION_FAILED)
                    .orElse(false);
            if ((allyEvent || cue.allies().isPresent()) && escort.isEmpty()) {
                problem(level, field, "convoy events and allies ranges need an escort objective");
            }
            int units = escort.map(e -> e.y().size()).orElse(0);
            cue.allies().ifPresent(range -> {
                if (range.max() > units) {
                    problem(level, field + ".allies", "the convoy has " + units + " units");
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
        if (ally.follows().equals("road") && level.road().isEmpty()) {
            problem(level, "objectives.escort", "a " + convoy.ally() + " follows the level's road: give road");
        }
        double length = ally.size().height();
        for (int k = 0; k < convoy.y().size(); k++) {
            double y = convoy.y().get(k);
            if (y - length / 2 < 0 || y + length / 2 > PlayField.HEIGHT) {
                problem(level, "objectives.escort.y[" + k + "]", "y=" + y + " puts the unit off the screen");
            }
            if (k > 0 && y - convoy.y().get(k - 1) < length) {
                problem(level, "objectives.escort.y[" + k + "]", "units overlap: less than " + length + " px apart");
            }
        }
        checkTime(level, "objectives.escort.enter.t", convoy.enter().t());
        for (String slug : convoy.hook().enemies()) {
            checkEnemyName(level, "objectives.escort.hook.enemies", slug);
            if (content.enemies().containsKey(slug) && !levelEnemies.contains(slug)) {
                problem(level, "objectives.escort.hook.enemies", "no '" + slug + "' in this level");
            }
        }
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
