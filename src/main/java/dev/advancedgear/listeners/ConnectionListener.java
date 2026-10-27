package dev.advancedgear.listeners;

import dev.advancedgear.AdvancedGearPlugin;
import net.kyori.adventure.text.Component;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.HexFormat;

/** Resource pack prompt and recipe-book discovery on join. */
public final class ConnectionListener implements Listener {
    private final AdvancedGearPlugin plugin;

    public ConnectionListener(AdvancedGearPlugin plugin) { this.plugin = plugin; }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        var p = e.getPlayer();
        if (plugin.getConfig().getBoolean("features.recipes-auto-discover", true)) {
            p.discoverRecipes(plugin.recipes().keys());
        }
        if (!plugin.getConfig().getBoolean("resource-pack.enabled", false)) return;
        String url = plugin.getConfig().getString("resource-pack.url", "");
        String sha1 = plugin.getConfig().getString("resource-pack.sha1", "");
        if (url.isBlank() || sha1.length() != 40) {
            plugin.getLogger().warning("resource-pack.enabled is true but url/sha1 are not set (sha1 must be 40 hex characters).");
            return;
        }
        try {
            p.setResourcePack(url, HexFormat.of().parseHex(sha1), Component.text("AdvancedGear textures"),
                    plugin.getConfig().getBoolean("resource-pack.required", true));
        } catch (IllegalArgumentException ex) {
            plugin.getLogger().warning("resource-pack.sha1 is not valid hex: " + ex.getMessage());
        }
    }
}
