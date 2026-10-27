package dev.advancedgear.abilities;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import net.kyori.adventure.text.format.NamedTextColor;

/** Infernal Greatsword (active): slam the ground, damaging, igniting and launching everything nearby. */
public final class MagmaSlamAbility implements Ability {
    @Override public String id() { return "magma_slam"; }
    @Override public boolean active() { return true; }

    @Override
    public boolean activate(AbilityContext c) {
        Player p = c.player();
        ConfigurationSection s = c.cfg();
        double radius = s.getDouble("radius", 4.5);
        double damage = s.getDouble("damage", 10.0);
        double kb = s.getDouble("knockback", 1.1);
        double launch = s.getDouble("launch", 0.55);
        double ignite = s.getDouble("ignite-seconds", 4.0);
        Location center = p.getLocation();

        int hits = 0;
        for (Entity en : p.getNearbyEntities(radius, radius, radius)) {
            if (!(en instanceof LivingEntity le) || !c.manager().canTarget(p, le)) continue;
            double dist = le.getLocation().distance(center);
            if (dist > radius) continue;
            double scaled = damage * (1.0 - 0.4 * dist / radius);
            if (c.manager().dealDamage(p, le, scaled)) {
                hits++;
                c.manager().ignite(p, le, ignite);
                Vector dir = le.getLocation().toVector().subtract(center.toVector()).setY(0);
                if (dir.lengthSquared() > 1.0E-6) dir.normalize().multiply(kb);
                le.setVelocity(dir.setY(launch));
            }
        }
        Fx fx = c.manager().fx();
        String particle = s.getString("particle", "flame");
        fx.ring(center, particle, radius * 0.5, 24);
        fx.ring(center, particle, radius, 36);
        fx.particles(center.clone().add(0, 0.5, 0), "lava", 20, radius * 0.3);
        fx.sound(center, s.getString("sound", "entity.generic.explode"), 0.9f, 0.7f);
        c.manager().feedback(p, s.getString("display-name", "Magma Slam") + (hits > 0 ? " - " + hits + " hit" : ""), NamedTextColor.GOLD);
        return true;
    }
}
