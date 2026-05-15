package me.chrommob.fakeplayer.data;

import me.chrommob.fakeplayer.data.model.FakePlayerProfile;
import me.chrommob.fakeplayer.data.model.FakePlayerProfileCodec;
import me.chrommob.fakeplayer.data.model.FakePlayerProfileFactory;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * Compatibility adapter for the old FakeData type. New plugin code should use FakePlayerProfile.
 */
@Deprecated
public class FakeData {
    private FakePlayerProfile profile;

    public FakeData(Player player, PlayerJoinEvent event) {
        this(FakePlayerProfileFactory.from(player, event));
    }

    public FakeData(String name, Component joinMessage, Component quitMessage, String texture, String signature) {
        this(new FakePlayerProfile(name, joinMessage, quitMessage, texture, signature));
    }

    public FakeData(FakePlayerProfile profile) {
        this.profile = profile;
    }

    public FakePlayerProfile toProfile() {
        return profile;
    }

    public String getTexture() {
        return profile.texture();
    }

    public String getSignature() {
        return profile.signature();
    }

    public String getName() {
        return profile.name();
    }

    public Component getJoinMessage() {
        return profile.joinMessage();
    }

    public void setQuitMessage(Component quitMessage) {
        profile = profile.withQuitMessage(quitMessage);
    }

    public Component getQuitMessage() {
        return profile.quitMessage();
    }

    public boolean isReady() {
        return profile.isReady();
    }

    @Override
    public String toString() {
        return FakePlayerProfileCodec.toLegacyJson(profile);
    }

    public static FakeData fromString(String string) {
        FakePlayerProfile profile = FakePlayerProfileCodec.fromLegacyJson(string);
        return profile == null ? null : new FakeData(profile);
    }
}
