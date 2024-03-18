package me.chrommob.fakeplayer.impl;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.json.JSONComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class FakeData {
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
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("name", name);
        jsonObject.put("joinMessage", JSONComponentSerializer.json().serialize(joinMessage));
        jsonObject.put("quitMessage", JSONComponentSerializer.json().serialize(quitMessage));
        jsonObject.put("texture", texture);
        jsonObject.put("signature", signature);
        return jsonObject.toJSONString();
    }

    public static FakeData fromString(String string) {
        JSONParser parser = new JSONParser();
        JSONObject jsonObject;
        try {
            jsonObject = (JSONObject) parser.parse(string);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return new FakeData((String) jsonObject.get("name"), JSONComponentSerializer.json().deserialize((String) jsonObject.get("joinMessage")), JSONComponentSerializer.json().deserialize((String) jsonObject.get("quitMessage")), (String) jsonObject.get("texture"), (String) jsonObject.get("signature"));
    }
}
