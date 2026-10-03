package vanguard.content;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import vanguard.sim.Armament;
import vanguard.sim.EnemyGun;
import vanguard.sim.EnemySpec;
import vanguard.sim.Hitbox;
import vanguard.sim.Hull;
import vanguard.sim.Layer;
import vanguard.sim.LevelScript;
import vanguard.sim.Loadout;
import vanguard.sim.PickupRules;
import vanguard.sim.PickupType;
import vanguard.sim.Plating;
import vanguard.sim.PlayField;
import vanguard.sim.Range;
import vanguard.sim.Rules;
import vanguard.sim.ScoringRules;
import vanguard.sim.ShieldModel;
import vanguard.sim.ShipSpec;
import vanguard.sim.WaveSpec;
import vanguard.sim.WeaponSpec;

/**
 * Builds the simulation's specs from the loaded content at one difficulty. The dependency points
 * this way ({@code content → sim}) so that the simulation knows nothing about data files, YAML or
 * Jackson: it defines the records it needs, and this class fills them, applying the difficulty
 * levers (design/systems/difficulty) and the level's easy/hard changes on the way.
 */
public final class SimSpecs {
    /** The starter front gun's slug. */
    public static final String PULSE_CANNON = "pulse-cannon";

    private static final Pattern LEVEL_KEY = Pattern.compile("act-(\\d+)-[a-z0-9-]+/level-(\\d{2})-[a-z0-9-]+");

    private SimSpecs() {}

    /** A weapon fitted in one of the Stormhawk's weapon slots at an upgrade level (1–5). */
    public record FittedWeapon(Armament.Slot slot, String weapon, int level) {}

    /**
     * The starting fit: the hull with the starter engine, the Pulse Cannon at L1, the starter
     * shield (with the difficulty's regen lever and the spare-power bonus of the starter generator)
     * and plating.
     */
    public static Loadout starterLoadout(Content content, Difficulty difficulty) {
        SystemsData.Engine engine = content.systems().engines().getFirst();
        ShieldData.Model shield = content.shields().models().getFirst();
        WeaponData pulse = content.weapon(PULSE_CANNON);
        double spare =
                content.generators().models().getFirst().output() - pulse.draw().min() - shield.draw() - engine.draw();
        return loadout(
                content,
                engine.name(),
                List.of(new FittedWeapon(Armament.Slot.FRONT, PULSE_CANNON, 1)),
                shield.name(),
                content.armour().plating().getFirst().name(),
                spare,
                difficulty);
    }

    /**
     * A fit of the parts the simulation flies: the hull with an engine, the weapons at their
     * levels, a shield (with the difficulty's regen lever and the bonus of {@code sparePower} MW,
     * design/player/generator) and a plating, each by its name in its data file.
     */
    public static Loadout loadout(
            Content content,
            String engine,
            List<FittedWeapon> weapons,
            String shield,
            String plating,
            double sparePower,
            Difficulty difficulty) {
        ShieldModel model =
                shield(content, named(content.shields().models(), ShieldData.Model::name, shield), difficulty);
        double regen = model.regenPerSecond() * (1 + regenBonus(content, sparePower));
        return new Loadout(
                ship(content, named(content.systems().engines(), SystemsData.Engine::name, engine)),
                new Armament(weapons.stream()
                        .map(fitted -> new Armament.Mount(
                                fitted.slot(),
                                weapon(content, fitted.slot(), fitted.weapon(), fitted.level()),
                                weapon(content, fitted.slot(), fitted.weapon(), fitted.level() + 1)))
                        .toList()),
                new ShieldModel(model.capacity(), regen, model.regenDelaySeconds(), model.breakSeconds()),
                plating(named(content.armour().plating(), ArmourData.Plating::name, plating)));
    }

    /** The shield regen bonus of {@code sparePower} spare MW (design/player/generator): +10 % per MW, at most +50 %. */
    public static double regenBonus(Content content, double sparePower) {
        GeneratorData.SparePower rule = content.generators().sparePower();
        return Math.clamp(sparePower * rule.regenBonusPerMw(), 0, rule.maxRegenBonus());
    }

    private static <T> T named(List<T> parts, Function<T, String> name, String wanted) {
        return parts.stream()
                .filter(part -> name.apply(part).equals(wanted))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("no part '" + wanted + "'"));
    }

    static ShipSpec ship(Content content, SystemsData.Engine engine) {
        ShipData ship = content.ship();
        return new ShipSpec(
                engine.speed(),
                ship.accelerationSeconds(),
                ship.stopSeconds(),
                ship.precisionFactor(),
                ship.size(),
                ship.edgeGap(),
                hull(ship),
                ship.mercySeconds(),
                ship.size() / 2 - ship.mounts().front().y(),
                ship.bankChangeSteps() / ShipSpec.HARD_BANK);
    }

    /**
     * Whether the simulation flies the weapon: the deliveries of the Act 1 arsenal (standard bolts,
     * homing missiles, dropped and lobbed ground-only shots); mines and torpedoes follow with Act 2.
     */
    public static boolean flies(Content content, String weapon) {
        return delivery(content.weapon(weapon)).isPresent();
    }

    private static Optional<WeaponSpec.Delivery> delivery(WeaponData weapon) {
        return switch (weapon.hits()) {
            case "standard" -> Optional.of(WeaponSpec.Delivery.BOLT);
            case "homing" -> Optional.of(WeaponSpec.Delivery.HOMING);
            case "ground-only" ->
                Optional.of(
                        weapon.range().orElseThrow().kind() == WeaponData.Range.Kind.DROP
                                ? WeaponSpec.Delivery.DROPPED
                                : WeaponSpec.Delivery.LOBBED);
            default -> Optional.empty();
        };
    }

    /**
     * A weapon in a slot at an upgrade level, 6 being its overdrive pattern. The muzzles follow from
     * the ship's mount points: the front muzzle, the rear muzzle or, for a weapon that fires to both
     * sides, the wing roots (the pattern to the right, mirrored to the left); a pod fires from its
     * wing mount, the left one mirrored, and turns in by the weapon's convergence.
     */
    static WeaponSpec weapon(Content content, Armament.Slot slot, String slug, int upgradeLevel) {
        WeaponData weapon = content.weapon(slug);
        WeaponSpec.Delivery delivery = delivery(weapon)
                .orElseThrow(
                        () -> new IllegalArgumentException(slug + ": its hits '" + weapon.hits() + "' do not fly yet"));
        WeaponData.Level level = upgradeLevel > weapon.levels().size()
                ? weapon.overdrive()
                : weapon.levels().get(upgradeLevel - 1);
        ShipData.Mounts mounts = content.ship().mounts();
        double centre = content.ship().size() / 2;
        List<WeaponSpec.Muzzle> muzzles = new ArrayList<>();
        for (WeaponData.Shot shot : level.pattern()) {
            switch (slot) {
                case FRONT -> muzzles.add(muzzle(mounts.front(), centre, shot.x(), shot.angle()));
                case REAR -> {
                    if (weapon.mirrored().orElse(false)) {
                        muzzles.add(muzzle(mounts.roots().get(1), centre, shot.x(), shot.angle()));
                        muzzles.add(muzzle(mounts.roots().get(0), centre, -shot.x(), -shot.angle()));
                    } else {
                        muzzles.add(muzzle(mounts.rear(), centre, shot.x(), shot.angle()));
                    }
                }
                case LEFT_WING ->
                    muzzles.add(muzzle(
                            mounts.wings().get(0),
                            centre,
                            -shot.x(),
                            -shot.angle() + weapon.converge().orElse(0.0)));
                case RIGHT_WING ->
                    muzzles.add(muzzle(
                            mounts.wings().get(1),
                            centre,
                            shot.x(),
                            shot.angle() - weapon.converge().orElse(0.0)));
            }
        }
        WeaponData.Range range = weapon.range().orElseThrow();
        return new WeaponSpec(
                slug,
                weapon.vfx(),
                weapon.sfx(),
                delivery,
                weapon.traits().contains("anti-ground"),
                level.rate(),
                level.damage(),
                weapon.speed().map(WeaponData.Speed::start).orElse(0.0),
                hitbox(weapon.size()),
                switch (range.kind()) {
                    case SCREEN -> Double.POSITIVE_INFINITY;
                    case DROP -> 0;
                    case DISTANCE -> range.px();
                },
                weapon.lifetime().orElse(Double.POSITIVE_INFINITY),
                level.pierce().orElse(1),
                level.blast().orElse(0.0),
                Math.toRadians(level.turn().orElse(0.0)),
                Math.toRadians(weapon.cone().orElse(360.0) / 2),
                weapon.fall().or(weapon::flight).orElse(0.0),
                weapon.snap().orElse(0.0),
                muzzles);
    }

    /** A projectile leaving a mount point (sprite pixels from the top left) with an offset and an angle in degrees. */
    private static WeaponSpec.Muzzle muzzle(Point mount, double centre, double offset, double degrees) {
        return new WeaponSpec.Muzzle(mount.x() - centre + offset, centre - mount.y(), Math.toRadians(degrees));
    }

    static ShieldModel shield(Content content, ShieldData.Model model, Difficulty difficulty) {
        return new ShieldModel(
                model.capacity(),
                model.regen() * content.difficulty().shieldRegen().of(difficulty),
                model.delay(),
                content.shields().breakSeconds());
    }

    static Plating plating(ArmourData.Plating plating) {
        return new Plating(plating.max());
    }

    /** The rules that do not depend on the level's script: bullet budget, pickups, scoring and credits. */
    public static Rules rules(Content content, String levelKey, Difficulty difficulty) {
        DifficultyData levers = content.difficulty();
        PlayerData player = content.player();
        PlayerData.Pickups pickups = player.pickups();
        PickupRules pickupRules = new PickupRules(
                pickups.salvage().credits().small(),
                pickups.salvage().credits().medium(),
                pickups.overdrive().seconds(),
                pickups.shieldCell().shieldPercent() / 100,
                pickups.armourPatch().armour(),
                player.pickupSeconds(),
                player.pickupDriftSpeed(),
                content.ship().collectionRadius(),
                pickups.salvage().credits().large());
        int act = levelKey(levelKey, 1);
        double creditFactor = levers.creditIncome().of(difficulty)
                * Math.pow(content.economy().actFactor(), act - 1);
        return new Rules(
                levers.bulletBudget().of(difficulty),
                Math.toRadians(levers.aimedSpreadDegrees().of(difficulty)),
                pickupRules,
                scoring(content.scoring(), levers.score().of(difficulty), creditFactor));
    }

    private static ScoringRules scoring(ScoringData scoring, double scoreFactor, double creditFactor) {
        ScoringData.Chain chain = scoring.chain();
        ScoringData.Rating rating = scoring.rating();
        return new ScoringRules(
                scoring.killScore(),
                scoring.pickupScore(),
                new ScoringRules.Chain(chain.window(), chain.step(), chain.increment(), chain.max()),
                scoreFactor,
                creditFactor,
                new ScoringRules.Weights(
                        rating.killRatio(), rating.armourDamage(), rating.secrets(), rating.maxChain()),
                scoring.bonuses().stream()
                        .map(bonus -> new ScoringRules.Bonus(
                                bonusKind(bonus.name()),
                                bonus.name(),
                                bonus.points(),
                                bonus.perKillPercent().orElse(false),
                                bonus.scale() == ScoringData.Scale.LEVEL))
                        .toList(),
                scoring.grades().stream()
                        .map(grade -> new ScoringRules.Grade(
                                grade.grade(), grade.rating().orElse(0), grade.creditBonus()))
                        .toList());
    }

    private static ScoringRules.BonusKind bonusKind(String name) {
        return switch (name) {
            case "Destruction" -> ScoringRules.BonusKind.DESTRUCTION;
            case "Untouched" -> ScoringRules.BonusKind.UNTOUCHED;
            case "Explorer" -> ScoringRules.BonusKind.EXPLORER;
            case "Boss rush" -> ScoringRules.BonusKind.BOSS_RUSH;
            default -> throw new IllegalArgumentException("no rule for the bonus '" + name + "'");
        };
    }

    /** The level's script at {@code difficulty}; {@code levelKey} as in {@link Content#level(String)}. */
    public static LevelScript level(Content content, String levelKey, Difficulty difficulty) {
        LevelData level = content.level(levelKey);
        if (!level.objectives().primary().equals("reach-end")) {
            throw new IllegalArgumentException(levelKey + ": only the reach-end objective is implemented");
        }
        LevelData.Secondary secondary = level.objectives()
                .secondary()
                .orElseThrow(() -> new IllegalArgumentException(levelKey + ": needs a secondary objective"));
        Optional<LevelData.Variant> variant =
                switch (difficulty) {
                    case EASY -> level.difficulty().easy();
                    case MEDIUM -> Optional.empty();
                    case HARD -> level.difficulty().hard();
                };
        List<LevelData.PlacedPickup> carried = new ArrayList<>(level.pickups());
        variant.flatMap(LevelData.Variant::extraPickups).ifPresent(carried::addAll);
        Map<String, LevelData.EnemyChange> enemyChanges =
                variant.flatMap(LevelData.Variant::enemies).orElse(Map.of());
        List<WaveSpec> waves = new ArrayList<>();
        for (LevelData.Wave wave : level.waves()) {
            addWave(content, wave, difficulty, enemyChanges, carried, waves);
        }
        return new LevelScript(
                levelKey(levelKey, 2),
                levelKey(levelKey, 1),
                level.launchSeconds(),
                level.sections().stream()
                        .map(section -> new LevelScript.Section(
                                section.end(), section.speed().orElse(level.scrollSpeed())))
                        .toList(),
                waves,
                groundObjects(level),
                groundUnits(content, level, difficulty, secondary),
                level.secrets().size(),
                radio(level, difficulty),
                new LevelScript.Secondary(
                        secondary.killRatio().orElse(0.0),
                        secondary.credits(),
                        secondary.groups().orElse(List.of()),
                        secondary.escapes().orElse("")),
                cranes(level, difficulty),
                debris(level, difficulty),
                level.setPieces().orElse(List.of()).stream()
                        .map(piece -> setPiece(content, piece, difficulty))
                        .toList());
    }

    /** The level's debris chunks at {@code difficulty}: every second large one left out on easy, faster on hard. */
    private static List<LevelScript.DebrisSpec> debris(LevelData level, Difficulty difficulty) {
        if (level.debris().isEmpty()) {
            return List.of();
        }
        LevelData.DebrisField field = level.debris().get();
        double factor = field.driftFactor(difficulty);
        return field.placedOn(difficulty).stream()
                .map(placed -> {
                    LevelData.Chunk chunk = field.chunks().get(placed.chunk());
                    if (chunk == null) {
                        throw new IllegalArgumentException("no debris chunk '" + placed.chunk() + "'");
                    }
                    return new LevelScript.DebrisSpec(
                            placed.t(),
                            placed.x(),
                            placed.chunk(),
                            hitbox(chunk.size()),
                            placed.drift().x() * factor,
                            placed.drift().y() * factor,
                            chunk.hp().orElse(Double.POSITIVE_INFINITY),
                            chunk.damage().orElse(0.0),
                            field.clearance());
                })
                .toList();
    }

    /**
     * A set piece at {@code difficulty} (design/enemies/space/leviathan): its parts with the HP
     * lever, their guns by their attack's name, and its passes. The parts of one attack fire
     * staggered: the n-th (from 0) first fires its attack's first-shot delay times n + 1 after the
     * unit reaches the player's layer, or one interval after it without a delay.
     */
    public static LevelScript.SetPieceSpec setPiece(
            Content content, LevelData.SetPieceData piece, Difficulty difficulty) {
        EnemyData enemy = content.enemy(piece.enemy());
        List<EnemyData.PartData> partList = enemy.partList()
                .orElseThrow(() -> new IllegalArgumentException(piece.enemy() + ": a set piece has a part_list"));
        List<LevelScript.PartSpec> parts = new ArrayList<>();
        Map<String, Integer> rank = new TreeMap<>();
        for (EnemyData.PartData part : partList) {
            Optional<EnemyGun> gun = Optional.empty();
            double first = 0;
            if (part.attack().isPresent()) {
                String name = part.attack().get();
                EnemyData.Attack attack = enemy.attack(name)
                        .orElseThrow(() -> new IllegalArgumentException(piece.enemy() + ": no attack '" + name + "'"));
                EnemyGun built = gun(content, enemy, attack, difficulty, Optional.empty());
                int n = rank.merge(name, 1, Integer::sum) - 1;
                first = attack.firstShotDelay().isPresent()
                        ? attack.firstShotDelay().get() * (n + 1)
                        : built.intervalSeconds();
                gun = Optional.of(built);
            }
            parts.add(new LevelScript.PartSpec(
                    part.name(),
                    part.offset().x(),
                    part.offset().y(),
                    hitbox(part.hitbox()),
                    content.difficulty().enemyHp(part.hp(), difficulty),
                    part.kind().equals("vital"),
                    part.bounty(),
                    gun,
                    first));
        }
        List<LevelScript.Pass> passes = new ArrayList<>();
        for (LevelData.PassData pass : piece.passes()) {
            List<LevelScript.Waypoint> path = pass.path().stream()
                    .map(point -> new LevelScript.Waypoint(point.t(), point.x(), PlayField.HEIGHT - point.y()))
                    .toList();
            double heading = -Math.toRadians(pass.heading());
            Layer layer = Layers.of(pass.layer());
            if (pass.descend().isEmpty()) {
                passes.add(LevelScript.Pass.flight(pass.name(), layer, heading, path));
            } else {
                LevelData.Descent descent = pass.descend().get();
                passes.add(new LevelScript.Pass(
                        pass.name(),
                        layer,
                        heading,
                        path,
                        descent.at(),
                        descent.seconds(),
                        descent.at() + pass.holdOn(difficulty).orElseThrow(),
                        pass.leaveSpeed().orElseThrow()));
            }
        }
        return new LevelScript.SetPieceSpec(
                piece.enemy(),
                hitbox(enemy.size()),
                hitbox(enemy.hitbox()),
                content.enemyBasis().contactDamage().get(enemy.tier()),
                parts,
                passes,
                drop(enemy).map(EnemySpec.Drop::pickup));
    }

    /** The ground enemies of the level's ground targets at {@code difficulty}, each in its group of the secondary objective. */
    private static List<LevelScript.GroundUnit> groundUnits(
            Content content, LevelData level, Difficulty difficulty, LevelData.Secondary secondary) {
        List<String> groups = secondary.groups().orElse(List.of());
        List<LevelScript.GroundUnit> units = new ArrayList<>();
        for (LevelData.GroundTarget target : level.groundTargets()) {
            if (target.enemy().isEmpty()) {
                continue;
            }
            EnemySpec enemy = enemy(content, target.enemy().get(), difficulty, Optional.empty());
            int group = target.group().map(groups::indexOf).orElse(-1);
            List<LevelData.Placement> at =
                    switch (difficulty) {
                        case EASY -> target.easy().map(LevelData.Placements::at).orElse(target.at());
                        case MEDIUM -> target.at();
                        case HARD -> target.hard().map(LevelData.Placements::at).orElse(target.at());
                    };
            for (LevelData.Placement placement : at) {
                units.add(new LevelScript.GroundUnit(placement.t(), placement.x(), enemy, group));
            }
        }
        units.sort(Comparator.comparingDouble(LevelScript.GroundUnit::t));
        return units;
    }

    /** The level's cranes at {@code difficulty}. */
    private static List<LevelScript.CraneSpec> cranes(LevelData level, Difficulty difficulty) {
        List<LevelScript.CraneSpec> cranes = new ArrayList<>();
        for (LevelData.CraneData crane : level.cranes().orElse(List.of())) {
            List<Double> swings =
                    switch (difficulty) {
                        case EASY -> crane.easy().map(LevelData.Swings::swings).orElse(crane.swings());
                        case MEDIUM -> crane.swings();
                        case HARD -> crane.hard().map(LevelData.Swings::swings).orElse(crane.swings());
                    };
            Optional<LevelData.Secret> secret = crane.clamp().flatMap(clamp -> level.secrets().stream()
                    .filter(s -> s.name().equals(clamp.reveals()))
                    .findFirst());
            cranes.add(new LevelScript.CraneSpec(
                    crane.x(),
                    crane.y(),
                    crane.length(),
                    crane.width(),
                    Math.toRadians(crane.from()),
                    Math.toRadians(crane.to()),
                    swings,
                    crane.swingSeconds(),
                    crane.telegraph(),
                    crane.damage(),
                    crane.clamp().map(LevelData.Clamp::hits).orElse(0),
                    secret.map(LevelData.Secret::crate).orElse(0),
                    secret.map(LevelData.Secret::name).orElse("")));
        }
        return cranes;
    }

    /** A number of the level key: group 1 is the act, group 2 the level. */
    private static int levelKey(String key, int group) {
        Matcher matcher = LEVEL_KEY.matcher(key);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("not a level key: " + key);
        }
        return Integer.parseInt(matcher.group(group));
    }

    private static void addWave(
            Content content,
            LevelData.Wave wave,
            Difficulty difficulty,
            Map<String, LevelData.EnemyChange> enemyChanges,
            List<LevelData.PlacedPickup> carried,
            List<WaveSpec> out) {
        Optional<LevelData.Change> change =
                switch (difficulty) {
                    case EASY -> wave.easy();
                    case MEDIUM -> Optional.empty();
                    case HARD -> wave.hard();
                };
        LevelData.Entry entry = change.flatMap(LevelData.Change::from).orElse(wave.from());
        Optional<LevelData.Edge> edge = change.flatMap(LevelData.Change::edge).or(wave::edge);
        int breakGroup = change.flatMap(LevelData.Change::breakGroup)
                .or(wave::breakGroup)
                .orElse(1);
        List<LevelData.Group> groups = wave.groupList();
        for (int g = 0; g < groups.size(); g++) {
            LevelData.Group group = groups.get(g);
            // An authored count replaces the formation size lever.
            int count = change.flatMap(LevelData.Change::count)
                    .orElseGet(() -> content.difficulty().formationSize(group.count(), difficulty));
            boolean first = g == 0;
            boolean last = g == groups.size() - 1;
            List<WaveSpec.Carried> pickups = carried.stream()
                    .filter(p -> p.droppedBy().wave() == wave.t())
                    .filter(p -> p.droppedBy().unit() == LevelData.CarrierUnit.LAST ? last : first)
                    .map(p -> new WaveSpec.Carried(
                            pickup(p.pickup()),
                            switch (p.droppedBy().unit()) {
                                case FIRST -> 0;
                                case SECOND -> 1;
                                case LAST -> WaveSpec.Carried.LAST;
                            }))
                    .toList();
            out.add(new WaveSpec(
                    wave.t(),
                    formation(group.formation()),
                    enemy(content, group.enemy(), difficulty, Optional.ofNullable(enemyChanges.get(group.enemy()))),
                    count,
                    switch (entry) {
                        case FRONT -> WaveSpec.Entry.FRONT;
                        case SIDES -> WaveSpec.Entry.SIDES;
                        case REAR -> WaveSpec.Entry.REAR;
                    },
                    edge.map(SimSpecs::edge).orElse(WaveSpec.Edge.NONE),
                    wave.hold(),
                    wave.warning(),
                    breakGroup,
                    wave.speed(),
                    wave.interval(),
                    pickups,
                    wave.at().map(at -> new WaveSpec.At(at.x(), at.y()))));
        }
    }

    private static WaveSpec.Edge edge(LevelData.Edge edge) {
        return switch (edge) {
            case LEFT -> WaveSpec.Edge.LEFT;
            case RIGHT -> WaveSpec.Edge.RIGHT;
            case ALTERNATING -> WaveSpec.Edge.ALTERNATING;
        };
    }

    private static WaveSpec.Formation formation(String name) {
        return switch (name) {
            case "snake" -> WaveSpec.Formation.SNAKE;
            case "V-wing" -> WaveSpec.Formation.V_WING;
            case "line abreast" -> WaveSpec.Formation.LINE_ABREAST;
            case "stream" -> WaveSpec.Formation.STREAM;
            case "pincer" -> WaveSpec.Formation.PINCER;
            case "circle" -> WaveSpec.Formation.CIRCLE;
            case "single" -> WaveSpec.Formation.SINGLE;
            case "column" -> WaveSpec.Formation.COLUMN;
            case "convoy" -> WaveSpec.Formation.CONVOY;
            case "whirl cluster" -> WaveSpec.Formation.WHIRL_CLUSTER;
            default -> throw new IllegalArgumentException("the formation '" + name + "' is not implemented yet");
        };
    }

    private static PickupType pickup(Pickup pickup) {
        return switch (pickup) {
            case SMALL_SALVAGE -> PickupType.SMALL_SALVAGE;
            case MEDIUM_SALVAGE -> PickupType.MEDIUM_SALVAGE;
            case LARGE_SALVAGE -> PickupType.LARGE_SALVAGE;
            case OVERDRIVE -> PickupType.OVERDRIVE;
            case SHIELD_CELL -> PickupType.SHIELD_CELL;
            case ARMOUR_PATCH -> PickupType.ARMOUR_PATCH;
            default -> throw new IllegalArgumentException("the pickup " + pickup + " is not implemented yet");
        };
    }

    /**
     * An enemy at {@code difficulty}: HP by the HP lever, its aimed attack (or mine) by the
     * fire-rate and bullet-speed levers, a level's burst change and the stat block's hooks; a
     * spiral-out, a convoy's strafe height and a death burst where the stat block has them.
     */
    public static EnemySpec enemy(
            Content content, String slug, Difficulty difficulty, Optional<LevelData.EnemyChange> change) {
        EnemyData enemy = content.enemy(slug);
        EnemyData.Movement movement = enemy.movement();
        return new EnemySpec(
                slug,
                content.difficulty().enemyHp(enemy.hp(), difficulty),
                hitbox(enemy.hitbox()),
                Layers.of(enemy.layer()),
                content.enemyBasis().contactDamage().get(enemy.tier()),
                enemy.tier().compareTo(Tier.SMALL) <= 0,
                enemy.bounty(),
                enemy.speed(),
                movement.snake().map(snake -> new EnemySpec.Snake(snake.spacing())),
                movement.straight().map(EnemyData.Straight::speed),
                movement.hover().map(hover -> new EnemySpec.Hover(range(hover.seconds()), range(hover.y()))),
                movement.orbit().map(orbit -> new EnemySpec.Orbit(orbit.radius(), orbit.turnRate())),
                gun(content, enemy, difficulty, change),
                drop(enemy),
                movement.dive()
                        .map(dive -> new EnemySpec.Dive(
                                range(dive.y()),
                                hook(enemy, difficulty)
                                        .flatMap(EnemyData.Hook::divePause)
                                        .orElse(dive.pause()),
                                dive.speed(),
                                dive.fireAfter())),
                movement.terrain().isPresent(),
                movement.spiralOut()
                        .map(spiral -> new EnemySpec.Spiral(
                                spiral.seconds(), spiral.turns(), spiral.growth(), spiral.drift(), spiral.ricochets())),
                movement.strafe().map(strafe -> range(strafe.y())),
                hook(enemy, difficulty)
                        .flatMap(EnemyData.Hook::deathBurst)
                        .map(puff -> new EnemySpec.DeathBurst(
                                puff.count(),
                                puff.speed()
                                        * content.difficulty()
                                                .enemyBulletSpeed()
                                                .of(difficulty),
                                bulletDamage(content, puff.bullet()))));
    }

    private static double bulletDamage(Content content, String bullet) {
        return content.enemyBasis().bulletDamage().stream()
                .filter(b -> b.bullet().equals(bullet))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("unknown bullet class '" + bullet + "'"))
                .damage();
    }

    private static Optional<EnemyData.Hook> hook(EnemyData enemy, Difficulty difficulty) {
        return enemy.difficulty().flatMap(hooks -> switch (difficulty) {
            case EASY -> hooks.easy();
            case MEDIUM -> Optional.empty();
            case HARD -> hooks.hard();
        });
    }

    private static Optional<EnemySpec.Drop> drop(EnemyData enemy) {
        if (enemy.drops().size() > 1) {
            throw new IllegalArgumentException(enemy.name() + ": only one drop rule is implemented");
        }
        return enemy.drops().stream().findFirst().map(drop -> new EnemySpec.Drop(pickup(drop.pickup()), drop.every()));
    }

    /**
     * An enemy's attack at {@code difficulty}: its interval by the fire-rate lever, its bullets by
     * the bullet-speed lever, a level's burst change or the stat block's hooks (burst, fan size,
     * leading the target in circles, a mine's ring and whether it bursts).
     */
    private static Optional<EnemyGun> gun(
            Content content, EnemyData enemy, Difficulty difficulty, Optional<LevelData.EnemyChange> change) {
        if (enemy.attacks().isEmpty()) {
            return Optional.empty();
        }
        if (enemy.attacks().size() > 1) {
            throw new IllegalArgumentException(enemy.name() + ": only a single attack is implemented");
        }
        return Optional.of(gun(content, enemy, enemy.attacks().getFirst(), difficulty, change));
    }

    private static EnemyGun gun(
            Content content,
            EnemyData enemy,
            EnemyData.Attack attack,
            Difficulty difficulty,
            Optional<LevelData.EnemyChange> change) {
        DifficultyData levers = content.difficulty();
        double damage = bulletDamage(content, attack.bullet());
        Optional<EnemyData.Hook> hook = hook(enemy, difficulty);
        // A burst is of aimed shots: a fan or a mine keeps one volley.
        int burst = attack.pattern().equals("aimed")
                ? change.flatMap(LevelData.EnemyChange::burst)
                        .or(() -> hook.flatMap(EnemyData.Hook::burst))
                        .orElse(1)
                : 1;
        int fan = attack.count()
                .map(count -> hook.flatMap(EnemyData.Hook::fanCount).orElse(count))
                .orElse(1);
        Optional<EnemyGun.MineSpec> mine = attack.mine()
                .map(spore -> new EnemyGun.MineSpec(
                        spore.arm(),
                        spore.life(),
                        spore.drift(),
                        spore.hp(),
                        hook.flatMap(EnemyData.Hook::ring).orElse(spore.ring()),
                        bulletDamage(content, spore.ringBullet()),
                        hook.flatMap(EnemyData.Hook::mineBursts).orElse(true),
                        spore.credits()));
        return new EnemyGun(
                attack.interval()
                        .map(i -> i / levers.enemyFireRate().of(difficulty))
                        .orElse(Double.POSITIVE_INFINITY),
                attack.firstShotDelay().orElse(0.0),
                burst,
                attack.speed() * levers.enemyBulletSpeed().of(difficulty),
                damage,
                hook.flatMap(EnemyData.Hook::leadsTargetIn)
                        .map(in -> in.contains("circle"))
                        .orElse(false),
                fan,
                Math.toRadians(attack.spread().orElse(0.0)),
                attack.turnRate().map(Math::toRadians).orElse(Double.POSITIVE_INFINITY),
                attack.arc().map(Math::toRadians).orElse(Double.POSITIVE_INFINITY),
                mine);
    }

    /**
     * The level's destructibles and triggers, in time order; triggers that reveal the same secret
     * reveal it together (Level 03's lifeboat lights: the crate drops when the last is shot).
     */
    private static List<LevelScript.GroundObjectSpec> groundObjects(LevelData level) {
        List<String> secretNames =
                level.secrets().stream().map(LevelData.Secret::name).toList();
        Map<String, Integer> triggersPerSecret = new TreeMap<>();
        for (LevelData.GroundTarget target : level.groundTargets()) {
            target.reveals()
                    .ifPresent(name -> triggersPerSecret.merge(name, target.at().size(), Integer::sum));
        }
        List<LevelScript.GroundObjectSpec> objects = new ArrayList<>();
        for (LevelData.GroundTarget target : level.groundTargets()) {
            if (target.enemy().isPresent()) {
                continue;
            }
            Optional<LevelData.Secret> secret = target.reveals().flatMap(name -> level.secrets().stream()
                    .filter(s -> s.name().equals(name))
                    .findFirst());
            for (LevelData.Placement placement : target.at()) {
                objects.add(new LevelScript.GroundObjectSpec(
                        placement.t(),
                        placement.x(),
                        hitbox(target.size().orElseThrow()),
                        target.hp().orElse(0.0),
                        target.bounty().orElse(0),
                        target.drop().map(SimSpecs::pickup),
                        target.hits().orElse(0),
                        secret.map(LevelData.Secret::crate).orElse(0),
                        secret.map(LevelData.Secret::name).orElse(""),
                        target.hardened().orElse(false),
                        secret.map(sec -> secretNames.indexOf(sec.name())).orElse(-1),
                        target.reveals().map(triggersPerSecret::get).orElse(1)));
            }
        }
        objects.sort(Comparator.comparingDouble(LevelScript.GroundObjectSpec::t));
        return objects;
    }

    private static List<LevelScript.RadioCue> radio(LevelData level, Difficulty difficulty) {
        List<LevelScript.RadioCue> cues = new ArrayList<>();
        for (LevelData.RadioCue cue : level.radio()) {
            Optional<LevelData.RadioChange> change =
                    switch (difficulty) {
                        case EASY -> cue.easy();
                        case MEDIUM -> Optional.empty();
                        case HARD -> cue.hard();
                    };
            LevelScript.CueTrigger trigger = cue.event()
                    .map(event -> switch (event) {
                        case FIRST_KILL -> LevelScript.CueTrigger.FIRST_KILL;
                        case SECONDARY_OBJECTIVE -> LevelScript.CueTrigger.SECONDARY_OBJECTIVE;
                        case LEVEL_END -> LevelScript.CueTrigger.LEVEL_END;
                        case GROUP_CLEARED -> LevelScript.CueTrigger.GROUP_CLEARED;
                        case GROUP_LOST -> LevelScript.CueTrigger.GROUP_LOST;
                        case FIRST_GROUP_LOST -> LevelScript.CueTrigger.FIRST_GROUP_LOST;
                        case ENEMY_ESCAPED -> LevelScript.CueTrigger.ENEMY_ESCAPED;
                    })
                    .orElse(LevelScript.CueTrigger.TIME);
            cues.add(new LevelScript.RadioCue(
                    trigger,
                    cue.t().orElse(0.0),
                    cue.enemy().or(cue::group).orElse(""),
                    cue.speaker(),
                    change.map(LevelData.RadioChange::line).orElse(cue.line()),
                    cue.distorted().orElse(false),
                    cue.expression().orElse(Expression.NEUTRAL).slug(),
                    cue.portrait().orElse(cue.speaker())));
        }
        for (LevelData.Secret secret : level.secrets()) {
            LevelData.RadioLine line = secret.radio();
            cues.add(new LevelScript.RadioCue(
                    LevelScript.CueTrigger.SECRET,
                    0,
                    secret.name(),
                    line.speaker(),
                    line.line(),
                    line.distorted().orElse(false),
                    line.expression().orElse(Expression.NEUTRAL).slug()));
        }
        return cues;
    }

    private static Range range(Span span) {
        return new Range(span.min(), span.max());
    }

    /** The hull boxes, from sprite pixels (y down from the top left) to offsets around the centre (y up). */
    private static Hull hull(ShipData ship) {
        double centre = ship.size() / 2;
        return new Hull(ship.hull().stream()
                .map(box -> new Hull.Part(
                        box.x() + box.width() / 2 - centre,
                        centre - (box.y() + box.height() / 2),
                        new Hitbox(box.width(), box.height())))
                .toList());
    }

    private static Hitbox hitbox(Size size) {
        return new Hitbox(size.width(), size.height());
    }
}
