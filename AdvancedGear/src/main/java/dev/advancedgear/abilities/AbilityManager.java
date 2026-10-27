package dev.advancedgear.abilities;

import dev.advancedgear.AdvancedGearPlugin;
import dev.advancedgear.items.ItemDefinition;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Boss;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.event.entity.EntityCombustByEntityEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scoreboard.Team;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Ability registry, cooldowns, target rules, damage helpers and player feedback. */
public final class AbilityManager {
    private final AdvancedGearPlugin plugin;
    private final Fx fx;
    private final Map<String, Ability> abilities = new HashMap<>();
    private final Map<UUID, Map<String, Long>> cooldowns = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastFeedback = new ConcurrentHashMap<>();
    private final Set<EntityType> ignored = EnumSet.noneOf(EntityType.class);

    private boolean inAbilityDamage = false;
    private boolean lastDamageLanded = false;

    public AbilityManager(AdvancedGearPlugin plugin) {
        this.plugin = plugin;
        this.fx = new Fx(plugin);
        register(new MagmaSlamAbility());
        register(new CleaveAbility());
        register(new FlameFlurryAbility());
        register(new BackstabAbility());
        register(new ShockwaveAbility());
    }

    private void register(Ability a) { abilities.put(a.id(), a); }
    public Ability get(String id) { return abilities.get(id); }
    public Set<String> ids() { return abilities.keySet(); }
    public Fx fx() { return fx; }

    public void reload() {
        ignored.clear();
        for (String name : plugin.getConfig().getStringList("pvp.ignore-entity-types")) {
            try { ignored.add(EntityType.valueOf(name.toUpperCase())); }
            catch (IllegalArgumentException ex) { plugin.getLogger().warning("pvp.ignore-entity-types: unknown type " + name); }
        }
    }

    // ------------------------------------------------------------ config / world checks
    public ConfigurationSection cfg(String id) {
        ConfigurationSection s = plugin.getConfig().getConfigurationSection("abilities." + id);
        return s != null ? s : new YamlConfiguration();
    }

    public boolean worldAllows(World w) {
        return !plugin.getConfig().getStringList("worlds.disabled").contains(w.getName());
    }

    public boolean allowedIn(World w, String abilityId) {
        if (!plugin.getConfig().getBoolean("features.abilities", true) || !worldAllows(w)) return false;
        ConfigurationSection s = cfg(abilityId);
        return s.getBoolean("enabled", true) && !s.getStringList("disabled-worlds").contains(w.getName());
    }

    // ------------------------------------------------------------ cooldowns
    public boolean ready(Player p, String key) {
        Map<String, Long> m = cooldowns.get(p.getUniqueId());
        Long until = m == null ? null : m.get(key);
        return until == null || until <= System.currentTimeMillis();
    }

    public double remainingSeconds(Player p, String key) {
        Map<String, Long> m = cooldowns.get(p.getUniqueId());
        Long until = m == null ? null : m.get(key);
        return until == null ? 0 : Math.max(0, (until - System.currentTimeMillis()) / 1000.0);
    }

    public void start(Player p, String key, double seconds) {
        if (seconds <= 0) return;
        cooldowns.computeIfAbsent(p.getUniqueId(), k -> new ConcurrentHashMap<>())
                .put(key, System.currentTimeMillis() + (long) (seconds * 1000));
    }

    public void cleanup(UUID id) {
        cooldowns.remove(id);
        lastFeedback.remove(id);
        for (Ability a : abilities.values()) a.cleanup(id);
    }

    // ------------------------------------------------------------ feedback
    public void feedback(Player p, String text, NamedTextColor color) {
        if (!plugin.getConfig().getBoolean("effects.action-bar-feedback", true)) return;
        long now = System.currentTimeMillis();
        Long last = lastFeedback.get(p.getUniqueId());
        if (last != null && now - last < 400) return;      // anti-spam
        lastFeedback.put(p.getUniqueId(), now);
        p.sendActionBar(Component.text(text, color));
    }

    // ------------------------------------------------------------ dispatch
    private AbilityContext ctx(Player p, ItemStack item, ItemDefinition def, String abilityId) {
        return new AbilityContext(plugin, this, p, item, def, cfg(abilityId));
    }

    /** Right-click control. Returns true if an ability handled the click (fired or was on cooldown). */
    public boolean tryActivate(Player p, ItemStack item, ItemDefinition def) {
        for (String id : def.abilities) {
            Ability a = abilities.get(id);
            if (a == null || !a.active() || !allowedIn(p.getWorld(), id)) continue;
            AbilityContext c = ctx(p, item, def, id);
            String label = c.cfg().getString("display-name", id);
            if (!ready(p, id)) {
                feedback(p, label + " ready in " + String.format("%.1f", remainingSeconds(p, id)) + "s", NamedTextColor.GRAY);
                return true;
            }
            if (a.activate(c)) {
                start(p, id, c.cfg().getDouble("cooldown-seconds", 10.0));
                return true;
            }
        }
        return false;
    }

    public void handleAttack(Player attacker, ItemStack item, ItemDefinition def, LivingEntity victim, EntityDamageByEntityEvent e) {
        for (String id : def.abilities) {
            Ability a = abilities.get(id);
            if (a == null || !allowedIn(attacker.getWorld(), id)) continue;
            a.onAttack(ctx(attacker, item, def, id), victim, e);
        }
    }

    // ------------------------------------------------------------ targeting / damage
    public boolean canTarget(Player source, Entity target) {
        if (!(target instanceof LivingEntity)) return false;
        if (target.equals(source) || target.isDead() || target.isInvulnerable()) return false;
        if (ignored.contains(target.getType())) return false;
        if (target instanceof Tameable t && t.isTamed() && t.getOwner() != null
                && t.getOwner().getUniqueId().equals(source.getUniqueId())) return false;
        if (target instanceof Player tp) {
            if (!plugin.getConfig().getBoolean("pvp.abilities-vs-players", true)) return false;
            if (tp.getGameMode() == GameMode.CREATIVE || tp.getGameMode() == GameMode.SPECTATOR) return false;
            if (!plugin.getConfig().getBoolean("pvp.friendly-fire", false)) {
                Team mine = source.getScoreboard().getEntryTeam(source.getName());
                if (mine != null && mine.hasEntry(tp.getName())) return false;
            }
        }
        return true;
    }

    /** Multiplier applied to ability damage depending on the victim (PvP and boss balance). */
    public double scaleFor(LivingEntity victim) {
        double m = 1.0;
        if (victim instanceof Player) m *= plugin.getConfig().getDouble("pvp.damage-multiplier", 0.75);
        if (victim instanceof Boss) m *= plugin.getConfig().getDouble("pvp.boss-damage-multiplier", 0.5);
        return m;
    }

    public boolean inAbilityDamage() { return inAbilityDamage; }
    public void markDamage(boolean landed) { lastDamageLanded = landed; }

    /** Deals ability damage through the normal damage event so protection plugins can cancel it. */
    public boolean dealDamage(Player source, LivingEntity target, double amount) {
        lastDamageLanded = false;
        inAbilityDamage = true;
        try {
            target.setNoDamageTicks(0);
            target.damage(amount * scaleFor(target), source);
        } finally {
            inAbilityDamage = false;
        }
        return lastDamageLanded;
    }

    /** Ignites through EntityCombustByEntityEvent so protection plugins can cancel it. */
    public boolean ignite(Player source, LivingEntity target, double seconds) {
        if (seconds <= 0) return false;
        EntityCombustByEntityEvent ev = new EntityCombustByEntityEvent(source, target, (float) seconds);
        plugin.getServer().getPluginManager().callEvent(ev);
        if (ev.isCancelled()) return false;
        target.setFireTicks((int) (ev.getDuration() * 20));
        return true;
    }
}
