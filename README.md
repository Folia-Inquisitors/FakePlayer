# FakePlayer
Website: None

# Official Discord 

https://discord.gg/aT9z7q7hX8

## Building instructions

./gradlew build

Requires Java 25 for Folia 26.1.2.

PacketEvents is shaded into the release jar by default, so a separate PacketEvents plugin is not required.

To build against an external PacketEvents plugin instead:

```
./gradlew build -PshadePE=false
```

That external build declares `depend: [packetevents]` in `plugin.yml`.

## Known bugs
None currently tracked for Folia 26.1.2.

## Runtime data

FakePlayer stores learned runtime data in `plugins/FakePlayer/data/state.yml` using a versioned model format.
Real-player join trust and exemptions are stored in `plugins/FakePlayer/data/exempted-players.yml`.
Older data files such as `frequencies.yml`, `map.yml`, `deathMap.yml`, and `potentialFakePlayers.yml` are still loaded and mirrored on save for compatibility.

## Behavior model

FakePlayer learns from real server activity, turns real messages into templates, and then generates fake activity from those learned templates.
Death and achievement templates replace the real player with `%player%`; death templates can replace a second real player with `%player2%`.
Generated fake messages only use online fake players for those placeholders, which prevents fake deaths or achievements from naming real players.
Achievement generation combines global weighted realism with per-fake-player progression. Each fake player remembers shown achievement templates, avoids repeats when possible, and gradually becomes more likely to receive later-game achievement categories.
See `docs/activity-models.md` for the model notes and Mermaid graphs.
 
## Description

*FakePlayer is suppose to emulate the activities of real players in achievement, death, and leave/join messages. It does this by collecting data on the most frequent death, achievement and join/leave frequencies and emulates them randomly based on what’s most common. This plugin also has support for DiscordSRV, SQL, Tab and Velocity support using FakePlayerAPI.*

*If you have velocity use FakePlayersAPI to display player count on the proxies.*

**These messages would follow logic such as not displaying messages of fake players who are in the game**. *That is to keep the realism of the fake players. These fake messages also only generate for fake players. So it wouldn’t say a real player was killed.*

> - Fake player placeholder which includes online players 
> 
> - An api so other plugins can hook into it. Ability to hook into other plugins, like YATPA, FASTMOTD.  
> 
>   It pull from past joined players usernames, and if they happen to join it replaced with another’s.
> 
> - Config option - fake join and leave  messages ( where you could set a range of how many fake players will leave and join within a configurable time period )  
> 
> - Config option - ( be able to turn off and on  ) fake dynamic death messages of people fake dying ( with a configurable time period of how often these messages occur ) ( death messages based on real rates ) 
> 
> - Config option - ( be able to turn off and on ) fake dynamic achievement message  ( with a configurable time rate of how often it occurs ) ( achievement message based on real rates  )
>
> - SQL and Velocity support.
>
> - DiscordSRV Support

## Default Config

```yaml
# Server identity
server:
  # Unique server UUID. Required only when MySQL is enabled.
  id: ""

# MySQL/shared player count settings
mysql:
  enabled: false
  host: ""
  port: 3306
  database: ""
  username: ""
  password: ""

# Fake player population
fake-players:
  min: 6
  max: 10

# Fake activity timing.
# Frequency is measured in ticks.
# 20 ticks = 1 second.
# 1200 ticks = 1 minute.
# -1 = dynamic, learned from real server activity.
activity:
  join-leave:
    frequency: -1

  deaths:
    enabled: true
    frequency: -1

  achievements:
    enabled: true
    frequency: -1

# Learning settings for dynamic timing.
learning:
  # Drops timing samples that are unusually far from normal.
  # 0 drops nearly everything.
  # 100 keeps everything.
  outlier-drop-percent: 97

# Real-player identity tracking
identity:
  # Exempt a real player from being used as a fake player after this many real joins.
  exempt-after-joins: 3

# Interaction protections
interactions:
  tpa-guard:
    # Blocks TPA-like commands when the target is a suspected fake player.
    enabled: true
    # Message sent to the player whose command was blocked. Supports %target%.
    deny-message: "That player is not accepting teleport requests."
    # Regex patterns for TPA-like commands.
    # Each pattern must expose a named group called target, or use the first capture group.
    command-patterns:
      - "^/(?:tpa|tpask|call|etpa|essentials:tpa)\\s+(?<target>[A-Za-z0-9_]{3,16})(?:\\s|$)"
      - "^/(?:tpahere|tphere|etpahere|essentials:tpahere)\\s+(?<target>[A-Za-z0-9_]{3,16})(?:\\s|$)"

# DiscordSRV forwarding
discordsrv:
  forward:
    join-leave: true
    deaths: true
    achievements: true
```

## Placeholders
>
> - %fakeplayer_count%

## Permissions

> - fakeplayer.reload
> - fakeplayer.exempt
> - fakeplayer.interaction.bypass

### Hard Dependencies
> None for the default shaded build.
>
> PacketEvents is required only when building with `-PshadePE=false`.

### Soft Dependencies
>
> - [Tab Plugin](https://www.spigotmc.org/resources/tab-1-7-x-1-21-10.57806/) *Purpose: FakePlayer count on Tab*
> - [FakePlayerAPI](https://github.com/Folia-Inquisitors/FakePlayerAPI) *Purpose: Shows Player count on velocity*
> - [YATPA](https://github.com/Folia-Inquisitors/YATPA) *Purpose: A TPA plugin made for FakePlayers*
> - [FakePlayerYATPA](https://github.com/Folia-Inquisitors/FakePlayerYATPA) *Purpose: Purpose: Denies TPA requests to fake players*
> - [GrimYATPA](https://github.com/Folia-Inquisitors/GrimYATPA) *Purpose: Disables teleportation checks when using [Grim](https://github.com/GrimAnticheat/Grim)*
> - [RegularRank](https://github.com/Folia-Inquisitors/RegularRank) *Purpose: Adds a rank "regular" so it can be exempted from Fakeplayer names*

### Admin commands 
> fakeplayer:reloadfakeplayer
>
> fakeplayer:rfp
>

### Folia inquisitors

[<img src="https://github.com/Folia-Inquisitors.png" width=80 alt="Folia-Inquisitors">](https://github.com/orgs/Folia-Inquisitors/repositories)
[<img src="https://github.com/ChromMob.png" width=80 alt="C">](https://github.com/ChromeMob)
[<img src="https://github.com/HSGamer.png" width=80 alt="HSGamer">](https://github.com/HSGamer)
