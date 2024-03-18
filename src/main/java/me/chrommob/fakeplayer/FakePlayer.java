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
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

@SuppressWarnings("unused")
public final class FakePlayer extends JavaPlugin implements Listener {
    private File dataFolder;
    private File percentagesFile;
    private File mapFile;
    private File map2File;
    private File deathPercentagesFile;
    private File deathMapFile;
    private File deathMap2File;
    private final Yaml yaml = new Yaml();
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
        dataFolder = new File(getDataFolder(), "data");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        percentagesFile = new File(dataFolder, "percentages.yml");
        mapFile = new File(dataFolder, "map.yml");
        map2File = new File(dataFolder, "map2.yml");
        deathPercentagesFile = new File(dataFolder, "deathPercentages.yml");
        deathMapFile = new File(dataFolder, "deathMap.yml");
        deathMap2File = new File(dataFolder, "deathMap2.yml");
        loadData();
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
        int playerJoinQuitFrequency = fakePlayerConfig.getKey("player-join-quit-frequency").getAsInt();§
        if (playerJoinQuitFrequency == -1) {
            playerJoinQuitFrequency = getDynamicPlayerJoinQuitFrequency();
        }
        int minFakePlayers = fakePlayerConfig.getKey("min-fake-players").getAsInt();
        int maxFakePlayers = fakePlayerConfig.getKey("max-fake-players").getAsInt();
        addTaskS = Bukkit.getGlobalRegionScheduler().runAtFixedRate(this, scheduledTask -> addTask.run(), playerJoinQuitFrequency, playerJoinQuitFrequency);
        boolean fakeDeathMessages = fakePlayerConfig.getKey("fake-death-messages").getAsBoolean();
        int fakeMessageFrequency = fakePlayerConfig.getKey("fake-message-frequency").getAsInt();
        if (fakeDeathMessages) {
            if (fakeMessageFrequency == -1) {
                fakeMessageFrequency = getDynamicFakeMessageFrequency();
            }
            deathTaskS = Bukkit.getGlobalRegionScheduler().runAtFixedRate(this, scheduledTask -> deathTask.run(), fakeMessageFrequency, fakeMessageFrequency);
        }
        boolean fakeAchievementMessages = fakePlayerConfig.getKey("fake-achievement-messages").getAsBoolean();
        int fakeAchievementFrequency = fakePlayerConfig.getKey("fake-achievement-frequency").getAsInt();
        if (fakeAchievementMessages) {
            if (fakeAchievementFrequency == -1) {
                fakeAchievementFrequency = getDynamicFakeAchievementFrequency();
            }
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

    private List<Component> percentages;
    private Map<Integer, Component> map;
    private Map<String, Integer> map2;
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

    private List<Component> deathPercentages;
    private Map<Integer, Component> deathMap;
    private Map<String, Integer> deathMap2;
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
        saveData();
        PacketEvents.getAPI().terminate();
    }

    private void loadData() {
        Object percentagesObject = loadFromFile(percentagesFile);
        Object mapObject = loadFromFile(mapFile);
        Object map2Object = loadFromFile(map2File);
        Object deathPercentagesObject = loadFromFile(deathPercentagesFile);
        Object deathMapObject = loadFromFile(deathMapFile);
        Object deathMap2Object = loadFromFile(deathMap2File);

        if (percentagesObject instanceof List) {
            percentages = (List<Component>) percentagesObject;
        } else {
            percentages = new ArrayList<>();
        }
        if (mapObject instanceof Map) {
            map = (Map<Integer, Component>) mapObject;
        } else {
            map = new HashMap<>();
        }
        if (map2Object instanceof Map) {
            map2 = (Map<String, Integer>) map2Object;
        } else {
            map2 = new HashMap<>();
        }
        if (deathPercentagesObject instanceof List) {
            deathPercentages = (List<Component>) deathPercentagesObject;
        } else {
            deathPercentages = new ArrayList<>();
        }
        if (deathMapObject instanceof Map) {
            deathMap = (Map<Integer, Component>) deathMapObject;
        } else {
            deathMap = new HashMap<>();
        }
        if (deathMap2Object instanceof Map) {
            deathMap2 = (Map<String, Integer>) deathMap2Object;
        } else {
            deathMap2 = new HashMap<>();
        }
    }

    private void saveData() {
        String percentagesString = yaml.dump(percentages);
        String mapString = yaml.dump(map);
        String map2String = yaml.dump(map2);
        String deathPercentagesString = yaml.dump(deathPercentages);
        String deathMapString = yaml.dump(deathMap);
        String deathMap2String = yaml.dump(deathMap2);

        writeToFile(percentagesFile, percentagesString);
        writeToFile(mapFile, mapString);
        writeToFile(map2File, map2String);
        writeToFile(deathPercentagesFile, deathPercentagesString);
        writeToFile(deathMapFile, deathMapString);
        writeToFile(deathMap2File, deathMap2String);
    }

    private Object loadFromFile(File file) {
        try {
            return yaml.load(Files.newBufferedReader(file.toPath()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void writeToFile(File file, String string) {
        try {
            file.createNewFile();
            Files.write(file.toPath(), string.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
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

    public boolean isFakePlayer(String name) {
        return fakePlayers.get(name) != null;
    }

    public boolean isFakePlayer(Player player) {
        return fakePlayers.get(player.getName()) != null;
    }
}
