package me.chrommob.fakeplayer.commands;

import me.chrommob.fakeplayer.FakePlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class RemoveFakeCommand implements CommandExecutor {
    private final FakePlayer fakePlayer;

    public RemoveFakeCommand(FakePlayer fakePlayer) {
        this.fakePlayer = fakePlayer;
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if (strings.length == 0) {
            commandSender.sendMessage(Component.text("Usage: /removefakeplayer <name>").color(NamedTextColor.RED));
            return true;
        }
        String name = strings[0];
        boolean result = fakePlayer.removeFakePlayer(name);
        if (result) {
            commandSender.sendMessage(Component.text("Removed fake player " + name).color(NamedTextColor.GREEN));
        } else {
            commandSender.sendMessage(Component.text("Fake player " + name + " does not exist").color(NamedTextColor.RED));
        }
        return true;
    }
}
