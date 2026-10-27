package dev.advancedgear.commands;

import dev.advancedgear.AdvancedGearPlugin;
import dev.advancedgear.items.ItemDefinition;
import dev.advancedgear.tiers.TierInfo;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** /advancedgear give | template | reload | inspect | recipes | version */
public final class GearCommand implements CommandExecutor, TabCompleter {
    private static final List<String> SUBS = List.of("give", "template", "reload", "inspect", "recipes", "version");
    private final AdvancedGearPlugin plugin;

    public GearCommand(AdvancedGearPlugin plugin) { this.plugin = plugin; }

    private void msg(CommandSender s, String text, NamedTextColor color) { s.sendMessage(Component.text(text, color)); }
    private void err(CommandSender s, String text) { msg(s, text, NamedTextColor.RED); }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("advancedgear.admin")) { err(sender, "You don't have permission."); return true; }
        if (args.length == 0) { err(sender, "Usage: /" + label + " <" + String.join("|", SUBS) + ">"); return true; }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "give" -> give(sender, args);
            case "template" -> template(sender, args);
            case "reload" -> {
                plugin.reloadAll();
                msg(sender, "AdvancedGear reloaded: " + plugin.items().all().size() + " items, "
                        + plugin.recipes().keys().size() + " recipes.", NamedTextColor.GREEN);
            }
            case "inspect" -> inspect(sender);
            case "recipes" -> {
                msg(sender, "Custom recipes (" + plugin.recipes().summaries().size() + "):", NamedTextColor.GOLD);
                for (String line : plugin.recipes().summaries()) msg(sender, " - " + line, NamedTextColor.GRAY);
            }
            case "version" -> {
                msg(sender, "AdvancedGear " + plugin.getPluginMeta().getVersion(), NamedTextColor.GOLD);
                msg(sender, "Server: " + Bukkit.getName() + " " + Bukkit.getMinecraftVersion() + " (" + Bukkit.getVersion() + ")", NamedTextColor.GRAY);
                msg(sender, "Java: " + System.getProperty("java.version"), NamedTextColor.GRAY);
            }
            default -> err(sender, "Unknown subcommand. Use: " + String.join(", ", SUBS));
        }
        return true;
    }

    private Integer amount(CommandSender s, String[] args, int index) {
        if (args.length <= index) return 1;
        try {
            int n = Integer.parseInt(args[index]);
            if (n < 1 || n > 64) { err(s, "Amount must be between 1 and 64."); return null; }
            return n;
        } catch (NumberFormatException ex) { err(s, "'" + args[index] + "' is not a number."); return null; }
    }

    private Player target(CommandSender s, String name) {
        Player p = Bukkit.getPlayerExact(name);
        if (p == null) err(s, "Player '" + name + "' is not online.");
        return p;
    }

    private void give(CommandSender s, String[] args) {
        if (args.length < 3) { err(s, "Usage: /advancedgear give <player> <item> [amount]"); return; }
        Player p = target(s, args[1]);
        if (p == null) return;
        String id = args[2].toLowerCase(Locale.ROOT);
        if (!plugin.items().has(id)) { err(s, "Unknown item '" + id + "'. Use tab completion to list items."); return; }
        Integer n = amount(s, args, 3);
        if (n == null) return;
        deliver(p, plugin.items().create(id, n));
        msg(s, "Gave " + n + "x " + id + " to " + p.getName() + ".", NamedTextColor.GREEN);
    }

    private void template(CommandSender s, String[] args) {
        if (args.length < 3) { err(s, "Usage: /advancedgear template <player> <tier> [amount]"); return; }
        Player p = target(s, args[1]);
        if (p == null) return;
        TierInfo tier = plugin.tiers().get(args[2]);
        if (tier == null || !plugin.items().has(tier.templateId())) {
            err(s, "Unknown tier '" + args[2] + "'. Tiers: " + String.join(", ", plugin.tiers().all().keySet())); return;
        }
        Integer n = amount(s, args, 3);
        if (n == null) return;
        deliver(p, plugin.items().create(tier.templateId(), n));
        msg(s, "Gave " + n + "x " + tier.displayName() + " Upgrade Template to " + p.getName() + ".", NamedTextColor.GREEN);
    }

    private void deliver(Player p, ItemStack item) {
        p.getInventory().addItem(item).values().forEach(left -> p.getWorld().dropItemNaturally(p.getLocation(), left));
    }

    private void inspect(CommandSender s) {
        if (!(s instanceof Player p)) { err(s, "Only players can inspect an item in hand."); return; }
        ItemStack hand = p.getInventory().getItemInMainHand();
        ItemDefinition d = plugin.items().defOf(hand);
        if (d == null) { err(s, "The item in your main hand is not AdvancedGear equipment."); return; }
        msg(s, "Item id: " + d.id + "   tier: " + d.tier + "   category: " + d.category
                + (d.family != null ? "   family: " + d.family : ""), NamedTextColor.GOLD);
        msg(s, "Base material: " + d.material.name() + (d.upgradeFrom != null ? "   upgrades from: " + d.upgradeFrom : ""), NamedTextColor.GRAY);
        if (d.abilities.isEmpty()) msg(s, "Abilities: none", NamedTextColor.GRAY);
        for (String a : d.abilities) {
            double cd = plugin.abilities().remainingSeconds(p, a);
            boolean on = plugin.abilities().allowedIn(p.getWorld(), a);
            msg(s, "Ability " + a + ": " + (on ? "enabled" : "DISABLED here")
                    + (cd > 0 ? " (cooldown " + String.format("%.1f", cd) + "s)" : " (ready)"), NamedTextColor.GRAY);
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (!sender.hasPermission("advancedgear.admin")) return out;
        if (args.length == 1) out.addAll(SUBS);
        else if (args.length == 2 && (args[0].equalsIgnoreCase("give") || args[0].equalsIgnoreCase("template"))) {
            Bukkit.getOnlinePlayers().forEach(p -> out.add(p.getName()));
        } else if (args.length == 3 && args[0].equalsIgnoreCase("give")) out.addAll(plugin.items().all().keySet());
        else if (args.length == 3 && args[0].equalsIgnoreCase("template")) out.addAll(plugin.tiers().all().keySet());
        else if (args.length == 4 && (args[0].equalsIgnoreCase("give") || args[0].equalsIgnoreCase("template"))) out.addAll(List.of("1", "16", "64"));
        String last = args[args.length - 1].toLowerCase(Locale.ROOT);
        return out.stream().filter(x -> x.toLowerCase(Locale.ROOT).startsWith(last)).toList();
    }
}
