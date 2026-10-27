package dev.advancedgear.listeners;

import dev.advancedgear.AdvancedGearPlugin;
import dev.advancedgear.items.ItemDefinition;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;

import java.util.EnumMap;
import java.util.Iterator;
import java.util.Map;

/** Magma Touch: tools with the ability smelt selected raw-ore drops. Runs after the block break was allowed. */
public final class MiningListener implements Listener {
    private final AdvancedGearPlugin plugin;
    private final Map<Material, Material> smelt = new EnumMap<>(Material.class);

    public MiningListener(AdvancedGearPlugin plugin) { this.plugin = plugin; }

    /** Reads the furnace recipes once (call on enable/reload). */
    public void rebuild() {
        smelt.clear();
        Iterator<Recipe> it = Bukkit.recipeIterator();
        while (it.hasNext()) {
            if (it.next() instanceof FurnaceRecipe fr && fr.getInputChoice() instanceof RecipeChoice.MaterialChoice mc) {
                for (Material in : mc.getChoices()) smelt.putIfAbsent(in, fr.getResult().getType());
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDrops(BlockDropItemEvent e) {
        ItemStack tool = e.getPlayer().getInventory().getItemInMainHand();
        ItemDefinition def = plugin.items().defOf(tool);
        if (def == null || !def.hasAbility("magma_touch")) return;
        if (!plugin.abilities().allowedIn(e.getPlayer().getWorld(), "magma_touch")) return;

        ConfigurationSection s = plugin.abilities().cfg("magma_touch");
        boolean any = false;
        for (Item drop : e.getItems()) {
            ItemStack stack = drop.getItemStack();
            if (!s.getStringList("allowed-drops").contains(stack.getType().name())) continue;
            Material out = smelt.get(stack.getType());
            if (out == null) continue;
            drop.setItemStack(new ItemStack(out, stack.getAmount()));
            any = true;
        }
        if (any) {
            var at = e.getBlock().getLocation().add(0.5, 0.5, 0.5);
            plugin.abilities().fx().particles(at, s.getString("particle", "flame"), 6, 0.25);
            plugin.abilities().fx().sound(at, s.getString("sound", "block.fire.extinguish"), 0.4f, 1.4f);
        }
    }
}
