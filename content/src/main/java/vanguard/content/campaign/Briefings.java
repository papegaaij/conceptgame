package vanguard.content.campaign;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import vanguard.content.ActData;
import vanguard.content.BriefingPage;
import vanguard.content.Content;
import vanguard.content.EnemyData;
import vanguard.content.LevelData;

/**
 * Builds the briefing before a level from the design data: a level that opens its act gets the
 * act's title card and briefing first (design/campaign, Act intro and outro), so the intro
 * briefing of a new game is the one before Level 01; and the act's outro after its last level.
 */
public final class Briefings {
    private Briefings() {}

    /** The briefing before level {@code number}, if the level and its act exist in the data yet. */
    public static Optional<BriefingScript> before(Content content, int number) {
        Optional<String> key = content.levelKey(number);
        Optional<Map.Entry<String, ActData>> act = content.actOf(number);
        if (key.isEmpty() || act.isEmpty()) {
            return Optional.empty();
        }
        ActData actData = act.get().getValue();
        LevelData level = content.level(key.get());
        boolean opensAct = actData.levels().first() == number;
        List<BriefingPage> pages = new ArrayList<>();
        if (opensAct) {
            pages.addAll(actData.briefing());
        }
        pages.addAll(level.briefing().pages());
        ActData.TitleCard card = actData.titleCard();
        return Optional.of(new BriefingScript(
                opensAct ? Optional.of(card) : Optional.empty(),
                act.get().getKey(),
                card.act() + " - " + card.name(),
                number,
                Content.levelName(key.get()).toUpperCase(Locale.ROOT),
                pages,
                objectives(content, level),
                level.briefing().teaser()));
    }

    /**
     * The act outro after the act's last debrief, if the act has one.
     *
     * @param nextLevel the level the campaign goes on with
     */
    public static Optional<OutroScript> outro(CampaignRoute.ActEnd end, int nextLevel) {
        ActData act = end.act();
        ActData.TitleCard card = act.titleCard();
        return act.outro()
                .map(outro -> new OutroScript(
                        end.actDirectory(),
                        card.act() + " - " + card.name(),
                        act.levels().first(),
                        end.level(),
                        nextLevel,
                        outro.music(),
                        outro.pages()));
    }

    /**
     * The objective lines of the briefing: the primary objective, then the secondary one as a bonus,
     * by its name where the data gives one ("BONUS: HOLD THE BRIDGE").
     */
    static List<String> objectives(Content content, LevelData.Objectives objectives) {
        return objectives(content, objectives, Optional.empty());
    }

    /**
     * The objective lines of {@code level}'s briefing; M5 part H: an anchored act boss that holds a {@code
     * reach-end} level's end (Level 14's Siege Spire, the arena halting the scroll) reads "DESTROY THE SIEGE
     * SPIRE", the boss's bar name.
     */
    static List<String> objectives(Content content, LevelData level) {
        Optional<String> boss = level.boss()
                .map(placement -> content.enemy(placement.enemy()))
                .filter(enemy -> enemy.movement().anchored().isPresent())
                .flatMap(EnemyData::boss)
                .filter(script -> !script.midBoss())
                .map(EnemyData.BossData::barName);
        return objectives(content, level.objectives(), boss);
    }

    private static List<String> objectives(Content content, LevelData.Objectives objectives, Optional<String> actBoss) {
        List<String> lines = new ArrayList<>();
        lines.add(
                switch (objectives.primary()) {
                    case "reach-end" ->
                        actBoss.map(name -> "DESTROY THE " + name).orElse("SURVIVE TO THE END OF THE MISSION");
                    case "escort" -> {
                        // "ESCORT THE 5 CRAWLERS TO THE END": the ally slug's last word
                        LevelData.Escort escort = objectives.escort().orElseThrow();
                        String ally = escort.ally().substring(escort.ally().lastIndexOf('-') + 1);
                        yield String.format(
                                Locale.ROOT,
                                "ESCORT THE %d %sS TO THE END",
                                escort.units(),
                                ally.toUpperCase(Locale.ROOT));
                    }
                    case "destroy-targets" -> {
                        // "DESTROY ALL 4 BATTERIES": the groups' common first word in plural
                        List<String> targets = objectives.targets().orElseThrow();
                        yield String.format(
                                Locale.ROOT,
                                "DESTROY ALL %d %s",
                                targets.size(),
                                plural(firstWord(targets.getFirst())));
                    }
                    // M5 part G: "DEFEND NANSEN RELAY", the defended ally's name (Level 13).
                    case LevelData.Objectives.DEFEND ->
                        "DEFEND "
                                + content.allies()
                                        .allies()
                                        .get(objectives.defend().orElseThrow().ally())
                                        .name()
                                        .toUpperCase(Locale.ROOT);
                    default ->
                        throw new IllegalArgumentException("no briefing line for objective " + objectives.primary());
                });
        objectives
                .secondary()
                .ifPresent(secondary -> lines.add(secondary
                        .name()
                        .map(name -> "BONUS: " + name.toUpperCase(Locale.ROOT))
                        .or(() -> secondary
                                .killRatio()
                                .map(ratio -> String.format(
                                        Locale.ROOT, "BONUS: DESTROY %.0f %% OF ALL ENEMIES", 100 * ratio)))
                        .or(() -> secondary
                                .escapes()
                                .map(slug -> spawner(content, slug)
                                        // a spawner's self-burst is its escape (Level 04's Brood Pods)
                                        ? "BONUS: KILL EVERY "
                                                + slug.replace('-', ' ').toUpperCase(Locale.ROOT) + " BEFORE IT BURSTS"
                                        : "BONUS: NO " + slug.replace('-', ' ').toUpperCase(Locale.ROOT)
                                                + " GETS THROUGH"))
                        .or(() -> secondary.parts().map(parts -> partsLine(secondary, parts)))
                        // M5 part E: Level 11's "Convoy afloat" (its data may name it instead).
                        .or(() -> secondary.afloat().map(afloat -> "BONUS: NO CONVOY SHIP SUNK"))
                        // M5 part F: Level 12's sondes (its data names the objective instead).
                        .or(() ->
                                secondary.collect().map(what -> "BONUS: COLLECT ALL " + what.toUpperCase(Locale.ROOT)))
                        // M5 part G: Level 13's relay integrity (its data names the objective instead).
                        .or(() -> secondary
                                .integrity()
                                .map(share -> String.format(Locale.ROOT, "BONUS: KEEP %.0f %% INTEGRITY", 100 * share)))
                        .or(() -> secondary
                                .killAll()
                                .map(slugs -> "BONUS: DESTROY EVERY "
                                        + String.join(
                                                        " AND ",
                                                        slugs.stream()
                                                                .map(slug -> slug.replace('-', ' '))
                                                                .toList())
                                                .toUpperCase(Locale.ROOT)))
                        .orElseGet(() -> String.format(
                                Locale.ROOT,
                                "BONUS: CLEAR ALL %d %s",
                                secondary.groups().orElseThrow().size(),
                                groupsName(secondary)))));
        return lines;
    }

    /**
     * A parts secondary's line (Level 07's "Gut the bays"): its parts by their tracker label, to be
     * destroyed before the boss phase it names ends, "BONUS: DESTROY ALL 8 BAYS BEFORE THE BROADSIDE ENDS".
     */
    static String partsLine(LevelData.Secondary secondary, List<String> parts) {
        return String.format(
                Locale.ROOT,
                "BONUS: DESTROY ALL %d %s BEFORE THE %s ENDS",
                parts.size(),
                secondary.label().orElseThrow().toUpperCase(Locale.ROOT),
                secondary.before().orElseThrow().replace('-', ' ').toUpperCase(Locale.ROOT));
    }

    private static boolean spawner(Content content, String slug) {
        return content.enemy(slug).attacks().stream()
                .anyMatch(attack -> attack.pattern().equals("spawn"));
    }

    /** What the groups of a group objective are: their names' common first word in plural ("Dock One" ... "DOCKS"). */
    private static String groupsName(LevelData.Secondary secondary) {
        return plural(firstWord(secondary.groups().orElseThrow().getFirst()));
    }

    private static String firstWord(String name) {
        return name.contains(" ") ? name.substring(0, name.indexOf(' ')) : name;
    }

    /** "DOCK" reads "DOCKS", "BATTERY" "BATTERIES". */
    private static String plural(String word) {
        String upper = word.toUpperCase(Locale.ROOT);
        return upper.endsWith("Y") ? upper.substring(0, upper.length() - 1) + "IES" : upper + "S";
    }
}
