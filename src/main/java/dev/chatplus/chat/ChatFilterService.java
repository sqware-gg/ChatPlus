package dev.chatplus.chat;

import dev.chatplus.config.ChatPlusConfig;
import dev.chatplus.settings.PlayerChatSettings;
import dev.chatplus.settings.PlayerSettingsStore;
import dev.chatplus.util.Text;
import java.util.Locale;
import org.bukkit.entity.Player;

public final class ChatFilterService {
    private final ChatPlusConfig config;
    private final PlayerSettingsStore settingsStore;

    public ChatFilterService(ChatPlusConfig config, PlayerSettingsStore settingsStore) {
        this.config = config;
        this.settingsStore = settingsStore;
    }

    public boolean shouldReceiveRegularChat(Player sender, Player recipient, String plainMessage) {
        if (sender.getUniqueId().equals(recipient.getUniqueId()) && config.alwaysShowOwnChat()) {
            return true;
        }
        if (config.senderBypassPermission() && sender.hasPermission("chatplus.bypass")) {
            return true;
        }
        ChatCategory category = isMention(plainMessage, recipient) ? ChatCategory.MENTION : ChatCategory.REGULAR_CHAT;
        return allows(recipient, category);
    }

    public boolean allows(Player recipient, ChatCategory category) {
        PlayerChatSettings settings = settingsStore.settings(recipient.getUniqueId());
        return settings.allows(category, config.modeAllows(settings.mode(), category));
    }

    public boolean isMention(String plainMessage, Player recipient) {
        if (!config.mentionsEnabled() || plainMessage == null || plainMessage.isBlank()) {
            return false;
        }
        String message = config.mentionCaseSensitive() ? plainMessage : plainMessage.toLowerCase(Locale.ROOT);
        String playerName = config.mentionCaseSensitive() ? recipient.getName() : recipient.getName().toLowerCase(Locale.ROOT);
        String strippedDisplayName = Text.strip(recipient.getDisplayName());
        String displayName = config.mentionCaseSensitive()
                ? strippedDisplayName
                : strippedDisplayName.toLowerCase(Locale.ROOT);

        if (containsToken(message, config.mentionPrefix() + playerName)
                || containsToken(message, config.mentionPrefix() + displayName)) {
            return true;
        }
        return config.mentionMatchWithoutPrefix()
                && (containsToken(message, playerName) || containsToken(message, displayName));
    }

    private boolean containsToken(String message, String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        int index = message.indexOf(token);
        while (index >= 0) {
            boolean leftOk = index == 0 || !isNameCharacter(message.charAt(index - 1));
            int end = index + token.length();
            boolean rightOk = end >= message.length() || !isNameCharacter(message.charAt(end));
            if (leftOk && rightOk) {
                return true;
            }
            index = message.indexOf(token, index + 1);
        }
        return false;
    }

    private boolean isNameCharacter(char character) {
        return Character.isLetterOrDigit(character) || character == '_';
    }
}
