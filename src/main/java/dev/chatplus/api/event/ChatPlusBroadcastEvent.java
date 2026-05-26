package dev.chatplus.api.event;

import dev.chatplus.chat.ChatCategory;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public final class ChatPlusBroadcastEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final ChatCategory category;
    private final String categoryKey;
    private final String categoryLabel;
    private final String message;
    private final int recipients;

    public ChatPlusBroadcastEvent(ChatCategory category, String message, int recipients) {
        this.category = category;
        this.categoryKey = category.key();
        this.categoryLabel = category.label();
        this.message = message == null ? "" : message;
        this.recipients = recipients;
    }

    public ChatCategory category() {
        return category;
    }

    public String categoryKey() {
        return categoryKey;
    }

    public String categoryLabel() {
        return categoryLabel;
    }

    public String message() {
        return message;
    }

    public int recipients() {
        return recipients;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
