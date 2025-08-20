package me.javivi.pp.screen;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.util.math.BlockPos;
    
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class ScreenPreset {
    private final String id;
    private final BlockPos min;
    private final BlockPos max;
    
    public ScreenPreset(String id, BlockPos min, BlockPos max) {
        this.id = id;
        this.min = min;
        this.max = max;
    }
    
    public String getId() { return id; }
    public BlockPos getMin() { return min; }
    public BlockPos getMax() { return max; }
    
    private static final Map<String, ScreenPreset> presets = new HashMap<>();
    private static final File PRESETS_FILE = new File("config/pixelplay_screen_presets.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    public static void loadPresets() {
        if (!PRESETS_FILE.exists()) return;
        
        try (FileReader reader = new FileReader(PRESETS_FILE)) {
            Type type = new TypeToken<Map<String, ScreenPreset>>(){}.getType();
            Map<String, ScreenPreset> loaded = GSON.fromJson(reader, type);
            if (loaded != null) {
                presets.clear();
                presets.putAll(loaded);
            }
        } catch (IOException e) {
            System.err.println("Error loading screen presets: " + e.getMessage());
        }
    }
    
    public static void savePresets() {
        try {
            PRESETS_FILE.getParentFile().mkdirs();
            try (FileWriter writer = new FileWriter(PRESETS_FILE)) {
                GSON.toJson(presets, writer);
            }
        } catch (IOException e) {
            System.err.println("Error saving screen presets: " + e.getMessage());
        }
    }
    
    public static void addPreset(String id, BlockPos min, BlockPos max) {
        presets.put(id, new ScreenPreset(id, min, max));
        savePresets();
    }
    
    public static ScreenPreset getPreset(String id) {
        return presets.get(id);
    }
    
    public static void removePreset(String id) {
        presets.remove(id);
        savePresets();
    }
    
    public static Map<String, ScreenPreset> getAllPresets() {
        return new HashMap<>(presets);
    }
    
    public static boolean hasPreset(String id) {
        return presets.containsKey(id);
    }
}
