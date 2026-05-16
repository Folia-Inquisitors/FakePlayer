package me.chrommob.fakeplayer.interaction;

import me.chrommob.fakeplayer.FakePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public final class YatpaTeleportGuard implements Listener {
    private static final String PRE_REQUEST_EVENT = "me.hsgamer.yatpa.event.PreTeleportRequestEvent";
    private static final String MESSAGE_CONFIG = "me.hsgamer.yatpa.config.MessageConfig";
    private static final String MESSAGE_UTILS = "me.hsgamer.yatpa.lib.core.bukkit.utils.MessageUtils";

    private final FakePlayer plugin;
    private boolean active;

    public YatpaTeleportGuard(FakePlayer plugin) {
        this.plugin = plugin;
    }

    public void registerIfAvailable() {
        Plugin yatpa = plugin.getServer().getPluginManager().getPlugin("YATPA");
        if (yatpa == null || !yatpa.isEnabled()) {
            return;
        }
        try {
            Class<? extends Event> eventClass = Class.forName(PRE_REQUEST_EVENT, false, yatpa.getClass().getClassLoader())
                    .asSubclass(Event.class);
            plugin.getServer().getPluginManager().registerEvent(eventClass,
                    this,
                    EventPriority.HIGHEST,
                    this::handlePreRequest,
                    plugin,
                    true);
            active = true;
            plugin.getDebugger().debug("Enabled YATPA fake-player teleport denial hook");
        } catch (ReflectiveOperationException | LinkageError exception) {
            plugin.getLogger().warning("YATPA was found, but FakePlayer could not hook its teleport request event.");
        }
    }

    public boolean isActive() {
        return active;
    }

    private void handlePreRequest(Listener listener, Event event) {
        String targetName = invokeString(event, "getTargetPlayerName");
        if (!plugin.isSuspectedFakePlayerName(targetName)) {
            return;
        }
        Player requester = invokePlayer(event, "getPlayer");
        invokeVoid(event, "setCancelled", boolean.class, true);
        if (requester != null) {
            sendYatpaDenyFromMessage(requester, targetName);
        }
        plugin.getDebugger().debug("Denied YATPA teleport request from "
                + (requester == null ? "unknown" : requester.getName())
                + " to fake player " + targetName);
    }

    private void sendYatpaDenyFromMessage(Player requester, String targetName) {
        Plugin yatpa = plugin.getServer().getPluginManager().getPlugin("YATPA");
        if (yatpa == null || !yatpa.isEnabled()) {
            return;
        }
        try {
            Object messageConfig = yatpa.getClass().getMethod("getMessageConfig").invoke(yatpa);
            ClassLoader classLoader = yatpa.getClass().getClassLoader();
            Method denyFrom = Class.forName(MESSAGE_CONFIG, false, classLoader).getMethod("getRequestDenyFrom");
            Object rawMessage = denyFrom.invoke(messageConfig);
            if (!(rawMessage instanceof String message) || message.isBlank()) {
                return;
            }
            message = message.replace("{player}", targetName);
            Class.forName(MESSAGE_UTILS, false, classLoader)
                    .getMethod("sendMessage", CommandSender.class, String.class)
                    .invoke(null, requester, message);
        } catch (ReflectiveOperationException | LinkageError exception) {
            plugin.getDebugger().debug("Could not send YATPA deny message through YATPA; no FakePlayer fallback was sent");
        }
    }

    private String invokeString(Event event, String methodName) {
        Object value = invoke(event, methodName);
        return value instanceof String string ? string : null;
    }

    private Player invokePlayer(Event event, String methodName) {
        Object value = invoke(event, methodName);
        return value instanceof Player player ? player : null;
    }

    private Object invoke(Event event, String methodName) {
        try {
            return event.getClass().getMethod(methodName).invoke(event);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException exception) {
            return null;
        }
    }

    private void invokeVoid(Event event, String methodName, Class<?> parameterType, Object value) {
        try {
            event.getClass().getMethod(methodName, parameterType).invoke(event, value);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException ignored) {
        }
    }
}
