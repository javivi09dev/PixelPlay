package me.javivi.pp.network;

import me.javivi.pp.network.payload.*;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;

public final class PixelPlayServerNetwork {
    private PixelPlayServerNetwork() {}

    public static void init() {
        // Handle StartVideoC2SPayload - forward to target player
        ServerPlayNetworking.registerGlobalReceiver(StartVideoC2SPayload.ID, (payload, context) -> {
            if (payload instanceof StartVideoC2SPayload p) {
                context.server().execute(() -> {
                    ServerPlayerEntity targetPlayer = context.server().getPlayerManager().getPlayer(p.targetPlayer());
                    if (targetPlayer != null) {
                        StartVideoPayload forwardPayload = new StartVideoPayload(
                            p.url(), 
                            p.freeze(), 
                            p.white(), 
                            p.intro(), 
                            p.outro()
                        );
                        ServerPlayNetworking.send(targetPlayer, forwardPayload);
                    }
                });
            }
        });

        // Handle StopVideoC2SPayload - forward to target player
        ServerPlayNetworking.registerGlobalReceiver(StopVideoC2SPayload.ID, (payload, context) -> {
            if (payload instanceof StopVideoC2SPayload p) {
                context.server().execute(() -> {
                    ServerPlayerEntity targetPlayer = context.server().getPlayerManager().getPlayer(p.targetPlayer());
                    if (targetPlayer != null) {
                        StopVideoPayload forwardPayload = new StopVideoPayload();
                        ServerPlayNetworking.send(targetPlayer, forwardPayload);
                    }
                });
            }
        });

        // Handle StartAudioC2SPayload - forward to target player
        ServerPlayNetworking.registerGlobalReceiver(StartAudioC2SPayload.ID, (payload, context) -> {
            if (payload instanceof StartAudioC2SPayload p) {
                context.server().execute(() -> {
                    ServerPlayerEntity targetPlayer = context.server().getPlayerManager().getPlayer(p.targetPlayer());
                    if (targetPlayer != null) {
                        StartAudioPayload forwardPayload = new StartAudioPayload(
                            p.url(), 
                            p.intro(), 
                            p.outro()
                        );
                        ServerPlayNetworking.send(targetPlayer, forwardPayload);
                    }
                });
            }
        });

        // Handle StopAudioC2SPayload - forward to target player
        ServerPlayNetworking.registerGlobalReceiver(StopAudioC2SPayload.ID, (payload, context) -> {
            if (payload instanceof StopAudioC2SPayload p) {
                context.server().execute(() -> {
                    ServerPlayerEntity targetPlayer = context.server().getPlayerManager().getPlayer(p.targetPlayer());
                    if (targetPlayer != null) {
                        StopAudioPayload forwardPayload = new StopAudioPayload();
                        ServerPlayNetworking.send(targetPlayer, forwardPayload);
                    }
                });
            }
        });

        // Handle StartEaseC2SPayload - forward to target player
        ServerPlayNetworking.registerGlobalReceiver(StartEaseC2SPayload.ID, (payload, context) -> {
            if (payload instanceof StartEaseC2SPayload p) {
                context.server().execute(() -> {
                    ServerPlayerEntity targetPlayer = context.server().getPlayerManager().getPlayer(p.targetPlayer());
                    if (targetPlayer != null) {
                        StartEasePayload forwardPayload = new StartEasePayload(
                            p.white(), 
                            p.intro(), 
                            p.total(), 
                            p.outro()
                        );
                        ServerPlayNetworking.send(targetPlayer, forwardPayload);
                    }
                });
            }
        });

        // Handle StartImageC2SPayload - forward to target player
        ServerPlayNetworking.registerGlobalReceiver(StartImageC2SPayload.ID, (payload, context) -> {
            if (payload instanceof StartImageC2SPayload p) {
                context.server().execute(() -> {
                    ServerPlayerEntity targetPlayer = context.server().getPlayerManager().getPlayer(p.targetPlayer());
                    if (targetPlayer != null) {
                        StartImagePayload forwardPayload = new StartImagePayload(
                            p.url(), 
                            p.freeze(), 
                            p.white(), 
                            p.intro(), 
                            p.outro(), 
                            p.duration()
                        );
                        ServerPlayNetworking.send(targetPlayer, forwardPayload);
                    }
                });
            }
        });

        // Handle ScreenVolumeC2SPayload - update screen and sync to all clients
        ServerPlayNetworking.registerGlobalReceiver(ScreenVolumeC2SPayload.ID, (payload, context) -> {
            if (payload instanceof ScreenVolumeC2SPayload p) {
                context.server().execute(() -> {
                    var player = context.player();
                    if (player != null && player.getWorld() != null) {
                        var be = player.getWorld().getBlockEntity(p.pos());
                        if (be instanceof me.javivi.pp.block.entity.ScreenBlockEntity screen) {
                            screen.setVolume(p.volume());
                            // setVolume ya sincroniza a todos los clientes
                        }
                    }
                });
            }
        });

        // Handle ScreenPauseC2SPayload - update screen and sync to all clients
        ServerPlayNetworking.registerGlobalReceiver(ScreenPauseC2SPayload.ID, (payload, context) -> {
            if (payload instanceof ScreenPauseC2SPayload p) {
                context.server().execute(() -> {
                    var player = context.player();
                    if (player != null && player.getWorld() != null) {
                        var be = player.getWorld().getBlockEntity(p.pos());
                        if (be instanceof me.javivi.pp.block.entity.ScreenBlockEntity screen) {
                            screen.setPaused(p.paused());
                            // setPaused ya sincroniza a todos los clientes
                        }
                    }
                });
            }
        });
    }
}

