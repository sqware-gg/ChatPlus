# Changelog

All notable changes to this project will be documented here.

## Unreleased

### Added

- Interactive item sharing with `[item]`, `[i]`, `[hand]`, `[offhand]`, and `[off]`.
- Clickable read-only inventory and ender chest snapshots with `[inventory]`, `[inv]`, `[enderchest]`, `[ender]`, and `[ec]`.
- Discord rendering API for bridge plugins to relay shared items and inventories as useful plain text.
- Configurable share placeholders, cooldowns, permissions, display format, click behavior, snapshot expiry, Discord fallback text, and failure messages.
- `{name}` and `{plain_item}` item-share display placeholders for clean item names while preserving Paper item hover tooltips.

## 0.1.0 - 2026-05-25

Initial public release.

### Added

- Player chat modes: normal, quiet, focus, and off.
- Per-recipient public chat filtering.
- Mention passthrough.
- Per-category allow/block overrides.
- Persistent player preferences.
- Admin notifications and broadcasts.
- Public `ChatPlusApi` for categorized message integrations.
- Modern Paper chat support with legacy fallback.
- GitHub Actions build workflow and release jar.
