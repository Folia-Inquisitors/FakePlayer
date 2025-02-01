package me.chrommob.fakeplayer.packet;

import com.github.retrooper.packetevents.event.SimplePacketListenerAbstract;
import com.github.retrooper.packetevents.event.simple.PacketStatusSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.status.server.WrapperStatusServerResponse;
import com.google.gson.JsonObject;
import me.chrommob.fakeplayer.api.FakePlayerAPI;

public class PlayerCount extends SimplePacketListenerAbstract {
    private final FakePlayerAPI plugin = FakePlayerAPI.getInstance();
    @Override
    public void onPacketStatusSend(PacketStatusSendEvent event) {
        if (!event.getPacketType().equals(PacketType.Status.Server.RESPONSE)) {
            return;
        }
        WrapperStatusServerResponse response = new WrapperStatusServerResponse(event);
        JsonObject payload = response.getComponent();
        JsonObject playersElement = payload.get("players").getAsJsonObject();
        int online = playersElement.get("online").getAsInt();
        int fakePlayers = plugin.getFakePlayers().size();
        playersElement.addProperty("online", online + fakePlayers);
        response.setComponent(payload);
    }
}
