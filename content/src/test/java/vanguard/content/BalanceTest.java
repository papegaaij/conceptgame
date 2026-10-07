package vanguard.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;
import vanguard.content.campaign.Campaign;
import vanguard.content.campaign.CampaignRules;
import vanguard.content.campaign.Catalogue;
import vanguard.content.campaign.Fitted;
import vanguard.content.campaign.Hangar;
import vanguard.content.campaign.LoadoutSlot;
import vanguard.content.campaign.SaveGame;
import vanguard.sim.EnemySpec;

/**
 * The balancing checks over the data (design/systems/economy, design/enemies Balancing basis;
 * architecture: balance checks as JUnit tests), ported from tools/balance.py, which prints the same
 * plan as the balancing sheet:
 *
 * <ul>
 *   <li>the balance plan's purchases are in the shop and affordable at every hangar visit, with
 *       the levels' typical hauls as income (budget(n) for a level without data yet), within the
 *       generator's output; on medium with the repairs of the plan's estimated damage, on hard
 *       (income × 0.9, repairs 10 cr a point) with the repairs the credits left allow, the
 *       shortfall printed as accepted ({@link #HARD_REPAIRS});
 *   <li>the plan's fit reaches the reference DPS the enemy stat blocks assume (0.75–1.33×), with
 *       Levels 01–03's gentle onboarding (about 1.5×) as the accepted exception;
 *   <li>every enemy's time to kill at its first level and its bounty fit its size class, with the
 *       Coilwyrm's bounty and the Ravager's time to kill as the accepted exceptions ({@link #ACCEPTED}); a boss's bounty as paid
 *       there (its Act 1 terms × the act factor × the level's bounty scale);
 *   <li>a returning {@code medium} or larger unit keeps its time to kill at every Act 2 level after
 *       its first, with the act HP factor (user decision D5 = c of M5 part A);
 *   <li>the plan's anti-ground sources clear every hold zone's hardened cluster in at most {@link
 *       #HOLD_SHARE} of the hold's window at medium (user decision D8 = c of M5 part C; hard's
 *       tighter ratio is accepted, user decision 2026-10-07).
 * </ul>
 */
class BalanceTest {
    private static final Content CONTENT = ContentLoader.fromClasspath();
    private static final Catalogue CATALOGUE = Catalogue.of(CONTENT);
    private static final CampaignRules RULES = CampaignRules.of(CONTENT);

    /** The plan's fit against the reference DPS: inside this band. */
    static final double DPS_LOW = 0.75;

    static final double DPS_HIGH = 1.33;

    /**
     * Levels 01–03: a typical loadout is about 1.5× the reference DPS, accepted as gentle onboarding
     * (user decision 2026-10-01, design/enemies Balancing basis); up to {@link #ONBOARDING_HIGH}.
     */
    static final Set<Integer> ONBOARDING = Set.of(1, 2, 3);

    static final double ONBOARDING_HIGH = 1.75;

    /** Bosses and set pieces assume an effective DPS of 0.6 × reference (design/enemies Balancing basis). */
    static final double EFFECTIVE = 0.6;

    /**
     * M5 part C (D8 = c): a hold zone's cluster dies within this share of its window at medium, the
     * plan's anti-ground DPS against the cluster's HP (hard is not checked: about 0.73 there,
     * accepted by the user on 2026-10-07).
     */
    static final double HOLD_SHARE = 0.6;

    /** A boss's or set piece's bounty may lie this share of its target either side (the basis says "about"). */
    static final double SHARE_TOLERANCE = 1 / 3.0;

    /**
     * Deviations from the balancing basis the user accepted (design/enemies Balancing basis), with the
     * reason; the test fails when one disappears (remove it here).
     */
    static final Map<String, String> ACCEPTED = Map.of(
            "coilwyrm bounty",
            "the parts' sum 86 (head 40, 12 segments × 3, tail 10) is above the large class's 40-60:"
                    + " a multi-part enemy, cutting it up is extra work; a head-first kill pays 40"
                    + " (user decision 2026-10-05)",
            "ravager ttk",
            "16 HP at L09's reference DPS 63 is 0.25 s, below the medium class's 0.4-1.5 s: a fast,"
                    + " fragile pack hunter (user decision 2026-10-07, M5 part C)");

    /**
     * Hard: the static estimate cannot always pay both the plan's purchases and the repairs of its
     * {@code repair_points_per_level} at 10 cr a point; accepted (user decision 2026-10-05,
     * design/systems/economy): hard is meant to be tighter, a typical player there takes less damage
     * or skips some repairs.
     */
    static final String HARD_REPAIRS =
            "hard is tighter: a typical player takes less damage or skips some repairs (user decision 2026-10-05)";

    /**
     * Deviations from the balancing basis reported for a decision; the test fails when one is fixed
     * (remove it here) or a new one appears. None are open.
     */
    static final Map<String, String> PENDING = Map.of();

    /** One hangar visit of the balancing sheet. */
    record Row(
            int level,
            double budget,
            int income,
            boolean haul,
            int before,
            BalancePlan.Visit visit,
            int repairs,
            int unrepaired,
            int left,
            double load,
            double output,
            double front,
            double rear,
            double volley,
            Optional<Double> reference,
            double antiGround) {
        Optional<Double> ratio() {
            return reference.map(ref -> front / ref);
        }
    }

    /**
     * The balancing sheet: the plan bought visit by visit through the shop on medium, the static
     * repair estimate after every level, and each level's typical haul as its income.
     */
    static List<Row> sheet() {
        return sheet(Difficulty.MEDIUM);
    }

    /**
     * The balancing sheet on a difficulty: the income is the typical haul of the level as the
     * difficulty builds it (its formation sizes; budget(n) without level data) × the difficulty's
     * credit income, the repairs cost the difficulty's price; on another difficulty than medium it
     * repairs what the credits left allow and records the rest as unrepaired.
     */
    static List<Row> sheet(Difficulty difficulty) {
        BalancePlan plan = BalancePlan.load(CATALOGUE);
        double incomeFactor = CONTENT.difficulty().creditIncome().of(difficulty);
        List<Row> rows = new ArrayList<>();
        SaveGame save = Campaign.start(RULES, difficulty).save(Instant.EPOCH);
        for (int level : plan.levels()) {
            Campaign campaign = Campaign.load(RULES, save);
            campaign.giveFreeCharges(CONTENT.specials());
            Hangar hangar = new Hangar(CATALOGUE, campaign);
            int before = campaign.credits();
            BalancePlan.Visit visit = plan.buy(hangar, level);
            List<String> problems = new ArrayList<>(visit.problems());
            int repairs = 0;
            int unrepaired = 0;
            if (hangar.missingArmour() > 0) {
                int credits = campaign.credits();
                int missing = hangar.missingArmour();
                int points = difficulty == Difficulty.MEDIUM ? missing : hangar.affordableRepair();
                if (points > 0 && !hangar.repair(points)) {
                    problems.add("repairs: " + missing + " points cost " + missing * hangar.repairCost() + ", "
                            + credits + " credits left");
                } else {
                    unrepaired = missing - points;
                }
                repairs = credits - campaign.credits();
            }
            if (hangar.load() > hangar.output() + 1e-9) {
                problems.add("load " + hangar.load() + " MW over the output " + hangar.output());
            }
            double[] dps = dps(campaign.gear().loadout());
            Optional<String> key = CONTENT.levelKey(level);
            double budget = CONTENT.economy().budget().of(level);
            int income = (int) Math.rint(incomeFactor
                    * key.map(k -> TypicalHaul.of(CONTENT, k, difficulty).typical())
                            .orElse(budget));
            rows.add(new Row(
                    level,
                    budget,
                    income,
                    key.isPresent(),
                    before,
                    new BalancePlan.Visit(visit.spent(), visit.log(), problems),
                    repairs,
                    unrepaired,
                    campaign.credits(),
                    hangar.load(),
                    hangar.output(),
                    dps[0],
                    dps[1],
                    dps[0] + dps[1],
                    Optional.ofNullable(CONTENT.enemyBasis().referenceDps().get(level)),
                    antiGround(campaign.gear().loadout())));
            SaveGame after = campaign.save(Instant.EPOCH);
            save = next(after, after.credits() + income, campaign.maxArmour() - plan.repairPoints());
        }
        return rows;
    }

    /** The save after the level: its income banked and the estimated damage taken. */
    private static SaveGame next(SaveGame save, int credits, double armour) {
        return new SaveGame(
                save.version(),
                save.created(),
                save.playtime(),
                save.difficulty(),
                save.nextLevel() + 1,
                credits,
                save.score(),
                save.loadout(),
                save.inventory(),
                save.unlocks(),
                save.specials(),
                armour,
                save.retriesLeft(),
                save.grades(),
                save.dataCores(),
                save.storyFlags(),
                save.stats());
    }

    /** {forward single-target DPS, rear single-target DPS} of a loadout's weapons. */
    static double[] dps(Map<LoadoutSlot, Fitted> loadout) {
        double front = 0;
        double rear = 0;
        for (LoadoutSlot slot :
                List.of(LoadoutSlot.FRONT, LoadoutSlot.LEFT_WING, LoadoutSlot.RIGHT_WING, LoadoutSlot.REAR)) {
            Fitted fitted = loadout.get(slot);
            if (fitted == null) {
                continue;
            }
            WeaponData weapon = CONTENT.weapon(fitted.item());
            double single = singleTarget(weapon, weapon.levels().get(fitted.level() - 1));
            if (slot == LoadoutSlot.REAR) {
                rear += single;
            } else {
                front += single;
            }
        }
        return new double[] {front, rear};
    }

    /**
     * The single-target DPS of a loadout's anti-ground sources (the only damage a hardened unit
     * takes, the specials aside): its {@code anti-ground} weapons and Rook's gun if its base weapon
     * is one (his gun's scale × the base weapon's single-target DPS at its level).
     */
    static double antiGround(Map<LoadoutSlot, Fitted> loadout) {
        double dps = 0;
        for (Map.Entry<LoadoutSlot, Fitted> entry : loadout.entrySet()) {
            Fitted fitted = entry.getValue();
            if (entry.getKey() == LoadoutSlot.ESCORT) {
                WingmenData.Gun gun = CONTENT.wingmen().guns().list().stream()
                        .filter(g -> g.id().equals(fitted.item()))
                        .findFirst()
                        .orElseThrow();
                WeaponData base = CONTENT.weapon(gun.base());
                if (base.traits().contains("anti-ground")) {
                    dps += gun.scale() * singleTarget(base, base.levels().get(fitted.level() - 1));
                }
            } else if (List.of(LoadoutSlot.FRONT, LoadoutSlot.LEFT_WING, LoadoutSlot.RIGHT_WING, LoadoutSlot.REAR)
                    .contains(entry.getKey())) {
                WeaponData weapon = CONTENT.weapon(fitted.item());
                if (weapon.traits().contains("anti-ground")) {
                    dps += singleTarget(weapon, weapon.levels().get(fitted.level() - 1));
                }
            }
        }
        return dps;
    }

    /**
     * A level's single-target DPS: its volley DPS × the share of a volley that hits a target {@code
     * single_target.width} wide {@code single_target.distance} from the muzzle (a seeking weapon:
     * all). The same model as tools/design_data.py's {@code stats} and the weapon tables.
     */
    static double singleTarget(WeaponData weapon, WeaponData.Level level) {
        double volley = level.pattern().size() * level.damage() * level.rate();
        if (weapon.seek().orElse(false)) {
            return volley;
        }
        WeaponRulesData.SingleTarget target = CONTENT.weaponRules().singleTarget();
        double half = target.width() / 2 + weapon.size().width() / 2;
        double base = baseAngle(weapon, level);
        long hits = level.pattern().stream()
                .filter(shot ->
                        Math.abs(shot.x() + target.distance() * Math.tan(Math.toRadians(shot.angle() - base))) <= half)
                .count();
        return volley * hits / level.pattern().size();
    }

    private static double baseAngle(WeaponData weapon, WeaponData.Level level) {
        if (weapon.mirrored().orElse(false)) {
            return level.pattern().stream()
                    .mapToDouble(WeaponData.Shot::angle)
                    .average()
                    .orElseThrow();
        }
        return weapon.slot() == WeaponData.Slot.REAR ? 180 : 0;
    }

    @Test
    void thePlanIsInTheShopAndAffordableAtEveryVisit() {
        List<String> problems = print(sheet());
        assertTrue(problems.isEmpty(), "the balance plan: " + problems);
    }

    /**
     * Hard: the plan's purchases are in the shop and fit the generator at every visit; where the
     * credits fall short, of the repairs or of a purchase after the repairs of the visits before,
     * the shortfall is printed as accepted ({@link #HARD_REPAIRS}).
     */
    @Test
    void onHardThePlanIsInTheShopWithTheShortfallAccepted() {
        List<Row> rows = sheet(Difficulty.HARD);
        int cost = CONTENT.difficulty().repairCost().of(Difficulty.HARD);
        System.out.printf(
                "Hard (income × %.2f of the hard levels' typical hauls, repairs %d cr a point):%n",
                CONTENT.difficulty().creditIncome().of(Difficulty.HARD), cost);
        List<String> problems = new ArrayList<>();
        for (String problem : print(rows)) {
            if (problem.contains(": " + Hangar.Refusal.CREDITS.name() + " (")) {
                System.out.println(problem + ": accepted, " + HARD_REPAIRS);
            } else {
                problems.add(problem);
            }
        }
        for (Row row : rows) {
            if (row.unrepaired() > 0) {
                System.out.printf(
                        "L%02d visit: %d of %d armour points unrepaired (%d cr short): accepted, %s%n",
                        row.level(),
                        row.unrepaired(),
                        row.unrepaired() + row.repairs() / cost,
                        row.unrepaired() * cost,
                        HARD_REPAIRS);
            }
        }
        assertTrue(problems.isEmpty(), "the balance plan on hard: " + problems);
    }

    /** Prints the sheet and returns its problems. */
    private static List<String> print(List<Row> rows) {
        System.out.println(
                "Lvl | Budget | Income | Before | Spent | Repairs | Left | Load/Out | Fwd DPS | Ref | Fwd/Ref | Rear | Purchases");
        List<String> problems = new ArrayList<>();
        for (Row row : rows) {
            System.out.printf(
                    "%02d  | %6.0f | %6d%s | %6d | %5d | %7d | %4d | %4.1f/%-3.0f | %7.1f | %3.0f | %7s | %4.1f | %s%n",
                    row.level(),
                    row.budget(),
                    row.income(),
                    row.haul() ? "*" : " ",
                    row.before(),
                    row.visit().spent(),
                    row.repairs(),
                    row.left(),
                    row.load(),
                    row.output(),
                    row.front(),
                    row.reference().orElse(0.0),
                    row.ratio().map(r -> String.format("%.2f", r)).orElse("-"),
                    row.rear(),
                    String.join("; ", row.visit().log()));
            row.visit().problems().forEach(problem -> problems.add("L" + row.level() + ": " + problem));
        }
        System.out.println("* the level's typical haul (design/systems/economy); otherwise budget(n)");
        return problems;
    }

    /**
     * M5 part C (user decision D8 = c): at every level with hold zones, the plan's fit kills each
     * hold's hardened cluster (its units' HP at medium) with its anti-ground sources in at most
     * {@link #HOLD_SHARE} of the hold's window: from the trigger (its first unit {@code depth} px
     * below the top edge) until that unit's far edge leaves the bottom edge, the scroll easing over
     * the ramp (a smoothstep, so the section's and the hold's speeds averaged) and then at the hold
     * speed. Medium only (the user's call; hard's 40 px/s window and 83 HP nodes give about 0.73).
     */
    @Test
    void thePlansAntiGroundClearsEveryHoldInTime() {
        List<String> slow = new ArrayList<>();
        int holds = 0;
        for (Row row : sheet()) {
            Optional<String> key = CONTENT.levelKey(row.level());
            if (key.isEmpty()) {
                continue;
            }
            vanguard.sim.LevelScript level = SimSpecs.level(CONTENT, key.get(), Difficulty.MEDIUM);
            double scroll = CONTENT.level(key.get()).scrollSpeed();
            for (vanguard.sim.LevelScript.Hold hold : level.holds()) {
                holds++;
                double hp = 0;
                double half = 0;
                for (vanguard.sim.LevelScript.GroundUnit unit : level.groundUnits()) {
                    if (hold.groups().contains(unit.group())) {
                        hp += unit.enemy().hp();
                        half = Math.max(half, unit.enemy().hitbox().height() / 2);
                    }
                }
                double ease = hold.rampSeconds() * (scroll + hold.speed()) / 2;
                double window = hold.rampSeconds()
                        + (vanguard.sim.PlayField.HEIGHT + half - hold.depth() - ease) / hold.speed();
                double kill = hp / row.antiGround();
                System.out.printf(
                        "L%02d hold %s: %.0f HP / anti-ground %.1f DPS = %.1f s of a %.1f s window (%.2f, at most %.1f)%n",
                        row.level(), hold.groups(), hp, row.antiGround(), kill, window, kill / window, HOLD_SHARE);
                if (!(kill <= HOLD_SHARE * window)) {
                    slow.add(
                            String.format("L%02d hold %s: %.1f s of %.1f s", row.level(), hold.groups(), kill, window));
                }
            }
        }
        assertTrue(holds > 0, "Level 09's holds are checked");
        assertTrue(slow.isEmpty(), "the plan's anti-ground is too slow for: " + slow);
    }

    @Test
    void thePlansFitMeetsTheReferenceDps() {
        List<String> off = new ArrayList<>();
        for (Row row : sheet()) {
            if (row.ratio().isEmpty()) {
                continue;
            }
            double ratio = row.ratio().get();
            boolean inside = ratio >= DPS_LOW && ratio <= DPS_HIGH;
            boolean onboarding = ONBOARDING.contains(row.level()) && ratio > DPS_HIGH && ratio <= ONBOARDING_HIGH;
            if (onboarding) {
                System.out.printf(
                        "L%02d: DPS %.0f vs reference %.0f (%.2f×): accepted, gentle onboarding%n",
                        row.level(), row.front(), row.reference().get(), ratio);
            } else if (!inside) {
                off.add(String.format(
                        "L%02d: DPS %.0f vs reference %.0f (%.2f×)",
                        row.level(), row.front(), row.reference().get(), ratio));
            }
        }
        assertTrue(off.isEmpty(), "outside " + DPS_LOW + "-" + DPS_HIGH + "× the reference: " + off);
    }

    @Test
    void everyEnemyFitsTheBalancingBasis() {
        Map<String, String> found = new TreeMap<>();
        CONTENT.enemies().entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            String slug = entry.getKey();
            EnemyData enemy = entry.getValue();
            timeToKill(enemy).ifPresent(problem -> found.put(slug + " ttk", problem));
            bounty(enemy).ifPresent(problem -> found.put(slug + " bounty", problem));
        });
        found.forEach((key, problem) -> System.out.println(key + ": " + problem
                + (ACCEPTED.containsKey(key) ? " (accepted: " + ACCEPTED.get(key) + ")" : "")
                + (PENDING.containsKey(key) ? " (pending a decision)" : "")));
        Set<String> expected = new TreeSet<>(ACCEPTED.keySet());
        expected.addAll(PENDING.keySet());
        assertEquals(expected, found.keySet(), "deviations from the balancing basis: " + found);
    }

    /**
     * The act HP factor (design/enemies Balancing basis, D5 = c of M5 part A): a returning unit of
     * tier {@code medium} or larger, built by {@link SimSpecs#enemy} for each Act 2 level after its
     * first (Levels 08–14, as far as the reference DPS curve goes), keeps its time to kill inside its
     * size class's target at that level's reference DPS (its parts' and segments' HP summed; 1 HP of
     * rounding allowed). Units already off target at their first level are left to {@link
     * #everyEnemyFitsTheBalancingBasis}.
     */
    @Test
    void everyReturningUnitKeepsItsTimeToKillInActTwo() {
        Map<String, String> found = new TreeMap<>();
        int checked = 0;
        for (var entry : new TreeMap<>(CONTENT.enemies()).entrySet()) {
            EnemyData enemy = entry.getValue();
            if (enemy.boss().isPresent()
                    || enemy.tier().compareTo(Tier.MEDIUM) < 0
                    || enemy.tier() == Tier.HUGE
                    || timeToKill(enemy).isPresent()) {
                continue;
            }
            double[] target = enemy.tier() == Tier.MEDIUM ? new double[] {0.4, 1.5} : new double[] {1, 3};
            for (int level = Math.max(ACT_2_FIRST, enemy.firstLevel() + 1); level <= ACT_2_LAST; level++) {
                Double reference = CONTENT.enemyBasis().referenceDps().get(level);
                if (reference == null) {
                    continue;
                }
                EnemySpec spec = SimSpecs.enemy(CONTENT, entry.getKey(), Difficulty.MEDIUM, Optional.empty(), 2, level);
                double hp = spec.hp()
                        + spec.chain()
                                .map(chain -> chain.segment().hp()
                                                * chain.segmentBoxes().size()
                                        + chain.tail().hp())
                                .orElse(0.0);
                checked++;
                if (hp < target[0] * reference - 1 || hp > target[1] * reference + 1) {
                    found.put(
                            entry.getKey() + " L" + level,
                            String.format(
                                    "HP %.0f / %.1f DPS = %.2f s, target %s-%s s",
                                    hp, reference, hp / reference, target[0], target[1]));
                }
            }
        }
        assertTrue(checked > 0, "returning units checked");
        assertEquals(Map.of(), found, "returning units off their time to kill");
    }

    /** Act 2's levels (design/campaign/act-2-homefront). */
    static final int ACT_2_FIRST = 8;

    static final int ACT_2_LAST = 14;

    /**
     * Time to kill at the unit's first level (design/enemies Balancing basis): {@code tiny} one hit
     * of the starting gun, {@code small} ≤ 0.3 s, {@code medium} 0.4–1.5 s, {@code large} 1–3 s,
     * {@code huge} set pieces 20–40 s, mid-bosses 45–75 s, act bosses 90–180 s; bosses and set
     * pieces at the effective 0.6 × reference DPS. A multi-part unit counts its parts' sum. The HP
     * are whole damage units, so the bounds allow 1 HP of rounding.
     */
    static Optional<String> timeToKill(EnemyData enemy) {
        Double reference = CONTENT.enemyBasis().referenceDps().get(enemy.firstLevel());
        if (reference == null) {
            return Optional.empty();
        }
        if (enemy.tier() == Tier.TINY && enemy.boss().isEmpty()) {
            double shot =
                    CONTENT.weapon(SimSpecs.PULSE_CANNON).levels().getFirst().damage();
            return enemy.hp() <= shot
                    ? Optional.empty()
                    : Optional.of("HP " + enemy.hp() + ": not one hit of the starting gun (" + shot + ")");
        }
        double[] target = enemy.boss()
                .map(boss -> boss.kind().equals("mid-boss") ? new double[] {45, 75} : new double[] {90, 180})
                .orElseGet(() -> switch (enemy.tier()) {
                    case SMALL -> new double[] {0, 0.3};
                    case MEDIUM -> new double[] {0.4, 1.5};
                    case LARGE -> new double[] {1, 3};
                    default -> new double[] {20, 40};
                });
        double dps = reference * (enemy.boss().isPresent() || enemy.tier() == Tier.HUGE ? EFFECTIVE : 1);
        if (enemy.hp() < target[0] * dps - 1 || enemy.hp() > target[1] * dps + 1) {
            return Optional.of(String.format(
                    "HP %.0f / %.1f DPS = %.2f s, target %s-%s s",
                    enemy.hp(), dps, enemy.hp() / dps, target[0], target[1]));
        }
        return Optional.empty();
    }

    /**
     * The bounty's class (design/enemies Balancing basis, Act 1 terms): {@code tiny} 2–5, {@code
     * small} 10–15, {@code medium} 18–30, hardened and {@code large} 40–60; a {@code huge} set piece
     * about 15 % of its level's budget, a mid-boss 15 % and an act boss 30 %, as paid there (× the
     * level's bounty scale).
     */
    static Optional<String> bounty(EnemyData enemy) {
        boolean hardened = enemy.armour().text().filter("hardened"::equals).isPresent();
        if (enemy.boss().isPresent() || enemy.tier() == Tier.HUGE) {
            double share = enemy.boss()
                    .map(boss -> boss.kind().equals("mid-boss") ? 0.15 : 0.30)
                    .orElse(0.15);
            Optional<String> key = CONTENT.levelKey(enemy.firstLevel());
            if (key.isEmpty()) {
                return Optional.empty();
            }
            // Boss bounties hold Act 1 terms; the act factor applies to them like every payout (no exemption).
            int act = Integer.parseInt(Content.actDirectory(key.get()).split("-")[1]);
            double paid = enemy.bounty()
                    * Math.pow(CONTENT.economy().actFactor(), act - 1)
                    * CONTENT.level(key.get()).bounties();
            double actual = paid / CONTENT.economy().budget().of(enemy.firstLevel());
            if (Math.abs(actual - share) > share * SHARE_TOLERANCE) {
                return Optional.of(String.format(
                        "%.0f paid = %.1f %% of L%02d's budget, target about %.0f %%",
                        paid, 100 * actual, enemy.firstLevel(), 100 * share));
            }
            return Optional.empty();
        }
        int[] range =
                switch (enemy.tier()) {
                    case TINY -> new int[] {2, 5};
                    case SMALL -> new int[] {10, 15};
                    case MEDIUM -> hardened ? new int[] {40, 60} : new int[] {18, 30};
                    default -> new int[] {40, 60};
                };
        if (enemy.bounty() < range[0] || enemy.bounty() > range[1]) {
            return Optional.of(enemy.bounty() + " outside the " + enemy.tier() + " class " + range[0] + "-" + range[1]);
        }
        return Optional.empty();
    }
}
