package dev.chatplus.command;

import dev.chatplus.chat.ChatCategory;
import dev.chatplus.chat.ChatMode;
import dev.chatplus.config.ChatPlusConfig;
import dev.chatplus.settings.PlayerChatSettings;
import dev.chatplus.settings.PlayerSettingsStore;
import dev.chatplus.util.Text;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public final class ChatCommand implements CommandExecutor, TabCompleter {
    private final ChatPlusConfig config;
    private final PlayerSettingsStore settingsStore;

    public ChatCommand(ChatPlusConfig config, PlayerSettingsStore settingsStore) {
        this.config = config;
        this.settingsStore = settingsStore;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("chatplus.command")) {
            message(sender, "&#ED4245No permission.");
            return true;
        }
        if (!(sender instanceof Player player)) {
            message(sender, "&#ED4245Only players can manage chat preferences.");
            return true;
        }
        if (args.length == 0) {
            status(player, player);
            help(player);
            return true;
        }

        String subCommand = args[0].toLowerCase(Locale.ROOT);
        if (setMode(player, subCommand)) {
            return true;
        }

        switch (subCommand) {
            case "mode" -> {
                if (args.length < 2 || !setMode(player, args[1])) {
                    message(player, "&7Usage: &#2b98fd/chat mode <normal|quiet|focus|off>");
                }
            }
            case "allow" -> category(player, args, true);
            case "block" -> category(player, args, false);
            case "reset" -> reset(player, args);
            case "mentions" -> mentions(player, args);
            case "status" -> status(player, player);
            case "categories" -> categories(player);
            default -> help(player);
        }
        return true;
    }

    private boolean setMode(Player player, String value) {
        ChatMode mode = ChatMode.from(value).orElse(null);
        if (mode == null) {
            return false;
        }
        PlayerChatSettings settings = settingsStore.settings(player.getUniqueId());
        settings.mode(mode);
        settingsStore.save();
        message(player, render(config.modeChangedMessage(), "{mode}", mode.label()));
        return true;
    }

    private void category(Player player, String[] args, boolean allow) {
        if (args.length < 2) {
            message(player, allow ? "&7Usage: &#2b98fd/chat allow <category>" : "&7Usage: &#2b98fd/chat block <category>");
            return;
        }
        ChatCategory category = ChatCategory.from(args[1]).orElse(null);
        if (category == null) {
            message(player, "&#ED4245Unknown category. &7Use &#2b98fd/chat categories&7.");
            return;
        }
        PlayerChatSettings settings = settingsStore.settings(player.getUniqueId());
        if (allow) {
            settings.allow(category);
            message(player, render(config.categoryAllowedMessage(), "{category}", category.label()));
        } else {
            settings.block(category);
            message(player, render(config.categoryBlockedMessage(), "{category}", category.label()));
        }
        settingsStore.save();
    }

    private void reset(Player player, String[] args) {
        PlayerChatSettings settings = settingsStore.settings(player.getUniqueId());
        if (args.length >= 2 && !"all".equalsIgnoreCase(args[1])) {
            ChatCategory category = ChatCategory.from(args[1]).orElse(null);
            if (category == null) {
                message(player, "&#ED4245Unknown category. &7Use &#2b98fd/chat categories&7.");
                return;
            }
            settings.reset(category);
            settingsStore.save();
            message(player, render(config.categoryResetMessage(), "{category}", category.label()));
            return;
        }
        settings.resetAll(config.defaultMode());
        settingsStore.save();
        message(player, config.settingsResetMessage());
    }

    private void mentions(Player player, String[] args) {
        if (args.length < 2) {
            message(player, "&7Usage: &#2b98fd/chat mentions <on|off>");
            return;
        }
        PlayerChatSettings settings = settingsStore.settings(player.getUniqueId());
        boolean enabled = args[1].equalsIgnoreCase("on") || args[1].equalsIgnoreCase("true");
        settings.mentionsEnabled(enabled);
        settingsStore.save();
        message(player, enabled ? config.mentionsOnMessage() : config.mentionsOffMessage());
    }

    private void status(CommandSender sender, Player target) {
        PlayerChatSettings settings = settingsStore.settings(target.getUniqueId());
        message(sender, "&7Mode: &#2b98fd" + settings.mode().label() + "&7. Mentions: &f"
                + (settings.mentionsEnabled() ? "on" : "off") + "&7.");
        List<String> allowed = Arrays.stream(ChatCategory.values())
                .filter(category -> settings.allows(category, config.modeAllows(settings.mode(), category)))
                .map(ChatCategory::label)
                .toList();
        message(sender, "&7Allowed now: &f" + String.join("&7, &f", allowed));
    }

    private void categories(CommandSender sender) {
        message(sender, "&7Categories: &f" + String.join("&7, &f",
                Arrays.stream(ChatCategory.values()).map(ChatCategory::key).toList()));
    }

    private void help(CommandSender sender) {
        message(sender, "&7Commands: &#2b98fd/chat normal&7, &#2b98fd/chat quiet&7, &#2b98fd/chat focus&7, &#2b98fd/chat off");
        message(sender, "&7Prefs: &#2b98fd/chat allow <category>&7, &#2b98fd/chat block <category>&7, &#2b98fd/chat reset [category|all]&7, &#2b98fd/chat mentions <on|off>");
    }

    private String render(String template, String placeholder, String value) {
        return template.replace(placeholder, value);
    }

    private void message(CommandSender sender, String message) {
        sender.sendMessage(Text.color(config.messagePrefix() + message));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> options = new ArrayList<>(List.of("normal", "quiet", "focus", "off",
                    "mode", "allow", "block", "reset", "mentions", "status", "categories"));
            return filter(options, args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("mode")) {
            return filter(Arrays.stream(ChatMode.values()).map(ChatMode::key).toList(), args[1]);
        }
        if (args.length == 2 && List.of("allow", "block", "reset").contains(args[0].toLowerCase(Locale.ROOT))) {
            List<String> categories = new ArrayList<>(Arrays.stream(ChatCategory.values()).map(ChatCategory::key).toList());
            if (args[0].equalsIgnoreCase("reset")) {
                categories.add("all");
            }
            return filter(categories, args[1]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("mentions")) {
            return filter(List.of("on", "off"), args[1]);
        }
        return List.of();
    }

    private List<String> filter(List<String> values, String prefix) {
        String normalized = prefix.toLowerCase(Locale.ROOT);
        return values.stream()
                .filter(value -> value.toLowerCase(Locale.ROOT).startsWith(normalized))
                .toList();
    }
}
