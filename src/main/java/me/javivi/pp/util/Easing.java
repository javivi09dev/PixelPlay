package me.javivi.pp.util;

/**
 * Utilidades de mates para el easing (de hecho 🥸)
 * 
 */
public final class Easing {

    private Easing() {}

    public enum Curve {
        LINEAR,
        EASE_IN_SINE,
        EASE_OUT_SINE,
        EASE_IN_OUT_SINE,
        EASE_IN_CUBIC,
        EASE_OUT_CUBIC,
        EASE_IN_OUT_CUBIC,
        EASE_IN_QUINT,
        EASE_OUT_QUINT,
        EASE_IN_OUT_QUINT,
        EASE_IN_EXPO,
        EASE_OUT_EXPO,
        EASE_IN_OUT_EXPO
    }

    /**
     * t = 0 -> 0, t = 1 -> 1
     */
    public static float ease(float t, Curve curve) {
        float clamped = clamp01(t);
        return switch (curve) {
            case LINEAR -> clamped;
            case EASE_IN_SINE -> easeInSine(clamped);
            case EASE_OUT_SINE -> easeOutSine(clamped);
            case EASE_IN_OUT_SINE -> easeInOutSine(clamped);
            case EASE_IN_CUBIC -> easeInCubic(clamped);
            case EASE_OUT_CUBIC -> easeOutCubic(clamped);
            case EASE_IN_OUT_CUBIC -> easeInOutCubic(clamped);
            case EASE_IN_QUINT -> easeInQuint(clamped);
            case EASE_OUT_QUINT -> easeOutQuint(clamped);
            case EASE_IN_OUT_QUINT -> easeInOutQuint(clamped);
            case EASE_IN_EXPO -> easeInExpo(clamped);
            case EASE_OUT_EXPO -> easeOutExpo(clamped);
            case EASE_IN_OUT_EXPO -> easeInOutExpo(clamped);
        };
    }

    public static float clamp01(float v) {
        if (v < 0f) return 0f;
        if (v > 1f) return 1f;
        return v;
    }

    private static float easeInSine(float t) {
        return 1.0f - (float) Math.cos((t * Math.PI) / 2.0);
    }

    private static float easeOutSine(float t) {
        return (float) Math.sin((t * Math.PI) / 2.0);
    }

    private static float easeInOutSine(float t) {
        return -(float) ((Math.cos(Math.PI * t) - 1.0) / 2.0);
    }

    private static float easeInCubic(float t) {
        return t * t * t;
    }

    private static float easeOutCubic(float t) {
        float u = 1.0f - t;
        return 1.0f - u * u * u;
    }

    private static float easeInOutCubic(float t) {
        return t < 0.5f
                ? 4.0f * t * t * t
                : 1.0f - (float) Math.pow(-2.0f * t + 2.0f, 3.0) / 2.0f;
    }


    private static float easeInQuint(float t) {
        return t * t * t * t * t;
    }

    private static float easeOutQuint(float t) {
        float u = 1.0f - t;
        return 1.0f - u * u * u * u * u;
    }

    private static float easeInOutQuint(float t) {
        return t < 0.5f
                ? 16.0f * t * t * t * t * t
                : 1.0f - (float) Math.pow(-2.0f * t + 2.0f, 5.0) / 2.0f;
    }

    private static float easeInExpo(float t) {
        return t == 0f ? 0f : (float) Math.pow(2.0, 10.0 * (t - 1.0));
    }

    private static float easeOutExpo(float t) {
        return t == 1f ? 1f : 1f - (float) Math.pow(2.0, -10.0 * t);
    }

    private static float easeInOutExpo(float t) {
        if (t == 0f) return 0f;
        if (t == 1f) return 1f;
        return t < 0.5f
                ? (float) Math.pow(2.0, 20.0 * t - 10.0) / 2.0f
                : (2.0f - (float) Math.pow(2.0, -20.0 * t + 10.0)) / 2.0f;
    }

    public static float easeInParametric(float t, float exponent) {
        return (float) Math.pow(clamp01(t), Math.max(0.01, exponent));
    }

    public static float easeOutParametric(float t, float exponent) {
        float u = 1.0f - clamp01(t);
        return 1.0f - (float) Math.pow(u, Math.max(0.01, exponent));
    }

    public static float easeInOutParametric(float t, float exponent) {
        float clamped = clamp01(t);
        if (clamped < 0.5f) {
            return 0.5f * easeInParametric(clamped * 2.0f, exponent);
        }
        return 0.5f + 0.5f * easeOutParametric((clamped - 0.5f) * 2.0f, exponent);
    }
}


