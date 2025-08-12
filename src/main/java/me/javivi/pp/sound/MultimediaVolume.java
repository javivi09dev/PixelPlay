package me.javivi.pp.sound;

/**
 * Control global de volumen
 */
public final class MultimediaVolume {
    private static volatile float masterMultiplier = 1.0f;

    private MultimediaVolume() {}

    public static float getMasterMultiplier() {
        return masterMultiplier;
    }

    public static void setMasterMultiplier(float value) {
        masterMultiplier = Math.max(0f, Math.min(1f, value));
    }
}


