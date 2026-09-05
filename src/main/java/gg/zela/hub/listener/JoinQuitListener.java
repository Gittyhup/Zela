package gg.zela.hub.listener;

import gg.zela.hub.ZelaHub;
import gg.zela.hub.menu.Actions;
import gg.zela.hub.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.FireworkEffect;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.meta.FireworkMeta;

import java.util.List;

public class JoinQuitListener implements Listener {

    private final ZelaHub plugin;

    public JoinQuitListener(ZelaHub plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        String join = plugin.getConfig().getString("join.message", "");
        event.joinMessage(join.isEmpty() ? null : Text.of(join, plugin.placeholders(player)));

        if (plugin.getConfig().getBoolean("join.gamemode-adventure", true)
                && !player.hasPermission("zela.build")) {
            player.setGameMode(GameMode.ADVENTURE);
        }
        if (plugin.getConfig().getBoolean("join.heal", true)) {
            try {
                player.setHealth(20.0);
            } catch (IllegalArgumentException ignored) {
                // Custom max health — leave it alone.
            }
            player.setFoodLevel(20);
            player.setSaturation(20f);
            player.setFireTicks(0);
        }
        if (plugin.getConfig().getBoolean("join.clear-inventory", true)) {
            player.getInventory().clear();
            player.getInventory().setArmorContents(null);
        }

        Location spawn = plugin.spawn();
        if (spawn != null && plugin.getConfig().getBoolean("join.teleport-to-spawn", true)) {
            player.teleport(spawn);
        }

        new HotbarListener(plugin).giveHotbar(player);
        plugin.board().apply(player);

        // Respect other players' "hide everyone" setting, and apply this player's.
        Actions.applyVisibility(plugin, player);
        Actions.applyVisibilityForNewcomer(plugin, player);

        // Double jump needs flight allowed but not active.
        player.setAllowFlight(plugin.getConfig().getBoolean("mechanics.double-jump.enabled", true));
        player.setFlying(false);

        List<String> welcome = plugin.getConfig().getStringList("join.welcome");
        for (String line : welcome) {
            player.sendMessage(Text.of(line, plugin.placeholders(player)));
        }

        String sound = plugin.getConfig().getString("join.sound", "");
        if (sound != null && !sound.isEmpty()) {
            player.playSound(player.getLocation(), sound, 0.7f, 1.2f);
        }

        if (plugin.getConfig().getBoolean("join.firework-for-ranks", true) && isRanked(player)) {
            spawnFirework(player.getLocation());
        }

        sendResourcePack(player);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        String quit = plugin.getConfig().getString("join.quit-message", "");
        event.quitMessage(quit.isEmpty() ? null : Text.of(quit, plugin.placeholders(player)));
        plugin.board().remove(player);
        plugin.clearState(player);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Location spawn = plugin.spawn();
        if (spawn != null) event.setRespawnLocation(spawn);
    }

    private boolean isRanked(Player player) {
        return plugin.ranks().stream()
                .anyMatch(r -> !r.permission().isEmpty() && player.hasPermission(r.permission()));
    }

    private void spawnFirework(Location loc) {
        Firework fw = loc.getWorld().spawn(loc, Firework.class);
        FireworkMeta meta = fw.getFireworkMeta();
        meta.addEffect(FireworkEffect.builder()
                .with(FireworkEffect.Type.BALL)
                .withColor(Color.fromRGB(0x8B45F0))
                .withFade(Color.fromRGB(0xB57CFF))
                .trail(true)
                .build());
        meta.setPower(0);
        fw.setFireworkMeta(meta);
        Bukkit.getScheduler().runTaskLater(plugin, fw::detonate, 2L);
    }

    private void sendResourcePack(Player player) {
        String url = plugin.getConfig().getString("resource-pack.url", "");
        if (url == null || url.isEmpty()) return;

        String sha = plugin.getConfig().getString("resource-pack.sha1", "");
        boolean required = plugin.getConfig().getBoolean("resource-pack.required", false);
        String prompt = plugin.getConfig().getString("resource-pack.prompt", "");

        try {
            player.setResourcePack(url,
                    (sha == null || sha.isEmpty()) ? null : sha,
                    required,
                    (prompt == null || prompt.isEmpty()) ? null : Text.of(prompt));
        } catch (Exception e) {
            plugin.getLogger().warning("Could not send resource pack: " + e.getMessage());
        }
    }
}
