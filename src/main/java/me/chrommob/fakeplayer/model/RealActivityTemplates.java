package me.chrommob.fakeplayer.model;

import me.chrommob.fakeplayer.util.SystemChatComponentSanitizer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.serializer.json.JSONComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

public final class RealActivityTemplates {
    private static final JSONComponentSerializer JSON = JSONComponentSerializer.json();
    private static final PlainTextComponentSerializer PLAIN_TEXT = PlainTextComponentSerializer.plainText();

    private RealActivityTemplates() {
    }

    public static Component achievementTemplate(Player player, Component achievementMessage) {
        return SystemChatComponentSanitizer.sanitize(replacePlayerReferences(achievementMessage, player, "%player%"));
    }

    public static String deathTemplate(Player deadPlayer, Component deathMessage, Collection<? extends Player> onlinePlayers) {
        Component template = replacePlayerReferences(deathMessage, deadPlayer, "%player%");
        for (Player otherPlayer : onlinePlayers) {
            if (samePlayer(deadPlayer, otherPlayer)) {
                continue;
            }
            Component withOtherPlayer = replacePlayerReferences(template, otherPlayer, "%player2%");
            if (!JSON.serialize(withOtherPlayer).equals(JSON.serialize(template))) {
                template = withOtherPlayer;
                break;
            }
        }
        return JSON.serialize(SystemChatComponentSanitizer.sanitize(template));
    }

    private static Component replacePlayerReferences(Component message, Player player, String placeholder) {
        Component replaced = message;
        for (String reference : playerReferences(player)) {
            replaced = replaced.replaceText(TextReplacementConfig.builder()
                    .match(Pattern.compile(Pattern.quote(reference)))
                    .replacement(placeholder)
                    .build());
        }
        return replaced;
    }

    private static List<String> playerReferences(Player player) {
        Set<String> references = new LinkedHashSet<>();
        references.add(player.getName());
        references.add(player.getDisplayName());
        references.add(PLAIN_TEXT.serialize(player.displayName()));
        references.removeIf(reference -> reference == null || reference.isBlank());

        List<String> sortedReferences = new ArrayList<>(references);
        sortedReferences.sort(Comparator.comparingInt(String::length).reversed());
        return sortedReferences;
    }

    private static boolean samePlayer(Player first, Player second) {
        UUID firstUuid = first.getUniqueId();
        UUID secondUuid = second.getUniqueId();
        return firstUuid.equals(secondUuid);
    }
}
