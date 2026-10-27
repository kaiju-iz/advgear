package dev.advancedgear.listeners;

import dev.advancedgear.AdvancedGearPlugin;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.world.LootGenerateEvent;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/** Material and template acquisition from mobs and generated structure chests (loot.yml). */
public final class LootListener implements Listener {
    private final AdvancedGearPlugin plugin;
    private final NamespacedKey noLootKey;
    private YamlConfiguration loot = new YamlConfiguration();

    public LootListener(AdvancedGearPlugin plugin) {
        this.plugin = plugin;
        this.noLootKey = new NamespacedKey(plugin, "no_loot");
    }

    public void load(YamlConfiguration yaml) { this.loot = yaml; }

    /** Spawner and spawn-egg mobs never drop custom materials (anti-farming). */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSpawn(CreatureSpawnEvent e) {
        switch (e.getSpawnReason()) {
            case SPAWNER, SPAWNER_EGG, DISPENSE_EGG ->
                    e.getEntity().getPersistentDataContainer().set(noLootKey, PersistentDataType.BYTE, (byte) 1);
            default -> { }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDeath(EntityDeathEvent e) {
        if (!plugin.getConfig().getBoolean("features.loot", true)) return;
        LivingEntity dead = e.getEntity();
        Player killer = dead.getKiller();
        if (killer == null) return;
        if (dead.getPersistentDataContainer().has(noLootKey, PersistentDataType.BYTE)) return;

        ConfigurationSection drops = loot.getConfigurationSection("mob-drops");
        if (drops == null) return;
        List<Map<?, ?>> entries = drops.getMapList(dead.getType().name());
        if (entries.isEmpty()) return;

        int looting = killer.getInventory().getItemInMainHand().getEnchantmentLevel(Enchantment.LOOTING);
        for (Map<?, ?> en : entries) {
            String itemId = String.valueOf(en.get("item"));
            if (!plugin.items().has(itemId)) continue;
            double chance = num(en.get("chance"), 0.0) + looting * num(en.get("looting-bonus-per-level"), 0.0);
            if (ThreadLocalRandom.current().nextDouble() >= chance) continue;
            e.getDrops().add(plugin.items().create(itemId, roll((int) num(en.get("min"), 1), (int) num(en.get("max"), 1))));
        }
    }

    /** Adds templates/materials to generated chests of the configured vanilla loot tables. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onLoot(LootGenerateEvent e) {
        if (!plugin.getConfig().getBoolean("features.loot", true)) return;
        ConfigurationSection chests = loot.getConfigurationSection("chest-loot");
        if (chests == null) return;
        List<Map<?, ?>> entries = chests.getMapList(e.getLootTable().getKey().toString());
        for (Map<?, ?> en : entries) {
            String itemId = String.valueOf(en.get("item"));
            if (!plugin.items().has(itemId)) continue;
            if (ThreadLocalRandom.current().nextDouble() >= num(en.get("chance"), 0.0)) continue;
            e.getLoot().add(plugin.items().create(itemId, roll((int) num(en.get("min"), 1), (int) num(en.get("max"), 1))));
        }
    }

    private static double num(Object o, double def) { return o instanceof Number n ? n.doubleValue() : def; }
    private static int roll(int min, int max) {
        if (max <= min) return Math.max(1, min);
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }
}
