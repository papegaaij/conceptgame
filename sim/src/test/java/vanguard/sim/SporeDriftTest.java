package vanguard.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static vanguard.sim.TestSpecs.FULL_ARMOUR;
import static vanguard.sim.TestSpecs.INFINITE;
import static vanguard.sim.TestSpecs.LOADOUT;
import static vanguard.sim.TestSpecs.NEEDLER;
import static vanguard.sim.TestSpecs.SCORING;
import static vanguard.sim.TestSpecs.muzzle;
import static vanguard.sim.TestSpecs.wave;
import static vanguard.sim.WaveSpec.Edge.NONE;
import static vanguard.sim.WaveSpec.Entry.FRONT;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * The mechanics Level 03 brings (design/campaign, Level 03 – Spore Drift): spore mines, whirl
 * clusters and death bursts, convoys and the "nothing gets through" objective, debris chunks,
 * triggers that reveal a secret together, large salvage and the multi-part set piece.
 */
class SporeDriftTest {
    private static final Rules RULES =
            new Rules(120, 0, new PickupRules(10, 50, 20, 0.25, 10, 6, 40, 36, 200), SCORING);
    private static final EnemyGun.MineSpec SPORE = new EnemyGun.MineSpec(1, 8, 20, 1, 6, 4, true, 1);
    private static final EnemyGun SPORE_GUN = mineGun(SPORE);
    private static final EnemySpec BOMBER = bomber(SPORE_GUN);

    private static EnemyGun mineGun(EnemyGun.MineSpec spore) {
        return new EnemyGun(1.6, 0, 1, 90, 6, false, 1, 0, INFINITE, INFINITE, Optional.of(spore));
    }

    private static EnemySpec bomber(EnemyGun gun) {
        return new EnemySpec(
                "spore-bomber",
                24,
                new Hitbox(52, 56),
                Layer.LOW_AIR,
                10,
                false,
                25,
                45,
                Optional.empty(),
                Optional.of(45.0),
                Optional.empty(),
                Optional.empty(),
                Optional.of(gun),
                Optional.empty(),
                Optional.empty(),
                false,
                Optional.empty(),
                Optional.of(new Range(100, 300)),
                Optional.empty());
    }

    private static EnemySpec seed(Optional<EnemySpec.DeathBurst> burst) {
        return new EnemySpec(
                "whirl-seed",
                1,
                new Hitbox(16, 16),
                Layer.AIR,
                6,
                true,
                3,
                160,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                false,
                Optional.of(new EnemySpec.Spiral(1.5, 1, 90, 60, 3)),
                Optional.empty(),
                burst);
    }

    private static LevelScript level(
            double seconds,
            List<WaveSpec> waves,
            List<LevelScript.GroundObjectSpec> ground,
            List<LevelScript.RadioCue> radio,
            LevelScript.Secondary secondary,
            List<LevelScript.DebrisSpec> debris,
            List<LevelScript.SetPieceSpec> setPieces,
            int secrets) {
        return new LevelScript(
                3,
                1,
                0,
                List.of(new LevelScript.Section(seconds, 130)),
                waves,
                ground,
                List.of(),
                secrets,
                radio,
                secondary,
                List.of(),
                debris,
                setPieces);
    }

    private static LevelScript level(double seconds, List<WaveSpec> waves) {
        return level(seconds, waves, List.of(), List.of(), new LevelScript.Secondary(0.8, 50), List.of(), List.of(), 0);
    }

    private static Sortie sortie(LevelScript level) {
        return new Sortie(1, LOADOUT, level, RULES, FULL_ARMOUR);
    }

    private static int run(Sortie sortie, double seconds, int commands, SimEvents.Type counted) {
        int count = 0;
        for (int i = 0; i < SimStep.ticks(seconds); i++) {
            sortie.step(commands);
            count += sortie.events().count(counted);
        }
        return count;
    }

    // --- spore mines --------------------------------------------------------------------------

    @Test
    void aMineLayerDropsASporeEveryIntervalWhileOnTheScreen() {
        var sortie = sortie(level(30, List.of(wave(0, WaveSpec.Formation.SINGLE, BOMBER, 1, FRONT, NONE))));

        int dropped = run(sortie, 6, Command.LEFT.bit(), SimEvents.Type.MINE_DROPPED);

        // In the field after about 0.3 s, then one every 1.6 s: at 1.9, 3.5 and 5.1 s.
        assertEquals(3, dropped);
        assertEquals(3, sortie.mineCount());
    }

    @Test
    void aSporeCannotBeShotUntilItArmsThenPaysOneCreditButNoKill() {
        var sortie = sortie(level(20, List.of()));
        sortie.dropMine(SPORE_GUN, Ship.START_X, 300, Math.PI / 2);
        sortie.step(Command.NONE);
        assertFalse(sortie.mine(0).armed());

        int before = 0;
        for (int i = 0; i < SimStep.ticks(1) - 2; i++) {
            sortie.step(Command.FIRE.bit());
            before += sortie.events().count(SimEvents.Type.MINE_DESTROYED);
        }
        int after = run(sortie, 1, Command.FIRE.bit(), SimEvents.Type.MINE_DESTROYED);

        assertEquals(0, before, "below the player's layer, the shots pass under it");
        assertEquals(1, after);
        assertEquals(1, sortie.credits());
        assertEquals(0, sortie.kills());
        assertEquals(0, sortie.mineCount());
    }

    @Test
    void anArmedSporeBurstsOnTheHullForItsAttacksDamage() {
        var sortie = sortie(level(20, List.of()));
        sortie.dropMine(mineGun(new EnemyGun.MineSpec(1, 8, 0, 1, 6, 4, true, 1)), Ship.START_X, Ship.START_Y, 0);

        int bursts = run(sortie, 1.2, Command.NONE, SimEvents.Type.MINE_BURST);

        assertEquals(1, bursts);
        assertEquals(20 - 6, sortie.ship().defences().shield(), 0.5, "medium = 6, shield first");
        assertEquals(0, sortie.mineCount());
    }

    @Test
    void aSporeBurstsIntoItsRingAtTheEndOfItsLifeUnlessItNeverBursts() {
        var bursting = sortie(level(20, List.of()));
        bursting.dropMine(SPORE_GUN, 60, 450, 0);
        int bursts = run(bursting, 8.2, Command.NONE, SimEvents.Type.MINE_BURST);

        var fading = sortie(level(20, List.of()));
        fading.dropMine(mineGun(new EnemyGun.MineSpec(1, 8, 20, 1, 6, 4, false, 1)), 60, 450, 0);
        int fades = run(fading, 8.2, Command.NONE, SimEvents.Type.MINE_BURST);

        assertEquals(1, bursts);
        assertEquals(6, bursting.bulletCount());
        assertEquals(0, fades);
        assertEquals(0, fading.bulletCount());
        assertEquals(0, fading.mineCount());
    }

    // --- whirl seeds -----------------------------------------------------------------------------

    @Test
    void aWhirlClusterSpiralsOutFromItsPointAndLeavesThroughTheBottom() {
        WaveSpec cluster = new WaveSpec(
                0,
                WaveSpec.Formation.WHIRL_CLUSTER,
                seed(Optional.empty()),
                6,
                FRONT,
                NONE,
                Optional.empty(),
                Optional.empty(),
                1,
                Optional.empty(),
                Optional.empty(),
                List.of(),
                Optional.of(new WaveSpec.At(240, 100)));
        var sortie = sortie(level(20, List.of(cluster)));

        run(sortie, 0.3, Command.LEFT.bit(), SimEvents.Type.ENEMY_FIRED);
        assertEquals(6, sortie.enemyCount(), "all released within 0.3 s");
        run(sortie, 1.2, Command.LEFT.bit(), SimEvents.Type.ENEMY_FIRED);
        // The release point drifted down 60 px/s; each seed is about 1.5 s × 90 px/s from it.
        for (int i = 0; i < sortie.enemyCount(); i++) {
            Enemy seed = sortie.enemy(i);
            double age = 1.5 - 0.05 * i;
            double dx = seed.renderX(1) - 240;
            double dy = seed.renderY(1) - (440 - 60 * age);
            assertEquals(90 * age, Math.hypot(dx, dy), 3, "seed " + i);
        }
        double lowest = PlayField.HEIGHT;
        while (sortie.enemyCount() > 0 && sortie.levelSeconds() < 19) {
            sortie.step(Command.LEFT.bit());
            for (int i = 0; i < sortie.enemyCount(); i++) {
                Enemy seed = sortie.enemy(i);
                assertTrue(seed.renderX(1) > -8 && seed.renderX(1) < PlayField.WIDTH + 8, "it bounces off the sides");
                lowest = Math.min(lowest, seed.renderY(1));
            }
        }
        assertEquals(0, sortie.enemyCount(), "every seed has left");
        assertTrue(lowest < 10);
    }

    @Test
    void aSeedWithADeathBurstPopsIntoItsPuff() {
        var burst = Optional.of(new EnemySpec.DeathBurst(3, 90, 4));
        var sortie = sortie(level(20, List.of(wave(0, WaveSpec.Formation.SINGLE, seed(burst), 1, FRONT, NONE))));

        while (sortie.kills() == 0 && sortie.levelSeconds() < 3) {
            sortie.step(Command.FIRE.bit());
        }

        assertEquals(1, sortie.kills());
        assertEquals(3, sortie.bulletCount());
    }

    // --- convoy and "nothing gets through" -----------------------------------------------------

    @Test
    void aConvoyTurnsAcrossAtItsStrafeHeightAndOneGettingAwayFailsTheObjective() {
        var escaped = new LevelScript.RadioCue(
                LevelScript.CueTrigger.ENEMY_ESCAPED, 0, "spore-bomber", "Okafor", "One got past.", false, "grim");
        var secondary = new LevelScript.Secondary(0, 50, List.of(), "spore-bomber");
        var sortie = sortie(level(
                40,
                List.of(wave(0, WaveSpec.Formation.CONVOY, BOMBER, 3, FRONT, NONE)),
                List.of(),
                List.of(escaped),
                secondary,
                List.of(),
                List.of(),
                0));
        assertEquals(3, sortie.escapesTotal());

        double lowest = PlayField.HEIGHT;
        int failed = 0;
        int radio = 0;
        int entered = 0;
        while (sortie.levelSeconds() < 30) {
            int before = sortie.enemyCount();
            sortie.step(Command.LEFT.bit());
            entered += Math.max(0, sortie.enemyCount() - before);
            failed += sortie.events().count(SimEvents.Type.OBJECTIVE_FAILED);
            radio += sortie.events().count(SimEvents.Type.RADIO);
            for (int i = 0; i < sortie.enemyCount(); i++) {
                lowest = Math.min(lowest, sortie.enemy(i).renderY(1));
            }
        }

        assertEquals(3, entered);
        assertEquals(PlayField.HEIGHT - 200, lowest, 10, "turns across at y≈200 below the top");
        assertEquals(1, failed);
        assertEquals(1, radio, "the first escape calls its line once");
        assertTrue(sortie.secondaryFailed());
        assertFalse(sortie.secondaryMet());
    }

    @Test
    void destroyingEveryUnitOfAnEscapesObjectiveMeetsIt() {
        var secondary = new LevelScript.Secondary(0, 50, List.of(), "spore-bomber");
        var sortie = sortie(level(
                30,
                List.of(wave(0, WaveSpec.Formation.SINGLE, BOMBER, 1, FRONT, NONE)),
                List.of(),
                List.of(),
                secondary,
                List.of(),
                List.of(),
                0));

        int met = run(sortie, 12, Command.FIRE.bit(), SimEvents.Type.OBJECTIVE_MET);

        assertEquals(1, met);
        assertTrue(sortie.secondaryMet());
        assertFalse(sortie.secondaryFailed());
        assertEquals(50, sortie.result().credits().objectives());
    }

    // --- debris ----------------------------------------------------------------------------------

    private static LevelScript.DebrisSpec chunk(double t, double x, Hitbox size, double vy, double hp, double damage) {
        return new LevelScript.DebrisSpec(t, x, hp > 1e9 ? "large-a" : "small-a", size, 0, vy, hp, damage, 120);
    }

    private static LevelScript debrisLevel(List<WaveSpec> waves, LevelScript.DebrisSpec... chunks) {
        return level(
                30, waves, List.of(), List.of(), new LevelScript.Secondary(0.8, 50), List.of(chunks), List.of(), 0);
    }

    @Test
    void aLargeChunkMakesShotsGlanceAndASmallOneBreaksAfterItsHp() {
        var large = sortie(debrisLevel(List.of(), chunk(0, Ship.START_X, new Hitbox(96, 80), -30, INFINITE, 15)));
        int glanced = run(large, 3, Command.FIRE.bit(), SimEvents.Type.SHOT_GLANCED);

        var small = sortie(debrisLevel(List.of(), chunk(0, Ship.START_X, new Hitbox(32, 28), -30, 6, 0)));
        int hits = 0;
        int broken = 0;
        for (int i = 0; i < SimStep.ticks(3); i++) {
            small.step(Command.FIRE.bit());
            hits += small.events().count(SimEvents.Type.DEBRIS_HIT);
            broken += small.events().count(SimEvents.Type.DEBRIS_DESTROYED);
        }

        assertTrue(glanced > 10, "every shot that reaches it glances: " + glanced);
        assertEquals(1, large.debrisCount());
        assertEquals(3, hits, "6 HP, 2 per Pulse Cannon shot");
        assertEquals(1, broken);
        assertEquals(0, small.debrisCount());
        assertEquals(0, small.credits(), "pays nothing");
    }

    @Test
    void debrisStopsEnemyBullets() {
        var needler = List.of(wave(0, WaveSpec.Formation.LINE_ABREAST, NEEDLER, 1, FRONT, NONE));
        var open = sortie(debrisLevel(needler));
        int openHits = run(open, 5, Command.NONE, SimEvents.Type.SHIELD_HIT);

        // A wall-wide chunk drifting down over the hovering Needler, still clear of the ship after 5 s.
        var covered = sortie(debrisLevel(needler, chunk(0, 240, new Hitbox(480, 400), -100, INFINITE, 0)));
        int fired = 0;
        int hits = 0;
        for (int i = 0; i < SimStep.ticks(5); i++) {
            covered.step(Command.NONE);
            fired += covered.events().count(SimEvents.Type.ENEMY_FIRED);
            hits += covered.events().count(SimEvents.Type.SHIELD_HIT);
        }

        assertTrue(openHits > 0);
        assertTrue(fired > 0);
        assertEquals(0, hits);
    }

    @Test
    void aLargeChunkDealsItsContactDamageOncePerSecond() {
        var sortie = sortie(debrisLevel(List.of(), chunk(0, Ship.START_X, new Hitbox(96, 80), -40, INFINITE, 15)));

        List<Integer> strikes = new ArrayList<>();
        double firstLoss = 0;
        for (int i = 0; i < SimStep.ticks(20); i++) {
            double lost = sortie.ship().defences().armourLost();
            sortie.step(Command.NONE);
            if (sortie.ship().defences().armourLost() > lost) {
                if (strikes.isEmpty()) {
                    firstLoss = sortie.ship().defences().armourLost() - lost;
                }
                strikes.add(i);
            }
        }

        assertTrue(strikes.size() >= 2, "strikes " + strikes);
        for (int i = 1; i < strikes.size(); i++) {
            assertTrue(strikes.get(i) - strikes.get(i - 1) >= SimStep.PER_SECOND, "strikes " + strikes);
        }
        assertEquals(7.5, firstLoss, 1e-9, "a collision: half of the 15 to the shield, half to armour");
    }

    @Test
    void aChunkNeverEntersCloseToTheShip() {
        var sortie = sortie(debrisLevel(List.of(), chunk(3, Ship.START_X, new Hitbox(56, 48), -30, INFINITE, 15)));
        run(sortie, 2.9, Command.UP.bit(), SimEvents.Type.SHIELD_HIT);
        sortie.step(Command.NONE);
        sortie.step(Command.NONE);

        Debris debris = sortie.debris(0);
        double dx = debris.renderX(1) - sortie.ship().x();
        double dy = debris.renderY(1) - sortie.ship().y();
        assertTrue(Math.hypot(dx, dy) >= 119, "entered " + Math.hypot(dx, dy) + " px from the ship");
    }

    // --- triggers and pickups ----------------------------------------------------------------------

    @Test
    void triggersThatShareASecretRevealItWhenTheLastIsShot() {
        var light1 = new LevelScript.GroundObjectSpec(
                0, Ship.START_X, new Hitbox(12, 12), 0, 0, Optional.empty(), 1, 75, "lifeboat rack", false, 0, 2);
        var light2 = new LevelScript.GroundObjectSpec(
                1, Ship.START_X, new Hitbox(12, 12), 0, 0, Optional.empty(), 1, 75, "lifeboat rack", false, 0, 2);
        var sortie = sortie(level(
                20,
                List.of(),
                List.of(light1, light2),
                List.of(),
                new LevelScript.Secondary(0.8, 50),
                List.of(),
                List.of(),
                1));

        int foundEarly = run(sortie, 1, Command.FIRE.bit(), SimEvents.Type.SECRET_FOUND);
        int found = run(sortie, 2, Command.FIRE.bit(), SimEvents.Type.SECRET_FOUND);

        assertEquals(0, foundEarly, "the first light alone reveals nothing");
        assertEquals(1, found);
        assertEquals(PickupType.HIDDEN_CRATE, sortie.pickup(0).type());
        assertEquals(75, sortie.pickup(0).credits());
    }

    // --- the set piece -----------------------------------------------------------------------------

    /** A vital core on the body's centre line, a side part off its right flank with a gun; 80 credits in all. */
    private static LevelScript.SetPieceSpec piece(Hitbox body, double leaveAt, Optional<EnemyGun> gun) {
        var core = new LevelScript.PartSpec("core", 0, 50, new Hitbox(40, 40), 10, true, 60, Optional.empty(), 0);
        var side = new LevelScript.PartSpec("side", 80, 0, new Hitbox(30, 30), 4, false, 20, gun, 0.5);
        var high = LevelScript.Pass.flight(
                "cross",
                Layer.HIGH_AIR,
                0,
                List.of(new LevelScript.Waypoint(0, 240, 700), new LevelScript.Waypoint(3, 240, -200)));
        var low = new LevelScript.Pass(
                "descend",
                Layer.AIR,
                0,
                List.of(new LevelScript.Waypoint(4, 240, 800), new LevelScript.Waypoint(5, 240, 400)),
                5,
                1,
                leaveAt,
                200);
        return new LevelScript.SetPieceSpec(
                "leviathan",
                new Hitbox(300, 480),
                body,
                25,
                List.of(core, side),
                List.of(high, low),
                Optional.of(PickupType.LARGE_SALVAGE));
    }

    private static LevelScript pieceLevel(LevelScript.SetPieceSpec piece, LevelScript.RadioCue... cues) {
        return level(
                30,
                List.of(),
                List.of(),
                List.of(cues),
                new LevelScript.Secondary(0.8, 50),
                List.of(),
                List.of(piece),
                0);
    }

    @Test
    void aSetPieceFliesItsPassesAndEscapesAfterTheLast() {
        var escaped = new LevelScript.RadioCue(
                LevelScript.CueTrigger.ENEMY_ESCAPED, 0, "leviathan", "Varga", "It's leaving.", false, "neutral");
        var sortie = sortie(pieceLevel(piece(new Hitbox(100, 200), 12, Optional.empty()), escaped));
        SetPiece leviathan = sortie.setPiece(0);
        assertEquals(1, sortie.enemyTotal());

        sortie.step(Command.NONE);
        assertTrue(leviathan.present());
        assertEquals(Layer.HIGH_AIR, leviathan.layer());
        run(sortie, 3.5, Command.NONE, SimEvents.Type.RADIO);
        assertFalse(leviathan.present(), "between its passes");
        assertEquals(1, leviathan.pass());
        run(sortie, 2, Command.NONE, SimEvents.Type.RADIO);
        assertEquals(Layer.HIGH_AIR, leviathan.layer(), "descending");
        assertEquals(0.5, leviathan.altitude(1), 0.02);
        int descended = run(sortie, 1, Command.NONE, SimEvents.Type.SET_PIECE_DESCENDED);
        assertEquals(1, descended);
        assertEquals(Layer.AIR, leviathan.layer());
        assertTrue(leviathan.onPlane());

        int escapes = 0;
        int radio = 0;
        while (sortie.levelSeconds() < 20) {
            sortie.step(Command.NONE);
            escapes += sortie.events().count(SimEvents.Type.SET_PIECE_ESCAPED);
            radio += sortie.events().count(SimEvents.Type.RADIO);
        }
        assertEquals(1, escapes);
        assertEquals(1, radio);
        assertTrue(leviathan.escaped());
        assertFalse(leviathan.present());
    }

    @Test
    void standardShotsCannotReachTheHighAirPass() {
        var sortie = sortie(pieceLevel(piece(new Hitbox(100, 200), 12, Optional.empty())));

        int hits = run(sortie, 3, Command.FIRE.bit(), SimEvents.Type.ENEMY_HIT);
        int glanced = sortie.events().count(SimEvents.Type.SHOT_GLANCED);

        assertEquals(0, hits);
        assertEquals(0, glanced);
    }

    @Test
    void homingMissilesLockOntoItsPartsEvenOnHighAir() {
        WeaponSpec missile = new WeaponSpec(
                "micro-missile-pod",
                "micromissile",
                "micromissile",
                WeaponSpec.Delivery.HOMING,
                false,
                2,
                4,
                500,
                new Hitbox(4, 10),
                600,
                1.2,
                1,
                0,
                Math.toRadians(270),
                Math.toRadians(70),
                0,
                0,
                List.of(muzzle(0, 21, 0)));
        Loadout loadout = TestSpecs.loadout(new Armament.Mount(Armament.Slot.FRONT, missile, missile));
        var sortie = new Sortie(
                1, loadout, pieceLevel(piece(new Hitbox(100, 200), 12, Optional.empty())), RULES, FULL_ARMOUR);

        int hits = run(sortie, 3, Command.FIRE.bit(), SimEvents.Type.ENEMY_HIT);

        assertTrue(hits > 0);
    }

    @Test
    void shotsOverTheArmouredBodyGlanceUnlessAPartLiesAhead() {
        var sortie = sortie(pieceLevel(piece(new Hitbox(100, 200), 20, Optional.empty())));
        run(sortie, 6.5, Command.NONE, SimEvents.Type.RADIO);
        // Line up off the core's column but over the body.
        while (sortie.ship().x() < 270) {
            sortie.step(Command.RIGHT.bit());
        }
        run(sortie, 0.2, Command.NONE, SimEvents.Type.RADIO);
        assertTrue(
                Math.abs(sortie.ship().x() - 240) > 25 && Math.abs(sortie.ship().x() - 240) < 50);

        int glanced = 0;
        int hits = 0;
        for (int i = 0; i < SimStep.PER_SECOND; i++) {
            sortie.step(Command.FIRE.bit());
            glanced += sortie.events().count(SimEvents.Type.SHOT_GLANCED);
            hits += sortie.events().count(SimEvents.Type.ENEMY_HIT);
        }

        assertTrue(glanced > 0);
        assertEquals(0, hits);
    }

    @Test
    void destroyingTheVitalPartDestroysTheRestForTheFullBountyAndDropsLargeSalvage() {
        var down = new LevelScript.RadioCue(
                LevelScript.CueTrigger.FIRST_KILL, 0, "leviathan", "Ring Control", "It's down.", false, "neutral");
        var sortie = sortie(pieceLevel(piece(new Hitbox(100, 200), 20, Optional.empty()), down));
        run(sortie, 6.5, Command.NONE, SimEvents.Type.RADIO);

        int parts = 0;
        int destroyed = 0;
        int radio = 0;
        for (int i = 0; i < 2 * SimStep.PER_SECOND; i++) {
            sortie.step(Command.FIRE.bit());
            parts += sortie.events().count(SimEvents.Type.PART_DESTROYED);
            destroyed += sortie.events().count(SimEvents.Type.SET_PIECE_DESTROYED);
            radio += sortie.events().count(SimEvents.Type.RADIO);
        }

        SetPiece leviathan = sortie.setPiece(0);
        assertEquals(2, parts, "the core and the side part it took with it");
        assertEquals(1, destroyed);
        assertEquals(1, radio);
        assertTrue(leviathan.destroyed());
        assertTrue(leviathan.partWrecked(0) && leviathan.partWrecked(1));
        assertEquals(80, sortie.result().credits().kills());
        assertEquals(1, sortie.kills());
        assertEquals(PickupType.LARGE_SALVAGE, sortie.pickup(0).type());
        assertEquals(200, sortie.pickup(0).credits());
    }

    @Test
    void itsPartsFireOnThePlayersLayerUntilWrecked() {
        EnemyGun gun = EnemyGun.aimed(1, 0, 1, 130, 6, false);
        var sortie = sortie(pieceLevel(piece(new Hitbox(100, 200), 20, Optional.of(gun))));

        int early = run(sortie, 6, Command.NONE, SimEvents.Type.ENEMY_FIRED);
        // On the layer from t = 6: the side part fires at 6.5, 7.5, 8.5, ...
        int firing = run(sortie, 3, Command.NONE, SimEvents.Type.ENEMY_FIRED);

        assertEquals(0, early, "not before it is on the player's layer");
        assertEquals(3, firing);
    }

    @Test
    void itsBodyDealsContactDamageOncePerSecondOnThePlayersLayer() {
        var sortie = sortie(pieceLevel(piece(new Hitbox(100, 640), 20, Optional.empty())));

        List<Integer> strikes = new ArrayList<>();
        for (int i = 0; i < SimStep.ticks(10); i++) {
            double lost = sortie.ship().defences().armourLost();
            sortie.step(Command.NONE);
            if (sortie.ship().defences().armourLost() > lost) {
                strikes.add(i);
            }
        }

        assertTrue(strikes.getFirst() >= SimStep.ticks(6) - 1, "not while it descends: " + strikes);
        assertTrue(strikes.size() >= 3, "strikes " + strikes);
        for (int i = 1; i < strikes.size(); i++) {
            assertTrue(strikes.get(i) - strikes.get(i - 1) >= SimStep.PER_SECOND, "strikes " + strikes);
        }
    }

    @Test
    void steppingThroughTheNewMechanicsDoesNotAllocate() {
        var threads = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        LevelScript script = level(
                30,
                List.of(
                        wave(0, WaveSpec.Formation.CONVOY, BOMBER, 3, FRONT, NONE),
                        new WaveSpec(
                                1,
                                WaveSpec.Formation.WHIRL_CLUSTER,
                                seed(Optional.of(new EnemySpec.DeathBurst(3, 90, 4))),
                                8,
                                FRONT,
                                NONE,
                                Optional.empty(),
                                Optional.empty(),
                                1,
                                Optional.empty(),
                                Optional.empty(),
                                List.of(),
                                Optional.of(new WaveSpec.At(240, 100)))),
                List.of(),
                List.of(),
                new LevelScript.Secondary(0, 50, List.of(), "spore-bomber"),
                List.of(
                        chunk(0.5, 120, new Hitbox(96, 80), -30, INFINITE, 15),
                        chunk(1, 360, new Hitbox(32, 28), -30, 6, 0)),
                List.of(piece(new Hitbox(100, 200), 12, Optional.of(EnemyGun.aimed(1, 0, 2, 130, 6, false)))),
                0);
        // A first run loads and initialises every class the mechanics touch.
        Sortie warmUp = sortie(script);
        for (int i = 0; i < SimStep.ticks(20); i++) {
            warmUp.step(SortieTest.Pilot.commands(i));
        }
        var sortie = sortie(script);

        long before = threads.getCurrentThreadAllocatedBytes();
        for (int i = 0; i < SimStep.ticks(20); i++) {
            sortie.step(SortieTest.Pilot.commands(i));
        }
        long allocated = threads.getCurrentThreadAllocatedBytes() - before;

        assertTrue(allocated < 1024, "20 s allocated " + allocated + " bytes");
    }
}
