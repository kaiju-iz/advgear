package dev.advancedgear.listeners;

import dev.advancedgear.AdvancedGearPlugin;
import org.bukkit.Keyed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;

/**
 * Custom items may only be used in this plugin's own recipes. This stops e.g. an Infernal Ingot
 * (a Netherite Scrap underneath) being crafted into a Netherite Ingot, or custom gear being
 * destroyed by vanilla repair-crafting.
 */
public final class CraftGuardListener implements Listener {
    private final AdvancedGearPlugin plugin;

    public CraftGuardListener(AdvancedGearPlugin plugin) { this.plugin = plugin; }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPrepare(PrepareItemCraftEvent e) {
        Recipe recipe = e.getRecipe();
        if (recipe instanceof Keyed k && k.getKey().getNamespace().equals(plugin.namespace())) return;
        for (ItemStack stack : e.getInventory().getMatrix()) {
            if (plugin.items().idOf(stack) != null) {
                e.getInventory().setResult(null);
                return;
            }
        }
    }
}
