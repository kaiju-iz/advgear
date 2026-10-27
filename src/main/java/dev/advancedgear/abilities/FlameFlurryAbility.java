package dev.advancedgear.abilities;

import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Ember Dagger (active): the next few hits within a short window deal bonus damage and ignite. */
public final class FlameFlurryAbility implements Ability {
    private record State(int hitsLeft, long expires) { }
    private final Map<UUID, State> empowered = new ConcurrentHashMap<>();

    @Override public String id() { return "flame_flurry"; }
    @Override public boolean active() { return true; }

    @Override
    public boolean activate(AbilityContext c) {
        Player p = c.player();
        ConfigurationSection s = c.cfg();
        int hits = Math.max(1, s.getInt("hits", 3));
        long expires = System.currentTimeMillis() + (long) (s.getDouble("duration-seconds", 6.0) * 1000);
        empowered.put(p.getUniqueId(), new State(hits, expires));
        c.manager().fx().particles(p.getLocation().add(0, 1, 0), s.getString("particle", "flame"), 25, 0.5);
        c.manager().fx().sound(p.getLocation(), s.getString("sound", "item.firecharge.use"), 0.8f, 1.2f);
        c.manager().feedback(p, s.getString("display-name", "Flame Flurry") + ": next " + hits + " hits empowered", NamedTextColor.GOLD);
        return true;
    }

    @Override
    public void onAttack(AbilityContext c, LivingEntity victim, EntityDamageByEntityEvent e) {
        Player p = c.player();
        State st = empowered.get(p.getUniqueId());
        if (st == null) return;
        if (st.expires() < System.currentTimeMillis()) { empowered.remove(p.getUniqueId()); return; }
        if (!c.manager().canTarget(p, victim)) return;

        ConfigurationSection s = c.cfg();
        e.setDamage(e.getDamage() + s.getDouble("bonus-damage", 2.5) * c.manager().scaleFor(victim));
        c.manager().ignite(p, victim, s.getDouble("ignite-seconds", 3.0));
        c.manager().fx().particles(victim.getLocation().add(0, 1, 0), s.getString("particle", "flame"), 12, 0.3);

        if (st.hitsLeft() <= 1) empowered.remove(p.getUniqueId());
        else empowered.put(p.getUniqueId(), new State(st.hitsLeft() - 1, st.expires()));
    }

    @Override public void cleanup(UUID player) { empowered.remove(player); }
}
