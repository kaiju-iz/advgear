package dev.advancedgear.listeners;

import dev.advancedgear.AdvancedGearPlugin;
import dev.advancedgear.items.ItemDefinition;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/** Active ability trigger (right-click, optionally while sneaking). */
public final class AbilityListener implements Listener {
    private final AdvancedGearPlugin plugin;

    public AbilityListener(AdvancedGearPlugin plugin) { this.plugin = plugin; }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (e.useItemInHand() == Event.Result.DENY) return;
        if (!plugin.getConfig().getBoolean("features.abilities", true)) return;

        Player p = e.getPlayer();
        String control = plugin.getConfig().getString("controls.active-ability", "RIGHT_CLICK");
        if (control.equalsIgnoreCase("SNEAK_RIGHT_CLICK") && !p.isSneaking()) return;
        // Don't hijack interactive blocks (doors, chests...) unless the player is sneaking.
        if (e.getClickedBlock() != null && e.getClickedBlock().getType().isInteractable() && !p.isSneaking()) return;

        ItemStack item = p.getInventory().getItemInMainHand();
        ItemDefinition def = plugin.items().defOf(item);
        if (def == null || def.abilities.isEmpty()) return;
        if (plugin.abilities().tryActivate(p, item, def)) e.setCancelled(true);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        plugin.abilities().cleanup(e.getPlayer().getUniqueId());
    }
}
