package me.chrommob.fakeplayer.model;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Keeps fake-player count moving toward a stable target instead of rerolling every tick.
 * Username cooldown is a preference, not a hard block, so sparse data can still produce activity.
 */
public final class JoinQuitPopulationModel {
    private static final long PROFILE_REUSE_COOLDOWN_MILLIS = 10 * 60 * 1000L;

    private final Map<String, Long> recentlyUsedProfiles = new ConcurrentHashMap<>();
    private int targetFakePlayers = -1;
    private int stableDecisions;
    private int decisionsBeforeRetarget = 2;

    public synchronized Decision nextDecision(int minFakePlayers, int maxFakePlayers, int currentFakePlayers, int availableProfiles) {
        int min = Math.max(0, minFakePlayers);
        int max = Math.max(min, maxFakePlayers);
        if (targetFakePlayers < min || targetFakePlayers > max) {
            retarget(min, max);
        }

        if (currentFakePlayers < min && availableProfiles > 0) {
            stableDecisions = 0;
            targetFakePlayers = Math.max(targetFakePlayers, min);
            return new Decision(Action.JOIN, targetFakePlayers);
        }
        if (currentFakePlayers > max) {
            stableDecisions = 0;
            targetFakePlayers = Math.min(targetFakePlayers, max);
            return new Decision(Action.QUIT, targetFakePlayers);
        }
        if (currentFakePlayers < targetFakePlayers && availableProfiles > 0) {
            stableDecisions = 0;
            return new Decision(Action.JOIN, targetFakePlayers);
        }
        if (currentFakePlayers > targetFakePlayers) {
            stableDecisions = 0;
            return new Decision(Action.QUIT, targetFakePlayers);
        }

        stableDecisions++;
        if (stableDecisions >= decisionsBeforeRetarget) {
            int previousTarget = targetFakePlayers;
            retarget(min, max);
            stableDecisions = 0;
            if (targetFakePlayers > currentFakePlayers && availableProfiles > 0) {
                return new Decision(Action.JOIN, targetFakePlayers);
            }
            if (targetFakePlayers < currentFakePlayers) {
                return new Decision(Action.QUIT, targetFakePlayers);
            }
            targetFakePlayers = previousTarget;
        }

        return new Decision(Action.WAIT, targetFakePlayers);
    }

    public boolean isPreferredProfile(String profileName) {
        if (profileName == null) {
            return false;
        }
        Long reusableAtMillis = recentlyUsedProfiles.get(profileName);
        if (reusableAtMillis == null) {
            return true;
        }
        if (System.currentTimeMillis() >= reusableAtMillis) {
            recentlyUsedProfiles.remove(profileName);
            return true;
        }
        return false;
    }

    public void recordProfileUsed(String profileName) {
        if (profileName != null) {
            recentlyUsedProfiles.put(profileName, System.currentTimeMillis() + PROFILE_REUSE_COOLDOWN_MILLIS);
        }
    }

    private void retarget(int minFakePlayers, int maxFakePlayers) {
        if (maxFakePlayers <= minFakePlayers) {
            targetFakePlayers = minFakePlayers;
        } else {
            targetFakePlayers = ThreadLocalRandom.current().nextInt(minFakePlayers, maxFakePlayers + 1);
        }
        decisionsBeforeRetarget = ThreadLocalRandom.current().nextInt(2, 5);
    }

    public enum Action {
        JOIN,
        QUIT,
        WAIT
    }

    public record Decision(
            Action action,
            int targetFakePlayers
    ) {
    }
}
