package vanguard.content;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import vanguard.sim.Armament;
import vanguard.sim.EnemyGun;
import vanguard.sim.EnemySpec;
import vanguard.sim.Hitbox;
import vanguard.sim.Hull;
import vanguard.sim.LevelScript;
import vanguard.sim.Loadout;
import vanguard.sim.PickupRules;
import vanguard.sim.PickupType;
import vanguard.sim.Plating;
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
                pickups.shieldCell().shieldPercent() / 100,
                pickups.armourPatch().armour(),
                player.pickupSeconds(),
                player.pickupDriftSpeed(),
                content.ship().collectionRadius());
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
                level.secrets().size(),
                radio(level, difficulty),
                new LevelScript.Secondary(secondary.killRatio(), secondary.credits()));
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
                            pickup(p.pickup()), p.droppedBy().unit() == LevelData.CarrierUnit.LAST))
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
                    pickups));
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
            default -> throw new IllegalArgumentException("the formation '" + name + "' is not implemented yet");
        };
    }

    private static PickupType pickup(Pickup pickup) {
        return switch (pickup) {
            case SMALL_SALVAGE -> PickupType.SMALL_SALVAGE;
            case SHIELD_CELL -> PickupType.SHIELD_CELL;
            case ARMOUR_PATCH -> PickupType.ARMOUR_PATCH;
            default -> throw new IllegalArgumentException("the pickup " + pickup + " is not implemented yet");
        };
    }

    /**
     * An enemy at {@code difficulty}: HP by the HP lever, its aimed attack by the fire-rate and
     * bullet-speed levers, a level's burst change and the stat block's hard-mode hook.
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
                drop(enemy));
    }

    private static Optional<EnemySpec.Drop> drop(EnemyData enemy) {
        if (enemy.drops().size() > 1) {
            throw new IllegalArgumentException(enemy.name() + ": only one drop rule is implemented");
        }
        return enemy.drops().stream().findFirst().map(drop -> new EnemySpec.Drop(pickup(drop.pickup()), drop.every()));
    }

    private static Optional<EnemyGun> gun(
            Content content, EnemyData enemy, Difficulty difficulty, Optional<LevelData.EnemyChange> change) {
        if (enemy.attacks().isEmpty()) {
            return Optional.empty();
        }
        EnemyData.Attack attack = enemy.attacks().getFirst();
        if (enemy.attacks().size() > 1 || !attack.pattern().equals("aimed")) {
            throw new IllegalArgumentException(enemy.name() + ": only a single aimed attack is implemented");
        }
        DifficultyData levers = content.difficulty();
        double damage = content.enemyBasis().bulletDamage().stream()
                .filter(b -> b.bullet().equals(attack.bullet()))
                .findFirst()
                .orElseThrow()
                .damage();
        Optional<EnemyData.Hook> hook = enemy.difficulty().flatMap(hooks -> switch (difficulty) {
            case EASY -> hooks.easy();
            case MEDIUM -> Optional.empty();
            case HARD -> hooks.hard();
        });
        return Optional.of(new EnemyGun(
                attack.interval() / levers.enemyFireRate().of(difficulty),
                attack.firstShotDelay(),
                change.flatMap(LevelData.EnemyChange::burst).orElse(1),
                attack.speed() * levers.enemyBulletSpeed().of(difficulty),
                damage,
                hook.map(h -> h.leadsTargetIn().contains("circle")).orElse(false)));
    }

    private static List<LevelScript.GroundObjectSpec> groundObjects(LevelData level) {
        List<LevelScript.GroundObjectSpec> objects = new ArrayList<>();
        for (LevelData.GroundTarget target : level.groundTargets()) {
            Optional<LevelData.Secret> secret = target.reveals().flatMap(name -> level.secrets().stream()
                    .filter(s -> s.name().equals(name))
                    .findFirst());
            for (LevelData.Placement placement : target.at()) {
                objects.add(new LevelScript.GroundObjectSpec(
                        placement.t(),
                        placement.x(),
                        hitbox(target.size()),
                        target.hp().orElse(0.0),
                        target.bounty().orElse(0),
                        target.drop().map(SimSpecs::pickup),
                        target.hits().orElse(0),
                        secret.map(LevelData.Secret::crate).orElse(0),
                        secret.map(LevelData.Secret::name).orElse(""),
                        target.hardened().orElse(false)));
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
                    })
                    .orElse(LevelScript.CueTrigger.TIME);
            cues.add(new LevelScript.RadioCue(
                    trigger,
                    cue.t().orElse(0.0),
                    cue.enemy().orElse(""),
                    cue.speaker(),
                    change.map(LevelData.RadioChange::line).orElse(cue.line()),
                    cue.distorted().orElse(false),
                    cue.expression().orElse(Expression.NEUTRAL).slug()));
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
