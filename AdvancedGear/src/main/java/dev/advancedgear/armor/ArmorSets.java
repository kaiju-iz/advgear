package dev.advancedgear.armor;

import dev.advancedgear.AdvancedGearPlugin;
import dev.advancedgear.items.ItemDefinition;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

/** Counts worn set pieces and derives the per-piece and full-set bonuses from config.yml (armor-sets). */
public final class ArmorSets {
    public static final int FULL = 4;
    private final AdvancedGearPlugin plugin;

    public ArmorSets(AdvancedGearPlugin plugin) { this.plugin = plugin; }

    public boolean enabled(Player p) {
        return plugin.getConfig().getBoolean("features.armor-sets", true) && plugin.abilities().worldAllows(p.getWorld());
    }

    /** set id -> number of worn pieces (armor slots only). */
    public Map<String, Integer> worn(Player p) {
        Map<String, Integer> out = new HashMap<>();
        for (ItemStack s : p.getInventory().getArmorContents()) {
            ItemDefinition d = plugin.items().defOf(s);
            if (d != null && d.setId != null) out.merge(d.setId, 1, Integer::sum);
        }
        return out;
    }

    public ConfigurationSection cfg(String setId) {
        return plugin.getConfig().getConfigurationSection("armor-sets." + setId);
    }

    /** Total fire/lava/magma damage reduction (0..max) from all worn sets. */
    public double fireReduction(Player p) {
        double total = 0, max = 0;
        for (Map.Entry<String, Integer> en : worn(p).entrySet()) {
            ConfigurationSection s = cfg(en.getKey());
            if (s == null) continue;
            int n = en.getValue();
            total += n * s.getDouble("fire-reduction-per-piece", 0.0);
            if (n >= FULL) total += s.getDouble("full-set-fire-bonus", 0.0);
            max = Math.max(max, s.getDouble("max-fire-reduction", 0.8));
        }
        return Math.min(total, max);
    }

    public double burnDurationReduction(Player p) {
        double total = 0;
        for (Map.Entry<String, Integer> en : worn(p).entrySet()) {
            ConfigurationSection s = cfg(en.getKey());
            if (s != null) total += en.getValue() * s.getDouble("burn-duration-reduction-per-piece", 0.0);
        }
        return Math.min(total, 0.9);
    }
}
