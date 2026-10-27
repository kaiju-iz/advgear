package dev.advancedgear.abilities;

import dev.advancedgear.AdvancedGearPlugin;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.World;

/** Config-driven particles and sounds with a global particle cap. */
public final class Fx {
    private final AdvancedGearPlugin plugin;

    public Fx(AdvancedGearPlugin plugin) { this.plugin = plugin; }

    private int cap() { return Math.max(1, plugin.getConfig().getInt("effects.max-particles-per-effect", 60)); }

    public void particles(Location loc, String name, int count, double spread) {
        if (!plugin.getConfig().getBoolean("effects.particles", true) || name == null || loc.getWorld() == null) return;
        NamespacedKey key = NamespacedKey.fromString(name.toLowerCase());
        Particle p = key == null ? null : Registry.PARTICLE_TYPE.get(key);
        if (p == null) { plugin.debug("Unknown particle: " + name); return; }
        try {
            loc.getWorld().spawnParticle(p, loc, Math.min(count, cap()), spread, spread * 0.4, spread, 0.02);
        } catch (RuntimeException ex) {
            plugin.debug("Particle failed (" + name + "): " + ex.getMessage());
        }
    }

    public void ring(Location center, String particle, double radius, int points) {
        if (!plugin.getConfig().getBoolean("effects.particles", true)) return;
        World w = center.getWorld();
        if (w == null) return;
        int n = Math.min(points, cap());
        for (int i = 0; i < n; i++) {
            double a = (Math.PI * 2 * i) / n;
            Location at = center.clone().add(Math.cos(a) * radius, 0.15, Math.sin(a) * radius);
            particles(at, particle, 1, 0.02);
        }
    }

    public void sound(Location loc, String name, float volume, float pitch) {
        if (!plugin.getConfig().getBoolean("effects.sounds", true) || name == null || loc.getWorld() == null) return;
        NamespacedKey key = NamespacedKey.fromString(name.toLowerCase());
        Sound s = key == null ? null : Registry.SOUNDS.get(key);
        if (s == null) { plugin.debug("Unknown sound: " + name); return; }
        loc.getWorld().playSound(loc, s, volume, pitch);
    }
}
