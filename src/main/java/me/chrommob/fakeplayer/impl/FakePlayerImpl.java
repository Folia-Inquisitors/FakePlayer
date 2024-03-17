package me.chrommob.fakeplayer.impl;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.protocol.player.TextureProperty;
import com.github.retrooper.packetevents.protocol.player.UserProfile;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoRemove;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoUpdate;
import me.chrommob.fakeplayer.FakePlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

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
        TextureProperty textureProperty = new TextureProperty("textures", fakeData.getTexture(), fakeData.getSignature());
        UserProfile userProfile = new UserProfile(uuid, fakeData.getName(), List.of(textureProperty));
        WrapperPlayServerPlayerInfoUpdate.PlayerInfo playerInfo = new WrapperPlayServerPlayerInfoUpdate.PlayerInfo(userProfile, true, 50, GameMode.SURVIVAL, Component.text(fakeData.getName()), null);
        EnumSet<WrapperPlayServerPlayerInfoUpdate.Action> actions = EnumSet.of(WrapperPlayServerPlayerInfoUpdate.Action.ADD_PLAYER, WrapperPlayServerPlayerInfoUpdate.Action.UPDATE_LISTED);
        return new WrapperPlayServerPlayerInfoUpdate(actions, playerInfo);
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
            quit();
            return;
        }
        PacketEvents.getAPI().getPlayerManager().sendPacket(event.getPlayer(), clone(playerInfoPacket));
    }

    private boolean rename() {
        isOnline = false;
        FakeData newFakeData = plugin.getNextAvailableFakePlayer();
        if (newFakeData == null) {
            plugin.removeFakePlayer(fakeData.getName());
            return false;
        }
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

