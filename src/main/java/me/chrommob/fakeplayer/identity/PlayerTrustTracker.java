package me.chrommob.fakeplayer.identity;

import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerTrustTracker {
    private static final String REPEATED_REAL_JOINS_REASON = "repeated-real-joins";

    private final ExemptPlayerStorage storage;
    private final Map<UUID, ExemptPlayerRecord> records = new ConcurrentHashMap<>();
    private final Map<String, UUID> uuidByName = new ConcurrentHashMap<>();

    public PlayerTrustTracker(ExemptPlayerStorage storage) {
        this.storage = storage;
    }

    public void load() {
        records.clear();
        uuidByName.clear();
        records.putAll(storage.load());
        records.values().forEach(this::indexName);
    }

    public void save() {
        storage.save(records);
    }

    public boolean recordJoin(Player player, int exemptAfterJoins) {
        UUID uuid = player.getUniqueId();
        ExemptPlayerRecord record = records.computeIfAbsent(uuid, ignored -> ExemptPlayerRecord.create(uuid, player.getName()));
        record.recordJoin(player.getName(), System.currentTimeMillis());
        indexName(record);
        if (exemptAfterJoins > 0 && record.joins() >= exemptAfterJoins) {
            record.exempt(REPEATED_REAL_JOINS_REASON);
        }
        save();
        return record.exempt();
    }

    public boolean isExempt(Player player) {
        ExemptPlayerRecord record = records.get(player.getUniqueId());
        return record != null && record.exempt();
    }

    public boolean isExemptName(String name) {
        if (name == null) {
            return false;
        }
        UUID uuid = uuidByName.get(normalizeName(name));
        if (uuid == null) {
            return false;
        }
        ExemptPlayerRecord record = records.get(uuid);
        return record != null && record.exempt();
    }

    private void indexName(ExemptPlayerRecord record) {
        if (record.name() != null && !record.name().isBlank()) {
            uuidByName.put(normalizeName(record.name()), record.uuid());
        }
    }

    private String normalizeName(String name) {
        return name.toLowerCase(Locale.ROOT);
    }
}
