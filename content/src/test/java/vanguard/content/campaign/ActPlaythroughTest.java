package vanguard.content.campaign;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;
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
 * Acts 1 and 2 flown as one campaign on each difficulty (M4 part H, the chained act playthrough;
 * carried on through Act 2 in M5 part I): from the new game's intro briefing, every hangar visit
 * buys the balance plan's purchases through the shop (each one in the shop and affordable; a charge
 * the plan buys while the special already carries its most, because the autopilot found or kept
 * one, is printed as accepted) and
 * repairs what the credits left allow, the ship first and then Rook (from Level 08, when he joins),
 * the test autopilot flies the level with the campaign's own loadout, armour, special charges and
 * escort, a failed attempt is retried as the mission failed screen offers it (from the boss
 * checkpoint where the difficulty has one; Rook with his level-start armour, raised to the floor), the
 * won level is banked with Rook's armour as it ended, and the campaign goes through a save and a load
 * before and after every visit. Level 07's win ends Act 1 with its outro and Act 2's intro briefing
 * follows; Level 14's win ends Act 2 with its outro. The credits and the fit per visit are printed
 * per act (the playthrough sheet in the test report).
 */
class ActPlaythroughTest {
    private static final Content CONTENT = ContentLoader.fromClasspath();
    private static final Catalogue CATALOGUE = Catalogue.of(CONTENT);
    private static final CampaignRules RULES = CampaignRules.of(CONTENT);
    /** The game's seed for every level (LevelScreen). */
    private static final long SEED = 2185;

    /** The acts' last levels: Act 1 ends at Level 07, Act 2 at Level 14. */
    private static final List<Integer> ACT_ENDS = List.of(7, 14);

    private static final int LAST = 14;

    /** Rook joins at the hangar visit before Level 08 (design/player/wingmen). */
    private static final int ESCORT_FROM = 8;
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
            int escortRepaired,
            int escortRepairs,
            double armour,
            String escort,
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
    void theAutopilotFliesActsOneAndTwoOnTheBalancePlan(Difficulty difficulty) {
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
            for (String problem : bought.problems()) {
                if (problem.contains("charge: " + Hangar.Refusal.FULL.name() + " (")) {
                    // The autopilot found or kept charges the plan expects used: nothing to buy, accepted.
                    System.out.printf(
                            "%s L%02d visit: %s: accepted, the special carries its most%n", difficulty, at, problem);
                } else {
                    problems.add("L" + at + " visit: " + problem);
                }
            }
            int repaired = hangar.affordableRepair();
            int credits = campaign.credits();
            if (repaired > 0) {
                assertTrue(hangar.repair(repaired));
            }
            int repairs = credits - campaign.credits();
            // Rook's repair line, from what the ship's repairs left.
            assertEquals(level >= ESCORT_FROM, hangar.escortHired(), "Rook is hired from Level 08 on");
            int escortRepaired = hangar.affordableEscortRepair();
            credits = campaign.credits();
            if (escortRepaired > 0) {
                assertTrue(hangar.repairEscort(escortRepaired));
            }
            int escortRepairs = credits - campaign.credits();
            String fit = fit(campaign);
            // The pre-launch save.
            campaign = roundTrip(campaign);
            double armour = campaign.armour();
            double escortMax = campaign.escortMaxArmour();
            String escort = campaign.escortFlight()
                    .map(rook -> String.format("%.0f/%.0f", rook.armour(), escortMax))
                    .orElse(hangar.escortHired() ? "grounded" : "-");
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
                        // Rook starts over with his level-start armour, raised to the floor (LevelScreen.retry).
                        double rook = campaign.escortFlight()
                                .map(Campaign.EscortFlight::armour)
                                .orElse(0.0);
                        if (sortie.wingman().isPresent() && rook > 0) {
                            sortie.retry(campaign.armour(), rook);
                        } else {
                            sortie.retry(campaign.armour());
                        }
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
                    sortie.special().found(),
                    sortie.wingman()
                            .map(rook -> OptionalDouble.of(rook.armour()))
                            .orElse(OptionalDouble.empty()));
            visits.add(new Visit(
                    level,
                    before,
                    bought,
                    repaired,
                    repairs,
                    escortRepaired,
                    escortRepairs,
                    armour,
                    escort,
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
            if (!ACT_ENDS.contains(level)) {
                assertTrue(CampaignRoute.actEnd(CONTENT, campaign).isEmpty(), "Level " + level + " ends no act");
                assertTrue(!(next instanceof CampaignRoute.Step.Outro), "no outro after Level " + level);
            } else {
                assertEquals(
                        level,
                        CampaignRoute.actEnd(CONTENT, campaign).orElseThrow().level());
                assertInstanceOf(
                        CampaignRoute.Step.Outro.class, next, "Level " + level + " ends its act with its outro");
                report(difficulty, ACT_ENDS.indexOf(level) + 1, visits, campaign);
                visits.clear();
                if (level < LAST) {
                    // After the outro: the next act's intro briefing before its first level.
                    assertInstanceOf(
                            CampaignRoute.Step.Briefing.class,
                            CampaignRoute.beforeNextLevel(CONTENT, campaign),
                            "the next act opens with its briefing");
                }
            }
            // The autosave after the debrief.
            campaign = roundTrip(campaign);
        }
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

    private static void report(Difficulty difficulty, int act, List<Visit> visits, Campaign campaign) {
        System.out.printf("Act %d playthrough, %s%n", act, difficulty);
        System.out.println("Lvl | Before | Plan | Repairs (pts) | Rook repairs (pts) | Left | Armour | Rook"
                + " | Attempts | Game overs | Earned | Grade | Kills | Bought / fit");
        for (Visit visit : visits) {
            System.out.printf(
                    "%02d  | %6d | %4d | %4d (%3d) | %4d (%3d) | %4d | %3.0f/%-3.0f | %-8s | %8d | %10d | %6d | %-5s | %3d/%-3d | %s | %s%s%n",
                    visit.level(),
                    visit.before(),
                    visit.plan().spent(),
                    visit.repairs(),
                    visit.repaired(),
                    visit.escortRepairs(),
                    visit.escortRepaired(),
                    visit.before() - visit.plan().spent() - visit.repairs() - visit.escortRepairs(),
                    visit.armour(),
                    visit.maxArmour(),
                    visit.escort(),
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
                "End of the act: %d credits, armour %.0f/%.0f, Rook %s, score %d, deaths %d%n",
                campaign.credits(),
                campaign.armour(),
                campaign.maxArmour(),
                campaign.gear().escort().hired()
                        ? String.format("%.0f/%.0f", campaign.gear().escort().armour(), campaign.escortMaxArmour())
                        : "-",
                campaign.score(),
                campaign.deaths());
    }
}
