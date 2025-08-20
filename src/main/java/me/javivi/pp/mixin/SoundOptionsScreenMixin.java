package me.javivi.pp.mixin;

import me.javivi.pp.sound.MultimediaVolume;
import net.minecraft.client.gui.screen.option.SoundOptionsScreen;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SoundOptionsScreen.class)
public abstract class SoundOptionsScreenMixin {

    @Inject(
        method = "getVolumeOptions()[Lnet/minecraft/client/option/SimpleOption;",
        at = @At("RETURN"),
        cancellable = true
    )
    private void pixelplay$injectMultimediaSlider(CallbackInfoReturnable<SimpleOption<?>[]> cir) {
        SimpleOption<?>[] orig = cir.getReturnValue();
        SimpleOption<Double> multimediaOption = new SimpleOption<>(
                "options.pixelplay.multimedia_volume",
                SimpleOption.emptyTooltip(),
                (ignoredName, value) -> Text.translatable("options.pixelplay.multimedia_volume.value", Math.round(value * 100.0)),
                SimpleOption.DoubleSliderCallbacks.INSTANCE,
                1.0,
                value -> MultimediaVolume.setMasterMultiplier(value.floatValue())
        );
        SimpleOption<?>[] merged = new SimpleOption[orig.length + 1];
        System.arraycopy(orig, 0, merged, 0, orig.length);
        merged[orig.length] = multimediaOption; 
        cir.setReturnValue(merged);
    }
}


