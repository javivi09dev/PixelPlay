package me.javivi.pp.registry;

import me.javivi.pp.block.entity.ScreenBlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class ModBlockEntities {
    private ModBlockEntities() {}

    public static BlockEntityType<ScreenBlockEntity> SCREEN;

    public static void register() {
        BlockEntityType<ScreenBlockEntity> type = BlockEntityType.Builder.<ScreenBlockEntity>create((pos, state) -> new ScreenBlockEntity(pos, state), ModBlocks.SCREEN).build();
        SCREEN = Registry.register(Registries.BLOCK_ENTITY_TYPE, Identifier.of("pixelplay", "screen"), type);
    }
}


