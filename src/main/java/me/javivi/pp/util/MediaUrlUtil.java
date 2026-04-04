package me.javivi.pp.util;

import java.util.Locale;

public final class MediaUrlUtil {

    private MediaUrlUtil() {}

    /**
     * WaterMedia 3 aún no implementa {@code YoutubePlatform#getSources}; evita spamear MRL y muestra error claro.
     */
    public static boolean isYoutubeWatchUrl(String url) {
        if (url == null || url.isEmpty()) return false;
        String u = url.toLowerCase(Locale.ROOT);
        return u.contains("youtube.com/watch")
            || u.contains("youtu.be/")
            || u.contains("youtube.com/shorts")
            || u.contains("m.youtube.com/watch");
    }
}
