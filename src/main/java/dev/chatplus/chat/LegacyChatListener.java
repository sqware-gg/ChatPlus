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
    private final ItemShareService itemShareService;
    private final boolean compatibilityFallback;

    public LegacyChatListener(ChatFilterService filterService, ItemShareService itemShareService, boolean compatibilityFallback) {
        this.filterService = filterService;
        this.itemShareService = itemShareService;
        this.compatibilityFallback = compatibilityFallback;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        String plainMessage = Text.oneLine(event.getMessage());
        ItemShareService.ItemShareResult itemShare = compatibilityFallback
                ? itemShareService.renderLegacyFallback(event.getPlayer(), plainMessage)
                : itemShareService.render(event.getPlayer(), plainMessage);
        if (itemShare.cancelled()) {
            event.setCancelled(true);
            itemShareService.sendFailure(event.getPlayer(), itemShare.failureMessage());
            return;
        }
        if (itemShare.changed()) {
            event.setMessage(itemShare.plainMessage());
        }
        Iterator<Player> iterator = event.getRecipients().iterator();
        while (iterator.hasNext()) {
            Player recipient = iterator.next();
            if (!filterService.shouldReceiveRegularChat(event.getPlayer(), recipient, plainMessage)) {
                iterator.remove();
            }
        }
    }
}
