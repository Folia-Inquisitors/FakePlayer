package me.chrommob.fakeplayer.impl;

import java.util.List;
import java.util.Set;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.TabCompleteEvent;

import me.chrommob.fakeplayer.api.FakePlayerAPI;

public class PlayerCommandCompletion implements Listener {
    @EventHandler
    public void onTabComplete(TabCompleteEvent event) {
        if (event.getBuffer().startsWith("/")) {
            if (event.getBuffer().split(" ").length < 1) {
                return;
            }
            String command = event.getBuffer().substring(1).split(" ")[0];
            if (command.equalsIgnoreCase("msg") || command.equalsIgnoreCase("tell") || command.equalsIgnoreCase("w")
                    || command.equalsIgnoreCase("whisper") || command.equalsIgnoreCase("r")
                    || command.equalsIgnoreCase("reply") || command.equalsIgnoreCase("me")
                    || command.equalsIgnoreCase("minecraft:me") || command.equalsIgnoreCase("bukkit:me")
                    || command.equalsIgnoreCase("say") || command.equalsIgnoreCase("minecraft:say")
                    || command.equalsIgnoreCase("bukkit:say") || command.equalsIgnoreCase("minecraft:msg")
                    || command.equalsIgnoreCase("bukkit:msg") || command.equalsIgnoreCase("minecraft:tell")
                    || command.equalsIgnoreCase("bukkit:tell") || command.equalsIgnoreCase("minecraft:w")
                    || command.equalsIgnoreCase("bukkit:w") || command.equalsIgnoreCase("minecraft:whisper")
                    || command.equalsIgnoreCase("bukkit:whisper") || command.equalsIgnoreCase("minecraft:r")
                    || command.equalsIgnoreCase("bukkit:r") || command.equalsIgnoreCase("minecraft:reply")
                    || command.equalsIgnoreCase("bukkit:reply")) {
                if (event.getBuffer().split(" ").length < 3) {
                    return;
                }
                Set<String> fakePlayers = FakePlayerAPI.getInstance().getFakePlayerNames();
                event.getCompletions().addAll(fakePlayers);
            }
        }
    }

}
