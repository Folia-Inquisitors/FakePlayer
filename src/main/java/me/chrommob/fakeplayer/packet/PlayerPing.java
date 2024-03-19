package me.chrommob.fakeplayer.packet;

import com.github.retrooper.packetevents.event.SimplePacketListenerAbstract;
import com.github.retrooper.packetevents.event.simple.PacketPlaySendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoUpdate;

import java.util.HashMap;
import java.util.Map;

public class PlayerPing extends SimplePacketListenerAbstract {
    private Map<String, Long> lastPing = new HashMap<>();
    public void onPacketPlaySend(PacketPlaySendEvent event) {
        if (!event.getPacketType().equals(PacketType.Play.Server.PLAYER_INFO_UPDATE)) {
            return;
        }
        WrapperPlayServerPlayerInfoUpdate playerInfoUpdate = new WrapperPlayServerPlayerInfoUpdate(event);
        if (playerInfoUpdate.getActions().stream().noneMatch(action -> action.equals(WrapperPlayServerPlayerInfoUpdate.Action.UPDATE_LATENCY))) {
            return;
        }
        playerInfoUpdate.getEntries().forEach(entry -> {
            if (entry.getLatency() == -1) {
                lastPing.put(entry.getGameProfile().getName(), System.currentTimeMillis());
            } else
                if (lastPing.containsKey(entry.getGameProfile().getName())) {
                    long ping = System.currentTimeMillis() - lastPing.get(entry.getGameProfile().getName());
                    System.out.println("Sent ping update for " + entry.getGameProfile().getName() + " after " + ping + "ms");
                } else {
                    lastPing.put(entry.getGameProfile().getName(), System.currentTimeMillis());
                }
        });
    }
}
