package dev.chatplus.command;

import dev.chatplus.chat.ChatCategory;
import dev.chatplus.chat.ChatMode;
import dev.chatplus.chat.NotificationService;
import dev.chatplus.config.ChatPlusConfig;
import dev.chatplus.settings.PlayerChatSettings;
import dev.chatplus.settings.PlayerSettingsStore;
import dev.chatplus.util.Text;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public final class ChatAdminCommand implements CommandExecutor, TabCompleter {
    private final ChatPlusConfig config;
    private final PlayerSettingsStore settingsStore;
    private final NotificationService notificationService;

    public ChatAdminCommand(ChatPlusConfig config, PlayerSettingsStore settingsStore, NotificationService notificationService) {
        this.config = config;
        this.settingsStore = settingsStore;
        this.notificationService = notificationService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (!canAdmin(sender) && !canNotify(sender)) {
                message(sender, "&cNo permission.");
                return true;
            }
            help(sender);
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "reload" -> {
                if (requireAdmin(sender)) {
                    reload(sender);
                }
            }
            case "set" -> {
                if (requireAdmin(sender)) {
                    set(sender, args);
                }
            }
            case "reset" -> {
                if (requireAdmin(sender)) {
                    reset(sender, args);
                }
            }
            case "status" -> {
                if (requireAdmin(sender)) {
                    status(sender, args);
                }
            }
            case "notify" -> {
                if (requireNotify(sender)) {
                    notify(sender, args);
                }
            }
            case "broadcast" -> {
                if (requireNotify(sender)) {
                    broadcast(sender, args);
                }
            }
            default -> help(sender);
        }
        return true;
    }

    private boolean requireAdmin(CommandSender sender) {
        if (canAdmin(sender)) {
            return true;
        }
        message(sender, "&cNo permission.");
        return false;
    }

    private boolean requireNotify(CommandSender sender) {
        if (canNotify(sender)) {
            return true;
        }
        message(sender, "&cNo permission.");
        return false;
    }

    private boolean canAdmin(CommandSender sender) {
        return sender.hasPermission("chatplus.admin");
    }

    private boolean canNotify(CommandSender sender) {
        return sender.hasPermission("chatplus.admin") || sender.hasPermission("chatplus.notify");
    }

    private void reload(CommandSender sender) {
        config.reload();
        settingsStore.reload();
        message(sender, "&7ChatPlus reloaded.");
    }

    private void set(CommandSender sender, String[] args) {
        if (args.length < 3) {
            message(sender, "&cUsage: /chatadmin set <player> <mode>");
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            message(sender, "&cPlayer is not online.");
            return;
        }
        ChatMode mode = ChatMode.from(args[2]).orElse(null);
        if (mode == null) {
            message(sender, "&cUnknown mode.");
            return;
        }
        PlayerChatSettings settings = settingsStore.settings(target.getUniqueId());
        settings.mode(mode);
        settingsStore.save();
        message(sender, "&7Set &f" + target.getName() + "&7 to &f" + mode.label() + "&7.");
    }

    private void reset(CommandSender sender, String[] args) {
        if (args.length < 2) {
            message(sender, "&cUsage: /chatadmin reset <player>");
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            message(sender, "&cPlayer is not online.");
            return;
        }
        settingsStore.reset(target.getUniqueId());
        message(sender, "&7Reset ChatPlus preferences for &f" + target.getName() + "&7.");
    }

    private void status(CommandSender sender, String[] args) {
        if (args.length < 2) {
            message(sender, "&cUsage: /chatadmin status <player>");
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            message(sender, "&cPlayer is not online.");
            return;
        }
        PlayerChatSettings settings = settingsStore.settings(target.getUniqueId());
        message(sender, "&7" + target.getName() + ": mode &f" + settings.mode().label()
                + "&7, mentions &f" + (settings.mentionsEnabled() ? "on" : "off") + "&7.");
    }

    private void notify(CommandSender sender, String[] args) {
        if (args.length < 4) {
            message(sender, "&cUsage: /chatadmin notify <player> <category> <message>");
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            message(sender, "&cPlayer is not online.");
            return;
        }
        ChatCategory category = ChatCategory.from(args[2]).orElse(null);
        if (category == null) {
            message(sender, "&cUnknown category.");
            return;
        }
        boolean sent = notificationService.send(target, category, joinArgs(args, 3));
        message(sender, sent ? "&7Sent categorized notification." : "&7Target currently blocks that category.");
    }

    private void broadcast(CommandSender sender, String[] args) {
        if (args.length < 3) {
            message(sender, "&cUsage: /chatadmin broadcast <category> <message>");
            return;
        }
        ChatCategory category = ChatCategory.from(args[1]).orElse(null);
        if (category == null) {
            message(sender, "&cUnknown category.");
            return;
        }
        int sent = notificationService.broadcast(category, joinArgs(args, 2));
        message(sender, "&7Sent categorized broadcast to &f" + sent + "&7 players.");
    }

    private void help(CommandSender sender) {
        message(sender, "&7Commands: &f/chatadmin reload&7, &f/chatadmin set <player> <mode>&7, &f/chatadmin reset <player>");
        message(sender, "&7Notify: &f/chatadmin notify <player> <category> <message>&7, &f/chatadmin broadcast <category> <message>");
    }

    private void message(CommandSender sender, String message) {
        sender.sendMessage(Text.color(config.messagePrefix() + message));
    }

    private String joinArgs(String[] args, int start) {
        StringBuilder builder = new StringBuilder();
        for (int index = start; index < args.length; index++) {
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(args[index]);
        }
        return builder.toString();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!canAdmin(sender) && !canNotify(sender)) {
            return List.of();
        }
        if (args.length == 1) {
            List<String> options = new ArrayList<>();
            if (canAdmin(sender)) {
                options.addAll(List.of("reload", "set", "reset", "status"));
            }
            if (canNotify(sender)) {
                options.addAll(List.of("notify", "broadcast"));
            }
            return filter(options, args[0]);
        }
        if (args.length == 2 && List.of("set", "reset", "status", "notify").contains(args[0].toLowerCase(Locale.ROOT))) {
            return filter(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList(), args[1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("set")) {
            return filter(Arrays.stream(ChatMode.values()).map(ChatMode::key).toList(), args[2]);
        }
        if ((args.length == 3 && args[0].equalsIgnoreCase("notify"))
                || (args.length == 2 && args[0].equalsIgnoreCase("broadcast"))) {
            return filter(new ArrayList<>(Arrays.stream(ChatCategory.values()).map(ChatCategory::key).toList()),
                    args[args.length - 1]);
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
