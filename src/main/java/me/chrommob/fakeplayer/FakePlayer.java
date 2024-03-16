package me.chrommob.fakeplayer;

import com.github.retrooper.packetevents.PacketEvents;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import me.chrommob.fakeplayer.commands.AddFakeCommand;
import me.chrommob.fakeplayer.commands.RemoveFakeCommand;
import me.chrommob.fakeplayer.impl.FakeData;
import me.chrommob.fakeplayer.impl.FakePlayerImpl;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unused")
public final class FakePlayer extends JavaPlugin implements Listener {
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
        getCommand("addfakeplayer").setExecutor(new AddFakeCommand(this));
        getCommand("removefakeplayer").setExecutor(new RemoveFakeCommand(this));
        PacketEvents.getAPI().init();
    }

    private final Map<String, FakeData> potentialFakePlayers = new HashMap<>();
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        potentialFakePlayers.put(event.getPlayer().getName(), new FakeData(event.getPlayer()));
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public Map<String, FakePlayerImpl> getFakePlayers() {
        return new HashMap<>(fakePlayers);
    }

    public boolean removeFakePlayer(String name) {
        if (fakePlayers.get(name) == null) {
            return false;
        }
        fakePlayers.get(name).quit();
        fakePlayers.remove(name);
        return true;
    }

    public void addFakePlayer(String name) {
        FakePlayerImpl fakePlayer = new FakePlayerImpl(name);
        Bukkit.getPluginManager().registerEvents(fakePlayer, this);
        fakePlayers.put(name, fakePlayer);
    }
}
