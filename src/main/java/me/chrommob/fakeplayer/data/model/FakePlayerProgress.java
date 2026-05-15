package me.chrommob.fakeplayer.data.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class FakePlayerProgress {
    private final Set<String> shownAchievementIds = ConcurrentHashMap.newKeySet();
    private final Set<String> shownAchievementKeys = ConcurrentHashMap.newKeySet();
    private int achievementScore;
    private long lastAchievementMillis;

    public static FakePlayerProgress empty() {
        return new FakePlayerProgress();
    }

    public static FakePlayerProgress fromYaml(Object object) {
        FakePlayerProgress progress = new FakePlayerProgress();
        if (!(object instanceof Map<?, ?> yaml)) {
            return progress;
        }
        progress.achievementScore = getInt(yaml.get("achievement-score"), 0);
        progress.lastAchievementMillis = getLong(yaml.get("last-achievement-millis"), 0L);

        Object shownAchievements = yaml.get("shown-achievements");
        if (shownAchievements instanceof Iterable<?> shownAchievementList) {
            for (Object shownAchievement : shownAchievementList) {
                if (shownAchievement instanceof String shownAchievementId && !shownAchievementId.isBlank()) {
                    progress.shownAchievementIds.add(shownAchievementId);
                }
            }
        }
        Object shownAchievementKeys = yaml.get("shown-achievement-keys");
        if (shownAchievementKeys instanceof Iterable<?> shownAchievementKeyList) {
            for (Object shownAchievementKey : shownAchievementKeyList) {
                if (shownAchievementKey instanceof String key && !key.isBlank()) {
                    progress.shownAchievementKeys.add(key);
                }
            }
        }
        return progress;
    }

    public boolean hasSeenAchievement(String achievementId) {
        return shownAchievementIds.contains(achievementId);
    }

    public boolean hasSeenAchievementKey(String achievementKey) {
        return achievementKey != null && shownAchievementKeys.contains(achievementKey);
    }

    public int achievementScore() {
        return achievementScore;
    }

    public void recordAchievement(WeightedMessageSet.WeightedMessage achievement, String achievementKey, long nowMillis) {
        shownAchievementIds.add(achievement.id());
        if (achievementKey != null && !achievementKey.isBlank()) {
            shownAchievementKeys.add(achievementKey);
        }
        achievementScore++;
        lastAchievementMillis = nowMillis;
    }

    public Map<String, Object> toYaml() {
        Map<String, Object> yaml = new LinkedHashMap<>();
        yaml.put("achievement-score", achievementScore);
        yaml.put("last-achievement-millis", lastAchievementMillis);
        yaml.put("shown-achievements", new ArrayList<>(shownAchievementIds));
        yaml.put("shown-achievement-keys", new ArrayList<>(shownAchievementKeys));
        return yaml;
    }

    public boolean hasRuntimeData() {
        return achievementScore > 0 || !shownAchievementIds.isEmpty() || !shownAchievementKeys.isEmpty();
    }

    private static int getInt(Object object, int defaultValue) {
        if (object instanceof Number number) {
            return number.intValue();
        }
        if (object instanceof String string) {
            try {
                return Integer.parseInt(string);
            } catch (NumberFormatException ignored) {
            }
        }
        return defaultValue;
    }

    private static long getLong(Object object, long defaultValue) {
        if (object instanceof Number number) {
            return number.longValue();
        }
        if (object instanceof String string) {
            try {
                return Long.parseLong(string);
            } catch (NumberFormatException ignored) {
            }
        }
        return defaultValue;
    }
}
