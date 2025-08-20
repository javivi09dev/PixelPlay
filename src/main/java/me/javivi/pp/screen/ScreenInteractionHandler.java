package me.javivi.pp.screen;

import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public class ScreenInteractionHandler {
    
    public static void init() {
        AttackBlockCallback.EVENT.register(ScreenInteractionHandler::onBlockAttack);
    }
    
    private static ActionResult onBlockAttack(PlayerEntity player, World world, Hand hand, BlockPos pos, Direction direction) {
        if (world.isClient) return ActionResult.PASS;
        
        BlockState state = world.getBlockState(pos);
        
        if (state.getBlock() instanceof me.javivi.pp.block.ScreenBlock) {
            if (ScreenSetupManager.hasActiveSetup(player.getUuid())) {
                ScreenSetupManager.handleBlockClick(player.getUuid(), pos, player.getServer().getCommandSource());
                return ActionResult.FAIL; 
            }
        }
        return ActionResult.PASS; 
    }
}
