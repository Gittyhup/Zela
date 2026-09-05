package gg.zela.hub.menu;

import gg.zela.hub.ZelaHub;
import gg.zela.hub.util.Items;
import gg.zela.hub.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Reads the "menus" section of config.yml and opens them. */
public class Menus {

    private final ZelaHub plugin;
    private final Map<String, Def> defs = new HashMap<>();

    public Menus(ZelaHub plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        defs.clear();
        ConfigurationSection root = plugin.getConfig().getConfigurationSection("menus");
        if (root == null) return;

        for (String key : root.getKeys(false)) {
            ConfigurationSection m = root.getConfigurationSection(key);
            if (m == null) continue;

            Def def = new Def();
            def.title = m.getString("title", "<white>Menu</white>");
            def.rows = Math.max(1, Math.min(6, m.getInt("rows", 3)));
            def.filler = Material.matchMaterial(m.getString("filler", "AIR"));

            List<Map<?, ?>> items = m.getMapList("items");
            for (int i = 0; i < items.size(); i++) {
                org.bukkit.configuration.MemoryConfiguration mem =
                        new org.bukkit.configuration.MemoryConfiguration();
                ConfigurationSection sec = mem.createSection("item", castMap(items.get(i)));
                Entry e = new Entry();
                e.slot = sec.getInt("slot", i);
                e.section = sec;
                e.action = sec.getString("action", "none");
                def.entries.add(e);
            }
            defs.put(key.toLowerCase(), def);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castMap(Map<?, ?> in) {
        Map<String, Object> out = new HashMap<>();
        for (Map.Entry<?, ?> e : in.entrySet()) {
            out.put(String.valueOf(e.getKey()), e.getValue());
        }
        return out;
    }

    public boolean open(Player player, String name) {
        Def def = defs.get(name == null ? "" : name.toLowerCase());
        if (def == null) {
            plugin.getLogger().warning("Menu '" + name + "' is not defined in config.yml");
            return false;
        }

        Map<String, String> ph = plugin.placeholders(player);
        MenuHolder holder = new MenuHolder(name);
        Inventory inv = Bukkit.createInventory(holder, def.rows * 9, Text.of(def.title, ph));
        holder.setInventory(inv);

        if (def.filler != null && def.filler != Material.AIR) {
            ItemStack pane = Items.simple(def.filler, " ");
            for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, pane);
        }

        for (Entry e : def.entries) {
            if (e.slot < 0 || e.slot >= inv.getSize()) continue;
            inv.setItem(e.slot, Items.from(e.section, ph));
            holder.actions().put(e.slot, e.action);
        }

        player.openInventory(inv);
        return true;
    }

    private static class Def {
        String title;
        int rows;
        Material filler;
        final List<Entry> entries = new ArrayList<>();
    }

    private static class Entry {
        int slot;
        ConfigurationSection section;
        String action;
    }
}
