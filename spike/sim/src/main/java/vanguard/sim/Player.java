package vanguard.sim;

/** The player ship: moves by commands, regenerating shield over non-regenerating armour. */
public final class Player extends Body {
    static final double SPEED = 240 * World.STEP_SECONDS;
    static final double MAX_SHIELD = 100;
    static final int MAX_ARMOUR = 100;
    private static final double SHIELD_REGEN = 8 * World.STEP_SECONDS;
    private static final double MARGIN = 20;

    private double shield = MAX_SHIELD;
    private int armour = MAX_ARMOUR;
    private int retries;
    int fireCooldown;

    Player() {
        radius = 6;
        place(World.WIDTH / 2.0, 90);
    }

    void move(int commands) {
        double dx = (Command.RIGHT.in(commands) ? 1 : 0) - (Command.LEFT.in(commands) ? 1 : 0);
        double dy = (Command.UP.in(commands) ? 1 : 0) - (Command.DOWN.in(commands) ? 1 : 0);
        if (dx != 0 && dy != 0) {
            dx *= StrictMath.sqrt(0.5);
            dy *= StrictMath.sqrt(0.5);
        }
        x = clamp(x + dx * SPEED, MARGIN, World.WIDTH - MARGIN);
        y = clamp(y + dy * SPEED, MARGIN, World.HEIGHT - MARGIN);
        shield = Math.min(MAX_SHIELD, shield + SHIELD_REGEN);
    }

    /**
     * Shield absorbs damage first, then armour. At zero armour the real game retries the level;
     * the spike only counts the retry and restores the ship.
     */
    void takeDamage(double damage) {
        double throughShield = damage - shield;
        shield = Math.max(0, shield - damage);
        if (throughShield > 0) {
            armour -= (int) StrictMath.ceil(throughShield);
            if (armour <= 0) {
                retries++;
                armour = MAX_ARMOUR;
                shield = MAX_SHIELD;
            }
        }
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public double shield() {
        return shield;
    }

    public int armour() {
        return armour;
    }

    public int retries() {
        return retries;
    }
}
