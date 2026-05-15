# Third-party notices

FakePlayer can bundle third-party libraries into the release jar.

## PacketEvents

The default build shades PacketEvents so the server does not need a separate PacketEvents plugin.

- Project: https://github.com/retrooper/packetevents
- License: GPL-3.0-or-later
- Default dependency: `com.github.retrooper:packetevents-spigot:2.12.1`

To build without bundling PacketEvents, run:

```sh
./gradlew build -PshadePE=false
```

That build expects PacketEvents to be installed as a server plugin.

## HScore

FakePlayer shades HScore config libraries for configuration support.

- Project: https://github.com/HSGamer/HScore
- Default dependencies: `me.hsgamer:hscore-config-proxy:4.9.0`, `me.hsgamer:hscore-bukkit-config:4.9.0`
