package dev.advancedgear.recipes;

import dev.advancedgear.AdvancedGearPlugin;
import dev.advancedgear.items.ItemDefinition;
import dev.advancedgear.tiers.TierInfo;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.SmithingTransformRecipe;

import java.util.ArrayList;
import java.util.List;

/** Registers smithing upgrade recipes (generated from items.yml) and the crafting recipes of recipes.yml. */
public final class RecipeManager {
    public static final String UPGRADE_PREFIX = "upgrade_";

    private final AdvancedGearPlugin plugin;
    private final List<NamespacedKey> keys = new ArrayList<>();
    private final List<String> summaries = new ArrayList<>();

    public RecipeManager(AdvancedGearPlugin plugin) { this.plugin = plugin; }

    public List<NamespacedKey> keys() { return keys; }
    public List<String> summaries() { return summaries; }

    public void unregister() {
        for (NamespacedKey k : keys) plugin.getServer().removeRecipe(k);
        keys.clear();
        summaries.clear();
    }

    public void register(YamlConfiguration recipesYaml) {
        unregister();
        registerUpgrades();
        registerCrafting(recipesYaml);
        plugin.getLogger().info("Registered " + keys.size() + " custom recipes.");
    }

    // ------------------------------------------------------------- smithing upgrades
    private void registerUpgrades() {
        for (ItemDefinition def : plugin.items().all().values()) {
            if (def.upgradeFrom == null) continue;
            TierInfo tier = plugin.tiers().get(def.tier);
            if (tier == null || tier.templateId() == null || tier.ingotId() == null
                    || !plugin.items().has(tier.templateId()) || !plugin.items().has(tier.ingotId())) {
                plugin.getLogger().warning("Upgrade for '" + def.id + "' skipped: tier '" + def.tier + "' is incomplete.");
                continue;
            }
            Material baseMaterial = baseMaterialOf(def.upgradeFrom);
            if (baseMaterial == null) {
                plugin.getLogger().warning("Upgrade for '" + def.id + "' skipped: bad upgrade-from '" + def.upgradeFrom + "'.");
                continue;
            }
            NamespacedKey key = new NamespacedKey(plugin, UPGRADE_PREFIX + def.id);
            SmithingTransformRecipe recipe = new SmithingTransformRecipe(
                    key,
                    plugin.items().create(def.id, 1),
                    new RecipeChoice.ExactChoice(plugin.items().create(tier.templateId(), 1)),
                    new RecipeChoice.MaterialChoice(baseMaterial),
                    new RecipeChoice.ExactChoice(plugin.items().create(tier.ingotId(), 1)));
            if (plugin.getServer().addRecipe(recipe)) {
                keys.add(key);
                summaries.add(def.id + "  <-  smithing: " + tier.templateId() + " + " + def.upgradeFrom + " + " + tier.ingotId());
            } else {
                plugin.getLogger().warning("Could not register smithing recipe for " + def.id);
            }
        }
    }

    /** upgrade-from is either a custom item id (the item's base material is used) or a vanilla material. */
    public Material baseMaterialOf(String upgradeFrom) {
        if (plugin.items().has(upgradeFrom)) return plugin.items().get(upgradeFrom).material;
        return Material.matchMaterial(upgradeFrom);
    }

    /** True when upgrade-from refers to a custom item (the smithing base must then carry that exact id). */
    public boolean requiresCustomBase(String upgradeFrom) { return plugin.items().has(upgradeFrom); }

    // ------------------------------------------------------------- crafting-table recipes
    private void registerCrafting(YamlConfiguration yaml) {
        ConfigurationSection root = yaml.getConfigurationSection("recipes");
        if (root == null) return;
        for (String id : root.getKeys(false)) {
            ConfigurationSection s = root.getConfigurationSection(id);
            if (s == null) continue;
            try {
                String resultId = s.getString("result", id);
                if (!plugin.items().has(resultId)) throw new IllegalArgumentException("unknown result item '" + resultId + "'");
                var result = plugin.items().create(resultId, Math.max(1, s.getInt("amount", 1)));
                NamespacedKey key = new NamespacedKey(plugin, "craft_" + id);
                Recipe recipe;
                String type = s.getString("type", "shaped").toLowerCase();
                if (type.equals("shapeless")) {
                    ShapelessRecipe r = new ShapelessRecipe(key, result);
                    for (String ing : s.getStringList("ingredients")) {
                        int count = 1;
                        int star = ing.indexOf('*');
                        if (star > 0) { count = Integer.parseInt(ing.substring(star + 1).trim()); ing = ing.substring(0, star).trim(); }
                        for (int i = 0; i < count; i++) r.addIngredient(choice(ing));
                    }
                    recipe = r;
                } else {
                    ShapedRecipe r = new ShapedRecipe(key, result);
                    List<String> shape = s.getStringList("shape");
                    r.shape(shape.toArray(new String[0]));
                    ConfigurationSection ings = s.getConfigurationSection("ingredients");
                    if (ings == null) throw new IllegalArgumentException("missing ingredients");
                    for (String symbol : ings.getKeys(false)) r.setIngredient(symbol.charAt(0), choice(ings.getString(symbol)));
                    recipe = r;
                }
                if (plugin.getServer().addRecipe(recipe)) {
                    keys.add(key);
                    summaries.add(resultId + "  <-  crafting (" + type + ")");
                }
            } catch (RuntimeException ex) {
                plugin.getLogger().warning("recipes.yml: skipping '" + id + "': " + ex.getMessage());
            }
        }
    }

    private RecipeChoice choice(String spec) {
        spec = spec.trim();
        if (spec.toLowerCase().startsWith("custom:")) {
            String cid = spec.substring(7).trim();
            if (!plugin.items().has(cid)) throw new IllegalArgumentException("unknown custom ingredient '" + cid + "'");
            return new RecipeChoice.ExactChoice(plugin.items().create(cid, 1));
        }
        Material m = Material.matchMaterial(spec);
        if (m == null) throw new IllegalArgumentException("unknown material '" + spec + "'");
        return new RecipeChoice.MaterialChoice(m);
    }
}
