package me.chrommob.fakeplayer.data.model;

import me.chrommob.fakeplayer.data.FrequencyData;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public record StoredFakePlayerState(
        WeightedMessageSet achievementMessages,
        WeightedMessageSet deathMessages,
        Map<String, FakePlayerProfile> potentialFakePlayers,
        FrequencyData frequencyData,
        Map<String, FakePlayerProgress> fakePlayerProgress
) {
    public static final int CURRENT_VERSION = 4;

    public StoredFakePlayerState {
        achievementMessages = achievementMessages == null ? WeightedMessageSet.empty() : achievementMessages;
        deathMessages = deathMessages == null ? WeightedMessageSet.empty() : deathMessages;
        potentialFakePlayers = potentialFakePlayers == null
                ? new ConcurrentHashMap<>()
                : new ConcurrentHashMap<>(potentialFakePlayers);
        frequencyData = frequencyData == null ? new FrequencyData() : frequencyData;
        fakePlayerProgress = fakePlayerProgress == null
                ? new ConcurrentHashMap<>()
                : new ConcurrentHashMap<>(fakePlayerProgress);
    }

    public static StoredFakePlayerState empty() {
        return new StoredFakePlayerState(
                WeightedMessageSet.empty(),
                WeightedMessageSet.empty(),
                new ConcurrentHashMap<>(),
                new FrequencyData(),
                new ConcurrentHashMap<>()
        );
    }

    public static StoredFakePlayerState fromYaml(Object object) {
        if (!(object instanceof Map<?, ?> yaml)) {
            return null;
        }
        Object version = yaml.get("version");
        if (!(version instanceof Number number) || number.intValue() < 2 || number.intValue() > CURRENT_VERSION) {
            return null;
        }

        Map<String, FakePlayerProfile> fakePlayers = new ConcurrentHashMap<>();
        Object fakePlayersObject = yaml.get("potential-fake-players");
        if (fakePlayersObject instanceof Map<?, ?> fakePlayersYaml) {
            for (Map.Entry<?, ?> entry : fakePlayersYaml.entrySet()) {
                if (!(entry.getKey() instanceof String key)) {
                    continue;
                }
                FakePlayerProfile profile = FakePlayerProfileCodec.fromYaml(entry.getValue(), key);
                if (profile != null) {
                    fakePlayers.put(key, profile);
                }
            }
        }

        return new StoredFakePlayerState(
                WeightedMessageSet.fromYaml(yaml.get("achievement-messages")),
                WeightedMessageSet.fromYaml(yaml.get("death-messages")),
                fakePlayers,
                FrequencyData.fromYaml(yaml.get("frequencies")),
                loadFakePlayerProgress(yaml.get("fake-player-progress"))
        );
    }

    private static Map<String, FakePlayerProgress> loadFakePlayerProgress(Object object) {
        Map<String, FakePlayerProgress> progress = new ConcurrentHashMap<>();
        if (!(object instanceof Map<?, ?> progressYaml)) {
            return progress;
        }
        for (Map.Entry<?, ?> entry : progressYaml.entrySet()) {
            if (!(entry.getKey() instanceof String playerName)) {
                continue;
            }
            progress.put(playerName, FakePlayerProgress.fromYaml(entry.getValue()));
        }
        return progress;
    }

    public Map<String, Object> toYaml() {
        Map<String, Object> yaml = new LinkedHashMap<>();
        yaml.put("version", CURRENT_VERSION);
        yaml.put("frequencies", frequencyData.toYaml());
        yaml.put("achievement-messages", achievementMessages.toYaml());
        yaml.put("death-messages", deathMessages.toYaml());

        Map<String, Object> fakePlayers = new LinkedHashMap<>();
        potentialFakePlayers.forEach((name, profile) -> {
            if (name != null && profile != null) {
                fakePlayers.put(name, FakePlayerProfileCodec.toYaml(profile));
            }
        });
        yaml.put("potential-fake-players", fakePlayers);

        Map<String, Object> progress = new LinkedHashMap<>();
        fakePlayerProgress.forEach((name, playerProgress) -> {
            if (name != null && playerProgress != null && playerProgress.hasRuntimeData()) {
                progress.put(name, playerProgress.toYaml());
            }
        });
        yaml.put("fake-player-progress", progress);
        return yaml;
    }

    public boolean hasRuntimeData() {
        return achievementMessages.size() > 0
                || deathMessages.size() > 0
                || !potentialFakePlayers.isEmpty()
                || fakePlayerProgress.values().stream().anyMatch(FakePlayerProgress::hasRuntimeData)
                || frequencyData.joinQuitFrequency().averageTicks() > 0
                || frequencyData.deathMessageFrequency().averageTicks() > 0
                || frequencyData.achievementFrequency().averageTicks() > 0;
    }
}
