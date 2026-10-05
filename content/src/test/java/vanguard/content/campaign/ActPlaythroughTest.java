package vanguard.content.campaign;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import vanguard.content.Autopilot;
import vanguard.content.BalancePlan;
import vanguard.content.Content;
import vanguard.content.ContentLoader;
import vanguard.content.Difficulty;
import vanguard.content.SimSpecs;
import vanguard.sim.LevelResult;
import vanguard.sim.Rules;
import vanguard.sim.Sortie;

/**
 * Act 1 flown as one campaign on each difficulty (M4 part H, the chained act playthrough): from the
 * new game's intro briefing, every hangar visit buys the balance plan's purchases through the shop
 * (each one in the shop and affordable) and repairs what the credits left allow, the test autopilot
 * flies the level with the campaign's own loadout, armour and special charges, a failed attempt is
 * retried as the mission failed screen offers it (from the boss checkpoint where the difficulty has
 * one), the won level is banked, and the campaign goes through a save and a load before and after
 * every visit. Level 07's win ends the act with the outro. The credits and the fit per visit are
 * printed (the playthrough sheet in the test report).
 */
class ActPlaythroughTest {
    private static final Content CONTENT = ContentLoader.fromClasspath();
    private static final Catalogue CATALOGUE = Catalogue.of(CONTENT);
    private static final CampaignRules RULES = CampaignRules.of(CONTENT);
    /** The game's seed for every level (LevelScreen). */
    private static final long SEED = 2185;

    private static final int LAST = 7;
    /** Steps one attempt may take at most (20 minutes at 60 steps/s). */
    private static final int MAX_STEPS = 60 * 60 * 20;
    /** Attempts one level may take at most before the test gives up. */
    private static final int MAX_ATTEMPTS = 12;

    /** One hangar visit and the level that followed. */
    record Visit(
            int level,
            int before,
            BalancePlan.Visit plan,
            int repaired,
            int repairs,
            double armour,
            double maxArmour,
            String fit,
            int attempts,
            int gameOvers,
            List<String> failures,
            int earned,
            String grade,
            int kills,
            int enemies) {}

    @ParameterizedTest
    @EnumSource(Difficulty.class)
    void theAutopilotFliesActOneOnTheBalancePlan(Difficulty difficulty) {
        BalancePlan plan = BalancePlan.load(CATALOGUE);
        boolean checkpoints = CONTENT.difficulty().bossCheckpoint().of(difficulty);
        Campaign campaign = Campaign.start(RULES, difficulty);
        assertInstanceOf(CampaignRoute.Step.Briefing.class, CampaignRoute.beforeNextLevel(CONTENT, campaign));
        List<Visit> visits = new ArrayList<>();
        List<String> problems = new ArrayList<>();

        for (int level = 1; level <= LAST; level++) {
            assertEquals(level, campaign.nextLevel());
            // The hangar opens: the free charges, then its autosave.
            campaign.giveFreeCharges(CONTENT.specials());
            campaign = roundTrip(campaign);
            Hangar hangar = new Hangar(CATALOGUE, campaign);
            int before = campaign.credits();
            BalancePlan.Visit bought = plan.buy(hangar, level);
            final int at = level;
            bought.problems().forEach(problem -> problems.add("L" + at + " visit: " + problem));
            int repaired = hangar.affordableRepair();
            int credits = campaign.credits();
            if (repaired > 0) {
                assertTrue(hangar.repair(repaired));
            }
            int repairs = credits - campaign.credits();
            String fit = fit(campaign);
            // The pre-launch save.
            campaign = roundTrip(campaign);
            double armour = campaign.armour();
            String key = CampaignRoute.launch(CONTENT, campaign).orElseThrow();

            Rules rules = SimSpecs.rules(CONTENT, key, difficulty);
            campaign.launch();
            Sortie sortie = sortie(campaign, key, rules);
            int attempts = 1;
            int gameOvers = 0;
            List<String> failures = new ArrayList<>();
            int steps = 0;
            while (!sortie.complete() && attempts <= MAX_ATTEMPTS && steps++ < MAX_STEPS * MAX_ATTEMPTS) {
                if (!sortie.flying() || sortie.primaryFailed()) {
                    attempts++;
                    failures.add(String.format(
                            "%s at %.0f s%s",
                            sortie.flying() ? "primary failed" : "destroyed",
                            sortie.levelSeconds(),
                            sortie.bossCheckpoint() ? " (boss)" : ""));
                    if (campaign.fail() == Campaign.Failure.GAME_OVER) {
                        // Continue: the hangar before the level, launched again with the gear of then.
                        gameOvers++;
                        campaign = roundTrip(campaign);
                        campaign.launch();
                        sortie = sortie(campaign, key, rules);
                        continue;
                    }
                    if (checkpoints && sortie.bossCheckpoint()) {
                        sortie.retryFromBoss();
                    } else {
                        sortie.retry(campaign.armour());
                    }
                }
                sortie.step(Autopilot.commands(sortie));
            }
            assertTrue(
                    sortie.complete(),
                    difficulty + ": Level " + level + " is not completed in " + attempts + " attempts: " + failures);
            LevelResult result = sortie.result();
            campaign.complete(
                    result,
                    sortie.ship().defences().armour(),
                    sortie.special().used(),
                    sortie.special().found());
            visits.add(new Visit(
                    level,
                    before,
                    bought,
                    repaired,
                    repairs,
                    armour,
                    campaign.maxArmour(),
                    fit,
                    attempts,
                    gameOvers,
                    failures,
                    result.credits().total() + result.gradeBonus(),
                    result.grade().letter(),
                    result.kills(),
                    result.enemies()));

            CampaignRoute.Step next = CampaignRoute.afterLevel(CONTENT, campaign);
            if (level < LAST) {
                assertTrue(CampaignRoute.actEnd(CONTENT, campaign).isEmpty(), "Level " + level + " ends no act");
                assertTrue(!(next instanceof CampaignRoute.Step.Outro), "no outro after Level " + level);
            } else {
                assertEquals(
                        LAST,
                        CampaignRoute.actEnd(CONTENT, campaign).orElseThrow().level());
                assertInstanceOf(CampaignRoute.Step.Outro.class, next, "Level 07 ends the act with its outro");
            }
            // The autosave after the debrief.
            campaign = roundTrip(campaign);
        }
        report(difficulty, visits, campaign);
        assertEquals(LAST + 1, campaign.nextLevel());
        assertTrue(problems.isEmpty(), difficulty + ": the balance plan at the visits: " + problems);
    }

    private static Sortie sortie(Campaign campaign, String key, Rules rules) {
        Flight flight = Flight.of(CONTENT, CATALOGUE, campaign);
        return new Sortie(
                SEED, flight.loadout(), SimSpecs.level(CONTENT, key, campaign.difficulty()), rules, campaign.armour());
    }

    /** The campaign written as a save and loaded again; the save reads back unchanged. */
    private static Campaign roundTrip(Campaign campaign) {
        SaveGame save = campaign.save(Instant.EPOCH);
        try {
            SaveGame read = SaveFormat.read(SaveFormat.write(save));
            assertEquals(save, read, "the save reads back unchanged");
            return Campaign.load(RULES, read);
        } catch (SaveException e) {
            throw new AssertionError(e);
        }
    }

    /** The fitted items with their levels and the special charges, as the report prints them. */
    private static String fit(Campaign campaign) {
        String items = campaign.gear().loadout().entrySet().stream()
                .map(entry -> {
                    String name = CATALOGUE
                            .item(entry.getKey().kind(), entry.getValue().item())
                            .name();
                    return entry.getValue().level() > 1
                            ? name + " L" + entry.getValue().level()
                            : name;
                })
                .collect(Collectors.joining(", "));
        String charges = campaign.gear().specials().entrySet().stream()
                .filter(entry -> entry.getValue() > 0)
                .map(entry -> entry.getValue() + "× " + entry.getKey())
                .sorted()
                .collect(Collectors.joining(", "));
        return charges.isEmpty() ? items : items + "; " + charges;
    }

    private static void report(Difficulty difficulty, List<Visit> visits, Campaign campaign) {
        System.out.printf("Act 1 playthrough, %s%n", difficulty);
        System.out.println(
                "Lvl | Before | Plan | Repairs (pts) | Left | Armour | Attempts | Game overs | Earned | Grade | Kills | Bought / fit");
        for (Visit visit : visits) {
            System.out.printf(
                    "%02d  | %6d | %4d | %4d (%3d) | %4d | %3.0f/%-3.0f | %8d | %10d | %6d | %-5s | %3d/%-3d | %s | %s%s%n",
                    visit.level(),
                    visit.before(),
                    visit.plan().spent(),
                    visit.repairs(),
                    visit.repaired(),
                    visit.before() - visit.plan().spent() - visit.repairs(),
                    visit.armour(),
                    visit.maxArmour(),
                    visit.attempts(),
                    visit.gameOvers(),
                    visit.earned(),
                    visit.grade(),
                    visit.kills(),
                    visit.enemies(),
                    String.join("; ", visit.plan().log()),
                    visit.fit(),
                    visit.failures().isEmpty() ? "" : " | failed: " + String.join(", ", visit.failures()));
        }
        System.out.printf(
                "End of the act: %d credits, armour %.0f/%.0f, score %d, deaths %d%n",
                campaign.credits(), campaign.armour(), campaign.maxArmour(), campaign.score(), campaign.deaths());
    }
}
