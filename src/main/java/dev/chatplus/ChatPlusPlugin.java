package dev.chatplus;

import dev.chatplus.api.ChatPlusApi;
import dev.chatplus.chat.ChatFilterService;
import dev.chatplus.chat.LegacyChatListener;
import dev.chatplus.chat.NotificationService;
import dev.chatplus.command.ChatAdminCommand;
import dev.chatplus.command.ChatCommand;
import dev.chatplus.config.ChatPlusConfig;
import dev.chatplus.config.ConfigReferenceWriter;
import dev.chatplus.settings.PlayerSettingsStore;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public final class ChatPlusPlugin extends JavaPlugin {
    private ChatPlusConfig chatConfig;
    private PlayerSettingsStore settingsStore;
    private NotificationService notificationService;

    @Override
    public void onEnable() {
        ConfigReferenceWriter.saveDefaultAndReferenceIfNeeded(this);

        chatConfig = new ChatPlusConfig(this);
        settingsStore = new PlayerSettingsStore(this, chatConfig);
        ChatFilterService filterService = new ChatFilterService(chatConfig, settingsStore);
        notificationService = new NotificationService(chatConfig, filterService);
        ChatPlusApi.register(notificationService);

        boolean modernChat = registerPaperChatListener(filterService);
        getServer().getPluginManager().registerEvents(new LegacyChatListener(filterService, !modernChat), this);

        ChatCommand chatCommand = new ChatCommand(chatConfig, settingsStore);
        PluginCommand chat = getCommand("chat");
        if (chat != null) {
            chat.setExecutor(chatCommand);
            chat.setTabCompleter(chatCommand);
        }

        ChatAdminCommand adminCommand = new ChatAdminCommand(chatConfig, settingsStore, notificationService);
        PluginCommand admin = getCommand("chatadmin");
        if (admin != null) {
            admin.setExecutor(adminCommand);
            admin.setTabCompleter(adminCommand);
        }
    }

    private boolean registerPaperChatListener(ChatFilterService filterService) {
        try {
            Class.forName("io.papermc.paper.event.player.AsyncChatEvent");
            Class<?> listenerClass = Class.forName("dev.chatplus.chat.PaperChatListener");
            Listener listener = (Listener) listenerClass
                    .getConstructor(ChatFilterService.class)
                    .newInstance(filterService);
            getServer().getPluginManager().registerEvents(listener, this);
            getLogger().info("Using Paper modern chat event.");
            return true;
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return false;
        }
    }

    @Override
    public void onDisable() {
        ChatPlusApi.unregister();
        if (settingsStore != null) {
            settingsStore.save();
        }
    }
}
