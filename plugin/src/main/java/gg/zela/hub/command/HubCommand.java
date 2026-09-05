package gg.zela.hub.command;

import gg.zela.hub.ZelaHub;
import gg.zela.hub.listener.HotbarListener;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class HubCommand implements CommandExecutor {

    private final ZelaHub plugin;

    public HubCommand(ZelaHub plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {

        if (command.getName().equalsIgnoreCase("hub")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("Players only.");
                return true;
            }
            Location spawn = plugin.spawn();
            if (spawn == null) {
                plugin.msg(player, "<#FF5D7A>Spawn is not set. An admin needs to run /zelahub setspawn.</#FF5D7A>");
                return true;
            }
            player.teleport(spawn);
            new HotbarListener(plugin).giveHotbar(player);
            player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.6f, 1.4f);
            plugin.msg(player, "<#B57CFF>Sent to spawn.</#B57CFF>");
            return true;
        }

        // /zelahub
        if (args.length == 0) {
            sender.sendMessage("§8» §dZelaHub §7/zelahub <reload|setspawn>");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> {
                plugin.reloadAll();
                sender.sendMessage("§8» §dZelaHub §7config reloaded.");
            }
            case "setspawn" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("Players only.");
                    return true;
                }
                plugin.setSpawn(player.getLocation());
                sender.sendMessage("§8» §dZelaHub §7spawn set to where you are standing.");
            }
            default -> sender.sendMessage("§8» §dZelaHub §7/zelahub <reload|setspawn>");
        }
        return true;
    }
}
