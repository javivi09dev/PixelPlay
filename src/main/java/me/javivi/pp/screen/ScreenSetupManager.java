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
            source.sendFeedback(() -> Text.translatable("message.pixelplay.screen_first_block_selected", pos.getX(), pos.getY(), pos.getZ()), false);
        } else if (state.getSecondBlock() == null) {
            state.setSecondBlock(pos);
            source.sendFeedback(() -> Text.translatable("message.pixelplay.screen_second_block_selected", pos.getX(), pos.getY(), pos.getZ()), false);
        }
    }
    
    public static void completeSetup(UUID playerId, String presetId, ServerCommandSource source) {
        SetupState state = getSetup(playerId);
        if (state == null) {
            source.sendError(Text.translatable("message.pixelplay.screen_no_active_setup"));
            return;
        }
        
        if (state.getFirstBlock() == null || state.getSecondBlock() == null) {
            source.sendError(Text.translatable("message.pixelplay.screen_setup_incomplete"));
            return;
        }
        
        // Create the preset
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
        source.sendFeedback(() -> Text.translatable("message.pixelplay.screen_preset_created", presetId), false);
        
        // Clean up setup
        removeSetup(playerId);
    }
}
