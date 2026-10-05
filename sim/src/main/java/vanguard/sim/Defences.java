package vanguard.sim;

/**
 * The ship's regenerating shield over non-regenerating armour (design/player/shields,
 * design/player/armor): damage goes to the shield first and its overflow to armour; collisions
 * deal half to the shield and half straight to armour. After armour damage the ship ignores all
 * damage for a short mercy time (design/player/ship, Hitbox); shield hits give none.
 *
 * <p>The first armour hit of an attempt that leaves the armour at or below {@link #CRITICAL_SHARE}
 * of its maximum (and above zero) sets off {@link SimEvents.Type#ARMOUR_CRITICAL}, Okafor's
 * low-armour radio line (design/player/armor); repairs do not set it off again until the next
 * attempt.
 */
public final class Defences {
    /** The share of the max armour at or below which the armour is critical: sparks, the fast beep, the radio line. */
    public static final double CRITICAL_SHARE = 0.15;

    private final ShieldModel model;
    private final Plating plating;
    private final int mercyTicks;
    private final int delayTicks;
    private final int breakTicks;
    private final double regenPerStep;
    private double shield;
    private double armour;
    private double armourLost;
    private int regenWait;
    private int mercy;
    private boolean broken;
    /** Whether this attempt's armour has been critical: the low-armour radio line has been set off. */
    private boolean critical;

    Defences(ShieldModel model, Plating plating, double mercySeconds) {
        this.model = model;
        this.plating = plating;
        this.mercyTicks = SimStep.ticks(mercySeconds);
        this.delayTicks = SimStep.ticks(model.regenDelaySeconds());
        this.breakTicks = SimStep.ticks(model.breakSeconds());
        this.regenPerStep = model.regenPerSecond() * SimStep.SECONDS;
        restore(plating.maxArmour());
    }

    /** A full shield over {@code armour} points, as at the start of an attempt. */
    void restore(double armour) {
        if (!(armour > 0 && armour <= plating.maxArmour())) {
            throw new IllegalArgumentException("armour " + armour + " outside (0, " + plating.maxArmour() + "]");
        }
        shield = model.capacity();
        this.armour = armour;
        armourLost = 0;
        regenWait = 0;
        mercy = 0;
        broken = false;
        critical = false;
    }

    /** Back to a boss checkpoint's shield and armour, with the armour lost in the level until then. */
    void restore(double shield, double armour, double armourLost) {
        restore(armour);
        this.shield = Math.min(shield, model.capacity());
        this.armourLost = armourLost;
    }

    /** Counts down the mercy time and regenerates the shield once the delay has passed. */
    void step() {
        if (mercy > 0) {
            mercy--;
        }
        if (regenWait > 0) {
            regenWait--;
        } else if (shield < model.capacity()) {
            broken = false;
            shield = Math.min(model.capacity(), shield + regenPerStep);
        }
    }

    /** A bullet: everything to the shield, the overflow to armour. Returns whether armour reached zero. */
    boolean takeShot(double damage, SimEvents events, double x, double y) {
        return absorb(damage, 0, events, x, y);
    }

    /** Contact with an enemy: half to the shield, half to armour. Returns whether armour reached zero. */
    boolean takeCollision(double damage, SimEvents events, double x, double y) {
        return absorb(damage / 2, damage / 2, events, x, y);
    }

    private boolean absorb(double shieldDamage, double armourDamage, SimEvents events, double x, double y) {
        if (mercy > 0 || armour <= 0) {
            return false;
        }
        regenWait = Math.max(regenWait, delayTicks);
        double absorbed = Math.min(shield, shieldDamage);
        if (absorbed > 0) {
            shield -= absorbed;
            events.add(SimEvents.Type.SHIELD_HIT, x, y);
            if (shield <= 0) {
                shield = 0;
                broken = true;
                regenWait = breakTicks + delayTicks;
                events.add(SimEvents.Type.SHIELD_BROKEN, x, y);
            }
        }
        double toArmour = armourDamage + shieldDamage - absorbed;
        if (toArmour > 0) {
            armourLost += Math.min(armour, toArmour);
            armour = Math.max(0, armour - toArmour);
            mercy = mercyTicks;
            events.add(SimEvents.Type.ARMOUR_HIT, x, y);
            if (!critical && armour > 0 && critical(armour, plating.maxArmour())) {
                critical = true;
                events.add(SimEvents.Type.ARMOUR_CRITICAL, x, y);
            }
        }
        return armour <= 0;
    }

    /** A shield cell: adds {@code points} up to the capacity. */
    void restoreShield(double points) {
        shield = Math.min(model.capacity(), shield + points);
    }

    /** An armour patch: adds {@code points} up to the plating's maximum. */
    void repair(double points) {
        armour = Math.min(plating.maxArmour(), armour + points);
    }

    void addTo(StateHash hash) {
        hash.add(shield).add(armour).add(armourLost).add(regenWait).add(mercy).add(broken ? 1 : 0);
        if (critical) {
            // Only once set, so the hash of a run whose armour never gets this low stays as it was.
            hash.add(1);
        }
    }

    /** Whether {@code armour} of {@code maxArmour} is at or below {@link #CRITICAL_SHARE}. */
    public static boolean critical(double armour, double maxArmour) {
        return armour / maxArmour <= CRITICAL_SHARE;
    }

    /** Whether this attempt's armour has dropped to the critical share (the low-armour radio line was set off). */
    public boolean wasCritical() {
        return critical;
    }

    /** Armour points lost since the defences were last restored (repairs do not undo it). */
    public double armourLost() {
        return armourLost;
    }

    public double shield() {
        return shield;
    }

    public double maxShield() {
        return model.capacity();
    }

    public double armour() {
        return armour;
    }

    public double maxArmour() {
        return plating.maxArmour();
    }

    /** Whether the shield is down after a break and not regenerating yet. */
    public boolean broken() {
        return broken;
    }

    /** The ship takes no damage for the next {@code ticks} steps (the Smart Bomb's), as in its mercy time. */
    void guard(int ticks) {
        mercy = Math.max(mercy, ticks);
    }

    /** Steps of mercy invulnerability left; the hull flashes white meanwhile. */
    public int mercyTicks() {
        return mercy;
    }
}
