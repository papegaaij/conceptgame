package vanguard.sim;

import java.util.Optional;

/**
 * The special slot in flight (design/player/specials): the charges carried, the special button and
 * the Airstrike. A press calls a strike when the slot has a charge and no strike is flying; while
 * one flies the press waits for the input buffer, and a press that finds no charge, no special or
 * a strike still flying after the buffer is denied. A call uses a charge; after the strike's delay
 * two bombers enter at the bottom edge left and right of the ship's x at the call and fly up,
 * dropping a bomb every bomb spacing. A bomb bursts after its fall where the ground scroll has
 * carried its release point: it hits the {@code ground} and {@code low-air} targets (hardened
 * included) and the {@code air} targets in its radius, never {@code high-air}, with at most the
 * layer's cap per target in one strike. Its kills pay their bounty and keep the chain, as the
 * guns' kills do ({@link PlayerFire.Hits}). A new strike can be called once the bombers have left
 * the screen.
 *
 * <p>Charges found in the level (the special charge pickup) count up to the most carried. The
 * sortie reports the charges used and found; the campaign applies them for a won level only, so a
 * retry starts with the level-start charges again. Without a fitted special nothing here is part
 * of the state hash, so recordings without the special button replay as before.
 */
public final class SpecialSlot {
    /** The two bombers. */
    public static final int BOMBERS = 2;
    /** The targets one strike can track for its caps; more are not damaged. */
    private static final int LEDGER_CAPACITY = 128;

    private final SpecialSpec spec;
    private final AirstrikeSpec airstrike;
    private final SimEvents events;
    private final Pool<AirstrikeBomb> bombs;
    private final int bufferTicks;
    private final int delayTicks;
    private final int fallTicks;

    private int charges;
    private int used;
    private int found;
    /** Whether the special button was down in the last step: a press is a step it goes down. */
    private boolean held;
    /** Steps a press still waits for the strike in flight; 0 for none. */
    private int buffer;
    /** Steps since the call of the strike in flight; -1 when none flies. */
    private int sinceCall = -1;

    private double callX;
    private boolean bombersIn;
    private final double[] bomberX = new double[BOMBERS];
    private double bomberY;
    private double prevBomberY;
    private double nextDrop;
    private int blasts;

    /** Per target struck in this strike: its key (as {@link Shot#struck}'s) and the damage dealt. */
    private final int[] ledgerKeys = new int[LEDGER_CAPACITY];

    private final double[] ledgerDealt = new double[LEDGER_CAPACITY];
    private int ledgerSize;

    SpecialSlot(Optional<SpecialSpec> fitted, SimEvents events) {
        spec = fitted.orElse(null);
        airstrike = spec == null ? null : spec.airstrike();
        this.events = events;
        int capacity = airstrike == null ? 0 : BOMBERS * ((int) (PlayField.HEIGHT / airstrike.bombSpacing()) + 1);
        bombs = new Pool<>(capacity, AirstrikeBomb::new, AirstrikeBomb[]::new);
        bufferTicks = spec == null ? 0 : SimStep.ticks(spec.bufferSeconds());
        delayTicks = airstrike == null ? 0 : Math.max(1, SimStep.ticks(airstrike.delaySeconds()));
        fallTicks = airstrike == null ? 0 : Math.max(1, SimStep.ticks(airstrike.fallSeconds()));
        reset();
    }

    /** Back to the level start: the level-start charges, nothing flying. */
    void reset() {
        charges = spec == null ? 0 : spec.charges();
        used = 0;
        found = 0;
        held = false;
        buffer = 0;
        sinceCall = -1;
        bombersIn = false;
        ledgerSize = 0;
        blasts = 0;
        Pools.clear(bombs);
    }

    /**
     * The special button for one step: a press calls a strike, waits for the one in flight or is
     * denied. Only while {@code active} (the ship flies the level); other presses are ignored.
     */
    void command(int commands, boolean active, double shipX, double shipY) {
        boolean down = Command.SPECIAL.in(commands);
        boolean pressed = down && !held;
        held = down;
        if (!active) {
            buffer = 0;
            return;
        }
        if (pressed) {
            if (spec == null || charges == 0) {
                events.add(SimEvents.Type.SPECIAL_DENIED, shipX, shipY);
                return;
            }
            buffer = bufferTicks + 1;
        }
        if (buffer == 0) {
            return;
        }
        if (sinceCall < 0) {
            buffer = 0;
            call(shipX, shipY);
        } else if (--buffer == 0) {
            events.add(SimEvents.Type.SPECIAL_DENIED, shipX, shipY);
        }
    }

    private void call(double shipX, double shipY) {
        charges--;
        used++;
        sinceCall = 0;
        callX = shipX;
        events.add(SimEvents.Type.SPECIAL_CALLED, shipX, shipY);
    }

    /**
     * One step of the strike: the delay runs, the bombers fly and drop their bombs, and the bombs
     * fall with the ground scroll and burst on {@code enemies}, {@code ground} and the set pieces'
     * parts; what a blast destroys goes to {@code hits}.
     */
    void update(
            double groundScroll,
            Pool<Enemy> enemies,
            Pool<GroundObject> ground,
            SetPiece[] setPieces,
            PlayerFire.Hits hits) {
        if (airstrike == null) {
            return;
        }
        // The bombs in the air first, so one released in this step falls from the next.
        for (int i = bombs.size() - 1; i >= 0; i--) {
            AirstrikeBomb bomb = bombs.get(i);
            if (bomb.fall(groundScroll)) {
                double x = bomb.x();
                double y = bomb.y();
                bombs.free(i);
                blast(x, y, enemies, ground, setPieces, hits);
            }
        }
        if (sinceCall >= 0) {
            // The call's own step makes it 1: the bombers enter the delay's steps after that one.
            sinceCall++;
            if (sinceCall == delayTicks + 1) {
                enter();
            } else if (bombersIn) {
                flyBombers();
            }
        }
    }

    /** The bombers enter at the bottom edge, below it by half their length; the new strike's caps start. */
    private void enter() {
        double half = airstrike.bomber().width() / 2;
        bomberX[0] = Math.clamp(callX - airstrike.offset(), half, PlayField.WIDTH - half);
        bomberX[1] = Math.clamp(callX + airstrike.offset(), half, PlayField.WIDTH - half);
        bomberY = prevBomberY = -airstrike.bomber().height() / 2;
        nextDrop = airstrike.bombSpacing() / 2;
        bombersIn = true;
        ledgerSize = 0;
        blasts = 0;
        events.add(SimEvents.Type.AIRSTRIKE_INBOUND, (bomberX[0] + bomberX[1]) / 2, 0);
    }

    /** The bombers climb a step, drop a bomb each at every spacing they pass and are gone past the top edge. */
    private void flyBombers() {
        prevBomberY = bomberY;
        bomberY += airstrike.speed() * SimStep.SECONDS;
        while (nextDrop < PlayField.HEIGHT && bomberY >= nextDrop) {
            for (int b = 0; b < BOMBERS; b++) {
                AirstrikeBomb bomb = bombs.obtain();
                if (bomb != null) {
                    bomb.release(bomberX[b], nextDrop, fallTicks);
                }
            }
            nextDrop += airstrike.bombSpacing();
        }
        if (bomberY - airstrike.bomber().height() / 2 >= PlayField.HEIGHT) {
            bombersIn = false;
            sinceCall = -1;
        }
    }

    private void blast(
            double x,
            double y,
            Pool<Enemy> enemies,
            Pool<GroundObject> ground,
            SetPiece[] setPieces,
            PlayerFire.Hits hits) {
        events.add(SimEvents.Type.AIRSTRIKE_BLAST, x, y, blasts++);
        double radius = airstrike.blastRadius();
        for (int j = enemies.size() - 1; j >= 0; j--) {
            Enemy enemy = enemies.get(j);
            EnemySpec spec = enemy.spec();
            Layer layer = spec.layer();
            if (layer == Layer.HIGH_AIR
                    || !PlayerFire.onField(enemy)
                    || !PlayerFire.inBlast(x, y, radius, enemy.x(), enemy.y(), spec.hitbox())) {
                continue;
            }
            double amount = deal(2 * enemy.serial(), layer);
            if (amount > 0 && enemy.damage(amount, true)) {
                hits.enemyDestroyed(j);
            }
        }
        for (int j = ground.size() - 1; j >= 0; j--) {
            GroundObject object = ground.get(j);
            LevelScript.GroundObjectSpec spec = object.spec();
            if (!object.hittable() || !PlayerFire.inBlast(x, y, radius, object.x(), object.y(), spec.size())) {
                continue;
            }
            if (spec.trigger()) {
                if (object.countHit()) {
                    hits.triggerReleased(j);
                }
            } else {
                double amount = deal(2 * object.serial() + 1, Layer.GROUND);
                if (amount > 0 && object.damage(amount)) {
                    hits.groundDestroyed(j);
                }
            }
        }
        for (int k = 0; k < setPieces.length; k++) {
            SetPiece piece = setPieces[k];
            if (!piece.present() || piece.layer() == Layer.HIGH_AIR) {
                continue;
            }
            for (int p = 0; p < piece.partCount() && piece.present(); p++) {
                Hitbox box = piece.spec().parts().get(p).box();
                double px = piece.partX(p);
                double py = piece.partY(p);
                if (piece.partWrecked(p)
                        || !PlayField.overlaps(px, py, box)
                        || !PlayerFire.inBlast(x, y, radius, px, py, box)) {
                    continue;
                }
                double amount = deal(-1 - k * LevelScript.SetPieceSpec.MAX_PARTS - p, piece.layer());
                if (amount > 0 && piece.damagePart(p, amount)) {
                    hits.partDestroyed(k, p);
                }
            }
        }
    }

    /**
     * The damage a blast deals to the target with {@code key} on {@code layer}: the layer's damage,
     * cut to what its cap leaves after this strike's earlier blasts; recorded against the cap.
     */
    private double deal(int key, Layer layer) {
        boolean air = layer == Layer.AIR;
        double damage = air ? airstrike.airDamage() : airstrike.groundDamage();
        double cap = air ? airstrike.airCap() : airstrike.groundCap();
        int entry = 0;
        while (entry < ledgerSize && ledgerKeys[entry] != key) {
            entry++;
        }
        if (entry == ledgerSize) {
            if (ledgerSize == LEDGER_CAPACITY) {
                return 0;
            }
            ledgerKeys[entry] = key;
            ledgerDealt[entry] = 0;
            ledgerSize++;
        }
        double amount = Math.min(damage, cap - ledgerDealt[entry]);
        if (amount <= 0) {
            return 0;
        }
        ledgerDealt[entry] += amount;
        return amount;
    }

    /** A special charge pickup: one more charge, up to the most carried; returns whether it counted. */
    boolean collect() {
        if (spec == null || charges >= spec.maxCharges()) {
            return false;
        }
        charges++;
        found++;
        return true;
    }

    void addTo(StateHash hash) {
        if (spec == null) {
            return;
        }
        hash.add(charges)
                .add(used)
                .add(found)
                .add(held ? 1 : 0)
                .add(buffer)
                .add(sinceCall)
                .add(callX)
                .add(bombersIn ? 1 : 0)
                .add(bomberX[0])
                .add(bomberX[1])
                .add(bomberY)
                .add(prevBomberY)
                .add(nextDrop)
                .add(blasts)
                .add(ledgerSize);
        for (int i = 0; i < ledgerSize; i++) {
            hash.add(ledgerKeys[i]).add(ledgerDealt[i]);
        }
        Pools.addAll(hash, bombs);
    }

    /** Whether a special is fitted. */
    public boolean fitted() {
        return spec != null;
    }

    /** The fitted special's name; empty without one. */
    public String name() {
        return spec == null ? "" : spec.name();
    }

    /** The charges left. */
    public int charges() {
        return charges;
    }

    public int maxCharges() {
        return spec == null ? 0 : spec.maxCharges();
    }

    /** Charges used in this attempt. */
    public int used() {
        return used;
    }

    /** Charges found in this attempt (special charge pickups that counted). */
    public int found() {
        return found;
    }

    /** Whether a strike is flying: called and its bombers not yet past the top edge. */
    public boolean busy() {
        return sinceCall >= 0;
    }

    /** Whether a press now calls a strike. */
    public boolean ready() {
        return spec != null && charges > 0 && sinceCall < 0;
    }

    /** Whether the bombers are on their way up the screen. */
    public boolean bombersIn() {
        return bombersIn;
    }

    public double bomberX(int b) {
        return bomberX[b];
    }

    public double bomberRenderY(double alpha) {
        return prevBomberY + (bomberY - prevBomberY) * alpha;
    }

    /** The bomber's sprite box; meaningful with a special fitted. */
    public Hitbox bomber() {
        return airstrike.bomber();
    }

    public int bombCount() {
        return bombs.size();
    }

    public AirstrikeBomb bomb(int index) {
        return bombs.get(index);
    }
}
