package me.chrommob.fakeplayer.impl;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.protocol.player.TextureProperty;
import com.github.retrooper.packetevents.protocol.player.UserProfile;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoRemove;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoUpdate;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import me.chrommob.fakeplayer.FakePlayer;
import me.chrommob.fakeplayer.data.model.FakePlayerProfile;
import me.chrommob.fakeplayer.util.SystemChatComponentSanitizer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class FakePlayerImpl implements Listener {
    private final FakePlayer plugin = FakePlayer.getPlugin(FakePlayer.class);
    private FakePlayerProfile fakeData;
    private final UUID uuid = UUID.randomUUID();
    private boolean isOnline;
    private final WrapperPlayServerPlayerInfoUpdate playerInfoPacket;
    private ScheduledTask scheduledTask;
    private BukkitTask bukkitTask;

    public FakePlayerImpl(FakePlayerProfile fakePlayer) {
        this.fakeData = fakePlayer;
        playerInfoPacket = createPlayerInfoPacket();
        onJoin();
        if (plugin.isFolia()) {
            scheduledTask = Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, scheduledTask -> updateLatency(),
                    7 * 20L, 30 * 20L);
        } else {
            bukkitTask = Bukkit.getScheduler().runTaskTimer(plugin, this::updateLatency, 7 * 20L, 30 * 20L);
        }
    }

    public boolean isOnline() {
        return isOnline;
    }

    public WrapperPlayServerPlayerInfoUpdate createPlayerInfoPacket() {
        UserProfile userProfile = getUserProfile();
        WrapperPlayServerPlayerInfoUpdate.PlayerInfo playerInfo = new WrapperPlayServerPlayerInfoUpdate.PlayerInfo(
                userProfile, true, 0, GameMode.SURVIVAL, Component.text(fakeData.name()), null);
        EnumSet<WrapperPlayServerPlayerInfoUpdate.Action> actions = EnumSet.of(
                WrapperPlayServerPlayerInfoUpdate.Action.ADD_PLAYER,
                WrapperPlayServerPlayerInfoUpdate.Action.UPDATE_LISTED,
                WrapperPlayServerPlayerInfoUpdate.Action.UPDATE_LATENCY);
        return new WrapperPlayServerPlayerInfoUpdate(actions, playerInfo);
    }

    private void updateLatency() {
        if (isOnline) {
            int latency;
            boolean isHighLatency = Math.random() < 0.05;
            if (isHighLatency) {
                latency = ThreadLocalRandom.current().nextInt(1000);
            } else {
                latency = ThreadLocalRandom.current().nextInt(50, 200);
            }
            plugin.getDebugger()
                    .debug("Setting latency of " + fakeData.name() + " with UUID " + uuid + " to " + latency);
            playerInfoPacket.getEntries().get(0).setLatency(latency);
            for (Player player : Bukkit.getOnlinePlayers()) {
                PacketEvents.getAPI().getPlayerManager().sendPacket(player, clone(playerInfoPacket));
            }
        }
    }

    @NotNull
    private UserProfile getUserProfile() {
        TextureProperty textureProperty = null;
        if (fakeData.texture() != null && fakeData.signature() != null) {
            textureProperty = new TextureProperty("textures", fakeData.texture(), fakeData.signature());
        }
        UserProfile userProfile;
        if (textureProperty == null) {
            userProfile = new UserProfile(uuid, fakeData.name());
        } else {
            userProfile = new UserProfile(uuid, fakeData.name(), List.of(textureProperty));
        }
        return userProfile;
    }

    private void onJoin() {
        isOnline = true;
        broadcastToOnlinePlayers(fakeData.joinMessage());
        if (shouldForwardJoinLeaveToDiscordSrv()) {
            DiscordSrvBridge.sendJoinMessage(fakeData, uuid);
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            PacketEvents.getAPI().getPlayerManager().sendPacket(player, clone(playerInfoPacket));
        }
    }

    public void quit() {
        if (isOnline) {
            broadcastQuitMessage();
            cancelLatencyTask();
        }
        WrapperPlayServerPlayerInfoRemove removePacket = new WrapperPlayServerPlayerInfoRemove(uuid);
        for (Player player : Bukkit.getOnlinePlayers()) {
            PacketEvents.getAPI().getPlayerManager().sendPacket(player, removePacket);
        }
        if (plugin.isFolia()) {
            Bukkit.getGlobalRegionScheduler().runDelayed(plugin, scheduledTask -> {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    PacketEvents.getAPI().getPlayerManager().sendPacket(player, removePacket);
                }
            }, 20);
        } else {
            Bukkit.getScheduler().runTaskLater(FakePlayer.getPlugin(FakePlayer.class), () -> {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    PacketEvents.getAPI().getPlayerManager().sendPacket(player, removePacket);
                }
            }, 20);
        }
    }

    public void onPlayerJoin(PlayerJoinEvent event) {
        if (event.getPlayer().getName().equals(fakeData.name())) {
            if (event.joinMessage() != null) {
                event.getPlayer().sendMessage(event.joinMessage());
            }
            event.joinMessage(null);
            if (rename()) {
                int delay = (int) (Math.random() * 10) + 5;
                if (plugin.isFolia()) {
                    Bukkit.getGlobalRegionScheduler().runDelayed(plugin, scheduledTask -> onJoin(), delay);
                } else {
                    Bukkit.getScheduler().runTaskLater(FakePlayer.getPlugin(FakePlayer.class), this::onJoin, delay);
                }
            }
            return;
        }
        if (!isOnline) {
            return;
        }
        PacketEvents.getAPI().getPlayerManager().sendPacket(event.getPlayer(), clone(playerInfoPacket));
    }

    private boolean rename() {
        isOnline = false;
        FakePlayerProfile newFakeData = plugin.getNextAvailableFakePlayer();
        plugin.removeFakePlayer(fakeData.name());
        if (newFakeData == null) {
            cancelLatencyTask();
            return false;
        }
        fakeData = newFakeData;
        plugin.addSelf(fakeData.name(), this);
        WrapperPlayServerPlayerInfoUpdate.PlayerInfo playerInfo = playerInfoPacket.getEntries().get(0);
        playerInfo.setGameProfile(getUserProfile());
        playerInfo.setDisplayName(Component.text(fakeData.name()));
        return true;
    }

    public void broadcastQuitMessage() {
        broadcastToOnlinePlayers(fakeData.quitMessage());
        if (shouldForwardJoinLeaveToDiscordSrv()) {
            DiscordSrvBridge.sendLeaveMessage(fakeData, uuid);
        }
    }

    public static WrapperPlayServerPlayerInfoUpdate clone(WrapperPlayServerPlayerInfoUpdate packet) {
        List<WrapperPlayServerPlayerInfoUpdate.PlayerInfo> copiedEntries = packet.getEntries().stream()
                .map(WrapperPlayServerPlayerInfoUpdate.PlayerInfo::new)
                .toList();
        return new WrapperPlayServerPlayerInfoUpdate(EnumSet.copyOf(packet.getActions()), copiedEntries);
    }

    public void death(Component fakeDeathMessage, FakePlayerImpl other) {
        Component message = fakeDeathMessage
                .replaceText(TextReplacementConfig.builder().match("%player%").replacement(fakeData.name()).build())
                .replaceText(TextReplacementConfig.builder().match("%player2%").replacement(other.fakeData.name()).build());
        broadcastToOnlinePlayers(message);
        if (shouldForwardDeathToDiscordSrv()) {
            DiscordSrvBridge.sendDeathMessage(fakeData, uuid, message);
        }
    }

    public void achievement(Component fakeAchievementMessage) {
        Component message = fakeAchievementMessage
                .replaceText(TextReplacementConfig.builder().match("%player%").replacement(fakeData.name()).build());
        broadcastToOnlinePlayers(message);
        if (shouldForwardAchievementToDiscordSrv()) {
            DiscordSrvBridge.sendAchievementMessage(fakeData, uuid, message);
        }
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getProfileName() {
        return fakeData.name();
    }

    private boolean isDiscordSrvEnabled() {
        return Bukkit.getPluginManager().isPluginEnabled("DiscordSRV");
    }

    private void broadcastToOnlinePlayers(Component message) {
        Component safeMessage = SystemChatComponentSanitizer.sanitize(message);
        if (safeMessage == null) {
            return;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (plugin.isFolia()) {
                player.getScheduler().run(plugin, task -> player.sendMessage(safeMessage), () -> {
                });
            } else {
                player.sendMessage(safeMessage);
            }
        }
    }

    private void cancelLatencyTask() {
        if (plugin.isFolia()) {
            if (scheduledTask != null) {
                scheduledTask.cancel();
            }
        } else if (bukkitTask != null) {
            bukkitTask.cancel();
        }
    }

    private boolean shouldForwardJoinLeaveToDiscordSrv() {
        return isDiscordSrvEnabled() && plugin.discordSrvFakeJoinLeaveMessages();
    }

    private boolean shouldForwardDeathToDiscordSrv() {
        return isDiscordSrvEnabled() && plugin.discordSrvFakeDeathMessages();
    }

    private boolean shouldForwardAchievementToDiscordSrv() {
        return isDiscordSrvEnabled() && plugin.discordSrvFakeAchievementMessages();
    }
}
