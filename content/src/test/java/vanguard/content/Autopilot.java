package vanguard.content;

import vanguard.sim.Command;
import vanguard.sim.Debris;
import vanguard.sim.Enemy;
import vanguard.sim.EnemyBullet;
import vanguard.sim.GroundObject;
import vanguard.sim.Mine;
import vanguard.sim.Pickup;
import vanguard.sim.PlayField;
import vanguard.sim.SetPiece;
import vanguard.sim.Sortie;

/**
 * A simple pilot for headless runs of a whole level: fires all the time, lines up under the lowest
 * enemy on screen (or a ground object when the air is clear), sidesteps bullets, rammers, armed
 * spores and large debris that come close, stays under a set piece on the player's layer and
 * aims at its vital part, and picks up what drops when nothing threatens. It reads the sortie's state only, so
 * it is deterministic; the recorded replay stores its commands.
 */
final class Autopilot {
    private static final double CRUISE_Y = 110;
    private static final double DANGER = 70;
    private static final double DEAD_ZONE = 4;

    private Autopilot() {}

    static int commands(Sortie sortie) {
        double shipX = sortie.ship().x();
        double shipY = sortie.ship().y();
        int commands = Command.FIRE.bit();
        double threat = threat(sortie, shipX, shipY);
        if (threat != 0) {
            return commands | (threat < 0 ? Command.LEFT.bit() : Command.RIGHT.bit()) | Command.DOWN.bit();
        }
        double targetX = shipX;
        double targetY = CRUISE_Y;
        SetPiece piece = descended(sortie);
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
        if (lowest != null) {
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
        if (lowest != null && sortie.pickupCount() > 0) {
            Pickup pickup = sortie.pickup(0);
            if (Math.abs(pickup.renderX() - shipX) < 120 && pickup.renderY(1) < 260) {
                targetX = pickup.renderX();
                targetY = Math.max(40, pickup.renderY(1) - 10);
            }
        }
        return commands | steer(shipX, shipY, targetX, targetY);
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

    /** Where to line up under a set piece: its vital part, while it lives, else its centre. */
    private static double vitalX(SetPiece piece) {
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

    /** The side to dodge to: negative = left, positive = right, 0 = nothing close. */
    private static double threat(Sortie sortie, double shipX, double shipY) {
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
            if (chunk.large() && Math.abs(dx) < halfW + 30 && dy > -halfH - 20 && dy < halfH + DANGER) {
                return away(dx, shipX);
            }
        }
        for (int i = 0; i < sortie.bulletCount(); i++) {
            EnemyBullet bullet = sortie.bullet(i);
            double dx = bullet.renderX(1) - shipX;
            double dy = bullet.renderY(1) - shipY;
            if (Math.abs(dx) < 24 && dy > -10 && dy < DANGER) {
                return away(dx, shipX);
            }
        }
        for (int i = 0; i < sortie.enemyCount(); i++) {
            Enemy enemy = sortie.enemy(i);
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
