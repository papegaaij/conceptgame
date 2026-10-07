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
import vanguard.sim.AirstrikeSpec;
import vanguard.sim.AllySpec;
import vanguard.sim.Armament;
import vanguard.sim.BossSpec;
import vanguard.sim.EnemyGun;
import vanguard.sim.EnemySpec;
import vanguard.sim.Hitbox;
import vanguard.sim.Hull;
import vanguard.sim.Layer;
import vanguard.sim.LevelResult;
import vanguard.sim.LevelScript;
import vanguard.sim.Loadout;
import vanguard.sim.Magnet;
import vanguard.sim.PickupRules;
import vanguard.sim.PickupType;
import vanguard.sim.Plating;
import vanguard.sim.PlayField;
import vanguard.sim.Range;
import vanguard.sim.Road;
import vanguard.sim.Rules;
import vanguard.sim.ScoringRules;
import vanguard.sim.ShieldModel;
import vanguard.sim.ShipSpec;
import vanguard.sim.SmartBombSpec;
import vanguard.sim.SpecialSpec;
import vanguard.sim.WaveSpec;
import vanguard.sim.WeaponSpec;
import vanguard.sim.WingmanSpec;

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
        return loadout(content, engine, weapons, shield, plating, sparePower, difficulty, 0);
    }

    /**
     * As {@link #loadout(Content, String, List, String, String, double, Difficulty)}, the weapons'
     * one turn rate (a homing missile's turn, a turret's slew) raised by {@code turnBonus}: the
     * fitted Targeting computer's share (design/player/systems), 0 without one. Only the
     * Stormhawk's own weapons: Rook's guns are built by {@link #wingman} at their base turn.
     */
    public static Loadout loadout(
            Content content,
            String engine,
            List<FittedWeapon> weapons,
            String shield,
            String plating,
            double sparePower,
            Difficulty difficulty,
            double turnBonus) {
        ShieldModel model =
                shield(content, named(content.shields().models(), ShieldData.Model::name, shield), difficulty);
        double regen = model.regenPerSecond() * (1 + regenBonus(content, sparePower));
        return new Loadout(
                ship(content, named(content.systems().engines(), SystemsData.Engine::name, engine)),
                new Armament(weapons.stream()
                        .map(fitted -> new Armament.Mount(
                                fitted.slot(),
                                weapon(content, fitted.slot(), fitted.weapon(), fitted.level(), turnBonus),
                                weapon(content, fitted.slot(), fitted.weapon(), fitted.level() + 1, turnBonus)))
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
     * Whether the simulation flies the weapon: standard bolts, homing missiles and turrets, dropped
     * and lobbed ground-only shots and proximity mines; the torpedoes follow with the {@code sub}
     * layer.
     */
    public static boolean flies(Content content, String weapon) {
        return delivery(content.weapon(weapon)).isPresent();
    }

    /** The utility modules the simulation flies (design/player/systems). */
    public static final String PICKUP_MAGNET = "Pickup magnet";

    public static final String TARGETING_COMPUTER = "Targeting computer";

    public static final String SALVAGE_SCANNER = "Salvage scanner";

    /**
     * Whether the simulation flies the utility module of this name: the Pickup magnet, the Targeting
     * computer and the Salvage scanner; the later acts' modules follow with them.
     */
    public static boolean fliesUtility(String name) {
        return name.equals(PICKUP_MAGNET) || name.equals(TARGETING_COMPUTER) || name.equals(SALVAGE_SCANNER);
    }

    /** The Targeting computer's numbers from design/player/systems/data.yaml. */
    public static SystemsData.Targeting targeting(Content content) {
        return named(content.systems().utility(), SystemsData.Utility::name, TARGETING_COMPUTER)
                .targeting()
                .orElseThrow(() -> new IllegalArgumentException(TARGETING_COMPUTER + " has no targeting numbers"));
    }

    /** The Salvage scanner's credit bonus at {@code level} (1–2): the share added to salvage and hidden crates. */
    public static double salvageBonus(Content content, int level) {
        return named(content.systems().utility(), SystemsData.Utility::name, SALVAGE_SCANNER)
                .salvage()
                .orElseThrow(() -> new IllegalArgumentException(SALVAGE_SCANNER + " has no salvage numbers"))
                .bonus()
                .get(level - 1);
    }

    /** The Pickup magnet at {@code level} (1–3) from design/player/systems/data.yaml. */
    public static Magnet magnet(Content content, int level) {
        SystemsData.Magnet numbers = named(content.systems().utility(), SystemsData.Utility::name, PICKUP_MAGNET)
                .magnet()
                .orElseThrow(() -> new IllegalArgumentException(PICKUP_MAGNET + " has no magnet numbers"));
        return new Magnet(numbers.radius().get(level - 1), numbers.pull().get(level - 1));
    }

    /**
     * Rook in the escort slot (design/player/wingmen) with his gun {@code gunId} at {@code level}
     * (1–5), on {@code side}, starting the level with {@code armour}: his gun is the base weapon's
     * pattern at that level with each projectile's damage scaled, fired from his one muzzle at the
     * nose (a pod weapon's pattern as the pod on his side has it, so the Missiles launch outward);
     * no overdrive.
     */
    public static WingmanSpec wingman(Content content, String gunId, int level, WingmanSpec.Side side, double armour) {
        WingmenData data = content.wingmen();
        WingmenData.Rook rook = data.rook();
        WingmenData.Gun gun = data.guns().gun(gunId);
        WeaponData base = content.weapon(gun.base());
        if (level < 1 || level > base.levels().size()) {
            throw new IllegalArgumentException(
                    gunId + ": level " + level + " outside 1.." + base.levels().size());
        }
        WeaponSpec pattern = weapon(content, Armament.Slot.FRONT, gun.base(), level);
        Point front = content.ship().mounts().front();
        double shipCentre = content.ship().size() / 2;
        double mirror = side == WingmanSpec.Side.LEFT && base.pod().orElse(false) ? -1 : 1;
        double noseX = rook.muzzle().x() - rook.size().width() / 2;
        double noseY = rook.size().height() / 2 - rook.muzzle().y();
        List<WeaponSpec.Muzzle> muzzles = pattern.muzzles().stream()
                .map(muzzle -> new WeaponSpec.Muzzle(
                        noseX + mirror * (muzzle.dx() - (front.x() - shipCentre)), noseY, mirror * muzzle.angle()))
                .toList();
        WingmenData.Formations formations = rook.formations();
        WingmenData.Dodge dodge = rook.dodge();
        double lowArmour = data.barks()
                .bark(WingmenData.ROOK_ARMOUR)
                .flatMap(WingmenData.Bark::below)
                .orElseThrow();
        return new WingmanSpec(
                side,
                armour,
                new WingmanSpec.Craft(
                        rook.size().width(),
                        rook.armour(),
                        hitbox(rook.hitbox()),
                        rook.speed(),
                        rook.accelerationSeconds(),
                        rook.minDistance(),
                        rook.edgeGap(),
                        rook.ramDamage(),
                        lowArmour,
                        rook.eject().podSpeed(),
                        content.ship().bankChangeSteps() / ShipSpec.HARD_BANK),
                new WingmanSpec.Ai(
                        offset(formations.wing()),
                        offset(formations.wide()),
                        offset(formations.trail()),
                        rook.glideSeconds(),
                        rook.swapSeconds(),
                        rook.flankDistance(),
                        rook.reactionSeconds(),
                        dodge.interval(),
                        dodge.lookAhead(),
                        dodge.clearance(),
                        dodge.step(),
                        dodge.reacts(),
                        Math.toRadians(rook.cone() / 2),
                        rook.range(),
                        rook.recentHitSeconds()),
                pattern.scaled(gun.scale(), muzzles));
    }

    private static WingmanSpec.Offset offset(Point slot) {
        return new WingmanSpec.Offset(slot.x(), slot.y());
    }

    /** The specials the simulation flies so far (design/player/specials): the Airstrike and the Smart Bomb. */
    public static final String AIRSTRIKE = "Airstrike";

    public static final String SMART_BOMB = "Smart Bomb";

    /** Whether the simulation flies the special of this name; the Decoy Flares follow later in M4. */
    public static boolean fliesSpecial(String name) {
        return name.equals(AIRSTRIKE) || name.equals(SMART_BOMB);
    }

    /** The special of this name with {@code charges} carried into the level; it must fly. */
    public static SpecialSpec special(Content content, String name, int charges) {
        if (!fliesSpecial(name)) {
            throw new IllegalArgumentException("the special " + name + " does not fly yet");
        }
        SpecialsData specials = content.specials();
        SpecialsData.Special special = specials.specials().stream()
                .filter(item -> item.name().equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("no special '" + name + "'"));
        int carried = Math.min(charges, special.maxCharges());
        if (name.equals(SMART_BOMB)) {
            SpecialsData.SmartBomb bomb = specials.smartBomb();
            return new SpecialSpec(
                    name,
                    carried,
                    special.maxCharges(),
                    specials.inputBuffer(),
                    new SmartBombSpec(
                            bomb.ring(),
                            bomb.damage().all(),
                            bomb.damage().bossPart(),
                            bomb.invulnerable(),
                            bomb.repeat(),
                            bomb.flash().seconds(),
                            bomb.flash().opacity(),
                            bomb.flash().fade()));
        }
        SpecialsData.Airstrike data = specials.airstrike();
        AirstrikeSpec airstrike = new AirstrikeSpec(
                data.delay(),
                data.offset(),
                data.speed(),
                hitbox(data.bomberSize()),
                data.bombSpacing(),
                data.fall(),
                data.blastRadius(),
                data.damage().ground(),
                data.cap().ground(),
                data.damage().air(),
                data.cap().air(),
                data.cap().bossPart());
        return new SpecialSpec(
                name, Math.min(charges, special.maxCharges()), special.maxCharges(), specials.inputBuffer(), airstrike);
    }

    private static Optional<WeaponSpec.Delivery> delivery(WeaponData weapon) {
        return switch (weapon.hits()) {
            case "standard" -> Optional.of(WeaponSpec.Delivery.BOLT);
            case "homing" ->
                Optional.of(weapon.slew().isPresent() ? WeaponSpec.Delivery.TURRET : WeaponSpec.Delivery.HOMING);
            case WeaponData.MINES -> Optional.of(WeaponSpec.Delivery.MINE);
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
     * wing mount, the left one mirrored, and turns in by the weapon's convergence. A weapon with
     * ports fires the shots of a volley from them in turn, left first.
     */
    static WeaponSpec weapon(Content content, Armament.Slot slot, String slug, int upgradeLevel) {
        return weapon(content, slot, slug, upgradeLevel, 0);
    }

    /** As {@link #weapon(Content, Armament.Slot, String, int)}, its turn rate raised by {@code turnBonus} (a share). */
    static WeaponSpec weapon(Content content, Armament.Slot slot, String slug, int upgradeLevel, double turnBonus) {
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
        double ports = weapon.ports().orElse(0.0);
        for (WeaponData.Shot shot : level.pattern()) {
            double port = muzzles.size() % 2 == 0 ? -ports : ports;
            switch (slot) {
                case FRONT -> muzzles.add(muzzle(mounts.front(), centre, shot.x() + port, shot.angle()));
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
        double speed = weapon.speed().map(WeaponData.Speed::start).orElse(0.0);
        WeaponSpec.Mines mines = delivery == WeaponSpec.Delivery.MINE
                ? new WeaponSpec.Mines(
                        weapon.drift().orElseThrow(),
                        weapon.arm().orElseThrow(),
                        weapon.trigger().orElseThrow(),
                        level.maxLive().orElseThrow())
                : WeaponSpec.Mines.NONE;
        return new WeaponSpec(
                slug,
                weapon.vfx(),
                weapon.sfx(),
                delivery,
                weapon.traits().contains("anti-ground"),
                level.rate(),
                level.damage(),
                speed,
                hitbox(weapon.size()),
                weapon.range()
                        .map(range -> switch (range.kind()) {
                            case SCREEN -> Double.POSITIVE_INFINITY;
                            case DROP -> 0.0;
                            case DISTANCE -> range.px();
                        })
                        .orElse(Double.POSITIVE_INFINITY),
                weapon.lifetime().orElse(Double.POSITIVE_INFINITY),
                level.pierce().orElse(1),
                level.blast().orElse(0.0),
                // The one turn rate (a homing missile's or a turret's slew) the Targeting computer's bonus scales.
                Math.toRadians(level.turn().or(weapon::slew).orElse(0.0) * (1 + turnBonus)),
                Math.toRadians(weapon.cone().orElse(360.0) / 2),
                weapon.fall().or(weapon::flight).orElse(0.0),
                weapon.snap().orElse(0.0),
                muzzles,
                weapon.speed().map(WeaponData.Speed::end).orElse(speed),
                weapon.accelerate().orElse(0.0),
                mines);
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
                scoring(
                        content.scoring(),
                        levers.score().of(difficulty),
                        creditFactor,
                        content.level(levelKey).bounties()));
    }

    private static ScoringRules scoring(
            ScoringData scoring, double scoreFactor, double creditFactor, double bountyScale) {
        ScoringData.Chain chain = scoring.chain();
        ScoringData.Rating rating = scoring.rating();
        return new ScoringRules(
                scoring.killScore(),
                scoring.pickupScore(),
                new ScoringRules.Chain(chain.window(), chain.step(), chain.increment(), chain.max()),
                scoreFactor,
                creditFactor,
                new ScoringRules.Weights(
                        rating.killRatio(),
                        rating.armourDamage(),
                        rating.secrets(),
                        rating.maxChain(),
                        rating.fullChain()),
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
                        .toList(),
                bountyScale);
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
        // Part G (LevelRules): a placed pickup may be left out on a difficulty.
        carried.removeIf(placed -> !placed.dropsOn(difficulty));
        Map<String, LevelData.EnemyChange> enemyChanges =
                variant.flatMap(LevelData.Variant::enemies).orElse(Map.of());
        InLevel inLevel = new InLevel(levelKey(levelKey, 1), levelKey(levelKey, 2));
        List<WaveSpec> waves = new ArrayList<>();
        for (LevelData.Wave wave : level.waves()) {
            if (!wave.fliesOn(difficulty)) {
                continue;
            }
            addWave(content, wave, difficulty, enemyChanges, carried, waves, inLevel);
        }
        return new LevelScript(
                levelKey(levelKey, 2),
                levelKey(levelKey, 1),
                level.launchSeconds(),
                level.sections().stream()
                        .map(section -> new LevelScript.Section(
                                section.end(), section.speed().orElse(level.scrollSpeed()), section.isArena()))
                        .toList(),
                waves,
                groundObjects(level),
                groundUnits(content, level, difficulty, secondary, inLevel),
                level.secrets().size(),
                radio(level, difficulty),
                LevelRules.secondary(content, level, secondary),
                cranes(level, difficulty),
                debris(level, difficulty),
                java.util.stream.Stream.concat(
                                level.setPieces().orElse(List.of()).stream()
                                        .map(piece -> setPiece(content, piece, difficulty, inLevel)),
                                level.boss().map(boss -> boss(content, boss, difficulty, inLevel)).stream())
                        .toList(),
                level.objectives().escort().map(escort -> escort(content, escort, difficulty)),
                level.road().map(road -> road(level, road)),
                level.objectives().targets().orElse(List.of()),
                level.sleds().map(sleds -> sled(sleds, difficulty)),
                level.rocks()
                        .filter(rocks ->
                                difficulty != Difficulty.EASY || rocks.onEasy().orElse(false))
                        .map(SimSpecs::rocks),
                groupDrops(level, carried),
                level.darkness().map(darkness -> darkness(darkness, difficulty)),
                LevelRules.tows(level),
                LevelRules.partDrops(content, level, carried, SimSpecs::pickup),
                PartCRules.holds(level, difficulty),
                PartCRules.collapse(level));
    }

    /** A dark level's light at {@code difficulty} (Level 06): easy's longer headlight and flares, the flares it fires. */
    static LevelScript.Darkness darkness(LevelData.Darkness darkness, Difficulty difficulty) {
        boolean easy = difficulty == Difficulty.EASY;
        LevelData.Headlight headlight = darkness.headlight();
        LevelData.FlareFall fall = darkness.flare();
        return new LevelScript.Darkness(
                headlight.from(),
                easy ? headlight.easy().orElse(headlight.length()) : headlight.length(),
                Math.toRadians(headlight.angle()),
                darkness.flares().stream()
                        .filter(flare -> flare.firedOn(difficulty))
                        .map(flare -> new LevelScript.Darkness.Flare(flare.t(), flare.x(), flare.y()))
                        .toList(),
                easy ? fall.easySeconds().orElse(fall.seconds()) : fall.seconds(),
                fall.radius(),
                fall.drift(),
                darkness.lights().orElse(List.of()).stream()
                        .map(light -> new LevelScript.Darkness.Light(light.t(), light.x(), light.radius()))
                        .toList(),
                darkness.ambient());
    }

    /** The level's sleds at {@code difficulty}: the period of easy or hard. */
    static LevelScript.SledSpec sled(LevelData.Sleds sleds, Difficulty difficulty) {
        Optional<LevelData.SledChange> change =
                switch (difficulty) {
                    case EASY -> sleds.easy();
                    case MEDIUM -> Optional.empty();
                    case HARD -> sleds.hard();
                };
        return new LevelScript.SledSpec(
                sleds.x(),
                sleds.width(),
                sleds.first(),
                change.map(LevelData.SledChange::period).orElse(sleds.period()),
                sleds.until(),
                sleds.lights(),
                sleds.run(),
                sleds.damage(),
                sleds.clamp().orElse(""));
    }

    private static LevelScript.RockSpec rocks(LevelData.Rocks rocks) {
        return new LevelScript.RockSpec(
                rocks.count().min(),
                rocks.count().max(),
                rocks.speed().min(),
                rocks.speed().max(),
                rocks.life(),
                hitbox(rocks.size()),
                rocks.hp(),
                rocks.damage(),
                rocks.clearance());
    }

    /** The pickups a ground-target group's last unit drops when the group is cleared (Level 05's battery C). */
    private static List<LevelScript.GroupDrop> groupDrops(LevelData level, List<LevelData.PlacedPickup> carried) {
        List<String> groups = level.objectives().groups();
        return carried.stream()
                .filter(p -> p.droppedBy().group().isPresent())
                .map(p -> new LevelScript.GroupDrop(
                        groups.indexOf(p.droppedBy().group().get()), pickup(p.pickup())))
                .toList();
    }

    /**
     * The level's road for the simulation: each point's {@code t} turned into the ground distance
     * that passes the middle of the screen then.
     */
    public static Road road(LevelData level, LevelData.Road road) {
        List<LevelData.RoadPoint> points = road.points();
        double[] distances = new double[points.size()];
        double[] xs = new double[points.size()];
        for (int i = 0; i < points.size(); i++) {
            distances[i] = level.scrollAt(points.get(i).t());
            xs[i] = points.get(i).x();
        }
        return new Road(road.width(), distances, xs);
    }

    /** The escort objective's convoy at {@code difficulty}: the ally's spec with the level's HP for it. */
    static LevelScript.Escort escort(Content content, LevelData.Escort escort, Difficulty difficulty) {
        AlliesData.Ally ally = content.allies().allies().get(escort.ally());
        if (ally == null) {
            throw new IllegalArgumentException("no ally '" + escort.ally() + "'");
        }
        Optional<LevelData.AllyChange> change =
                switch (difficulty) {
                    case EASY -> escort.easy();
                    case MEDIUM -> Optional.empty();
                    case HARD -> escort.hard();
                };
        AllySpec spec = new AllySpec(
                escort.ally(),
                hitbox(ally.size()),
                hitbox(ally.hitbox()),
                change.map(LevelData.AllyChange::hp).orElse(ally.hp()),
                ally.damagedBy().objectiveAimed(),
                ally.damagedBy().claws(),
                ally.smokeBelow(),
                (ally.headings().count() - 1) / 2 * ally.headings().step());
        return new LevelScript.Escort(
                spec,
                escort.y().stream().map(y -> PlayField.HEIGHT - y).toList(),
                escort.enter().t(),
                escort.enter().interval(),
                escort.enter().speed(),
                escort.credits(),
                escort.hook().enemies());
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
        return setPiece(content, piece, difficulty, InLevel.NOWHERE);
    }

    /** As {@link #setPiece(Content, LevelData.SetPieceData, Difficulty)}, a returning one with the act HP factor of its level. */
    private static LevelScript.SetPieceSpec setPiece(
            Content content, LevelData.SetPieceData piece, Difficulty difficulty, InLevel inLevel) {
        EnemyData enemy = content.enemy(piece.enemy());
        double actHp = actHpFactor(content, enemy, inLevel);
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
                    content.difficulty().enemyHp(part.hp() * actHp, difficulty),
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

    /**
     * The level's boss at {@code difficulty} (design/enemies/bosses): a set piece with its parts (the
     * HP lever, weak-point multipliers), its chains, its attacks (the fire-rate and bullet-speed
     * levers, the stat block's per-attack changes) and its phases, arriving where the level places it.
     */
    public static LevelScript.SetPieceSpec boss(
            Content content, LevelData.BossPlacement placement, Difficulty difficulty) {
        return boss(content, placement, difficulty, InLevel.NOWHERE);
    }

    /**
     * As {@link #boss(Content, LevelData.BossPlacement, Difficulty)} in a level: a returning boss's
     * parts and the units it releases get the act HP factor of that level.
     */
    private static LevelScript.SetPieceSpec boss(
            Content content, LevelData.BossPlacement placement, Difficulty difficulty, InLevel inLevel) {
        String slug = placement.enemy();
        EnemyData enemy = content.enemy(slug);
        EnemyData.BossData script =
                enemy.boss().orElseThrow(() -> new IllegalArgumentException(slug + ": no boss script"));
        List<EnemyData.PartData> partList =
                enemy.partList().orElseThrow(() -> new IllegalArgumentException(slug + ": a boss has a part_list"));
        List<LevelScript.PartSpec> parts = new ArrayList<>();
        List<Integer> armoured = new ArrayList<>();
        for (int p = 0; p < partList.size(); p++) {
            EnemyData.PartData part = partList.get(p);
            if (part.armoured()) {
                // A fire-only part (part G): it never takes damage, so its HP only keeps it alive.
                armoured.add(p);
            }
            parts.add(new LevelScript.PartSpec(
                    part.name(),
                    part.offset().x(),
                    part.offset().y(),
                    hitbox(part.hitbox()),
                    part.armoured()
                            ? 1
                            : content.difficulty()
                                    .enemyHp(part.hp() * actHpFactor(content, enemy, inLevel), difficulty),
                    part.kind().equals("vital"),
                    part.bounty(),
                    Optional.empty(),
                    0,
                    part.multiplier().orElse(1.0)));
        }
        List<BossSpec.Chain> chains = new ArrayList<>();
        for (EnemyData.ChainData chain : enemy.chains().orElse(List.of())) {
            chains.add(new BossSpec.Chain(
                    chain.name(),
                    chain.from().x(),
                    chain.from().y(),
                    partIndex(enemy, chain.to()),
                    chain.segments(),
                    hitbox(chain.hitbox()),
                    chain.lag(),
                    Math.toRadians(chain.bend())));
        }
        List<String> attackNames = new ArrayList<>();
        List<BossSpec.Attack> attacks = new ArrayList<>();
        DifficultyData levers = content.difficulty();
        Optional<EnemyData.Hook> hook = hook(enemy, difficulty);
        Map<String, EnemyData.AttackChange> changes =
                hook.flatMap(EnemyData.Hook::attacks).orElse(Map.of());
        for (EnemyData.Attack attack : enemy.attacks()) {
            String name =
                    attack.name().orElseThrow(() -> new IllegalArgumentException(slug + ": a boss names its attacks"));
            Optional<EnemyData.AttackChange> change = Optional.ofNullable(changes.get(name));
            BossSpec.Pattern pattern =
                    switch (attack.pattern()) {
                        case "aimed" -> BossSpec.Pattern.AIMED;
                        case "ring" -> BossSpec.Pattern.RING;
                        case "spiral" -> BossSpec.Pattern.SPIRAL;
                        case "fan" -> BossSpec.Pattern.FAN;
                        default ->
                            throw new IllegalArgumentException(slug
                                    + ": a boss fires aimed, fan, ring or spiral attacks, not " + attack.pattern());
                    };
            int burst = change.flatMap(EnemyData.AttackChange::burst)
                    .or(attack::burst)
                    .orElse(1);
            int count = change.flatMap(EnemyData.AttackChange::count)
                    .or(attack::count)
                    .orElse(1);
            List<Integer> firing = new ArrayList<>();
            for (int p = 0; p < partList.size(); p++) {
                if (partList.get(p).attack().filter(name::equals).isPresent()) {
                    firing.add(p);
                }
            }
            double interval =
                    attack.interval().orElseThrow() / levers.enemyFireRate().of(difficulty);
            double speed =
                    attack.speed().orElseThrow() * levers.enemyBulletSpeed().of(difficulty);
            double damage = bulletDamage(content, attack.bullet().orElseThrow());
            EnemyGun gun = pattern == BossSpec.Pattern.FAN
                    ? new EnemyGun(
                            interval,
                            0,
                            1,
                            speed,
                            damage,
                            false,
                            count,
                            Math.toRadians(attack.spread().orElseThrow()),
                            Double.POSITIVE_INFINITY,
                            Double.POSITIVE_INFINITY)
                    : EnemyGun.aimed(interval, 0, burst, speed, damage, false);
            attackNames.add(name);
            attacks.add(new BossSpec.Attack(
                    name,
                    pattern,
                    gun,
                    attack.burstGap().orElse(EnemyGun.BURST_GAP_SECONDS),
                    attack.rotate().orElse(false),
                    count,
                    change.flatMap(EnemyData.AttackChange::arms)
                            .or(attack::arms)
                            .orElse(0),
                    Math.toRadians(attack.turnRate().orElse(0.0)),
                    attack.duration().orElse(0.0),
                    firing));
        }
        Map<String, Integer> spawnCounts = hook.flatMap(EnemyData.Hook::spawns).orElse(Map.of());
        List<BossSpec.Phase> phases = new ArrayList<>();
        for (int f = 0; f < script.phases().size(); f++) {
            EnemyData.PhaseData phase = script.phases().get(f);
            List<String> fired = phase.alternate().or(phase::attacks).orElse(List.of());
            List<Integer> firedIndexes = fired.stream()
                    .map(name -> {
                        int index = attackNames.indexOf(name);
                        if (index < 0) {
                            throw new IllegalArgumentException(slug + ": no attack '" + name + "'");
                        }
                        return index;
                    })
                    .toList();
            if (phase.alternate().isPresent()) {
                for (int a : firedIndexes) {
                    if (attacks.get(a).pattern() == BossSpec.Pattern.SPIRAL
                            && !(attacks.get(a).durationSeconds() > 0)) {
                        throw new IllegalArgumentException(
                                slug + ": spiral " + attacks.get(a).name() + " alternates without a duration");
                    }
                }
            }
            // A later phase that alternates holds its fire a beat by default (the frigate's crown).
            double delay =
                    phase.delay().orElse(f > 0 && phase.alternate().isPresent() ? BossSpec.PHASE_DELAY_SECONDS : 0.0);
            phases.add(new BossSpec.Phase(
                    phase.name(),
                    phase.until().parts().orElse(List.of()).stream()
                            .map(part -> partIndex(enemy, part))
                            .toList(),
                    phase.until().left().orElse(0),
                    firedIndexes,
                    phase.alternate().isPresent(),
                    phase.streams()
                            .map(stream -> new BossSpec.Stream(
                                    enemy(content, stream.enemy(), difficulty, Optional.empty(), inLevel),
                                    stream.count(),
                                    stream.every(),
                                    stream.interval(),
                                    switch (stream.edge()) {
                                        case "left" -> WaveSpec.Edge.LEFT;
                                        case "right" -> WaveSpec.Edge.RIGHT;
                                        default -> WaveSpec.Edge.ALTERNATING;
                                    })),
                    phase.exposes().orElse(List.of()).stream()
                            .map(part -> partIndex(enemy, part))
                            .toList(),
                    phase.bend().map(Math::toRadians).orElse(Double.NaN),
                    phase.until().seconds().orElse(Double.POSITIVE_INFINITY),
                    delay,
                    Optional.empty(),
                    phase.windows()
                            .map(windows -> bossWindows(content, enemy, windows, spawnCounts, difficulty, inLevel))));
        }
        EnemyData.Movement movement = enemy.movement();
        EnemyData.Hover hover =
                movement.hover().orElseThrow(() -> new IllegalArgumentException(slug + ": a boss hovers"));
        // The moves need the station before them: walk the phases with the place, layer and pose so far.
        double stationX = placement.x();
        double stationY = PlayField.HEIGHT - hover.y().min();
        Layer stationLayer = Layers.of(enemy.layer());
        int stationPose = 0;
        for (int f = 0; f < phases.size(); f++) {
            Optional<EnemyData.MoveData> data = script.phases().get(f).move();
            if (data.isEmpty()) {
                continue;
            }
            EnemyData.MoveData move = data.get();
            if (move.to().isPresent()) {
                stationX = move.to().get().x();
                stationY = PlayField.HEIGHT - move.to().get().y();
            }
            stationLayer = move.layer().map(Layers::of).orElse(stationLayer);
            if (move.pose().isPresent()) {
                stationPose = script.poseIndex(move.pose().get());
                if (stationPose < 0) {
                    throw new IllegalArgumentException(
                            slug + ": no pose '" + move.pose().get() + "'");
                }
            }
            BossSpec.Phase plain = phases.get(f);
            phases.set(
                    f,
                    new BossSpec.Phase(
                            plain.name(),
                            plain.untilParts(),
                            plain.left(),
                            plain.attacks(),
                            plain.alternate(),
                            plain.stream(),
                            plain.exposes(),
                            plain.bendRadians(),
                            plain.seconds(),
                            plain.delaySeconds(),
                            Optional.of(new BossSpec.Move(
                                    stationX,
                                    stationY,
                                    stationLayer,
                                    move.descend().orElse(0.0),
                                    stationPose,
                                    move.turn().orElse(0.0))),
                            plain.windows()));
        }
        List<BossSpec.Pose> poses = new ArrayList<>();
        if (script.poses().isPresent()) {
            poses.add(new BossSpec.Pose(
                    "arrival",
                    hitbox(enemy.hitbox()),
                    partList.stream()
                            .map(part -> new BossSpec.Offset(
                                    part.offset().x(), part.offset().y()))
                            .toList()));
            for (EnemyData.PoseData pose : script.poses().get()) {
                for (String name : pose.offsets().keySet()) {
                    partIndex(enemy, name);
                }
                poses.add(new BossSpec.Pose(
                        pose.name(),
                        hitbox(pose.hitbox().orElse(enemy.hitbox())),
                        partList.stream()
                                .map(part -> {
                                    Point at = pose.offsets().getOrDefault(part.name(), part.offset());
                                    return new BossSpec.Offset(at.x(), at.y());
                                })
                                .toList()));
            }
        }
        return new LevelScript.SetPieceSpec(
                slug,
                hitbox(enemy.size()),
                hitbox(enemy.hitbox()),
                content.enemyBasis().contactDamage().get(enemy.tier()),
                parts,
                List.of(),
                drop(enemy).map(EnemySpec.Drop::pickup),
                Optional.of(new BossSpec(
                        placement.t(),
                        placement.x(),
                        PlayField.HEIGHT - hover.y().min(),
                        movement.straight().map(EnemyData.Straight::speed).orElse(enemy.speed()),
                        movement.sine().map(EnemyData.Sine::amplitude).orElse(0.0),
                        movement.sine().map(EnemyData.Sine::period).orElse(1.0),
                        Layers.of(enemy.layer()),
                        script.midBoss(),
                        script.barName(),
                        script.par(),
                        chains,
                        attacks,
                        phases,
                        script.engagesOnArrival().orElse(false),
                        armoured,
                        poses,
                        script.deathSeconds().orElse(0.0))));
    }

    /**
     * A boss phase's windows at {@code difficulty} (part G): the groups by part index, the spawns'
     * units at the difficulty and their counts after the hook's {@code spawns}.
     */
    private static BossSpec.Windows bossWindows(
            Content content,
            EnemyData enemy,
            EnemyData.WindowData windows,
            Map<String, Integer> spawnCounts,
            Difficulty difficulty,
            InLevel inLevel) {
        return new BossSpec.Windows(
                windows.groups().stream()
                        .map(group -> group.stream()
                                .map(part -> partIndex(enemy, part))
                                .toList())
                        .toList(),
                windows.every(),
                windows.open(),
                windows.offset().orElse(0.0),
                windows.all().orElse(false),
                windows.spawns().orElse(List.of()).stream()
                        .map(spawn -> new BossSpec.Spawn(
                                spawn.name(),
                                enemy(content, spawn.enemy(), difficulty, Optional.empty(), inLevel),
                                spawnCounts.getOrDefault(spawn.name(), spawn.count()),
                                spawn.speed(),
                                Math.toRadians(spawn.arc().orElse(0.0)),
                                spawn.glide().orElse(0.0)))
                        .toList());
    }

    private static int partIndex(EnemyData enemy, String part) {
        int index = enemy.partIndex(part);
        if (index < 0) {
            throw new IllegalArgumentException(enemy.name() + ": no part '" + part + "'");
        }
        return index;
    }

    /** The ground enemies of the level's ground targets at {@code difficulty}, each in its group of the secondary objective. */
    private static List<LevelScript.GroundUnit> groundUnits(
            Content content, LevelData level, Difficulty difficulty, LevelData.Secondary secondary, InLevel inLevel) {
        List<String> groups = level.objectives().groups();
        List<LevelScript.GroundUnit> units = new ArrayList<>();
        for (LevelData.GroundTarget target : level.groundTargets()) {
            if (target.enemy().isEmpty()) {
                continue;
            }
            EnemySpec enemy = enemy(content, target.enemy().get(), difficulty, Optional.empty(), inLevel);
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
            List<WaveSpec> out,
            InLevel inLevel) {
        Optional<LevelData.Change> change =
                switch (difficulty) {
                    case EASY -> wave.easy();
                    case MEDIUM -> Optional.empty();
                    case HARD -> wave.hard();
                };
        LevelData.Entry entry = change.flatMap(LevelData.Change::from).orElse(wave.from());
        Optional<LevelData.Edge> edge = change.flatMap(LevelData.Change::edge).or(wave::edge);
        Optional<Double> warning = change.flatMap(LevelData.Change::warning).or(wave::warning);
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
                    .filter(p -> p.droppedBy().waveT() == wave.t())
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
                    enemy(
                            content,
                            group.enemy(),
                            difficulty,
                            Optional.ofNullable(enemyChanges.get(group.enemy())),
                            inLevel),
                    count,
                    switch (entry) {
                        case FRONT -> WaveSpec.Entry.FRONT;
                        case SIDES -> WaveSpec.Entry.SIDES;
                        case REAR -> WaveSpec.Entry.REAR;
                    },
                    edge.map(SimSpecs::edge).orElse(WaveSpec.Edge.NONE),
                    change.flatMap(LevelData.Change::hold).or(wave::hold),
                    warning,
                    breakGroup,
                    wave.speed(),
                    wave.interval(),
                    pickups,
                    wave.at().map(at -> new WaveSpec.At(at.x(), at.y())),
                    wave.paths().orElse(List.of()).stream()
                            .map(path -> path.stream()
                                    .map(point -> new WaveSpec.At(point.x(), point.y()))
                                    .toList())
                            .toList(),
                    wave.loopBack()
                            .map(back -> new WaveSpec.LoopBack(
                                    back.after(),
                                    back.path().orElse(List.of()).stream()
                                            .map(point -> new WaveSpec.At(point.x(), point.y()))
                                            .toList())),
                    wave.tag().orElse("")));
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
            case "carrier + escorts" -> WaveSpec.Formation.CARRIER_ESCORTS;
            case "pack" -> WaveSpec.Formation.PACK;
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
            case SPECIAL_CHARGE -> PickupType.SPECIAL_CHARGE;
            case DATA_CORE -> PickupType.DATA_CORE;
            default -> throw new IllegalArgumentException("the pickup " + pickup + " is not implemented yet");
        };
    }

    /**
     * An enemy at {@code difficulty}: HP by the HP lever, its aimed attack (or mine) by the
     * fire-rate and bullet-speed levers, a level's burst change and the stat block's hooks; a
     * spiral-out, a convoy's strafe height and a death burst where the stat block has them; a
     * spawner's brood and a walker's walk, armour and spit (Level 04), a level's speed factor
     * applying to the walk.
     */
    public static EnemySpec enemy(
            Content content, String slug, Difficulty difficulty, Optional<LevelData.EnemyChange> change) {
        return enemy(content, slug, difficulty, change, InLevel.NOWHERE);
    }

    /**
     * As {@link #enemy(Content, String, Difficulty, Optional)} in Level {@code level} of Act {@code
     * act}: a returning unit of tier {@code medium} or larger gets the act HP factor there (see
     * {@link #actHpFactor}) on its HP and its chain's, rounded once with the HP lever.
     */
    public static EnemySpec enemy(
            Content content,
            String slug,
            Difficulty difficulty,
            Optional<LevelData.EnemyChange> change,
            int act,
            int level) {
        return enemy(content, slug, difficulty, change, new InLevel(act, level));
    }

    /**
     * The act HP factor of a unit in Level {@code level} of Act {@code act} (design/enemies,
     * Balancing basis; user decision D5 = c of M5 part A): from Act 2 on, a unit of tier {@code
     * medium} or larger that returns from an earlier level (its {@code first_level}) gets the
     * reference DPS at the level over that at its first level, so its time to kill stays; {@code
     * tiny} and {@code small} units, Act 1's levels and a unit's own first level get 1. Its bounty is
     * unchanged (the act factor scales the credits).
     */
    public static double actHpFactor(Content content, String slug, int act, int level) {
        return actHpFactor(content, content.enemy(slug), new InLevel(act, level));
    }

    private static double actHpFactor(Content content, EnemyData enemy, InLevel inLevel) {
        if (inLevel.act() < 2 || enemy.tier().compareTo(Tier.MEDIUM) < 0 || enemy.firstLevel() >= inLevel.level()) {
            return 1;
        }
        Map<Integer, Double> reference = content.enemyBasis().referenceDps();
        Double now = reference.get(inLevel.level());
        Double first = reference.get(enemy.firstLevel());
        if (now == null || first == null) {
            throw new IllegalArgumentException(enemy.name() + ": no reference DPS for Level " + inLevel.level()
                    + " or its first level " + enemy.firstLevel() + " (design/enemies/data.yaml)");
        }
        return now / first;
    }

    /** Where a unit flies, for the act HP factor: a level's act and number; {@link #NOWHERE} outside a level. */
    private record InLevel(int act, int level) {
        static final InLevel NOWHERE = new InLevel(1, 0);
    }

    private static EnemySpec enemy(
            Content content,
            String slug,
            Difficulty difficulty,
            Optional<LevelData.EnemyChange> change,
            InLevel inLevel) {
        EnemyData enemy = content.enemy(slug);
        double actHp = actHpFactor(content, enemy, inLevel);
        EnemyData.Movement movement = enemy.movement();
        Optional<EnemyData.Hook> hook = hook(enemy, difficulty);
        Optional<EnemySpec.Brood> brood = enemy.attacks().stream()
                .flatMap(attack -> attack.spawn().stream())
                .filter(spawn -> !spawn.periodic())
                .findFirst()
                .map(spawn -> new EnemySpec.Brood(
                        enemy(content, spawn.enemy(), difficulty, Optional.empty(), inLevel),
                        hook.flatMap(EnemyData.Hook::spawnCount).orElse(spawn.count()),
                        hook.flatMap(EnemyData.Hook::spawnAfter)
                                .orElse(spawn.after().orElseThrow()),
                        spawn.telegraph(),
                        Math.toRadians(spawn.arc()),
                        spawn.speed(),
                        spawn.burstBounty().orElseThrow()));
        // M5 part C: a periodic spawn (the Hive Node) with the hook's count.
        Optional<EnemySpec.Spawner> spawner = enemy.attacks().stream()
                .flatMap(attack -> attack.spawn().stream())
                .filter(EnemyData.Spawn::periodic)
                .findFirst()
                .map(spawn -> new EnemySpec.Spawner(
                        enemy(content, spawn.enemy(), difficulty, Optional.empty(), inLevel),
                        hook.flatMap(EnemyData.Hook::spawnCount).orElse(spawn.count()),
                        spawn.every().orElseThrow(),
                        spawn.telegraph(),
                        Math.toRadians(spawn.arc()),
                        spawn.speed(),
                        spawn.shutWithin().orElse(0.0)));
        // M5 part C: a pounce (the Ravager), its interval by the hook's authored one or the fire-rate lever.
        Optional<EnemySpec.Pounce> pounce = enemy.attacks().stream()
                .filter(attack -> attack.pounce().isPresent())
                .findFirst()
                .map(attack -> {
                    EnemyData.Pounce leap = attack.pounce().orElseThrow();
                    return new EnemySpec.Pounce(
                            leap.range(),
                            leap.leap(),
                            leap.air(),
                            leap.scale(),
                            interval(content, attack, hook, difficulty));
                });
        double speedFactor = change.flatMap(LevelData.EnemyChange::speedFactor).orElse(1.0);
        Optional<EnemySpec.Walker> walker = movement.walk()
                .map(walk -> new EnemySpec.Walker(
                        walk.speed() * speedFactor,
                        Math.toRadians(walk.turnRate()),
                        walk.stride(),
                        Math.toRadians(enemy.armour().frontArc().orElse(0.0)),
                        enemy.attacks().stream()
                                .filter(attack -> attack.away().isPresent())
                                .findFirst()
                                .map(attack -> gun(content, enemy, attack, difficulty, change)),
                        Math.toRadians(enemy.attacks().stream()
                                .flatMap(attack -> attack.away().stream())
                                .findFirst()
                                .orElse(0.0)),
                        // M5 part B: a walker's fan with aim target (the default) goes at the ship.
                        walkerFan(enemy)
                                .map(fan -> fan.aim().orElse("target").equals("target"))
                                .orElse(false),
                        walkerFan(enemy).flatMap(EnemyData.Attack::stagger).orElse(0.0)));
        // A segment chain's unit is its head: the head part's HP and bounty (the unit's include the body).
        Optional<EnemyData.PartData> head = enemy.segmentChain()
                .flatMap(chain -> enemy.partList().flatMap(parts -> parts.stream()
                        .filter(part -> part.kind().equals("vital"))
                        .findFirst()));
        EnemySpec spec = new EnemySpec(
                slug,
                content.difficulty().enemyHp(head.map(EnemyData.PartData::hp).orElse(enemy.hp()) * actHp, difficulty),
                hitbox(enemy.hitbox()),
                Layers.of(enemy.layer()),
                content.enemyBasis().contactDamage().get(enemy.tier()),
                enemy.tier().compareTo(Tier.SMALL) <= 0,
                head.map(EnemyData.PartData::bounty).orElse(enemy.bounty()),
                movement.drift()
                                .map(EnemyData.Drift::speed)
                                .or(() -> movement.path().map(EnemyData.PathMove::speed))
                                .orElse(enemy.speed())
                        * speedFactor,
                movement.snake().map(snake -> new EnemySpec.Snake(snake.spacing())),
                movement.straight().map(EnemyData.Straight::speed),
                movement.hover()
                        .map(hover -> new EnemySpec.Hover(
                                hover.seconds()
                                        .map(SimSpecs::range)
                                        .orElse(new Range(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY)),
                                range(hover.y()))),
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
                                bulletDamage(content, puff.bullet()))),
                movement.sine().map(sine -> new EnemySpec.Sine(sine.amplitude(), sine.period())),
                brood,
                walker,
                movement.hover()
                        .flatMap(EnemyData.Hover::edgeX)
                        .map(edgeX -> new EnemySpec.SideHover(
                                edgeX,
                                movement.hover().flatMap(EnemyData.Hover::exit).isPresent())),
                sweep(content, enemy, difficulty),
                Optional.empty(),
                enemy.armour().hardened(),
                spawner,
                pounce);
        return enemy.segmentChain().isPresent() ? withChain(content, enemy, spec, difficulty, actHp) : spec;
    }

    /** A walker's fan: its attack with the {@code fan} pattern. */
    private static Optional<EnemyData.Attack> walkerFan(EnemyData enemy) {
        return enemy.attacks().stream()
                .filter(attack -> attack.pattern().equals("fan"))
                .findFirst();
    }

    /**
     * An attack's interval at {@code difficulty}: the hook's authored {@code attacks.<name>.interval}
     * as it is (the fire-rate lever does not apply on top of it: the Mantis's beam, the Creeper's
     * hard fan), otherwise its interval by the fire-rate lever; infinite without one.
     */
    private static double interval(
            Content content, EnemyData.Attack attack, Optional<EnemyData.Hook> hook, Difficulty difficulty) {
        Optional<Double> authored = attack.name()
                .flatMap(name -> hook.flatMap(EnemyData.Hook::attacks).map(changes -> changes.get(name)))
                .flatMap(EnemyData.AttackChange::interval);
        return authored.orElseGet(() -> attack.interval()
                .map(i -> i / content.difficulty().enemyFireRate().of(difficulty))
                .orElse(Double.POSITIVE_INFINITY));
    }

    /**
     * A laser sweep at {@code difficulty} (design/enemies/air/mantis): the stat block's sweep with
     * the hooks' arc and interval (an authored interval is final; otherwise the fire-rate lever
     * applies), the beam's bullet class as its damage and its origin at the eye.
     */
    private static Optional<EnemySpec.Sweep> sweep(Content content, EnemyData enemy, Difficulty difficulty) {
        Optional<EnemyData.Hook> hook = hook(enemy, difficulty);
        return enemy.attacks().stream()
                .filter(attack -> attack.pattern().equals("laser-sweep"))
                .findFirst()
                .map(attack -> {
                    EnemyData.Sweep sweep = attack.sweep().orElseThrow();
                    double interval = interval(content, attack, hook, difficulty);
                    return new EnemySpec.Sweep(
                            Math.toRadians(
                                    hook.flatMap(EnemyData.Hook::sweepArc).orElse(sweep.arc())),
                            sweep.duration(),
                            sweep.telegraph(),
                            sweep.length(),
                            sweep.width(),
                            interval,
                            attack.firstShotDelay().orElse(0.0),
                            bulletDamage(content, attack.bullet().orElseThrow()),
                            sweep.origin().map(Point::x).orElse(0.0),
                            sweep.origin().map(Point::y).orElse(0.0));
                });
    }

    /**
     * A segment chain's head with its chain (design/enemies/air/coilwyrm): the segments taper
     * evenly from the first to the last size, with hit boxes their share of it; the members follow
     * one another their spacing × their mean length apart along the head's path (the head as long
     * as its sprite, the tail as its hit box over the share); the tail and the regrown head from the
     * part list and the regrow block, the regrown head firing the head's fan (the hook's count).
     */
    private static EnemySpec withChain(
            Content content, EnemyData enemy, EnemySpec head, Difficulty difficulty, double actHp) {
        EnemyData.SegmentChain chain = enemy.segmentChain().orElseThrow();
        Optional<EnemyData.Hook> hook = hook(enemy, difficulty);
        int segments = hook.flatMap(EnemyData.Hook::segments).orElse(chain.segments());
        EnemyData.PartData tail = enemy.partList().orElseThrow().stream()
                .filter(part -> !part.kind().equals("vital"))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(enemy.name() + ": a chain has a tail part"));
        double small = content.enemyBasis().contactDamage().get(chain.contact());
        boolean rammed = chain.contact().compareTo(Tier.SMALL) <= 0;
        List<Hitbox> boxes = new ArrayList<>();
        List<Double> lengths = new ArrayList<>();
        lengths.add(enemy.size().height());
        for (int i = 0; i < segments; i++) {
            double t = segments == 1 ? 0 : (double) i / (segments - 1);
            double size =
                    chain.size().get(0) + (chain.size().get(1) - chain.size().get(0)) * t;
            boxes.add(new Hitbox(size * chain.hitboxShare(), size * chain.hitboxShare()));
            lengths.add(size);
        }
        lengths.add(tail.hitbox().height() / chain.hitboxShare());
        List<Double> offsets = new ArrayList<>();
        offsets.add(0.0);
        for (int i = 1; i < lengths.size(); i++) {
            offsets.add(offsets.get(i - 1) + chain.spacing() * (lengths.get(i - 1) + lengths.get(i)) / 2);
        }
        String slug = head.slug();
        EnemySpec segment = part(
                content,
                slug + "-segment",
                head,
                chain.hp() * actHp,
                boxes.getFirst(),
                small,
                rammed,
                chain.bounty(),
                Optional.empty(),
                head.speed(),
                difficulty);
        EnemySpec tailSpec = part(
                content,
                slug + "-tail",
                head,
                tail.hp() * actHp,
                hitbox(tail.hitbox()),
                small,
                rammed,
                tail.bounty(),
                Optional.empty(),
                head.speed(),
                difficulty);
        Optional<EnemyGun> fan = head.gun().map(gun -> hook.flatMap(EnemyData.Hook::regrownFanCount)
                .map(count -> new EnemyGun(
                        gun.intervalSeconds(),
                        gun.firstShotDelay(),
                        gun.burst(),
                        gun.bulletSpeed(),
                        gun.damage(),
                        gun.leadsTargetInCircle(),
                        count,
                        gun.spreadRadians(),
                        gun.turnRate(),
                        gun.arcRadians(),
                        gun.mine(),
                        gun.mortar()))
                .orElse(gun));
        EnemySpec regrown = part(
                content,
                slug + "-regrown",
                head,
                chain.regrow().hp() * actHp,
                head.hitbox(),
                head.contactDamage(),
                false,
                chain.regrow().bounty(),
                fan,
                chain.regrow().speed(),
                difficulty);
        EnemySpec.ChainSpec spec = new EnemySpec.ChainSpec(
                segment,
                boxes,
                tailSpec,
                tail.firstBonus().orElse(0),
                regrown,
                chain.regrow().seconds(),
                chain.regrow().speed(),
                offsets,
                chain.popInterval(),
                enemy.partList().orElseThrow().stream()
                        .filter(part -> part.kind().equals("vital"))
                        .findFirst()
                        .flatMap(EnemyData.PartData::multiplier)
                        .orElse(1.0));
        return new EnemySpec(
                head.slug(),
                head.hp(),
                head.hitbox(),
                head.layer(),
                head.contactDamage(),
                head.destroyedByRamming(),
                head.bounty(),
                head.speed(),
                head.snake(),
                head.streamSpeed(),
                head.hover(),
                head.orbit(),
                head.gun(),
                head.drop(),
                head.dive(),
                head.terrain(),
                head.spiral(),
                head.strafe(),
                head.deathBurst(),
                head.sine(),
                head.brood(),
                head.walker(),
                head.sideHover(),
                head.sweep(),
                Optional.of(spec),
                head.hardened(),
                head.spawner(),
                head.pounce());
    }

    /** A chain's part as a unit of its own: its HP by the HP lever, on the head's layer. */
    private static EnemySpec part(
            Content content,
            String slug,
            EnemySpec head,
            double hp,
            Hitbox box,
            double contactDamage,
            boolean rammed,
            int bounty,
            Optional<EnemyGun> gun,
            double speed,
            Difficulty difficulty) {
        return new EnemySpec(
                slug,
                content.difficulty().enemyHp(hp, difficulty),
                box,
                head.layer(),
                contactDamage,
                rammed,
                bounty,
                speed,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                gun,
                Optional.empty(),
                Optional.empty(),
                false);
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
     * An enemy's attack at {@code difficulty}: its interval by the fire-rate lever (or the hook's
     * authored interval, M5 part B), its bullets by the bullet-speed lever, a level's burst change or the stat block's hooks (burst, fan size,
     * leading the target in circles, a mine's ring and whether it bursts).
     */
    private static Optional<EnemyGun> gun(
            Content content, EnemyData enemy, Difficulty difficulty, Optional<LevelData.EnemyChange> change) {
        // A spawner's spawn is its brood, a walker's spit (aimed while facing away) its second attack.
        List<EnemyData.Attack> attacks = enemy.attacks().stream()
                .filter(attack -> !attack.pattern().equals("spawn"))
                .filter(attack -> !attack.pattern().equals("laser-sweep"))
                .filter(attack -> !attack.pattern().equals("pounce"))
                .filter(attack ->
                        attack.away().isEmpty() || enemy.movement().walk().isEmpty())
                .toList();
        if (attacks.isEmpty()) {
            return Optional.empty();
        }
        if (attacks.size() > 1) {
            throw new IllegalArgumentException(enemy.name() + ": only a single attack is implemented");
        }
        return Optional.of(gun(content, enemy, attacks.getFirst(), difficulty, change));
    }

    private static EnemyGun gun(
            Content content,
            EnemyData enemy,
            EnemyData.Attack attack,
            Difficulty difficulty,
            Optional<LevelData.EnemyChange> change) {
        DifficultyData levers = content.difficulty();
        double damage = bulletDamage(content, attack.bullet().orElseThrow());
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
        Optional<EnemyGun.MortarSpec> mortar = attack.mortar()
                .map(lob -> new EnemyGun.MortarSpec(
                        lob.marker(),
                        lob.impact() / 2,
                        hook.flatMap(EnemyData.Hook::ring).orElse(lob.ring()),
                        bulletDamage(content, lob.ringBullet())));
        return new EnemyGun(
                interval(content, attack, hook, difficulty),
                attack.firstShotDelay().orElse(0.0),
                burst,
                attack.speed().orElseThrow() * levers.enemyBulletSpeed().of(difficulty),
                damage,
                hook.flatMap(EnemyData.Hook::leadsTargetIn)
                        .map(in -> in.contains("circle"))
                        .orElse(false),
                fan,
                Math.toRadians(attack.spread().orElse(0.0)),
                attack.turnRate().map(Math::toRadians).orElse(Double.POSITIVE_INFINITY),
                attack.arc().map(Math::toRadians).orElse(Double.POSITIVE_INFINITY),
                mine,
                mortar);
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
                        target.reveals().map(triggersPerSecret::get).orElse(1),
                        target.bonusDrop().map(SimSpecs::pickup),
                        target.sprite().orElse(LevelScript.GroundObjectSpec.CARGO_CONTAINER),
                        target.dark().orElse(false),
                        secret.flatMap(sec ->
                                sec.dataCore().map(core -> new LevelResult.DataCore(sec.name(), core.unlocks())))));
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
                        case FIRST_ALLY_HIT -> LevelScript.CueTrigger.FIRST_ALLY_HIT;
                        case FIRST_ALLY_LOST -> LevelScript.CueTrigger.FIRST_ALLY_LOST;
                        case ESCORT_FIRST_KILL -> LevelScript.CueTrigger.ESCORT_FIRST_KILL;
                        case MISSION_FAILED -> LevelScript.CueTrigger.MISSION_FAILED;
                        case BOSS_PHASE -> LevelScript.CueTrigger.BOSS_PHASE;
                        case BOSS_DESTROYED -> LevelScript.CueTrigger.BOSS_DESTROYED;
                        case HOLD_START -> LevelScript.CueTrigger.HOLD_START;
                        case FIRST_POUNCE -> LevelScript.CueTrigger.FIRST_POUNCE;
                        case COLLAPSE -> LevelScript.CueTrigger.COLLAPSE;
                    })
                    .orElse(LevelScript.CueTrigger.TIME);
            // Part G (LevelRules): a boss-destroyed cue's subject is the boss, a timeout cue its own
            // trigger, and the requirements bits (a special, a homing weapon; or none of them).
            cues.add(new LevelScript.RadioCue(
                    LevelRules.trigger(cue, trigger),
                    cue.t().orElse(0.0),
                    LevelRules.subject(level, cue),
                    cue.speaker(),
                    change.map(LevelData.RadioChange::line).orElse(cue.line()),
                    cue.distorted().orElse(false),
                    cue.expression().orElse(Expression.NEUTRAL).slug(),
                    cue.portrait().orElse(cue.speaker()),
                    false,
                    cue.allies().map(LevelData.Count::min).orElse(0),
                    cue.allies().map(LevelData.Count::max).orElse(Integer.MAX_VALUE),
                    LevelRules.requirement(cue.requires()),
                    LevelRules.requirement(cue.requiresNot())));
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
