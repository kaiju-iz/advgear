package dev.advancedgear.items;

import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.EquipmentSlot;

import java.util.List;

/** One entry of items.yml. */
public final class ItemDefinition {
    public final String id;
    public final Material material;
    public final String tier;
    public final String category;      // material, template, armor, tool, weapon
    public final String family;        // weapon family, may be null
    public final String setId;         // armor set, may be null
    public final EquipmentSlot slot;   // armor slot, may be null
    public final String name;
    public final List<String> lore;
    public final boolean glint;
    public final String upgradeFrom;   // vanilla material or custom item id, may be null
    public final String equipmentModel;
    public final List<String> abilities;
    public final Double armor, toughness, knockbackResistance, damage, attackSpeed, miningEfficiency, reach;
    public final Integer durability;

    public ItemDefinition(String id, ConfigurationSection s) {
        this.id = id;
        Material m = Material.matchMaterial(s.getString("material", "STONE"));
        if (m == null) throw new IllegalArgumentException("unknown material '" + s.getString("material") + "'");
        this.material = m;
        this.tier = s.getString("tier", "none");
        this.category = s.getString("category", "material");
        this.family = s.getString("family");
        this.setId = s.getString("set");
        String slotName = s.getString("slot");
        this.slot = slotName == null ? null : EquipmentSlot.valueOf(slotName.toUpperCase());
        this.name = s.getString("name", id);
        this.lore = s.getStringList("lore");
        this.glint = s.getBoolean("glint", false);
        this.upgradeFrom = s.getString("upgrade-from");
        this.equipmentModel = s.getString("equipment-model");
        this.abilities = s.getStringList("abilities");
        ConfigurationSection st = s.getConfigurationSection("stats");
        this.armor = num(st, "armor");
        this.toughness = num(st, "toughness");
        this.knockbackResistance = num(st, "knockback-resistance");
        this.damage = num(st, "damage");
        this.attackSpeed = num(st, "attack-speed");
        this.miningEfficiency = num(st, "mining-efficiency");
        this.reach = num(st, "reach");
        this.durability = st != null && st.contains("durability") ? st.getInt("durability") : null;
    }

    private static Double num(ConfigurationSection s, String key) {
        return s != null && s.contains(key) ? s.getDouble(key) : null;
    }

    public boolean isArmor() { return "armor".equals(category); }
    public boolean hasAbility(String abilityId) { return abilities.contains(abilityId); }

    public static TextColor parseColor(String hex, TextColor fallback) {
        TextColor c = hex == null ? null : TextColor.fromHexString(hex);
        return c == null ? fallback : c;
    }
}
