package gg.zela.hub;

import org.bukkit.Particle;

/** Per-player toggles. Lives in memory only — resets on rejoin, which is fine for a lobby. */
public class PlayerState {

    public boolean playersVisible = true;
    public boolean scoreboardVisible = true;
    public boolean doubleJumpEnabled = true;
    public Particle trail = null;          // null = no trail

    public static Particle trailFor(String id) {
        if (id == null) return null;
        return switch (id.toLowerCase()) {
            case "zela"  -> Particle.WITCH;
            case "flame" -> Particle.FLAME;
            case "happy" -> Particle.HAPPY_VILLAGER;
            case "none"  -> null;
            default      -> null;
        };
    }
}
