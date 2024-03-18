package me.chrommob.fakeplayer;

import com.github.retrooper.packetevents.PacketEvents;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import me.chrommob.config.ConfigManager;
import me.chrommob.fakeplayer.config.FakePlayerConfig;
import me.chrommob.fakeplayer.impl.FakeData;
import me.chrommob.fakeplayer.impl.FakePlayerImpl;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

@SuppressWarnings("unused")
public final class FakePlayer extends JavaPlugin implements Listener {
    private ConfigManager configManager;
    private boolean isFolia = false;
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
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            isFolia = true;
        } catch (ClassNotFoundException ignored) {
        }
        getCommand("reloadfakeplayer").setExecutor((sender, command, label, args) -> {
            if (sender.hasPermission("fakeplayer.reload")) {
                reloadConfig();
                sender.sendMessage("Config reloaded");
            }
            return true;
        });
        configManager = new ConfigManager(getDataFolder());
        configManager.addConfig(fakePlayerConfig);
        getServer().getPluginManager().registerEvents(this, this);
        PacketEvents.getAPI().init();
        startSchedulers();
    }

    private int addTaskId;
    private ScheduledTask addTaskS;
    private int deathTaskId;
    private ScheduledTask deathTaskS;
    private int achievementTaskId;
    private ScheduledTask achievementTaskS;
    private void startSchedulers() {
        if (isFolia)
            startFoliaSchedulers();
        else
            startBukkitSchedulers();
    }

    private void startFoliaSchedulers() {
        int playerJoinQuitFrequency = fakePlayerConfig.getKey("player-join-quit-frequency").getAsInt();
        int minFakePlayers = fakePlayerConfig.getKey("min-fake-players").getAsInt();
        int maxFakePlayers = fakePlayerConfig.getKey("max-fake-players").getAsInt();
        addTaskS = Bukkit.getGlobalRegionScheduler().runAtFixedRate(this, scheduledTask -> addTask.run(), playerJoinQuitFrequency, playerJoinQuitFrequency);
        boolean fakeDeathMessages = fakePlayerConfig.getKey("fake-death-messages").getAsBoolean();
        int fakeMessageFrequency = fakePlayerConfig.getKey("fake-message-frequency").getAsInt();
        if (fakeDeathMessages) {
            deathTaskS = Bukkit.getGlobalRegionScheduler().runAtFixedRate(this, scheduledTask -> deathTask.run(), fakeMessageFrequency, fakeMessageFrequency);
        }
        boolean fakeAchievementMessages = fakePlayerConfig.getKey("fake-achievement-messages").getAsBoolean();
        int fakeAchievementFrequency = fakePlayerConfig.getKey("fake-achievement-frequency").getAsInt();
        if (fakeAchievementMessages) {
            achievementTaskS= Bukkit.getGlobalRegionScheduler().runAtFixedRate(this, scheduledTask -> achievementTask.run(), fakeAchievementFrequency, fakeAchievementFrequency);
        }
    }

    private final Runnable addTask = () -> {
        int minFakePlayers = fakePlayerConfig.getKey("min-fake-players").getAsInt();
        int maxFakePlayers = fakePlayerConfig.getKey("max-fake-players").getAsInt();
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
    };

    private final Runnable deathTask = () -> {
        FakePlayerImpl fakePlayer = getRandomFakePlayer();
        calculateDeathPercentage();
        if (fakePlayer != null) {
            Component fakeDeathMessage = getDeathMessage();
            if (fakeDeathMessage == null) {
                return;
            }
            fakePlayer.death(fakeDeathMessage);
        }
    };

    private final Runnable achievementTask = () -> {
        FakePlayerImpl fakePlayer = getRandomFakePlayer();
        calculatePercentage();
        if (fakePlayer != null) {
            Component fakeAchievementMessage = getAchievementMessage();
            if (fakeAchievementMessage == null) {
                return;
            }
            fakePlayer.achievement(fakeAchievementMessage);
        }
    };

    private void startBukkitSchedulers() {
        int playerJoinQuitFrequency = fakePlayerConfig.getKey("player-join-quit-frequency").getAsInt();
        addTaskId = Bukkit.getScheduler().runTaskTimer(this, addTask, playerJoinQuitFrequency, playerJoinQuitFrequency).getTaskId();
        boolean fakeDeathMessages = fakePlayerConfig.getKey("fake-death-messages").getAsBoolean();
        int fakeMessageFrequency = fakePlayerConfig.getKey("fake-message-frequency").getAsInt();
        if (fakeDeathMessages) {
            deathTaskId = Bukkit.getScheduler().runTaskTimer(this, deathTask, fakeMessageFrequency, fakeMessageFrequency).getTaskId();
        }
        boolean fakeAchievementMessages = fakePlayerConfig.getKey("fake-achievement-messages").getAsBoolean();
        int fakeAchievementFrequency = fakePlayerConfig.getKey("fake-achievement-frequency").getAsInt();
        if (fakeAchievementMessages) {
            achievementTaskId = Bukkit.getScheduler().runTaskTimer(this, achievementTask, fakeAchievementFrequency, fakeAchievementFrequency).getTaskId();
        }
    }

    public void reloadConfig() {
        configManager.reloadConfig("config");
        Bukkit.getScheduler().cancelTask(addTaskId);
        Bukkit.getScheduler().cancelTask(deathTaskId);
        Bukkit.getScheduler().cancelTask(achievementTaskId);
        if (isFolia) {
            addTaskS.cancel();
            deathTaskS.cancel();
            achievementTaskS.cancel();
        }
        startSchedulers();
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

    private Component getAchievementMessage() {
        if (percentages.isEmpty()) {
            return null;
        }
        return percentages.get((int) (Math.random() * percentages.size()));
    }

    private Component getDeathMessage() {
        if (deathPercentages.isEmpty()) {
            return null;
        }
        return deathPercentages.get((int) (Math.random() * deathPercentages.size()));
    }

    private final List<Component> percentages = new ArrayList<>();

    private final Map<Integer, Component> map = new TreeMap<>();
    private final Map<String, Integer> map2 = new HashMap<>();
    @EventHandler
    public void onPlayerAchievement(PlayerAdvancementDoneEvent event) {
        if (event.message() == null) {
            return;
        }
        TextReplacementConfig replacementConfig = TextReplacementConfig.builder().match(event.getPlayer().getName()).replacement("%player%").build();
        TextReplacementConfig replacementConfig2 = TextReplacementConfig.builder().match(PlainTextComponentSerializer.plainText().serialize(event.getPlayer().displayName())).replacement("%player%").build();
        Component message = event.message().replaceText(replacementConfig).replaceText(replacementConfig2);
        String messageString = PlainTextComponentSerializer.plainText().serialize(message);
        if (map2.get(messageString) == null) {
            map2.put(messageString, 1);
            map.put(1, message);
        } else {
            int count = map2.get(messageString);
            map2.put(messageString, count + 1);
            map.put(count + 1, message);
        }
    }

    private final List<Component> deathPercentages = new ArrayList<>();
    private final Map<Integer, Component> deathMap = new TreeMap<>();
    private final Map<String, Integer> deathMap2 = new HashMap<>();
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (event.deathMessage() == null) {
            return;
        }
        TextReplacementConfig replacementConfig = TextReplacementConfig.builder().match(event.getPlayer().getName()).replacement("%player%").build();
        TextReplacementConfig replacementConfig2 = TextReplacementConfig.builder().match(PlainTextComponentSerializer.plainText().serialize(event.getPlayer().displayName())).replacement("%player%").build();
        Component message = event.deathMessage().replaceText(replacementConfig).replaceText(replacementConfig2);
        String messageString = PlainTextComponentSerializer.plainText().serialize(message);
        if (deathMap2.get(messageString) == null) {
            deathMap2.put(messageString, 1);
            deathMap.put(1, message);
        } else {
            int count = deathMap2.get(messageString);
            deathMap2.put(messageString, count + 1);
            deathMap.put(count + 1, message);
        }
    }

    private void calculateDeathPercentage() {
        int total = 0;
        for (int i : deathMap2.values()) {
            total += i;
        }
        for (Map.Entry<Integer, Component> entry : deathMap.entrySet()) {
            int count = entry.getKey();
            Component message = entry.getValue();
            int percentage = (int) ((count / (double) total) * 100);
            for (int i = 0; i < percentage; i++) {
                deathPercentages.add(message);
            }
        }
    }

    private void calculatePercentage() {
        int total = 0;
        for (int i : map2.values()) {
            total += i;
        }
        for (Map.Entry<Integer, Component> entry : map.entrySet()) {
            int count = entry.getKey();
            Component message = entry.getValue();
            int percentage = (int) ((count / (double) total) * 100);
            for (int i = 0; i < percentage; i++) {
                percentages.add(message);
            }
        }
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
