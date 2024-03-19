package me.chrommob.fakeplayer;

import com.github.retrooper.packetevents.PacketEvents;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import me.chrommob.config.ConfigManager;
import me.chrommob.fakeplayer.config.FakePlayerConfig;
import me.chrommob.fakeplayer.data.FrequencyData;
import me.chrommob.fakeplayer.data.FakeData;
import me.chrommob.fakeplayer.impl.FakePlayerImpl;
import me.chrommob.fakeplayer.packet.PlayerCount;
import me.chrommob.fakeplayer.packet.PlayerPing;
import me.chrommob.fakeplayer.placeholder.PlayerCountPlaceholder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.serializer.json.JSONComponentSerializer;
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

import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;

@SuppressWarnings("unused")
public final class FakePlayer extends JavaPlugin implements Listener {
    private File percentagesFile;
    private File mapFile;
    private File deathPercentagesFile;
    private File deathMapFile;
    private File potentialFakePlayersFile;
    private File frequenciesFile;
    private final Yaml yaml = new Yaml();
    private ConfigManager configManager;
    private final FakePlayerConfig fakePlayerConfig = new FakePlayerConfig("config");
    private final Map<String, FakePlayerImpl> fakePlayers = new HashMap<>();
    private FrequencyData frequencyData;

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
        File dataFolder = new File(getDataFolder(), "data");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            getLogger().info("PlaceholderAPI found, enabling support");
            new PlayerCountPlaceholder().register();
        }
        percentagesFile = new File(dataFolder, "percentages.yml");
        mapFile = new File(dataFolder, "map.yml");
        deathPercentagesFile = new File(dataFolder, "deathPercentages.yml");
        deathMapFile = new File(dataFolder, "deathMap.yml");
        potentialFakePlayersFile = new File(dataFolder, "potentialFakePlayers.yml");
        frequenciesFile = new File(dataFolder, "frequencies.yml");
        if (!percentagesFile.exists()) {
            try {
                percentagesFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (!mapFile.exists()) {
            try {
                mapFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (!deathPercentagesFile.exists()) {
            try {
                deathPercentagesFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (!deathMapFile.exists()) {
            try {
                deathMapFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (!potentialFakePlayersFile.exists()) {
            try {
                potentialFakePlayersFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (!frequenciesFile.exists()) {
            try {
                frequenciesFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        String frequenciesFileString = readFromFile(frequenciesFile);
        if (frequenciesFileString == null || frequenciesFileString.isEmpty()) {
            frequencyData = new FrequencyData();
        } else {
            frequencyData = FrequencyData.fromString(frequenciesFileString);
        }
        loadData();
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
        PacketEvents.getAPI().getEventManager().registerListener(new PlayerCount());
        PacketEvents.getAPI().getEventManager().registerListener(new PlayerPing());
        PacketEvents.getAPI().init();
        startSchedulers();
    }

    private long shouldRunAdd = 0;
    private long shouldRunDeath = 0;
    private long shouldRunAchievement = 0;
    private void startSchedulers() {
        new Thread(() -> {
            while (true) {
                int addFrequency = fakePlayerConfig.getKey("player-join-quit-frequency").getAsInt();
                int deathFrequency = fakePlayerConfig.getKey("fake-message-frequency").getAsInt();
                int achievementFrequency = fakePlayerConfig.getKey("fake-achievement-frequency").getAsInt();
                boolean fakeDeathMessages = fakePlayerConfig.getKey("fake-death-messages").getAsBoolean();
                boolean fakeAchievementMessages = fakePlayerConfig.getKey("fake-achievement-messages").getAsBoolean();
                if (addFrequency == -1) {
                    addFrequency = frequencyData.getRandomPlayerJoinQuitFrequency();
                }
                if (deathFrequency == -1) {
                    deathFrequency = frequencyData.getRandomMessageFrequency();
                }
                if (achievementFrequency == -1) {
                    achievementFrequency = frequencyData.getRandomAchievementFrequency();
                }
                int lowest = getLowestFromThatIsNotMinusOne(addFrequency, deathFrequency, achievementFrequency);
                lowest = (int) Math.min(lowest, getNearestShouldRun());
                if (lowest == -1) {
                    lowest = 100;
                }
                if (addFrequency != -1) {
                    if (shouldRunAdd == 0) {
                        shouldRunAdd = System.currentTimeMillis() + addFrequency * 50L;
                    }
                }
                if (deathFrequency != -1) {
                    if (shouldRunDeath == 0) {
                        shouldRunDeath = System.currentTimeMillis() + deathFrequency * 50L;
                    }
                }
                if (achievementFrequency != -1) {
                    if (shouldRunAchievement == 0) {
                        shouldRunAchievement = System.currentTimeMillis() + achievementFrequency * 50L;
                    }
                }
                if (addFrequency != -1) {
                    if (shouldRunAdd < System.currentTimeMillis()) {
                        addTask.run();
                        shouldRunAdd = System.currentTimeMillis() + addFrequency * 50L;
                    }
                }
                if (deathFrequency != -1) {
                    if (shouldRunDeath < System.currentTimeMillis() && fakeDeathMessages) {
                        deathTask.run();
                        shouldRunDeath = System.currentTimeMillis() + deathFrequency * 50L;
                    }
                }
                if (achievementFrequency != -1) {
                    if (shouldRunAchievement < System.currentTimeMillis() && fakeAchievementMessages) {
                        achievementTask.run();
                        shouldRunAchievement = System.currentTimeMillis() + achievementFrequency * 50L;
                    }
                }
                try {
                    Thread.sleep(lowest * 50L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private int getLowestFromThatIsNotMinusOne(int number1, int number2, int number3) {
        if (number1 == -1) {
            if (number2 == -1) {
                return number3;
            }
            if (number3 == -1) {
                return number2;
            }
            return Math.min(number2, number3);
        }
        if (number2 == -1) {
            if (number3 == -1) {
                return number1;
            }
            return Math.min(number1, number3);
        }
        if (number3 == -1) {
            return Math.min(number1, number2);
        }
        return Math.min(Math.min(number1, number2), number3);
    }

    private long getNearestShouldRun() {
        long currentTime = System.currentTimeMillis();
        long first = shouldRunAdd - currentTime;
        long second = shouldRunDeath - currentTime;
        long third = shouldRunAchievement - currentTime;
        if (first < 0) {
            first = Long.MAX_VALUE;
        }
        if (second < 0) {
            second = Long.MAX_VALUE;
        }
        if (third < 0) {
            third = Long.MAX_VALUE;
        }
        return Math.min(Math.min(first, second), third)/50L;
    }

    private final Runnable addTask = () -> {
        int minFakePlayers = fakePlayerConfig.getKey("min-fake-players").getAsInt();
        int maxFakePlayers = fakePlayerConfig.getKey("max-fake-players").getAsInt();
        int random = (int) (Math.random() * (maxFakePlayers - minFakePlayers + 1) + minFakePlayers);
        if (fakePlayers.size() < random) {
            FakeData fakeData = getNextAvailableFakePlayer();
            if (fakeData != null) {
                addFakePlayer(fakeData);
            }
        }
        if (fakePlayers.size() > random) {
            int randomIndex = (int) (Math.random() * fakePlayers.size());
            String name = (String) fakePlayers.keySet().toArray()[randomIndex];
            removeFakePlayer(name);
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

    public void reloadConfig() {
        configManager.reloadConfig("config");
    }

    private final Map<String, FakeData> potentialFakePlayers = new HashMap<>();
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (event.joinMessage() == null) {
            return;
        }
        potentialFakePlayers.put(event.getPlayer().getName(), new FakeData(event.getPlayer(), event));
        frequencyData.newTimeBetweenJoins();
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (event.quitMessage() == null) {
            return;
        }
        potentialFakePlayers.get(event.getPlayer().getName()).setQuitMessage(event.quitMessage());
        frequencyData.newTimeBetweenJoins();
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
    private Map<String, Integer> map;
    @EventHandler
    public void onPlayerAchievement(PlayerAdvancementDoneEvent event) {
        if (event.message() == null) {
            return;
        }
        TextReplacementConfig replacementConfig = TextReplacementConfig.builder().match(event.getPlayer().getName()).replacement("%player%").build();
        TextReplacementConfig replacementConfig2 = TextReplacementConfig.builder().match(PlainTextComponentSerializer.plainText().serialize(event.getPlayer().displayName())).replacement("%player%").build();
        Component message = event.message().replaceText(replacementConfig).replaceText(replacementConfig2);
        String messageString = JSONComponentSerializer.json().serialize(message);
        if (map.get(messageString) == null) {
            map.put(messageString, 1);
        } else {
            int count = map.get(messageString);
            map.put(messageString, count + 1);
        }
        frequencyData.newTimeBetweenAchievements();
    }

    private List<Component> deathPercentages;
    private Map<String, Integer> deathMap;
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (event.deathMessage() == null) {
            return;
        }
        TextReplacementConfig replacementConfig = TextReplacementConfig.builder().match(event.getPlayer().getName()).replacement("%player%").build();
        TextReplacementConfig replacementConfig2 = TextReplacementConfig.builder().match(PlainTextComponentSerializer.plainText().serialize(event.getPlayer().displayName())).replacement("%player%").build();
        Component message = event.deathMessage().replaceText(replacementConfig).replaceText(replacementConfig2);
        String messageString = JSONComponentSerializer.json().serialize(message);
        if (deathMap.get(messageString) == null) {
            deathMap.put(messageString, 1);
        } else {
            int count = deathMap.get(messageString);
            deathMap.put(messageString, count + 1);
        }
        frequencyData.newTimeBetweenMessages();
    }

    private void calculateDeathPercentage() {
        int total = 0;
        for (int i : deathMap.values()) {
            total += i;
        }
        deathPercentages.clear();
        for (Map.Entry<String, Integer> entry : deathMap.entrySet()) {
            int count = entry.getValue();
            Component message = JSONComponentSerializer.json().deserialize(entry.getKey());
            int percentage = (int) ((count / (double) total) * 100);
            for (int i = 0; i < percentage; i++) {
                deathPercentages.add(message);
            }
        }
    }

    private void calculatePercentage() {
        int total = 0;
        for (int i : map.values()) {
            total += i;
        }
        percentages.clear();
        for (Map.Entry<String, Integer> entry : map.entrySet()) {
            int count = entry.getValue();
            Component message = JSONComponentSerializer.json().deserialize(entry.getKey());
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
        Object deathPercentagesObject = loadFromFile(deathPercentagesFile);
        Object deathMapObject = loadFromFile(deathMapFile);

        if (percentagesObject instanceof List) {
            List<String> percentagesDumpable = (List<String>) percentagesObject;
            percentages = new ArrayList<>();
            for (String string : percentagesDumpable) {
                percentages.add(JSONComponentSerializer.json().deserialize(string));
            }
        } else {
            percentages = new ArrayList<>();
        }
        if (mapObject instanceof Map) {
            map = (Map<String, Integer>) mapObject;
        } else {
            map = new HashMap<>();
        }
        if (deathPercentagesObject instanceof List) {
            List<String> deathPercentagesDumpable = (List<String>) deathPercentagesObject;
            deathPercentages = new ArrayList<>();
            for (String string : deathPercentagesDumpable) {
                deathPercentages.add(JSONComponentSerializer.json().deserialize(string));
            }
        } else {
            deathPercentages = new ArrayList<>();
        }
        if (deathMapObject instanceof Map) {
            deathMap = (Map<String, Integer>) deathMapObject;
        } else {
            deathMap = new HashMap<>();
        }

        Object potentialFakePlayersObject = loadFromFile(potentialFakePlayersFile);
        if (potentialFakePlayersObject instanceof Map) {
            Map<String, String> potentialFakePlayersDumpable = (Map<String, String>) potentialFakePlayersObject;
            for (Map.Entry<String, String> entry : potentialFakePlayersDumpable.entrySet()) {
                potentialFakePlayers.put(entry.getKey(), FakeData.fromString(entry.getValue()));
            }
        }
    }

    private String readFromFile(File file) {
        try {
            return Files.readString(file.toPath());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void saveData() {
        List<String> percentagesDumpable = new ArrayList<>();
        List<String> deathPercentagesDumpable = new ArrayList<>();
        for (Component component : percentages) {
            percentagesDumpable.add(JSONComponentSerializer.json().serialize(component));
        }
        for (Component component : deathPercentages) {
            deathPercentagesDumpable.add(JSONComponentSerializer.json().serialize(component));
        }

        String percentagesString = yaml.dump(percentagesDumpable);
        String mapString = yaml.dump(map);
        String deathPercentagesString = yaml.dump(deathPercentagesDumpable);
        String deathMapString = yaml.dump(deathMap);

        Map<String, String> potentialFakePlayersDumpable = new HashMap<>();
        for (Map.Entry<String, FakeData> entry : potentialFakePlayers.entrySet()) {
            potentialFakePlayersDumpable.put(entry.getKey(), entry.getValue().toString());
        }
        String potentialFakePlayersString = yaml.dump(potentialFakePlayersDumpable);

        writeToFile(percentagesFile, percentagesString);
        writeToFile(mapFile, mapString);
        writeToFile(deathPercentagesFile, deathPercentagesString);
        writeToFile(deathMapFile, deathMapString);
        writeToFile(potentialFakePlayersFile, potentialFakePlayersString);
        writeToFile(frequenciesFile, frequencyData.toString());
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
            //Overwrite the previous content
            Files.write(file.toPath(), string.getBytes(), TRUNCATE_EXISTING);
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

    public void addFakePlayer(FakeData fakeData) {
        FakePlayerImpl fakePlayer = new FakePlayerImpl(fakeData);
        Bukkit.getPluginManager().registerEvents(fakePlayer, this);
        fakePlayers.put(fakeData.getName(), fakePlayer);
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

    public void addSelf(String name, FakePlayerImpl fakePlayer) {
        fakePlayers.put(name, fakePlayer);
    }
}
