package me.chrommob.fakeplayer.interaction;

import me.chrommob.fakeplayer.FakePlayer;

public final class FakeTargetResolver {
    private final FakePlayer plugin;

    public FakeTargetResolver(FakePlayer plugin) {
        this.plugin = plugin;
    }

    public boolean isSuspectedFakeTarget(String targetName) {
        return plugin.isSuspectedFakePlayerName(targetName);
    }
}
