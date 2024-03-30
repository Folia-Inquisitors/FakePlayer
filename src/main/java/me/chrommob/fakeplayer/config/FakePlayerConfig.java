package me.chrommob.fakeplayer.config;

import me.chrommob.config.ConfigKey;
import me.chrommob.config.ConfigWrapper;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class FakePlayerConfig extends ConfigWrapper {
    public FakePlayerConfig(String name) {
        super(name, FakePlayerConfig.getKeys());
    }

    private static List<ConfigKey> getKeys() {
        List<ConfigKey> keys = new ArrayList<>();

        keys.add(new ConfigKey("id", UUID.randomUUID().toString(), List.of("Server UUID")));

        List<ConfigKey> mysqlKeys = new ArrayList<>();
        mysqlKeys.add(new ConfigKey("enabled", false, List.of("Whether to use MySQL")));
        mysqlKeys.add(new ConfigKey("host", "localhost", List.of("MySQL host")));
        mysqlKeys.add(new ConfigKey("port", 3306, List.of("MySQL port")));
        mysqlKeys.add(new ConfigKey("database", "minecraft", List.of("MySQL database")));
        mysqlKeys.add(new ConfigKey("username", "root", List.of("MySQL username")));
        mysqlKeys.add(new ConfigKey("password", "", List.of("MySQL password")));
        keys.add(new ConfigKey("mysql", mysqlKeys, List.of("MySQL settings")));

        keys.add(new ConfigKey("min-fake-players", 0, List.of("Minimum amount of fake players to appear on the server")));
        keys.add(new ConfigKey("max-fake-players", 10, List.of("Maximum amount of fake players to appear on the server")));
        keys.add(new ConfigKey("player-join-quit-frequency", 100, List.of("Frequency of fake players joining the server in ticks", "20 ticks = 1 second", "Set to -1 to use dynamic value based on real players")));

        keys.add(new ConfigKey("fake-death-messages", true, List.of("Whether to display fake death messages")));
        keys.add(new ConfigKey("fake-message-frequency", 100, List.of("Frequency of fake messages in ticks", "20 ticks = 1 second", "Set to -1 to use dynamic value based on real players")));

        keys.add(new ConfigKey("fake-achievement-messages", true, List.of("Whether to display fake achievement messages")));
        keys.add(new ConfigKey("fake-achievement-frequency", 100, List.of("Frequency of fake achievement messages in ticks", "20 ticks = 1 second", "Set to -1 to use dynamic value based on real players")));

        keys.add(new ConfigKey("dynamic-frequency-outliers-drop", 97, List.of("When the data is very farther than the usual, it will be dropped", "Set to 0 to drop basically everything", "Set to 100 to drop nothing")));

        return keys;
    }
}
