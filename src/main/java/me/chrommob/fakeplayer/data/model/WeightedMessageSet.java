package me.chrommob.fakeplayer.data.model;

import me.chrommob.fakeplayer.util.SystemChatComponentSanitizer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.json.JSONComponentSerializer;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

public final class WeightedMessageSet {
    private static final JSONComponentSerializer JSON = JSONComponentSerializer.json();

    private final Map<String, Integer> serializedCounts = new ConcurrentHashMap<>();

    private WeightedMessageSet() {
    }

    public static WeightedMessageSet empty() {
        return new WeightedMessageSet();
    }

    public static WeightedMessageSet fromSerializedCounts(Map<String, Integer> counts) {
        WeightedMessageSet set = new WeightedMessageSet();
        counts.forEach((message, count) -> {
            if (message != null && count != null && count > 0) {
                set.serializedCounts.put(message, count);
            }
        });
        return set;
    }

    public static WeightedMessageSet fromYaml(Object object) {
        WeightedMessageSet set = new WeightedMessageSet();
        if (object instanceof Map<?, ?> map) {
            Object messages = map.get("messages");
            if (messages != null) {
                return fromYaml(messages);
            }
            map.forEach((message, count) -> {
                if (message instanceof String messageString && count instanceof Number countNumber && countNumber.intValue() > 0) {
                    set.serializedCounts.put(messageString, countNumber.intValue());
                }
            });
            return set;
        }
        if (!(object instanceof List<?> messages)) {
            return set;
        }
        for (Object messageObject : messages) {
            if (!(messageObject instanceof Map<?, ?> messageMap)) {
                continue;
            }
            Object message = messageMap.get("message");
            Object count = messageMap.get("count");
            if (message instanceof String messageString && count instanceof Number countNumber && countNumber.intValue() > 0) {
                set.serializedCounts.put(messageString, countNumber.intValue());
            }
        }
        return set;
    }

    public void record(Component message) {
        serializedCounts.merge(JSON.serialize(SystemChatComponentSanitizer.sanitize(message)), 1, Integer::sum);
    }

    public void recordSerialized(String serializedMessage) {
        String sanitizedMessage = SystemChatComponentSanitizer.sanitizeSerializedJson(serializedMessage);
        if (sanitizedMessage == null) {
            return;
        }
        serializedCounts.merge(sanitizedMessage, 1, Integer::sum);
    }

    public Component randomMessage() {
        WeightedMessage message = randomWeightedMessage(null, WeightedMessage::count);
        return message == null ? null : message.component();
    }

    public WeightedMessage randomWeightedMessage(
            Predicate<WeightedMessage> filter,
            ToIntFunction<WeightedMessage> weightFunction
    ) {
        List<WeightedCandidate> candidates = new ArrayList<>();
        int total = 0;
        for (Map.Entry<String, Integer> entry : serializedCounts.entrySet()) {
            Integer count = entry.getValue();
            if (count == null || count <= 0) {
                continue;
            }
            Component component;
            try {
                component = SystemChatComponentSanitizer.sanitize(JSON.deserialize(entry.getKey()));
            } catch (RuntimeException ignored) {
                serializedCounts.remove(entry.getKey());
                continue;
            }
            WeightedMessage message = new WeightedMessage(messageId(entry.getKey()), entry.getKey(), component, count);
            if (filter != null && !filter.test(message)) {
                continue;
            }
            int adjustedWeight = Math.max(0, weightFunction.applyAsInt(message));
            if (adjustedWeight <= 0) {
                continue;
            }
            candidates.add(new WeightedCandidate(message, adjustedWeight));
            total += adjustedWeight;
        }
        if (total <= 0) {
            return null;
        }

        int selected = ThreadLocalRandom.current().nextInt(total);
        for (WeightedCandidate candidate : candidates) {
            selected -= candidate.weight();
            if (selected < 0) {
                return candidate.message();
            }
        }
        return null;
    }

    public Map<String, Integer> serializedCounts() {
        return new LinkedHashMap<>(serializedCounts);
    }

    public int size() {
        return serializedCounts.size();
    }

    public List<Map<String, Object>> toYaml() {
        List<Map<String, Object>> messages = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : serializedCounts.entrySet()) {
            Map<String, Object> message = new LinkedHashMap<>();
            message.put("message", entry.getKey());
            message.put("count", entry.getValue());
            messages.add(message);
        }
        return messages;
    }

    public List<Component> percentageBuckets() {
        List<Component> percentages = new ArrayList<>();
        int total = serializedCounts.values().stream()
                .filter(Objects::nonNull)
                .filter(count -> count > 0)
                .mapToInt(Integer::intValue)
                .sum();
        if (total <= 0) {
            return percentages;
        }

        for (Map.Entry<String, Integer> entry : serializedCounts.entrySet()) {
            Integer count = entry.getValue();
            if (count == null || count <= 0) {
                continue;
            }
            Component message;
            try {
                message = JSON.deserialize(entry.getKey());
            } catch (RuntimeException ignored) {
                continue;
            }
            int percentage = (int) ((count / (double) total) * 100);
            for (int i = 0; i < percentage; i++) {
                percentages.add(message);
            }
        }
        return percentages;
    }

    private static String messageId(String serializedMessage) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(serializedMessage.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            return Integer.toHexString(serializedMessage.hashCode());
        }
    }

    public record WeightedMessage(
            String id,
            String serializedMessage,
            Component component,
            int count
    ) {
    }

    private record WeightedCandidate(
            WeightedMessage message,
            int weight
    ) {
    }
}
