package me.javivi.pp.util;

public final class TimeUtil {
    private TimeUtil() {}

    /**
     * Convierte segundos a milisegundos
     */
    public static long secondsToMillis(double seconds) {
        if (Double.isNaN(seconds) || Double.isInfinite(seconds)) return 0L;
        if (seconds <= 0) return 0L;
        double ms = seconds * 1000.0;
        if (ms > Long.MAX_VALUE) return Long.MAX_VALUE;
        return (long) ms;
    }
}


