package me.chrommob.fakeplayer.interaction;

import me.chrommob.fakeplayer.FakePlayer;
import net.kyori.adventure.text.Component;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.List;

public final class FakeInteractionGuard implements Listener {
    private final FakePlayer plugin;
    private final FakeTargetResolver targetResolver;
    private List<CommandPatternRule> tpaRules = List.of();

    public FakeInteractionGuard(FakePlayer plugin) {
        this.plugin = plugin;
        this.targetResolver = new FakeTargetResolver(plugin);
        reload();
    }

    public void reload() {
        tpaRules = plugin.tpaGuardCommandPatterns().stream()
                .map(CommandPatternRule::compile)
                .flatMap(java.util.Optional::stream)
                .toList();
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        if (!plugin.tpaGuardEnabled() || event.getPlayer().hasPermission("fakeplayer.interaction.bypass")) {
            return;
        }
        String command = event.getMessage();
        for (CommandPatternRule rule : tpaRules) {
            String target = rule.target(command).orElse(null);
            if (target == null || !targetResolver.isSuspectedFakeTarget(target)) {
                continue;
            }
            event.setCancelled(true);
            event.getPlayer().sendMessage(Component.text(plugin.tpaGuardDenyMessage().replace("%target%", target)));
            plugin.getDebugger().debug("Blocked TPA-like command from " + event.getPlayer().getName()
                    + " to suspected fake player " + target + " using pattern " + rule.rawPattern());
            return;
        }
    }
}
