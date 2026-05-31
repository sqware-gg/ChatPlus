package dev.chatplus.config;

import dev.chatplus.chat.ChatCategory;
import dev.chatplus.chat.ChatMode;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
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
    private ItemShareSettings itemShare;
    private ViewShareSettings inventoryShare;
    private ViewShareSettings enderChestShare;

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
        notificationFormat = string(config.getString("notifications.format"), "&#2b98fdChatPlus &8› &7{category_label} &8› &f{message}");
        broadcastFormat = string(config.getString("notifications.broadcast-format"), "&#2b98fdChatPlus &8› &7{category_label} &8› &f{message}");
        messagePrefix = string(config.getString("messages.prefix"), "&#2b98fdChatPlus &8› &7");
        modeChangedMessage = string(config.getString("messages.mode-changed"), "&7Chat mode set to &#2b98fd{mode}&7.");
        categoryAllowedMessage = string(config.getString("messages.category-allowed"), "&7Allowed &f{category}&7.");
        categoryBlockedMessage = string(config.getString("messages.category-blocked"), "&7Blocked &f{category}&7.");
        categoryResetMessage = string(config.getString("messages.category-reset"), "&7Reset &f{category}&7.");
        settingsResetMessage = string(config.getString("messages.settings-reset"), "&7Chat preferences reset.");
        mentionsOnMessage = string(config.getString("messages.mentions-on"), "&7Mentions are &#57F287enabled&7.");
        mentionsOffMessage = string(config.getString("messages.mentions-off"), "&7Mentions are &#ED4245disabled&7.");
        itemShare = readItemShare(config);
        inventoryShare = readViewShare(config, "inventory-share", "Inventory", List.of("[inventory]", "[inv]"),
                "chatplus.inventory", "&8[&#2b98fd{player}'s Inventory&8]",
                "[{player}'s Inventory]", "**{player}'s inventory** - {summary}",
                "&7Click to view &#2b98fd{player}'s inventory&7.", "&#2b98fd{player}'s Inventory");
        enderChestShare = readViewShare(config, "ender-chest-share", "Ender Chest", List.of("[enderchest]", "[ender]", "[ec]"),
                "chatplus.enderchest", "&8[&#b642ff{player}'s Ender Chest&8]",
                "[{player}'s Ender Chest]", "**{player}'s ender chest** - {summary}",
                "&7Click to view &#b642ff{player}'s ender chest&7.", "&#b642ff{player}'s Ender Chest");
    }

    private ItemShareSettings readItemShare(FileConfiguration config) {
        String base = "item-share.";
        return new ItemShareSettings(
                config.getBoolean(base + "enabled", true),
                config.getBoolean(base + "case-sensitive-placeholders", false),
                stringList(config, base + "main-hand-placeholders", List.of("[item]", "[i]", "[hand]")),
                stringList(config, base + "off-hand-placeholders", List.of("[offhand]", "[off]")),
                Math.max(0, config.getInt(base + "max-per-message", 1)),
                Math.max(0L, Math.round(config.getDouble(base + "cooldown-seconds", 5.0D) * 1000.0D)),
                string(config.getString(base + "permission"), "chatplus.item"),
                string(config.getString(base + "off-hand-permission"), "chatplus.item.offhand"),
                string(config.getString(base + "bypass-cooldown-permission"), "chatplus.item.bypass-cooldown"),
                string(config.getString(base + "display.format"), "&8[&f{name}{amount}&8]"),
                string(config.getString(base + "display.plain-format"), "[{name}{amount}]"),
                string(config.getString(base + "display.discord-format"), "**{name}{amount}**"),
                string(config.getString(base + "display.amount-format"), " &7x{amount}"),
                config.getBoolean(base + "display.show-amount", true),
                ItemClickAction.from(config.getString(base + "click.action", "suggest")),
                config.getBoolean(base + "click.shift-click-insertion", true),
                string(config.getString(base + "messages.no-permission"), "&#ED4245You cannot share items in chat."),
                string(config.getString(base + "messages.cooldown"), "&7Wait &#2b98fd{seconds}s &7before sharing another item."),
                string(config.getString(base + "messages.empty-hand"), "&#ED4245Hold an item before using &f[item]&#ED4245."),
                string(config.getString(base + "messages.too-many-items"), "&7You can share up to &#2b98fd{max} &7item per message."),
                string(config.getString(base + "messages.unavailable"), "&#ED4245Could not read your held item. Try again.")
        );
    }

    private ViewShareSettings readViewShare(
            FileConfiguration config,
            String section,
            String label,
            List<String> fallbackPlaceholders,
            String fallbackPermission,
            String fallbackDisplayFormat,
            String fallbackPlainFormat,
            String fallbackDiscordFormat,
            String fallbackHoverText,
            String fallbackTitle
    ) {
        String base = section + ".";
        return new ViewShareSettings(
                label,
                config.getBoolean(base + "enabled", true),
                stringList(config, base + "placeholders", fallbackPlaceholders),
                string(config.getString(base + "permission"), fallbackPermission),
                Math.max(15_000L, Math.round(config.getDouble(base + "expires-seconds", 120.0D) * 1000.0D)),
                string(config.getString(base + "display.format"), fallbackDisplayFormat),
                string(config.getString(base + "display.plain-format"), fallbackPlainFormat),
                string(config.getString(base + "display.discord-format"), fallbackDiscordFormat),
                string(config.getString(base + "display.hover-text"), fallbackHoverText),
                string(config.getString(base + "display.title"), fallbackTitle),
                string(config.getString(base + "messages.empty"), "&7That " + label.toLowerCase(Locale.ROOT) + " is empty."),
                string(config.getString(base + "messages.expired"), "&#ED4245That shared " + label.toLowerCase(Locale.ROOT) + " expired.")
        );
    }

    private List<String> stringList(FileConfiguration config, String path, List<String> fallback) {
        List<String> values = config.getStringList(path).stream()
                .filter(value -> value != null && !value.isBlank())
                .toList();
        return values.isEmpty() ? fallback : List.copyOf(values);
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

    public ItemShareSettings itemShare() {
        return itemShare;
    }

    public ViewShareSettings inventoryShare() {
        return inventoryShare;
    }

    public ViewShareSettings enderChestShare() {
        return enderChestShare;
    }

    public enum ItemClickAction {
        NONE,
        SUGGEST,
        COPY;

        public static ItemClickAction from(String value) {
            if (value == null || value.isBlank()) {
                return SUGGEST;
            }
            try {
                return ItemClickAction.valueOf(value.trim().toUpperCase(Locale.ROOT).replace('-', '_'));
            } catch (IllegalArgumentException ignored) {
                return SUGGEST;
            }
        }
    }

    public record ItemShareSettings(
            boolean enabled,
            boolean caseSensitive,
            List<String> mainHandPlaceholders,
            List<String> offHandPlaceholders,
            int maxPerMessage,
            long cooldownMillis,
            String permission,
            String offHandPermission,
            String bypassCooldownPermission,
            String displayFormat,
            String plainFormat,
            String discordFormat,
            String amountFormat,
            boolean showAmount,
            ItemClickAction clickAction,
            boolean shiftClickInsertion,
            String noPermissionMessage,
            String cooldownMessage,
            String emptyHandMessage,
            String tooManyItemsMessage,
            String unavailableMessage
    ) {
    }

    public record ViewShareSettings(
            String label,
            boolean enabled,
            List<String> placeholders,
            String permission,
            long expiresMillis,
            String displayFormat,
            String plainFormat,
            String discordFormat,
            String hoverText,
            String title,
            String emptyMessage,
            String expiredMessage
    ) {
    }
}
