package me.chrommob.fakeplayer.api;

import me.chrommob.fakeplayer.FakePlayer;
import me.chrommob.fakeplayer.impl.FakePlayerImpl;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Set;

@SuppressWarnings("unused")
public class FakePlayerAPI {
    private static FakePlayerAPI instance;
    public static FakePlayerAPI getInstance() {
        if (instance == null) {
            instance = new FakePlayerAPI();
        }
        return instance;
    }

    public Map<String, FakePlayerImpl> getFakePlayers() {
        return FakePlayer.getPlugin(FakePlayer.class).getFakePlayers();
    }

    public Set<String> getFakePlayerNames() {
        return FakePlayer.getPlugin(FakePlayer.class).getFakePlayers().keySet();
    }

    public int getFakePlayerCount() {
        return FakePlayer.getPlugin(FakePlayer.class).getFakePlayers().size();
    }

    public boolean isFakePlayer(String name) {
        return FakePlayer.getPlugin(FakePlayer.class).isFakePlayer(name);
    }

    public boolean isFakePlayer(Player player) {
        return FakePlayer.getPlugin(FakePlayer.class).isFakePlayer(player);
    }
}
