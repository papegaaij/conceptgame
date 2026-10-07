package vanguard.content;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import vanguard.sim.LevelScript;

/**
 * M5 part C's level rules for the simulation (design/campaign, Level 09 Arcology Fall; user
 * decisions D2, D3 and D5 = a), used by {@link SimSpecs#level}: the hold zones at a difficulty and
 * the collapse, their groups as indexes into the objectives' groups.
 */
final class PartCRules {
    private PartCRules() {}

    /** The level's hold zones at {@code difficulty}: each one's groups, depth, speed and ease. */
    static List<LevelScript.Hold> holds(LevelData level, Difficulty difficulty) {
        List<LevelScript.Hold> holds = new ArrayList<>();
        for (LevelData.Hold hold : level.holds().orElse(List.of())) {
            holds.add(new LevelScript.Hold(
                    groups(level, hold.groups(), "holds"), hold.y(), hold.speedOn(difficulty), hold.ramp()));
        }
        return holds;
    }

    /**
     * The level's collapse, its band on the ground under its tower ({@link LevelData#collapseBand});
     * the dust's seconds time the hold over its groups (it lasts until the dust settles), the rest of
     * the dust and the rubble are presentation only.
     */
    static Optional<LevelScript.Collapse> collapse(LevelData level) {
        return level.collapse().map(collapse -> {
            LevelData.Band band = level.collapseBand().orElseThrow();
            return new LevelScript.Collapse(
                    groups(level, collapse.groups(), "collapse"),
                    collapse.warning(),
                    collapse.drop(),
                    collapse.blast().seconds(),
                    collapse.blast().from(),
                    collapse.blast().to(),
                    collapse.dust().seconds(),
                    band.x(),
                    band.bottom(),
                    band.top());
        });
    }

    private static List<Integer> groups(LevelData level, List<String> names, String field) {
        List<String> groups = level.objectives().groups();
        List<Integer> indexes = new ArrayList<>();
        for (String name : names) {
            int index = groups.indexOf(name);
            if (index < 0) {
                throw new IllegalArgumentException(field + ": no group '" + name + "' in the objectives' groups");
            }
            indexes.add(index);
        }
        return indexes;
    }
}
