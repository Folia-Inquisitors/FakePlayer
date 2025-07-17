package me.chrommob.fakeplayer.data;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.json.JSONComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;

public class FakeData {
    private static final Gson GSON = new GsonBuilder().create();

    private final String name;
    private final Component joinMessage;
    private Component quitMessage = null;
    private String texture = null;
    private String signature = null;
    public FakeData(Player player, PlayerJoinEvent event) {
        this.name = player.getName();
        joinMessage = event.joinMessage();
        PlayerProfile playerProfile = player.getPlayerProfile();
        for (ProfileProperty property : playerProfile.getProperties()) {
            if (property.getName().equals("textures")) {
                texture = property.getValue();
                signature = property.getSignature();
            }
        }

    }

    public FakeData(String name, Component joinMessage, Component quitMessage, String texture, String signature) {
        this.name = name;
        this.joinMessage = joinMessage;
        this.quitMessage = quitMessage;
        this.texture = texture;
        this.signature = signature;
    }

    public String getTexture() {
        return texture;
    }

    public String getSignature() {
        return signature;
    }

    public String getName() {
        return name;
    }

    public Component getJoinMessage() {
        return joinMessage;
    }

    public void setQuitMessage(Component quitMessage) {
        this.quitMessage = quitMessage;
    }

    public Component getQuitMessage() {
        return quitMessage;
    }

    public boolean isReady() {
        return quitMessage != null;
    }

    @Override
    public String toString() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("name", name);
        jsonObject.addProperty("joinMessage", JSONComponentSerializer.json().serialize(joinMessage));
        jsonObject.addProperty("quitMessage", quitMessage == null ? null : JSONComponentSerializer.json().serialize(quitMessage));
        jsonObject.addProperty("texture", texture);
        jsonObject.addProperty("signature", signature);
        return GSON.toJson(jsonObject);
    }

    public static FakeData fromString(String string) {
        JsonObject jsonObject;
        try {
            jsonObject = GSON.fromJson(string, JsonObject.class);
            if (jsonObject == null) {
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        String name = jsonObject.get("name").getAsString();
        Component joinMessage = JSONComponentSerializer.json().deserialize(jsonObject.get("joinMessage").getAsString());
        Component quitMessage;

        JsonElement quitMessageElement = jsonObject.get("quitMessage");
        if (quitMessageElement == null || quitMessageElement.isJsonNull()) {
            quitMessage = null;
        } else {
            try {
                quitMessage = JSONComponentSerializer.json().deserialize(quitMessageElement.getAsString());
            } catch (Exception e) {
                quitMessage = null;
            }
        }

        JsonElement textureElement = jsonObject.get("texture");
        String texture = textureElement == null || textureElement.isJsonNull() ? null : textureElement.getAsString();

        JsonElement signatureElement = jsonObject.get("signature");
        String signature = signatureElement == null || signatureElement.isJsonNull() ? null : signatureElement.getAsString();

        return new FakeData(name, joinMessage, quitMessage, texture, signature);
    }
}
