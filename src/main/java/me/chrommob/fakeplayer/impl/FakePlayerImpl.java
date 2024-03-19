package me.chrommob.fakeplayer.impl;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.protocol.player.TextureProperty;
import com.github.retrooper.packetevents.protocol.player.UserProfile;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoRemove;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoUpdate;
import me.chrommob.fakeplayer.FakePlayer;
import me.chrommob.fakeplayer.data.FakeData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;
import java.util.List;
import java.util.UUID;


public class FakePlayerImpl implements Listener {
    private final FakePlayer plugin = FakePlayer.getPlugin(FakePlayer.class);
    private FakeData fakeData;
    private final UUID uuid = UUID.randomUUID();
    private boolean isOnline;
    private final WrapperPlayServerPlayerInfoUpdate playerInfoPacket;
    public FakePlayerImpl(FakeData fakePlayer) {
        this.fakeData = fakePlayer;
        playerInfoPacket = createPlayerInfoPacket();
        onJoin();
    }

    public WrapperPlayServerPlayerInfoUpdate createPlayerInfoPacket() {
        UserProfile userProfile = getUserProfile();
        WrapperPlayServerPlayerInfoUpdate.PlayerInfo playerInfo = new WrapperPlayServerPlayerInfoUpdate.PlayerInfo(userProfile, true, 50, GameMode.SURVIVAL, Component.text(fakeData.getName()), null);
        EnumSet<WrapperPlayServerPlayerInfoUpdate.Action> actions = EnumSet.of(WrapperPlayServerPlayerInfoUpdate.Action.ADD_PLAYER, WrapperPlayServerPlayerInfoUpdate.Action.UPDATE_LISTED);
        return new WrapperPlayServerPlayerInfoUpdate(actions, playerInfo);
    }

    @NotNull
    private UserProfile getUserProfile() {
        TextureProperty textureProperty = null;
        if (fakeData.getTexture() != null && fakeData.getSignature() != null) {
            textureProperty = new TextureProperty("textures", fakeData.getTexture(), fakeData.getSignature());
        }
        UserProfile userProfile;
        if (textureProperty == null) {
            userProfile = new UserProfile(uuid, fakeData.getName());
        } else {
            userProfile = new UserProfile(uuid, fakeData.getName(), List.of(textureProperty));
        }
        return userProfile;
    }

    private void onJoin() {
        isOnline = true;
        Bukkit.getServer().broadcast(fakeData.getJoinMessage());
        for (Player player : Bukkit.getOnlinePlayers()) {
            PacketEvents.getAPI().getPlayerManager().sendPacket(player, clone(playerInfoPacket));
        }
    }

    public void quit() {
        if (isOnline) {
            broadcastQuitMessage();
        }
        WrapperPlayServerPlayerInfoRemove removePacket = new WrapperPlayServerPlayerInfoRemove(uuid);
        for (Player player : Bukkit.getOnlinePlayers()) {
            PacketEvents.getAPI().getPlayerManager().sendPacket(player, removePacket);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (event.getPlayer().getName().equals(fakeData.getName())) {
            if (event.joinMessage() != null) {
                event.getPlayer().sendMessage(event.joinMessage());
            }
            event.joinMessage(null);
            if (rename()) {
                int delay = (int) (Math.random() * 10) + 5;
                Bukkit.getScheduler().runTaskLater(FakePlayer.getPlugin(FakePlayer.class), this::onJoin, delay);
            }
            return;
        }
        PacketEvents.getAPI().getPlayerManager().sendPacket(event.getPlayer(), clone(playerInfoPacket));
    }

    private boolean rename() {
        isOnline = false;
        FakeData newFakeData = plugin.getNextAvailableFakePlayer();
        plugin.removeFakePlayer(fakeData.getName());
        if (newFakeData == null) {
            return false;
        }
        plugin.addSelf(fakeData.getName(), this);
        fakeData = newFakeData;
        playerInfoPacket.getEntries().get(0).getGameProfile().setName(fakeData.getName());
        return true;
    }

    public void broadcastQuitMessage() {
        Bukkit.getServer().broadcast(fakeData.getQuitMessage());
    }

    public static WrapperPlayServerPlayerInfoUpdate clone(WrapperPlayServerPlayerInfoUpdate packet) {
        return new WrapperPlayServerPlayerInfoUpdate(packet.getActions(), packet.getEntries());
    }

    public void death(Component fakeDeathMessage) {
        Bukkit.getServer().broadcast(fakeDeathMessage.replaceText(TextReplacementConfig.builder().match("%player%").replacement(fakeData.getName()).build()));
    }

    public void achievement(Component fakeAchievementMessage) {
        Bukkit.getServer().broadcast(fakeAchievementMessage.replaceText(TextReplacementConfig.builder().match("%player%").replacement(fakeData.getName()).build()));
    }
}

