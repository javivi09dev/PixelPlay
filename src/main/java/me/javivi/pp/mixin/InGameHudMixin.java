package me.javivi.pp.mixin;

import me.javivi.pp.client.PixelplayClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public abstract class InGameHudMixin {

    // Inserta después de renderizar HUD para que el ease tape todo (crosshair, hotbar, chat)
    @Inject(method = "render", at = @At(value = "TAIL"))
    private void pixelplay$renderEaseOnTop(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        var overlay = PixelplayClient.getOverlay();
        if (overlay == null) return;
        // Re-render solo la capa de ease por encima del HUD
        // Reutilizamos el método existente: el overlay ya dibuja ease al final
        overlay.render(context, tickCounter.getTickDelta(true));
    }
}


