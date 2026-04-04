package me.javivi.pixelplayfolia;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public final class PixelPlayFoliaPlugin extends JavaPlugin implements TabExecutor {

    private static final String[] SUBS = {
        "startvideo", "startvideowithease", "stopvideo",
        "startaudio", "startaudiowithsoundease", "stopaudio",
        "startease",
        "startimage", "startimagewithease"
    };

    private static final String[] CHANNELS = {
        PixelPlayPayloads.CH_START_VIDEO,
        PixelPlayPayloads.CH_STOP_VIDEO,
        PixelPlayPayloads.CH_START_AUDIO,
        PixelPlayPayloads.CH_STOP_AUDIO,
        PixelPlayPayloads.CH_START_EASE,
        PixelPlayPayloads.CH_START_IMAGE
    };

    private static final String[] BOOL_SUGGEST = {"true", "false"};
    private static final String[] DURATION_HINTS = {"0", "1", "2", "3", "5", "10", "30", "60", "120"};

    @Override
    public void onEnable() {
        for (String ch : CHANNELS) {
            getServer().getMessenger().registerOutgoingPluginChannel(this, ch);
        }
        var cmd = getCommand("pixelplay");
        if (cmd != null) {
            cmd.setExecutor(this);
            cmd.setTabCompleter(this);
        } else {
            getLogger().severe("Command 'pixelplay' missing from plugin.yml");
        }
    }

    @Override
    public void onDisable() {
        for (String ch : CHANNELS) {
            getServer().getMessenger().unregisterOutgoingPluginChannel(this, ch);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("pixelplay.admin")) {
            sender.sendMessage("No permission.");
            return true;
        }
        if (args.length < 1) {
            sendUsage(sender);
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        String[] rest = Arrays.copyOfRange(args, 1, args.length);
        try {
            switch (sub) {
                case "startvideo" -> handleStartVideo(sender, rest);
                case "startvideowithease" -> handleStartVideoWithEase(sender, rest);
                case "stopvideo" -> handleStopVideo(sender, rest);
                case "startaudio" -> handleStartAudio(sender, rest, false);
                case "startaudiowithsoundease" -> handleStartAudio(sender, rest, true);
                case "stopaudio" -> handleStopAudio(sender, rest);
                case "startease" -> handleStartEase(sender, rest);
                case "startimage" -> handleStartImage(sender, rest, false);
                case "startimagewithease" -> handleStartImage(sender, rest, true);
                default -> sendUsage(sender);
            }
        } catch (NumberFormatException ex) {
            sender.sendMessage("Invalid number argument.");
        } catch (CommandException ex) {
            sender.sendMessage(ex.getMessage());
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("pixelplay.admin")) {
            return Collections.emptyList();
        }
        if (args.length == 1) {
            return filterPrefix(args[0], SUBS);
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        if (args.length == 2) {
            return onlineNames(args[1]);
        }
        return switch (sub) {
            case "startvideo" -> completeStartVideo(args);
            case "startvideowithease" -> completeStartVideoWithEase(args);
            case "stopvideo", "stopaudio" -> Collections.emptyList();
            case "startaudio" -> Collections.emptyList();
            case "startaudiowithsoundease" -> completeStartAudioWithEase(args);
            case "startease" -> completeStartEase(args);
            case "startimage" -> completeStartImage(args);
            case "startimagewithease" -> completeStartImageWithEase(args);
            default -> Collections.emptyList();
        };
    }

    private static List<String> filterPrefix(String prefix, String... candidates) {
        String p = prefix.toLowerCase(Locale.ROOT);
        return Arrays.stream(candidates)
            .filter(s -> s.toLowerCase(Locale.ROOT).startsWith(p))
            .collect(Collectors.toList());
    }

    private List<String> completeStartVideo(String[] args) {
        if (args.length == 3) {
            return colorTab(args[2]);
        }
        if (args.length == 4) {
            return filterPrefix(args[3], BOOL_SUGGEST);
        }
        return Collections.emptyList();
    }

    private List<String> completeStartVideoWithEase(String[] args) {
        if (args.length == 3) {
            return colorTab(args[2]);
        }
        if (args.length == 4 || args.length == 5) {
            return filterPrefix(args[args.length - 1], DURATION_HINTS);
        }
        if (args.length == 6) {
            return filterPrefix(args[5], BOOL_SUGGEST);
        }
        return Collections.emptyList();
    }

    private List<String> completeStartAudioWithEase(String[] args) {
        if (args.length == 3 || args.length == 4) {
            return filterPrefix(args[args.length - 1], DURATION_HINTS);
        }
        return Collections.emptyList();
    }

    private List<String> completeStartEase(String[] args) {
        if (args.length == 3) {
            return colorTab(args[2]);
        }
        if (args.length >= 4 && args.length <= 6) {
            return filterPrefix(args[args.length - 1], DURATION_HINTS);
        }
        return Collections.emptyList();
    }

    private List<String> completeStartImage(String[] args) {
        if (args.length == 3) {
            return filterPrefix(args[2], BOOL_SUGGEST);
        }
        if (args.length == 4) {
            return filterPrefix(args[3], DURATION_HINTS);
        }
        return Collections.emptyList();
    }

    private List<String> completeStartImageWithEase(String[] args) {
        if (args.length == 3) {
            return colorTab(args[2]);
        }
        if (args.length == 4 || args.length == 5) {
            return filterPrefix(args[args.length - 1], DURATION_HINTS);
        }
        if (args.length == 6) {
            return filterPrefix(args[5], BOOL_SUGGEST);
        }
        if (args.length == 7) {
            return filterPrefix(args[6], DURATION_HINTS);
        }
        return Collections.emptyList();
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage("/pixelplay startvideo <player|@a> <easecolor> <freeze:true|false> <url>");
        sender.sendMessage("/pixelplay startvideowithease <player|@a> <easecolor> <intro> <outro> <freeze> <url>");
        sender.sendMessage("/pixelplay stopvideo <player|@a>");
        sender.sendMessage("/pixelplay startaudio <player|@a> <url>");
        sender.sendMessage("/pixelplay startaudiowithsoundease <player|@a> <intro> <outro> <url>");
        sender.sendMessage("/pixelplay stopaudio <player|@a>");
        sender.sendMessage("/pixelplay startease <player|@a> <easecolor> <intro> <total> <outro>");
        sender.sendMessage("/pixelplay startimage <player|@a> <freeze> <durationSeconds> <url>");
        sender.sendMessage("/pixelplay startimagewithease <player|@a> <easecolor> <intro> <outro> <freeze> <durationSeconds> <url>");
    }

    private List<String> onlineNames(String prefix) {
        String p = prefix.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        out.add("@a");
        out.add("@s");
        for (Player pl : Bukkit.getOnlinePlayers()) {
            if (pl.getName().toLowerCase(Locale.ROOT).startsWith(p)) {
                out.add(pl.getName());
            }
        }
        return out;
    }

    private List<String> colorTab(String prefix) {
        String p = prefix.toLowerCase(Locale.ROOT);
        return Arrays.stream(new String[]{"white", "black", "whiteease", "blackease"})
            .filter(s -> s.startsWith(p))
            .collect(Collectors.toList());
    }

    private void handleStartVideo(CommandSender sender, String[] a) throws CommandException {
        if (a.length < 4) {
            throw new CommandException("Usage: /pixelplay startvideo <player|@a> <easecolor> <freeze:true|false> <url>");
        }
        List<Player> targets = resolveTargets(sender, a[0]);
        boolean white = parseWhite(a[1]);
        boolean freeze = parseBool(a[2]);
        String url = cleanUrl(join(3, a));
        validateUrl(url);
        byte[] payload = PixelPlayPayloads.startVideo(url, freeze, white, 0, 0);
        sendAll(targets, PixelPlayPayloads.CH_START_VIDEO, payload);
        sender.sendMessage("Sent start_video to " + targets.size() + " player(s).");
    }

    private void handleStartVideoWithEase(CommandSender sender, String[] a) throws CommandException {
        if (a.length < 6) {
            throw new CommandException("Usage: /pixelplay startvideowithease <player|@a> <easecolor> <intro> <outro> <freeze:true|false> <url>");
        }
        List<Player> targets = resolveTargets(sender, a[0]);
        boolean white = parseWhite(a[1]);
        double intro = clamp(Double.parseDouble(a[2]), 0, 600);
        double outro = clamp(Double.parseDouble(a[3]), 0, 600);
        boolean freeze = parseBool(a[4]);
        String url = cleanUrl(join(5, a));
        validateUrl(url);
        byte[] payload = PixelPlayPayloads.startVideo(url, freeze, white, intro, outro);
        sendAll(targets, PixelPlayPayloads.CH_START_VIDEO, payload);
        sender.sendMessage("Sent start_video (with ease) to " + targets.size() + " player(s).");
    }

    private void handleStopVideo(CommandSender sender, String[] a) throws CommandException {
        if (a.length < 1) {
            throw new CommandException("Usage: /pixelplay stopvideo <player|@a>");
        }
        List<Player> targets = resolveTargets(sender, a[0]);
        sendAll(targets, PixelPlayPayloads.CH_STOP_VIDEO, PixelPlayPayloads.stopVideo());
        sender.sendMessage("Sent stop_video to " + targets.size() + " player(s).");
    }

    private void handleStartAudio(CommandSender sender, String[] a, boolean withEase) throws CommandException {
        if (withEase) {
            if (a.length < 4) {
                throw new CommandException("Usage: /pixelplay startaudiowithsoundease <player|@a> <intro> <outro> <url>");
            }
            List<Player> targets = resolveTargets(sender, a[0]);
            double intro = clamp(Double.parseDouble(a[1]), 0, 600);
            double outro = clamp(Double.parseDouble(a[2]), 0, 600);
            String url = cleanUrl(join(3, a));
            validateUrl(url);
            sendAll(targets, PixelPlayPayloads.CH_START_AUDIO, PixelPlayPayloads.startAudio(url, intro, outro));
        } else {
            if (a.length < 2) {
                throw new CommandException("Usage: /pixelplay startaudio <player|@a> <url>");
            }
            List<Player> targets = resolveTargets(sender, a[0]);
            String url = cleanUrl(join(1, a));
            validateUrl(url);
            sendAll(targets, PixelPlayPayloads.CH_START_AUDIO, PixelPlayPayloads.startAudio(url, 0, 0));
        }
        sender.sendMessage("Sent start_audio.");
    }

    private void handleStopAudio(CommandSender sender, String[] a) throws CommandException {
        if (a.length < 1) {
            throw new CommandException("Usage: /pixelplay stopaudio <player|@a>");
        }
        List<Player> targets = resolveTargets(sender, a[0]);
        sendAll(targets, PixelPlayPayloads.CH_STOP_AUDIO, PixelPlayPayloads.stopAudio());
        sender.sendMessage("Sent stop_audio to " + targets.size() + " player(s).");
    }

    private void handleStartEase(CommandSender sender, String[] a) throws CommandException {
        if (a.length < 5) {
            throw new CommandException("Usage: /pixelplay startease <player|@a> <easecolor> <intro> <total> <outro>");
        }
        List<Player> targets = resolveTargets(sender, a[0]);
        boolean white = parseWhite(a[1]);
        double intro = clamp(Double.parseDouble(a[2]), 0, 600);
        double total = clamp(Double.parseDouble(a[3]), 0, 600);
        double outro = clamp(Double.parseDouble(a[4]), 0, 600);
        sendAll(targets, PixelPlayPayloads.CH_START_EASE, PixelPlayPayloads.startEase(white, intro, total, outro));
        sender.sendMessage("Sent start_ease to " + targets.size() + " player(s).");
    }

    private void handleStartImage(CommandSender sender, String[] a, boolean withEase) throws CommandException {
        if (withEase) {
            if (a.length < 7) {
                throw new CommandException("Usage: /pixelplay startimagewithease <player|@a> <easecolor> <intro> <outro> <freeze> <duration> <url>");
            }
            List<Player> targets = resolveTargets(sender, a[0]);
            boolean white = parseWhite(a[1]);
            double intro = clamp(Double.parseDouble(a[2]), 0, 600);
            double outro = clamp(Double.parseDouble(a[3]), 0, 600);
            boolean freeze = parseBool(a[4]);
            double duration = clamp(Double.parseDouble(a[5]), 0, 3600);
            String url = cleanUrl(join(6, a));
            validateUrl(url);
            sendAll(targets, PixelPlayPayloads.CH_START_IMAGE, PixelPlayPayloads.startImage(url, freeze, white, intro, outro, duration));
        } else {
            if (a.length < 4) {
                throw new CommandException("Usage: /pixelplay startimage <player|@a> <freeze> <duration> <url>");
            }
            List<Player> targets = resolveTargets(sender, a[0]);
            boolean freeze = parseBool(a[1]);
            double duration = clamp(Double.parseDouble(a[2]), 0, 3600);
            String url = cleanUrl(join(3, a));
            validateUrl(url);
            sendAll(targets, PixelPlayPayloads.CH_START_IMAGE, PixelPlayPayloads.startImage(url, freeze, false, 0, 0, duration));
        }
        sender.sendMessage("Sent start_image.");
    }

    private static String join(int from, String[] a) {
        return String.join(" ", Arrays.copyOfRange(a, from, a.length));
    }

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }

    private static boolean parseBool(String s) {
        if ("true".equalsIgnoreCase(s) || "1".equals(s)) {
            return true;
        }
        if ("false".equalsIgnoreCase(s) || "0".equals(s)) {
            return false;
        }
        throw new CommandException("Expected true or false, got: " + s);
    }

    private static boolean parseWhite(String color) {
        String c = color.toLowerCase(Locale.ROOT);
        return "white".equals(c) || "whiteease".equals(c);
    }

    private List<Player> resolveTargets(CommandSender sender, String token) throws CommandException {
        if ("@a".equalsIgnoreCase(token)) {
            return new ArrayList<>(Bukkit.getOnlinePlayers());
        }
        if ("@s".equalsIgnoreCase(token)) {
            if (sender instanceof Player p) {
                return List.of(p);
            }
            throw new CommandException("@s requires a player sender.");
        }
        Player p = Bukkit.getPlayerExact(token);
        if (p == null) {
            throw new CommandException("Player not found: " + token);
        }
        return List.of(p);
    }

    private void sendAll(List<Player> targets, String channel, byte[] data) throws CommandException {
        if (targets.isEmpty()) {
            throw new CommandException("No target players.");
        }
        for (Player p : targets) {
            p.sendPluginMessage(this, channel, data);
        }
    }

    private static String cleanUrl(String url) {
        return url.trim();
    }

    private static void validateUrl(String url) throws CommandException {
        if (url == null || url.isEmpty()) {
            throw new CommandException("Invalid URL.");
        }
        if (url.contains("youtube.com/watch")
            || url.contains("youtu.be/")
            || url.contains("youtube.com/shorts")
            || url.contains("m.youtube.com/watch")) {
            throw new CommandException(
                "YouTube no funciona con WaterMedia 3 (YoutubePlatform sin implementar en el cliente). "
                    + "Usa una URL HTTP(S) directa a un .mp4, .webm, etc."
            );
        }
        if (url.startsWith("http://") || url.startsWith("https://")) {
            try {
                new URI(url);
                return;
            } catch (Exception ignored) {
            }
        }
        throw new CommandException("Invalid URL.");
    }

    private static final class CommandException extends RuntimeException {
        CommandException(String message) {
            super(message);
        }
    }
}
