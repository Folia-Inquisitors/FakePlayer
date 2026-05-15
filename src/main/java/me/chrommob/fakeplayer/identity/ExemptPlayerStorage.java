package me.chrommob.fakeplayer.identity;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public final class ExemptPlayerStorage {
    private final Plugin plugin;
    private final File file;

    public ExemptPlayerStorage(Plugin plugin, File dataFolder) {
        this.plugin = plugin;
        if (!dataFolder.exists() && !dataFolder.mkdirs()) {
            plugin.getLogger().warning("Could not create data folder " + dataFolder);
        }
        file = new File(dataFolder, "exempted-players.yml");
    }

    public Map<UUID, ExemptPlayerRecord> load() {
        Map<UUID, ExemptPlayerRecord> records = new LinkedHashMap<>();
        if (!file.exists()) {
            return records;
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection players = yaml.getConfigurationSection("players");
        if (players == null) {
            return records;
        }

        for (String key : players.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                ExemptPlayerRecord record = ExemptPlayerRecord.fromYaml(uuid, players.get(key));
                if (record != null) {
                    records.put(uuid, record);
                }
            } catch (RuntimeException ignored) {
            }
        }
        return records;
    }

    public void save(Map<UUID, ExemptPlayerRecord> records) {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("version", 1);
        for (Map.Entry<UUID, ExemptPlayerRecord> entry : records.entrySet()) {
            ExemptPlayerRecord record = entry.getValue();
            if (record != null) {
                yaml.set("players." + entry.getKey(), record.toYaml());
            }
        }
        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Could not save exempted-players.yml", e);
        }
    }
}
