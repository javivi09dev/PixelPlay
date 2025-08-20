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
import me.javivi.pp.util.Easing;

public final class PixelPlayCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess) {
        dispatcher.register(CommandManager.literal("pixelplay")
            .requires(src -> src.hasPermissionLevel(2))
            .then(CommandManager.literal("startvideo")
                .then(CommandManager.argument("targets", EntityArgumentType.entities())
                    .then(CommandManager.argument("freeze_screen", BoolArgumentType.bool())
                        .then(CommandManager.argument("url", StringArgumentType.greedyString())
                            .suggests((ctx, b) -> { b.suggest("https://www.youtube.com/watch?v="); b.suggest("https://"); return b.buildFuture(); })
                            .executes(ctx -> {
                                boolean freeze = BoolArgumentType.getBool(ctx, "freeze_screen");
                                String url = cleanUrl(StringArgumentType.getString(ctx, "url"));
                                if (!isValidUrl(url)) { ctx.getSource().sendError(Text.translatable("message.pixelplay.invalid_url")); return 0; }
                                if (ctx.getSource().getServer().isSingleplayer()) {
                                    MinecraftClient.getInstance().execute(() -> {
                                        var color = false ? VideoSession.EaseColor.WHITE : VideoSession.EaseColor.BLACK;
                                        var session = new VideoSession(MinecraftClient.getInstance(), url, freeze, color, 0, 0, Easing.Curve.EASE_IN_OUT_SINE);
                                        PixelplayClient.setVideoSession(session);
                                    });
                                } else {
                                    Collection<ServerPlayerEntity> targets = EntityArgumentType.getPlayers(ctx, "targets");
                                    StartVideoPayload payload = new StartVideoPayload(url, freeze, false, 0, 0);
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
            .then(CommandManager.literal("startvideowithease")
                .then(CommandManager.argument("targets", EntityArgumentType.entities())
                    .then(CommandManager.argument("easecolor", StringArgumentType.word())
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
            .then(CommandManager.literal("startease")
                .then(CommandManager.argument("targets", EntityArgumentType.entities())
                    .then(CommandManager.argument("easecolor", StringArgumentType.string())
                        .then(CommandManager.argument("introsecondsofease", DoubleArgumentType.doubleArg(0, 600))
                            .then(CommandManager.argument("totaleaseseconds", DoubleArgumentType.doubleArg(0, 600))
                                .then(CommandManager.argument("outrosecondsofease", DoubleArgumentType.doubleArg(0, 600))
                                    .executes(ctx -> {
                                        String color = StringArgumentType.getString(ctx, "easecolor");
                                        boolean white = color.equalsIgnoreCase("whiteease");
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
            .then(CommandManager.literal("screenurl")
                .then(CommandManager.argument("targets", EntityArgumentType.entities())
                    .then(CommandManager.argument("url", StringArgumentType.greedyString())
                        .then(CommandManager.argument("loop", BoolArgumentType.bool())
                            .executes(ctx -> {
                                String url = cleanUrl(StringArgumentType.getString(ctx, "url"));
                                boolean loop = BoolArgumentType.getBool(ctx, "loop");
                                if (!isValidUrl(url)) { ctx.getSource().sendError(Text.translatable("message.pixelplay.invalid_url")); return 0; }
                                if (ctx.getSource().getServer().isSingleplayer()) {
                                    MinecraftClient.getInstance().execute(() -> {
                                        var mc = MinecraftClient.getInstance();
                                        var hit = mc.crosshairTarget;
                                        if (hit instanceof net.minecraft.util.hit.BlockHitResult bhr) {
                                            var pos = bhr.getBlockPos();
                                            var be = mc.world.getBlockEntity(pos);
                                            if (be instanceof me.javivi.pp.block.entity.ScreenBlockEntity sbe) {
                                                sbe.setLoop(loop);
                                                sbe.setUrl(url);
                                            }
                                        }
                                    });
                                }
                                ctx.getSource().sendFeedback(() -> Text.translatable("message.pixelplay.screen_url_set"), false);
                                return 1;
                            })
                        )
                        .executes(ctx -> {
                            String raw = cleanUrl(StringArgumentType.getString(ctx, "url"));
                            boolean loop = false;
                            String url = raw;
                            int sp = raw.lastIndexOf(' ');
                            if (sp > 0) {
                                String tail = raw.substring(sp + 1).trim();
                                if ("true".equalsIgnoreCase(tail) || "false".equalsIgnoreCase(tail)) {
                                    loop = Boolean.parseBoolean(tail);
                                    url = raw.substring(sp + 1).equals(tail) ? raw.substring(0, sp).trim() : raw;
                                }
                            }
                            if (!isValidUrl(url)) { ctx.getSource().sendError(Text.translatable("message.pixelplay.invalid_url")); return 0; }
                            if (ctx.getSource().getServer().isSingleplayer()) {
                                boolean finalLoop = loop;
                                String finalUrl = url;
                                MinecraftClient.getInstance().execute(() -> {
                                    var mc = MinecraftClient.getInstance();
                                    var hit = mc.crosshairTarget;
                                    if (hit instanceof net.minecraft.util.hit.BlockHitResult bhr) {
                                        var pos = bhr.getBlockPos();
                                        var be = mc.world.getBlockEntity(pos);
                                        if (be instanceof me.javivi.pp.block.entity.ScreenBlockEntity sbe) {
                                            sbe.setLoop(finalLoop);
                                            sbe.setUrl(finalUrl);
                                        }
                                    }
                                });
                            }
                            ctx.getSource().sendFeedback(() -> Text.translatable("message.pixelplay.screen_url_set"), false);
                            return 1;
                        })
                    )
                )
            )
            .then(CommandManager.literal("stopscreen")
                .then(CommandManager.argument("targets", EntityArgumentType.entities())
                    .executes(ctx -> {
                        if (ctx.getSource().getServer().isSingleplayer()) {
                            MinecraftClient.getInstance().execute(() -> {
                                var mc = MinecraftClient.getInstance();
                                var hit = mc.crosshairTarget;
                                if (hit instanceof net.minecraft.util.hit.BlockHitResult bhr) {
                                    var pos = bhr.getBlockPos();
                                    var be = mc.world.getBlockEntity(pos);
                                    if (be instanceof me.javivi.pp.block.entity.ScreenBlockEntity sbe) {
                                        sbe.stopPlayer();
                                        sbe.clearUrl();
                                    }
                                }
                            });
                        }
                        ctx.getSource().sendFeedback(() -> Text.translatable("message.pixelplay.screen_stopped"), false);
                        return 1;
                    })
                )
            )
            .then(CommandManager.literal("screensetup")
                .then(CommandManager.argument("targets", EntityArgumentType.entities())
                    .then(CommandManager.literal("clear")
                        .executes(ctx -> {
                            if (ctx.getSource().getServer().isSingleplayer()) {
                                MinecraftClient.getInstance().execute(() -> {
                                    var mc = MinecraftClient.getInstance();
                                    var hit = mc.crosshairTarget;
                                    if (hit instanceof net.minecraft.util.hit.BlockHitResult bhr) {
                                        var pos = bhr.getBlockPos();
                                        var be = mc.world.getBlockEntity(pos);
                                        if (be instanceof me.javivi.pp.block.entity.ScreenBlockEntity sbe) {
                                            sbe.clearExplicitRegion();
                                        }
                                    }
                                });
                            }
                            ctx.getSource().sendFeedback(() -> Text.translatable("message.pixelplay.screen_region_cleared"), false);
                            return 1;
                        })
                    )
                    .then(CommandManager.literal("select")
                        .then(CommandManager.argument("second", BoolArgumentType.bool())
                            .executes(ctx -> {
                                boolean second = BoolArgumentType.getBool(ctx, "second");
                                if (ctx.getSource().getServer().isSingleplayer()) {
                                    MinecraftClient.getInstance().execute(() -> {
                                        var mc = MinecraftClient.getInstance();
                                        var hit = mc.crosshairTarget;
                                        if (hit instanceof net.minecraft.util.hit.BlockHitResult bhr) {
                                            var pos = bhr.getBlockPos();
                                            var be = mc.world.getBlockEntity(pos);
                                            if (be instanceof me.javivi.pp.block.entity.ScreenBlockEntity sbe) {
                                                // Guardamos en el propio BE usando NBT transitivo: primera y segunda esquina
                                                var min = second ? sbe.regionMin() : pos;
                                                var max = second ? pos : sbe.regionMax();
                                                // Normalizar
                                                int minX = Math.min(min.getX(), max.getX());
                                                int minY = Math.min(min.getY(), max.getY());
                                                int minZ = Math.min(min.getZ(), max.getZ());
                                                int maxX = Math.max(min.getX(), max.getX());
                                                int maxY = Math.max(min.getY(), max.getY());
                                                int maxZ = Math.max(min.getZ(), max.getZ());
                                                sbe.setExplicitRegion(new net.minecraft.util.math.BlockPos(minX, minY, minZ), new net.minecraft.util.math.BlockPos(maxX, maxY, maxZ));
                                            }
                                        }
                                    });
                                }
                                ctx.getSource().sendFeedback(() -> Text.translatable(second ? "message.pixelplay.screen_corner_top" : "message.pixelplay.screen_corner_bottom"), false);
                        return 1;
                    })
                        )
                    )
                )
            )
        );
    }

    private static String cleanUrl(String url) { return url.trim(); }
    private static boolean isValidUrl(String url) { try { new URI(url); return url.startsWith("http"); } catch (Exception e) { return false; } }
    private static double clamp(double v, double min, double max) { return Math.max(min, Math.min(max, v)); }
}


