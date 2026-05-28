# ChatPlus

[![Build](https://github.com/sqware-gg/ChatPlus/actions/workflows/build.yml/badge.svg)](https://github.com/sqware-gg/ChatPlus/actions/workflows/build.yml)

**Get the plugin jar, setup help, and updates in the SQWARE Discord: [discord.sqware.gg](https://discord.sqware.gg).**

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
```

## Java API

```java
ChatPlusApi.send(player, ChatCategory.TELEPORT_REQUEST, "0xConflict wants to teleport to you.");
ChatPlusApi.broadcast(ChatCategory.PURCHASE, "Hilal_h18 bought Premium.");
ChatPlusApi.send(player, "server-notice", "Restart in 5 minutes.");
```

## Build

```powershell
mvn package
```

The jar is written to `target/ChatPlus-0.1.0.jar`.

## License

ChatPlus is licensed under the Apache License, Version 2.0.
