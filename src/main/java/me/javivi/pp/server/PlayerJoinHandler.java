package me.javivi.pp.server;

import me.javivi.pp.block.entity.ScreenBlockEntity;
import me.javivi.pp.network.payload.ScreenVideoPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public final class PlayerJoinHandler {
    public static void init() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.player;
            ServerWorld world = player.getServerWorld();
            
            // Send all screen states to the newly connected player
            server.execute(() -> {
                // Iterate through all block entities in loaded chunks near the player
                int playerChunkX = (int) player.getX() >> 4;
                int playerChunkZ = (int) player.getZ() >> 4;
                int range = 8; // 8 chunks = 128 blocks
                
                for (int cx = playerChunkX - range; cx <= playerChunkX + range; cx++) {
                    for (int cz = playerChunkZ - range; cz <= playerChunkZ + range; cz++) {
                        var chunk = world.getChunk(cx, cz);
                        if (chunk != null && !chunk.isEmpty()) {
                            for (var be : chunk.getBlockEntities().values()) {
                                if (be instanceof ScreenBlockEntity screen && screen.getVideoUrl() != null) {
                                    BlockPos pos = screen.getPos();
                                    double dist = player.getBlockPos().getSquaredDistance(pos);
                                    if (dist <= 128 * 128) { // Within 128 blocks
                                        ScreenVideoPayload payload = new ScreenVideoPayload(
                                            pos,
                                            screen.getVideoUrl(),
                                            screen.isLooping(),
                                            screen.getScreenMin(),
                                            screen.getScreenMax()
                                        );
                                        ServerPlayNetworking.send(player, payload);
                                    }
                                }
                            }
                        }
                    }
                }
            });
        });
    }
}

