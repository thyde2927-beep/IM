package com.ironmod;

import com.ironmod.item.ModItems;
import com.ironmod.net.ActivateAbilityPayload;
import com.ironmod.net.SuitStatePayload;
import com.ironmod.suit.SuitDefinitions;
import com.ironmod.suit.SuitManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IronMod implements ModInitializer {
	public static final String MOD_ID = "ironmod";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static ResourceLocation id(String path) {
		return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		ModItems.register();

		// Networking: client -> server "use ability N", server -> client HUD state.
		PayloadTypeRegistry.playC2S().register(ActivateAbilityPayload.TYPE, ActivateAbilityPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(SuitStatePayload.TYPE, SuitStatePayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(ActivateAbilityPayload.TYPE,
				(payload, context) -> SuitManager.activate(context.player(), payload.slot()));

		// Suit stats/abilities live in data packs: data/<namespace>/suit_sets/<suit_id>.json
		ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(new SuitDefinitions());

		ServerTickEvents.END_SERVER_TICK.register(SuitManager::tick);
		ServerLivingEntityEvents.ALLOW_DAMAGE.register(SuitManager::allowDamage);
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> SuitManager.remove(handler.player));

		LOGGER.info("Iron Man Mod loaded.");
	}
}
