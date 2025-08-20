package me.javivi.pp.screen;

import net.minecraft.util.math.BlockPos;
import net.minecraft.text.Text;
import net.minecraft.server.command.ServerCommandSource;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ScreenSetupManager {
    private static final Map<UUID, SetupState> activeSetups = new HashMap<>();
    
    public static class SetupState {
        private BlockPos firstBlock;
        private BlockPos secondBlock;
        private String presetId;
        
        public void setFirstBlock(BlockPos pos) { this.firstBlock = pos; }
        public void setSecondBlock(BlockPos pos) { this.secondBlock = pos; }
        public void setPresetId(String id) { this.presetId = id; }
        
        public BlockPos getFirstBlock() { return firstBlock; }
        public BlockPos getSecondBlock() { return secondBlock; }
        public String getPresetId() { return presetId; }
        
        public boolean isComplete() {
            return firstBlock != null && secondBlock != null && presetId != null;
        }
        
        public void reset() {
            firstBlock = null;
            secondBlock = null;
            presetId = null;
        }
    }
    
    public static void startSetup(UUID playerId) {
        activeSetups.put(playerId, new SetupState());
    }
    
    public static SetupState getSetup(UUID playerId) {
        return activeSetups.get(playerId);
    }
    
    public static void removeSetup(UUID playerId) {
        activeSetups.remove(playerId);
    }
    
    public static boolean hasActiveSetup(UUID playerId) {
        return activeSetups.containsKey(playerId);
    }
    
    public static void handleBlockClick(UUID playerId, BlockPos pos, ServerCommandSource source) {
        SetupState state = getSetup(playerId);
        if (state == null) return;
        
        if (state.getFirstBlock() == null) {
            state.setFirstBlock(pos);
            source.sendFeedback(() -> Text.literal("§a[PixelPlay] §ePrimer bloque seleccionado en " + pos + ". Haz click en el segundo bloque."), false);
        } else if (state.getSecondBlock() == null) {
            state.setSecondBlock(pos);
            source.sendFeedback(() -> Text.literal("§a[PixelPlay] §eSegundo bloque seleccionado en " + pos + ". Escribe: /pixelplay screen setup name <nombre_del_preset>"), false);
        }
    }
    
    public static void completeSetup(UUID playerId, String presetId, ServerCommandSource source) {
        SetupState state = getSetup(playerId);
        if (state == null) {
            source.sendError(Text.literal("§c[PixelPlay] No hay setup activo. Usa /pixelplay screen setup para empezar."));
            return;
        }
        
        if (state.getFirstBlock() == null || state.getSecondBlock() == null) {
            source.sendError(Text.literal("§c[PixelPlay] Setup incompleto. Selecciona ambos bloques primero."));
            return;
        }
        
        // Debug: mostrar información del setup
        source.sendFeedback(() -> Text.literal("§a[PixelPlay] §eDebug: Primer bloque: " + state.getFirstBlock() + ", Segundo bloque: " + state.getSecondBlock()), false);
        
        // Crear el preset
        BlockPos min = new BlockPos(
            Math.min(state.getFirstBlock().getX(), state.getSecondBlock().getX()),
            Math.min(state.getFirstBlock().getY(), state.getSecondBlock().getY()),
            Math.min(state.getFirstBlock().getZ(), state.getSecondBlock().getZ())
        );
        
        BlockPos max = new BlockPos(
            Math.max(state.getFirstBlock().getX(), state.getSecondBlock().getX()),
            Math.max(state.getFirstBlock().getY(), state.getSecondBlock().getY()),
            Math.max(state.getFirstBlock().getZ(), state.getSecondBlock().getZ())
        );
        
        ScreenPreset.addPreset(presetId, min, max);
        source.sendFeedback(() -> Text.literal("§a[PixelPlay] §aPreset '" + presetId + "' creado exitosamente!"), false);
        
        // Limpiar el setup
        removeSetup(playerId);
    }
}
