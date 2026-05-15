package me.chrommob.fakeplayer.model;

import me.chrommob.fakeplayer.data.FrequencyData;
import me.chrommob.fakeplayer.data.model.FakePlayerProfile;
import me.chrommob.fakeplayer.data.model.FakePlayerProgress;
import me.chrommob.fakeplayer.data.model.StoredFakePlayerState;
import me.chrommob.fakeplayer.data.model.WeightedMessageSet;
import net.kyori.adventure.text.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class FakeActivityModel {
    private final WeightedMessageSet achievementMessages;
    private final WeightedMessageSet deathMessages;
    private final FrequencyData frequencies;
    private final Map<String, FakePlayerProgress> fakePlayerProgress;
    private final AchievementProgressionModel achievementProgressionModel;
    private final DeathMessageModel deathMessageModel;
    private final JoinQuitPopulationModel joinQuitPopulationModel = new JoinQuitPopulationModel();

    private FakeActivityModel(
            WeightedMessageSet achievementMessages,
            WeightedMessageSet deathMessages,
            FrequencyData frequencies,
            Map<String, FakePlayerProgress> fakePlayerProgress
    ) {
        this.achievementMessages = achievementMessages;
        this.deathMessages = deathMessages;
        this.frequencies = frequencies;
        this.fakePlayerProgress = new ConcurrentHashMap<>(fakePlayerProgress);
        this.achievementProgressionModel = new AchievementProgressionModel(this.achievementMessages);
        this.deathMessageModel = new DeathMessageModel(this.deathMessages);
    }

    public static FakeActivityModel fromStoredState(StoredFakePlayerState state) {
        return new FakeActivityModel(
                state.achievementMessages(),
                state.deathMessages(),
                state.frequencyData(),
                state.fakePlayerProgress()
        );
    }

    public StoredFakePlayerState toStoredState(Map<String, FakePlayerProfile> potentialFakePlayers) {
        return new StoredFakePlayerState(
                achievementMessages,
                deathMessages,
                potentialFakePlayers,
                frequencies,
                fakePlayerProgress
        );
    }

    public boolean learnJoinQuit(long nowMillis, int outlierDropPercent) {
        return frequencies.recordJoinQuit(nowMillis, outlierDropPercent);
    }

    public boolean learnAchievement(Component achievementTemplate, long nowMillis, int outlierDropPercent) {
        achievementMessages.record(achievementTemplate);
        return frequencies.recordAchievement(nowMillis, outlierDropPercent);
    }

    public boolean learnDeathMessage(String deathTemplate, long nowMillis, int outlierDropPercent) {
        deathMessages.recordSerialized(deathTemplate);
        return frequencies.recordDeathMessage(nowMillis, outlierDropPercent);
    }

    public int getRandomJoinQuitFrequency() {
        return frequencies.getRandomPlayerJoinQuitFrequency();
    }

    public int getRandomDeathMessageFrequency() {
        return frequencies.getRandomMessageFrequency();
    }

    public int getRandomAchievementFrequency() {
        return frequencies.getRandomAchievementFrequency();
    }

    public JoinQuitPopulationModel.Decision nextJoinQuitDecision(
            int minFakePlayers,
            int maxFakePlayers,
            int currentFakePlayers,
            int availableProfiles
    ) {
        return joinQuitPopulationModel.nextDecision(minFakePlayers, maxFakePlayers, currentFakePlayers, availableProfiles);
    }

    public boolean isPreferredFakePlayerProfile(String profileName) {
        return joinQuitPopulationModel.isPreferredProfile(profileName);
    }

    public void recordFakePlayerProfileUsed(String profileName) {
        joinQuitPopulationModel.recordProfileUsed(profileName);
    }

    public Component nextAchievementMessage(String fakePlayerName) {
        FakePlayerProgress progress = fakePlayerProgress.computeIfAbsent(fakePlayerName, name -> FakePlayerProgress.empty());
        return achievementProgressionModel.nextMessage(progress);
    }

    public Component nextDeathMessage(String victimName, boolean hasDistinctKiller) {
        return deathMessageModel.nextMessage(victimName, hasDistinctKiller);
    }

    public FrequencyData frequencies() {
        return frequencies;
    }

    public int achievementTemplateCount() {
        return achievementMessages.size();
    }

    public int deathTemplateCount() {
        return deathMessages.size();
    }
}
