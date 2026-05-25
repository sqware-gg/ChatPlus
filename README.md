# ChatPlus

[![Build](https://github.com/sqware-gg/ChatPlus/actions/workflows/build.yml/badge.svg)](https://github.com/sqware-gg/ChatPlus/actions/workflows/build.yml)

ChatPlus gives Minecraft players a cleaner chat experience without hiding messages that matter. Players can reduce regular public chat noise while still seeing mentions, teleport requests, private-message style notices, purchases, warnings, and server information.

It is designed for Paper server owners who want a practical middle ground between global chat spam and fully disabling chat.

## Links

- Website: https://sqware.gg
- Plugin information and support: https://discord.sqware.gg

## Compatibility

- Server software: Paper, with modern Paper chat support and legacy chat fallback
- API target: Paper `1.18.2`
- Java: `17+`
- Build tool: Maven

As of May 2026, ChatPlus is intended for Paper-based servers. Other Bukkit forks may load the legacy chat listener, but Paper is the supported target.

## Features

- Per-player chat modes: `normal`, `quiet`, `focus`, and `off`.
- Per-recipient filtering for regular player chat.
- Mention passthrough with configurable `@Player` detection.
- Per-category allow/block overrides.
- Persistent preferences in `plugins/ChatPlus/players.yml`.
- Admin commands for reloads, status checks, forced mode changes, notifications, and broadcasts.
- Public `ChatPlusApi` for integrations with teleport, store, moderation, or information plugins.
- Config update safety: existing `config.yml` files are preserved and a fresh `config-new.yml` is written when needed.

## Installation

1. Download the latest ChatPlus jar from GitHub Releases.
2. Stop your Minecraft server.
3. Put the jar in your server `plugins` folder.
4. Start the server once to generate `plugins/ChatPlus/config.yml`.
5. Review the default modes and category rules.
6. Restart the server, or run `/chatadmin reload`.

## Player Commands

```text
/chat
/chat normal
/chat quiet
/chat focus
/chat off
/chat mode <normal|quiet|focus|off>
/chat allow <category>
/chat block <category>
/chat reset [category|all]
/chat mentions <on|off>
/chat status
/chat categories
```

Useful examples:

```text
/chat focus
/chat allow teleport-request
/chat block regular-chat
/chat mentions on
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

Examples:

```text
/chatadmin notify Hilal_h18 teleport-request 0xConflict wants to teleport to you.
/chatadmin broadcast purchase Hilal_h18 bought Premium.
```

`notify` and `broadcast` require `chatplus.notify` or `chatplus.admin`. Other admin commands require `chatplus.admin`.

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

Regular player chat is automatically categorized as `regular-chat`, unless it mentions the recipient. Mentioned messages are treated as `mentions`.

Messages sent directly by other plugins with `player.sendMessage(...)` cannot be reliably recategorized after the fact. For teleport requests, store messages, moderation warnings, or custom notices, send the message through `/chatadmin notify`, `/chatadmin broadcast`, or the `ChatPlusApi`.

## Default Modes

- `normal`: shows every category.
- `quiet`: hides regular public chat, keeps mentions and important categories.
- `focus`: starts similar to quiet, but is meant for stricter server-specific tuning.
- `off`: hides regular chat and mentions, keeps important categorized notices.

Mode defaults are fully configurable in `config.yml`.

## Permissions

```text
chatplus.command  - use /chat, default true
chatplus.admin    - administrative commands, default op
chatplus.notify   - /chatadmin notify and /chatadmin broadcast, default op
chatplus.bypass   - sender's regular chat bypasses recipient filters, default op
```

## API Integration

Other plugins can respect ChatPlus preferences by sending categorized messages through the API:

```java
ChatPlusApi.send(player, ChatCategory.TELEPORT_REQUEST, "0xConflict wants to teleport to you.");
ChatPlusApi.broadcast(ChatCategory.PURCHASE, "Hilal_h18 bought Premium.");
```

String category keys are also supported:

```java
ChatPlusApi.send(player, "server-notice", "Restart in 5 minutes.");
```

Choose categories by what the player needs to know, not by which plugin sent the message.

## Updating

ChatPlus does not overwrite your existing `config.yml`. If the bundled config changes, the plugin writes `plugins/ChatPlus/config-new.yml` so you can compare and copy new settings deliberately.

Player preferences are stored separately in `players.yml`.

## Build From Source

```powershell
mvn package
```

The compiled jar is written to:

```text
target/ChatPlus-0.1.0-SNAPSHOT.jar
```

## Troubleshooting

- If regular chat is not filtering, confirm the server is running Paper and no other chat plugin is cancelling or replacing the chat event first.
- If teleport or store messages still appear while chat is hidden, integrate that plugin through `ChatPlusApi` or send the message with `/chatadmin notify`.
- If mentions do not pass through, check `mentions.prefix`, `mentions.case-sensitive`, and the player's `/chat mentions` setting.
- If a player should always be heard, grant the sender `chatplus.bypass`.

## Support

For setup help, compatibility questions, and plugin information, use https://discord.sqware.gg.
