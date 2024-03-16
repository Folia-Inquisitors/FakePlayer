package me.chrommob.fakeplayer.impl;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.protocol.player.UserProfile;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoRemove;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoUpdate;
import me.chrommob.fakeplayer.FakePlayer;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.EnumSet;
import java.util.UUID;


public class FakePlayerImpl implements Listener {
    private final UUID uuid = UUID.randomUUID();
    private String name;
    private Component joinMessage;
    private Component quitMessage;
    private boolean isOnline;
    private final WrapperPlayServerPlayerInfoUpdate playerInfoPacket;
    public FakePlayerImpl(String name) {
        this.name = name;
        joinMessage = Component.translatable("multiplayer.player.joined", net.kyori.adventure.text.format.NamedTextColor.YELLOW, Component.text(name));
        quitMessage = Component.translatable("multiplayer.player.left", net.kyori.adventure.text.format.NamedTextColor.YELLOW, Component.text(name));
        playerInfoPacket = createPlayerInfoPacket();
        onJoin();
    }

    public WrapperPlayServerPlayerInfoUpdate createPlayerInfoPacket() {
        UserProfile userProfile = new UserProfile(uuid, name);
        WrapperPlayServerPlayerInfoUpdate.PlayerInfo playerInfo = new WrapperPlayServerPlayerInfoUpdate.PlayerInfo(userProfile, true, 50, GameMode.SURVIVAL, Component.text(name), null);
        EnumSet<WrapperPlayServerPlayerInfoUpdate.Action> actions = EnumSet.of(WrapperPlayServerPlayerInfoUpdate.Action.ADD_PLAYER, WrapperPlayServerPlayerInfoUpdate.Action.UPDATE_LISTED);
        return new WrapperPlayServerPlayerInfoUpdate(actions, playerInfo);
    }

    private void onJoin() {
        isOnline = true;
        Bukkit.getServer().broadcast(joinMessage);
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
        if (event.getPlayer().getName().equals(name)) {
            if (event.joinMessage() != null) {
                event.getPlayer().sendMessage(event.joinMessage());
            }
            event.joinMessage(null);
            rename(name + "1");
            quit();
            int delay = (int) (Math.random() * 10) + 5;
            Bukkit.getScheduler().runTaskLater(FakePlayer.getPlugin(FakePlayer.class), this::onJoin, delay * 20L);
            return;
        }
        PacketEvents.getAPI().getPlayerManager().sendPacket(event.getPlayer(), clone(playerInfoPacket));
    }

    private void rename(String newName) {
        isOnline = false;
        playerInfoPacket.getEntries().get(0).getGameProfile().setName(newName);
        name = newName;
        joinMessage = Component.translatable("multiplayer.player.joined", net.kyori.adventure.text.format.NamedTextColor.YELLOW, Component.text(name));
        quitMessage = Component.translatable("multiplayer.player.left", net.kyori.adventure.text.format.NamedTextColor.YELLOW, Component.text(name));
    }

    public void broadcastQuitMessage() {
        Bukkit.getServer().broadcast(quitMessage);
    }

    public static WrapperPlayServerPlayerInfoUpdate clone(WrapperPlayServerPlayerInfoUpdate packet) {
        return new WrapperPlayServerPlayerInfoUpdate(packet.getActions(), packet.getEntries());
    }
}

