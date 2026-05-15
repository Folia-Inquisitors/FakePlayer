package me.chrommob.fakeplayer.data.model;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;

public final class FakePlayerProfileFactory {
    private FakePlayerProfileFactory() {
    }

    public static FakePlayerProfile from(Player player, PlayerJoinEvent event) {
        String texture = null;
        String signature = null;
        PlayerProfile playerProfile = player.getPlayerProfile();
        for (ProfileProperty property : playerProfile.getProperties()) {
            if ("textures".equals(property.getName())) {
                texture = property.getValue();
                signature = property.getSignature();
                break;
            }
        }
        return new FakePlayerProfile(player.getName(), event.joinMessage(), null, texture, signature);
    }
}
