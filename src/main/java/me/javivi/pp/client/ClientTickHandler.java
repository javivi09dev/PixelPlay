package me.javivi.pp.client;

import me.javivi.pp.client.playback.PlaybackManager;
import me.javivi.pp.block.entity.ScreenBlockEntity;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;

public final class ClientTickHandler implements ClientTickEvents.EndTick {
    @Override
    public void onEndTick(MinecraftClient client) {
        PlaybackManager.tick(client);
        
        if (client.world != null && client.player != null) {
            for (int x = -2; x <= 2; x++) {
                for (int z = -2; z <= 2; z++) {
                    var chunk = client.world.getChunk(
                        (int)client.player.getX() / 16 + x,
                        (int)client.player.getZ() / 16 + z
                    );
                    if (chunk != null) {
                        for (var be : chunk.getBlockEntities().values()) {
                            if (be instanceof ScreenBlockEntity screen) {
                                screen.clientTick();
                            }
                        }
                    }
                }
            }
        }
    }
}


