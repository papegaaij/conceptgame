package vanguard.content.campaign;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.IntFunction;
import vanguard.content.ActData;
import vanguard.content.Content;

/**
 * The mission select's list (design/ui/mission-select): the campaign's levels act by act, with the
 * levels already flown open for a replay and showing their best grade, and every later level
 * locked with its name hidden; an act the campaign has not reached hides its name too.
 */
public final class Missions {
    /** Shown in place of a hidden name. */
    public static final String HIDDEN = "???";

    /**
     * An act of the list.
     *
     * @param label the act's number label, "ACT I"
     * @param name the act's name, empty while the campaign has not reached the act
     */
    public record Act(String label, Optional<String> name, List<Mission> missions) {
        public Act {
            missions = List.copyOf(missions);
        }
    }

    /**
     * A level of the list.
     *
     * @param name the level's name, empty while it is locked
     * @param grade the best grade, if the level was completed
     * @param open whether it can be replayed: flown before and built
     */
    public record Mission(int number, Optional<String> name, Optional<String> grade, boolean open) {}

    /**
     * What the list is made of for one act, independent of the loaded content (for tests).
     *
     * @param label "ACT I"
     * @param name "FIRST CONTACT"
     */
    record ActInfo(String label, String name, int first, int last) {}

    private Missions() {}

    /** The list for a save: every act of the content, in order. */
    public static List<Act> of(Content content, SaveGame save) {
        List<ActInfo> acts = content.acts().values().stream()
                .sorted(Comparator.comparingInt(act -> act.levels().first()))
                .map(Missions::info)
                .toList();
        return of(acts, number -> content.levelKey(number).map(Missions::displayName), save);
    }

    private static ActInfo info(ActData act) {
        return new ActInfo(
                act.titleCard().act(),
                act.titleCard().name(),
                act.levels().first(),
                act.levels().last());
    }

    /** A level key's name as the list shows it: "BREAK AT DAWN". */
    static String displayName(String key) {
        return Content.levelName(key).toUpperCase(Locale.ROOT);
    }

    /**
     * The list for a save.
     *
     * @param levelName a level's name, empty while its data file does not exist yet
     */
    static List<Act> of(List<ActInfo> acts, IntFunction<Optional<String>> levelName, SaveGame save) {
        List<Act> list = new ArrayList<>();
        for (ActInfo act : acts) {
            List<Mission> missions = new ArrayList<>();
            for (int number = act.first(); number <= act.last(); number++) {
                Optional<String> name = levelName.apply(number);
                boolean open = played(save, number) && name.isPresent();
                missions.add(new Mission(
                        number,
                        open ? name : Optional.empty(),
                        Optional.ofNullable(save.grades().get(number)),
                        open));
            }
            boolean reached = act.first() <= save.nextLevel();
            list.add(new Act(act.label(), reached ? Optional.of(act.name()) : Optional.empty(), missions));
        }
        return list;
    }

    /** Whether the campaign has flown the level: only levels before its next one can be replayed. */
    public static boolean played(SaveGame save, int level) {
        return level >= 1 && level < save.nextLevel();
    }

    /**
     * The save with a replay's grade recorded when it beats the level's best (or is the first);
     * everything else, its write time included, stays as it was.
     *
     * @param order the grades from best to worst
     */
    public static SaveGame withGrade(SaveGame save, int level, String grade, List<String> order) {
        String best = save.grades().get(level);
        if (best != null && order.indexOf(grade) >= order.indexOf(best)) {
            return save;
        }
        Map<Integer, String> grades = new HashMap<>(save.grades());
        grades.put(level, grade);
        return new SaveGame(
                save.version(),
                save.created(),
                save.playtime(),
                save.difficulty(),
                save.nextLevel(),
                save.credits(),
                save.score(),
                save.loadout(),
                save.inventory(),
                save.unlocks(),
                save.specials(),
                save.armour(),
                save.escort(),
                save.retriesLeft(),
                grades,
                save.dataCores(),
                save.storyFlags(),
                save.stats());
    }
}
