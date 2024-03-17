package me.chrommob.fakeplayer.impl;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;

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
}
