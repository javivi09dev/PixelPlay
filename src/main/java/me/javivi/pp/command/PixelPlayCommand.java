package me.javivi.pp.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import me.javivi.pp.network.payload.StartEasePayload;
import me.javivi.pp.network.payload.StartAudioPayload;
import me.javivi.pp.network.payload.StartVideoPayload;
import me.javivi.pp.network.payload.StopVideoPayload;
import me.javivi.pp.network.payload.StopAudioPayload;
import me.javivi.pp.network.payload.StartImagePayload;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import java.net.URI;
import java.util.Collection;
import net.minecraft.client.MinecraftClient;
import me.javivi.pp.client.PixelplayClient;
import me.javivi.pp.play.VideoSession;
import me.javivi.pp.play.ImageSession;
import me.javivi.pp.util.Easing;

public final class PixelPlayCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess) {
        dispatcher.register(CommandManager.literal("pixelplay")
            .requires(src -> src.hasPermissionLevel(2))
            
            .then(CommandManager.literal("startvideo")
                .then(CommandManager.argument("targets", EntityArgumentType.entities())
                    .then(CommandManager.argument("easecolor", StringArgumentType.word())
                        .suggests((ctx, b) -> { 
                            b.suggest("white"); 
                            b.suggest("black"); 
                            b.suggest("whiteease"); 
                            b.suggest("blackease"); 
                            return b.buildFuture(); 
                        })
                        .then(CommandManager.argument("freeze_screen", BoolArgumentType.bool())
                            .then(CommandManager.argument("url", StringArgumentType.greedyString())
                                .suggests((ctx, b) -> { 
                                    b.suggest("https://www.youtube.com/watch?v="); 
                                    b.suggest("https://"); 
                                    return b.buildFuture(); 
                                })
                                .executes(ctx -> {
                                    String color = StringArgumentType.getString(ctx, "easecolor");
                                    boolean freeze = BoolArgumentType.getBool(ctx, "freeze_screen");
                                    String url = cleanUrl(StringArgumentType.getString(ctx, "url"));
                                    if (!isValidUrl(url)) { ctx.getSource().sendError(Text.translatable("message.pixelplay.invalid_url")); return 0; }
                                    
                                    boolean white = color.equalsIgnoreCase("white") || color.equalsIgnoreCase("whiteease");
                                    if (ctx.getSource().getServer().isSingleplayer()) {
                                        MinecraftClient.getInstance().execute(() -> {
                                            var easeColor = white ? VideoSession.EaseColor.WHITE : VideoSession.EaseColor.BLACK;
                                            var session = new VideoSession(MinecraftClient.getInstance(), url, freeze, easeColor, 0, 0, Easing.Curve.EASE_IN_OUT_SINE);
                                            PixelplayClient.setVideoSession(session);
                                        });
                                    } else {
                                        Collection<ServerPlayerEntity> targets = EntityArgumentType.getPlayers(ctx, "targets");
                                        StartVideoPayload payload = new StartVideoPayload(url, freeze, white, 0, 0);
                                        for (ServerPlayerEntity p : targets) {
                                            ServerPlayNetworking.send(p, payload);
                                        }
                                    }
                                    ctx.getSource().sendFeedback(() -> Text.translatable("message.pixelplay.video_started"), false);
                                    return 1;
                                })
                            )
                        )
                    )
                )
            )
            
            .then(CommandManager.literal("startvideowithease")
                .then(CommandManager.argument("targets", EntityArgumentType.entities())
                    .then(CommandManager.argument("easecolor", StringArgumentType.word())
                        .suggests((ctx, b) -> { 
                            b.suggest("white"); 
                            b.suggest("black"); 
                            b.suggest("whiteease"); 
                            b.suggest("blackease"); 
                            return b.buildFuture(); 
                        })
                        .then(CommandManager.argument("introsecondsofease", DoubleArgumentType.doubleArg(0, 600))
                            .then(CommandManager.argument("outrosecondsofease", DoubleArgumentType.doubleArg(0, 600))
                                .then(CommandManager.argument("freeze_screen", BoolArgumentType.bool())
                                    .then(CommandManager.argument("url", StringArgumentType.greedyString())
                                        .suggests((ctx, b) -> { b.suggest("https://www.youtube.com/watch?v="); return b.buildFuture(); })
                                        .executes(ctx -> {
                                            String color = StringArgumentType.getString(ctx, "easecolor");
                                            double intro = clamp(DoubleArgumentType.getDouble(ctx, "introsecondsofease"), 0, 600);
                                            double outro = clamp(DoubleArgumentType.getDouble(ctx, "outrosecondsofease"), 0, 600);
                                            boolean freeze = BoolArgumentType.getBool(ctx, "freeze_screen");
                                            String url = cleanUrl(StringArgumentType.getString(ctx, "url"));
                                            if (!isValidUrl(url)) { ctx.getSource().sendError(Text.translatable("message.pixelplay.invalid_url")); return 0; }
                                            boolean white = color.equalsIgnoreCase("white") || color.equalsIgnoreCase("whiteease");
                                            if (ctx.getSource().getServer().isSingleplayer()) {
                                                MinecraftClient.getInstance().execute(() -> {
                                                    var easeColor = white ? VideoSession.EaseColor.WHITE : VideoSession.EaseColor.BLACK;
                                                    var session = new VideoSession(MinecraftClient.getInstance(), url, freeze, easeColor, intro, outro, Easing.Curve.EASE_IN_OUT_SINE);
                                                    PixelplayClient.setVideoSession(session);
                                                });
                                            } else {
                                                Collection<ServerPlayerEntity> targets = EntityArgumentType.getPlayers(ctx, "targets");
                                                StartVideoPayload payload = new StartVideoPayload(url, freeze, white, intro, outro);
                                                for (ServerPlayerEntity p : targets) {
                                                    ServerPlayNetworking.send(p, payload);
                                                }
                                            }
                                            ctx.getSource().sendFeedback(() -> Text.translatable("message.pixelplay.video_with_ease"), false);
                                            return 1;
                                        })
                                    )
                                )
                            )
                        )
                    )
                )
            )
            
            .then(CommandManager.literal("stopvideo")
                .then(CommandManager.argument("targets", EntityArgumentType.entities())
                    .executes(ctx -> {
                        var payload = new StopVideoPayload();
                        Collection<ServerPlayerEntity> targets = EntityArgumentType.getPlayers(ctx, "targets");
                        for (ServerPlayerEntity p : targets) ServerPlayNetworking.send(p, payload);
                        ctx.getSource().sendFeedback(() -> Text.translatable("message.pixelplay.video_stopped"), false);
                        return 1;
                    })
                )
            )
            
            .then(CommandManager.literal("startaudio")
                .then(CommandManager.argument("targets", EntityArgumentType.entities())
                    .then(CommandManager.argument("url", StringArgumentType.greedyString())
                        .suggests((ctx, b) -> { b.suggest("https://"); return b.buildFuture(); })
                        .executes(ctx -> {
                            String url = cleanUrl(StringArgumentType.getString(ctx, "url"));
                            if (!isValidUrl(url)) { ctx.getSource().sendError(Text.translatable("message.pixelplay.invalid_url")); return 0; }
                            Collection<ServerPlayerEntity> targets = EntityArgumentType.getPlayers(ctx, "targets");
                            var payload = new StartAudioPayload(url, 0, 0);
                            for (ServerPlayerEntity p : targets) ServerPlayNetworking.send(p, payload);
                            ctx.getSource().sendFeedback(() -> Text.translatable("message.pixelplay.audio_started"), false);
                            return 1;
                        })
                    )
                )
            )
            
            .then(CommandManager.literal("startaudiowithsoundease")
                .then(CommandManager.argument("targets", EntityArgumentType.entities())
                    .then(CommandManager.argument("introsecondsofease", DoubleArgumentType.doubleArg(0, 600))
                        .then(CommandManager.argument("outrosecondsofease", DoubleArgumentType.doubleArg(0, 600))
                            .then(CommandManager.argument("url", StringArgumentType.greedyString())
                                .suggests((ctx, b) -> { b.suggest("https://"); return b.buildFuture(); })
                                .executes(ctx -> {
                                    double intro = clamp(DoubleArgumentType.getDouble(ctx, "introsecondsofease"), 0, 600);
                                    double outro = clamp(DoubleArgumentType.getDouble(ctx, "outrosecondsofease"), 0, 600);
                                    String url = cleanUrl(StringArgumentType.getString(ctx, "url"));
                                    if (!isValidUrl(url)) { ctx.getSource().sendError(Text.translatable("message.pixelplay.invalid_url")); return 0; }
                                    Collection<ServerPlayerEntity> targets = EntityArgumentType.getPlayers(ctx, "targets");
                                    var payload = new StartAudioPayload(url, intro, outro);
                                    for (ServerPlayerEntity p : targets) ServerPlayNetworking.send(p, payload);
                                    ctx.getSource().sendFeedback(() -> Text.translatable("message.pixelplay.audio_with_ease"), false);
                                    return 1;
                                })
                            )
                        )
                    )
                )
            )
            
            .then(CommandManager.literal("stopaudio")
                .then(CommandManager.argument("targets", EntityArgumentType.entities())
                    .executes(ctx -> {
                        var payload = new StopAudioPayload();
                        Collection<ServerPlayerEntity> targets = EntityArgumentType.getPlayers(ctx, "targets");
                        for (ServerPlayerEntity p : targets) ServerPlayNetworking.send(p, payload);
                        ctx.getSource().sendFeedback(() -> Text.translatable("message.pixelplay.audio_stopped"), false);
                        return 1;
                    })
                )
            )
            
            .then(CommandManager.literal("startease")
                .then(CommandManager.argument("targets", EntityArgumentType.entities())
                    .then(CommandManager.argument("easecolor", StringArgumentType.word())
                        .suggests((ctx, b) -> { 
                            b.suggest("white"); 
                            b.suggest("black"); 
                            b.suggest("whiteease"); 
                            b.suggest("blackease"); 
                            return b.buildFuture(); 
                        })
                        .then(CommandManager.argument("introsecondsofease", DoubleArgumentType.doubleArg(0, 600))
                            .then(CommandManager.argument("totaleaseseconds", DoubleArgumentType.doubleArg(0, 600))
                                .then(CommandManager.argument("outrosecondsofease", DoubleArgumentType.doubleArg(0, 600))
                                    .executes(ctx -> {
                                        String color = StringArgumentType.getString(ctx, "easecolor");
                                        boolean white = color.equalsIgnoreCase("white") || color.equalsIgnoreCase("whiteease");
                                        double intro = clamp(DoubleArgumentType.getDouble(ctx, "introsecondsofease"), 0, 600);
                                        double total = clamp(DoubleArgumentType.getDouble(ctx, "totaleaseseconds"), 0, 600);
                                        double outro = clamp(DoubleArgumentType.getDouble(ctx, "outrosecondsofease"), 0, 600);
                                        var payload = new StartEasePayload(white, intro, total, outro);
                                        Collection<ServerPlayerEntity> targets = EntityArgumentType.getPlayers(ctx, "targets");
                                        for (ServerPlayerEntity p : targets) ServerPlayNetworking.send(p, payload);
                                        ctx.getSource().sendFeedback(() -> Text.translatable("message.pixelplay.ease_applied"), false);
                                        return 1;
                                    })
                                )
                            )
                        )
                    )
                )
            )
            
            .then(CommandManager.literal("startimage")
                .then(CommandManager.argument("targets", EntityArgumentType.entities())
                    .then(CommandManager.argument("freeze_screen", BoolArgumentType.bool())
                        .then(CommandManager.argument("duration", DoubleArgumentType.doubleArg(0, 3600))
                            .then(CommandManager.argument("url", StringArgumentType.greedyString())
                                .suggests((ctx, b) -> { 
                                    b.suggest("https://"); 
                                    return b.buildFuture(); 
                                })
                                .executes(ctx -> {
                                    boolean freeze = BoolArgumentType.getBool(ctx, "freeze_screen");
                                    double duration = clamp(DoubleArgumentType.getDouble(ctx, "duration"), 0, 3600);
                                    String url = cleanUrl(StringArgumentType.getString(ctx, "url"));
                                    if (!isValidUrl(url)) { ctx.getSource().sendError(Text.translatable("message.pixelplay.invalid_url")); return 0; }
                                    
                                    if (ctx.getSource().getServer().isSingleplayer()) {
                                        MinecraftClient.getInstance().execute(() -> {
                                            var easeColor = ImageSession.EaseColor.BLACK;
                                            var session = new ImageSession(MinecraftClient.getInstance(), url, freeze, easeColor, 0, 0, duration, Easing.Curve.EASE_IN_OUT_SINE);
                                            PixelplayClient.setImageSession(session);
                                        });
                                    } else {
                                        Collection<ServerPlayerEntity> targets = EntityArgumentType.getPlayers(ctx, "targets");
                                        StartImagePayload payload = new StartImagePayload(url, freeze, false, 0, 0, duration);
                                        for (ServerPlayerEntity p : targets) {
                                            ServerPlayNetworking.send(p, payload);
                                        }
                                    }
                                    ctx.getSource().sendFeedback(() -> Text.translatable("message.pixelplay.image_started"), false);
                                    return 1;
                                })
                            )
                        )
                    )
                )
            )
            
            .then(CommandManager.literal("startimagewithease")
                .then(CommandManager.argument("targets", EntityArgumentType.entities())
                    .then(CommandManager.argument("easecolor", StringArgumentType.word())
                        .suggests((ctx, b) -> { 
                            b.suggest("white"); 
                            b.suggest("black"); 
                            b.suggest("whiteease"); 
                            b.suggest("blackease"); 
                            return b.buildFuture(); 
                        })
                        .then(CommandManager.argument("introsecondsofease", DoubleArgumentType.doubleArg(0, 600))
                            .then(CommandManager.argument("outrosecondsofease", DoubleArgumentType.doubleArg(0, 600))
                                .then(CommandManager.argument("freeze_screen", BoolArgumentType.bool())
                                    .then(CommandManager.argument("duration", DoubleArgumentType.doubleArg(0, 3600))
                                        .then(CommandManager.argument("url", StringArgumentType.greedyString())
                                            .suggests((ctx, b) -> { b.suggest("https://"); return b.buildFuture(); })
                                            .executes(ctx -> {
                                                String color = StringArgumentType.getString(ctx, "easecolor");
                                                double intro = clamp(DoubleArgumentType.getDouble(ctx, "introsecondsofease"), 0, 600);
                                                double outro = clamp(DoubleArgumentType.getDouble(ctx, "outrosecondsofease"), 0, 600);
                                                boolean freeze = BoolArgumentType.getBool(ctx, "freeze_screen");
                                                double duration = clamp(DoubleArgumentType.getDouble(ctx, "duration"), 0, 3600);
                                                String url = cleanUrl(StringArgumentType.getString(ctx, "url"));
                                                if (!isValidUrl(url)) { ctx.getSource().sendError(Text.translatable("message.pixelplay.invalid_url")); return 0; }
                                                boolean white = color.equalsIgnoreCase("white") || color.equalsIgnoreCase("whiteease");
                                                if (ctx.getSource().getServer().isSingleplayer()) {
                                                    MinecraftClient.getInstance().execute(() -> {
                                                        var easeColor = white ? ImageSession.EaseColor.WHITE : ImageSession.EaseColor.BLACK;
                                                        var session = new ImageSession(MinecraftClient.getInstance(), url, freeze, easeColor, intro, outro, duration, Easing.Curve.EASE_IN_OUT_SINE);
                                                        PixelplayClient.setImageSession(session);
                                                    });
                                                } else {
                                                    Collection<ServerPlayerEntity> targets = EntityArgumentType.getPlayers(ctx, "targets");
                                                    StartImagePayload payload = new StartImagePayload(url, freeze, white, intro, outro, duration);
                                                    for (ServerPlayerEntity p : targets) {
                                                        ServerPlayNetworking.send(p, payload);
                                                    }
                                                }
                                                ctx.getSource().sendFeedback(() -> Text.translatable("message.pixelplay.image_with_ease"), false);
                                                return 1;
                                            })
                                        )
                                    )
                                )
                            )
                        )
                    )
                )
            )
            
            .then(CommandManager.literal("screen")
                .then(CommandManager.literal("setup")
                    .executes(ctx -> {
                        var player = ctx.getSource().getPlayer();
                        if (player == null) {
                            ctx.getSource().sendError(Text.literal("§c[PixelPlay] Este comando solo puede ser usado por jugadores."));
                            return 0;
                        }
                        
                        me.javivi.pp.screen.ScreenSetupManager.startSetup(player.getUuid());
                        ctx.getSource().sendFeedback(() -> Text.translatable("message.pixelplay.screen_setup_started"), false);
                        return 1;
                    })
                )
                
                .then(CommandManager.literal("setup")
                    .then(CommandManager.literal("name")
                        .then(CommandManager.argument("preset_id", StringArgumentType.word())
                            .executes(ctx -> {
                                var player = ctx.getSource().getPlayer();
                                if (player == null) {
                                    ctx.getSource().sendError(Text.literal("§c[PixelPlay] Este comando solo puede ser usado por jugadores."));
                                    return 0;
                                }
                                
                                String presetId = StringArgumentType.getString(ctx, "preset_id");
                                me.javivi.pp.screen.ScreenSetupManager.completeSetup(player.getUuid(), presetId, ctx.getSource());
                                return 1;
                            })
                        )
                    )
                )
                
                .then(CommandManager.literal("play")
                    .then(CommandManager.argument("preset_id", StringArgumentType.word())
                        .then(CommandManager.argument("loop", BoolArgumentType.bool())
                            .then(CommandManager.argument("url", StringArgumentType.greedyString())
                                .executes(ctx -> {
                                    String presetId = StringArgumentType.getString(ctx, "preset_id");
                                    boolean loop = BoolArgumentType.getBool(ctx, "loop");
                                    String url = cleanUrl(StringArgumentType.getString(ctx, "url"));
                                    
                                    if (!me.javivi.pp.screen.ScreenPreset.hasPreset(presetId)) {
                                        ctx.getSource().sendError(Text.literal("§c[PixelPlay] Preset '" + presetId + "' no encontrado."));
                                        return 0;
                                    }
                                    
                                    if (!isValidUrl(url)) {
                                        ctx.getSource().sendError(Text.translatable("message.pixelplay.invalid_url"));
                                        return 0;
                                    }
                                    
                                    var preset = me.javivi.pp.screen.ScreenPreset.getPreset(presetId);
                                    if (ctx.getSource().getServer().isSingleplayer()) {
                                        MinecraftClient.getInstance().execute(() -> {
                                            var mc = MinecraftClient.getInstance();
                                            if (mc.world != null) {
                                                for (int x = preset.getMin().getX(); x <= preset.getMax().getX(); x++) {
                                                    for (int y = preset.getMin().getY(); y <= preset.getMax().getY(); y++) {
                                                        for (int z = preset.getMin().getZ(); z <= preset.getMax().getZ(); z++) {
                                                            var pos = new net.minecraft.util.math.BlockPos(x, y, z);
                                                            var be = mc.world.getBlockEntity(pos);
                                                            if (be instanceof me.javivi.pp.block.entity.ScreenBlockEntity screen) {
                                                                screen.setScreenArea(preset.getMin(), preset.getMax());
                                                                if (pos.equals(preset.getMin())) {
                                                                    screen.setVideo(url, loop);
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        });
                                    }
                                    
                                    ctx.getSource().sendFeedback(() -> Text.literal("§a[PixelPlay] §aVideo iniciado en preset '" + presetId + "'"), false);
                                    return 1;
                                })
                            )
                        )
                    )
                )
                
                .then(CommandManager.literal("stop")
                    .then(CommandManager.argument("preset_id", StringArgumentType.word())
                        .executes(ctx -> {
                            String presetId = StringArgumentType.getString(ctx, "preset_id");
                            
                            if (!me.javivi.pp.screen.ScreenPreset.hasPreset(presetId)) {
                                ctx.getSource().sendError(Text.literal("§c[PixelPlay] Preset '" + presetId + "' no encontrado."));
                                return 0;
                            }
                            
                            var preset = me.javivi.pp.screen.ScreenPreset.getPreset(presetId);
                            if (ctx.getSource().getServer().isSingleplayer()) {
                                MinecraftClient.getInstance().execute(() -> {
                                    var mc = MinecraftClient.getInstance();
                                    if (mc.world != null) {
                                        for (int x = preset.getMin().getX(); x <= preset.getMax().getX(); x++) {
                                            for (int y = preset.getMin().getY(); y <= preset.getMax().getY(); y++) {
                                                for (int z = preset.getMin().getZ(); z <= preset.getMax().getZ(); z++) {
                                                    var pos = new net.minecraft.util.math.BlockPos(x, y, z);
                                                    var be = mc.world.getBlockEntity(pos);
                                                    if (be instanceof me.javivi.pp.block.entity.ScreenBlockEntity screen) {
                                                        screen.stopVideo();
                                                    }
                                                }
                                            }
                                        }
                                    }
                                });
                            }
                            
                            ctx.getSource().sendFeedback(() -> Text.literal("§a[PixelPlay] §cVideo detenido en preset '" + presetId + "'"), false);
                            return 1;
                        })
                    )
                )
                
                .then(CommandManager.literal("list")
                    .executes(ctx -> {
                        var presets = me.javivi.pp.screen.ScreenPreset.getAllPresets();
                        if (presets.isEmpty()) {
                            ctx.getSource().sendFeedback(() -> Text.literal("§a[PixelPlay] §eNo hay presets configurados."), false);
                        } else {
                            ctx.getSource().sendFeedback(() -> Text.literal("§a[PixelPlay] §aPresets disponibles:"), false);
                            for (String id : presets.keySet()) {
                                var preset = presets.get(id);
                                ctx.getSource().sendFeedback(() -> Text.literal("§e- " + id + " §7(" + 
                                    preset.getMin().getX() + "," + preset.getMin().getY() + "," + preset.getMin().getZ() + ") a (" +
                                    preset.getMax().getX() + "," + preset.getMax().getY() + "," + preset.getMax().getZ() + ")"), false);
                            }
                        }
                        return 1;
                        })
                    )
                
                .then(CommandManager.literal("delete")
                    .then(CommandManager.argument("preset_id", StringArgumentType.word())
                        .executes(ctx -> {
                            String presetId = StringArgumentType.getString(ctx, "preset_id");
                            
                            if (!me.javivi.pp.screen.ScreenPreset.hasPreset(presetId)) {
                                ctx.getSource().sendError(Text.literal("§c[PixelPlay] Preset '" + presetId + "' no encontrado."));
                                return 0;
                            }
                            
                            me.javivi.pp.screen.ScreenPreset.removePreset(presetId);
                            ctx.getSource().sendFeedback(() -> Text.literal("§a[PixelPlay] §cPreset '" + presetId + "' eliminado."), false);
                            return 1;
                        })
                    )
                )
            )
        );
    }

    private static String cleanUrl(String url) { 
        return url.trim(); 
    }
    
    private static boolean isValidUrl(String url) { 
        if (url == null || url.trim().isEmpty()) return false;
        
        if (url.contains("youtube.com/watch") || url.contains("youtu.be/")) {
            return true;
        }
                
        if (url.startsWith("http://") || url.startsWith("https://")) {
            try {
                new URI(url);
                return true;
            } catch (Exception e) {
                return false;
            }
        }
        
        return false;
    }
    
    private static double clamp(double v, double min, double max) { 
        return Math.max(min, Math.min(max, v)); 
    }
}


