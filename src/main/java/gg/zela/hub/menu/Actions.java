package gg.zela.hub.menu;

import gg.zela.hub.PlayerState;
import gg.zela.hub.ZelaHub;
import gg.zela.hub.listener.HotbarListener;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.entity.Player;

/**
 * Runs an action string.
 *   menu:&lt;name&gt;      open another menu
 *   command:&lt;cmd&gt;    run as the player
 *   console:&lt;cmd&gt;    run from console
 *   message:&lt;text&gt;   send MiniMessage text
 *   toggle:&lt;what&gt;    visibility | scoreboard | doublejump
 *   trail:&lt;id&gt;       none | zela | flame | happy
 *   none              do nothing
 */
public final class Actions {

    private Actions() {
    }

    public static void run(ZelaHub plugin, Player player, String action) {
        if (action == null || action.isEmpty() || action.equalsIgnoreCase("none")) return;

        int i = action.indexOf(':');
        String type = i < 0 ? action : action.substring(0, i);
        String arg = i < 0 ? "" : action.substring(i + 1);

        switch (type.toLowerCase()) {
            case "menu" -> {
                plugin.menus().open(player, arg);
                player.playSound(player.getLocation(), "ui.button.click", 0.6f, 1.3f);
            }
            case "command" -> {
                player.closeInventory();
                player.performCommand(arg);
            }
            case "console" -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), arg);
            case "message" -> {
                player.closeInventory();
                plugin.msg(player, arg);
            }
            case "toggle" -> toggle(plugin, player, arg);
            case "trail" -> {
                PlayerState st = plugin.state(player);
                st.trail = PlayerState.trailFor(arg);
                plugin.msg(player, st.trail == null
                        ? "<#6F6488>Trail turned off.</#6F6488>"
                        : "<#B57CFF>Trail equipped.</#B57CFF>");
                player.playSound(player.getLocation(), "entity.experience_orb.pickup", 0.7f, 1.6f);
            }
            default -> plugin.getLogger().warning("Unknown menu action: " + action);
        }
    }

    private static void toggle(ZelaHub plugin, Player player, String what) {
        PlayerState st = plugin.state(player);

        switch (what.toLowerCase()) {
            case "visibility" -> {
                st.playersVisible = !st.playersVisible;
                applyVisibility(plugin, player);
                plugin.msg(player, st.playersVisible
                        ? "<#52E08A>Players are visible.</#52E08A>"
                        : "<#6F6488>Players hidden.</#6F6488>");
                new HotbarListener(plugin).giveHotbar(player);
            }
            case "scoreboard" -> {
                st.scoreboardVisible = !st.scoreboardVisible;
                plugin.board().apply(player);
                plugin.msg(player, st.scoreboardVisible
                        ? "<#52E08A>Scoreboard shown.</#52E08A>"
                        : "<#6F6488>Scoreboard hidden.</#6F6488>");
            }
            case "doublejump" -> {
                st.doubleJumpEnabled = !st.doubleJumpEnabled;
                player.setAllowFlight(st.doubleJumpEnabled && !player.isFlying());
                plugin.msg(player, st.doubleJumpEnabled
                        ? "<#52E08A>Double jump on.</#52E08A>"
                        : "<#6F6488>Double jump off.</#6F6488>");
            }
            default -> plugin.getLogger().warning("Unknown toggle: " + what);
        }
        player.playSound(player.getLocation(), "ui.button.click", 0.6f, 1.1f);
    }

    /** Show or hide everyone else for this player. */
    public static void applyVisibility(ZelaHub plugin, Player player) {
        boolean show = plugin.state(player).playersVisible;
        for (Player other : Bukkit.getOnlinePlayers()) {
            if (other.equals(player)) continue;
            if (show) player.showPlayer(plugin, other);
            else player.hidePlayer(plugin, other);
        }
    }

    /** Make sure a newly joined player respects everyone else's visibility choice. */
    public static void applyVisibilityForNewcomer(ZelaHub plugin, Player joined) {
        for (Player other : Bukkit.getOnlinePlayers()) {
            if (other.equals(joined)) continue;
            if (!plugin.state(other).playersVisible) other.hidePlayer(plugin, joined);
        }
    }

    public static Particle particleOf(String id) {
        return PlayerState.trailFor(id);
    }
}
