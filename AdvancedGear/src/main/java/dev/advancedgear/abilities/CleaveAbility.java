package dev.advancedgear.abilities;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.util.Vector;

/** Infernal Greatsword (passive): hits also strike a few enemies standing next to the victim. */
public final class CleaveAbility implements Ability {
    @Override public String id() { return "cleave"; }

    @Override
    public void onAttack(AbilityContext c, LivingEntity victim, EntityDamageByEntityEvent e) {
        Player p = c.player();
        ConfigurationSection s = c.cfg();
        double radius = s.getDouble("radius", 2.5);
        double fraction = s.getDouble("damage-fraction", 0.4);
        int max = s.getInt("max-targets", 3);
        double dmg = e.getFinalDamage() * fraction;
        if (dmg <= 0) return;
        Vector facing = p.getLocation().getDirection().setY(0);
        if (facing.lengthSquared() < 1.0E-6) return;
        facing.normalize();

        int count = 0;
        for (Entity en : victim.getNearbyEntities(radius, radius, radius)) {
            if (count >= max) break;
            if (!(en instanceof LivingEntity le) || le.equals(victim) || !c.manager().canTarget(p, le)) continue;
            Vector to = le.getLocation().toVector().subtract(p.getLocation().toVector()).setY(0);
            if (to.lengthSquared() < 1.0E-6 || to.normalize().dot(facing) < 0.2) continue;   // front arc only
            if (c.manager().dealDamage(p, le, dmg)) count++;
        }
        if (count > 0) {
            Location at = victim.getLocation().add(0, 1, 0);
            c.manager().fx().particles(at, s.getString("particle", "sweep_attack"), 1, 0.3);
            c.manager().fx().sound(at, s.getString("sound", "entity.player.attack.sweep"), 0.7f, 0.8f);
        }
    }
}
