package dev.advancedgear.listeners;

import dev.advancedgear.AdvancedGearPlugin;
import dev.advancedgear.armor.ArmorSets;
import dev.advancedgear.items.ItemDefinition;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

/** Passive weapon hooks, armor-set bonuses and fire protection. */
public final class CombatListener implements Listener {
    private final AdvancedGearPlugin plugin;

    public CombatListener(AdvancedGearPlugin plugin) { this.plugin = plugin; }

    /** Records whether ability-dealt damage actually landed (not cancelled by a protection plugin). */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onDamageResult(EntityDamageByEntityEvent e) {
        if (plugin.abilities().inAbilityDamage()) plugin.abilities().markDamage(!e.isCancelled());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMelee(EntityDamageByEntityEvent e) {
        if (plugin.abilities().inAbilityDamage()) return;              // never chain off ability damage
        if (e.getCause() != DamageCause.ENTITY_ATTACK) return;
        if (!(e.getEntity() instanceof LivingEntity victim)) return;

        if (e.getDamager() instanceof Player attacker) {
            if (plugin.abilities().worldAllows(attacker.getWorld())) {
                ItemStack hand = attacker.getInventory().getItemInMainHand();
                ItemDefinition def = plugin.items().defOf(hand);
                if (def != null && !def.abilities.isEmpty()) {
                    plugin.abilities().handleAttack(attacker, hand, def, victim, e);
                }
                if (plugin.armorSets().enabled(attacker)) igniteOnHit(attacker, victim);
            }
        }
        if (victim instanceof Player defender && e.getDamager() instanceof LivingEntity attackerEntity
                && plugin.armorSets().enabled(defender)) {
            retaliate(defender, attackerEntity);
        }
    }

    private void igniteOnHit(Player attacker, LivingEntity victim) {
        for (Map.Entry<String, Integer> en : plugin.armorSets().worn(attacker).entrySet()) {
            if (en.getValue() < ArmorSets.FULL) continue;
            ConfigurationSection s = plugin.armorSets().cfg(en.getKey());
            ConfigurationSection full = s == null ? null : s.getConfigurationSection("full-set");
            if (full == null) continue;
            double seconds = full.getDouble("ignite-on-hit-seconds", 0);
            String key = "set_ignite_" + en.getKey();
            if (seconds <= 0 || !plugin.abilities().ready(attacker, key) || !plugin.abilities().canTarget(attacker, victim)) continue;
            if (plugin.abilities().ignite(attacker, victim, seconds)) {
                plugin.abilities().start(attacker, key, full.getDouble("ignite-on-hit-cooldown-seconds", 1.0));
            }
        }
    }

    private void retaliate(Player defender, LivingEntity attacker) {
        for (Map.Entry<String, Integer> en : plugin.armorSets().worn(defender).entrySet()) {
            if (en.getValue() < ArmorSets.FULL) continue;
            ConfigurationSection s = plugin.armorSets().cfg(en.getKey());
            ConfigurationSection r = s == null ? null : s.getConfigurationSection("full-set.retaliation");
            if (r == null) continue;
            String key = "set_retaliation_" + en.getKey();
            if (!plugin.abilities().ready(defender, key) || !plugin.abilities().canTarget(defender, attacker)) continue;
            if (Math.random() >= r.getDouble("chance", 0.0)) continue;
            if (plugin.abilities().ignite(defender, attacker, r.getDouble("ignite-seconds", 3.0))) {
                plugin.abilities().start(defender, key, r.getDouble("cooldown-seconds", 6.0));
                plugin.abilities().fx().particles(attacker.getLocation().add(0, 1, 0), r.getString("particle", "flame"), 14, 0.4);
                plugin.abilities().fx().sound(attacker.getLocation(), r.getString("sound", "item.firecharge.use"), 0.7f, 1.0f);
            }
        }
    }

    /** Fire, lava and magma damage reduction from worn set pieces. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFireDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        DamageCause c = e.getCause();
        if (c != DamageCause.FIRE && c != DamageCause.FIRE_TICK && c != DamageCause.LAVA && c != DamageCause.HOT_FLOOR) return;
        if (!plugin.armorSets().enabled(p)) return;
        double reduction = plugin.armorSets().fireReduction(p);
        if (reduction > 0) e.setDamage(e.getDamage() * (1.0 - reduction));
    }

    /** Shorter burn time while wearing set pieces. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCombust(EntityCombustEvent e) {
        if (!(e.getEntity() instanceof Player p) || !plugin.armorSets().enabled(p)) return;
        double reduction = plugin.armorSets().burnDurationReduction(p);
        if (reduction > 0) e.setDuration((float) (e.getDuration() * (1.0 - reduction)));
    }
}
