package me.chrommob.fakeplayer.commands;

import me.chrommob.fakeplayer.FakePlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class AddFakeCommand implements CommandExecutor {
    private final FakePlayer fakePlayer;


    public AddFakeCommand(FakePlayer fakePlayer) {
        this.fakePlayer = fakePlayer;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        if (strings.length == 0) {
            commandSender.sendMessage(Component.text("Usage: /addfakeplayer <name>").color(NamedTextColor.RED));
            return true;
        }
        String name = strings[0];
        this.fakePlayer.addFakePlayer(name);
        return true;
    }
}
