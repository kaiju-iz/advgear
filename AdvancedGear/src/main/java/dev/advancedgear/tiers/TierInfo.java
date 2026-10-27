package dev.advancedgear.tiers;

import net.kyori.adventure.text.format.TextColor;

/** A tier from config.yml. {@code previous} is the id of the tier that is upgraded (or "netherite"). */
public record TierInfo(String id, String displayName, TextColor color, String previous,
                       String templateId, String ingotId) { }
