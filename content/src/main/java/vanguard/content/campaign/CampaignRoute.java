package vanguard.content.campaign;

import java.util.Optional;
import vanguard.content.Content;

/**
 * Where the campaign goes between levels (design/ui, Screen flow): a new game opens with the
 * intro briefing, a won level leads to the next level's briefing, and every briefing leads to the
 * hangar, from which the next level launches. While the next level is not built yet, a won level
 * goes straight back to the hangar and the hangar cannot launch.
 */
public final class CampaignRoute {
    /** A step between levels. */
    public sealed interface Step {
        /** The briefing, then the hangar. */
        record Briefing(BriefingScript script) implements Step {}

        record Hangar() implements Step {}
    }

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

    /** The level the hangar launches, if it is built yet. */
    public static Optional<String> launch(Content content, Campaign campaign) {
        return content.levelKey(campaign.nextLevel());
    }
}
