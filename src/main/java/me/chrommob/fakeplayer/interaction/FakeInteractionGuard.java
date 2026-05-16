package me.chrommob.fakeplayer.interaction;

import me.chrommob.fakeplayer.FakePlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.List;
import java.util.function.BooleanSupplier;

public final class FakeInteractionGuard implements Listener {
    private final FakePlayer plugin;
    private final FakeTargetResolver targetResolver;
    private final BooleanSupplier teleportPluginHookActive;
    private List<CommandPatternRule> tpaRules = List.of();

    public FakeInteractionGuard(FakePlayer plugin, BooleanSupplier teleportPluginHookActive) {
        this.plugin = plugin;
        this.targetResolver = new FakeTargetResolver(plugin);
        this.teleportPluginHookActive = teleportPluginHookActive;
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
                || teleportPluginHookActive.getAsBoolean()
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
