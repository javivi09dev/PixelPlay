package me.javivi.pp;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import me.javivi.pp.command.PixelPlayCommand;
import me.javivi.pp.network.PixelPlayNetwork;
import me.javivi.pp.network.PixelPlayServerNetwork;
import me.javivi.pp.server.PlayerJoinHandler;

public class Pixelplay implements ModInitializer {

    @Override
    public void onInitialize() {
        PixelPlayNetwork.init();
        PixelPlayServerNetwork.init();
        me.javivi.pp.registry.ModBlocks.register();
        me.javivi.pp.registry.ModBlockEntities.register();
        CommandRegistrationCallback.EVENT.register((dispatcher, registry, environment) -> PixelPlayCommand.register(dispatcher, registry));
        
        me.javivi.pp.screen.ScreenPreset.loadPresets();
        
        me.javivi.pp.screen.ScreenInteractionHandler.init();
        
        // Initialize multimedia volume system
        me.javivi.pp.sound.MultimediaVolume.init();
        
        // Register player join handler to sync screen states
        PlayerJoinHandler.init();
    }
}
