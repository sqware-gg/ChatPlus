package dev.chatplus.chat;

import dev.chatplus.config.ChatPlusConfig;
import dev.chatplus.config.ChatPlusConfig.ItemClickAction;
import dev.chatplus.config.ChatPlusConfig.ItemShareSettings;
import dev.chatplus.config.ChatPlusConfig.ViewShareSettings;
import dev.chatplus.util.Text;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.UnaryOperator;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

public final class ItemShareService implements CommandExecutor, Listener {
    private static final long SNAPSHOT_TIMEOUT_SECONDS = 2L;
    private static final int INVENTORY_VIEW_SIZE = 54;
    private static final int SUMMARY_LIMIT = 6;

    private final JavaPlugin plugin;
    private final ChatPlusConfig config;
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();
    private final Map<String, ViewSnapshot> snapshots = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();

    public ItemShareService(JavaPlugin plugin, ChatPlusConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    public ItemShareResult render(Player sender, String message) {
        if (!sharesEnabled() || message == null || message.isBlank()) {
            return ItemShareResult.unchanged();
        }

        List<Occurrence> occurrences = findOccurrences(message);
        if (occurrences.isEmpty()) {
            return ItemShareResult.unchanged();
        }

        String validationMessage = validate(sender, occurrences);
        if (validationMessage != null) {
            return ItemShareResult.cancelled(validationMessage);
        }

        SnapshotData snapshotData = snapshot(sender, occurrences, true);
        if (snapshotData == null) {
            return ItemShareResult.cancelled(config.itemShare().unavailableMessage());
        }

        String emptyMessage = firstEmptyMessage(occurrences, snapshotData);
        if (emptyMessage != null) {
            return ItemShareResult.cancelled(emptyMessage);
        }

        storeSnapshots(snapshotData);
        applyCooldown(sender);

        RenderedMessage rendered = renderMessage(message, occurrences, snapshotData);
        return ItemShareResult.changed(rendered.component(), rendered.plainMessage());
    }

    public String renderDiscord(Player sender, String message) {
        if (!sharesEnabled() || message == null || message.isBlank()) {
            return Text.oneLine(message);
        }
        List<Occurrence> occurrences = findOccurrences(message);
        if (occurrences.isEmpty()) {
            return Text.oneLine(message);
        }
        SnapshotData snapshotData = snapshot(sender, occurrences, false);
        if (snapshotData == null || firstEmptyMessage(occurrences, snapshotData) != null) {
            return Text.oneLine(message);
        }
        return renderDiscordMessage(message, occurrences, snapshotData);
    }

    public boolean hasPlaceholders(String message) {
        return sharesEnabled() && message != null && !message.isBlank() && !findOccurrences(message).isEmpty();
    }

    public void sendFailure(Player player, String message) {
        if (message == null || message.isBlank()) {
            return;
        }
        Runnable sender = () -> player.sendMessage(Text.color(config.messagePrefix() + message));
        if (Bukkit.isPrimaryThread()) {
            sender.run();
        } else {
            Bukkit.getScheduler().runTask(plugin, sender);
        }
    }

    public void clearCooldowns() {
        cooldowns.clear();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Text.color(config.messagePrefix() + "&#ED4245Only players can view shared inventories."));
            return true;
        }
        if (args.length < 1) {
            sendFailure(player, "&#ED4245That shared inventory expired.");
            return true;
        }
        openSnapshot(player, args[0]);
        return true;
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (isSharedInventory(event.getInventory()) || isSharedInventory(event.getClickedInventory())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (isSharedInventory(event.getInventory())) {
            event.setCancelled(true);
        }
    }

    private void openSnapshot(Player player, String id) {
        cleanupSnapshots();
        ViewSnapshot snapshot = snapshots.get(id);
        if (snapshot == null || snapshot.expired()) {
            snapshots.remove(id);
            sendFailure(player, snapshot == null
                    ? "&#ED4245That shared inventory expired."
                    : viewSettings(snapshot.kind()).expiredMessage());
            return;
        }

        SharedInventoryHolder holder = new SharedInventoryHolder(id);
        Inventory inventory = Bukkit.createInventory(holder, snapshot.contents().length,
                Text.component(renderViewPlaceholders(viewSettings(snapshot.kind()).title(), snapshot)));
        holder.inventory(inventory);
        for (int slot = 0; slot < snapshot.contents().length; slot++) {
            ItemStack item = snapshot.contents()[slot];
            if (!isEmpty(item)) {
                inventory.setItem(slot, item.clone());
            }
        }
        player.openInventory(inventory);
    }

    private boolean isSharedInventory(Inventory inventory) {
        return inventory != null && inventory.getHolder() instanceof SharedInventoryHolder;
    }

    private boolean sharesEnabled() {
        return config.itemShare().enabled()
                || config.inventoryShare().enabled()
                || config.enderChestShare().enabled();
    }

    private String validate(Player sender, List<Occurrence> occurrences) {
        ItemShareSettings settings = config.itemShare();
        if (settings.maxPerMessage() > 0 && occurrences.size() > settings.maxPerMessage()) {
            return settings.tooManyItemsMessage()
                    .replace("{max}", Integer.toString(settings.maxPerMessage()))
                    .replace("{count}", Integer.toString(occurrences.size()));
        }
        for (Occurrence occurrence : occurrences) {
            String permissionMessage = permissionMessage(sender, occurrence);
            if (permissionMessage != null) {
                return permissionMessage;
            }
        }
        if (settings.cooldownMillis() <= 0L || sender.hasPermission(settings.bypassCooldownPermission())) {
            return null;
        }
        long remainingMillis = cooldowns.getOrDefault(sender.getUniqueId(), 0L) - System.currentTimeMillis();
        if (remainingMillis <= 0L) {
            return null;
        }
        long remainingSeconds = Math.max(1L, (long) Math.ceil(remainingMillis / 1000.0D));
        return settings.cooldownMessage().replace("{seconds}", Long.toString(remainingSeconds));
    }

    private String permissionMessage(Player sender, Occurrence occurrence) {
        if (occurrence.kind() == ShareKind.ITEM) {
            ItemShareSettings settings = config.itemShare();
            if (!sender.hasPermission(settings.permission())) {
                return settings.noPermissionMessage();
            }
            if (occurrence.hand() == SharedHand.OFF && !sender.hasPermission(settings.offHandPermission())) {
                return settings.noPermissionMessage();
            }
            return null;
        }
        ViewShareSettings settings = viewSettings(occurrence.kind());
        return sender.hasPermission(settings.permission()) ? null : config.itemShare().noPermissionMessage();
    }

    private void applyCooldown(Player sender) {
        ItemShareSettings settings = config.itemShare();
        if (settings.cooldownMillis() > 0L && !sender.hasPermission(settings.bypassCooldownPermission())) {
            cooldowns.put(sender.getUniqueId(), System.currentTimeMillis() + settings.cooldownMillis());
        }
    }

    private SnapshotData snapshot(Player sender, List<Occurrence> occurrences, boolean storeViews) {
        if (Bukkit.isPrimaryThread()) {
            return snapshotNow(sender, occurrences, storeViews);
        }
        try {
            return Bukkit.getScheduler()
                    .callSyncMethod(plugin, () -> snapshotNow(sender, occurrences, storeViews))
                    .get(SNAPSHOT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } catch (ExecutionException | TimeoutException e) {
            plugin.getLogger().warning("Could not snapshot chat share data: " + e.getMessage());
            return null;
        }
    }

    private SnapshotData snapshotNow(Player sender, List<Occurrence> occurrences, boolean storeViews) {
        EnumMap<SharedHand, ItemStack> items = new EnumMap<>(SharedHand.class);
        EnumMap<ShareKind, ViewSnapshot> views = new EnumMap<>(ShareKind.class);
        for (Occurrence occurrence : occurrences) {
            if (occurrence.kind() == ShareKind.ITEM) {
                items.computeIfAbsent(occurrence.hand(), hand -> cloneItem(sender, hand));
                continue;
            }
            views.computeIfAbsent(occurrence.kind(), kind -> snapshotView(sender, kind, storeViews));
        }
        return new SnapshotData(items, views);
    }

    private ItemStack cloneItem(Player sender, SharedHand hand) {
        ItemStack item = hand == SharedHand.MAIN
                ? sender.getInventory().getItemInMainHand()
                : sender.getInventory().getItemInOffHand();
        return item == null ? new ItemStack(Material.AIR) : item.clone();
    }

    private ViewSnapshot snapshotView(Player sender, ShareKind kind, boolean storeViews) {
        long expiresAt = System.currentTimeMillis() + viewSettings(kind).expiresMillis();
        String id = storeViews ? newSnapshotId() : "";
        ItemStack[] contents = kind == ShareKind.INVENTORY
                ? inventoryContents(sender)
                : cloneContents(sender.getEnderChest().getContents(), 27);
        return new ViewSnapshot(id, sender.getUniqueId(), sender.getName(), kind, contents, expiresAt);
    }

    private ItemStack[] inventoryContents(Player sender) {
        ItemStack[] contents = new ItemStack[INVENTORY_VIEW_SIZE];
        ItemStack[] storage = sender.getInventory().getStorageContents();
        for (int index = 0; index < Math.min(36, storage.length); index++) {
            contents[index] = cloneOrAir(storage[index]);
        }
        ItemStack[] armor = sender.getInventory().getArmorContents();
        for (int index = 0; index < Math.min(4, armor.length); index++) {
            contents[45 + index] = cloneOrAir(armor[index]);
        }
        contents[50] = cloneOrAir(sender.getInventory().getItemInOffHand());
        return contents;
    }

    private ItemStack[] cloneContents(ItemStack[] source, int size) {
        ItemStack[] contents = new ItemStack[size];
        for (int index = 0; index < Math.min(size, source.length); index++) {
            contents[index] = cloneOrAir(source[index]);
        }
        return contents;
    }

    private ItemStack cloneOrAir(ItemStack item) {
        return item == null ? new ItemStack(Material.AIR) : item.clone();
    }

    private String newSnapshotId() {
        cleanupSnapshots();
        String id;
        do {
            id = Long.toUnsignedString(random.nextLong(), 36);
        } while (snapshots.containsKey(id));
        return id;
    }

    private void storeSnapshots(SnapshotData snapshotData) {
        snapshotData.views().values().stream()
                .filter(snapshot -> !snapshot.id().isBlank())
                .forEach(snapshot -> snapshots.put(snapshot.id(), snapshot));
    }

    private void cleanupSnapshots() {
        long now = System.currentTimeMillis();
        snapshots.entrySet().removeIf(entry -> entry.getValue().expiresAtMillis() <= now);
    }

    private String firstEmptyMessage(List<Occurrence> occurrences, SnapshotData snapshotData) {
        for (Occurrence occurrence : occurrences) {
            if (occurrence.kind() == ShareKind.ITEM) {
                ItemStack item = snapshotData.items().get(occurrence.hand());
                if (isEmpty(item)) {
                    return config.itemShare().emptyHandMessage();
                }
                continue;
            }
            ViewSnapshot snapshot = snapshotData.views().get(occurrence.kind());
            if (snapshot == null || snapshot.filledSlots() == 0) {
                return viewSettings(occurrence.kind()).emptyMessage();
            }
        }
        return null;
    }

    private RenderedMessage renderMessage(String message, List<Occurrence> occurrences, SnapshotData snapshotData) {
        Component component = Component.empty();
        StringBuilder plain = new StringBuilder();
        int cursor = 0;
        for (Occurrence occurrence : occurrences) {
            String before = message.substring(cursor, occurrence.start());
            component = component.append(Component.text(before));
            plain.append(before);

            if (occurrence.kind() == ShareKind.ITEM) {
                ItemStack item = snapshotData.items().get(occurrence.hand());
                component = component.append(renderItemComponent(item, occurrence.placeholder()));
                plain.append(renderPlainItem(item, occurrence.placeholder()));
            } else {
                ViewSnapshot snapshot = snapshotData.views().get(occurrence.kind());
                component = component.append(renderViewComponent(snapshot, occurrence.placeholder()));
                plain.append(renderPlainView(snapshot));
            }
            cursor = occurrence.end();
        }
        String after = message.substring(cursor);
        component = component.append(Component.text(after));
        plain.append(after);
        return new RenderedMessage(component, plain.toString());
    }

    private String renderDiscordMessage(String message, List<Occurrence> occurrences, SnapshotData snapshotData) {
        StringBuilder builder = new StringBuilder();
        int cursor = 0;
        for (Occurrence occurrence : occurrences) {
            builder.append(message, cursor, occurrence.start());
            if (occurrence.kind() == ShareKind.ITEM) {
                builder.append(renderDiscordItem(snapshotData.items().get(occurrence.hand()), occurrence.placeholder()));
            } else {
                builder.append(renderDiscordView(snapshotData.views().get(occurrence.kind())));
            }
            cursor = occurrence.end();
        }
        builder.append(message.substring(cursor));
        return Text.oneLine(builder.toString());
    }

    private Component renderItemComponent(ItemStack item, String placeholder) {
        ItemShareSettings settings = config.itemShare();
        Component displayName = itemNameComponent(item);
        String plainName = itemName(item);
        String amount = amountText(item, settings);
        String template = itemTextPlaceholders(settings.displayFormat(), item, plainName, amount, placeholder);
        HoverEvent<?> hover = item.asHoverEvent(UnaryOperator.identity());

        return renderTemplateWithItem(template, displayName, plainName, hover, settings);
    }

    private Component renderViewComponent(ViewSnapshot snapshot, String placeholder) {
        ViewShareSettings settings = viewSettings(snapshot.kind());
        String display = renderViewPlaceholders(settings.displayFormat(), snapshot)
                .replace("{placeholder}", placeholder);
        String hover = renderViewPlaceholders(settings.hoverText(), snapshot)
                .replace("{placeholder}", placeholder);
        return Text.component(display)
                .hoverEvent(HoverEvent.showText(Text.component(hover)))
                .clickEvent(ClickEvent.runCommand("/chatplusview " + snapshot.id()));
    }

    private Component renderTemplateWithItem(
            String template,
            Component itemName,
            String plainName,
            HoverEvent<?> hover,
            ItemShareSettings settings
    ) {
        Component component = Component.empty();
        int cursor = 0;
        int index = template.indexOf("{item}");
        while (index >= 0) {
            component = component.append(itemInteractive(Text.component(template.substring(cursor, index)), hover, plainName, settings));
            component = component.append(itemInteractive(itemName, hover, plainName, settings));
            cursor = index + "{item}".length();
            index = template.indexOf("{item}", cursor);
        }
        return component.append(itemInteractive(Text.component(template.substring(cursor)), hover, plainName, settings));
    }

    private Component itemInteractive(Component component, HoverEvent<?> hover, String plainName, ItemShareSettings settings) {
        Component interactive = component.hoverEvent(hover);
        if (settings.shiftClickInsertion()) {
            interactive = interactive.insertion(plainName);
        }
        if (settings.clickAction() == ItemClickAction.SUGGEST) {
            interactive = interactive.clickEvent(ClickEvent.suggestCommand(plainName));
        } else if (settings.clickAction() == ItemClickAction.COPY) {
            interactive = interactive.clickEvent(ClickEvent.copyToClipboard(plainName));
        }
        return interactive;
    }

    private String renderPlainItem(ItemStack item, String placeholder) {
        ItemShareSettings settings = config.itemShare();
        String amount = Text.strip(amountText(item, settings));
        String plainName = itemName(item);
        String template = itemTextPlaceholders(settings.plainFormat(), item, plainName, amount, placeholder)
                .replace("{item}", plainName);
        return Text.strip(template);
    }

    private String renderDiscordItem(ItemStack item, String placeholder) {
        ItemShareSettings settings = config.itemShare();
        String amount = Text.strip(amountText(item, settings));
        String plainName = itemName(item);
        String template = itemTextPlaceholders(settings.discordFormat(), item, plainName, amount, placeholder)
                .replace("{item}", plainName);
        return Text.strip(template);
    }

    private String itemTextPlaceholders(String template, ItemStack item, String plainName, String amount, String placeholder) {
        return template
                .replace("{name}", plainName)
                .replace("{plain_item}", plainName)
                .replace("{plain-item}", plainName)
                .replace("{amount}", amount)
                .replace("{material}", item.getType().getKey().toString())
                .replace("{type}", item.getType().name())
                .replace("{placeholder}", placeholder);
    }

    private String renderPlainView(ViewSnapshot snapshot) {
        return Text.strip(renderViewPlaceholders(viewSettings(snapshot.kind()).plainFormat(), snapshot));
    }

    private String renderDiscordView(ViewSnapshot snapshot) {
        return Text.strip(renderViewPlaceholders(viewSettings(snapshot.kind()).discordFormat(), snapshot));
    }

    private String renderViewPlaceholders(String template, ViewSnapshot snapshot) {
        return template.replace("{player}", snapshot.ownerName())
                .replace("{uuid}", snapshot.ownerUuid().toString())
                .replace("{label}", viewSettings(snapshot.kind()).label())
                .replace("{type}", snapshot.kind().key())
                .replace("{count}", Integer.toString(snapshot.filledSlots()))
                .replace("{summary}", snapshot.summary())
                .replace("{id}", snapshot.id());
    }

    private String amountText(ItemStack item, ItemShareSettings settings) {
        if (!settings.showAmount() || item.getAmount() <= 1) {
            return "";
        }
        return settings.amountFormat().replace("{amount}", Integer.toString(item.getAmount()));
    }

    private String itemName(ItemStack item) {
        return itemNamePlain(item);
    }

    private static String itemNamePlain(ItemStack item) {
        ItemMeta meta = item.hasItemMeta() ? item.getItemMeta() : null;
        String displayName = meta != null && meta.hasDisplayName()
                ? Text.oneLine(PlainTextComponentSerializer.plainText().serialize(meta.displayName()))
                : "";
        if (!displayName.isBlank()) {
            return displayName;
        }
        String i18nName = Text.oneLine(item.getI18NDisplayName());
        if (!i18nName.isBlank()) {
            return i18nName;
        }
        return item.getType().name().toLowerCase(Locale.ROOT).replace('_', ' ');
    }

    private Component itemNameComponent(ItemStack item) {
        ItemMeta meta = item.hasItemMeta() ? item.getItemMeta() : null;
        if (meta != null && meta.hasDisplayName()) {
            return meta.displayName().colorIfAbsent(NamedTextColor.WHITE);
        }
        return Component.translatable(item.translationKey()).colorIfAbsent(NamedTextColor.WHITE);
    }

    private List<Occurrence> findOccurrences(String message) {
        List<Placeholder> placeholders = placeholders();
        if (placeholders.isEmpty()) {
            return List.of();
        }

        List<Occurrence> occurrences = new ArrayList<>();
        int index = 0;
        while (index < message.length()) {
            Placeholder match = matchAt(message, index, placeholders, config.itemShare().caseSensitive());
            if (match == null) {
                index++;
                continue;
            }
            occurrences.add(new Occurrence(index, index + match.token().length(),
                    match.token(), match.kind(), match.hand()));
            index += match.token().length();
        }
        return occurrences;
    }

    private List<Placeholder> placeholders() {
        List<Placeholder> placeholders = new ArrayList<>();
        ItemShareSettings itemSettings = config.itemShare();
        if (itemSettings.enabled()) {
            itemSettings.mainHandPlaceholders().forEach(value ->
                    placeholders.add(new Placeholder(value, ShareKind.ITEM, SharedHand.MAIN)));
            itemSettings.offHandPlaceholders().forEach(value ->
                    placeholders.add(new Placeholder(value, ShareKind.ITEM, SharedHand.OFF)));
        }
        if (config.inventoryShare().enabled()) {
            config.inventoryShare().placeholders().forEach(value ->
                    placeholders.add(new Placeholder(value, ShareKind.INVENTORY, null)));
        }
        if (config.enderChestShare().enabled()) {
            config.enderChestShare().placeholders().forEach(value ->
                    placeholders.add(new Placeholder(value, ShareKind.ENDER_CHEST, null)));
        }
        placeholders.sort(Comparator.comparingInt((Placeholder placeholder) -> placeholder.token().length()).reversed());
        return placeholders;
    }

    private Placeholder matchAt(String message, int start, List<Placeholder> placeholders, boolean caseSensitive) {
        for (Placeholder placeholder : placeholders) {
            String token = placeholder.token();
            if (start + token.length() > message.length()) {
                continue;
            }
            if (message.regionMatches(!caseSensitive, start, token, 0, token.length())) {
                return placeholder;
            }
        }
        return null;
    }

    private boolean isEmpty(ItemStack item) {
        return item == null || item.getType() == Material.AIR || item.getType().isAir() || item.getAmount() <= 0;
    }

    private ViewShareSettings viewSettings(ShareKind kind) {
        return kind == ShareKind.ENDER_CHEST ? config.enderChestShare() : config.inventoryShare();
    }

    private enum SharedHand {
        MAIN,
        OFF
    }

    private enum ShareKind {
        ITEM("item"),
        INVENTORY("inventory"),
        ENDER_CHEST("ender-chest");

        private final String key;

        ShareKind(String key) {
            this.key = key;
        }

        private String key() {
            return key;
        }
    }

    private record Placeholder(String token, ShareKind kind, SharedHand hand) {
    }

    private record Occurrence(int start, int end, String placeholder, ShareKind kind, SharedHand hand) {
    }

    private record SnapshotData(EnumMap<SharedHand, ItemStack> items, EnumMap<ShareKind, ViewSnapshot> views) {
    }

    private record RenderedMessage(Component component, String plainMessage) {
    }

    private record ViewSnapshot(
            String id,
            UUID ownerUuid,
            String ownerName,
            ShareKind kind,
            ItemStack[] contents,
            long expiresAtMillis
    ) {
        private boolean expired() {
            return expiresAtMillis <= System.currentTimeMillis();
        }

        private int filledSlots() {
            int count = 0;
            for (ItemStack item : contents) {
                if (item != null && item.getType() != Material.AIR && !item.getType().isAir() && item.getAmount() > 0) {
                    count++;
                }
            }
            return count;
        }

        private String summary() {
            Map<String, Integer> totals = new LinkedHashMap<>();
            for (ItemStack item : contents) {
                if (item == null || item.getType() == Material.AIR || item.getType().isAir() || item.getAmount() <= 0) {
                    continue;
                }
                String name = itemNamePlain(item);
                if (name.isBlank()) {
                    name = item.getType().name().toLowerCase(Locale.ROOT).replace('_', ' ');
                }
                totals.merge(name, item.getAmount(), Integer::sum);
            }
            if (totals.isEmpty()) {
                return "empty";
            }
            List<String> parts = new ArrayList<>();
            int index = 0;
            for (Map.Entry<String, Integer> entry : totals.entrySet()) {
                if (index >= SUMMARY_LIMIT) {
                    parts.add("+" + (totals.size() - SUMMARY_LIMIT) + " more");
                    break;
                }
                parts.add(entry.getKey() + (entry.getValue() > 1 ? " x" + entry.getValue() : ""));
                index++;
            }
            return String.join(", ", parts);
        }
    }

    private static final class SharedInventoryHolder implements InventoryHolder {
        private final String id;
        private Inventory inventory;

        private SharedInventoryHolder(String id) {
            this.id = id;
        }

        private void inventory(Inventory inventory) {
            this.inventory = inventory;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }

        @SuppressWarnings("unused")
        private String id() {
            return id;
        }
    }

    public static final class ItemShareResult {
        private static final ItemShareResult UNCHANGED = new ItemShareResult(false, false, null, null, null);

        private final boolean changed;
        private final boolean cancelled;
        private final Component component;
        private final String plainMessage;
        private final String failureMessage;

        private ItemShareResult(
                boolean changed,
                boolean cancelled,
                Component component,
                String plainMessage,
                String failureMessage
        ) {
            this.changed = changed;
            this.cancelled = cancelled;
            this.component = component;
            this.plainMessage = plainMessage;
            this.failureMessage = failureMessage;
        }

        public static ItemShareResult unchanged() {
            return UNCHANGED;
        }

        public static ItemShareResult changed(Component component, String plainMessage) {
            return new ItemShareResult(true, false, component, plainMessage, null);
        }

        public static ItemShareResult cancelled(String failureMessage) {
            return new ItemShareResult(false, true, null, null, failureMessage);
        }

        public boolean changed() {
            return changed;
        }

        public boolean cancelled() {
            return cancelled;
        }

        public Component component() {
            return component;
        }

        public String plainMessage() {
            return plainMessage;
        }

        public String failureMessage() {
            return failureMessage;
        }
    }
}
