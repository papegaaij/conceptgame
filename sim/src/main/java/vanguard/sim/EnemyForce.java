package vanguard.sim;

import java.util.ArrayList;
import java.util.List;

/**
 * The level's enemies: the units entering by the {@link WaveSchedule} and the units fixed to the
 * ground ({@link LevelScript.GroundUnit}, entering at the top edge with the scroll), flying or
 * scrolling and firing at the ship, and their bullets. What a kill earns is the {@link Sortie}'s.
 */
final class EnemyForce {
    private static final int ENEMY_CAPACITY = 192;
    /** Segment chains in flight: the waves' and their regrown rear parts. */
    private static final int CHAIN_CAPACITY = 12;

    static final int BULLET_CAPACITY = 256;
    /** No enemy bullet spawns this close to the ship (design/enemies, bullet readability rules). */
    private static final double NO_FIRE_DISTANCE = 72;

    /** What the sortie does when a unit leaves the screen alive (just before it is gone). */
    interface Escapes {
        void escaped(Enemy enemy);

        /**
         * A spawner burst on its own (design/enemies/air/brood-pod), just before it is gone: its
         * units are released already.
         */
        default void burst(Enemy enemy) {
            escaped(enemy);
        }
    }

    private static final int MINE_CAPACITY = 96;
    private static final int LOB_CAPACITY = 32;
    /** How far outside a lob's impact circle its ring starts, px. */
    private static final double RING_MARGIN = 12;

    private final SplitMix64 rng;
    private final Rules rules;
    private final SimEvents events;
    private final Escapes escapes;
    private final List<EnemySpec> kinds;
    private final WaveSchedule waves;
    private final List<LevelScript.GroundUnit> groundUnits;
    private final int[] groundKinds;
    private final int[] groundTicks;
    private final Pool<Enemy> enemies = new Pool<>(ENEMY_CAPACITY, Enemy::new, Enemy[]::new);
    private final Pool<EnemyBullet> bullets = new Pool<>(BULLET_CAPACITY, EnemyBullet::new, EnemyBullet[]::new);
    private final Pool<Mine> mines = new Pool<>(MINE_CAPACITY, Mine::new, Mine[]::new);
    private final Pool<Lob> lobs = new Pool<>(LOB_CAPACITY, Lob::new, Lob[]::new);
    private final Pool<Chain> chains = new Pool<>(CHAIN_CAPACITY, Chain::new, Chain[]::new);
    /** Chains started in this attempt, for their serials. */
    private int chainsSpawned;
    /** Per enemy kind, its chain's segment, tail and regrown head kinds; -1 for others. */
    private final int[] segmentKinds;

    private final int[] tailKinds;
    private final int[] regrownKinds;
    private int spawned;
    private int nextGround;
    /** The convoy the target-the-objective hook aims at; null without one. */
    private Convoy convoy;
    /** Per enemy kind, whether its aimed attacks use the hook (mode {@code nearest}). */
    private boolean[] hooked;
    /** Per enemy kind, the kind a spawner releases; -1 for others. */
    private final int[] broodKinds;
    /** The spawner that entered last, which the escorts entering after it circle. */
    private Enemy lastCarrier;

    /** Plans the waves with {@code rng}, which also spreads the aimed shots later. */
    EnemyForce(
            List<WaveSpec> waveSpecs,
            List<LevelScript.GroundUnit> groundUnits,
            SplitMix64 rng,
            Rules rules,
            SimEvents events,
            Escapes escapes) {
        this(waveSpecs, groundUnits, List.of(), rng, rules, events, escapes);
    }

    /** @param extraKinds enemies that enter outside the waves (a boss's streams), among the kinds too */
    EnemyForce(
            List<WaveSpec> waveSpecs,
            List<LevelScript.GroundUnit> groundUnits,
            List<EnemySpec> extraKinds,
            SplitMix64 rng,
            Rules rules,
            SimEvents events,
            Escapes escapes) {
        this.rng = rng;
        this.rules = rules;
        this.events = events;
        this.escapes = escapes;
        List<EnemySpec> distinct = new ArrayList<>(
                waveSpecs.stream().map(WaveSpec::enemy).distinct().toList());
        for (LevelScript.GroundUnit unit : groundUnits) {
            if (!distinct.contains(unit.enemy())) {
                distinct.add(unit.enemy());
            }
        }
        for (EnemySpec extra : extraKinds) {
            if (!distinct.contains(extra)) {
                distinct.add(extra);
            }
        }
        // The units a spawner releases are among the kinds too.
        for (int k = 0; k < distinct.size(); k++) {
            EnemySpec spec = distinct.get(k);
            if (spec.brood().isPresent()
                    && !distinct.contains(spec.brood().get().enemy())) {
                distinct.add(spec.brood().get().enemy());
            }
        }
        // So are a chain's segments, tail and regrown head.
        for (int k = 0; k < distinct.size(); k++) {
            EnemySpec spec = distinct.get(k);
            if (spec.chain().isPresent()) {
                EnemySpec.ChainSpec chain = spec.chain().get();
                for (EnemySpec part : List.of(chain.segment(), chain.tail(), chain.regrown())) {
                    if (!distinct.contains(part)) {
                        distinct.add(part);
                    }
                }
            }
        }
        kinds = List.copyOf(distinct);
        segmentKinds = new int[kinds.size()];
        tailKinds = new int[kinds.size()];
        regrownKinds = new int[kinds.size()];
        for (int k = 0; k < kinds.size(); k++) {
            var chain = kinds.get(k).chain();
            segmentKinds[k] = chain.isPresent() ? kinds.indexOf(chain.get().segment()) : -1;
            tailKinds[k] = chain.isPresent() ? kinds.indexOf(chain.get().tail()) : -1;
            regrownKinds[k] = chain.isPresent() ? kinds.indexOf(chain.get().regrown()) : -1;
        }
        broodKinds = new int[kinds.size()];
        for (int k = 0; k < kinds.size(); k++) {
            EnemySpec spec = kinds.get(k);
            broodKinds[k] =
                    spec.brood().isPresent() ? kinds.indexOf(spec.brood().get().enemy()) : -1;
        }
        this.waves = new WaveSchedule(waveSpecs, kinds, rng);
        this.groundUnits = groundUnits;
        groundKinds = new int[groundUnits.size()];
        groundTicks = new int[groundUnits.size()];
        for (int i = 0; i < groundUnits.size(); i++) {
            groundKinds[i] = kinds.indexOf(groundUnits.get(i).enemy());
            groundTicks[i] = SimStep.ticks(groundUnits.get(i).t());
        }
    }

    /**
     * The target-the-objective hook in mode {@code nearest} (design/enemies): the aimed attacks of
     * the enemies {@code slugs} go at the ship or the nearest unit of {@code target}, whichever is
     * closer, chosen every step and as each shot is fired.
     */
    void hook(Convoy target, List<String> slugs) {
        convoy = target;
        hooked = new boolean[kinds.size()];
        for (int k = 0; k < kinds.size(); k++) {
            hooked[k] = slugs.contains(kinds.get(k).slug());
        }
    }

    /** Back to the level start: no enemies or bullets, the schedule at its first unit. */
    void reset() {
        Pools.clear(enemies);
        Pools.clear(bullets);
        Pools.clear(mines);
        Pools.clear(lobs);
        Pools.clear(chains);
        chainsSpawned = 0;
        waves.reset();
        spawned = 0;
        nextGround = 0;
        lastCarrier = null;
    }

    /** Lets in every unit due at {@code levelTick}, flying and on the ground. */
    void spawn(int levelTick) {
        for (Spawn spawn = waves.due(levelTick); spawn != null; spawn = waves.due(levelTick)) {
            if (spawn.enemy().chain().isPresent()) {
                spawnChain(spawn);
                continue;
            }
            Enemy enemy = enemies.obtain();
            if (enemy != null) {
                enemy.spawn(spawn, spawned);
                if (spawn.escort().isPresent()) {
                    enemy.escort(lastCarrier);
                }
            }
            if (spawn.enemy().brood().isPresent()) {
                lastCarrier = enemy;
            }
            spawned++;
        }
        while (nextGround < groundTicks.length && groundTicks[nextGround] <= levelTick) {
            LevelScript.GroundUnit unit = groundUnits.get(nextGround);
            Enemy enemy = enemies.obtain();
            if (enemy != null) {
                enemy.root(unit.enemy(), groundKinds[nextGround], unit.x(), spawned, unit.group());
            }
            spawned++;
            nextGround++;
        }
    }

    /**
     * A wave's segment chain enters: its head (the unit that counts as the kill, carrying the
     * wave's pickup), its segments and its tail, strung out behind the path's start; members the
     * pool has no room for are left out.
     */
    private void spawnChain(Spawn spawn) {
        Chain chain = chains.obtain();
        if (chain == null) {
            spawned++;
            return;
        }
        EnemySpec head = spawn.enemy();
        EnemySpec.ChainSpec spec = head.chain().orElseThrow();
        chain.start(spec, chainsSpawned++, spawn, regrownKinds[spawn.kind()]);
        int members = spec.members();
        for (int i = 0; i < members; i++) {
            Enemy member = enemies.obtain();
            if (member == null) {
                break;
            }
            boolean tail = i == members - 1;
            EnemySpec part = i == 0 ? head : tail ? spec.tail() : spec.segment();
            int kind = i == 0 ? spawn.kind() : tail ? tailKinds[spawn.kind()] : segmentKinds[spawn.kind()];
            Hitbox box = i == 0 || tail ? part.hitbox() : spec.segmentBoxes().get(i - 1);
            member.link(chain, i, part, kind, box, spawned++, 0, 0, 0);
            if (i == 0) {
                member.carry(spawn.carried());
            }
            chain.member(i, member);
        }
        chain.settle();
    }

    /**
     * The chains fly a step: a rear part whose new head has grown gets it (and lunges at the ship),
     * the dying members pop one by one, and a chain that has flown its course leaves with its
     * members (its wave's head counting as escaped).
     */
    void moveChains(Ship ship) {
        for (int c = chains.size() - 1; c >= 0; c--) {
            Chain chain = chains.get(c);
            if (chain.regrowDue()) {
                Enemy head = null;
                if (chain.alive()) {
                    head = enemies.obtain();
                    if (head != null) {
                        int kind = chain.regrownKind();
                        EnemySpec regrown = chain.spec().regrown();
                        head.link(
                                chain,
                                0,
                                regrown,
                                kind,
                                regrown.hitbox(),
                                spawned++,
                                chain.headX(),
                                chain.headY(),
                                chain.headFacing());
                        events.add(SimEvents.Type.CHAIN_REGROWN, chain.headX(), chain.headY(), kind);
                    }
                }
                chain.grown(head, ship.x(), ship.y());
            }
            Enemy popped = chain.popDue();
            if (popped != null) {
                events.add(
                        SimEvents.Type.CHAIN_POP,
                        popped.x(),
                        popped.y(),
                        SimEvents.chainPopValue(popped.kind(), popped.hitbox().width()));
                free(popped);
            }
            if (!chain.advance()) {
                for (int i = 0; i < chain.size(); i++) {
                    Enemy member = chain.member(i);
                    if (member != null) {
                        if (i == 0 && chain.original()) {
                            escapes.escaped(member);
                        }
                        free(member);
                    }
                }
                chains.free(c);
            } else if (!chain.alive() && !chain.regrowing()) {
                chains.free(c);
            }
        }
    }

    /** Frees the unit {@code enemy} from the pool. */
    private void free(Enemy enemy) {
        for (int i = enemies.size() - 1; i >= 0; i--) {
            if (enemies.get(i) == enemy) {
                enemies.free(i);
                return;
            }
        }
    }

    /**
     * A chain member was destroyed (before it is freed): the chain is cut there; a split-off rear
     * part becomes a chain of its own that grows a new head.
     */
    void memberDestroyed(Enemy enemy) {
        Chain chain = enemy.chain();
        Chain rear = chains.obtain();
        boolean split = chain.destroyed(enemy.link(), rear, chainsSpawned);
        if (split) {
            chainsSpawned++;
            events.add(SimEvents.Type.CHAIN_CUT, enemy.x(), enemy.y(), enemy.kind());
        } else if (rear != null) {
            chains.free(chains.size() - 1);
        }
    }

    /** Whether destroying chain member {@code enemy} now pays its chain's first bonus (the tail, first). */
    boolean firstBonusDue(Enemy enemy) {
        return enemy.chain().firstBonusDue(enemy.link());
    }

    Pool<Chain> chains() {
        return chains;
    }

    /**
     * Flies every unit and scrolls the ground units by {@code groundScroll}; those whose gun is
     * ready fire at the ship when {@code firing}. A grouped ground unit that leaves the screen
     * alive is reported as escaped.
     */
    void move(Ship ship, boolean firing, double groundScroll) {
        if (chains.size() > 0) {
            moveChains(ship);
        }
        for (int i = enemies.size() - 1; i >= 0; i--) {
            Enemy enemy = enemies.get(i);
            int target = target(enemy, ship);
            double aimX = target < 0 ? ship.x() : convoy.get(target).x();
            double aimY = target < 0 ? ship.y() : convoy.get(target).y();
            if (!enemy.move(ship.x(), ship.y(), groundScroll, aimX, aimY)) {
                if (enemy.spec().brood().isPresent()) {
                    carrierEnded(enemy);
                }
                escapes.escaped(enemy);
                enemies.free(i);
            } else if (enemy.spec().brood().isPresent() && enemy.burstDue()) {
                hatch(enemy, ship);
                escapes.burst(enemy);
                enemies.free(i);
            } else if (enemy.walking()) {
                fireWalker(enemy, ship, firing);
            } else if (enemy.spec().sweep().isPresent()) {
                if (enemy.telegraphStarted()) {
                    events.add(SimEvents.Type.SWEEP_TELEGRAPH, enemy.x(), enemy.y(), enemy.kind());
                } else if (enemy.sweepStarted()) {
                    events.add(SimEvents.Type.SWEEP_FIRED, enemy.x(), enemy.y(), enemy.kind());
                }
            } else if (enemy.trigger() && firing) {
                EnemyGun gun = enemy.spec().gun().orElseThrow();
                if (gun.mine().isPresent()) {
                    dropMine(enemy, gun);
                } else if (gun.mortar().isPresent()) {
                    lob(enemy, gun, ship);
                } else {
                    fireAt(enemy, ship, target(enemy, ship));
                }
            }
        }
    }

    /**
     * A walker's attacks (design/enemies/ground/scuttler): its fan along its facing, which aims at
     * nobody, and its spit, aimed (at the ship, or the convoy unit the hook picks) while the ship
     * is behind it.
     */
    private void fireWalker(Enemy enemy, Ship ship, boolean firing) {
        boolean fan = enemy.trigger();
        boolean spit = enemy.spit(ship.x(), ship.y());
        if (!firing) {
            return;
        }
        if (fan) {
            fire(enemy.x(), enemy.y(), enemy.spec().gun().orElseThrow(), false, enemy.facing(), ship, -1, true);
        }
        if (spit) {
            EnemyGun gun = enemy.spec().walker().orElseThrow().spit().orElseThrow();
            int target = convoy != null && hooked[enemy.kind()]
                    ? convoy.nearest(enemy.x(), enemy.y(), ship.x(), ship.y())
                    : -1;
            fire(enemy.x(), enemy.y(), gun, false, 0, ship, target, false);
        }
    }

    /**
     * A spawner ends, destroyed or bursting on its own (design/enemies/air/brood-pod): its units
     * fly out of its centre, spread evenly over its arc centred on the direction to the ship, and
     * its escorts break off.
     */
    void hatch(Enemy pod, Ship ship) {
        EnemySpec.Brood brood = pod.spec().brood().orElseThrow();
        int kind = broodKinds[pod.kind()];
        double toShip = StrictMath.atan2(ship.y() - pod.y(), ship.x() - pod.x());
        for (int k = 0; k < brood.count(); k++) {
            double angle = brood.count() == 1
                    ? toShip
                    : toShip - brood.arcRadians() / 2 + k * brood.arcRadians() / (brood.count() - 1);
            Enemy unit = enemies.obtain();
            if (unit != null) {
                unit.hatch(
                        brood.enemy(),
                        kind,
                        pod.x(),
                        pod.y(),
                        Trig.cos(angle) * brood.speed(),
                        Trig.sin(angle) * brood.speed(),
                        spawned);
            }
            spawned++;
        }
        events.add(SimEvents.Type.BROOD_HATCHED, pod.x(), pod.y(), pod.kind());
        carrierEnded(pod);
    }

    /**
     * One unit of {@code enemy} launched by a boss's window from (x, y) at {@code angle} radians
     * (0 = right, y up) and {@code speed} px/s, gliding {@code glideSeconds} before it holds (see
     * {@link Enemy#launch}); {@code enemy} is one of {@link #kinds()}.
     */
    void launch(EnemySpec enemy, double x, double y, double angle, double speed, double glideSeconds) {
        int kind = kinds.indexOf(enemy);
        if (kind < 0) {
            throw new IllegalArgumentException(enemy.slug() + " is not among the level's enemy kinds");
        }
        Enemy unit = enemies.obtain();
        if (unit != null) {
            unit.launch(enemy, kind, x, y, Trig.cos(angle) * speed, Trig.sin(angle) * speed, glideSeconds, spawned);
        }
        spawned++;
    }

    /** The escorts circling {@code carrier} break off: it is gone. */
    private void carrierEnded(Enemy carrier) {
        for (int i = 0; i < enemies.size(); i++) {
            Enemy enemy = enemies.get(i);
            if (enemy.escorts(carrier)) {
                enemy.carrierEnded();
            }
        }
        if (lastCarrier == carrier) {
            lastCarrier = null;
        }
    }

    /**
     * Whom a unit's aimed attack goes at now: the convoy unit the hook picks, or -1 for the ship
     * (units without the hook, fans and mines always aim at the ship).
     */
    private int target(Enemy enemy, Ship ship) {
        if (convoy == null || !hooked[enemy.kind()] || enemy.spec().gun().isEmpty()) {
            return -1;
        }
        EnemyGun gun = enemy.spec().gun().get();
        if (gun.fan() != 1 || gun.mine().isPresent()) {
            return -1;
        }
        return convoy.nearest(enemy.x(), enemy.y(), ship.x(), ship.y());
    }

    /**
     * A shot at the ship (or where it is going for a unit that leads the target), a turret's along
     * its barrel, or a fan centred on that line; turned by a random angle within the difficulty's
     * spread.
     */
    private void fireAt(Enemy enemy, Ship ship, int target) {
        fire(
                enemy.x(),
                enemy.y(),
                enemy.spec().gun().orElseThrow(),
                enemy.leadsTarget(),
                enemy.aim(),
                ship,
                target,
                false);
    }

    /** Lets in one unit planned outside the waves (a boss's stream), now. */
    void release(Spawn plan) {
        Enemy enemy = enemies.obtain();
        if (enemy != null) {
            enemy.spawn(plan, spawned);
        }
        spawned++;
    }

    /**
     * One bullet from (x, y) at {@code angle} radians (0 = right, y up), within the bullet budget;
     * none when the ship is closer than the bullets may spawn.
     */
    void fireAngle(double x, double y, double angle, double speed, double damage, Ship ship) {
        double dx = ship.x() - x;
        double dy = ship.y() - y;
        if (dx * dx + dy * dy < NO_FIRE_DISTANCE * NO_FIRE_DISTANCE || bullets.size() >= rules.bulletBudget()) {
            return;
        }
        bullets.obtain().fire(x, y, Trig.cos(angle) * speed, Trig.sin(angle) * speed, damage);
    }

    /** The schedule's place, for a boss checkpoint: the next wave unit, the next ground unit, the units let in. */
    int scheduleNext() {
        return waves.next();
    }

    int nextGround() {
        return nextGround;
    }

    int spawned() {
        return spawned;
    }

    /** Back to a boss checkpoint: an empty field, the schedule where it was then. */
    void restore(int scheduleNext, int ground, int spawnedUnits) {
        reset();
        waves.next(scheduleNext);
        nextGround = ground;
        spawned = spawnedUnits;
    }

    /** A set piece's part fires its gun from (x, y) at the ship, as a unit that aims at once. */
    void fireFrom(double x, double y, EnemyGun gun, Ship ship) {
        fire(x, y, gun, false, 0, ship, -1, false);
    }

    /**
     * @param target the convoy unit the shot is aimed at; -1 for the ship
     * @param alongAim whether the volley goes along {@code aim} (a walker's facing fan) whatever the gun
     */
    private void fire(
            double x, double y, EnemyGun gun, boolean leads, double aim, Ship ship, int target, boolean alongAim) {
        double dx = ship.x() - x;
        double dy = ship.y() - y;
        double distance = Math.sqrt(dx * dx + dy * dy);
        if (distance < NO_FIRE_DISTANCE || bullets.size() >= rules.bulletBudget()) {
            return;
        }
        if (target >= 0) {
            dx = convoy.get(target).x() - x;
            dy = convoy.get(target).y() - y;
            distance = Math.sqrt(dx * dx + dy * dy);
            if (distance == 0) {
                return;
            }
        }
        if (alongAim || Double.isFinite(gun.turnRate())) {
            // Along the barrel: radians clockwise from straight down.
            dx = -Trig.sin(aim);
            dy = -Trig.cos(aim);
            distance = 1;
        } else if (leads && target < 0) {
            double flight = distance / gun.bulletSpeed();
            dx += ship.vx() * flight;
            dy += ship.vy() * flight;
            distance = Math.sqrt(dx * dx + dy * dy);
        }
        dx /= distance;
        dy /= distance;
        double turn = rules.aimedSpread() > 0 ? rng.range(-rules.aimedSpread(), rules.aimedSpread()) : 0;
        for (int k = 0; k < gun.fan(); k++) {
            if (bullets.size() >= rules.bulletBudget()) {
                break;
            }
            double angle =
                    turn + (gun.fan() == 1 ? 0 : -gun.spreadRadians() / 2 + k * gun.spreadRadians() / (gun.fan() - 1));
            double cos = Trig.cos(angle);
            double sin = Trig.sin(angle);
            EnemyBullet bullet = bullets.obtain();
            bullet.fire(
                    x,
                    y,
                    (dx * cos - dy * sin) * gun.bulletSpeed(),
                    (dx * sin + dy * cos) * gun.bulletSpeed(),
                    gun.damage(),
                    target);
        }
        events.add(SimEvents.Type.ENEMY_FIRED, x, y, (int) gun.damage());
    }

    /** A mine layer drops a spore under it, drifting in a random direction. */
    private void dropMine(Enemy enemy, EnemyGun gun) {
        dropMine(gun, enemy.x(), enemy.y(), rng.range(0, 2 * StrictMath.PI));
    }

    /** A spore of {@code gun}'s mine at (x, y), drifting at {@code angle} radians (0 = right, y up). */
    void dropMine(EnemyGun gun, double x, double y, double angle) {
        Mine mine = mines.obtain();
        if (mine != null) {
            mine.drop(gun, x, y, angle);
            events.add(SimEvents.Type.MINE_DROPPED, x, y);
        }
    }

    /**
     * Drifts the spores; one whose life ran out bursts into its ring (unless it never bursts),
     * away from the ship (no bullet spawns close to it).
     */
    void moveMines(Ship ship) {
        for (int i = mines.size() - 1; i >= 0; i--) {
            Mine mine = mines.get(i);
            if (!mine.move()) {
                EnemyGun.MineSpec spec = mine.spec();
                if (mine.expired() && spec.bursts()) {
                    burst(mine.x(), mine.y(), spec.ring(), mine.gun().bulletSpeed(), spec.ringDamage(), ship);
                    events.add(SimEvents.Type.MINE_BURST, mine.x(), mine.y());
                }
                mines.free(i);
            }
        }
    }

    /** A mortar lobs a blob at where the ship is now (the marker does not follow it). */
    private void lob(Enemy enemy, EnemyGun gun, Ship ship) {
        Lob lob = lobs.obtain();
        if (lob != null) {
            lob.launch(gun, enemy.x(), enemy.y(), ship.x(), ship.y());
            events.add(SimEvents.Type.MORTAR_LOBBED, enemy.x(), enemy.y());
        }
    }

    /**
     * Flies the lobs, their start scrolling with the ground by {@code groundScroll}; one that lands
     * bursts into its ring (whether the ship is near or not: the ring starts just outside the impact
     * circle) and hits a ship within its impact circle. Returns the damage of the direct hits in this
     * step, which the ship takes when it can be hit.
     */
    double moveLobs(Ship ship, double groundScroll) {
        double direct = 0;
        for (int i = lobs.size() - 1; i >= 0; i--) {
            Lob lob = lobs.get(i);
            lob.scroll(groundScroll);
            if (!lob.fly()) {
                continue;
            }
            EnemyGun gun = lob.gun();
            EnemyGun.MortarSpec mortar = gun.mortar().orElseThrow();
            double x = lob.targetX();
            double y = lob.targetY();
            // The ring starts just outside the impact circle, so a ship hit directly is not also
            // hit by the ring as it forms.
            double start = mortar.impactRadius() + RING_MARGIN;
            for (int k = 0; k < mortar.ring() && bullets.size() < rules.bulletBudget(); k++) {
                double angle = 2 * StrictMath.PI * k / mortar.ring();
                double cos = Trig.cos(angle);
                double sin = Trig.sin(angle);
                bullets.obtain()
                        .fire(
                                x + cos * start,
                                y + sin * start,
                                cos * gun.bulletSpeed(),
                                sin * gun.bulletSpeed(),
                                mortar.ringDamage());
            }
            double dx = ship.x() - x;
            double dy = ship.y() - y;
            boolean hit = dx * dx + dy * dy <= mortar.impactRadius() * mortar.impactRadius();
            if (hit) {
                direct += gun.damage();
            }
            events.add(SimEvents.Type.MORTAR_IMPACT, x, y, hit ? 1 : 0);
            lobs.free(i);
        }
        return direct;
    }

    Pool<Lob> lobs() {
        return lobs;
    }

    /** A destroyed unit with a death burst pops into its puff of bullets. */
    void deathBurst(Enemy enemy, Ship ship) {
        EnemySpec.DeathBurst puff = enemy.spec().deathBurst().orElseThrow();
        burst(enemy.x(), enemy.y(), puff.count(), puff.speed(), puff.damage(), ship);
    }

    /**
     * A ring of {@code count} bullets from (x, y), within the bullet budget; none when the ship is
     * closer than the bullets may spawn (design/enemies, bullet readability rules).
     */
    void burst(double x, double y, int count, double speed, double damage, Ship ship) {
        double dx = ship.x() - x;
        double dy = ship.y() - y;
        if (dx * dx + dy * dy < NO_FIRE_DISTANCE * NO_FIRE_DISTANCE) {
            return;
        }
        for (int k = 0; k < count && bullets.size() < rules.bulletBudget(); k++) {
            double angle = 2 * StrictMath.PI * k / count;
            bullets.obtain().fire(x, y, Trig.cos(angle) * speed, Trig.sin(angle) * speed, damage);
        }
    }

    Pool<Mine> mines() {
        return mines;
    }

    void moveBullets() {
        for (int i = bullets.size() - 1; i >= 0; i--) {
            EnemyBullet bullet = bullets.get(i);
            bullet.move();
            if (!PlayField.overlaps(bullet.x(), bullet.y(), EnemyGun.BULLET)) {
                bullets.free(i);
            }
        }
    }

    /** The edges showing an edge warning at {@code levelTick}, as {@link WarningEdge} bits. */
    int warnings(int levelTick) {
        return waves.warnings(levelTick);
    }

    Pool<Enemy> enemies() {
        return enemies;
    }

    Pool<EnemyBullet> bullets() {
        return bullets;
    }

    /** The distinct enemies of the level; {@link Enemy#kind()} indexes this list. */
    List<EnemySpec> kinds() {
        return kinds;
    }

    /** The units of the enemy {@code slug} the level sends, flying and on the ground. */
    int unitsOf(String slug) {
        int count = waves.unitsOf(slug);
        for (LevelScript.GroundUnit unit : groundUnits) {
            count += unit.enemy().slug().equals(slug) ? 1 : 0;
        }
        for (EnemySpec kind : kinds) {
            if (kind.brood().isPresent() && kind.brood().get().enemy().slug().equals(slug)) {
                count += waves.unitsOf(kind.slug()) * kind.brood().get().count();
            }
        }
        return count;
    }

    /** Every unit the level sends, flying and on the ground, and the units its spawners release. */
    int units() {
        int count = waves.units() + groundUnits.size();
        for (EnemySpec kind : kinds) {
            if (kind.brood().isPresent()) {
                count += waves.unitsOf(kind.slug()) * kind.brood().get().count();
            }
        }
        return count;
    }
}
