package dev.chatplus.settings;

import dev.chatplus.chat.ChatCategory;
import dev.chatplus.chat.ChatMode;
import java.util.EnumSet;
import java.util.Set;

public final class PlayerChatSettings {
    private ChatMode mode;
    private boolean mentionsEnabled;
    private final EnumSet<ChatCategory> allowedOverrides;
    private final EnumSet<ChatCategory> blockedOverrides;

    public PlayerChatSettings(ChatMode mode, boolean mentionsEnabled,
                              Set<ChatCategory> allowedOverrides, Set<ChatCategory> blockedOverrides) {
        this.mode = mode;
        this.mentionsEnabled = mentionsEnabled;
        this.allowedOverrides = allowedOverrides.isEmpty()
                ? EnumSet.noneOf(ChatCategory.class)
                : EnumSet.copyOf(allowedOverrides);
        this.blockedOverrides = blockedOverrides.isEmpty()
                ? EnumSet.noneOf(ChatCategory.class)
                : EnumSet.copyOf(blockedOverrides);
    }

    public synchronized ChatMode mode() {
        return mode;
    }

    public synchronized void mode(ChatMode mode) {
        this.mode = mode;
    }

    public synchronized boolean mentionsEnabled() {
        return mentionsEnabled;
    }

    public synchronized void mentionsEnabled(boolean mentionsEnabled) {
        this.mentionsEnabled = mentionsEnabled;
    }

    public synchronized Set<ChatCategory> allowedOverrides() {
        return allowedOverrides.isEmpty()
                ? EnumSet.noneOf(ChatCategory.class)
                : EnumSet.copyOf(allowedOverrides);
    }

    public synchronized Set<ChatCategory> blockedOverrides() {
        return blockedOverrides.isEmpty()
                ? EnumSet.noneOf(ChatCategory.class)
                : EnumSet.copyOf(blockedOverrides);
    }

    public synchronized void allow(ChatCategory category) {
        blockedOverrides.remove(category);
        allowedOverrides.add(category);
    }

    public synchronized void block(ChatCategory category) {
        allowedOverrides.remove(category);
        blockedOverrides.add(category);
    }

    public synchronized void reset(ChatCategory category) {
        allowedOverrides.remove(category);
        blockedOverrides.remove(category);
    }

    public synchronized void resetAll(ChatMode defaultMode) {
        mode = defaultMode;
        mentionsEnabled = true;
        allowedOverrides.clear();
        blockedOverrides.clear();
    }

    public synchronized boolean allows(ChatCategory category, boolean modeDefault) {
        if (category == ChatCategory.MENTION && !mentionsEnabled) {
            return false;
        }
        if (allowedOverrides.contains(category)) {
            return true;
        }
        if (blockedOverrides.contains(category)) {
            return false;
        }
        return modeDefault;
    }
}
