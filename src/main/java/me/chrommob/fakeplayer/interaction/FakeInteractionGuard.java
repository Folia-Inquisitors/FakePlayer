package me.chrommob.fakeplayer.interaction;

import me.chrommob.fakeplayer.FakePlayer;
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
        if (tpaRules.isEmpty()
                || event.getPlayer().hasPermission("fakeplayer.interaction.bypass")) {
            return;
        }
        String command = event.getMessage();
        for (CommandPatternRule rule : tpaRules) {
            String target = rule.target(command).orElse(null);
            if (target == null || !targetResolver.isSuspectedFakeTarget(target)) {
                continue;
            }
            event.setCancelled(true);
            plugin.getDebugger().debug("Rejected TPA-like command from " + event.getPlayer().getName()
                    + " to suspected fake player " + target + " using pattern " + rule.rawPattern()
                    + "; no FakePlayer-owned denial message was sent");
            return;
        }
    }
}
