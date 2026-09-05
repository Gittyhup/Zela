package gg.zela.hub.listener;

import gg.zela.hub.ZelaHub;
import gg.zela.hub.util.Text;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.weather.WeatherChangeEvent;

import java.util.HashMap;
import java.util.Map;

/** Everything a lobby should refuse to let players do. */
public class ProtectionListener implements Listener {

    private final ZelaHub plugin;

    public ProtectionListener(ZelaHub plugin) {
        this.plugin = plugin;
    }

    private boolean on(String path) {
        return plugin.getConfig().getBoolean("mechanics.protect." + path, true);
    }

    private boolean bypass(Player p) {
        return p.hasPermission("zela.build");
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        if (on("build") && !bypass(e.getPlayer())) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent e) {
        if (on("build") && !bypass(e.getPlayer())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onInteract(PlayerInteractEvent e) {
        if (!on("interact")) return;
        if (bypass(e.getPlayer())) return;
        if (e.getClickedBlock() == null) return;
        e.setCancelled(true);
    }

    @EventHandler
    public void onDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        if (!on("damage")) return;
        if (bypass(p) && e.getCause() != EntityDamageEvent.DamageCause.VOID) return;
        e.setCancelled(true);
    }

    @EventHandler
    public void onHunger(FoodLevelChangeEvent e) {
        if (on("hunger")) {
            e.setCancelled(true);
            e.setFoodLevel(20);
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent e) {
        if (on("item-drop") && !bypass(e.getPlayer())) e.setCancelled(true);
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        if (on("item-pickup") && !bypass(p)) e.setCancelled(true);
    }

    @EventHandler
    public void onWeather(WeatherChangeEvent e) {
        if (on("weather") && e.toWeatherState()) e.setCancelled(true);
    }

    /** Stop players rearranging their own hotbar. */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (bypass(p)) return;
        if (e.getInventory().getHolder() instanceof gg.zela.hub.menu.MenuHolder) return;  // handled elsewhere
        e.setCancelled(true);
    }

    @EventHandler
    public void onChat(AsyncChatEvent e) {
        if (!plugin.getConfig().getBoolean("mechanics.chat.enabled", true)) return;

        Player p = e.getPlayer();
        String raw = PlainTextComponentSerializer.plainText().serialize(e.message());

        Map<String, String> ph = new HashMap<>(plugin.placeholders(p));
        ph.put("message", raw);

        Component formatted = Text.of(
                plugin.getConfig().getString("mechanics.chat.format",
                        "<rank><white><player></white> <dark_gray>»</dark_gray> <gray><message></gray>"),
                ph);

        e.renderer((source, displayName, message, audience) -> formatted);
    }
}
