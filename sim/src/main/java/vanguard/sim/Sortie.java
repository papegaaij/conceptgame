package vanguard.sim;

import java.util.Arrays;
import java.util.List;

/**
 * A level in play, advanced in fixed 60 Hz steps by {@link #step(int)}: the launch, the scroll
 * through the sections, the ship and its {@link PlayerFire}, the {@link EnemyForce} of the
 * {@link LevelScript}, its {@link SetPiece}s, the ground objects, the {@link Debris} chunks, the
 * pickups, the {@link Radio} cues, the {@link Objectives} and the tally. All entities live in pools allocated up front and the waves
 * are planned when the sortie is created, so stepping does not allocate, and the same seed and
 * commands give the same {@link #stateHash()} on every platform.
 *
 * <p>When armour reaches zero the ship explodes and the level runs on without it until the
 * presentation, after the mission failed screen, restarts it with {@link #retry(double)} from the
 * level-start state (design/systems/retry): what the attempt earned is lost. The level is over
 * when the scroll reaches the end of the last section; the ship then flies on, out of harm's way,
 * until the presentation moves to the debrief.
 *
 * <p>A boss (a {@link SetPiece} with a {@link BossSpec}) arrives on the level clock; in its arena
 * section the clock halts at the section's end while it lives, and jumps to that end when it dies
 * earlier (the ground scroll jumps with it), after which the scroll ramps up to the next section's
 * speed over {@link #ARENA_RAMP_SECONDS}. Just before it arrives a boss checkpoint is recorded
 * (design/systems/retry): {@link #retryFromBoss()} restarts there on an empty field with the
 * defences, charges, credits, score and objective tallies of that moment.
 *
 * <p>A level with an {@code escort} primary objective has a {@link Convoy} on its road; it fails
 * when the last unit is lost (design/systems/retry, on a failed primary objective): the ship flies
 * on but nothing can hurt it, and the presentation retries the level as after a wreck.
 */
public final class Sortie {
    private static final int GROUND_CAPACITY = 32;
    private static final int PICKUP_CAPACITY = 64;
    private static final int EVENT_CAPACITY = 256;
    private static final int DEBRIS_CAPACITY = 48;
    /** Thrown rocks' serials start here, above the level's placed chunks. */
    private static final int DEBRIS_THROWN = 1 << 20;

    private final SplitMix64 rng;
    private final Ship ship;
    private final LevelScript script;
    private final Rules rules;
    private final PlayerFire fire;
    private final SpecialSlot special;
    private final EnemyForce force;
    private final Pool<GroundObject> ground = new Pool<>(GROUND_CAPACITY, GroundObject::new, GroundObject[]::new);
    private final Pool<Pickup> pickups = new Pool<>(PICKUP_CAPACITY, Pickup::new, Pickup[]::new);
    private final Pool<Debris> debris = new Pool<>(DEBRIS_CAPACITY, Debris::new, Debris[]::new);
    private final SetPiece[] setPieces;
    /** What the bosses do to the level. */
    private final SetPiece.BossActions bossActions;
    /** The boss streams' units, planned once: per stream, from the left and from the right. */
    private final BossSpec.Stream[] streams;

    private final Spawn[] streamFromLeft;
    private final Spawn[] streamFromRight;
    /** The boss whose arrival records the checkpoint (the first boss); -1 without one. */
    private final int checkpointBoss;
    /** The arena section's end and start, steps; -1 without an arena. */
    private final int arenaEndTicks;

    private final int arenaStartTicks;
    private final double arenaSpeed;
    /** The boss checkpoint; null in a level without a boss. */
    private final Checkpoint checkpoint;
    /** What the guns and the Airstrike destroy is handed here. */
    private final PlayerFire.Hits hits;
    /** Per secret, the triggers spent that reveal it together (Level 03's lifeboat lights). */
    private final int[] secretTriggersSpent;

    private final SimEvents events = new SimEvents(EVENT_CAPACITY);
    private final Tally tally;
    private final Objectives objectives;
    private final Radio radio;
    private final Crane[] cranes;
    /** The mass-driver sleds; null without them. */
    private final Sled sled;
    /** {@link #sled} for the presentation, made once (stepping allocates nothing). */
    private final java.util.Optional<Sled> sledView;
    /** The chunk a thrown rock is; null in a level without rocks. */
    private final LevelScript.DebrisSpec rock;
    /** Rocks thrown in this attempt, for their serials. */
    private int rocksThrown;
    /** Per secret, its data core when it is one (Level 06's settlement log); null for a crate. */
    private final LevelResult.DataCore[] cores;
    /** Per secret, whether its data core was collected in this attempt. */
    private final boolean[] coresCollected;
    /** Per secret, its name, for the core's radio line. */
    private final String[] secretNames;
    /** The level's darkness; null in a lit level. */
    private final LevelScript.Darkness darkness;
    /** Part G: the tows (Level 07's lifeboat). */
    private final Tow[] tows;
    /** Part G: per part drop of the script, the index of its set piece; -1 when the level has none of that slug. */
    private final int[] partDropPiece;
    /** Part G: the set piece of a parts objective (Level 07's carrier); -1 without one. */
    private final int partsPiece;

    /** The damage of the lobs that landed on the ship in this step. */
    private double directHits;
    /** Whether a group's outcome was paid and called in this attempt. */
    private final boolean[] groupCalled;
    /** The escort objective's convoy; null without one. */
    private final Convoy convoy;

    private final int launchTicks;
    private final int endTicks;
    private final int pickupTicks;
    private long tick;
    private int levelTick;
    private int attempt = 1;
    private boolean wrecked;
    /** Whether the primary objective failed in this attempt (the convoy is lost, a battery got away). */
    private boolean failed;
    /** The destroy-targets group whose loss failed the primary; -1 for none. */
    private int failedGroup = -1;
    /** What the convoy earned at the level end, for the debrief. */
    private int escortCredits;
    /** The armour of the next attempt, once {@link #retry(double)} asked for one; 0 while none is asked for. */
    private double retryArmour;

    private double groundScroll;
    private int nextGroundObject;
    private int nextDebris;
    private int edgeWarnings;
    private boolean complete;
    /** Boss stream units let in in this attempt (they count among the enemies). */
    private int streamReleased;
    /** Whether this attempt recorded the boss checkpoint. */
    private boolean checkpointTaken;
    /** Whether the next step restarts at the boss checkpoint. */
    private boolean bossRetry;
    /** Whether the next step jumps the clock to the arena's end (the boss died early). */
    private boolean arenaJump;
    /** The scroll ramps from this speed ... */
    private double rampFrom;
    /** ... for this many steps so far; -1 without a ramp. */
    private int rampTicks = -1;
    /** The phase a boss entered in this step, for its event; -1 for none. */
    private int bossPhase = -1;

    /**
     * The state recorded just before the boss arrives (design/systems/retry, the boss checkpoint),
     * allocated with the sortie.
     */
    private final class Checkpoint {
        long rng;
        final Tally tally = new Tally(rules.scoring());
        final Objectives objectives = new Objectives(
                script.secondary(),
                script.groups().size(),
                force.kinds().size(),
                enemyTotal(),
                script.groundUnits(),
                0);
        final boolean[] radioFired = new boolean[radio.size()];
        final int[] secretTriggersSpent = new int[Sortie.this.secretTriggersSpent.length];
        final boolean[] groupCalled = new boolean[Sortie.this.groupCalled.length];
        final int[] towHits = new int[tows.length];
        final int[] towCuts = new int[tows.length];
        final boolean[] towCrates = new boolean[tows.length];
        double shield;
        double armour;
        double armourLost;
        int charges;
        int chargesUsed;
        int chargesFound;
        int scheduleNext;
        int nextGround;
        int spawned;
        int nextGroundObject;
        int nextDebris;
        double groundScroll;
        int levelTick;
        int streamReleased;
    }

    /** @param armour the ship's armour at the level start (design/systems/retry: not full, unless it was full) */
    public Sortie(long seed, Loadout loadout, LevelScript script, Rules rules, double armour) {
        if (rules.bulletBudget() > EnemyForce.BULLET_CAPACITY) {
            throw new IllegalArgumentException("the bullet budget exceeds the pool of " + EnemyForce.BULLET_CAPACITY);
        }
        rng = new SplitMix64(seed);
        ship = new Ship(
                loadout.ship(),
                new Defences(loadout.shield(), loadout.plating(), loadout.ship().mercySeconds()));
        this.script = script;
        this.rules = rules;
        setPieces = script.setPieces().stream().map(SetPiece::new).toArray(SetPiece[]::new);
        List<BossSpec.Stream> bossStreams = script.setPieces().stream()
                .flatMap(piece -> piece.boss().stream())
                .flatMap(boss -> boss.phases().stream())
                .flatMap(phase -> phase.stream().stream())
                .toList();
        streams = bossStreams.toArray(BossSpec.Stream[]::new);
        int firstBoss = -1;
        for (int k = setPieces.length - 1; k >= 0; k--) {
            firstBoss = setPieces[k].boss().isPresent() ? k : firstBoss;
        }
        checkpointBoss = firstBoss;
        int arenaEnd = -1;
        int arenaStart = -1;
        double arenaSpeedFound = 0;
        for (int i = 0; i < script.sections().size(); i++) {
            if (script.sections().get(i).arena()) {
                arenaStart =
                        i == 0 ? 0 : SimStep.ticks(script.sections().get(i - 1).end());
                arenaEnd = SimStep.ticks(script.sections().get(i).end());
                arenaSpeedFound = script.sections().get(i).speed();
            }
        }
        arenaEndTicks = arenaEnd;
        arenaStartTicks = arenaStart;
        arenaSpeed = arenaSpeedFound;
        secretTriggersSpent = new int[script.secrets()];
        cores = new LevelResult.DataCore[script.secrets()];
        coresCollected = new boolean[script.secrets()];
        secretNames = new String[script.secrets()];
        for (LevelScript.GroundObjectSpec object : script.groundObjects()) {
            if (object.secretIndex() >= 0) {
                secretNames[object.secretIndex()] = object.secret();
                cores[object.secretIndex()] = object.core().orElse(null);
            }
        }
        darkness = script.darkness().orElse(null);
        tows = script.tows().stream().map(Tow::new).toArray(Tow[]::new);
        partDropPiece = new int[script.partDrops().size()];
        for (int d = 0; d < partDropPiece.length; d++) {
            partDropPiece[d] = pieceOf(script.partDrops().get(d).slug());
        }
        partsPiece = script.secondary().byParts() ? pieceOf(script.secondary().partsOf()) : -1;
        PlayerFire.Hits hits = new PlayerFire.Hits() {
            @Override
            public void enemyDestroyed(int index) {
                destroy(index);
            }

            @Override
            public void groundDestroyed(int index) {
                demolish(index);
            }

            @Override
            public void triggerReleased(int index) {
                released(ground.get(index));
            }

            @Override
            public void mineDestroyed(int index) {
                shootMine(index);
            }

            @Override
            public void partDestroyed(int piece, int part) {
                wreck(piece, part);
            }
        };
        this.hits = hits;
        fire = new PlayerFire(ship, loadout.armament(), events, hits, setPieces);
        special = new SpecialSlot(loadout.special(), events, ship.defences());
        // The boss streams' units and (part G) the units its windows launch are enemy kinds of the level.
        List<EnemySpec> streamKinds = java.util.stream.Stream.concat(
                        bossStreams.stream().map(BossSpec.Stream::enemy),
                        script.setPieces().stream()
                                .flatMap(piece -> piece.boss().stream())
                                .flatMap(boss -> boss.spawnKinds().stream()))
                .distinct()
                .toList();
        force = new EnemyForce(
                script.waves(), script.groundUnits(), streamKinds, rng, rules, events, new EnemyForce.Escapes() {
                    @Override
                    public void escaped(Enemy enemy) {
                        Sortie.this.escaped(enemy);
                    }

                    @Override
                    public void burst(Enemy enemy) {
                        selfBurst(enemy);
                    }
                });
        streamFromLeft = new Spawn[streams.length];
        streamFromRight = new Spawn[streams.length];
        for (int i = 0; i < streams.length; i++) {
            int kind = force.kinds().indexOf(streams[i].enemy());
            streamFromLeft[i] = Formations.streamUnit(streams[i].enemy(), kind, true);
            streamFromRight[i] = Formations.streamUnit(streams[i].enemy(), kind, false);
        }
        bossActions = new SetPiece.BossActions() {
            @Override
            public void aimed(double x, double y, EnemyGun gun) {
                force.fireFrom(x, y, gun, ship);
            }

            @Override
            public void ring(double x, double y, int count, double speed, double damage) {
                force.burst(x, y, count, speed, damage, ship);
                events.add(SimEvents.Type.ENEMY_FIRED, x, y);
            }

            @Override
            public void bullet(double x, double y, double angle, double speed, double damage) {
                force.fireAngle(x, y, angle, speed, damage, ship);
            }

            @Override
            public void release(BossSpec.Stream stream, boolean left) {
                for (int i = 0; i < streams.length; i++) {
                    if (streams[i] == stream) {
                        force.release(left ? streamFromLeft[i] : streamFromRight[i]);
                        streamReleased++;
                        return;
                    }
                }
            }

            @Override
            public void phase(int phase) {
                bossPhase = phase;
            }

            @Override
            public void launch(BossSpec.Spawn spawn, double x, double y, double angle) {
                // A window's unit counts among the level's enemies, as a stream's does.
                force.launch(spawn.enemy(), x, y, angle, spawn.speed(), spawn.glideSeconds());
                streamReleased++;
                events.add(SimEvents.Type.BOSS_LAUNCHED, x, y, force.kinds().indexOf(spawn.enemy()));
            }
        };
        tally = new Tally(rules.scoring());
        int escapers = force.unitsOf(script.secondary().escapes());
        for (String slug : script.secondary().killAll()) {
            escapers += force.unitsOf(slug);
        }
        for (SetPiece piece : setPieces) {
            escapers += script.secondary().counts(piece.slug()) ? 1 : 0;
        }
        escapers += partsPiece >= 0 ? script.secondary().parts().size() : 0;
        objectives = new Objectives(
                script.secondary(),
                script.groups().size(),
                force.kinds().size(),
                enemyTotal(),
                script.groundUnits(),
                escapers);
        cranes = script.cranes().stream().map(Crane::new).toArray(Crane[]::new);
        sled = script.sled().map(Sled::new).orElse(null);
        sledView = java.util.Optional.ofNullable(sled);
        rock = script.rocks().map(LevelScript.RockSpec::chunk).orElse(null);
        groupCalled = new boolean[script.groups().size()];
        radio = new Radio(script.radio(), events, ship, Radio.fitted(loadout.armament(), special));
        convoy = script.escort()
                .map(escort -> new Convoy(escort, script.road().orElseThrow()))
                .orElse(null);
        if (convoy != null) {
            force.hook(convoy, convoy.escort().targetedBy());
        }
        checkpoint = checkpointBoss < 0 ? null : new Checkpoint();
        launchTicks = SimStep.ticks(script.launchSeconds());
        endTicks = SimStep.ticks(script.seconds());
        pickupTicks = SimStep.ticks(rules.pickups().seconds());
        startAttempt(armour);
    }

    /** The index of the set piece {@code slug} among the level's; -1 for none. */
    private int pieceOf(String slug) {
        for (int k = 0; k < setPieces.length; k++) {
            if (setPieces[k].slug().equals(slug)) {
                return k;
            }
        }
        return -1;
    }

    /** Advances the sortie by one step with the given {@link Command} set. */
    public void step(int commands) {
        tick++;
        events.clear();
        if (retryArmour > 0) {
            restart();
        } else if (bossRetry) {
            restoreCheckpoint();
        }
        if (checkpoint != null && !checkpointTaken && levelTick + 1 == arrivalTicks()) {
            recordCheckpoint();
        }
        double scrollStep;
        if (arenaJump) {
            // The boss died before the arena's end: the next section starts now.
            arenaJump = false;
            groundScroll += (arenaEndTicks - 1 - levelTick) * arenaSpeed * SimStep.SECONDS;
            levelTick = arenaEndTicks - 1;
            rampFrom = arenaSpeed;
            rampTicks = 0;
        }
        if (clockHeld()) {
            scrollStep = 0;
            rampFrom = 0;
            rampTicks = 0;
        } else {
            scrollStep = scrollSpeed() * SimStep.SECONDS;
            if (rampTicks >= 0 && ++rampTicks >= SimStep.ticks(ARENA_RAMP_SECONDS)) {
                rampTicks = -1;
            }
            levelTick++;
        }
        groundScroll += scrollStep;
        ship.rememberPosition();
        if (launching()) {
            ship.launch((double) levelTick / launchTicks);
        } else if (flying()) {
            ship.fly(commands);
            fire.fire(commands, force.enemies(), ground);
        }
        special.command(commands, !launching() && flying() && !complete, ship.x(), ship.y());
        tally.step(chainTargets());
        if (!complete) {
            force.spawn(levelTick);
            placeGroundObjects();
            placeDebris();
            radio.byTime(levelTick);
        }
        for (Crane crane : cranes) {
            crane.update(levelTick);
        }
        for (Tow tow : tows) {
            tow.update(levelTick);
            if (tow.releaseCrate()) {
                dropTowCrate(tow);
            }
        }
        if (sled != null) {
            updateSled();
        }
        flySetPieces();
        edgeWarnings = complete ? 0 : force.warnings(levelTick);
        fire.move(scrollStep, force.enemies());
        boolean firing = flying() && !complete;
        force.move(ship, firing, scrollStep);
        fireSetPieces(firing);
        force.moveBullets();
        force.moveMines(ship);
        directHits = force.moveLobs(ship, scrollStep);
        moveDebris();
        blockShots();
        scrollGround(scrollStep);
        driftPickups();
        fire.hitEnemies(force.enemies());
        fire.hitSetPieces();
        fire.hitMines(force.mines());
        if (darkness != null) {
            light();
        }
        fire.hitGround(ground, force.enemies());
        special.update(scrollStep, force.enemies(), ground, setPieces, hits);
        special.bomb(force.enemies(), setPieces, hits, force.bullets(), force.lobs(), force.mines());
        if (convoy != null) {
            convoy.update(levelTick, groundScroll, scrollStep);
        }
        if (flying() && !complete && !failed && !rules.invulnerableShip()) {
            hitShip();
            ramShip();
            hitByCranes();
            hitBySled();
            hitByDebris();
            hitBySetPieces();
        }
        if (convoy != null && !complete && !wrecked && !failed) {
            hitAllies();
        }
        if (flying()) {
            collectPickups();
        }
        if (!complete && !wrecked && !failed && levelTick >= endTicks) {
            completeLevel();
        }
    }

    /**
     * Restarts the level at the next step from its start state, after the ship's destruction or from
     * the pause menu (design/systems/retry): a new attempt with {@code armour} points of armour, and
     * what this one earned is lost.
     */
    public void retry(double armour) {
        if (!(armour > 0)) {
            throw new IllegalArgumentException("a retry needs armour");
        }
        retryArmour = armour;
    }

    private void startAttempt(double armour) {
        ship.reset(armour);
        if (launchTicks > 0) {
            ship.launch(0);
            ship.rememberPosition();
        }
    }

    /** When the checkpoint boss arrives, steps on the level clock; -1 without one. */
    private int arrivalTicks() {
        return SimStep.ticks(setPieces[checkpointBoss].boss().orElseThrow().arriveSeconds());
    }

    /**
     * Whether the level clock halts: at the end of the arena section while the boss that fights
     * there lives (the scroll stops until it dies).
     */
    private boolean clockHeld() {
        if (arenaEndTicks < 0 || levelTick + 1 < arenaEndTicks || levelTick >= arenaEndTicks) {
            return false;
        }
        for (SetPiece piece : setPieces) {
            if (piece.boss().isPresent() && piece.present() && !piece.destroyed()) {
                return true;
            }
        }
        return false;
    }

    /** Whether the level clock is halted in the arena now (the boss outlived its arena). */
    public boolean arenaHalted() {
        return clockHeld();
    }

    /**
     * Restarts at the boss checkpoint at the next step (design/systems/retry, Retry from boss): a
     * new attempt with the defences, charges, credits, score and objective tallies recorded just
     * before the boss arrived, on an empty field; the boss arrives again.
     */
    public void retryFromBoss() {
        if (!bossCheckpoint()) {
            throw new IllegalStateException("no boss checkpoint in this attempt");
        }
        bossRetry = true;
    }

    /** Whether this attempt has reached a boss checkpoint, so {@link #retryFromBoss()} can restart there. */
    public boolean bossCheckpoint() {
        return checkpointTaken;
    }

    private void recordCheckpoint() {
        Checkpoint c = checkpoint;
        c.rng = rng.state();
        c.tally.copyFrom(tally);
        c.objectives.copyFrom(objectives);
        radio.saveFired(c.radioFired);
        System.arraycopy(secretTriggersSpent, 0, c.secretTriggersSpent, 0, secretTriggersSpent.length);
        System.arraycopy(groupCalled, 0, c.groupCalled, 0, groupCalled.length);
        for (int i = 0; i < tows.length; i++) {
            c.towHits[i] = tows[i].hitsLeft();
            c.towCuts[i] = tows[i].cutTick();
            c.towCrates[i] = tows[i].crateDue();
        }
        Defences defences = ship.defences();
        c.shield = defences.shield();
        c.armour = defences.armour();
        c.armourLost = defences.armourLost();
        c.charges = special.charges();
        c.chargesUsed = special.used();
        c.chargesFound = special.found();
        c.scheduleNext = force.scheduleNext();
        c.nextGround = force.nextGround();
        c.spawned = force.spawned();
        c.nextGroundObject = nextGroundObject;
        c.nextDebris = nextDebris;
        c.groundScroll = groundScroll;
        c.levelTick = levelTick;
        c.streamReleased = streamReleased;
        checkpointTaken = true;
    }

    private void restoreCheckpoint() {
        Checkpoint c = checkpoint;
        bossRetry = false;
        fire.reset();
        special.restore(c.charges, c.chargesUsed, c.chargesFound);
        force.restore(c.scheduleNext, c.nextGround, c.spawned);
        Pools.clear(ground);
        Pools.clear(pickups);
        Pools.clear(debris);
        for (SetPiece piece : setPieces) {
            piece.reset();
        }
        rng.state(c.rng);
        System.arraycopy(c.secretTriggersSpent, 0, secretTriggersSpent, 0, secretTriggersSpent.length);
        tally.copyFrom(c.tally);
        objectives.copyFrom(c.objectives);
        radio.restoreFired(c.radioFired);
        for (Crane crane : cranes) {
            crane.reset();
        }
        System.arraycopy(c.groupCalled, 0, groupCalled, 0, groupCalled.length);
        levelTick = c.levelTick;
        for (int i = 0; i < tows.length; i++) {
            tows[i].restore(c.towHits[i], c.towCuts[i], c.towCrates[i], levelTick);
        }
        groundScroll = c.groundScroll;
        nextGroundObject = c.nextGroundObject;
        nextDebris = c.nextDebris;
        streamReleased = c.streamReleased;
        resetArena();
        complete = false;
        wrecked = false;
        failed = false;
        failedGroup = -1;
        attempt++;
        ship.reset(c.armour);
        ship.defences().restore(c.shield, c.armour, c.armourLost);
        events.add(SimEvents.Type.SORTIE_RESTARTED, ship.x(), ship.y());
        events.add(SimEvents.Type.BOSS_RETRY, ship.x(), ship.y());
    }

    private void resetArena() {
        arenaJump = false;
        rampFrom = 0;
        rampTicks = -1;
        bossPhase = -1;
    }

    /**
     * The darkness (Level 06): a flare fired now is announced, and the triggers marked dark can be
     * hit only while the headlight or a flare lights them (shots pass them in the dark).
     */
    private void light() {
        double seconds = levelSeconds();
        List<LevelScript.Darkness.Flare> flares = darkness.flares();
        for (int i = 0; i < flares.size(); i++) {
            if (SimStep.ticks(flares.get(i).t()) == levelTick && !complete) {
                events.add(SimEvents.Type.FLARE_FIRED, flares.get(i).x(), darkness.flareY(i, seconds), i);
            }
        }
        for (int i = 0; i < ground.size(); i++) {
            GroundObject object = ground.get(i);
            if (object.spec().dark()) {
                object.shut(!darkness.lit(object.x(), object.y(), object.spec().size(), ship.x(), ship.y(), seconds));
            }
        }
    }

    /** Whether the ground object {@code object} is lit now: always outside a dark level. */
    public boolean lit(GroundObject object) {
        return darkness == null
                || darkness.lit(object.x(), object.y(), object.spec().size(), ship.x(), ship.y(), levelSeconds());
    }

    private void restart() {
        checkpointTaken = false;
        Arrays.fill(coresCollected, false);
        bossRetry = false;
        streamReleased = 0;
        resetArena();
        fire.reset();
        special.reset();
        force.reset();
        Pools.clear(ground);
        Pools.clear(pickups);
        Pools.clear(debris);
        for (SetPiece piece : setPieces) {
            piece.reset();
        }
        Arrays.fill(secretTriggersSpent, 0);
        tally.reset();
        objectives.reset();
        radio.reset();
        for (Crane crane : cranes) {
            crane.reset();
        }
        if (sled != null) {
            sled.reset();
        }
        for (Tow tow : tows) {
            tow.reset();
        }
        rocksThrown = 0;
        Arrays.fill(groupCalled, false);
        levelTick = 0;
        groundScroll = 0;
        nextGroundObject = 0;
        nextDebris = 0;
        complete = false;
        wrecked = false;
        failed = false;
        failedGroup = -1;
        escortCredits = 0;
        if (convoy != null) {
            convoy.reset();
        }
        attempt++;
        startAttempt(retryArmour);
        retryArmour = 0;
        events.add(SimEvents.Type.SORTIE_RESTARTED, ship.x(), ship.y());
    }

    private double scrollSpeed() {
        double speed = script.sections().get(section() - 1).speed();
        if (rampTicks >= 0) {
            // Out of the arena the scroll ramps up to the section's speed.
            speed = rampFrom + (speed - rampFrom) * rampTicks / SimStep.ticks(ARENA_RAMP_SECONDS);
        }
        return speed;
    }

    /** Out of the arena the scroll ramps up to the next section's speed over this time. */
    public static final double ARENA_RAMP_SECONDS = 1;

    private void placeGroundObjects() {
        List<LevelScript.GroundObjectSpec> objects = script.groundObjects();
        while (nextGroundObject < objects.size()
                && SimStep.ticks(objects.get(nextGroundObject).t()) <= levelTick) {
            GroundObject object = ground.obtain();
            if (object != null) {
                object.place(objects.get(nextGroundObject), nextGroundObject);
            }
            nextGroundObject++;
        }
    }

    /**
     * Lets in the debris chunks due now, each at its x unless that is closer to the ship than its
     * clearance: then it enters as far to the side, away from the ship.
     */
    private void placeDebris() {
        List<LevelScript.DebrisSpec> specs = script.debris();
        while (nextDebris < specs.size() && SimStep.ticks(specs.get(nextDebris).t()) <= levelTick) {
            Debris chunk = debris.obtain();
            if (chunk != null) {
                LevelScript.DebrisSpec spec = specs.get(nextDebris);
                chunk.place(spec, nextDebris, clearOfShip(spec));
            }
            nextDebris++;
        }
    }

    /** Where a chunk enters: its x, or moved sideways away from the ship to its clearance. */
    private double clearOfShip(LevelScript.DebrisSpec spec) {
        double dy = PlayField.HEIGHT + spec.size().height() / 2 - ship.y();
        double dx = spec.x() - ship.x();
        double clearance = spec.clearance();
        if (dx * dx + dy * dy >= clearance * clearance) {
            return spec.x();
        }
        double shift = Math.sqrt(clearance * clearance - dy * dy);
        double half = spec.size().width() / 2;
        double away = Math.clamp(ship.x() + (dx >= 0 ? shift : -shift), half, PlayField.WIDTH - half);
        if (Math.abs(away - ship.x()) >= shift) {
            return away;
        }
        return Math.clamp(ship.x() + (dx >= 0 ? -shift : shift), half, PlayField.WIDTH - half);
    }

    private void moveDebris() {
        for (int i = debris.size() - 1; i >= 0; i--) {
            if (!debris.get(i).move()) {
                debris.free(i);
            }
        }
    }

    /**
     * The set pieces fly their passes: one reaching the player's layer is announced, one ending its
     * last pass alive has escaped (its line, and an escapes objective about it fails).
     */
    private void flySetPieces() {
        for (int k = 0; k < setPieces.length; k++) {
            SetPiece piece = setPieces[k];
            boolean onPlane = piece.onPlane();
            if (piece.boss().isPresent()) {
                piece.update(levelTick);
                if (piece.bossTicks() == 0) {
                    events.add(SimEvents.Type.BOSS_ARRIVED, piece.x(), piece.y(), k);
                } else if (piece.swayTicks() == 0) {
                    events.add(SimEvents.Type.BOSS_SETTLED, piece.x(), piece.y(), k);
                }
                continue;
            }
            if (piece.update(levelTick)) {
                events.add(SimEvents.Type.SET_PIECE_ESCAPED, piece.x(), piece.y(), k);
                if (objectives.escapeLost(piece.slug())) {
                    events.add(SimEvents.Type.OBJECTIVE_FAILED, piece.x(), piece.y());
                }
                radio.cue(LevelScript.CueTrigger.ENEMY_ESCAPED, piece.slug());
            } else if (!onPlane && piece.onPlane()) {
                events.add(SimEvents.Type.SET_PIECE_DESCENDED, piece.x(), piece.y(), k);
            }
        }
    }

    /** The set pieces' living parts fire their guns while on the player's layer. */
    private void fireSetPieces(boolean firing) {
        for (int k = 0; k < setPieces.length; k++) {
            SetPiece piece = setPieces[k];
            if (piece.boss().isPresent()) {
                bossPhase = -1;
                piece.act(ship.x(), ship.y(), bossActions, firing);
                if (bossPhase > 0) {
                    phaseEntered(k, piece);
                }
                continue;
            }
            List<LevelScript.PartSpec> parts = piece.spec().parts();
            for (int p = 0; p < parts.size(); p++) {
                if (piece.trigger(p) && firing) {
                    force.fireFrom(
                            piece.partX(p), piece.partY(p), parts.get(p).gun().orElseThrow(), ship);
                }
            }
        }
    }

    /**
     * Boss {@code k} entered phase {@link #bossPhase}: its event and line, the timeout line first
     * when the phase before ran out of time with parts it waited for alive (Level 07's "Forget the
     * sacs"), and a parts objective whose phase is over fails with one of its parts alive.
     */
    private void phaseEntered(int k, SetPiece piece) {
        BossSpec boss = piece.boss().orElseThrow();
        String name = boss.phases().get(bossPhase).name();
        events.add(SimEvents.Type.BOSS_PHASE, piece.x(), piece.y(), bossPhase);
        if (timedOut(piece, boss, bossPhase - 1)) {
            radio.cue(LevelScript.CueTrigger.BOSS_TIMEOUT, name);
        }
        radio.cue(LevelScript.CueTrigger.BOSS_PHASE, name);
        if (k == partsPiece && bossPhase > script.secondary().beforePhase() && objectives.partsSurvived()) {
            events.add(SimEvents.Type.OBJECTIVE_FAILED, piece.x(), piece.y());
        }
    }

    /**
     * Whether phase {@code phase} timed out: it ended on its timer while it also waited for parts
     * (Level 07's broadside phase with sacs alive). A purely timed phase (the overhead pass) never
     * times out in this sense.
     */
    private static boolean timedOut(SetPiece piece, BossSpec boss, int phase) {
        return piece.endedOnTimeout(phase)
                && !boss.phases().get(phase).untilParts().isEmpty();
    }

    private void scrollGround(double distance) {
        for (int i = ground.size() - 1; i >= 0; i--) {
            if (!ground.get(i).scroll(distance)) {
                ground.free(i);
            }
        }
    }

    private void driftPickups() {
        double distance = rules.pickups().driftSpeed() * SimStep.SECONDS;
        for (int i = pickups.size() - 1; i >= 0; i--) {
            if (!pickups.get(i).drift(distance)) {
                pickups.free(i);
            }
        }
    }

    /** How far to the right of a destroyed object its bonus drop lands. */
    private static final double BONUS_DROP_OFFSET = 16;

    private void demolish(int index) {
        GroundObject object = ground.get(index);
        LevelScript.GroundObjectSpec spec = object.spec();
        events.add(
                SimEvents.Type.GROUND_DESTROYED,
                object.x(),
                object.y(),
                script.groundObjects().indexOf(spec));
        tally.bounty(CreditSource.GROUND_TARGETS, spec.bounty());
        tally.scoreValue(spec.bounty());
        if (spec.drop().isPresent()) {
            drop(spec.drop().get(), object.x(), object.y());
        }
        if (spec.bonusDrop().isPresent()) {
            // Beside the first, so both show.
            drop(spec.bonusDrop().get(), object.x() + BONUS_DROP_OFFSET, object.y());
        }
        if (spec.secretIndex() >= 0) {
            // A destructible that hides a secret (Level 04's dugout) reveals it when destroyed.
            released(object);
        }
        ground.free(index);
    }

    /**
     * A trigger took its last hit: it reveals its secret, or, when several triggers reveal it
     * together, counts towards it and the last one reveals it.
     */
    private void released(GroundObject trigger) {
        LevelScript.GroundObjectSpec spec = trigger.spec();
        if (spec.secretTriggers() > 1 && ++secretTriggersSpent[spec.secretIndex()] < spec.secretTriggers()) {
            return;
        }
        revealSecret(trigger);
    }

    private void revealSecret(GroundObject trigger) {
        objectives.secretFound();
        LevelScript.GroundObjectSpec spec = trigger.spec();
        events.add(SimEvents.Type.SECRET_FOUND, trigger.x(), trigger.y());
        Pickup crate = pickups.obtain();
        if (spec.core().isPresent()) {
            // A data core: its line plays when it is collected; the pickup carries its secret's index.
            if (crate != null) {
                crate.drop(PickupType.DATA_CORE, spec.secretIndex(), trigger.x(), trigger.y(), pickupTicks);
            }
            return;
        }
        if (crate != null) {
            crate.drop(PickupType.HIDDEN_CRATE, spec.crateCredits(), trigger.x(), trigger.y(), pickupTicks);
        }
        radio.cue(LevelScript.CueTrigger.SECRET, spec.secret());
    }

    private void destroy(int index) {
        Pool<Enemy> enemies = force.enemies();
        Enemy enemy = enemies.get(index);
        EnemySpec spec = enemy.spec();
        events.add(SimEvents.Type.ENEMY_DESTROYED, enemy.x(), enemy.y(), enemy.kind());
        if (enemy.chain() != null) {
            // A segment chain counts as one enemy, its wave's head: the other parts pay their bounty only.
            boolean kill = enemy.link() == 0 && enemy.chain().original();
            if (!kill) {
                int bonus = force.firstBonusDue(enemy) ? enemy.chain().spec().tailFirstBonus() : 0;
                tally.partKill(spec.bounty() + bonus);
                force.memberDestroyed(enemy);
                enemies.free(index);
                return;
            }
            force.memberDestroyed(enemy);
        }
        tally.kill(spec.bounty(), enemy.grounded() ? CreditSource.GROUND_TARGETS : CreditSource.KILLS);
        int kills = objectives.kill(enemy.kind());
        int group = enemy.group();
        double x = enemy.x();
        double y = enemy.y();
        if (rock != null && enemy.grounded()) {
            throwRocks(x, y);
        }
        if (enemy.carried().isPresent()) {
            drop(enemy.carried().get(), enemy.x(), enemy.y());
        }
        if (spec.drop().isPresent() && kills % spec.drop().get().every() == 0) {
            drop(spec.drop().get().pickup(), enemy.x(), enemy.y());
        }
        if (spec.deathBurst().isPresent()) {
            force.deathBurst(enemy, ship);
        }
        if (spec.brood().isPresent()) {
            force.hatch(enemy, ship);
        }
        if (enemy.walking()) {
            events.add(
                    SimEvents.Type.WALKER_DOWN,
                    enemy.x(),
                    enemy.y(),
                    SimEvents.walkerValue(enemy.kind(), enemy.facing()));
        }
        enemies.free(index);
        if (kills == 1) {
            radio.cue(LevelScript.CueTrigger.FIRST_KILL, spec.slug());
        }
        if (group >= 0) {
            decided(group, objectives.groupUnitDestroyed(group), x, y);
        }
        if (objectives.escapeDestroyed(spec.slug())) {
            paySecondary();
        }
    }

    /**
     * A destroyed ground unit throws its rocks in low gravity (Level 05): a number between the
     * spec's least and most, each drifting in a random direction; none when it lies close to the ship.
     */
    private void throwRocks(double x, double y) {
        LevelScript.RockSpec spec = script.rocks().orElseThrow();
        int count = spec.min() + rng.nextInt(spec.max() - spec.min() + 1);
        double dx = ship.x() - x;
        double dy = ship.y() - y;
        if (dx * dx + dy * dy < spec.clearance() * spec.clearance()) {
            return;
        }
        for (int k = 0; k < count; k++) {
            double angle = rng.range(0, 2 * StrictMath.PI);
            double speed = rng.range(spec.minSpeed(), spec.maxSpeed());
            Debris chunk = debris.obtain();
            if (chunk != null) {
                chunk.toss(
                        rock,
                        DEBRIS_THROWN + rocksThrown,
                        x,
                        y,
                        Trig.cos(angle) * speed,
                        Trig.sin(angle) * speed,
                        SimStep.ticks(spec.lifeSeconds()));
                events.add(SimEvents.Type.ROCK_THROWN, x, y);
            }
            rocksThrown++;
        }
    }

    /** The rail's sleds: their lights, their launches, and the stuck sled's clamp that only takes hits in the dark. */
    private void updateSled() {
        SimEvents.Type event = sled.update(levelTick);
        if (event != null) {
            events.add(event, sled.spec().x(), 0);
        }
        String clamp = sled.spec().clampSecret();
        for (int i = 0; i < ground.size(); i++) {
            GroundObject object = ground.get(i);
            if (object.spec().trigger() && object.spec().secret().equals(clamp)) {
                object.shut(sled.lit());
            }
        }
    }

    /** A running sled hits the ship whatever its layer, once per sled. */
    private void hitBySled() {
        if (sled != null && sled.strikes(ship.spec().hull(), ship.x(), ship.y())) {
            events.add(SimEvents.Type.SLED_HIT, ship.x(), ship.y());
            double lost = ship.defences().armourLost();
            damaged(lost, ship.defences().takeCollision(sled.spec().damage(), events, ship.x(), ship.y()));
        }
    }

    /** A spore mine was shot: it pays its credits (no kill, no chain). */
    private void shootMine(int index) {
        Pool<Mine> mines = force.mines();
        Mine mine = mines.get(index);
        events.add(SimEvents.Type.MINE_DESTROYED, mine.x(), mine.y());
        int credits = mine.spec().credits();
        tally.bounty(CreditSource.KILLS, credits);
        tally.scoreValue(credits);
        mines.free(index);
    }

    /**
     * Part {@code part} of set piece {@code k} was destroyed: it pays its bounty; the vital part
     * takes the rest with it, paying theirs, and the unit is destroyed.
     */
    private void wreck(int k, int part) {
        SetPiece piece = setPieces[k];
        payPart(piece, part);
        shotOff(k, piece, part);
        if (!piece.spec().parts().get(part).vital()) {
            return;
        }
        for (int p = 0; p < piece.partCount(); p++) {
            if (piece.wreckPart(p)) {
                payPart(piece, p);
            }
        }
        LevelScript.SetPieceSpec spec = piece.spec();
        events.add(SimEvents.Type.SET_PIECE_DESTROYED, piece.x(), piece.y(), k);
        tally.countKill();
        if (spec.drop().isPresent()) {
            double half = EnemyGun.BULLET.width();
            drop(
                    spec.drop().get(),
                    Math.clamp(piece.x(), half, PlayField.WIDTH - half),
                    Math.clamp(piece.y(), half, PlayField.HEIGHT - half));
        }
        piece.destroy();
        radio.cue(LevelScript.CueTrigger.FIRST_KILL, spec.slug());
        if (spec.isBoss()) {
            events.add(SimEvents.Type.BOSS_DESTROYED, piece.x(), piece.y(), bossCredits(spec));
            radio.cue(LevelScript.CueTrigger.BOSS_DESTROYED, spec.slug());
            if (arenaEndTicks >= 0 && levelTick >= arenaStartTicks && levelTick < arenaEndTicks - 1) {
                arenaJump = true;
            }
        }
        if (objectives.escapeDestroyed(spec.slug())) {
            paySecondary();
        }
        if (k == partsPiece && objectives.partsSurvived()) {
            events.add(SimEvents.Type.OBJECTIVE_FAILED, piece.x(), piece.y());
        }
    }

    /**
     * Part {@code part} of set piece {@code k} was shot off (not lost in the unit's death): the
     * pickup of a part drop whose turn it is falls where it broke (Level 07's first bay sac), and a
     * parts objective counts it.
     */
    private void shotOff(int k, SetPiece piece, int part) {
        List<LevelScript.PartDrop> drops = script.partDrops();
        for (int d = 0; d < drops.size(); d++) {
            LevelScript.PartDrop drop = drops.get(d);
            if (partDropPiece[d] == k && among(drop.parts(), part) && wrecked(piece, drop.parts()) == drop.dropsAt()) {
                double half = EnemyGun.BULLET.width();
                drop(
                        drop.pickup(),
                        Math.clamp(piece.partX(part), half, PlayField.WIDTH - half),
                        Math.clamp(piece.partY(part), half, PlayField.HEIGHT - half));
            }
        }
        if (k == partsPiece && among(script.secondary().parts(), part) && objectives.partShotOff()) {
            paySecondary();
        }
    }

    private static boolean among(List<Integer> parts, int part) {
        for (int i = 0; i < parts.size(); i++) {
            if (parts.get(i) == part) {
                return true;
            }
        }
        return false;
    }

    /** How many of {@code parts} of {@code piece} are wrecked. */
    private static int wrecked(SetPiece piece, List<Integer> parts) {
        int wrecked = 0;
        for (int i = 0; i < parts.size(); i++) {
            wrecked += piece.partWrecked(parts.get(i)) ? 1 : 0;
        }
        return wrecked;
    }

    /** What a boss's parts paid together, after the credit factor: the credit shower's number. */
    private int bossCredits(LevelScript.SetPieceSpec spec) {
        int credits = 0;
        for (LevelScript.PartSpec part : spec.parts()) {
            credits += (int) Math.rint(part.bounty() * rules.scoring().creditFactor());
        }
        return credits;
    }

    private void payPart(SetPiece piece, int part) {
        tally.partKill(piece.spec().parts().get(part).bounty());
        events.add(SimEvents.Type.PART_DESTROYED, piece.partX(part), piece.partY(part), part);
    }

    /** The secondary objective is met: its credits, its event and its line. */
    private void paySecondary() {
        int credits = script.secondary().credits();
        tally.earn(CreditSource.OBJECTIVES, credits);
        tally.scoreValue(credits);
        events.add(SimEvents.Type.OBJECTIVE_MET, ship.x(), ship.y());
        radio.cue(LevelScript.CueTrigger.SECONDARY_OBJECTIVE, "");
    }

    /**
     * A spawner burst on its own (design/enemies/air/brood-pod): it pays its burst bounty, which is
     * not a kill (no kill counter, no chain), and counts as an escape for an escapes objective.
     */
    private void selfBurst(Enemy enemy) {
        int credits = enemy.spec().brood().orElseThrow().burstBounty();
        tally.unchained(credits);
        events.add(SimEvents.Type.BROOD_BURST, enemy.x(), enemy.y(), enemy.kind());
        escaped(enemy);
    }

    /**
     * A unit left the screen alive: a ground group's unit can lose its group, one of an escapes
     * objective's enemy fails it, and the first of an enemy calls its line.
     */
    private void escaped(Enemy enemy) {
        String slug = enemy.spec().slug();
        if (enemy.group() >= 0) {
            int group = enemy.group();
            int state = objectives.groupUnitEscaped(group);
            if (!script.targets().isEmpty()) {
                // A battery whose unit got away can no longer be destroyed: the mission fails at once.
                failTargets(group);
            } else {
                decided(group, state, enemy.x(), enemy.y());
            }
        }
        if (objectives.escapeLost(slug)) {
            events.add(SimEvents.Type.OBJECTIVE_FAILED, enemy.x(), enemy.y());
        }
        radio.cue(LevelScript.CueTrigger.ENEMY_ESCAPED, slug);
    }

    /**
     * A group's outcome once it is decided: a cleared group pays its credits and calls its line,
     * the objective is met when every group is cleared; a lost group calls its line, and the first
     * lost one also the first-loss line.
     */
    private void decided(int group, int state, double x, double y) {
        String name = script.groups().get(group);
        if (state == Objectives.CLEARED && !groupCalled[group]) {
            groupCalled[group] = true;
            if (script.targets().isEmpty()) {
                int credits = script.secondary().credits();
                tally.earn(CreditSource.OBJECTIVES, credits);
                tally.scoreValue(credits);
            }
            for (LevelScript.GroupDrop drop : script.groupDrops()) {
                if (drop.group() == group) {
                    drop(drop.pickup(), x, y);
                }
            }
            events.add(SimEvents.Type.GROUP_CLEARED, ship.x(), ship.y(), group);
            radio.cue(LevelScript.CueTrigger.GROUP_CLEARED, name);
            if (objectives.secondaryMet()) {
                events.add(SimEvents.Type.OBJECTIVE_MET, ship.x(), ship.y());
                radio.cue(LevelScript.CueTrigger.SECONDARY_OBJECTIVE, "");
            }
        } else if (state == Objectives.LOST && !groupCalled[group]) {
            groupCalled[group] = true;
            events.add(SimEvents.Type.GROUP_LOST, ship.x(), ship.y(), group);
            radio.cue(LevelScript.CueTrigger.GROUP_LOST, name);
            if (objectives.groupsLost() == 1) {
                radio.cue(LevelScript.CueTrigger.FIRST_GROUP_LOST, "");
            }
        }
    }

    /**
     * A unit of a destroy-targets group left the screen alive: the group is lost and the primary
     * objective fails at once (design/systems/retry, on a failed primary objective), with the
     * level's line naming the group.
     */
    private void failTargets(int group) {
        if (failed || complete || wrecked || groupCalled[group]) {
            return;
        }
        groupCalled[group] = true;
        events.add(SimEvents.Type.GROUP_LOST, ship.x(), ship.y(), group);
        if (rules.invulnerableShip()) {
            // The debug option that lets a capture see the level to its end keeps it going too.
            return;
        }
        failed = true;
        failedGroup = group;
        events.add(SimEvents.Type.PRIMARY_FAILED, ship.x(), ship.y(), group);
        radio.cue(LevelScript.CueTrigger.MISSION_FAILED, "");
    }

    private void drop(PickupType type, double x, double y) {
        if (type == PickupType.SPECIAL_CHARGE && !special.fitted()) {
            return;
        }
        Pickup pickup = pickups.obtain();
        if (pickup != null) {
            int credits =
                    switch (type) {
                        case SMALL_SALVAGE -> rules.pickups().smallSalvageCredits();
                        case MEDIUM_SALVAGE -> rules.pickups().mediumSalvageCredits();
                        case LARGE_SALVAGE -> rules.pickups().largeSalvageCredits();
                        default -> 0;
                    };
            pickup.drop(type, credits, x, y, pickupTicks);
        }
    }

    /** The cranes' arms stop every shot that touches them; the player's hits on a swinging arm's clamp count. */
    private void blockShots() {
        Pool<Shot> shots = fire.shots();
        Pool<EnemyBullet> bullets = force.bullets();
        if (sled != null && sled.running()) {
            // A running sled stops every shot and bullet that crosses the rail.
            for (int i = shots.size() - 1; i >= 0; i--) {
                Shot shot = shots.get(i);
                if (!shot.weapon().delivery().landing()
                        && sled.blocks(shot.x(), shot.y(), shot.weapon().size())) {
                    events.add(SimEvents.Type.SHOT_GLANCED, shot.x(), shot.y(), shot.mount());
                    shots.free(i);
                }
            }
            for (int i = bullets.size() - 1; i >= 0; i--) {
                EnemyBullet bullet = bullets.get(i);
                if (sled.blocks(bullet.x(), bullet.y(), EnemyGun.BULLET)) {
                    bullets.free(i);
                }
            }
        }
        for (Crane crane : cranes) {
            if (!crane.present()) {
                continue;
            }
            for (int i = shots.size() - 1; i >= 0; i--) {
                Shot shot = shots.get(i);
                Hitbox size = shot.weapon().size();
                if (shot.weapon().delivery().landing()) {
                    continue;
                }
                if (crane.clampHit(shot.x(), shot.y(), size)) {
                    events.add(SimEvents.Type.CLAMP_HIT, shot.x(), shot.y(), shot.mount());
                    shots.free(i);
                    if (crane.countClampHit()) {
                        releaseCrate(crane);
                    }
                } else if (crane.touches(shot.x(), shot.y(), size.width() / 2, size.height() / 2)) {
                    events.add(SimEvents.Type.SHOT_GLANCED, shot.x(), shot.y(), shot.mount());
                    shots.free(i);
                }
            }
            for (int i = bullets.size() - 1; i >= 0; i--) {
                EnemyBullet bullet = bullets.get(i);
                if (crane.touches(bullet.x(), bullet.y(), EnemyGun.BULLET.width() / 2, EnemyGun.BULLET.height() / 2)) {
                    bullets.free(i);
                }
            }
        }
        hitTows(shots);
        blockByDebris(shots, bullets);
    }

    /**
     * Part G: the tows' cables take the player's shots that touch them while they hold their pods;
     * the last hit cuts one and its pod falls loose with its secret's crate. Every other shot and
     * bullet passes the boats and the pods.
     */
    private void hitTows(Pool<Shot> shots) {
        for (Tow tow : tows) {
            if (!tow.present() || !tow.holding()) {
                continue;
            }
            for (int i = shots.size() - 1; i >= 0; i--) {
                Shot shot = shots.get(i);
                if (shot.weapon().delivery().landing()
                        || !tow.cableHit(shot.x(), shot.y(), shot.weapon().size())) {
                    continue;
                }
                events.add(SimEvents.Type.CLAMP_HIT, shot.x(), shot.y(), shot.mount());
                shots.free(i);
                if (tow.countHit(levelTick)) {
                    cutTow(tow);
                    break;
                }
            }
        }
    }

    /**
     * A tow's cable was cut: the secret is found and its pod falls loose ({@link Tow}); the crate
     * falls out of it at once if the pod is already down at the ship.
     */
    private void cutTow(Tow tow) {
        objectives.secretFound();
        events.add(SimEvents.Type.SECRET_FOUND, towPodX(tow), towPodY(tow));
        radio.cue(LevelScript.CueTrigger.SECRET, tow.spec().secret());
        if (tow.releaseCrate()) {
            dropTowCrate(tow);
        }
    }

    /** The crate falls out of a tow's loose pod, an ordinary hidden crate from here on. */
    private void dropTowCrate(Tow tow) {
        Pickup crate = pickups.obtain();
        if (crate != null) {
            crate.drop(PickupType.HIDDEN_CRATE, tow.spec().crateCredits(), towPodX(tow), towPodY(tow), pickupTicks);
        }
    }

    /** A tow's pod's centre kept a bullet's width inside the play field. */
    private static double towPodX(Tow tow) {
        double half = EnemyGun.BULLET.width();
        return Math.clamp(tow.podX(), half, PlayField.WIDTH - half);
    }

    private static double towPodY(Tow tow) {
        double half = EnemyGun.BULLET.width();
        return Math.clamp(tow.podY(), half, PlayField.HEIGHT - half);
    }

    /**
     * The debris chunks stop every shot and enemy bullet that touches them: shots glance off a
     * large chunk and damage a small one, which breaks after its HP.
     */
    private void blockByDebris(Pool<Shot> shots, Pool<EnemyBullet> bullets) {
        for (int d = debris.size() - 1; d >= 0; d--) {
            Debris chunk = debris.get(d);
            boolean broken = false;
            for (int i = shots.size() - 1; i >= 0; i--) {
                Shot shot = shots.get(i);
                if (shot.weapon().delivery().landing()
                        || !chunk.touches(shot.x(), shot.y(), shot.weapon().size())) {
                    continue;
                }
                events.add(
                        chunk.large() ? SimEvents.Type.SHOT_GLANCED : SimEvents.Type.DEBRIS_HIT,
                        shot.x(),
                        shot.y(),
                        shot.mount());
                broken = chunk.damage(shot.damage());
                shots.free(i);
                if (broken) {
                    events.add(SimEvents.Type.DEBRIS_DESTROYED, chunk.x(), chunk.y());
                    debris.free(d);
                    break;
                }
            }
            if (broken) {
                continue;
            }
            for (int i = bullets.size() - 1; i >= 0; i--) {
                EnemyBullet bullet = bullets.get(i);
                if (chunk.touches(bullet.x(), bullet.y(), EnemyGun.BULLET)) {
                    bullets.free(i);
                }
            }
        }
    }

    /** A large debris chunk touching the hull deals its contact damage, at most once per its interval. */
    private void hitByDebris() {
        Hull hull = ship.spec().hull();
        for (int d = 0; d < debris.size(); d++) {
            Debris chunk = debris.get(d);
            LevelScript.DebrisSpec spec = chunk.spec();
            if (spec.damage() > 0
                    && hull.overlaps(ship.x(), ship.y(), spec.size(), chunk.x(), chunk.y())
                    && chunk.strike()) {
                double lost = ship.defences().armourLost();
                boolean thrown = chunk.thrown();
                if (thrown) {
                    // A thrown rock breaks on the hull.
                    events.add(SimEvents.Type.DEBRIS_DESTROYED, chunk.x(), chunk.y());
                    debris.free(d--);
                }
                if (damaged(lost, ship.defences().takeCollision(spec.damage(), events, ship.x(), ship.y()))) {
                    return;
                }
            }
        }
    }

    /** A set piece's body on the player's layer touching the hull deals its contact damage, at most once per its interval. */
    private void hitBySetPieces() {
        Hull hull = ship.spec().hull();
        for (SetPiece piece : setPieces) {
            LevelScript.SetPieceSpec spec = piece.spec();
            if (piece.present()
                    && piece.onPlane()
                    && hull.overlaps(ship.x(), ship.y(), piece.body(), piece.x(), piece.y())
                    && piece.strike()) {
                double lost = ship.defences().armourLost();
                if (damaged(lost, ship.defences().takeCollision(spec.contactDamage(), events, ship.x(), ship.y()))) {
                    return;
                }
            }
        }
    }

    /** A crane's clamp let go: its secret's crate drops from the hook. */
    private void releaseCrate(Crane crane) {
        objectives.secretFound();
        LevelScript.CraneSpec spec = crane.spec();
        events.add(SimEvents.Type.SECRET_FOUND, crane.tipX(), crane.tipY());
        Pickup crate = pickups.obtain();
        if (crate != null) {
            crate.drop(PickupType.HIDDEN_CRATE, spec.crateCredits(), crane.tipX(), crane.tipY(), pickupTicks);
        }
        radio.cue(LevelScript.CueTrigger.SECRET, spec.secret());
    }

    /** A crane's arm touching the hull deals its damage, shield first, at most once per its interval. */
    private void hitByCranes() {
        Hull hull = ship.spec().hull();
        for (Crane crane : cranes) {
            if (!crane.present()) {
                continue;
            }
            for (int p = 0; p < hull.parts().size(); p++) {
                Hull.Part part = hull.parts().get(p);
                if (crane.touches(
                                ship.x() + part.dx(),
                                ship.y() + part.dy(),
                                part.box().width() / 2,
                                part.box().height() / 2)
                        && crane.strike()) {
                    double lost = ship.defences().armourLost();
                    if (damaged(lost, ship.defences().takeShot(crane.spec().damage(), events, ship.x(), ship.y()))) {
                        return;
                    }
                    break;
                }
            }
        }
    }

    private void hitShip() {
        if (directHits > 0) {
            double lost = ship.defences().armourLost();
            if (damaged(lost, ship.defences().takeShot(directHits, events, ship.x(), ship.y()))) {
                return;
            }
        }
        Pool<EnemyBullet> bullets = force.bullets();
        Hull hull = ship.spec().hull();
        for (int i = bullets.size() - 1; i >= 0; i--) {
            EnemyBullet bullet = bullets.get(i);
            if (hull.overlaps(ship.x(), ship.y(), EnemyGun.BULLET, bullet.x(), bullet.y())) {
                bullets.free(i);
                double lost = ship.defences().armourLost();
                if (damaged(lost, ship.defences().takeShot(bullet.damage(), events, ship.x(), ship.y()))) {
                    return;
                }
            }
        }
        if (beamsHit(hull)) {
            return;
        }
        // An armed spore bursts on contact with the hull, dealing its layer's attack damage.
        Pool<Mine> mines = force.mines();
        for (int i = mines.size() - 1; i >= 0; i--) {
            Mine mine = mines.get(i);
            if (mine.armed() && hull.overlaps(ship.x(), ship.y(), EnemyGun.MineSpec.BOX, mine.x(), mine.y())) {
                double damage = mine.gun().damage();
                events.add(SimEvents.Type.MINE_BURST, mine.x(), mine.y());
                mines.free(i);
                double lost = ship.defences().armourLost();
                if (damaged(lost, ship.defences().takeShot(damage, events, ship.x(), ship.y()))) {
                    return;
                }
            }
        }
    }

    /**
     * A sweeping beam (design/enemies/air/mantis) touching the hull deals its damage, at most once
     * per sweep; returns whether it wrecked the ship.
     */
    private boolean beamsHit(Hull hull) {
        Pool<Enemy> enemies = force.enemies();
        for (int j = 0; j < enemies.size(); j++) {
            Enemy enemy = enemies.get(j);
            if (enemy.spec().sweep().isEmpty() || !enemy.sweeping() || enemy.sweepHit()) {
                continue;
            }
            EnemySpec.Sweep sweep = enemy.spec().sweep().get();
            double heading = enemy.beam(0);
            double fromX = enemy.x() + enemy.beamOffsetX();
            double fromY = enemy.y() + enemy.beamOffsetY();
            double endX = fromX - Trig.sin(heading) * sweep.length();
            double endY = fromY - Trig.cos(heading) * sweep.length();
            if (hull.touchesSegment(ship.x(), ship.y(), fromX, fromY, endX, endY, sweep.width() / 2)) {
                enemy.markSweepHit();
                events.add(SimEvents.Type.SWEEP_HIT, ship.x(), ship.y());
                double lost = ship.defences().armourLost();
                if (damaged(lost, ship.defences().takeShot(sweep.damage(), events, ship.x(), ship.y()))) {
                    return true;
                }
            }
        }
        return false;
    }

    /** A rammer on the player's layer deals its contact damage; a small one is destroyed by the impact. */
    private void ramShip() {
        Pool<Enemy> enemies = force.enemies();
        Hull hull = ship.spec().hull();
        for (int j = enemies.size() - 1; j >= 0; j--) {
            Enemy enemy = enemies.get(j);
            EnemySpec spec = enemy.spec();
            if (spec.layer().collidesWithPlayer()
                    && !Chain.doomed(enemy)
                    && hull.overlaps(ship.x(), ship.y(), enemy.hitbox(), enemy.x(), enemy.y())) {
                double lost = ship.defences().armourLost();
                boolean wrecked = ship.defences().takeCollision(spec.contactDamage(), events, ship.x(), ship.y());
                if (spec.destroyedByRamming()) {
                    destroy(j);
                }
                if (damaged(lost, wrecked)) {
                    return;
                }
            }
        }
    }

    /**
     * After a hit: armour damage (more armour lost than {@code lostBefore}) ends the chain, and a
     * hit that took the last armour wrecks the ship. Returns whether it did.
     */
    private boolean damaged(double lostBefore, boolean destroyed) {
        if (ship.defences().armourLost() > lostBefore) {
            tally.breakChain();
        }
        if (destroyed) {
            wrecked = true;
            events.add(SimEvents.Type.SHIP_DESTROYED, ship.x(), ship.y());
        }
        return destroyed;
    }

    /**
     * The convoy takes its damage (design/allies, civilian crawler): an enemy shot the
     * target-the-objective hook aimed at a unit hits the first unit it touches and is spent (shots
     * aimed at the ship pass over), and a walker (a ground unit that is not fixed to the ground)
     * claws every unit its hit box overlaps, per second.
     */
    private void hitAllies() {
        AllySpec spec = convoy.escort().ally();
        if (spec.objectiveAimed()) {
            Pool<EnemyBullet> bullets = force.bullets();
            for (int i = bullets.size() - 1; i >= 0 && !failed; i--) {
                EnemyBullet bullet = bullets.get(i);
                if (!bullet.objectiveAimed()) {
                    continue;
                }
                int k = convoy.touching(bullet.x(), bullet.y(), EnemyGun.BULLET);
                if (k >= 0) {
                    bullets.free(i);
                    hurt(k, bullet.damage(), true);
                }
            }
        }
        if (spec.clawsPerSecond() > 0) {
            double claws = spec.clawsPerSecond() * SimStep.SECONDS;
            Pool<Enemy> enemies = force.enemies();
            for (int j = 0; j < enemies.size() && !failed; j++) {
                Enemy enemy = enemies.get(j);
                if (enemy.spec().layer() != Layer.GROUND || enemy.grounded()) {
                    continue;
                }
                for (int k = 0; k < convoy.size() && !failed; k++) {
                    if (convoy.touches(k, enemy.x(), enemy.y(), enemy.hitbox())) {
                        // A new contact flashes and sounds; a continuing one only takes its damage.
                        hurt(k, claws, convoy.get(k).ticksSinceHit() > 1);
                    }
                }
            }
        }
    }

    /** Unit {@code k} of the convoy takes {@code damage}: the first hit's line, a loss's line, the failure. */
    private void hurt(int k, double damage, boolean announce) {
        Ally ally = convoy.get(k);
        boolean first = convoy.firstHit() < 0;
        boolean destroyed = convoy.damage(k, damage);
        if (announce || destroyed) {
            events.add(SimEvents.Type.ALLY_HIT, ally.x(), ally.y(), k);
        }
        if (first) {
            radio.cue(LevelScript.CueTrigger.FIRST_ALLY_HIT, "");
        }
        if (!destroyed) {
            return;
        }
        events.add(SimEvents.Type.ALLY_LOST, ally.x(), ally.y(), k);
        if (convoy.firstLost() == k && convoy.alive() == convoy.size() - 1) {
            radio.cue(LevelScript.CueTrigger.FIRST_ALLY_LOST, "");
        }
        if (convoy.allLost()) {
            failed = true;
            events.add(SimEvents.Type.PRIMARY_FAILED, ship.x(), ship.y());
            radio.cue(LevelScript.CueTrigger.MISSION_FAILED, "");
        }
    }

    private void collectPickups() {
        double radius = rules.pickups().collectionRadius();
        for (int i = pickups.size() - 1; i >= 0; i--) {
            Pickup pickup = pickups.get(i);
            double dx = pickup.x() - ship.x();
            double dy = pickup.y() - ship.y();
            if (dx * dx + dy * dy <= radius * radius) {
                apply(pickup);
                pickups.free(i);
            }
        }
    }

    private void apply(Pickup pickup) {
        Defences defences = ship.defences();
        switch (pickup.type()) {
            case SMALL_SALVAGE, MEDIUM_SALVAGE, LARGE_SALVAGE -> payPickup(CreditSource.SALVAGE, pickup);
            case OVERDRIVE -> fire.overdrive(SimStep.ticks(rules.pickups().overdriveSeconds()));
            case HIDDEN_CRATE -> payPickup(CreditSource.SECRETS, pickup);
            case SHIELD_CELL -> defences.restoreShield(rules.pickups().shieldCellShare() * defences.maxShield());
            case ARMOUR_PATCH -> defences.repair(rules.pickups().armourPatch());
            case SPECIAL_CHARGE -> special.collect();
            case DATA_CORE -> {
                coresCollected[pickup.credits()] = true;
                radio.cue(LevelScript.CueTrigger.SECRET, secretNames[pickup.credits()]);
            }
        }
        events.add(
                SimEvents.Type.PICKUP_COLLECTED,
                pickup.x(),
                pickup.y(),
                pickup.type().ordinal());
    }

    private void payPickup(CreditSource source, Pickup pickup) {
        int paid = tally.earn(source, pickup.credits());
        tally.scoreValue(pickup.credits());
        events.add(SimEvents.Type.CREDITS_PICKED_UP, pickup.x(), pickup.y(), paid);
    }

    private void completeLevel() {
        complete = true;
        events.add(SimEvents.Type.LEVEL_COMPLETE, ship.x(), ship.y());
        int home = convoy == null ? 0 : convoy.alive();
        if (convoy != null) {
            // Each unit home is a payout of its own (design/campaign, Level 04: the escort objective).
            int credits = convoy.escort().credits();
            for (int k = 0; k < home; k++) {
                escortCredits += tally.earn(CreditSource.OBJECTIVES, credits);
                tally.scoreValue(credits);
            }
        }
        // A kill-ratio secondary is judged at the end (design/systems/scoring): its line before the level-end line.
        if (objectives.meetsKillRatio(tally.kills())) {
            paySecondary();
        }
        radio.end(home);
    }

    /**
     * Whether anything that keeps the chain going is on screen (design/systems/scoring, Chain
     * multiplier): a live enemy on a layer the standard shots reach (not high air) whose hit box
     * overlaps the play field, or a set piece or boss with a part on such a layer that is on the
     * field, not wrecked and not shielded (a boss's descent). Otherwise the chain window pauses.
     */
    private boolean chainTargets() {
        Pool<Enemy> enemies = force.enemies();
        for (int i = 0; i < enemies.size(); i++) {
            Enemy enemy = enemies.get(i);
            if (enemy.spec().layer().hitByStandardShots() && PlayerFire.onField(enemy)) {
                return true;
            }
        }
        for (SetPiece piece : setPieces) {
            if (!piece.present() || !piece.layer().hitByStandardShots()) {
                continue;
            }
            List<LevelScript.PartSpec> parts = piece.spec().parts();
            for (int p = 0; p < parts.size(); p++) {
                if (!piece.partWrecked(p)
                        && !piece.partShielded(p)
                        && PlayField.overlaps(
                                piece.partX(p), piece.partY(p), parts.get(p).box())) {
                    return true;
                }
            }
        }
        return false;
    }

    /** A hash over the complete state; equal hashes mean equal replays. */
    public long stateHash() {
        StateHash hash = new StateHash()
                .add(tick)
                .add(levelTick)
                .add(attempt)
                .add(wrecked ? 1 : 0)
                .add(groundScroll)
                .add(nextGroundObject)
                .add(nextDebris)
                .add(objectives.secretsFound())
                .add(objectives.secondaryMet() ? 1 : 0)
                .add(complete ? 1 : 0)
                .add(rng.state());
        ship.addTo(hash);
        fire.addTo(hash);
        special.addTo(hash);
        tally.addTo(hash);
        objectives.addKillsTo(hash);
        for (Crane crane : cranes) {
            crane.addTo(hash);
        }
        for (Tow tow : tows) {
            tow.addTo(hash);
        }
        for (SetPiece piece : setPieces) {
            piece.addTo(hash);
        }
        for (int spent : secretTriggersSpent) {
            hash.add(spent);
        }
        Pools.addAll(hash, fire.shots());
        Pools.addAll(hash, force.enemies());
        Pools.addAll(hash, force.bullets());
        Pools.addAll(hash, ground);
        Pools.addAll(hash, pickups);
        Pools.addAll(hash, force.mines());
        Pools.addAll(hash, debris);
        if (force.lobs().size() > 0) {
            Pools.addAll(hash, force.lobs());
        }
        if (sled != null) {
            sled.addTo(hash);
        }
        if (force.chains().size() > 0) {
            Pools.addAll(hash, force.chains());
        }
        for (int k = 0; k < cores.length; k++) {
            if (cores[k] != null) {
                hash.add(coresCollected[k] ? 1 : 0);
            }
        }
        if (!script.targets().isEmpty()) {
            hash.add(failed ? 1 : 0).add(failedGroup).add(rocksThrown);
        }
        if (convoy != null) {
            hash.add(failed ? 1 : 0).add(escortCredits);
            convoy.addTo(hash);
        }
        if (checkpoint != null || arenaEndTicks >= 0) {
            hash.add(checkpointTaken ? 1 : 0)
                    .add(streamReleased)
                    .add(arenaJump ? 1 : 0)
                    .add(rampFrom)
                    .add(rampTicks);
        }
        return hash.value();
    }

    /** The totals for the debrief; meaningful once the level is {@link #complete()}. */
    public LevelResult result() {
        Defences defences = ship.defences();
        return LevelResult.of(
                        rules.scoring(),
                        script,
                        tally,
                        enemyTotal(),
                        defences.armourLost(),
                        defences.maxArmour(),
                        objectives.secretsFound(),
                        objectives.secondaryMet(),
                        convoy == null
                                ? LevelResult.Escort.NONE
                                : new LevelResult.Escort(
                                        convoy.escort().ally().slug(), convoy.alive(), convoy.size(), escortCredits),
                        bossTime())
                .withDataCores(dataCores());
    }

    /** The data cores collected in this attempt, in the level's secret order. */
    public List<LevelResult.DataCore> dataCores() {
        List<LevelResult.DataCore> collected = new java.util.ArrayList<>();
        for (int k = 0; k < cores.length; k++) {
            if (cores[k] != null && coresCollected[k]) {
                collected.add(cores[k]);
            }
        }
        return collected;
    }

    /** The level's boss's kill time and par, for the debrief and the Boss rush bonus; {@link LevelResult.BossTime#NONE} without a boss. */
    private LevelResult.BossTime bossTime() {
        if (checkpointBoss < 0) {
            return LevelResult.BossTime.NONE;
        }
        SetPiece boss = setPieces[checkpointBoss];
        return new LevelResult.BossTime(boss.boss().orElseThrow().parSeconds(), boss.killSeconds());
    }

    public LevelScript script() {
        return script;
    }

    /** The distinct enemies of the level; {@link Enemy#kind()} and enemy events index this list. */
    public List<EnemySpec> enemyKinds() {
        return force.kinds();
    }

    public Ship ship() {
        return ship;
    }

    /** The fitted weapons; shot events and {@link Shot#mount()} index its mounts. */
    public Armament armament() {
        return fire.armament();
    }

    /** Steps since mount {@code m} last fired, for its muzzle flash; {@link Integer#MAX_VALUE} before its first shot. */
    public int ticksSinceShot(int m) {
        return fire.ticksSinceShot(m);
    }

    /** The special slot: its charges, and the Airstrike in flight for the presentation. */
    public SpecialSlot special() {
        return special;
    }

    /** Seconds of overdrive left; 0 without one. */
    public double overdriveSeconds() {
        return fire.overdriveTicks() * SimStep.SECONDS;
    }

    /** Destroys enemy {@code index} as a hit would, paying it (for tests). */
    void destroyEnemy(int index) {
        destroy(index);
    }

    /** The segment chains in flight (a regrowing one draws its growing head). */
    public int chainCount() {
        return force.chains().size();
    }

    public Chain chain(int index) {
        return force.chains().get(index);
    }

    /** The level's darkness, if it is dark. */
    public java.util.Optional<LevelScript.Darkness> darkness() {
        return script.darkness();
    }

    /** Destroys part {@code part} of set piece {@code piece} as a hit would, paying it (for tests). */
    void destroyPart(int piece, int part) {
        if (setPieces[piece].damagePart(part, Double.MAX_VALUE)) {
            wreck(piece, part);
        }
    }

    /** Drops a spore of {@code gun}'s mine at (x, y) drifting at {@code angle}, as a mine layer does (for tests). */
    void dropMine(EnemyGun gun, double x, double y, double angle) {
        force.dropMine(gun, x, y, angle);
    }

    /** Starts an overdrive of {@code seconds}, as its pickup does; a running one starts over. */
    void overdrive(double seconds) {
        fire.overdrive(SimStep.ticks(seconds));
    }

    public int shotCount() {
        return fire.shots().size();
    }

    public Shot shot(int index) {
        return fire.shots().get(index);
    }

    public int enemyCount() {
        return force.enemies().size();
    }

    public Enemy enemy(int index) {
        return force.enemies().get(index);
    }

    public int bulletCount() {
        return force.bullets().size();
    }

    public EnemyBullet bullet(int index) {
        return force.bullets().get(index);
    }

    public int groundObjectCount() {
        return ground.size();
    }

    public GroundObject groundObject(int index) {
        return ground.get(index);
    }

    public int pickupCount() {
        return pickups.size();
    }

    public Pickup pickup(int index) {
        return pickups.get(index);
    }

    /** The spore mines drifting on the screen (below the player's layer until {@link Mine#armed()}). */
    public int mineCount() {
        return force.mines().size();
    }

    public Mine mine(int index) {
        return force.mines().get(index);
    }

    /** The debris chunks on the screen. */
    public int debrisCount() {
        return debris.size();
    }

    public Debris debris(int index) {
        return debris.get(index);
    }

    /** The level's set pieces, present or not; {@link SetPiece#present()} tells whether one is on screen. */
    public int setPieceCount() {
        return setPieces.length;
    }

    public SetPiece setPiece(int index) {
        return setPieces[index];
    }

    public int craneCount() {
        return cranes.length;
    }

    /** Part G: the tows of the level (Level 07's lifeboat), present or not. */
    public int towCount() {
        return tows.length;
    }

    public Tow tow(int index) {
        return tows[index];
    }

    public Crane crane(int index) {
        return cranes[index];
    }

    /** The groups of a group objective (Level 02's docks); 0 otherwise. */
    public int groupCount() {
        return objectives.groupCount();
    }

    /** Whether group {@code g} is still open (0), cleared (1) or lost (2). */
    public int groupState(int g) {
        return objectives.groupState(g);
    }

    /** The units of the escort objective's convoy; 0 without one. */
    public int allyCount() {
        return convoy == null ? 0 : convoy.size();
    }

    /** Convoy unit {@code k}, the leading one first. */
    public Ally ally(int k) {
        return convoy.get(k);
    }

    /** The convoy's units still alive in this attempt; 0 without a convoy. */
    public int alliesAlive() {
        return convoy == null ? 0 : convoy.alive();
    }

    /** The convoy unit hit first in this attempt, for its radio line; -1 before. */
    public int firstAllyHit() {
        return convoy == null ? -1 : convoy.firstHit();
    }

    /** The convoy unit lost first in this attempt, for its radio line; -1 before. */
    public int firstAllyLost() {
        return convoy == null ? -1 : convoy.firstLost();
    }

    /** The lobs in flight, their markers on the ground. */
    public int lobCount() {
        return force.lobs().size();
    }

    public Lob lob(int index) {
        return force.lobs().get(index);
    }

    /** The mass-driver sleds, if the level has them. */
    public java.util.Optional<Sled> sled() {
        return sledView;
    }

    /** The destroy-targets group whose loss failed the primary objective; -1 for none. */
    public int failedGroup() {
        return failedGroup;
    }

    /** Whether the primary objective failed in this attempt: the level is lost though the ship flies on. */
    public boolean primaryFailed() {
        return failed;
    }

    /** Events of the last step. */
    public SimEvents events() {
        return events;
    }

    /** Steps since the sortie was created. */
    public long tick() {
        return tick;
    }

    /** Seconds since the start of this attempt. */
    public double levelSeconds() {
        return levelTick * SimStep.SECONDS;
    }

    /** 1 for the first attempt, counting up with every restart after the ship was destroyed. */
    public int attempt() {
        return attempt;
    }

    /** Distance the ground layer has scrolled in this attempt, in pixels. */
    public double groundScroll() {
        return groundScroll;
    }

    /** The 1-based number of the section the scroll is in; the last one after the end. */
    public int section() {
        double t = levelSeconds();
        List<LevelScript.Section> sections = script.sections();
        for (int i = 0; i < sections.size(); i++) {
            if (t < sections.get(i).end()) {
                return i + 1;
            }
        }
        return sections.size();
    }

    /** The ground layer's current scroll speed in px/s. */
    public double groundSpeed() {
        return scrollSpeed();
    }

    /** Whether the non-playable launch is still running. */
    public boolean launching() {
        return levelTick <= launchTicks;
    }

    /** Whether the ship is flying, rather than wrecked and waiting for a retry. */
    public boolean flying() {
        return !wrecked;
    }

    /** Whether the scroll has reached the end: the level is won. */
    public boolean complete() {
        return complete;
    }

    /** The edges showing an edge warning, as {@link WarningEdge} bits. */
    public int edgeWarnings() {
        return edgeWarnings;
    }

    public long score() {
        return tally.score();
    }

    /** Credits earned in this attempt. */
    public int credits() {
        return tally.credits();
    }

    public int chain() {
        return tally.chain();
    }

    public double chainMultiplier() {
        return tally.multiplier();
    }

    /** The share of the chain window left, 1 right after a kill. */
    public double chainWindow() {
        return tally.window();
    }

    public int kills() {
        return tally.kills();
    }

    /** Every enemy the level sends, its set pieces among them and the boss streams' units let in so far. */
    public int enemyTotal() {
        return force.units() + setPieces.length + streamReleased;
    }

    /** Kills needed for the secondary objective. */
    public int requiredKills() {
        return objectives.requiredKills();
    }

    public boolean secondaryMet() {
        return objectives.secondaryMet();
    }

    /**
     * Whether the secondary objective counts units (parts) destroyed of all: none of an enemy gets
     * through (Level 03's Spore Bombers), every unit of some enemies (Level 05), or every one of a
     * boss's parts shot off in time (Level 07's bay sacs).
     */
    public boolean secondaryByEscapes() {
        return script.secondary().byEscapes();
    }

    /** The units of an escapes objective's enemy (the parts of a parts objective) destroyed so far in this attempt. */
    public int escapesDestroyed() {
        return objectives.escapesDestroyed();
    }

    /** The units of an escapes objective's enemy the level sends (the parts of a parts objective). */
    public int escapesTotal() {
        return objectives.escapesTotal();
    }

    /** Whether a unit of an escapes objective's enemy got away: the objective failed for this attempt. */
    public boolean secondaryFailed() {
        return objectives.escapesFailed();
    }
}
