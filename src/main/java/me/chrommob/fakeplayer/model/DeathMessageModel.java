package me.chrommob.fakeplayer.model;

import me.chrommob.fakeplayer.data.model.WeightedMessageSet;
import net.kyori.adventure.text.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Selects death templates using server-observed frequency, while softly discouraging repeated templates
 * and repeated deaths for the same fake player.
 */
public final class DeathMessageModel {
    private static final int RECENT_TEMPLATE_LIMIT = 8;
    private static final long PLAYER_DEATH_COOLDOWN_MILLIS = 120_000L;

    private final WeightedMessageSet deathMessages;
    private final Map<String, Long> lastDeathByPlayer = new ConcurrentHashMap<>();
    private final Deque<String> recentTemplateIds = new ArrayDeque<>();

    public DeathMessageModel(WeightedMessageSet deathMessages) {
        this.deathMessages = deathMessages;
    }

    public synchronized Component nextMessage(String victimName, boolean hasDistinctKiller) {
        long nowMillis = System.currentTimeMillis();
        WeightedMessageSet.WeightedMessage message = deathMessages.randomWeightedMessage(
                deathMessage -> canUseTemplate(deathMessage, hasDistinctKiller),
                deathMessage -> deathWeight(victimName, deathMessage, nowMillis)
        );
        if (message == null) {
            return null;
        }

        lastDeathByPlayer.put(victimName, nowMillis);
        recentTemplateIds.addLast(message.id());
        while (recentTemplateIds.size() > RECENT_TEMPLATE_LIMIT) {
            recentTemplateIds.removeFirst();
        }
        return message.component();
    }

    private boolean canUseTemplate(WeightedMessageSet.WeightedMessage deathMessage, boolean hasDistinctKiller) {
        return hasDistinctKiller || !requiresSecondPlayer(deathMessage);
    }

    private int deathWeight(String victimName, WeightedMessageSet.WeightedMessage deathMessage, long nowMillis) {
        int baseWeight = Math.max(1, deathMessage.count());
        int recentTemplateWeight = recentTemplateIds.contains(deathMessage.id()) ? 1 : 6;
        Long lastDeathMillis = lastDeathByPlayer.get(victimName);
        int victimCooldownWeight = lastDeathMillis != null
                && nowMillis - lastDeathMillis < PLAYER_DEATH_COOLDOWN_MILLIS ? 2 : 6;
        int secondPlayerWeight = requiresSecondPlayer(deathMessage) ? 4 : 6;

        long weighted = (long) baseWeight * recentTemplateWeight * victimCooldownWeight * secondPlayerWeight;
        return (int) Math.min(Integer.MAX_VALUE, Math.max(1L, weighted));
    }

    private static boolean requiresSecondPlayer(WeightedMessageSet.WeightedMessage deathMessage) {
        return deathMessage.serializedMessage().contains("%player2%");
    }
}
