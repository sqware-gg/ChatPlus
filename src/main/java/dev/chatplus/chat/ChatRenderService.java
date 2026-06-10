package dev.chatplus.chat;

import dev.chatplus.config.ChatPlusConfig;
import dev.chatplus.config.ChatPlusConfig.ChatClickAction;
import dev.chatplus.config.ChatPlusConfig.ChatFormatSettings;
import dev.chatplus.util.Text;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class ChatRenderService {
    private static final Pattern HEX_COLOR = Pattern.compile("&#[A-Fa-f0-9]{6}");
    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();

    private final JavaPlugin plugin;
    private final ChatPlusConfig config;
    private boolean warnedLuckPerms;
    private boolean warnedPlaceholderApi;

    public ChatRenderService(JavaPlugin plugin, ChatPlusConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    public ChatPlusConfig config() {
        return config;
    }

    public Component prepareMessage(Player player, Component currentMessage, String plainMessage, boolean alreadyRich) {
        ChatFormatSettings settings = config.chatFormat();
        if (alreadyRich || !settings.messageColorsEnabled() || !player.hasPermission(settings.legacyColorPermission())) {
            return currentMessage;
        }
        String rendered = plainMessage == null ? "" : plainMessage;
        if (!player.hasPermission(settings.hexColorPermission())) {
            rendered = HEX_COLOR.matcher(rendered).replaceAll("");
        }
        return Text.component(rendered);
    }

    public Component render(Player source, Component sourceDisplayName, Component message, Audience viewer) {
        ChatFormatSettings settings = config.chatFormat();
        ChatMeta meta = chatMeta(source, sourceDisplayName);
        String template = selectFormat(source, meta, settings);
        template = applyExternalPlaceholders(source, Text.render(template, meta.placeholders()));

        Map<String, Component> components = new LinkedHashMap<>();
        components.put("message", message);
        components.put("prefix", Text.component(applyExternalPlaceholders(source,
                Text.render(meta.prefix(), meta.placeholders()))));
        components.put("suffix", Text.component(applyExternalPlaceholders(source,
                Text.render(meta.suffix(), meta.placeholders()))));
        components.put("username", decorateName(Component.text(source.getName()), source, meta));
        components.put("name", components.get("username"));
        components.put("display_name", decorateName(meta.displayName(), source, meta));
        components.put("displayname", components.get("display_name"));
        components.put("world", Text.component(meta.world()));
        components.put("primary_group", Text.component(meta.primaryGroup()));
        components.put("group", components.get("primary_group"));

        return renderTemplate(template, components);
    }

    private String selectFormat(Player player, ChatMeta meta, ChatFormatSettings settings) {
        for (Map.Entry<String, String> entry : settings.permissionFormats().entrySet()) {
            if (player.hasPermission(entry.getKey())) {
                return entry.getValue();
            }
        }
        String groupFormat = settings.groupFormats().get(meta.primaryGroup().toLowerCase(Locale.ROOT));
        return groupFormat == null || groupFormat.isBlank() ? settings.format() : groupFormat;
    }

    private ChatMeta chatMeta(Player player, Component sourceDisplayName) {
        ChatFormatSettings settings = config.chatFormat();
        LuckPermsMeta luckPerms = luckPermsMeta(player);
        String primaryGroup = fallback(luckPerms.primaryGroup(), settings.fallbackPrimaryGroup());
        String prefix = fallback(luckPerms.prefix(), settings.fallbackPrefix());
        String suffix = fallback(luckPerms.suffix(), settings.fallbackSuffix());
        Component displayName = settings.usePaperDisplayName() && sourceDisplayName != null
                ? sourceDisplayName
                : Text.component(settings.fallbackNameColor() + player.getName());
        String displayNamePlain = PLAIN.serialize(displayName);

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("username", player.getName());
        placeholders.put("name", player.getName());
        placeholders.put("display_name", displayNamePlain);
        placeholders.put("displayname", displayNamePlain);
        placeholders.put("world", player.getWorld().getName());
        placeholders.put("primary_group", primaryGroup);
        placeholders.put("group", primaryGroup);
        placeholders.put("prefix", prefix);
        placeholders.put("suffix", suffix);

        return new ChatMeta(prefix, suffix, primaryGroup, player.getWorld().getName(), displayName, placeholders);
    }

    private Component decorateName(Component component, Player player, ChatMeta meta) {
        ChatFormatSettings settings = config.chatFormat();
        Component decorated = component;
        if (settings.playerHoverEnabled() && !settings.playerHoverLines().isEmpty()) {
            StringBuilder hover = new StringBuilder();
            for (String line : settings.playerHoverLines()) {
                String rendered = applyExternalPlaceholders(player, Text.render(line, meta.placeholders()));
                if (!rendered.isBlank()) {
                    if (!hover.isEmpty()) {
                        hover.append('\n');
                    }
                    hover.append(rendered);
                }
            }
            if (!hover.isEmpty()) {
                decorated = decorated.hoverEvent(HoverEvent.showText(Text.component(hover.toString())));
            }
        }

        String clickValue = applyExternalPlaceholders(player, Text.render(settings.playerClickValue(), meta.placeholders()));
        if (!clickValue.isBlank()) {
            decorated = switch (settings.playerClickAction()) {
                case NONE -> decorated;
                case SUGGEST -> decorated.clickEvent(ClickEvent.suggestCommand(clickValue));
                case RUN -> decorated.clickEvent(ClickEvent.runCommand(clickValue));
                case COPY -> decorated.clickEvent(ClickEvent.copyToClipboard(clickValue));
            };
        }
        return decorated;
    }

    private Component renderTemplate(String template, Map<String, Component> placeholders) {
        Component component = Component.empty();
        int cursor = 0;
        while (cursor < template.length()) {
            Match match = nextMatch(template, cursor, placeholders);
            if (match == null) {
                component = component.append(Text.component(template.substring(cursor)));
                break;
            }
            if (match.index() > cursor) {
                component = component.append(Text.component(template.substring(cursor, match.index())));
            }
            component = component.append(match.component());
            cursor = match.index() + match.token().length();
        }
        return component;
    }

    private Match nextMatch(String template, int cursor, Map<String, Component> placeholders) {
        Match best = null;
        for (Map.Entry<String, Component> entry : placeholders.entrySet()) {
            String token = "{" + entry.getKey() + "}";
            int index = template.indexOf(token, cursor);
            if (index < 0) {
                continue;
            }
            if (best == null || index < best.index()
                    || (index == best.index() && token.length() > best.token().length())) {
                best = new Match(index, token, entry.getValue());
            }
        }
        return best;
    }

    private String applyExternalPlaceholders(Player player, String value) {
        ChatFormatSettings settings = config.chatFormat();
        if (!settings.placeholderApiEnabled() || value == null || value.isBlank()
                || !Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            return value == null ? "" : value;
        }
        try {
            Class<?> placeholderApi = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
            Method method = placeholderApi.getMethod("setPlaceholders", OfflinePlayer.class, String.class);
            Object rendered = method.invoke(null, player, value);
            return rendered == null ? value : rendered.toString();
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException
                 | LinkageError e) {
            if (!warnedPlaceholderApi) {
                warnedPlaceholderApi = true;
                plugin.getLogger().warning("Could not apply PlaceholderAPI chat placeholders: " + e.getMessage());
            }
            return value;
        }
    }

    private LuckPermsMeta luckPermsMeta(Player player) {
        ChatFormatSettings settings = config.chatFormat();
        if (!settings.luckPermsEnabled() || !Bukkit.getPluginManager().isPluginEnabled("LuckPerms")) {
            return LuckPermsMeta.empty();
        }
        try {
            Class<?> luckPermsClass = Class.forName("net.luckperms.api.LuckPerms");
            RegisteredServiceProvider<?> registration = Bukkit.getServicesManager().getRegistration(luckPermsClass);
            if (registration == null) {
                return LuckPermsMeta.empty();
            }
            Object luckPerms = registration.getProvider();
            Object userManager = invoke(luckPerms, "getUserManager");
            Object user = invoke(userManager, "getUser", player.getUniqueId());
            if (user == null) {
                return LuckPermsMeta.empty();
            }
            String primaryGroup = stringValue(invoke(user, "getPrimaryGroup"));
            Object cachedData = invoke(user, "getCachedData");
            Object metaData = invoke(cachedData, "getMetaData");
            String prefix = stringValue(invoke(metaData, "getPrefix"));
            String suffix = stringValue(invoke(metaData, "getSuffix"));
            return new LuckPermsMeta(prefix, suffix, primaryGroup);
        } catch (ReflectiveOperationException | LinkageError e) {
            if (!warnedLuckPerms) {
                warnedLuckPerms = true;
                plugin.getLogger().warning("Could not read LuckPerms chat metadata: " + e.getMessage());
            }
            return LuckPermsMeta.empty();
        }
    }

    private Object invoke(Object target, String method, Object... arguments) throws ReflectiveOperationException {
        if (target == null) {
            return null;
        }
        Method match = null;
        for (Method candidate : target.getClass().getMethods()) {
            if (candidate.getName().equals(method) && compatible(candidate.getParameterTypes(), arguments)) {
                match = candidate;
                break;
            }
        }
        if (match == null) {
            throw new NoSuchMethodException(target.getClass().getName() + "." + method);
        }
        return match.invoke(target, arguments);
    }

    private boolean compatible(Class<?>[] parameterTypes, Object[] arguments) {
        if (parameterTypes.length != arguments.length) {
            return false;
        }
        for (int index = 0; index < parameterTypes.length; index++) {
            Object argument = arguments[index];
            if (argument != null && !wrap(parameterTypes[index]).isInstance(argument)) {
                return false;
            }
        }
        return true;
    }

    private Class<?> wrap(Class<?> type) {
        if (!type.isPrimitive()) {
            return type;
        }
        if (type == boolean.class) {
            return Boolean.class;
        }
        if (type == int.class) {
            return Integer.class;
        }
        if (type == long.class) {
            return Long.class;
        }
        if (type == double.class) {
            return Double.class;
        }
        if (type == float.class) {
            return Float.class;
        }
        if (type == short.class) {
            return Short.class;
        }
        if (type == byte.class) {
            return Byte.class;
        }
        if (type == char.class) {
            return Character.class;
        }
        return Void.class;
    }

    private String stringValue(Object value) {
        return value == null ? "" : value.toString();
    }

    private String fallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private record ChatMeta(
            String prefix,
            String suffix,
            String primaryGroup,
            String world,
            Component displayName,
            Map<String, String> placeholders
    ) {
    }

    private record LuckPermsMeta(String prefix, String suffix, String primaryGroup) {
        private static LuckPermsMeta empty() {
            return new LuckPermsMeta("", "", "");
        }
    }

    private record Match(int index, String token, Component component) {
    }
}
