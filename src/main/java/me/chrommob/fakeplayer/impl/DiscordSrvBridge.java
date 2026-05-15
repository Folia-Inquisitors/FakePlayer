package me.chrommob.fakeplayer.impl;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Message;
import github.scarsz.discordsrv.dependencies.jda.api.entities.TextChannel;
import github.scarsz.discordsrv.objects.MessageFormat;
import github.scarsz.discordsrv.util.DiscordUtil;
import github.scarsz.discordsrv.util.MessageUtil;
import github.scarsz.discordsrv.util.PlaceholderUtil;
import github.scarsz.discordsrv.util.TimeUtil;
import github.scarsz.discordsrv.util.WebhookUtil;
import me.chrommob.fakeplayer.FakePlayer;
import me.chrommob.fakeplayer.data.model.FakePlayerProfile;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;

import java.util.UUID;
import java.util.function.BiFunction;

final class DiscordSrvBridge {
    private static final PlainTextComponentSerializer PLAIN_TEXT = PlainTextComponentSerializer.plainText();

    private DiscordSrvBridge() {
    }

    static void sendJoinMessage(FakePlayerProfile fakeData, UUID uuid) {
        if (!isAvailable()) {
            return;
        }
        sendPlayerActivityMessage(
                "MinecraftPlayerJoinMessage",
                "join",
                fakeData,
                uuid,
                PLAIN_TEXT.serialize(fakeData.joinMessage()),
                "join"
        );
    }

    static void sendLeaveMessage(FakePlayerProfile fakeData, UUID uuid) {
        if (!isAvailable()) {
            return;
        }
        sendPlayerActivityMessage(
                "MinecraftPlayerLeaveMessage",
                "leave",
                fakeData,
                uuid,
                PLAIN_TEXT.serialize(fakeData.quitMessage()),
                "quit"
        );
    }

    static void sendDeathMessage(FakePlayerProfile fakeData, UUID uuid, Component deathMessage) {
        if (!isAvailable()) {
            return;
        }
        String plainMessage = PLAIN_TEXT.serialize(deathMessage);
        sendGameMessage(
                "MinecraftPlayerDeathMessage",
                "deaths",
                fakeData,
                uuid,
                plainMessage,
                plainMessage,
                "deathmessage"
        );
    }

    static void sendAchievementMessage(FakePlayerProfile fakeData, UUID uuid, Component achievementMessage) {
        if (!isAvailable()) {
            return;
        }
        String plainMessage = PLAIN_TEXT.serialize(achievementMessage);
        String achievementName = extractAchievementName(plainMessage);
        sendGameMessage(
                "MinecraftPlayerAchievementMessage",
                "awards",
                fakeData,
                uuid,
                plainMessage,
                achievementName,
                "achievement"
        );
    }

    private static boolean isAvailable() {
        return Bukkit.getPluginManager().isPluginEnabled("DiscordSRV");
    }

    private static void sendPlayerActivityMessage(String messageKey, String channelKey, FakePlayerProfile fakeData, UUID uuid,
                                                  String minecraftMessage, String debugName) {
        try {
            DiscordSRV discordSRV = DiscordSRV.getPlugin();
            MessageFormat messageFormat = discordSRV.getMessageFromConfiguration(messageKey);
            if (messageFormat == null || !messageFormat.isAnyContent()) {
                DiscordSRV.debug("Not sending fake " + debugName + " message due to it being disabled");
                return;
            }

            TextChannel textChannel = discordSRV.getOptionalTextChannel(channelKey);
            if (textChannel == null) {
                DiscordSRV.debug("Not sending fake " + debugName + " message, text channel is null");
                return;
            }

            MessageContext context = MessageContext.from(fakeData, uuid, minecraftMessage, textChannel);
            BiFunction<String, Boolean, String> translator = (content, needsEscape) -> translateActivity(content,
                    needsEscape, context);
            deliver(messageFormat, textChannel, translator);
        } catch (Throwable throwable) {
            FakePlayer.getPlugin(FakePlayer.class).getLogger()
                    .warning("Failed to send fake " + debugName + " DiscordSRV message: " + throwable.getMessage());
        }
    }

    private static void sendGameMessage(String messageKey, String channelKey, FakePlayerProfile fakeData, UUID uuid,
                                        String minecraftMessage, String formattedValue, String formattedValueKey) {
        try {
            DiscordSRV discordSRV = DiscordSRV.getPlugin();
            String gameChannelName = discordSRV.getOptionalChannel(channelKey);
            MessageFormat messageFormat = discordSRV.getMessageFromConfiguration(messageKey);
            if (messageFormat == null || !messageFormat.isAnyContent()) {
                DiscordSRV.debug("Not sending fake " + formattedValueKey + " message due to it being disabled");
                return;
            }

            TextChannel textChannel = discordSRV.getDestinationTextChannelForGameChannelName(gameChannelName);
            if (textChannel == null) {
                DiscordSRV.debug("Not sending fake " + formattedValueKey + " message, text channel is null");
                return;
            }

            MessageContext context = MessageContext.from(fakeData, uuid, minecraftMessage, textChannel);
            BiFunction<String, Boolean, String> translator = (content, needsEscape) -> translateGame(content,
                    needsEscape, context, formattedValueKey, formattedValue);
            deliver(messageFormat, textChannel, translator);
        } catch (Throwable throwable) {
            FakePlayer.getPlugin(FakePlayer.class).getLogger().warning(
                    "Failed to send fake " + formattedValueKey + " DiscordSRV message: " + throwable.getMessage()
            );
        }
    }

    private static void deliver(MessageFormat messageFormat, TextChannel textChannel,
                                BiFunction<String, Boolean, String> translator) {
        Message discordMessage = DiscordSRV.translateMessage(messageFormat, translator);
        if (discordMessage == null || DiscordSRV.getLength(discordMessage) < 3) {
            return;
        }

        String webhookName = translator.apply(messageFormat.getWebhookName(), false);
        String webhookAvatarUrl = translator.apply(messageFormat.getWebhookAvatarUrl(), false);

        if (messageFormat.isUseWebhooks()) {
            WebhookUtil.deliverMessage(textChannel, webhookName, webhookAvatarUrl,
                    discordMessage.getContentRaw(), discordMessage.getEmbeds().stream().findFirst().orElse(null));
        } else {
            DiscordUtil.queueMessage(textChannel, discordMessage, true);
        }
    }

    private static String translateActivity(String content, boolean needsEscape, MessageContext context) {
        if (content == null) {
            return null;
        }
        return applyPlaceholders(content, needsEscape, context)
                .replace("%message%", MessageUtil.strip(escapeIfNeeded(context.minecraftMessage, needsEscape)));
    }

    private static String translateGame(String content, boolean needsEscape, MessageContext context,
                                        String formattedValueKey, String formattedValue) {
        if (content == null) {
            return null;
        }

        String translated = applyPlaceholders(content, needsEscape, context)
                .replace("%message%", MessageUtil.strip(escapeIfNeeded(context.minecraftMessage, needsEscape)))
                .replace("%" + formattedValueKey + "%", MessageUtil.strip(escapeIfNeeded(formattedValue, needsEscape)));

        if ("deathmessage".equals(formattedValueKey)) {
            translated = translated.replace("%deathmessagenoescapes%", MessageUtil.strip(formattedValue));
        }
        return translated;
    }

    private static String applyPlaceholders(String content, boolean needsEscape, MessageContext context) {
        String translated = content
                .replaceAll("%time%|%date%", TimeUtil.timeStamp())
                .replace("%username%", escapeIfNeeded(context.name, needsEscape))
                .replace("%displayname%", escapeIfNeeded(context.displayName, needsEscape))
                .replace("%usernamenoescapes%", context.name)
                .replace("%displaynamenoescapes%", context.displayName)
                .replace("%world%", context.world)
                .replace("%embedavatarurl%", context.avatarUrl)
                .replace("%botavatarurl%", context.botAvatarUrl)
                .replace("%botname%", context.botName);

        translated = DiscordUtil.translateEmotes(translated, context.textChannel.getGuild());
        return PlaceholderUtil.replacePlaceholdersToDiscord(translated, Bukkit.getOfflinePlayer(context.uuid));
    }

    private static String escapeIfNeeded(String value, boolean needsEscape) {
        String safeValue = value == null ? "" : value;
        return needsEscape ? DiscordUtil.escapeMarkdown(safeValue) : safeValue;
    }

    private static String extractAchievementName(String plainMessage) {
        int end = plainMessage.lastIndexOf(']');
        int start = plainMessage.lastIndexOf('[', end);
        if (start != -1 && end > start) {
            return plainMessage.substring(start + 1, end);
        }
        return plainMessage;
    }

    private static String firstWorldName() {
        if (Bukkit.getWorlds().isEmpty()) {
            return "world";
        }
        return Bukkit.getWorlds().getFirst().getName();
    }

    private record MessageContext(
            String name,
            String displayName,
            UUID uuid,
            String minecraftMessage,
            String world,
            String avatarUrl,
            String botAvatarUrl,
            String botName,
            TextChannel textChannel
    ) {
        private static MessageContext from(FakePlayerProfile fakeData, UUID uuid, String minecraftMessage,
                                           TextChannel textChannel) {
            DiscordSRV discordSRV = DiscordSRV.getPlugin();
            String name = fakeData.name();
            String displayName = MessageUtil.strip(name);
            String avatarUrl = "https://cravatar.eu/helmavatar/" + name + "/128.png";
            String botAvatarUrl = DiscordUtil.getJda().getSelfUser().getEffectiveAvatarUrl();
            String botName = discordSRV.getMainGuild() != null
                    ? discordSRV.getMainGuild().getSelfMember().getEffectiveName()
                    : DiscordUtil.getJda().getSelfUser().getName();

            return new MessageContext(
                    name,
                    displayName,
                    uuid,
                    minecraftMessage == null ? "" : minecraftMessage,
                    firstWorldName(),
                    avatarUrl,
                    botAvatarUrl,
                    botName,
                    textChannel
            );
        }
    }
}
