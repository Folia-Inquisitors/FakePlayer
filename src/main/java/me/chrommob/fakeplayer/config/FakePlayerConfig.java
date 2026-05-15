package me.chrommob.fakeplayer.config;

import me.hsgamer.hscore.config.annotation.Comment;
import me.hsgamer.hscore.config.annotation.ConfigPath;

public interface FakePlayerConfig {
    @ConfigPath({"server", "id"})
    @Comment("Unique server UUID. Required only when MySQL is enabled.")
    default String id() {
        return "";
    }

    @ConfigPath({"mysql", "enabled"})
    @Comment("Whether to use MySQL")
    default boolean mysqlEnabled() {
        return false;
    }

    @ConfigPath({"mysql", "host"})
    @Comment("MySQL host")
    default String mysqlHost() {
        return "";
    }

    @ConfigPath({"mysql", "port"})
    @Comment("MySQL port")
    default int mysqlPort() {
        return 3306;
    }

    @ConfigPath({"mysql", "database"})
    @Comment("MySQL database")
    default String mysqlDatabase() {
        return "";
    }

    @ConfigPath({"mysql", "username"})
    @Comment("MySQL username")
    default String mysqlUsername() {
        return "";
    }

    @ConfigPath({"mysql", "password"})
    @Comment("MySQL password")
    default String mysqlPassword() {
        return "";
    }

    @ConfigPath({"fake-players", "min"})
    @Comment("Minimum amount of fake players to appear on the server")
    default int minFakePlayers() {
        return 6;
    }

    @ConfigPath({"fake-players", "max"})
    @Comment("Maximum amount of fake players to appear on the server")
    default int maxFakePlayers() {
        return 10;
    }

    @ConfigPath({"activity", "join-leave", "frequency"})
    @Comment({
            "Fake activity timing.",
            "Frequency is measured in ticks.",
            "20 ticks = 1 second.",
            "1200 ticks = 1 minute.",
            "-1 = dynamic, learned from real server activity."
    })
    default int playerJoinQuitFrequency() {
        return -1;
    }

    @ConfigPath({"activity", "deaths", "enabled"})
    @Comment("Whether to display fake death messages")
    default boolean fakeDeathMessages() {
        return true;
    }

    @ConfigPath({"activity", "deaths", "frequency"})
    @Comment("Frequency of fake death messages in ticks. Set to -1 for dynamic timing.")
    default int fakeMessageFrequency() {
        return -1;
    }

    @ConfigPath({"activity", "achievements", "enabled"})
    @Comment("Whether to display fake achievement messages")
    default boolean fakeAchievementMessages() {
        return true;
    }

    @ConfigPath({"discordsrv", "forward", "join-leave"})
    @Comment("Whether to forward fake join and leave messages to DiscordSRV")
    default boolean discordSrvFakeJoinLeaveMessages() {
        return true;
    }

    @ConfigPath({"discordsrv", "forward", "deaths"})
    @Comment("Whether to forward fake death messages to DiscordSRV")
    default boolean discordSrvFakeDeathMessages() {
        return true;
    }

    @ConfigPath({"discordsrv", "forward", "achievements"})
    @Comment("Whether to forward fake achievement messages to DiscordSRV")
    default boolean discordSrvFakeAchievementMessages() {
        return true;
    }

    @ConfigPath({"activity", "achievements", "frequency"})
    @Comment("Frequency of fake achievement messages in ticks. Set to -1 for dynamic timing.")
    default int fakeAchievementFrequency() {
        return -1;
    }

    @ConfigPath({"learning", "outlier-drop-percent"})
    @Comment({"Drops timing samples that are unusually far from normal.", "0 drops nearly everything.", "100 keeps everything."})
    default int dynamicFrequencyOutliersDrop() {
        return 97;
    }

    void reloadConfig();
}
