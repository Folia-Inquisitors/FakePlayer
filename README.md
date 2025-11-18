# FakePlayer
Website: None

# Official Discord 

https://discord.gg/aT9z7q7hX8

## Building instructions

./gradlew shadowjar

*Disclaimer: There is no dependency to build this, but you will need PacketEvents in the plugins folder.* 
 
## Description

*FakePlayer is suppose to emulate the activities of real players in achievement, death, and leave/join messages. It does this by collecting data on the most frequent death, achievement and join/leave frequencies and emulates them randomly based on what’s most common. This plugin also has support for DiscordSRV, SQL, Tab and Velocity support using FakePlayerAPI.*

*If you have velocity use FakePlayersAPI to display player count on the proxies.*

**These messages would follow logic such as not displaying messages of fake players who aren’t in the game**. *That is to keep the realism of the fake players. These fake messages also only generate for fake players. So it wouldn’t say a real player was killed, or killed a real player.*


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

```
# Server UUID
id: 
# MySQL settings
mysql:
  # Whether to use MySQL
  enabled: true
  # MySQL host
  host: 
  # MySQL port
  port: 3365
  # MySQL database
  database: 
  # MySQL username
  username: 
  # MySQL password
  password: 
# Minimum amount of fake players to appear on the server
min-fake-players: 6
# Maximum amount of fake players to appear on the server
max-fake-players: 10
# Frequency of fake players joining the server in ticks
# 20 ticks = 1 second
# Set to -1 to use dynamic value based on real players
player-join-quit-frequency: -1
# Whether to display fake death messages
fake-death-messages: true
# Frequency of fake messages in ticks
# 20 ticks = 1 second
# Set to -1 to use dynamic value based on real players
fake-message-frequency: -1
# Whether to display fake achievement messages
fake-achievement-messages: true
# Frequency of fake achievement messages in ticks
# 20 ticks = 1 second
# Set to -1 to use dynamic value based on real players
fake-achievement-frequency: -1
# When the data is very farther than the usual, it will be dropped
# Set to 0 to drop basically everything
# Set to 100 to drop nothing
dynamic-frequency-outliers-drop: 97

```

## Placeholders
>
> - %fakeplayer_count%

## Permissions

> fakeplayer.reload
> fakeplayer.exempt
> 

### SoftDependencies
>
> - [Tab Plugin](https://www.spigotmc.org/resources/tab-1-7-x-1-21-10.57806/)
> - FakePlayerAPI
> - KeepInv
> - 

### Admin commands 
> fakeplayer:reloadfakeplayer
>
> fakeplayer:rfp
>

### Folia inquisitors

[<img src="https://github.com/Folia-Inquisitors.png" width=80 alt="Folia-Inquisitors">](https://github.com/orgs/Folia-Inquisitors/repositories)
[<img src="https://github.com/ChromMob.png" width=80 alt="C">](https://github.com/ChromeMob)
[<img src="https://github.com/HSGamer.png" width=80 alt="HSGamer">](https://github.com/HSGamer)
