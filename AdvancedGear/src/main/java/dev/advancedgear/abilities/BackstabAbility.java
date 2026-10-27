package dev.advancedgear.abilities;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.util.Vector;

/** Ember Dagger (passive): hitting a target from behind deals bonus damage and ignites it. */
public final class BackstabAbility implements Ability {
    @Override public String id() { return "backstab"; }

    @Override
    public void onAttack(AbilityContext c, LivingEntity victim, EntityDamageByEntityEvent e) {
        Player p = c.player();
        ConfigurationSection s = c.cfg();
        if (!c.manager().ready(p, id()) || !c.manager().canTarget(p, victim)) return;

        Vector facing = victim.getLocation().getDirection().setY(0);
        Vector toVictim = victim.getLocation().toVector().subtract(p.getLocation().toVector()).setY(0);
        if (facing.lengthSquared() < 1.0E-6 || toVictim.lengthSquared() < 1.0E-6) return;
        double dot = facing.normalize().dot(toVictim.normalize());   // ~1 when the attacker is directly behind
        if (dot < s.getDouble("behind-dot", 0.5)) return;

        double bonus = s.getDouble("damage-multiplier", 0.75) * c.manager().scaleFor(victim);
        e.setDamage(e.getDamage() * (1.0 + bonus));
        c.manager().ignite(p, victim, s.getDouble("ignite-seconds", 2.0));
        c.manager().start(p, id(), s.getDouble("internal-cooldown-seconds", 2.0));
        c.manager().fx().particles(victim.getLocation().add(0, 1, 0), s.getString("particle", "crit"), 15, 0.3);
        c.manager().fx().sound(victim.getLocation(), s.getString("sound", "entity.player.attack.crit"), 0.8f, 1.1f);
    }
}
