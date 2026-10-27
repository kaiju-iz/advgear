package dev.advancedgear.items;

import dev.advancedgear.AdvancedGearPlugin;
import dev.advancedgear.tiers.TierInfo;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.EquippableComponent;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Builds and recognises every custom item. Identity is a PDC string, never the display name. */
public final class ItemRegistry {
    private final AdvancedGearPlugin plugin;
    private final NamespacedKey idKey;
    private final Map<String, ItemDefinition> defs = new LinkedHashMap<>();

    public ItemRegistry(AdvancedGearPlugin plugin) {
        this.plugin = plugin;
        this.idKey = new NamespacedKey(plugin, "item_id");
    }

    public void load(YamlConfiguration yaml) {
        defs.clear();
        ConfigurationSection root = yaml.getConfigurationSection("items");
        if (root == null) { plugin.getLogger().warning("items.yml has no 'items' section."); return; }
        for (String id : root.getKeys(false)) {
            ConfigurationSection s = root.getConfigurationSection(id);
            if (s == null) continue;
            try {
                defs.put(id, new ItemDefinition(id, s));
            } catch (RuntimeException ex) {
                plugin.getLogger().warning("items.yml: skipping '" + id + "': " + ex.getMessage());
            }
        }
    }

    public Map<String, ItemDefinition> all() { return defs; }
    public boolean has(String id) { return id != null && defs.containsKey(id); }
    public ItemDefinition get(String id) { return id == null ? null : defs.get(id); }

    public String idOf(ItemStack stack) {
        if (stack == null || stack.getType().isAir() || !stack.hasItemMeta()) return null;
        String id = stack.getItemMeta().getPersistentDataContainer().get(idKey, PersistentDataType.STRING);
        return id != null && defs.containsKey(id) ? id : null;
    }

    public ItemDefinition defOf(ItemStack stack) { return get(idOf(stack)); }

    public ItemStack create(String id, int amount) {
        ItemDefinition d = defs.get(id);
        if (d == null) throw new IllegalArgumentException("Unknown item: " + id);
        TierInfo tier = plugin.tiers().get(d.tier);
        TextColor color = tier != null ? tier.color() : NamedTextColor.WHITE;

        ItemStack item = new ItemStack(d.material, Math.max(1, amount));
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(d.name, color).decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        for (String line : d.lore) {
            lore.add(Component.text(line, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        }
        if (tier != null) {
            lore.add(Component.empty());
            lore.add(Component.text(tier.displayName() + " Tier", color).decoration(TextDecoration.ITALIC, false));
        }
        meta.lore(lore);

        meta.getPersistentDataContainer().set(idKey, PersistentDataType.STRING, d.id);
        meta.setItemModel(new NamespacedKey(AdvancedGearPlugin.ASSET_NAMESPACE, d.id));
        if (d.glint) meta.setEnchantmentGlintOverride(true);

        // Custom attribute modifiers REPLACE vanilla defaults, so everything is declared explicitly.
        EquipmentSlotGroup group = d.isArmor() ? groupOf(d) : EquipmentSlotGroup.MAINHAND;
        if (d.armor != null) addMod(meta, Attribute.ARMOR, d, "armor", d.armor, group);
        if (d.toughness != null) addMod(meta, Attribute.ARMOR_TOUGHNESS, d, "toughness", d.toughness, group);
        if (d.knockbackResistance != null) addMod(meta, Attribute.KNOCKBACK_RESISTANCE, d, "kbres", d.knockbackResistance, group);
        if (d.damage != null) addMod(meta, Attribute.ATTACK_DAMAGE, d, "damage", d.damage - 1.0, group);
        if (d.attackSpeed != null) addMod(meta, Attribute.ATTACK_SPEED, d, "speed", d.attackSpeed - 4.0, group);
        if (d.miningEfficiency != null) addMod(meta, Attribute.MINING_EFFICIENCY, d, "mining", d.miningEfficiency, group);
        if (d.reach != null) addMod(meta, Attribute.ENTITY_INTERACTION_RANGE, d, "reach", d.reach, group);

        if (d.durability != null && meta instanceof Damageable dmg) dmg.setMaxDamage(d.durability);

        if (d.isArmor() && d.slot != null && d.equipmentModel != null) {
            EquippableComponent eq = meta.getEquippable();
            eq.setSlot(d.slot);
            eq.setModel(new NamespacedKey(AdvancedGearPlugin.ASSET_NAMESPACE, d.equipmentModel));
            meta.setEquippable(eq);
        }
        item.setItemMeta(meta);
        return item;
    }

    private void addMod(ItemMeta meta, Attribute attr, ItemDefinition d, String tag, double value, EquipmentSlotGroup group) {
        meta.addAttributeModifier(attr, new AttributeModifier(
                new NamespacedKey(plugin, d.id + "_" + tag), value, AttributeModifier.Operation.ADD_NUMBER, group));
    }

    private static EquipmentSlotGroup groupOf(ItemDefinition d) {
        if (d.slot == null) return EquipmentSlotGroup.ARMOR;
        return switch (d.slot) {
            case HEAD -> EquipmentSlotGroup.HEAD;
            case CHEST -> EquipmentSlotGroup.CHEST;
            case LEGS -> EquipmentSlotGroup.LEGS;
            case FEET -> EquipmentSlotGroup.FEET;
            default -> EquipmentSlotGroup.ARMOR;
        };
    }
}
