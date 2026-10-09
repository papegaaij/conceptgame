package vanguard.content;

import java.util.List;
import java.util.Optional;
import vanguard.sim.AllySpec;
import vanguard.sim.Hitbox;
import vanguard.sim.LevelScript;
import vanguard.sim.PlayField;

/**
 * M5 part E's level rules for the simulation (design/campaign Level 11; user decision E8 = a and the
 * stated defaults of 2026-10-08): the naval convoy outside the objectives ({@link LevelData.Convoy})
 * and the lanes it glides to, which are the level boss's ({@link EnemyData.Lanes}).
 */
final class PartERules {
    private PartERules() {}

    /**
     * The naval convoy at {@code difficulty}: each unit's ally with the level's HP for the units that
     * can be damaged, its name, its station (y up), its lane (0 for a unit that leaves) and its hold
     * point (y up; NaN without one); the glide, the lanes' height (y up), the level boss's lanes (none
     * without them) and the scroll the units hold clear for after the halt.
     */
    static LevelScript.Naval convoy(Content content, LevelData level, LevelData.Convoy convoy, Difficulty difficulty) {
        Optional<LevelData.AllyChange> change =
                switch (difficulty) {
                    case EASY -> convoy.easy();
                    case MEDIUM -> Optional.empty();
                    case HARD -> convoy.hard();
                };
        List<LevelScript.NavalUnit> units = convoy.units().stream()
                .map(unit -> new LevelScript.NavalUnit(
                        ally(content, unit.ally(), change),
                        unit.name(),
                        unit.station().x(),
                        PlayField.HEIGHT - unit.station().y(),
                        unit.lane().orElse(0),
                        unit.hold().map(Point::x).orElse(Double.NaN),
                        unit.hold().map(hold -> PlayField.HEIGHT - hold.y()).orElse(Double.NaN)))
                .toList();
        Optional<EnemyData.Lanes> lanes = lanes(content, level);
        return new LevelScript.Naval(
                units,
                convoy.glide(),
                PlayField.HEIGHT - convoy.laneY().orElse(PlayField.HEIGHT / 2.0),
                lanes.map(EnemyData.Lanes::count).orElse(0),
                lanes.map(EnemyData.Lanes::width).orElse(0.0),
                convoy.holdClear().orElse(0.0));
    }

    /** The level boss's slam lanes; none without a boss or without lanes. */
    static Optional<EnemyData.Lanes> lanes(Content content, LevelData level) {
        return level.boss()
                .map(LevelData.BossPlacement::enemy)
                .filter(content.enemies()::containsKey)
                .flatMap(slug -> content.enemy(slug).boss())
                .flatMap(EnemyData.BossData::lanes);
    }

    /**
     * A naval convoy's ally (design/allies/data.yaml, {@code follows: stations}): hurt only by slams
     * ({@code damaged_by.slams}, its HP counting them; {@code change}'s HP on easy or hard for a unit
     * that can be damaged), its flak a presentation timer.
     */
    static AllySpec ally(Content content, String slug, Optional<LevelData.AllyChange> change) {
        AlliesData.Ally ally = content.allies().allies().get(slug);
        if (ally == null) {
            throw new IllegalArgumentException("no ally '" + slug + "'");
        }
        double slams = ally.damagedBy().hitsPerSlam();
        return new AllySpec(
                slug,
                hitbox(ally.size()),
                hitbox(ally.hitbox()),
                slams > 0 ? change.map(LevelData.AllyChange::hp).orElse(ally.hp()) : ally.hp(),
                false,
                0,
                ally.smokeBelow(),
                (ally.headings().count() - 1) / 2 * ally.headings().step(),
                false,
                false,
                0,
                0,
                0,
                slams,
                ally.flak().map(AlliesData.Flak::every).orElse(0.0));
    }

    private static Hitbox hitbox(Size size) {
        return new Hitbox(size.width(), size.height());
    }
}
