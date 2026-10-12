package vanguard.content;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import vanguard.sim.EnemySpec;
import vanguard.sim.LevelScript;
import vanguard.sim.PickupType;

/**
 * M5 part F's rules for the simulation (design/campaign Level 12 Storm Front, design/enemies/air/lamprey;
 * user decisions F1–F9 of 2026-10-09 and the stated defaults): a chaser's {@code chase} and its {@code
 * latch} with the {@code drain} attack, the weapons whose shots hit a latched Lamprey (F3 = a), the
 * level's {@code weather} at a difficulty, the weather sondes and the pickups placed at a time and x.
 */
final class PartFRules {
    /** The Lamprey's attack while latched: shield points a second, no bullet. */
    static final String DRAIN = "drain";

    private PartFRules() {}

    /**
     * {@code spec} with the stat block's {@code chase} and (with it) its {@code latch}: the hull spots
     * (y up), the shake, the stun and throw, the drain attack's rate; the hook's {@code drain} and {@code
     * latch_slots} instead on its difficulty. A unit without a chase is returned as it is.
     */
    static EnemySpec chaser(EnemyData enemy, Optional<EnemyData.Hook> hook, EnemySpec spec) {
        Optional<EnemyData.Chase> chase = enemy.movement().chase();
        if (chase.isEmpty()) {
            return spec;
        }
        EnemyData.Chase homing = chase.get();
        Optional<EnemySpec.Latch> latch = enemy.latch().map(data -> {
            double rate = hook.flatMap(EnemyData.Hook::drain).orElseGet(() -> drainRate(enemy));
            EnemyData.Spots spots = data.spots();
            return new EnemySpec.Latch(
                    hook.flatMap(EnemyData.Hook::latchSlots).orElse(data.slots()),
                    List.of(spot(spots.left()), spot(spots.right()), spot(spots.tail())),
                    data.damageFactor(),
                    data.shake().reversals(),
                    data.shake().within(),
                    data.stun(),
                    data.throwSpeed(),
                    rate);
        });
        return spec.withChase(
                new EnemySpec.Chase(homing.speed(), Math.toRadians(homing.turnRate()), homing.seconds()), latch);
    }

    /** The rate of a unit's {@code drain} attack, shield points a second; 0 without one. */
    static double drainRate(EnemyData enemy) {
        return enemy.attacks().stream()
                .filter(attack -> attack.pattern().equals(DRAIN))
                .findFirst()
                .flatMap(EnemyData.Attack::rate)
                .orElse(0.0);
    }

    private static EnemySpec.Spot spot(Point offset) {
        return new EnemySpec.Spot(offset.x(), offset.y());
    }

    /** Whether a weapon's shots hit a latched Lamprey: it has the {@code side} or the {@code rear} trait (F3 = a). */
    static boolean flanking(WeaponData weapon) {
        return weapon.traits().contains("side") || weapon.traits().contains("rear");
    }

    /**
     * The level's weather at {@code difficulty}: the gusts (hard's extra ones merged in by time) with the
     * difficulty's push and telegraph, the drift through its section with the difficulty's push (left out
     * at 0), the lightning's times. None when the block has neither wind nor lightning (rain and occlusion
     * alone are presentation).
     */
    static Optional<LevelScript.Weather> weather(LevelData level, LevelData.Weather weather, Difficulty difficulty) {
        if (weather.gusts().isEmpty()
                && weather.drift().isEmpty()
                && weather.lightning().isEmpty()) {
            return Optional.empty();
        }
        List<LevelScript.Gust> gusts = new ArrayList<>();
        double seconds = 1;
        double ramp = 0;
        double push = 0;
        double telegraph = 0;
        if (weather.gusts().isPresent()) {
            LevelData.Gusts data = weather.gusts().get();
            Optional<LevelData.GustChange> change =
                    switch (difficulty) {
                        case EASY -> data.easy();
                        case MEDIUM -> Optional.empty();
                        case HARD -> data.hard();
                    };
            List<LevelData.GustAt> at = new ArrayList<>(data.at());
            change.flatMap(LevelData.GustChange::extra).ifPresent(at::addAll);
            at.sort(Comparator.comparingDouble(LevelData.GustAt::t));
            for (LevelData.GustAt gust : at) {
                gusts.add(new LevelScript.Gust(gust.t(), gust.from() == LevelData.WindSide.LEFT));
            }
            seconds = data.seconds();
            ramp = data.ramp();
            push = change.flatMap(LevelData.GustChange::push).orElse(data.push());
            telegraph = change.flatMap(LevelData.GustChange::telegraph).orElse(data.telegraph());
        }
        List<LevelScript.Drift> drifts = new ArrayList<>();
        weather.drift().ifPresent(drift -> {
            Optional<LevelData.DriftChange> change =
                    switch (difficulty) {
                        case EASY -> drift.easy();
                        case MEDIUM -> Optional.empty();
                        case HARD -> drift.hard();
                    };
            double drifting = change.map(LevelData.DriftChange::push).orElse(drift.push());
            if (drifting > 0) {
                int index = drift.section() - 1;
                drifts.add(new LevelScript.Drift(
                        level.sectionStart(index),
                        level.sections().get(index).end(),
                        drifting,
                        drift.from() == LevelData.WindSide.LEFT));
            }
        });
        List<Double> flashes = weather.lightning().map(LevelData.Lightning::at).orElse(List.of());
        return Optional.of(new LevelScript.Weather(gusts, seconds, ramp, push, telegraph, drifts, flashes));
    }

    /** The level's weather sondes for the simulation. */
    static LevelScript.Sondes sondes(LevelData.SondesData sondes) {
        return new LevelScript.Sondes(
                sondes.speed(),
                sondes.radius(),
                sondes.at().stream()
                        .map(at -> new LevelScript.Sonde(at.t(), at.x()))
                        .toList());
    }

    /** The pickups placed at a time and x (not a carrier's), in time order. */
    static List<LevelScript.PlacedPickup> placedPickups(
            List<LevelData.PlacedPickup> placed, Function<Pickup, PickupType> pickupType) {
        return placed.stream()
                .map(p -> new LevelScript.PlacedPickup(
                        p.at().orElseThrow().t(), p.at().orElseThrow().x(), pickupType.apply(p.pickup())))
                .sorted(Comparator.comparingDouble(LevelScript.PlacedPickup::t))
                .toList();
    }
}
