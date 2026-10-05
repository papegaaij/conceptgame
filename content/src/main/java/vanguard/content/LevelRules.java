package vanguard.content;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import vanguard.sim.Hitbox;
import vanguard.sim.LevelScript;
import vanguard.sim.PickupType;

/**
 * Part G's level rules for the simulation (design/campaign, Level 07 Brood Carrier), used by
 * {@link SimSpecs#level}: the tows, the pickups a boss's parts drop, the parts objective, and the
 * radio cues' requirements, boss subject and timeout trigger.
 */
final class LevelRules {
    private LevelRules() {}

    /** The level's tows (Level 07's lifeboat), each with its secret's crate. */
    static List<LevelScript.TowSpec> tows(LevelData level) {
        List<LevelScript.TowSpec> tows = new ArrayList<>();
        for (LevelData.Tow tow : level.tows().orElse(List.of())) {
            LevelData.Secret secret = level.secrets().stream()
                    .filter(s -> s.name().equals(tow.reveals()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("tows: no secret '" + tow.reveals() + "'"));
            tows.add(new LevelScript.TowSpec(
                    tow.t(),
                    tow.x(),
                    tow.drift().x(),
                    tow.drift().y(),
                    hitbox(tow.boat()),
                    hitbox(tow.pod()),
                    tow.tether().x(),
                    tow.tether().y(),
                    hitbox(tow.cable()),
                    tow.hits(),
                    secret.crate(),
                    secret.name()));
        }
        return tows;
    }

    /**
     * The pickups the level boss's parts drop (Level 07: the first bay sac shot off), from the
     * {@code carried} pickups of the difficulty whose carrier names parts.
     */
    static List<LevelScript.PartDrop> partDrops(
            Content content,
            LevelData level,
            List<LevelData.PlacedPickup> carried,
            Function<Pickup, PickupType> pickupType) {
        List<LevelScript.PartDrop> drops = new ArrayList<>();
        for (LevelData.PlacedPickup placed : carried) {
            Optional<List<String>> parts = placed.droppedBy().parts();
            if (parts.isEmpty()) {
                continue;
            }
            String boss = boss(level, "dropped_by.parts");
            drops.add(new LevelScript.PartDrop(
                    boss,
                    partIndexes(content.enemy(boss), parts.get()),
                    switch (placed.droppedBy().unit()) {
                        case FIRST -> 1;
                        case SECOND -> 2;
                        case LAST -> LevelScript.PartDrop.LAST;
                    },
                    pickupType.apply(placed.pickup())));
        }
        return drops;
    }

    /** The secondary objective for the simulation; a parts objective's parts and phase as indexes into the boss's. */
    static LevelScript.Secondary secondary(Content content, LevelData level, LevelData.Secondary secondary) {
        String partsOf = "";
        List<Integer> parts = List.of();
        int beforePhase = -1;
        if (secondary.parts().isPresent()) {
            partsOf = boss(level, "objectives.secondary.parts");
            EnemyData enemy = content.enemy(partsOf);
            parts = partIndexes(enemy, secondary.parts().get());
            String before = secondary.before().orElseThrow();
            List<EnemyData.PhaseData> phases = enemy.boss().orElseThrow().phases();
            for (int i = 0; i < phases.size() && beforePhase < 0; i++) {
                beforePhase = phases.get(i).name().equals(before) ? i : -1;
            }
            if (beforePhase < 0) {
                throw new IllegalArgumentException(partsOf + ": no boss phase '" + before + "'");
            }
        }
        return new LevelScript.Secondary(
                secondary.killRatio().orElse(0.0),
                secondary.credits(),
                secondary.groups().orElse(List.of()),
                secondary.escapes().orElse(""),
                secondary.killAll().orElse(List.of()),
                secondary.label().orElse(""),
                partsOf,
                parts,
                beforePhase);
    }

    /** What a radio cue's {@code requires} or {@code requires_not} names, as the simulation's fitted bits (0 for none). */
    static int requirement(Optional<String> name) {
        return name.map(n -> switch (n) {
                    case LevelData.Requirement.SPECIAL -> LevelScript.RadioCue.FITTED_SPECIAL;
                    case LevelData.Requirement.HOMING -> LevelScript.RadioCue.FITTED_HOMING;
                    default -> throw new IllegalArgumentException("requires: unknown '" + n + "'");
                })
                .orElse(0);
    }

    /**
     * A radio cue's subject: the enemy, group or phase it names; for a {@code boss-destroyed} cue the
     * level boss's slug, which the simulation cues it with.
     */
    static String subject(LevelData level, LevelData.RadioCue cue) {
        if (cue.event().orElse(null) == LevelData.CueEvent.BOSS_DESTROYED) {
            return boss(level, "a boss-destroyed cue");
        }
        return cue.enemy().or(cue::group).or(cue::phase).orElse("");
    }

    /** A cue's trigger: a boss-phase cue that waits for a timeout has its own. */
    static LevelScript.CueTrigger trigger(LevelData.RadioCue cue, LevelScript.CueTrigger trigger) {
        return trigger == LevelScript.CueTrigger.BOSS_PHASE && cue.timeout().orElse(false)
                ? LevelScript.CueTrigger.BOSS_TIMEOUT
                : trigger;
    }

    private static String boss(LevelData level, String what) {
        return level.boss()
                .map(LevelData.BossPlacement::enemy)
                .orElseThrow(() -> new IllegalArgumentException(what + " needs the level's boss"));
    }

    private static List<Integer> partIndexes(EnemyData enemy, List<String> names) {
        List<Integer> indexes = new ArrayList<>();
        for (String name : names) {
            int index = enemy.partIndex(name);
            if (index < 0) {
                throw new IllegalArgumentException(enemy.name() + ": no part '" + name + "'");
            }
            indexes.add(index);
        }
        return indexes;
    }

    private static Hitbox hitbox(Size size) {
        return new Hitbox(size.width(), size.height());
    }
}
