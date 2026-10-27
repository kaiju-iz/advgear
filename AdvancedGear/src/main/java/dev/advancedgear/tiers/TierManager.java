package dev.advancedgear.tiers;

import dev.advancedgear.AdvancedGearPlugin;
import dev.advancedgear.items.ItemDefinition;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.configuration.ConfigurationSection;

import java.util.LinkedHashMap;
import java.util.Map;

public final class TierManager {
    private final AdvancedGearPlugin plugin;
    private final Map<String, TierInfo> tiers = new LinkedHashMap<>();

    public TierManager(AdvancedGearPlugin plugin) { this.plugin = plugin; }

    public void load() {
        tiers.clear();
        ConfigurationSection root = plugin.getConfig().getConfigurationSection("tiers");
        if (root == null) return;
        for (String id : root.getKeys(false)) {
            ConfigurationSection s = root.getConfigurationSection(id);
            if (s == null) continue;
            tiers.put(id, new TierInfo(id, s.getString("display-name", id),
                    ItemDefinition.parseColor(s.getString("color"), NamedTextColor.WHITE),
                    s.getString("previous", "netherite"),
                    s.getString("template"), s.getString("ingot")));
        }
    }

    public TierInfo get(String id) { return id == null ? null : tiers.get(id.toLowerCase()); }
    public Map<String, TierInfo> all() { return tiers; }
}
