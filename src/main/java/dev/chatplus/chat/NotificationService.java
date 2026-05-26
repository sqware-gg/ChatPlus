package dev.chatplus.chat;

import dev.chatplus.api.event.ChatPlusBroadcastEvent;
import dev.chatplus.config.ChatPlusConfig;
import dev.chatplus.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class NotificationService {
    private final ChatPlusConfig config;
    private final ChatFilterService filterService;

    public NotificationService(ChatPlusConfig config, ChatFilterService filterService) {
        this.config = config;
        this.filterService = filterService;
    }

    public boolean send(Player player, ChatCategory category, String message) {
        if (player == null || category == null || message == null || message.isBlank()) {
            return false;
        }
        if (!filterService.allows(player, category)) {
            return false;
        }
        player.sendMessage(Text.color(render(config.notificationFormat(), category, message)));
        return true;
    }

    public int broadcast(ChatCategory category, String message) {
        int sent = 0;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (sendBroadcast(player, category, message)) {
                sent++;
            }
        }
        Bukkit.getPluginManager().callEvent(new ChatPlusBroadcastEvent(category, message, sent));
        return sent;
    }

    private boolean sendBroadcast(Player player, ChatCategory category, String message) {
        if (!filterService.allows(player, category)) {
            return false;
        }
        player.sendMessage(Text.color(render(config.broadcastFormat(), category, message)));
        return true;
    }

    private String render(String template, ChatCategory category, String message) {
        return template.replace("{category}", category.key())
                .replace("{category_label}", category.label())
                .replace("{message}", message);
    }
}
