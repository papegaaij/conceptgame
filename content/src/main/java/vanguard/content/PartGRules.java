package vanguard.content;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Function;
import vanguard.sim.AllySpec;
import vanguard.sim.EnemySpec;
import vanguard.sim.Hitbox;
import vanguard.sim.LevelScript;
import vanguard.sim.PickupType;
import vanguard.sim.WaveSpec;

/**
 * M5 part G's rules for the simulation (design/campaign Level 13 Polar Relay, design/enemies/naval/skimmer,
 * design/allies Nansen Relay; user decisions G1–G9 = a of 2026-10-11 and the stated defaults, pending the
 * user's review in the combined round): the {@code defend} primary with its ally and the hook modes per
 * enemy kind at a difficulty, a timed halt's sections, the lane walkers' lanes, a {@code cross}'s entry,
 * hard's pairs, a nest entering from a side, the pickups released at a point (the supply drones) and the
 * {@code ally-below} cues' share. Javadoc "M5 part G": M4's part G is {@link LevelRules}.
 */
final class PartGRules {
    private PartGRules() {}

    /** The level's sections for the simulation: a timed halt with its {@code ramp} (the default 2 s if not given). */
    static List<LevelScript.Section> sections(LevelData level) {
        return level.sections().stream()
                .map(section -> new LevelScript.Section(
                        section.end(),
                        section.speed().orElse(level.scrollSpeed()),
                        section.isArena(),
                        section.rampSeconds()))
                .toList();
    }

    /**
     * The defend primary at {@code difficulty}: its ally (an ally that {@code follows: ground}, its
     * integrity the difficulty's {@code hp}), where it stands, its start and duration, and the hook mode
     * per enemy slug (the difficulty's merged over {@code targeted_by}; {@code none} left out).
     */
    static LevelScript.Defend defend(Content content, LevelData.Defend defend, Difficulty difficulty) {
        Map<String, LevelScript.HookMode> modes = new TreeMap<>();
        defend.modes(difficulty).forEach((slug, mode) -> {
            if (mode != LevelData.HookMode.NONE) {
                modes.put(slug, mode(mode));
            }
        });
        return new LevelScript.Defend(
                ally(content, defend.ally(), defend.change(difficulty).flatMap(LevelData.DefendChange::hp)),
                defend.at().x(),
                defend.at().y(),
                defend.from(),
                defend.duration(),
                modes);
    }

    /** A defend primary's ally (design/allies/data.yaml, {@code follows: ground}) with {@code hp} instead of its own. */
    static AllySpec ally(Content content, String slug, Optional<Double> hp) {
        AlliesData.Ally ally = content.allies().allies().get(slug);
        if (ally == null) {
            throw new IllegalArgumentException("no ally '" + slug + "'");
        }
        if (!ally.standing()) {
            throw new IllegalArgumentException("a defended ally follows the ground: '" + slug + "' does not");
        }
        return new AllySpec(
                slug,
                hitbox(ally.size()),
                hitbox(ally.hitbox()),
                hp.orElse(ally.hp()),
                ally.damagedBy().objectiveAimed(),
                0,
                ally.smokeBelow(),
                0,
                false,
                false,
                0,
                0,
                0,
                0,
                0,
                ally.damagedBy().diveDamage(),
                ally.damagedBy().hitByLobs());
    }

    /** The simulation's hook mode for the data's. */
    static LevelScript.HookMode mode(LevelData.HookMode mode) {
        return switch (mode) {
            case NONE -> LevelScript.HookMode.NONE;
            case NEAREST -> LevelScript.HookMode.NEAREST;
            case ALTERNATE -> LevelScript.HookMode.ALTERNATE;
            case IN_ARC -> LevelScript.HookMode.IN_ARC;
            case ALWAYS -> LevelScript.HookMode.ALWAYS;
        };
    }

    /**
     * {@code spec} with the wave's lanes (lane walkers, the Skimmer) and its difficulty's pairs (a column
     * entering in pairs, hard's Stingers); as it is without either.
     */
    static WaveSpec lanesAndPairs(LevelData.Wave wave, Optional<LevelData.Change> change, WaveSpec spec) {
        WaveSpec out = spec;
        if (wave.lanes().isPresent()) {
            out = out.withLanes(wave.lanes().get().stream()
                    .map(lane -> lane.stream()
                            .map(point -> new WaveSpec.At(point.x(), point.y()))
                            .toList())
                    .toList());
        }
        Optional<Double> pairs = change.flatMap(LevelData.Change::pairs);
        if (pairs.isPresent()) {
            out = out.withPairs(pairs.get());
        }
        return out;
    }

    /** The simulation's entry for the data's: a {@code cross} wave's ({@code all}) enters by its lanes, given as the front. */
    static WaveSpec.Entry entry(LevelData.Entry entry) {
        return switch (entry) {
            case FRONT, ALL -> WaveSpec.Entry.FRONT;
            case SIDES -> WaveSpec.Entry.SIDES;
            case REAR -> WaveSpec.Entry.REAR;
        };
    }

    /** A unit of a nest entering at a side edge: its placement's {@code [t, y]}, y up, drifting across. */
    static LevelScript.GroundUnit fromSide(
            LevelData.Placement placement, LevelData.SideEntry side, EnemySpec enemy, int group) {
        return LevelScript.GroundUnit.fromSide(
                placement.t(),
                enemy,
                group,
                new LevelScript.SideEntry(side.from() == LevelData.WindSide.LEFT, placement.x(), side.drift()));
    }

    /** The pickups released at a point (the data's {@code drops}) beside the ones placed at a time and x, in time order. */
    static List<LevelScript.PlacedPickup> placedPickups(
            List<LevelScript.PlacedPickup> placed,
            List<LevelData.Drop> drops,
            Function<Pickup, PickupType> pickupType) {
        return java.util.stream.Stream.concat(
                        placed.stream(),
                        drops.stream()
                                .map(drop -> new LevelScript.PlacedPickup(
                                        drop.t(), drop.x(), drop.y(), pickupType.apply(drop.pickup()))))
                .sorted(Comparator.comparingDouble(LevelScript.PlacedPickup::t))
                .toList();
    }

    /** An {@code ally-below} cue's subject: its share as the decimal the simulation reads ("0.5"). */
    static String share(double below) {
        return Double.toString(below);
    }

    private static Hitbox hitbox(Size size) {
        return new Hitbox(size.width(), size.height());
    }
}
