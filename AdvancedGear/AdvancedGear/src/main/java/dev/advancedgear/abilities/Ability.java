package dev.advancedgear.abilities;

import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.UUID;

/** A weapon/tool ability. Active abilities fire on the configured control; passive ones hook melee hits. */
public interface Ability {
    String id();

    /** True if the ability is triggered by the player's control (right-click by default). */
    default boolean active() { return false; }

    /** Runs the active effect. Return true if it fired (this starts the cooldown). */
    default boolean activate(AbilityContext ctx) { return false; }

    /** Called when the holder lands a melee hit. {@code event} may be modified (damage, etc.). */
    default void onAttack(AbilityContext ctx, LivingEntity victim, EntityDamageByEntityEvent event) { }

    /** Drop per-player state (called on quit). */
    default void cleanup(UUID player) { }
}
