# FakePlayer
Website: None

# Official Discord 

https://discord.gg/aT9z7q7hX8

## Building instructions

./gradlew build
 
## Description

This allows you others to mute people that are annoying and spam chat. This is meant to be a simple plugin for servers. It is highly optimized and heavily tested. Feel free to constribute,.

### Admin commands 
> fakeplayer:reloadfakeplayer
>
> fakeplayer:rfp
>
> fakevote

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
player-join-quit-frequency: 8000
# Whether to display fake death messages
fake-death-messages: true
# Frequency of fake messages in ticks
# 20 ticks = 1 second
# Set to -1 to use dynamic value based on real players
fake-message-frequency: 6000
# Whether to display fake achievement messages
fake-achievement-messages: true
# Frequency of fake achievement messages in ticks
# 20 ticks = 1 second
# Set to -1 to use dynamic value based on real players
fake-achievement-frequency: 2000
# When the data is very farther than the usual, it will be dropped
# Set to 0 to drop basically everything
# Set to 100 to drop nothing
dynamic-frequency-outliers-drop: 97

```
## Documentation

### Folia inquisitors

[<img src="https://github.com/Folia-Inquisitors.png" width=80 alt="Folia-Inquisitors">](https://github.com/orgs/Folia-Inquisitors/repositories)
[<img src="https://github.com/ChromMob.png" width=80 alt="C">](https://github.com/ChromeMob)
