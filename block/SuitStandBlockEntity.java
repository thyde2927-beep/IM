package com.ironmod.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Holds four armor pieces: index 0 = helmet, 1 = chestplate, 2 = leggings, 3 = boots. */
public class SuitStandBlockEntity extends BlockEntity {
	private final NonNullList<ItemStack> items = NonNullList.withSize(4, ItemStack.EMPTY);

	public SuitStandBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlocks.SUIT_STAND_BE, pos, state);
	}

	public ItemStack getItem(int index) {
		return items.get(index);
	}

	public void setItem(int index, ItemStack stack) {
		items.set(index, stack);
		setChanged();
		if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
	}

	public boolean isFull() {
		for (ItemStack s : items) if (s.isEmpty()) return false;
		return true;
	}

	public boolean isEmpty() {
		for (ItemStack s : items) if (!s.isEmpty()) return false;
		return true;
	}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);
		ContainerHelper.saveAllItems(tag, items, registries);
	}

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);
		for (int i = 0; i < items.size(); i++) items.set(i, ItemStack.EMPTY);
		ContainerHelper.loadAllItems(tag, items, registries);
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		return saveWithoutMetadata(registries);
	}
}
