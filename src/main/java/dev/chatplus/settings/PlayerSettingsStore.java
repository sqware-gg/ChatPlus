package dev.chatplus.settings;

import dev.chatplus.chat.ChatCategory;
import dev.chatplus.chat.ChatMode;
import dev.chatplus.config.ChatPlusConfig;
import java.io.File;
import java.io.IOException;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class PlayerSettingsStore {
    private final JavaPlugin plugin;
    private final ChatPlusConfig config;
    private final File file;
    private final Map<UUID, PlayerChatSettings> settings = new ConcurrentHashMap<>();

    public PlayerSettingsStore(JavaPlugin plugin, ChatPlusConfig config) {
        this.plugin = plugin;
        this.config = config;
        this.file = new File(plugin.getDataFolder(), "players.yml");
        reload();
    }

    public void reload() {
        settings.clear();
        if (!file.exists()) {
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection players = yaml.getConfigurationSection("players");
        if (players == null) {
            return;
        }
        for (String key : players.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                settings.put(uuid, readSettings(players.getConfigurationSection(key)));
            } catch (IllegalArgumentException ignored) {
                plugin.getLogger().fine("Ignoring invalid UUID in players.yml: " + key);
            }
        }
    }

    private PlayerChatSettings readSettings(ConfigurationSection section) {
        if (section == null) {
            return defaultSettings();
        }
        ChatMode mode = ChatMode.from(section.getString("mode", config.defaultMode().key())).orElse(config.defaultMode());
        boolean mentionsEnabled = section.getBoolean("mentions-enabled", true);
        Set<ChatCategory> allowed = readCategories(section, "allow");
        Set<ChatCategory> blocked = readCategories(section, "block");
        return new PlayerChatSettings(mode, mentionsEnabled, allowed, blocked);
    }

    private Set<ChatCategory> readCategories(ConfigurationSection section, String path) {
        EnumSet<ChatCategory> categories = EnumSet.noneOf(ChatCategory.class);
        for (String value : section.getStringList(path)) {
            ChatCategory.from(value).ifPresent(categories::add);
        }
        return categories;
    }

    public PlayerChatSettings settings(UUID uuid) {
        return settings.computeIfAbsent(uuid, ignored -> defaultSettings());
    }

    public void reset(UUID uuid) {
        settings.remove(uuid);
        save();
    }

    public void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<UUID, PlayerChatSettings> entry : settings.entrySet()) {
            String path = "players." + entry.getKey();
            PlayerChatSettings value = entry.getValue();
            yaml.set(path + ".mode", value.mode().key());
            yaml.set(path + ".mentions-enabled", value.mentionsEnabled());
            yaml.set(path + ".allow", value.allowedOverrides().stream().map(ChatCategory::key).sorted().toList());
            yaml.set(path + ".block", value.blockedOverrides().stream().map(ChatCategory::key).sorted().toList());
        }
        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save players.yml: " + e.getMessage());
        }
    }

    private PlayerChatSettings defaultSettings() {
        return new PlayerChatSettings(config.defaultMode(), true, Set.of(), Set.of());
    }
}
