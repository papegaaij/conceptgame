package vanguard.sim;

import java.util.Optional;

/**
 * An enemy's stat block (design/enemies/&lt;category&gt;/&lt;slug&gt;/data.yaml) at one
 * difficulty, built by {@code vanguard.content.SimSpecs}: the HP already scaled, the movement
 * numbers its formations use, its gun and its drop rule.
 *
 * @param slug its name in the design data, e.g. {@code needler}
 * @param hp hit points in damage units
 * @param hitbox the hit box (the sprite is larger)
 * @param layer the layer it flies on
 * @param contactDamage damage of ramming the player
 * @param destroyedByRamming whether ramming the ship destroys it ({@code tiny} and {@code small} units)
 * @param bounty credits at medium in Act 1 terms
 * @param speed its own speed in px/s
 * @param snake the gap between two units of a snake
 * @param streamSpeed its speed in streams
 * @param hover how long and how far below the top edge it hovers
 * @param orbit its orbit in circle formations
 * @param gun its attack; none for rammers
 * @param drop the pickup every n-th kill drops
 * @param dive how it dives, for a diver
 * @param terrain whether it is fixed to the ground layer and moves only with the scroll
 * @param spiral how it spirals out of a whirl cluster
 * @param strafe how far below the top edge a convoy unit turns across the screen
 * @param deathBurst the puff of bullets it pops into when destroyed; none for most
 * @param sine a side-to-side offset on its path (the Brood Pod's drift)
 * @param brood the units it releases when it is destroyed or bursts on its own (a spawner)
 * @param walker how a walker (the Scuttler) walks its authored ground path, its frontal armour and
 *     its second attack
 * @param sideHover a unit entering from a side edge (the Mantis): where it hovers and how it leaves
 * @param sweep its laser sweep (the Mantis), fired while it hovers instead of a gun
 * @param chain the segment chain behind its head (the Coilwyrm)
 * @param hardened M5 part C (design/enemies/ground/hive-node): only {@code anti-ground} deliveries,
 *     the Airstrike and the Smart Bomb damage it; other shots and blasts glance off, and homing
 *     shots and the wingman do not pick it as a target unless their weapon is {@code anti-ground}
 * @param spawner M5 part C: a periodic spawner's cycle (the Hive Node)
 * @param pounce M5 part C: a walker's leap at the ship (the Ravager)
 * @param cloak M5 part D: its cloak (design/enemies/air/wraith): its stat block's {@code layer} is the
 *     cloaked one, the cloak's from its decloak on
 * @param ambush M5 part D: the path of a {@code rear ambush} wave's unit (the Wraith)
 * @param flock M5 part D: how the members of a {@code swarm} wave steer round its leader point (the
 *     Mote Swarm)
 * @param submerge M5 part E: how it surfaces and submerges (design/enemies/naval/driftjelly): its
 *     current layer swaps between its stat block's ({@code ground}) and {@link Layer#SUB}
 * @param ring M5 part E: its proximity ring (the Driftjelly), fired instead of a gun
 * @param drift M5 part E: a ground unit's drift along its nest's current on top of the scroll, px/s
 *     (the Reef Spitter's raft); 0 for none. A {@code field} wave's units drift at their {@code speed}
 */
public record EnemySpec(
        String slug,
        double hp,
        Hitbox hitbox,
        Layer layer,
        double contactDamage,
        boolean destroyedByRamming,
        int bounty,
        double speed,
        Optional<Snake> snake,
        Optional<Double> streamSpeed,
        Optional<Hover> hover,
        Optional<Orbit> orbit,
        Optional<EnemyGun> gun,
        Optional<Drop> drop,
        Optional<Dive> dive,
        boolean terrain,
        Optional<Spiral> spiral,
        Optional<Range> strafe,
        Optional<DeathBurst> deathBurst,
        Optional<Sine> sine,
        Optional<Brood> brood,
        Optional<Walker> walker,
        Optional<SideHover> sideHover,
        Optional<Sweep> sweep,
        Optional<ChainSpec> chain,
        boolean hardened,
        Optional<Spawner> spawner,
        Optional<Pounce> pounce,
        Optional<Cloak> cloak,
        Optional<Ambush> ambush,
        Optional<FlockSpec> flock,
        Optional<Submerge> submerge,
        Optional<ProximityRing> ring,
        double drift) {
    public EnemySpec {
        if (!(drift >= 0)) {
            throw new IllegalArgumentException(slug + ": a drift is 0 px/s or more: " + drift);
        }
        if (submerge.isPresent() && layer != Layer.GROUND) {
            throw new IllegalArgumentException(
                    slug + ": only a unit on the ground layer (the sea's surface) submerges");
        }
    }

    /** The constructor of Level 10: without the submerging, proximity rings and drifting rafts of Level 11 (M5 part E). */
    public EnemySpec(
            String slug,
            double hp,
            Hitbox hitbox,
            Layer layer,
            double contactDamage,
            boolean destroyedByRamming,
            int bounty,
            double speed,
            Optional<Snake> snake,
            Optional<Double> streamSpeed,
            Optional<Hover> hover,
            Optional<Orbit> orbit,
            Optional<EnemyGun> gun,
            Optional<Drop> drop,
            Optional<Dive> dive,
            boolean terrain,
            Optional<Spiral> spiral,
            Optional<Range> strafe,
            Optional<DeathBurst> deathBurst,
            Optional<Sine> sine,
            Optional<Brood> brood,
            Optional<Walker> walker,
            Optional<SideHover> sideHover,
            Optional<Sweep> sweep,
            Optional<ChainSpec> chain,
            boolean hardened,
            Optional<Spawner> spawner,
            Optional<Pounce> pounce,
            Optional<Cloak> cloak,
            Optional<Ambush> ambush,
            Optional<FlockSpec> flock) {
        this(
                slug,
                hp,
                hitbox,
                layer,
                contactDamage,
                destroyedByRamming,
                bounty,
                speed,
                snake,
                streamSpeed,
                hover,
                orbit,
                gun,
                drop,
                dive,
                terrain,
                spiral,
                strafe,
                deathBurst,
                sine,
                brood,
                walker,
                sideHover,
                sweep,
                chain,
                hardened,
                spawner,
                pounce,
                cloak,
                ambush,
                flock,
                Optional.empty(),
                Optional.empty(),
                0);
    }

    /**
     * M5 part E, surfacing and submerging (design/enemies/naval/driftjelly; the stated defaults of
     * 2026-10-08): every {@code everyMin}–{@code everyMax} s, drawn per unit from its own seed on the
     * simulation's real steps (so it keeps going in a halted arena), the unit swaps between the
     * surface ({@link Layer#GROUND}) and {@link Layer#SUB} over {@code swapSeconds}, its current
     * layer flipping at the swap's middle; a {@code field} wave starts the share {@code start} of its
     * units submerged.
     */
    public record Submerge(double everyMin, double everyMax, double swapSeconds, double start) {
        public Submerge {
            if (!(everyMin > 0) || !(everyMax >= everyMin) || !(swapSeconds > 0) || !(start >= 0 && start <= 1)) {
                throw new IllegalArgumentException(
                        "a submerge swaps every [min, max] s (0 < min <= max) over a swap time, a share starting submerged");
            }
        }
    }

    /**
     * M5 part E, a proximity ring (design/enemies/naval/driftjelly, user decision E3 = b): while the
     * ship's centre is within {@code within} px of its centre (on either of its layers) it fires a
     * ring of {@code count} bullets at {@code bulletSpeed} px/s, each dealing {@code damage}, at most
     * once every {@code cooldownSeconds} (counted from the last ring). A ring due while the ship is
     * closer than the bullets may spawn waits (design/enemies, bullet readability rules).
     */
    public record ProximityRing(int count, double bulletSpeed, double damage, double within, double cooldownSeconds) {
        public ProximityRing {
            if (count < 1 || !(bulletSpeed > 0) || !(within > 0) || !(cooldownSeconds > 0)) {
                throw new IllegalArgumentException("a proximity ring has bullets, a speed, a reach and a cooldown");
            }
        }
    }

    /** The constructor of Level 09: without the cloaks, ambushes and flocks of Level 10 (M5 part D). */
    public EnemySpec(
            String slug,
            double hp,
            Hitbox hitbox,
            Layer layer,
            double contactDamage,
            boolean destroyedByRamming,
            int bounty,
            double speed,
            Optional<Snake> snake,
            Optional<Double> streamSpeed,
            Optional<Hover> hover,
            Optional<Orbit> orbit,
            Optional<EnemyGun> gun,
            Optional<Drop> drop,
            Optional<Dive> dive,
            boolean terrain,
            Optional<Spiral> spiral,
            Optional<Range> strafe,
            Optional<DeathBurst> deathBurst,
            Optional<Sine> sine,
            Optional<Brood> brood,
            Optional<Walker> walker,
            Optional<SideHover> sideHover,
            Optional<Sweep> sweep,
            Optional<ChainSpec> chain,
            boolean hardened,
            Optional<Spawner> spawner,
            Optional<Pounce> pounce) {
        this(
                slug,
                hp,
                hitbox,
                layer,
                contactDamage,
                destroyedByRamming,
                bounty,
                speed,
                snake,
                streamSpeed,
                hover,
                orbit,
                gun,
                drop,
                dive,
                terrain,
                spiral,
                strafe,
                deathBurst,
                sine,
                brood,
                walker,
                sideHover,
                sweep,
                chain,
                hardened,
                spawner,
                pounce,
                Optional.empty(),
                Optional.empty(),
                Optional.empty());
    }

    /**
     * M5 part D, a cloak (design/enemies/air/wraith): the unit flies on its stat block's layer (the
     * cloaked one, {@code high-air}: only homing hits it, no contact, the specials skip it) until it
     * decloaks at its hold point; from the start of the {@code flashSeconds} flash on its current
     * layer is {@code layer}, and its gun stays silent until the flash ends.
     */
    public record Cloak(Layer layer, double flashSeconds) {
        public Cloak {
            if (!(flashSeconds >= 0)) {
                throw new IllegalArgumentException("a decloak flash lasts 0 s or more: " + flashSeconds);
            }
        }
    }

    /**
     * M5 part D, the path of a {@code rear ambush} wave's unit (design/enemies/air/wraith): it enters
     * at the top edge in its lane and flies straight down it at its speed, past the ship and off the
     * bottom edge; {@code gapSeconds} below it, it comes back up its lane to its {@link Hover} depth
     * (the hold point), decloaks there (its {@link Cloak}'s flash) and holds its hover time, its gun
     * counting its first-shot delay from the stop; then it leaves up the nearer side lane, its centre
     * {@code lane} px from that edge (a tie goes left), at {@code exitSpeed} px/s.
     */
    public record Ambush(double gapSeconds, double lane, double exitSpeed) {
        public Ambush {
            if (!(gapSeconds >= 0) || !(lane >= 0) || !(exitSpeed > 0)) {
                throw new IllegalArgumentException("an ambush has a gap, a lane and an exit speed");
            }
        }
    }

    /**
     * M5 part D, a flock (design/enemies/air/mote-swarm, user decision D8 = a): the members of a
     * {@code swarm} wave steer round a leader point that flies the wave's route. Each member keeps
     * {@code separation} px from the others, matches the heading of ({@code alignment}) and closes on
     * ({@code cohesion}) the members within {@code radius} px, and is pulled toward the leader point
     * ({@code leader}); it turns at most {@code turnRate} radians per second and flies about
     * {@code speed} px/s, {@code diveSpeed} after a loop-back. At most {@code max} members ({@link
     * Flock#MAX_MEMBERS} at the most).
     */
    public record FlockSpec(
            double separation,
            double radius,
            double alignment,
            double cohesion,
            double leader,
            double speed,
            double diveSpeed,
            double turnRate,
            int max) {
        public FlockSpec {
            if (!(separation > 0)
                    || !(radius >= separation)
                    || !(alignment >= 0)
                    || !(cohesion >= 0)
                    || !(leader > 0)
                    || !(speed > 0)
                    || !(diveSpeed > 0)
                    || !(turnRate > 0)
                    || max < 1
                    || max > Flock.MAX_MEMBERS) {
                throw new IllegalArgumentException("a flock has a separation inside its radius, a pull toward"
                        + " its leader, speeds, a turn rate and 1 to " + Flock.MAX_MEMBERS + " members");
            }
        }
    }

    /** The constructor of Level 08: without the hardened units, spawners and pounces of Level 09 (M5 part C). */
    public EnemySpec(
            String slug,
            double hp,
            Hitbox hitbox,
            Layer layer,
            double contactDamage,
            boolean destroyedByRamming,
            int bounty,
            double speed,
            Optional<Snake> snake,
            Optional<Double> streamSpeed,
            Optional<Hover> hover,
            Optional<Orbit> orbit,
            Optional<EnemyGun> gun,
            Optional<Drop> drop,
            Optional<Dive> dive,
            boolean terrain,
            Optional<Spiral> spiral,
            Optional<Range> strafe,
            Optional<DeathBurst> deathBurst,
            Optional<Sine> sine,
            Optional<Brood> brood,
            Optional<Walker> walker,
            Optional<SideHover> sideHover,
            Optional<Sweep> sweep,
            Optional<ChainSpec> chain) {
        this(
                slug,
                hp,
                hitbox,
                layer,
                contactDamage,
                destroyedByRamming,
                bounty,
                speed,
                snake,
                streamSpeed,
                hover,
                orbit,
                gun,
                drop,
                dive,
                terrain,
                spiral,
                strafe,
                deathBurst,
                sine,
                brood,
                walker,
                sideHover,
                sweep,
                chain,
                false,
                Optional.empty(),
                Optional.empty());
    }

    /**
     * A periodic spawner (design/enemies/ground/hive-node, M5 part C): every {@code everySeconds},
     * counted from when its centre crosses the top edge, its iris opens over {@code
     * telegraphSeconds} and then releases {@code count} units of {@code enemy} from its centre,
     * spread evenly over {@code arcRadians} centred on the direction to the ship and flying straight
     * out at {@code speed} px/s (as a {@link Brood}'s). An opening due while the ship's centre is
     * within {@code shutWithin} px of its own is skipped (no telegraph), and so is a release the ship
     * came that close to during the telegraph; the next one is due a whole cycle later. It never
     * bursts on its own.
     */
    public record Spawner(
            EnemySpec enemy,
            int count,
            double everySeconds,
            double telegraphSeconds,
            double arcRadians,
            double speed,
            double shutWithin) {
        public Spawner {
            if (count < 1 || !(everySeconds > telegraphSeconds) || telegraphSeconds < 0 || shutWithin < 0) {
                throw new IllegalArgumentException(
                        "a spawner releases at least one unit, its telegraph inside its cycle");
            }
        }
    }

    /**
     * A pounce (design/enemies/ground/ravager, M5 part C): a walker on the screen whose centre is
     * within {@code range} px of the ship's leaps at the ship's position at take-off over {@code
     * leapSeconds} (no homing); for the middle {@code airSeconds} of the leap its current layer is
     * {@link Layer#AIR} (contact with the ship and the wingman once per leap, air-reaching weapons,
     * mines), else {@link Layer#GROUND}; drawn up to {@code scale} at the apex. It lands, rejoins its
     * path and may leap again {@code intervalSeconds} after landing.
     */
    public record Pounce(double range, double leapSeconds, double airSeconds, double scale, double intervalSeconds) {
        public Pounce {
            if (!(range > 0)
                    || !(leapSeconds > 0)
                    || airSeconds < 0
                    || airSeconds > leapSeconds
                    || !(scale > 0)
                    || intervalSeconds < 0) {
                throw new IllegalArgumentException("a pounce has a range, a leap and its air window inside it");
            }
        }
    }

    /** The constructor of Level 05: without the side hover, sweep and chain of Level 06. */
    public EnemySpec(
            String slug,
            double hp,
            Hitbox hitbox,
            Layer layer,
            double contactDamage,
            boolean destroyedByRamming,
            int bounty,
            double speed,
            Optional<Snake> snake,
            Optional<Double> streamSpeed,
            Optional<Hover> hover,
            Optional<Orbit> orbit,
            Optional<EnemyGun> gun,
            Optional<Drop> drop,
            Optional<Dive> dive,
            boolean terrain,
            Optional<Spiral> spiral,
            Optional<Range> strafe,
            Optional<DeathBurst> deathBurst,
            Optional<Sine> sine,
            Optional<Brood> brood,
            Optional<Walker> walker) {
        this(
                slug,
                hp,
                hitbox,
                layer,
                contactDamage,
                destroyedByRamming,
                bounty,
                speed,
                snake,
                streamSpeed,
                hover,
                orbit,
                gun,
                drop,
                dive,
                terrain,
                spiral,
                strafe,
                deathBurst,
                sine,
                brood,
                walker,
                Optional.empty(),
                Optional.empty(),
                Optional.empty());
    }

    /**
     * A unit that enters from a side edge and hovers {@code edgeX} px from it (design/enemies/air/mantis);
     * with {@code exitBack} it leaves through that edge when its hover ends.
     */
    public record SideHover(double edgeX, boolean exitBack) {}

    /**
     * A laser sweep (design/enemies/air/mantis): while it hovers, every {@code intervalSeconds} (the
     * first {@code firstDelaySeconds} after it settles) a telegraph of {@code telegraphSeconds}
     * fixes the sweep's centre on the ship's bearing, clamped between straight inward and straight
     * down; then a beam {@code length} × {@code width} px from its eye sweeps {@code arcRadians}
     * over {@code sweepSeconds}, dealing {@code damage} at most once per sweep. A sweep starts only
     * if it ends before the hover does. The eye, where the beam and its telegraph start, is
     * {@code originIn} px from the unit's centre toward the field (mirrored on the right edge) and
     * {@code originDown} px down the screen.
     */
    public record Sweep(
            double arcRadians,
            double sweepSeconds,
            double telegraphSeconds,
            double length,
            double width,
            double intervalSeconds,
            double firstDelaySeconds,
            double damage,
            double originIn,
            double originDown) {

        /** A sweep from the unit's centre. */
        public Sweep(
                double arcRadians,
                double sweepSeconds,
                double telegraphSeconds,
                double length,
                double width,
                double intervalSeconds,
                double firstDelaySeconds,
                double damage) {
            this(
                    arcRadians,
                    sweepSeconds,
                    telegraphSeconds,
                    length,
                    width,
                    intervalSeconds,
                    firstDelaySeconds,
                    damage,
                    0,
                    0);
        }
    }

    /**
     * A segment chain (design/enemies/air/coilwyrm): the unit is its head, flying the wave's path;
     * behind it {@code segmentBoxes.size()} segments of {@code segment} (each with its own hit box)
     * and the {@code tail} follow the head's path history, member {@code i} at {@code offsets[i]} px
     * of history behind the head (the head's is 0). A segment's destruction cuts the chain: the
     * rear part grows a {@code regrown} head over {@code regrowSeconds} (once per chain, and not
     * when the rear part died meanwhile) and lunges at the ship at {@code regrowSpeed}; a second cut
     * kills the severed part from the cut backwards, one member every {@code popSeconds}, as the
     * head's death does with the whole chain. The tail pays {@code tailFirstBonus} more when it is
     * destroyed before any other part. The head is a weak point: every damage it takes is
     * multiplied by {@code headMultiplier} (its part's {@code multiplier}, like a set piece's part);
     * a regrown head's is 1.
     */
    public record ChainSpec(
            EnemySpec segment,
            java.util.List<Hitbox> segmentBoxes,
            EnemySpec tail,
            int tailFirstBonus,
            EnemySpec regrown,
            double regrowSeconds,
            double regrowSpeed,
            java.util.List<Double> offsets,
            double popSeconds,
            double headMultiplier) {
        public ChainSpec {
            segmentBoxes = java.util.List.copyOf(segmentBoxes);
            offsets = java.util.List.copyOf(offsets);
            if (offsets.size() != segmentBoxes.size() + 2) {
                throw new IllegalArgumentException("a chain has an offset per member: head, segments, tail");
            }
            if (!(headMultiplier > 0)) {
                throw new IllegalArgumentException("a chain's head multiplier is positive: " + headMultiplier);
            }
        }

        /** A chain whose head takes plain damage (multiplier 1). */
        public ChainSpec(
                EnemySpec segment,
                java.util.List<Hitbox> segmentBoxes,
                EnemySpec tail,
                int tailFirstBonus,
                EnemySpec regrown,
                double regrowSeconds,
                double regrowSpeed,
                java.util.List<Double> offsets,
                double popSeconds) {
            this(
                    segment,
                    segmentBoxes,
                    tail,
                    tailFirstBonus,
                    regrown,
                    regrowSeconds,
                    regrowSpeed,
                    offsets,
                    popSeconds,
                    1);
        }

        /** The members: head, segments and tail. */
        public int members() {
            return offsets.size();
        }
    }

    /** A snake's units follow one another {@code spacingSeconds} apart. */
    public record Snake(double spacingSeconds) {}

    /** Hovers for {@code seconds}, {@code depth} px below the top edge of the play field. */
    public record Hover(Range seconds, Range depth) {}

    /** Circles a point at {@code radius} px and {@code degreesPerSecond}. */
    public record Orbit(double radius, double degreesPerSecond) {}

    /** Every {@code every}-th kill of this enemy in an attempt drops {@code pickup}. */
    public record Drop(PickupType pickup, int every) {}

    /**
     * Pauses {@code depth} px below the top edge for {@code pauseSeconds}, then dives at
     * {@code speed} px/s at where the ship was, firing once when it passes the ship's height or
     * {@code fireAfterSeconds} into the dive.
     */
    public record Dive(Range depth, double pauseSeconds, double speed, double fireAfterSeconds) {}

    /**
     * Circles its release point for {@code seconds} at {@code turnsPerSecond} while its radius grows
     * {@code growth} px/s and the release point drifts down {@code drift} px/s; then flies on at its
     * speed along its outward angle turned downward, bouncing off the side edges up to
     * {@code ricochets} times.
     */
    public record Spiral(double seconds, double turnsPerSecond, double growth, double drift, int ricochets) {}

    /** {@code count} bullets in a ring at {@code speed} px/s, each dealing {@code damage}. */
    public record DeathBurst(int count, double speed, double damage) {}

    /** Swings {@code amplitude} px to each side of its path, {@code periodSeconds} per full swing. */
    public record Sine(double amplitude, double periodSeconds) {}

    /**
     * A spawner (design/enemies/air/brood-pod): {@code afterSeconds} after its centre crosses the
     * top edge it bursts on its own (the pulse speeding up over the last {@code telegraphSeconds}),
     * paying {@code burstBounty} instead of its bounty, which is not a kill; destroyed or burst, it
     * releases {@code count} units of {@code enemy} from its centre, spread evenly over
     * {@code arcRadians} centred on the direction to the ship and flying straight out at
     * {@code speed} px/s.
     */
    public record Brood(
            EnemySpec enemy,
            int count,
            double afterSeconds,
            double telegraphSeconds,
            double arcRadians,
            double speed,
            int burstBounty) {}

    /**
     * A walker (design/enemies/ground/scuttler): it walks its wave's ground path at {@code speed}
     * px/s over the ground, its facing turning at most {@code turnRate} radians per second; one walk
     * cycle per {@code stride} px. Direct shots arriving within {@code frontArc} radians of its
     * facing glance off; its {@link EnemySpec#gun()} is a fan along its facing (the Scuttler) or, with
     * {@code fanAtShip}, centred on the ship (the Creeper, M5 part B), and {@code spit} an aimed
     * attack that fires only while the ship is more than {@code awayRadians} off its facing.
     *
     * @param staggerSeconds M5 part B (design/enemies/ground/creeper): with more than 0, the units of
     *     one wave share a volley clock that starts as the first of them comes onto the screen; unit
     *     i (in the order they enter) fires its fan i × this many seconds after the volley's start,
     *     the first volley half an interval after the clock starts and then one every interval; a
     *     unit off the screen skips its turn. 0: each unit keeps its own clock
     */
    public record Walker(
            double speed,
            double turnRate,
            double stride,
            double frontArc,
            Optional<EnemyGun> spit,
            double awayRadians,
            boolean fanAtShip,
            double staggerSeconds) {
        /** A walker whose fan goes along its facing, each unit on its own clock (Level 04's Scuttler). */
        public Walker(
                double speed,
                double turnRate,
                double stride,
                double frontArc,
                Optional<EnemyGun> spit,
                double awayRadians) {
            this(speed, turnRate, stride, frontArc, spit, awayRadians, false, 0);
        }

        /** Whether the units of a wave share a staggered volley clock. */
        public boolean staggered() {
            return staggerSeconds > 0;
        }
    }

    /** The constructor of Level 03: without the spawners and walkers of Level 04. */
    public EnemySpec(
            String slug,
            double hp,
            Hitbox hitbox,
            Layer layer,
            double contactDamage,
            boolean destroyedByRamming,
            int bounty,
            double speed,
            Optional<Snake> snake,
            Optional<Double> streamSpeed,
            Optional<Hover> hover,
            Optional<Orbit> orbit,
            Optional<EnemyGun> gun,
            Optional<Drop> drop,
            Optional<Dive> dive,
            boolean terrain,
            Optional<Spiral> spiral,
            Optional<Range> strafe,
            Optional<DeathBurst> deathBurst) {
        this(
                slug,
                hp,
                hitbox,
                layer,
                contactDamage,
                destroyedByRamming,
                bounty,
                speed,
                snake,
                streamSpeed,
                hover,
                orbit,
                gun,
                drop,
                dive,
                terrain,
                spiral,
                strafe,
                deathBurst,
                Optional.empty(),
                Optional.empty(),
                Optional.empty());
    }

    /** The current constructor with the movements and bursts of Level 03 left out. */
    public EnemySpec(
            String slug,
            double hp,
            Hitbox hitbox,
            Layer layer,
            double contactDamage,
            boolean destroyedByRamming,
            int bounty,
            double speed,
            Optional<Snake> snake,
            Optional<Double> streamSpeed,
            Optional<Hover> hover,
            Optional<Orbit> orbit,
            Optional<EnemyGun> gun,
            Optional<Drop> drop,
            Optional<Dive> dive,
            boolean terrain) {
        this(
                slug,
                hp,
                hitbox,
                layer,
                contactDamage,
                destroyedByRamming,
                bounty,
                speed,
                snake,
                streamSpeed,
                hover,
                orbit,
                gun,
                drop,
                dive,
                terrain,
                Optional.empty(),
                Optional.empty(),
                Optional.empty());
    }
}
