package com.ironmod.block;

import com.ironmod.IronMod;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class ModBlocks {
	public static Block SUIT_STAND;
	public static Item SUIT_STAND_ITEM;
	public static BlockEntityType<SuitStandBlockEntity> SUIT_STAND_BE;

	public static void register() {
		SUIT_STAND = Registry.register(BuiltInRegistries.BLOCK, IronMod.id("suit_stand"),
				new SuitStandBlock(BlockBehaviour.Properties.of().strength(3.0f, 6.0f)
						.sound(SoundType.METAL).noOcclusion().lightLevel(state -> 8)));
		SUIT_STAND_ITEM = Registry.register(BuiltInRegistries.ITEM, IronMod.id("suit_stand"),
				new BlockItem(SUIT_STAND, new Item.Properties()));
		SUIT_STAND_BE = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, IronMod.id("suit_stand"),
				BlockEntityType.Builder.of(SuitStandBlockEntity::new, SUIT_STAND).build(null));
	}

	private ModBlocks() {}
}
