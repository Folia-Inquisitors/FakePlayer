package me.chrommob.fakeplayer;

import com.github.retrooper.packetevents.PacketEvents;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import me.chrommob.config.ConfigManager;
import me.chrommob.fakeplayer.config.FakePlayerConfig;
import me.chrommob.fakeplayer.impl.FakeData;
import me.chrommob.fakeplayer.impl.FakePlayerImpl;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unused")
public final class FakePlayer extends JavaPlugin implements Listener {
    private final FakePlayerConfig fakePlayerConfig = new FakePlayerConfig("config");
    private final Map<String, FakePlayerImpl> fakePlayers = new HashMap<>();

    @Override
    public void onLoad() {
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this));
        // Are all listeners read only?
        PacketEvents.getAPI().getSettings().reEncodeByDefault(true)
                .checkForUpdates(false)
                .bStats(true);
        PacketEvents.getAPI().load();
    }


    @Override
    public void onEnable() {
        ConfigManager configManager = new ConfigManager(getDataFolder());
        configManager.addConfig(fakePlayerConfig);
        getServer().getPluginManager().registerEvents(this, this);
        int playerJoinQuitFrequency = fakePlayerConfig.getKey("player-join-quit-frequency").getAsInt();
        int minFakePlayers = fakePlayerConfig.getKey("min-fake-players").getAsInt();
        int maxFakePlayers = fakePlayerConfig.getKey("max-fake-players").getAsInt();
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            int random = (int) (Math.random() * (maxFakePlayers - minFakePlayers + 1) + minFakePlayers);
            if (fakePlayers.size() < random) {
                for (int i = 0; i < random - fakePlayers.size(); i++) {
                    FakeData fakeData = getNextAvailableFakePlayer();
                    if (fakeData != null) {
                        addFakePlayer(fakeData.getName());
                    }
                }
            }
            if (fakePlayers.size() > random) {
                for (int i = 0; i < fakePlayers.size() - random; i++) {
                    int randomIndex = (int) (Math.random() * fakePlayers.size());
                    String name = (String) fakePlayers.keySet().toArray()[randomIndex];
                    removeFakePlayer(name);
                }
            }
        }, 0, playerJoinQuitFrequency);
        boolean fakeDeathMessages = fakePlayerConfig.getKey("fake-death-messages").getAsBoolean();
        int fakeMessageFrequency = fakePlayerConfig.getKey("fake-message-frequency").getAsInt();
        if (fakeDeathMessages) {
            Bukkit.getScheduler().runTaskTimer(this, () -> {
                FakePlayerImpl fakePlayer = getRandomFakePlayer();
                if (fakePlayer != null) {
                    fakePlayer.death();
                }
            }, 0, fakeMessageFrequency);
        }
        boolean fakeAchievementMessages = fakePlayerConfig.getKey("fake-achievement-messages").getAsBoolean();
        int fakeAchievementFrequency = fakePlayerConfig.getKey("fake-achievement-frequency").getAsInt();
        if (fakeAchievementMessages) {
            Bukkit.getScheduler().runTaskTimer(this, () -> {
                FakePlayerImpl fakePlayer = getRandomFakePlayer();
                if (fakePlayer != null) {
                    fakePlayer.achievement();
                }
            }, 0, fakeAchievementFrequency);
        }
        PacketEvents.getAPI().init();
    }

    private final Map<String, FakeData> potentialFakePlayers = new HashMap<>();
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        potentialFakePlayers.put(event.getPlayer().getName(), new FakeData(event.getPlayer(), event));
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        potentialFakePlayers.get(event.getPlayer().getName()).setQuitMessage(event.quitMessage());
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public Map<String, FakePlayerImpl> getFakePlayers() {
        return new HashMap<>(fakePlayers);
    }

    public void removeFakePlayer(String name) {
        if (fakePlayers.get(name) == null) {
            return;
        }
        fakePlayers.get(name).quit();
        fakePlayers.remove(name);
    }

    public void addFakePlayer(String name) {
        FakePlayerImpl fakePlayer = new FakePlayerImpl(potentialFakePlayers.get(name));
        Bukkit.getPluginManager().registerEvents(fakePlayer, this);
        fakePlayers.put(name, fakePlayer);
    }

    public FakePlayerImpl getRandomFakePlayer() {
        if (fakePlayers.isEmpty()) {
            return null;
        }
        int randomIndex = (int) (Math.random() * fakePlayers.size());
        return (FakePlayerImpl) fakePlayers.values().toArray()[randomIndex];
    }

    public FakeData getNextAvailableFakePlayer() {
        for (FakeData fakeData : potentialFakePlayers.values()) {
            if (!fakePlayers.containsKey(fakeData.getName()) && fakeData.isReady() && Bukkit.getPlayer(fakeData.getName()) == null) {
                return fakeData;
            }
        }
        return null;
    }
}
