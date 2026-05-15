package me.chrommob.fakeplayer.data.model;

import me.chrommob.fakeplayer.util.SystemChatComponentSanitizer;
import net.kyori.adventure.text.Component;
import java.util.Objects;

public record FakePlayerProfile(
        String name,
        Component joinMessage,
        Component quitMessage,
        String texture,
        String signature
) {
    public FakePlayerProfile {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(joinMessage, "joinMessage");
        joinMessage = SystemChatComponentSanitizer.sanitize(joinMessage);
        quitMessage = SystemChatComponentSanitizer.sanitize(quitMessage);
    }

    public FakePlayerProfile withQuitMessage(Component quitMessage) {
        return new FakePlayerProfile(name, joinMessage, quitMessage, texture, signature);
    }

    public boolean isReady() {
        return quitMessage != null;
    }
}
