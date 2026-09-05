package gg.zela.hub.board;

import gg.zela.hub.ZelaHub;
import gg.zela.hub.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Owns one Scoreboard per player. The sidebar lives here; TabManager writes its
 * teams into the same board, because a player can only ever view one at a time.
 */
public class BoardManager {

    /** Unique, invisible entry strings — one per sidebar line. */
    private static final String[] ENTRIES = {
            "§0§r", "§1§r", "§2§r", "§3§r", "§4§r", "§5§r", "§6§r", "§7§r",
            "§8§r", "§9§r", "§a§r", "§b§r", "§c§r", "§d§r", "§e§r"
    };

    private final ZelaHub plugin;
    private final Map<UUID, Scoreboard> boards = new HashMap<>();
    private BukkitTask task;

    public BoardManager(ZelaHub plugin) {
        this.plugin = plugin;
    }

    public void start() {
        int ticks = Math.max(5, plugin.getConfig().getInt("scoreboard.refresh-ticks", 20));
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, ticks);
    }

    public void stop() {
        if (task != null) task.cancel();
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        }
        boards.clear();
    }

    /** The player's personal board, created on first use. */
    public Scoreboard boardOf(Player player) {
        return boards.computeIfAbsent(player.getUniqueId(),
                k -> Bukkit.getScoreboardManager().getNewScoreboard());
    }

    /** Attach the board and draw the sidebar (or hide it). */
    public void apply(Player player) {
        Scoreboard board = boardOf(player);
        player.setScoreboard(board);
        draw(player, board);
    }

    public void remove(Player player) {
        boards.remove(player.getUniqueId());
    }

    private void tick() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            Scoreboard board = boardOf(p);
            if (p.getScoreboard() != board) p.setScoreboard(board);
            draw(p, board);
        }
    }

    private void draw(Player player, Scoreboard board) {
        boolean on = plugin.getConfig().getBoolean("scoreboard.enabled", true)
                && plugin.state(player).scoreboardVisible;

        Objective obj = board.getObjective("zela");

        if (!on) {
            if (obj != null) board.clearSlot(DisplaySlot.SIDEBAR);
            return;
        }

        Map<String, String> ph = plugin.placeholders(player);

        if (obj == null) {
            obj = board.registerNewObjective("zela", Criteria.DUMMY, "Zela");
        }
        obj.displayName(Text.of(plugin.getConfig().getString("scoreboard.title", "<white>Server</white>"), ph));
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        List<String> lines = plugin.getConfig().getStringList("scoreboard.lines");
        int count = Math.min(lines.size(), ENTRIES.length);

        for (int i = 0; i < count; i++) {
            String entry = ENTRIES[i];
            String teamName = "zl" + i;

            Team team = board.getTeam(teamName);
            if (team == null) team = board.registerNewTeam(teamName);
            if (!team.hasEntry(entry)) team.addEntry(entry);

            team.prefix(Text.item(lines.get(i), ph));
            obj.getScore(entry).setScore(count - i);
        }

        // Clean up if the config now has fewer lines than last time.
        for (int i = count; i < ENTRIES.length; i++) {
            Team team = board.getTeam("zl" + i);
            if (team != null) team.unregister();
            board.resetScores(ENTRIES[i]);
        }
    }
}
