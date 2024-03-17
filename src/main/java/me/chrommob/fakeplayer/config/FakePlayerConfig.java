package me.chrommob.fakeplayer.config;

import me.chrommob.config.ConfigKey;
import me.chrommob.config.ConfigWrapper;

import java.util.ArrayList;
import java.util.List;

public class FakePlayerConfig extends ConfigWrapper {
    public FakePlayerConfig(String name) {
        super(name, FakePlayerConfig.getKeys());
    }

    private static List<ConfigKey> getKeys() {
        List<ConfigKey> keys = new ArrayList<>();

        keys.add(new ConfigKey("min-fake-players", 0, List.of("Minimum amount of fake players to appear on the server")));
        keys.add(new ConfigKey("max-fake-players", 10, List.of("Maximum amount of fake players to appear on the server")));
        keys.add(new ConfigKey("player-join-quit-frequency", 100, List.of("Frequency of fake players joining the server in ticks", "20 ticks = 1 second", "Set to -1 to use dynamic value based on real players")));

        keys.add(new ConfigKey("fake-death-messages", true, List.of("Whether to display fake death messages")));
        keys.add(new ConfigKey("fake-message-frequency", 100, List.of("Frequency of fake messages in ticks", "20 ticks = 1 second", "Set to -1 to use dynamic value based on real players")));

        keys.add(new ConfigKey("fake-achievement-messages", true, List.of("Whether to display fake achievement messages")));
        keys.add(new ConfigKey("fake-achievement-frequency", 100, List.of("Frequency of fake achievement messages in ticks", "20 ticks = 1 second", "Set to -1 to use dynamic value based on real players")));
        return keys;
    }
}
