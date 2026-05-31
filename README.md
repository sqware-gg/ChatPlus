# ChatPlus

[![Build](https://github.com/sqware-gg/ChatPlus/actions/workflows/build.yml/badge.svg)](https://github.com/sqware-gg/ChatPlus/actions/workflows/build.yml)

**Join the SQWARE Discord: [discord.sqware.gg](https://discord.sqware.gg).**

ChatPlus is a Paper chat control plugin for Minecraft servers. It lets players hide public chat while still receiving important messages such as mentions, teleport requests, warnings, private notices, purchase alerts, and server announcements.

Use it when you want chat toggle, quiet mode, focus mode, mention passthrough, and categorized plugin notifications without forcing one global chat rule.

## Features

- Player chat modes: `normal`, `quiet`, `focus`, and `off`.
- Per-recipient filtering for regular player chat.
- Mention passthrough with configurable `@Player` matching.
- Per-player category allow/block overrides.
- Persistent preferences in `plugins/ChatPlus/players.yml`.
- Admin notifications and broadcasts.
- Public `ChatPlusApi` for teleport, store, moderation, and utility plugins.
- Config-safe updates through `config-new.yml`.
- Interactive item and inventory sharing in chat with `[item]`, `[inv]`, and `[ender]` style placeholders.

## Requirements

- Paper with modern chat support
- API target: Paper `1.18.2`
- Java `17+`
- Maven

Paper is the supported target. Bukkit-based servers may load the legacy listener, but Paper gives the most reliable per-recipient filtering.

## Player Commands

```text
/chat
/chat normal
/chat quiet
/chat focus
/chat off
/chat allow <category>
/chat block <category>
/chat reset [category|all]
/chat mentions <on|off>
/chat status
/chat categories
```

## Admin Commands

```text
/chatadmin reload
/chatadmin set <player> <mode>
/chatadmin reset <player>
/chatadmin status <player>
/chatadmin notify <player> <category> <message>
/chatadmin broadcast <category> <message>
```

## Categories

```text
regular-chat
mentions
private-message
teleport-request
server-notice
staff-notice
purchase
warning
```

Regular player chat is categorized automatically. Messages sent directly by another plugin with `player.sendMessage(...)` cannot be recategorized after they are sent. Use `/chatadmin notify`, `/chatadmin broadcast`, or `ChatPlusApi` for plugin notices.

## Permissions

```text
chatplus.command  - use /chat, default true
chatplus.admin    - admin commands, default op
chatplus.notify   - notify and broadcast commands, default op
chatplus.bypass   - sender bypasses recipient regular-chat filters, default op
chatplus.item     - use [item], [i], and [hand], default true
chatplus.item.offhand - use [offhand] and [off], default true
chatplus.item.bypass-cooldown - bypass item share cooldowns, default op
chatplus.inventory - use [inventory] and [inv], default true
chatplus.enderchest - use [enderchest], [ender], and [ec], default true
```

## Interactive Sharing

Players can type `[item]`, `[i]`, or `[hand]` in public chat to show the item in their main hand. `[offhand]` and `[off]` show the off hand item. On Paper modern chat, the replacement is an interactive component with the real Minecraft item hover tooltip, including custom names, lore, enchantments, and NBT supplied by Paper. Legacy chat falls back to a plain item name.

Item display formats support `{name}` and `{plain_item}` for a clean text name such as `Emerald Block`, while keeping the real item hover tooltip attached to the rendered replacement. `{item}` remains available for the raw Paper item display component.

Players can also type `[inventory]` or `[inv]` to share a clickable, read-only inventory snapshot, and `[enderchest]`, `[ender]`, or `[ec]` to share a clickable ender chest snapshot. Snapshots expire automatically and cannot be edited by viewers.

The feature is controlled by `item-share`, `inventory-share`, and `ender-chest-share` in `config.yml`: placeholders, cooldowns, max replacements per message, permissions, display format, Discord fallback text, click behavior, snapshot expiry, and failure messages are configurable.

## Java API

```java
ChatPlusApi.send(player, ChatCategory.TELEPORT_REQUEST, "0xConflict wants to teleport to you.");
ChatPlusApi.broadcast(ChatCategory.PURCHASE, "Hilal_h18 bought Premium.");
ChatPlusApi.send(player, "server-notice", "Restart in 5 minutes.");
String discordText = ChatPlusApi.renderDiscordChat(player, "Selling [item], see [inv].");
```

## Build

```powershell
mvn package
```

The jar is written to `target/ChatPlus-0.1.0.jar`.

## License

ChatPlus is licensed under the Apache License, Version 2.0.
