package vanguard.sim;

/** A pooled enemy on one of the parallax layers, weaving down the play field. */
public final class Enemy extends Body {
    private Layer layer;
    private int variant;
    private double baseX;
    private double amplitude;
    private double phase;
    private double frequency;
    private double speed;
    int hp;
    int fireCooldown;

    void spawn(Layer layer, int variant, double x, double y, SplitMix64 rng) {
        this.layer = layer;
        this.variant = variant;
        this.baseX = x;
        this.amplitude = rng.range(0, 40);
        this.phase = rng.range(0, 2 * StrictMath.PI);
        this.frequency = rng.range(0.5, 2.0) * World.STEP_SECONDS;
        this.speed = rng.range(20, 60) * World.STEP_SECONDS;
        this.radius = 8 + variant * 4;
        this.hp = 3 + variant * 2;
        this.fireCooldown = rng.nextInt(240);
        place(x, y);
    }

    void move(double groundScrollPerStep) {
        phase += frequency;
        x = baseX + amplitude * Trig.sin(phase);
        y -= speed + groundScrollPerStep * layer.scrollFactor();
    }

    boolean belowScreen() {
        return y < -radius - 40;
    }

    boolean onScreen() {
        return y < World.HEIGHT && y > 0;
    }

    public Layer layer() {
        return layer;
    }

    /** Sprite variant 0..2: small, medium, large. */
    public int variant() {
        return variant;
    }
}
