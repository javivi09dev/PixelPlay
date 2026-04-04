package me.javivi.pp.client;

import me.javivi.pp.client.playback.PlaybackManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;

public final class ClientTickHandler implements ClientTickEvents.EndTick {
    @Override
    public void onEndTick(MinecraftClient client) {
        PlaybackManager.tick(client);
    }
}
