package me.chrommob.fakeplayer.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.json.JSONComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.Map;

/**
 * Removes interactive component payloads before FakePlayer replays learned messages as system chat.
 * Paper/Folia can reject JSON-deserialized item hover data when it converts Adventure components to vanilla packets.
 */
public final class SystemChatComponentSanitizer {
    private static final Gson GSON = new GsonBuilder().create();
    private static final JSONComponentSerializer JSON = JSONComponentSerializer.json();
    private static final PlainTextComponentSerializer PLAIN_TEXT = PlainTextComponentSerializer.plainText();

    private SystemChatComponentSanitizer() {
    }

    public static Component sanitize(Component component) {
        if (component == null) {
            return null;
        }
        try {
            String sanitizedJson = sanitizeSerializedJson(JSON.serialize(component));
            if (sanitizedJson != null) {
                return JSON.deserialize(sanitizedJson);
            }
        } catch (RuntimeException ignored) {
        }
        return Component.text(PLAIN_TEXT.serialize(component));
    }

    public static String sanitizeSerializedJson(String serializedComponent) {
        if (serializedComponent == null || serializedComponent.isBlank()) {
            return null;
        }
        try {
            JsonElement json = JsonParser.parseString(serializedComponent);
            stripUnsafeEvents(json);
            String sanitizedJson = GSON.toJson(json);
            JSON.deserialize(sanitizedJson);
            return sanitizedJson;
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static void stripUnsafeEvents(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return;
        }
        if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            for (JsonElement child : array) {
                stripUnsafeEvents(child);
            }
            return;
        }
        if (!element.isJsonObject()) {
            return;
        }

        JsonObject object = element.getAsJsonObject();
        object.remove("hoverEvent");
        object.remove("clickEvent");
        object.remove("insertion");

        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            stripUnsafeEvents(entry.getValue());
        }
    }
}
