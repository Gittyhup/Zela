package gg.zela.hub.listener;

import gg.zela.hub.ZelaHub;
import gg.zela.hub.menu.Actions;
import gg.zela.hub.util.Items;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Gives the lobby hotbar and runs whatever an item is bound to. */
public class HotbarListener implements Listener {

    private final ZelaHub plugin;
    private final NamespacedKey key;

    public HotbarListener(ZelaHub plugin) {
        this.plugin = plugin;
        this.key = new NamespacedKey(plugin, "action");
    }

    public void giveHotbar(Player player) {
        player.getInventory().clear();

        List<Map<?, ?>> entries = plugin.getConfig().getMapList("hotbar");
        for (Map<?, ?> raw : entries) {
            Map<String, Object> map = new HashMap<>();
            for (Map.Entry<?, ?> e : raw.entrySet()) map.put(String.valueOf(e.getKey()), e.getValue());

            MemoryConfiguration mem = new MemoryConfiguration();
            ConfigurationSection sec = mem.createSection("item", map);

            int slot = sec.getInt("slot", 0);
            if (slot < 0 || slot > 8) continue;

            ItemStack stack = Items.from(sec, plugin.placeholders(player));

            String action = sec.getString("action", "none");
            ItemMeta meta = stack.getItemMeta();
            if (meta != null) {
                meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, action);
                stack.setItemMeta(meta);
            }

            player.getInventory().setItem(slot, stack);
        }
        player.updateInventory();
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != org.bukkit.inventory.EquipmentSlot.HAND) return;

        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK
                && action != Action.LEFT_CLICK_AIR && action != Action.LEFT_CLICK_BLOCK) return;

        ItemStack item = event.getItem();
        if (item == null || item.getItemMeta() == null) return;

        String bound = item.getItemMeta().getPersistentDataContainer()
                .get(key, PersistentDataType.STRING);
        if (bound == null) return;

        event.setCancelled(true);
        Actions.run(plugin, event.getPlayer(), bound);
    }

    @EventHandler
    public void onSwap(PlayerSwapHandItemsEvent event) {
        if (!event.getPlayer().hasPermission("zela.build")) event.setCancelled(true);
    }
}
