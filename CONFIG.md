### Example Config:

```json
{
  "___comment": "Unplugged-AFK-Development Version-0.1.0 Config",
  "config_date": "Fri, 10 Jul 2026 15:28:59 -0400",
  "main": {
    "unpluggedAfkEnabled": true,
    "debugMode": false,
    "reducedListDebugInfo": true
  },
  "unplugged": {
    "resetHealthUponDeath": false,
    "defaultUnpluggedTimeout": 129600,
    "unpluggedCommandPermissions": 0,
    "unpluggedAdminCommandPermissions": 3,
    "unpluggedDisableDamage": false,
    "unpluggedHidePlayer": false,
    "unpluggedHideFromOps": false
  },
  "messages": {
    "broadcastMessages": false,
    "hideUnpluggedJoin": false,
    "displayDuration": true,
    "defaultUnpluggedReason": "§7unplugged§r",
    "unpluggedKickMessage": "§6Congrats, you've been unplugged§r",
    "unpluggedExpiredReason": "§eTimeout expired§r",
    "unpluggedStarted": "§r §eis now unplugged§r",
    "unpluggedPunctuation": "§e,§r ",
    "unpluggedReturned": "§r §ehas returned§r",
    "whenReturnDurationPrefix": " §7(Gone for: §a",
    "whenReturnDurationSuffix": "§7)",
    "duration": {
      "option": "PRETTY",
      "customFormat": ""
    },
    "timeDate": {
      "option": "RFC1123",
      "customFormat": ""
    }
  },
  "players": [
    {
      "uuid": "eb662411-185a-3694-b480-0c259ae00075",
      "name": "Player487",
      "state": {
        "enabled": true,
        "time": 129600,
        "timeout": 7775824189,
        "reason": "§7unplugged§r"
      },
      "pos": {
        "location": "minecraft:overworld",
        "x": -87,
        "y": 89,
        "z": -16,
        "yaw": -38.9999,
        "pitch": 26.850018
      },
      "game": {
        "gameMode": "creative",
        "flying": true
      }
    }
  ]
}
```