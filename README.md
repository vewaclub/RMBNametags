[🇷🇺Прочитать на Русском](https://github.com/vewaclub/RMBNametags/blob/master/README_ru_ru.md)

# RMBNametags

[![CodeFactor](https://www.codefactor.io/repository/github/vewaclub/rmbnametags/badge)](https://www.codefactor.io/repository/github/vewaclub/rmbnametags)
[![Modrinth downloads](https://img.shields.io/modrinth/dt/rmbnametags)](https://modrinth.com/plugin/rmbnametags)

[![Modrinth](https://raw.githubusercontent.com/gist/jenchanws/842eee8428e1e0aec20de4594878156a/raw/0dbefc2fcbec362d14f1689acb807183ceffdbe1/modrinth.svg)](https://modrinth.com/plugin/rmbnametags)

A simple Minecraft plugin to hide player names and show them on right-click in actionbar. PlaceholderAPI supported include Relational placeholders.

![Nickname pops up in actionbar when you press rmb](https://cdn.modrinth.com/data/cached_images/3232f03c8108ea611b1bdf8b42e6ce3320641d7c.png)

## Features

- Hide player name tags by default
- Show names on right-click (actionbar or subtitle)
- PlaceholderAPI support with relational placeholders
- Custom hide names with special format
- Hide/show commands with custom names
- Invisibility respect option

## Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/rmbnametags reload` | Reload plugin configuration | `rmbnametags.reload` |
| `/rmbnametags hide` | Hide your own nickname | `rmbnametags.hide-show` |
| `/rmbnametags hide <player>` | Hide another player's nickname | `rmbnametags.hide-show.other` |
| `/rmbnametags hide <player> <custom name>` | Hide with custom display name | `rmbnametags.hide-show.other` + `rmbnametags.hide.custom` |
| `/rmbnametags show` | Show your own nickname | `rmbnametags.hide-show` |
| `/rmbnametags show <player>` | Show another player's nickname | `rmbnametags.hide-show.other` |

## Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `rmbnametags.reload` | Reload plugin configuration | op |
| `rmbnametags.hide-show` | Hide or show own nametag | true |
| `rmbnametags.hide-show.other` | Hide or show other players nametags | op |
| `rmbnametags.hide.custom` | Set custom names when hiding | op |

## Configuration

```yml
# display time in seconds (min 1s)
display-time: 3

# Change nickname format here
# Use & for colors or HEX (#RRGGBB) and {PLAYER_NAME} variable
name-format: "&a&l{PLAYER_NAME}"

# Format for hidden players (if empty, nothing will be shown)
hide-name-format: "&a&lUnknown"

# If true, plugin will NOT show nametag when the target player is invisible
respect-invisibility: false

# Where to display the nickname: "actionbar" (default) or "subtitle"
display-location: "actionbar"

# If true, hidden players will still show their custom name when clicked
show-hidden-players: true

# Messages (English by default)
Messages:
  Reload: "&a&lRMBNametags config reloaded!"
  Not-Reload: "&c&lAn error occurred during a config reload"
  No-Permission: "&cYou don't have permission to use this command!"
  Player-Not-Found: "&cPlayer not found or offline!"
  Console-Specify-Player: "&cConsole must specify a player!"
  Hide-Self: "&aYour nickname is now hidden"
  Hide-Other: "&aPlayer %player%'s nickname is now hidden"
  Show-Self: "&aYour nickname is now visible"
  Show-Other: "&aPlayer %player%'s nickname is now visible"
  Hide-Custom-Self: "&aYour nickname is now hidden with custom name: %custom_name%"
  Hide-Custom-Other: "&aPlayer %player%'s nickname is now hidden with custom name: %custom_name%"
  Already-hidden: "&cPlayer %player%'s nickname is already hidden"
  Already-Visible: "&cPlayer %player%'s nickname is already visible"
```

### bStats
[![Статистика использования плагина на bStats](https://bstats.org/signatures/bukkit/RMBNametags.svg)](https://bstats.org/plugin/bukkit/RMBNametags)