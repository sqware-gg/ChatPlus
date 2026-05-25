package dev.chatplus.api;

import dev.chatplus.chat.ChatCategory;
import dev.chatplus.chat.NotificationService;
import org.bukkit.entity.Player;

public final class ChatPlusApi {
    private static NotificationService notificationService;

    private ChatPlusApi() {
    }

    public static boolean send(Player player, ChatCategory category, String message) {
        return notificationService != null && notificationService.send(player, category, message);
    }

    public static boolean send(Player player, String category, String message) {
        return ChatCategory.from(category)
                .map(parsed -> send(player, parsed, message))
                .orElse(false);
    }

    public static int broadcast(ChatCategory category, String message) {
        return notificationService == null ? 0 : notificationService.broadcast(category, message);
    }

    public static int broadcast(String category, String message) {
        return ChatCategory.from(category)
                .map(parsed -> broadcast(parsed, message))
                .orElse(0);
    }

    public static void register(NotificationService service) {
        notificationService = service;
    }

    public static void unregister() {
        notificationService = null;
    }
}
