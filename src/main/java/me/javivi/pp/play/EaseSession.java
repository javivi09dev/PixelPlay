package me.javivi.pp.play;

import me.javivi.pp.util.Easing;


public final class EaseSession {
    private final boolean white;
    private final long introMs;
    private final long totalMs;
    private final long outroMs;
    private final Easing.Curve curve;
    private final long startMs;

    public EaseSession(boolean white, double introSeconds, double totalSeconds, double outroSeconds, Easing.Curve curve) {
        this.white = white;
        this.introMs = (long) (Math.max(0, introSeconds) * 1000.0);
        this.totalMs = (long) (Math.max(0, totalSeconds) * 1000.0);
        this.outroMs = (long) (Math.max(0, outroSeconds) * 1000.0);
        this.curve = curve != null ? curve : Easing.Curve.EASE_IN_OUT_SINE;
        this.startMs = System.currentTimeMillis();
    }

    public boolean isWhite() { return white; }

    
    public float alpha() {
        long now = System.currentTimeMillis();
        long dt = now - startMs;
        if (dt <= introMs) {
            float t = introMs == 0 ? 1f : (float) dt / (float) introMs;
            return Easing.ease(t, curve);
        }
        dt -= introMs;
        if (dt <= totalMs) {
            return 1f;
        }
        dt -= totalMs;
        if (dt <= outroMs) {
            float t = outroMs == 0 ? 1f : (float) dt / (float) outroMs;
            return 1f - Easing.ease(t, curve);
        }
        return 0f;
    }

    public boolean finished() {
        return System.currentTimeMillis() - startMs > (introMs + totalMs + outroMs);
    }
}


