package com.starshooterstudios.waypointhomes;

import net.kyori.adventure.key.Key;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class CustomAdvancements {
    @SuppressWarnings("deprecation")
    public static @NotNull Advancement makeAdvancement(NamespacedKey key, String name, String description, ItemStack icon, AdvancementFrame frame, AdvancementData data, boolean hidden, boolean showToast, boolean announce, boolean showEnchanted) {
        String s = data.getAdvancementFormat();
        s = s.replaceFirst("ITEM_ID", icon.getType().getKey().asString());
        int cmd;
        if (icon.getItemMeta().hasCustomModelData()) cmd = icon.getItemMeta().getCustomModelData();
        else cmd = 0;
        s = s.replaceFirst("CUSTOM_MODEL_DATA", String.valueOf(cmd));
        s = s.replaceFirst("SHOW_ENCHANTED", showEnchanted ? "true" : "false");
        s = s.replaceFirst("ADVANCEMENT", name);
        s = s.replaceFirst("ADVANCEMENT_DESCRIPTION", description);
        s = s.replaceFirst("FRAME", frame.getName());
        s = s.replaceFirst("SHOW_TOAST", showToast ? "true" : "false");
        s = s.replaceFirst("ANNOUNCE", announce ? "true" : "false");
        s = s.replaceFirst("HIDDEN", hidden ? "true" : "false");
        return new RealAdvancement(Bukkit.getUnsafe().loadAdvancement(key, s));
    }

    public static class RealAdvancement implements Advancement {
        private final org.bukkit.advancement.Advancement bukkitAdvancement;

        public RealAdvancement(org.bukkit.advancement.Advancement bukkitAdvancement) {
            this.bukkitAdvancement = bukkitAdvancement;
        }

        @Override
        public void grant(Player player) {
            if (player.getAdvancementProgress(bukkitAdvancement).isDone()) return;
            player.getAdvancementProgress(bukkitAdvancement).awardCriteria("supernova_internals");
        }
    }

    public enum AdvancementFrame {
        TASK("task"),
        GOAL("goal");

        private final String name;

        AdvancementFrame(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    public interface Advancement {
        void grant(Player player);
    }

    public interface AdvancementData {
        String getAdvancementFormat();
    }

    public record ParentedAdvancementData(Key parent) implements AdvancementData {

        @Override
        public String getAdvancementFormat() {
            return """
            {
              "display": {
                "icon": {
                  "id": "ITEM_ID",
                  "count": 1,
                  "components": {
                    "minecraft:custom_model_data": CUSTOM_MODEL_DATA,
                    "minecraft:enchantment_glint_override": SHOW_ENCHANTED
                  }
                },
                "title": "ADVANCEMENT",
                "description": "ADVANCEMENT_DESCRIPTION",
                "frame": "FRAME",
                "show_toast": SHOW_TOAST,
                "announce_to_chat": ANNOUNCE,
                "hidden": HIDDEN
              },
              "parent": "%s",
              "criteria": {
                "supernova_internals": {
                  "trigger": "minecraft:impossible"
                }
              }
            }
            """.formatted(parent.asString());
        }
    }
}
