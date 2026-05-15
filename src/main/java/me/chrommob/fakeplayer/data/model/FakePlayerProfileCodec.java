package me.chrommob.fakeplayer.data.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.json.JSONComponentSerializer;

import java.util.LinkedHashMap;
import java.util.Map;

public final class FakePlayerProfileCodec {
    private static final Gson GSON = new GsonBuilder().create();
    private static final JSONComponentSerializer JSON = JSONComponentSerializer.json();

    private FakePlayerProfileCodec() {
    }

    public static Map<String, Object> toYaml(FakePlayerProfile profile) {
        Map<String, Object> yaml = new LinkedHashMap<>();
        yaml.put("name", profile.name());
        yaml.put("join-message", JSON.serialize(profile.joinMessage()));
        yaml.put("quit-message", profile.quitMessage() == null ? null : JSON.serialize(profile.quitMessage()));
        yaml.put("texture", profile.texture());
        yaml.put("signature", profile.signature());
        return yaml;
    }

    public static FakePlayerProfile fromYaml(Object object, String fallbackName) {
        if (!(object instanceof Map<?, ?> yaml)) {
            return null;
        }
        String name = getString(yaml, "name", fallbackName);
        String joinMessageString = getString(yaml, "join-message", getString(yaml, "joinMessage", null));
        if (name == null || joinMessageString == null) {
            return null;
        }
        Component joinMessage = deserializeComponent(joinMessageString);
        if (joinMessage == null) {
            return null;
        }
        String quitMessageString = getString(yaml, "quit-message", getString(yaml, "quitMessage", null));
        Component quitMessage = quitMessageString == null ? null : deserializeComponent(quitMessageString);
        return new FakePlayerProfile(
                name,
                joinMessage,
                quitMessage,
                getString(yaml, "texture", null),
                getString(yaml, "signature", null)
        );
    }

    public static String toLegacyJson(FakePlayerProfile profile) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("name", profile.name());
        jsonObject.addProperty("joinMessage", JSON.serialize(profile.joinMessage()));
        jsonObject.addProperty("quitMessage", profile.quitMessage() == null ? null : JSON.serialize(profile.quitMessage()));
        jsonObject.addProperty("texture", profile.texture());
        jsonObject.addProperty("signature", profile.signature());
        return GSON.toJson(jsonObject);
    }

    public static FakePlayerProfile fromLegacyJson(String string) {
        JsonObject jsonObject;
        try {
            jsonObject = GSON.fromJson(string, JsonObject.class);
            if (jsonObject == null) {
                return null;
            }
        } catch (RuntimeException ignored) {
            return null;
        }

        String name = getString(jsonObject, "name", null);
        String joinMessageString = getString(jsonObject, "joinMessage", null);
        if (name == null || joinMessageString == null) {
            return null;
        }
        Component joinMessage = deserializeComponent(joinMessageString);
        if (joinMessage == null) {
            return null;
        }

        String quitMessageString = getString(jsonObject, "quitMessage", null);
        Component quitMessage = quitMessageString == null ? null : deserializeComponent(quitMessageString);
        return new FakePlayerProfile(
                name,
                joinMessage,
                quitMessage,
                getString(jsonObject, "texture", null),
                getString(jsonObject, "signature", null)
        );
    }

    private static Component deserializeComponent(String value) {
        try {
            return JSON.deserialize(value);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static String getString(Map<?, ?> yaml, String key, String defaultValue) {
        Object value = yaml.get(key);
        return value == null ? defaultValue : value.toString();
    }

    private static String getString(JsonObject jsonObject, String key, String defaultValue) {
        JsonElement element = jsonObject.get(key);
        if (element == null || element.isJsonNull()) {
            return defaultValue;
        }
        return element.getAsString();
    }
}
