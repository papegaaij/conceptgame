package vanguard.sim;

/**
 * The ship's regenerating shield over non-regenerating armour (design/player/shields,
 * design/player/armor): damage goes to the shield first and its overflow to armour; collisions
 * deal half to the shield and half straight to armour. After armour damage the ship ignores all
 * damage for a short mercy time (design/player/ship, Hitbox); shield hits give none.
 */
public final class Defences {
    private final ShieldModel model;
    private final Plating plating;
    private final int mercyTicks;
    private final int delayTicks;
    private final int breakTicks;
    private final double regenPerStep;
    private double shield;
    private double armour;
    private int regenWait;
    private int mercy;
    private boolean broken;

    Defences(ShieldModel model, Plating plating, double mercySeconds) {
        this.model = model;
        this.plating = plating;
        this.mercyTicks = SimStep.ticks(mercySeconds);
        this.delayTicks = SimStep.ticks(model.regenDelaySeconds());
        this.breakTicks = SimStep.ticks(model.breakSeconds());
        this.regenPerStep = model.regenPerSecond() * SimStep.SECONDS;
        restore();
    }

    /** Full shield and armour, as at the start of a sortie. */
    void restore() {
        shield = model.capacity();
        armour = plating.maxArmour();
        regenWait = 0;
        mercy = 0;
        broken = false;
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
            armour = Math.max(0, armour - toArmour);
            mercy = mercyTicks;
            events.add(SimEvents.Type.ARMOUR_HIT, x, y);
        }
        return armour <= 0;
    }

    void addTo(StateHash hash) {
        hash.add(shield).add(armour).add(regenWait).add(mercy).add(broken ? 1 : 0);
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

    /** Steps of mercy invulnerability left; the hull flashes white meanwhile. */
    public int mercyTicks() {
        return mercy;
    }
}
