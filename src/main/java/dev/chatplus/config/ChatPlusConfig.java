package dev.chatplus.config;

import dev.chatplus.chat.ChatCategory;
import dev.chatplus.chat.ChatMode;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class ChatPlusConfig {
    private final JavaPlugin plugin;
    private ChatMode defaultMode;
    private boolean alwaysShowOwnChat;
    private boolean senderBypassPermission;
    private boolean mentionsEnabled;
    private String mentionPrefix;
    private boolean mentionCaseSensitive;
    private boolean mentionMatchWithoutPrefix;
    private EnumMap<ChatMode, EnumSet<ChatCategory>> modeDefaults;
    private String notificationFormat;
    private String broadcastFormat;
    private String messagePrefix;
    private String modeChangedMessage;
    private String categoryAllowedMessage;
    private String categoryBlockedMessage;
    private String categoryResetMessage;
    private String settingsResetMessage;
    private String mentionsOnMessage;
    private String mentionsOffMessage;

    public ChatPlusConfig(JavaPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();
        defaultMode = ChatMode.from(config.getString("defaults.mode", "normal")).orElse(ChatMode.NORMAL);
        alwaysShowOwnChat = config.getBoolean("defaults.always-show-own-chat", true);
        senderBypassPermission = config.getBoolean("defaults.sender-bypass-permission", true);
        mentionsEnabled = config.getBoolean("mentions.enabled", true);
        mentionPrefix = config.getString("mentions.prefix", "@");
        if (mentionPrefix == null || mentionPrefix.isBlank()) {
            mentionPrefix = "@";
        }
        mentionCaseSensitive = config.getBoolean("mentions.case-sensitive", false);
        mentionMatchWithoutPrefix = config.getBoolean("mentions.match-without-prefix", false);
        modeDefaults = readModeDefaults(config);
        notificationFormat = string(config.getString("notifications.format"), "&8[&b{category_label}&8] &f{message}");
        broadcastFormat = string(config.getString("notifications.broadcast-format"), "&8[&b{category_label}&8] &f{message}");
        messagePrefix = string(config.getString("messages.prefix"), "&bChat+ &8> &f");
        modeChangedMessage = string(config.getString("messages.mode-changed"), "&7Chat mode set to &f{mode}&7.");
        categoryAllowedMessage = string(config.getString("messages.category-allowed"), "&7Allowed &f{category}&7.");
        categoryBlockedMessage = string(config.getString("messages.category-blocked"), "&7Blocked &f{category}&7.");
        categoryResetMessage = string(config.getString("messages.category-reset"), "&7Reset &f{category}&7 to your mode default.");
        settingsResetMessage = string(config.getString("messages.settings-reset"), "&7Your ChatPlus preferences were reset.");
        mentionsOnMessage = string(config.getString("messages.mentions-on"), "&7Mentions are now &fenabled&7.");
        mentionsOffMessage = string(config.getString("messages.mentions-off"), "&7Mentions are now &fdisabled&7.");
    }

    private EnumMap<ChatMode, EnumSet<ChatCategory>> readModeDefaults(FileConfiguration config) {
        EnumMap<ChatMode, EnumSet<ChatCategory>> defaults = new EnumMap<>(ChatMode.class);
        for (ChatMode mode : ChatMode.values()) {
            EnumSet<ChatCategory> allowed = EnumSet.noneOf(ChatCategory.class);
            for (ChatCategory category : ChatCategory.values()) {
                boolean fallback = fallbackAllows(mode, category);
                if (config.getBoolean("modes." + mode.key() + "." + category.key(), fallback)) {
                    allowed.add(category);
                }
            }
            defaults.put(mode, allowed);
        }
        return defaults;
    }

    private boolean fallbackAllows(ChatMode mode, ChatCategory category) {
        if (mode == ChatMode.NORMAL) {
            return true;
        }
        if (mode == ChatMode.OFF && category == ChatCategory.MENTION) {
            return false;
        }
        return category != ChatCategory.REGULAR_CHAT;
    }

    private String string(String value, String fallback) {
        return value == null ? fallback : value;
    }

    public boolean modeAllows(ChatMode mode, ChatCategory category) {
        return modeDefaults.getOrDefault(mode, EnumSet.noneOf(ChatCategory.class)).contains(category);
    }

    public Set<ChatCategory> modeAllowedCategories(ChatMode mode) {
        EnumSet<ChatCategory> allowed = modeDefaults.getOrDefault(mode, EnumSet.noneOf(ChatCategory.class));
        return allowed.isEmpty() ? EnumSet.noneOf(ChatCategory.class) : EnumSet.copyOf(allowed);
    }

    public ChatMode defaultMode() {
        return defaultMode;
    }

    public boolean alwaysShowOwnChat() {
        return alwaysShowOwnChat;
    }

    public boolean senderBypassPermission() {
        return senderBypassPermission;
    }

    public boolean mentionsEnabled() {
        return mentionsEnabled;
    }

    public String mentionPrefix() {
        return mentionPrefix;
    }

    public boolean mentionCaseSensitive() {
        return mentionCaseSensitive;
    }

    public boolean mentionMatchWithoutPrefix() {
        return mentionMatchWithoutPrefix;
    }

    public String notificationFormat() {
        return notificationFormat;
    }

    public String broadcastFormat() {
        return broadcastFormat;
    }

    public String messagePrefix() {
        return messagePrefix;
    }

    public String modeChangedMessage() {
        return modeChangedMessage;
    }

    public String categoryAllowedMessage() {
        return categoryAllowedMessage;
    }

    public String categoryBlockedMessage() {
        return categoryBlockedMessage;
    }

    public String categoryResetMessage() {
        return categoryResetMessage;
    }

    public String settingsResetMessage() {
        return settingsResetMessage;
    }

    public String mentionsOnMessage() {
        return mentionsOnMessage;
    }

    public String mentionsOffMessage() {
        return mentionsOffMessage;
    }

    public Map<ChatMode, EnumSet<ChatCategory>> modeDefaults() {
        return new EnumMap<>(modeDefaults);
    }
}
