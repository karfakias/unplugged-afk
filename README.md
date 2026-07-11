# Unplugged-AFK

[![License](https://img.shields.io/github/license/Fallen-Breath/fabric-mod-template.svg)](http://www.gnu.org/licenses/lgpl-3.0.html)
[![workflow](https://github.com/sakura-ryoko/unplugged-afk/actions/workflows/gradle.yml/badge.svg)](https://github.com/sakura-ryoko/unplugged-afk/actions/workflows/gradle.yml)

**Unplugged-AFK** is an eco-friendly Minecraft mod that allows players to "go unplugged." By utilizing this mod, players can spawn a bot of themselves to stay AFK at their farms while they disconnect. This allows you to safely shut off your computer, saving electricity and promoting a "Green" approach to server farming!

## Prerequisites & Installation
* **Mod Loader:** Fabric
* **Minecraft Version:** 1.19.2 up to 26.2

## Features
* **Go Green:** Turn off your PC while your player-bot continues to AFK for you.
* **Customizable Timeouts:** Set specific durations for how long a bot should remain active. The default timeout is 129600.
* **Admin Control:** Server administrators have full command control to spawn, kill, or manage unplugged players.
* **Safety Options:** Configurations allow you to reset health upon death, disable damage for unplugged players, or even hide them from other players and operators.

## Commands

### Player Commands
* **`/unplugged [<minutes>] [<reason>]`**: Disconnects you and leaves an unplugged bot in your place.
  * *Note: This command cannot be used by the single-player server owner*.

### Admin Commands
Requires permission level 3 by default.
* **`/unplugged-admin`**: Displays information about the mod.
* **`/unplugged-admin save`**: Saves the current configuration.
* **`/unplugged-admin reload`**: Reloads the configuration.
* **`/unplugged-admin list [players|unplugged|all]`**: Lists currently tracked players or active unplugged bots.
* **`/unplugged-admin info [<player>]`**: Displays detailed debug information for a specific player.
* **`/unplugged-admin purge`**: Purges players and resyncs the current player map.
* **`/unplugged-admin spawn <player> [<minutes>] [<reason>]`**: Manually spawns an unplugged bot for a specified player.
* **`/unplugged-admin kill <target>`**: Removes/kills an active unplugged bot.

## Configuration

The mod features a highly customizable `config.json` file. Key options include:

| Category      | Option                             | Description                                                                                               | Default  |
|:--------------|:-----------------------------------|:----------------------------------------------------------------------------------------------------------|:---------|
| **Main**      | `unpluggedAfkEnabled`              | Toggles the entire AFK feature.                                                                           | `true`   |
| **Main**      | `debugMode`                        | Enables debugging output.                                                                                 | `false`  |
| **Main**      | `reducedListDebugInfo`             | Enables Reduced output for various information commands.                                                  | `true`   |
| **Unplugged** | `resetHealthUponDeath`             | Resets the AFK bots Health when killed.                                                                   | `false`  |
| **Unplugged** | `unpluggedHidePlayer`              | Makes the bot invisible to others.                                                                        | `false`  |
| **Unplugged** | `unpluggedHideFromOps`             | Makes the bot invisible to to Operators as well.                                                          | `false`  |
| **Unplugged** | `unpluggedDisableDamage`           | Prevents the AFK bot from taking damage.                                                                  | `false`  |
| **Unplugged** | `defaultUnpluggedTimeout`          | Set the default timeout (in minutes).  The default is for 90 days.                                        | `129600` |
| **Unplugged** | `unpluggedCommandPermissions`      | Permission level required to use `/unplugged`.                                                            | `0`      |
| **Unplugged** | `unpluggedAdminCommandPermissions` | Permission level for `/unplugged-admin`.                                                                  | `3`      |
| **Messages**  | `broadcastMessages`                | Enables the broadcasting of Unplugged status messages.                                                    | `false`  |
| **Messages**  | `displayDuration`                  | Enables the duration display of Unplugged status messages.                                                | `true`   |
| **Messages**  | `hideUnpluggedJoin`                | Enables the disabling of the default `player has joined` messages while bots are spawned, where possible. | `true`   |

**Messages & Formatting:**
Server owners can extensively customize broadcast messages and formatting. For example, the default kick message when a player successfully uses the command is `"§6Congrats, you've been unplugged§r"`.
The `duration` and the `timeDate` are CoreLib time formatting options for the broadcast messages while `displayDuration` is enabled.

[![Join Sakura's RyokoCraft Discord](https://sakuraryoko.com/files/1398873/discord-300px.png)](https://discord.gg/ryokocraftmc)
