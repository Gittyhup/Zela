package gg.zela.hub;

import gg.zela.hub.board.BoardManager;
import gg.zela.hub.command.HubCommand;
import gg.zela.hub.cosmetic.TrailTask;
import gg.zela.hub.listener.HotbarListener;
import gg.zela.hub.listener.JoinQuitListener;
import gg.zela.hub.listener.MechanicsListener;
import gg.zela.hub.listener.ProtectionListener;
import gg.zela.hub.menu.MenuListener;
import gg.zela.hub.menu.Menus;
import gg.zela.hub.tab.TabManager;
import gg.zela.hub.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Monster;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ZelaHub extends JavaPlugin {

    private final Map<UUID, PlayerState> states = new HashMap<>();
    private final List<Rank> ranks = new ArrayList<>();

    private TabManager tab;
    private BoardManager board;
    private Menus menus;
    private Location spawn;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadRanks();
        loadSpawn();

        this.menus = new Menus(this);
        this.board = new BoardManager(this);
        this.tab = new TabManager(this);

        getServer().getPluginManager().registerEvents(new JoinQuitListener(this), this);
        getServer().getPluginManager().registerEvents(new ProtectionListener(this), this);
        getServer().getPluginManager().registerEvents(new HotbarListener(this), this);
        getServer().getPluginManager().registerEvents(new MechanicsListener(this), this);
        getServer().getPluginManager().registerEvents(new MenuListener(this), this);

        HubCommand cmd = new HubCommand(this);
        if (getCommand("hub") != null) getCommand("hub").setExecutor(cmd);
        if (getCommand("zelahub") != null) getCommand("zelahub").setExecutor(cmd);

        tab.start();
        board.start();
        new TrailTask(this).runTaskTimer(this, 20L, 3L);

        applyWorldRules();

        // Players already online when the plugin reloads.
        for (Player p : Bukkit.getOnlinePlayers()) {
            states.put(p.getUniqueId(), new PlayerState());
            board.apply(p);
        }

        getLogger().info("ZelaHub enabled — " + ranks.size() + " ranks loaded.");
    }

    @Override
    public void onDisable() {
        if (tab != null) tab.stop();
        if (board != null) board.stop();
    }

    // ---------------------------------------------------------------- config

    public void reloadAll() {
        reloadConfig();
        loadRanks();
        loadSpawn();
        menus.reload();
        applyWorldRules();
        for (Player p : Bukkit.getOnlinePlayers()) {
            board.apply(p);
            new HotbarListener(this).giveHotbar(p);
        }
    }

    private void loadRanks() {
        ranks.clear();
        List<Map<?, ?>> raw = getConfig().getMapList("ranks");

        for (Map<?, ?> entry : raw) {
            Map<String, Object> m = new HashMap<>();
            for (Map.Entry<?, ?> e : entry.entrySet()) {
                m.put(String.valueOf(e.getKey()), e.getValue());
            }

            String id = str(m.get("id"), "rank");
            String perm = str(m.get("permission"), "");
            String prefix = str(m.get("prefix"), "");
            String name = str(m.get("name"), "<gray>Player</gray>");

            int weight = 90;
            Object w = m.get("weight");
            if (w instanceof Number n) weight = n.intValue();

            ranks.add(new Rank(id, perm, prefix, name, weight));
        }

        if (ranks.isEmpty()) {
            ranks.add(new Rank("default", "", "", "<gray>Player</gray>", 90));
        }
    }

    private static String str(Object value, String fallback) {
        if (value == null) return fallback;
        String s = String.valueOf(value);
        return "null".equals(s) ? fallback : s;
    }

    private void loadSpawn() {
        ConfigurationSection s = getConfig().getConfigurationSection("spawn");
        if (s == null) {
            spawn = null;
            return;
        }
        World world = Bukkit.getWorld(s.getString("world", "world"));
        if (world == null) world = Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0);
        if (world == null) {
            spawn = null;
            return;
        }
        spawn = new Location(world,
                s.getDouble("x", 0.5), s.getDouble("y", 100), s.getDouble("z", 0.5),
                (float) s.getDouble("yaw", 0), (float) s.getDouble("pitch", 0));
    }

    public void setSpawn(Location loc) {
        getConfig().set("spawn.world", loc.getWorld().getName());
        getConfig().set("spawn.x", loc.getX());
        getConfig().set("spawn.y", loc.getY());
        getConfig().set("spawn.z", loc.getZ());
        getConfig().set("spawn.yaw", (double) loc.getYaw());
        getConfig().set("spawn.pitch", (double) loc.getPitch());
        saveConfig();
        this.spawn = loc.clone();
        loc.getWorld().setSpawnLocation(loc);
    }

    private void applyWorldRules() {
        ConfigurationSection p = getConfig().getConfigurationSection("mechanics.protect");
        if (p == null) return;
        for (World w : Bukkit.getWorlds()) {
            if (p.getBoolean("weather", true)) {
                w.setStorm(false);
                w.setThundering(false);
                w.setWeatherDuration(Integer.MAX_VALUE);
            }
            int lock = p.getInt("time-lock", -1);
            if (lock >= 0) {
                w.setTime(lock);
                w.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
            }
            if (p.getBoolean("mob-spawning", true)) {
                w.setGameRule(GameRule.DO_MOB_SPAWNING, false);
                for (Entity e : w.getEntities()) {
                    if (e instanceof Monster) e.remove();
                }
            }
            w.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
            w.setGameRule(GameRule.DO_FIRE_TICK, false);
            w.setGameRule(GameRule.MOB_GRIEFING, false);
        }
    }

    // -------------------------------------------------------------- accessors

    public PlayerState state(Player p) {
        return states.computeIfAbsent(p.getUniqueId(), k -> new PlayerState());
    }

    public void clearState(Player p) {
        states.remove(p.getUniqueId());
    }

    public Rank rankOf(Player p) {
        for (Rank r : ranks) {
            if (r.matches(p)) return r;
        }
        return ranks.get(ranks.size() - 1);
    }

    public List<Rank> ranks() {
        return ranks;
    }

    public Location spawn() {
        return spawn == null ? null : spawn.clone();
    }

    public TabManager tab() {
        return tab;
    }

    public BoardManager board() {
        return board;
    }

    public Menus menus() {
        return menus;
    }

    /** Placeholders available everywhere. */
    public Map<String, String> placeholders(Player p) {
        Map<String, String> m = new HashMap<>();
        m.put("player", p.getName());
        m.put("online", String.valueOf(Bukkit.getOnlinePlayers().size()));
        m.put("max", String.valueOf(Bukkit.getMaxPlayers()));
        m.put("ping", String.valueOf(p.getPing()));
        m.put("world", p.getWorld().getName());
        Rank r = rankOf(p);
        m.put("rank", r.prefix());
        m.put("rank-name", r.name());
        m.put("rank-id", r.id());
        return m;
    }

    public void msg(Player p, String miniMessage) {
        if (miniMessage == null || miniMessage.isEmpty()) return;
        p.sendMessage(Text.of(miniMessage, placeholders(p)));
    }
}
