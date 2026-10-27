package dev.advancedgear.abilities;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.util.Vector;

/** Magma Warhammer (passive): a falling hit sends a burning shockwave through nearby enemies. */
public final class ShockwaveAbility implements Ability {
    @Override public String id() { return "shockwave"; }

    @Override
    public void onAttack(AbilityContext c, LivingEntity victim, EntityDamageByEntityEvent e) {
        Player p = c.player();
        ConfigurationSection s = c.cfg();
        if (p.getFallDistance() < s.getDouble("min-fall-distance", 2.0)) return;
        if (!c.manager().ready(p, id())) return;
        c.manager().start(p, id(), s.getDouble("internal-cooldown-seconds", 4.0));

        double radius = s.getDouble("radius", 3.5);
        double damage = s.getDouble("damage", 6.0);
        double kb = s.getDouble("knockback", 0.9);
        double ignite = s.getDouble("ignite-seconds", 3.0);
        Location center = victim.getLocation();

        for (Entity en : victim.getNearbyEntities(radius, radius, radius)) {
            if (!(en instanceof LivingEntity le) || le.equals(victim) || !c.manager().canTarget(p, le)) continue;
            if (c.manager().dealDamage(p, le, damage)) {
                c.manager().ignite(p, le, ignite);
                Vector dir = le.getLocation().toVector().subtract(center.toVector()).setY(0);
                if (dir.lengthSquared() > 1.0E-6) dir.normalize().multiply(kb);
                le.setVelocity(dir.setY(0.4));
            }
        }
        c.manager().fx().ring(center, s.getString("particle", "lava"), radius, 30);
        c.manager().fx().sound(center, s.getString("sound", "entity.generic.explode"), 0.8f, 0.9f);
    }
}
