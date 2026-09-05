package gg.zela.hub.cosmetic;

import gg.zela.hub.PlayerState;
import gg.zela.hub.ZelaHub;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

/** Draws the particle trail behind anyone who picked one. */
public class TrailTask extends BukkitRunnable {

    private final ZelaHub plugin;

    public TrailTask(ZelaHub plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerState state = plugin.state(player);
            Particle particle = state.trail;
            if (particle == null) continue;
            if (player.getVelocity().lengthSquared() < 0.005) continue;

            player.getWorld().spawnParticle(
                    particle,
                    player.getLocation().add(0, 0.15, 0),
                    3, 0.15, 0.05, 0.15, 0.0);
        }
    }
}
