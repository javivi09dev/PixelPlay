package me.javivi.pp.client.gui;

import me.javivi.pp.block.entity.ScreenBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

public final class ScreenControlScreen extends Screen {
    private final BlockPos screenPos;
    private ScreenBlockEntity screenEntity;
    private VolumeSlider volumeSlider;
    private ButtonWidget pauseButton;
    private float currentVolume;
    private boolean currentPaused;

    public ScreenControlScreen(BlockPos screenPos) {
        super(Text.translatable("gui.pixelplay.screen_control"));
        this.screenPos = screenPos;
    }

    @Override
    protected void init() {
        super.init();
        
        var mc = MinecraftClient.getInstance();
        if (mc.world != null) {
            var be = mc.world.getBlockEntity(screenPos);
            if (be instanceof ScreenBlockEntity screen) {
                this.screenEntity = screen;
                this.currentVolume = screen.getVolume();
                this.currentPaused = screen.isPaused();
            } else {
                // Si no hay pantalla, cerrar
                mc.setScreen(null);
                return;
            }
        } else {
            mc.setScreen(null);
            return;
        }

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // Slider de volumen (200px de ancho)
        this.volumeSlider = new VolumeSlider(
            centerX - 100, centerY - 30, 200, 20,
            Text.translatable("gui.pixelplay.volume", Math.round(currentVolume * 100)),
            currentVolume
        );
        this.addDrawableChild(volumeSlider);

        // Botón de pausa/resume
        this.pauseButton = ButtonWidget.builder(
            currentPaused ? Text.translatable("gui.pixelplay.resume") : Text.translatable("gui.pixelplay.pause"),
            button -> {
                if (screenEntity != null) {
                    boolean newPaused = !screenEntity.isPaused();
                    // Ejecutar en el hilo del servidor si estamos en servidor
                    var server = MinecraftClient.getInstance().getServer();
                    if (server != null && !server.isSingleplayer()) {
                        server.execute(() -> {
                            screenEntity.setPaused(newPaused);
                        });
                    } else {
                        screenEntity.setPaused(newPaused);
                    }
                    currentPaused = newPaused;
                    updatePauseButton();
                }
            }
        ).dimensions(centerX - 50, centerY + 10, 100, 20).build();
        this.addDrawableChild(pauseButton);

        // Botón de cerrar
        this.addDrawableChild(ButtonWidget.builder(
            Text.translatable("gui.done"),
            button -> this.close()
        ).dimensions(centerX - 50, centerY + 40, 100, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        
        // Título
        context.drawCenteredTextWithShadow(this.textRenderer, 
            Text.translatable("gui.pixelplay.screen_control"), 
            this.width / 2, 20, 0xFFFFFF);

        // Actualizar estado si cambió
        if (screenEntity != null) {
            if (screenEntity.getVolume() != currentVolume) {
                currentVolume = screenEntity.getVolume();
                // Recrear el slider con el nuevo valor
                this.remove(volumeSlider);
                this.volumeSlider = new VolumeSlider(
                    this.width / 2 - 100, this.height / 2 - 30, 200, 20,
                    Text.translatable("gui.pixelplay.volume", Math.round(currentVolume * 100)),
                    currentVolume
                );
                this.addDrawableChild(volumeSlider);
            }
            if (screenEntity.isPaused() != currentPaused) {
                currentPaused = screenEntity.isPaused();
                updatePauseButton();
            }
        }

        super.render(context, mouseX, mouseY, delta);
    }

    private void updatePauseButton() {
        if (pauseButton != null) {
            pauseButton.setMessage(currentPaused ? 
                Text.translatable("gui.pixelplay.resume") : 
                Text.translatable("gui.pixelplay.pause"));
        }
    }

    private class VolumeSlider extends SliderWidget {
        public VolumeSlider(int x, int y, int width, int height, Text message, double value) {
            super(x, y, width, height, message, value);
        }

        @Override
        protected void updateMessage() {
            int volumePercent = Math.round((float) this.value * 100);
            this.setMessage(Text.translatable("gui.pixelplay.volume", volumePercent));
        }

        @Override
        protected void applyValue() {
            if (screenEntity != null) {
                // Ejecutar en el hilo del servidor si estamos en servidor
                var server = MinecraftClient.getInstance().getServer();
                if (server != null && !server.isSingleplayer()) {
                    server.execute(() -> {
                        screenEntity.setVolume((float) this.value);
                    });
                } else {
                    screenEntity.setVolume((float) this.value);
                }
            }
        }
    }
}

