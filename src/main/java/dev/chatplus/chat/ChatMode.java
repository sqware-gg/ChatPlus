package dev.chatplus.chat;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

public enum ChatMode {
    NORMAL("normal", "Normal"),
    QUIET("quiet", "Quiet"),
    FOCUS("focus", "Focus"),
    OFF("off", "Off");

    private final String key;
    private final String label;

    ChatMode(String key, String label) {
        this.key = key;
        this.label = label;
    }

    public String key() {
        return key;
    }

    public String label() {
        return label;
    }

    public static Optional<ChatMode> from(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT).replace('_', '-');
        return Arrays.stream(values())
                .filter(mode -> mode.key.equals(normalized)
                        || mode.name().toLowerCase(Locale.ROOT).replace('_', '-').equals(normalized))
                .findFirst();
    }
}
