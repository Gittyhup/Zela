package gg.zela.hub.tab;

import gg.zela.hub.Rank;
import gg.zela.hub.ZelaHub;
import gg.zela.hub.util.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.HashSet;
import java.util.Set;

/**
 * The player list: branded header and footer, rank prefixes on every name, and
 * rank-ordered sorting. Every online player appears for everyone.
 */
public class TabManager {

    private final ZelaHub plugin;
    private BukkitTask task;

    public TabManager(ZelaHub plugin) {
        this.plugin = plugin;
    }

    public void start() {
        int ticks = Math.max(5, plugin.getConfig().getInt("tab.refresh-ticks", 20));
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, ticks);
    }

    public void stop() {
        if (task != null) task.cancel();
    }

    private void tick() {
        if (!plugin.getConfig().getBoolean("tab.enabled", true)) return;

        for (Player viewer : Bukkit.getOnlinePlayers()) {
            header(viewer);
            sort(viewer);
        }
    }

    /** Header and footer are per-viewer, so <ping> is that player's own ping. */
    private void header(Player viewer) {
        Component head = Text.lines(plugin.getConfig().getStringList("tab.header"), plugin.placeholders(viewer));
        Component foot = Text.lines(plugin.getConfig().getStringList("tab.footer"), plugin.placeholders(viewer));
        viewer.sendPlayerListHeaderAndFooter(head, foot);
    }

    /**
     * Writes one team per online player into the viewer's board. The team name is
     * the rank weight plus the player name, which is what the client sorts by.
     */
    private void sort(Player viewer) {
        Scoreboard board = plugin.board().boardOf(viewer);
        Set<String> wanted = new HashSet<>();

        for (Player target : Bukkit.getOnlinePlayers()) {
            Rank rank = plugin.rankOf(target);

            // Team names are capped at 16 characters on older clients — keep it short.
            String teamName = rank.sortKey() + shorten(target.getName());
            wanted.add(teamName);

            Team team = board.getTeam(teamName);
            if (team == null) team = board.registerNewTeam(teamName);

            team.prefix(Text.item(rank.prefix(), plugin.placeholders(target)));
            team.suffix(Component.empty());

            if (!team.hasEntry(target.getName())) {
                // A player can only be on one team — clear any stale one first.
                Team old = board.getEntryTeam(target.getName());
                if (old != null && !old.equals(team)) old.removeEntry(target.getName());
                team.addEntry(target.getName());
            }

            // The name shown in the list itself.
            target.playerListName(Text.item(
                    plugin.getConfig().getString("tab.format", "<rank><white><player></white>"),
                    plugin.placeholders(target)));
        }

        // Drop teams for players who have left.
        for (Team team : new HashSet<>(board.getTeams())) {
            String name = team.getName();
            if (name.startsWith("zl")) continue;            // sidebar lines
            if (!wanted.contains(name)) team.unregister();
        }
    }

    private static String shorten(String name) {
        return name.length() > 12 ? name.substring(0, 12) : name;
    }
}
