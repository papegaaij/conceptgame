package vanguard.sim;

/**
 * The player's AF-12 Stormhawk: flies by commands with quick acceleration, stays inside the play
 * field, banks with its horizontal speed and fires its front gun while fire is held.
 */
public final class Ship {
    /** Where every sortie starts: centred in the lower third. */
    static final double START_X = PlayField.WIDTH / 2.0;

    static final double START_Y = 96;
    private static final double DIAGONAL = StrictMath.sqrt(0.5);

    private final ShipSpec spec;
    private final PulseCannon gun;
    private final Defences defences;
    private final double accelerationPerStep;
    private final double stopPerStep;
    private double x;
    private double y;
    private double prevX;
    private double prevY;
    private double vx;
    private double vy;
    private int bank;
    private int bankTimer;
    private int fireCooldown;
    private int ticksSinceShot;

    Ship(ShipSpec spec, PulseCannon gun, Defences defences) {
        this.spec = spec;
        this.gun = gun;
        this.defences = defences;
        this.accelerationPerStep = spec.speed() / spec.accelerationSeconds() * SimStep.SECONDS;
        this.stopPerStep = spec.speed() / spec.stopSeconds() * SimStep.SECONDS;
        reset(defences.maxArmour());
    }

    /**
     * The launch off the rail (design/campaign, Level 01: the non-playable launch): from below the
     * play field up to the start position, slowing down as it arrives. {@code progress} runs from 0
     * to 1; commands are ignored meanwhile.
     */
    void launch(double progress) {
        double remaining = 1 - progress;
        double railY = -spec.size();
        x = START_X;
        y = START_Y + (railY - START_Y) * remaining * remaining;
        vx = vy = 0;
    }

    /** Back at the start position, at rest, with full defences. */
    void reset(double armour) {
        x = prevX = START_X;
        y = prevY = START_Y;
        vx = vy = 0;
        bank = bankTimer = 0;
        fireCooldown = 0;
        ticksSinceShot = Integer.MAX_VALUE;
        defences.restore(armour);
    }

    void rememberPosition() {
        prevX = x;
        prevY = y;
    }

    /** Steers towards the commanded velocity, moves, banks and regenerates the shield. */
    void fly(int commands) {
        double dx = (Command.RIGHT.in(commands) ? 1 : 0) - (Command.LEFT.in(commands) ? 1 : 0);
        double dy = (Command.UP.in(commands) ? 1 : 0) - (Command.DOWN.in(commands) ? 1 : 0);
        double speed = spec.speed() * (Command.PRECISION.in(commands) ? spec.precisionFactor() : 1);
        if (dx != 0 && dy != 0) {
            speed *= DIAGONAL;
        }
        vx = approach(vx, dx * speed);
        vy = approach(vy, dy * speed);
        x += vx * SimStep.SECONDS;
        y += vy * SimStep.SECONDS;
        double margin = spec.edgeLimit();
        if (x < margin || x > PlayField.WIDTH - margin) {
            x = Math.clamp(x, margin, PlayField.WIDTH - margin);
            vx = 0;
        }
        if (y < margin || y > PlayField.HEIGHT - margin) {
            y = Math.clamp(y, margin, PlayField.HEIGHT - margin);
            vy = 0;
        }
        steerBank();
        defences.step();
    }

    /** Moves one velocity component towards its target: accelerating, or braking to a stop. */
    private double approach(double velocity, double target) {
        double rate = target == 0 ? stopPerStep : accelerationPerStep;
        if (velocity < target) {
            return Math.min(target, velocity + rate);
        }
        return Math.max(target, velocity - rate);
    }

    /** Banking follows the horizontal speed, one frame every few steps (design/art-direction). */
    private void steerBank() {
        int target =
                Math.clamp(Math.round(ShipSpec.HARD_BANK * vx / spec.speed()), -ShipSpec.HARD_BANK, ShipSpec.HARD_BANK);
        if (bank == target) {
            bankTimer = 0;
        } else if (++bankTimer >= spec.bankStepTicks()) {
            bank += Integer.signum(target - bank);
            bankTimer = 0;
        }
    }

    /** Fires a volley when fire is held and the gun is ready; returns whether it fired. */
    boolean fireGun(int commands) {
        if (ticksSinceShot < Integer.MAX_VALUE) {
            ticksSinceShot++;
        }
        if (fireCooldown > 0) {
            fireCooldown--;
        }
        if (fireCooldown > 0 || !Command.FIRE.in(commands)) {
            return false;
        }
        fireCooldown = gun.intervalTicks();
        ticksSinceShot = 0;
        return true;
    }

    void addTo(StateHash hash) {
        hash.add(x)
                .add(y)
                .add(vx)
                .add(vy)
                .add(bank)
                .add(bankTimer)
                .add(fireCooldown)
                .add(ticksSinceShot);
        defences.addTo(hash);
    }

    public double x() {
        return x;
    }

    public double y() {
        return y;
    }

    /** Position between the previous and the current step; {@code alpha} in [0, 1]. */
    public double renderX(double alpha) {
        return prevX + (x - prevX) * alpha;
    }

    /** Position between the previous and the current step; {@code alpha} in [0, 1]. */
    public double renderY(double alpha) {
        return prevY + (y - prevY) * alpha;
    }

    public double vx() {
        return vx;
    }

    public double vy() {
        return vy;
    }

    /** The banking frame: -2 hard left, 0 level, 2 hard right. */
    public int bank() {
        return bank;
    }

    /** Steps since the gun last fired, for the muzzle flash; {@link Integer#MAX_VALUE} before the first shot. */
    public int ticksSinceShot() {
        return ticksSinceShot;
    }

    public Defences defences() {
        return defences;
    }

    public ShipSpec spec() {
        return spec;
    }

    public PulseCannon gun() {
        return gun;
    }
}
