package dev.chatplus.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;

public final class Text {
    private static final Pattern HEX_COLOR = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final LegacyComponentSerializer LEGACY_COMPONENTS = LegacyComponentSerializer.legacySection();

    private Text() {
    }

    public static String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', replaceHex(text == null ? "" : text));
    }

    public static String strip(String text) {
        String stripped = ChatColor.stripColor(color(text));
        return stripped == null ? "" : stripped;
    }

    public static String oneLine(String text) {
        return strip(text).replace('\r', ' ').replace('\n', ' ').trim();
    }

    public static Component component(String text) {
        return LEGACY_COMPONENTS.deserialize(color(text));
    }

    private static String replaceHex(String text) {
        Matcher matcher = HEX_COLOR.matcher(text);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String hex = matcher.group(1);
            StringBuilder replacement = new StringBuilder(String.valueOf(ChatColor.COLOR_CHAR)).append('x');
            for (char character : hex.toCharArray()) {
                replacement.append(ChatColor.COLOR_CHAR).append(character);
            }
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement.toString()));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }
}
