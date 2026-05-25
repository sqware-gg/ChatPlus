package dev.chatplus.chat;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

public enum ChatCategory {
    REGULAR_CHAT("regular-chat", "Regular Chat"),
    MENTION("mentions", "Mentions"),
    PRIVATE_MESSAGE("private-message", "Private Messages"),
    TELEPORT_REQUEST("teleport-request", "Teleport Requests"),
    SERVER_NOTICE("server-notice", "Server Notices"),
    STAFF_NOTICE("staff-notice", "Staff Notices"),
    PURCHASE("purchase", "Purchases"),
    WARNING("warning", "Warnings");

    private final String key;
    private final String label;

    ChatCategory(String key, String label) {
        this.key = key;
        this.label = label;
    }

    public String key() {
        return key;
    }

    public String label() {
        return label;
    }

    public static Optional<ChatCategory> from(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT).replace('_', '-');
        return Arrays.stream(values())
                .filter(category -> category.key.equals(normalized)
                        || category.name().toLowerCase(Locale.ROOT).replace('_', '-').equals(normalized))
                .findFirst();
    }
}
