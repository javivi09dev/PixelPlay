package me.javivi.pp.client.gui;

import me.javivi.pp.client.playback.PlaybackManager;
import me.javivi.pp.client.PixelplayClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;


public final class FreezeScreen extends Screen {
    public FreezeScreen() {
        super(Text.literal("PixelPlay Freeze"));
    }

    @Override
    public boolean shouldPause() {
        return false; 
    }

    @Override
    public void close() {
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // No dibujamos nada para evitar blur/dimming
        // Auto-cierre defensivo y conducción del fin/outro cuando hay FreezeScreen
        try {
            var session = PlaybackManager.getVideoSession();
            if (this.client != null) {
                boolean shouldClose = false;
                if (session == null) {
                    shouldClose = true;
                } else if (!session.freezeScreen()) {
                    shouldClose = true;
                } else {
                    var player = session.player();
                    if (player != null) {
                        long duration = player.getDuration();
                        long time = player.getTime();
                        // Arrancar/seguir outro aunque HUD no renderice
                        session.maybeStartOutro(duration, time, player.isEnded());
                        boolean endReached = player.isEnded() || (duration > 0 && time >= duration - 50);
                        // Si hay outro y ya terminó, cerramos aunque el player no marque ended
                        if (session.hasOutro() && session.isOutroFinished()) shouldClose = true;
                        // Si no hay outro, cerramos cuando alcance fin
                        if (!session.hasOutro() && endReached) shouldClose = true;
                    }
                }
                if (shouldClose) {
                    // Centralizar limpieza en el mismo lugar que el resto de flujos
                    PixelplayClient.setVideoSession(null);
                }
            }
        } catch (Throwable ignored) {}
    }
}


