package me.javivi.pp.sound;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Control global de volumen con persistencia de configuración
 */
public final class MultimediaVolume {
    private static volatile float masterMultiplier = 1.0f;
    private static final File CONFIG_FILE = new File("config/pixelplay_multimedia_volume.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private MultimediaVolume() {}

    public static void init() {
        loadVolume();
    }

    public static float getMasterMultiplier() {
        return masterMultiplier;
    }

    public static void setMasterMultiplier(float value) {
        masterMultiplier = Math.max(0f, Math.min(1f, value));
        saveVolume();
    }

    private static void loadVolume() {
        if (!CONFIG_FILE.exists()) return;
        
        try (FileReader reader = new FileReader(CONFIG_FILE)) {
            VolumeConfig config = GSON.fromJson(reader, VolumeConfig.class);
            if (config != null && config.volume >= 0f && config.volume <= 1f) {
                masterMultiplier = config.volume;
            }
        } catch (IOException e) {
            System.err.println("Error loading multimedia volume config: " + e.getMessage());
        }
    }

    private static void saveVolume() {
        try {
            CONFIG_FILE.getParentFile().mkdirs();
            try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
                VolumeConfig config = new VolumeConfig(masterMultiplier);
                GSON.toJson(config, writer);
            }
        } catch (IOException e) {
            System.err.println("Error saving multimedia volume config: " + e.getMessage());
        }
    }

    private static class VolumeConfig {
        public final float volume;
        
        public VolumeConfig(float volume) {
            this.volume = volume;
        }
    }
}


