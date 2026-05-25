package dev.chatplus.chat;

import dev.chatplus.util.Text;
import java.util.Iterator;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public final class LegacyChatListener implements Listener {
    private final ChatFilterService filterService;
    private final boolean enabled;

    public LegacyChatListener(ChatFilterService filterService, boolean enabled) {
        this.filterService = filterService;
        this.enabled = enabled;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        if (!enabled) {
            return;
        }
        String plainMessage = Text.oneLine(event.getMessage());
        Iterator<Player> iterator = event.getRecipients().iterator();
        while (iterator.hasNext()) {
            Player recipient = iterator.next();
            if (!filterService.shouldReceiveRegularChat(event.getPlayer(), recipient, plainMessage)) {
                iterator.remove();
            }
        }
    }
}
