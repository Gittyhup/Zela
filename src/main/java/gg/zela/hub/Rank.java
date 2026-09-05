package gg.zela.hub;

import org.bukkit.entity.Player;

/** A rank read from config.yml. Matched by permission node, first match wins. */
public record Rank(String id, String permission, String prefix, String name, int weight) {

    public boolean matches(Player player) {
        if (permission == null || permission.isEmpty()) return true;   // default rank
        return player.hasPermission(permission);
    }

    /** Four-digit sort key so tab ordering is stable. */
    public String sortKey() {
        return String.format("%04d", Math.max(0, Math.min(9999, weight)));
    }
}
