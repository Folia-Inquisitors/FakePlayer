package me.chrommob.fakeplayer;

import com.github.retrooper.packetevents.PacketEvents;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import me.chrommob.fakeplayer.config.FakePlayerConfig;
import me.chrommob.fakeplayer.data.Database;
import me.chrommob.fakeplayer.data.FakeData;
import me.chrommob.fakeplayer.data.FakePlayerStorage;
import me.chrommob.fakeplayer.data.model.FakePlayerProfile;
import me.chrommob.fakeplayer.data.model.FakePlayerProfileFactory;
import me.chrommob.fakeplayer.data.model.StoredFakePlayerState;
import me.chrommob.fakeplayer.impl.Debugger;
import me.chrommob.fakeplayer.impl.FakeActivityScheduler;
import me.chrommob.fakeplayer.impl.FakePlayerImpl;
import me.chrommob.fakeplayer.impl.FakePlayerRegistry;
import me.chrommob.fakeplayer.impl.PlayerCommand;
import me.chrommob.fakeplayer.impl.PlayerCommandCompletion;
import me.chrommob.fakeplayer.identity.ExemptPlayerStorage;
import me.chrommob.fakeplayer.identity.PlayerTrustTracker;
import me.chrommob.fakeplayer.interaction.FakeInteractionGuard;
import me.chrommob.fakeplayer.model.FakeActivityModel;
import me.chrommob.fakeplayer.model.JoinQuitPopulationModel;
import me.chrommob.fakeplayer.model.RealActivityTemplates;
import me.chrommob.fakeplayer.packet.PlayerCount;
import me.chrommob.fakeplayer.placeholder.PlayerCountPlaceholder;
import me.hsgamer.hscore.bukkit.config.BukkitConfig;
import me.hsgamer.hscore.config.proxy.ConfigGenerator;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.IntSupplier;

@SuppressWarnings("unused")
public final class FakePlayer extends JavaPlugin implements Listener {
    private final FakePlayerConfig fakePlayerConfig = ConfigGenerator.newInstance(FakePlayerConfig.class, new BukkitConfig(this));
    private final FakePlayerRegistry fakePlayerRegistry = new FakePlayerRegistry(this);
    private Database database;
    private Debugger debugger;
    private FakePlayerStorage storage;
    private FakeActivityModel activityModel = FakeActivityModel.fromStoredState(StoredFakePlayerState.empty());
    private FakeActivityScheduler activityScheduler;
    private PlayerTrustTracker playerTrustTracker;
    private FakeInteractionGuard fakeInteractionGuard;
    private YamlConfiguration rawConfig;
    private boolean folia;
    private boolean startupComplete;

    @Override
    public void onLoad() {
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this));
        PacketEvents.getAPI().getSettings().reEncodeByDefault(true).checkForUpdates(false);
        PacketEvents.getAPI().load();
    }

    @Override
    public void onEnable() {
        folia = detectFolia();
        debugger = new Debugger(this);
        saveDefaultConfig();
        fakePlayerConfig.reloadConfig();
        reloadRawConfig();
        playerTrustTracker = new PlayerTrustTracker(new ExemptPlayerStorage(this, new File(getDataFolder(), "data")));
        playerTrustTracker.load();
        storage = new FakePlayerStorage(this, new File(getDataFolder(), "data"));
        loadStoredData();
        registerCommand();
        registerHooks();
        configureDatabase();
        startActivityScheduler();
        startupComplete = true;
    }

    @Override
    public void onDisable() {
        if (activityScheduler != null) {
            activityScheduler.cancel();
        }
        if (startupComplete) {
            saveData();
        }
        if (playerTrustTracker != null) {
            playerTrustTracker.save();
        }
        PacketEvents.getAPI().terminate();
    }

    @Override
    public void reloadConfig() {
        super.reloadConfig();
        fakePlayerConfig.reloadConfig();
        reloadRawConfig();
        if (fakeInteractionGuard != null) {
            fakeInteractionGuard.reload();
        }
        if (activityScheduler != null) {
            activityScheduler.resetDelays();
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        boolean trustExempted = playerTrustTracker != null
                && playerTrustTracker.recordJoin(event.getPlayer(), identityExemptAfterJoins());
        if (trustExempted) {
            fakePlayerRegistry.removePotentialFakePlayer(event.getPlayer().getName());
            removeFakePlayer(event.getPlayer().getName());
        }
        for (FakePlayerImpl fakePlayer : fakePlayerRegistry.getFakePlayerValues()) {
            fakePlayer.onPlayerJoin(event);
        }
        if (event.joinMessage() == null) {
            return;
        }
        learnJoinQuitActivity();

        String playerName = event.getPlayer().getName();
        boolean isExempted = event.getPlayer().hasPermission("fakeplayer.exempt") || trustExempted;
        FakePlayerProfile knownFakeData = fakePlayerRegistry.getPotentialFakePlayer(playerName);
        if (knownFakeData == null) {
            if (!isExempted) {
                fakePlayerRegistry.addPotentialFakePlayer(playerName, FakePlayerProfileFactory.from(event.getPlayer(), event));
            }
            return;
        }
        if (isExempted) {
            fakePlayerRegistry.removePotentialFakePlayer(playerName);
            removeFakePlayer(playerName);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (event.quitMessage() == null) {
            return;
        }
        learnJoinQuitActivity();
        FakePlayerProfile fakeData = fakePlayerRegistry.getPotentialFakePlayer(event.getPlayer().getName());
        if (fakeData != null) {
            fakePlayerRegistry.addPotentialFakePlayer(event.getPlayer().getName(), fakeData.withQuitMessage(event.quitMessage()));
        }
    }

    @EventHandler
    public void onPlayerAchievement(PlayerAdvancementDoneEvent event) {
        if (event.message() == null) {
            return;
        }
        Component template = RealActivityTemplates.achievementTemplate(event.getPlayer(), event.message());
        learnAchievementActivity(template);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (event.deathMessage() == null) {
            return;
        }
        String template = RealActivityTemplates.deathTemplate(event.getPlayer(), event.deathMessage(), Bukkit.getOnlinePlayers());
        learnDeathActivity(template);
    }

    private void loadStoredData() {
        StoredFakePlayerState storedData = storage.load();
        activityModel = FakeActivityModel.fromStoredState(storedData);
        fakePlayerRegistry.loadPotentialFakePlayers(storedData.potentialFakePlayers());
        fakePlayerRegistry.removePotentialFakePlayers(profile -> isExemptPlayerName(profile.name()));
        debugger.debug("Loaded " + fakePlayerRegistry.potentialSize() + " potential fake players, "
                + activityModel.achievementTemplateCount() + " achievement templates, "
                + activityModel.deathTemplateCount() + " death templates");
    }

    private void registerCommand() {
        PluginCommand reloadCommand = getCommand("reloadfakeplayer");
        if (reloadCommand != null) {
            reloadCommand.setExecutor((sender, command, label, args) -> {
                if (sender.hasPermission("fakeplayer.reload")) {
                    reloadConfig();
                    sender.sendMessage("Config reloaded");
                }
                return true;
            });
        }
    }

    private void registerHooks() {
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            getLogger().info("PlaceholderAPI found, enabling support");
            new PlayerCountPlaceholder().register();
        }
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(new PlayerCommand(), this);
        getServer().getPluginManager().registerEvents(new PlayerCommandCompletion(), this);
        fakeInteractionGuard = new FakeInteractionGuard(this);
        getServer().getPluginManager().registerEvents(fakeInteractionGuard, this);
        PacketEvents.getAPI().getEventManager().registerListener(new PlayerCount());
        PacketEvents.getAPI().init();
    }

    private void configureDatabase() {
        if (mysqlEnabled()) {
            UUID serverUuid;
            try {
                serverUuid = UUID.fromString(serverId());
            } catch (RuntimeException exception) {
                getLogger().warning("MySQL is enabled, but server.id is not a valid UUID. Disabling MySQL support.");
                database = null;
                return;
            }
            database = new Database(serverUuid,
                    mysqlHost(),
                    mysqlPort(),
                    mysqlDatabase(),
                    mysqlUsername(),
                    mysqlPassword()
            );
        } else {
            database = null;
        }
    }

    private void startActivityScheduler() {
        debugger.debug("Starting fake activity scheduler in " + (folia ? "Folia" : "Bukkit") + " mode");
        activityScheduler = new FakeActivityScheduler(this, folia,
                () -> resolveFrequency(playerJoinQuitFrequency(),
                        activityModel::getRandomJoinQuitFrequency),
                this::fakeDeathMessages,
                () -> resolveFrequency(fakeMessageFrequency(),
                        activityModel::getRandomDeathMessageFrequency),
                this::fakeAchievementMessages,
                () -> resolveFrequency(fakeAchievementFrequency(),
                        activityModel::getRandomAchievementFrequency),
                addTask,
                deathTask,
                achievementTask);
        activityScheduler.start();
    }

    private int resolveFrequency(int configuredFrequency, IntSupplier dynamicFrequency) {
        if (configuredFrequency != -1) {
            return configuredFrequency;
        }
        return dynamicFrequency.getAsInt();
    }

    private void learnJoinQuitActivity() {
        if (activityModel.learnJoinQuit(System.currentTimeMillis(), dynamicFrequencyOutliersDrop())) {
            debugger.debug("New time between joins: " + activityModel.frequencies().joinQuitFrequency().samples());
        }
    }

    private void learnDeathActivity(String deathTemplate) {
        if (activityModel.learnDeathMessage(deathTemplate, System.currentTimeMillis(), dynamicFrequencyOutliersDrop())) {
            debugger.debug("New time between messages: " + activityModel.frequencies().deathMessageFrequency().samples());
        }
    }

    private void learnAchievementActivity(Component achievementTemplate) {
        if (activityModel.learnAchievement(achievementTemplate, System.currentTimeMillis(), dynamicFrequencyOutliersDrop())) {
            debugger.debug("New time between achievements: " + activityModel.frequencies().achievementFrequency().samples());
        }
    }

    private boolean detectFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }

    private final Runnable addTask = () -> {
        int minFakePlayers = Math.max(0, minFakePlayers());
        int maxFakePlayers = Math.max(minFakePlayers, maxFakePlayers());
        JoinQuitPopulationModel.Decision decision = activityModel.nextJoinQuitDecision(
                minFakePlayers,
                maxFakePlayers,
                fakePlayerRegistry.size(),
                fakePlayerRegistry.availableFakePlayerCount()
        );

        if (decision.action() == JoinQuitPopulationModel.Action.JOIN) {
            FakePlayerProfile fakeData = fakePlayerRegistry.getNextAvailableFakePlayer(
                    profile -> activityModel.isPreferredFakePlayerProfile(profile.name())
            );
            if (fakeData == null) {
                debugger.debug("No available fake player profile to add");
            } else {
                debugger.debug("Adding fake player " + fakeData.name()
                        + " toward target " + decision.targetFakePlayers());
                activityModel.recordFakePlayerProfileUsed(fakeData.name());
                addFakePlayer(fakeData);
            }
        } else if (decision.action() == JoinQuitPopulationModel.Action.QUIT) {
            String name = fakePlayerRegistry.getRandomFakePlayerName();
            if (name != null) {
                debugger.debug("Removing fake player " + name
                        + " toward target " + decision.targetFakePlayers());
                activityModel.recordFakePlayerProfileUsed(name);
                removeFakePlayer(name);
            }
        }
        if (database != null) {
            database.updatePlayerCount(fakePlayerRegistry.size());
        }
    };

    private final Runnable deathTask = () -> {
        FakePlayerImpl fakePlayer = getRandomOnlineFakePlayer();
        if (fakePlayer == null) {
            return;
        }
        FakePlayerImpl otherFakePlayer = getRandomOnlineFakePlayer(fakePlayer);
        Component fakeDeathMessage = getDeathMessage(fakePlayer, otherFakePlayer != null);
        if (fakeDeathMessage != null) {
            if (otherFakePlayer == null) {
                otherFakePlayer = fakePlayer;
            }
            fakePlayer.death(fakeDeathMessage, otherFakePlayer);
        }
    };

    private final Runnable achievementTask = () -> {
        FakePlayerImpl fakePlayer = getRandomOnlineFakePlayer();
        if (fakePlayer == null) {
            return;
        }
        Component fakeAchievementMessage = getAchievementMessage(fakePlayer);
        if (fakeAchievementMessage != null) {
            fakePlayer.achievement(fakeAchievementMessage);
        }
    };

    private Component getAchievementMessage(FakePlayerImpl fakePlayer) {
        return activityModel.nextAchievementMessage(fakePlayer.getProfileName());
    }

    private Component getDeathMessage(FakePlayerImpl fakePlayer, boolean hasDistinctKiller) {
        return activityModel.nextDeathMessage(fakePlayer.getProfileName(), hasDistinctKiller);
    }

    private void saveData() {
        if (storage == null) {
            return;
        }
        storage.save(activityModel.toStoredState(fakePlayerRegistry.getPotentialFakePlayers()));
    }

    public Map<String, FakePlayerImpl> getFakePlayers() {
        return fakePlayerRegistry.getFakePlayers();
    }

    public void removeFakePlayer(String name) {
        fakePlayerRegistry.removeFakePlayer(name);
    }

    public void addFakePlayer(FakePlayerProfile fakeData) {
        fakePlayerRegistry.addFakePlayer(fakeData);
    }

    @Deprecated
    public void addFakePlayer(FakeData fakeData) {
        if (fakeData != null) {
            addFakePlayer(fakeData.toProfile());
        }
    }

    public FakePlayerImpl getRandomFakePlayer() {
        return fakePlayerRegistry.getRandomFakePlayer();
    }

    public FakePlayerImpl getRandomOnlineFakePlayer() {
        return fakePlayerRegistry.getRandomOnlineFakePlayer();
    }

    public FakePlayerImpl getRandomOnlineFakePlayer(FakePlayerImpl excludedFakePlayer) {
        return fakePlayerRegistry.getRandomOnlineFakePlayer(excludedFakePlayer);
    }

    public FakePlayerProfile getNextAvailableFakePlayer() {
        return fakePlayerRegistry.getNextAvailableFakePlayer();
    }

    public boolean isFakePlayer(String name) {
        return fakePlayerRegistry.isFakePlayer(name);
    }

    public boolean isSuspectedFakePlayerName(String name) {
        if (name == null || isExemptPlayerName(name)) {
            return false;
        }
        if (Bukkit.getOnlinePlayers().stream().anyMatch(player -> player.getName().equalsIgnoreCase(name))) {
            return false;
        }
        return fakePlayerRegistry.isFakePlayer(name) || fakePlayerRegistry.isPotentialFakePlayer(name);
    }

    public boolean isExemptPlayerName(String name) {
        return playerTrustTracker != null && playerTrustTracker.isExemptName(name);
    }

    public boolean isFakePlayer(Player player) {
        return fakePlayerRegistry.isFakePlayer(player);
    }

    public void addSelf(String name, FakePlayerImpl fakePlayer) {
        fakePlayerRegistry.addSelf(name, fakePlayer);
    }

    public boolean isFolia() {
        return folia;
    }

    public Debugger getDebugger() {
        return debugger;
    }

    public FakePlayerConfig getFakePlayerConfig() {
        return fakePlayerConfig;
    }

    public boolean discordSrvFakeJoinLeaveMessages() {
        return configBoolean("discordsrv.forward.join-leave",
                "discordsrv.forward-fake-join-leave-messages",
                fakePlayerConfig.discordSrvFakeJoinLeaveMessages());
    }

    public boolean discordSrvFakeDeathMessages() {
        return configBoolean("discordsrv.forward.deaths",
                "discordsrv.forward-fake-death-messages",
                fakePlayerConfig.discordSrvFakeDeathMessages());
    }

    public boolean discordSrvFakeAchievementMessages() {
        return configBoolean("discordsrv.forward.achievements",
                "discordsrv.forward-fake-achievement-messages",
                fakePlayerConfig.discordSrvFakeAchievementMessages());
    }

    public List<String> tpaGuardCommandPatterns() {
        return configStringList("interactions.tpa-guard.command-patterns-recognized",
                "interactions.tpa-guard.command-patterns",
                fakePlayerConfig.tpaGuardCommandPatterns());
    }

    private void reloadRawConfig() {
        rawConfig = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "config.yml"));
    }

    private String serverId() {
        return configString("server.id", "id", fakePlayerConfig.id());
    }

    private boolean mysqlEnabled() {
        return configBoolean("mysql.enabled", "mysql.enabled", fakePlayerConfig.mysqlEnabled());
    }

    private String mysqlHost() {
        return configString("mysql.host", "mysql.host", fakePlayerConfig.mysqlHost());
    }

    private int mysqlPort() {
        return configInt("mysql.port", "mysql.port", fakePlayerConfig.mysqlPort());
    }

    private String mysqlDatabase() {
        return configString("mysql.database", "mysql.database", fakePlayerConfig.mysqlDatabase());
    }

    private String mysqlUsername() {
        return configString("mysql.username", "mysql.username", fakePlayerConfig.mysqlUsername());
    }

    private String mysqlPassword() {
        return configString("mysql.password", "mysql.password", fakePlayerConfig.mysqlPassword());
    }

    private int minFakePlayers() {
        return configInt("fake-players.min", "min-fake-players", fakePlayerConfig.minFakePlayers());
    }

    private int maxFakePlayers() {
        return configInt("fake-players.max", "max-fake-players", fakePlayerConfig.maxFakePlayers());
    }

    private int playerJoinQuitFrequency() {
        return configInt("activity.join-leave.frequency",
                "player-join-quit-frequency",
                fakePlayerConfig.playerJoinQuitFrequency());
    }

    private boolean fakeDeathMessages() {
        return configBoolean("activity.deaths.enabled", "fake-death-messages", fakePlayerConfig.fakeDeathMessages());
    }

    private int fakeMessageFrequency() {
        return configInt("activity.deaths.frequency", "fake-message-frequency", fakePlayerConfig.fakeMessageFrequency());
    }

    private boolean fakeAchievementMessages() {
        return configBoolean("activity.achievements.enabled",
                "fake-achievement-messages",
                fakePlayerConfig.fakeAchievementMessages());
    }

    private int fakeAchievementFrequency() {
        return configInt("activity.achievements.frequency",
                "fake-achievement-frequency",
                fakePlayerConfig.fakeAchievementFrequency());
    }

    private int dynamicFrequencyOutliersDrop() {
        return configInt("learning.outlier-drop-percent",
                "dynamic-frequency-outliers-drop",
                fakePlayerConfig.dynamicFrequencyOutliersDrop());
    }

    private int identityExemptAfterJoins() {
        return configInt("identity.exempt-after-joins",
                "identity.exempt-after-joins",
                fakePlayerConfig.identityExemptAfterJoins());
    }

    private int configInt(String path, String legacyPath, int defaultValue) {
        if (rawConfig != null && rawConfig.contains(path)) {
            int value = rawConfig.getInt(path);
            if (value == defaultValue && rawConfig.contains(legacyPath)) {
                return rawConfig.getInt(legacyPath);
            }
            return value;
        }
        if (rawConfig != null && rawConfig.contains(legacyPath)) {
            return rawConfig.getInt(legacyPath);
        }
        return defaultValue;
    }

    private boolean configBoolean(String path, String legacyPath, boolean defaultValue) {
        if (rawConfig != null && rawConfig.contains(path)) {
            boolean value = rawConfig.getBoolean(path);
            if (value == defaultValue && rawConfig.contains(legacyPath)) {
                return rawConfig.getBoolean(legacyPath);
            }
            return value;
        }
        if (rawConfig != null && rawConfig.contains(legacyPath)) {
            return rawConfig.getBoolean(legacyPath);
        }
        return defaultValue;
    }

    private String configString(String path, String legacyPath, String defaultValue) {
        if (rawConfig != null && rawConfig.contains(path)) {
            String value = rawConfig.getString(path, defaultValue);
            if ((value == null || value.equals(defaultValue)) && rawConfig.contains(legacyPath)) {
                return rawConfig.getString(legacyPath, defaultValue);
            }
            return value;
        }
        if (rawConfig != null && rawConfig.contains(legacyPath)) {
            return rawConfig.getString(legacyPath, defaultValue);
        }
        return defaultValue;
    }

    private List<String> configStringList(String path, List<String> defaultValue) {
        if (rawConfig != null && rawConfig.contains(path)) {
            List<String> value = rawConfig.getStringList(path);
            if (!value.isEmpty()) {
                return value;
            }
        }
        return defaultValue;
    }

    private List<String> configStringList(String path, String legacyPath, List<String> defaultValue) {
        if (rawConfig != null && rawConfig.contains(path)) {
            List<String> value = rawConfig.getStringList(path);
            if (!value.isEmpty()) {
                return value;
            }
        }
        if (rawConfig != null && rawConfig.contains(legacyPath)) {
            List<String> value = rawConfig.getStringList(legacyPath);
            if (!value.isEmpty()) {
                return value;
            }
        }
        return defaultValue;
    }
}
