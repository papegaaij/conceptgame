package vanguard.content;

import vanguard.sim.Hitbox;
import vanguard.sim.Loadout;
import vanguard.sim.Plating;
import vanguard.sim.PulseCannon;
import vanguard.sim.ShieldModel;
import vanguard.sim.ShipSpec;
import vanguard.sim.SkitterSpec;

/**
 * Builds the simulation's specs from the loaded content. The dependency points this way
 * ({@code content → sim}) so that the simulation knows nothing about data files, YAML or Jackson:
 * it defines the records it needs, and this class fills them.
 */
public final class SimSpecs {
    private SimSpecs() {}

    /** The starting fit: the hull with the starter engine, the Pulse Cannon at L1, the starter shield and plating. */
    public static Loadout starterLoadout(Content content) {
        return new Loadout(
                ship(content, content.systems().engines().getFirst()),
                pulseCannon(content),
                shield(content, content.shields().models().getFirst()),
                plating(content.armour().plating().getFirst()));
    }

    static ShipSpec ship(Content content, SystemsData.Engine engine) {
        ShipData ship = content.ship();
        return new ShipSpec(
                engine.speed(),
                ship.accelerationSeconds(),
                ship.stopSeconds(),
                ship.precisionFactor(),
                ship.size(),
                ship.edgeGap(),
                hitbox(ship.hitbox()),
                ship.mercySeconds(),
                ship.size() / 2 - ship.mounts().front().y(),
                ship.bankChangeSteps() / ShipSpec.HARD_BANK);
    }

    /** The Pulse Cannon at level 1, a single bolt: the simulation fires no wider patterns yet. */
    static PulseCannon pulseCannon(Content content) {
        WeaponData cannon = content.weapon("pulse-cannon");
        WeaponData.Level level = cannon.levels().getFirst();
        return new PulseCannon(
                level.rate(), level.damage(), cannon.speed().orElseThrow().start(), hitbox(cannon.size()));
    }

    static ShieldModel shield(Content content, ShieldData.Model model) {
        return new ShieldModel(
                model.capacity(),
                model.regen(),
                model.delay(),
                content.shields().breakSeconds());
    }

    static Plating plating(ArmourData.Plating plating) {
        return new Plating(plating.max());
    }

    /** The Skitter on its snake paths. */
    public static SkitterSpec skitter(Content content) {
        EnemyData skitter = content.enemy("skitter");
        return new SkitterSpec(
                skitter.hp(),
                hitbox(skitter.hitbox()),
                skitter.speed(),
                skitter.movement().snake().orElseThrow().spacing(),
                content.enemyBasis().contactDamage().get(skitter.tier()),
                Layers.of(skitter.layer()));
    }

    private static Hitbox hitbox(Size size) {
        return new Hitbox(size.width(), size.height());
    }
}
