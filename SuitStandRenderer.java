package com.ironmod.client;

import com.ironmod.block.SuitStandBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Draws the stored armor pieces floating and slowly spinning above the stand. */
public class SuitStandRenderer implements BlockEntityRenderer<SuitStandBlockEntity> {
	private static final double[] HEIGHTS = {1.45, 1.05, 0.7, 0.4}; // helmet, chest, legs, boots
	private final ItemRenderer itemRenderer;

	public SuitStandRenderer(BlockEntityRendererProvider.Context context) {
		this.itemRenderer = context.getItemRenderer();
	}

	@Override
	public void render(SuitStandBlockEntity stand, float partialTick, PoseStack pose, MultiBufferSource buffer,
					   int light, int overlay) {
		Level level = stand.getLevel();
		if (level == null) return;
		float spin = (level.getGameTime() + partialTick) * 2.0f;
		int lit = LevelRenderer.getLightColor(level, stand.getBlockPos().above());
		for (int i = 0; i < 4; i++) {
			ItemStack stack = stand.getItem(i);
			if (stack.isEmpty()) continue;
			pose.pushPose();
			pose.translate(0.5, HEIGHTS[i], 0.5);
			pose.mulPose(Axis.YP.rotationDegrees(spin));
			pose.scale(0.6f, 0.6f, 0.6f);
			itemRenderer.renderStatic(stack, ItemDisplayContext.FIXED, lit, overlay, pose, buffer, level, i);
			pose.popPose();
		}
	}
}
