package me.chrommob.fakeplayer.config;

import me.hsgamer.hscore.config.annotation.Comment;
import me.hsgamer.hscore.config.annotation.ConfigPath;

import java.util.UUID;

public interface FakePlayerConfig {
    @ConfigPath("id")
    @Comment("Server UUID")
    default String id() {
        return UUID.randomUUID().toString();
    }

    @ConfigPath({"mysql", "enabled"})
    @Comment("Whether to use MySQL")
    default boolean mysqlEnabled() {
        return false;
    }

    @ConfigPath({"mysql", "host"})
    @Comment("MySQL host")
    default String mysqlHost() {
        return "localhost";
    }

    @ConfigPath({"mysql", "port"})
    @Comment("MySQL port")
    default int mysqlPort() {
        return 3306;
    }

    @ConfigPath({"mysql", "database"})
    @Comment("MySQL database")
    default String mysqlDatabase() {
        return "minecraft";
    }

    @ConfigPath({"mysql", "username"})
    @Comment("MySQL username")
    default String mysqlUsername() {
        return "root";
    }

    @ConfigPath({"mysql", "password"})
    @Comment("MySQL password")
    default String mysqlPassword() {
        return "";
    }

    @ConfigPath("min-fake-players")
    @Comment("Minimum amount of fake players to appear on the server")
    default int minFakePlayers() {
        return 0;
    }

    @ConfigPath("max-fake-players")
    @Comment("Maximum amount of fake players to appear on the server")
    default int maxFakePlayers() {
        return 10;
    }

    @ConfigPath("player-join-quit-frequency")
    @Comment({"Frequency of fake players joining the server in ticks", "20 ticks = 1 second", "Set to -1 to use dynamic value based on real players"})
    default int playerJoinQuitFrequency() {
        return 100;
    }

    @ConfigPath("fake-death-messages")
    @Comment("Whether to display fake death messages")
    default boolean fakeDeathMessages() {
        return true;
    }

    @ConfigPath("fake-message-frequency")
    @Comment({"Frequency of fake messages in ticks", "20 ticks = 1 second", "Set to -1 to use dynamic value based on real players"})
    default int fakeMessageFrequency() {
        return 100;
    }

    @ConfigPath("fake-achievement-messages")
    @Comment("Whether to display fake achievement messages")
    default boolean fakeAchievementMessages() {
        return true;
    }

    @ConfigPath("fake-achievement-frequency")
    @Comment({"Frequency of fake achievement messages in ticks", "20 ticks = 1 second", "Set to -1 to use dynamic value based on real players"})
    default int fakeAchievementFrequency() {
        return 100;
    }

    @ConfigPath("dynamic-frequency-outliers-drop")
    @Comment({"When the data is very farther than the usual, it will be dropped", "Set to 0 to drop basically everything", "Set to 100 to drop nothing"})
    default int dynamicFrequencyOutliersDrop() {
        return 97;
    }

    void reloadConfig();
}
