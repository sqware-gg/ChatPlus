package dev.chatplus.api;

import dev.chatplus.chat.ChatCategory;
import dev.chatplus.chat.ItemShareService;
import dev.chatplus.chat.NotificationService;
import org.bukkit.entity.Player;

public final class ChatPlusApi {
    private static NotificationService notificationService;
    private static ItemShareService itemShareService;

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

    public static String renderDiscordChat(Player player, String message) {
        return itemShareService == null ? message : itemShareService.renderDiscord(player, message);
    }

    public static boolean hasInteractivePlaceholders(String message) {
        return itemShareService != null && itemShareService.hasPlaceholders(message);
    }

    public static void register(NotificationService service, ItemShareService shareService) {
        notificationService = service;
        itemShareService = shareService;
    }

    public static void register(NotificationService service) {
        register(service, null);
    }

    public static void unregister() {
        notificationService = null;
        itemShareService = null;
    }
}
