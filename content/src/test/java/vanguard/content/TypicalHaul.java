package vanguard.content;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import tools.jackson.databind.JsonNode;
import tools.jackson.dataformat.yaml.YAMLMapper;
import vanguard.sim.BossSpec;
import vanguard.sim.EnemySpec;
import vanguard.sim.Layer;
import vanguard.sim.LevelScript;
import vanguard.sim.PickupType;

/**
 * A level's credits at medium from its data (design/systems/economy, per-level budget): a perfect
 * run, and the typical haul, each source weighted by the economy's typical player. Bounties are ×
 * the act factor × the level's bounty scale, rounded half to even per payout; the other payouts
 * only × the act factor. The same model as tools/sync_tables.py's credit-budget table.
 *
 * @param perfect every enemy, ground target, part, pickup, secret and objective
 * @param typical what the typical medium player collects
 * @param budget budget(n) of the economy, the typical haul's target
 */
record TypicalHaul(int perfect, double typical, double budget) {

    /** The typical haul's distance from the budget, as a share of it (+0.1 = 10 % above). */
    double offBudget() {
        return (typical - budget) / budget;
    }

    static TypicalHaul of(Content content, String key) {
        return of(content, key, Difficulty.MEDIUM);
    }

    /**
     * The same from the level as {@code difficulty} builds it (its formation sizes), before that
     * difficulty's credit income; BalanceTest's hard sheet.
     */
    static TypicalHaul of(Content content, String key, Difficulty difficulty) {
        LevelData data = content.level(key);
        LevelScript level = SimSpecs.level(content, key, difficulty);
        EconomyData economy = content.economy();
        EconomyData.TypicalPlayer rate = economy.typicalPlayer();
        double factor = Math.pow(economy.actFactor(), level.act() - 1);
        Sum sum = new Sum(factor, factor * data.bounties());
        PlayerData.Credits salvage = content.player().pickups().salvage().credits();

        for (var wave : level.waves()) {
            sum.bounty(wave.count(), wave.enemy().bounty(), rate(rate, wave.enemy()));
            // a segment chain's unit is its head; its segments and tail pay their own bounty
            wave.enemy().chain().ifPresent(chain -> {
                sum.bounty(
                        wave.count() * chain.segmentBoxes().size(),
                        chain.segment().bounty(),
                        rate(rate, wave.enemy()));
                sum.bounty(wave.count(), chain.tail().bounty(), rate(rate, wave.enemy()));
            });
            wave.enemy()
                    .brood()
                    .ifPresent(brood -> sum.bounty(
                            wave.count() * brood.count(), brood.enemy().bounty(), rate(rate, brood.enemy())));
        }
        for (var unit : level.groundUnits()) {
            sum.bounty(1, unit.enemy().bounty(), rate.groundTargets());
            // M5 part C: a periodic spawner (Level 09's Hive Nodes) counts one release, as DensityTest
            unit.enemy()
                    .spawner()
                    .ifPresent(spawner ->
                            sum.bounty(spawner.count(), spawner.enemy().bounty(), rate(rate, spawner.enemy())));
        }
        for (var object : level.groundObjects()) {
            sum.bounty(1, object.bounty(), rate.groundTargets());
            sum.pay(1, credits(salvage, object.drop()), rate.groundTargets() * rate.pickups());
        }
        JsonNode bossNotes = bossNotes(key);
        int streams = bossNotes.path("streams").asInt(0);
        for (var piece : level.setPieces()) {
            // a boss dies in every won run; another set piece's parts are ground-target-like
            double share = piece.boss().isPresent() ? 1 : rate.groundTargets();
            for (var part : piece.parts()) {
                sum.bounty(1, part.bounty(), share);
            }
            sum.pay(1, credits(salvage, piece.drop()), share * rate.pickups());
            for (BossSpec.Phase phase : piece.boss().map(BossSpec::phases).orElse(List.of())) {
                phase.stream()
                        .ifPresent(stream -> sum.bounty(
                                streams * stream.count(), stream.enemy().bounty(), rate(rate, stream.enemy())));
            }
            // part G: the units a boss's windows launch in a typical fight (Level 07's Skitters and Needlers)
            for (EnemySpec kind : piece.boss().map(BossSpec::spawnKinds).orElse(List.of())) {
                int launched = bossNotes.path("spawns").path(kind.slug()).asInt(0);
                sum.bounty(launched, kind.bounty(), rate(rate, kind));
            }
        }
        // Each saveable unit home pays (M5 part D: not the scripted loss's, Level 10's Lifeline Three).
        level.escort().ifPresent(escort -> sum.pay(escort.saveable(), escort.credits(), rate.primary()));
        for (var secret : data.secrets()) {
            sum.pay(1, secret.crate(), rate.secrets());
        }
        LevelScript.Secondary secondary = level.secondary();
        int payouts = secondary.byGroups() ? secondary.groups().size() : 1;
        sum.pay(payouts, secondary.credits(), rate.secondary());

        return new TypicalHaul(sum.perfect, sum.typical, economy.budget().of(level.number()));
    }

    private static double rate(EconomyData.TypicalPlayer rate, EnemySpec enemy) {
        return enemy.layer() == Layer.GROUND ? rate.groundTargets() : rate.airKills();
    }

    private static int credits(PlayerData.Credits salvage, Optional<PickupType> drop) {
        return switch (drop.orElse(PickupType.OVERDRIVE)) {
            case SMALL_SALVAGE -> salvage.small();
            case MEDIUM_SALVAGE -> salvage.medium();
            case LARGE_SALVAGE -> salvage.large();
            default -> 0;
        };
    }

    /**
     * The level data's {@code boss.notes}: {@code streams}, the boss streams a fight at par sends, and
     * {@code spawns}, the units its windows launch in a typical fight by enemy slug.
     */
    private static JsonNode bossNotes(String key) {
        Path file = DesignTree.ROOT.resolve("campaign").resolve(key).resolve("data.yaml");
        try {
            return YAMLMapper.builder()
                    .build()
                    .readTree(Files.readString(file))
                    .path("boss")
                    .path("notes");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static final class Sum {
        private final double payFactor;
        private final double bountyFactor;
        int perfect;
        double typical;

        Sum(double payFactor, double bountyFactor) {
            this.payFactor = payFactor;
            this.bountyFactor = bountyFactor;
        }

        void bounty(int count, int credits, double share) {
            add(count, (int) Math.rint(credits * bountyFactor), share);
        }

        void pay(int count, int credits, double share) {
            add(count, (int) Math.rint(credits * payFactor), share);
        }

        private void add(int count, int paid, double share) {
            perfect += count * paid;
            typical += count * paid * share;
        }
    }
}
