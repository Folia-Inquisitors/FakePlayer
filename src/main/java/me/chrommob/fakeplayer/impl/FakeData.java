package me.chrommob.fakeplayer.impl;

import org.bukkit.entity.Player;

public class FakeData {
    public FakeData(Player player) {
        player.getPlayerProfile().getProperties().forEach(property -> {
            System.out.println(property.getName() + " " + property.getValue() + " " + property.getSignature());
        });
    }
}
