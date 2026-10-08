package com.ironmod.block;

import com.ironmod.item.SuitArmorItem;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Suit-Up Stand.
 *  - Right-click with a suit piece: puts it on the stand (one per slot).
 *  - Right-click with an empty hand: SUIT UP. Your current armor and the stand's pieces swap places.
 *  - Sneak + empty hand: take every piece off the stand.
 */
public class SuitStandBlock extends BaseEntityBlock {
	public static final MapCodec<SuitStandBlock> CODEC = simpleCodec(SuitStandBlock::new);
	private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 16, 14);
	private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

	public SuitStandBlock(Properties properties) {
		super(properties);
	}

	@Override
	public MapCodec<? extends BaseEntityBlock> codec() {
		return CODEC;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new SuitStandBlockEntity(pos, state);
	}

	@Override
	public RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL; // BaseEntityBlock defaults to INVISIBLE
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
		return SHAPE;
	}

	@Override
	public ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
										   Player player, InteractionHand hand, BlockHitResult hit) {
		if (!(stack.getItem() instanceof SuitArmorItem piece)) {
			return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
		}
		if (!(level.getBlockEntity(pos) instanceof SuitStandBlockEntity stand)) {
			return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
		}
		int index = switch (piece.getEquipmentSlot()) {
			case HEAD -> 0;
			case CHEST -> 1;
			case LEGS -> 2;
			default -> 3;
		};
		if (!stand.getItem(index).isEmpty()) return ItemInteractionResult.FAIL;
		if (!level.isClientSide) {
			ItemStack one = stack.copyWithCount(1);
			stack.consume(1, player);
			stand.setItem(index, one);
			level.playSound(null, pos, SoundEvents.ARMOR_EQUIP_IRON.value(), SoundSource.BLOCKS, 1.0f, 1.0f);
		}
		return ItemInteractionResult.sidedSuccess(level.isClientSide);
	}

	@Override
	public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (level.isClientSide) return InteractionResult.SUCCESS;
		if (!(level.getBlockEntity(pos) instanceof SuitStandBlockEntity stand)) return InteractionResult.PASS;

		if (player.isShiftKeyDown()) {
			// Take everything back.
			for (int i = 0; i < 4; i++) {
				ItemStack s = stand.getItem(i);
				if (!s.isEmpty()) {
					player.getInventory().placeItemBackInInventory(s);
					stand.setItem(i, ItemStack.EMPTY);
				}
			}
			return InteractionResult.CONSUME;
		}

		if (!stand.isFull()) {
			player.displayClientMessage(Component.translatable("message.ironmod.stand_incomplete"), true);
			return InteractionResult.CONSUME;
		}

		// SUIT UP: swap worn armor with the stand's contents.
		for (int i = 0; i < 4; i++) {
			ItemStack worn = player.getItemBySlot(SLOTS[i]).copy();
			player.setItemSlot(SLOTS[i], stand.getItem(i).copy());
			stand.setItem(i, worn);
		}
		ServerLevel server = (ServerLevel) level;
		server.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 40, 0.4, 0.7, 0.4, 0.05);
		level.playSound(null, pos, SoundEvents.ARMOR_EQUIP_NETHERITE.value(), SoundSource.PLAYERS, 1.2f, 0.9f);
		level.playSound(null, pos, SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.0f, 1.6f);
		return InteractionResult.CONSUME;
	}

	@Override
	public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
		if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof SuitStandBlockEntity stand) {
			for (int i = 0; i < 4; i++) {
				ItemStack s = stand.getItem(i);
				if (!s.isEmpty()) Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, s);
			}
		}
		super.onRemove(state, level, pos, newState, movedByPiston);
	}
}
