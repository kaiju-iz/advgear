package dev.advancedgear.listeners;

import dev.advancedgear.AdvancedGearPlugin;
import dev.advancedgear.items.ItemDefinition;
import dev.advancedgear.recipes.RecipeManager;
import dev.advancedgear.tiers.TierInfo;
import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.PrepareSmithingEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.SmithingInventory;
import org.bukkit.inventory.meta.ArmorMeta;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.Repairable;

import java.util.Map;

/** Validates tier upgrades in the smithing table and carries over enchantments, name, trim and durability. */
public final class SmithingListener implements Listener {
    private final AdvancedGearPlugin plugin;

    public SmithingListener(AdvancedGearPlugin plugin) { this.plugin = plugin; }

    private boolean isOurUpgrade(Recipe recipe) {
        return recipe instanceof Keyed k
                && k.getKey().getNamespace().equals(plugin.namespace())
                && k.getKey().getKey().startsWith(RecipeManager.UPGRADE_PREFIX);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPrepare(PrepareSmithingEvent e) {
        SmithingInventory inv = e.getInventory();
        ItemStack template = inv.getInputTemplate();
        ItemStack base = inv.getInputEquipment();
        ItemStack addition = inv.getInputMineral();
        Recipe recipe = inv.getRecipe();
        boolean anyCustom = plugin.items().idOf(template) != null
                || plugin.items().idOf(base) != null
                || plugin.items().idOf(addition) != null;

        if (!isOurUpgrade(recipe)) {
            // Custom items must never be accepted by vanilla smithing recipes (e.g. Netherite upgrade or trims).
            if (anyCustom) e.setResult(null);
            return;
        }

        String defId = ((Keyed) recipe).getKey().getKey().substring(RecipeManager.UPGRADE_PREFIX.length());
        ItemDefinition def = plugin.items().get(defId);
        if (def == null) { e.setResult(null); return; }
        TierInfo tier = plugin.tiers().get(def.tier);
        if (tier == null) { e.setResult(null); return; }

        // Strict validation: exact template, exact ingot, and a correct base.
        if (!tier.templateId().equals(plugin.items().idOf(template))
                || !tier.ingotId().equals(plugin.items().idOf(addition))) {
            e.setResult(null);
            return;
        }
        String baseId = plugin.items().idOf(base);
        if (plugin.recipes().requiresCustomBase(def.upgradeFrom)) {
            if (!def.upgradeFrom.equals(baseId)) { e.setResult(null); return; }   // previous-tier item required
        } else if (baseId != null) {
            e.setResult(null);                                                    // only plain vanilla items upgrade from vanilla
            return;
        }
        if (base == null || base.getType() != plugin.recipes().baseMaterialOf(def.upgradeFrom)) { e.setResult(null); return; }

        e.setResult(transfer(plugin.items().create(def.id, 1), base));
    }

    /** Copies supported metadata from the old item onto the freshly built upgraded item. */
    private ItemStack transfer(ItemStack result, ItemStack base) {
        var cfg = plugin.getConfig();
        ItemMeta rm = result.getItemMeta();
        ItemMeta bm = base.getItemMeta();
        if (rm == null || bm == null) return result;

        if (cfg.getBoolean("smithing.preserve-enchantments", true)) {
            for (Map.Entry<Enchantment, Integer> en : bm.getEnchants().entrySet()) {
                rm.addEnchant(en.getKey(), en.getValue(), true);
            }
        }
        if (cfg.getBoolean("smithing.preserve-name", true) && bm.hasDisplayName()) {
            rm.displayName(bm.displayName());
        }
        if (cfg.getBoolean("smithing.preserve-trim", true) && bm instanceof ArmorMeta bam && rm instanceof ArmorMeta ram && bam.hasTrim()) {
            ram.setTrim(bam.getTrim());
        }
        if (bm instanceof Repairable br && rm instanceof Repairable rr) rr.setRepairCost(br.getRepairCost());
        if (cfg.getBoolean("smithing.preserve-durability-ratio", true)
                && bm instanceof Damageable bd && rm instanceof Damageable rd) {
            int baseMax = bd.hasMaxDamage() ? bd.getMaxDamage() : base.getType().getMaxDurability();
            int resMax = rd.hasMaxDamage() ? rd.getMaxDamage() : result.getType().getMaxDurability();
            if (baseMax > 0 && resMax > 0) {
                int dmg = (int) Math.round(bd.getDamage() * (resMax / (double) baseMax));
                rd.setDamage(Math.min(Math.max(0, dmg), resMax - 1));
            }
        }
        result.setItemMeta(rm);
        return result;
    }

    /** Reusable templates: hand the template back after a successful (non-cancelled) upgrade. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onResultTaken(InventoryClickEvent e) {
        if (plugin.getConfig().getBoolean("smithing.consume-template", true)) return;
        if (!(e.getInventory() instanceof SmithingInventory inv)) return;
        if (e.getClickedInventory() != inv || e.getRawSlot() != 3) return;
        ItemStack taken = e.getCurrentItem();
        ItemDefinition def = plugin.items().defOf(taken);
        if (def == null || def.upgradeFrom == null) return;

        ItemStack templateBefore = inv.getInputTemplate();
        if (templateBefore == null || templateBefore.getType() == Material.AIR) return;
        ItemStack snapshot = templateBefore.clone();
        int before = snapshot.getAmount();

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            ItemStack now = inv.getInputTemplate();
            int amountNow = (now == null || now.getType().isAir()) ? 0 : now.getAmount();
            if (amountNow < before) {
                ItemStack restore = snapshot.clone();
                restore.setAmount(before);
                inv.setInputTemplate(restore);
            }
        });
    }
}
