package me.chrommob.fakeplayer.impl;

import java.util.Set;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import me.chrommob.fakeplayer.api.FakePlayerAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

public class PlayerCommand implements Listener {
    @EventHandler
    public void onPlayerExecuteCommand(PlayerCommandPreprocessEvent event) {
        String message = event.getMessage();
        if (message.startsWith("/")) {
            if (message.split(" ").length < 1) {
                return;
            }
            String command = message.substring(1).split(" ")[0];
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
                if (message.split(" ").length < 3) {
                    return;
                }
                String target = message.split(" ")[1];
                if (message.split(" ").length < 4) {
                    return;
                }
                String messageToSend = message.substring(command.length() + target.length() + 2);
                Set<String> fakePlayers = FakePlayerAPI.getInstance().getFakePlayerNames();
                if (fakePlayers.contains(target)) {
                    event.setCancelled(true);
                    event.getPlayer().sendMessage(Component.text("You whisper to " + target + ": " + messageToSend)
                            .color(NamedTextColor.GRAY).decorate(TextDecoration.ITALIC));
                }
            }
        }
    }

}
