package dev.chatplus.chat;

import dev.chatplus.util.Text;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public final class PaperChatListener implements Listener {
    private final ChatFilterService filterService;

    public PaperChatListener(ChatFilterService filterService) {
        this.filterService = filterService;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        String plainMessage = Text.oneLine(PlainTextComponentSerializer.plainText().serialize(event.message()));
        for (Player recipient : Bukkit.getOnlinePlayers()) {
            if (!filterService.shouldReceiveRegularChat(event.getPlayer(), recipient, plainMessage)) {
                event.viewers().remove(recipient);
            }
        }
    }
}
