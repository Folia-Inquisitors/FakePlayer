package me.chrommob.fakeplayer.impl;

import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

import me.chrommob.fakeplayer.FakePlayer;

public class Debugger {
    private final File debugFile;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("[HH:mm:ss]");
    public Debugger(FakePlayer fakePlayer) {
        this.debugFile = new File(fakePlayer.getDataFolder(), "debug.log");
        if (!debugFile.exists()) {
            try {
                debugFile.createNewFile();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            debugFile.delete();
            try {
                debugFile.createNewFile();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void debug(String message) {
        String formattedMessage = dateFormat.format(new Date()) + " " + message;
        try (FileWriter writer = new FileWriter(debugFile, true)) {
            writer.write(formattedMessage + "\n");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
