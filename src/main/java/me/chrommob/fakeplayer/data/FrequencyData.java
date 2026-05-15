package me.chrommob.fakeplayer.data;

import me.chrommob.fakeplayer.data.model.FrequencyMetric;

import java.util.LinkedHashMap;
import java.util.Map;

public class FrequencyData {
    private final FrequencyMetric joinQuitFrequency;
    private final FrequencyMetric deathMessageFrequency;
    private final FrequencyMetric achievementFrequency;

    public FrequencyData() {
        this(new FrequencyMetric(), new FrequencyMetric(), new FrequencyMetric());
    }

    private FrequencyData(
            FrequencyMetric joinQuitFrequency,
            FrequencyMetric deathMessageFrequency,
            FrequencyMetric achievementFrequency
    ) {
        this.joinQuitFrequency = joinQuitFrequency;
        this.deathMessageFrequency = deathMessageFrequency;
        this.achievementFrequency = achievementFrequency;
    }

    public static FrequencyData fromString(String data) {
        FrequencyData frequencyData = new FrequencyData();
        try {
            String[] split = data.split(",");
            return new FrequencyData(
                    FrequencyMetric.fromAverage(Integer.parseInt(split[0])),
                    FrequencyMetric.fromAverage(Integer.parseInt(split[1])),
                    FrequencyMetric.fromAverage(Integer.parseInt(split[2]))
            );
        } catch (RuntimeException ignored) {
            return frequencyData;
        }
    }

    public static FrequencyData fromYaml(Object object) {
        if (!(object instanceof Map<?, ?> yaml)) {
            return new FrequencyData();
        }
        return new FrequencyData(
                FrequencyMetric.fromYaml(yaml.get("join-quit")),
                FrequencyMetric.fromYaml(yaml.get("death-messages")),
                FrequencyMetric.fromYaml(yaml.get("achievements"))
        );
    }

    public synchronized boolean recordJoinQuit(long nowMillis, int outlierDropPercent) {
        return joinQuitFrequency.recordNow(nowMillis, outlierDropPercent);
    }

    public synchronized boolean recordDeathMessage(long nowMillis, int outlierDropPercent) {
        return deathMessageFrequency.recordNow(nowMillis, outlierDropPercent);
    }

    public synchronized boolean recordAchievement(long nowMillis, int outlierDropPercent) {
        return achievementFrequency.recordNow(nowMillis, outlierDropPercent);
    }

    public synchronized int getRandomPlayerJoinQuitFrequency() {
        return joinQuitFrequency.randomFrequencyTicks();
    }

    public synchronized int getRandomMessageFrequency() {
        return deathMessageFrequency.randomFrequencyTicks();
    }

    public synchronized int getRandomAchievementFrequency() {
        return achievementFrequency.randomFrequencyTicks();
    }

    public synchronized FrequencyMetric joinQuitFrequency() {
        return joinQuitFrequency;
    }

    public synchronized FrequencyMetric deathMessageFrequency() {
        return deathMessageFrequency;
    }

    public synchronized FrequencyMetric achievementFrequency() {
        return achievementFrequency;
    }

    public synchronized Map<String, Object> toYaml() {
        Map<String, Object> yaml = new LinkedHashMap<>();
        yaml.put("join-quit", joinQuitFrequency.toYaml());
        yaml.put("death-messages", deathMessageFrequency.toYaml());
        yaml.put("achievements", achievementFrequency.toYaml());
        return yaml;
    }

    public synchronized String toLegacyString() {
        return joinQuitFrequency.averageTicks() + ","
                + deathMessageFrequency.averageTicks() + ","
                + achievementFrequency.averageTicks();
    }

    @Override
    public synchronized String toString() {
        return toLegacyString();
    }
}
