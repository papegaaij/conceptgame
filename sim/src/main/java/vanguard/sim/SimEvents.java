package vanguard.sim;

/**
 * What happened during the last step, for the presentation (sound effects, explosions, HUD
 * flashes, radio); the simulation never calls the presentation itself. Fixed capacity in parallel
 * arrays, so recording an event never allocates; events beyond the capacity are dropped, which is
 * harmless because they are not part of the game state.
 */
public final class SimEvents {
    /** Event kinds; the position is where it happened, in play-field pixels, and the value is named per kind. */
    public enum Type {
        /** A weapon fired a volley (at its muzzles); value: the mount's index in the sortie's {@link Armament}. */
        SHOT_FIRED,
        /** A shot hit an enemy (at the shot); value: the mount that fired it. */
        ENEMY_HIT,
        /** An enemy was destroyed (at the enemy); value: its kind, an index into {@link Sortie#enemyKinds()}. */
        ENEMY_DESTROYED,
        /**
         * An enemy fired a shot (at the enemy); value: its bullets' damage in whole points, which tells
         * the bullet class (a {@code medium} bullet deals 6, a {@code small} one less).
         */
        ENEMY_FIRED,
        /** A shot hit a ground object (at the shot); value: the mount that fired it. */
        GROUND_HIT,
        /**
         * A shot glanced off a hardened ground target or (M5 part C) a hardened enemy without damage
         * (at the shot; a blast's glance at the enemy); value: the mount.
         */
        SHOT_GLANCED,
        /** A bomb or shell burst on the ground (at its landing point); value: the mount that fired it. */
        BLAST,
        /** The overdrive ran out (at the ship). */
        OVERDRIVE_ENDED,
        /** A shot hit a crane's clamp (at the shot); value: the mount that fired it. */
        CLAMP_HIT,
        /** A group of the secondary objective was cleared; value: the group. */
        GROUP_CLEARED,
        /** A group was lost; value: the group. */
        GROUP_LOST,
        /** A mine layer dropped a spore (at the layer). */
        MINE_DROPPED,
        /** A spore burst into its ring, or on contact with the ship (at the spore). */
        MINE_BURST,
        /** A spore was shot (at the spore). */
        MINE_DESTROYED,
        /** A shot hit a small debris chunk (at the shot); a large one makes it glance ({@link #SHOT_GLANCED}). */
        DEBRIS_HIT,
        /** A small debris chunk broke (at the chunk). */
        DEBRIS_DESTROYED,
        /** A set piece reached the player's layer (at the set piece); value: its index. */
        SET_PIECE_DESCENDED,
        /** A part of a set piece was destroyed (at the part); value: the part's index. */
        PART_DESTROYED,
        /** A set piece was destroyed (at its centre, its chained death); value: its index. */
        SET_PIECE_DESTROYED,
        /** A set piece ended its last pass alive (at its centre); value: its index. */
        SET_PIECE_ESCAPED,
        /** A destructible ground object was destroyed (at the object); value: its index in the script. */
        GROUND_DESTROYED,
        /** A trigger released its secret's hidden crate (at the trigger). */
        SECRET_FOUND,
        /**
         * A ground trigger took its last hit and is spent (at the trigger), before the
         * {@link #SECRET_FOUND} it may bring (with the last of several triggers sharing a secret);
         * value: its index in the script's ground objects. Level 08's billboard topples with it.
         */
        TRIGGER_SPENT,
        /** The ship collected a pickup (at the pickup); value: its {@link PickupType} ordinal. */
        PICKUP_COLLECTED,
        /** Credits were picked up (at the pickup); value: the credits. */
        CREDITS_PICKED_UP,
        /** The shield absorbed damage (at the ship). */
        SHIELD_HIT,
        /** The shield dropped to zero (at the ship). */
        SHIELD_BROKEN,
        /** The armour took damage (at the ship). */
        ARMOUR_HIT,
        /**
         * An armour hit left the armour at or below {@link Defences#CRITICAL_SHARE} of its maximum for
         * the first time in the attempt (at the ship): Okafor's low-armour radio line.
         */
        ARMOUR_CRITICAL,
        /** Armour reached zero (at the ship). */
        SHIP_DESTROYED,
        /** A radio cue starts; value: its index in the level script's radio list. */
        RADIO,
        /** The secondary objective was met. */
        OBJECTIVE_MET,
        /** The secondary objective failed (a unit of an escapes objective got away). */
        OBJECTIVE_FAILED,
        /** The scroll reached the end: the primary objective is met and the level is over. */
        LEVEL_COMPLETE,
        /** The level started again after the ship was destroyed (at the ship's start position). */
        SORTIE_RESTARTED,
        /** The special was called and used a charge (at the ship): the Airstrike's radio call. */
        SPECIAL_CALLED,
        /** The special button found no charge, no special, or a strike still flying (at the ship). */
        SPECIAL_DENIED,
        /** The Airstrike's bombers entered at the bottom edge (between them). */
        AIRSTRIKE_INBOUND,
        /** An Airstrike bomb burst on the ground (at its landing point); value: its number in the strike, from 0. */
        AIRSTRIKE_BLAST,
        /** A convoy unit took a hit, or a walker's claws started on it (at the unit); value: its index. */
        ALLY_HIT,
        /** A convoy unit was destroyed (at the unit); value: its index. M5 part D: not for the scripted loss. */
        ALLY_LOST,
        /** The primary objective failed (the last convoy unit was lost): the level fails without a wreck (at the ship). */
        PRIMARY_FAILED,
        /**
         * A spawner released its units, destroyed or bursting on its own (at its centre); value: its
         * kind. A self-burst has no {@link #ENEMY_DESTROYED}: this event is its end.
         */
        BROOD_HATCHED,
        /** A spawner burst on its own (at its centre), after {@link #BROOD_HATCHED}; value: its kind. */
        BROOD_BURST,
        /**
         * A walker was destroyed (at it), after its {@link #ENEMY_DESTROYED}, for its remains at its
         * last heading; value: {@link #walkerValue(int, double)}.
         */
        WALKER_DOWN,
        /** A boss arrived: it starts its descent, its bar and its sting start (at its centre); value: its set piece's index. */
        BOSS_ARRIVED,
        /** A boss ended its descent (at its centre); value: its set piece's index. */
        BOSS_SETTLED,
        /** A boss entered a phase after its first (at its centre); value: the phase's index. */
        BOSS_PHASE,
        /**
         * A boss was destroyed (at its centre), after its {@link #SET_PIECE_DESTROYED}: the credit
         * shower; value: the credits its parts paid.
         */
        BOSS_DESTROYED,
        /** A Retry from boss restarted the level at its boss checkpoint (at the ship). */
        BOSS_RETRY,
        /** A mortar lobbed a blob (at the mortar). */
        MORTAR_LOBBED,
        /** A lob landed and burst (at its marker); value: 1 for a direct hit on the ship, else 0. */
        MORTAR_IMPACT,
        /** The rail lights start their chase before a sled (at the rail's foot). */
        SLED_LIGHTS,
        /** A sled shoots up the rail (at the rail's foot). */
        SLED_LAUNCHED,
        /** A sled struck the ship (at the ship). */
        SLED_HIT,
        /** A destroyed ground target threw a rock (at the rock). */
        ROCK_THROWN,
        /** A chain's segment was cut and its rear part starts to grow a new head (at the cut); value: the segment's kind. */
        CHAIN_CUT,
        /** A cut chain's new head has grown (at it); value: its kind. */
        CHAIN_REGROWN,
        /**
         * A dying chain member burst (at it), paying nothing; value: its kind and its hit box's width
         * ({@link #chainPopKind}, {@link #chainPopWidth}), so the burst can follow the chain's taper.
         */
        CHAIN_POP,
        /** A laser sweep's telegraph started (at the unit); value: its kind. */
        SWEEP_TELEGRAPH,
        /** A laser sweep's beam started (at the unit); value: its kind. */
        SWEEP_FIRED,
        /** A laser sweep's beam hit the ship (at the ship). */
        SWEEP_HIT,
        /** A Smart Bomb went off (at the ship). */
        SMART_BOMB,
        /** A scripted flare was fired (at its start); value: its index. */
        FLARE_FIRED,
        /**
         * Part G: a boss's open window launched a unit (at the window part; the Brood Carrier's sacs);
         * value: the unit's kind.
         */
        BOSS_LAUNCHED,
        /**
         * M5 part A: the wingman took damage (at him); value: the damage, rounded up. His shots carry
         * {@link Sortie#wingmanMount()} as their mount in the shot events.
         */
        WINGMAN_HIT,
        /** The wingman's armour dropped below his low-armour share, once per attempt (at him): his bark. */
        WINGMAN_CRITICAL,
        /**
         * The wingman's armour reached zero: he ejects, his craft explodes (at him); value: the pod's
         * drift, -1 to the left edge, 1 to the right.
         */
        WINGMAN_EJECTED,
        /**
         * M5 part A: one of the ship's proximity mines armed (at the mine), 0.4 s after its drop: its
         * arming beep; value: the mount that dropped it.
         */
        PROXIMITY_MINE_ARMED,
        /**
         * M5 part C: a periodic spawner's iris started to open, the telegraph of its release (at its
         * centre; design/enemies/ground/hive-node); value: its kind.
         */
        SPAWN_TELEGRAPH,
        /** M5 part C: a periodic spawner released its units (at its centre); value: its kind. */
        SPAWN_RELEASED,
        /** M5 part C: a walker took off for a pounce (at it; design/enemies/ground/ravager); value: its kind. */
        POUNCE,
        /** M5 part C: a pounce landed (at it); value: its kind. */
        POUNCE_LANDED,
        /**
         * M5 part C: a hold zone started (at the ship; design/campaign Level 09): the scroll eases down
         * to its speed from the next step; value: the hold's index. The music's {@code full_on: hold}
         * fades in from here.
         */
        HOLD_START,
        /**
         * M5 part C: a hold zone ended, every unit of its groups gone (at the ship): the scroll eases
         * back to the section's speed; value: the hold's index. The {@code full_on: hold} mix fades
         * out from here.
         */
        HOLD_END,
        /**
         * M5 part C: the collapse started (at the ship): its tower leans (the warning) over the band,
         * {@link Sortie#collapseBandTop()} to {@link Sortie#collapseBandBottom()}.
         */
        COLLAPSE_WARNING,
        /** M5 part C: the collapse's tower starts to drop straight down (at the ship), the warning over. */
        COLLAPSE_FALL,
        /**
         * M5 part C: the collapse's tower hits the ground (at the ship): its blast rolls out from the
         * foot, killing the ground units in the band as it reaches them ({@link Sortie#collapseBlastRadius()}).
         */
        COLLAPSE_IMPACT,
        /** M5 part C: the collapse's blast has rolled out (at the ship): it is over, a hold through it ends. */
        COLLAPSE_END,
        /**
         * M5 part D: a cloaked unit decloaks at its hold point, the start of its flash (at it; design/
         * enemies/air/wraith): on its decloaked layer from here, its gun silent until the flash ends;
         * the decloak sound and the radio event {@code first-decloak}; value: its kind.
         */
        DECLOAK,
        /**
         * M5 part D: a swarm's leader point re-enters below the bottom edge for a loop-back (at the
         * leader point; design/enemies/air/mote-swarm), when the bottom edge's warning ends: the
         * swarm's sound and the radio event {@code first-loop-back}; value: the loop-back's number,
         * from 1.
         */
        LOOP_BACK,
        /**
         * M5 part D: the scripted loss's glow starts over its unit (at the unit; design/campaign Level
         * 10, the alien glow in the cloud deck, which follows the unit's sway until the lance); value:
         * the unit's index.
         */
        LOSS_GLOW,
        /**
         * M5 part D: the scripted loss's lance takes its unit (at the unit): the lance, its sound and
         * the music's duck; the unit is lost (it glides down, a presentation effect) without an
         * {@link #ALLY_LOST} event; value: the unit's index.
         */
        SCRIPTED_LOSS;

        private static final Type[] VALUES = values();
    }

    /** A {@link Type#WALKER_DOWN} value: the kind and its facing (radians clockwise from straight down). */
    static int walkerValue(int kind, double facing) {
        double turn = 2 * StrictMath.PI;
        return kind * WALKER_KIND + (int) Math.floor(((facing % turn) + turn) % turn * 1000);
    }

    /** The kind of a {@link Type#WALKER_DOWN} value. */
    public static int walkerKind(int value) {
        return value / WALKER_KIND;
    }

    /** The facing of a {@link Type#WALKER_DOWN} value, radians clockwise from straight down. */
    public static double walkerFacing(int value) {
        return value % WALKER_KIND / 1000.0;
    }

    private static final int WALKER_KIND = 10_000;

    /** A {@link Type#CHAIN_POP} value: the member's kind and its hit box's width, px (to 0.1 px, below 100). */
    static int chainPopValue(int kind, double width) {
        return kind * CHAIN_POP_KIND + (int) Math.min(CHAIN_POP_KIND - 1, Math.round(width * 10));
    }

    /** The kind of a {@link Type#CHAIN_POP} value. */
    public static int chainPopKind(int value) {
        return value / CHAIN_POP_KIND;
    }

    /** The hit box width of a {@link Type#CHAIN_POP} value, px. */
    public static double chainPopWidth(int value) {
        return value % CHAIN_POP_KIND / 10.0;
    }

    private static final int CHAIN_POP_KIND = 1_000;

    private final int[] types;
    private final double[] xs;
    private final double[] ys;
    private final int[] values;
    private int size;

    SimEvents(int capacity) {
        types = new int[capacity];
        xs = new double[capacity];
        ys = new double[capacity];
        values = new int[capacity];
    }

    void add(Type type, double x, double y) {
        add(type, x, y, 0);
    }

    void add(Type type, double x, double y, int value) {
        if (size < types.length) {
            types[size] = type.ordinal();
            xs[size] = x;
            ys[size] = y;
            values[size] = value;
            size++;
        }
    }

    void clear() {
        size = 0;
    }

    public int size() {
        return size;
    }

    public Type type(int index) {
        return Type.VALUES[types[index]];
    }

    public double x(int index) {
        return xs[index];
    }

    public double y(int index) {
        return ys[index];
    }

    /** The event's value; its meaning depends on the {@link Type}. */
    public int value(int index) {
        return values[index];
    }

    /** How many events of {@code type} happened in the last step. */
    public int count(Type type) {
        int count = 0;
        for (int i = 0; i < size; i++) {
            if (types[i] == type.ordinal()) {
                count++;
            }
        }
        return count;
    }
}
