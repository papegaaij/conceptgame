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
 * included) and the {@code air} targets in its radius, never {@code high-air} nor (M5 part E)
 * {@code sub} under the water or a sunken trigger, with at most the layer's cap per target in one
 * strike. Its kills pay their bounty and keep the chain, as the
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
    /** The Smart Bomb's numbers; null for an Airstrike or no special. */
    private final SmartBombSpec smartBomb;
    /** The ship's defences, which a Smart Bomb guards; null without one. */
    private final Defences defences;

    private final int ringTicks;
    private final int repeatTicks;
    private double bombX;
    private double bombY;
    /** The ring's radius when it covers the whole play field from the bomb's centre. */
    private double ringReach;

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
        this(fitted, events, null);
    }

    /** @param defences the ship's, which a Smart Bomb guards */
    SpecialSlot(Optional<SpecialSpec> fitted, SimEvents events, Defences defences) {
        spec = fitted.orElse(null);
        airstrike = spec == null ? null : spec.airstrike();
        smartBomb = spec == null ? null : spec.smartBomb();
        this.defences = defences;
        ringTicks = smartBomb == null ? 0 : Math.max(1, SimStep.ticks(smartBomb.ringSeconds()));
        repeatTicks = smartBomb == null ? 0 : Math.max(1, SimStep.ticks(smartBomb.repeatSeconds()));
        this.events = events;
        int capacity = airstrike == null ? 0 : BOMBERS * ((int) (PlayField.HEIGHT / airstrike.bombSpacing()) + 1);
        bombs = new Pool<>(capacity, AirstrikeBomb::new, AirstrikeBomb[]::new);
        bufferTicks = spec == null ? 0 : SimStep.ticks(spec.bufferSeconds());
        delayTicks = airstrike == null ? 0 : Math.max(1, SimStep.ticks(airstrike.delaySeconds()));
        fallTicks = airstrike == null ? 0 : Math.max(1, SimStep.ticks(airstrike.fallSeconds()));
        reset();
    }

    /** Back to a boss checkpoint: its charges, those used and found until then, nothing flying. */
    void restore(int charges, int used, int found) {
        reset();
        this.charges = charges;
        this.used = used;
        this.found = found;
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
        bombX = 0;
        bombY = 0;
        ringReach = 0;
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
        if (smartBomb != null) {
            bombX = shipX;
            bombY = shipY;
            double dx = Math.max(shipX, PlayField.WIDTH - shipX);
            double dy = Math.max(shipY, PlayField.HEIGHT - shipY);
            ringReach = Math.sqrt(dx * dx + dy * dy);
            ledgerSize = 0;
            if (defences != null) {
                defences.guard(SimStep.ticks(smartBomb.invulnerableSeconds()));
            }
            events.add(SimEvents.Type.SMART_BOMB, shipX, shipY);
        }
    }

    /**
     * One step of a Smart Bomb: its ring grows; at once every enemy bullet (and mortar blob) goes,
     * then those the ring passes; the spore mines it passes pop as if shot; every enemy (M5 part E:
     * those under the water too) and boss part on the screen the ring reaches takes its damage once,
     * and a sunken trigger it reaches is spent at once (M5 part E). It is busy until its repeat time
     * has passed.
     */
    void bomb(
            Pool<Enemy> enemies,
            Pool<GroundObject> ground,
            SetPiece[] setPieces,
            PlayerFire.Hits hits,
            Pool<EnemyBullet> bullets,
            Pool<Lob> lobs,
            Pool<Mine> mines) {
        if (smartBomb == null || sinceCall < 0) {
            return;
        }
        sinceCall++;
        if (sinceCall > repeatTicks) {
            sinceCall = -1;
            return;
        }
        if (sinceCall > ringTicks) {
            return;
        }
        double radius = ringRadius();
        for (int i = bullets.size() - 1; i >= 0; i--) {
            EnemyBullet bullet = bullets.get(i);
            if (sinceCall == 1 || within(bullet.x(), bullet.y(), radius)) {
                bullets.free(i);
            }
        }
        for (int i = lobs.size() - 1; i >= 0; i--) {
            lobs.free(i);
        }
        for (int i = mines.size() - 1; i >= 0; i--) {
            Mine mine = mines.get(i);
            if (within(mine.x(), mine.y(), radius)) {
                hits.mineDestroyed(i);
            }
        }
        for (int j = enemies.size() - 1; j >= 0; j--) {
            Enemy enemy = enemies.get(j);
            if (!PlayerFire.onField(enemy) || !within(enemy.x(), enemy.y(), radius)) {
                continue;
            }
            if (strike(2 * enemy.serial()) && enemy.damage(smartBomb.damage(), true)) {
                hits.enemyDestroyed(j);
            }
        }
        for (int j = ground.size() - 1; j >= 0; j--) {
            GroundObject object = ground.get(j);
            if (object.spec().submerged()
                    && object.hittable()
                    && PlayField.overlaps(object.x(), object.y(), object.spec().size())
                    && within(object.x(), object.y(), radius)
                    && object.spend()) {
                hits.triggerReleased(j);
            }
        }
        for (int k = 0; k < setPieces.length; k++) {
            SetPiece piece = setPieces[k];
            for (int p = 0; p < piece.partCount() && piece.present(); p++) {
                Hitbox box = piece.spec().parts().get(p).box();
                double px = piece.partX(p);
                double py = piece.partY(p);
                if (piece.partWrecked(p)
                        || piece.partShielded(p)
                        || !PlayField.overlaps(px, py, box)
                        || !within(px, py, radius)) {
                    continue;
                }
                if (strike(-1 - k * LevelScript.SetPieceSpec.MAX_PARTS - p)
                        && piece.damagePart(p, smartBomb.bossPartDamage())) {
                    hits.partDestroyed(k, p);
                }
            }
        }
    }

    private boolean within(double x, double y, double radius) {
        double dx = x - bombX;
        double dy = y - bombY;
        return dx * dx + dy * dy <= radius * radius;
    }

    /** Records a strike on the target with {@code key}; false when the bomb struck it already (or the ledger is full). */
    private boolean strike(int key) {
        for (int entry = 0; entry < ledgerSize; entry++) {
            if (ledgerKeys[entry] == key) {
                return false;
            }
        }
        if (ledgerSize == LEDGER_CAPACITY) {
            return false;
        }
        ledgerKeys[ledgerSize] = key;
        ledgerDealt[ledgerSize] = smartBomb.damage();
        ledgerSize++;
        return true;
    }

    /** The Smart Bomb ring's radius now; 0 when none expands. */
    public double ringRadius() {
        if (smartBomb == null || sinceCall < 0 || sinceCall > ringTicks) {
            return 0;
        }
        return ringReach * sinceCall / ringTicks;
    }

    /** The ring's radius between the last step and this one; {@code alpha} in [0, 1]. */
    public double ringRadius(double alpha) {
        if (smartBomb == null || sinceCall < 0 || sinceCall > ringTicks) {
            return 0;
        }
        return ringReach * Math.max(0, sinceCall - 1 + alpha) / ringTicks;
    }

    /** Seconds since the last Smart Bomb went off, while it is busy; negative when none is. */
    public double bombSeconds(double alpha) {
        return smartBomb == null || sinceCall < 0 ? -1 : (sinceCall - 1 + alpha) * SimStep.SECONDS;
    }

    /** The Smart Bomb's centre: the ship's position when it went off. */
    public double bombX() {
        return bombX;
    }

    public double bombY() {
        return bombY;
    }

    /** The Smart Bomb's numbers; null when the fitted special is none. */
    public SmartBombSpec smartBomb() {
        return smartBomb;
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
            // Its current layer (M5 part C: a pounce's air window takes the air damage).
            Layer layer = enemy.layer();
            if (layer == Layer.HIGH_AIR
                    || layer == Layer.SUB
                    || !PlayerFire.onField(enemy)
                    || !PlayerFire.inBlast(x, y, radius, enemy.x(), enemy.y(), enemy.hitbox())) {
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
            if (!object.hittable()
                    || spec.submerged()
                    || !PlayerFire.inBlast(x, y, radius, object.x(), object.y(), spec.size())) {
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
            if (!piece.present()) {
                continue;
            }
            for (int p = 0; p < piece.partCount() && piece.present(); p++) {
                // M5 part E: on the part's own layer (an arena boss's part under the water is out of reach).
                Layer layer = piece.partLayer(p);
                if (layer == Layer.HIGH_AIR
                        || layer == Layer.SUB
                        || piece.partWrecked(p)
                        || piece.partShielded(p)
                        || !piece.partOnField(p)
                        || !piece.partInBlast(p, x, y, radius)) {
                    continue;
                }
                double amount = deal(-1 - k * LevelScript.SetPieceSpec.MAX_PARTS - p, layer);
                if (amount > 0 && piece.damagePartAt(p, amount, x, y)) {
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
        if (smartBomb != null) {
            hash.add(bombX).add(bombY).add(ringReach);
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
