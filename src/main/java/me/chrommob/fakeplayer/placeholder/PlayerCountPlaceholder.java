package me.chrommob.fakeplayer.placeholder;

import me.chrommob.fakeplayer.FakePlayer;
import me.chrommob.fakeplayer.api.FakePlayerAPI;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public class PlayerCountPlaceholder extends PlaceholderExpansion {
    @Override
    public @NotNull String getIdentifier() {
        return "fakeplayer";
    }

    @Override
    public @NotNull String getAuthor() {
        return "ChromMob";
    }

    @Override
    public @NotNull String getVersion() {
        return FakePlayer.getPlugin(FakePlayer.class).getDescription().getVersion();
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        if (params.equals("count")) {
            return String.valueOf(FakePlayerAPI.getInstance().getFakePlayerCount() + Bukkit.getOnlinePlayers().size());
        }
        return "Invalid parameter. Use 'count' to get the amount of fake players.";
    }
}
