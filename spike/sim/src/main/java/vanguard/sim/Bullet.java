package vanguard.sim;

/** A pooled straight-flying bullet. */
public final class Bullet extends Body {
    private Faction faction;
    private double vx;
    private double vy;
    int damage;

    void fire(Faction faction, double x, double y, double vx, double vy, int damage) {
        this.faction = faction;
        this.vx = vx;
        this.vy = vy;
        this.damage = damage;
        this.radius = faction == Faction.PLAYER ? 3 : 4;
        place(x, y);
    }

    void move() {
        x += vx;
        y += vy;
    }

    boolean offScreen() {
        return x < -16 || x > World.WIDTH + 16 || y < -16 || y > World.HEIGHT + 16;
    }

    public Faction faction() {
        return faction;
    }

    double vx() {
        return vx;
    }

    double vy() {
        return vy;
    }
}
