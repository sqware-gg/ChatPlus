package dev.chatplus.util;

import org.bukkit.ChatColor;

public final class Text {
    private Text() {
    }

    public static String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text == null ? "" : text);
    }

    public static String strip(String text) {
        String stripped = ChatColor.stripColor(color(text));
        return stripped == null ? "" : stripped;
    }

    public static String oneLine(String text) {
        return strip(text).replace('\r', ' ').replace('\n', ' ').trim();
    }
}
