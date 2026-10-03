package vanguard.content.campaign;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import vanguard.content.ActData;
import vanguard.content.BriefingPage;
import vanguard.content.Content;
import vanguard.content.LevelData;

/**
 * Builds the briefing before a level from the design data: a level that opens its act gets the
 * act's title card and briefing first (design/campaign, Act intro and outro), so the intro
 * briefing of a new game is the one before Level 01.
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
                objectives(content, level.objectives()),
                level.briefing().teaser()));
    }

    /** The objective lines of the briefing: the primary objective, then the secondary one as a bonus. */
    static List<String> objectives(Content content, LevelData.Objectives objectives) {
        List<String> lines = new ArrayList<>();
        lines.add(
                switch (objectives.primary()) {
                    case "reach-end" -> "SURVIVE TO THE END OF THE MISSION";
                    case "escort" -> {
                        // "ESCORT THE 5 CRAWLERS TO THE END": the ally slug's last word
                        LevelData.Escort escort = objectives.escort().orElseThrow();
                        String ally = escort.ally().substring(escort.ally().lastIndexOf('-') + 1);
                        yield String.format(
                                Locale.ROOT,
                                "ESCORT THE %d %sS TO THE END",
                                escort.y().size(),
                                ally.toUpperCase(Locale.ROOT));
                    }
                    default ->
                        throw new IllegalArgumentException("no briefing line for objective " + objectives.primary());
                });
        objectives
                .secondary()
                .ifPresent(secondary -> lines.add(secondary
                        .killRatio()
                        .map(ratio -> String.format(Locale.ROOT, "BONUS: DESTROY %.0f %% OF ALL ENEMIES", 100 * ratio))
                        .or(() -> secondary
                                .escapes()
                                .map(slug -> spawner(content, slug)
                                        // a spawner's self-burst is its escape (Level 04's Brood Pods)
                                        ? "BONUS: KILL EVERY "
                                                + slug.replace('-', ' ').toUpperCase(Locale.ROOT) + " BEFORE IT BURSTS"
                                        : "BONUS: NO " + slug.replace('-', ' ').toUpperCase(Locale.ROOT)
                                                + " GETS THROUGH"))
                        .orElseGet(() -> String.format(
                                Locale.ROOT,
                                "BONUS: CLEAR ALL %d %s",
                                secondary.groups().orElseThrow().size(),
                                groupsName(secondary)))));
        return lines;
    }

    private static boolean spawner(Content content, String slug) {
        return content.enemy(slug).attacks().stream()
                .anyMatch(attack -> attack.pattern().equals("spawn"));
    }

    /** What the groups of a group objective are: their names' common last word in plural ("Dock One" ... "DOCKS"). */
    private static String groupsName(LevelData.Secondary secondary) {
        String first = secondary.groups().orElseThrow().getFirst();
        String word = first.contains(" ") ? first.substring(0, first.indexOf(' ')) : first;
        return word.toUpperCase(Locale.ROOT) + "S";
    }
}
