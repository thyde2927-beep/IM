package com.ironmod.client;

import com.ironmod.block.ModBlocks;
import com.ironmod.net.ActivateAbilityPayload;
import com.ironmod.net.SuitStatePayload;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;

public class IronModClient implements ClientModInitializer {
	/** Ability slots 1-4 map to the first four abilities listed in the suit's JSON. */
	public static final KeyMapping[] KEYS = new KeyMapping[4];

	@Override
	public void onInitializeClient() {
		int[] defaults = {InputConstants.KEY_Z, InputConstants.KEY_X, InputConstants.KEY_C, InputConstants.KEY_V};
		for (int i = 0; i < KEYS.length; i++) {
			KEYS[i] = KeyBindingHelper.registerKeyBinding(
					new KeyMapping("key.ironmod.ability_" + (i + 1), defaults[i], "key.categories.ironmod"));
		}

		BlockEntityRenderers.register(ModBlocks.SUIT_STAND_BE, SuitStandRenderer::new);

		ClientPlayNetworking.registerGlobalReceiver(SuitStatePayload.TYPE,
				(payload, context) -> ClientSuitState.update(payload));
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ClientSuitState.clear());
		ClientTickEvents.END_CLIENT_TICK.register(IronModClient::tick);
		HudRenderCallback.EVENT.register((graphics, delta) -> SuitHud.render(graphics));
	}

	private static void tick(Minecraft mc) {
		LocalPlayer player = mc.player;
		if (player == null) {
			ClientSuitState.clear();
			return;
		}

		for (int i = 0; i < KEYS.length; i++) {
			while (KEYS[i].consumeClick()) {
				if (ClientSuitState.active() && mc.screen == null) {
					ClientPlayNetworking.send(new ActivateAbilityPayload(i));
				}
			}
		}

		// Iron Man style thrust: while flying and holding W, fly where you look. Sprint = afterburners.
		if (ClientSuitState.flightOn && player.getAbilities().flying && mc.screen == null
				&& mc.options.keyUp.isDown()) {
			double speed = ClientSuitState.flightSpeed * (mc.options.keySprint.isDown() ? 1.6 : 1.0);
			player.setDeltaMovement(player.getLookAngle().scale(speed));
		}
	}

	public static String keyName(int slot) {
		return KEYS[slot].getTranslatedKeyMessage().getString();
	}
}
