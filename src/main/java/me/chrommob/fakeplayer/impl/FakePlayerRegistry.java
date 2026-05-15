package me.chrommob.fakeplayer.impl;

import me.chrommob.fakeplayer.FakePlayer;
import me.chrommob.fakeplayer.data.model.FakePlayerProfile;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;

public final class FakePlayerRegistry {
    private final FakePlayer plugin;
    private final Map<String, FakePlayerImpl> fakePlayers = new ConcurrentHashMap<>();
    private final Map<String, FakePlayerProfile> potentialFakePlayers = new ConcurrentHashMap<>();

    public FakePlayerRegistry(FakePlayer plugin) {
        this.plugin = plugin;
    }

    public Map<String, FakePlayerImpl> getFakePlayers() {
        return new HashMap<>(fakePlayers);
    }

    public Collection<FakePlayerImpl> getFakePlayerValues() {
        return List.copyOf(fakePlayers.values());
    }

    public Map<String, FakePlayerProfile> getPotentialFakePlayers() {
        return new HashMap<>(potentialFakePlayers);
    }

    public void loadPotentialFakePlayers(Map<String, FakePlayerProfile> loadedFakePlayers) {
        potentialFakePlayers.clear();
        loadedFakePlayers.forEach((name, profile) -> {
            if (name != null && profile != null) {
                potentialFakePlayers.put(name, profile);
            }
        });
    }

    public FakePlayerProfile getPotentialFakePlayer(String name) {
        return potentialFakePlayers.get(name);
    }

    public void addPotentialFakePlayer(String name, FakePlayerProfile profile) {
        potentialFakePlayers.put(name, profile);
    }

    public void removePotentialFakePlayer(String name) {
        potentialFakePlayers.remove(name);
        potentialFakePlayers.keySet().removeIf(candidate -> candidate.equalsIgnoreCase(name));
    }

    public void removePotentialFakePlayers(Predicate<FakePlayerProfile> predicate) {
        potentialFakePlayers.values().removeIf(predicate);
    }

    public void removeFakePlayer(String name) {
        FakePlayerImpl fakePlayer = fakePlayers.remove(name);
        if (fakePlayer == null) {
            return;
        }
        fakePlayer.quit();
        debug("Removed fake player " + name + " with UUID " + fakePlayer.getUuid());
    }

    public void addFakePlayer(FakePlayerProfile profile) {
        FakePlayerImpl fakePlayer = new FakePlayerImpl(profile);
        Bukkit.getPluginManager().registerEvents(fakePlayer, plugin);
        fakePlayers.put(profile.name(), fakePlayer);
        debug("Added fake player " + profile.name() + " with UUID " + fakePlayer.getUuid());
    }

    public void addSelf(String name, FakePlayerImpl fakePlayer) {
        fakePlayers.put(name, fakePlayer);
    }

    public String getRandomFakePlayerName() {
        if (fakePlayers.isEmpty()) {
            return null;
        }
        List<String> names = new ArrayList<>(fakePlayers.keySet());
        return names.get(ThreadLocalRandom.current().nextInt(names.size()));
    }

    public FakePlayerImpl getRandomFakePlayer() {
        if (fakePlayers.isEmpty()) {
            return null;
        }
        List<FakePlayerImpl> candidates = new ArrayList<>(fakePlayers.values());
        return candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
    }

    public FakePlayerImpl getRandomOnlineFakePlayer() {
        return getRandomOnlineFakePlayer(null);
    }

    public FakePlayerImpl getRandomOnlineFakePlayer(FakePlayerImpl excludedFakePlayer) {
        List<FakePlayerImpl> onlineFakePlayers = fakePlayers.values().stream()
                .filter(FakePlayerImpl::isOnline)
                .filter(fakePlayer -> fakePlayer != excludedFakePlayer)
                .toList();
        if (onlineFakePlayers.isEmpty()) {
            return null;
        }
        return onlineFakePlayers.get(ThreadLocalRandom.current().nextInt(onlineFakePlayers.size()));
    }

    public FakePlayerProfile getNextAvailableFakePlayer() {
        return getNextAvailableFakePlayer(null);
    }

    public FakePlayerProfile getNextAvailableFakePlayer(Predicate<FakePlayerProfile> preferredProfile) {
        List<FakePlayerProfile> availableFakePlayers = getAvailableFakePlayers();
        if (availableFakePlayers.isEmpty()) {
            return null;
        }
        if (preferredProfile != null) {
            List<FakePlayerProfile> preferredFakePlayers = availableFakePlayers.stream()
                    .filter(preferredProfile)
                    .toList();
            if (!preferredFakePlayers.isEmpty()) {
                availableFakePlayers = preferredFakePlayers;
            }
        }
        return availableFakePlayers.get(ThreadLocalRandom.current().nextInt(availableFakePlayers.size()));
    }

    public int availableFakePlayerCount() {
        return getAvailableFakePlayers().size();
    }

    private List<FakePlayerProfile> getAvailableFakePlayers() {
        return potentialFakePlayers.values().stream()
                .filter(Objects::nonNull)
                .filter(FakePlayerProfile::isReady)
                .filter(profile -> !fakePlayers.containsKey(profile.name()))
                .filter(profile -> Bukkit.getPlayer(profile.name()) == null)
                .toList();
    }

    public boolean isFakePlayer(String name) {
        return fakePlayers.containsKey(name) || fakePlayers.keySet().stream().anyMatch(candidate -> candidate.equalsIgnoreCase(name));
    }

    public boolean isPotentialFakePlayer(String name) {
        if (name == null) {
            return false;
        }
        String normalizedName = name.toLowerCase(Locale.ROOT);
        return potentialFakePlayers.keySet().stream()
                .map(candidate -> candidate.toLowerCase(Locale.ROOT))
                .anyMatch(normalizedName::equals);
    }

    public boolean isFakePlayer(Player player) {
        return isFakePlayer(player.getName());
    }

    public int size() {
        return fakePlayers.size();
    }

    public int potentialSize() {
        return potentialFakePlayers.size();
    }

    private void debug(String message) {
        Debugger debugger = plugin.getDebugger();
        if (debugger != null) {
            debugger.debug(message);
        }
    }
}
