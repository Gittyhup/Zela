package gg.zela.hub.menu;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/** Marks an inventory as one of ours and remembers what each slot does. */
public class MenuHolder implements InventoryHolder {

    private final String name;
    private final Map<Integer, String> actions = new HashMap<>();
    private Inventory inventory;

    public MenuHolder(String name) {
        this.name = name;
    }

    public String name() {
        return name;
    }

    public Map<Integer, String> actions() {
        return actions;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
