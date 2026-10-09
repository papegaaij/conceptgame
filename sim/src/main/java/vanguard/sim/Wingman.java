package vanguard.sim;

import java.util.List;

/**
 * A wingman flying in the escort slot (design/player/wingmen: Rook), stepped by the {@link Sortie}:
 * he keeps a formation slot beside and behind the player, gliding to a new one when the waves
 * change (Wing by default, Wide while a sides wave is active or an enemy is on his flank, Trail
 * while a rear wave is active) and taking the mirrored slot while his own lies outside the play
 * field; he dodges the enemy bullets he predicts to pass close, picks a target in his firing cone
 * (the player's last hit first, then a flank threat, then the nearest) for a homing gun's lock or
 * a lobbed gun's aim, and fires his gun whenever the player fires, with a target or without. Every decision waits his
 * reaction delay. He reacts to only a share of the bullets he predicts (user decision 2026-10-06:
 * about 70 %), so stray bullets hit him now and then; his slot keeps clear of the air enemies'
 * bodies. He takes enemy bullets and the contact of air enemies; at zero armour he ejects and is
 * out for the rest of the attempt.
 *
 * <p>No allocation, and his only randomness is his own generator seeded from the sortie's seed:
 * the same inputs give the same flight, and the state is hashed by the sortie only when he flies.
 */
public final class Wingman {
    /** His formation (design/player/wingmen, Formations). */
    public enum Formation {
        WING,
        WIDE,
        TRAIL
    }

    /** He slows down as he arrives at his slot: his wanted speed is the distance over this time. */
    private static final double ARRIVE_SECONDS = 0.1;

    /**
     * A sidestep is a jink: he accelerates this many times harder while he dodges, so the time his
     * reaction delay leaves him (the look-ahead minus the delay) is enough for most single bullets.
     */
    private static final double JINK = 4;

    /** The most air enemies whose contact he remembers, so a lasting contact hurts once. */
    private static final int CONTACTS = 4;

    /** The most bullets he remembers deciding on at once, so each one is decided once. */
    private static final int DECIDED = 4;

    /** Two predicted lines this close (px) at the same velocity are the same bullet's. */
    private static final double SAME_LINE = 1;

    /** Passes over the enemies when his slot is moved clear of a body (it may land on another). */
    private static final int BODY_PASSES = 3;

    /** A move clear of a body that would bring his slot within his minimum distance of the player costs this much more, px. */
    private static final double NEAR_PLAYER_COST = 1000;

    /** A move clear of a body whose way crosses the body (or, from outside, its box) costs this much more, px. */
    private static final double CROSSING_COST = 500;

    /** A way that only touches a box's edge (in parts of its length) does not cross it. */
    private static final double CROSSING_EPSILON = 1e-9;

    /** An enemy that moved farther than this in a step jumped (a loop-back's re-entry): no velocity, px. */
    private static final double JUMP = 16;

    /**
     * Around the player (design/player/wingmen: his way to a slot on the other side), he steers to a
     * point this far round the circle at his minimum distance ahead of where he is, radians.
     */
    private static final double DETOUR_STEP = StrictMath.PI / 4;

    /** Each way round the player is checked for room inside the margins at this many points. */
    private static final int DETOUR_SAMPLES = 24;

    /** He steers round the player this far outside his minimum distance, px. */
    private static final double DETOUR_GAP = 8;

    /** A way round the player may touch a margin by this much, px (he starts on one). */
    private static final double DETOUR_TOLERANCE = 1;

    /** Mixed into the sortie's seed for his own generator, so it does not follow the sortie's. */
    private static final long LUCK_SALT = 0x524F4F4B5F4C55L;

    private static final double LINE_EPSILON = 1e-9;

    /**
     * How far above or below his eject pod's line the ship's centre must be for the pod to pass it:
     * half the ship's 48 px, half the pod's 16 px and a little air.
     */
    static final double POD_CLEARANCE = 36;

    private final WingmanSpec spec;
    private final Hull hull;
    private final double accelerationStep;
    private final double limit;
    private final double tanHalfCone;
    private final double podStep;
    private final int glideTicks;
    private final int swapTicks;
    private final int reactionTicks;
    private final int intervalTicks;
    private final int holdTicks;
    private final int recentHitTicks;
    /** Whether he reacts to a bullet he predicts: his own generator, seeded from the sortie's seed. */
    private final SplitMix64 luck;

    private double x;
    private double y;
    private double prevX;
    private double prevY;
    private double vx;
    private double vy;
    private int bank;
    private int bankTimer;

    private double armour;
    private boolean critical;
    private boolean ejected;
    private int ejectTicks;
    private double ejectX;
    private double ejectY;
    private int podSign;
    /** The serials of the air enemies touching him in the last step, and how many. */
    private final int[] contacts = new int[CONTACTS];

    private int contactCount;
    private final int[] touching = new int[CONTACTS];
    private int touchingCount;

    private Formation formation;
    private int formationWait;
    private WingmanSpec.Side side;
    private int sideWait;
    /** Steps his own slot has been inside the field while he flies the mirrored one. */
    private int inside;
    /** The offset the glide started from (px right of and above the player) and its steps so far. */
    private double fromX;

    private double fromY;
    private int glided;

    private int scanWait;
    /** Steps until the dodge he decided on starts; -1 for none waiting. */
    private int dodgeWait;

    private double pendingX;
    private double pendingY;
    private double dodgeX;
    private double dodgeY;
    private int dodgeLeft;

    /**
     * The bullets he decided on (and whether he reacted), each by its velocity and its line (the cross
     * product of position and velocity, the same all along its flight), with the steps he still
     * remembers it; so a bullet is decided once, not again at every prediction.
     */
    private final double[] decidedVx = new double[DECIDED];

    private final double[] decidedVy = new double[DECIDED];
    private final double[] decidedLine = new double[DECIDED];
    private final int[] decidedTicks = new int[DECIDED];
    private final boolean[] reacted = new boolean[DECIDED];

    /** Where he flies to this step: his slot plus a sidestep, moved clear of the enemies' bodies. */
    private double goalX;

    private double goalY;

    /** Where he steers this step: his goal, or a point on his way round the player to it. */
    private double aimX;

    private double aimY;
    /** Whether his slot was moved clear of a body this step: he jinks as in a sidestep. */
    private boolean evading;

    /** His target: an enemy's serial, or a set-piece part's as the homing locks use; -1 for none. */
    private int target;

    private int targetWait;
    private int cooldown;
    private int sinceShot;

    /** Outside a sortie (tests): his generator seeded from seed 0. */
    Wingman(WingmanSpec spec) {
        this(spec, 0);
    }

    /** @param seed the sortie's seed, from which his own generator is seeded */
    Wingman(WingmanSpec spec, long seed) {
        this.spec = spec;
        luck = new SplitMix64(seed ^ LUCK_SALT);
        WingmanSpec.Craft craft = spec.craft();
        hull = new Hull(List.of(new Hull.Part(0, 0, craft.hitbox())));
        accelerationStep = craft.speed() / craft.accelerationSeconds() * SimStep.SECONDS;
        limit = craft.edgeLimit();
        WingmanSpec.Ai ai = spec.ai();
        tanHalfCone = StrictMath.tan(ai.coneHalfAngle());
        podStep = craft.podSpeed() * SimStep.SECONDS;
        glideTicks = Math.max(1, SimStep.ticks(ai.glideSeconds()));
        swapTicks = SimStep.ticks(ai.swapSeconds());
        reactionTicks = SimStep.ticks(ai.reactionSeconds());
        intervalTicks = Math.max(1, SimStep.ticks(ai.dodgeInterval()));
        holdTicks = Math.max(1, SimStep.ticks(ai.lookAhead()));
        recentHitTicks = SimStep.ticks(ai.recentHitSeconds());
        reset(spec.armour(), Ship.START_X, Ship.START_Y);
    }

    /** The start of an attempt: {@code armour} points, in his Wing slot beside the ship at (shipX, shipY). */
    void reset(double startArmour, double shipX, double shipY) {
        armour = startArmour;
        critical = false;
        ejected = false;
        ejectTicks = 0;
        contactCount = 0;
        formation = Formation.WING;
        formationWait = -1;
        side = spec.side();
        sideWait = -1;
        inside = 0;
        glided = glideTicks;
        scanWait = 0;
        dodgeWait = -1;
        dodgeX = dodgeY = 0;
        dodgeLeft = 0;
        for (int i = 0; i < DECIDED; i++) {
            decidedTicks[i] = 0;
        }
        evading = false;
        target = -1;
        targetWait = -1;
        cooldown = 0;
        sinceShot = Integer.MAX_VALUE;
        bank = bankTimer = 0;
        formUp(shipX, shipY);
        prevX = x;
        prevY = y;
    }

    /**
     * Back to a boss checkpoint's armour, ejected or not, in formation beside the ship, his
     * generator as it was then.
     */
    void restore(
            double checkpointArmour,
            boolean wasEjected,
            boolean wasCritical,
            long checkpointLuck,
            double shipX,
            double shipY) {
        reset(wasEjected ? spec.armour() : checkpointArmour, shipX, shipY);
        luck.state(checkpointLuck);
        armour = wasEjected ? 0 : checkpointArmour;
        ejected = wasEjected;
        critical = wasCritical;
    }

    void rememberPosition() {
        prevX = x;
        prevY = y;
    }

    /** In his slot beside the ship at (shipX, shipY), at rest: during the launch, and at a (re)start. */
    void formUp(double shipX, double shipY) {
        x = shipX + slotX();
        y = shipY + slotY();
        vx = vy = 0;
    }

    /**
     * One step of flight while the player flies (or waits for his retry): formation, dodging,
     * targeting, then the move towards his slot. {@code activeEdges} are the edges with an active
     * wave ({@link WarningEdge} bits) and {@code playerHit} the enemy the player's shots damaged last,
     * with the steps since.
     */
    void fly(
            double shipX,
            double shipY,
            int activeEdges,
            Pool<Enemy> enemies,
            Pool<EnemyBullet> bullets,
            SetPiece[] setPieces,
            int playerHit) {
        if (ejected) {
            ejectTicks++;
            return;
        }
        if (cooldown > 0) {
            cooldown--;
        }
        if (sinceShot < Integer.MAX_VALUE) {
            sinceShot++;
        }
        boolean flank = scanEnemies(enemies, setPieces, playerHit);
        chooseFormation(shipX, activeEdges, flank);
        dodge(bullets);
        glided = Math.min(glideTicks, glided + 1);
        evading = clearOfBodies(enemies, shipX, shipY, shipX + slotX() + dodgeX, shipY + slotY() + dodgeY);
        evading |= clearOfLanes(setPieces);
        move(shipX, shipY);
        steerBank();
    }

    /** His generator's state, for a boss checkpoint. */
    long luck() {
        return luck.state();
    }

    /** The player's last hit counts as his first choice while it is this recent, steps. */
    int recentHitTicks() {
        return recentHitTicks;
    }

    /**
     * Picks his target among the enemies and set-piece parts in his cone and range on the layers his
     * gun reaches (after his reaction delay), and returns whether an enemy is on his flank (within
     * the flank distance of him).
     */
    private boolean scanEnemies(Pool<Enemy> enemies, SetPiece[] setPieces, int playerHit) {
        boolean flank = false;
        boolean targetSeen = false;
        int best = -1;
        int bestRank = Integer.MAX_VALUE;
        double bestDistance = Double.MAX_VALUE;
        double flankDistance = spec.ai().flankDistance();
        for (int j = 0; j < enemies.size(); j++) {
            Enemy enemy = enemies.get(j);
            // M5 part E (default 12): a unit under the water is neither his target nor a flank threat.
            if (!PlayerFire.onField(enemy) || enemy.layer() == Layer.SUB) {
                continue;
            }
            double dx = enemy.x() - x;
            double dy = enemy.y() - y;
            // A flank threat: an enemy within the flank distance of him (not merely in the same
            // column of the screen, which every enemy far ahead would be).
            boolean beside = dx * dx + dy * dy <= flankDistance * flankDistance;
            flank |= beside;
            // M5 part C: on its current layer; a hardened unit only for an anti-ground gun.
            if (!reaches(enemy.layer())
                    || (enemy.spec().hardened() && !spec.gun().antiGround())
                    || !inCone(dx, dy)) {
                continue;
            }
            targetSeen |= enemy.serial() == target;
            int rank = enemy.serial() == playerHit ? 1 : beside ? 2 : 3;
            double distance = dx * dx + dy * dy;
            if (rank < bestRank || (rank == bestRank && distance < bestDistance)) {
                best = enemy.serial();
                bestRank = rank;
                bestDistance = distance;
            }
        }
        for (int k = 0; k < setPieces.length; k++) {
            SetPiece piece = setPieces[k];
            if (!piece.present()) {
                continue;
            }
            List<LevelScript.PartSpec> parts = piece.spec().parts();
            for (int p = 0; p < parts.size(); p++) {
                double px = piece.partX(p);
                double py = piece.partY(p);
                // M5 part E: on the part's own layer (an arena boss's part under the water is none of his).
                if (!reaches(piece.partLayer(p))
                        || piece.partWrecked(p)
                        || piece.partShielded(p)
                        || !PlayField.overlaps(px, py, parts.get(p).box())
                        || !inCone(px - x, py - y)) {
                    continue;
                }
                int serial = PlayerFire.PART_SERIAL + k * LevelScript.SetPieceSpec.MAX_PARTS + p;
                targetSeen |= serial == target;
                double distance = (px - x) * (px - x) + (py - y) * (py - y);
                if (3 < bestRank || (bestRank == 3 && distance < bestDistance)) {
                    best = serial;
                    bestRank = 3;
                    bestDistance = distance;
                }
            }
        }
        if (!targetSeen) {
            // A target that died or left his cone is dropped at once: no homing shot locks onto it.
            target = -1;
        }
        if (best == target) {
            targetWait = -1;
        } else if (targetWait < 0) {
            targetWait = reactionTicks;
        }
        if (targetWait >= 0 && targetWait-- == 0) {
            target = best;
            targetWait = -1;
        }
        return flank;
    }

    /**
     * Whether his gun's shots reach a target on {@code layer}: a shell or bomb only the ground; none
     * of his guns reaches {@code sub} (M5 part E).
     */
    private boolean reaches(Layer layer) {
        WeaponSpec.Delivery delivery = spec.gun().delivery();
        return delivery.landing() ? layer == Layer.GROUND : delivery.reaches(layer);
    }

    /**
     * Whether a target (dx, dy) from him lies in his firing cone ahead and in range; for a lobbed
     * gun (the Mortar), which he aims at its target (user decision 2026-10-07), anywhere ahead of him
     * within the lob's range, so a ground target beside the player is his while he flies in
     * formation.
     */
    private boolean inCone(double dx, double dy) {
        if (spec.gun().delivery() == WeaponSpec.Delivery.LOBBED) {
            double reach = spec.gun().range();
            return dy > 0 && dx * dx + dy * dy <= reach * reach;
        }
        double range = spec.ai().range();
        return dy > 0 && Math.abs(dx) <= dy * tanHalfCone && dx * dx + dy * dy <= range * range;
    }

    /**
     * Trail while a rear wave is active, Wide while a sides wave is or an enemy is on his flank,
     * Wing otherwise; the mirrored side while his own slot lies outside the play field, back once it
     * has been inside for the swap time. Each change waits his reaction delay, then he glides.
     */
    private void chooseFormation(double shipX, int activeEdges, boolean flank) {
        Formation wanted;
        if (WarningEdge.BOTTOM.in(activeEdges)) {
            wanted = Formation.TRAIL;
        } else if (WarningEdge.LEFT.in(activeEdges) || WarningEdge.RIGHT.in(activeEdges) || flank) {
            wanted = Formation.WIDE;
        } else {
            wanted = Formation.WING;
        }
        if (wanted == formation) {
            formationWait = -1;
        } else if (formationWait < 0) {
            formationWait = reactionTicks;
        }
        if (formationWait >= 0 && formationWait-- == 0) {
            startGlide();
            formation = wanted;
            formationWait = -1;
        }
        WingmanSpec.Side own = spec.side();
        double ownX = shipX + own.sign() * offset(formation).x();
        boolean ownOutside = ownX < limit || ownX > PlayField.WIDTH - limit;
        WingmanSpec.Side wantedSide;
        if (side == own) {
            inside = 0;
            wantedSide = ownOutside ? own.other() : own;
        } else {
            inside = ownOutside ? 0 : inside + 1;
            wantedSide = inside >= swapTicks ? own : side;
        }
        if (wantedSide == side) {
            sideWait = -1;
        } else if (sideWait < 0) {
            sideWait = reactionTicks;
        }
        if (sideWait >= 0 && sideWait-- == 0) {
            startGlide();
            side = wantedSide;
            sideWait = -1;
            inside = 0;
        }
    }

    private void startGlide() {
        fromX = slotX();
        fromY = slotY();
        glided = 0;
    }

    private WingmanSpec.Offset offset(Formation of) {
        WingmanSpec.Ai ai = spec.ai();
        return switch (of) {
            case WING -> ai.wing();
            case WIDE -> ai.wide();
            case TRAIL -> ai.trail();
        };
    }

    /** His slot now, px right of the player's centre: the formation's, part way through a glide. */
    private double slotX() {
        double to = side.sign() * offset(formation).x();
        return fromX + (to - fromX) * glided / glideTicks;
    }

    /** His slot now, px above the player's centre (a slot behind him is below). */
    private double slotY() {
        double to = -offset(formation).y();
        return fromY + (to - fromY) * glided / glideTicks;
    }

    /**
     * Every dodge interval he predicts the enemy bullets over his look-ahead: the soonest one passing
     * within his clearance is decided once. He reacts to it with his reaction share (his generator):
     * a sidestep at right angles to it, away from its line, which starts after his reaction delay
     * and lasts the look-ahead, then back to his slot; otherwise he misses it. Either way he
     * remembers his decision until the bullet has passed, so it is decided once: a bullet he missed
     * he ignores from then on, one he reacted to he keeps sidestepping (planned afresh, no new
     * decision) while it is still the soonest.
     */
    private void dodge(Pool<EnemyBullet> bullets) {
        if (dodgeLeft > 0 && --dodgeLeft == 0) {
            dodgeX = dodgeY = 0;
        }
        for (int m = 0; m < DECIDED; m++) {
            if (decidedTicks[m] > 0) {
                decidedTicks[m]--;
            }
        }
        if (dodgeWait >= 0 && dodgeWait-- == 0) {
            dodgeX = pendingX;
            dodgeY = pendingY;
            dodgeLeft = holdTicks;
            dodgeWait = -1;
        }
        if (scanWait > 0) {
            scanWait--;
            return;
        }
        scanWait = intervalTicks - 1;
        if (dodgeWait >= 0) {
            return;
        }
        WingmanSpec.Ai ai = spec.ai();
        double clearance = ai.clearance() * ai.clearance();
        double soonest = ai.lookAhead();
        int threat = -1;
        boolean known = false;
        for (int i = 0; i < bullets.size(); i++) {
            EnemyBullet bullet = bullets.get(i);
            double rx = bullet.x() - x;
            double ry = bullet.y() - y;
            double bvx = bullet.vx();
            double bvy = bullet.vy();
            double speed = bvx * bvx + bvy * bvy;
            if (speed <= 0) {
                continue;
            }
            double t = -(rx * bvx + ry * bvy) / speed;
            if (t < 0 || t > soonest) {
                continue;
            }
            double cx = rx + bvx * t;
            double cy = ry + bvy * t;
            if (cx * cx + cy * cy >= clearance) {
                continue;
            }
            int memory = decided(bullet, speed);
            if (memory < 0 || reacted[memory]) {
                soonest = t;
                threat = i;
                known = memory >= 0;
            }
        }
        if (threat < 0) {
            return;
        }
        EnemyBullet bullet = bullets.get(threat);
        if (!known) {
            boolean reacts = luck.nextDouble() < ai.reacts();
            remember(bullet, reacts);
            if (!reacts) {
                return;
            }
        }
        double speed = Math.sqrt(bullet.vx() * bullet.vx() + bullet.vy() * bullet.vy());
        // At right angles to the bullet's flight, to the side of its line he is on.
        double px = -bullet.vy() / speed;
        double py = bullet.vx() / speed;
        double offLine = px * (x - bullet.x()) + py * (y - bullet.y());
        // Right on its line: towards his own side.
        double away =
                Math.abs(offLine) < LINE_EPSILON ? side.sign() * Math.signum(px + LINE_EPSILON) : Math.signum(offLine);
        pendingX = away * ai.dodgeStep() * px;
        pendingY = away * ai.dodgeStep() * py;
        dodgeWait = reactionTicks;
    }

    /**
     * His memory of his decision on {@code bullet} (its squared speed {@code speed}), or -1 if he
     * has none (any more).
     */
    private int decided(EnemyBullet bullet, double speed) {
        double line = bullet.x() * bullet.vy() - bullet.y() * bullet.vx();
        double same = SAME_LINE * Math.sqrt(speed);
        for (int m = 0; m < DECIDED; m++) {
            if (decidedTicks[m] > 0
                    && decidedVx[m] == bullet.vx()
                    && decidedVy[m] == bullet.vy()
                    && Math.abs(decidedLine[m] - line) <= same) {
                return m;
            }
        }
        return -1;
    }

    /**
     * Remembers his decision on a bullet, whether he {@code reacts} to it, until it has passed (in
     * place of the oldest memory).
     */
    private void remember(EnemyBullet bullet, boolean reacts) {
        int slot = 0;
        for (int m = 1; m < DECIDED; m++) {
            if (decidedTicks[m] < decidedTicks[slot]) {
                slot = m;
            }
        }
        decidedVx[slot] = bullet.vx();
        decidedVy[slot] = bullet.vy();
        decidedLine[slot] = bullet.x() * bullet.vy() - bullet.y() * bullet.vx();
        decidedTicks[slot] = holdTicks + intervalTicks;
        reacted[slot] = reacts;
    }

    /**
     * Where he flies to: the point (tx, ty) inside the play field's margins, kept clear of every air
     * enemy's body. Around each body lies a box: its hit box grown by his half hit box and his
     * clearance, stretched over where it flies in his look-ahead. While he is outside a box and his
     * goal lies in it or his way there crosses it, the goal moves to the box's edge the shortest way
     * that keeps him from crossing it (he waits on his side while it passes); once he is inside one,
     * and his goal lies in it or his way there crosses the body itself, he leaves it the shortest way
     * that does not cross the body. Either way the move stays inside the field and prefers one that
     * keeps his minimum distance to the player. Sets {@link #goalX} and {@link #goalY}; returns
     * whether it moved the point.
     */
    private boolean clearOfBodies(Pool<Enemy> enemies, double shipX, double shipY, double tx, double ty) {
        WingmanSpec.Craft craft = spec.craft();
        double gap = spec.ai().clearance();
        double ahead = spec.ai().lookAhead() / SimStep.SECONDS;
        double min = craft.minDistance();
        double right = PlayField.WIDTH - limit;
        double top = PlayField.HEIGHT - limit;
        goalX = Math.clamp(tx, limit, right);
        goalY = Math.clamp(ty, limit, top);
        boolean shifted = false;
        for (int pass = 0; pass < BODY_PASSES; pass++) {
            boolean moved = false;
            for (int j = 0; j < enemies.size(); j++) {
                Enemy enemy = enemies.get(j);
                if (!enemy.layer().collidesWithPlayer() || !PlayerFire.onField(enemy)) {
                    continue;
                }
                double ex = enemy.x();
                double ey = enemy.y();
                double stepX = ex - enemy.renderX(0);
                double stepY = ey - enemy.renderY(0);
                if (stepX * stepX + stepY * stepY > JUMP * JUMP) {
                    stepX = stepY = 0;
                }
                // The body itself as he touches it, and the box he keeps out of.
                double touchW = enemy.hitbox().width() / 2 + craft.hitbox().width() / 2;
                double touchH = enemy.hitbox().height() / 2 + craft.hitbox().height() / 2;
                double boxLeft = Math.min(ex, ex + stepX * ahead) - touchW - gap;
                double boxRight = Math.max(ex, ex + stepX * ahead) + touchW + gap;
                double boxBottom = Math.min(ey, ey + stepY * ahead) - touchH - gap;
                double boxTop = Math.max(ey, ey + stepY * ahead) + touchH + gap;
                boolean goalIn = goalX > boxLeft && goalX < boxRight && goalY > boxBottom && goalY < boxTop;
                boolean in = x > boxLeft && x < boxRight && y > boxBottom && y < boxTop;
                // What his way must not cross: the box from outside it, the body from inside it.
                double wallLeft = in ? ex - touchW : boxLeft;
                double wallRight = in ? ex + touchW : boxRight;
                double wallBottom = in ? ey - touchH : boxBottom;
                double wallTop = in ? ey + touchH : boxTop;
                if (!goalIn && !crosses(x, y, goalX, goalY, wallLeft, wallRight, wallBottom, wallTop)) {
                    continue;
                }
                // From outside, the goal moves to an edge; from inside, he leaves from where he is.
                double fromX = in ? x : goalX;
                double fromY = in ? y : goalY;
                double bestX = goalX;
                double bestY = goalY;
                double best = Double.MAX_VALUE;
                for (int k = 0; k < 4; k++) {
                    double cx = k == 0 ? boxLeft : k == 1 ? boxRight : fromX;
                    double cy = k == 2 ? boxBottom : k == 3 ? boxTop : fromY;
                    if (cx < limit || cx > right || cy < limit || cy > top) {
                        continue;
                    }
                    double cost = Math.abs(cx - fromX) + Math.abs(cy - fromY);
                    if (crosses(x, y, cx, cy, wallLeft, wallRight, wallBottom, wallTop)) {
                        cost += CROSSING_COST;
                    }
                    double px = cx - shipX;
                    double py = cy - shipY;
                    if (px * px + py * py < min * min) {
                        cost += NEAR_PLAYER_COST;
                    }
                    if (cost < best) {
                        best = cost;
                        bestX = cx;
                        bestY = cy;
                    }
                }
                if (best < Double.MAX_VALUE && (bestX != goalX || bestY != goalY)) {
                    goalX = bestX;
                    goalY = bestY;
                    moved = true;
                    shifted = true;
                }
            }
            if (!moved) {
                break;
            }
        }
        return shifted;
    }

    /** He keeps this far beside a telegraphed lane's edge, px (M5 part E). */
    private static final double LANE_GAP = 6;

    /**
     * M5 part E (design/player/wingmen, Act 2 hazards; default 12): while an arena boss's lanes are
     * telegraphed, his goal moves out of them: beside the run of telegraphed lanes it lies in, on the
     * side nearer him that is inside the play field's margins (he waits there while the player is in
     * the lane); nothing changes while his goal and he are above the lanes. Returns whether it moved
     * the goal.
     */
    private boolean clearOfLanes(SetPiece[] setPieces) {
        boolean moved = false;
        for (SetPiece piece : setPieces) {
            if (!piece.present() || piece.arena().isEmpty()) {
                continue;
            }
            SlamArena arena = piece.arena().get();
            int mask = arena.telegraphed();
            double halfH = spec.craft().hitbox().height() / 2;
            if (mask == 0 || (goalY - halfH > arena.laneTop() && y - halfH > arena.laneTop())) {
                continue;
            }
            double width = arena.laneWidth();
            double half = spec.craft().hitbox().width() / 2 + LANE_GAP;
            for (int lane = 1; lane <= arena.laneCount(); lane++) {
                double left = (lane - 1) * width;
                double right = lane * width;
                if ((mask & (1 << lane)) == 0 || goalX + half <= left || goalX - half >= right) {
                    continue;
                }
                int lo = lane;
                int hi = lane;
                while (lo > 1 && (mask & (1 << (lo - 1))) != 0) {
                    lo--;
                }
                while (hi < arena.laneCount() && (mask & (1 << (hi + 1))) != 0) {
                    hi++;
                }
                double toLeft = (lo - 1) * width - half;
                double toRight = hi * width + half;
                boolean leftOk = toLeft >= limit;
                boolean rightOk = toRight <= PlayField.WIDTH - limit;
                if (leftOk && (!rightOk || Math.abs(toLeft - x) <= Math.abs(toRight - x))) {
                    goalX = toLeft;
                    moved = true;
                } else if (rightOk) {
                    goalX = toRight;
                    moved = true;
                }
                break;
            }
        }
        return moved;
    }

    /**
     * Whether the way from (ax, ay) to (bx, by) passes through the inside of the box (touching its
     * edge is not crossing it).
     */
    private static boolean crosses(
            double ax, double ay, double bx, double by, double left, double right, double bottom, double top) {
        double dx = bx - ax;
        double dy = by - ay;
        double enter = 0;
        double leave = 1;
        for (int k = 0; k < 4; k++) {
            double p = k == 0 ? -dx : k == 1 ? dx : k == 2 ? -dy : dy;
            double q = k == 0 ? ax - left : k == 1 ? right - ax : k == 2 ? ay - bottom : top - ay;
            if (p == 0) {
                if (q <= 0) {
                    return false;
                }
            } else if (p < 0) {
                enter = Math.max(enter, q / p);
            } else {
                leave = Math.min(leave, q / p);
            }
        }
        return leave - enter > CROSSING_EPSILON;
    }

    /**
     * Flies towards his goal (his slot plus a sidestep, clear of the bodies) at up to his top speed,
     * accelerating at his rate (jinking while he sidesteps or clears a body), slowing as he arrives;
     * then {@linkplain #keepClear kept} inside the play field's margins and his minimum distance from
     * the player.
     */
    private void move(double shipX, double shipY) {
        WingmanSpec.Craft craft = spec.craft();
        aimAround(shipX, shipY);
        double ex = aimX - x;
        double ey = aimY - y;
        double distance = Math.sqrt(ex * ex + ey * ey);
        double wantedX = 0;
        double wantedY = 0;
        if (distance > 0) {
            double speed = Math.min(craft.speed(), distance / ARRIVE_SECONDS);
            wantedX = ex / distance * speed;
            wantedY = ey / distance * speed;
        }
        double dvx = wantedX - vx;
        double dvy = wantedY - vy;
        double change = Math.sqrt(dvx * dvx + dvy * dvy);
        double most = dodgeLeft > 0 || evading ? JINK * accelerationStep : accelerationStep;
        if (change > most) {
            dvx *= most / change;
            dvy *= most / change;
        }
        vx += dvx;
        vy += dvy;
        x += vx * SimStep.SECONDS;
        y += vy * SimStep.SECONDS;
        keepClear(shipX, shipY);
    }

    /**
     * Sets {@link #aimX} and {@link #aimY}: his goal, unless his straight way there passes within his
     * minimum distance of the player (a slot on the player's other side) on a side without room
     * inside the play field's margins at that distance, while the other side has it. Then he steers
     * round the player the other way, a little outside that distance: with the player low on the
     * screen, the way beneath him is closed and he passes over him (Level 08's capture, round 30:
     * pushed back by {@link #keepClear} at the bottom margin, he stayed beneath the player to the
     * level's end). Otherwise straight on, {@link #keepClear} sliding him round the player.
     */
    private void aimAround(double shipX, double shipY) {
        aimX = goalX;
        aimY = goalY;
        double min = spec.craft().minDistance();
        double gx = goalX - shipX;
        double gy = goalY - shipY;
        double px = x - shipX;
        double py = y - shipY;
        if (gx * gx + gy * gy < min * min) {
            return;
        }
        // The point of his way nearest the player's centre.
        double wx = gx - px;
        double wy = gy - py;
        double length = wx * wx + wy * wy;
        double along = length > 0 ? Math.clamp(-(px * wx + py * wy) / length, 0, 1) : 0;
        double nx = px + wx * along;
        double ny = py + wy * along;
        if (nx * nx + ny * ny >= min * min) {
            return;
        }
        double from = StrictMath.atan2(py, px);
        double to = StrictMath.atan2(gy, gx);
        double counter = to - from;
        while (counter < 0) {
            counter += 2 * StrictMath.PI;
        }
        while (counter >= 2 * StrictMath.PI) {
            counter -= 2 * StrictMath.PI;
        }
        double clockwise = 2 * StrictMath.PI - counter;
        // The way the push-out slides him round: the side his straight way passes the player on.
        boolean pushedCounter;
        if (nx == 0 && ny == 0) {
            pushedCounter = counter <= clockwise;
        } else {
            double near = StrictMath.atan2(ny, nx) - from;
            while (near < 0) {
                near += 2 * StrictMath.PI;
            }
            while (near >= 2 * StrictMath.PI) {
                near -= 2 * StrictMath.PI;
            }
            pushedCounter = near <= counter;
        }
        if (roomAround(shipX, shipY, from, pushedCounter ? counter : -clockwise, min)
                || !roomAround(shipX, shipY, from, pushedCounter ? -clockwise : counter, min)) {
            // That way has room (or neither has): straight on, kept clear as before.
            return;
        }
        double turn = pushedCounter ? -Math.min(clockwise, DETOUR_STEP) : Math.min(counter, DETOUR_STEP);
        double radius = min + DETOUR_GAP;
        aimX = Math.clamp(shipX + radius * Trig.cos(from + turn), limit, PlayField.WIDTH - limit);
        aimY = Math.clamp(shipY + radius * Trig.sin(from + turn), limit, PlayField.HEIGHT - limit);
    }

    /**
     * Whether the arc at {@code radius} round the player from angle {@code from} through {@code
     * turn} radians (positive: counter-clockwise) lies inside the play field's margins.
     */
    private boolean roomAround(double shipX, double shipY, double from, double turn, double radius) {
        double right = PlayField.WIDTH - limit + DETOUR_TOLERANCE;
        double top = PlayField.HEIGHT - limit + DETOUR_TOLERANCE;
        double low = limit - DETOUR_TOLERANCE;
        for (int k = 1; k <= DETOUR_SAMPLES; k++) {
            double angle = from + turn * k / DETOUR_SAMPLES;
            double ax = shipX + radius * Trig.cos(angle);
            double ay = shipY + radius * Trig.sin(angle);
            if (ax < low || ax > right || ay < low || ay > top) {
                return false;
            }
        }
        return true;
    }

    /**
     * Inside the play field's margins and never closer to the player's centre than his minimum
     * distance, also where the two meet (the player near an edge, a side swap gliding past him):
     * pushed straight out from the player when that point lies inside the margins, else to the
     * nearest point at the minimum distance on a margin.
     */
    private void keepClear(double shipX, double shipY) {
        double right = PlayField.WIDTH - limit;
        double top = PlayField.HEIGHT - limit;
        if (x < limit || x > right) {
            x = Math.clamp(x, limit, right);
            vx = 0;
        }
        if (y < limit || y > top) {
            y = Math.clamp(y, limit, top);
            vy = 0;
        }
        double rx = x - shipX;
        double ry = y - shipY;
        double near = rx * rx + ry * ry;
        double min = spec.craft().minDistance();
        if (near >= min * min) {
            return;
        }
        if (near == 0) {
            rx = side.sign();
            ry = 0;
            near = 1;
        }
        double scale = min / Math.sqrt(near);
        double outX = shipX + rx * scale;
        double outY = shipY + ry * scale;
        if (outX >= limit && outX <= right && outY >= limit && outY <= top) {
            x = outX;
            y = outY;
            return;
        }
        // Where the circle at his minimum distance crosses a margin, inside the others: the nearest.
        double bestX = outX;
        double bestY = outY;
        double best = Double.MAX_VALUE;
        for (int k = 0; k < 4; k++) {
            boolean vertical = k < 2;
            double line = k == 0 ? limit : k == 1 ? right : k == 2 ? limit : top;
            double across = line - (vertical ? shipX : shipY);
            if (Math.abs(across) > min) {
                continue;
            }
            double along = Math.sqrt(min * min - across * across);
            for (int sign = -1; sign <= 1; sign += 2) {
                double cx = vertical ? line : shipX + sign * along;
                double cy = vertical ? shipY + sign * along : line;
                if (cx < limit || cx > right || cy < limit || cy > top) {
                    continue;
                }
                double d = (cx - x) * (cx - x) + (cy - y) * (cy - y);
                if (d < best) {
                    best = d;
                    bestX = cx;
                    bestY = cy;
                }
            }
        }
        x = Math.clamp(bestX, limit, right);
        y = Math.clamp(bestY, limit, top);
        vx = vy = 0;
    }

    /** Banking follows the horizontal speed, one frame every few steps, as the player's. */
    private void steerBank() {
        int wanted = Math.clamp(
                Math.round(ShipSpec.HARD_BANK * vx / spec.craft().speed()), -ShipSpec.HARD_BANK, ShipSpec.HARD_BANK);
        if (bank == wanted) {
            bankTimer = 0;
        } else if (++bankTimer >= spec.craft().bankStepTicks()) {
            bank += Integer.signum(wanted - bank);
            bankTimer = 0;
        }
    }

    /**
     * Whether he fires a volley now: his gun is ready and the player fires, whether he has a target
     * or not (the target only steers a homing shot's lock). Starts the gun's interval when he does.
     */
    boolean fires(boolean playerFiring) {
        if (ejected || cooldown > 0 || !playerFiring) {
            return false;
        }
        cooldown = spec.gun().intervalTicks();
        sinceShot = 0;
        return true;
    }

    /** Starts a new step's contact bookkeeping: the air enemies touching him are counted afresh. */
    void beginContacts() {
        touchingCount = 0;
    }

    /**
     * An air enemy touches him: returns whether it is a new contact (one that did not touch him in
     * the last step), which hurts; a lasting contact hurts once.
     */
    boolean touch(int serial) {
        if (touchingCount < CONTACTS) {
            touching[touchingCount++] = serial;
        }
        for (int i = 0; i < contactCount; i++) {
            if (contacts[i] == serial) {
                return false;
            }
        }
        return true;
    }

    /** Ends the step's contact bookkeeping. */
    void endContacts() {
        System.arraycopy(touching, 0, contacts, 0, touchingCount);
        contactCount = touchingCount;
    }

    /**
     * He takes {@code damage} (no shield, no mercy time): the hit's event, his low-armour event the
     * first time he drops below its share in the attempt, and at zero the ejection, whose pod keeps
     * clear of the ship at (shipX, shipY). Returns whether he ejected.
     */
    boolean hit(double damage, double shipX, double shipY, SimEvents events) {
        if (ejected || !(damage > 0)) {
            return false;
        }
        armour = Math.max(0, armour - damage);
        events.add(SimEvents.Type.WINGMAN_HIT, x, y, (int) Math.ceil(damage));
        if (armour <= 0) {
            eject(shipX, shipY, events);
            return true;
        }
        WingmanSpec.Craft craft = spec.craft();
        if (!critical && armour < craft.lowArmour() * craft.maxArmour()) {
            critical = true;
            events.add(SimEvents.Type.WINGMAN_CRITICAL, x, y);
        }
        return false;
    }

    /**
     * At zero armour: the pod pops out and drifts to a side edge ({@link #podSide}); he is out for
     * the attempt.
     */
    private void eject(double shipX, double shipY, SimEvents events) {
        ejected = true;
        ejectTicks = 0;
        ejectX = x;
        ejectY = y;
        podSign = podSide(x, y, shipX, shipY);
        vx = vy = 0;
        target = -1;
        dodgeX = dodgeY = 0;
        events.add(SimEvents.Type.WINGMAN_EJECTED, x, y, podSign);
    }

    /**
     * The side edge an eject pod at (x, y) drifts to, -1 left or 1 right: the nearer one, unless the
     * ship at (shipX, shipY) lies on that way (beside the pod's line, within {@link #POD_CLEARANCE}
     * above or below it); then the other one, so the pod never crosses the ship.
     */
    static int podSide(double x, double y, double shipX, double shipY) {
        int nearer = x < PlayField.WIDTH / 2.0 ? -1 : 1;
        boolean inTheWay = Math.signum(shipX - x) == nearer && Math.abs(shipY - y) < POD_CLEARANCE;
        return inTheWay ? -nearer : nearer;
    }

    void addTo(StateHash hash) {
        hash.add(x)
                .add(y)
                .add(vx)
                .add(vy)
                .add(bank)
                .add(bankTimer)
                .add(armour)
                .add(critical ? 1 : 0)
                .add(ejected ? 1 : 0)
                .add(ejectTicks)
                .add(formation.ordinal())
                .add(formationWait)
                .add(side.ordinal())
                .add(sideWait)
                .add(inside)
                .add(fromX)
                .add(fromY)
                .add(glided)
                .add(scanWait)
                .add(dodgeWait)
                .add(pendingX)
                .add(pendingY)
                .add(dodgeX)
                .add(dodgeY)
                .add(dodgeLeft)
                .add(luck.state())
                .add(target)
                .add(targetWait)
                .add(cooldown)
                .add(sinceShot)
                .add(contactCount);
        for (int i = 0; i < contactCount; i++) {
            hash.add(contacts[i]);
        }
        for (int m = 0; m < DECIDED; m++) {
            if (decidedTicks[m] > 0) {
                hash.add(decidedVx[m])
                        .add(decidedVy[m])
                        .add(decidedLine[m])
                        .add(decidedTicks[m])
                        .add(reacted[m] ? 1 : 0);
            }
        }
    }

    Hull hull() {
        return hull;
    }

    public WingmanSpec spec() {
        return spec;
    }

    /** His gun at its level. */
    public WeaponSpec gun() {
        return spec.gun();
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

    public double armour() {
        return armour;
    }

    public double maxArmour() {
        return spec.craft().maxArmour();
    }

    /** Whether he has ejected in this attempt: out for the rest of it. */
    public boolean ejected() {
        return ejected;
    }

    /** Whether his armour has dropped below his low-armour share in this attempt. */
    public boolean wasCritical() {
        return critical;
    }

    /** The formation he flies (or glides to). */
    public Formation formation() {
        return formation;
    }

    /** The side he flies on now: his own, or the mirrored one while his own slot is outside the field. */
    public WingmanSpec.Side side() {
        return side;
    }

    /** His target (an enemy's serial, or a set-piece part's lock serial); -1 for none. */
    public int target() {
        return target;
    }

    /** Whether he is sidestepping a bullet now. */
    public boolean dodging() {
        return dodgeLeft > 0;
    }

    /** Steps since his gun last fired, for its muzzle flash; {@link Integer#MAX_VALUE} before its first shot. */
    public int ticksSinceShot() {
        return sinceShot;
    }

    /** Steps since he ejected; 0 while he flies. */
    public int ticksSinceEject() {
        return ejected ? ejectTicks : 0;
    }

    /** Where his eject pod is: drifting from where he ejected to a side edge ({@link #podSide}). */
    public double podX(double alpha) {
        return ejectX + podSign * podStep * (ejectTicks + alpha);
    }

    public double podY() {
        return ejectY;
    }
}
