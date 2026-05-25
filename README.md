# ChatPlus

[![Build](https://github.com/sqware-gg/ChatPlus/actions/workflows/build.yml/badge.svg)](https://github.com/sqware-gg/ChatPlus/actions/workflows/build.yml)

ChatPlus is a Minecraft chat focus plugin for Paper servers. It lets players hide regular public chat while still receiving important messages such as mentions, teleport requests, private notices, purchase alerts, warnings, and server announcements.

Use it for SMPs, survival servers, lifesteal servers, roleplay communities, event servers, or any public server where players want control over chat noise without missing useful information.

## Links

- Website: https://sqware.gg
- Support and plugin updates: https://discord.sqware.gg

## Screenshots

Screenshot capture guidance is available in [docs/screenshots](docs/screenshots/README.md).

## Compatibility

- Server software: Paper, with modern Paper chat support and legacy chat fallback
- API target: Paper `1.18.2`
- Java: `17+`
- Build tool: Maven

As of May 2026, Paper is the supported target. Other Bukkit-based servers may load the legacy chat listener, but Paper provides the most reliable per-recipient chat filtering.

## Why Server Owners Use It

- Give players a built-in way to reduce public chat spam.
- Keep staff notices, teleport requests, warnings, and server information visible.
- Let players choose their own focus mode instead of forcing one global chat rule.
- Provide a simple API for other plugins to send categorized messages.

## Features

- Player chat modes: `normal`, `quiet`, `focus`, and `off`.
- Per-recipient filtering for normal player chat.
- Mention passthrough with configurable `@Player` matching.
- Category allow/block overrides for each player.
- Persistent preferences in `plugins/ChatPlus/players.yml`.
- Admin notifications and broadcasts.
- Public `ChatPlusApi` for teleport, store, moderation, and server utility plugins.
- Config-safe updates through `config-new.yml`.

## Installation

1. Download the latest jar from the GitHub Releases page.
2. Stop your Minecraft server.
3. Put the jar in the server `plugins` folder.
4. Start the server once to generate `plugins/ChatPlus/config.yml`.
5. Review chat modes, categories, and mention settings.
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

Examples:

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

Messages sent directly by another plugin with `player.sendMessage(...)` cannot be categorized after they have already been sent. For teleport requests, store alerts, moderation warnings, or custom notices, send the message through `/chatadmin notify`, `/chatadmin broadcast`, or `ChatPlusApi`.

## Default Modes

- `normal`: shows every category.
- `quiet`: hides regular public chat, keeps mentions and important categories.
- `focus`: starts similar to quiet and is intended for stricter server-specific tuning.
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

Other plugins can respect player chat preferences by sending categorized messages through the API:

```java
ChatPlusApi.send(player, ChatCategory.TELEPORT_REQUEST, "0xConflict wants to teleport to you.");
ChatPlusApi.broadcast(ChatCategory.PURCHASE, "Hilal_h18 bought Premium.");
```

String category keys are also supported:

```java
ChatPlusApi.send(player, "server-notice", "Restart in 5 minutes.");
```

## Updating

ChatPlus does not overwrite your existing `config.yml`. If the bundled config changes, the plugin writes `plugins/ChatPlus/config-new.yml` so you can compare and copy new options.

Player preferences are stored separately in `players.yml`.

Release history is tracked in [CHANGELOG.md](CHANGELOG.md).

## Build From Source

```powershell
mvn package
```

The compiled jar is written to:

```text
target/ChatPlus-0.1.0.jar
```

## Troubleshooting

- Regular chat is not filtering: confirm the server is running Paper and no other chat plugin replaces the chat event first.
- Teleport or store messages still appear: integrate that plugin through `ChatPlusApi` or send messages with `/chatadmin notify`.
- Mentions do not pass through: check `mentions.prefix`, `mentions.case-sensitive`, and `/chat mentions`.
- A sender should always be visible: grant that sender `chatplus.bypass`.

## Support

For setup help, compatibility questions, and plugin updates, use https://discord.sqware.gg.
