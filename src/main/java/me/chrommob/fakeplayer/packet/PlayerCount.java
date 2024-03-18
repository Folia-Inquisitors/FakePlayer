package me.chrommob.fakeplayer.packet;

import com.github.retrooper.packetevents.event.SimplePacketListenerAbstract;
import com.github.retrooper.packetevents.event.simple.PacketStatusSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.status.server.WrapperStatusServerResponse;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import me.chrommob.fakeplayer.FakePlayerAPI;

public class PlayerCount extends SimplePacketListenerAbstract {
    private final FakePlayerAPI plugin = FakePlayerAPI.getInstance();
    @Override
    public void onPacketStatusSend(PacketStatusSendEvent event) {
        if (!event.getPacketType().equals(PacketType.Status.Server.RESPONSE)) {
            return;
        }
        WrapperStatusServerResponse response = new WrapperStatusServerResponse(event);
        JsonObject jsonObject = response.getComponent();
        JsonElement jsonElement = jsonObject.get("players").getAsJsonObject().get("online");
        int online = jsonElement.getAsInt();
        int fakePlayers = plugin.getFakePlayers().size();
        jsonObject.get("players").getAsJsonObject().addProperty("online", online + fakePlayers);
        response.setComponent(jsonObject);
    }
}
