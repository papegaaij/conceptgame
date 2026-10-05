package vanguard.content.campaign;

import java.util.Map;
import java.util.Optional;
import vanguard.content.ActData;
import vanguard.content.Content;

/**
 * Where the campaign goes between levels (design/ui, Screen flow): a new game opens with the
 * intro briefing, a won level leads to the next level's briefing, and every briefing leads to the
 * hangar, from which the next level launches. A won act's last level adds the act summary to its
 * debrief and then plays the act's outro before that (design/campaign, Act intro and outro; user
 * decision D2 of M4 part G); a replay never does. While the next level is not built yet, a won
 * level goes straight back to the hangar and the hangar cannot launch.
 */
public final class CampaignRoute {
    /** A step between levels. */
    public sealed interface Step {
        /** The briefing, then the hangar. */
        record Briefing(BriefingScript script) implements Step {}

        /** The act outro, then the step {@link #beforeNextLevel} gives. */
        record Outro(OutroScript script) implements Step {}

        record Hangar() implements Step {}
    }

    /**
     * The act whose last level the campaign has just won.
     *
     * @param actDirectory the act's directory under design/campaign
     * @param level the level that ended it: the act's last, or an earlier one with {@code --act-end}
     */
    public record ActEnd(String actDirectory, ActData act, int level) {}

    private CampaignRoute() {}

    /**
     * The step before the campaign's next level: after the difficulty select of a new game (the
     * intro briefing: the act intro and the Level 01 briefing) and after a won level's debrief.
     */
    public static Step beforeNextLevel(Content content, Campaign campaign) {
        return Briefings.before(content, campaign.nextLevel())
                .<Step>map(Step.Briefing::new)
                .orElseGet(Step.Hangar::new);
    }

    /**
     * The step after a won level's debrief: the act outro when the level ended its act and the act
     * has one, otherwise {@link #beforeNextLevel}.
     */
    public static Step afterLevel(Content content, Campaign campaign) {
        return actEnd(content, campaign)
                .flatMap(end -> Briefings.outro(end, campaign.nextLevel()))
                .<Step>map(Step.Outro::new)
                .orElseGet(() -> beforeNextLevel(content, campaign));
    }

    /**
     * The act the level just won ended (called after {@link Campaign#complete}): the act's last
     * level, or the level of the {@code --act-end} debug option. Never in a replay.
     */
    public static Optional<ActEnd> actEnd(Content content, Campaign campaign) {
        int won = campaign.nextLevel() - 1;
        if (campaign.replay().isPresent() || won < 1) {
            return Optional.empty();
        }
        Optional<Map.Entry<String, ActData>> act = content.actOf(won);
        boolean last = act.map(entry -> entry.getValue().levels().last() == won).orElse(false)
                || campaign.debugActEndLevel().equals(Optional.of(won));
        return act.filter(entry -> last).map(entry -> new ActEnd(entry.getKey(), entry.getValue(), won));
    }

    /** The level the hangar launches, if it is built yet. */
    public static Optional<String> launch(Content content, Campaign campaign) {
        return content.levelKey(campaign.nextLevel());
    }
}
