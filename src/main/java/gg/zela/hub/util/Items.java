package gg.zela.hub.util;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.Map;

/** Builds ItemStacks from a config section. */
public final class Items {

    private Items() {
    }

    public static ItemStack from(ConfigurationSection sec, Map<String, String> placeholders) {
        Material mat = Material.matchMaterial(sec.getString("material", "STONE"));
        if (mat == null) mat = Material.STONE;

        ItemStack stack = new ItemStack(mat, Math.max(1, sec.getInt("amount", 1)));
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return stack;

        String name = sec.getString("name");
        if (name != null && !name.isEmpty()) meta.displayName(Text.item(name, placeholders));

        List<String> lore = sec.getStringList("lore");
        if (!lore.isEmpty()) meta.lore(Text.items(lore, placeholders));

        if (sec.getBoolean("glow", false)) {
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);

        stack.setItemMeta(meta);
        return stack;
    }

    public static ItemStack simple(Material mat, String name) {
        ItemStack stack = new ItemStack(mat);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.displayName(Text.item(name));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
            stack.setItemMeta(meta);
        }
        return stack;
    }
}
