package gg.zela.hub.listener;

import gg.zela.hub.ZelaHub;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.util.Vector;

/** Double jump, launch pads and void protection. */
public class MechanicsListener implements Listener {

    private final ZelaHub plugin;

    public MechanicsListener(ZelaHub plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onFlightToggle(PlayerToggleFlightEvent event) {
        Player player = event.getPlayer();

        if (!plugin.getConfig().getBoolean("mechanics.double-jump.enabled", true)) return;
        if (!plugin.state(player).doubleJumpEnabled) return;
        if (player.getGameMode() == org.bukkit.GameMode.CREATIVE
                || player.getGameMode() == org.bukkit.GameMode.SPECTATOR) return;
        if (!event.isFlying()) return;

        event.setCancelled(true);
        player.setFlying(false);
        player.setAllowFlight(false);

        double power = plugin.getConfig().getDouble("mechanics.double-jump.power", 1.0);
        double up = plugin.getConfig().getDouble("mechanics.double-jump.up", 0.9);

        Vector push = player.getLocation().getDirection().multiply(power).setY(up);
        player.setVelocity(push);

        playSound(player, plugin.getConfig().getString("mechanics.double-jump.sound", ""), 0.8f, 1.4f);
        player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 12, 0.3, 0.1, 0.3, 0.02);
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (event.getTo() == null) return;
        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockY() == event.getTo().getBlockY()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) return;

        Player player = event.getPlayer();

        // Give double jump back once they land.
        if (player.isOnGround()
                && plugin.getConfig().getBoolean("mechanics.double-jump.enabled", true)
                && plugin.state(player).doubleJumpEnabled
                && !player.getAllowFlight()) {
            player.setAllowFlight(true);
        }

        // Void protection.
        int voidY = plugin.getConfig().getInt("spawn.void-y", 0);
        Location spawn = plugin.spawn();
        if (spawn != null && player.getLocation().getY() < voidY) {
            player.teleport(spawn);
            player.setFallDistance(0f);
            playSound(player, "ENTITY_ENDERMAN_TELEPORT", 0.6f, 1.4f);
            return;
        }

        // Launch pads.
        if (!plugin.getConfig().getBoolean("mechanics.launch-pads.enabled", true)) return;

        Material padMat = Material.matchMaterial(
                plugin.getConfig().getString("mechanics.launch-pads.material", "SLIME_BLOCK"));
        if (padMat == null) return;

        Material under = player.getLocation().clone().subtract(0, 1, 0).getBlock().getType();
        if (under != padMat) return;

        double power = plugin.getConfig().getDouble("mechanics.launch-pads.power", 2.4);
        double up = plugin.getConfig().getDouble("mechanics.launch-pads.up", 1.0);

        player.setVelocity(player.getLocation().getDirection().multiply(power).setY(up));
        playSound(player, plugin.getConfig().getString("mechanics.launch-pads.sound", ""), 1.0f, 1.0f);
        player.getWorld().spawnParticle(Particle.END_ROD, player.getLocation(), 20, 0.4, 0.1, 0.4, 0.05);
    }

    private void playSound(Player player, String name, float volume, float pitch) {
        if (name == null || name.isEmpty()) return;
        try {
            player.playSound(player.getLocation(), Sound.valueOf(name.toUpperCase()), volume, pitch);
        } catch (IllegalArgumentException ignored) {
            // Unknown sound name in config — silently skip rather than spam the log.
        }
    }
}
