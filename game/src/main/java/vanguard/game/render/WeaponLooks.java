package vanguard.game.render;

import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.utils.Array;
import vanguard.sim.Armament;
import vanguard.sim.Shot;
import vanguard.sim.WeaponSpec;

/**
 * How the fitted weapons look (design/player/weapons, the projectile families of concept round 08,
 * produced by tools/art/pulse_cannon.py and tools/art/weapon_fx.py): per mount its projectile, its
 * muzzle flash and its impact, and the wing pod drawn on the ship. Straight shots have a sprite per
 * angle their patterns use, homing missiles an angle set of 32 headings, and the lance one per
 * level; bombs and shells are single sprites the renderer scales on their way down. Weapons without
 * their own art yet (Act 2) borrow the family of their delivery.
 */
public final class WeaponLooks {
    private static final int MISSILE_HEADINGS = 32;
    private static final int DEGREES = 360;

    /** The look of one mount. */
    static final class Look {
        /** By whole degree of heading (clockwise from up), the nearest drawn angle; one entry for one sprite. */
        final AtlasRegion[] shots;

        final AtlasRegion[] overdriveShots;
        /** Tracers, bolts and lances add light; missiles, bombs and shells are solid. */
        final boolean glowingShot;

        final Array<AtlasRegion> muzzle;
        final boolean glowingMuzzle;
        final Array<AtlasRegion> impact;
        /** The wing pod on the ship by banking frame, hard left .. hard right; {@code null} for other slots. */
        final Array<AtlasRegion> pod;
        /** The pod sprite's top-left on the 48x48 hull sprite per banking frame, y down. */
        final int[][] podOffsets;

        Look(
                AtlasRegion[] shots,
                AtlasRegion[] overdriveShots,
                boolean glowingShot,
                Array<AtlasRegion> muzzle,
                boolean glowingMuzzle,
                Array<AtlasRegion> impact,
                Array<AtlasRegion> pod,
                int[][] podOffsets) {
            this.shots = shots;
            this.overdriveShots = overdriveShots;
            this.glowingShot = glowingShot;
            this.muzzle = muzzle;
            this.glowingMuzzle = glowingMuzzle;
            this.impact = impact;
            this.pod = pod;
            this.podOffsets = podOffsets;
        }
    }

    private final Armament armament;
    private final Look[] looks;

    /**
     * @param levels the upgrade level of each mount, in the armament's order
     * @param pods the pods' offsets on the hull ({@code assets/pivots/pods.json})
     */
    public WeaponLooks(Armament armament, int[] levels, Sprites sprites, PodPivots pods) {
        this.armament = armament;
        looks = new Look[armament.size()];
        for (int m = 0; m < armament.size(); m++) {
            looks[m] = look(armament.mount(m), levels[m], sprites, pods);
        }
    }

    private static Look look(Armament.Mount mount, int level, Sprites sprites, PodPivots pods) {
        WeaponSpec weapon = mount.weapon();
        String slug = weapon.slug();
        boolean glowing;
        AtlasRegion[] shots;
        AtlasRegion[] overdrive;
        switch (slug) {
            case "pulse-cannon" -> {
                shots = overdrive = single(sprites.pulseBolt);
                glowing = true;
            }
            case "scatter-vulcan", "autocannon-pod", "side-splitter" -> {
                shots = overdrive = byAngle(sprites.frames(slug + "-shot"));
                glowing = true;
            }
            case "lance-laser" -> {
                Array<AtlasRegion> lances = sprites.frames("lance-laser-shot");
                shots = single(indexed(lances, level));
                overdrive = single(indexed(lances, level + 1));
                glowing = true;
            }
            case "micro-missile-pod" -> {
                shots = overdrive = byHeading(sprites.frames("micro-missile-pod-shot"));
                glowing = false;
            }
            case "bomb-rack", "hammer-mortar" -> {
                shots = overdrive = single(sprites.region(slug + "-shot"));
                glowing = false;
            }
            default -> {
                // A weapon without its own art yet: the family of its delivery.
                switch (weapon.delivery()) {
                    case BOLT -> {
                        shots = overdrive = single(sprites.pulseBolt);
                        glowing = true;
                    }
                    case HOMING -> {
                        shots = overdrive = byHeading(sprites.frames("micro-missile-pod-shot"));
                        glowing = false;
                    }
                    default -> {
                        shots = overdrive = single(sprites.region("bomb-rack-shot"));
                        glowing = false;
                    }
                }
            }
        }
        Array<AtlasRegion> pod = null;
        int[][] offsets = null;
        String podType = podType(slug);
        boolean wing = mount.slot() == Armament.Slot.LEFT_WING || mount.slot() == Armament.Slot.RIGHT_WING;
        if (wing && podType != null) {
            String name = "pod-" + podType + (mount.slot() == Armament.Slot.LEFT_WING ? "-left" : "-right");
            pod = sprites.frames(name);
            offsets = pods.offsets(name);
        }
        return new Look(
                shots,
                overdrive,
                glowing,
                muzzle(weapon.vfx(), sprites),
                !launcher(weapon.vfx()),
                impact(weapon.vfx(), sprites),
                pod,
                offsets);
    }

    /** The pod sprite type a wing weapon is drawn with (tools/art/stormhawk.py); {@code null} if it has none. */
    private static String podType(String slug) {
        return switch (slug) {
            case "autocannon-pod" -> "autocannon";
            case "bomb-rack" -> "bomb-rack";
            case "micro-missile-pod" -> "micro-missile";
            case "swivel-gun" -> "swivel";
            case "torpedo-pod" -> "torpedo";
            default -> null;
        };
    }

    private static boolean launcher(String vfx) {
        return vfx.equals("micromissile") || vfx.equals("mortar") || vfx.equals("bomb") || vfx.equals("missile");
    }

    private static Array<AtlasRegion> muzzle(String vfx, Sprites sprites) {
        return switch (vfx) {
            case "vulcan", "ballistic" -> sprites.frames("ballistic-muzzle");
            default -> launcher(vfx) ? sprites.frames("launcher-muzzle") : sprites.pulseMuzzle;
        };
    }

    private static Array<AtlasRegion> impact(String vfx, Sprites sprites) {
        return switch (vfx) {
            case "vulcan", "ballistic" -> sprites.frames("ballistic-impact");
            default -> launcher(vfx) ? sprites.frames("explosive-impact") : sprites.pulseImpact;
        };
    }

    private static AtlasRegion[] single(AtlasRegion region) {
        return new AtlasRegion[] {region};
    }

    private static AtlasRegion indexed(Array<AtlasRegion> regions, int index) {
        for (AtlasRegion region : regions) {
            if (region.index == index) {
                return region;
            }
        }
        throw new IllegalStateException("no frame " + index + " of '" + regions.first().name + "'");
    }

    /** Regions indexed by their angle in degrees: for every whole degree the nearest of them. */
    private static AtlasRegion[] byAngle(Array<AtlasRegion> regions) {
        AtlasRegion[] nearest = new AtlasRegion[DEGREES];
        for (int degree = 0; degree < DEGREES; degree++) {
            int best = Integer.MAX_VALUE;
            for (AtlasRegion region : regions) {
                int off = Math.abs(degree - region.index) % DEGREES;
                off = Math.min(off, DEGREES - off);
                if (off < best) {
                    best = off;
                    nearest[degree] = region;
                }
            }
        }
        return nearest;
    }

    /** An angle set of headings, k x 360/n degrees: for every whole degree the nearest heading. */
    private static AtlasRegion[] byHeading(Array<AtlasRegion> headings) {
        if (headings.size != MISSILE_HEADINGS) {
            throw new IllegalStateException("'" + headings.first().name + "' needs " + MISSILE_HEADINGS + " headings");
        }
        AtlasRegion[] nearest = new AtlasRegion[DEGREES];
        for (int degree = 0; degree < DEGREES; degree++) {
            nearest[degree] = headings.get(Math.round(degree * MISSILE_HEADINGS / (float) DEGREES) % MISSILE_HEADINGS);
        }
        return nearest;
    }

    Look look(int mount) {
        return looks[mount];
    }

    /** The impact animation of mount {@code m}'s shots. */
    public Array<AtlasRegion> impact(int m) {
        return looks[m].impact;
    }

    int size() {
        return looks.length;
    }

    /** The sprite of a shot in flight, for its heading and the pattern it was fired with. */
    AtlasRegion sprite(Shot shot) {
        Look look = looks[shot.mount()];
        AtlasRegion[] set =
                shot.weapon() == armament.mount(shot.mount()).overdrive() ? look.overdriveShots : look.shots;
        if (set.length == 1) {
            return set[0];
        }
        int degree = (int) Math.round(Math.toDegrees(shot.heading())) % DEGREES;
        return set[degree < 0 ? degree + DEGREES : degree];
    }
}
