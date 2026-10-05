package vanguard.content;

import vanguard.sim.Command;
import vanguard.sim.Debris;
import vanguard.sim.Enemy;
import vanguard.sim.EnemyBullet;
import vanguard.sim.GroundObject;
import vanguard.sim.Layer;
import vanguard.sim.Lob;
import vanguard.sim.Mine;
import vanguard.sim.Pickup;
import vanguard.sim.PlayField;
import vanguard.sim.SetPiece;
import vanguard.sim.SimStep;
import vanguard.sim.Sortie;

/**
 * A simple pilot for headless runs of a whole level: fires all the time, lines up under the lowest
 * enemy on screen (or a ground object when the air is clear), sidesteps bullets, rammers, armed
 * spores and large debris that come close, stays under a set piece on the player's layer and
 * aims at its vital part (a boss's lowest part that takes damage; in a boss fight each move is
 * chosen by a short look-ahead over the bullets and air enemies, keeping to the field's centre
 * band), and picks up what drops when nothing threatens. At Level 05's batteries it flies low, so
 * a unit stays in its line of fire as long as possible. With a convoy (Level 04) it
 * cruises among the column, so more of the ground enemies' aimed shots (at the nearer of the ship
 * and the convoy) go for it, and it shoots the ground enemies nearest the convoy first. With a segment chain on the
 * screen (Level 06) it slips sideways to the side clear of its members and lines up under a
 * chain's head rather than its body (a cut grows a new head). It reads the sortie's state only, so
 * it is deterministic; the recorded replay stores its commands. Public for the campaign tests (the
 * Act 1 playthrough, {@code ActPlaythroughTest}).
 */
public final class Autopilot {
    private static final double CRUISE_Y = 110;
    private static final double DANGER = 70;
    private static final double DEAD_ZONE = 4;
    /** Its cruise height over a convoy (the column spans y = 118-526). */
    private static final double CONVOY_CRUISE = 220;
    /** How far it keeps from a lit rail (Level 05's sleds), px. */
    private static final double RAIL_CLEARANCE = 44;
    /** How close a chain member may come before it slips aside (Level 06), px. */
    private static final double CHAIN_DX = 52;
    /**
     * Its height while a battery unit is its target (Level 05): low, so a unit scrolling down stays
     * above it, in its line of fire, as long as possible.
     */
    private static final double BATTERY_Y = 45;

    private static final double CHAIN_ABOVE = 120;
    private static final double CHAIN_BELOW = 70;

    private Autopilot() {}

    public static int commands(Sortie sortie) {
        double shipX = sortie.ship().x();
        double shipY = sortie.ship().y();
        int commands = Command.FIRE.bit();
        double lob = lobThreat(sortie, shipX, shipY);
        if (lob != 0) {
            return commands | (lob < 0 ? Command.LEFT.bit() : Command.RIGHT.bit());
        }
        double coil = chainThreat(sortie, shipX, shipY);
        if (coil != 0) {
            return commands | (coil < 0 ? Command.LEFT.bit() : Command.RIGHT.bit());
        }
        double hazard = hazard(sortie, shipX, shipY);
        if (hazard != 0) {
            return commands | (hazard < 0 ? Command.LEFT.bit() : Command.RIGHT.bit()) | Command.DOWN.bit();
        }
        SetPiece piece = descended(sortie);
        if (piece != null && piece.boss().isPresent()) {
            return commands | bossMove(sortie, vitalX(piece), Math.min(CRUISE_Y, bodyBottom(piece) - 70));
        }
        double threat = threat(sortie, shipX, shipY);
        if (threat != 0) {
            return commands | (threat < 0 ? Command.LEFT.bit() : Command.RIGHT.bit()) | Command.DOWN.bit();
        }
        double targetX = shipX;
        double targetY = cruise(sortie);
        if (piece != null) {
            return commands | steer(shipX, shipY, vitalX(piece), Math.min(CRUISE_Y, bodyBottom(piece) - 70));
        }
        Enemy lowest = null;
        for (int i = 0; i < sortie.enemyCount(); i++) {
            Enemy enemy = sortie.enemy(i);
            double y = enemy.renderY(1);
            if (y < PlayField.HEIGHT - 10 && y > shipY + 40 && (lowest == null || y < lowest.renderY(1))) {
                lowest = enemy;
            }
        }
        Enemy battery = battery(sortie, shipY);
        if (battery != null) {
            lowest = battery;
            targetY = Math.min(targetY, BATTERY_Y);
        }
        Enemy head = chainHead(sortie, shipY);
        if (head != null) {
            lowest = head;
        }
        Enemy walker = convoyThreat(sortie, shipY);
        if (walker != null) {
            targetX = walker.renderX(1);
        } else if (lowest != null) {
            targetX = lowest.renderX(1);
        } else {
            GroundObject ground = lowestGround(sortie, shipY);
            Pickup pickup = sortie.pickupCount() > 0 ? sortie.pickup(0) : null;
            if (pickup != null) {
                targetX = pickup.renderX();
                targetY = Math.max(40, pickup.renderY(1) - 10);
            } else if (ground != null) {
                targetX = ground.renderX();
            }
        }
        if ((lowest != null || walker != null) && sortie.pickupCount() > 0) {
            Pickup pickup = sortie.pickup(0);
            if (Math.abs(pickup.renderX() - shipX) < 120 && pickup.renderY(1) < 260) {
                targetX = pickup.renderX();
                targetY = Math.max(40, pickup.renderY(1) - 10);
            }
        }
        return commands | steer(shipX, shipY, clearOfRail(sortie, targetX), targetY);
    }

    /**
     * Level 05: the lowest unit of a destroy-targets group (a battery) on the screen above the ship,
     * which must die before it leaves; null without one.
     */
    private static Enemy battery(Sortie sortie, double shipY) {
        if (sortie.script().targets().isEmpty()) {
            return null;
        }
        Enemy lowest = null;
        for (int i = 0; i < sortie.enemyCount(); i++) {
            Enemy enemy = sortie.enemy(i);
            double y = enemy.renderY(1);
            if (enemy.groupIndex() >= 0
                    && y < PlayField.HEIGHT - 10
                    && y > shipY + 20
                    && (lowest == null || y < lowest.renderY(1))) {
                lowest = enemy;
            }
        }
        return lowest;
    }

    /** Level 06: the lowest head of a segment chain on the screen above the ship; null without one. */
    private static Enemy chainHead(Sortie sortie, double shipY) {
        Enemy lowest = null;
        for (int c = 0; c < sortie.chainCount(); c++) {
            Enemy head = sortie.chain(c).member(0);
            if (head == null) {
                continue;
            }
            double x = head.renderX(1);
            double y = head.renderY(1);
            if (x > 0
                    && x < PlayField.WIDTH
                    && y < PlayField.HEIGHT - 10
                    && y > shipY + 60
                    && (lowest == null || y < lowest.renderY(1))) {
                lowest = head;
            }
        }
        return lowest;
    }

    /**
     * Level 06: with a member of a segment chain close above, beside or below, the side whose
     * position 50 px away lies farther from every member (negative = left); 0 when none is close.
     */
    private static double chainThreat(Sortie sortie, double shipX, double shipY) {
        boolean close = false;
        for (int c = 0; c < sortie.chainCount() && !close; c++) {
            var chain = sortie.chain(c);
            for (int k = 0; k < chain.size(); k++) {
                Enemy member = chain.member(k);
                if (member != null
                        && Math.abs(member.renderX(1) - shipX) < CHAIN_DX
                        && member.renderY(1) - shipY > -CHAIN_BELOW
                        && member.renderY(1) - shipY < CHAIN_ABOVE) {
                    close = true;
                    break;
                }
            }
        }
        if (!close) {
            return 0;
        }
        if (shipX < 60) {
            return 1;
        }
        if (shipX > PlayField.WIDTH - 60) {
            return -1;
        }
        return chainClearance(sortie, shipX - 50, shipY) > chainClearance(sortie, shipX + 50, shipY) ? -1 : 1;
    }

    /** The distance from (x, y) to the nearest chain member, the vertical counted at 0.7. */
    private static double chainClearance(Sortie sortie, double x, double y) {
        double nearest = Double.POSITIVE_INFINITY;
        for (int c = 0; c < sortie.chainCount(); c++) {
            var chain = sortie.chain(c);
            for (int k = 0; k < chain.size(); k++) {
                Enemy member = chain.member(k);
                if (member != null) {
                    nearest = Math.min(nearest, Math.hypot(member.renderX(1) - x, 0.7 * (member.renderY(1) - y)));
                }
            }
        }
        return nearest;
    }

    /** Level 05: while the rail is lit (lights or a sled), a target beside the rail rather than on it. */
    private static double clearOfRail(Sortie sortie, double targetX) {
        if (sortie.sled().isEmpty() || !sortie.sled().get().lit()) {
            return targetX;
        }
        double rail = sortie.sled().get().spec().x();
        return Math.abs(targetX - rail) < RAIL_CLEARANCE ? rail - RAIL_CLEARANCE : targetX;
    }

    /**
     * Its cruise height: low, or with a convoy alive among the column, high enough that the air
     * enemies above still give it time and close enough that the ground enemies' aimed shots go for
     * it rather than the crawlers.
     */
    private static double cruise(Sortie sortie) {
        for (int k = 0; k < sortie.allyCount(); k++) {
            if (sortie.ally(k).alive()) {
                return CONVOY_CRUISE;
            }
        }
        return CRUISE_Y;
    }

    /**
     * With a convoy, the ground enemy on the screen above the ship that is closest to a convoy unit
     * (a turret or a walker about to fire at it or claw it); null without one.
     */
    private static Enemy convoyThreat(Sortie sortie, double shipY) {
        if (sortie.allyCount() == 0) {
            return null;
        }
        Enemy best = null;
        double bestDistance = Double.POSITIVE_INFINITY;
        for (int i = 0; i < sortie.enemyCount(); i++) {
            Enemy enemy = sortie.enemy(i);
            double x = enemy.renderX(1);
            double y = enemy.renderY(1);
            if (enemy.spec().layer() != Layer.GROUND
                    || y > PlayField.HEIGHT - 10
                    || y < shipY - 20
                    || x < 0
                    || x > PlayField.WIDTH) {
                continue;
            }
            for (int k = 0; k < sortie.allyCount(); k++) {
                if (sortie.ally(k).alive()) {
                    double distance = Math.hypot(
                            sortie.ally(k).renderX(1) - x, sortie.ally(k).renderY(1) - y);
                    if (distance < bestDistance) {
                        bestDistance = distance;
                        best = enemy;
                    }
                }
            }
        }
        return best;
    }

    private static int steer(double shipX, double shipY, double targetX, double targetY) {
        int commands = 0;
        if (targetX < shipX - DEAD_ZONE) {
            commands |= Command.LEFT.bit();
        } else if (targetX > shipX + DEAD_ZONE) {
            commands |= Command.RIGHT.bit();
        }
        if (targetY < shipY - DEAD_ZONE) {
            commands |= Command.DOWN.bit();
        } else if (targetY > shipY + DEAD_ZONE) {
            commands |= Command.UP.bit();
        }
        return commands;
    }

    /** The moves the boss-fight planner weighs: stay, the four sides, the four diagonals. */
    private static final int[] MOVES = {
        0,
        Command.LEFT.bit(),
        Command.RIGHT.bit(),
        Command.UP.bit(),
        Command.DOWN.bit(),
        Command.LEFT.bit() | Command.UP.bit(),
        Command.RIGHT.bit() | Command.UP.bit(),
        Command.LEFT.bit() | Command.DOWN.bit(),
        Command.RIGHT.bit() | Command.DOWN.bit()
    };

    private static final double DIAGONAL = Math.sqrt(0.5);
    private static final double[] MOVE_X = {0, -1, 1, 0, 0, -DIAGONAL, DIAGONAL, -DIAGONAL, DIAGONAL};
    private static final double[] MOVE_Y = {0, 0, 0, 1, -1, DIAGONAL, DIAGONAL, -DIAGONAL, -DIAGONAL};
    /** How far ahead the planner looks, s, in samples of {@link #SAMPLE} s. */
    private static final int SAMPLES = 12;

    private static final double SAMPLE = 0.05;
    /** The ship's hull from its centre, px, with a small bullet's radius added. */
    private static final double HULL_X = 22;

    private static final double HULL_Y = 25;
    /** A move is safe when nothing comes closer to the hull than this in the look-ahead, px. */
    private static final double SAFE = 6;
    /** The band of the field it prefers while it dodges a boss, px from the left. */
    private static final double BAND_LEFT = 130;

    private static final double BAND_RIGHT = PlayField.WIDTH - 130.0;

    /**
     * In a boss fight: the move toward the boss's vital part ({@code targetX}, {@code targetY}) if
     * no bullet or air enemy comes too close to the hull while it flies it for the next 0.6 s (each
     * flying straight on, the ship at full speed); otherwise the safe move that keeps nearest the
     * target and the field's centre band (it keeps away from the edges, where an aimed burst traps
     * it); with no safe move, the one that keeps farthest from them.
     */
    private static int bossMove(Sortie sortie, double targetX, double targetY) {
        double shipX = sortie.ship().x();
        double shipY = sortie.ship().y();
        int wanted = steer(shipX, shipY, targetX, targetY);
        int best = -1;
        double bestScore = Double.NEGATIVE_INFINITY;
        int clearest = 0;
        double clearestDistance = Double.NEGATIVE_INFINITY;
        for (int m = 0; m < MOVES.length; m++) {
            double clearance = clearance(sortie, MOVE_X[m], MOVE_Y[m]);
            if (clearance > clearestDistance) {
                clearestDistance = clearance;
                clearest = m;
            }
            if (clearance < SAFE) {
                continue;
            }
            if (MOVES[m] == wanted) {
                return wanted;
            }
            double x = ahead(shipX, MOVE_X[m], sortie, true);
            double y = ahead(shipY, MOVE_Y[m], sortie, false);
            double score = -Math.abs(x - targetX)
                    - 0.5 * Math.abs(y - targetY)
                    - 2 * Math.max(0, Math.max(BAND_LEFT - x, x - BAND_RIGHT));
            if (score > bestScore) {
                bestScore = score;
                best = m;
            }
        }
        return MOVES[best >= 0 ? best : clearest];
    }

    /** Where a move takes the ship in two samples, inside its bounds. */
    private static double ahead(double from, double direction, Sortie sortie, boolean horizontal) {
        double margin = sortie.ship().spec().edgeLimit();
        double to = from + direction * sortie.ship().spec().speed() * 2 * SAMPLE;
        return Math.clamp(to, margin, (horizontal ? PlayField.WIDTH : PlayField.HEIGHT) - margin);
    }

    /**
     * The closest any enemy bullet or air enemy comes to the hull's box over the look-ahead while
     * the ship flies (dx, dy) at full speed, px (0 = a hit).
     */
    private static double clearance(Sortie sortie, double dx, double dy) {
        double speed = sortie.ship().spec().speed();
        double margin = sortie.ship().spec().edgeLimit();
        double x0 = sortie.ship().x();
        double y0 = sortie.ship().y();
        double nearest = Double.POSITIVE_INFINITY;
        for (int k = 1; k <= SAMPLES; k++) {
            double t = k * SAMPLE;
            double x = Math.clamp(x0 + dx * speed * t, margin, PlayField.WIDTH - margin);
            double y = Math.clamp(y0 + dy * speed * t, margin, PlayField.HEIGHT - margin);
            double steps = t / SimStep.SECONDS;
            for (int i = 0; i < sortie.bulletCount(); i++) {
                EnemyBullet bullet = sortie.bullet(i);
                double bx = bullet.renderX(1) + (bullet.renderX(1) - bullet.renderX(0)) * steps;
                double by = bullet.renderY(1) + (bullet.renderY(1) - bullet.renderY(0)) * steps;
                nearest = Math.min(nearest, gap(bx - x, by - y, HULL_X, HULL_Y));
            }
            for (int i = 0; i < sortie.enemyCount(); i++) {
                Enemy enemy = sortie.enemy(i);
                if (enemy.spec().layer() == Layer.GROUND) {
                    continue;
                }
                double ex = enemy.renderX(1) + (enemy.renderX(1) - enemy.renderX(0)) * steps;
                double ey = enemy.renderY(1) + (enemy.renderY(1) - enemy.renderY(0)) * steps;
                nearest = Math.min(
                        nearest,
                        gap(
                                ex - x,
                                ey - y,
                                HULL_X + enemy.hitbox().width() / 2,
                                HULL_Y + enemy.hitbox().height() / 2));
            }
        }
        return nearest;
    }

    /** The distance between a point (dx, dy) from a box's centre and the box (halfW, halfH); 0 inside. */
    private static double gap(double dx, double dy, double halfW, double halfH) {
        return Math.hypot(Math.max(0, Math.abs(dx) - halfW), Math.max(0, Math.abs(dy) - halfH));
    }

    /** A set piece on the player's layer, or null. */
    private static SetPiece descended(Sortie sortie) {
        for (int i = 0; i < sortie.setPieceCount(); i++) {
            SetPiece piece = sortie.setPiece(i);
            if (piece.present() && piece.onPlane()) {
                return piece;
            }
        }
        return null;
    }

    /**
     * Where to line up under a set piece: its vital part, while it lives, else its centre; under a
     * boss, its lowest part that takes damage now (a head, then the core).
     */
    private static double vitalX(SetPiece piece) {
        if (piece.boss().isPresent()) {
            int lowest = -1;
            for (int p = 0; p < piece.partCount(); p++) {
                if (!piece.partWrecked(p)
                        && !piece.partShielded(p)
                        && (lowest < 0 || piece.partY(p) < piece.partY(lowest))) {
                    lowest = p;
                }
            }
            return lowest < 0 ? piece.renderX(1) : piece.partX(lowest);
        }
        for (int p = 0; p < piece.partCount(); p++) {
            if (piece.spec().parts().get(p).vital() && !piece.partWrecked(p)) {
                return piece.partX(p);
            }
        }
        return piece.renderX(1);
    }

    private static double bodyBottom(SetPiece piece) {
        return piece.renderY(1) - piece.spec().body().height() / 2;
    }

    /**
     * Level 05: the side to slip to out of a mortar's marker, sideways only and toward the battery
     * it is shooting at if there is one (it keeps its line); 0 when no marker lies under it.
     */
    private static double lobThreat(Sortie sortie, double shipX, double shipY) {
        for (int i = 0; i < sortie.lobCount(); i++) {
            Lob lob = sortie.lob(i);
            double dx = lob.targetX() - shipX;
            double dy = lob.targetY() - shipY;
            double reach = lob.impactRadius() + 30;
            if (dx * dx + dy * dy < reach * reach) {
                Enemy battery = battery(sortie, shipY);
                if (battery != null && Math.abs(battery.renderX(1) - lob.targetX()) > reach) {
                    return battery.renderX(1) < shipX ? -1 : 1;
                }
                return away(dx, shipX);
            }
        }
        return 0;
    }

    /**
     * The side to dodge a lit rail, a rising spore or large debris to: negative = left, positive =
     * right, 0 = nothing close.
     */
    private static double hazard(Sortie sortie, double shipX, double shipY) {
        if (sortie.sled().isPresent() && sortie.sled().get().lit()) {
            double dx = sortie.sled().get().spec().x() - shipX;
            if (Math.abs(dx) < RAIL_CLEARANCE) {
                return dx >= 0 ? -1 : 1;
            }
        }
        for (int i = 0; i < sortie.mineCount(); i++) {
            Mine mine = sortie.mine(i);
            double dx = mine.renderX(1) - shipX;
            double dy = mine.renderY(1) - shipY;
            if (mine.rising() > 0.5 && Math.abs(dx) < 26 && dy > -16 && dy < DANGER) {
                return away(dx, shipX);
            }
        }
        for (int i = 0; i < sortie.debrisCount(); i++) {
            Debris chunk = sortie.debris(i);
            double dx = chunk.renderX(1) - shipX;
            double dy = chunk.renderY(1) - shipY;
            double halfW = chunk.spec().size().width() / 2;
            double halfH = chunk.spec().size().height() / 2;
            if ((chunk.large() || chunk.thrown())
                    && Math.abs(dx) < halfW + 30
                    && dy > -halfH - 20
                    && dy < halfH + DANGER) {
                return away(dx, shipX);
            }
        }
        return 0;
    }

    /** The side to dodge bullets and rammers to: negative = left, positive = right, 0 = nothing close. */
    private static double threat(Sortie sortie, double shipX, double shipY) {
        // Level 05: with a battery unit on the screen it holds its line and dodges only the closest bullets.
        boolean holding = battery(sortie, shipY) != null;
        double bulletDx = holding ? 18 : 24;
        double bulletDy = holding ? 40 : DANGER;
        for (int i = 0; i < sortie.bulletCount(); i++) {
            EnemyBullet bullet = sortie.bullet(i);
            double dx = bullet.renderX(1) - shipX;
            double dy = bullet.renderY(1) - shipY;
            if (Math.abs(dx) < bulletDx && dy > -10 && dy < bulletDy) {
                return away(dx, shipX);
            }
        }
        // Level 05: the batteries' units lie on the ground and cannot ram, so it flies over them.
        boolean overGround = !sortie.script().targets().isEmpty();
        for (int i = 0; i < sortie.enemyCount(); i++) {
            Enemy enemy = sortie.enemy(i);
            if (overGround && enemy.spec().layer() == Layer.GROUND) {
                continue;
            }
            double dx = enemy.renderX(1) - shipX;
            double dy = enemy.renderY(1) - shipY;
            if (Math.abs(dx) < 30 && Math.abs(dy) < DANGER) {
                return away(dx, shipX);
            }
        }
        return 0;
    }

    private static double away(double dx, double shipX) {
        if (shipX < 60) {
            return 1;
        }
        if (shipX > PlayField.WIDTH - 60) {
            return -1;
        }
        return dx >= 0 ? -1 : 1;
    }

    private static GroundObject lowestGround(Sortie sortie, double shipY) {
        GroundObject lowest = null;
        for (int i = 0; i < sortie.groundObjectCount(); i++) {
            GroundObject object = sortie.groundObject(i);
            double y = object.renderY(1);
            if (!object.spent()
                    && y > shipY + 30
                    && y < PlayField.HEIGHT
                    && (lowest == null || y < lowest.renderY(1))) {
                lowest = object;
            }
        }
        return lowest;
    }
}
