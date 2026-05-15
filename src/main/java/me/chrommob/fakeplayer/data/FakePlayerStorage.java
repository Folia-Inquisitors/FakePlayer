package me.chrommob.fakeplayer.data;

import me.chrommob.fakeplayer.FakePlayer;
import me.chrommob.fakeplayer.data.model.FakePlayerProfile;
import me.chrommob.fakeplayer.data.model.FakePlayerProfileCodec;
import me.chrommob.fakeplayer.data.model.FakePlayerProgress;
import me.chrommob.fakeplayer.data.model.StoredFakePlayerState;
import me.chrommob.fakeplayer.data.model.WeightedMessageSet;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.json.JSONComponentSerializer;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.LoaderOptions;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;

public final class FakePlayerStorage {
    private static final JSONComponentSerializer JSON = JSONComponentSerializer.json();

    private final FakePlayer plugin;
    private final Yaml yaml;
    private final File stateFile;
    private final File percentagesFile;
    private final File mapFile;
    private final File deathPercentagesFile;
    private final File deathMapFile;
    private final File potentialFakePlayersFile;
    private final File frequenciesFile;

    public FakePlayerStorage(FakePlayer plugin, File dataFolder) {
        this.plugin = plugin;
        LoaderOptions loaderOptions = new LoaderOptions();
        setCodePointLimitIfAvailable(loaderOptions);
        yaml = new Yaml(loaderOptions);
        createDirectory(dataFolder);
        stateFile = file(dataFolder, "state.yml");
        percentagesFile = file(dataFolder, "percentages.yml");
        mapFile = file(dataFolder, "map.yml");
        deathPercentagesFile = file(dataFolder, "deathPercentages.yml");
        deathMapFile = file(dataFolder, "deathMap.yml");
        potentialFakePlayersFile = file(dataFolder, "potentialFakePlayers.yml");
        frequenciesFile = file(dataFolder, "frequencies.yml");
    }

    public StoredFakePlayerState load() {
        StoredFakePlayerState versionedState = StoredFakePlayerState.fromYaml(loadFromFile(stateFile));
        if (versionedState != null && (versionedState.hasRuntimeData() || !legacyHasRuntimeData())) {
            return versionedState;
        }
        return loadLegacyState();
    }

    public void save(StoredFakePlayerState state) {
        writeToFile(stateFile, yaml.dump(state.toYaml()));
        saveLegacyMirrors(state);
    }

    private StoredFakePlayerState loadLegacyState() {
        WeightedMessageSet achievementMessages = WeightedMessageSet.fromSerializedCounts(loadIntegerMap(loadFromFile(mapFile)));
        WeightedMessageSet deathMessages = WeightedMessageSet.fromSerializedCounts(loadIntegerMap(loadFromFile(deathMapFile)));
        Map<String, FakePlayerProfile> potentialFakePlayers = loadLegacyPotentialFakePlayers(loadFromFile(potentialFakePlayersFile));
        FrequencyData frequencyData = loadLegacyFrequencyData();
        return new StoredFakePlayerState(
                achievementMessages,
                deathMessages,
                potentialFakePlayers,
                frequencyData,
                new ConcurrentHashMap<String, FakePlayerProgress>()
        );
    }

    private boolean legacyHasRuntimeData() {
        return hasMeaningfulContent(mapFile)
                || hasMeaningfulContent(deathMapFile)
                || hasMeaningfulContent(potentialFakePlayersFile)
                || hasMeaningfulContent(frequenciesFile);
    }

    private boolean hasMeaningfulContent(File file) {
        return file.exists() && file.length() > 3;
    }

    private void setCodePointLimitIfAvailable(LoaderOptions loaderOptions) {
        try {
            loaderOptions.getClass()
                    .getMethod("setCodePointLimit", int.class)
                    .invoke(loaderOptions, Integer.MAX_VALUE);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private void saveLegacyMirrors(StoredFakePlayerState state) {
        writeToFile(percentagesFile, yaml.dump(dumpComponents(state.achievementMessages().percentageBuckets())));
        writeToFile(mapFile, yaml.dump(state.achievementMessages().serializedCounts()));
        writeToFile(deathPercentagesFile, yaml.dump(dumpComponents(state.deathMessages().percentageBuckets())));
        writeToFile(deathMapFile, yaml.dump(state.deathMessages().serializedCounts()));
        writeToFile(potentialFakePlayersFile, yaml.dump(dumpLegacyPotentialFakePlayers(state.potentialFakePlayers())));
        writeToFile(frequenciesFile, state.frequencyData().toLegacyString());
    }

    private FrequencyData loadLegacyFrequencyData() {
        String frequenciesFileString = readFromFile(frequenciesFile);
        if (frequenciesFileString == null || frequenciesFileString.isBlank()) {
            return new FrequencyData();
        }
        return FrequencyData.fromString(frequenciesFileString);
    }

    private Map<String, Integer> loadIntegerMap(Object object) {
        Map<String, Integer> integerMap = new ConcurrentHashMap<>();
        if (!(object instanceof Map<?, ?> dumpableMap)) {
            return integerMap;
        }
        for (Map.Entry<?, ?> entry : dumpableMap.entrySet()) {
            if (!(entry.getKey() instanceof String key) || !(entry.getValue() instanceof Number value)) {
                continue;
            }
            integerMap.put(key, value.intValue());
        }
        return integerMap;
    }

    private Map<String, FakePlayerProfile> loadLegacyPotentialFakePlayers(Object object) {
        Map<String, FakePlayerProfile> loadedFakePlayers = new ConcurrentHashMap<>();
        if (!(object instanceof Map<?, ?> dumpableMap)) {
            return loadedFakePlayers;
        }
        for (Map.Entry<?, ?> entry : dumpableMap.entrySet()) {
            if (!(entry.getKey() instanceof String key) || !(entry.getValue() instanceof String value)) {
                continue;
            }
            FakePlayerProfile profile = FakePlayerProfileCodec.fromLegacyJson(value);
            if (profile != null) {
                loadedFakePlayers.put(key, profile);
            }
        }
        return loadedFakePlayers;
    }

    private List<String> dumpComponents(List<Component> components) {
        List<String> dumpableComponents = new ArrayList<>();
        for (Component component : components) {
            dumpableComponents.add(JSON.serialize(component));
        }
        return dumpableComponents;
    }

    private Map<String, String> dumpLegacyPotentialFakePlayers(Map<String, FakePlayerProfile> potentialFakePlayers) {
        Map<String, String> dumpableFakePlayers = new HashMap<>();
        for (Map.Entry<String, FakePlayerProfile> entry : potentialFakePlayers.entrySet()) {
            if (entry.getValue() != null) {
                dumpableFakePlayers.put(entry.getKey(), FakePlayerProfileCodec.toLegacyJson(entry.getValue()));
            }
        }
        return dumpableFakePlayers;
    }

    private Object loadFromFile(File file) {
        try {
            return yaml.load(Files.newBufferedReader(file.toPath()));
        } catch (IOException | RuntimeException e) {
            plugin.getLogger().log(Level.WARNING, "Could not load " + file.getName(), e);
        }
        return null;
    }

    private String readFromFile(File file) {
        try {
            return Files.readString(file.toPath());
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Could not read " + file.getName(), e);
        }
        return null;
    }

    private void writeToFile(File file, String string) {
        try {
            Files.writeString(file.toPath(), string == null ? "" : string, CREATE, TRUNCATE_EXISTING);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Could not write " + file.getName(), e);
        }
    }

    private File file(File dataFolder, String name) {
        File file = new File(dataFolder, name);
        try {
            if (!file.exists()) {
                file.createNewFile();
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Could not create " + name, e);
        }
        return file;
    }

    private void createDirectory(File dataFolder) {
        if (!dataFolder.exists() && !dataFolder.mkdirs()) {
            plugin.getLogger().warning("Could not create data folder " + dataFolder);
        }
    }
}
