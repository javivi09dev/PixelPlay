package me.javivi.pp.registry;

import me.javivi.pp.block.ScreenBlock;
import net.minecraft.block.Block;
import net.minecraft.block.MapColor;
import net.minecraft.block.AbstractBlock;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class ModBlocks {
    private ModBlocks() {}

    public static final Block SCREEN = new ScreenBlock(AbstractBlock.Settings.create().mapColor(MapColor.BLACK).nonOpaque().strength(1.0f));

    public static void register() {
        Identifier id = Identifier.of("pixelplay", "screen");
        Registry.register(Registries.BLOCK, id, SCREEN);
        Registry.register(Registries.ITEM, id, new BlockItem(SCREEN, new Item.Settings()));
    }
}


