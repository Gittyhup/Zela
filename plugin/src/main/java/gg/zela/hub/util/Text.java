package gg.zela.hub.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** MiniMessage helpers. Everything the plugin prints goes through here. */
public final class Text {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private Text() {
    }

    /** Parse MiniMessage, replacing &lt;key&gt; placeholders first. */
    public static Component of(String raw, Map<String, String> placeholders) {
        String s = raw == null ? "" : raw;
        if (placeholders != null) {
            for (Map.Entry<String, String> e : placeholders.entrySet()) {
                s = s.replace("<" + e.getKey() + ">", e.getValue() == null ? "" : e.getValue());
            }
        }
        return MM.deserialize(s);
    }

    public static Component of(String raw) {
        return of(raw, null);
    }

    /** Same as {@link #of} but with italics switched off — for item names and lore. */
    public static Component item(String raw, Map<String, String> placeholders) {
        return of(raw, placeholders).decoration(TextDecoration.ITALIC, false);
    }

    public static Component item(String raw) {
        return item(raw, null);
    }

    public static List<Component> items(List<String> raw, Map<String, String> placeholders) {
        List<Component> out = new ArrayList<>();
        if (raw == null) return out;
        for (String line : raw) out.add(item(line, placeholders));
        return out;
    }

    /** Join a list of MiniMessage lines into one component separated by newlines. */
    public static Component lines(List<String> raw, Map<String, String> placeholders) {
        Component out = Component.empty();
        if (raw == null || raw.isEmpty()) return out;
        for (int i = 0; i < raw.size(); i++) {
            if (i > 0) out = out.append(Component.newline());
            out = out.append(of(raw.get(i), placeholders));
        }
        return out;
    }
}
