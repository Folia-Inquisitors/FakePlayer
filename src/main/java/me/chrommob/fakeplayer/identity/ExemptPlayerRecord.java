package me.chrommob.fakeplayer.identity;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class ExemptPlayerRecord {
    private final UUID uuid;
    private String name;
    private int joins;
    private boolean exempt;
    private String reason;
    private long lastSeenMillis;

    public ExemptPlayerRecord(UUID uuid, String name, int joins, boolean exempt, String reason, long lastSeenMillis) {
        this.uuid = uuid;
        this.name = name;
        this.joins = Math.max(0, joins);
        this.exempt = exempt;
        this.reason = reason == null ? "" : reason;
        this.lastSeenMillis = Math.max(0L, lastSeenMillis);
    }

    public static ExemptPlayerRecord create(UUID uuid, String name) {
        return new ExemptPlayerRecord(uuid, name, 0, false, "", 0L);
    }

    public static ExemptPlayerRecord fromYaml(UUID uuid, Object object) {
        if (!(object instanceof Map<?, ?> yaml)) {
            return null;
        }
        String name = getString(yaml.get("name"), "");
        int joins = getInt(yaml.get("joins"), 0);
        boolean exempt = getBoolean(yaml.get("exempt"), false);
        String reason = getString(yaml.get("reason"), "");
        long lastSeenMillis = getLong(yaml.get("last-seen-millis"), 0L);
        return new ExemptPlayerRecord(uuid, name, joins, exempt, reason, lastSeenMillis);
    }

    public UUID uuid() {
        return uuid;
    }

    public String name() {
        return name;
    }

    public int joins() {
        return joins;
    }

    public boolean exempt() {
        return exempt;
    }

    public void recordJoin(String name, long nowMillis) {
        this.name = name;
        joins++;
        lastSeenMillis = nowMillis;
    }

    public void exempt(String reason) {
        exempt = true;
        this.reason = reason == null ? "" : reason;
    }

    public Map<String, Object> toYaml() {
        Map<String, Object> yaml = new LinkedHashMap<>();
        yaml.put("name", name);
        yaml.put("joins", joins);
        yaml.put("exempt", exempt);
        yaml.put("reason", reason);
        yaml.put("last-seen-millis", lastSeenMillis);
        return yaml;
    }

    private static String getString(Object object, String defaultValue) {
        return object == null ? defaultValue : object.toString();
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

    private static boolean getBoolean(Object object, boolean defaultValue) {
        if (object instanceof Boolean bool) {
            return bool;
        }
        if (object instanceof String string) {
            return Boolean.parseBoolean(string);
        }
        return defaultValue;
    }
}
