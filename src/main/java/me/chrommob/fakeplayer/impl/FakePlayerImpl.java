package me.chrommob.fakeplayer.impl;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.protocol.player.TextureProperty;
import com.github.retrooper.packetevents.protocol.player.UserProfile;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoRemove;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoUpdate;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Message;
import github.scarsz.discordsrv.dependencies.jda.api.entities.TextChannel;
import github.scarsz.discordsrv.objects.MessageFormat;
import github.scarsz.discordsrv.util.*;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import me.chrommob.fakeplayer.FakePlayer;
import me.chrommob.fakeplayer.data.FakeData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.function.BiFunction;

public class FakePlayerImpl implements Listener {
    private final FakePlayer plugin = FakePlayer.getPlugin(FakePlayer.class);
    private FakeData fakeData;
    private final UUID uuid = UUID.randomUUID();
    private boolean isOnline;
    private final WrapperPlayServerPlayerInfoUpdate playerInfoPacket;
    private ScheduledTask scheduledTask;
    private BukkitTask bukkitTask;

    public FakePlayerImpl(FakeData fakePlayer) {
        this.fakeData = fakePlayer;
        playerInfoPacket = createPlayerInfoPacket();
        onJoin();
        if (plugin.isFolia()) {
            scheduledTask = Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, scheduledTask -> updateLatency(),
                    7 * 20L, 30 * 20L);
        } else {
            bukkitTask = Bukkit.getScheduler().runTaskTimer(plugin, this::updateLatency, 7 * 20L, 30 * 20L);
        }
    }

    public boolean isOnline() {
        return isOnline;
    }

    public WrapperPlayServerPlayerInfoUpdate createPlayerInfoPacket() {
        UserProfile userProfile = getUserProfile();
        WrapperPlayServerPlayerInfoUpdate.PlayerInfo playerInfo = new WrapperPlayServerPlayerInfoUpdate.PlayerInfo(
                userProfile, true, 0, GameMode.SURVIVAL, Component.text(fakeData.getName()), null);
        EnumSet<WrapperPlayServerPlayerInfoUpdate.Action> actions = EnumSet.of(
                WrapperPlayServerPlayerInfoUpdate.Action.ADD_PLAYER,
                WrapperPlayServerPlayerInfoUpdate.Action.UPDATE_LISTED,
                WrapperPlayServerPlayerInfoUpdate.Action.UPDATE_LATENCY);
        return new WrapperPlayServerPlayerInfoUpdate(actions, playerInfo);
    }

    private void updateLatency() {
        if (isOnline) {
            int latency;
            boolean isHighLatency = Math.random() < 0.05;
            if (isHighLatency) {
                latency = new Random().nextInt(1000);
            } else {
                latency = new Random().nextInt(150) + 50;
            }
            plugin.getDebugger()
                    .debug("Setting latency of " + fakeData.getName() + " with UUID " + uuid + " to " + latency);
            playerInfoPacket.getEntries().get(0).setLatency(latency);
            for (Player player : Bukkit.getOnlinePlayers()) {
                PacketEvents.getAPI().getPlayerManager().sendPacket(player, clone(playerInfoPacket));
            }
        }
    }

    @NotNull
    private UserProfile getUserProfile() {
        TextureProperty textureProperty = null;
        if (fakeData.getTexture() != null && fakeData.getSignature() != null) {
            textureProperty = new TextureProperty("textures", fakeData.getTexture(), fakeData.getSignature());
        }
        UserProfile userProfile;
        if (textureProperty == null) {
            userProfile = new UserProfile(uuid, fakeData.getName());
        } else {
            userProfile = new UserProfile(uuid, fakeData.getName(), List.of(textureProperty));
        }
        return userProfile;
    }

    private void onJoin() {
        isOnline = true;
        Bukkit.getServer().broadcast(fakeData.getJoinMessage());
        if (Bukkit.getPluginManager().getPlugin("DiscordSRV") != null) {
            DiscordSRV discordSRV = DiscordSRV.getPlugin(DiscordSRV.class);
            MessageFormat messageFormat = discordSRV.getMessageFromConfiguration("MinecraftPlayerJoinMessage");
            if (messageFormat == null || !messageFormat.isAnyContent()) {
                DiscordSRV.debug("Not sending join message due to it being disabled");
                return;
            }

            TextChannel textChannel = discordSRV.getOptionalTextChannel("join");
            if (textChannel == null) {
                DiscordSRV.debug("Not sending join message, text channel is null");
                return;
            }

            final String joinMessage = PlainTextComponentSerializer.plainText().serialize(fakeData.getJoinMessage());
            final String displayName = StringUtils.isNotBlank(fakeData.getName()) ? MessageUtil.strip(fakeData.getName()) : "";
            final String message = StringUtils.isNotBlank(joinMessage) ? joinMessage : "";
            final String name = fakeData.getName();
            final String avatarUrl = "https://cravatar.eu/helmavatar/" + name + "/128.png";

            final String botAvatarUrl = DiscordUtil.getJda().getSelfUser().getEffectiveAvatarUrl();
            String botName = discordSRV.getMainGuild() != null ? discordSRV.getMainGuild().getSelfMember().getEffectiveName() : DiscordUtil.getJda().getSelfUser().getName();

            BiFunction<String, Boolean, String> translator = (content, needsEscape) -> {
                if (content == null) return null;
                content = content
                        .replaceAll("%time%|%date%", TimeUtil.timeStamp())
                        .replace("%message%", MessageUtil.strip(needsEscape ? DiscordUtil.escapeMarkdown(message) : message))
                        .replace("%username%", needsEscape ? DiscordUtil.escapeMarkdown(name) : name)
                        .replace("%displayname%", needsEscape ? DiscordUtil.escapeMarkdown(displayName) : displayName)
                        .replace("%usernamenoescapes%", name)
                        .replace("%displaynamenoescapes%", displayName)
                        .replace("%embedavatarurl%", avatarUrl)
                        .replace("%botavatarurl%", botAvatarUrl)
                        .replace("%botname%", botName);
                content = DiscordUtil.translateEmotes(content, textChannel.getGuild());
                content = PlaceholderUtil.replacePlaceholdersToDiscord(content, Bukkit.getOfflinePlayer(uuid));
                return content;
            };

            Message discordMessage = DiscordSRV.translateMessage(messageFormat, translator);
            if (discordMessage == null) return;

            String webhookName = translator.apply(messageFormat.getWebhookName(), false);
            String webhookAvatarUrl = translator.apply(messageFormat.getWebhookAvatarUrl(), false);

            if (messageFormat.isUseWebhooks()) {
                WebhookUtil.deliverMessage(textChannel, webhookName, webhookAvatarUrl,
                        discordMessage.getContentRaw(), discordMessage.getEmbeds().stream().findFirst().orElse(null));
            } else {
                DiscordUtil.queueMessage(textChannel, discordMessage, true);
            }
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            PacketEvents.getAPI().getPlayerManager().sendPacket(player, clone(playerInfoPacket));
        }
    }

    public void quit() {
        if (isOnline) {
            broadcastQuitMessage();
            if (plugin.isFolia()) {
                scheduledTask.cancel();
            } else {
                bukkitTask.cancel();
            }
        }
        WrapperPlayServerPlayerInfoRemove removePacket = new WrapperPlayServerPlayerInfoRemove(uuid);
        for (Player player : Bukkit.getOnlinePlayers()) {
            PacketEvents.getAPI().getPlayerManager().sendPacket(player, removePacket);
        }
        if (plugin.isFolia()) {
            Bukkit.getGlobalRegionScheduler().runDelayed(plugin, scheduledTask -> {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    PacketEvents.getAPI().getPlayerManager().sendPacket(player, removePacket);
                }
            }, 20);
        } else {
            Bukkit.getScheduler().runTaskLater(FakePlayer.getPlugin(FakePlayer.class), () -> {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    PacketEvents.getAPI().getPlayerManager().sendPacket(player, removePacket);
                }
            }, 20);
        }
    }

    public void onPlayerJoin(PlayerJoinEvent event) {
        if (event.getPlayer().getName().equals(fakeData.getName())) {
            if (event.joinMessage() != null) {
                event.getPlayer().sendMessage(event.joinMessage());
            }
            event.joinMessage(null);
            if (rename()) {
                int delay = (int) (Math.random() * 10) + 5;
                if (plugin.isFolia()) {
                    Bukkit.getGlobalRegionScheduler().runDelayed(plugin, scheduledTask -> onJoin(), delay);
                } else {
                    Bukkit.getScheduler().runTaskLater(FakePlayer.getPlugin(FakePlayer.class), this::onJoin, delay);
                }
            }
            return;
        }
        if (!isOnline) {
            return;
        }
        PacketEvents.getAPI().getPlayerManager().sendPacket(event.getPlayer(), clone(playerInfoPacket));
    }

    private boolean rename() {
        isOnline = false;
        FakeData newFakeData = plugin.getNextAvailableFakePlayer();
        plugin.removeFakePlayer(fakeData.getName());
        if (newFakeData == null) {
            return false;
        }
        plugin.addSelf(fakeData.getName(), this);
        fakeData = newFakeData;
        playerInfoPacket.getEntries().get(0).getGameProfile().setName(fakeData.getName());
        return true;
    }

    public void broadcastQuitMessage() {
        Bukkit.getServer().broadcast(fakeData.getQuitMessage());
        if (Bukkit.getPluginManager().getPlugin("DiscordSRV") != null) {
                DiscordSRV discordSRV = DiscordSRV.getPlugin();
                MessageFormat messageFormat = discordSRV.getMessageFromConfiguration("MinecraftPlayerLeaveMessage");
                if (messageFormat == null || !messageFormat.isAnyContent()) {
                    DiscordSRV.debug("Not sending leave message due to it being disabled");
                    return;
                }

                TextChannel textChannel = discordSRV.getOptionalTextChannel("leave");
                if (textChannel == null) {
                    DiscordSRV.debug("Not sending quit message, text channel is null");
                    return;
                }

                final String quitMessage = PlainTextComponentSerializer.plainText().serialize(fakeData.getQuitMessage());
                final String displayName = StringUtils.isNotBlank(fakeData.getName()) ? MessageUtil.strip(fakeData.getName()) : "";
                final String message = StringUtils.isNotBlank(quitMessage) ? quitMessage : "";
                final String name = fakeData.getName();

                String avatarUrl = "https://cravatar.eu/helmavatar/" + name + "/128.png";
                String botAvatarUrl = DiscordUtil.getJda().getSelfUser().getEffectiveAvatarUrl();
                String botName = discordSRV.getMainGuild() != null ? discordSRV.getMainGuild().getSelfMember().getEffectiveName() : DiscordUtil.getJda().getSelfUser().getName();

                BiFunction<String, Boolean, String> translator = (content, needsEscape) -> {
                    if (content == null) return null;
                    content = content
                            .replaceAll("%time%|%date%", TimeUtil.timeStamp())
                            .replace("%message%", MessageUtil.strip(needsEscape ? DiscordUtil.escapeMarkdown(message) : message))
                            .replace("%username%", MessageUtil.strip(needsEscape ? DiscordUtil.escapeMarkdown(name) : name))
                            .replace("%displayname%", needsEscape ? DiscordUtil.escapeMarkdown(displayName) : displayName)
                            .replace("%usernamenoescapes%", name)
                            .replace("%displaynamenoescapes%", displayName)
                            .replace("%embedavatarurl%", avatarUrl)
                            .replace("%botavatarurl%", botAvatarUrl)
                            .replace("%botname%", botName);
                    content = DiscordUtil.translateEmotes(content, textChannel.getGuild());
                    return content;
                };

                Message discordMessage = DiscordSRV.translateMessage(messageFormat, translator);
                if (discordMessage == null) return;

                String webhookName = translator.apply(messageFormat.getWebhookName(), false);
                String webhookAvatarUrl = translator.apply(messageFormat.getWebhookAvatarUrl(), false);

                if (messageFormat.isUseWebhooks()) {
                    WebhookUtil.deliverMessage(textChannel, webhookName, webhookAvatarUrl,
                            discordMessage.getContentRaw(), discordMessage.getEmbeds().stream().findFirst().orElse(null));
                } else {
                    DiscordUtil.queueMessage(textChannel, discordMessage, true);
                }
        }
    }

    public static WrapperPlayServerPlayerInfoUpdate clone(WrapperPlayServerPlayerInfoUpdate packet) {
        return new WrapperPlayServerPlayerInfoUpdate(packet.getActions(), packet.getEntries());
    }

    public void death(Component fakeDeathMessage, FakePlayerImpl other) {
        Bukkit.getServer().broadcast(fakeDeathMessage
                .replaceText(TextReplacementConfig.builder().match("%player%").replacement(fakeData.getName()).build())
                .replaceText(TextReplacementConfig.builder().match("%player2%").replacement(other.fakeData.getName()).build())
        );
    }

    public void achievement(Component fakeAchievementMessage) {
        Bukkit.getServer().broadcast(fakeAchievementMessage
                .replaceText(TextReplacementConfig.builder().match("%player%").replacement(fakeData.getName()).build())
        );
    }

    public UUID getUuid() {
        return uuid;
    }
}
