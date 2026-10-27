package dev.advancedgear;

import dev.advancedgear.abilities.AbilityManager;
import dev.advancedgear.armor.ArmorSets;
import dev.advancedgear.commands.GearCommand;
import dev.advancedgear.items.ItemRegistry;
import dev.advancedgear.listeners.AbilityListener;
import dev.advancedgear.listeners.CombatListener;
import dev.advancedgear.listeners.ConnectionListener;
import dev.advancedgear.listeners.CraftGuardListener;
import dev.advancedgear.listeners.LootListener;
import dev.advancedgear.listeners.MiningListener;
import dev.advancedgear.listeners.SmithingListener;
import dev.advancedgear.recipes.RecipeManager;
import dev.advancedgear.tiers.TierManager;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class AdvancedGearPlugin extends JavaPlugin {
    /** Namespace of the resource pack assets (assets/advancedgear/...). */
    public static final String ASSET_NAMESPACE = "advancedgear";

    private TierManager tiers;
    private ItemRegistry items;
    private RecipeManager recipes;
    private AbilityManager abilities;
    private ArmorSets armorSets;
    private MiningListener mining;
    private LootListener loot;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        for (String f : new String[]{"items.yml", "recipes.yml", "loot.yml"}) {
            if (!new File(getDataFolder(), f).exists()) saveResource(f, false);
        }

        tiers = new TierManager(this);
        items = new ItemRegistry(this);
        recipes = new RecipeManager(this);
        abilities = new AbilityManager(this);
        armorSets = new ArmorSets(this);
        mining = new MiningListener(this);
        loot = new LootListener(this);

        var pm = getServer().getPluginManager();
        pm.registerEvents(new SmithingListener(this), this);
        pm.registerEvents(new CraftGuardListener(this), this);
        pm.registerEvents(new AbilityListener(this), this);
        pm.registerEvents(new CombatListener(this), this);
        pm.registerEvents(mining, this);
        pm.registerEvents(loot, this);
        pm.registerEvents(new ConnectionListener(this), this);

        GearCommand cmd = new GearCommand(this);
        var command = getCommand("advancedgear");
        if (command != null) {
            command.setExecutor(cmd);
            command.setTabCompleter(cmd);
        }

        loadData();
        getLogger().info("AdvancedGear enabled: " + items.all().size() + " items, " + tiers.all().size() + " tier(s).");
    }

    @Override
    public void onDisable() {
        if (recipes != null) recipes.unregister();
    }

    /** (Re)loads every config file and rebuilds items, recipes and loot. */
    public void loadData() {
        tiers.load();
        items.load(yaml("items.yml"));
        recipes.register(yaml("recipes.yml"));
        loot.load(yaml("loot.yml"));
        abilities.reload();
        mining.rebuild();
    }

    public void reloadAll() {
        reloadConfig();
        recipes.unregister();
        loadData();
    }

    private YamlConfiguration yaml(String name) {
        return YamlConfiguration.loadConfiguration(new File(getDataFolder(), name));
    }

    public void debug(String msg) {
        if (getConfig().getBoolean("debug", false)) getLogger().info("[debug] " + msg);
    }

    /** NamespacedKey namespace used for this plugin's recipes. */
    public String namespace() { return getName().toLowerCase(); }

    public TierManager tiers() { return tiers; }
    public ItemRegistry items() { return items; }
    public RecipeManager recipes() { return recipes; }
    public AbilityManager abilities() { return abilities; }
    public ArmorSets armorSets() { return armorSets; }
}
