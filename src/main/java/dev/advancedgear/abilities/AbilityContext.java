package dev.advancedgear.abilities;

import dev.advancedgear.AdvancedGearPlugin;
import dev.advancedgear.items.ItemDefinition;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public record AbilityContext(AdvancedGearPlugin plugin, AbilityManager manager, Player player,
                             ItemStack item, ItemDefinition def, ConfigurationSection cfg) { }
